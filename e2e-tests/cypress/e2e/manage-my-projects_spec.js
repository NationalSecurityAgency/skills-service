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

        Cypress.Commands.add('addToMyProjectsViaBtn', (rowNum, expectedNumRows = 6) => {
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

        Cypress.Commands.add('containsCellValue', (rowNum, columnNum, expectedValue, numRows = 6) => {
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
                .should('have.length', 6)
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
        cy.contains('Projects can be created and managed from the "Project Admin" view');
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

        cy.addToMyProjectsViaBtn(1);
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('3');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');

        cy.addToMyProjectsViaBtn(4);
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('4');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('5');

        cy.removeFromMyProjectsViaBtn(0);
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('3');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');

        cy.removeFromMyProjectsViaBtn(2);
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

        cy.doesNotContainsCellValue(0, 1, 'My Project');
        cy.doesNotContainsCellValue(1, 1, 'My Project');
        cy.doesNotContainsCellValue(2, 1, 'My Project');
        cy.doesNotContainsCellValue(3, 1, 'My Project');
        cy.doesNotContainsCellValue(4, 1, 'My Project');
        cy.doesNotContainsCellValue(4, 1, 'My Project');

        cy.addToMyProjectsViaBtn(0);
        cy.addToMyProjectsViaBtn(2);
        cy.addToMyProjectsViaBtn(4);
        cy.containsCellValue(0, 1, 'My Project');
        cy.doesNotContainsCellValue(1, 1, 'My Project');
        cy.containsCellValue(2, 1, 'My Project');
        cy.doesNotContainsCellValue(3, 1, 'My Project');
        cy.containsCellValue(4, 1, 'My Project');
        cy.doesNotContainsCellValue(5, 1, 'My Project');

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

        cy.doesNotContainsCellValue(0, 1, 'My Project');
        cy.containsCellValue(1, 1, 'My Project');
        cy.doesNotContainsCellValue(2, 1, 'My Project');
        cy.containsCellValue(3, 1, 'My Project');
        cy.containsCellValue(4, 1, 'My Project');
        cy.doesNotContainsCellValue(5, 1, 'My Project');

        cy.removeFromMyProjectsViaBtn(3);
        cy.doesNotContainsCellValue(0, 1, 'My Project');
        cy.containsCellValue(1, 1, 'My Project');
        cy.doesNotContainsCellValue(2, 1, 'My Project');
        cy.doesNotContainsCellValue(3, 1, 'My Project');
        cy.containsCellValue(4, 1, 'My Project');
        cy.doesNotContainsCellValue(5, 1, 'My Project');
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
        cy.containsCellValue(0, 0, 'Do we Eat');
        cy.containsCellValue(1, 0, 'This is project 2');
        cy.containsCellValue(2, 0, 'This is project 3');
        cy.containsCellValue(3, 0, 'this is project 3 fleat beat');
        cy.containsCellValue(4, 0, 'This is project 5');
        cy.containsCellValue(5, 0, 'This is project 6');
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(9);

        cy.get('[data-cy="searchInput"]')
            .type('eAT');
        cy.containsCellValue(0, 0, 'Do we Eat', 3);
        cy.containsCellValue(1, 0, 'this is project 3 fleat beat', 3);
        cy.containsCellValue(2, 0, 'Very Neat project', 3);
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(3);

        // verify there is no padding added to the highlighted text
        cy.get('[data-cy="discoverProjectsTable"] tbody tr')
          .eq(2)
          .find('td mark')
          .eq(0)
          .should('have.css', 'padding', '0px');

        cy.get('[data-cy="searchInput"]')
            .type('{backspace}{backspace}{backspace}');
        cy.containsCellValue(5, 0, 'This is project 6');
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(9);

        // test for spaces and case
        cy.get('[data-cy="searchInput"]')
            .type('   eAt    ');
        cy.containsCellValue(0, 0, 'Do we Eat', 3);
        cy.containsCellValue(1, 0, 'this is project 3 fleat beat', 3);
        cy.containsCellValue(2, 0, 'Very Neat project', 3);
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(3);

        // clear search
        cy.get('[data-cy="clearSearch"]')
            .click();
        cy.containsCellValue(5, 0, 'This is project 6');
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(9);
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
        cy.containsCellValue(0, 0, 'This is project 3', 2);
        cy.containsCellValue(1, 0, 'this is project 3 fleat beat', 2);
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(2);

        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('5');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('4');

        cy.addToMyProjectsViaBtn(1, 2);
        cy.get('[data-cy="allProjectsCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('9');
        cy.get('[data-cy="myProjectCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('6');
        cy.get('[data-cy="discoverNewProjCount"] [data-cy="mediaInfoCardTitle"]')
            .contains('3');

        cy.removeFromMyProjectsViaBtn(0, 2);
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
        cy.get('[data-cy="projectsTableTotalRows"]')
            .contains(0);
        cy.get('[data-cy="discoverProjectsTable"]')
            .contains('There are no records');
        cy.get('[data-cy="discoverProjectsTable"]')
            .contains('Please modify your search string: [dljlajd]');

    });

});

