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

describe('Configure Video and SkillTree Features Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/video').as('getVideoProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        Cypress.Commands.add("visitVideoConfPage", (projNum) => {
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
            cy.wait('@getVideoProps')
            cy.wait('@getSkillInfo')
            cy.get('.spinner-border').should('not.exist')
        });

        cy.intercept('GET', '/admin/projects/proj2/skills/skill1/video').as('getVideoPropsProj2')
        cy.intercept('GET', '/admin/projects/proj2/subjects/subj1/skills/skill1').as('getSkillInfoProj2')
    });

    it('cannot configure video settings on reused skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const vidAttr = { videoUrl: testVideo, captions: 'some\nok\nglad\n\nok\nmore\nlines\nshould\nrender\nok', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)

        cy.createSubject(1, 2);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj2/skills/skill1STREUSESKILLST0/config-video');
        cy.get('[data-cy="readOnlyAlert"]').contains('Reused')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="videoCaptions"]').should('be.disabled')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('not.exist')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('not.exist')
    });

    it('cannot configure video settings on imported skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const vidAttr = { videoUrl: testVideo, captions: 'some\nok\nglad\n\nok\nmore\nlines\nshould\nrender\nok', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/config-video');
        cy.get('[data-cy="readOnlyAlert"]').contains('Imported')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="videoCaptions"]').should('be.disabled')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('not.exist')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('not.exist')
    });

    it('copying skill with video self-report type resets the self-report type', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const vidAttr = { videoUrl: testVideo, captions: 'some\nok\nglad\n\nok\nmore\nlines\nshould\nrender\nok', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="copySkillButton_skill1"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked')
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked')
    });

    it('video upload warning message is present when configured', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const msg = 'Friendly Reminder: Only safe videos please'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.videoUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitVideoConfPage();
        cy.wait('@loadConfig')
        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains(msg)

        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', videoFile)

        // click away and return
        cy.get('[data-cy="nav-Overview"]').click()
        cy.get('[data-cy="skillOverviewTotalpoints"] [data-cy="mediaInfoCardTitle"]').should('have.text', '200 Points')

        cy.get('[data-cy="nav-Video"]').click()
        cy.wait('@getVideoProps')
        cy.get('.spinner-border').should('not.exist')
        cy.wait(5000)
    });

    it('video upload warning message supports community.descriptor property ', () => {
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.createProject(2, {enableProtectedUserCommunity: true});
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        const msg = 'Friendly Reminder: Only safe videos please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.videoUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitVideoConfPage();
        cy.wait('@loadConfig')

        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for All Dragons")

        // nav to project 2
        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj2_manageBtn"]').click()

        cy.get('[data-cy="manageBtn_subj1"]').click()
        cy.get('[data-cy="manageSkillLink_skill1"]').click()
        cy.get('[data-cy="nav-Video"').click();
        cy.wait('@getVideoPropsProj2')
        cy.wait('@getSkillInfoProj2')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for Divine Dragon")

        // nav to project 1
        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.get('[data-cy="manageBtn_subj1"]').click()
        cy.get('[data-cy="manageSkillLink_skill1"]').click()
        cy.get('[data-cy="nav-Video"').click();
        cy.wait('@getVideoProps')
        cy.wait('@getSkillInfo')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for All Dragons")

        // straight to project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/config-video');
        cy.wait('@getVideoPropsProj2')
        cy.wait('@getSkillInfoProj2')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for Divine Dragon")
    });

    it('video upload warning message uses community.descriptor after project\'s UC protection is raised', () => {
        cy.intercept('/admin/projects/proj1/settings').as('getProj1Settings')
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const msg = 'Friendly Reminder: Only safe videos please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.videoUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitVideoConfPage();
        cy.wait('@loadConfig')

        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for All Dragons")

        // change UC
        cy.get('[data-cy="breadcrumb-proj1"]').click()
        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click({force: true})
        cy.get('[data-cy="restrictCommunityControls"]').contains('Please note that once the restriction is enabled it cannot be lifted/disabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.wait('@getProj1Settings')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="manageBtn_subj1"]').click()
        cy.get('[data-cy="manageSkillLink_skill1"]').click()
        cy.get('[data-cy="nav-Video"]').click()
        cy.wait('@getVideoProps')
        cy.wait('@getSkillInfo')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for Divine Dragon")
    });

    it('video upload warning message uses community.descriptor for a brand new project with UC protection', () => {
        cy.intercept('/admin/projects/proj1/settings').as('getProj1Settings')
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1Skill/video').as('getVideoProps1')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1Subject/skills/skill1Skill').as('getSkillInfo1')
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });


        const msg = 'Friendly Reminder: Only safe videos please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.videoUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitAdmin();
        cy.wait('@loadConfig')

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('proj1')
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click({force: true})
        cy.get('[data-cy="restrictCommunityControls"]').contains('Please note that once the restriction is enabled it cannot be lifted/disabled')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')

        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.wait('@getProj1Settings')
        cy.get('[data-cy="btn_Subjects"]').click()
        cy.get('[data-cy="subjectName"]').type('subj1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
        cy.get('[data-cy="manageBtn_subj1Subject"]').click()

        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="skillName"]').type('skill1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
        cy.get('[data-cy="manageSkillLink_skill1Skill"]').click()
        cy.get('[data-cy="nav-Video"]').click()
        cy.wait('@getVideoProps1')
        cy.wait('@getSkillInfo1')
        cy.get('.spinner-border').should('not.exist')
        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoUploadWarningMessage"]').contains("Friendly Reminder: Only safe videos please for Divine Dragon")
    });

    it('video upload warning message is not present when NOT configured', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.visitVideoConfPage();
        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.wait(5000)
        cy.get('[data-cy="videoUploadWarningMessage"]').should('not.exist')
    });

    it('throw an error if video warning messages has community property but community setting is not available', () => {
        const msg = 'Friendly Reminder: Only safe videos please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.videoUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.intercept('POST', '/public/log').as('reportError')

        cy.intercept('GET', '/admin/projects/proj1/settings', (req) => {
            req.reply((res) => {
                res.send([]);
            });
        }).as('loadProjectSettings');

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.visitVideoConfPage();
        cy.wait('@loadProjectSettings')
        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.wait('@reportError')
        cy.get('[data-cy="errorPage"]').contains('something went wrong')
    });

});