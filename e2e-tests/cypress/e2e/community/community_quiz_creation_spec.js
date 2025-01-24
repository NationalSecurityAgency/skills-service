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

describe('Community Quiz Creation Tests', () => {

    const allDragonsUser = 'allDragons@email.org'
    const quizTableSelector = '[data-cy="quizDefinitionsTable"]';

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, {tags: ['DivineDragon']});
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, {tags: ['DivineDragon']});
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('create restricted community quiz', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')
        cy.get('[data-cy="quizName"]').type('My First Quiz')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 0,
                value: 'For Divine Dragon Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
    });

    it('show docs links if configured', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.userCommunityDocsLabel = 'User Community Docs';
                conf.userCommunityDocsLink = 'https://somedocs.com';
                res.send(conf);
            });
        })
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"] a').contains('User Community Docs')
        cy.get('[data-cy="userCommunityDocsLink"] a[href="https://somedocs.com"]')
    });

    it('create non-restricted community quiz', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')
        cy.get('[data-cy="quizName"]').type('My First Quiz')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 0,
                value: 'All Dragons Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
    });

    it('create restricted community quiz by editing existing quiz', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 0,
                value: 'All Dragons Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
        cy.get('[data-cy="editQuizButton_quiz1"]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 0,
                value: 'For Divine Dragon Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);

        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('create restricted community quiz by editing existing quiz from quiz page', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')

        cy.get('[data-cy="editQuizButton"]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('once restricted community is enabled it cannot be disabled', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 0,
                value: 'For Divine Dragon Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('community validator should be selected based on the community - quiz creation', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')
        cy.get('[data-cy="quizName"]').type('one')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('community validator should be selected based on the community - edit existing project', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: false, description: 'some text'})
        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 0,
                value: 'All Dragons Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('community validator should be selected based on the community - edit existing project from quiz page', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: false, description: 'some text'})

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('edit existing community project should use community validator', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true, description: 'some text'})
        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 0,
                value: 'For Divine Dragon Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('edit existing community project should use community validator - quiz page', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true, description: 'some text'})

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Quiz/Survey Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('do not allow to enable if quiz does not meet community requirements', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.request('POST', `/admin/projects/proj1/users/${allDragonsUser}/roles/ROLE_PROJECT_APPROVER`);

        cy.createQuizDef(1, {enableProtectedUserCommunity: false, description: 'some text'})
        cy.request('POST', `/admin/quiz-definitions/quiz1/users/${allDragonsUser}/roles/ROLE_QUIZ_ADMIN`);

        cy.createAdminGroupDef(1, { name: 'Non-UC Protected Admin Group', enableProtectedUserCommunity: false });
        cy.addQuizToAdminGroupDef()

        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 0,
                value: 'For All Dragons Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains(`Has existing ${allDragonsUser} user that is not authorized`)
        cy.get('[data-cy="communityProtectionErrors"]').contains('This quiz is part of one or more Admin Groups that do no have Divine Dragon permission')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    });

    it('Community protection cannot be enabled if quiz is assigned a skill from a non community project', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/abcSkill').as('saveSkill')
        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createQuizDef(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="skillName"]').type('abc')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Quiz"]').click({ force: true })
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()
        cy.get('[data-cy="quizSelected-quiz1"]')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@saveSkill')

        cy.visit('/administrator/quizzes');
        cy.get('[data-cy="editQuizButton_quiz1"]').click()

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains('This quiz is linked to the following project(s) that do not have Divine Dragon permission: proj1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    });

    it('A UC protected quiz cannot be assigned assigned to a skill of a non community project', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.createSubject(1,1)
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="skillName"]').type('abc')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Quiz"]').click({ force: true })
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()
        cy.get('[data-cy="quizSelected-quiz1"]')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="associatedQuizError"]').contains('Quiz is not allowed to be assigned to this project. Project does not have Divine Dragon permission')
    });

    it('A UC protected quiz can be assigned to a skill of a UC protected community project', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/abcSkill').as('saveSkill')
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1,1)
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="skillName"]').type('abc')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Quiz"]').click({ force: true })
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()
        cy.get('[data-cy="quizSelected-quiz1"]')
        cy.get('[data-cy="associatedQuizError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@saveSkill')
    });

    it('A non UC quiz can be assigned to a skill of a UC protected community project', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/abcSkill').as('saveSkill')
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1,1)
        cy.createQuizDef(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="skillName"]').type('abc')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Quiz"]').click({ force: true })
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()
        cy.get('[data-cy="quizSelected-quiz1"]')
        cy.get('[data-cy="associatedQuizError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@saveSkill')
    });

    it('attachments are not enabled on project creation when UC protection is available', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')
        cy.get('[data-cy="quizName"]').type('My First Quiz')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get(`button.attachment-button`).should('not.exist');

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 0,
                value: 'For Divine Dragon Nation'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);

        cy.get('[data-cy="editQuizButton_MyFirstQuiz"]').click()
        cy.get(`button.attachment-button`).should('exist');

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(allDragonsUser, vars.defaultPass);
            cy.visit('/administrator/quizzes/')
            cy.get('[data-cy="inception-button"]').contains('Level')
            cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
            cy.get('[data-cy="restrictCommunityControls"]').should('not.exist');
            cy.get(`button.attachment-button`).should('exist');
        })
    });
});
