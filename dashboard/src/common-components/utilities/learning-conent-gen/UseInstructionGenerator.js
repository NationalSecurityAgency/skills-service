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
import {useLog} from "@/components/utils/misc/useLog.js";

export const useInstructionGenerator = () => {

    const log = useLog()

    const newSkillDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a skill based on this information: "${userEnteredText}". 

# Requirements:
- Do not provide an introduction. 
- Use extensive Markdown formatting
- Avoid using the word "skill" in any headers
- Do not wrap sections with \`\`\`
- Maintain a professional yet engaging tone
`
        log.debug(res)
        return res
    }

    const newBadgeDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a badge that will be part of a training program based on this information: "${userEnteredText}". 

# Requirements:
- Generate engaging overview and background information for the badge
- Focus on the badge as a whole, not individual skills
- Use Markdown formatting
- Do not wrap sections with \`\`\`
- Keep the description engaging and interesting
- Do not include titles or headers

# Badge Context:
- A badge represents a collection of related skills
- It's earned when all required skills are completed
- Badges serve as gamification elements to recognize achievement
- The description should provide an overview of the badge's purpose and value
- Focus on the badge's overall theme, not specific skills
`
        log.debug(res)
        return res
    }

    const newSubjectDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a training subject based on this information: "${userEnteredText}". 

# Requirements:
- Create an engaging overview of the subject
- Focus on the subject as a whole, not individual skills
- Start directly with the content (no introduction needed)
- Use Markdown formatting
- Do not wrap sections with \`\`\`
- Keep the description engaging and informative
- Do not include titles or headers

# Subject Context:
- A Subject is a collection of related skills in a training program
- It helps organize skills into logical groupings
- The description should explain the subject's purpose and scope
- Focus on the overall subject, not specific skills
- Keep the tone professional yet engaging
`
        log.debug(res)
        return res
    }

    const newSkillGroupDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a Skill Group based on this information: "${userEnteredText}". 

# Requirements:
- Create a clear and engaging overview of the Skill Group
- Focus on the group's collective purpose, not individual skills
- Start directly with the content (no introduction needed)
- Use Markdown formatting
- Do not wrap sections with \`\`\`
- Keep the description concise and informative
- Do not include titles or headers
- Keep it brief (2-3 sentences)

# Skill Group Context:
- A way to organize related skills under a subject
- Represents a collection of 2+ skills to be completed together
- Can require all skills or a subset to be completed
- Description should explain the group's learning objectives
- Emphasize the collective value of the skills
- Maintain a professional yet engaging tone
`
        log.debug(res)
        return res
    }

    const newProjectDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a training program based on this information: "${userEnteredText}". 

# Requirements:
- Create an engaging overview of the training program
- Focus on the overall training, not individual skills
- Use Markdown formatting
- Do not wrap sections with \`\`\`
- Make the description compelling and informative
- Keep the tone professional yet engaging

# Training Context:
- A SkillTree training is a comprehensive gamified learning experience
- It includes skills, subjects, levels, and learning paths
- The description should provide an overview of the training's purpose and value
- Focus on the training as a whole, not specific skills
- Highlight the learning journey and outcomes
`
        log.debug(res)
        return res
    }

    const newQuizDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a Quiz based on this information: "${userEnteredText}". 

# Requirements:
- Create a clear and engaging overview of the quiz
- Focus on the assessment's purpose and scope
- Use Markdown formatting
- Do not wrap sections with \`\`\`
- Do not generate sample questions

# Quiz Context:
- A knowledge assessment with multiple question types 
- Includes a passing score requirement
- Emphasize the learning outcomes and objectives 
- Avoid mentioning specific questions
`
        log.debug(res)
        return res
    }

    const newSurveyDescriptionInstructions = (userEnteredText) => {
        const res = `Generate a detailed description for a Survey based on this information: "${userEnteredText}". 

# Requirements:
- Create a clear and engaging introduction of the survey
- Focus on the survey's purpose and scope
- Use Markdown formatting
- Do not wrap sections with \`\`\`
- Do not generate sample questions
- Keep the description concise and informative
- Consider that description's audience is the participants of the survey

# Survey Context:
- A tool for gathering feedback and insights
- Used to collect opinions, preferences, or experiences
- Emphasize the value of participant responses
- Avoid mentioning specific questions
- Description will be displayed on the survey's splash page
`
        log.debug(res)
        return res
    }

    const existingDescriptionInstructions = (existingText, userInstructions, instructionsToKeepPlaceholders) => {
        const res = `Modify current descriptions based on the following user instructions: "${userInstructions}"
        
# Current Description:
${existingText}

# Specific Requirements:
- First provide the new text without any comments or fields (such as "corrected text")
- At the very end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
${instructionsToKeepPlaceholders ? `-${instructionsToKeepPlaceholders}` : ''}
`
        log.debug(res)
        return res
    }

    const questionGenRules = {
        SingleChoice: `- MUST have exactly 1 correct answer
- MUST have 3-5 answer options`,
        MultipleChoice: `- MUST have 2 or more correct answers
- MUST have between 3-5 answer options`,
        TextInput: `- MUST have empty answers array`,
        Matching: `- ALL answers must be marked true for isCorrect
- ALL answers must have a multiPartAnswer with a term and and a value`,
    }

    const questionExampleJson = {
        SingleChoice: `[
      {"answer": "3", "isCorrect": false},
      {"answer": "4", "isCorrect": true},  // Only ONE true
      {"answer": "5", "isCorrect": false}
    ]`,
        MultipleChoice: `[
      {"answer": "2", "isCorrect": true},   // First correct
      {"answer": "3", "isCorrect": true},   // Second correct
      {"answer": "4", "isCorrect": false},
      {"answer": "5", "isCorrect": true}    // Third correct
    ]`,
        TextInput: `[]  // ALWAYS empty for TextInput`,
        Matching: `[
      { "multiPartAnswer": { "term": "banana", "value": "yellow" }, "isCorrect": true },
      { "multiPartAnswer": { "term": "apple", "value": "red" }, "isCorrect": true },
      { "multiPartAnswer": { "term": "carrot", "value": "orange" }, "isCorrect": true },
  ]`
    }

    const questionRules = {
        SingleChoice: {
            typeRule: `### 1. SingleChoice Questions
- MUST have \`"questionType": "SingleChoice"\`
${questionGenRules.SingleChoice}
- Example:
  \`\`\`json
  {
    "question": "What is 2+2?",
    "questionType": "SingleChoice",
    "answers": ${questionExampleJson.SingleChoice}
  }
  \`\`\``,
            validationCheck: `1. For SingleChoice:
   - Exactly ONE answer has \`"isCorrect": true\`
   - All others have \`"isCorrect": false\`
   - ALL objects in the answers array should have the \`"answer"\` property and should not have the \`"multiPartAnswer"\` property
   - 3-5 answer options total`,
            mistakesToAvoid: `- SingleChoice with 0 or >1 correct answers → INVALID`,
            isCorrectCheck: `- SingleChoice: Exactly 1`,
            countCorrectCheck: `- If count == 1 then questionType *Must* be 'SingleChoice'`,
        },
        MultipleChoice: {
            typeRule: `### 2. MultipleChoice Questions
- MUST have \`"questionType": "MultipleChoice"\`
${questionGenRules.MultipleChoice}
- Example:
  \`\`\`json
  {
    "question": "Which are prime numbers?",
    "questionType": "MultipleChoice",
    "answers": ${questionExampleJson.MultipleChoice}
  }
  \`\`\``,
            validationCheck: `2. For MultipleChoice:
   - AT LEAST TWO answers have \`"isCorrect": true\`
   - All others have \`"isCorrect": false\`
   - ALL objects in the answers array should have the \`"answer"\` property and should not have the \`"multiPartAnswer"\` property
   - 3-5 answer options total`,
            mistakesToAvoid: `- MultipleChoice with only 1 correct answer → INVALID`,
            isCorrectCheck: `MultipleChoice: 2 or more`,
            countCorrectCheck: `- If count >= 2 → then questionType *Must* be 'MultipleChoice'`,
        },
        TextInput: {
            typeRule: `### 3. TextInput Questions
- MUST have \`"questionType": "TextInput"\`
${questionGenRules.TextInput}
- Example:
  \`\`\`json
  {
    "question": "What is the capital of France?",
    "questionType": "TextInput",
    "answers": ${questionExampleJson.TextInput}
  }
  \`\`\``,
            validationCheck: `3. For TextInput:
   - \`"answers": []\` (empty array)
   - No answer objects`,
            mistakesToAvoid: `- TextInput with answers array not empty → INVALID`,
            isCorrectCheck: `- TextInput: No answers`,
            countCorrectCheck: ``,
        },
        Matching: {
          typeRule: `### 4. Matching Questions
- MUST have \`"questionType": "Matching"\`
${questionGenRules.Matching}
- Example:
  \`\`\`json
  {
  "question": "What are the colors of these fruits?",
  "questionType": "Matching",
  "answers": [
    {
      "isCorrect": true,
      "multiPartAnswer": {
        "term": "banana",
        "value": "yellow"
      }
    },
    {
      "isCorrect": true
      "multiPartAnswer": {
        "term": "apple",
        "value": "red"
      }
    },
    {
      "isCorrect": true
      "multiPartAnswer": {
        "term": "carrot",
        "value": "orange"
      }
    }
  ]
}   
  \`\`\``,
          validationCheck: `4. For Matching:
   - ALL objects in the answers array have the property \`"isCorrect": true\`
   - ALL objects in the answers array should have the \`"multiPartAnswer"\` property and should not have the \`"answer"\` property
   - ALL objects in the answers array have the property \`"multiPartAnswer"\` with \`"term"\` and \`"value"\` properties that are not repeated
   - ALL multiPartAnswers must not contain any duplicate term or duplicate value fields`,
          mistakesToAvoid: `- All objects in the answers array have the property \`"multiPartAnswer"\` with \`"term"\` and \`"value"\` properties, eg: \`"multiPartAnswer": { "term": "carrot", "value": "orange" }\`
- All multiPartAnswer.term and multiPartAnswer.value field values in the answers array must be unique.  Duplicate term or value fields → INVALID`,
          isCorrectCheck: ``,
          countCorrectCheck: ``,
        }
    }

    const quizRules = `
# Quiz Generation Instructions - STRICT RULES

## Output Format
Return a JSON object with exactly two properties:
1. \`numQuestions\`: Number of questions generated
2. \`questions\`: Array of question objects

## Question Type Rules

${questionRules.SingleChoice.typeRule}

${questionRules.MultipleChoice.typeRule}

${questionRules.TextInput.typeRule}

${questionRules.Matching.typeRule}

## Validation Checklist
BEFORE finalizing the response, VERIFY each question:

${questionRules.SingleChoice.validationCheck}

${questionRules.MultipleChoice.validationCheck}

${questionRules.TextInput.validationCheck}

## Common Mistakes to AVOID:
${questionRules.MultipleChoice.mistakesToAvoid}
${questionRules.SingleChoice.mistakesToAvoid}
${questionRules.TextInput.mistakesToAvoid}
${questionRules.Matching.mistakesToAvoid}

## Final Check
Before responding, count the number of \`"isCorrect": true\` for EACH question and verify:
${questionRules.MultipleChoice.isCorrectCheck}
${questionRules.SingleChoice.isCorrectCheck}
${questionRules.TextInput.isCorrectCheck}
${questionRules.Matching.isCorrectCheck}

## IMPORTANT: Count the correct answers
For EACH question, before including it:
- Count the number of answers where \`"isCorrect": true\`
${questionRules.MultipleChoice.countCorrectCheck}
${questionRules.SingleChoice.countCorrectCheck}
- If questionType doesn't match, FIX IT
`

    const newQuizInstructions = (existingDescription, numQuestions, userEnteredText) => {
        return `# Quiz Generation Task

## Skill Description:
"${existingDescription}"

## Requirements:
- Generate ${numQuestions} questions, unless otherwise specified in the user instructions
- Include at least one of each question type
- Follow ALL rules strictly
- Double-check the number of correct answers for each question type

${(userEnteredText ? `Please generate it based on the following user instructions: "${userEnteredText}"` : '')}

${quizRules}

## Output:
Provide ONLY valid JSON in this exact format:
\`\`\`json
{
  "numQuestions": ${numQuestions},
  "questions": [
    // Your questions here
  ]
}
\`\`\`
`
}

    const updateQuizInstructions = ( userEnteredText ) => {
        return `Apply the following instructions to this conversation: "${userEnteredText}"

Here are the specific requirements:
- Make sure to regenerate the entire quiz again
- Add a new parameter to the returned json called "changes" that will contain a list of comments or suggestions.

## Output:
Provide ONLY valid JSON in this exact format:
\`\`\`json
{
  "numQuestions": Number,
  "questions": [
    // Your questions here
  ],
  "changes": [
    // Your changes here
  ]
}
\`\`\`
`
    }

  const updateSingleQuestionTypeChangedInstructions = ( userEnteredText, previousQuestionType, questionType ) => {
    return `${followOnConvoInstructions(userEnteredText)}

## IMPORTANT: The Question Type has changed from \`${previousQuestionType}\` to \`${questionType.id}\`.  
- **You must change the answers array to follow the new questionType (${questionType.id}) rules!**  
- Disregard the "Question Type Rules (${previousQuestionType})" from earlier in the conversation and use only the following "Question Type Rules (${questionType.id})" for this response.

# Question Generation Instructions - STRICT RULES

## Output Format
Return the answers as JSON array of objects that strictly adhere to the following rules based on the new questionType (${questionType.id}):

${singleQuestionRulesInstructions(questionType)}
`
  }

    const followOnConvoInstructions = (userEnteredText) => {
        return `Apply the following instructions to this conversation: "${userEnteredText}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
`
    }

    const singleQuestionInstructions = (userInput, questionType, existingQuestionInfo, instructionsToKeepPlaceholders) => {
        const intro = existingQuestionInfo ? `# Task: Update an existing ${questionType.id} Question Type and its answers based on the user's feedback.`
            : `# Task: Generate a ${questionType.id} Question Type with answers based on the user's request.`

        const usersRequestWord = existingQuestionInfo ? 'Feedback' : 'Request'

        const additionalInstructions = !existingQuestionInfo ? '' : `
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
`

        let answersJson = []
        if (existingQuestionInfo?.answers?.length > 0 && (QuestionType.isMultipleChoice(questionType.id) || QuestionType.isSingleChoice(questionType.id))) {
            answersJson = existingQuestionInfo.answers.map((answer) => ({ answer: answer.answer, isCorrect: answer.isCorrect}))
        }

        const existingQuestionInfoString = !existingQuestionInfo ? '' : `

## Existing Question
        
### Question:
${existingQuestionInfo.question}
   
### Answers:
${JSON.stringify(answersJson)}

`
        const res = `${intro}${existingQuestionInfoString}

## User's ${usersRequestWord}:
"${userInput}"

${singleQuestionRulesInstructions(questionType, additionalInstructions)}`

        log.debug(res)
        return res
    }

    const singleQuestionRulesInstructions = (questionType, additionalInstructions) => {
      const questionTypeRules = questionRules[questionType.id]
      const exampleAnswerJson = questionExampleJson[questionType.id]
      const genRules = questionGenRules[questionType.id]
      return `
# Question Type Rules (${questionType.id})

## Instructions:
- First, generate a clear and concise question based on the user's description.
${genRules}
- Ensure answers are plausible and relevant to the question.${additionalInstructions}

## Required Response Format:
### Question:
[Your generated question here]

### Answers:
[Your JSON array of answers here]

## Example Response:
### Question:
What are some popular chess openings?

### Answers:
${exampleAnswerJson}

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (${questionType.id}):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
${questionTypeRules.validationCheck}

## Common Mistakes to AVOID:
${questionTypeRules.mistakesToAvoid}

## Final Check
Before responding, count the number of \`"isCorrect": true\` for the question and verify:
${questionTypeRules.isCorrectCheck}

## IMPORTANT: Count the correct answers
- Count the number of answers where \`"isCorrect": true\`
${questionTypeRules.countCorrectCheck}
- If questionType doesn't match, FIX IT
`
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