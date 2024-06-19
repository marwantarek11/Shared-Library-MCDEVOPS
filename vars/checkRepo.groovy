#!usr/bin/env groovy
def call() {
	echo "checking Git Repo..."
	git branch: 'dev', credentialsId: 'GitHub', url: 'https://github.com/marwantarek11/MultiCloudDevOpsProject.git'
}
