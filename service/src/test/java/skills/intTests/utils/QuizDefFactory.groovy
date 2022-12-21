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

import skills.services.quiz.QuizQuestionType

class QuizDefFactory {

    static String DEFAULT_QUIZ_NAME = "Test Quiz"
    static String DEFAULT_QUIZ_ID_PREPEND = DEFAULT_QUIZ_NAME.replaceAll(" ", "")

    static String getDefaultQuizId(int projNum = 1) {
        DEFAULT_QUIZ_ID_PREPEND + "${projNum}"
    }

    static String getDefaultQuizName(int projNum = 1) {
        DEFAULT_QUIZ_NAME + " #${projNum}"
    }

    static createQuiz(int quizNumber = 1, String description = null) {
        return [quizId: getDefaultQuizId(quizNumber), name: getDefaultQuizName(quizNumber), description: description]
    }

    static createMultipleChoiceQuestions(int quizNumber = 1, int numberOfQuestions = 1, int numberOfAnswers = 2) {
        return (1..numberOfQuestions).collect {
            createMultipleChoiceQuestion(quizNumber, it, numberOfAnswers)
        }
    }

    static createMultipleChoiceQuestion(int quizNumber = 1, int questionsNumber = 1, int numberOfAnswers = 2) {
        String question = "This is questions #${questionsNumber}".toString()
        List answers = (1..numberOfAnswers).collect {
            return [
                    answer: "Answer #${it}".toString(),
                    isCorrect: it == 1 ? true : false,
            ]
        }
        return [
                quizId  : getDefaultQuizId(quizNumber),
                question: question,
                questionType: QuizQuestionType.MultipleChoice.toString(),
                answers: answers,
        ]
    }
}
