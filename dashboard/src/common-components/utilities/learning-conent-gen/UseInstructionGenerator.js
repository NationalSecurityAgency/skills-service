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
        return `Generate a detailed description for a skill that will be part of a larger training. Here is user provided text that gives information about the skills: "${userEnteredText}". 

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

Here are the specific requirements:
- First provide the new text without any comments or fields (such as "corrected text")
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
${instructionsToKeepPlaceholders ? `-${instructionsToKeepPlaceholders}` : ''}
`
    }

    const quizRules = `
# Quiz Generation Instructions - STRICT RULES

## Output Format
Return a JSON object with exactly two properties:
1. \`numQuestions\`: Number of questions generated
2. \`questions\`: Array of question objects

## Question Type Rules

### 1. SingleChoice Questions
- MUST have \`"questionType": "SingleChoice"\`
- MUST have exactly 1 correct answer
- MUST have 3-5 answer options
- Example:
  \`\`\`json
  {
    "question": "What is 2+2?",
    "questionType": "SingleChoice",
    "answers": [
      {"answer": "3", "isCorrect": false},
      {"answer": "4", "isCorrect": true},  // Only ONE true
      {"answer": "5", "isCorrect": false}
    ]
  }
  \`\`\`

### 2. MultipleChoice Questions
- MUST have \`"questionType": "MultipleChoice"\`
- MUST have 2 or more correct answers
- MUST have between 3-5 answer options
- Example:
  \`\`\`json
  {
    "question": "Which are prime numbers?",
    "questionType": "MultipleChoice",
    "answers": [
      {"answer": "2", "isCorrect": true},   // First correct
      {"answer": "3", "isCorrect": true},   // Second correct
      {"answer": "4", "isCorrect": false},
      {"answer": "5", "isCorrect": true}    // Third correct
    ]
  }
  \`\`\`

### 3. TextInput Questions
- MUST have \`"questionType": "TextInput"\`
- MUST have empty answers array
- Example:
  \`\`\`json
  {
    "question": "What is the capital of France?",
    "questionType": "TextInput",
    "answers": []  // ALWAYS empty for TextInput
  }
  \`\`\`

## Validation Checklist
BEFORE finalizing the response, VERIFY each question:

1. For SingleChoice:
   - Exactly ONE answer has \`"isCorrect": true\`
   - All others have \`"isCorrect": false\`
   - 3-5 answer options total

2. For MultipleChoice:
   - AT LEAST TWO answers have \`"isCorrect": true\`
   - All others have \`"isCorrect": false\`
   - 3-5 answer options total

3. For TextInput:
   - \`"answers": []\` (empty array)
   - No answer objects

## Common Mistakes to AVOID:
- MultipleChoice with only 1 correct answer → INVALID
- SingleChoice with 0 or >1 correct answers → INVALID
- TextInput with answers array not empty → INVALID

## Final Check
Before responding, count the number of \`"isCorrect": true\` for EACH question and verify:
- SingleChoice: Exactly 1
- MultipleChoice: 2 or more
- TextInput: No answers

## IMPORTANT: Count the correct answers
For EACH question, before including it:
1. Count the number of answers where \`"isCorrect": true\`
2. If count == 1 then questionType *Must* be 'SingleChoice'
3. If count >= 2 → then questionType *Must* be 'MultipleChoice'
4. If questionType doesn't match, FIX IT

`

    const newQuizInstructions = (existingDescription, numQuestions) => {
        return `# Quiz Generation Task

## Skill Description:
"${existingDescription}"

## Requirements:
- Generate ${numQuestions} questions
- Include at least one of each question type
- Follow ALL rules strictly
- Double-check the number of correct answers for each question type

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

    const updateQuizInstructions = ( existingDescription, existingQuiz, userEnteredText, instructionsToKeepPlaceholders ) => {
        return `
# Task: Modify an existing quiz for a skill that will be part of a larger training. 

## Objective
Modify an existing quiz

The quiz was originally built based on this description:
"${existingDescription}".

Here is the existing quiz:
${existingQuiz} 

Please modify it based on the following instructions: "${userEnteredText}"

${quizRules}
`
    }

    const newQuestionInstructions = (userInput) => {
        return `# Task: Generate a multiple-choice question with answers based on the user's description.

## User's Request:
"${userInput}"

## Instructions:
1. First, generate a clear and concise question based on the user's description.
2. Then, provide 3-5 answer choices in JSON format.
3. Mark 1-3 answers as correct (must have at least 1 correct answer).
4. Ensure answers are plausible and relevant to the question.

## Required Response Format:
### Question:
[Your generated question here]

### Answers:
[Your JSON array of answers here]

## Example Response:
### Question:
What are some popular chess openings?

### Answers:
[
  {"answer": "The Ruy Lopez", "isCorrect": true},
  {"answer": "The Sicilian Defense", "isCorrect": true},
  {"answer": "The King's Gambit", "isCorrect": false},
  {"answer": "The Italian Game", "isCorrect": true},
  {"answer": "The Caro-Kann Defense", "isCorrect": false}
]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers`
    }

    return {
        newDescriptionInstructions,
        existingDescriptionInstructions,
        newQuizInstructions,
        updateQuizInstructions,
        newQuestionInstructions
    }
}