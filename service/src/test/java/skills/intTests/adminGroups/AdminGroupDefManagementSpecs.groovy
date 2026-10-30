/**
 * Copyright 2024 SkillTree
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
package skills.intTests.adminGroups

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRoleRepo
import spock.lang.Unroll

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup
import static skills.intTests.utils.SkillsFactory.*

class AdminGroupDefManagementSpecs extends DefaultIntSpec {

    @Autowired
    UserRoleRepo userRoleRepository

    def "no admin group definitions"() {
        when:
        def adminGroupDefs = skillsService.getAdminGroupDefs()

        then:
        !adminGroupDefs
    }

    def "create admin group"() {
        def adminGroup = createAdminGroup(1)
        when:

        skillsService.createAdminGroupDef(adminGroup)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)

        then:

        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.name == adminGroup.name
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfMembers == 0
        adminGroupRes.numberOfProjects == 0
        adminGroupRes.numberOfQuizzesAndSurveys == 0
        adminGroupRes.userCommunity == 'All Dragons'
    }

    def "group id must not be null string"() {
        def adminGroup = createAdminGroup(1)
        when:
        adminGroup.adminGroupId = "null"
        skillsService.createAdminGroupDef(adminGroup)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("adminGroupId was not provided")
    }

    def "remove admin group definition"() {
        def adminGroup1 = createAdminGroup(1)
        def adminGroup2 = createAdminGroup(2)
        skillsService.createAdminGroupDef(adminGroup1)
        skillsService.createAdminGroupDef(adminGroup2)

        when:
        def adminGroups = skillsService.getAdminGroupDefs()
        skillsService.removeAdminGroupDef(adminGroup1.adminGroupId)
        def adminGroupDefsAfter = skillsService.getAdminGroupDefs()
        then:
        adminGroups.adminGroupId == [adminGroup2.adminGroupId, adminGroup1.adminGroupId]
        adminGroupDefsAfter.adminGroupId == [adminGroup2.adminGroupId]
    }

    def "owner can add owner to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        when:

        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupOwner(adminGroup.adminGroupId, otherUserId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembers = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        then:

        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 2
        adminGroupMembers.size() == 2
        adminGroupMembers.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembers.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
    }

    def "owner can remove owner to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupOwner(adminGroup.adminGroupId, otherUserId)

        when:
        def adminGroupResBefore = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembersBefore = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)
        skillsService.deleteAdminGroupOwner(adminGroup.adminGroupId, otherUserId)
        def adminGroupResAfter = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembersAfter = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        then:

        adminGroupResBefore.adminGroupId == adminGroup.adminGroupId
        adminGroupResBefore.numberOfOwners == 2
        adminGroupMembersBefore.size() == 2
        adminGroupMembersBefore.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembersBefore.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}

        adminGroupResAfter.adminGroupId == adminGroup.adminGroupId
        adminGroupResAfter.numberOfOwners == 1
        adminGroupMembersAfter.size() == 1
        !adminGroupMembersAfter.find { it.userId == otherUserId}
    }

    def "owner can add member to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        // add project to admin group
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        // add badge to admin group
        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        // add quiz to admin group
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        when:

        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembers = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        then:

        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfMembers == 1
        adminGroupMembers.size() == 2
        adminGroupMembers.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembers.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER.toString()}
    }

    def "owner can remove member to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)

        when:
        def adminGroupResBefore = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembersBefore = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)
        skillsService.deleteAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        def adminGroupResAfter = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembersAfter = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        then:

        adminGroupResBefore.adminGroupId == adminGroup.adminGroupId
        adminGroupResBefore.numberOfOwners == 1
        adminGroupResBefore.numberOfMembers == 1
        adminGroupMembersBefore.size() == 2
        adminGroupMembersBefore.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembersBefore.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER.toString()}

        adminGroupResAfter.adminGroupId == adminGroup.adminGroupId
        adminGroupResAfter.numberOfOwners == 1
        adminGroupResAfter.numberOfMembers == 0
        adminGroupMembersAfter.size() == 1
        !adminGroupMembersAfter.find { it.userId == otherUserId}
    }

    def "member cannot add member to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        when:

        memberSkillsService.addAdminGroupMember(adminGroup.adminGroupId, "someOtherUserId")

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "member cannot remove member from admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        when:

        memberSkillsService.deleteAdminGroupMember(adminGroup.adminGroupId, "someOtherUserId")

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "member cannot add owner to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)

        when:
        memberSkillsService.addAdminGroupOwner(adminGroup.adminGroupId, "someOtherUserId")

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "member cannot remove owner from admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)

        when:
        memberSkillsService.deleteAdminGroupOwner(adminGroup.adminGroupId, "someOtherUserId")

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "owner can add project to admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [skill2])

        when:
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupProjects = skillsService.getAdminGroupProjects(adminGroup.adminGroupId)

        then:
        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfProjects == 1
        adminGroupRes.numberOfQuizzesAndSurveys == 0
        adminGroupRes.numberOfGlobalBadges == 0

        adminGroupProjects.adminGroupId == adminGroup.adminGroupId
        adminGroupProjects.assignedProjects.size() == 1 && adminGroupProjects.assignedProjects.find { it.projectId == proj.projectId }
        adminGroupProjects.availableProjects.size() == 1 && adminGroupProjects.availableProjects.find { it.projectId == proj2.projectId }
    }

    def "member cannot add project to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [skill2])

        when:
        memberSkillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "cannot add same project to same admin group more than once"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [skill2])

        when:
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        then:

        SkillsClientException e = thrown(SkillsClientException)
    }

    def "owner can add quiz to admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)

        when:
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupQuizzesAndSurveys = skillsService.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId)

        then:
        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfProjects == 0
        adminGroupRes.numberOfQuizzesAndSurveys == 1
        adminGroupRes.numberOfGlobalBadges == 0

        adminGroupQuizzesAndSurveys.adminGroupId == adminGroup.adminGroupId
        adminGroupQuizzesAndSurveys.assignedQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.assignedQuizzes.find { it.quizId == quiz.quizId }
        adminGroupQuizzesAndSurveys.availableQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.availableQuizzes.find { it.quizId == quiz2.quizId }
    }

    def "member cannot add quiz to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)

        when:
        memberSkillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "cannot add same quiz to same admin group more than once"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)

        when:
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
    }

    def "owner can add badge to admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def badge2 = createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)

        when:
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupGlobalBadges = skillsService.getAdminGroupGlobalBadges(adminGroup.adminGroupId)

        then:
        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfProjects == 0
        adminGroupRes.numberOfQuizzesAndSurveys == 0
        adminGroupRes.numberOfGlobalBadges == 1

        adminGroupGlobalBadges.adminGroupId == adminGroup.adminGroupId
        adminGroupGlobalBadges.assignedGlobalBadges.size() == 1 && adminGroupGlobalBadges.assignedGlobalBadges.find { it.badgeId == badge.badgeId }
        adminGroupGlobalBadges.availableGlobalBadges.size() == 1 && adminGroupGlobalBadges.availableGlobalBadges.find { it.badgeId == badge2.badgeId }
    }

    def "member cannot add badge to admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def badge2 = createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)

        when:
        memberSkillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "cannot add same badge to same admin group more than once"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def badge2 = createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)

        when:
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
    }

    def "owner can remove project from admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [skill2])

        when:
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupProjectsBeforeRemove = skillsService.getAdminGroupProjects(adminGroup.adminGroupId)

        skillsService.deleteProjectFromAdminGroup(adminGroup.adminGroupId, proj.projectId)
        def adminGroupProjectsAfterRemove = skillsService.getAdminGroupProjects(adminGroup.adminGroupId)

        then:
        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfProjects == 1
        adminGroupRes.numberOfQuizzesAndSurveys == 0
        adminGroupRes.numberOfGlobalBadges == 0

        adminGroupProjectsBeforeRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupProjectsBeforeRemove.assignedProjects.size() == 1 && adminGroupProjectsBeforeRemove.assignedProjects.find { it.projectId == proj.projectId }
        adminGroupProjectsBeforeRemove.availableProjects.size() == 1 && adminGroupProjectsBeforeRemove.availableProjects.find { it.projectId == proj2.projectId }

        adminGroupProjectsAfterRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupProjectsAfterRemove.assignedProjects.size() == 0
        adminGroupProjectsAfterRemove.availableProjects.size() == 2
                && adminGroupProjectsAfterRemove.availableProjects.find { it.projectId == proj.projectId }
                && adminGroupProjectsAfterRemove.availableProjects.find { it.projectId == proj2.projectId }
    }

    def "member cannot remove project from admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [skill2])
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        when:
        memberSkillsService.deleteProjectFromAdminGroup(adminGroup.adminGroupId, proj.projectId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "owner can remove quiz from admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)

        when:
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupQuizzesAndSurveys = skillsService.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId)

        skillsService.deleteQuizFromAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        def adminGroupQuizzesAndSurveysAfterRemove = skillsService.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId)

        then:
        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfProjects == 0
        adminGroupRes.numberOfQuizzesAndSurveys == 1
        adminGroupRes.numberOfGlobalBadges == 0

        adminGroupQuizzesAndSurveys.adminGroupId == adminGroup.adminGroupId
        adminGroupQuizzesAndSurveys.assignedQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.assignedQuizzes.find { it.quizId == quiz.quizId }
        adminGroupQuizzesAndSurveys.availableQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.availableQuizzes.find { it.quizId == quiz2.quizId }

        adminGroupQuizzesAndSurveysAfterRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupQuizzesAndSurveysAfterRemove.assignedQuizzes.size() == 0
        adminGroupQuizzesAndSurveysAfterRemove.availableQuizzes.size() == 2
                && adminGroupQuizzesAndSurveysAfterRemove.availableQuizzes.find { it.quizId == quiz.quizId }
                && adminGroupQuizzesAndSurveysAfterRemove.availableQuizzes.find { it.quizId == quiz2.quizId }
    }

    def "member cannot remove quiz from admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        when:
        memberSkillsService.deleteQuizFromAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "owner can remove badge from admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def badge2 = createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)

        when:
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)
        def adminGroupRes = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupGlobalBadges = skillsService.getAdminGroupGlobalBadges(adminGroup.adminGroupId)

        skillsService.deleteGlobalBadgeFromAdminGroup(adminGroup.adminGroupId, badge.badgeId)
        def adminGroupGlobalBadgesAfterRemove = skillsService.getAdminGroupGlobalBadges(adminGroup.adminGroupId)

        then:
        adminGroupRes.adminGroupId == adminGroup.adminGroupId
        adminGroupRes.numberOfOwners == 1
        adminGroupRes.numberOfProjects == 0
        adminGroupRes.numberOfQuizzesAndSurveys == 0
        adminGroupRes.numberOfGlobalBadges == 1

        adminGroupGlobalBadges.adminGroupId == adminGroup.adminGroupId
        adminGroupGlobalBadges.assignedGlobalBadges.size() == 1 && adminGroupGlobalBadges.assignedGlobalBadges.find { it.badgeId == badge.badgeId }
        adminGroupGlobalBadges.availableGlobalBadges.size() == 1 && adminGroupGlobalBadges.availableGlobalBadges.find { it.badgeId == badge2.badgeId }

        adminGroupGlobalBadgesAfterRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupGlobalBadgesAfterRemove.assignedGlobalBadges.size() == 0
        adminGroupGlobalBadgesAfterRemove.availableGlobalBadges.size() == 2
                && adminGroupGlobalBadgesAfterRemove.availableGlobalBadges.find { it.badgeId == badge.badgeId }
                && adminGroupGlobalBadgesAfterRemove.availableGlobalBadges.find { it.badgeId == badge2.badgeId }
    }

    def "member cannot remove badge from admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        def badge2 = createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        when:
        memberSkillsService.deleteGlobalBadgeFromAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "removing an admin group removes all other members/owners from quiz/project/badge admin roles except current user, who remains as a 'local' admin"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)
        when:

        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        def projectAdminsBeforeRemove = userRoleRepository.findAllByProjectIdIgnoreCase(proj.projectId)
        def quizAdminsBeforeRemove = userRoleRepository.findAllByQuizIdIgnoreCase(quiz.quizId)
        def badgeAdminsBeforeRemove = userRoleRepository.findAllByGlobalBadgeIdIgnoreCase(badge.badgeId)
        def adminGroupRolesBeforeRemove = userRoleRepository.findAllByAdminGroupIdIgnoreCase(adminGroup.adminGroupId)
        def adminGroupDefsBeforeRemove = skillsService.getAdminGroupDefs()

        skillsService.removeAdminGroupDef(adminGroup.adminGroupId)

        def projectAdminsAfterRemove = userRoleRepository.findAllByProjectIdIgnoreCase(proj.projectId)
        def quizAdminsAfterRemove = userRoleRepository.findAllByQuizIdIgnoreCase(quiz.quizId)
        def badgeAdminsAfterRemove = userRoleRepository.findAllByGlobalBadgeIdIgnoreCase(badge.badgeId)
        def adminGroupRolesAfterRemove = userRoleRepository.findAllByAdminGroupIdIgnoreCase(adminGroup.adminGroupId)
        def adminGroupDefsAfterRemove = skillsService.getAdminGroupDefs()

        then:
        adminGroupDefsBeforeRemove && adminGroupDefsBeforeRemove.size() == 1 && adminGroupDefsBeforeRemove[0].adminGroupId == adminGroup.adminGroupId
        projectAdminsBeforeRemove.size() == 2
        projectAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup.adminGroupId }
        projectAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup.adminGroupId }
        quizAdminsBeforeRemove.size() == 2
        quizAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup.adminGroupId }
        quizAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup.adminGroupId }
        badgeAdminsBeforeRemove.size() == 2
        badgeAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup.adminGroupId }
        badgeAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup.adminGroupId }
        adminGroupRolesBeforeRemove.size() == 8
        adminGroupRolesBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER && it.adminGroupId == adminGroup.adminGroupId }
        adminGroupRolesBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER && it.adminGroupId == adminGroup.adminGroupId }
        projectAdminsAfterRemove.size() == 1
        projectAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == null }
        quizAdminsAfterRemove.size() == 1
        quizAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == null }
        badgeAdminsAfterRemove.size() == 1
        badgeAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == null }
        adminGroupRolesAfterRemove.size() == 0

        !adminGroupDefsAfterRemove
    }

    def "change role between owner and member"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [skill2])
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        when:
        skillsService.addAdminGroupOwner(adminGroup.adminGroupId, otherUserId)
        def adminGroupRes1 = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembers1 = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        def adminGroupRes2 = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembers2 = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        then:
        adminGroupRes1.adminGroupId == adminGroup.adminGroupId
        adminGroupRes1.numberOfOwners == 2
        adminGroupRes1.numberOfMembers == 0
        adminGroupMembers1.size() == 2
        adminGroupMembers1.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembers1.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        !adminGroupMembers1.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER.toString()}

        adminGroupRes2.adminGroupId == adminGroup.adminGroupId
        adminGroupRes2.numberOfOwners == 1
        adminGroupRes2.numberOfMembers == 1
        adminGroupMembers2.size() == 2
        adminGroupMembers2.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembers2.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER.toString()}
        !adminGroupMembers2.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
    }

    def "remove admin group with project/quiz/badge, members are removed from project, quizzes and badges for this group but remain if also assigned to the same project/quiz/badge in other groups"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup1 = createAdminGroup(1)
        def adminGroup2 = createAdminGroup(2)
        def adminGroup3 = createAdminGroup(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)
        when:

        skillsService.createAdminGroupDef(adminGroup1)
        skillsService.addAdminGroupMember(adminGroup1.adminGroupId, otherUserId)
        skillsService.addQuizToAdminGroup(adminGroup1.adminGroupId, quiz.quizId)
        skillsService.addProjectToAdminGroup(adminGroup1.adminGroupId, proj.projectId)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup1.adminGroupId, badge.badgeId)

        skillsService.createAdminGroupDef(adminGroup2)
        skillsService.addAdminGroupMember(adminGroup2.adminGroupId, otherUserId)
        skillsService.addQuizToAdminGroup(adminGroup2.adminGroupId, quiz.quizId)
        skillsService.addProjectToAdminGroup(adminGroup2.adminGroupId, proj.projectId)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup2.adminGroupId, badge.badgeId)

        skillsService.createAdminGroupDef(adminGroup3)
        skillsService.addAdminGroupMember(adminGroup3.adminGroupId, otherUserId)
        skillsService.addQuizToAdminGroup(adminGroup3.adminGroupId, quiz.quizId)
        skillsService.addProjectToAdminGroup(adminGroup3.adminGroupId, proj.projectId)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup3.adminGroupId, badge.badgeId)

        def projectAdminsBeforeRemove = userRoleRepository.findAllByProjectIdIgnoreCase(proj.projectId)
        def quizAdminsBeforeRemove = userRoleRepository.findAllByQuizIdIgnoreCase(quiz.quizId)
        def badgeAdminsBeforeRemove = userRoleRepository.findAllByGlobalBadgeIdIgnoreCase(badge.badgeId)
        def adminGroupRolesBeforeRemove = userRoleRepository.findAllByAdminGroupIdIgnoreCase(adminGroup1.adminGroupId)
        def adminGroupDefsBeforeRemove = skillsService.getAdminGroupDefs()

        skillsService.removeAdminGroupDef(adminGroup1.adminGroupId)

        def projectAdminsAfterRemove = userRoleRepository.findAllByProjectIdIgnoreCase(proj.projectId)
        def quizAdminsAfterRemove = userRoleRepository.findAllByQuizIdIgnoreCase(quiz.quizId)
        def badgeAdminsAfterRemove = userRoleRepository.findAllByGlobalBadgeIdIgnoreCase(badge.badgeId)
        def adminGroupRolesAfterRemove = userRoleRepository.findAllByAdminGroupIdIgnoreCase(adminGroup1.adminGroupId)
        def adminGroupDefsAfterRemove = skillsService.getAdminGroupDefs()

        then:
        adminGroupDefsBeforeRemove && adminGroupDefsBeforeRemove.size() == 3 &&
                adminGroupDefsBeforeRemove.find { it.adminGroupId == adminGroup1.adminGroupId } &&
                adminGroupDefsBeforeRemove.find { it.adminGroupId == adminGroup2.adminGroupId } &&
                adminGroupDefsBeforeRemove.find { it.adminGroupId == adminGroup3.adminGroupId }
        projectAdminsBeforeRemove.size() == 6
        projectAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup1.adminGroupId }
        projectAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup1.adminGroupId }
        projectAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        projectAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        projectAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }
        projectAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }
        quizAdminsBeforeRemove.size() == 6
        quizAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup1.adminGroupId }
        quizAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup1.adminGroupId }
        quizAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        quizAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        quizAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }
        quizAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }

        badgeAdminsBeforeRemove.size() == 6
        badgeAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup1.adminGroupId }
        badgeAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup1.adminGroupId }
        badgeAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        badgeAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        badgeAdminsBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }
        badgeAdminsBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }

        adminGroupRolesBeforeRemove.size() == 8
        adminGroupRolesBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER && it.adminGroupId == adminGroup1.adminGroupId }
        adminGroupRolesBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER && it.adminGroupId == adminGroup1.adminGroupId }


        projectAdminsAfterRemove.size() == 4
        projectAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup2.adminGroupId  }
        projectAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup2.adminGroupId  }
        projectAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup3.adminGroupId  }
        projectAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == adminGroup3.adminGroupId  }
        quizAdminsAfterRemove.size() == 4
        quizAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        quizAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        quizAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        quizAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup2.adminGroupId }
        quizAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }
        quizAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == adminGroup3.adminGroupId }
        badgeAdminsAfterRemove.size() == 4
        badgeAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup2.adminGroupId  }
        badgeAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup2.adminGroupId  }
        badgeAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup3.adminGroupId  }
        badgeAdminsAfterRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_GLOBAL_BADGE_ADMIN && it.adminGroupId == adminGroup3.adminGroupId  }

        adminGroupRolesAfterRemove.size() == 0
        adminGroupDefsAfterRemove && adminGroupDefsAfterRemove.size() == 2 && adminGroupDefsAfterRemove.find { it.adminGroupId == adminGroup2.adminGroupId } && adminGroupDefsAfterRemove.find { it.adminGroupId == adminGroup2.adminGroupId}
    }

    def "get admin groups for project returns proper results"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        def adminGroup2 = createAdminGroup(2)
        skillsService.createAdminGroupDef(adminGroup2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
        skillsService.addProjectToAdminGroup(adminGroup2.adminGroupId, proj.projectId)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)

        when:
        def adminGroupsForProjectsAsOwner = skillsService.getAdminGroupsForProject(proj.projectId)
        def adminGroupsForProjectsAsMember = memberSkillsService.getAdminGroupsForProject(proj.projectId)

        then:

        adminGroupsForProjectsAsOwner.size() == 2
        adminGroupsForProjectsAsOwner.find { it.adminGroupId == adminGroup.adminGroupId }
        adminGroupsForProjectsAsOwner.find { it.adminGroupId == adminGroup2.adminGroupId }

        adminGroupsForProjectsAsMember.size() == 2
        adminGroupsForProjectsAsMember.find { it.adminGroupId == adminGroup.adminGroupId }
        adminGroupsForProjectsAsMember.find { it.adminGroupId == adminGroup2.adminGroupId }
    }

    def "get admin groups for quiz returns proper results"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        def adminGroup2 = createAdminGroup(2)
        skillsService.createAdminGroupDef(adminGroup2)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        skillsService.addQuizToAdminGroup(adminGroup2.adminGroupId, quiz.quizId)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)

        when:
        def adminGroupsForProjectsAsOwner = skillsService.getAdminGroupsForQuiz(quiz.quizId)
        def adminGroupsForProjectsAsMember = memberSkillsService.getAdminGroupsForQuiz(quiz.quizId)

        then:

        adminGroupsForProjectsAsOwner.size() == 2
        adminGroupsForProjectsAsOwner.find { it.adminGroupId == adminGroup.adminGroupId }
        adminGroupsForProjectsAsOwner.find { it.adminGroupId == adminGroup2.adminGroupId }

        adminGroupsForProjectsAsMember.size() == 2
        adminGroupsForProjectsAsMember.find { it.adminGroupId == adminGroup.adminGroupId }
        adminGroupsForProjectsAsMember.find { it.adminGroupId == adminGroup2.adminGroupId }
    }

    def "get admin groups for global badge returns proper results"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService memberSkillsService = createService(otherUserId)

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        def adminGroup2 = createAdminGroup(2)
        skillsService.createAdminGroupDef(adminGroup2)

        def badge1 = createBadge(1, 1)
        skillsService.createGlobalBadge(badge1)

        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup2.adminGroupId, badge1.badgeId)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)

        when:
        def adminGroupsForProjectsAsOwner = skillsService.getAdminGroupsForGlobalBadge(badge1.badgeId)
        def adminGroupsForProjectsAsMember = memberSkillsService.getAdminGroupsForGlobalBadge(badge1.badgeId)

        then:

        adminGroupsForProjectsAsOwner.size() == 2
        adminGroupsForProjectsAsOwner.find { it.adminGroupId == adminGroup.adminGroupId }
        adminGroupsForProjectsAsOwner.find { it.adminGroupId == adminGroup2.adminGroupId }

        adminGroupsForProjectsAsMember.size() == 2
        adminGroupsForProjectsAsMember.find { it.adminGroupId == adminGroup.adminGroupId }
        adminGroupsForProjectsAsMember.find { it.adminGroupId == adminGroup2.adminGroupId }
    }

    def "user can be a member of multiple admin groups that are assigned to the same quiz"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)

        def adminGroup1 = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup1)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.addQuizToAdminGroup(adminGroup1.adminGroupId, quiz.quizId)
        skillsService.addAdminGroupMember(adminGroup1.adminGroupId, otherUserId)

        def adminGroup2 = createAdminGroup(2)
        skillsService.createAdminGroupDef(adminGroup2)
        skillsService.addQuizToAdminGroup(adminGroup2.adminGroupId, quiz.quizId)

        when:
        skillsService.addAdminGroupMember(adminGroup2.adminGroupId, otherUserId)

        def adminGroup1Res = skillsService.getAdminGroupDef(adminGroup1.adminGroupId)
        def adminGroup1QuizzesAndSurveys = skillsService.getAdminGroupQuizzesAndSurveys(adminGroup1.adminGroupId)

        def adminGroup2Res = skillsService.getAdminGroupDef(adminGroup2.adminGroupId)
        def adminGroup2QuizzesAndSurveys = skillsService.getAdminGroupQuizzesAndSurveys(adminGroup2.adminGroupId)

        then:
        adminGroup1Res.adminGroupId == adminGroup1.adminGroupId
        adminGroup1Res.numberOfOwners == 1
        adminGroup1Res.numberOfProjects == 0
        adminGroup1Res.numberOfQuizzesAndSurveys == 1

        adminGroup1QuizzesAndSurveys.adminGroupId == adminGroup1.adminGroupId
        adminGroup1QuizzesAndSurveys.assignedQuizzes.size() == 1 && adminGroup1QuizzesAndSurveys.assignedQuizzes.find { it.quizId == quiz.quizId }


        adminGroup2Res.adminGroupId == adminGroup2.adminGroupId
        adminGroup2Res.numberOfOwners == 1
        adminGroup2Res.numberOfProjects == 0
        adminGroup2Res.numberOfQuizzesAndSurveys == 1

        adminGroup2QuizzesAndSurveys.adminGroupId == adminGroup2.adminGroupId
        adminGroup2QuizzesAndSurveys.assignedQuizzes.size() == 1 && adminGroup1QuizzesAndSurveys.assignedQuizzes.find { it.quizId == quiz.quizId }
    }

    def "user can be a member of multiple admin groups that are assigned to the same global badge"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)

        def adminGroup1 = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup1)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup1.adminGroupId, badge.badgeId)
        skillsService.addAdminGroupMember(adminGroup1.adminGroupId, otherUserId)

        def adminGroup2 = createAdminGroup(2)
        skillsService.createAdminGroupDef(adminGroup2)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup2.adminGroupId, badge.badgeId)

        when:
        skillsService.addAdminGroupMember(adminGroup2.adminGroupId, otherUserId)

        def adminGroup1Res = skillsService.getAdminGroupDef(adminGroup1.adminGroupId)
        def adminGroup1GlobalBadges = skillsService.getAdminGroupGlobalBadges(adminGroup1.adminGroupId)

        def adminGroup2Res = skillsService.getAdminGroupDef(adminGroup2.adminGroupId)
        def adminGroup2GlobalBadges = skillsService.getAdminGroupGlobalBadges(adminGroup2.adminGroupId)

        then:
        adminGroup1Res.adminGroupId == adminGroup1.adminGroupId
        adminGroup1Res.numberOfOwners == 1
        adminGroup1Res.numberOfProjects == 0
        adminGroup1Res.numberOfGlobalBadges == 1

        adminGroup1GlobalBadges.adminGroupId == adminGroup1.adminGroupId
        adminGroup1GlobalBadges.assignedGlobalBadges.size() == 1 && adminGroup1GlobalBadges.assignedGlobalBadges.find { it.badgeId == badge.badgeId }


        adminGroup2Res.adminGroupId == adminGroup2.adminGroupId
        adminGroup2Res.numberOfOwners == 1
        adminGroup2Res.numberOfProjects == 0
        adminGroup2Res.numberOfQuizzesAndSurveys == 0
        adminGroup2Res.numberOfGlobalBadges == 1

        adminGroup2GlobalBadges.adminGroupId == adminGroup2.adminGroupId
        adminGroup2GlobalBadges.assignedGlobalBadges.size() == 1 && adminGroup1GlobalBadges.assignedGlobalBadges.find { it.badgeId == badge.badgeId }
    }

    def "owner can remove self if there are other admins in admin group"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        def otherService = createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupOwner(adminGroup.adminGroupId, otherUserId)

        when:
        def adminGroupResBefore = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembersBefore = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)
        otherService.deleteAdminGroupOwner(adminGroup.adminGroupId, otherUserId)
        def adminGroupResAfter = skillsService.getAdminGroupDef(adminGroup.adminGroupId)
        def adminGroupMembersAfter = skillsService.getAdminGroupMembers(adminGroup.adminGroupId)

        then:

        adminGroupResBefore.adminGroupId == adminGroup.adminGroupId
        adminGroupResBefore.numberOfOwners == 2
        adminGroupMembersBefore.size() == 2
        adminGroupMembersBefore.find {it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}
        adminGroupMembersBefore.find {it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.toString()}

        adminGroupResAfter.adminGroupId == adminGroup.adminGroupId
        adminGroupResAfter.numberOfOwners == 1
        adminGroupMembersAfter.size() == 1
        !adminGroupMembersAfter.find { it.userId == otherUserId }
    }

    def "owner can not remove self if there are no other admins in admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        when:
        skillsService.deleteAdminGroupOwner(adminGroup.adminGroupId, skillsService.currentUser.userId)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot delete roles for myself when there are no other admins")
    }

    @Unroll
    def "removing unrelated #resourceType must not grant admin after a denied add - resource group assigned=#assignedToResourceGroup, group casing=#groupCasing, resource casing=#resourceCasing"() {
        List<String> users = getRandomUsers(3)
        SkillsService resourceOwner = createService(users[0])
        SkillsService groupOwner = createService(users[1])
        SkillsService resourceGroupMember = createService(users[2])
        Map resource = createRemovalTestResource(resourceOwner, resourceType)
        Map unrelatedGroup = createAdminGroup(1)
        Map resourceGroup = createAdminGroup(2)
        groupOwner.createAdminGroupDef(unrelatedGroup)
        if (assignedToResourceGroup) {
            resourceOwner.createAdminGroupDef(resourceGroup)
            resourceOwner.addAdminGroupMember(resourceGroup.adminGroupId, users[2])
            addRemovalTestResource(resourceOwner, resourceType, resourceGroup.adminGroupId, resource.id)
            assert readRemovalTestResource(resourceGroupMember, resourceType, resource.id).name == resource.name
        }
        assert !groupOwner.isRoot()
        assert groupOwner.getAdminGroupMembers(unrelatedGroup.adminGroupId).find {
            it.userId == groupOwner.userName.toLowerCase() && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER.name()
        }
        assert !assignedRemovalTestResources(groupOwner, resourceType, unrelatedGroup.adminGroupId)
        assert removalRequestFailure { readRemovalTestResource(groupOwner, resourceType, resource.id) }?.httpStatus?.value() == 403
        assert removalRequestFailure {
            addRemovalTestResource(groupOwner, resourceType, unrelatedGroup.adminGroupId, resource.id)
        }?.httpStatus?.value() == 403
        List<Map> rolesBefore = removalTestRoleSnapshot(resourceType, resource.id)

        when:
        SkillsClientException removalFailure = removalRequestFailure {
            removeRemovalTestResource(groupOwner, resourceType, removalTestIdCasing(unrelatedGroup.adminGroupId, groupCasing), removalTestIdCasing(resource.id, resourceCasing))
        }

        then:
        // Check all outcomes even when DELETE incorrectly succeeds, including subsequent resource access.
        verifyAll {
            removalFailure?.httpStatus?.value() == 403
            removalRequestFailure { readRemovalTestResource(groupOwner, resourceType, resource.id) }?.httpStatus?.value() == 403
            removalTestRoleSnapshot(resourceType, resource.id) == rolesBefore
            readRemovalTestResource(resourceOwner, resourceType, resource.id).name == resource.name
            !assignedRemovalTestResources(groupOwner, resourceType, unrelatedGroup.adminGroupId)
            if (assignedToResourceGroup) {
                assert assignedRemovalTestResources(resourceOwner, resourceType, resourceGroup.adminGroupId)*.name == [resource.name]
                assert readRemovalTestResource(resourceGroupMember, resourceType, resource.id).name == resource.name
            }
        }

        where:
        [resourceType, assignedToResourceGroup, groupCasing, resourceCasing] << [
                ['project', 'quiz', 'survey', 'global badge'], [false, true],
                ['original', 'lower', 'upper'], ['original', 'lower', 'upper']
        ].combinations()
    }

    @Unroll
    def "community membership alone must not authorize removing unrelated protected #resourceType - member=#communityMember"() {
        List<String> users = getRandomUsers(2)
        SkillsService resourceOwner = createService(users[0])
        SkillsService groupOwner = createService(users[1])
        SkillsService root = createRootSkillService()
        root.saveUserTag(resourceOwner.userName, 'dragons', ['DivineDragon'])
        if (communityMember) {
            root.saveUserTag(groupOwner.userName, 'dragons', ['DivineDragon'])
        }
        Map resource = createRemovalTestResource(resourceOwner, resourceType, true)
        Map unrelatedGroup = createAdminGroup(1)
        groupOwner.createAdminGroupDef(unrelatedGroup)
        assert !groupOwner.isRoot()
        assert removalRequestFailure { readRemovalTestResource(groupOwner, resourceType, resource.id) }?.httpStatus?.value() == 403
        List<Map> rolesBefore = removalTestRoleSnapshot(resourceType, resource.id)

        when:
        SkillsClientException removalFailure = removalRequestFailure {
            removeRemovalTestResource(groupOwner, resourceType, unrelatedGroup.adminGroupId, resource.id)
        }

        then:
        verifyAll {
            // Existing community checks can return a structured AccessDenied response with HTTP 400.
            removalFailure?.httpStatus?.value() in [403, 400]
            removalRequestFailure { readRemovalTestResource(groupOwner, resourceType, resource.id) }?.httpStatus?.value() == 403
            removalTestRoleSnapshot(resourceType, resource.id) == rolesBefore
            readRemovalTestResource(resourceOwner, resourceType, resource.id).name == resource.name
            !assignedRemovalTestResources(groupOwner, resourceType, unrelatedGroup.adminGroupId)
        }

        where:
        [resourceType, communityMember] << [['project', 'quiz', 'survey', 'global badge'], [false, true]].combinations()
    }

    @Unroll
    def "resource admin cannot remove #resourceType through an unrelated group they own - group casing=#groupCasing, resource casing=#resourceCasing"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService member = createService(users[1])
        Map resource = createRemovalTestResource(owner, resourceType)
        Map assignedGroup = createAdminGroup(1)
        Map unrelatedGroup = createAdminGroup(2)
        owner.createAdminGroupDef(assignedGroup)
        owner.createAdminGroupDef(unrelatedGroup)
        owner.addAdminGroupMember(assignedGroup.adminGroupId, users[1])
        addRemovalTestResource(owner, resourceType, assignedGroup.adminGroupId, resource.id)
        List<Map> rolesBefore = removalTestRoleSnapshot(resourceType, resource.id)

        when:
        SkillsClientException removalFailure = removalRequestFailure {
            removeRemovalTestResource(owner, resourceType, removalTestIdCasing(unrelatedGroup.adminGroupId, groupCasing), removalTestIdCasing(resource.id, resourceCasing))
        }

        then:
        verifyAll {
            removalFailure?.httpStatus?.value() in [403, 400]
            removalTestRoleSnapshot(resourceType, resource.id) == rolesBefore
            assignedRemovalTestResources(owner, resourceType, assignedGroup.adminGroupId)*.name == [resource.name]
            !assignedRemovalTestResources(owner, resourceType, unrelatedGroup.adminGroupId)
            readRemovalTestResource(member, resourceType, resource.id).name == resource.name
        }

        where:
        [resourceType, groupCasing, resourceCasing] << [
                ['project', 'quiz', 'survey', 'global badge'],
                ['original', 'lower', 'upper'], ['original', 'lower', 'upper']
        ].combinations()
    }

    @Unroll
    def "group member cannot detach #resourceType despite having resource admin access"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService member = createService(users[1])
        Map resource = createRemovalTestResource(owner, resourceType)
        Map group = createAdminGroup(1)
        owner.createAdminGroupDef(group)
        owner.addAdminGroupMember(group.adminGroupId, users[1])
        addRemovalTestResource(owner, resourceType, group.adminGroupId, resource.id)
        assert readRemovalTestResource(member, resourceType, resource.id).name == resource.name
        List<Map> rolesBefore = removalTestRoleSnapshot(resourceType, resource.id)

        when:
        removeRemovalTestResource(member, resourceType, group.adminGroupId, resource.id)

        then:
        SkillsClientException removalFailure = thrown(SkillsClientException)
        removalFailure.httpStatus.value() == 403
        removalTestRoleSnapshot(resourceType, resource.id) == rolesBefore
        assignedRemovalTestResources(owner, resourceType, group.adminGroupId)*.name == [resource.name]
        readRemovalTestResource(member, resourceType, resource.id).name == resource.name

        where:
        resourceType << ['project', 'quiz', 'survey', 'global badge']
    }

    @Unroll
    def "legitimate #resourceType removal retains direct owner admin and revokes group member access - group casing=#groupCasing, resource casing=#resourceCasing"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService member = createService(users[1])
        Map resource = createRemovalTestResource(owner, resourceType)
        Map group = createAdminGroup(1)
        owner.createAdminGroupDef(group)
        owner.addAdminGroupMember(group.adminGroupId, users[1])
        addRemovalTestResource(owner, resourceType, group.adminGroupId, resource.id)
        assert assignedRemovalTestResources(owner, resourceType, group.adminGroupId)*.name == [resource.name]
        assert readRemovalTestResource(member, resourceType, resource.id).name == resource.name

        when:
        removeRemovalTestResource(owner, resourceType, removalTestIdCasing(group.adminGroupId, groupCasing), removalTestIdCasing(resource.id, resourceCasing))

        then:
        readRemovalTestResource(owner, resourceType, resource.id).name == resource.name
        !assignedRemovalTestResources(owner, resourceType, group.adminGroupId)
        removalRequestFailure { readRemovalTestResource(member, resourceType, resource.id) }?.httpStatus?.value() == 403
        owner.getAdminGroupMembers(group.adminGroupId)*.userId.toSet() == users.collect { String user -> user.toLowerCase() }.toSet()
        // The API does not expose whether retained administration is standalone or group-derived.
        List<Map> rolesAfter = removalTestRoleSnapshot(resourceType, resource.id)
        rolesAfter.size() == 1
        rolesAfter[0].userId == owner.userName.toLowerCase()
        rolesAfter[0].adminGroupId == null
        rolesAfter[0].roleName == resource.roleName

        where:
        [resourceType, groupCasing, resourceCasing] << [
                ['project', 'quiz', 'survey', 'global badge'],
                ['original', 'lower', 'upper'], ['original', 'lower', 'upper']
        ].combinations()
    }

    @Unroll
    def "removing a #membershipRole revokes #resourceType group access and preserves other group access=#retainOtherGroupAccess"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService groupUser = createService(users[1])
        Map resource = createRemovalTestResource(owner, resourceType)
        Map group = createAdminGroup(1)
        Map otherGroup = createAdminGroup(2)
        owner.createAdminGroupDef(group)
        if (membershipRole == 'owner') {
            owner.addAdminGroupOwner(group.adminGroupId, groupUser.userName)
        } else {
            owner.addAdminGroupMember(group.adminGroupId, groupUser.userName)
        }
        addRemovalTestResource(owner, resourceType, group.adminGroupId, resource.id)
        if (retainOtherGroupAccess) {
            owner.createAdminGroupDef(otherGroup)
            owner.addAdminGroupMember(otherGroup.adminGroupId, groupUser.userName)
            addRemovalTestResource(owner, resourceType, otherGroup.adminGroupId, resource.id)
        }
        assert readRemovalTestResource(groupUser, resourceType, resource.id).name == resource.name

        when:
        if (membershipRole == 'owner') {
            owner.deleteAdminGroupOwner(group.adminGroupId, groupUser.userName)
        } else {
            owner.deleteAdminGroupMember(group.adminGroupId, groupUser.userName)
        }

        then:
        !owner.getAdminGroupMembers(group.adminGroupId).find { it.userId == groupUser.userName.toLowerCase() }
        readRemovalTestResource(owner, resourceType, resource.id).name == resource.name
        assignedRemovalTestResources(owner, resourceType, group.adminGroupId)*.name == [resource.name]
        if (retainOtherGroupAccess) {
            assert readRemovalTestResource(groupUser, resourceType, resource.id).name == resource.name
            assert owner.getAdminGroupMembers(otherGroup.adminGroupId).find { it.userId == groupUser.userName.toLowerCase() }
            assert assignedRemovalTestResources(owner, resourceType, otherGroup.adminGroupId)*.name == [resource.name]
        } else {
            assert removalRequestFailure { readRemovalTestResource(groupUser, resourceType, resource.id) }?.httpStatus == HttpStatus.FORBIDDEN
        }
        // Confirm that any retained administration is not derived from the removed membership.
        !removalTestRoleSnapshot(resourceType, resource.id).find { Map role ->
            role.userId == groupUser.userName.toLowerCase() && role.adminGroupId == group.adminGroupId
        }

        where:
        [resourceType, membershipRole, retainOtherGroupAccess] << [
                ['project', 'quiz', 'survey', 'global badge'], ['member', 'owner'], [false, true]
        ].combinations()
    }

    @Unroll
    def "removing missing or unauthorized #resourceType returns the same denial - casing=#casing"() {
        List<String> users = getRandomUsers(2)
        SkillsService resourceOwner = createService(users[0])
        SkillsService groupOwner = createService(users[1])
        Map resource = createRemovalTestResource(resourceOwner, resourceType)
        Map group = createAdminGroup(1)
        groupOwner.createAdminGroupDef(group)
        List<Map> rolesBefore = removalTestRoleSnapshot(resourceType, resource.id)
        String requestedGroupId = removalTestIdCasing(group.adminGroupId, casing)
        String requestedResourceId = removalTestIdCasing(resource.id, casing)
        String missingResourceId = removalTestIdCasing("Missing${resource.id}".toString(), casing)

        when:
        SkillsClientException existingFailure = removalRequestFailure {
            removeRemovalTestResource(groupOwner, resourceType, requestedGroupId, requestedResourceId)
        }
        SkillsClientException missingFailure = removalRequestFailure {
            removeRemovalTestResource(groupOwner, resourceType, requestedGroupId, missingResourceId)
        }

        then:
        existingFailure?.httpStatus == HttpStatus.FORBIDDEN
        missingFailure?.httpStatus == HttpStatus.FORBIDDEN
        existingFailure.res.body == [errorCode: 'AccessDenied', explanation: 'Access Denied', success: false]
        missingFailure.res.body == existingFailure.res.body
        removalTestRoleSnapshot(resourceType, resource.id) == rolesBefore
        readRemovalTestResource(resourceOwner, resourceType, resource.id).name == resource.name
        removalRequestFailure { readRemovalTestResource(groupOwner, resourceType, resource.id) }?.httpStatus == HttpStatus.FORBIDDEN
        !assignedRemovalTestResources(groupOwner, resourceType, group.adminGroupId)

        where:
        [resourceType, casing] << [['project', 'quiz', 'survey', 'global badge'], ['original', 'lower', 'upper']].combinations()
    }

    private static String removalTestIdCasing(String id, String casing) {
        switch (casing) {
            case 'original': return id
            case 'lower': return id.toLowerCase(Locale.ROOT)
            case 'upper': return id.toUpperCase(Locale.ROOT)
            default: throw new IllegalArgumentException("Unsupported ID casing [${casing}]")
        }
    }

    private Map createRemovalTestResource(SkillsService owner, String resourceType, boolean protectedCommunity = false) {
        Map definition
        RoleName roleName
        String id
        switch (resourceType) {
            case 'project':
                definition = createProject(1)
                definition.enableProtectedUserCommunity = protectedCommunity
                owner.createProject(definition)
                id = definition.projectId
                roleName = RoleName.ROLE_PROJECT_ADMIN
                break
            case 'quiz':
            case 'survey':
                definition = resourceType == 'quiz' ? QuizDefFactory.createQuiz(1) : QuizDefFactory.createQuizSurvey(1)
                definition.enableProtectedUserCommunity = protectedCommunity
                owner.createQuizDef(definition)
                id = definition.quizId
                roleName = RoleName.ROLE_QUIZ_ADMIN
                break
            case 'global badge':
                definition = createBadge(1, 1)
                definition.enableProtectedUserCommunity = protectedCommunity
                owner.createGlobalBadge(definition)
                id = definition.badgeId
                roleName = RoleName.ROLE_GLOBAL_BADGE_ADMIN
                break
            default:
                throw new IllegalArgumentException("Unsupported resource type [${resourceType}]")
        }
        return [id: id, name: definition.name, roleName: roleName]
    }

    private static Map readRemovalTestResource(SkillsService client, String resourceType, String resourceId) {
        switch (resourceType) {
            case 'project': return client.getProject(resourceId)
            case 'quiz':
            case 'survey': return client.getQuizDef(resourceId)
            case 'global badge': return client.getGlobalBadge(resourceId)
            default: throw new IllegalArgumentException("Unsupported resource type [${resourceType}]")
        }
    }

    private static void addRemovalTestResource(SkillsService client, String resourceType, String adminGroupId, String resourceId) {
        switch (resourceType) {
            case 'project':
                client.addProjectToAdminGroup(adminGroupId, resourceId)
                break
            case 'quiz':
            case 'survey':
                client.addQuizToAdminGroup(adminGroupId, resourceId)
                break
            case 'global badge':
                client.addGlobalBadgeToAdminGroup(adminGroupId, resourceId)
                break
            default: throw new IllegalArgumentException("Unsupported resource type [${resourceType}]")
        }
    }

    private static void removeRemovalTestResource(SkillsService client, String resourceType, String adminGroupId, String resourceId) {
        switch (resourceType) {
            case 'project':
                client.deleteProjectFromAdminGroup(adminGroupId, resourceId)
                break
            case 'quiz':
            case 'survey':
                client.deleteQuizFromAdminGroup(adminGroupId, resourceId)
                break
            case 'global badge':
                client.deleteGlobalBadgeFromAdminGroup(adminGroupId, resourceId)
                break
            default: throw new IllegalArgumentException("Unsupported resource type [${resourceType}]")
        }
    }

    private static List<Map> assignedRemovalTestResources(SkillsService client, String resourceType, String adminGroupId) {
        switch (resourceType) {
            case 'project': return client.getAdminGroupProjects(adminGroupId).assignedProjects
            case 'quiz':
            case 'survey': return client.getAdminGroupQuizzesAndSurveys(adminGroupId).assignedQuizzes
            case 'global badge': return client.getAdminGroupGlobalBadges(adminGroupId).assignedGlobalBadges
            default: throw new IllegalArgumentException("Unsupported resource type [${resourceType}]")
        }
    }

    private List<Map> removalTestRoleSnapshot(String resourceType, String resourceId) {
        List<UserRole> roles
        switch (resourceType) {
            case 'project':
                roles = userRoleRepository.findAllByProjectIdIgnoreCase(resourceId)
                break
            case 'quiz':
            case 'survey':
                roles = userRoleRepository.findAllByQuizIdIgnoreCase(resourceId)
                break
            case 'global badge':
                roles = userRoleRepository.findAllByGlobalBadgeIdIgnoreCase(resourceId)
                break
            default: throw new IllegalArgumentException("Unsupported resource type [${resourceType}]")
        }
        return roles.sort { UserRole role -> role.id }.collect { UserRole role ->
            [id: role.id, userId: role.userId, roleName: role.roleName, adminGroupId: role.adminGroupId]
        }
    }

    private static SkillsClientException removalRequestFailure(Closure request) {
        try {
            request.call()
            return null
        } catch (SkillsClientException exception) {
            return exception
        }
    }

    def "owner cannot add project they do not administrate to admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)

        SkillsService otherUser = createService("someOtherUser")
        otherUser.createProjectAndSubjectAndSkills(proj, subj, [skill])

        when:
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        then:
        def exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }
    def "owner cannot add quiz they do not administrate to admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        SkillsService otherUser = createService("someOtherUser")
        otherUser.createQuizDef(quiz)

        when:
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        then:
        def exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "owner cannot add badge they do not administrate to admin group"() {
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge = createBadge(1, 1)
        SkillsService otherUser = createService("someOtherUser")
        otherUser.createGlobalBadge(badge)

        when:
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge.badgeId)

        then:
        def exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

}
