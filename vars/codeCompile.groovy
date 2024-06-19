#!/usr/bin/env groovy
def call() {
	echo "Running Code Compile..."
	sh 'mvn clean compile'	
}