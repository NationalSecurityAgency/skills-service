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
package skills.auth


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
