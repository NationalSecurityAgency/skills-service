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
describe('Contact Project Users Specs', () => {

    beforeEach(() => {
        cy.logout();
        cy.resetEmail();

        cy.fixture('vars.json')
            .then((vars) => {
                cy.register(vars.rootUser, vars.defaultPass, true);
            });

        cy.login('root@skills.org', 'password');

        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                host: 'localhost',
                port: 1025,
                'protocol': 'smtp',
                publicUrl: 'http://localhost:8082/',
                fromEmail: 'noreploy@skilltreeemail.org',
            },
        });

        cy.request({
            method: 'POST',
            url: '/root/saveSystemSettings',
            body: {
                resetTokenExpiration: 'PT2H',
            }
        });

        cy.logout();

        cy.register('user1', 'password1', false);
        cy.login('user1', 'password1');
    });

    it('contact user form query interactions', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            enabled: true,
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        const numSkills = 15;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `Skill ${i}`,
                pointIncrement: '50',
                numPerformToCompletion: 1
            });

            if (i <= 3) {
                cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill${i}`);
            }
        }
        // cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1);

        // user 1 achieved badge
        cy.reportSkill(1, 0, 'user1', 'now');
        cy.reportSkill(1, 1, 'user1', 'now');
        cy.reportSkill(1, 2, 'user1', 'now');
        cy.reportSkill(1, 3, 'user1', 'now');

        cy.reportSkill(1, 7, 'user2', 'now');
        cy.reportSkill(1, 8, 'user2', 'now');
        cy.reportSkill(1, 9, 'user2', 'now');

        cy.reportSkill(1, 5, 'user3', 'now');
        cy.reportSkill(1, 6, 'user3', 'now');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
            .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
            .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');

        cy.get('[data-cy="nav-Contact Users"]')
            .click();
        cy.wait('@emailSupported');

        cy.selectItem('[data-cy="filterSelector"]', 'Project');

        cy.get('[data-cy=emailUsers-levelsInput]').should('exist');
        cy.get('[data-cy=emailUsers-addBtn]').click();
        cy.wait('@updateCount');
        cy.get('[data-cy=filterBadge]').eq(0).contains('All Users');
        cy.get('[data-cy=usersMatchingFilters] .p-badge').should('have.text', '3');
        cy.get('[data-cy=emailUsers-addBtn]').should('be.disabled');
        cy.get('[data-cy="emailUsers-levelsInput"]').should('not.be.enabled');

        cy.get('[data-cy=filterSelector]').should('not.be.enabled');
        cy.get('[data-cy=emailUsers-submitBtn]').should('be.disabled');

        cy.get('[data-cy=filterBadge]').eq(0).contains('All Users').get('[data-pc-section="removeicon"]').click();
        cy.contains('All Users').should('not.exist');
        cy.wait('@updateCount');
        cy.get('[data-cy=filterSelector]').should('not.be.disabled');

        cy.get('[data-cy=emailUsers-submitBtn]').should('be.disabled');
        cy.get('[data-cy=usersMatchingFilters] .p-badge').should('have.text', '0');

        cy.selectItem('[data-cy="emailUsers-levelsInput"]', '1');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.get('[data-cy=filterBadge]').eq(0).contains('Level 1 or greater');
        cy.get('[data-cy=usersMatchingFilters] .p-badge').should('have.text', '3');
        cy.get('[data-cy=filterBadge]').eq(0).get('[data-pc-section="removeicon"]').click();
        cy.contains('Level 1 or greater').should('not.exist');

        cy.selectItem('[data-cy="emailUsers-levelsInput"]', '5');

        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.get('[data-cy=filterBadge]')
            .eq(0)
            .contains('Level 5 or greater');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '0');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.disabled');
        cy.get('[data-cy=filterBadge]').eq(0).get('[data-pc-section="removeicon"]').click();

        cy.selectItem('[data-cy="filterSelector"]', 'Badge');
        cy.wait(200);
        cy.selectItem('[data-cy="name-selector"]', 'Badge 1');

        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.enabled');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.contains('Achieved Badge Badge 1');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '1');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.disabled');
        cy.get('[data-cy=filterBadge]').eq(0).get('[data-pc-section="removeicon"]').click();
        cy.wait('@updateCount');
        cy.contains('Achieved Badge Badge 1')
            .should('not.exist');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '0');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.disabled');

        cy.selectItem('[data-cy="filterSelector"]', 'Subject');

        cy.wait(200);
        cy.selectItem('[data-cy="name-selector"]', 'Subject 1');

        cy.wait('@getSubjectLevels');
        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.disabled');
        cy.selectItem('[data-cy="emailUsers-levelsInput"]', '1');
        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.enabled');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.contains('Level 1 or greater in Subject Subject 1')
            .should('be.visible');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '3');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.disabled');
        // should not be able to add multiple levels for the same subject
        cy.selectItem('[data-cy="filterSelector"]', 'Badge');

        cy.wait(200);
        cy.selectItem('[data-cy="filterSelector"]', 'Subject');

        cy.wait(200);
        cy.selectItem('[data-cy="name-selector"]', 'Subject 1');

        cy.wait('@getSubjectLevels');
        cy.selectItem('[data-cy="emailUsers-levelsInput"]', '2');
        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.enabled');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.get('[data-cy=filterExists]')
            .should('be.visible');
        cy.contains('Level 2 or greater in Subject Subject 1')
            .should('not.exist');

        cy.get('[data-cy=filterBadge]').eq(0).get('[data-pc-section="removeicon"]').click();

        cy.wait('@updateCount');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '0');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.disabled');

        cy.selectItem('[data-cy="filterSelector"]', 'Skill');
        cy.wait(200);
        cy.get('[data-cy=emailUsers-levelsInput]').should('not.exist');
        cy.selectItem('[data-cy="name-selector"]', 'Skill 1');

        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.enabled');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '1');
        cy.get('[data-cy=filterBadge]').eq(0).get('[data-pc-section="removeicon"]').click();

        cy.wait('@updateCount');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '0');

        cy.selectItem('[data-cy="filterSelector"]', 'Skill');
        cy.wait(200);
        cy.get('[data-cy=emailUsers-levelsInput]').should('not.exist');
        cy.selectItem('[data-cy="name-selector"]', 'Skill 1');

        cy.get('[data-cy=skillAchievedSwitch]')
            .click();
        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.enabled');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.contains('Not Achieved Skill Skill 1')
            .should('be.visible');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '2');

        cy.selectItem('[data-cy="name-selector"]', 'Skill 1');

        cy.get('[data-cy=skillAchievedSwitch]')
            .click();
        cy.get('[data-cy=emailUsers-addBtn]')
            .should('be.enabled');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.get('[data-cy=filterExists]')
            .should('be.visible');
        cy.contains('Not Achieved Skill Skill 1')
            .should('have.length', 1);

        const addSkillCriteria = (skillName) => {
            cy.selectItem('[data-cy="name-selector"]', skillName);
            cy.get('[data-cy=emailUsers-addBtn]').click();
        };

        addSkillCriteria('Skill 0');
        addSkillCriteria('Skill 1');
        addSkillCriteria('Skill 2');
        addSkillCriteria('Skill 3');
        addSkillCriteria('Skill 4');
        addSkillCriteria('Skill 5');
        addSkillCriteria('Skill 6');
        addSkillCriteria('Skill 7');
        addSkillCriteria('Skill 8');
        addSkillCriteria('Skill 9');
        addSkillCriteria('Skill 10');
        addSkillCriteria('Skill 11');
        addSkillCriteria('Skill 12');
        addSkillCriteria('Skill 13');

        cy.get('[data-cy=maxFiltersReached]')
            .should('be.visible')
            .contains('Only 15 filters are allowed');

        //make sure the correct item is removed
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(1).click();
        cy.contains('Achieved Skill Skill 0').should('not.exist');
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').should('have.length', 14);
        cy.get('[data-cy=maxFiltersReached]').should('not.exist');

        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').eq(0).click();
        cy.get('[data-cy=filterBadge] [data-pc-section="removeicon"]').should('have.length', 0);

        cy.selectItem('[data-cy="filterSelector"]', 'Project');
        cy.get('[data-cy=emailUsers-addBtn]')
            .click();
        cy.wait('@updateCount');
        cy.get('[data-cy=filterBadge]')
            .eq(0)
            .contains('All Users');
        cy.get('[data-cy=usersMatchingFilters] .p-badge')
            .should('have.text', '3');

        cy.get('[data-cy=emailUsers_subject]')
            .type('Test Subject');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]')
            .type('Test Body');
        cy.get('[data-cy=emailUsers-submitBtn]')
            .should('be.enabled');

        cy.intercept('POST', '/admin/projects/proj1/contactUsers', {
            statusCode: 200,
            body: {
                success: true
            }
        });
        cy.get('[data-cy=emailUsers-submitBtn]')
            .click();
        cy.get('[data-cy=emailSent]')
            .should('be.visible');
    });

    it('email not enabled on instance', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            enabled: true,
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.visit('/administrator/projects/proj1/');
        cy.contains('Contact Users')
            .click();
        cy.get('[data-cy=contactUsers_emailServiceWarning]')
            .should('be.visible');
        cy.contains('Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.')
            .should('be.visible');
    });

    it('preview email button', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        cy.intercept('POST', '/admin/projects/proj1/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
            .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
            .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');

        cy.get('[data-cy="nav-Contact Users"]')
            .click();
        cy.wait('@emailSupported');

        cy.get('[data-cy=previewUsersEmail]')
            .should('be.disabled');
        cy.get('[data-cy=emailUsers_subject]')
            .type('Test Subject');
        cy.get('[data-cy=previewUsersEmail]')
            .should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]')
            .type('Test Body');
        cy.get('[data-cy=previewUsersEmail]')
            .should('be.enabled');

        cy.get('[data-cy=previewUsersEmail]')
            .click();
        cy.get('[data-cy=emailSent]')
            .should('be.visible');
    });

    it('attachments are not enabled', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
          .as('createProject');

        cy.intercept('POST', '/admin/projects/proj1/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
          .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
          .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');

        cy.get('[data-cy="nav-Contact Users"]')
          .click();
        cy.wait('@emailSupported');

        cy.get(`button.bold`).should('exist');
        cy.get(`button.attachment-button`).should('not.exist');
    });

    it('attachments cannot be uploaded via drag and drop', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
          .as('createProject');

        cy.intercept('POST', '/admin/projects/proj1/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
          .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
          .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');

        cy.get('[data-cy="nav-Contact Users"]')
          .click();
        cy.wait('@emailSupported');
        const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]';
        cy.get(markdownInput).focus().selectFile('cypress/attachments/test-pdf.pdf', { action: 'drag-drop' })

        cy.get('a[href^="/api/download/"]:contains(test-pdf.pdf)').should('not.exist');
    });

    it('validation works correctly', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        cy.intercept('POST', '/admin/projects/proj1/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
            .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
            .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');
        cy.get('[data-cy="nav-Contact Users"]')
            .click();
        cy.wait('@emailSupported');

        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');

        cy.get('[data-cy="emailUsers_subject"]').type('test');
        cy.get('[data-cy="emailUsers_body"]').type('test');
        cy.get('[data-cy="previewUsersEmail"]').should('be.enabled');

        cy.get('[data-cy="emailUsers_subject"]').type('jabberwocky');
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');
        cy.get('[data-cy="subjectLineError"]').contains('paragraphs may not contain jabberwocky')
        //
        cy.get('[data-cy="emailUsers_subject"]').clear();
        cy.get('[data-cy="emailUsers_subject"]').type('test');
        cy.get('[data-cy="previewUsersEmail"]').should('be.enabled');
        cy.get('[data-cy="subjectLineError"]').should('be.not.visible');

        cy.get('[data-cy="emailUsers_body"]').type('jabberwocky');
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');
        cy.get('[data-cy="descriptionError"]').contains('paragraphs may not contain jabberwocky')

        cy.get('[data-cy="emailUsers_body"] [data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="emailUsers_body"]').type('test');
        cy.get('[data-cy="previewUsersEmail"]').should('be.enabled');
        cy.get('[data-cy="descriptionError"]').should('be.not.visible');
    });

    it('email body and subject validation for community-protected project still uses non-community validator', () => {
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');

        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.visit('/administrator/projects/proj1/contact-users');
        cy.wait('@emailSupported');
        // ensure that this is a community-protected project
        cy.get('[data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');

        cy.get('[data-cy="emailUsers_body"]').type('jabberwocky');
        cy.get('#emailBodyError').contains('paragraphs may not contain jabberwocky')
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');

        cy.get('[data-cy="emailUsers_subject"]').type('jabberwocky');
        cy.get('#subjectLineError').contains('paragraphs may not contain jabberwocky')
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');
    });

    it('multiple paragraph validation', () => {
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');

        cy.createProject(1)
        cy.visit('/administrator/projects/proj1/contact-users');
        cy.wait('@emailSupported');
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');

        cy.get('[data-cy="emailUsers_subject"]').type('subj');
        cy.get('[data-cy="emailUsers_body"]').type('first paragraph\n\nsecond paragraph');
        cy.get('#emailBodyError').should('not.be.visible')
        cy.get('[data-cy="previewUsersEmail"]').should('be.enabled');

        cy.get('[data-cy="emailUsers_body"]').type('\n\nthird has jabberwocky yes');
        cy.get('#emailBodyError').contains('paragraphs may not contain jabberwocky')
        cy.get('[data-cy="previewUsersEmail"]').should('be.disabled');
    });
});
