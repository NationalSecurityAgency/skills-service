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
package skills.intTests.quiz

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

@Slf4j
class QuizDefValidationSpecs extends DefaultIntSpec {

    def "only quiz admin can remove quiz"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        // create project where projectId = quizId
        skillsService.createProject([projectId: quiz1.quizId, name: "Some Project Name"])
        when:
        otherUser.removeQuizDef(quiz1.quizId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("code=403 FORBIDDEN")
    }

}

