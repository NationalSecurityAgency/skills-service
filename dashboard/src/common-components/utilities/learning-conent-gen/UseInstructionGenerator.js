/*
 * Copyright 2025 SkillTree
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
import QuestionType from "@/skills-display/components/quiz/QuestionType.js";
import { useLog } from "@/components/utils/misc/useLog.js";
import { useAiPromptState } from '@/common-components/utilities/learning-conent-gen/UseAiPromptState.js'

export const useInstructionGenerator = () => {

  const log = useLog()
  const aiPrompts = useAiPromptState()

  const newSkillDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newSkillDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const newBadgeDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newBadgeDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const newSubjectDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newSubjectDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const newSkillGroupDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newSkillGroupDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const newProjectDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newProjectDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const newQuizDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newQuizDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const newSurveyDescriptionInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.newSurveyDescriptionInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
    log.debug(res)
    return res
  }

  const existingDescriptionInstructions = (existingText, userInstructions, instructionsToKeepPlaceholders) => {
    let res = aiPrompts.aiPromptSettings.existingDescriptionInstructions.value
      .replace(/\{\{\s*userInstructions\s*\}\}/g, userInstructions)
      .replace(/\{\{\s*existingText\s*\}\}/g, existingText);

    if (instructionsToKeepPlaceholders) {
      res = res.replace(/\{\{\s*instructionsToKeepPlaceholders\s*\}\}/g, instructionsToKeepPlaceholders);
    } else {
      res = res.replace(/\{\{\s*instructionsToKeepPlaceholders\s*\}\}/g, '');
    }

    log.debug(res);
    return res;
  }

  const newQuizInstructions = (existingDescription, numQuestions, userEnteredText) => {
    let res = aiPrompts.aiPromptSettings.newQuizInstructions.value
      .replace(/\{\{\s*existingDescription\s*\}\}/g, existingDescription)
      .replace(/\{\{\s*numQuestions\s*\}\}/g, numQuestions);

    if (userEnteredText) {
      res = res.replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText);
    } else {
      res = res.replace(/\{\{\s*userEnteredText\s*\}\}/g, '');
    }
    log.debug(res);
    return res;
  }

  const updateQuizInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.updateQuizInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText);
    log.debug(res);
    return res;
  }

  const updateSingleQuestionTypeChangedInstructions = (userEnteredText, previousQuestionType, questionType) => {
    const template = aiPrompts.aiPromptSettings[`updateSingleQuestionTypeChangedTo${questionType.id}Instructions`].value
    let res = template
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText)
      .replace(/\{\{\s*previousQuestionType\s*\}\}/g, previousQuestionType)
    log.debug(res);
    return res;
  }

  const followOnConvoInstructions = (userEnteredText) => {
    const res = aiPrompts.aiPromptSettings.followOnConvoInstructions.value
      .replace(/\{\{\s*userEnteredText\s*\}\}/g, userEnteredText);
    log.debug(res);
    return res;
  }

  const singleQuestionInstructions = (userInput, questionType, existingQuestionInfo, instructionsToKeepPlaceholders) => {
    const intro = existingQuestionInfo
      ? `# Task: Update an existing ${questionType.id} Question Type and its answers based on the user's feedback.`
      : `# Task: Generate a ${questionType.id} Question Type with answers based on the user's request.`;

    const usersRequestWord = existingQuestionInfo ? 'Feedback' : 'Request';
    const additionalInstructions = !existingQuestionInfo ? '' :
      `\n- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.\n`;

    let answersJson = [];
    if (existingQuestionInfo?.answers?.length > 0 &&
      (QuestionType.isMultipleChoice(questionType.id) || QuestionType.isSingleChoice(questionType.id))) {
      answersJson = existingQuestionInfo.answers.map((answer) => ({
        answer: answer.answer,
        isCorrect: answer.isCorrect
      }));
    }

    const existingQuestionInfoString = !existingQuestionInfo ? '' :
      `\n\n## Existing Question\n        \n### Question:\n${existingQuestionInfo.question}\n   \n### Answers:\n${JSON.stringify(answersJson)}\n\n`;

    const template = aiPrompts.aiPromptSettings[`singleQuestionInstructions${questionType.id}`].value
    let res = template
      .replace(/\{\{\s*intro\s*\}\}/g, intro)
      .replace(/\{\{\s*existingQuestionInfoString\s*\}\}/g, existingQuestionInfoString)
      .replace(/\{\{\s*usersRequestWord\s*\}\}/g, usersRequestWord)
      .replace(/\{\{\s*userInput\s*\}\}/g, userInput)
      .replace(/\{\{\s*additionalInstructions\s*\}\}/g, additionalInstructions);

    log.debug(res);
    return res;
  }

  return {
    newSkillDescriptionInstructions,
    newBadgeDescriptionInstructions,
    newSubjectDescriptionInstructions,
    newSkillGroupDescriptionInstructions,
    newProjectDescriptionInstructions,
    newQuizDescriptionInstructions,
    newSurveyDescriptionInstructions,
    existingDescriptionInstructions,
    newQuizInstructions,
    updateQuizInstructions,
    singleQuestionInstructions,
    updateSingleQuestionTypeChangedInstructions,
    followOnConvoInstructions
  }
}