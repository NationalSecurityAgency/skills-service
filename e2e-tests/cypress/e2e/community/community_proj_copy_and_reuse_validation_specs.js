/*
 * Copyright 2025 SkillTree
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

    it('gracefully handle skill reuse if any of the skills have an invalid description', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'test project' })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, {description: 'jabberwocky'})
        cy.createSkill(1, 1, 3)

        cy.createSubject(1, 2);

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsReuseModal([0, 1, 2])

        // step 1
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        cy.get('[data-cy="failedReuseMessage"]')
            .contains('The skill with ID skill2 has a description that doesn\'t meet the validation requirements.')
        cy.get('[data-cy="failedReuseMessage"]')
            .contains('Please update the skill\'s description to resolve the issue, then try reusing again.')
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]').should('be.disabled')

        cy.get('[data-cy="failedReuseMessage"] [data-cy="failedSkillLink"]').contains('skill2')
        cy.get('[data-cy="failedReuseMessage"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"]').contains('ID: skill2')
    });

    it('gracefully handle skill reuse if any of the skills video transcript are invalid', () => {
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

        cy.createSubject(1, 2);

        cy.execSql(`delete from settings where project_id='proj1' and setting='user_community'`, true)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsReuseModal([0, 1, 2])

        // step 1
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        cy.get('[data-cy="failedReuseMessage"]')
            .contains('The skill with ID skill2 has a video/audio transcript that doesn\'t meet the validation requirements.')
        cy.get('[data-cy="failedReuseMessage"]')
            .contains('Please update the skill\'s video/audio transcript to resolve the issue, then try reusing again.')
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]').should('be.disabled')
        cy.get('[data-cy="failedReuseMessage"] [data-cy="failedSkillLink"]').contains('skill2')
        cy.get('[data-cy="failedReuseMessage"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"]').contains('ID: skill2')
    });

});
