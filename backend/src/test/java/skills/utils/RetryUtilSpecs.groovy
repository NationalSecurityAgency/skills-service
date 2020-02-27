/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.utils

import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillExceptionBuilder
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


    def "retry multiple times and still fail - log as code executes"() {
        when:
        int numRuns = 0
        RetryUtil.withRetry(5, false) {
            numRuns++
            throw new RuntimeException("hi")
        }

        then:
        RuntimeException r = thrown(RuntimeException)
        r.message == "hi"
        numRuns == 6 // retries = runs + 1
    }

    def "do not retry if SkillException.doNotRetry=true"() {
        when:
        int count = 0
        Integer res = RetryUtil.withRetry(1) {
            count++
            throw new SkillExceptionBuilder().msg("hi").doNotRetry(true).build()
            return 5
        }

        then:
        SkillException e = thrown(SkillException)
        e.message == "hi"
        count == 1
    }
}
