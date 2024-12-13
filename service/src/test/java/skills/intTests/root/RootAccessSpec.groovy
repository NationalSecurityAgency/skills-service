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
package skills.intTests.root

import groovy.json.JsonOutput
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.controller.result.model.SettingsResult
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.ProjAdminService
import skills.services.settings.SettingsService
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import spock.lang.IgnoreIf
import spock.lang.Requires

import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class RootAccessSpec extends DefaultIntSpec {

    @Autowired
    UserAchievedLevelRepo achievedRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SettingsService settingsService

    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService
    String nonRootUserId = 'foo@bar.com'
    SkillsService nonRootSkillsService
    String secondRoot = "bob@email.com"
    SkillsService secondRootSkillService

    def setup() {
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        secondRootSkillService = createService(secondRoot, 'bbbbbbbbbbbbbbb')
        nonRootSkillsService = createService(nonRootUserId)

        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
        if(!secondRootSkillService.isRoot()) {
            rootSkillsService.grantRootRole(secondRoot)
            assert secondRootSkillService.isRoot()
        }
        if (nonRootSkillsService.isRoot()) {
            rootSkillsService.removeRootRole(nonRootUserId)
            assert !nonRootSkillsService.isRoot()
        }
    }

    def 'prevent a user being created with root privileges if a root account already exists'() {
        setup:
        String userId = RandomStringUtils.randomAlphanumeric(14)
        Map<String, String> userInfo = [
                firstName: 'A',
                lastName : 'B',
                email    : userId,
                password : 'aaaaaaaa',
        ]

        when:
        nonRootSkillsService.createRootAccount(userInfo)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('root user already exists')
    }

    def 'prevent granting root user privileges if a root account already exists'() {
        when:
        nonRootSkillsService.grantRoot()

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('root user already exists')
    }

    def 'verify retrieving root users as a root user'() {
        when:
        def result = rootSkillsService.getRootUsers()

        then:
        result.size() >= 1
        result.find {it.userId == 'jh@dojo.com'}
        result.each {
            assert it.roleName == 'ROLE_SUPER_DUPER_USER'
            true
        }
    }

    def 'verify non-root users cannot retrieve root user information'() {
        when:
        nonRootSkillsService.getRootUsers()

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'verify retrieving non-root users as a root user'() {
        when:
        def result = rootSkillsService.getNonRootUsers("skills")

        then:
        result.size() >= 1
        !result.find {it.userId == 'jh@dojo.com'}
        result.find {it.userId == 'skills@skills.org'}
    }

    def 'verify non-root users cannot retrieve user information about non-root users'() {
        when:
        nonRootSkillsService.getNonRootUsers("skills")

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'verify isRoot with root user'() {
        expect:
        rootSkillsService.isRoot() == true
    }

    def 'verify isRoot with non-root user'() {
        expect:
        nonRootSkillsService.isRoot() == false
    }

    def 'verify root users can add other root users'() {
        setup:
        def originalRootUsers = rootSkillsService.getRootUsers()
        assert !originalRootUsers.find {it.userId == nonRootUserId}

        when:
        rootSkillsService.addRootRole(nonRootUserId)

        then:
        def rootUsers = rootSkillsService.getRootUsers()
        rootUsers.size() == originalRootUsers.size() + 1
        rootUsers.find {it.userId == nonRootUserId}

        cleanup:
        rootSkillsService.removeRootRole(nonRootUserId)
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'verify the server sends a failure when a root user tries to add root privileges to a user that does not exist'() {
        when:
        rootSkillsService.addRootRole(RandomStringUtils.randomAlphanumeric(14))

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('does not exist')
    }


    @Requires({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'verify the server sends a failure when a root user tries to add root privileges to a user that does not exist - 2-way ssl'() {
        when:
        rootSkillsService.addRootRole("doesNotExist")

        then:
        // because 2-way ssl uses a user-info-service, a non-existant user can result in different error messages than
        // the form based version
        thrown(Exception)
    }

    def 'verify non-root users cannot add root users'() {
        when:
        nonRootSkillsService.addRootRole("skills@skills.org")

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'verify the server will prevent all root users from being removed'() {
        setup:
        Collection rootUsers = rootSkillsService.getRootUsers()
        rootUsers.removeAll {it.userId == ultimateRoot}

        when:
        rootUsers.each {
            rootSkillsService.removeRootRole(it.userId)
        }
        // only one root user should remain
        rootSkillsService.removeRootRole(ultimateRoot)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST

        cleanup:
        rootUsers.each {
            rootSkillsService.addRootRole(it.userId)
        }
    }

    def 'verify non-root users cannot remove root user privileges'() {
        when:
        nonRootSkillsService.removeRootRole(ultimateRoot)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'verify that inception project was created and assigned ot the root user'() {
        when:
        def inception = rootSkillsService.getProject("Inception")

        then:
        inception.projectId == 'Inception'
    }

    def 'verify that inception project is disassociated when user loses root privileges'() {
        setup:
        def originalRootUsers = rootSkillsService.getRootUsers()
        assert !originalRootUsers.find {it.userId == nonRootUserId}

        when:
        rootSkillsService.addRootRole(nonRootUserId)
        def inception = nonRootSkillsService.getProject("Inception")
        rootSkillsService.removeRootRole(nonRootUserId)
        def inception1 = nonRootSkillsService.getProject("Inception")

        then:
        SkillsClientException exception = thrown()
        inception
        !inception1
        exception.httpStatus == org.springframework.http.HttpStatus.FORBIDDEN
    }

    def 'insert new global setting'() {
        Map settingRequest = [
                settingGroup : "public_header",
                setting : "classification",
                value : "MY_CLASS"
        ]
        when :
        rootSkillsService.addOrUpdateGlobalSetting("classification", settingRequest)
        def settingsResult = rootSkillsService.getPublicSettings("public_header")

        then:
        settingsResult
        settingsResult.size() == 1
        settingsResult[0].setting == "classification"
        settingsResult[0].value == "MY_CLASS"
    }


    def 'update existing global setting'() {
        Map settingRequest = [
                settingGroup : "public_header",
                setting : "classification",
                value : "MY_CLASS1"
        ]
        when :
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        def settingsResult1 = rootSkillsService.getPublicSettings("public_header")

        settingRequest.value = "MY_CLASS2"
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        def settingsResult2 = rootSkillsService.getPublicSettings("public_header")

        then:
        settingsResult1
        settingsResult1.size() == 1
        settingsResult1[0].setting == "classification"
        settingsResult1[0].value == "MY_CLASS1"

        settingsResult2
        settingsResult2.size() == 1
        settingsResult2[0].setting == "classification"
        settingsResult2[0].value == "MY_CLASS2"
    }

    def 'get projects for root returns zero by default'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        rootSkillsService.createProject(proj)
        rootSkillsService.createProject(proj2)
        rootSkillsService.createProject(proj3)

        when:
        def projects = rootSkillsService.getProjects()

        then:
        !projects
    }

    def 'get projects for root returns only pinned projects'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        rootSkillsService.createProject(proj)
        rootSkillsService.createProject(proj2)
        rootSkillsService.createProject(proj3)
        rootSkillsService.pinProject(proj.projectId)

        when:
        def projects = rootSkillsService.getProjects()

        then:
        projects.size() == 1
        projects.find { it.projectId == proj.projectId }
    }

    def 'a non-root users existing projects are auto-pinned when granted root and unpinned when root is revoked'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        nonRootSkillsService.createProject(proj)
        nonRootSkillsService.createProject(proj2)
        nonRootSkillsService.createProject(proj3)

        def preRootProjects = nonRootSkillsService.getProjects()
        List<SettingsResult> pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(nonRootUserId, ProjAdminService.rootUserPinnedProjectGroup)
        List<String> preRootPinnedProjects = pinnedProjectSettings.collect { it.projectId }

        when:

        rootSkillsService.addRootRole(nonRootUserId)
        def postRootProjects = nonRootSkillsService.getProjects()
        pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(nonRootUserId, ProjAdminService.rootUserPinnedProjectGroup)
        List<String> postRootPinnedProjects = pinnedProjectSettings.collect { it.projectId }

        rootSkillsService.removeRootRole(nonRootUserId)
        def postRootRemovalProjects = nonRootSkillsService.getProjects()
        pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(nonRootUserId, ProjAdminService.rootUserPinnedProjectGroup)
        List<String> postRootRemovalPinnedProjects = pinnedProjectSettings.collect { it.projectId }

        then:
        preRootProjects.size() == 3
        preRootProjects.find { it.projectId == proj.projectId }
        preRootProjects.find { it.projectId == proj2.projectId }
        preRootProjects.find { it.projectId == proj3.projectId }
        !preRootPinnedProjects

        postRootProjects.size() == 3
        postRootProjects.find { it.projectId == proj.projectId }
        postRootProjects.find { it.projectId == proj2.projectId }
        postRootProjects.find { it.projectId == proj3.projectId }
        postRootPinnedProjects.size() == 3
        postRootPinnedProjects.find { proj.projectId }
        postRootPinnedProjects.find { proj2.projectId }
        postRootPinnedProjects.find { proj3.projectId }

        postRootRemovalProjects.size() == 3
        postRootRemovalProjects.find { it.projectId == proj.projectId }
        postRootRemovalProjects.find { it.projectId == proj2.projectId }
        postRootRemovalProjects.find { it.projectId == proj3.projectId }
        !postRootRemovalPinnedProjects
    }

    def "pinned projects are unique per root user"() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        def proj4 = SkillsFactory.createProject(4)
        nonRootSkillsService.createProject(proj)
        nonRootSkillsService.createProject(proj2)
        nonRootSkillsService.createProject(proj3)
        nonRootSkillsService.createProject(proj4)

        rootSkillsService.pinProject(proj.projectId)
        rootSkillsService.pinProject(proj2.projectId)

        when:
        def prePinProjects = secondRootSkillService.getProjects()
        secondRootSkillService.pinProject(proj3.projectId)
        secondRootSkillService.pinProject(proj4.projectId)
        def postPinProjects = secondRootSkillService.getProjects()
        def otherRootUserProjects = rootSkillsService.getProjects()

        then:
        !prePinProjects
        postPinProjects.size() == 2
        postPinProjects.find { it.projectId == proj3.projectId}
        postPinProjects.find { it.projectId == proj4.projectId}
        otherRootUserProjects.size() == 2
        otherRootUserProjects.find { it.projectId == proj.projectId }
        otherRootUserProjects.find { it.projectId == proj2.projectId }
    }

    def 'able to search all projects'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        rootSkillsService.createProject(proj)
        rootSkillsService.createProject(proj2)
        rootSkillsService.createProject(proj3)

        when:
        def projects = rootSkillsService.searchProjects("3")

        then:
        projects.size() == 1
        projects.find { it.projectId == proj3.projectId }
    }

    def 'only root users can search all projects'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        nonRootSkillsService.createProject(proj)
        nonRootSkillsService.createProject(proj2)
        nonRootSkillsService.createProject(proj3)

        when:
        nonRootSkillsService.searchProjects("search")

        then:
        thrown(Exception)
    }

    def 'get all projects'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        rootSkillsService.createProject(proj)
        rootSkillsService.createProject(proj2)
        rootSkillsService.createProject(proj3)

        when:
        def projects = rootSkillsService.getAllProjects()

        then:
        projects.size() == 4
        projects.collect { it.projectId }.sort() == ["Inception", proj.projectId, proj2.projectId, proj3.projectId ]
    }

    def 'only root users can get all projects'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        nonRootSkillsService.createProject(proj)
        nonRootSkillsService.createProject(proj2)
        nonRootSkillsService.createProject(proj3)

        when:
        nonRootSkillsService.getAllProjects()

        then:
        thrown(Exception)
    }


    def 'only root users can pin projects'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        nonRootSkillsService.createProject(proj)
        nonRootSkillsService.createProject(proj2)
        nonRootSkillsService.createProject(proj3)

        when:
        nonRootSkillsService.pinProject(proj.projectId)

        then:
        thrown(Exception)
    }

    def 'edit id of pinned project'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)
        rootSkillsService.createProject(proj)
        rootSkillsService.createProject(proj2)
        rootSkillsService.createProject(proj3)
        rootSkillsService.pinProject(proj.projectId)
        rootSkillsService.pinProject(proj2.projectId)


        def res = rootSkillsService.getProject(proj.projectId)
        def originalProjectId = res.projectId
        res.projectId = "ShinyNewProjectId"
        res.name = "NewNewNew"
        rootSkillsService.updateProject(res, originalProjectId)

        when:
        def projects = rootSkillsService.getProjects()

        then:
        projects.size() == 2
        projects.find { it.projectId == res.projectId }
        projects.find { it.projectId == proj2.projectId }
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'get users without role'() {
        expect:
//        this won't work until #145 is implemented and user clean up is added to the DefaultIntSpec
//        rootSkillsService.getUsersWithoutRole(role, usernameQuery).containsAll(result)


        rootSkillsService.getUsersWithoutRole(role, usernameQuery).size() >= expectedN
        where:
        role                    | usernameQuery | expectedN
        /*'ROLE_SUPER_DUPER_USER' | ''            | [[userId:'skills@skills.org', first:'Skills', last:'Test', nickname:'Skills Test', dn:''], [userId:'foo@bar.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]
        'ROLE_SUPER_DUPER_USER' | 'foo'         | [[userId:'foo@bar.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]
        'ROLE_SUPER_DUPER_USER' | 'bar'         | [[userId:'foo@bar.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]
        'ROLE_SUPER_DUPER_USER' | 'bar'         | [[userId:'foo@bar.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]
        'ROLE_SUPERVISOR'       | ''            | [[userId:'skills@skills.org', first:'Skills', last:'Test', nickname:'Skills Test', dn:''], [userId:'jh@dojo.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:''], [userId:'foo@bar.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]
        'ROLE_SUPERVISOR'       | 'dojo'        | [[userId:'jh@dojo.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]
        'ROLE_SUPERVISOR'       | 'foo'         | [[userId:'foo@bar.com', first:'Skills', last:'Test', nickname:'Skills Test', dn:'']]*/
        'ROLE_SUPER_DUPER_USER' | ''            | 2
        'ROLE_SUPER_DUPER_USER' | 'foo'         | 1
        'ROLE_SUPER_DUPER_USER' | 'bar'         | 1
        'ROLE_SUPER_DUPER_USER' | 'bar'         | 1
        'ROLE_SUPERVISOR'       | ''            | 2
        'ROLE_SUPERVISOR'       | 'foo'         | 1
    }

    def 'verify root user also gets supervisor role'() {
        when:
        def result = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        then:
        result
        result.find { it.userId == ultimateRoot }
    }

    def 'verify when adding root user that user also gets supervisor role'() {
        setup:
        def originalRootUsers = rootSkillsService.getRootUsers()
        assert !originalRootUsers.find {it.userId == nonRootUserId}

        def originalSupervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')
        assert !originalSupervisorUsers.find {it.userId == nonRootUserId}

        when:
        rootSkillsService.addRootRole(nonRootUserId)
        def rootUsers = rootSkillsService.getRootUsers()
        def supervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        then:
        rootUsers
        rootUsers.find { it.userId == nonRootUserId }
        supervisorUsers
        supervisorUsers.find { it.userId == nonRootUserId }
    }

    def 'verify when adding root user that already has supervisor role'() {
        setup:
        def originalRootUsers = rootSkillsService.getRootUsers()
        assert !originalRootUsers.find {it.userId == nonRootUserId}

        def originalSupervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')
        assert !originalSupervisorUsers.find {it.userId == nonRootUserId}

        rootSkillsService.grantSupervisorRole(nonRootUserId)
        originalSupervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')
        assert originalSupervisorUsers.find {it.userId == nonRootUserId}

        when:
        rootSkillsService.addRootRole(nonRootUserId)
        def rootUsers = rootSkillsService.getRootUsers()
        def supervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        then:
        rootUsers
        rootUsers.find { it.userId == nonRootUserId }
        supervisorUsers
        supervisorUsers.find { it.userId == nonRootUserId }
    }

    def 'verify root user loses supervisor role when root is removed'() {

        setup:
        def originalRootUsers = rootSkillsService.getRootUsers()
        assert !originalRootUsers.find {it.userId == nonRootUserId}

        def originalSupervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')
        assert !originalSupervisorUsers.find {it.userId == nonRootUserId}

        rootSkillsService.addRootRole(nonRootUserId)
        def rootUsers = rootSkillsService.getRootUsers()
        def supervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        assert rootUsers.find { it.userId == nonRootUserId }
        assert supervisorUsers.find { it.userId == nonRootUserId }

        when:
        rootSkillsService.removeRootRole(nonRootUserId)
        rootUsers = rootSkillsService.getRootUsers()
        supervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        then:
        rootUsers
        !rootUsers.find { it.userId == nonRootUserId }
        supervisorUsers
        !supervisorUsers.find { it.userId == nonRootUserId }
    }

    def 'verify root user loses supervisor role, and then can have root is removed'() {

        setup:
        def originalRootUsers = rootSkillsService.getRootUsers()
        assert !originalRootUsers.find {it.userId == nonRootUserId}

        def originalSupervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')
        assert !originalSupervisorUsers.find {it.userId == nonRootUserId}

        rootSkillsService.addRootRole(nonRootUserId)
        def rootUsers = rootSkillsService.getRootUsers()
        def supervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        assert rootUsers.find { it.userId == nonRootUserId }
        assert supervisorUsers.find { it.userId == nonRootUserId }

        when:
        rootSkillsService.removeSupervisorRole(nonRootUserId)

        // this was causing an assertion error since it also tries to remove supervisor role,
        // but it had already been removed previously
        rootSkillsService.removeRootRole(nonRootUserId)

        rootUsers = rootSkillsService.getRootUsers()
        supervisorUsers = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        then:
        rootUsers
        !rootUsers.find { it.userId == nonRootUserId }
        supervisorUsers
        !supervisorUsers.find { it.userId == nonRootUserId }
    }

    def 'root user can manually report skill for a project they are not an admin of' () {
        // need to call DefaultIntSpec.getRandomUsers so that tests will work in ssl mode
        String userId = getRandomUsers(1)[0]

        //we need a different userId from the default root user for this test
        while (userId.contains("jh@dojo")) {
            userId = getRandomUsers(1)[0]
        }

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = rootSkillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills[0].skillId], userId)

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"

        res.body.completed.size() == 3
        res.body.completed.find({ it.type == "Skill" }).id == skills[0].skillId
        res.body.completed.find({ it.type == "Skill" }).name == skills[0].name

        res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
        res.body.completed.find({ it.type == "Overall" }).level == 1

        res.body.completed.find({ it.type == "Subject" }).id == subj.subjectId
        res.body.completed.find({ it.type == "Subject" }).name == subj.name
        res.body.completed.find({ it.type == "Subject" }).level == 1
    }

    def 'root user can save user tags' () {
        // need to call DefaultIntSpec.getRandomUsers so that tests will work in ssl mode
        String userId = getRandomUsers(1)[0]

        //we need a different userId from the default root user for this test
        while (userId.contains("jh@dojo")) {
            userId = getRandomUsers(1)[0]
        }

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10,)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        rootSkillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills[0].skillId], userId)
        def res = rootSkillsService.saveUserTag(userId, "myKey", ["coolTag"]);

        String metricsId = "numUsersPerTagBuilder"
        def tags1 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: "myKey", currentPage: 1, pageSize: 5, sortDesc: true])
        println JsonOutput.toJson(tags1)
        then:
        res.success
        tags1.items[0].value == "coolTag"
    }

    def 'non-root user can NOT save user tags' () {
        // need to call DefaultIntSpec.getRandomUsers so that tests will work in ssl mode
        String userId = getRandomUsers(1)[0]

        //we need a different userId from the default root user for this test
        while (userId.contains("jh@dojo")) {
            userId = getRandomUsers(1)[0]
        }

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10,)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        rootSkillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills[0].skillId], userId)
        nonRootSkillsService.saveUserTag(userId, "myKey", ["coolTag"]);
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'rebuild a projects users user_points, subject and project definition total_points' () {
        // need to call DefaultIntSpec.getRandomUsers so that tests will work in ssl mode
        List<String> users = []
        users.addAll(getRandomUsers(4))
        users.removeAll { it.contains("jh@dojo")}
        assert users.size() >= 3
        users = users.take(3)
        String user1 = users[0]
        String user2 = users[1]
        String user3 = users[2]


        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(4)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def groupChildren = allSkills[1..2]
        groupChildren.each { skill ->
            skill.pointIncrement = 100
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.numSkillsRequired = 1
        skillsService.updateSkill(skillsGroup, null)

        // regular skill
        allSkills[3].numPerformToCompletion = 2
        skillsService.createSkill(allSkills[3])

        String projectId = proj.projectId
        String subjectId = subj.subjectId
        String childSkillId1 = groupChildren.first().skillId
        String childSkillId2 = groupChildren.last().skillId
        String regSkillId = allSkills[3].skillId

        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1subj4skillsGroup = SkillsFactory.createSkillsGroup(1, 2, 10)
        skillsService.createSubjectAndSkills(p1subj2, [p1subj4skillsGroup].flatten())
        skillsService.createSubject(p1subj3)
        def p3s2skillsGroup = SkillsFactory.createSkillsGroup(1, 3, 20)
        skillsService.createSkill(p3s2skillsGroup)
        skillsService.createSubject(p1subj4)
        def p3_subj3_skills = createSkills(3, 1, 4, 100)
        skillsService.createSkills(p3_subj3_skills)

        users.each { user ->
            def res = skillsService.addSkill([projectId: projectId, skillId: childSkillId1], user, new Date())
            assert res.body.skillApplied
            assert res.body.completed.find { it.id == childSkillId1 }

            res = skillsService.addSkill([projectId: projectId, skillId: childSkillId2], user, new Date())
            assert res.body.skillApplied
            assert res.body.completed.find { it.id == childSkillId2 }

            res = skillsService.addSkill([projectId: projectId, skillId: regSkillId], user, new Date())
            assert res.body.skillApplied

            def subjectSummary = skillsService.getSkillSummary(user, projectId, subjectId)
            List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, projectId, skillsGroupId)

            groupAchievements
            groupAchievements.size() == 1
            groupAchievements[0].userId == user1
            groupAchievements[0].projectId == projectId
            groupAchievements[0].skillId == skillsGroupId
            groupAchievements[0].pointsWhenAchieved == groupChildren.first().pointIncrement * groupChildren.first().numPerformToCompletion

            subjectSummary
            subjectSummary.skills
            subjectSummary.skills.size() == 2
            subjectSummary.skills[0].skillId == skillsGroupId
            subjectSummary.skills[0].points == 100
            subjectSummary.skills[0].totalPoints == 100 * groupChildren.size()
            subjectSummary.skills[0].children
            subjectSummary.skills[0].children.size() == groupChildren.size()
            subjectSummary.skills[0].children.find { it.skillId = childSkillId1 }
            subjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.points == 100
            subjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.totalPoints == 100
            subjectSummary.skills[0].children.find { it.skillId = childSkillId2 }
            subjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.points == 100
            subjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.totalPoints == 100

            List<UserPoints> userPoints = userPointsRepo.findByProjectIdAndUserId(projectId, user)
            assert !userPoints.find { it.skillId == skillsGroupId }
            assert userPoints.find { it.skillId == childSkillId1 }.points == 100
            assert userPoints.find { it.skillId == childSkillId2 }.points == 100
            assert userPoints.find { it.skillId == regSkillId }.points == 10
            assert userPoints.find { it.skillId == subjectId }.points == 210
            assert userPoints.find { it.skillId == null }.points == 210

            UserPoints subjectUserPoints = userPoints.find { it.skillId == subjectId }
            subjectUserPoints.points = 100
            userPointsRepo.save(subjectUserPoints)

            List<UserAchievement> userAchievements = userAchievedRepo.findAllByUserAndProjectIds(user, [projectId])
            assert userAchievements.size() == 10
            assert userAchievements.find { it.level  == 1 && it.skillId == subjectId }
            assert userAchievements.find { it.level  == 2 && it.skillId == subjectId }
            assert userAchievements.find { it.level  == 3 && it.skillId == subjectId }
            assert userAchievements.find { it.level  == 4 && it.skillId == subjectId }
            assert userAchievements.find { it.level  == 5 && it.skillId == subjectId }
            assert userAchievements.find { it.level  == 1 && it.skillId == null }
            assert userAchievements.find { it.level  == 2 && it.skillId == null }
            assert userAchievements.find { it.level  == null && it.skillId == skillsGroupId }
            assert userAchievements.find { it.level  == null && it.skillId == childSkillId1 }
            assert userAchievements.find { it.level  == null && it.skillId == childSkillId2 }

            // delete level 2 subject and project achievements as well as the skills group
            userAchievedRepo.delete(userAchievements.find { it.level  == 2 && it.skillId == subjectId })
            userAchievedRepo.delete(userAchievements.find { it.level  == 2 && it.skillId == null })
            userAchievedRepo.delete(userAchievements.find { it.level  == null && it.skillId == skillsGroupId })

            List<UserAchievement> userAchievements2 = userAchievedRepo.findAllByUserAndProjectIds(user, [projectId])
            assert userAchievements2.size() == 7
            assert userAchievements2.find { it.level  == 1 && it.skillId == subjectId }
            assert !userAchievements2.find { it.level  == 2 && it.skillId == subjectId }
            assert userAchievements2.find { it.level  == 3 && it.skillId == subjectId }
            assert userAchievements2.find { it.level  == 4 && it.skillId == subjectId }
            assert userAchievements2.find { it.level  == 5 && it.skillId == subjectId }
            assert userAchievements2.find { it.level  == 1 && it.skillId == null }
            assert !userAchievements2.find { it.level  == null && it.skillId == skillsGroupId }
            assert userAchievements2.find { it.level  == null && it.skillId == childSkillId1 }
            assert userAchievements2.find { it.level  == null && it.skillId == childSkillId2 }
        }

        // mess up points and achievements
        SkillDef groupSkillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillsGroupId)
        groupSkillDef.totalPoints = 123
        skillDefRepo.save(groupSkillDef)
        SkillDef groupSkillDef2 = skillDefRepo.findByProjectIdAndSkillId(projectId, skillsGroupId)
        assert groupSkillDef2.totalPoints == 123

        SkillDef subjectSkillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
        subjectSkillDef.totalPoints = 456
        skillDefRepo.save(subjectSkillDef)
        SkillDef subjectSkillDef2 = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
        assert subjectSkillDef2.totalPoints == 456

        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        projDef.totalPoints = 789
        projDefRepo.save(projDef)
        ProjDef projDef2 = projDefRepo.findByProjectId(projectId)
        assert projDef2.totalPoints == 789

        when:
        rootSkillsService.rebuildUserAndProjectPoints(projectId)

        List<UserAchievement> user1Achievements = userAchievedRepo.findAllByUserAndProjectIds(user1, [projectId])
        List<UserPoints> user1UserPoints = userPointsRepo.findByProjectIdAndUserId(projectId, user1)
        def user1SubjectSummary = skillsService.getSkillSummary(user1, projectId, subjectId)

        List<UserAchievement> user2Achievements = userAchievedRepo.findAllByUserAndProjectIds(user2, [projectId])
        List<UserPoints> user2UserPoints = userPointsRepo.findByProjectIdAndUserId(projectId, user2)
        def user2SubjectSummary = skillsService.getSkillSummary(user2, projectId, subjectId)

        List<UserAchievement> user3Achievements = userAchievedRepo.findAllByUserAndProjectIds(user3, [projectId])
        List<UserPoints> user3UserPoints = userPointsRepo.findByProjectIdAndUserId(projectId, user3)
        def user3SubjectSummary = skillsService.getSkillSummary(user3, projectId, subjectId)

        then:
        user1Achievements.size() == 10
        user1Achievements.find { it.level  == 1 && it.skillId == subjectId }
        user1Achievements.find { it.level == 2 && it.skillId == subjectId }
        user1Achievements.find { it.level == 3 && it.skillId == subjectId }
        user1Achievements.find { it.level == 4 && it.skillId == subjectId }
        user1Achievements.find { it.level == 5 && it.skillId == subjectId }
        user1Achievements.find { it.level == 1 && it.skillId == null }
        user1Achievements.find { it.level == 2 && it.skillId == null }
        user1Achievements.find { it.level == null && it.skillId == skillsGroupId }
        user1Achievements.find { it.level  == null && it.skillId == childSkillId1 }
        user1Achievements.find { it.level  == null && it.skillId == childSkillId2 }

        user1UserPoints.find { it.skillId == childSkillId1 }.points == 100
        user1UserPoints.find { it.skillId == childSkillId2 }.points == 100
        user1UserPoints.find { it.skillId == regSkillId }.points == 10
        user1UserPoints.find { it.skillId == subjectId }.points == 210
        user1UserPoints.find { it.skillId == null }.points == 210

        user1SubjectSummary
        user1SubjectSummary.skills
        user1SubjectSummary.skills.size() == 2
        user1SubjectSummary.skills[0].skillId == skillsGroupId
        user1SubjectSummary.skills[0].points == 100
        user1SubjectSummary.skills[0].totalPoints == 100 * groupChildren.size()
        user1SubjectSummary.skills[0].children
        user1SubjectSummary.skills[0].children.size() == groupChildren.size()
        user1SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }
        user1SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.points == 100
        user1SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.totalPoints == 100
        user1SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }
        user1SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.points == 100
        user1SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.totalPoints == 100

        user2Achievements.size() == 10
        user2Achievements.find { it.level  == 1 && it.skillId == subjectId }
        user2Achievements.find { it.level == 2 && it.skillId == subjectId }
        user2Achievements.find { it.level == 3 && it.skillId == subjectId }
        user2Achievements.find { it.level == 4 && it.skillId == subjectId }
        user2Achievements.find { it.level == 5 && it.skillId == subjectId }
        user2Achievements.find { it.level == 1 && it.skillId == null }
        user2Achievements.find { it.level == 2 && it.skillId == null }
        user2Achievements.find { it.level == null && it.skillId == skillsGroupId }
        user2Achievements.find { it.level  == null && it.skillId == childSkillId1 }
        user2Achievements.find { it.level  == null && it.skillId == childSkillId2 }

        user2UserPoints.find { it.skillId == childSkillId1 }.points == 100
        user2UserPoints.find { it.skillId == childSkillId2 }.points == 100
        user2UserPoints.find { it.skillId == regSkillId }.points == 10
        user2UserPoints.find { it.skillId == subjectId }.points == 210
        user2UserPoints.find { it.skillId == null }.points == 210

        user2SubjectSummary
        user2SubjectSummary.skills
        user2SubjectSummary.skills.size() == 2
        user2SubjectSummary.skills[0].skillId == skillsGroupId
        user2SubjectSummary.skills[0].points == 100
        user2SubjectSummary.skills[0].totalPoints == 100 * groupChildren.size()
        user2SubjectSummary.skills[0].children
        user2SubjectSummary.skills[0].children.size() == groupChildren.size()
        user2SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }
        user2SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.points == 100
        user2SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.totalPoints == 100
        user2SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }
        user2SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.points == 100
        user2SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.totalPoints == 100

        user3Achievements.size() == 10
        user3Achievements.find { it.level  == 1 && it.skillId == subjectId }
        user3Achievements.find { it.level == 2 && it.skillId == subjectId }
        user3Achievements.find { it.level == 3 && it.skillId == subjectId }
        user3Achievements.find { it.level == 4 && it.skillId == subjectId }
        user3Achievements.find { it.level == 5 && it.skillId == subjectId }
        user3Achievements.find { it.level == 1 && it.skillId == null }
        user3Achievements.find { it.level == 2 && it.skillId == null }
        user3Achievements.find { it.level == null && it.skillId == skillsGroupId }
        user3Achievements.find { it.level  == null && it.skillId == childSkillId1 }
        user3Achievements.find { it.level  == null && it.skillId == childSkillId2 }

        user3UserPoints.find { it.skillId == childSkillId1 }.points == 100
        user3UserPoints.find { it.skillId == childSkillId2 }.points == 100
        user3UserPoints.find { it.skillId == regSkillId }.points == 10
        user3UserPoints.find { it.skillId == subjectId }.points == 210
        user3UserPoints.find { it.skillId == null }.points == 210

        user3SubjectSummary
        user3SubjectSummary.skills
        user3SubjectSummary.skills.size() == 2
        user3SubjectSummary.skills[0].skillId == skillsGroupId
        user3SubjectSummary.skills[0].points == 100
        user3SubjectSummary.skills[0].totalPoints == 100 * groupChildren.size()
        user3SubjectSummary.skills[0].children
        user3SubjectSummary.skills[0].children.size() == groupChildren.size()
        user3SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }
        user3SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.points == 100
        user3SubjectSummary.skills[0].children.find { it.skillId = childSkillId1 }.totalPoints == 100
        user3SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }
        user3SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.points == 100
        user3SubjectSummary.skills[0].children.find { it.skillId = childSkillId2 }.totalPoints == 100
    }

    def 'get projects for pinned projects as root returns project protection'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        rootSkillsService.createProject(proj)
        rootSkillsService.createProject(proj2)
        rootSkillsService.pinProject(proj.projectId)
        rootSkillsService.pinProject(proj2.projectId)

        rootSkillsService.changeSetting(proj2.projectId, "project-protection", [projectId: proj2.projectId, setting: "project-protection", value: "true"])
        when:
        def projects = rootSkillsService.getProjects()

        then:
        projects.size() == 2
        !projects[0].isDeleteProtected
        projects[1].isDeleteProtected
    }

    def 'get projects for pinned projects as non-root returns project protection'() {
        def proj = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        nonRootSkillsService.createProject(proj)
        nonRootSkillsService.createProject(proj2)

        nonRootSkillsService.changeSetting(proj2.projectId, "project-protection", [projectId: proj2.projectId, setting: "project-protection", value: "true"])
        when:
        def projects = nonRootSkillsService.getProjects()

        then:
        projects.size() == 2
        !projects[0].isDeleteProtected
        projects[1].isDeleteProtected
    }
}

