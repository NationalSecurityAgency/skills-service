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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class UserInfoSpecs extends DefaultIntSpec {

    def "get user info"() {
        String user = "UserInfoSpecsUserA"
        createService(user)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1, 40)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], user, new Date())

        when:
        def userInfo = skillsService.getUserInfoForProject(proj1.projectId, user)
        then:
        userInfo
        userInfo.userId == user.toLowerCase()
        userInfo.userIdForDisplay == user
    }

}
