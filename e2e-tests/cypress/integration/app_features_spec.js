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
const dateFormatter = value => moment(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
describe('App Features Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('display new version banner when software is updated', () => {
        cy.server().route({
            url: '/admin/projects/proj1/subjects',
            status: 200,
            response: [],
            headers: {
                'skills-client-lib-version': dateFormatter(new Date())
            },
        }).as('getSubjects');
        cy.visit('/');
        cy.contains('My Projects')
        cy.contains('New Software Version is Available').should('not.exist')
        cy.contains('Manage').click()
        cy.wait('@getSubjects')

        cy.contains('New Software Version is Available')

        cy.contains('Here').click()
        cy.contains('New Software Version is Available').should('not.exist')
    });

    it('do not display new version banner if lib version in headers is older than lib version in local storage', () => {
        cy.server().route({
            url: '/admin/projects/proj1/subjects',
            status: 200,
            response: [],
            headers: {
                'skills-client-lib-version': dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 30)
            },
        }).as('getSubjects');
        cy.visit('/');
        cy.contains('Manage').click()
        cy.wait('@getSubjects')

        cy.contains('New Software Version is Available').should('not.exist')
    });

})
