#!/usr/bin/env groovy

// sonarServer: name of the SonarQube installation in Manage Jenkins (blank = the only one configured)
def call(String sonarServer = null){ 
	def analyze = {
		    sh 'chmod +x gradlew'
        	sh "./gradlew sonar" 
	}
	if (sonarServer) {
		withSonarQubeEnv(sonarServer, analyze)
	} else {
		withSonarQubeEnv(analyze)
	}
}
