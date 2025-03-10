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

describe('Cross-project Skills Tests', () => {

    const sharedWithOtherTableSelector = '[data-cy="sharedSkillsTable"]';
    const sharedWithMeTableSelector = '[data-cy="skillsSharedWithMeCard"] [data-cy="sharedSkillsTable"]';

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'Project 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `Very Great Skill # 1`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'Project 2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj2',
            name: 'Interesting Subject 2',
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj2/skills/skill3`, {
            projectId: 'proj2',
            subjectId: 'subj2',
            skillId: `skill3`,
            name: `Very Great Skill # 3`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'Project 3'
        });
        cy.request('POST', '/admin/projects/proj3/subjects/subj3', {
            projectId: 'proj3',
            subjectId: 'subj3',
            name: 'Interesting Subject 3',
        });
        cy.request('POST', `/admin/projects/proj3/subjects/subj3/skills/skill4`, {
            projectId: 'proj3',
            subjectId: 'subj3',
            skillId: `skill4`,
            name: `Very Great Skill # 4`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        Cypress.Commands.add('shareSkill', (skillText, projText) => {
            cy.get('[data-cy="shareButton"')
                .should('be.disabled');
            cy.get('[data-cy="skillSelector"]')
                .click()
            cy.get('[data-cy="skillSelector"]')
                .type(`{selectall}${skillText}`)
            cy.get('[data-cy="skillsSelector-skillName"]').contains(skillText).first()
                .click()
            cy.get('[data-cy="shareButton"]')
                .should('be.disabled');

            cy.get('[data-cy="projectSelector"]')
                .click()
            if (projText) {
                cy.get('[data-pc-name="pcfilter"]')
                  .type(`${projText}`)
                cy.get('[data-cy="projectSelector-projectName"]').contains(projText).first()
                  .click()
            } else {
                cy.get('[data-cy="projectSelector-projectName"]').first().click()
            }
            cy.get('[data-cy="shareButton"')
                .should('be.enabled');

            cy.get('[data-cy="shareButton"')
                .click();
        });
    });

    it('share skill with another project', () => {
        cy.visit('/administrator/projects/proj1');
        cy.clickNav('Learning Path');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.shareSkill('1', '2');

        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        // -------------------------
        // Project 2 should see the skill
        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        const sharedSkillSelector = '[data-cy="skillsSelectionItem-proj1-skill1"]'
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).contains('Shared Skill:');
        cy.get(sharedSkillSelector).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')

        // -------------------------
        // Project 3 should not see the shared skill
        cy.visit('/administrator/projects/proj3/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).should('not.exist')
    });

    it('share with all projects', () => {
        cy.visit('/administrator/projects/proj1/learning-path');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.get('[data-cy="shareButton"')
            .should('be.disabled');
        cy.get('[data-cy="skillSelector"]')
            .click()
            .type('1{enter}');
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill1"]`).click()
        cy.get('[data-cy="shareButton"')
            .should('be.disabled');

        cy.get('[data-cy="shareWithAllProjectsCheckbox"]').click()

        cy.get('[data-cy="shareButton"')
            .click();

        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'All Projects'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        // -------------------------
        // Project 2 should see the skill
        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        cy.contains('No Learning Path Yet');
        const sharedSkillSelector = '[data-cy="skillsSelectionItem-proj1-skill1"]'
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).contains('Shared Skill:');
        cy.get(sharedSkillSelector).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')

        // -------------------------
        // Project 3 should see the shared skill
        cy.visit('/administrator/projects/proj3');
        cy.clickNav('Learning Path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        cy.contains('No Learning Path Yet');
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).contains('Shared Skill:');
        cy.get(sharedSkillSelector).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')
    });

    it('remove share', () => {
        cy.visit('/administrator/projects/proj1/learning-path');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.shareSkill('1', '2');
        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false);

        cy.shareSkill('2', null);
        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 2'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false, 'Shared Skill');
        //
        // cy.get(`${sharedWithOtherTableSelector} [data-cy="sharedSkillsTable-removeBtn"]`)
        //     .first()
        //     .click();
        // cy.validateTable(sharedWithOtherTableSelector, [
        //     [{
        //         colIndex: 0,
        //         value: 'Very Great Skill # 2'
        //     }, {
        //         colIndex: 1,
        //         value: 'Project 2'
        //     }],
        // ], 5, true, null, false);
        //
        // // -------------------------
        // // Project 2 should see the skill2 but not skill1
        // cy.visit('/administrator/projects/proj2/learning-path');
        // cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
        //     .contains('Share skills from this project with other projects');
        //
        // cy.validateTable(sharedWithMeTableSelector, [
        //     [{
        //         colIndex: 0,
        //         value: 'Very Great Skill # 2'
        //     }, {
        //         colIndex: 1,
        //         value: 'Project 1'
        //     }],
        // ], 5, true, null, false);
        //
        // cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        // cy.get('[data-cy="skillsSelectionItem-proj1-skill2"]')
        // cy.get('[data-cy="skillsSelectionItem-proj1-skill1"]').should('not.exist')
    });
});
