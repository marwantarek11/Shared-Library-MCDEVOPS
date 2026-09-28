#!/usr/bin/env groovy
// Scan through the in-cluster Trivy server: an image (default) or a directory (type: 'rootfs', e.g. built app + sources).
// Scans once to JSON, then renders <report>.txt (console + artifact) and <report>.html ("<name> Report" link on the build).
//   trivyScan(image: 'registry/app:1')                                                     -> image scan, "Trivy Report"
//   trivyScan(type: 'rootfs', target: 'Application', scanners: 'vuln,secret',
//             id: 'trivy-deps', name: 'Dependency Scan')                                   -> jars + secrets, "Dependency Scan Report"
def call(Map args) {
    String type          = args.type ?: 'image'
    String target        = args.target ?: args.image
    String server        = args.server ?: 'http://trivy.trivy.svc:4954'
    String credentialsId = args.credentialsId ?: 'trivy-token'
    String severity      = args.severity ?: 'HIGH,CRITICAL'
    String scanners      = args.scanners ? "--scanners ${args.scanners}" : ''
    String extraArgs     = args.extraArgs ?: ''
    String id            = args.id ?: 'trivy'           // Warnings NG id, also names the report files
    String name          = args.name ?: 'Trivy'
    String report        = id == 'trivy' ? 'trivy-report' : "${id}-report"
    int exitCode         = args.failBuild ? 1 : 0

    echo "Scanning ${type} ${target} with Trivy (${severity})..."
    container('trivy') {
        withCredentials([string(credentialsId: credentialsId, variable: 'TRIVY_TOKEN')]) {
            withEnv(["TRIVY_SERVER=${server}", 'TRIVY_INSECURE=true', 'TRIVY_NO_PROGRESS=true', 'TRIVY_DISABLE_VEX_NOTICE=true',
                     // Fall back to ghcr.io when the mirror.gcr.io Java DB download fails
                     'TRIVY_JAVA_DB_REPOSITORY=mirror.gcr.io/aquasec/trivy-java-db:1,ghcr.io/aquasecurity/trivy-java-db:1']) {
                sh "trivy ${type} ${scanners} ${extraArgs} --severity ${severity} --format json --output ${report}.json ${target}"
                sh "trivy convert --format table --output ${report}.txt ${report}.json"
                sh "trivy convert --format template --template @/contrib/html.tpl --output ${report}.html ${report}.json"
                sh "cat ${report}.txt"
                archiveArtifacts artifacts: "${report}.*", allowEmptyArchive: true
                publishHTML(target: [reportName: "${name} Report", reportDir: '.', reportFiles: "${report}.html",
                                     keepAll: true, alwaysLinkToLastBuild: true, allowMissing: false])

                // Warnings NG: severity charts, trend across builds, new/fixed/outstanding tracking
                recordIssues(tools: [trivy(pattern: "${report}.json", id: id, name: name)],
                             enabledForFailure: true, skipPublishingChecks: true)

                // Severity counts on the build list / history
                def counts = severity.split(',').collect { sev ->
                    def n = sh(returnStdout: true, script: "grep -o '\"Severity\": *\"${sev}\"' ${report}.json | wc -l").trim()
                    "${sev} ${n}"
                }
                currentBuild.description = ([currentBuild.description, "${name}: ${counts.join(' · ')}"] - null).join('\n')
                int rc = sh(returnStatus: true, script: "trivy convert --severity ${severity} --exit-code ${exitCode} --format table --output /dev/null ${report}.json")
                if (rc != 0) {
                    error("${name} found ${severity} vulnerabilities (exit code ${rc})")
                }
            }
        }
    }
}
