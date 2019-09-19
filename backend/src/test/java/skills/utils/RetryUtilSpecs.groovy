package skills.utils

import spock.lang.Specification

class RetryUtilSpecs extends Specification {

    def "retry when exception is thrown"() {
        when:
        boolean thrownE = false
        Integer res = RetryUtil.withRetry(1) {
            if (!thrownE) {
                thrownE = true
                throw new RuntimeException("hi")
            }

            return 5
        }

        then:
        thrownE
        res == 5
    }

    def "retry 2 times"() {
        when:
        int numRuns = 0
        Integer res = RetryUtil.withRetry(2) {
            if (numRuns < 2) {
                numRuns++
                throw new RuntimeException("hi")
            }

            return 5
        }

        then:
        numRuns == 2
        res == 5
    }

    def "retry multiple times and still fail"() {
        when:
        int numRuns = 0
        RetryUtil.withRetry(5) {
            numRuns++
            throw new RuntimeException("hi")
        }

        then:
        RuntimeException r = thrown(RuntimeException)
        r.message == "hi"
        numRuns == 6 // retries = runs + 1

    }
}
