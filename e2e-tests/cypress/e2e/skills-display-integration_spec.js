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
import moment from 'moment';
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTimePlugin);

describe('Navigation Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.enableProdMode(1);

        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);

        cy.addToMyProjects(1);

    });

    if (!Cypress.env('oauthMode')) {
        it.only('Browser back button works in Skills Display', function () {
            cy.createSkill(1, 1, 1);
            cy.createSkill(1, 1, 2);
            cy.createSkill(1, 1, 3);
            cy.createSkill(1, 1, 4);

            cy.intercept('GET', '/api/projects/proj1/pointHistory*')
                .as('pointHistoryChart');

            cy.visit('/');

            cy.get('[data-cy=inception-button]')
                .should('not.exist');

            cy.get('[data-cy=project-link-proj1]')
                .click();
            cy.wait('@pointHistoryChart');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="pointHistoryChartNoData"]')
            cy.get('[data-cy="skillsDisplayHome"]')
                .contains('Overall Points');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="totalPoints"]')
                .contains('800');

            cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
                .should('be.visible');
            cy.get('[data-cy=breadcrumb-proj1]')
                .should('be.visible');
            cy.get('[data-cy=breadcrumb-projects]')
                .should('not.exist');

            cy.get('[data-cy="skillsDisplayHome"]')
                .find('[data-cy=back]')
                .should('not.exist');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
                .contains('Project: This is project 1');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="pointHistoryChartNoData"]')

            // to subject page
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]').first().click();
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
              .contains('Subject 1');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="pointHistoryChartNoData"]')
            cy.wait(1000);

            // navigate to Rank Overview and that it does NOT contains the internal back button
            cy.get('[data-cy="skillsDisplayHome"]')
                .find('[data-cy=myRankBtn]')
                .click();
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
                .contains('My Rank');
            cy.get('[data-cy="skillsDisplayHome"]')
                .find('[data-cy=back]')
                .should('not.exist');
            cy.get('[data-cy="levelBreakdownChart"] [data-cy="levelBreakdownChart-animationEnded"]')

            // click the browser back button and verify that we are still in the
            // client display (Subject page)
            cy.go('back');  // browser back button
            cy.wait(1000);
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
              .contains('Subject 1');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="pointHistoryChartNoData"]')

            // then back one more time and we should be back on the client display home page
            cy.go('back');  // browser back button
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
              .contains('Project: This is project 1');
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="pointHistoryChartNoData"]')
            cy.wait(1000);

            // finally back one more time and we should be back on the my progress page
            cy.go('back');  // browser back button
            cy.wait(1000);
            cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
                .contains('Progress And Rankings')
                .should('be.visible');
            cy.get('[data-cy=project-link-proj1]')
        });
    }
});

