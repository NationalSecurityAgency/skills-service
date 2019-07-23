package skills.services

import skills.services.IdFormatValidator
import spock.lang.Specification
import skills.controller.exceptions.SkillException

class IdFormatValidatorSpec extends Specification {

    def "allow alphanumeric and underscores"() {
        when:
        IdFormatValidator.validate(id)

        then:
        noExceptionThrown()

        where:
        id << ["Works_Well", "1sK20_dk28939", "1234"]
    }

    def "do not allow special characters"() {
        when:
        IdFormatValidator.validate(id)

        then:
        thrown(SkillException)

        where:
        id << ["Works_W\$ll", "1*K20_dk28939"]
    }

    def "fail if id is less than 3 chars"(){
        when:
        IdFormatValidator.validate(id)

        then:
        thrown(SkillException)

        where:
        id << ["1", "12"]
    }

    def "3 chars id is ok"(){
        when:
        IdFormatValidator.validate(id)

        then:
        noExceptionThrown()

        where:
        id << ["123", "abc"]
    }

    def "fail if id is more than 50 chars"() {
        String str = ""
        51.times {
            str += "a"
        }
        when:
        IdFormatValidator.validate(str)

        then:
        thrown(SkillException)
    }

    def "50 char id is ok"() {
        String str = ""
        50.times {
            str += "a"
        }
        when:
        IdFormatValidator.validate(str)

        then:
        noExceptionThrown()
    }

}
