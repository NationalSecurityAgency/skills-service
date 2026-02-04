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
package skills.intTests.ai


import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException

@Slf4j
class TextInputAIGraderConfigSpecs extends DefaultIntSpec {

    def "save text input AI grading configs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        when:
        def aiGradingConf_before = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer", 62)
        def aiGradingConf_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)

        // now disable
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer", 62, false)
        def aiGradingConf_after_disable = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)

        then:
        aiGradingConf_before.enabled == false
        aiGradingConf_before.correctAnswer == null
        aiGradingConf_before.minimumConfidenceLevel == 75 // default

        aiGradingConf_after.enabled == true
        aiGradingConf_after.correctAnswer == "Correct answer"
        aiGradingConf_after.minimumConfidenceLevel == 62

        aiGradingConf_after_disable.enabled == false
        aiGradingConf_after_disable.correctAnswer == "Correct answer"
        aiGradingConf_after_disable.minimumConfidenceLevel == 62
    }

    def "correct answer must be provided when saving AI grading configs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, null, 62)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Correct Answer was not provided")
    }

    def "min confidence level must be provided when saving AI grading configs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "correct", null)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Minimum Confidence Level")
    }

    def "min confidence level must be > 0 when saving AI grading configs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "correct", 1)
        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "correct", 0)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Minimum Confidence Level must be > 0 and <= 100")
    }

    def "min confidence level must be <= 100 when saving AI grading configs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "correct", 100)
        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "correct", 101)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Minimum Confidence Level must be > 0 and <= 100")
    }

    def "can only save AI grading configs for Text Input question"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createChoiceQuestions(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "correct", 62)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Only TextInput type is supported")
    }

    def "question id must exist"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id+1, "correct", 62)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Did not find question with the provided id")
    }

    def "question must be for the provided quizId"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        def quiz1 = QuizDefFactory.createQuiz(2)
        def quizDef1 = skillsService.createQuizDef(quiz1).body

        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef1.quizId, questionDef.id, "correct", 62)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Question's quiz id must match provided quiz id")
    }

    def "AI Grading and Video Settings can coexist - save grading after"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        String qIdAsStr = questionDef.id.toString()
        when:
        skillsService.saveSkillVideoAttributes(quizDef.quizId, qIdAsStr,
                [videoUrl: "http://some.url", transcript: "transcript", captions: "captions"], true)
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer", 62)
        def skillVideoRes = skillsService.getSkillVideoAttributes(quiz.quizId, qIdAsStr, true)
        def aiGradingRes = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)

        skillsService.saveSkillVideoAttributes(quizDef.quizId, qIdAsStr,
                [videoUrl: "http://some1.url", transcript: "transcript1", captions: "captions1"], true)
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer1", 63)
        def skillVideoRes_t1 = skillsService.getSkillVideoAttributes(quiz.quizId, qIdAsStr, true)
        def aiGradingRes_t1 = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)

        then:
        skillVideoRes.videoUrl == "http://some.url"
        skillVideoRes.captions == "captions"
        skillVideoRes.transcript == "transcript"
        skillVideoRes.isInternallyHosted == false

        aiGradingRes.enabled == true
        aiGradingRes.correctAnswer == "Correct answer"
        aiGradingRes.minimumConfidenceLevel == 62

        skillVideoRes_t1.videoUrl == "http://some1.url"
        skillVideoRes_t1.captions == "captions1"
        skillVideoRes_t1.transcript == "transcript1"
        skillVideoRes_t1.isInternallyHosted == false

        aiGradingRes_t1.enabled == true
        aiGradingRes_t1.correctAnswer == "Correct answer1"
        aiGradingRes_t1.minimumConfidenceLevel == 63
    }

    def "AI Grading and Video Settings can coexist - save grading before"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        String qIdAsStr = questionDef.id.toString()
        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer", 62)
        skillsService.saveSkillVideoAttributes(quizDef.quizId, qIdAsStr,
                [videoUrl: "http://some.url", transcript: "transcript", captions: "captions"], true)
        def aiGradingRes = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)
        def skillVideoRes = skillsService.getSkillVideoAttributes(quiz.quizId, qIdAsStr, true)

        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer1", 63)
        skillsService.saveSkillVideoAttributes(quizDef.quizId, qIdAsStr,
                [videoUrl: "http://some1.url", transcript: "transcript1", captions: "captions1"], true)
        def aiGradingRes_t1 = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)
        def skillVideoRes_t1 = skillsService.getSkillVideoAttributes(quiz.quizId, qIdAsStr, true)

        then:
        skillVideoRes.videoUrl == "http://some.url"
        skillVideoRes.captions == "captions"
        skillVideoRes.transcript == "transcript"
        skillVideoRes.isInternallyHosted == false

        aiGradingRes.enabled == true
        aiGradingRes.correctAnswer == "Correct answer"
        aiGradingRes.minimumConfidenceLevel == 62

        skillVideoRes_t1.videoUrl == "http://some1.url"
        skillVideoRes_t1.captions == "captions1"
        skillVideoRes_t1.transcript == "transcript1"
        skillVideoRes_t1.isInternallyHosted == false

        aiGradingRes_t1.enabled == true
        aiGradingRes_t1.correctAnswer == "Correct answer1"
        aiGradingRes_t1.minimumConfidenceLevel == 63
    }

    def "AI Grading and Video Settings can coexist - delete video settings"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = QuizDefFactory.createTextInputQuestion(1, 1)
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDef = skillsService.createQuizQuestionDef(questions).body

        String qIdAsStr = questionDef.id.toString()
        when:
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id, "Correct answer", 62)
        skillsService.saveSkillVideoAttributes(quizDef.quizId, qIdAsStr,
                [videoUrl: "http://some.url", transcript: "transcript", captions: "captions"], true)
        def aiGradingRes = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)
        def skillVideoRes = skillsService.getSkillVideoAttributes(quiz.quizId, qIdAsStr, true)

        skillsService.deleteSkillVideoAttributes(quizDef.quizId, qIdAsStr, true)
        def aiGradingRes_t1 = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDef.id)
        boolean noSkillVidAttrs = false
        try {
            skillsService.getSkillVideoAttributes(quiz.quizId, qIdAsStr, true)
        } catch (SkillsClientException sk) {
            noSkillVidAttrs = true
        }

        then:
        skillVideoRes.videoUrl == "http://some.url"
        skillVideoRes.captions == "captions"
        skillVideoRes.transcript == "transcript"
        skillVideoRes.isInternallyHosted == false

        aiGradingRes.enabled == true
        aiGradingRes.correctAnswer == "Correct answer"
        aiGradingRes.minimumConfidenceLevel == 62

        aiGradingRes_t1.enabled == true
        aiGradingRes_t1.correctAnswer == "Correct answer"
        aiGradingRes_t1.minimumConfidenceLevel == 62

        noSkillVidAttrs
    }
}
