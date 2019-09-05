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

}
