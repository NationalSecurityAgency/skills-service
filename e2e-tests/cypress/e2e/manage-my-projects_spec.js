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

describe('Manage My Projects Tests', () => {

    beforeEach(() => {

        Cypress.Commands.add('addToMyProjectsViaBtn', (rowNum, expectedNumRows = 5) => {
            const tableSelector = '[data-cy="discoverProjectsTable"]';
            const rowSelector = `${tableSelector} tbody tr`;
            cy.get(rowSelector)
                .should('have.length', expectedNumRows)
                .as('cyRows');
            cy.get('@cyRows')
                .eq(rowNum)
                .find('td')
                .as('row2');
            cy.get('@row2')
                .eq(1)
                .find('[data-cy="addButton"]')
                .click();
            cy.get('@row2')
                .eq(1)
                .contains('My Project');
            cy.get('@row2')
                .eq(1)
                .find('[data-cy="removeBtn"]');
        });

        Cypress.Commands.add('removeFromMyProjectsViaBtn', (rowNum, expectedNumRows = 6) => {
            const tableSelector = '[data-cy="discoverProjectsTable"]';
            const rowSelector = `${tableSelector} tbody tr`;
            cy.get(rowSelector)
                .should('have.length', expectedNumRows)
                .as('cyRows');
            cy.get('@cyRows')
                .eq(rowNum)
                .find('td')
                .as('row2');
            cy.get('@row2')
                .eq(1)
                .contains('My Project');
            cy.get('@row2')
                .eq(1)
                .find('[data-cy="removeBtn"]')
                .click();
            cy.get('@row2')
                .eq(1)
                .find('[data-cy="addButton"]');

        });

        Cypress.Commands.add('containsCellValue', (rowNum, columnNum, expectedValue, numRows = 5) => {
            const tableSelector = '[data-cy="discoverProjectsTable"]';
            const rowSelector = `${tableSelector} tbody tr`;
            cy.get(rowSelector)
                .should('have.length', numRows)
                .as('cyRows');
            cy.get('@cyRows')
                .eq(rowNum)
                .find('td')
                .as('row2');
            cy.get('@row2')
                .eq(columnNum)
                .contains(expectedValue);
        });

        Cypress.Commands.add('doesNotContainsCellValue', (rowNum, columnNum, expectedValue) => {
            const tableSelector = '[data-cy="discoverProjectsTable"]';
            const rowSelector = `${tableSelector} tbody tr`;
            cy.get(rowSelector)
                .should('have.length', 5)
                .as('cyRows');
            cy.get('@cyRows')
                .eq(rowNum)
                .find('td')
                .as('row2');
            cy.get('@row2')
                .eq(columnNum)
                .contains(expectedValue)
                .should('not.exist');
        });
    });

    it('display a message when there are no projects in prod-mode', function () {
        for (let i = 1; i <= 3; i += 1) {
            cy.createProject(i);
        }

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
            .click();
        cy.contains('No Discoverable Projects');
        cy.contains('Projects can be created and managed from the Project Admin view');
    });

    it('display stat cards', function () {
        for (let i = 2; i <= 9; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
            .click();

        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('8');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('8');

        cy.addToMyProjects(2);
        cy.addToMyProjects(4);

        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('8');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('2');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');
    });

    it('stat cards are updated in real time when projects are added and removed', function () {
        for (let i = 1; i <= 9; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.addToMyProjects(1);
        cy.addToMyProjects(3);

        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('2');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('7');

        cy.get('[data-cy="addButton-proj5"]').click()
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('3');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');

        cy.get('[data-cy="addButton-proj4"]').click()
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('4');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('5');

        cy.get('[data-cy="removeBtn-proj5"]').click()
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('3');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');

        cy.get('[data-cy="removeBtn-proj4"]').click()
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('2');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('7');

        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('2');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('7');
    });

    it('add to my projects', function () {
        for (let i = 1; i <= 9; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.doesNotContainsCellValue(0, 2, 'My Project');
        cy.doesNotContainsCellValue(1, 2, 'My Project');
        cy.doesNotContainsCellValue(2, 2, 'My Project');
        cy.doesNotContainsCellValue(3, 2, 'My Project');
        cy.doesNotContainsCellValue(4, 2, 'My Project');

        cy.get('[data-cy="addButton-proj1"]').click()
        cy.get('[data-cy="addButton-proj3"]').click()
        cy.get('[data-cy="addButton-proj5"]').click()
        cy.containsCellValue(0, 2, 'My Project');
        cy.doesNotContainsCellValue(1, 2, 'My Project');
        cy.containsCellValue(2, 2, 'My Project');
        cy.doesNotContainsCellValue(3, 2, 'My Project');
        cy.containsCellValue(4, 2, 'My Project');

    });

    it('remove my projects', function () {
        for (let i = 1; i <= 9; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.addToMyProjects(2);
        cy.addToMyProjects(4);
        cy.addToMyProjects(5);

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.doesNotContainsCellValue(0, 2, 'My Project');
        cy.containsCellValue(1, 2, 'My Project');
        cy.doesNotContainsCellValue(2, 2, 'My Project');
        cy.containsCellValue(3, 2, 'My Project');
        cy.containsCellValue(4, 2, 'My Project');

        cy.get('[data-cy="removeBtn-proj4"]').click()
        cy.doesNotContainsCellValue(0, 2, 'My Project');
        cy.containsCellValue(1, 2, 'My Project');
        cy.doesNotContainsCellValue(2, 2, 'My Project');
        cy.doesNotContainsCellValue(3, 2, 'My Project');
        cy.containsCellValue(4, 2, 'My Project');
    });

    it('search by name', function () {
        window.localStorage.setItem('tableState', JSON.stringify({'DiscoverProjects': {'sortDesc': false, 'sortBy': 'name'}}))
        cy.createProject(1, { name: 'Very Neat project' });
        cy.createProject(2);
        cy.createProject(3);
        cy.createProject(4, { name: 'Do we Eat' });
        cy.createProject(5);
        cy.createProject(6);
        cy.createProject(7);
        cy.createProject(8, { name: 'this is project 3 fleat beat' });
        cy.createProject(9);

        for (let i = 1; i <= 9; i += 1) {
            cy.enableProdMode(i);
        }

        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.containsCellValue(0, 1, 'Do we Eat');
        cy.containsCellValue(1, 1, 'This is project 2');
        cy.containsCellValue(2, 1, 'This is project 3');
        cy.containsCellValue(3, 1, 'this is project 3 fleat beat');
        cy.containsCellValue(4, 1, 'This is project 5');
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]')
            .should('have.text', '9');

        cy.get('[data-cy="searchInput"]')
            .type('eAT');
        cy.containsCellValue(0, 1, 'Do we Eat', 3);
        cy.containsCellValue(1, 1, 'this is project 3 fleat beat', 3);
        cy.containsCellValue(2, 1, 'Very Neat project', 3);

        cy.get('[data-cy="searchInput"]')
            .type('{backspace}{backspace}{backspace}');
        cy.containsCellValue(4, 1, 'This is project 5');

        // clear search
        cy.get('[data-cy="filterResetBtn"]')
            .click();
        cy.containsCellValue(4, 1, 'This is project 5');
    });

    it('stats cards update when adding and removing projects after performing a search', function () {
        cy.createProject(1, { name: 'Very Neat project' });
        cy.createProject(2);
        cy.createProject(3);
        cy.createProject(4, { name: 'Do we Eat' });
        cy.createProject(5);
        cy.createProject(6);
        cy.createProject(7);
        cy.createProject(8, { name: 'this is project 3 fleat beat' });
        cy.createProject(9);

        for (let i = 1; i <= 9; i += 1) {
            cy.enableProdMode(i);
        }

        cy.addToMyProjects(3);
        cy.addToMyProjects(4);
        cy.addToMyProjects(5);
        cy.addToMyProjects(6);
        cy.addToMyProjects(7);

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.get('[data-cy="searchInput"]')
            .type('project 3');
        cy.containsCellValue(0, 1, 'This is project 3', 2);
        cy.containsCellValue(1, 1, 'this is project 3 fleat beat', 2);

        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('5');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('4');

        cy.get('[data-cy="addButton-proj8"]').click()
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('3');

        cy.get('[data-cy="removeBtn-proj3"]').click()
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('5');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('4');
    });

    it('search filters all the rows', function () {
        cy.createProject(1, { name: 'Very Neat project' });
        cy.createProject(2);

        cy.enableProdMode(1);
        cy.enableProdMode(2);

        cy.intercept('GET', '/app/projects/Inception/users/*/token').as('getToken');
        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.wait('@getToken');
        cy.wait('@loadMyProjects');

        cy.get('[data-cy="searchInput"]')
            .type('dljlajd');
        cy.get('[data-cy="discoverProjectsTable"]')
            .contains('No results found');
        cy.get('[data-cy="discoverProjectsTable"]')
            .contains('Please modify your search string: [dljlajd]');

    });

    it('view button is removed when project is added', function () {
        for (let i = 1; i <= 3; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.addToMyProjects(1);
        cy.addToMyProjects(3);

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.get('[data-cy="viewButton-proj2"]').should('exist');
        cy.get('[data-cy="addButton-proj2"]').click()
        cy.get('[data-cy="viewButton-proj2"]').should('not.exist');
        cy.get('[data-cy="removeBtn-proj2"]').click()
        cy.get('[data-cy="viewButton-proj2"]').should('exist');

    });

    it('view button navigates to page', function () {

        cy.createProject(1);
        cy.enableProdMode(1);

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.get('[data-cy="viewButton-proj1"]').should('exist');
        cy.get('[data-cy="viewButton-proj1"]').click()

        cy.get('[data-cy="title"]').should('have.text', 'Project: This is project 1')


    });
});

