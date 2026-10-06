/*
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

export const validateGeneratedBlanks = (question, answers, maxAnswersPerQuizQuestion) => {
  // Match the blank syntax accepted by quiz validation and the question editor.
  const blankCount = (question.replace(/\\_/g, '_').match(/_{2,}/g) || []).length
  if (blankCount === 0) {
    throw new Error('The generated question needs at least one blank (___). Please ask the AI to regenerate it.')
  }
  if (blankCount > maxAnswersPerQuizQuestion) {
    throw new Error(`The generated question exceeds the maximum number of answers ${maxAnswersPerQuizQuestion}. Please ask the AI to regenerate it.`)
  }
  if (!Array.isArray(answers) || answers.length !== blankCount) {
    throw new Error(`The generated question has ${blankCount} blank(s), but ${Array.isArray(answers) ? answers.length : 'no valid array of'} answer(s). Please ask the AI to provide one answer per blank in order.`)
  }
  answers.forEach((answer, index) => {
    if (!answer || typeof answer.answer !== 'string' ||
        !answer.answer.trim() || answer.answer.split(';').some((option) => !option.trim()) ||
        answer.isCorrect !== true || 'multiPartAnswer' in answer) {
      throw new Error(`Answer ${index + 1} must have nonempty text (with no empty semicolon-separated options), isCorrect: true, and no multiPartAnswer. Please ask the AI to regenerate it.`)
    }
  })
}
