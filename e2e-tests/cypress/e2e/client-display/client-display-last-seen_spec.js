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
describe('Client Display Skills Last Viewed', () => {

    it('visiting a skill shows the Last Viewed indicator on the Subject page', () => {
        cy.intercept('/api/projects/proj1/subjects/subj1/pointHistory').as('getSubjectPointsHistory')
        cy.intercept('/api/projects/proj1/pointHistory').as('getProjPointsHistory')
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/?internalBackButton=true')
        cy.wait('@getProjPointsHistory')
        cy.cdClickSubj(0);
        cy.wait('@getSubjectPointsHistory')

        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressBar"]').should('exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressBar"]').click();
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1');

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="lastViewedIndicator"]').should('exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.get('#skillProgressTitleLink-skill1').should('have.focus');
    });

    it('skills-client: visiting a skill shows the Last Viewed indicator on the Subject page', () => {
        cy.on('uncaught:exception', (err, runnable) => {
            // cy.log(err.message)
            if (err.message.includes('Handshake Reply Failed')) {
                return false
            }
            return true
        })

        cy.intercept('/api/projects/proj1/subjects/subj1/pointHistory').as('getSubjectPointsHistory')
        cy.intercept('/api/projects/proj1/pointHistory').as('getProjPointsHistory')
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.visit('/test-skills-client/proj1')
        cy.wait('@getProjPointsHistory')
        cy.wrapIframe().find('[data-cy="subjectTileBtn"]').click()
        cy.wait('@getSubjectPointsHistory')

        cy.wrapIframe().find('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.wrapIframe().find('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressBar"]').should('exist');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressBar"]').click();
        cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1');

        cy.wrapIframe().find('[data-cy=back]').click()
        cy.wrapIframe().find('[data-cy="lastViewedIndicator"]').should('exist');
        cy.wrapIframe().find('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.wrapIframe().find('#skillProgressTitleLink-skill1').should('have.focus');
    });

    it('visiting a skill shows the Last Viewed indicator on the Subject page with skills and groups', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 10,
            numPerformToCompletion: 5
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 15,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 4, {
            pointIncrement: 15,
            numPerformToCompletion: 2
        });
        cy.createSkill(1, 1, 5);
        cy.createSkill(1, 1, 6);

        cy.cdVisit('/?internalBackButton=true')
        cy.cdClickSubj(0);

        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').should('exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click();
        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="lastViewedIndicator"]').should('exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.get('#skillProgressTitleLink-skill1').should('have.focus');
    });

    it('visiting a skill shows the Last Viewed indicator on the Subject page in a group', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 10,
            numPerformToCompletion: 5
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 15,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 15,
            numPerformToCompletion: 2
        });
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.cdVisit('/?internalBackButton=true')
        cy.cdClickSubj(0);

        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]').should('exist');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]').click();

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="lastViewedIndicator"]').should('exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.get('#skillProgressTitleLink-skill1').should('have.focus');
    });

    it('visiting a skill shows the Last Viewed indicator on the Badges page', () => {
        cy.createProject(1);
        cy.createBadge(1, 1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' });

        cy.cdVisit('/?internalBackButton=true')
        cy.cdClickBadges();
        cy.get('[data-cy=badgeDetailsLink_badge1]').click();

        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').should('exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click();
        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="lastViewedIndicator"]').should('exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.get('#skillProgressTitleLink-skill1').should('have.focus');
    });

    it('Global badges do not have last viewed features', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.contains('Global Badge 1');
        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.contains('Global Badge 1')
            .should('be.visible');
        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProgressTitle"]').click();

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');

    });

});
