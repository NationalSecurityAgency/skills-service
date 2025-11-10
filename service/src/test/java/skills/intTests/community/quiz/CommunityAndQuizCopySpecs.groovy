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
package skills.intTests.community.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.QuizSetting

class CommunityAndQuizCopySpecs extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    def "non-UC user cannot copy non-uc quiz to uc"() {
        def q1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(q1)

        when:
        def copy = [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: q1.type, enableProtectedUserCommunity: true]
        skillsService.copyQuiz(q1.quizId, copy)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${skillsService.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "copying quiz should copy the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: q1.type, enableProtectedUserCommunity: true]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        def copiedQuizDef = pristineDragonsUser.getQuizDefSummary(copy.quizId)
        then:
        copiedQuizDef.quizId == copy.quizId
        copiedQuizDef.userCommunity == 'Divine Dragon'
    }

    def "cannot disable community when copying a quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: q1.type, enableProtectedUserCommunity: false]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        def copiedQuizDef = pristineDragonsUser.getQuizDefSummary(copy.quizId)
        then:
        copiedQuizDef.quizId == copy.quizId
        copiedQuizDef.userCommunity == 'Divine Dragon'
    }

    def "enable protected community during copy"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = false
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        q2.enableProtectedUserCommunity = false
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: q1.type, enableProtectedUserCommunity: true]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        def copiedQuizDef = pristineDragonsUser.getQuizDefSummary(copy.quizId)
        def originalQuizDef = pristineDragonsUser.getQuizDefSummary(q1.quizId)
        then:
        copiedQuizDef.quizId == copy.quizId
        copiedQuizDef.userCommunity == 'Divine Dragon'

        originalQuizDef.quizId == q1.quizId
        originalQuizDef.userCommunity == 'All Dragons'

        List<QuizSetting> ucSettings = quizSettingsRepo.findAll().findAll({ it.setting == QuizSettings.UserCommunityOnlyQuiz.setting})
        ucSettings.size() == 1
        ucSettings[0].value == "true"
        ucSettings[0].quizRefId == quizDefRepo.findByQuizIdIgnoreCase(copy.quizId).id
    }

    def "enable protected community during copy and description has jabberwocky that's not allowed in non-uc quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = "divinedragon is not allowed in uc projects"
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    description: "This is a jabberwocky description that is not allowed in non-uc projects",
                    type: q1.type,
                    enableProtectedUserCommunity: true]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        def copiedQuizDef = pristineDragonsUser.getQuizDef(copy.quizId)
        def originalQuizDef = pristineDragonsUser.getQuizDef(q1.quizId)
        then:
        copiedQuizDef.quizId == copy.quizId
        copiedQuizDef.userCommunity == 'Divine Dragon'
        copiedQuizDef.description == "This is a jabberwocky description that is not allowed in non-uc projects"

        originalQuizDef.quizId == q1.quizId
        originalQuizDef.userCommunity == 'All Dragons'
        originalQuizDef.description == "divinedragon is not allowed in uc projects"
    }

    def "enable protected community during copy and description has divinedragon that's not allowed in uc projects"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = "divinedragon is not allowed in uc projects"
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    description: "divinedragon is not allowed in uc projects",
                    type: q1.type,
                    enableProtectedUserCommunity: true]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "keeping community disabled during copy and description has divinedragon that's not allowed in uc projects"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = "divinedragon is not allowed in uc projects"
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    description: "divinedragon is not allowed in uc projects",
                    type: q1.type]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        def copiedQuizDef = pristineDragonsUser.getQuizDef(copy.quizId)
        def originalQuizDef = pristineDragonsUser.getQuizDef(q1.quizId)
        then:
        copiedQuizDef.quizId == copy.quizId
        copiedQuizDef.userCommunity == 'All Dragons'
        copiedQuizDef.description == "divinedragon is not allowed in uc projects"

        originalQuizDef.quizId == q1.quizId
        originalQuizDef.userCommunity == 'All Dragons'
        originalQuizDef.description == "divinedragon is not allowed in uc projects"
    }

    def "copy non-protected community quiz without description"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)
        def q2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        pristineDragonsUser.createQuizQuestionDefs(q2Questions)

        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    type: q1.type]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        def copiedQuizDef = pristineDragonsUser.getQuizDef(copy.quizId)
        def originalQuizDef = pristineDragonsUser.getQuizDef(q1.quizId)
        then:
        copiedQuizDef.quizId == copy.quizId
        copiedQuizDef.userCommunity == 'All Dragons'
        !copiedQuizDef.description

        originalQuizDef.quizId == q1.quizId
        originalQuizDef.userCommunity == 'All Dragons'
        !originalQuizDef.description
    }

    def "clearly indicate which question is failing to copy due to paragraph validation"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        questions[1].question = "jabberwocky"
        pristineDragonsUser.createQuizQuestionDefs(questions)

        jdbcTemplate.execute("DELETE FROM quiz_settings qs USING quiz_definition qd WHERE qs.quiz_ref_id = qd.id " +
                " AND qd.quiz_id = '${q1.quizId}' AND qs.setting = 'user_community'")

        def origQuestionDefs = pristineDragonsUser.getQuizQuestionDefs(q1.quizId)
        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    type: q1.type]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("Validation failed for questionId")
        exception.message.contains("errorCode:ParagraphValidationFailed")
        exception.message.contains("questionId:${origQuestionDefs.questions[1].id}")
    }

    def "clearly indicate which question's video transcript is failing to copy due to paragraph validation"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def origQuestionDefs = pristineDragonsUser.getQuizQuestionDefs(q1.quizId)
        pristineDragonsUser.saveSkillVideoAttributes(q1.quizId, origQuestionDefs.questions[1].id?.toString(), [
                videoUrl: "http://some.url",
                transcript: "jabberwocky",
                captions: "captions",
        ], true)

        jdbcTemplate.execute("DELETE FROM quiz_settings qs USING quiz_definition qd WHERE qs.quiz_ref_id = qd.id " +
                " AND qd.quiz_id = '${q1.quizId}' AND qs.setting = 'user_community'")


        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    type: q1.type]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("Video transcript validation failed")
        exception.message.contains("errorCode:ParagraphValidationFailed")
        exception.message.contains("questionId:${origQuestionDefs.questions[1].id}")
    }

    def "clearly indicate which question's video transcript is failing to copy due to paragraph validation - internally hosted video"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        pristineDragonsUser.createQuizQuestionDefs(questions)

        def origQuestionDefs = pristineDragonsUser.getQuizQuestionDefs(q1.quizId)
        Resource video = new ClassPathResource("/testVideos/create-quiz.mp4")
        pristineDragonsUser.saveSkillVideoAttributes(q1.quizId, origQuestionDefs.questions[1].id?.toString(), [
                file: video,
                transcript: "jabberwocky",
                captions: "captions",
        ], true)

        jdbcTemplate.execute("DELETE FROM quiz_settings qs USING quiz_definition qd WHERE qs.quiz_ref_id = qd.id " +
                " AND qd.quiz_id = '${q1.quizId}' AND qs.setting = 'user_community'")

        when:
        def copy = [quizId: 'newQuizCopy',
                    name: 'Copy of Quiz',
                    type: q1.type]
        pristineDragonsUser.copyQuiz(q1.quizId, copy)
        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("Video transcript validation failed")
        exception.message.contains("errorCode:ParagraphValidationFailed")
        exception.message.contains("questionId:${origQuestionDefs.questions[1].id}")
    }

}
