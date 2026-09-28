#!/usr/bin/env groovy
// Roll a deployment back to its previous revision (used from post { failure } of the deploy / smoke test stages).
// Never throws: on the very first deploy there is no previous revision to go back to.
def call(String nameSpace, String deploymentName) {
    echo "Rolling back deployment/${deploymentName} in ${nameSpace}..."
    container('kubectl') {
        int rc = sh(returnStatus: true, script: """
            kubectl rollout undo deployment/${deploymentName} --namespace=${nameSpace} &&
            kubectl rollout status deployment/${deploymentName} --namespace=${nameSpace} --timeout=180s
        """)
        if (rc == 0) {
            currentBuild.description = ([currentBuild.description, 'Rolled back'] - null).join('\n')
        } else {
            echo "WARNING: rollback of deployment/${deploymentName} failed (exit ${rc}) - check the cluster manually"
        }
    }
}
