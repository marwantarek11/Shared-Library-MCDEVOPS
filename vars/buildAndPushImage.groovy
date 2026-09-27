#!/usr/bin/env groovy
// Build and push with Kaniko (no Docker daemon needed on containerd/kubeadm clusters)
def call(String image, boolean insecureRegistry = true) {
    echo "Building and Pushing ${image} with Kaniko..."
    def contextDir = pwd()
    container('kaniko') {
        sh """
            /kaniko/executor \\
              --context=dir://${contextDir} \\
              --dockerfile=${contextDir}/Dockerfile \\
              --destination=${image} \\
              ${insecureRegistry ? '--insecure --skip-tls-verify' : ''}
        """
    }
}
