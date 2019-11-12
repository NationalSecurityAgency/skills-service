package skills.intTests

import skills.intTests.utils.DefaultIntSpec

class StatusCheckSpecs extends DefaultIntSpec {

    def "check status of the service"() {
        when:
        def res = skillsService.getServiceStatus()
        then:
        res.status == 'OK'
    }
}
