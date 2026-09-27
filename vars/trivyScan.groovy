#!/usr/bin/env groovy
// Scan an image through the in-cluster Trivy server; archives trivy-report.txt
def call(Map args) {
    String image         = args.image
    String server        = args.server ?: 'http://trivy.trivy.svc:4954'
    String credentialsId = args.credentialsId ?: 'trivy-token'
    String severity      = args.severity ?: 'HIGH,CRITICAL'
    int exitCode         = args.failBuild ? 1 : 0

    echo "Scanning ${image} with Trivy (${severity})..."
    container('trivy') {
        withCredentials([string(credentialsId: credentialsId, variable: 'TRIVY_TOKEN')]) {
            withEnv(["TRIVY_SERVER=${server}", 'TRIVY_INSECURE=true', 'TRIVY_NO_PROGRESS=true']) {
                int rc = sh(returnStatus: true, script: "trivy image --severity ${severity} --format table --output trivy-report.txt --exit-code ${exitCode} ${image}")
                sh 'cat trivy-report.txt || true'
                archiveArtifacts artifacts: 'trivy-report.txt', allowEmptyArchive: true
                if (rc != 0) {
                    error("Trivy scan failed or found ${severity} vulnerabilities (exit code ${rc})")
                }
            }
        }
    }
}
