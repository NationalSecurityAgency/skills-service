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
var moment = require('moment-timezone');

describe('Self Report Skills Management Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })
    });

    it('manage self reporting settings at project level', () => {
        cy.visit('/projects/proj1/settings');

        cy.get('[data-cy="selfReportSwitch"]').should('not.be.checked');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="selfReportSwitch"]').should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');

        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force:true})
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');

        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');

        // refresh and check that the values persisted
        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist')

        // disable skill, refresh and validate
        cy.get('[data-cy="selfReportSwitch"]').uncheck({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes')
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');

        // enable then disable should disable save button
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="selfReportSwitch"]').uncheck({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');

        // enabled and save
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');

        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force:true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').click({force:true});
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');

        cy.get('[data-cy="selfReportSwitch"]').uncheck({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
    });

    it('create skills - self reporting disabled - no project level default', () => {
        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy=skillName]').type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
    });

    it('create skills - self reporting with approval - no project level default', () => {
        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportEnableCheckbox"]').check({force: true})
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy=skillName]').type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
    });

    it('create skills - self reporting with Honor System - no project level default', () => {
        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportEnableCheckbox"]').check({force: true})
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force: true});
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');

        cy.get('[data-cy=skillName]').type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');
    });

    it('create skill - project level default of Honor System', () => {
        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force:true})
        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');
    });

    it('create skill - project level default of Approval', () => {
        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
    });

    it('skill overview - display self reporting card', () => {
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `Very Great Skill # 1`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
            selfReportType:	'Approval'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
            selfReportType:	'HonorSystem'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill3`,
            name: `Very Great Skill # 3`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]').contains('Self Report: Approval');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Users can self report this skill and will go into an approval queue');

        cy.visit('/projects/proj1/subjects/subj1/skills/skill2');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]').contains('Self Report: Honor System');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Users can self report this skill and will apply immediately');

        cy.visit('/projects/proj1/subjects/subj1/skills/skill3');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]').contains('Self Report: Disabled');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Self reporting is disabled for this skill');
    });

    it('sorting and paging of the approval table', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user5Good@skills.org', '2020-09-13 11:00')
        cy.reportSkill(1, 3, 'user4Good@skills.org', '2020-09-14 11:00')
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00')
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1Good@skills.org', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0Good@skills.org', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        const expected = [
            [{ colIndex: 0,  value: 'user0good@skills.org ' }, { colIndex: 2,  value: '2020-09-18 11:00' }],
            [{ colIndex: 0,  value: 'user1good@skills.org ' }, { colIndex: 2,  value: '2020-09-17 11:00' }],
            [{ colIndex: 0,  value: 'user2good@skills.org ' }, { colIndex: 2,  value: '2020-09-16 11:00' }],
            [{ colIndex: 0,  value: 'user3good@skills.org ' }, { colIndex: 2,  value: '2020-09-15 11:00' }],
            [{ colIndex: 0,  value: 'user4good@skills.org ' }, { colIndex: 2,  value: '2020-09-14 11:00' }],
            [{ colIndex: 0,  value: 'user5good@skills.org ' }, { colIndex: 2,  value: '2020-09-13 11:00' }],
            [{ colIndex: 0,  value: 'user6good@skills.org ' }, { colIndex: 2,  value: '2020-09-12 11:00' }],
        ]
        const expectedReversed = [...expected].reverse();

        cy.validateTable(tableSelector, expected);

        cy.get(`${tableSelector} th`).contains('Requested On').click();
        cy.validateTable(tableSelector, expectedReversed);

        cy.get(`${tableSelector} th`).contains('User Id').click();
        cy.validateTable(tableSelector, expected);
        cy.get(`${tableSelector} th`).contains('User Id').click();
        cy.validateTable(tableSelector, expectedReversed);
    });

    it('change page size of the approval table', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user5Good@skills.org', '2020-09-13 11:00')
        cy.reportSkill(1, 3, 'user4Good@skills.org', '2020-09-14 11:00')
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00')
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1Good@skills.org', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0Good@skills.org', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');
        const rowSelector = '[data-cy="skillsReportApprovalTable"] tbody tr';
        cy.get(rowSelector).should('have.length', 5)

        cy.get('[data-cy="skillsBTablePageSize"]').select('10');
        cy.get(rowSelector).should('have.length', 7)
    });

    it('approve one', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0' }],
            [{ colIndex: 0,  value: 'user1' }],
            [{ colIndex: 0,  value: 'user2' }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');
        cy.get('[data-cy="approvalSelect_user1-skill3"]').click({force: true});
        cy.get('[data-cy="approveBtn"]').should('be.enabled');
        cy.get('[data-cy="rejectBtn"]').should('be.enabled');

        cy.get('[data-cy="approveBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0' }],
            [{ colIndex: 0,  value: 'user2' }],
        ]);

        cy.visit('/projects/proj1/users/user1/skillEvents');
        cy.validateTable('[data-cy="performedSkillsTable"]', [
            [{ colIndex: 0,  value: 'skill3' }],
        ]);
    });

    it('reject one', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject', (req) => {
            expect(req.body.rejectionMessage).to.include('Rejection message!')
        }).as('reject');

        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0' }],
            [{ colIndex: 0,  value: 'user1' }],
            [{ colIndex: 0,  value: 'user2' }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');
        cy.get('[data-cy="approvalSelect_user1-skill3"]').click({force: true});
        cy.get('[data-cy="approveBtn"]').should('be.enabled');
        cy.get('[data-cy="rejectBtn"]').should('be.enabled');

        cy.get('[data-cy="rejectBtn"]').click();
        cy.get('[data-cy="rejectionTitle"]').contains('This will permanently reject user\'s request(s) to get points')
        cy.get('[data-cy="rejectionInputMsg"]').type('Rejection message!');
        cy.get('[data-cy="confirmRejectionBtn"]').click();

        cy.wait('@reject')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0' }],
            [{ colIndex: 0,  value: 'user2' }],
        ]);

        cy.visit('/projects/proj1/users/user1/skillEvents');
        cy.get('[data-cy="performedSkillsTable"] tbody tr').should('have.length', 0)
    });


    it('custom validation for rejection message', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject', (req) => {
            expect(req.body.rejectionMessage).to.include('Rejection jabberwoc')
        }).as('reject');

        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');

        cy.get('[data-cy="approvalSelect_user1-skill3"]').click({force: true});
        cy.get('[data-cy="rejectBtn"]').click();
        cy.get('[data-cy="rejectionTitle"]').contains('This will permanently reject user\'s request(s) to get points')

        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.').should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.enabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('Rejection jabber');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.').should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.enabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('wock');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.').should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.enabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('y');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]').type(' ok');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('{backspace}{backspace}{backspace}');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('{backspace}{backspace}');
        cy.get('[data-cy="rejectionInputMsgError"]').contains('Rejection Message - paragraphs may not contain jabberwocky.').should('not.exist');
        cy.get('[data-cy="confirmRejectionBtn"]').should('be.enabled');


        cy.get('[data-cy="confirmRejectionBtn"]').click();
        cy.wait('@reject')
    });

    it('approve 1 page worth of records', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');

        cy.get('[data-cy="selectPageOfApprovalsBtn"]').click();
        cy.get('[data-cy="approveBtn"]').click();

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5' }],
            [{ colIndex: 0,  value: 'user6' }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');

        cy.visit('/projects/proj1/users');
        cy.validateTable('[data-cy="usersTable"]',  [
            [{ colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: '100' }],
            [{ colIndex: 0,  value: 'user1' }, { colIndex: 1,  value: '100' }],
            [{ colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: '100' }],
            [{ colIndex: 0,  value: 'user3' }, { colIndex: 1,  value: '100' }],
            [{ colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: '100' }],
        ]);
    });

    it('reject 1 page worth of records', () => {
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/approvals/reject',
        }).as('reject');

        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')

        cy.visit('/projects/proj1/self-report');

        cy.get('[data-cy="selectPageOfApprovalsBtn"]').click();

        cy.get('[data-cy="rejectBtn"]').click();
        cy.get('[data-cy="rejectionTitle"]').contains('This will permanently reject user\'s request(s) to get points')
        cy.get('[data-cy="rejectionInputMsg"]').type('Rejection message!');
        cy.get('[data-cy="confirmRejectionBtn"]').click();

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5' }],
            [{ colIndex: 0,  value: 'user6' }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');

        cy.visit('/projects/proj1/users');
        cy.get('[data-cy="usersTable"] tbody tr').should('have.length', 0)
    });

    it('select page and then clear', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');

        cy.visit('/projects/proj1/self-report');

        cy.get('[data-cy="selectPageOfApprovalsBtn"]').click();
        cy.get('[data-cy="approveBtn"]').should('be.enabled');
        cy.get('[data-cy="rejectBtn"]').should('be.enabled');

        cy.get('[data-cy="clearSelectedApprovalsBtn"]').click();
        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');
    });

    it('refresh button should pull from server', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.reportSkill(1, 2, 'user1', '2020-09-12 11:00');

        cy.visit('/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user1' }],
        ]);

        cy.reportSkill(1, 2, 'user2', '2020-09-11 11:00');

        cy.get('[data-cy="syncApprovalsBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user1' }],
            [{ colIndex: 0,  value: 'user2' }],
        ]);
    });

    it('self report stats', () => {
        cy.createSkill(1, 1, 1, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/projects/proj1/self-report');

        cy.get('[data-cy="selfReportInfoCardCount_Disabled"]').contains('1');
        cy.get('[data-cy="selfReportInfoCardCount_Approval"]').contains('3');
        cy.get('[data-cy="selfReportInfoCardCount_HonorSystem"]').contains('2');
    });

    it('do not display approval table if no approval configured', () => {
        cy.createSkill(1, 1, 4, { selfReportType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/projects/proj1/self-report');

        cy.get('[data-cy="selfReportInfoCardCount_Disabled"]').contains('1');
        cy.get('[data-cy="selfReportInfoCardCount_Approval"]').contains('0');
        cy.get('[data-cy="selfReportInfoCardCount_HonorSystem"]').contains('2');

        cy.get('[data-cy="noApprovalTableMsg"]').contains('No Skills Require Approval');
        cy.get( '[data-cy="skillsReportApprovalTable"]').should('not.exist');
    });

});

