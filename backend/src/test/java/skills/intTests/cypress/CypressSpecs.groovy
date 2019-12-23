package skills.intTests.cypress


import skills.intTests.utils.SkillsService

class CypressSpecs extends CypressIntSpec {

    def "CYPRESS TEST: login_spec.js" () {
        SkillsService myService = createService('dimay@evoforge.org', 'password')
        myService.createProject([projectId: "ProjectA", name: "ProjectA"])
        when:
        runCypress()
        then:
        true
    }

    def "CYPRESS TEST: project_management_spec.js"() {
        when:
        runCypress()
        then:
        true
    }
}
