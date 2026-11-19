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

        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.wait('@loadMyProjects');

        cy.get('[data-cy="searchInput"]')
            .type('dljlajd');
        cy.get('[data-cy="discoverProjectsTable"]')
            .contains('No results found');
        cy.get('[data-cy="discoverProjectsTable"]')
            .contains('Please modify your search string: [dljlajd]');
    });

    it('search matches project descriptions, subjects and skills', function () {
        cy.createProject(1, { name: 'Kubernetes Operations' });
        cy.createProject(2, { name: 'Second Project', description: 'Training on kubernetes clusters' });
        cy.createProject(3, { name: 'Third Project' });
        cy.createSubject(3, 1);
        cy.createSkill(3, 1, 1, { name: 'Kubernetes Deployments' });
        cy.createProject(4, { name: 'Fourth Project' });
        cy.createSubject(4, 1, { name: 'Kubernetes Networking' });
        cy.createProject(5, { name: 'Fifth Project', description: 'Docker only' });
        for (let i = 1; i <= 5; i += 1) {
            cy.enableProdMode(i);
        }

        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');
        cy.intercept('GET', '/api/availableForMyProjects/search*').as('searchProjects');
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.wait('@loadMyProjects');

        cy.get('[data-cy="searchInput"]').should('have.attr', 'placeholder', 'Search project names, descriptions and skills');
        cy.get('[data-cy="searchInput"]').type('KUBERNETES');
        cy.wait('@searchProjects').its('request.url').should('include', 'query=kubernetes');
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '4');
        cy.get('[data-cy="discoverProjectsTable"]').should('not.contain', 'Fifth Project');
        cy.get('[data-cy="searchMatch_proj1"]').should('not.exist');
        cy.get('[data-cy="searchMatch_proj2"] [data-cy="matchedDescription"]').should('contain.text', 'Matched in description');
        cy.get('[data-cy="searchMatch_proj3"] [data-cy="matchedSkills"]').should('contain.text', 'Matched skills: Kubernetes Deployments');
        cy.get('[data-cy="searchMatch_proj4"] [data-cy="matchedSkills"]').should('not.exist');
        cy.get('[data-cy="searchMatch_proj4"] [data-cy="matchedDescription"]').should('not.exist');

        cy.get('[data-cy="filterResetBtn"]').click();
        cy.get('[data-cy="searchInput"]').should('have.value', '');
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '5');
        cy.get('[data-cy="searchMatch_proj2"]').should('not.exist');
    });

    it('every search word must be found, in any order and in any field', function () {
        cy.createProject(1, { name: 'Cloud Training', description: 'Hands-on security labs' });
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Kubernetes Basics' });
        cy.createProject(2, { name: 'Cloud Training Two' });
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { name: 'Kubernetes Basics' });
        cy.enableProdMode(1);
        cy.enableProdMode(2);

        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');
        cy.intercept('GET', '/api/availableForMyProjects/search*').as('searchProjects');
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.wait('@loadMyProjects');

        cy.get('[data-cy="searchInput"]').type('security   kubernetes');
        cy.wait('@searchProjects');
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        cy.get('[data-cy="discoverProjectsTable"]').should('contain', 'Cloud Training').and('not.contain', 'Cloud Training Two');
        cy.get('[data-cy="searchMatch_proj1"] [data-cy="matchedSkills"]').should('contain.text', 'Matched skills: Kubernetes Basics');
        cy.get('[data-cy="searchMatch_proj1"] [data-cy="matchedDescription"]').should('exist');

        // name words are matched right away, in any order, before the search returns
        cy.get('[data-cy="searchInput"]').clear().type('two cloud');
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        cy.get('[data-cy="discoverProjectsTable"]').should('contain', 'Cloud Training Two');
    });

    it('search lists up to 5 matched skills and the number of other matches', function () {
        cy.createProject(1, { name: 'Secure Development' });
        cy.createSubject(1, 1);
        for (let i = 1; i <= 7; i += 1) {
            cy.createSkill(1, 1, i, { name: `Coding Exercise ${i}` });
        }
        cy.createSkill(1, 1, 8, { name: 'Threat Modeling' });
        cy.enableProdMode(1);

        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');
        cy.intercept('GET', '/api/availableForMyProjects/search*').as('searchProjects');
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.wait('@loadMyProjects');

        cy.get('[data-cy="searchInput"]').type('coding');
        cy.wait('@searchProjects');
        cy.get('[data-cy="searchMatch_proj1"] [data-cy="matchedSkills"]')
            .should('contain.text', 'Matched skills: Coding Exercise 1, Coding Exercise 2, Coding Exercise 3, Coding Exercise 4, Coding Exercise 5')
            .and('contain.text', '(+2 more)');
    });

    it('only the latest search is shown', function () {
        cy.createProject(1, { name: 'First Project' });
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Helm Charts' });
        cy.createProject(2, { name: 'Second Project' });
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { name: 'Terraform Modules' });
        cy.enableProdMode(1);
        cy.enableProdMode(2);

        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');
        cy.intercept('GET', '/api/availableForMyProjects/search?query=helm', (req) => {
            req.on('response', (res) => {
                res.setDelay(2000);
            });
        }).as('slowSearch');
        cy.intercept('GET', '/api/availableForMyProjects/search?query=terraform').as('fastSearch');
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.wait('@loadMyProjects');

        cy.get('[data-cy="searchInput"]').type('helm');
        cy.get('[data-cy="searchInProgress"]').should('exist');
        cy.get('[data-cy="searchInput"]').clear().type('terraform');
        cy.wait('@fastSearch');
        cy.wait('@slowSearch');
        cy.get('[data-cy="searchInProgress"]').should('not.exist');
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        cy.get('[data-cy="discoverProjectsTable"]').should('contain', 'Second Project').and('not.contain', 'First Project');
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

    it('can add to my projects from project page', function () {
        for (let i = 1; i <=5; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.visit('/progress-and-rankings/manage-my-projects');

        cy.doesNotContainsCellValue(0, 2, 'My Project');
        cy.doesNotContainsCellValue(1, 2, 'My Project');
        cy.doesNotContainsCellValue(2, 2, 'My Project');
        cy.doesNotContainsCellValue(3, 2, 'My Project');
        cy.doesNotContainsCellValue(4, 2, 'My Project');

        cy.get('[data-cy="viewButton-proj1"]').click()
        cy.get('[data-cy="addButton-proj1"]').should('exist')
        cy.get('[data-cy="addButton-proj1"]').click()
        cy.get('[data-cy="addButton-proj1"]').should('not.exist')

        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.containsCellValue(0, 2, 'My Project');

        cy.get('[data-cy="removeBtn-proj1"]').click()
        cy.doesNotContainsCellValue(0, 2, 'My Project');

        cy.get('[data-cy="viewButton-proj1"]').click()
        cy.get('[data-cy="addButton-proj1"]').should('exist')
        cy.get('[data-cy="addButton-proj1"]').click()
        cy.get('[data-cy="addButton-proj1"]').should('not.exist')

        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.containsCellValue(0, 2, 'My Project');

    });

    it('add button does not show for project already added', function () {
        for (let i = 1; i <=5; i += 1) {
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

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="overallPoints"]').should('exist')
        cy.get('[data-cy="addButton-proj1"]').should('not.exist')
        cy.visit('/progress-and-rankings/projects/proj2');
        cy.get('[data-cy="overallPoints"]').should('exist')
        cy.get('[data-cy="addButton-proj2"]').should('exist')

    });

    it('add button does not show on Inception', function () {
        cy.intercept('GET', '/api/myprojects/Inception/isMyProject', cy.spy().as('isMyProject'));
        cy.visit('/progress-and-rankings/projects/Inception');

        cy.get('[data-cy="overallPoints"]').should('exist')
        cy.get('@isMyProject').should('not.have.been.called');
        cy.get('[data-cy="addButton-Inception"]').should('not.exist')
    });

    it('add button does not show on skills-client', function () {
        for (let i = 1; i <=2; i += 1) {
            cy.createProject(i);
            cy.enableProdMode(i);
        }

        cy.intercept('GET', '/api/myprojects/proj1/isMyProject', cy.spy().as('isMyProject'));
        cy.visit('/test-skills-client/proj1');

        cy.wrapIframe().find('[data-cy="overallPoints"]')
        cy.get('@isMyProject').should('not.have.been.called');
        cy.wrapIframe().find('[data-cy="addButton-proj1"]').should('not.exist')

    });

    it('add button does not show on user skills-display preview', function () {
        cy.intercept('GET', '/api/myprojects/proj1/isMyProject', cy.spy().as('isMyProject'));
        cy.createProject(1);
        cy.enableProdMode(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.reportSkill(1, 1, 'user1', 'yesterday');

        cy.visit('/administrator/projects/proj1/users/user1')
        cy.get('[data-cy="overallPoints"]').should('exist')
        cy.get('@isMyProject').should('not.have.been.called');
        cy.get('[data-cy="addButton-proj1"]').should('not.exist')

    });
});

