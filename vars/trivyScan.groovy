#!/usr/bin/env groovy
// Scan an image through the in-cluster Trivy server.
// Scans once to JSON, then renders trivy-report.txt (console + artifact) and trivy-report.html ("Trivy Report" link on the build).
def call(Map args) {
    String image         = args.image
    String server        = args.server ?: 'http://trivy.trivy.svc:4954'
    String credentialsId = args.credentialsId ?: 'trivy-token'
    String severity      = args.severity ?: 'HIGH,CRITICAL'
    int exitCode         = args.failBuild ? 1 : 0

    echo "Scanning ${image} with Trivy (${severity})..."
    container('trivy') {
        withCredentials([string(credentialsId: credentialsId, variable: 'TRIVY_TOKEN')]) {
            withEnv(["TRIVY_SERVER=${server}", 'TRIVY_INSECURE=true', 'TRIVY_NO_PROGRESS=true', 'TRIVY_DISABLE_VEX_NOTICE=true']) {
                sh "trivy image --severity ${severity} --format json --output trivy-report.json ${image}"
                sh 'trivy convert --format table --output trivy-report.txt trivy-report.json'
                sh 'trivy convert --format template --template @/contrib/html.tpl --output trivy-report.html trivy-report.json'
                sh 'cat trivy-report.txt'
                archiveArtifacts artifacts: 'trivy-report.*', allowEmptyArchive: true
                publishHTML(target: [reportName: 'Trivy Report', reportDir: '.', reportFiles: 'trivy-report.html',
                                     keepAll: true, alwaysLinkToLastBuild: true, allowMissing: false])
                int rc = sh(returnStatus: true, script: "trivy convert --severity ${severity} --exit-code ${exitCode} --format table --output /dev/null trivy-report.json")
                if (rc != 0) {
                    error("Trivy found ${severity} vulnerabilities (exit code ${rc})")
                }
            }
        }
    }
}
