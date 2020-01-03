package skills.intTests.cypress


import skills.intTests.utils.SkillsService
import spock.lang.Ignore

class CypressSpecs extends CypressIntSpec {

    @Ignore
    def "CYPRESS TEST: login_spec.js" () {
        SkillsService myService = createService('dimay@evoforge.org', 'password')
        myService.createProject([projectId: "ProjectA", name: "ProjectA"])
        when:
        runCypress()
        then:
        true
    }

    @Ignore
    def "CYPRESS TEST: project_management_spec.js"() {
        when:
        runCypress()
        then:
        true
    }
}
