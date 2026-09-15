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
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class CommunityMarkdownWithAttachmentsValidationSpecs extends CopyIntSpec {

    def "not allowed to copy from UC-proj to non-UC project, global badge or quiz"() {
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
        skillsService.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        when:
        def projRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", p2.projectId).body
        def quizRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", null, null, quiz.quizId).body
        def globalBadgeRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", null, null, null, badge.badgeId).body
        then:
        !projRes.valid
        projRes.msg == 'Attachment [Link] is not allowed to be copied'

        !quizRes.valid
        quizRes.msg == 'Attachment [Link] is not allowed to be copied'

        !globalBadgeRes.valid
        globalBadgeRes.msg == 'Attachment [Link] is not allowed to be copied'
    }

    def "not allowed to copy from UC-quiz to non-UC project, quiz or global badge"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quizUC = QuizDefFactory.createQuiz(11)
        quizUC.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quizUC)

        def attachment1Href = attachFileForQuizAndReturnHref(quizUC.quizId, "some text", pristineDragonsUser)

        def p2 = createProject(2)
        def p2Subj1 = createSubject(2, 1)
        def p2Skill2 = createSkill(2, 1, 2, 0, 1000)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        when:
        def projRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", p2.projectId).body
        def quizRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", null, null, quiz.quizId).body
        def globalBadgeRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", null, null, null, badge.badgeId).body
        then:
        !projRes.valid
        projRes.msg == 'Attachment [Link] is not allowed to be copied'

        !quizRes.valid
        quizRes.msg == 'Attachment [Link] is not allowed to be copied'

        !globalBadgeRes.valid
        globalBadgeRes.msg == 'Attachment [Link] is not allowed to be copied'
    }

    def "not allowed to copy from UC-GlobalBadge to non-UC project, quiz or global badge"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badgeUC = createBadge(1, 15)
        badgeUC.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badgeUC)

        def attachment1Href = attachFileForGlobalBadgeAndReturnHref(badgeUC.badgeId, 'some value', pristineDragonsUser)

        def p2 = createProject(2)
        def p2Subj1 = createSubject(2, 1)
        def p2Skill2 = createSkill(2, 1, 2, 0, 1000)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(p2, p2Subj1, [p2Skill2])

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def badge = createBadge(1, 1)
        skillsService.createGlobalBadge(badge)

        when:
        def projRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", p2.projectId).body
        def quizRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", null, null, quiz.quizId).body
        def globalBadgeRes = skillsService.checkCustomDescriptionValidation("Here is a [Link](${attachment1Href})", null, null, null, badge.badgeId).body
        then:
        !projRes.valid
        projRes.msg == 'Attachment [Link] is not allowed to be copied'

        !quizRes.valid
        quizRes.msg == 'Attachment [Link] is not allowed to be copied'

        !globalBadgeRes.valid
        globalBadgeRes.msg == 'Attachment [Link] is not allowed to be copied'
    }

}

