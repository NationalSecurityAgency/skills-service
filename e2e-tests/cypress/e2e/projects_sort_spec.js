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

describe('Projects Sort Order Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles*')
            .as('getRolesForRoot');
    });

    it('drag-and-drop project sort management', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.createProject(4);
        cy.createProject(5);
        cy.visit('/administrator');

        const project1Card = '[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]';
        const project2Card = '[data-cy="projectCard_proj2"] [data-cy="sortControlHandle"]';
        const project4Card = '[data-cy="projectCard_proj4"] [data-cy="sortControlHandle"]';
        const project5Card = '[data-cy="projectCard_proj5"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 1', 'This is project 2', 'This is project 3', 'This is project 4', 'This is project 5']);
        cy.get(project1Card)
            .dragAndDrop(project4Card);
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 5']);

        // refresh to make sure it was saved
        cy.visit('/administrator');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 5']);

        cy.get(project5Card)
            .dragAndDrop(project2Card);
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 5', 'This is project 2', 'This is project 3', 'This is project 4', 'This is project 1']);

        cy.get(project2Card)
            .dragAndDrop(project1Card);
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 5', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 2']);

        // refresh to make sure it was saved
        cy.visit('/administrator');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 5', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 2']);

    });

    it('no drag-and-drag sort controls when there is only 1 project', () => {
        cy.createProject(1);

        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj1"]');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('not.exist');

        cy.createProject(2);
        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj1"]');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]');
    });

    it('drag-and-drag sort should spinner while backend operation is happening', () => {
        cy.intercept('/admin/projects/proj1', (req) => {
            req.reply((res) => {
                res.send({ delay: 6000 });
            });
        })
            .as('proj1Async');

        cy.createProject(1);
        cy.createProject(2);

        const proj1Card = '[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]';
        const proj2Card = '[data-cy="projectCard_proj2"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 1', 'This is project 2']);
        cy.get(proj1Card)
            .dragAndDrop(proj2Card);

        // overlay over both cards but loading message only on project 1
        cy.get('[data-cy="proj1_overlayShown"] [data-cy="overlaySpinner"]')
        cy.get('[data-cy="proj2_overlayShown"]');
        cy.get('[data-cy="proj2_overlayShown"] [data-cy="overlaySpinner"]').should('not.exist');
        cy.wait('@proj1Async');
        cy.get('[data-cy="proj1_overlayShown"]').should('not.exist');
        cy.get('[data-cy="proj2_overlayShown"]').should('not.exist');
    });

    it('change sort order using keyboard', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.visit('/administrator/');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 1', 'This is project 2', 'This is project 3']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // refesh and re-validate
        cy.visit('/administrator/');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 1']);

        // now let's move up
        cy.get('[data-cy="projectCard_proj3"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 3', 'This is project 2', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');
        cy.get('[data-cy="projectCard_proj3"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 3', 'This is project 2', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');
    });
});