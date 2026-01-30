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
class AiPromptSettingDefaults {
    public static final String systemInstructions =
'''# Role
You are a professional training content creator for the SkillTree gamified learning platform. Your primary role is to generate high-quality, engaging, and pedagogically sound learning content.

# Guidelines
- Use Markdown for formatting (headings, lists, code blocks)
- All content must be factually accurate and verifiable
- Avoid bias
- Keep language clear and free of jargon unless defined
- Do not generate harmful, offensive, or inappropriate content
- Respect intellectual property rights
- Break complex information into digestible chunks
- Include practical examples where beneficial'''
    
    public static final String newSkillDescriptionInstructions =
'''Generate a detailed description for a skill based on this information: "{{ userEnteredText }}". 

# Requirements:
- Do not provide an introduction. 
- Use extensive Markdown formatting
- Avoid using the word "skill" in any headers
- Do not wrap sections with ```
- Maintain a professional yet engaging tone
'''
    
    public static final String newBadgeDescriptionInstructions =
'''Generate a detailed description for a badge that will be part of a training program based on this information: "{{ userEnteredText }}". 

# Requirements:
- Generate engaging overview and background information for the badge
- Focus on the badge as a whole, not individual skills
- Use Markdown formatting
- Do not wrap sections with ```
- Keep the description engaging and interesting
- Do not include titles or headers

# Badge Context:
- A badge represents a collection of related skills
- It's earned when all required skills are completed
- Badges serve as gamification elements to recognize achievement
- The description should provide an overview of the badge's purpose and value
- Focus on the badge's overall theme, not specific skills
'''
    
    public static final String newSubjectDescriptionInstructions =
'''Generate a detailed description for a training subject based on this information: "{{ userEnteredText }}". 

# Requirements:
- Create an engaging overview of the subject
- Focus on the subject as a whole, not individual skills
- Start directly with the content (no introduction needed)
- Use Markdown formatting
- Do not wrap sections with ```
- Keep the description engaging and informative
- Do not include titles or headers

# Subject Context:
- A Subject is a collection of related skills in a training program
- It helps organize skills into logical groupings
- The description should explain the subject's purpose and scope
- Focus on the overall subject, not specific skills
- Keep the tone professional yet engaging
'''
    
    public static final String newSkillGroupDescriptionInstructions =
'''Generate a detailed description for a Skill Group based on this information: "{{ userEnteredText }}". 

# Requirements:
- Create a clear and engaging overview of the Skill Group
- Focus on the group's collective purpose, not individual skills
- Start directly with the content (no introduction needed)
- Use Markdown formatting
- Do not wrap sections with ```
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
'''
    
    public static final String newProjectDescriptionInstructions =
'''Generate a detailed description for a training program based on this information: "{{ userEnteredText }}". 

# Requirements:
- Create an engaging overview of the training program
- Focus on the overall training, not individual skills
- Use Markdown formatting
- Do not wrap sections with ```
- Make the description compelling and informative
- Keep the tone professional yet engaging

# Training Context:
- A SkillTree training is a comprehensive gamified learning experience
- It includes skills, subjects and levels
- The description should provide an overview of the training's purpose and value
- Focus on the training as a whole, not specific skills
- Highlight the learning journey and outcomes
'''
    
    public static final String newQuizDescriptionInstructions =
'''Generate a detailed description for a Quiz based on this information: "{{ userEnteredText }}". 

# Requirements:
- Create a clear and engaging overview of the quiz
- Focus on the assessment's purpose and scope
- Use Markdown formatting
- Do not wrap sections with ```
- Do not generate sample questions

# Quiz Context:
- A knowledge assessment with multiple question types 
- Includes a passing score requirement
- Emphasize the learning outcomes and objectives 
- Avoid mentioning specific questions
'''
    
    public static final String newSurveyDescriptionInstructions =
'''Generate a detailed description for a Survey based on this information: "{{ userEnteredText }}". 

# Requirements:
- Create a clear and engaging introduction of the survey
- Focus on the survey's purpose and scope
- Use Markdown formatting
- Do not wrap sections with ```
- Do not generate sample questions
- Keep the description concise and informative
- Consider that description's audience is the participants of the survey

# Survey Context:
- A tool for gathering feedback and insights
- Used to collect opinions, preferences, or experiences
- Emphasize the value of participant responses
- Avoid mentioning specific questions
- Description will be displayed on the survey's splash page
'''
    
    public static final String existingDescriptionInstructions =
'''Modify current descriptions based on the following user instructions: "{{ userInstructions }}"
        
# Current Description:
{{ existingText }}

# Specific Requirements:
- First provide the new text without any comments or fields (such as "corrected text")
- At the very end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
{{ instructionsToKeepPlaceholders }}
'''

    public static final String followOnConvoInstructions =
            '''Apply the following instructions to this conversation: "{{ userEnteredText }}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.
'''

    public static final String newQuizInstructions = '''# Quiz Generation Task

## Skill Description:
"{{ existingDescription }}"

## Requirements:
- Generate {{ numQuestions }} questions, unless otherwise specified in the user instructions
- Include at least one of each question type
- Follow ALL rules strictly
- Double-check the number of correct answers for each question type

Please generate it based on the following user instructions: "{{ userEnteredText }}"


# Quiz Generation Instructions - STRICT RULES

## Output Format
Return a JSON object with exactly two properties:
1. `numQuestions`: Number of questions generated
2. `questions`: Array of question objects

## Question Type Rules

### 1. SingleChoice Questions
- MUST have `"questionType": "SingleChoice"`
- MUST have exactly 1 correct answer
- MUST have 3-5 answer options
- Example:
  ```json
  {
    "question": "What is 2+2?",
    "questionType": "SingleChoice",
    "answers": [
      {"answer": "3", "isCorrect": false},
      {"answer": "4", "isCorrect": true},  // Only ONE true
      {"answer": "5", "isCorrect": false}
    ]
  }
  ```

### 2. MultipleChoice Questions
- MUST have `"questionType": "MultipleChoice"`
- MUST have 2 or more correct answers
- MUST have between 3-5 answer options
- Example:
  ```json
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
  ```

### 3. TextInput Questions
- MUST have `"questionType": "TextInput"`
- MUST have empty answers array
- Example:
  ```json
  {
    "question": "What is the capital of France?",
    "questionType": "TextInput",
    "answers": []  // ALWAYS empty for TextInput
  }
  ```

### 4. Matching Questions
- MUST have `"questionType": "Matching"`
- ALL answers must be marked true for isCorrect
- ALL answers must have a multiPartAnswer with a term and and a value
- Example:
  ```json
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
  ```

## Validation Checklist
BEFORE finalizing the response, VERIFY each question:

1. For SingleChoice:
   - Exactly ONE answer has `"isCorrect": true`
   - All others have `"isCorrect": false`
   - ALL objects in the answers array should have the `"answer"` property and should not have the `"multiPartAnswer"` property
   - 3-5 answer options total

2. For MultipleChoice:
   - AT LEAST TWO answers have `"isCorrect": true`
   - All others have `"isCorrect": false`
   - ALL objects in the answers array should have the `"answer"` property and should not have the `"multiPartAnswer"` property
   - 3-5 answer options total

3. For TextInput:
   - `"answers": []` (empty array)
   - No answer objects

## Common Mistakes to AVOID:
- MultipleChoice with only 1 correct answer → INVALID
- SingleChoice with 0 or >1 correct answers → INVALID
- TextInput with answers array not empty → INVALID
- All objects in the answers array have the property `"multiPartAnswer"` with `"term"` and `"value"` properties, eg: `"multiPartAnswer": { "term": "carrot", "value": "orange" }`
- All multiPartAnswer.term and multiPartAnswer.value field values in the answers array must be unique.  Duplicate term or value fields → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for EACH question and verify:
MultipleChoice: 2 or more
- SingleChoice: Exactly 1
- TextInput: No answers


## IMPORTANT: Count the correct answers
For EACH question, before including it:
- Count the number of answers where `"isCorrect": true`
- If count >= 2 → then questionType *Must* be 'MultipleChoice\'
- If count == 1 then questionType *Must* be 'SingleChoice\'
- If questionType doesn't match, FIX IT


## Output:
Provide ONLY valid JSON in this exact format:
```json
{
  "numQuestions": {{ numQuestions }},
  "questions": [
    // Your questions here
  ]
}
```
'''
    
    public static final String updateQuizInstructions =
'''Apply the following instructions to this conversation: "{{ userEnteredText }}"

Here are the specific requirements:
- Make sure to regenerate the entire quiz again
- Add a new parameter to the returned json called "changes" that will contain a list of comments or suggestions.

## Output:
Provide ONLY valid JSON in this exact format:
```json
{
  "numQuestions": Number,
  "questions": [
    // Your questions here
  ],
  "changes": [
    // Your changes here
  ]
}
```
'''
    
    public static final String singleQuestionInstructionsMultipleChoice =
'''{{ intro }}{{ existingQuestionInfoString }}

## User's {{ usersRequestWord }}:
"{{ userInput }}"


# Question Type Rules (MultipleChoice)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- MUST have 2 or more correct answers
- MUST have between 3-5 answer options
- Ensure answers are plausible and relevant to the question.{{ additionalInstructions }}

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
      {"answer": "2", "isCorrect": true},   // First correct
      {"answer": "3", "isCorrect": true},   // Second correct
      {"answer": "4", "isCorrect": false},
      {"answer": "5", "isCorrect": true}    // Third correct
    ]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (MultipleChoice):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
2. For MultipleChoice:
   - AT LEAST TWO answers have `"isCorrect": true`
   - All others have `"isCorrect": false`
   - ALL objects in the answers array should have the `"answer"` property and should not have the `"multiPartAnswer"` property
   - 3-5 answer options total

## Common Mistakes to AVOID:
- MultipleChoice with only 1 correct answer → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:
MultipleChoice: 2 or more

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`
- If count >= 2 → then questionType *Must* be 'MultipleChoice\'
- If questionType doesn't match, FIX IT
'''

    public static final String singleQuestionInstructionsSingleChoice =
'''{{ intro }}{{ existingQuestionInfoString }}

## User's {{ usersRequestWord }}:
"{{ userInput }}"


# Question Type Rules (SingleChoice)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- MUST have exactly 1 correct answer
- MUST have 3-5 answer options
- Ensure answers are plausible and relevant to the question.{{ additionalInstructions }}

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
      {"answer": "3", "isCorrect": false},
      {"answer": "4", "isCorrect": true},  // Only ONE true
      {"answer": "5", "isCorrect": false}
    ]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (SingleChoice):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
1. For SingleChoice:
   - Exactly ONE answer has `"isCorrect": true`
   - All others have `"isCorrect": false`
   - ALL objects in the answers array should have the `"answer"` property and should not have the `"multiPartAnswer"` property
   - 3-5 answer options total

## Common Mistakes to AVOID:
- SingleChoice with 0 or >1 correct answers → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:
- SingleChoice: Exactly 1

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`
- If count == 1 then questionType *Must* be 'SingleChoice\'
- If questionType doesn't match, FIX IT
'''

    public static final String singleQuestionInstructionsTextInput =
            '''
{{ intro }}{{ existingQuestionInfoString }}

## User's {{ usersRequestWord }}:
"{{ userInput }}"


# Question Type Rules (TextInput)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- MUST have empty answers array
- Ensure answers are plausible and relevant to the question.{{ additionalInstructions }}

## Required Response Format:
### Question:
[Your generated question here]

### Answers:
[Your JSON array of answers here]

## Example Response:
### Question:
What are some popular chess openings?

### Answers:
[]  // ALWAYS empty for TextInput

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (TextInput):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
3. For TextInput:
   - `"answers": []` (empty array)
   - No answer objects

## Common Mistakes to AVOID:
- TextInput with answers array not empty → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:
- TextInput: No answers

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`

- If questionType doesn't match, FIX IT
'''

    public static final String singleQuestionInstructionsMatching =
'''{{ intro }}{{ existingQuestionInfoString }}

## User's {{ usersRequestWord }}:
"{{ userInput }}"


# Question Type Rules (Matching)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- ALL answers must be marked true for isCorrect
- ALL answers must have a multiPartAnswer with a term and and a value
- Ensure answers are plausible and relevant to the question.{{ additionalInstructions }}

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
      { "multiPartAnswer": { "term": "banana", "value": "yellow" }, "isCorrect": true },
      { "multiPartAnswer": { "term": "apple", "value": "red" }, "isCorrect": true },
      { "multiPartAnswer": { "term": "carrot", "value": "orange" }, "isCorrect": true },
  ]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (Matching):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
4. For Matching:
   - ALL objects in the answers array have the property `"isCorrect": true`
   - ALL objects in the answers array should have the `"multiPartAnswer"` property and should not have the `"answer"` property
   - ALL objects in the answers array have the property `"multiPartAnswer"` with `"term"` and `"value"` properties that are not repeated
   - ALL multiPartAnswers must not contain any duplicate term or duplicate value fields

## Common Mistakes to AVOID:
- All objects in the answers array have the property `"multiPartAnswer"` with `"term"` and `"value"` properties, eg: `"multiPartAnswer": { "term": "carrot", "value": "orange" }`
- All multiPartAnswer.term and multiPartAnswer.value field values in the answers array must be unique.  Duplicate term or value fields → INVALID

## Final Check 
Before responding, count the number of `"isCorrect": true` for the question and verify:

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`

- If questionType doesn't match, FIX IT
'''

    public static final String updateSingleQuestionTypeChangedToMultipleChoiceInstructions =
'''Apply the following instructions to this conversation: "{{ userEnteredText }}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.


## IMPORTANT: The Question Type has changed from `{{ previousQuestionType }}` to `MultipleChoice`.  
- **You must change the answers array to follow the new questionType (MultipleChoice) rules!**  
- Disregard the "Question Type Rules ({{ previousQuestionType }})" from earlier in the conversation and use only the following "Question Type Rules (MultipleChoice)" for this response.

# Question Generation Instructions - STRICT RULES

## Output Format
Return the answers as JSON array of objects that strictly adhere to the following rules based on the new questionType (MultipleChoice):


# Question Type Rules (MultipleChoice)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- MUST have 2 or more correct answers
- MUST have between 3-5 answer options
- Ensure answers are plausible and relevant to the question.

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
      {"answer": "2", "isCorrect": true},   // First correct
      {"answer": "3", "isCorrect": true},   // Second correct
      {"answer": "4", "isCorrect": false},
      {"answer": "5", "isCorrect": true}    // Third correct
    ]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (MultipleChoice):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
2. For MultipleChoice:
   - AT LEAST TWO answers have `"isCorrect": true`
   - All others have `"isCorrect": false`
   - ALL objects in the answers array should have the `"answer"` property and should not have the `"multiPartAnswer"` property
   - 3-5 answer options total

## Common Mistakes to AVOID:
- MultipleChoice with only 1 correct answer → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:
MultipleChoice: 2 or more

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`
- If count >= 2 → then questionType *Must* be 'MultipleChoice\'
- If questionType doesn't match, FIX IT
'''
    public static final String updateSingleQuestionTypeChangedToSingleChoiceInstructions =
'''Apply the following instructions to this conversation: "{{ userEnteredText }}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.


## IMPORTANT: The Question Type has changed from `{{ previousQuestionType }}` to `SingleChoice`.  
- **You must change the answers array to follow the new questionType (SingleChoice) rules!**  
- Disregard the "Question Type Rules ({{ previousQuestionType }})" from earlier in the conversation and use only the following "Question Type Rules (SingleChoice)" for this response.

# Question Generation Instructions - STRICT RULES

## Output Format
Return the answers as JSON array of objects that strictly adhere to the following rules based on the new questionType (SingleChoice):


# Question Type Rules (SingleChoice)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- MUST have exactly 1 correct answer
- MUST have 3-5 answer options
- Ensure answers are plausible and relevant to the question.

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
      {"answer": "3", "isCorrect": false},
      {"answer": "4", "isCorrect": true},  // Only ONE true
      {"answer": "5", "isCorrect": false}
    ]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (SingleChoice):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
1. For SingleChoice:
   - Exactly ONE answer has `"isCorrect": true`
   - All others have `"isCorrect": false`
   - ALL objects in the answers array should have the `"answer"` property and should not have the `"multiPartAnswer"` property
   - 3-5 answer options total

## Common Mistakes to AVOID:
- SingleChoice with 0 or >1 correct answers → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:
- SingleChoice: Exactly 1

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`
- If count == 1 then questionType *Must* be 'SingleChoice\'
- If questionType doesn't match, FIX IT
'''
    public static final String updateSingleQuestionTypeChangedToTextInputInstructions =
'''Apply the following instructions to this conversation: "{{ userEnteredText }}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.


## IMPORTANT: The Question Type has changed from `{{ previousQuestionType }}` to `TextInput`.  
- **You must change the answers array to follow the new questionType (TextInput) rules!**  
- Disregard the "Question Type Rules ({{ previousQuestionType }})" from earlier in the conversation and use only the following "Question Type Rules (TextInput)" for this response.

# Question Generation Instructions - STRICT RULES

## Output Format
Return the answers as JSON array of objects that strictly adhere to the following rules based on the new questionType (TextInput):


# Question Type Rules (TextInput)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- MUST have empty answers array
- Ensure answers are plausible and relevant to the question.

## Required Response Format:
### Question:
[Your generated question here]

### Answers:
[Your JSON array of answers here]

## Example Response:
### Question:
What are some popular chess openings?

### Answers:
[]  // ALWAYS empty for TextInput

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (TextInput):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
3. For TextInput:
   - `"answers": []` (empty array)
   - No answer objects

## Common Mistakes to AVOID:
- TextInput with answers array not empty → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:
- TextInput: No answers

## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`

- If questionType doesn't match, FIX IT
'''
    public static final String updateSingleQuestionTypeChangedToMatchingInstructions =
'''Apply the following instructions to this conversation: "{{ userEnteredText }}"

Here are the specific requirements:
- Make sure to regenerate the entire content again
- At the end create a new section with the title of "Here is what was changed" - then list any comments or suggestions.


## IMPORTANT: The Question Type has changed from `{{ previousQuestionType }}` to `Matching`.  
- **You must change the answers array to follow the new questionType (Matching) rules!**  
- Disregard the "Question Type Rules ({{ previousQuestionType }})" from earlier in the conversation and use only the following "Question Type Rules (Matching)" for this response.

# Question Generation Instructions - STRICT RULES

## Output Format
Return the answers as JSON array of objects that strictly adhere to the following rules based on the new questionType (Matching):


# Question Type Rules (Matching)

## Instructions:
- First, generate a clear and concise question based on the user's description.
- ALL answers must be marked true for isCorrect
- ALL answers must have a multiPartAnswer with a term and and a value
- Ensure answers are plausible and relevant to the question.

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
      { "multiPartAnswer": { "term": "banana", "value": "yellow" }, "isCorrect": true },
      { "multiPartAnswer": { "term": "apple", "value": "red" }, "isCorrect": true },
      { "multiPartAnswer": { "term": "carrot", "value": "orange" }, "isCorrect": true },
  ]

## Important Notes:
- Start with "### Question:" on its own line
- Follow with the question text
- Add a blank line
- Then "### Answers:" on its own line
- Follow with the JSON array
- The JSON must be valid and properly formatted
- The JSON array must strictly adhere to the Question Type Rules (Matching):
- Include explanations in the answers if the question is complex
- Do not include any other text outside these sections
- Do not number answers

## Validation Checklist
BEFORE finalizing the response, that the question:
4. For Matching:
   - ALL objects in the answers array have the property `"isCorrect": true`
   - ALL objects in the answers array should have the `"multiPartAnswer"` property and should not have the `"answer"` property
   - ALL objects in the answers array have the property `"multiPartAnswer"` with `"term"` and `"value"` properties that are not repeated
   - ALL multiPartAnswers must not contain any duplicate term or duplicate value fields

## Common Mistakes to AVOID:
- All objects in the answers array have the property `"multiPartAnswer"` with `"term"` and `"value"` properties, eg: `"multiPartAnswer": { "term": "carrot", "value": "orange" }`
- All multiPartAnswer.term and multiPartAnswer.value field values in the answers array must be unique.  Duplicate term or value fields → INVALID

## Final Check
Before responding, count the number of `"isCorrect": true` for the question and verify:


## IMPORTANT: Count the correct answers
- Count the number of answers where `"isCorrect": true`

- If questionType doesn't match, FIX IT
'''


    public static final String textInputQuestionGradingInstructions =
'''# LLM Quiz Answer Grading Prompt

## System Prompt
You are an expert educational assessment AI designed to evaluate free-form text answers to quiz questions. Your role is to compare a student's answer against the provided correct answer and determine if the student's response demonstrates sufficient understanding and accuracy.

## Input Data
- **Question**: {{ question }}
- **Student's Answer**: {{ studentAnswer }}
- **Correct Answer**: {{ correctAnswer }}
- **Required Confidence Level**: {{ minimumConfidenceLevel }}

## Task Instructions
Evaluate the student's answer by comparing it to the correct answer. Consider:

1. **Content Accuracy**: Does the answer contain the key concepts, facts, and information present in the correct answer?
2. **Understanding**: Does the student demonstrate comprehension of the underlying concepts?
3. **Completeness**: Are the essential elements of the correct answer present?
4. **Clarity**: Is the answer coherent and well-expressed?
5. **Equivalent Meaning**: Even if worded differently, does the answer convey the same meaning as the correct answer?

## Grading Criteria
- **Correct**: The answer demonstrates sufficient understanding and contains the essential elements of the correct answer, even if phrased differently.
- **Incorrect**: The answer lacks key information, contains significant errors, or fails to demonstrate understanding of the core concepts.

## Confidence Level Assessment
Rate your confidence in the grading decision on a scale of 0-100:
- **90-100**: Very high confidence - answer clearly matches criteria
- **75-89**: High confidence - answer strongly matches criteria with minor ambiguities
- **60-74**: Moderate confidence - answer partially matches but has some concerns
- **40-59**: Low confidence - answer has significant issues or ambiguities
- **0-39**: Very low confidence - answer is clearly incorrect or incomprehensible

## Final Decision Rule
The answer is considered **correct** only if:
1. The content meets the correctness criteria AND
2. Your confidence level is equal to or greater than the required confidence level ({{ minimumConfidenceLevel }})

## Response Format
Respond with a JSON object containing exactly these fields:

```json
{
  "isCorrect": true/false,
  "confidenceLevel": 0-100,
  "gradingDecisionReason": "Detailed explanation of your reasoning, including comparison of key points, assessment of understanding, and justification for the confidence level and final decision."
}
```

## Example Response
```json
{
  "isCorrect": true,
  "confidenceLevel": 85,
  "gradingDecisionReason": "The student's answer correctly identifies the main concept of photosynthesis as the process by which plants convert sunlight into energy, mentioning chlorophyll and glucose production. While the student doesn't explicitly mention carbon dioxide and water as reactants, the core understanding is demonstrated. The confidence level is high because the essential elements are present, though some details are omitted."
}
```

## Important Notes
- Be objective and fair in your assessment
- Focus on conceptual understanding rather than exact wording
- Provide clear, specific reasoning for your decision
- Ensure your confidence level accurately reflects your certainty in the grading decision
- The response must be valid JSON that can be parsed programmatically
'''

            }
