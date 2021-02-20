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
const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
describe('App Features Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('display new version banner when software is updated', () => {
        cy.intercept({
            path: '/admin/projects/proj1/subjects',
            statusCode: 200,
        }, {
            body: [],
            headers: {
                'skills-client-lib-version': dateFormatter(new Date())
            },
        }).as('getSubjects');
        cy.visit('/administrator/');
       /* cy.injectAxe();
        cy.violationLoggingFunction().then((loggingFunc) => {
            cy.checkA11y(null, null, loggingFunc);
        });*/
        cy.get('[data-cy=subPageHeader]').contains('Projects');
        cy.contains('New Software Version is Available').should('not.exist')
        cy.get('[data-cy=projectCard]').last().contains('Manage').click()
        cy.wait('@getSubjects')

        cy.contains('New Software Version is Available')

        cy.contains('Here').click()
        cy.contains('New Software Version is Available').should('not.exist')
    });

    it('do not display new version banner if lib version in headers is older than lib version in local storage', () => {
        cy.intercept({
            path: '/admin/projects/proj1/subjects',
            statusCode: 200,
        }, {
            body: [],
            headers: {
                'skills-client-lib-version': dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 30)
            },
        }).as('getSubjects');
        cy.visit('/administrator/');
        cy.get('[data-cy=subPageHeader]').contains('Projects');
        cy.get('[data-cy=projectCard]').last().contains('Manage').click()
            cy.wait('@getSubjects')
        /*cy.injectAxe();
        cy.violationLoggingFunction().then((loggingFunc) => {
            cy.checkA11y(null, null, loggingFunc);
        });*/

        cy.contains('New Software Version is Available').should('not.exist')
    });

    it('access denied should show authorization failure page not error page', () => {

        cy.intercept({
            method: 'GET',
            path: '/admin/projects/proj1/subjects/subj1',
        }, {
            statusCode: 403,
            body: {errorCode: 'NotAuthorized', explanation: 'Not authorized to view this resource'}
        }).as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        /*cy.injectAxe();*/
        cy.wait('@loadSubject');
        cy.url().should('include', '/not-authorized');
        cy.contains('User Not Authorized').should('be.visible')
        /*cy.violationLoggingFunction().then((loggingFunc) => {
            cy.checkA11y(null, null, loggingFunc);
        });*/
    });

})
