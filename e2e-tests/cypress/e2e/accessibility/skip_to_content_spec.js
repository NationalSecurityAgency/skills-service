/*
 * Copyright 2024 SkillTree
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

describe('Skip To Content Tests', () => {

    beforeEach(() => {
        cy.createProject()
        cy.enableProdMode(1);

        Cypress.Commands.add("skipToContentAndValidate", (mainContentIdForSelector) => {
            cy.wait(1500)
            cy.get('[data-cy="skillTreeLogo"]').tab({ shift: true })
            cy.get('[data-cy="skipToContentButton"]').should('be.visible')
            cy.get('[data-cy="skipToContentButton"]').should('have.focus')
            cy.get('[data-cy="skipToContentButton"]').type("{enter}")
            cy.get(mainContentIdForSelector).should('have.focus')
        });
    });

    it('skip to content on progress and ranking', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
        cy.skipToContentAndValidate('#mainContent2')
    });

    it('skip to content on skills display', () => {
        cy.addToMyProjects(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="totalPoints"]').should('have.text', '800');
        cy.skipToContentAndValidate('#mainContent2')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="totalPoints"]').should('have.text', '800');
        cy.skipToContentAndValidate('#mainContent2')
    });

    it('skip to content on project admin', () => {
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);

        cy.visit('/administrator/');
        cy.get('[data-cy="projCard_proj1_manageBtn"]')
        cy.skipToContentAndValidate('#mainContent2')

        cy.visit('/administrator/projects/proj1/')
        cy.get('[data-cy="manageBtn_subj1"]')
        cy.skipToContentAndValidate('#mainContent2')

        cy.visit('/administrator/projects/proj1/subjects/subj1/')
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.skipToContentAndValidate('#mainContent2')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/')
        cy.get('[data-cy="mediaInfoCardTitle"]')
        cy.skipToContentAndValidate('#mainContent2')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users')
        cy.get('[data-cy="users-filterBtn"]')
        cy.skipToContentAndValidate('#mainContent2')
    });

    it('skip to content on project admin - metrics', () => {
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);

        cy.visit('/administrator/projects/proj1/metrics/')
        cy.contains('Users per day')
        cy.skipToContentAndValidate('#mainContent3')

        cy.visit('/administrator/projects/proj1/metrics/achievements')
        cy.get('[data-cy="achievementsNavigator-filterBtn"]')
        cy.skipToContentAndValidate('#mainContent3')
    });

    it('skip to content on project admin - self report', () => {
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);

        cy.visit('/administrator/projects/proj1/self-report/')
        cy.contains('No Skills Require Approval')
        cy.skipToContentAndValidate('#mainContent2')

        cy.visit('/administrator/projects/proj1/self-report/configure')
        cy.contains('The ability to split the approval workload is unavaila')
        cy.skipToContentAndValidate('#mainContent2')
    });


    it('skip to content on settings page', () => {
        cy.visit('/settings/');
        cy.get('[data-cy="generalSettingsSave"]')
        cy.skipToContentAndValidate('#mainContent2')
    });

    it('navigating to a new page should put focus right before "Skip To Content" button', () => {
        cy.createSubject(1,1)
        cy.createSkill(1,1,1)
        cy.visit('/administrator/');

        cy.wait(1500)
        cy.get('body').tab()
        cy.get('[data-cy="skipToContentButton"]').should('have.focus')

        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.get('[data-cy="manageBtn_subj1"]')
        cy.get('[data-cy="projectLastReportedSkillValue"]').should('have.text', 'Never')
        cy.wait(1500)
        cy.get('[data-cy="preSkipToContentPlaceholder"]').should('have.focus')
        cy.get('body').tab()
        cy.get('[data-cy="skipToContentButton"]').should('have.focus')

        cy.get('[data-cy="manageBtn_subj1"]').click()
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.wait(1500)
        cy.get('[data-cy="preSkipToContentPlaceholder"]').should('have.focus')
        cy.get('body').tab()
        cy.get('[data-cy="skipToContentButton"]').should('have.focus')
    });

});
