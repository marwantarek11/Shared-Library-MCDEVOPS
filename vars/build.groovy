#!/usr/bin/env groovy
// skipTests: true when an earlier stage already ran the tests (also skips `clean` so compiled classes are reused)
def call(Map args = [:]) {
	echo "Building App..."
	    sh 'chmod +x ./gradlew'
        sh(args.skipTests ? './gradlew build -x test' : './gradlew clean build')
}
