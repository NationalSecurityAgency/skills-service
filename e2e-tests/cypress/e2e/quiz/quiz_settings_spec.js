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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Quiz Metrics Tests', () => {


    it('quiz setting: configure question passing requirement', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')

        cy.get('[data-cy="quizPassingSelector"]').select('2')
        cy.get('[data-cy="quizPassingSelector"]').should('have.value', '2')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="quizPassingSelector"]').should('have.value', '2')

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="quizPassingSelector"]').should('have.value', '2')

        cy.get('[data-cy="quizPassingSelector"]').select('-1')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')
        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="quizPassingSelector"]').should('have.value', '-1')
    });

    it('quiz setting: configure number of attempts', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="numAttemptsInput"]').should('not.exist')

        cy.get('[data-cy="unlimitedAttemptsSwitch"]').click({force: true})

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')
        cy.get('[data-cy="numAttemptsInput"]').should('have.value', '3')
        cy.get('[data-cy="numAttemptsInput"]').type('0')

        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="numAttemptsInput"]').should('have.value', '30')

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="numAttemptsInput"]').should('have.value', '30')

        cy.get('[data-cy="unlimitedAttemptsSwitch"]').click({force: true})
        cy.get('[data-cy="numAttemptsInput"]').should('not.exist')

        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="numAttemptsInput"]').should('not.exist')

    });

    it('quiz setting: configure number of questions', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="numAttemptsInput"]').should('not.exist')

        cy.get('[data-cy="quizNumQuestions"]').select(2);

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="quizPassingSelector"]').select(3);
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="quizPassingSelector"]').select(2);
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

    });

    it('surveys do not have settings, at least yet...', function () {
        cy.createQuizDef(1, { type: 'Survey' });
        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="noSettingsAvailable"]')
    });

});
