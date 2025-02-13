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
describe('Badges Tests', () => {

    const makdownDivSelector = '#markdown-editor div.toastui-editor-main.toastui-editor-ww-mode > div > div.toastui-editor-ww-container > div > div'
    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        Cypress.Commands.add('gemNextMonth', () => {
            cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-name="pcnextbutton"]').click()
            cy.wait(150);
        });
        Cypress.Commands.add('gemPrevMonth', () => {
            cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-name="pcprevbutton"]').click()
            cy.wait(150);
        });
        Cypress.Commands.add('gemSetDay', (dayNum) => {
            cy.get('[data-pc-section="panel"] [data-pc-section="calendar"] [data-pc-section="day"]').contains(`${dayNum}`).click()
        });

        cy.intercept('POST', '/admin/projects/proj1/badgeNameExists')
            .as('nameExistsCheck');
        cy.intercept('GET', '/admin/projects/proj1/badges')
            .as('loadBadges');
        cy.intercept('GET', '/admin/projects/proj1/skills?*')
          .as('loadSkills');
    });

    it('cannot publish badge with no skills', () => {
        cy.intercept('POST', `/admin/projects/proj1/badges/anameBadge`)
            .as('postNewBadge');
        cy.intercept('POST', '/admin/projects/proj1/badgeNameExists')
            .as('nameExistsCheck');
        cy.intercept('GET', '/admin/projects/proj1/badges')
            .as('loadBadges');

        cy.get('@createProject')
            .should((response) => {
                expect(response.status)
                    .to
                    .eql(200);
            });

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadges');
        cy.get('[data-cy="btn_Badges"]').click();

        cy.get('[data-cy="name"]')
            .type('a name');

        cy.wait('@nameExistsCheck');

        cy.getIdField()
            .should('have.value', 'anameBadge');

        cy.clickSave();
        cy.wait('@postNewBadge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('This Badge has no assigned Skills. A Badge cannot be published without at least one assigned Skill.')
            .should('be.visible');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
    });

    it('name causes id to fail validation', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxIdLength = 50;
                res.send(conf);
            });
        })
            .as('loadConfig');
        cy.request('POST', '/admin/projects/proj1/badges/badgeExist', {
            projectId: 'proj1',
            name: 'Badge Exist',
            badgeId: 'badgeExist'
        });

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');

        // name causes id to be too long
        const msg = 'Badge ID cannot exceed 50 characters';
        const validNameButInvalidId = Array(46)
            .fill('a')
            .join('');
        cy.get('[data-cy=name]')
            .click();
        cy.get('[data-cy=name]')
            .fill(validNameButInvalidId);
        cy.get('[data-cy=idError]')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy=name]')
            .type('{backspace}');
        cy.get('[data-cy=idError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
    });

    it('badge validation', () => {
        // create existing badge
        cy.request('POST', '/admin/projects/proj1/badges/badgeExist', {
            projectId: 'proj1',
            name: 'Badge Exist',
            badgeId: 'badgeExist'
        });

        cy.intercept('POST', '/api/validation/url')
            .as('customUrlValidation');

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');

        // name is too short
        let msg = 'Badge Name must be at least 3 characters';
        cy.get('[data-cy="name"]')
            .type('Te');
        cy.get('[data-cy=nameError]')
            .contains(msg)
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy="name"]')
            .type('Tes');
        cy.get('[data-cy=nameError]')
            .should('not.be.visible');

        cy.get('[data-cy="enableIdInput"]').click();

        // name too long
        msg = 'Badge Name must be at most 50 characters';
        cy.contains('Enable')
            .click();
        cy.getIdField()
            .clear()
            .type('badgeId');
        const invalidName = Array(50)
            .fill('a')
            .join('');
        cy.get('[data-cy="name"]')
            .clear();
        cy.get('[data-cy="name"]')
            .type(invalidName);
        cy.get('[data-cy="name"]')
            .type('b');
        cy.get('[data-cy=nameError]')
            .contains(msg)
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy="name"]')
            .type('{backspace}');
        cy.get('[data-cy=nameError]')
            .should('not.be.visible');

        // id too short
        msg = 'Badge ID must be at least 3 characters';

        cy.getIdField()
            .clear()
            .type('aa');
        cy.get('[data-cy=idError]')
            .contains(msg)
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.getIdField()
            .type('a');
        cy.get('[data-cy=idError]')
            .should('not.be.visible');

        // id too long
        msg = 'Badge ID must be at most 100 characters';
        const invalidId = Array(101)
            .fill('a')
            .join('');
        cy.getIdField()
            .clear();
        cy.getIdField()
            .click()
            .type(invalidId);
        cy.get('[data-cy=idError]')
            .contains(msg)
            .should('be.visible');
        cy.getIdField()
            .type('{backspace}');
        cy.get('[data-cy=idError]')
            .should('not.be.visible');

        // id must not have special chars
        msg = 'Badge ID may only contain alpha-numeric characters';
        cy.getIdField()
            .clear()
            .type('With$Special');
        cy.get('[data-cy=idError]')
            .contains(msg)
            .should('be.visible');
        cy.getIdField()
            .clear()
            .type('GoodToGo');
        cy.get('[data-cy=idError]')
            .should('not.be.visible');

        cy.getIdField()
            .clear()
            .type('SomeId');
        // !L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c'ha'rs
        let specialChars = [' ', '!', '@', '#', '^', '&', '*', '(', ')', '-', '+', '='];
        specialChars.forEach((element) => {
            cy.getIdField()
                .type(element);
            cy.get('[data-cy=idError]')
                .contains(msg)
                .should('be.visible');
            cy.getIdField()
                .type('{backspace}');
            cy.get('[data-cy=idError]')
                .should('not.be.visible');
        });

        // badge name must not be already taken
        msg = 'Badge Name is already taken';
        cy.get('[data-cy="name"]')
            .clear()
            .type('Badge Exist');
        cy.get('[data-cy=nameError]')
            .contains(msg)
            .should('be.visible');
        cy.get('[data-cy="name"]')
            .type('1');
        cy.get('[data-cy=nameError]')
            .should('not.be.visible');

        // badge id must not already exist
        msg = 'Badge ID is already taken';
        cy.getIdField()
            .clear()
            .type('badgeExist');
        cy.get('[data-cy=idError]')
            .contains(msg)
            .should('be.visible');
        cy.getIdField()
            .type('1');
        cy.get('[data-cy=idError]')
            .should('not.be.visible');

        // max description
        msg = 'Badge Description must be at most 2000 characters';
        const invalidDescription = Array(2000)
            .fill('a')
            .join('');
        // it takes way too long using .type method
        cy.get(makdownDivSelector)
            .invoke('text', invalidDescription);
        cy.get('#markdown-editor')
            .type('a', 0);
        cy.get('[data-cy=descriptionError]')
            .contains(msg)
            .should('be.visible');
        cy.get('#markdown-editor')
            .type('{backspace}');
        cy.get('[data-cy=descriptionError]')
            .should('not.be.visible');

        //helpUrl
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('javascript:alert("uh oh");');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('be.visible');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('have.text', 'Help URL/Path must start with "/" or "http(s)"');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('/foo?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('http://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');

        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');

        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://---??..??##');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        // trailing space should work now
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://foo.bar?p1=v1&p2=v2 ');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');

        // finally let's save
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.contains('Badge Exist1');
    });

    it('gem start and end time validation', () => {
        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');
        cy.get('[data-cy="gemCheckbox"]').click();
        cy.contains('Date Range');

        cy.get('[data-cy="name"]')
            .type('Test Badge');


        // dates should not be in the past
        let msg = 'End date can not be in the past';
        cy.gemPrevMonth();
        cy.gemPrevMonth();

        cy.gemSetDay(1);
        cy.gemSetDay(15);

        cy.get('[data-cy=gemDatesError]').contains(msg);

        // should not save if there are validation errors
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        // fix the errors and save
        cy.gemNextMonth();
        cy.gemNextMonth();
        cy.gemNextMonth();
        cy.gemSetDay(1);
        cy.gemSetDay(15);


        cy.get('[data-cy=saveDialogBtn]').should('be.enabled');

        cy.clickSave();
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
    });

    it('description is validated against custom validators', () => {
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadges');
        cy.get('[data-cy="btn_Badges"]').click();

        cy.get('[data-cy="name"]')
            .type('Great Name');

        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]')
            .type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionMarkdownEditor"]')
            .contains('Badge Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]')
            .type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="descriptionMarkdownEditor"]')
            .contains('Badge Name - paragraphs may not contain jabberwocky')
            .should('not.exist');
    });

    it('name is validated against custom validators', () => {
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadges');
        cy.get('[data-cy="btn_Badges"]').click();

        cy.get('[data-cy="name"]')
          .type('Great Name');

        cy.get('[data-cy="nameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');

        cy.get('input[data-cy=name]')
          .type('{selectall}(A) Updated Badge Name');
        cy.get('[data-cy="nameError"]')
          .contains('Badge Name - names may not contain (A)');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.disabled');

        cy.get('input[data-cy=name]')
          .type('{selectall}(B) A Updated Badge Name');
        cy.get('[data-cy="nameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');
    });

    it('edit badge - run validation on load in case validation improved and existing values fail to validate', () => {
        cy.intercept('POST', '/api/validation/description*', {
            valid: false,
            msg: 'Mocked up validation failure'
        }).as('validateDesc');

        cy.createBadge(1, 1, {description: 'Very cool project'})
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="editBtn"]').click()
        cy.wait('@validateDesc')
        cy.get('[data-cy="descriptionMarkdownEditor"]').contains('Mocked up validation failure')
    });

    it('can not save badge with bad award data', () => {
        cy.createBadge(1, 1);

        cy.intercept('GET', '/admin/projects/proj1/badges/badge1')
            .as('loadBadge1');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadge1');

        cy.get('[data-cy="btn_edit-badge"]').click();
        cy.get('[data-cy="timeLimitCheckbox"]').click();

        cy.get('[data-cy=timeLimitDays]')
            .type('{selectall}800');
        cy.get('button[data-cy=saveDialogBtn]').should('not.be.enabled');
        cy.get('[data-cy=timeLimitDays]')
            .type('{selectall}1');
        cy.get('button[data-cy=saveDialogBtn]').should('be.enabled');

        cy.get('[data-cy=timeLimitHours]')
            .type('{selectall}800');
        cy.get('button[data-cy=saveDialogBtn]').should('not.be.enabled');
        cy.get('[data-cy=timeLimitHours]')
            .type('{selectall}1');
        cy.get('button[data-cy=saveDialogBtn]').should('be.enabled');

        cy.get('[data-cy=timeLimitMinutes]')
            .type('{selectall}900');
        cy.get('button[data-cy=saveDialogBtn]').should('not.be.enabled');
    });

    it('respect max badges per project config', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);
        cy.createBadge(1, 3);
        cy.intercept('GET', '/public/config', (req) => {
            req.continue((res) => {
                res.body.maxBadgesPerProject = 3
            })
        })
          .as('getConfig');

        cy.visit('/administrator/projects/proj1/badges')
        cy.wait('@getConfig')

        cy.get('[data-cy="subPageHeaderDisabledMsg"]').contains('The maximum number of Badges allowed is 3')
        cy.get('[data-cy="btn_Badges"]').should('be.disabled')
    })

    it('when adding skills to a badge and an error occurs redirect to the error page', () => {
        cy.intercept('POST', '/admin/projects/proj1/badge/badge1/skills/skill1', { statusCode: 500 }).as('saveEndpoint')
        cy.createBadge(1, 1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/badges/badge1')

        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('[data-pc-section="option"]').first().click();

        cy.wait('@saveEndpoint')

        cy.get('[data-cy="errorPage"]')
        cy.url().should('include', '/error')
    })

});
