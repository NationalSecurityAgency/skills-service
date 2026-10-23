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

describe('Copy selected quiz questions', () => {
    const questionCard = (number) => `[data-cy="questionDisplayCard-${number}"]`;
    const checkbox = (number) => `${questionCard(number)} [data-cy="selectSkillCheckbox"]`;

    const selectQuestions = (...numbers) => {
        cy.get('[data-cy="toggleSelectionMode"]').click();
        numbers.forEach((number) => cy.get(checkbox(number)).click());
        cy.get('[data-cy="questionActionsBtn"]').should('be.enabled');
    };

    const openCopyDialog = () => {
        cy.get('[data-cy="questionActionsBtn"]').click();
        cy.contains('.p-menu-item-link', 'Copy to Another Quiz').click();
        cy.get('[data-cy="quizzes-to-copy"]').should('be.visible');
    };

    const selectDestination = (name) => {
        cy.get('[data-cy="quizzes-to-copy"]').click();
        cy.contains('.p-select-option', name).click();
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    };

    const enableDivineDragonForAdmin = () => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();
            cy.loginAsAdminUser();
        });
    };

    it('copies only selected questions, in source order, after existing destination questions', () => {
        cy.createQuizDef(1);
        cy.createQuizDef(2);
        [1, 2, 3, 4].forEach((number) => cy.createQuizQuestionDef(1, number));
        cy.createQuizQuestionDef(2, 5);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(4)).should('be.visible');

        selectQuestions(4, 2);
        cy.get(`${checkbox(1)} input`).should('not.be.checked');
        cy.get(`${checkbox(2)} input`).should('be.checked');
        cy.get(`${checkbox(3)} input`).should('not.be.checked');
        cy.get(`${checkbox(4)} input`).should('be.checked');
        openCopyDialog();
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.contains('This will copy 2 questions').should('be.visible');
        selectDestination('This is quiz 2');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.contains('Successfully copied 2 questions').should('be.visible');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="questionActionsBtn"]').should('be.disabled');
        cy.get(`${checkbox(2)} input`).should('not.be.checked');
        cy.get(`${checkbox(4)} input`).should('not.be.checked');
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '4');

        cy.visit('/administrator/quizzes/quiz2');
        cy.get(questionCard(1)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 5');
        cy.get(questionCard(2)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 2');
        cy.get(questionCard(3)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 4');
        cy.get(questionCard(4)).should('not.exist');
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '3');
        cy.get(`${questionCard(2)} [data-cy="answer-1_displayText"]`).should('have.text', 'Question 2 - Second Answer');
    });

    it('copies all selected questions to a survey without offering quizzes as destinations', () => {
        cy.createSurveyDef(1);
        cy.createSurveyDef(2);
        cy.createQuizDef(3);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(2)).should('be.visible');

        cy.get('[data-cy="toggleSelectionMode"]').click();
        cy.get('[data-cy="selectAllQuestionsBtn"]').click();
        cy.get(`${checkbox(1)} input`).should('be.checked');
        cy.get(`${checkbox(2)} input`).should('be.checked');
        openCopyDialog();
        cy.get('[data-cy="quizzes-to-copy"]').click();
        cy.contains('.p-select-option', 'This is survey 2').should('be.visible');
        cy.contains('.p-select-option', 'This is quiz 3').should('not.exist');
        cy.contains('.p-select-option', 'This is survey 2').click();
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
        cy.contains('Successfully copied 2 questions').should('be.visible');
        cy.get('[data-cy="closeDialogBtn"]').click();

        cy.visit('/administrator/quizzes/quiz2');
        cy.get(questionCard(1)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 1');
        cy.get(questionCard(2)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 2');
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '2');
    });

    it('copies selected questions from one protected quiz to another protected quiz', () => {
        enableDivineDragonForAdmin();
        cy.createQuizDef(1, { enableProtectedUserCommunity: true });
        cy.createQuizDef(2, { enableProtectedUserCommunity: true });
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(2)).should('be.visible');

        selectQuestions(2);
        openCopyDialog();
        selectDestination('This is quiz 2');
        cy.get('[data-cy="validationFailedMsg"]').should('not.exist');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.contains('Successfully copied 1 question').should('be.visible');
        cy.get('[data-cy="closeDialogBtn"]').click();

        cy.visit('/administrator/quizzes/quiz2');
        cy.get(questionCard(1)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 2');
        cy.get(questionCard(2)).should('not.exist');
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '1');
    });

    it('does not copy selected questions from a protected quiz to an unrestricted quiz', () => {
        enableDivineDragonForAdmin();
        cy.createQuizDef(1, { enableProtectedUserCommunity: true });
        cy.createQuizDef(2);
        cy.createQuizQuestionDef(1, 1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(1)).should('be.visible');

        selectQuestions(1);
        openCopyDialog();
        cy.get('[data-cy="quizzes-to-copy"]').click();
        cy.contains('.p-select-option', 'This is quiz 2').click();
        cy.get('[data-cy="validationFailedMsg"]')
            .should('contain.text', 'Can not copy questions from Divine Dragon quizzes to All Dragons quizzes.');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').click();

        cy.visit('/administrator/quizzes/quiz2');
        cy.get('[data-cy="noQuestionsYet"]').should('be.visible');
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0');
    });

    it('keeps the selected questions when copying fails and succeeds on retry', () => {
        cy.createQuizDef(1);
        cy.createQuizDef(2);
        cy.createQuizQuestionDef(1, 1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(1)).should('be.visible');
        selectQuestions(1);
        openCopyDialog();
        selectDestination('This is quiz 2');

        let attempts = 0;
        cy.intercept('POST', '/admin/quiz-definitions/quiz1/questions/copy/quiz2', (req) => {
            attempts += 1;
            if (attempts === 1) {
                req.reply({ statusCode: 503, body: { explanation: 'Copy temporarily unavailable' } });
            } else {
                req.continue();
            }
        });

        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="copyQuestionsFailedMsg"]').should('contain.text', 'Copy temporarily unavailable');
        cy.contains('This will copy 1 question').should('be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
        cy.contains('Successfully copied 1 question').should('be.visible');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="questionActionsBtn"]').should('be.disabled');

        cy.visit('/administrator/quizzes/quiz2');
        cy.get(questionCard(1)).find('[data-cy="questionDisplayText"]').should('contain.text', 'This is a question # 1');
        cy.get(questionCard(2)).should('not.exist');
    });

    it('deselects questions when cancelling the copy dialog', () => {
        cy.createQuizDef(1);
        cy.createQuizDef(2);
        cy.createQuizQuestionDef(1, 1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(1)).should('be.visible');
        selectQuestions(1);
        openCopyDialog();
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="quizzes-to-copy"]').should('not.exist');
        cy.get(`${checkbox(1)} input`).should('not.be.checked');
        cy.get('[data-cy="questionActionsBtn"]').should('be.disabled');
    });

    it('supports keyboard selection, action menu navigation, and returning focus after closing', () => {
        cy.createQuizDef(1);
        cy.createQuizDef(2);
        cy.createQuizQuestionDef(1, 1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get(questionCard(1)).should('be.visible');

        cy.get('#toggleSelectionMode').focus().should('have.focus').type(' ');
        cy.get('#toggleSelectionMode').should('be.checked');
        cy.get(`${checkbox(1)} input`).focus().should('have.focus').type(' ');
        cy.get(`${checkbox(1)} input`).should('be.checked');

        cy.get('[data-cy="questionActionsBtn"]').focus().type('{enter}');
        cy.contains('.p-menu-item-link', 'Copy to Another Quiz').should('be.visible');
        cy.contains('.p-menu-item-link', 'Copy to Another Quiz').focus().should('have.focus').realPress('Tab');
        cy.contains('.p-menu-item-link', 'Copy to Another Quiz').should('not.have.focus');

        cy.get('[data-cy="questionActionsBtn"]').focus().type('{enter}');
        cy.contains('.p-menu-item-link', 'Copy to Another Quiz').should('be.visible').click();
        cy.get('[data-cy="quizzes-to-copy"]').should('be.visible');
        cy.focused().closest('.p-dialog').should('be.visible');

        cy.get('label[for="quizzesToCopy"]').click();
        cy.get('[data-cy="quizzes-to-copy"]').should('have.class', 'p-focus');
        cy.get('[data-cy="quizzes-to-copy"]').click();
        cy.contains('.p-select-option', 'This is quiz 2').click();
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="quizzes-to-copy"]').should('not.exist');
        cy.get('[data-cy="questionActionsBtn"]').should('be.disabled');
    });
});
