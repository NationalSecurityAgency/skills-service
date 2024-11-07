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
package skills.intTests.quiz

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.quizLoading.QuizUserPreferences
import skills.storage.model.auth.RoleName

class QuizUserPreferencesSpecs extends DefaultIntSpec {

    def "save and get user quiz preferences"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        List<String> users = getRandomUsers(3, true)
        List<SkillsService> services = users.collect { createService(it) }
        services.each {
            skillsService.addQuizUserRole(quiz1.quizId, it.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        services[0].saveQuizUserPreference(quiz1.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, true)
        services[1].saveQuizUserPreference(quiz1.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, false)

        when:
        def user1Preferences = services[0].getCurrentUserQuizPreferences(quiz1.quizId)
        def user2Preferences = services[1].getCurrentUserQuizPreferences(quiz1.quizId)
        def user3Preferences = services[2].getCurrentUserQuizPreferences(quiz1.quizId)
        then:
        user1Preferences.preference == [QuizUserPreferences.DisableGradingRequestNotification.preference]
        user1Preferences.value == ["true"]

        user2Preferences.preference == [QuizUserPreferences.DisableGradingRequestNotification.preference]
        user2Preferences.value == ["false"]

        !user3Preferences
    }

    def "can only save supported user preferences"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        when:
        skillsService.saveQuizUserPreference(quiz1.quizId, QuizSettings.QuizTimeLimit.setting, 55)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided preferenceKey [${QuizSettings.QuizTimeLimit.setting}] is not a valid setting")
    }
}

