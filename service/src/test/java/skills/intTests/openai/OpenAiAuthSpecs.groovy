package skills.intTests.openai

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec

@Slf4j
class OpenAiAuthSpecs extends DefaultIntSpec {

    def "logged in user can access the open ai endpoints"() {
        when:
        true
        then:
        println "done"
    }

}
