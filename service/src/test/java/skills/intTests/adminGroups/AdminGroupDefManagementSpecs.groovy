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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserRoleRepo

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
        when:

        skillsService.createAdminGroupDef(adminGroup)
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
}
