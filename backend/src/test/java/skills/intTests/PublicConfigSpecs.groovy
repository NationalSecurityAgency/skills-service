package skills.intTests

import skills.intTests.utils.DefaultIntSpec

class PublicConfigSpecs extends DefaultIntSpec {

    def "retrieve public configs"() {
        when:
        def config = skillsService.getPublicConfigs()
        then:
        config
        config.descriptionMaxLength == "2000"
    }
}
