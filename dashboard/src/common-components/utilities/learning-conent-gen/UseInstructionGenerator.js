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
### Question Types
1. **SingleChoice**
   - Must NOT provide less than 1 correct answer
   - Must NOT provide more than 1 correct answer
   - Must provide exactly 1 correct answer
   - 2-3 incorrect answers
   - Mark correct answer with \`"isCorrect": true\`

2. **MultipleChoice**
   - Must provide no less than 2 correct answers
   - Must provide 2 or more correct answers
   - 3-5 total options
   - Mark all correct answers with \`"isCorrect": true\`

3. **TextInput **
   - The user will provide the answer in the form of a text input
   - the answers will always be an empty array
   
### Question Content
- Ensure the question is clear and concise
- Include at least one question of each type
- Make incorrect answers plausible
- Avoid trick questions
- Avoid questions that are too easy or too hard
- Avoid questions that are too long or too short
- Avoid questions that are too complex or too simple
- Avoid questions that are too similar to each other
        
### Technical Requirements
- Format questions in JSON
- Each question should include:
  - Quiz ID (same quizId for all questions)
  - Clear question text (Markdown formatted)
  - Question type
  - Array of answer objects
- When streaming, send questions as complete json objects in a chunk
- All Questions should be returned in a single JSON array
- Provide a property called numQuestions with the number of questions generated before the array
- The entire response should be a single JSON object, with one property called 'numQuestions' and one property called 'questions' that is an array of question objects.
- Do not provide an introduction. 
- Do not provide any additional text.
- Do not provide any comments.
- Do not provide any explanations.

### Example Response Format
\`\`\`json
{
   "numQuestions": 5,
   questions: [
       {
           "quizId": "chess-fundamentals-quiz",
           "question": "How many unique pieces are there in a standard chess set?",
           "questionType": "SingleChoice",
           "answers": [
               {"answer": "6", "isCorrect": true},
               {"answer": "8", "isCorrect": false},
               {"answer": "7", "isCorrect": false},
               {"answer": "5", "isCorrect": false}
           ]
       },
       {
           "quizId": "chess-fundamentals-quiz",
           "question": "What opening strategies are listed in the training material?",
           "questionType": "MultipleChoice",
           "answers": [
               {"answer": "The Ruy Lopez", "isCorrect": true},
               {"answer": "The Sicilian Defense", "isCorrect": true},
               {"answer": "The King's Gambit", "isCorrect": false},
               {"answer": "The Italian Game", "isCorrect": true},
               {"answer": "The Caro-Kann Defense", "isCorrect": false}
           ]
       },
       {
           "quizId": "chess-fundamentals-quiz",
           "question": "Identify a common endgame position involving a king, a rook, and two pawns.",
           "questionType": "TextInput",
           "answers": []
       }
   ]
}
\`\`\`
    `

    const newQuizInstructions = (existingDescription, numQuestions) => {
        return `
# Task: Generate a quiz for a skill that will be part of a larger training. 

## Objective
Generate a quiz with ${numQuestions} questions for a skill that will be part of a larger training based on this description:
"${existingDescription}".

${quizRules}
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
"${existingQuiz}" 

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