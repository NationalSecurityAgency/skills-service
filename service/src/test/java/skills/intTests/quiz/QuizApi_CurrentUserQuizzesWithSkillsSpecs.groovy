/**
 * Copyright 2026 SkillTree
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
package skills.intTests.quiz

import skills.intTests.inviteOnly.InviteOnlyBaseSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.settings.Settings
import skills.storage.model.SkillDef
import skills.utils.WaitFor

import static skills.intTests.utils.SkillsFactory.*
import static skills.storage.model.QuizDefParent.QuizType.Quiz
import static skills.storage.model.QuizDefParent.QuizType.Survey
import static skills.storage.model.UserQuizAttempt.QuizAttemptStatus.FAILED
import static skills.storage.model.UserQuizAttempt.QuizAttemptStatus.PASSED

class QuizApi_CurrentUserQuizzesWithSkillsSpecs extends InviteOnlyBaseSpec {

    private static Integer runQuizOrSurvey(SkillsService service, Integer num, int answerNumToReport = 0, boolean complete = true) {
        String quizId = QuizDefFactory.getDefaultQuizId(num)
        def quizAttempt =  service.startQuizAttempt(quizId).body
        service.reportQuizAnswer(quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[answerNumToReport].id)
        if (complete) {
            service.completeQuizAttempt(quizId, quizAttempt.id)
        }
        return quizAttempt.id
    }
    private static def createSimpleSurvey(SkillsService service, Integer num) {
        def survey = QuizDefFactory.createQuizSurvey(num)
        service.createQuizDef(survey)
        def questions = [ QuizDefFactory.createSingleChoiceSurveyQuestion(num)]
        service.createQuizQuestionDefs(questions)
        service.saveQuizSettings(survey.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])

        return survey
    }
    private static def createSimpleQuiz(SkillsService service, Integer num, String name = null) {
        def quiz = QuizDefFactory.createQuiz(num)
        if (name) {
            quiz.name = name
        }
        service.createQuizDef(quiz)
        def questions = [ QuizDefFactory.createChoiceQuestion(num)]
        service.createQuizQuestionDefs(questions)
        service.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])
        return quiz
    }
    private static def createSimpleProject(SkillsService service, Integer num, Boolean enableProtectedUserCommunity = false) {
        def proj = createProject(num)
        proj.enableProtectedUserCommunity = enableProtectedUserCommunity
        def subj = createSubject(num, 1)
        def skills = SkillsFactory.createSkills(5, num, 1, 100)
        service.createProjectAndSubjectAndSkills(proj, subj, skills)

        def subj2 = createSubject(num, 2)
        def subj2Skills = SkillsFactory.createSkills(5, num, 2, 100)
        service.createProjectAndSubjectAndSkills(null, subj2, subj2Skills)

        return [skills, subj2Skills].flatten()
    }

    private static def associate(SkillsService service, def skill, def quiz) {
        skill.selfReportingType = SkillDef.SelfReportingType.Quiz
        skill.quizId = quiz.quizId
        service.updateSkill(skill)
    }

    def "get quiz attempts - multiple associated skills"() {
        List<SkillsService> allUsers = getRandomUserServices(5)
        List<SkillsService> admins = allUsers[0..1]
        List<SkillsService> users = allUsers[2..4]

        def survey1 = createSimpleSurvey(admins[0], 1)
        def quiz1 = createSimpleQuiz(admins[0], 2)

        def proj1Skills = createSimpleProject(admins[0], 1)
        def proj2Skills = createSimpleProject(admins[0], 2)
        def proj3Skills = createSimpleProject(admins[0], 3)

        associate(admins[0], proj1Skills[0], quiz1)
        associate(admins[0], proj1Skills[6], quiz1)
        associate(admins[0], proj2Skills[3], quiz1)

        associate(admins[0], proj3Skills[2], survey1)
        associate(admins[0], proj3Skills[3], survey1)
        associate(admins[0], proj3Skills[9], survey1)
        associate(admins[0], proj2Skills[0], survey1)

        runQuizOrSurvey(users[0], 1)
        runQuizOrSurvey(users[0], 2)
        runQuizOrSurvey(users[0], 2, 1 )

        runQuizOrSurvey(users[1], 2, 1)

        when:
        def res = users[0].getCurrentUserQuizAttempts()
        def user1Res = users[1].getCurrentUserQuizAttempts()
        then:
        res.count == 3
        res.totalCount == 3
        res.data.quizId == [survey1.quizId, quiz1.quizId, quiz1.quizId]
        res.data.status == [ PASSED.toString(), PASSED.toString(), FAILED.toString()]
        res.data.quizType == [Survey.toString(), Quiz.toString(), Quiz.toString()]
        res.data[0].associatedSkills
        def record1Skills = res.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record1Skills.projectId == [proj2Skills[0].projectId, proj3Skills[0].projectId, proj3Skills[0].projectId, proj3Skills[0].projectId]
        record1Skills.subjectId == [getSubjectId(1), getSubjectId(1), getSubjectId(1), getSubjectId(2)]
        record1Skills.projectName == [getDefaultProjName(2), getDefaultProjName(3), getDefaultProjName(3), getDefaultProjName(3)]
        record1Skills.skillId == [proj2Skills[0].skillId, proj3Skills[2].skillId, proj3Skills[3].skillId, proj3Skills[9].skillId]
        record1Skills.skillName == [proj2Skills[0].name, proj3Skills[2].name, proj3Skills[3].name, proj3Skills[9].name]

        res.data[1].associatedSkills
        def record2Skills = res.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record2Skills.projectId == [proj1Skills[0].projectId, proj1Skills[0].projectId, proj2Skills[0].projectId]
        record2Skills.projectName == [getDefaultProjName(1), getDefaultProjName(1), getDefaultProjName(2)]
        record2Skills.subjectId == [getSubjectId(1), getSubjectId(2), getSubjectId(1)]
        record2Skills.skillId == [proj1Skills[0].skillId, proj1Skills[6].skillId, proj1Skills[3].skillId]
        record2Skills.skillName == [proj1Skills[0].name, proj1Skills[6].name, proj1Skills[3].name]

        def record3Skills = res.data[2].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record3Skills.projectId == [proj1Skills[0].projectId, proj1Skills[0].projectId, proj2Skills[0].projectId]
        record3Skills.projectName == [getDefaultProjName(1), getDefaultProjName(1), getDefaultProjName(2)]
        record3Skills.subjectId == [getSubjectId(1), getSubjectId(2), getSubjectId(1)]
        record3Skills.skillId == [proj1Skills[0].skillId, proj1Skills[6].skillId, proj1Skills[3].skillId]
        record3Skills.skillName == [proj1Skills[0].name, proj1Skills[6].name, proj1Skills[3].name]

        // user 1
        user1Res.count == 1
        user1Res.totalCount == 1
        user1Res.data.quizId == [quiz1.quizId]
        user1Res.data.status == [FAILED.toString()]
        user1Res.data.quizType == [Quiz.toString()]
        user1Res.data[0].associatedSkills
        def user1ResRecord1Skills = user1Res.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        user1ResRecord1Skills.projectId == [proj1Skills[0].projectId, proj1Skills[0].projectId, proj2Skills[0].projectId]
        user1ResRecord1Skills.subjectId == [getSubjectId(1), getSubjectId(2), getSubjectId(1)]
        user1ResRecord1Skills.projectName == [getDefaultProjName(1), getDefaultProjName(1), getDefaultProjName(2)]
        user1ResRecord1Skills.skillId == [proj1Skills[0].skillId, proj1Skills[6].skillId, proj2Skills[3].skillId]
        user1ResRecord1Skills.skillName == [proj1Skills[0].name, proj1Skills[6].name, proj2Skills[3].name]
    }

    def "get quiz attempts - community protected skills are not returned for non-community users"() {
        SkillsService rootSkillsService = createRootSkillService()

        List<SkillsService> allUsers = getRandomUserServices(3)
        SkillsService admin = allUsers[0]
        rootSkillsService.saveUserTag(admin.userName, 'dragons', ['DivineDragon'])
        SkillsService dragonUser = allUsers[1]
        SkillsService otherUser = allUsers[2]
        rootSkillsService.saveUserTag(dragonUser.userName, 'dragons', ['DivineDragon'])

        def survey1 = createSimpleSurvey(admin, 1)
        def quiz1 = createSimpleQuiz(admin, 2)

        def proj1Skills = createSimpleProject(admin, 1, true)
        def proj2Skills = createSimpleProject(admin, 2)
        def proj3Skills = createSimpleProject(admin, 3, true)
        def proj4Skills = createSimpleProject(admin, 4)

        associate(admin, proj1Skills[0], quiz1)
        associate(admin, proj1Skills[1], quiz1)
        associate(admin, proj2Skills[0], quiz1)
        associate(admin, proj2Skills[1], quiz1)
        associate(admin, proj3Skills[0], quiz1)
        associate(admin, proj3Skills[1], quiz1)
        associate(admin, proj4Skills[0], quiz1)
        associate(admin, proj4Skills[1], quiz1)

        associate(admin, proj1Skills[3], survey1)
        associate(admin, proj2Skills[3], survey1)
        associate(admin, proj3Skills[3], survey1)
        associate(admin, proj4Skills[3], survey1)

        runQuizOrSurvey(dragonUser, 1)
        runQuizOrSurvey(dragonUser, 2)

        runQuizOrSurvey(otherUser, 1)
        runQuizOrSurvey(otherUser, 2)

        when:
        def dragonRes = dragonUser.getCurrentUserQuizAttempts()
        def otherUserRes = otherUser.getCurrentUserQuizAttempts()
        then:
        dragonRes.count == 2
        dragonRes.totalCount == 2
        dragonRes.data.quizId == [survey1.quizId, quiz1.quizId]
        dragonRes.data[0].associatedSkills
        def record1Skills = dragonRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj3Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        dragonRes.data[1].associatedSkills
        def record2Skills = dragonRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj3Skills[0].skillId,
                proj3Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // other user
        otherUserRes.count == 2
        otherUserRes.totalCount == 2
        otherUserRes.data.quizId == [survey1.quizId, quiz1.quizId]
        otherUserRes.data[0].associatedSkills
        def otherUserRecord1Skills = otherUserRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord1Skills.projectId == [
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord1Skills.skillId == [
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        otherUserRes.data[1].associatedSkills
        def otherUserRecord2Skills = otherUserRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord2Skills.projectId == [
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord2Skills.skillId == [
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

    }

    def "get quiz attempts - invite-only project skills are not returned for unauthorized users"() {
        SkillsService rootSkillsService = createRootSkillService()

        List<SkillsService> allUsers = getRandomUserServices(4)
        SkillsService admin = allUsers[0]
        SkillsService invitedToAllProjects = allUsers[1]
        SkillsService invitedToOneProject = allUsers[2]
        SkillsService otherUser = allUsers[3]

        def survey1 = createSimpleSurvey(admin, 1)
        def quiz1 = createSimpleQuiz(admin, 2)

        def proj1Skills = createSimpleProject(admin, 1)
        admin.changeSetting(proj1Skills[0].projectId, Settings.INVITE_ONLY_PROJECT.settingName,
                [projectId: proj1Skills[0].projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: "true"])
        def proj2Skills = createSimpleProject(admin, 2)
        def proj3Skills = createSimpleProject(admin, 3)
        admin.changeSetting(proj3Skills[0].projectId, Settings.INVITE_ONLY_PROJECT.settingName,
                [projectId: proj3Skills[0].projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: "true"])
        def proj4Skills = createSimpleProject(admin, 4)

        String invite1Email = 'invite1@email.foo'
        String invite2Email = 'invite2@email.foo'
        String invite3Email = 'invite3@email.foo'
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [invite1Email]])
        admin.inviteUsersToProject(proj3Skills[0].projectId, [validityDuration: "PT5M", recipients: [invite2Email]])
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [invite3Email]])

        WaitFor.wait { greenMail.getReceivedMessages().length > 2
                && EmailUtils.getEmails(greenMail).find { it.recipients.contains(invite1Email) }
                && EmailUtils.getEmails(greenMail).find { it.recipients.contains(invite2Email) }
                && EmailUtils.getEmails(greenMail).find { it.recipients.contains(invite3Email) }
        }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)
        EmailUtils.EmailRes invite1EmailRes = emails.find { it.recipients.contains(invite1Email) }
        EmailUtils.EmailRes invite2EmailRes = emails.find { it.recipients.contains(invite2Email) }
        EmailUtils.EmailRes invite3EmailRes = emails.find { it.recipients.contains(invite3Email) }
        def invite1 = extractInviteFromEmail(invite1EmailRes.html)
        def invite2 = extractInviteFromEmail(invite2EmailRes.html)
        def invite3 = extractInviteFromEmail(invite3EmailRes.html)
        invitedToAllProjects.joinProject(proj1Skills[0].projectId, invite1)
        invitedToAllProjects.joinProject(proj3Skills[0].projectId, invite2)
        invitedToOneProject.joinProject(proj1Skills[0].projectId, invite3)

        associate(admin, proj1Skills[0], quiz1)
        associate(admin, proj1Skills[1], quiz1)
        associate(admin, proj2Skills[0], quiz1)
        associate(admin, proj2Skills[1], quiz1)
        associate(admin, proj3Skills[0], quiz1)
        associate(admin, proj3Skills[1], quiz1)
        associate(admin, proj4Skills[0], quiz1)
        associate(admin, proj4Skills[1], quiz1)

        associate(admin, proj1Skills[3], survey1)
        associate(admin, proj2Skills[3], survey1)
        associate(admin, proj3Skills[3], survey1)
        associate(admin, proj4Skills[3], survey1)

        runQuizOrSurvey(invitedToAllProjects, 1)
        runQuizOrSurvey(invitedToAllProjects, 2)

        runQuizOrSurvey(invitedToOneProject, 1)
        runQuizOrSurvey(invitedToOneProject, 2)

        runQuizOrSurvey(otherUser, 1)
        runQuizOrSurvey(otherUser, 2)

        when:
        def invitedToAllRes = invitedToAllProjects.getCurrentUserQuizAttempts()
        def invitedToOneProjectRes = invitedToOneProject.getCurrentUserQuizAttempts()
        def otherUserRes = otherUser.getCurrentUserQuizAttempts()
        then:
        invitedToAllRes.count == 2
        invitedToAllRes.totalCount == 2
        invitedToAllRes.data.quizId == [survey1.quizId, quiz1.quizId]
        invitedToAllRes.data[0].associatedSkills
        def record1Skills = invitedToAllRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj3Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        invitedToAllRes.data[1].associatedSkills
        def record2Skills = invitedToAllRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj3Skills[0].skillId,
                proj3Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // invitedToOneProject user
        invitedToOneProjectRes.count == 2
        invitedToOneProjectRes.totalCount == 2
        invitedToOneProjectRes.data.quizId == [survey1.quizId, quiz1.quizId]
        invitedToOneProjectRes.data[0].associatedSkills
        def invitedToOneProjRecord1Skills = invitedToOneProjectRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneProjRecord1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneProjRecord1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        invitedToAllRes.data[1].associatedSkills
        def invitedToOneProjRecord2Skills = invitedToOneProjectRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneProjRecord2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneProjRecord2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]


        // other user
        otherUserRes.count == 2
        otherUserRes.totalCount == 2
        otherUserRes.data.quizId == [survey1.quizId, quiz1.quizId]
        otherUserRes.data[0].associatedSkills
        def otherUserRecord1Skills = otherUserRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord1Skills.projectId == [
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord1Skills.skillId == [
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        otherUserRes.data[1].associatedSkills
        def otherUserRecord2Skills = otherUserRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord2Skills.projectId == [
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord2Skills.skillId == [
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

    }

    def "get quiz attempts - invite-only project skills and community protected skills are not returned for unauthorized users"() {
        SkillsService rootSkillsService = createRootSkillService()

        List<SkillsService> allUsers = getRandomUserServices(6)
        SkillsService admin = allUsers[0]
        rootSkillsService.saveUserTag(admin.userName, 'dragons', ['DivineDragon'])
        SkillsService invitedToAllProjectsAndDragon = allUsers[1]
        rootSkillsService.saveUserTag(invitedToAllProjectsAndDragon.userName, 'dragons', ['DivineDragon'])

        SkillsService invitedToOneProjectAndDragon = allUsers[2]
        rootSkillsService.saveUserTag(invitedToOneProjectAndDragon.userName, 'dragons', ['DivineDragon'])

        SkillsService dragonUser = allUsers[3]
        rootSkillsService.saveUserTag(dragonUser.userName, 'dragons', ['DivineDragon'])

        SkillsService invitedToOne = allUsers[4]

        SkillsService otherUser = allUsers[5]

        def survey1 = createSimpleSurvey(admin, 1)
        def quiz1 = createSimpleQuiz(admin, 2)

        def proj1Skills = createSimpleProject(admin, 1, true)
        admin.changeSetting(proj1Skills[0].projectId, Settings.INVITE_ONLY_PROJECT.settingName,
                [projectId: proj1Skills[0].projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: "true"])
        def proj2Skills = createSimpleProject(admin, 2, true)
        def proj3Skills = createSimpleProject(admin, 3)
        admin.changeSetting(proj3Skills[0].projectId, Settings.INVITE_ONLY_PROJECT.settingName,
                [projectId: proj3Skills[0].projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: "true"])
        def proj4Skills = createSimpleProject(admin, 4)

        List<String> inviteEmails = (1..5).collect { "invite${it}@email.foo".toString() }
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [inviteEmails[0]]])
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [inviteEmails[1]]])
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [inviteEmails[2]]])
        admin.inviteUsersToProject(proj3Skills[0].projectId, [validityDuration: "PT5M", recipients: [inviteEmails[3]]])
        admin.inviteUsersToProject(proj3Skills[0].projectId, [validityDuration: "PT5M", recipients: [inviteEmails[4]]])

        WaitFor.wait { greenMail.getReceivedMessages().length >= inviteEmails.size()
                && EmailUtils.getEmails(greenMail).collect { it.recipients }.flatten().containsAll(inviteEmails)
        }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)
        List invites = inviteEmails.collect {String inviteEmail ->
            EmailUtils.EmailRes invite1EmailRes = emails.find { it.recipients.contains(inviteEmail) }
                             return extractInviteFromEmail(invite1EmailRes.html)
        }
        invitedToAllProjectsAndDragon.joinProject(proj1Skills[0].projectId, invites[0])
        invitedToAllProjectsAndDragon.joinProject(proj3Skills[0].projectId, invites[3])
        invitedToOneProjectAndDragon.joinProject(proj1Skills[0].projectId, invites[1])
        invitedToOne.joinProject(proj3Skills[0].projectId, invites[4])

        associate(admin, proj1Skills[0], quiz1)
        associate(admin, proj1Skills[1], quiz1)
        associate(admin, proj2Skills[0], quiz1)
        associate(admin, proj2Skills[1], quiz1)
        associate(admin, proj3Skills[0], quiz1)
        associate(admin, proj3Skills[1], quiz1)
        associate(admin, proj4Skills[0], quiz1)
        associate(admin, proj4Skills[1], quiz1)

        associate(admin, proj1Skills[3], survey1)
        associate(admin, proj2Skills[3], survey1)
        associate(admin, proj3Skills[3], survey1)
        associate(admin, proj4Skills[3], survey1)

        runQuizOrSurvey(invitedToAllProjectsAndDragon, 1)
        runQuizOrSurvey(invitedToAllProjectsAndDragon, 2)

        runQuizOrSurvey(invitedToOneProjectAndDragon, 1)
        runQuizOrSurvey(invitedToOneProjectAndDragon, 2)

        runQuizOrSurvey(dragonUser, 1)
        runQuizOrSurvey(dragonUser, 2)

        runQuizOrSurvey(invitedToOne, 1)
        runQuizOrSurvey(invitedToOne, 2)

        runQuizOrSurvey(otherUser, 1)
        runQuizOrSurvey(otherUser, 2)

        when:
        def invitedToAllAndDragonRes = invitedToAllProjectsAndDragon.getCurrentUserQuizAttempts()
        def invitedToOneProjectAndDragonRes = invitedToOneProjectAndDragon.getCurrentUserQuizAttempts()
        def dragonUserRes = dragonUser.getCurrentUserQuizAttempts()
        def invitedToOneRes = invitedToOne.getCurrentUserQuizAttempts()
        def otherUserRes = otherUser.getCurrentUserQuizAttempts()
        then:
        invitedToAllAndDragonRes.count == 2
        invitedToAllAndDragonRes.totalCount == 2
        invitedToAllAndDragonRes.data.quizId == [survey1.quizId, quiz1.quizId]
        invitedToAllAndDragonRes.data[0].associatedSkills
        def record1Skills = invitedToAllAndDragonRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj3Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        invitedToAllAndDragonRes.data[1].associatedSkills
        def record2Skills = invitedToAllAndDragonRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj3Skills[0].skillId,
                proj3Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // invitedToOneProjectAndDragonRes user
        invitedToOneProjectAndDragonRes.count == 2
        invitedToOneProjectAndDragonRes.totalCount == 2
        invitedToOneProjectAndDragonRes.data.quizId == [survey1.quizId, quiz1.quizId]
        invitedToOneProjectAndDragonRes.data[0].associatedSkills
        def invitedToOneProjRecord1Skills = invitedToOneProjectAndDragonRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneProjRecord1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneProjRecord1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        invitedToAllAndDragonRes.data[1].associatedSkills
        def invitedToOneProjRecord2Skills = invitedToOneProjectAndDragonRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneProjRecord2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneProjRecord2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // dragon user
        dragonUserRes.count == 2
        dragonUserRes.totalCount == 2
        dragonUserRes.data.quizId == [survey1.quizId, quiz1.quizId]
        dragonUserRes.data[0].associatedSkills
        def dragonUserResRecord1Skills = dragonUserRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        dragonUserResRecord1Skills.projectId == [
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        dragonUserResRecord1Skills.skillId == [
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        dragonUserRes.data[1].associatedSkills
        def dragonUserResRecord2Skills = dragonUserRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        dragonUserResRecord2Skills.projectId == [
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        dragonUserResRecord2Skills.skillId == [
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // invitedToOneRes user
        invitedToOneRes.count == 2
        invitedToOneRes.totalCount == 2
        invitedToOneRes.data.quizId == [survey1.quizId, quiz1.quizId]
        invitedToOneRes.data[0].associatedSkills
        def invitedToOneResRecord1Skills = invitedToOneRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneResRecord1Skills.projectId == [
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneResRecord1Skills.skillId == [
                proj3Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        invitedToOneRes.data[1].associatedSkills
        def invitedToOneResRecord2Skills = invitedToOneRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneResRecord2Skills.projectId == [
                proj3Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneResRecord2Skills.skillId == [
                proj3Skills[0].skillId,
                proj3Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // other user
        otherUserRes.count == 2
        otherUserRes.totalCount == 2
        otherUserRes.data.quizId == [survey1.quizId, quiz1.quizId]
        otherUserRes.data[0].associatedSkills
        def otherUserRecord1Skills = otherUserRes.data[0].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord1Skills.projectId == [
                proj4Skills[0].projectId,
        ]
        otherUserRecord1Skills.skillId == [
                proj4Skills[3].skillId,
        ]

        otherUserRes.data[1].associatedSkills
        def otherUserRecord2Skills = otherUserRes.data[1].associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord2Skills.projectId == [
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord2Skills.skillId == [
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

    }

    def "get current user single attempt - multiple associated skills"() {
        List<SkillsService> allUsers = getRandomUserServices(5)
        List<SkillsService> admins = allUsers[0..1]
        List<SkillsService> users = allUsers[2..4]

        def survey1 = createSimpleSurvey(admins[0], 1)
        def quiz1 = createSimpleQuiz(admins[0], 2)

        def proj1Skills = createSimpleProject(admins[0], 1)
        def proj2Skills = createSimpleProject(admins[0], 2)
        def proj3Skills = createSimpleProject(admins[0], 3)

        associate(admins[0], proj1Skills[0], quiz1)
        associate(admins[0], proj1Skills[6], quiz1)
        associate(admins[0], proj2Skills[3], quiz1)

        associate(admins[0], proj3Skills[2], survey1)
        associate(admins[0], proj3Skills[3], survey1)
        associate(admins[0], proj3Skills[9], survey1)
        associate(admins[0], proj2Skills[0], survey1)

        def user1Attempt1Id = runQuizOrSurvey(users[0], 1)
        def user1Attempt2Id = runQuizOrSurvey(users[0], 2)
        def user1Attempt3Id = runQuizOrSurvey(users[0], 2, 1 )

        def user2Attempt1Id = runQuizOrSurvey(users[1], 2, 1)

        when:
        def user1Attempt1Res = users[0].getCurrentUserSingleQuizAttempt(user1Attempt1Id)
        def user1Attempt2Res = users[0].getCurrentUserSingleQuizAttempt(user1Attempt2Id)
        def user1Attempt3Res = users[0].getCurrentUserSingleQuizAttempt(user1Attempt3Id)
        def user2Attempt1Res = users[1].getCurrentUserSingleQuizAttempt(user2Attempt1Id)
        then:
        user1Attempt1Res.associatedSkills
        def user1Atttemp1Skills = user1Attempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        user1Atttemp1Skills.projectId == [proj2Skills[0].projectId, proj3Skills[0].projectId, proj3Skills[0].projectId, proj3Skills[0].projectId]
        user1Atttemp1Skills.subjectId == [getSubjectId(1), getSubjectId(1), getSubjectId(1), getSubjectId(2)]
        user1Atttemp1Skills.projectName == [getDefaultProjName(2), getDefaultProjName(3), getDefaultProjName(3), getDefaultProjName(3)]
        user1Atttemp1Skills.skillId == [proj2Skills[0].skillId, proj3Skills[2].skillId, proj3Skills[3].skillId, proj3Skills[9].skillId]
        user1Atttemp1Skills.skillName == [proj2Skills[0].name, proj3Skills[2].name, proj3Skills[3].name, proj3Skills[9].name]

        user1Attempt2Res.associatedSkills
        def user1Attempt2Skills = user1Attempt2Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        user1Attempt2Skills.projectId == [proj1Skills[0].projectId, proj1Skills[0].projectId, proj2Skills[0].projectId]
        user1Attempt2Skills.projectName == [getDefaultProjName(1), getDefaultProjName(1), getDefaultProjName(2)]
        user1Attempt2Skills.subjectId == [getSubjectId(1), getSubjectId(2), getSubjectId(1)]
        user1Attempt2Skills.skillId == [proj1Skills[0].skillId, proj1Skills[6].skillId, proj1Skills[3].skillId]
        user1Attempt2Skills.skillName == [proj1Skills[0].name, proj1Skills[6].name, proj1Skills[3].name]

        def user1Attempt3Skills = user1Attempt3Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        user1Attempt3Skills.projectId == [proj1Skills[0].projectId, proj1Skills[0].projectId, proj2Skills[0].projectId]
        user1Attempt3Skills.projectName == [getDefaultProjName(1), getDefaultProjName(1), getDefaultProjName(2)]
        user1Attempt3Skills.subjectId == [getSubjectId(1), getSubjectId(2), getSubjectId(1)]
        user1Attempt3Skills.skillId == [proj1Skills[0].skillId, proj1Skills[6].skillId, proj1Skills[3].skillId]
        user1Attempt3Skills.skillName == [proj1Skills[0].name, proj1Skills[6].name, proj1Skills[3].name]

        // user 1
        user2Attempt1Res.associatedSkills
        def user2Attemp1Skills = user2Attempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        user2Attemp1Skills.projectId == [proj1Skills[0].projectId, proj1Skills[0].projectId, proj2Skills[0].projectId]
        user2Attemp1Skills.subjectId == [getSubjectId(1), getSubjectId(2), getSubjectId(1)]
        user2Attemp1Skills.projectName == [getDefaultProjName(1), getDefaultProjName(1), getDefaultProjName(2)]
        user2Attemp1Skills.skillId == [proj1Skills[0].skillId, proj1Skills[6].skillId, proj2Skills[3].skillId]
        user2Attemp1Skills.skillName == [proj1Skills[0].name, proj1Skills[6].name, proj2Skills[3].name]
    }

    def "get current user single attempt - community protected skills are not returned for non-community users"() {
        SkillsService rootSkillsService = createRootSkillService()

        List<SkillsService> allUsers = getRandomUserServices(3)
        SkillsService admin = allUsers[0]
        rootSkillsService.saveUserTag(admin.userName, 'dragons', ['DivineDragon'])
        SkillsService dragonUser = allUsers[1]
        SkillsService otherUser = allUsers[2]
        rootSkillsService.saveUserTag(dragonUser.userName, 'dragons', ['DivineDragon'])

        def survey1 = createSimpleSurvey(admin, 1)
        def quiz1 = createSimpleQuiz(admin, 2)

        def proj1Skills = createSimpleProject(admin, 1, true)
        def proj2Skills = createSimpleProject(admin, 2)
        def proj3Skills = createSimpleProject(admin, 3, true)
        def proj4Skills = createSimpleProject(admin, 4)

        associate(admin, proj1Skills[0], quiz1)
        associate(admin, proj1Skills[1], quiz1)
        associate(admin, proj2Skills[0], quiz1)
        associate(admin, proj2Skills[1], quiz1)
        associate(admin, proj3Skills[0], quiz1)
        associate(admin, proj3Skills[1], quiz1)
        associate(admin, proj4Skills[0], quiz1)
        associate(admin, proj4Skills[1], quiz1)

        associate(admin, proj1Skills[3], survey1)
        associate(admin, proj2Skills[3], survey1)
        associate(admin, proj3Skills[3], survey1)
        associate(admin, proj4Skills[3], survey1)

        def dragonUserAttempt1Id = runQuizOrSurvey(dragonUser, 1)
        def dragonUserAttempt2Id = runQuizOrSurvey(dragonUser, 2)

        def otherUserAttempt1Id = runQuizOrSurvey(otherUser, 1)
        def otherUserAttempt2Id = runQuizOrSurvey(otherUser, 2)

        when:
        def dragonUserAttempt1Res = dragonUser.getCurrentUserSingleQuizAttempt(dragonUserAttempt1Id)
        def dragonUserAttempt2Res = dragonUser.getCurrentUserSingleQuizAttempt(dragonUserAttempt2Id)
        def otherUserAttempt1Res = otherUser.getCurrentUserSingleQuizAttempt(otherUserAttempt1Id)
        def otherUserAttempt2Res = otherUser.getCurrentUserSingleQuizAttempt(otherUserAttempt2Id)

        then:
        def record1Skills = dragonUserAttempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj3Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        dragonUserAttempt2Res.associatedSkills
        def record2Skills = dragonUserAttempt2Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj3Skills[0].skillId,
                proj3Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // other user
        def otherUserRecord1Skills = otherUserAttempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord1Skills.projectId == [
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord1Skills.skillId == [
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        def otherUserRecord2Skills = otherUserAttempt2Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord2Skills.projectId == [
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord2Skills.skillId == [
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

    }

    def "get current user single attempt - invite-only project skills are not returned for unauthorized users"() {
        SkillsService rootSkillsService = createRootSkillService()

        List<SkillsService> allUsers = getRandomUserServices(4)
        SkillsService admin = allUsers[0]
        SkillsService invitedToAllProjects = allUsers[1]
        SkillsService invitedToOneProject = allUsers[2]
        SkillsService otherUser = allUsers[3]

        def survey1 = createSimpleSurvey(admin, 1)
        def quiz1 = createSimpleQuiz(admin, 2)

        def proj1Skills = createSimpleProject(admin, 1)
        admin.changeSetting(proj1Skills[0].projectId, Settings.INVITE_ONLY_PROJECT.settingName,
                [projectId: proj1Skills[0].projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: "true"])
        def proj2Skills = createSimpleProject(admin, 2)
        def proj3Skills = createSimpleProject(admin, 3)
        admin.changeSetting(proj3Skills[0].projectId, Settings.INVITE_ONLY_PROJECT.settingName,
                [projectId: proj3Skills[0].projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: "true"])
        def proj4Skills = createSimpleProject(admin, 4)

        String invite1Email = 'invite1@email.foo'
        String invite2Email = 'invite2@email.foo'
        String invite3Email = 'invite3@email.foo'
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [invite1Email]])
        admin.inviteUsersToProject(proj3Skills[0].projectId, [validityDuration: "PT5M", recipients: [invite2Email]])
        admin.inviteUsersToProject(proj1Skills[0].projectId, [validityDuration: "PT5M", recipients: [invite3Email]])

        WaitFor.wait { greenMail.getReceivedMessages().length > 2
                && EmailUtils.getEmails(greenMail).find { it.recipients.contains(invite1Email) }
                && EmailUtils.getEmails(greenMail).find { it.recipients.contains(invite2Email) }
                && EmailUtils.getEmails(greenMail).find { it.recipients.contains(invite3Email) }
        }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)
        EmailUtils.EmailRes invite1EmailRes = emails.find { it.recipients.contains(invite1Email) }
        EmailUtils.EmailRes invite2EmailRes = emails.find { it.recipients.contains(invite2Email) }
        EmailUtils.EmailRes invite3EmailRes = emails.find { it.recipients.contains(invite3Email) }
        def invite1 = extractInviteFromEmail(invite1EmailRes.html)
        def invite2 = extractInviteFromEmail(invite2EmailRes.html)
        def invite3 = extractInviteFromEmail(invite3EmailRes.html)
        invitedToAllProjects.joinProject(proj1Skills[0].projectId, invite1)
        invitedToAllProjects.joinProject(proj3Skills[0].projectId, invite2)
        invitedToOneProject.joinProject(proj1Skills[0].projectId, invite3)

        associate(admin, proj1Skills[0], quiz1)
        associate(admin, proj1Skills[1], quiz1)
        associate(admin, proj2Skills[0], quiz1)
        associate(admin, proj2Skills[1], quiz1)
        associate(admin, proj3Skills[0], quiz1)
        associate(admin, proj3Skills[1], quiz1)
        associate(admin, proj4Skills[0], quiz1)
        associate(admin, proj4Skills[1], quiz1)

        associate(admin, proj1Skills[3], survey1)
        associate(admin, proj2Skills[3], survey1)
        associate(admin, proj3Skills[3], survey1)
        associate(admin, proj4Skills[3], survey1)

        def invitedToAllProjectsAttempt1Id = runQuizOrSurvey(invitedToAllProjects, 1)
        def invitedToAllProjectsAttempt2Id = runQuizOrSurvey(invitedToAllProjects, 2)

        def invitedToOneProjectAttempt1Id = runQuizOrSurvey(invitedToOneProject, 1)
        def invitedToOneProjectAttempt2Id = runQuizOrSurvey(invitedToOneProject, 2)

        def otherUserAttempt1Id = runQuizOrSurvey(otherUser, 1)
        def otherUserAttempt2Id = runQuizOrSurvey(otherUser, 2)

        when:
        def invitedToAllProjectsAttempt1Res = invitedToAllProjects.getCurrentUserSingleQuizAttempt(invitedToAllProjectsAttempt1Id)
        def invitedToAllProjectsAttempt2Res = invitedToAllProjects.getCurrentUserSingleQuizAttempt(invitedToAllProjectsAttempt2Id)
        def invitedToOneProjectAttempt1Res = invitedToOneProject.getCurrentUserSingleQuizAttempt(invitedToOneProjectAttempt1Id)
        def invitedToOneProjectAttempt2Res = invitedToOneProject.getCurrentUserSingleQuizAttempt(invitedToOneProjectAttempt2Id)
        def otherUserAttempt1Res = otherUser.getCurrentUserSingleQuizAttempt(otherUserAttempt1Id)
        def otherUserAttempt2Res = otherUser.getCurrentUserSingleQuizAttempt(otherUserAttempt2Id)

        def invitedToAllRes = invitedToAllProjects.getCurrentUserQuizAttempts()
        def invitedToOneProjectRes = invitedToOneProject.getCurrentUserQuizAttempts()
        def otherUserRes = otherUser.getCurrentUserQuizAttempts()
        then:
        def record1Skills = invitedToAllProjectsAttempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj3Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        def record2Skills = invitedToAllProjectsAttempt2Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        record2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj3Skills[0].projectId,
                proj3Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        record2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj3Skills[0].skillId,
                proj3Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

        // invitedToOneProject user
        def invitedToOneProjRecord1Skills = invitedToOneProjectAttempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneProjRecord1Skills.projectId == [
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneProjRecord1Skills.skillId == [
                proj1Skills[3].skillId,
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        def invitedToOneProjRecord2Skills = invitedToOneProjectAttempt2Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        invitedToOneProjRecord2Skills.projectId == [
                proj1Skills[0].projectId,
                proj1Skills[0].projectId,
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        invitedToOneProjRecord2Skills.skillId == [
                proj1Skills[0].skillId,
                proj1Skills[1].skillId,
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]


        // other user
        def otherUserRecord1Skills = otherUserAttempt1Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord1Skills.projectId == [
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord1Skills.skillId == [
                proj2Skills[3].skillId,
                proj4Skills[3].skillId,
        ]

        def otherUserRecord2Skills = otherUserAttempt2Res.associatedSkills.sort { "${it.projectId}-${it.skillId}" }
        otherUserRecord2Skills.projectId == [
                proj2Skills[0].projectId,
                proj2Skills[0].projectId,
                proj4Skills[0].projectId,
                proj4Skills[0].projectId,
        ]
        otherUserRecord2Skills.skillId == [
                proj2Skills[0].skillId,
                proj2Skills[1].skillId,
                proj4Skills[0].skillId,
                proj4Skills[1].skillId,
        ]

    }
}


