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
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';
import localizedFormat from 'dayjs/plugin/localizedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);
dayjs.extend(localizedFormat);
describe('Dashboard User Actions Tests', () => {

    it('Display project user activity', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })

        cy.visit('/administrator/projects/proj1/activityHistory')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')
        cy.get('[data-cy="projectIdFilter"]').should('not.exist')
        cy.get('[data-cy="quizIdFilter"]').should('not.exist')

        cy.get('[data-cy="row0-action"]').contains('Create')
        cy.get('[data-cy="row0-item"]').contains('Skill')
        cy.get('[data-cy="row0-itemId"]').contains('skill1')
        cy.get('[data-cy="row0-projectId"]').should('not.exist')
        cy.get('[data-cy="row0-quizId"]').should('not.exist')

        cy.get('[data-cy="row1-item"]').contains('Subject')
        cy.get('[data-cy="row2-item"]').contains('Project')

        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="row0-expandedDetails"').contains('Skill Id:')
        cy.get('[data-cy="row0-expandedDetails"').contains('skill1')
    });

    it('filter project user activity', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })

        cy.visit('/administrator/projects/proj1/activityHistory')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]')
            .should('have.text', '3')
        cy.get('[data-cy="itemFilter"]').click()
        cy.get('[data-pc-section="itemlabel"]').contains('Subject').click()

        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]')
            .should('have.text', '1')
        cy.get('[data-cy="row0-item"]').contains('Subject')
    });

    it('show start of recording activity warning', () => {
        const tomorrow = dayjs().add(1, 'day');
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.activityHistoryStartDate = tomorrow.format('YYYY-MM-DD');
                res.send(conf);
            });
        }).as('loadConfig');
        cy.createProject(1)

        cy.visit('/administrator/projects/proj1/activityHistory')
        cy.wait('@loadConfig')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="activityHistoryStartRecordingWarning"]').contains(`Started recording user activity on ${tomorrow.format('ll')}`);
    });

    it('do not show recording activity warning if project create date is after start record date', () => {
        const tomorrow = dayjs().subtract(1, 'day');
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.activityHistoryStartDate = tomorrow.format('YYYY-MM-DD');
                res.send(conf);
            });
        }).as('loadConfig');
        cy.createProject(1)

        cy.visit('/administrator/projects/proj1/activityHistory')
        cy.wait('@loadConfig')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.wait(3000);
        cy.get('[data-cy="activityHistoryStartRecordingWarning"]').should('not.exist');
    });
});
