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
package skills.services.quiz

import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.QuizSettingsRequest
import skills.quizLoading.QuizSettings
import skills.storage.repos.QuizQuestionDefRepo
import spock.lang.Specification

class QuizSettingsServiceValidationSpec extends Specification {

    QuizQuestionDefRepo quizQuestionDefRepo = Mock()
    QuizSettingsService service = new QuizSettingsService(quizQuestionDefRepo: quizQuestionDefRepo)

    private void validate(QuizSettings setting, String value) {
        service.validateProvidedQuizSetting("quiz1", new QuizSettingsRequest(setting: setting.setting, value: value))
    }

    def "a numeric but non integer value is a BadParam instead of an unexpected error - #setting #value"() {
        when:
        validate(setting, value)

        then:
        SkillQuizException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "Provided value [${value}] for [${setting.setting}] setting must be an integer"

        where:
        [setting, value] << [
                [QuizSettings.MaxNumAttempts, QuizSettings.MinNumQuestionsToPass],
                ["1.5", "0x10", "1e3", ".5", "99999999999"],
        ].combinations()
    }

    def "a value that is not numeric keeps its existing message"() {
        when:
        validate(QuizSettings.MaxNumAttempts, "1a")

        then:
        SkillQuizException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "Provided value [1a] for [${QuizSettings.MaxNumAttempts.setting}] setting must be numeric"
    }

    def "integer values keep passing validation"() {
        given:
        quizQuestionDefRepo.countByQuizId("quiz1") >> 5

        when:
        validate(setting, value)

        then:
        noExceptionThrown()

        where:
        setting                            | value
        QuizSettings.MaxNumAttempts        | "-1"
        QuizSettings.MaxNumAttempts        | "3"
        QuizSettings.MinNumQuestionsToPass | "2"
    }

    def "values below -1 keep their existing message"() {
        when:
        validate(QuizSettings.MaxNumAttempts, "-2")

        then:
        SkillQuizException e = thrown()
        e.message == "Provided value [-2] for [${QuizSettings.MaxNumAttempts.setting}] setting must be >= -1"
    }
}
