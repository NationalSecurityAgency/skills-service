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
export const useInstructionGenerator = () => {

    const newDescriptionInstructions = (userEnteredText) => {
        return `Generate a detailed description for a skill that\'s will be part of a larger training. Here is user provided text that gives information about the skills: "${userEnteredText}". 

Here are the requirements:
- Do not provide an introduction. 
- Use Markdown. 
- Use the word "skill" instead of "training". 
- Do not include the word "skill" in any titles.
- Do not wrap sections with \`\`\`
`
    }

    const existingDescriptionInstructions = (existingDescription, userEnteredText, instructionsToKeepPlaceholders) => {
        return `Here is the current description:
"${existingDescription}"

Please modify it based on the following instructions: "${userEnteredText}"

Here are the requirements:
- Provide the new text after ---New Text--- in its raw form first without any comments or fields (such as "corrected text"), then use "---COMMENTS---" as a separator, and finally list any comments or suggestions.
${instructionsToKeepPlaceholders ? `-${instructionsToKeepPlaceholders}` : ''}
`
    }

    return {
        newDescriptionInstructions,
        existingDescriptionInstructions
    }
}