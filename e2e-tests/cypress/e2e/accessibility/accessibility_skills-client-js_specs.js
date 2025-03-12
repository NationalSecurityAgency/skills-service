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


describe('Accessibility for embedded skills-client-js Tests', () => {

    beforeEach(() => {
        cy.createProject(1)
    });


    it(`test skills-client-js`, () => {
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {selfReportingType: 'HonorSystem'})
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4, {selfReportingType: 'Approval'})
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.createSubject(1, 2)

        cy.createSubject(1, 3)
        cy.createSkill(1, 3, 4, {selfReportingType: 'HonorSystem'})
        // cy.doReportSkill({project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'yesterday'})
        // cy.doReportSkill({project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now'})

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, {enabled: true});

        cy.visit('/test-skills-client/proj1')
        cy.injectAxe();
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"]')
        cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"]')
        cy.wrapIframe().find('[data-cy="myRankBtn"]')

        cy.customLighthouse();
        cy.customA11y();
    })

})

