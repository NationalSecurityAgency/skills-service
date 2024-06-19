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
import moment from 'moment-timezone';

describe('Client Display Prerequisites Snapshot Tests', () => {

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.createProject(1);
        cy.createSubject(1, 1);

        Cypress.Commands.add('clickOnNode', (x, y) => {
            cy.contains('Prerequisites');
            cy.get('[data-cy="graphLegend"]').contains('Legend');
            cy.wait(2000); // wait for chart
            // have to click twice to it to work...
            cy.get('#dependent-skills-network canvas')
                .should('be.visible')
                .click(x, y);
        });

        Cypress.Commands.add('navToTheFirstSkill', (x, y) => {
            cy.cdVisit('/');
            cy.cdClickSubj(0);
            cy.cdClickSkill(1);
        });

        // must set viewport to show entire canvas or it will not appear in the screenshot
        cy.viewport(1280, 1280);
    });

    it('skill prerequisite graph', () => {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 2);
        cy.createBadge(1, 2, { enabled: true });

        cy.addLearningPathItem(1, 1, 2, true, true)
        cy.addLearningPathItem(1, 3, 2, false, true)
        cy.addLearningPathItem(1, 4, 3)

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

        cy.cdVisit('/?enableTheme=true', true);
        cy.cdClickSubj(0, 'Subject 1', true);
        cy.cdClickSkill(1);

        cy.wait(5000)
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('skill prerequisite graph - with paging', () => {
        const numSkills = 10;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, {
                name: `skill${i}`,
                numPerformToCompletion: 1
            });
        }

        for (let i = 0; i < numSkills - 1; i += 1) {
            cy.addLearningPathItem(1, i, i+1)
        }

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.cdClickSkill(9);

        cy.wait(5000)
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('skill prerequisite graph on badge page', () => {
        cy.viewport(1200, 1000);

        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)
        cy.createSkill(1, 1, 13)
        cy.createSkill(1, 1, 14)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 12);
        cy.createBadge(1, 2, { enabled: true });

        cy.createBadge(1, 3);
        cy.assignSkillToBadge(1, 3, 13);
        cy.createBadge(1, 3, { enabled: true })

        cy.addLearningPathItem(1, 2, 1, true, true)
        cy.addLearningPathItem(1, 3, 1, true, true)
        cy.addLearningPathItem(1, 1, 1, false, true)
        cy.addLearningPathItem(1, 2, 2, false, true)
        cy.addLearningPathItem(1, 3, 2)

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
        cy.reportSkill(1, 13, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 13, Cypress.env('proxyUser'), 'now')

        cy.cdVisit('/badges/badge1?enableTheme=true');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge2"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-badge3"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill1"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="4"] [data-cy="skillLink-proj1-skill3"]')
        cy.wait(5000)
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

});