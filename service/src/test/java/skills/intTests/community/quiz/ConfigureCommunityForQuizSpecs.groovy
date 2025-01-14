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
package skills.intTests.community.quiz

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

class ConfigureCommunityForQuizSpecs extends DefaultIntSpec {

    def "only member of the community can enable project "() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        when:
        allDragonsUser.createQuizDef(quiz)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }
}
