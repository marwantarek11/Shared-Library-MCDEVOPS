#!/usr/bin/env groovy

// KubernetesCredentialsID 'KubeConfig file'
def call(String imageName) {
    
    // Edit deployment.yaml with new Docker Hub image
    sh "sed -i 's|image:.*|image: ${imageName}:${BUILD_NUMBER}|g' deployment.yml"

}
