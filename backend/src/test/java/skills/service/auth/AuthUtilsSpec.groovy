package skills.service.auth

import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

class AuthUtilsSpec extends Specification {

    def "pick first project is if it appears twice in the url"(){
        HttpServletRequest httpServletRequest = Mock()
        httpServletRequest.getServletPath() >> "/admin/projects/test/skills/other1/shared/projects/testProj"

        when:
        String res = AuthUtils.getProjectIdFromRequest(httpServletRequest)
        then:
        res == "test"
    }

    def "parse project name from url"(){
        HttpServletRequest httpServletRequest = Mock()
        httpServletRequest.getServletPath() >> url

        expect:
        AuthUtils.getProjectIdFromRequest(httpServletRequest) == projectId

        where:
        projectId | url
        "test"    | "/admin/projects/test/shared"
        "test"    | "/admin/projects/test/dependency/graph"
    }

}
