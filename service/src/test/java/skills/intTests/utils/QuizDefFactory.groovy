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

import groovy.json.JsonOutput
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.QuizDefParent.QuizType

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
        return [quizId: getDefaultQuizId(quizNumber), name: getDefaultQuizName(quizNumber), type: QuizDefParent.QuizType.Quiz.toString(), description: description]
    }

    static createQuizSurvey(int quizNumber = 1, String description = null) {
        return [quizId: getDefaultQuizId(quizNumber), name: getDefaultQuizName(quizNumber), type: QuizDefParent.QuizType.Survey.toString(), description: description]
    }

    static createChoiceQuestions(int quizNumber = 1, int numberOfQuestions = 1, int numberOfAnswers = 2, QuizQuestionType questionType = QuizQuestionType.SingleChoice, QuizType quizType = QuizType.Quiz) {
        return (1..numberOfQuestions).collect {
            createChoiceQuestion(quizNumber, it, numberOfAnswers, questionType, quizType)
        }
    }

    static createMatchingQuestion(int quizNumber = 1, int questionsNumber = 1, int numberOfAnswers = 2) {
        String question = "This is questions #${questionsNumber}".toString()
        String answerHint = "This is a hint for question #${questionsNumber}".toString()
        List answers = numberOfAnswers > 0 ? (1..numberOfAnswers).collect {
            return [
                    answer: "",
                    isCorrect: true,
                    multiPartAnswer: JsonOutput.toJson([
                        term: 'term' + it,
                        value: 'value' + it
                    ])
            ]
        } : []
        return [
                quizId  : getDefaultQuizId(quizNumber),
                question: question,
                answerHint: answerHint,
                questionType: QuizQuestionType.Matching.toString(),
                answers: answers,
        ]
    }

    static createChoiceQuestion(int quizNumber = 1, int questionsNumber = 1, int numberOfAnswers = 2, QuizQuestionType questionType = QuizQuestionType.SingleChoice, QuizType quizType = QuizType.Quiz) {
        String question = "This is questions #${questionsNumber}".toString()
        String answerHint = "This is a hint for question #${questionsNumber}".toString()
        boolean isMultipleChoice = questionType == QuizQuestionType.MultipleChoice
        boolean isQuiz = quizType == QuizType.Quiz
        List answers = numberOfAnswers > 0 ? (1..numberOfAnswers).collect {
            boolean isEven = !(it % 2 == 0)
            return [
                    answer: "Answer #${it}".toString(),
                    isCorrect: isQuiz && ((isMultipleChoice && isEven) || (!isMultipleChoice && it == 1))  ? true : false,
            ]
        } : []

        return [
                quizId  : getDefaultQuizId(quizNumber),
                question: question,
                answerHint: answerHint,
                questionType: questionType.toString(),
                answers: answers,
        ]
    }

    static createMultipleChoiceSurveyQuestion(int quizNumber = 1, int questionsNumber = 1, int numberOfAnswers = 2, QuizType quizType = QuizType.Survey) {
        return this.createChoiceQuestion(quizNumber, questionsNumber, numberOfAnswers, QuizQuestionType.MultipleChoice, quizType)
    }

    static createSingleChoiceSurveyQuestion(int quizNumber = 1, int questionsNumber = 1, int numberOfAnswers = 2, QuizType quizType = QuizType.Survey) {
        return this.createChoiceQuestion(quizNumber, questionsNumber, numberOfAnswers, QuizQuestionType.SingleChoice, quizType)
    }

    static createTextInputQuestion(int quizNumber = 1, int questionsNumber = 1) {
        return this.createChoiceQuestion(quizNumber, questionsNumber, 0, QuizQuestionType.TextInput, QuizType.Survey)
    }

    static createRatingSurveyQuestion(int quizNumber = 1, int questionsNumber = 1) {
        return this.createChoiceQuestion(quizNumber, questionsNumber, 0, QuizQuestionType.Rating, QuizType.Survey)
    }

}
