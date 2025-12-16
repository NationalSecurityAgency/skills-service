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

    const newDescriptionInstructions = (userEnteredText) => {
        return `Generate a detailed description for a skill that will be part of a larger training. Here is user provided text that gives information about the skills: "${userEnteredText}". 

Here are the requirements:
- Do not provide an introduction. 
- Use Markdown. 
- Use the word "skill" instead of "training". 
- Do not include the word "skill" in any titles.
- Do not wrap sections with \`\`\`
`
    }

    const existingDescriptionInstructions = (existingText, userInstructions, instructionsToKeepPlaceholders) => {
        const res = `Here is the current description:
"${existingText}"

Please modify it based on the following instructions: "${userInstructions}"

Here are the specific requirements:
- First provide the new text without any comments or fields (such as "corrected text")
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
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
    {
      "answer": "",
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
    }
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
      "answer": "",
      "isCorrect": true,
      "multiPartAnswer": {
        "term": "banana",
        "value": "yellow"
      }
    },
    {
      "answer": "",
      "isCorrect": true
      "multiPartAnswer": {
        "term": "apple",
        "value": "red"
      }
    },
    {
      "answer": "",
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
   - ALL answers have \`"isCorrect": true\`
   - ALL answers have \`"multiPartAnswer"\` with \`"term"\` and \`"value"\` properties`,
          mistakesToAvoid: `- MultipleChoice with only 1 correct answer → INVALID`,
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

    const followOnConvoInstructions = (userEnteredText) => {
        return `Apply the following instructions to this conversation: "${userEnteredText}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
`
    }

    const singleQuestionInstructions = (userInput, questionType, existingQuestionInfo, instructionsToKeepPlaceholders) => {
        const questionTypeRules = questionRules[questionType.id]
        const exampleAnswerJson = questionExampleJson[questionType.id]
        const genRules = questionGenRules[questionType.id]

        const intro = existingQuestionInfo ? `# Task: Update an existing ${questionType.id} question and its answers based on the user's feedback.`
            : `# Task: Generate a ${questionType.id} question with answers based on the user's request.`

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
        log.debug(res)
        return res
    }

    return {
        newDescriptionInstructions,
        existingDescriptionInstructions,
        newQuizInstructions,
        updateQuizInstructions,
        singleQuestionInstructions,
        followOnConvoInstructions
    }
}