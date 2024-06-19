/*
 * Copyright 2020 SkillTree
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

describe('Quiz Read Only Role Tests', () => {
    const userName = 'readOnly@skills.org'
    const password = 'password'

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.resetEmail();

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.register(userName, password);
        cy.logout();
        cy.login(userName, password);

        cy.logout();

        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', `/admin/projects/proj1/users/${userName}/roles/ROLE_PROJECT_ADMIN`);

        cy.logout();
        cy.login(userName, password);

        cy.createQuizDef(2);
        cy.createQuizQuestionDef(2, 1);
        cy.createQuizQuestionDef(2, 2);
        cy.createQuizQuestionDef(2, 3);
        cy.createSkill(1, 1, 2, { selfReportingType: 'Quiz', quizId: 'quiz2',  pointIncrement: '150', numPerformToCompletion: 1 });

    });

    after(() => {
        Cypress.env('disableResetDb', false);
    });

    beforeEach(() => {
        cy.logout();
        cy.login(userName, password);
    });

    it('quiz page - read only has no mutation controls', function () {
        Cypress.Commands.add("runCheck", (assertChainPrepend) => {
            const chainerPrepend = assertChainPrepend ? assertChainPrepend : '';

            cy.get('[data-cy="nav-Access"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Settings"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="pageHeader"] [data-cy="editQuizButton"]').should(`${chainerPrepend}exist`)
            // cy.get('[data-cy="pageHeader"] [data-cy="shareQuizBtn"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="pageHeader"] [data-cy="quizPreview"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="editQuestionButton_1"').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="editQuestionButton_2"').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="editQuestionButton_3"').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="deleteQuestionButton_1"').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="deleteQuestionButton_2"').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="deleteQuestionButton_3"').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="sortControlHandle"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="questionDisplayCard-3"] [data-cy="sortControlHandle"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="btn_Questions"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="newQuestionOnBottomBtn"]').should(`${chainerPrepend}exist`)
        })

        cy.visit(`/administrator/projects/proj1/subjects/subj1/`);
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Self Report"]').click()
        cy.get('[data-cy="selfReportCell-skill2-quiz"]').contains('This is quiz 2').click()
        cy.get('[data-cy="pageHeader"] [data-cy="userRole"]').should('have.text', 'Admin')
        cy.runCheck()

        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.get('[data-cy="manageBtn_subj1"]').click()
        cy.get('[data-cy="selfReportCell-skill1-quiz"]').contains('This is quiz 1').click()
        cy.get('[data-cy="pageHeader"] [data-cy="userRole"]').should('have.text', 'Read Only')
        cy.runCheck('not.')

        // same checks but navigate directly to the page
        cy.visit(`/administrator/quizzes/quiz2/`);
        cy.get('[data-cy="pageHeader"] [data-cy="userRole"]').should('have.text', 'Admin')
        cy.runCheck()

        cy.visit(`/administrator/quizzes/quiz1/`);
        cy.get('[data-cy="pageHeader"] [data-cy="userRole"]').should('have.text', 'Read Only')
        cy.runCheck('not.')
    });
})