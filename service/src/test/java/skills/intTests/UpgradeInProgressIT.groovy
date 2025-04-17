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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import skills.SpringBootApp
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.services.admin.BadgeAdminService
import skills.services.admin.ProjAdminService
import skills.services.admin.SkillsAdminService
import skills.services.admin.SubjAdminService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.User
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRepo

@Slf4j
@SpringBootTest(properties = ['skills.h2.port=9097',
        'skills.config.ui.rankingAndProgressViewsEnabled=false',
        'skills.config.ui.defaultLandingPage=progress',
        'skills.config.db-upgrade-in-progress=true',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8189/status',
        'skills.authorization.userInfoUri=https://localhost:8189/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8189/userQuery?query={query}',
        ], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UpgradeInProgressIT extends DefaultIntSpec {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserRepo userRepository

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    SubjAdminService subjAdminService

    @Autowired
    BadgeAdminService badgeAdminService

    @Autowired
    SkillsAdminService skillsAdminService

    def setup() {
        if (!userRepository.findByUserId(skillsService.userName.toLowerCase())) {
            UserAttrs userAttrs = new UserAttrs(userId: skillsService.userName.toLowerCase(),
                    userIdForDisplay: skillsService.userName.toLowerCase(),
                    userTagsLastUpdated: new Date())
            userAttrsRepo.save(userAttrs)
            User user = new User(userId: skillsService.userName.toLowerCase())
            userRepository.save(user)
        }
        userAuthService.grantRoot(skillsService.userName)
        SkillsFactory.createProject(8)
        SkillsFactory.createSubject(8, 8)
        SkillsFactory.createSkill(8, 8, 8)
        SkillsFactory.createBadge(8, 8)

        SecurityContext securityContext = Mock()
        Authentication authentication = Mock()
        UserInfo userInfo = Mock()
        userInfo.getUsername() >> skillsService.userName.toLowerCase()
        authentication.getPrincipal() >> userInfo
        securityContext.getAuthentication() >> authentication
        SecurityContextHolder.setContext(securityContext)
        projAdminService.saveProject('SampleProject', new ProjectRequest('SampleProject', 'Sample Project'), skillsService.userName)
        subjAdminService.saveSubject('SampleProject', 'Subject1Subject', new SubjectRequest('Subject1Subject', 'Subject 1 Subject', 'blah', 'icon', 'helpUrl'))
        skillsAdminService.saveSkill('Skill1Skill', new SkillRequest('Skill1Skill', 'Subject1Subject', 'SampleProject', 'Skill1 Skill', 100, 60*8, 1, 1))
        badgeAdminService.saveBadge('SampleProject', 'SampleBadgeBadge', new BadgeRequest('SampleBadgeBadge', 'SampleBadge Badge', 'blah', 'icon'))
    }

    def "cannot create new project when upgrade mode is enabled"() {
        when:

        def project = SkillsFactory.createProject(11)
        skillsService.createProject(project)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot add a subject to an existing project when upgrade mode is enabled"() {
        when:
        def subject = SkillsFactory.createSubject(11, 1)
        subject.projectId = "SampleProject"
        skillsService.createSubject(subject)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot add skills to existing subject when upgrade mode is enabled"() {
        when:
        def skill = SkillsFactory.createSkill(1, 1, 99)
        skill.projectId = "SampleProject"
        skill.subjectId = "Subject1Subject"
        skillsService.createSkill(skill)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot edit existing subject when upgrade mode is enabled"() {
        def subj = skillsService.getSubject([projectId: "SampleProject", subjectId: "Subject1Subject"])

        when:
        subj.name = "new name"
        skillsService.createSubject(subj)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot edit skill when upgrade mode is enabled"() {
        def skill = skillsService.getSkill([projectId: "SampleProject", subjectId: "Subject1Subject", skillId: "Skill1Skill"])

        when:
        skill.name = "new name"
        skillsService.createSkill(skill)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot remove levels from an existing project when upgrade mode is enabled"() {
        when:
        skillsService.deleteLevel("SampleProject")
        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot create new badge for project when upgrade mode is enabled"() {
        when:
        def badge = SkillsFactory.createBadge(1, 11)
        badge.projectId = "SampleProject"
        skillsService.createBadge(badge)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot edit existing badge when upgrade mode is enabled"() {
        def badge = skillsService.getBadge([projectId: "SampleProject", badgeId: "SampleBadgeBadge"])

        when:
        badge.name = "new name"
        skillsService.createBadge(badge)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "cannot add skills to existing badge when upgrade mode is enabled"() {

        when:
        skillsService.assignSkillToBadge(projectId: "SampleProject", badgeId: "SampleBadgeBadge", skillId: "Skill1Skill")

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("A database upgrade is currently in progress, no training profile modifications are allowed at this time")
    }

    def "can report skill events when upgrade mode is enabled"() {
        def user = getRandomUsers(1)[0]

        when:
        def result = skillsService.addSkill([projectId: "SampleProject", skillId: "Skill1Skill"], user)

        then:
        result.body.success
        !result.body.skillApplied
        result.body.explanation == "A database upgrade is currently in progress. This Skill Event Request has been queued for future application."
    }

}
