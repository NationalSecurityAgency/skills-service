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
package skills.intTests.community

import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.Attachment
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class CommunityMarkdownWithAttachmentsCopySpecs extends CopyIntSpec {

    def "not allowed to copy from UC-proj to non-UC-proj"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId,  'Test is a test', pristineDragonsUser)

        def p2 = createProject(2)
        def p2Subj1 = createSubject(2, 1)
        def p2Skill2 = createSkill(2, 1, 2, 0, 1000)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        def p2Subj2 = createSubject(2, 2)
        def p2Skill = createSkill(2, 1, 1, 0, 1000)
        def p2SkillGroup = createSkillsGroup(2, 1, 11)
        def p2Badge = createBadge(2, 1)
        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        p2Subj2.description = "Here is a [Link](${attachment1Href})".toString()
        p2Skill.description = "Here is a [Link](${attachment1Href})".toString()
        p2SkillGroup.description = "Here is a [Link](${attachment1Href})".toString()
        p2Badge.description = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.updateProject(p2, p2.projectId) })
        expectAccessDenied({ pristineDragonsUser.createSubject(p2Subj2) })
        expectAccessDenied({ pristineDragonsUser.createSkill(p2Skill) })
        expectAccessDenied({ pristineDragonsUser.createSkill(p2SkillGroup) })
        expectAccessDenied { pristineDragonsUser.addSkill(p2Skill2, pristineDragonsUser.userName, new Date(), "Here is a [Link](${attachment1Href})".toString()) }
        expectAccessDenied({ pristineDragonsUser.createBadge(p2Badge) })
    }

    def "not allowed to copy from UC-quiz to non-UC-proj"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId, "some text", pristineDragonsUser)

        def p2 = createProject(2)
        def p2Subj1 = createSubject(2, 1)
        def p2Skill2 = createSkill(2, 1, 2, 0, 1000)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        def p2Subj2 = createSubject(2, 2)
        def p2Skill = createSkill(2, 1, 1, 0, 1000)
        def p2SkillGroup = createSkillsGroup(2, 1, 11)
        def p2Badge = createBadge(2, 1)
        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        p2Subj2.description = "Here is a [Link](${attachment1Href})".toString()
        p2Skill.description = "Here is a [Link](${attachment1Href})".toString()
        p2SkillGroup.description = "Here is a [Link](${attachment1Href})".toString()
        p2Badge.description = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.updateProject(p2, p2.projectId) })
        expectAccessDenied({ pristineDragonsUser.createSubject(p2Subj2) })
        expectAccessDenied({ pristineDragonsUser.createSkill(p2Skill) })
        expectAccessDenied({ pristineDragonsUser.createSkill(p2SkillGroup) })
        expectAccessDenied { pristineDragonsUser.addSkill(p2Skill2, pristineDragonsUser.userName, new Date(), "Here is a [Link](${attachment1Href})".toString()) }
        expectAccessDenied({ pristineDragonsUser.createBadge(p2Badge) })
    }

    def "not UC user is not allowed to attempt a copy"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz)

        def attachment1Href = attachFileForQuizAndReturnHref(quiz.quizId, "some text", pristineDragonsUser)

        def p2 = createProject(2)
        def p2Subj1 = createSubject(2, 1)
        def p2Skill2 = createSkill(2, 1, 2, 0, 1000)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        skillsService.updateProject(p2, p2.projectId)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("errorCode:AccessDenied")
        exception.message.contains("Not authorized to copy the attachment")
    }

    def "not allowed to copy from UC-GlobalBadge to non-UC-proj"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge = createBadge(1, 1)
        badge.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId, 'some value', pristineDragonsUser)

        def p2 = createProject(2)
        def p2Subj1 = createSubject(2, 1)
        def p2Skill2 = createSkill(2, 1, 2, 0, 1000)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        def p2Subj2 = createSubject(2, 2)
        def p2Skill = createSkill(2, 1, 1, 0, 1000)
        def p2SkillGroup = createSkillsGroup(2, 1, 11)
        def p2Badge = createBadge(2, 22)
        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        p2Subj2.description = "Here is a [Link](${attachment1Href})".toString()
        p2Skill.description = "Here is a [Link](${attachment1Href})".toString()
        p2SkillGroup.description = "Here is a [Link](${attachment1Href})".toString()
        p2Badge.description = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.updateProject(p2, p2.projectId) })
        expectAccessDenied({ pristineDragonsUser.createSubject(p2Subj2) })
        expectAccessDenied({ pristineDragonsUser.createSkill(p2Skill) })
        expectAccessDenied({ pristineDragonsUser.createSkill(p2SkillGroup) })
        expectAccessDenied { pristineDragonsUser.addSkill(p2Skill2, pristineDragonsUser.userName, new Date(), "Here is a [Link](${attachment1Href})".toString()) }
        expectAccessDenied({ pristineDragonsUser.createBadge(p2Badge) })
    }

    def "not allowed to copy from UC-proj to non-UC-quiz"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId,  'Test is a test', pristineDragonsUser)

        def quiz = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(quiz)
        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        pristineDragonsUser.createQuizQuestionDef(question1)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        def quizAttempt = pristineDragonsUser.startQuizAttempt(quiz.quizId).body

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        question2.question = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.createQuizDef(quiz, quiz.quizId) })
        expectAccessDenied({ pristineDragonsUser.createQuizQuestionDef(question2) })
        expectAccessDenied({ pristineDragonsUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()]) })
    }

    def "not allowed to copy from UC-quiz to non-UC-quiz"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quizUC = QuizDefFactory.createQuiz(2)
        quizUC.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quizUC)

        def attachment1Href = attachFileForQuizAndReturnHref(quizUC.quizId, "some text", pristineDragonsUser)

        def quiz = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(quiz)
        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        pristineDragonsUser.createQuizQuestionDef(question1)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        def quizAttempt = pristineDragonsUser.startQuizAttempt(quiz.quizId).body

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        question2.question = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.createQuizDef(quiz, quiz.quizId) })
        expectAccessDenied({ pristineDragonsUser.createQuizQuestionDef(question2) })
        expectAccessDenied({ pristineDragonsUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()]) })
    }

    def "not allowed to copy from UC-GlobalBadge to non-UC-quiz"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge = createBadge(1, 1)
        badge.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badge)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badge.badgeId, 'some value', pristineDragonsUser)

        def quiz = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(quiz)
        def question1 = QuizDefFactory.createTextInputQuestion(1, 1)
        pristineDragonsUser.createQuizQuestionDef(question1)

        def question2 = QuizDefFactory.createTextInputQuestion(1, 2)

        def quizAttempt = pristineDragonsUser.startQuizAttempt(quiz.quizId).body

        when:
        quiz.description = "Here is a [Link](${attachment1Href})".toString()
        question2.question = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.createQuizDef(quiz, quiz.quizId) })
        expectAccessDenied({ pristineDragonsUser.createQuizQuestionDef(question2) })
        expectAccessDenied({ pristineDragonsUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText:  "Here is a [Link](${attachment1Href})".toString()]) })
    }

    def "not allowed to copy from UC-proj to non-UC-GlobalBadge"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId,  'Test is a test', pristineDragonsUser)

        def badge = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.updateGlobalBadge(badge, badge.badgeId) })
    }

    def "not allowed to copy from UC-quiz to non-UC-GlobalBadge"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quizUC = QuizDefFactory.createQuiz(2)
        quizUC.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quizUC)

        def attachment1Href = attachFileForQuizAndReturnHref(quizUC.quizId, "some text", pristineDragonsUser)
        def badge = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.updateGlobalBadge(badge, badge.badgeId) })
    }

    def "not allowed to copy from UC-GlobalBadge to non-UC-GlobalBadge"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badgeUC = createBadge(1, 11)
        badgeUC.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badgeUC)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badgeUC.badgeId, 'some value', pristineDragonsUser)

        def badge = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge)

        when:
        badge.description = "Here is a [Link](${attachment1Href})".toString()

        then:
        expectAccessDenied({ pristineDragonsUser.updateGlobalBadge(badge, badge.badgeId) })
    }

    private boolean expectAccessDenied(Closure endpointCall) {
        try {
            endpointCall.call()
            assert false, "Expected SkillsClientException"
            return false
        } catch (SkillsClientException exception) {
            assert (exception.message.contains("errorCode:AccessDenied") && exception.message.contains("Not allowed to copy attachments to non-UC"))
                    || (
                    (exception.message.contains("errorCode:BadParam") || exception.message.contains("errorCode:ParagraphValidationFailed"))
                            && exception.message.contains("Attachment [Link] is not allowed to be copied")
            )
        }

        return true
    }

    def "allowed to copy from UC to UC project"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def attachment1Href = attachFileAndReturnHref(p1.projectId, 'some value', pristineDragonsUser)
        def attachment2Href = attachFileAndReturnHref(p1.projectId, 'some value', pristineDragonsUser)

        def p1Skills = createSkills(2, 1, 1, 100)
        p1Skills[0].description = "Here is a [Link](${attachment1Href})".toString()
        p1Skills[1].description = "Here is a [Link](${attachment2Href})".toString()
        pristineDragonsUser.createSkills(p1Skills)

        def p2 = createProject(2)
        p2.enableProtectedUserCommunity = true
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, null, null)

        when:
        p2.description = "Here is a [Link](${attachment1Href})".toString()
        pristineDragonsUser.updateProject(p2, p2.projectId)

        def origProjSkill1 = pristineDragonsUser.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def origProjSkill2 = pristineDragonsUser.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])

        def copyProj = pristineDragonsUser.getProjectDescription(p2.projectId)

        List<Attachment> attachments = attachmentRepo.findAll()

        // should not create new attachments
        p2.description = copyProj.description
        pristineDragonsUser.updateProject(p2, p2.projectId)
        pristineDragonsUser.updateProject(p2, p2.projectId)
        List<Attachment> attachments1 = attachmentRepo.findAll()
        then:
        origProjSkill1.description == "Here is a [Link](${attachment1Href})"
        origProjSkill2.description == "Here is a [Link](${attachment2Href})"

        attachments.size() == 3
        Attachment originalAttachment1 = attachments.find {  attachment1Href.contains(it.uuid)}
        Attachment originalAttachment2 = attachments.find {  attachment2Href.contains(it.uuid)}
        originalAttachment1.projectId == p1.projectId
        originalAttachment2.projectId == p1.projectId

        List<Attachment> newAttachments = attachments.findAll {
            !attachment1Href.contains(it.uuid) && !attachment2Href.contains(it.uuid)
        }

        assert newAttachments.size() == 1
        copyProj.description == "Here is a [Link](/api/download/${newAttachments[0].uuid})".toString()
        newAttachments[0].projectId == p2.projectId

        attachments1.uuid.sort() == attachments.uuid.sort()
    }
}