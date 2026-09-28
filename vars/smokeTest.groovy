#!/usr/bin/env groovy
// Hit the deployed app through its Service and require HTTP 200 (retries while the new pods settle).
//   smokeTest(url: 'http://my-svc.my-ns.svc:8080/')
def call(Map args) {
    String url   = args.url
    int attempts = args.attempts ?: 10
    int delay    = args.delaySeconds ?: 6

    echo "Smoke testing ${url}..."
    container('kubectl') {
        retry(attempts) {
            // -f: non-2xx is a failure; the sleep only runs after a failed attempt
            int rc = sh(returnStatus: true, script: "curl -fsS -o /dev/null --max-time 5 -w 'HTTP %{http_code} in %{time_total}s\\n' ${url}")
            if (rc != 0) {
                sleep(time: delay, unit: 'SECONDS')
                error("Smoke test failed for ${url} (curl exit ${rc})")
            }
        }
    }
}
