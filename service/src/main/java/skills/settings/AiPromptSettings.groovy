/**
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
package skills.settings

import groovy.transform.Canonical


@Canonical
class AiPromptSettings {
    AiPromptSetting systemInstructions
    AiPromptSetting newSkillDescriptionInstructions
    AiPromptSetting newBadgeDescriptionInstructions
    AiPromptSetting newSubjectDescriptionInstructions
    AiPromptSetting newSkillGroupDescriptionInstructions
    AiPromptSetting newProjectDescriptionInstructions
    AiPromptSetting newQuizDescriptionInstructions
    AiPromptSetting newSurveyDescriptionInstructions
    AiPromptSetting existingDescriptionInstructions
    AiPromptSetting followOnConvoInstructions
    AiPromptSetting newQuizInstructions
    AiPromptSetting updateQuizInstructions
    AiPromptSetting singleQuestionInstructionsMultipleChoice
    AiPromptSetting singleQuestionInstructionsSingleChoice
    AiPromptSetting singleQuestionInstructionsTextInput
    AiPromptSetting singleQuestionInstructionsMatching
    AiPromptSetting updateSingleQuestionTypeChangedToMultipleChoiceInstructions
    AiPromptSetting updateSingleQuestionTypeChangedToSingleChoiceInstructions
    AiPromptSetting updateSingleQuestionTypeChangedToTextInputInstructions
    AiPromptSetting updateSingleQuestionTypeChangedToMatchingInstructions
    AiPromptSetting inputTextQuizGradingInstructions
}
