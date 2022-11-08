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

describe('Approver Config User Tags Tests', () => {

    beforeEach(() => {
        cy.createProject(1)
        cy.enableProdMode(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' })

        cy.reportSkill(1, 1, 'userA', 'yesterday');
        cy.reportSkill(1, 1, 'userA', 'now');
        cy.reportSkill(1, 1, 'userB', 'now');
        cy.reportSkill(1, 1, 'userC', 'now');

        const pass = 'password';
        cy.register('user1', pass);
        cy.register('user2', pass);
        cy.register('user3', pass);
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
    });

    it('configure approver for single skill', function () {
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.disabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('skill 3');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelectionItem-proj1-skill3"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.enabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('not.exist')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 3'
            }],
        ], 5);

        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('1 Specific Skill')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.disabled')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).should('not.have.value')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('skill 5');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelectionItem-proj1-skill5"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 5'
            }],
            [{
                colIndex: 0,
                value: 'Skill 3'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Skill')

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 5'
            }],
            [{
                colIndex: 0,
                value: 'Skill 3'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Skill')
    });

    it('configure approver for All skill under a subject', function () {
        cy.createSubject(1, 2)
        cy.createSubject(1, 3)
        cy.createSubject(1, 4)
        cy.createSkill(1, 2, 8)
        cy.createSkill(1, 2, 9)
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.disabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).type('s');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelectionItem-proj1-subj2"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.enabled')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

        cy.get(`${tableSelector} tr th`).contains('Skill').click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.disabled')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 8'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }],
        ], 5);
        cy.get('[data-cy="skillsAddedAlert"]').contains('Added 2 skills')
        cy.get('[data-cy="closeSkillsAddedAlertBtn"]').click()
        cy.get('[data-cy="skillsAddedAlert"]').should('not.exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Skill')

        // refresh and validate
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Skill')
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 8'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }],
        ], 5);
    });


    // it('configure approver for skills', function () {
    //
    //     for (let i = 5; i < 80; i++) {
    //         cy.createSkill(1, 1, i, { selfReportingType: 'Approval' })
    //     }
    //
    //     cy.visit('/administrator/projects/proj1/self-report/configure');
    // });

});
