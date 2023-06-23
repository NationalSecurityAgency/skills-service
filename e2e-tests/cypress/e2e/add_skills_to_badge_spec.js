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
describe('Add Multiple Skills to Badge Tests', () => {

    const tableSelector = '[data-cy="simpleSkillsTable"]';
    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        // cy.createBadge(1, 1);
        // cy.assignSkillToBadge(1, 1, 1);
        // cy.createBadge(1, 1, { enabled: true });
    });

    it('add multiple skills to an enabled badge', () => {
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillSelect-skill2"]')
          .click({ force: true });
        cy.get('[data-cy="skillSelect-skill3"]')
          .click({ force: true });

        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked');
        cy.get('[data-cy="skillSelect-skill2"]').should('be.checked');
        cy.get('[data-cy="skillSelect-skill3"]').should('be.checked');
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked');

        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"] [data-cy="selectDest_badge1"]')
          .click();
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
          .should('not.exist');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('2 skills will be added to the [Badge 1] badge.');
        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .click();

        // step 3
        cy.get('[data-cy="addSkillsToBadgeModalStep3"]')
          .contains('Successfully added 2 skills to the [Badge 1] badge.');
        cy.get('[data-cy="okButton"]')
          .click();

        cy.get('[data-cy="skillSelect-skill1"]').should('not.be.checked');
        cy.get('[data-cy="skillSelect-skill2"]').should('not.be.checked');
        cy.get('[data-cy="skillSelect-skill3"]').should('not.be.checked');
        cy.get('[data-cy="skillSelect-skill4"]').should('not.be.checked');

        // verify badge contains all skills
        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get(`${tableSelector} th`)
          .contains('Skill ID')
          .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
        ], 5);

        cy.get('[data-cy=statPreformatted]')
          .contains('Live')
          .should('exist');
    });

    it('add multiple skills to a disabled badge', () => {
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });
        cy.createBadge(1, 2, { enabled: false });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="selectAllSkillsBtn"]')
          .click();
        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"] [data-cy="selectDest_badge2"]')
          .click();
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
          .should('not.exist');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('4 skills will be added to the [Badge 2] badge.');
        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .click();

        // step 3
        cy.get('[data-cy="addSkillsToBadgeModalStep3"]')
          .contains('Successfully added 4 skills to the [Badge 2] badge.');
        cy.get('[data-cy="okButton"]')
          .click();

        // verify badge contains all skills
        cy.visit('/administrator/projects/proj1/badges/badge2');
        cy.get(`${tableSelector} th`)
          .contains('Skill ID')
          .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill4'
            }],
        ], 5);

        cy.get('[data-cy=statPreformatted]')
          .contains('Disabled')
          .should('exist');
    });

    it('add multiple skills to a badge, some skills already added to badge', () => {
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="selectAllSkillsBtn"]')
          .click();
        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"] [data-cy="selectDest_badge1"]')
          .click();
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
          .should('not.exist');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('3 skills will be added to the [Badge 1] badge.');
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('1 selected skill has already been added to that badge!');
        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .click();

        // step 3
        cy.get('[data-cy="addSkillsToBadgeModalStep3"]')
          .contains('Successfully added 3 skills to the [Badge 1] badge.');
        cy.get('[data-cy="okButton"]')
          .click();
    });

    it('attempt to add multiple skills to a badge, all skills already added to badge', () => {
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="selectAllSkillsBtn"]')
          .click();
        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"] [data-cy="selectDest_badge1"]')
          .click();
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
          .should('not.exist');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('Selected skills can NOT be added to the Badge 1 badge');
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('Please cancel and select different skills.');
        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .should('not.enabled');
        cy.get('[data-cy="closeButton"]')
          .should('be.enabled');
        cy.get('[data-cy="okButton"]')
          .should('not.exist');
        cy.get('[data-cy="closeButton"]')
          .click();
        cy.get('[data-cy="addSkillsToBadgeModalStep3"]')
          .should('not.exist');
    });

    it('attempt to add multiple skills to a badge, but no badges exist', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="selectAllSkillsBtn"]')
          .click();
        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
            .contains('There are no Badges available. A badge must be created before adding skills to it.');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .should('not.exist');
        cy.get('[data-cy="addSkillsToBadgeModalStep3"]')
          .should('not.exist');
    });

    it('attempt to add multiple skills to a badge, where one of the skills is already is already a learning path dependency to the badge', () => {
        cy.createBadge(1, 5);
        cy.assignSkillToBadge(1, 5, 1);
        cy.createBadge(1, 5, { enabled: true });

        cy.addLearningPathItem(1, 2, 5, false, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="selectAllSkillsBtn"]')
          .click();
        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"] [data-cy="selectDest_badge5"]')
          .click();
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
          .should('not.exist');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('Selected skills can NOT be added to the Badge 5 badge');
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('Please cancel and select different skills.');
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('Unable to add Very Great Skill 2 skill to the badge. Adding this skill would result in a circular/infinite learning path. Please visit project\'s Learning Path page to review.');

        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .should('not.enabled');
        cy.get('[data-cy="closeButton"]')
          .should('be.enabled');
        cy.get('[data-cy="okButton"]')
          .should('not.exist');
        cy.get('[data-cy="closeButton"]')
          .click();
        cy.get('[data-cy="addSkillsToBadgeModalStep3"]')
          .should('not.exist');
    });

    it('attempt to add multiple skills to a badge, introducing a circular learning path dependency', () => {
        // skill1 -> badge1[skill2]  ; skill2 -> sill3 -> badge2[skill4], then attempt to add skill1 to badge2
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 2, 4);
        cy.createBadge(1, 1, { enabled: true });
        cy.createBadge(1, 2, { enabled: true });

        cy.addLearningPathItem(1, 1, 1, false, true)
        cy.addLearningPathItem(1, 2, 3)
        cy.addLearningPathItem(1, 3, 2, false, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');
        cy.get('[data-cy="manageSkillLink_skill4"]');


        cy.get('[data-cy="skillSelect-skill1"]')
          .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillAddToBadgeBtn"]')
          .click();

        // step 1
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]');
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"] [data-cy="selectDest_badge2"]')
          .click();
        cy.get('[ data-cy="addSkillsToBadgeModalStep1"]')
          .should('not.exist');

        // step 2
        cy.get('[ data-cy="addSkillsToBadgeModalStep2"]')
          .contains('1 skill will be added to the [Badge 2] badge.');
        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .click();

        // step 3
        cy.get('[data-cy="learningPathErrMsg"]')
          .contains('Failed to add Very Great Skill 1 skill to the badge. Adding this skill would result in a circular/infinite learning path. Please visit project\'s Learning Path page to review.');

        cy.get('[data-cy="closeButton"]')
          .should('be.enabled');
        cy.get('[data-cy="addSkillsToBadgeButton"]')
          .should('not.exist');
    });

});
