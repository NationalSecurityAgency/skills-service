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

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('2').click();
        cy.get('[data-cy="quizPassingSelector"]').contains('2')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="quizPassingSelector"]').contains('2')

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="quizPassingSelector"]').contains('2')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('ALL').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')
        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="quizPassingSelector"]').contains('ALL')
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

        cy.get('[data-cy="unlimitedAttemptsSwitch"] [role="switch"]').click({force: true})

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')
        cy.get('[data-cy="numAttemptsInput"] [data-pc-name="pcinputtext"]').should('have.value', '3')
        cy.get('[data-cy="numAttemptsInput"]').type('0')

        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="numAttemptsInput"] [data-pc-name="pcinputtext"]').should('have.value', '30')

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="numAttemptsInput"] [data-pc-name="pcinputtext"]').should('have.value', '30')

        cy.get('[data-cy="unlimitedAttemptsSwitch"] [role="switch"]').click({force: true})
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

        cy.get('[data-cy="quizNumQuestions"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('2').click();

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('3').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('2').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

    });

    it('quiz setting: configure number of questions and num to pass at 10 or greater', function () {
        cy.createQuizDef(1);
        for(var x = 1; x < 15; x++) {
            cy.createQuizQuestionDef(1, x);
        }

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="numAttemptsInput"]').should('not.exist')

        cy.get('[data-cy="quizNumQuestions"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('9 Questions').click();

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('10 Correct').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('9 Correct').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="quizNumQuestions"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('12 Questions').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('13 Correct').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('4 Correct').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="quizPassingSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('14 Correct').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="quizNumQuestions"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('14 Questions').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

    });

    it('quiz setting: configure quiz time limit', function () {
        cy.createQuizDef(1);
        for(var x = 1; x < 3; x++) {
            cy.createQuizQuestionDef(1, x);
        }

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="timeLimitHoursInput"]').should('not.exist')
        cy.get('[data-cy="timeLimitMinutesInput"]').should('not.exist')

        cy.get('[data-cy="unlimitedTimeSwitch"] [role="switch"]').click({force: true});
        cy.get('[data-cy="timeLimitHoursInput"]').should('exist')
        cy.get('[data-cy="timeLimitMinutesInput"]').should('exist')

        cy.get('[data-cy="timeLimitHoursInput"] [data-pc-name="pcinputtext"]').should('have.value', '1')
        cy.get('[data-cy="timeLimitMinutesInput"] [data-pc-name="pcinputtext"]').should('have.value', '0')

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')

        cy.get('[data-cy="timeLimitHoursInput"]').type('555');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="timeLimitHoursInput"]').type('{selectall}6');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="timeLimitMinutesInput"]').type('555');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="timeLimitMinutesInput"]').type('{selectall}30');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
    });

    it('quiz setting: only incorrect questions on retakes', function () {
        cy.createQuizDef(1);
        for(var x = 1; x < 3; x++) {
            cy.createQuizQuestionDef(1, x);
        }

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="retakeIncorrectQuestionsSwitch"] [role="switch"]').click({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')
        cy.get('[data-cy="saveSettingsBtn"]').click()

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="retakeIncorrectQuestionsSwitch"] [role="switch"]').should('be.checked')
    });

    it('quiz setting: show description on quiz', function () {
        cy.createQuizDef(1);
        for(var x = 1; x < 3; x++) {
            cy.createQuizQuestionDef(1, x);
        }

        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="quizDescription"]').should('not.exist')

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="showDescriptionOnQuizPageSwitch"] [role="switch"]').click({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="unsavedChangesAlert"]').should('exist')
        cy.get('[data-cy="saveSettingsBtn"]').click()

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showDescriptionOnQuizPageSwitch"] [role="switch"]').should('be.checked')

        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="quizDescription"]').should('exist')
    });

});
