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

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import skills.auth.pki.PkiUserLookup
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

    @Autowired
    PkiUserLookup pkiUserLookup

    def "new user attributes are saved on read"() {
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = "user1"
        rootUser.invalidateUserCache()
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
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = getRandomUsers(1)[0]
        when:
        rootUser.invalidateUserCache()
        skillsService.getSkillSummary(userId, proj1.projectId)
        UserAttrs user1_t0 = userAttrsRepo.findByUserIdIgnoreCase(userId)
        String firstNameOrig = user1_t0.firstName
        assert firstNameOrig != null
        user1_t0.firstName = "newOne"
        userAttrsRepo.save(user1_t0)
        UserAttrs user1_t1 = userAttrsRepo.findByUserIdIgnoreCase(userId)
        assert user1_t1.firstName != firstNameOrig
        rootUser.invalidateUserCache()
        skillsService.getSkillSummary(userId, proj1.projectId)
        UserAttrs user1_t2 = userAttrsRepo.findByUserIdIgnoreCase(userId)

        then:
        user1_t1.firstName != firstNameOrig
        user1_t2.firstName == firstNameOrig
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "update user attributes are saved on read in pki mode for multiple users"() {
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        List<String> userIds = getRandomUsers(3)
        when:
        rootUser.invalidateUserCache()
        skillsService.getSkillSummary(userIds[0], proj1.projectId)
        skillsService.getSkillSummary(userIds[1], proj1.projectId)
        skillsService.getSkillSummary(userIds[2], proj1.projectId)
        List<UserAttrs> userAttrs_t0 = userAttrsRepo.findAll().collect { it}
        List<String> origNames = userAttrs_t0.collect { it.firstName }.sort()
        userAttrs_t0.each { it.firstName = "newOne" }
        userAttrsRepo.saveAllAndFlush(userAttrs_t0)
        List<UserAttrs> userAttrs_t1 = userAttrsRepo.findAll().collect { it}
        assert userAttrs_t1.collect { it.firstName}.sort() != origNames
        rootUser.invalidateUserCache()
        skillsService.getSkillSummary(userIds[0], proj1.projectId)
        skillsService.getSkillSummary(userIds[1], proj1.projectId)
        skillsService.getSkillSummary(userIds[2], proj1.projectId)
        List<UserAttrs> userAttrs_t2 = userAttrsRepo.findAll().collect { it}

        then:
        userAttrs_t2.collect { it.firstName}.sort() == origNames
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "new user tag is saved on read"() {
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = getRandomUsers(1)[0]
        SkillsService otherUser = createService(new SkillsService.UseParams(username: userId, inPkiModeInitCallToAuth: false))
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        userAttrs.userTagsLastUpdated = new Date(userAttrs.userTagsLastUpdated.time - (1000*60*60*8))
        userAttrsRepo.save(userAttrs)
        when:
        MockUserInfoService.addUserTags(userId, "tag1", "value1")
        List<UserTag> userTags_t0 = userTagRepo.findAllByUserId(userId)
        rootUser.invalidateUserCache()
        def res = otherUser.getSkillsSummaryForCurrentUser(proj1.projectId)
        List<UserTag> userTags_t1 = userTagRepo.findAllByUserId(userId)
        then:
        res
        !userTags_t0
        userTags_t1.size() == 1
        userTags_t1.userId == [userId]
        userTags_t1.key == ["tag1"]
        userTags_t1.value == ["value1"]
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "new user tag is updated on read"() {
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = getRandomUsers(1)[0]
        MockUserInfoService.addUserTags(userId, "tag1", "value1")
        SkillsService otherUser = createService(new SkillsService.UseParams(username: userId, inPkiModeInitCallToAuth: false))
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        when:
        List<UserTag> userTags_t0 = userTagRepo.findAllByUserId(userId)
        rootUser.invalidateUserCache()
        def res = otherUser.getSkillsSummaryForCurrentUser(proj1.projectId)
        List<UserTag> userTags_t1 = userTagRepo.findAllByUserId(userId)

        MockUserInfoService.resetUserTags()
        MockUserInfoService.addUserTags(userId, "tag1", "value2")

        rootUser.invalidateUserCache()
        def res2 = otherUser.getSkillsSummaryForCurrentUser(proj1.projectId)
        List<UserTag> userTags_t2 = userTagRepo.findAll().findAll { it.userId.equalsIgnoreCase(userId) }

        then:
        res
        res2
        !userTags_t0
        userTags_t1.size() == 1
        userTags_t1.userId == [userId]
        userTags_t1.key == ["tag1"]
        userTags_t1.value == ["value1"]

        userTags_t2.userId == [userId]
        userTags_t2.key == ["tag1"]
        userTags_t2.value == ["value2"]
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "new user tag is not saved when less than 24 hours of last update"() {
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        String userId = getRandomUsers(1)[0]
        SkillsService otherUser = createService(new SkillsService.UseParams(username: userId, inPkiModeInitCallToAuth: false))
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)

        MockUserInfoService.addUserTags(userId, "tag1", "value1")
        when:
        List<UserTag> userTags_t0 = userTagRepo.findAllByUserId(userId)
        rootUser.invalidateUserCache()
        def res = otherUser.getSkillsSummaryForCurrentUser(proj1.projectId)
        List<UserTag> userTags_t1 = userTagRepo.findAllByUserId(userId)

        MockUserInfoService.resetUserTags()
        MockUserInfoService.addUserTags(userId, "tag1", "value2")

        def res2 = otherUser.getSkillsSummaryForCurrentUser(proj1.projectId)
        List<UserTag> userTags_t2 = userTagRepo.findAll().findAll { it.userId.equalsIgnoreCase(userId) }

        then:
        res
        res2
        !userTags_t0
        userTags_t1.size() == 1
        userTags_t1.userId == [userId]
        userTags_t1.key == ["tag1"]
        userTags_t1.value == ["value1"]

        userTags_t2.userId == [userId]
        userTags_t2.key == ["tag1"]
        userTags_t2.value == ["value1"]
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "new user tag is updated on read for multiple users"() {
        SkillsService rootUser = createRootSkillService()
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)

        List<String> userIds = getRandomUsers(3)
        List<SkillsService> users = userIds.collect { createService(it) }
        MockUserInfoService.addUserTags(userIds[0], "tag1", "value1")
        MockUserInfoService.addUserTags(userIds[1], "tag1", "value2")
        MockUserInfoService.addUserTags(userIds[2], "tag1", "value1")

        Closure getSkillsSummaryForAllUsers = {
            rootUser.invalidateUserCache()
            users.collect {
                it.getSkillsSummaryForCurrentUser(proj1.projectId)
            }
        }

        when:
        List<UserTag> userTags_t0 = userTagRepo.findAll().collect { it}
        getSkillsSummaryForAllUsers.call()

        List<UserTag> userTags_t1 = userTagRepo.findAll().collect { it}

        MockUserInfoService.removeUserTagsForUser(users[1].userName)
        MockUserInfoService.addUserTags(users[1].userName, "tag1", "value3")
        MockUserInfoService.removeUserTagsForUser(users[2].userName)
        MockUserInfoService.addUserTags(users[2].userName, "tag1", "value4")
        MockUserInfoService.addUserTags(users[2].userName, "tag2", "value5")

        getSkillsSummaryForAllUsers.call()
        List<UserTag> userTags_t2 = userTagRepo.findAll().collect { it}

        then:
        !userTags_t0
        userTags_t1.size() == 3
        List<UserTag> user1Tags_t1 = userTags_t1.findAll { it.userId.equalsIgnoreCase(users[0].userName) }
        user1Tags_t1.key == ["tag1"]
        user1Tags_t1.value == ["value1"]
        List<UserTag> user2Tags_t1 = userTags_t1.findAll { it.userId.equalsIgnoreCase(users[1].userName) }
        user2Tags_t1.key == ["tag1"]
        user2Tags_t1.value == ["value2"]
        List<UserTag> user3Tags_t1 = userTags_t1.findAll { it.userId.equalsIgnoreCase(users[2].userName) }
        user3Tags_t1.key == ["tag1"]
        user3Tags_t1.value == ["value1"]


        userTags_t2.size() == 4
        List<UserTag> user1Tags_t2 = userTags_t2.findAll { it.userId.equalsIgnoreCase(users[0].userName) }
        user1Tags_t2.key == ["tag1"]
        user1Tags_t2.value == ["value1"]
        List<UserTag> user2Tags_t2 = userTags_t2.findAll { it.userId.equalsIgnoreCase(users[1].userName) }
        user2Tags_t2.key == ["tag1"]
        user2Tags_t2.value == ["value3"]
        List<UserTag> user3Tags_t2 = userTags_t2.findAll { it.userId.equalsIgnoreCase(users[2].userName) }
        user3Tags_t2.find { it.key == "tag1" }.value == "value4"
        user3Tags_t2.find { it.key == "tag2" }.value == "value5"
    }
}
