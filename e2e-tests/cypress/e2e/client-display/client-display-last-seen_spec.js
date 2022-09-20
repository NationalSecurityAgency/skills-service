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
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/?internalBackButton=true')
        cy.cdClickSubj(0);

        cy.get('[data-cy="lastViewedIndicator"]').should('not.exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"]').should('exist');
        cy.get('[data-cy="skillProgress_index-0"]').click();
        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="lastViewedIndicator"]').should('exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.get('[id=skillProgressTitle-skill1]').should('have.focus');
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
        cy.get('[data-cy="skillProgress_index-0"]').should('exist');
        cy.get('[data-cy="skillProgress_index-0"]').click();
        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');

        cy.get('[data-cy=back]').click()
        cy.get('[data-cy="lastViewedIndicator"]').should('exist');
        cy.get('[data-cy="jumpToLastViewedButton"]').should('exist');
        cy.get('[id=skillProgressTitle-skill1]').should('have.focus');
    });
});
