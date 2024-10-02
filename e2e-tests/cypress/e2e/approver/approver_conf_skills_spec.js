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

describe('Approver Config Skills Tests', () => {

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

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill3"]`).click()

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

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill5"]`).click()
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

    it('remove skill conf from an approver', function () {
        cy.configureApproverForSkillId(1, 'user1', 1)
        cy.configureApproverForSkillId(1, 'user1', 2)
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Skills')

        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillCell-skill1"]`)
        cy.get(`${tableSelector} [data-cy="skillCell-skill2"]`)
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')

        cy.get(`${tableSelector} [data-cy="skillCell-skill1"] [data-cy="deleteBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillCell-skill1"]`).should('not.exist')
        cy.get(`${tableSelector} [data-cy="skillCell-skill2"]`)
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('1 Specific Skill')

        cy.visit('/administrator/projects/proj1/self-report/configure');

        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillCell-skill1"]`).should('not.exist')
        cy.get(`${tableSelector} [data-cy="skillCell-skill2"]`)
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('1 Specific Skill')

        cy.get(`${tableSelector} [data-cy="skillCell-skill2"] [data-cy="deleteBtn"]`).click()
        cy.get(`${tableSelector}`).should('not.exist')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
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

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).click();
        cy.get(`[data-pc-section="filterinput"]`).type('s');
        cy.get(`[data-cy="subjectSelectionItem-proj1-subj2"]`).click()
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
        // cy.get('[data-cy="skillsAddedAlert"]').contains('Added 2 skills')
        // cy.get('[data-cy="closeSkillsAddedAlertBtn"]').click()
        // cy.get('[data-cy="skillsAddedAlert"]').should('not.exist')
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

    it('configure approver for All skill under a subject where some skills are under a group', function () {
        cy.createSubject(1, 2)
        cy.createSubject(1, 3)
        cy.createSubject(1, 4)
        cy.createSkill(1, 2, 8)
        cy.createSkillsGroup(1, 2, 5);
        cy.addSkillToGroup(1, 2, 5, 9);
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.disabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).click();
        cy.get(`[data-pc-section="filterinput"]`).type('s');
        cy.get(`[data-cy="subjectSelectionItem-proj1-subj2"]`).click()
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

    it('configured skills are paged', function () {
        for (let i = 5; i < 13; i++) {
            cy.createSkill(1, 1, i, { selfReportingType: 'Approval' })
        }
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).click();
        cy.get(`[data-cy="subjectSelectionItem-proj1-subj1"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

        cy.get(`${tableSelector} tr th`).contains('Skill').click();
        cy.get('[data-cy="skillApprovalSkillConfTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '12')

        // has 3 pages
        cy.get('[data-cy="skillCell-skill1"]')
        cy.get('[data-cy="skillCell-skill2"]')
        cy.get('[data-cy="skillCell-skill3"]')
        cy.get('[data-cy="skillCell-skill4"]')
        cy.get('[data-pc-name="paginator"] [aria-label="Page 2"]').click()
        cy.get('[data-cy="skillCell-skill5"]')
        cy.get('[data-cy="skillCell-skill6"]')
        cy.get('[data-cy="skillCell-skill7"]')
        cy.get('[data-cy="skillCell-skill8"]')
        cy.get('[data-pc-name="paginator"] [aria-label="Page 3"]').click()
        cy.get('[data-cy="skillCell-skill9"]')
        cy.get('[data-cy="skillCell-skill10"]')
        cy.get('[data-cy="skillCell-skill11"]')
        cy.get('[data-cy="skillCell-skill12"]')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('12 Specific Skill')
    });

    it('some skills in the subject already added', function () {
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get('[data-cy="skillsSelectionItem-proj1-skill3"]').click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()
        cy.get('[data-cy="skillCell-skill3"]')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).click();
        cy.get(`[data-pc-section="filterinput"]`).type('s');
        cy.get(`[data-cy="subjectSelectionItem-proj1-subj1"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')

        // re-add again
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).click();
        cy.get(`[data-pc-section="filterinput"]`).type('s');
        cy.get(`[data-cy="subjectSelectionItem-proj1-subj1"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')
    });

    it('added skills are not presented in the skills selector', function () {
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill3"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()
        cy.get('[data-cy="skillCell-skill3"]')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.wait(1000)
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill3"]`).should('not.exist')

        cy.get(`[data-cy="skillsSelectionItem-proj1-skill4"]`).should('exist');
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill4"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

        cy.get(`${tableSelector} [data-cy="skillCell-skill3"]`)
        cy.get(`${tableSelector} [data-cy="skillCell-skill4"]`)
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="subjectSelector"]`).click();
        cy.get(`[data-pc-section="filterinput"]`).type('s');
        cy.get(`[data-cy="subjectSelectionItem-proj1-subj1"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('ski');
        cy.wait(1000)
        cy.contains('No results found')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelectionItem-proj1-skill4"]`).should('not.exist');

        // refresh and re-test
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '5')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('ski');
        cy.wait(1000)
        cy.contains('No results found')
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill4"]`).should('not.exist');
    });

    it('configure approver for single skill, then update skill search should reset selected skill', function () {
        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.disabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('skill 3');
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill3"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.enabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('{backspace}');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.enabled')
    });

});
