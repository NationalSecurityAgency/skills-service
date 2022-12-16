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
package skills.intTests.utils

class QuizDefFactory {

    static String DEFAULT_QUIZ_NAME = "Test Quiz"
    static String DEFAULT_QUIZ_ID_PREPEND = DEFAULT_QUIZ_NAME.replaceAll(" ", "")

    static String getDefaultQuizId(int projNum = 1) {
        DEFAULT_QUIZ_ID_PREPEND + "${projNum}"
    }

    static String getDefaultQuizName(int projNum = 1) {
        DEFAULT_QUIZ_NAME + "#${projNum}"
    }

    static createQuiz(int projNumber = 1, String description = null) {
        return [quizId: getDefaultQuizId(projNumber), name: getDefaultQuizName(projNumber), description: description]
    }
}
