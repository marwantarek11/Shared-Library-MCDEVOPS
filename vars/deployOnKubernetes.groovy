#!/usr/bin/env groovy
// Apply manifests in the current dir using the pod's service account (jenkins-deployer)
def call(String nameSpace, String deploymentName) {
    container('kubectl') {
        sh "kubectl apply -f . --namespace=${nameSpace}"
        sh "kubectl rollout status deployment/${deploymentName} --namespace=${nameSpace} --timeout=180s"
    }
}
