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

    def "add owner to admin group"() {
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

    def "add member to admin group"() {
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

    def "add project to admin group"() {
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

        adminGroupProjects.adminGroupId == adminGroup.adminGroupId
        adminGroupProjects.assignedProjects.size() == 1 && adminGroupProjects.assignedProjects.find { it.projectId == proj.projectId }
        adminGroupProjects.availableProjects.size() == 1 && adminGroupProjects.availableProjects.find { it.projectId == proj2.projectId }
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

    def "add quiz to admin group"() {
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

        adminGroupQuizzesAndSurveys.adminGroupId == adminGroup.adminGroupId
        adminGroupQuizzesAndSurveys.assignedQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.assignedQuizzes.find { it.quizId == quiz.quizId }
        adminGroupQuizzesAndSurveys.availableQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.availableQuizzes.find { it.quizId == quiz2.quizId }
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

    def "remove project from admin group"() {
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

        adminGroupProjectsBeforeRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupProjectsBeforeRemove.assignedProjects.size() == 1 && adminGroupProjectsBeforeRemove.assignedProjects.find { it.projectId == proj.projectId }
        adminGroupProjectsBeforeRemove.availableProjects.size() == 1 && adminGroupProjectsBeforeRemove.availableProjects.find { it.projectId == proj2.projectId }

        adminGroupProjectsAfterRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupProjectsAfterRemove.assignedProjects.size() == 0
        adminGroupProjectsAfterRemove.availableProjects.size() == 2
                && adminGroupProjectsAfterRemove.availableProjects.find { it.projectId == proj.projectId }
                && adminGroupProjectsAfterRemove.availableProjects.find { it.projectId == proj2.projectId }
    }

    def "remove quiz from admin group"() {
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

        adminGroupQuizzesAndSurveys.adminGroupId == adminGroup.adminGroupId
        adminGroupQuizzesAndSurveys.assignedQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.assignedQuizzes.find { it.quizId == quiz.quizId }
        adminGroupQuizzesAndSurveys.availableQuizzes.size() == 1 && adminGroupQuizzesAndSurveys.availableQuizzes.find { it.quizId == quiz2.quizId }

        adminGroupQuizzesAndSurveysAfterRemove.adminGroupId == adminGroup.adminGroupId
        adminGroupQuizzesAndSurveysAfterRemove.assignedQuizzes.size() == 0
        adminGroupQuizzesAndSurveysAfterRemove.availableQuizzes.size() == 2
                && adminGroupQuizzesAndSurveysAfterRemove.availableQuizzes.find { it.quizId == quiz.quizId }
                && adminGroupQuizzesAndSurveysAfterRemove.availableQuizzes.find { it.quizId == quiz2.quizId }
    }

    def "removing an admin group removes all other members/owners from quiz/project admin roles except current user, who remains as a 'local' admin"() {
        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        when:

        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        def projectAdminsBeforeRemove = userRoleRepository.findAllByProjectIdIgnoreCase(proj.projectId)
        def quizAdminsBeforeRemove = userRoleRepository.findAllByQuizIdIgnoreCase(quiz.quizId)
        def adminGroupRolesBeforeRemove = userRoleRepository.findAllByAdminGroupIdIgnoreCase(adminGroup.adminGroupId)
        def adminGroupDefsBeforeRemove = skillsService.getAdminGroupDefs()

        skillsService.removeAdminGroupDef(adminGroup.adminGroupId)

        def projectAdminsAfterRemove = userRoleRepository.findAllByProjectIdIgnoreCase(proj.projectId)
        def quizAdminsAfterRemove = userRoleRepository.findAllByQuizIdIgnoreCase(quiz.quizId)
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
        adminGroupRolesBeforeRemove.size() == 6
        adminGroupRolesBeforeRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_ADMIN_GROUP_OWNER && it.adminGroupId == adminGroup.adminGroupId }
        adminGroupRolesBeforeRemove.find { it.userId == otherUserId && it.roleName == RoleName.ROLE_ADMIN_GROUP_MEMBER && it.adminGroupId == adminGroup.adminGroupId }


        projectAdminsAfterRemove.size() == 1
        projectAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN && it.adminGroupId == null }
        quizAdminsAfterRemove.size() == 1
        quizAdminsAfterRemove.find { it.userId == skillsService.currentUser.userId && it.roleName == RoleName.ROLE_QUIZ_ADMIN && it.adminGroupId == null }
        adminGroupRolesAfterRemove.size() == 0

        !adminGroupDefsAfterRemove
    }

}
