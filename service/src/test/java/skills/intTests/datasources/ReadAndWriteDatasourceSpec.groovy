/**
 * Copyright 2025 SkillTree
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
package skills.intTests.datasources

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.MockUserInfoService
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.storage.model.UserTag
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import spock.lang.IgnoreIf

class ReadAndWriteDatasourceSpec extends DefaultIntSpec {

    def "new user attributes are saved on read"() {
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = "user1"
        when:
        UserAttrs user1_t0 = userAttrsRepo.findByUserIdIgnoreCase(userId)
        List<UserRole> roles_t0 = userRoleRepo.findAllByUserId(userId)
        def res = skillsService.getSkillSummary(userId, proj1.projectId)
        UserAttrs user1_t1 = userAttrsRepo.findByUserIdIgnoreCase(userId)
        List<UserRole> roles_t1 = userRoleRepo.findAllByUserId(userId)
        then:
        res
        user1_t0 == null
        user1_t1.userId == userId

        !roles_t0
        // role is only inserted in pki mode
        System.getenv("SPRING_PROFILES_ACTIVE") != 'pki' || roles_t1.roleName == [RoleName.ROLE_APP_USER]
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "update user attributes are saved on read in pki mode"() {
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = getRandomUsers(1)[0]
        when:
        skillsService.getSkillSummary(userId, proj1.projectId)
        UserAttrs user1_t0 = userAttrsRepo.findByUserIdIgnoreCase(userId)
        String firstNameOrig = user1_t0.firstName
        assert firstNameOrig != null
        user1_t0.firstName = "newOne"
        userAttrsRepo.save(user1_t0)
        UserAttrs user1_t1 = userAttrsRepo.findByUserIdIgnoreCase(userId)
        assert user1_t1.firstName != firstNameOrig
        skillsService.getSkillSummary(userId, proj1.projectId)
        UserAttrs user1_t2 = userAttrsRepo.findByUserIdIgnoreCase(userId)

        then:
        user1_t1.firstName != firstNameOrig
        user1_t2.firstName == firstNameOrig
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "new user tag is saved on read"() {
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = getRandomUsers(1)[0]
        MockUserInfoService.addUserTags(userId, "tag1", "value1")
        SkillsService otherUser = createService(userId)
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        userAttrs.userTagsLastUpdated = (new Date() - 1)
        userAttrsRepo.save(userAttrs)
        when:
        List<UserTag> userTags_t0 = userTagRepo.findAllByUserId(userId)
        def res = otherUser.getSkillsSummaryForCurrentUser(proj1.projectId)
        List<UserTag> userTags_t1 = userTagRepo.findAllByUserId(userId)
        then:
        res
        !userTags_t0
        userTags_t1.size() == 1
        userTags_t1.userId == [userId]
    }
}
