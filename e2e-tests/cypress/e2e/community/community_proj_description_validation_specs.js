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
import '../copy/copy_commands'

describe('Community Project Creation Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('project description is validated against custom validators', () => {
        cy.viewport(1200, 1400)
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('GET', '/app/projects/proj1/description').as('loadDescription');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');

        cy.get('[data-cy="btn_edit-project"]').click();
        cy.wait('@loadDescription');

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'ldkj aljdl aj\n\njabberwocky');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'ldkj aljdl aj\n\ndivinedragon');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').contains('Project Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{backspace}');
        cy.wait('@validateDescription');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('subject description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');

        cy.get('[data-cy="btn_Subjects"]').click();

        cy.get('[data-cy="subjectName"]').type('Great Name');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Subject Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('skill description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.openNewSkillDialog();

        cy.get('[data-cy="skillName"]').type('Great Name');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Skill Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('description validates multiple paragraphs', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.openNewSkillDialog();

        cy.get('[data-cy="skillName"]').type('Great Name');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('first\n\nsecond\n\nthird has divinedragon yes');
        cy.get('[data-cy="descriptionError"]').contains('Skill Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
    });

    it('badge description is validated against custom validators', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('GET', '/admin/projects/proj1/badges').as('loadBadges');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/badges');
        cy.wait('@loadBadges');
        cy.get('[data-cy="btn_Badges"]').click();

        cy.get('[data-cy=name]').type('Great Name');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Badge Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('self report approval messages are validated against custom validators', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/approve').as('approve');
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');
        // cy.get('[data-cy="approvalSelect_user1-skill3"]').click({ force: true });
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="approveBtn"]').should('be.enabled');
        cy.get('[data-cy="rejectBtn"]').should('be.enabled');

        cy.get('[data-cy="approveBtn"]').click();
        cy.get('[data-cy="approvalTitle"]').contains('This will approve user\'s request(s) to get points');
        cy.get('[data-cy="approvalInputMsg"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="approvalRequiredMsgError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="approvalInputMsg"]').clear().type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="approvalRequiredMsgError"]').contains('Message - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="approvalInputMsg"]').type('{backspace}');
        cy.clickSaveDialogBtn()

        cy.wait('@approve');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
        ]);
    });

    it('self report reject messages are validated against custom validators', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject').as('reject');
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
        ]);

        cy.get('[data-cy="approveBtn"]').should('be.disabled');
        cy.get('[data-cy="rejectBtn"]').should('be.disabled');
        // cy.get('[data-cy="approvalSelect_user1-skill3"]').click({ force: true });
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="approveBtn"]').should('be.enabled');
        cy.get('[data-cy="rejectBtn"]').should('be.enabled');

        cy.get('[data-cy="rejectBtn"]').click();
        cy.get('[data-cy="rejectionTitle"]').contains('This will reject user\'s request(s) to get points');
        cy.get('[data-cy="rejectionInputMsg"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="approvalRequiredMsgError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="rejectionInputMsg"]').clear().type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="approvalRequiredMsgError"]').contains('Message - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="rejectionInputMsg"]').type('{backspace}');
        cy.clickSaveDialogBtn()

        cy.wait('@reject');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
        ]);
    });

    it('self report justification is validated against custom validators', () => {
        cy.intercept('POST', '/admin/projects/proj1/approvals/reject').as('reject');
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.get('[data-cy="selfReportSubmitBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="selfReportSubmitBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Skill Description - May not contain divinedragon word');
        cy.get('[data-cy="selfReportSubmitBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="selfReportSubmitBtn"]').should('be.enabled');
    });

    it('contact project admins form shall never using community validator', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createProject(2)

        const validateDefaultCustomValidatorIsUsed = () => {
            cy.get('[data-cy="pointHistoryChartNoData"]')
            cy.get('[data-cy="myRankBtn"]')
            cy.openDialog('[data-cy="contactOwnerBtn"]', true)
            cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

            cy.get('[data-cy="contactOwnersMsgInput"]').type('ldkj aljdl aj\n\nndivinedragon');
            cy.wait(1000)
            const errorSelector = '[data-cy="messageError"]'
            cy.get(errorSelector).should('not.be.visible')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

            cy.get('[data-cy="contactOwnersMsgInput"]').type('ldkj aljdl aj\n\njabberwocky');
            cy.get(errorSelector).should('be.visible').contains('Message - paragraphs may not contain jabberwocky');
            cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

            cy.get('[data-cy="contactOwnersMsgInput"]').type('{backspace}');
            cy.get(errorSelector).should('not.be.visible')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
            cy.get('[data-cy="closeDialogBtn"]').click()
            cy.get('[data-cy="contactOwnersMsgInput"]').should('not.exist')
        }

        cy.visit('/progress-and-rankings/projects/proj1');
        validateDefaultCustomValidatorIsUsed()

        cy.visit('/progress-and-rankings/projects/proj2');
        validateDefaultCustomValidatorIsUsed()
    });

    it('contact project users form shall never using community validator', () => {
        cy.createProject(1, { enableProtectedUserCommunity: true })
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.reportSkill(1, 1, 'user1', 'now');

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1)
        cy.reportSkill(2, 1, 'user1', 'now');

        const validateDefaultCustomValidatorIsUsed = () => {
            cy.get('[data-cy="emailUsers_subject"]').type('subject');
            cy.selectItem('[data-cy="filterSelector"]', 'Project');
            cy.get('[data-cy="emailUsers-addBtn"]').click();
            cy.get('[data-cy="emailUsers-submitBtn"]').should('be.disabled')


            cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\nndivinedragon');
            cy.wait(1000)
            const errorSelector = '[data-cy="descriptionError"]'
            cy.get(errorSelector).should('not.be.visible')
            cy.get('[data-cy="emailUsers-submitBtn"]').should('be.enabled')

            cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
            cy.get(errorSelector).should('be.visible').contains('Email Body - paragraphs may not contain jabberwocky');
            cy.get('[data-cy="emailUsers-submitBtn"]').should('be.disabled')
        }

        cy.visit('/administrator/projects/proj1/contact-users');
        validateDefaultCustomValidatorIsUsed()

        cy.visit('/administrator/projects/proj2/contact-users');
        validateDefaultCustomValidatorIsUsed()
    });

    it('contact all admins form shall never using community validator', () => {
        cy.createProject(1, { enableProtectedUserCommunity: true })
        cy.createProject(2)

        cy.logout();
        cy.fixture('vars.json')
          .then((vars) => {
              cy.login(vars.rootUser, vars.defaultPass);

              cy.visit('/administrator/contactAdmins');

              cy.get('[data-cy="emailUsers_subject"]').type('subject');
              cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\nndivinedragon');
              cy.wait(1000)
              const errorSelector = '[data-cy="descriptionError"]'
              cy.get(errorSelector).should('not.be.visible')
              cy.get('[data-cy="emailUsers-submitBtn"]').should('be.enabled')

              cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
              cy.get(errorSelector).should('be.visible').contains('Email Body - paragraphs may not contain jabberwocky');
              cy.get('[data-cy="emailUsers-submitBtn"]').should('be.disabled')
          });
    });

    it('contact project admins should not submit on enter', () => {
        cy.intercept('POST', '/api/projects/*/contact', cy.spy().as('contact')).as('contactProject');
        cy.createProject(1)

        cy.visit('/progress-and-rankings/projects/proj1');

        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="myRankBtn"]')
        cy.openDialog('[data-cy="contactOwnerBtn"]', true)
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="contactOwnersMsgInput"]').type('l{enter}d{enter}k{enter}{enter}j');
        cy.wait(1000)

        cy.get('@contact').should('not.have.been.called');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="contactOwnersMsgInput"]').type('ldkj aljdl aj{enter}{enter}gjoijsgojidfsgsdfg{enter}{enter}gofdjoigjdfgdfgdf');
        cy.wait(1000)

        cy.get('@contact').should('not.have.been.called');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.clickSaveDialogBtn()
        cy.wait('@contactProject');
        cy.get('@contact').should('have.been.calledOnce');
    });

    it('video transcript is validated against custom validators', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');

        const videoFile = 'create-subject.webm';

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/${videoFile}`,  { force: true })

        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').type('{selectall}{backspace}')
        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled');

        cy.get('[data-cy="videoTranscript"]').type('{backspace}');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled');
    });

    it('video transcript is validated against custom validators - uc protected project', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');

        const videoFile = 'create-subject.webm';

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/${videoFile}`,  { force: true })

        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').type('{selectall}{backspace}')
        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript - May not contain divinedragon word');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled');

        cy.get('[data-cy="videoTranscript"]').type('{backspace}');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled');
    });

    it('copy project gracefully handles errors when a skill description does not validate', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, {description: 'jabberwocky'})
        cy.createSkill(1, 1, 3)

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('The skill with ID skill2 has a description that doesn\'t meet the validation requirements.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the skill\'s description to resolve the issue, then try copying the project again.')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').contains('skill2')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"]').contains('ID: skill2')
    });

    it('copy project gracefully handles errors when a subject description does not validate', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSubject(1, 2, {description: 'jabberwocky'});
        cy.createSkill(1, 2, 3)

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('The subject with ID subj2 has a description that doesn\'t meet the validation requirements.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the subject\'s description to resolve the issue, then try copying the project again.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').contains('subj2')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: subj2')
    });

    it('copy project gracefully handles errors when a badge description does not validate', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 3)

        cy.createBadge(1, 1, {description: 'jabberwocky'});
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('The badge with ID badge1 has a description that doesn\'t meet the validation requirements.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the badge\'s description to resolve the issue, then try copying the project again.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').contains('badge1')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: badge1')
    });

    it('copy project gracefully handles errors when video transcript does not validate', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        const vidAttr = {
            videoUrl: '/static/videos/create-quiz.mp4',
            captions: 'some',
            transcript: 'jabberwocky',
        }
        cy.saveVideoAttrs(1, 2, vidAttr)
        cy.createSkill(1, 1, 3)

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Video/Audio for Skill ID skill2 has a transcript that doesn\'t meet the validation requirements.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the skill\'s video transcript to resolve the issue, then try copying the project again.')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').contains('skill2')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: skill2')
    });

    it('prevent export of skills with invalid descriptions to catalog', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, {description: 'jabberwocky'})
        cy.createSkill(1, 1, 3)

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsNumSelected"]').should('have.text', '3');
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()

        const exportMsg = 'This will export 3 Skills to the SkillTree Catalog'
        cy.get('[data-pc-name="dialog"]').contains(exportMsg);
        cy.get('[data-cy="exportToCatalogButton"]').click();
        cy.get('[data-cy="exportFailedMessage"]').contains('The skill with ID skill2 has a description that doesn\'t meet the validation requirements')
        cy.get('[data-pc-name="dialog"]').should('not.contain', exportMsg)

        cy.get('[data-cy="exportToCatalogButton"]').should('be.disabled')
        cy.get('[data-cy="closeButton"]').should('be.enabled')
        cy.get('[data-cy="exportFailedMessage"] [data-cy="failedSkillLink"]').contains('skill2')
        cy.get('[data-cy="exportFailedMessage"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: skill2')
    });

    it('prevent subject copy if any of the skills have an invalid description', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, {description: 'jabberwocky'})
        cy.createSkill(1, 1, 3)

        cy.createProject(2)

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="btn_copy-subject"]').click();
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')

        cy.get('[data-cy="validationFailedMsg"]').should('be.visible');
        cy.get('[data-cy="validationFailedMsg"]').contains('The skill [Very Great Skill 2] has a description that doesn\'t meet the validation requirements. Please fix and try again.')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

    });

    it('prevent batch skill copy if any of the skills have an invalid description', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, {description: 'jabberwocky'})
        cy.createSkill(1, 1, 3)

        cy.createProject(2)
        cy.createSubject(2, 1)

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([0, 1, 2])

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();

        cy.get('[data-cy="validationFailedMsg"]').contains('The skill [Very Great Skill 2] has a description that doesn\'t meet the validation requirements. Please fix and try again.')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

});
