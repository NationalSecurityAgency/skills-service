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
describe('Resource Not Found Tests', () => {

    beforeEach(() => {
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
    });

    it('invalid subject results in not found page', () => {
        cy.intercept('GET', '/api/myProgressSummary')
            .as('loadProgress');
        cy.visit('/administrator/projects/proj1/subjects/fooo');

        cy.url().should('include', '/error')
        cy.get('[data-cy=errExplanation]')
            .should('be.visible')
            .contains('Subject [fooo] doesn\'t exist');
    });

    it('invalid route in not found page', () => {
        cy.intercept('GET', '/api/myProgressSummary')
            .as('loadProgress');
        cy.visit('/administrator/doesnotexist');

        cy.url().should('include', '/not-found')
        cy.get('[data-cy=notFoundExplanation]')
            .should('be.visible')
            .contains('The resource you requested cannot be located.');

        cy.get('[data-cy="breadcrumb-bar"]').should('have.text', 'Not Found');
    });
    
});
