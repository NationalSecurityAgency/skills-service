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

describe('Project Expiration Tests', () => {
    it('Project Expiration Projects Display', () => {
        const markedExpired = dayjs()
            .utc()
            .subtract(5, 'days')
            .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
        cy.intercept(/^\/app\/projects$/, {
            body: [{
                'projectId': 'proj1',
                'name': 'Proj 1',
                'created': '2020-01-01T12:01:01.001+00:00',
                'lastReportedSkill': '',
                'totalPoints': 500,
                'numSubjects': 3,
                'numSkills': 12,
                'numBadges': 3,
                'displayOrder': 0,
                'isFirst': true,
                'isLast': true,
                'pinned': true,
                'numErrors': 0,
                'levelsArePoints': false,
                'expiring': true,
                'expirationTriggered': markedExpired,
            }]
        })
            .as('getProjects');

        cy.intercept('POST', '/admin/projects/proj1/cancelExpiration', { body: { success: true } })
            .as('stopExpiration');

        cy.visit('/administrator');
        cy.wait('@getProjects');

        cy.get('[data-cy=projectExpiration]')
            .should('be.visible');
        cy.get('[data-cy=projectExpiration]')
            .contains('Project has not been used in over 180 days and will be deleted in 2 days');
        cy.get('button[data-cy=keepIt]')
            .should('be.visible');
        cy.get('button[data-cy=keepIt]')
            .click();
        cy.wait('@stopExpiration');
        cy.get('[data-cy=projectExpiration]')
            .should('not.exist');
        cy.get('button[data-cy=keepIt]')
            .should('not.exist');
    });

    it('Project Expiration Project Display', () => {
        cy.intercept('POST', '/admin/projects/proj1/cancelExpiration')
            .as('stopExpiration');
        cy.createProject(1);
        const markedExpired = dayjs()
            .utc()
            .subtract(5, 'days')
            .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
        const results = [{
            'projectId': 'proj1',
            'name': 'Proj 1',
            'created': '2020-01-01T12:01:01.001+00:00',
            'lastReportedSkill': '',
            'totalPoints': 500,
            'numSubjects': 3,
            'numSkills': 12,
            'numBadges': 3,
            'displayOrder': 0,
            'isFirst': true,
            'isLast': true,
            'pinned': true,
            'numErrors': 0,
            'levelsArePoints': false,
            'expiring': true,
            'expirationTriggered': markedExpired,
        },
            {
                'projectId': 'proj1',
                'name': 'Proj 1',
                'created': '2020-01-01T12:01:01.001+00:00',
                'lastReportedSkill': '',
                'totalPoints': 500,
                'numSubjects': 3,
                'numSkills': 12,
                'numBadges': 3,
                'displayOrder': 0,
                'isFirst': true,
                'isLast': true,
                'pinned': true,
                'numErrors': 0,
                'levelsArePoints': false,
                'expiring': false,
                'expirationTriggered': '',
            }, []];
        cy.intercept('/admin/projects/proj1', (req) => {
            if (req.url.endsWith('settings')) {
                req.reply([]);
            } else {
                req.reply(results.shift());
            }
        })
            .as('getProject');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@getProject');

        cy.get('[data-cy=projectExpiration]')
            .should('be.visible');
        cy.get('[data-cy=projectExpiration]')
            .contains('Project has not been used in over 180 days and will be deleted in 2 days');
        cy.get('button[data-cy=keepIt]')
            .should('be.visible');
        cy.get('button[data-cy=keepIt]')
            .click();
        cy.wait('@stopExpiration');
        cy.contains('PROJECT: Proj 1');
        cy.get('[data-cy=projectExpiration]')
            .should('not.exist');
        cy.get('button[data-cy=keepIt]')
            .should('not.exist');
    });

    it('Keeping an expired project is recorded in the activity table', () => {
        cy.intercept('POST', '/admin/projects/proj1/cancelExpiration')
            .as('stopExpiration');
        cy.createProject(1);
        const markedExpired = dayjs()
            .utc()
            .subtract(5, 'days')
            .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
        const results = [{
            'projectId': 'proj1',
            'name': 'Proj 1',
            'created': '2020-01-01T12:01:01.001+00:00',
            'lastReportedSkill': '',
            'totalPoints': 500,
            'numSubjects': 3,
            'numSkills': 12,
            'numBadges': 3,
            'displayOrder': 0,
            'isFirst': true,
            'isLast': true,
            'pinned': true,
            'numErrors': 0,
            'levelsArePoints': false,
            'expiring': true,
            'expirationTriggered': markedExpired,
        },
            {
                'projectId': 'proj1',
                'name': 'Proj 1',
                'created': '2020-01-01T12:01:01.001+00:00',
                'lastReportedSkill': '',
                'totalPoints': 500,
                'numSubjects': 3,
                'numSkills': 12,
                'numBadges': 3,
                'displayOrder': 0,
                'isFirst': true,
                'isLast': true,
                'pinned': true,
                'numErrors': 0,
                'levelsArePoints': false,
                'expiring': false,
                'expirationTriggered': '',
            }, []];
        cy.intercept('/admin/projects/proj1', (req) => {
            if (req.url.endsWith('settings')) {
                req.reply([]);
            } else {
                req.reply(results.shift());
            }
        })
            .as('getProject');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@getProject');

        cy.get('[data-cy=projectExpiration]')
            .should('be.visible');
        cy.get('[data-cy=projectExpiration]')
            .contains('Project has not been used in over 180 days and will be deleted in 2 days');
        cy.get('button[data-cy=keepIt]')
            .should('be.visible');
        cy.get('button[data-cy=keepIt]')
            .click();
        cy.wait('@stopExpiration');
        cy.contains('PROJECT: Proj 1');
        cy.get('[data-cy=projectExpiration]')
            .should('not.exist');
        cy.get('button[data-cy=keepIt]')
            .should('not.exist');

        cy.visit('/administrator/projects/proj1/activityHistory');

        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')

        cy.get('[data-cy="row0-action"]').contains('Cancel Expiration')
        cy.get('[data-cy="row0-item"]').contains('Project')
        cy.get('[data-cy="row0-itemId"]').contains('proj1')
    });
});
