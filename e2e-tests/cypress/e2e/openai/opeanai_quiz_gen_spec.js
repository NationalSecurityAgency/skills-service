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

import {
    newQuizWelcomeMsg,
    chessGenValue,
    existingDescWelcomeMsg,
    completedMsg,
    gotStartedMsg,
    newQuizGeneratingMsg
}
    from './openai_helper_commands'


const validateAnswers = (questionNum, expected, matching = false) => {
    expected.forEach((answer, index) => {
        if (matching) {
            cy.get(`[data-cy="questionDisplayCard-${questionNum}"] [data-cy="question-${questionNum}-answer-${index}-term"]`).should('have.text', answer.term)
            cy.get(`[data-cy="questionDisplayCard-${questionNum}"] [data-cy="question-${questionNum}-answer-${index}-value"]`).should('have.text', answer.value)
        } else {
            cy.get(`[data-cy="questionDisplayCard-${questionNum}"] [data-cy="answer-${index}_displayText"]`).should('have.text', answer.text)
            answer.isCorrect ? cy.get(`[data-cy="questionDisplayCard-${questionNum}"] [data-cy="answerDisplay-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]`) : cy.get(`[data-cy="questionDisplayCard-${questionNum}"] [data-cy="answerDisplay-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="notSelected"]`)
        }
    })
}

describe('Generate Quiz Tests', () => {

    it('generate a new quiz for a skill', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: chessGenValue, numPerformToCompletion: 1 })

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="skillOverviewDescription"]').contains(chessGenValue)

        cy.get('[data-cy="generateQuizBtn"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newQuizWelcomeMsg)

        cy.get('[data-cy="instructionsInput"]').type('Quiz chess{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Quiz chess')

        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(newQuizGeneratingMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('Which piece can move diagonally in any direction?')
        let expectedAnswers = [
            { text: 'Pawn', isCorrect: false },
            { text: 'Rook', isCorrect: false },
            { text: 'Knight', isCorrect: false },
            { text: 'Bishop', isCorrect: true },
        ]
        validateAnswers(1, expectedAnswers)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('Which piece can jump over other pieces?')
        expectedAnswers = [
            { text: 'Pawn', isCorrect: false },
            { text: 'Knight', isCorrect: true },
            { text: 'Bishop', isCorrect: false },
            { text: 'Rook', isCorrect: false },
        ]
        validateAnswers(2, expectedAnswers)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('Match the pieces with their movements:')
        expectedAnswers = [
            { term: 'King', value: 'Moves one square in any direction' },
            { term: 'Queen', value: 'Can move any number of squares along a rank, file, or diagonal' },
            { term: 'Rook', value: 'Can move any number of squares along a rank or file' },
            { term: 'Bishop', value: 'Can move any number of squares diagonally' },
            { term: 'Knight', value: 'Moves in an L-shape: two squares in one direction (horizontally or vertically) and then one square perpendicular to that' },
        ]
        validateAnswers(3, expectedAnswers, true)
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="selfReportMediaCard"]').contains('Self Report: Quiz')
        cy.get('[data-cy="selfReportMediaCard"]').contains('Users can self report this skill and points will be awarded after the Very Great Skill 1 Quiz Quiz is passed!')
        cy.get('[data-cy="linkToQuiz"]')
          .should('have.attr', 'href')
          .and('include', '/administrator/quizzes/skill1Quiz');
        cy.get('[data-cy="buttonToQuiz"]')
          .should('have.attr', 'href')
          .and('include', '/administrator/quizzes/skill1Quiz');
        cy.get('[data-cy="buttonToQuiz"]').contains('View Quiz')
    });

    it('generate a new quiz for a skill, modify questions inline', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: chessGenValue, numPerformToCompletion: 1 })

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="skillOverviewDescription"]').contains(chessGenValue)

        cy.get('[data-cy="generateQuizBtn"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newQuizWelcomeMsg)

        cy.get('[data-cy="instructionsInput"]').type('Quiz chess{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Quiz chess')

        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(newQuizGeneratingMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('Which piece can move diagonally in any direction?')
        let expectedAnswers = [
            { text: 'Pawn', isCorrect: false },
            { text: 'Rook', isCorrect: false },
            { text: 'Knight', isCorrect: false },
            { text: 'Bishop', isCorrect: true },
        ]
        validateAnswers(1, expectedAnswers)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('Which piece can jump over other pieces?')
        expectedAnswers = [
            { text: 'Pawn', isCorrect: false },
            { text: 'Knight', isCorrect: true },
            { text: 'Bishop', isCorrect: false },
            { text: 'Rook', isCorrect: false },
        ]
        validateAnswers(2, expectedAnswers)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('Match the pieces with their movements:')
        expectedAnswers = [
            { term: 'King', value: 'Moves one square in any direction' },
            { term: 'Queen', value: 'Can move any number of squares along a rank, file, or diagonal' },
            { term: 'Rook', value: 'Can move any number of squares along a rank or file' },
            { term: 'Bishop', value: 'Can move any number of squares diagonally' },
            { term: 'Knight', value: 'Moves in an L-shape: two squares in one direction (horizontally or vertically) and then one square perpendicular to that' },
        ]
        validateAnswers(3, expectedAnswers, true)

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="markdownEditorInput"]').should('not.be.visible')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="markdownEditorInput"]').should('not.be.visible')
        cy.get('[data-cy="enabledEitQuestionsBtn-2"]').should('be.enabled')
        cy.get('[data-cy="enabledEitQuestionsBtn-2"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.typeInMarkdownEditor('[data-cy="questionDisplayCard-1"] [data-cy="markdownEditorInput"]', '{selectall}this is a new question 1');
        cy.validateMarkdownEditorText('[data-cy="questionDisplayCard-1"] [data-cy="markdownEditorInput"]', ['this is a new question 1']);
        cy.typeInMarkdownEditor('[data-cy="questionDisplayCard-2"] [data-cy="markdownEditorInput"]', '{selectall}this is a new question 2');
        cy.validateMarkdownEditorText('[data-cy="questionDisplayCard-2"] [data-cy="markdownEditorInput"]', ['this is a new question 2']);
        cy.typeInMarkdownEditor('[data-cy="questionDisplayCard-3"] [data-cy="markdownEditorInput"]', '{selectall}this is a new question 3');
        cy.validateMarkdownEditorText('[data-cy="questionDisplayCard-3"] [data-cy="markdownEditorInput"]', ['this is a new question 3']);
        cy.wait(500) //wait for debounce

        cy.intercept('POST', '/admin/quiz-definitions/skill1Quiz/create-question', (req) => {
            if (req.body && req.body.question) {
                expect(req.body.question).to.match(/^this is a new question ([123])/)
            }
            req.continue();
        }).as('createQuestion');
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.wait('@createQuestion')
        cy.wait('@createQuestion')
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="selfReportMediaCard"]').contains('Self Report: Quiz')
        cy.get('[data-cy="selfReportMediaCard"]').contains('Users can self report this skill and points will be awarded after the Very Great Skill 1 Quiz Quiz is passed!')
        cy.get('[data-cy="linkToQuiz"]')
          .should('have.attr', 'href')
          .and('include', '/administrator/quizzes/skill1Quiz');
        cy.get('[data-cy="buttonToQuiz"]')
          .should('have.attr', 'href')
          .and('include', '/administrator/quizzes/skill1Quiz');
        cy.get('[data-cy="buttonToQuiz"]').contains('View Quiz')
    });

    it('apply prefix after generating a new quiz for a skill, Add Prefix Then Use', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                res.send(conf);
            });
        }).as('getConfig');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: chessGenValue, numPerformToCompletion: 1 })

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="skillOverviewDescription"]').contains(chessGenValue)

        cy.get('[data-cy="generateQuizBtn"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newQuizWelcomeMsg)

        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('invalidquiz{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('invalidquiz')

        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(newQuizGeneratingMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.validateMarkdownViewerText('[data-cy="aiMsg-2"] [data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]', [
            'Has jabberwocky',
            'clean line',
            'jabberwocky again',
            'and nothing here'
        ])

        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="useGenValueBtn-2"]').should('be.disabled')
        cy.get(`[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="addPrefixBtn"]`).should('be.enabled')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.get(`[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="prefixSelect"]`).click()
        const options = ['A', 'B', 'C', 'D']
        options.forEach((val) => {
            cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${val}) "]`)
        })
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${options[2]}) "]`).click()

        // Intercept and modify the create-question requests
        cy.intercept('POST', '/admin/quiz-definitions/skill1Quiz/create-question', (req) => {
          if (req.body && req.body.question) {
              const question = req.body.question
              if (question.includes('jabberwock')) {
                  expect(question).to.match(/^\(C\) /)
                  req.body.question = question.replace(/jabberwocky/gi, '');
              }
          }
          req.continue();
        }).as('createQuestion');


        cy.get(`[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="addPrefixBtn"]`).should('be.enabled');
        cy.get(`[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="addPrefixBtn"]`).click()

        // Wait for the request to complete
        cy.wait('@createQuestion');

        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`[data-cy="aiMsg-2"] [data-cy="finalSegment"] [data-cy="addPrefixBtn"]`).should('not.exist')
    });

    it('conversation to generate a new quiz for a skill', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: chessGenValue, numPerformToCompletion: 1 })

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="skillOverviewDescription"]').contains(chessGenValue)

        cy.get('[data-cy="generateQuizBtn"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newQuizWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.intercept('POST', '/openai/chat', (req) => {
            const requestBody = req.body;
            req.reply();

            req.on('response', (res) => {
                expect(requestBody.messages).to.have.length(1);
                expect(requestBody.messages[0].role).to.equal('User')
                expect(requestBody.messages[0].content).to.contain('Generate 5 - 10 questions, unless otherwise specified in the user instructions');
                expect(requestBody.messages[0].content).to.contain('Quiz chess');
            });
        }).as('genQuiz1');
        cy.get('[data-cy="instructionsInput"]').type('Quiz chess{enter}')
        cy.get('@genQuiz1')

        cy.get('[data-cy="userMsg-1"]').contains('Quiz chess')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(newQuizGeneratingMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')
        cy.get('[data-cy="enabledEitQuestionsBtn-2"]').should('be.enabled')
        cy.get('[data-cy="useGenValueBtn-4"]').should('not.exist')
        cy.get('[data-cy="enabledEitQuestionsBtn-4"]').should('not.exist')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.intercept('POST', '/openai/chat', (req) => {
            const requestBody = req.body;
            req.reply();

            req.on('response', (res) => {
                expect(requestBody.messages).to.have.length(3);
                expect(requestBody.messages[0].role).to.equal('User')
                expect(requestBody.messages[0].content).to.contain('Generate 5 - 10 questions, unless otherwise specified in the user instructions');
                expect(requestBody.messages[0].content).to.contain('Quiz chess');

                expect(requestBody.messages[1].role).to.equal('Assistant')
                expect(requestBody.messages[1].content).to.contain('Which piece can move diagonally in any direction?')

                expect(requestBody.messages[2].role).to.equal('User')
                expect(requestBody.messages[2].content).to.contain('Apply the following instructions to this conversation')


            });
        }).as('genQuiz2');
        cy.get('[data-cy="instructionsInput"]').type('Quiz chess{enter}')
        cy.get('@genQuiz2')

        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get('[data-cy="useGenValueBtn-4"]').should('be.enabled')
        cy.get('[data-cy="enabledEitQuestionsBtn-2"]').should('not.exist')
        cy.get('[data-cy="enabledEitQuestionsBtn-4"]').should('be.enabled')
    });
});


