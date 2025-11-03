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

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        Cypress.Commands.add('gemNextMonth', () => {
            cy.get('[data-cy="gemDates"] [data-pc-section="nextbutton"]')
                .first()
                .click();
            cy.wait(150);
        });
        Cypress.Commands.add('gemPrevMonth', () => {
            cy.get('[data-cy="gemDates"] [data-pc-section="previousbutton"]')
                .first()
                .click();
            cy.wait(150);
        });
        Cypress.Commands.add('gemSetDay', (dayNum) => {
            cy.get(`[data-cy="gemDates"] [data-pc-section="table"] [aria-label="${dayNum}"]`)
              .not('[data-p-other-month="true"]')
                .click();
        });

        cy.intercept('POST', '/admin/projects/proj1/badgeNameExists')
            .as('nameExistsCheck');
        cy.intercept('GET', '/admin/projects/proj1/badges')
            .as('loadBadges');
        cy.intercept('GET', '/admin/projects/proj1/skills?*')
          .as('loadSkills');
    });

    it('create badge with special chars', () => {
        const expectedId = 'LotsofspecialPcharsBadge';
        const providedName = '!L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c\'ha\'rs';

        cy.intercept('POST', `/admin/projects/proj1/badges/${expectedId}`)
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

        cy.get('[data-cy="name"]').type(providedName);

        cy.wait('@nameExistsCheck');

        cy.getIdField().should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewBadge');

        cy.contains('ID: Lotsofspecial');
    });

    it('create badge with enter key', () => {
        const expectedId = 'LotsofspecialPcharsBadge';
        const providedName = '!L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c\'ha\'rs';

        cy.intercept('POST', `/admin/projects/proj1/badges/${expectedId}`)
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
            .type(providedName);

        cy.wait('@nameExistsCheck');

        cy.getIdField()
            .should('have.value', expectedId);

        cy.get('[data-cy="name"]')
            .type('{enter}');
        cy.wait('@postNewBadge');

        cy.contains('ID: Lotsofspecial');
    });

    it('Close badge dialog', () => {
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
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        //have to wait for dialog to close
        cy.wait(500);
        cy.get('[data-cy=closeDialogBtn]')
            .should('not.exist');
    });

    it('inactive badge displays warning', () => {
        cy.createBadge(1,1)
        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.contains('ID: badge1')
        cy.contains('This badge cannot be achieved until it is live')
    });

    it('badge modal allows Help Url to have spaces', () => {
        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.get('[data-cy="name"]').type('badge1')
        cy.get('[data-cy="skillHelpUrl"]').type('https://someCoolWebsite.com/some url with spaces')
        cy.get('[data-cy="skillHelpUrlError"]').should('not.be.visible');
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="badgeCard-badge1Badge"] [data-cy="editBtn"]').click()
        cy.get('[data-cy="skillHelpUrl"]').should('have.value', 'https://someCoolWebsite.com/some%20url%20with%20spaces')
    })

    it('Badge is disabled when created, can only be enabled once', () => {

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');
        cy.get('[data-cy="name"]')
            .type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');

        cy.get('[data-cy=manageBtn_TestBadgeBadge]')
            .click();
        cy.wait(500);
        cy.wait('@loadSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.contains('[data-cy="breadcrumbItemValue"]', 'Badges')
            .click();

        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Yes, Go Live!')
            .click();

        cy.wait('@loadBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Live')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .should('not.exist');
    });

    it('Badge is disabled when created, canceling confirm dialog leaves badge disabled', () => {

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');
        cy.get('[data-cy="name"]')
            .type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');

        cy.get('[data-cy=manageBtn_TestBadgeBadge]')
            .click();
        cy.wait(500);
        cy.wait('@loadSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.contains('[data-cy="breadcrumbItemValue"]', 'Badges')
            .click();

        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Cancel')
            .click();

        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Live')
            .should('not.exist');
        cy.get('[data-cy=goLive]')
            .should('exist');
    });

    it('new badge button should retain focus after dialog is closed', () => {
        cy.visit('/administrator/projects/proj1');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy=nav-Badges]')
            .click();

        cy.get('[aria-label="new badge"]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[aria-label="new badge"]')
            .should('have.focus');

        cy.get('[aria-label="new badge"]')
            .click();
        cy.get('[data-cy=name]')
            .type('{esc}');
        cy.get('[aria-label="new badge"]')
            .should('have.focus');

        cy.get('[aria-label="new badge"]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[aria-label="new badge"]')
            .should('have.focus');

        cy.get('[aria-label="new badge"]')
            .click();
        cy.get('[data-cy=name]')
            .type('test 123');
        cy.get('[data-cy=saveDialogBtn]')
            .click();
        cy.get('[aria-label="new badge"]')
            .should('have.focus');
    });

    it('edit badge from badges page', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy="titleLink"]')
            .contains('Badge 1');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="subTitle"]')
            .contains('ID: badge1');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy="titleLink"]')
            .contains('Badge 2');
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="subTitle"]')
            .contains('ID: badge2');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy="editBtn"]')
            .click();
        cy.get('input[data-cy=name]')
            .type('{selectall}Updated Name');
        cy.get('button[data-cy=saveDialogBtn]')
            .click();

        cy.get('button[data-cy=saveDialogBtn]').should('not.exist');
        cy.wait('@loadBadges')

        cy.get('[data-cy="badgeCard-badge1"] [data-cy="titleLink"]')
            .contains('Badge 1');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="subTitle"]')
            .contains('ID: badge1');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy="subTitle"]')
            .contains('ID: badge2');
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="titleLink"]')
            .contains('Updated Name');
    });

    it('delete badge', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="badgeCard-badge1"]')
            .should('exist');
        cy.get('[data-cy="badgeCard-badge2"]')
            .should('exist');

        cy.openDialog('[data-cy="badgeCard-badge2"] [data-cy="deleteBtn"]')
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=currentValidationText]')
            .type('Delete Me');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled')
            .click();

        cy.get('[data-cy="badgeCard-badge1"]')
            .should('exist');
        cy.get('[data-cy="badgeCard-badge2"]')
            .should('not.exist');

        cy.openDialog('[data-cy="badgeCard-badge1"] [data-cy="deleteBtn"]')
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=currentValidationText]')
            .type('Delete Me', {delay: 0});
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled')
            .click();

        cy.get('[data-cy="badgeCard-badge1"]')
            .should('not.exist');
        cy.get('[data-cy="badgeCard-badge2"]')
            .should('not.exist');

        cy.contains('No Badges Yet');
    });

    it('navigate to badge by clicking on name and icon', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);

        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');

        // using title link
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="titleLink"]')
            .click();
        cy.contains('No Skills Selected Yet');
        cy.contains('ID: badge2');

        // using icon
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="iconLink"]')
            .click();
        cy.contains('No Skills Selected Yet');
        cy.contains('ID: badge2');
    });

    it('badge card stats', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);

        cy.createSubject(1, 1);

        cy.createSkill(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 1);

        cy.createSkill(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 2);

        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .contains(2);
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .contains(400);

        cy.get('[data-cy="badgeCard-badge2"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .contains(0);
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .contains(0);
    });

    it('edit badge button should retain focus after dialog is closed', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });

        cy.visit('/administrator/projects/proj1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy=nav-Badges]')
            .click();

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy=name]')
            .type('{esc}');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy=name]')
            .type('{esc}');
        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .should('have.focus');
    });

    it('edit in place', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1');
        cy.intercept('GET', '/admin/projects/proj1/badges/badge1')
            .as('loadBadge1');
        cy.intercept('GET', '/admin/projects/proj1/badges/iwasedited/users**')
            .as('loadBadgeUsers');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadge1');
        cy.contains('BADGE: Badge 1')
            .should('be.visible');
        cy.contains('ID: badge1')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-badge1]')
            .should('be.visible');
        cy.get('button[data-cy=deleteSkill_skill1]')
            .should('be.visible');
        cy.get('[data-cy=btn_edit-badge]')
            .should('be.visible')
            .click();
        cy.get('input[data-cy=name]')
            .type('{selectall}Updated Badge Name');
        cy.get('button[data-cy=saveDialogBtn]')
            .click();
        cy.contains('BADGE: Badge 1')
            .should('not.exist');
        cy.contains('BADGE: Updated Badge Name')
            .should('be.visible');

        cy.get('[data-cy=btn_edit-badge]')
            .click();
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('input[data-cy=idInputValue]')
            .type('{selectall}iwasedited');
        cy.get('button[data-cy=saveDialogBtn]')
            .click();
        cy.contains('ID: badge1')
            .should('not.exist');
        cy.contains('ID: iwasedited')
            .should('be.visible');
        cy.location()
            .should((loc) => {
                expect(loc.pathname)
                    .to
                    .eq('/administrator/projects/proj1/badges/iwasedited');
            });
        cy.get('[data-cy=breadcrumb-badge1]')
            .should('not.exist');
        cy.get('[data-cy=breadcrumb-iwasedited]')
            .should('be.visible');
        cy.get('button[data-cy=deleteSkill_skill1]')
            .click();
        cy.contains('YES, Delete It!')
            .click();
        cy.contains('No Skills Selected Yet...')
            .should('be.visible');
        cy.get('[data-cy=nav-Users]')
            .click();
        cy.wait('@loadBadgeUsers');
    });

    it('badge modal shows Root Help Url when configured', () => {
        cy.viewport(1200, 1400)

        cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
            projectId: 'proj1',
            setting: 'help.url.root',
            value: 'https://SomeArticleRepo.com/'
        });
        cy.createBadge(1, 2, { helpUrl: '/some/path' });
        cy.createBadge(1, 3, { helpUrl: 'https://www.OverrideHelpUrl.com/other/path' });

        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]')
            .click();
        cy.get('[data-cy="rootHelpUrlSetting"]')
            .contains('https://SomeArticleRepo.com');

        // strike-through when url starts with http:// or https://
        cy.get('[data-cy="skillHelpUrl"]').should('be.visible')
        cy.get('[data-cy="skillHelpUrl"]').click()
        cy.get('[data-cy="skillHelpUrl"]').type('https:/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.class', 'line-through');
        cy.get('[data-cy="skillHelpUrl"]').click()
        cy.get('[data-cy="skillHelpUrl"]').type('/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('have.class', 'line-through');

        cy.get('[data-cy="skillHelpUrl"]').clear()
        cy.get('[data-cy="skillHelpUrl"]').click()
        cy.get('[data-cy="skillHelpUrl"]').type('http:/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.class', 'line-through');
        cy.get('[data-cy="skillHelpUrl"]')
            .type('/');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('have.class', 'line-through');

        // now test edit
        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="editBtn"]')
            .click();
        cy.get('[data-cy="rootHelpUrlSetting"]')
            .contains('https://SomeArticleRepo.com');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.class', 'line-through');

        // edit again - anything that starts with https or http must not use Root Help Url
        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="badgeCard-badge3"] [data-cy="editBtn"]')
            .click();
        cy.get('[data-cy="rootHelpUrlSetting"]')
            .contains('https://SomeArticleRepo.com');
        cy.get('[data-cy="rootHelpUrlSetting"]').should('have.class', 'line-through');

        // do not show Root Help Url if it's not configured
        cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
            projectId: 'proj1',
            setting: 'help.url.root',
            value: ''
        });
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]')
            .click();
        cy.get('[data-cy="skillHelpUrl"]');
        cy.get('[data-cy="rootHelpUrlSetting"]')
            .should('not.exist');
        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="editBtn"]')
            .click();
        cy.get('[data-cy="skillHelpUrl"]');
        cy.get('[data-cy="rootHelpUrlSetting"]')
            .should('not.exist');
    });

    it('badge details has go live button and can go live', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            'iconClass': 'fas fa-ghost',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
        });

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy=statPreformatted]')
            .contains('Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('This Badge has no assigned Skills. A Badge cannot be published without at least one assigned Skill.')
            .should('be.visible');
        cy.get('[data-cy=statPreformatted]')
            .contains('Disabled')
            .should('exist');

        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1');
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: 'someuser',
            timestamp: new Date().getTime()
        });

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy=statPreformatted]')
            .contains('Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Yes, Go Live!')
            .click();

        cy.get('[data-cy=statPreformatted]')
            .contains('Live')
            .should('exist');

    });

    it('add bonus award to badge', () => {
        cy.createBadge(1, 1);

        cy.intercept('GET', '/admin/projects/proj1/badges/badge1')
            .as('loadBadge1');

        cy.intercept('POST', `/admin/projects/proj1/badges/badge1`)
            .as('saveBadge');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadge1');

        cy.get('[data-cy="btn_edit-badge"]').click();
        cy.get('[data-cy="timeLimitCheckbox"]').click();
        cy.get('input[data-cy="awardAttrs.name"]')
            .type('{selectall}Bonus Award Name');
        cy.get('[data-cy="timeLimitDays"]')
            .type('{selectall}25');
        cy.get('[data-cy="timeLimitHours"]')
            .type('{selectall}22');
        cy.get('[data-cy="timeLimitMinutes"]')
            .type('{selectall}30');
        cy.clickSaveDialogBtn()

        cy.wait('@saveBadge');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadge1');

        cy.get('[data-cy="btn_edit-badge"]').click();

        cy.get('input[data-cy="awardAttrs.name"]').should('have.value', 'Bonus Award Name');
        cy.get('[data-cy="timeLimitDays"] input').should('have.value', '25');
        cy.get('[data-cy="timeLimitHours"] input').should('have.value', '22');
        cy.get('[data-cy="timeLimitMinutes"] input').should('have.value', '30');
    });

    it('can edit existing badge award', () => {
        cy.createBadge(1, 1, {awardAttrs: {iconClass: 'test-icon', name: 'Test Badge Award', numMinutes: 120}});

        cy.intercept('GET', '/admin/projects/proj1/badges/badge1')
            .as('loadBadge1');

        cy.intercept('POST', `/admin/projects/proj1/badges/badge1`)
            .as('saveBadge');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadge1');

        cy.get('[data-cy="btn_edit-badge"]').click();
        cy.get('[data-cy="timeLimitCheckbox"] > input').should('be.checked');

        cy.get('input[data-cy="awardAttrs.name"]').should('have.value', 'Test Badge Award');
        cy.get('[data-cy="timeLimitHours"] input').should('have.value', '2');
        cy.get('[data-cy="timeLimitMinutes"] input').should('have.value', '0');

        cy.get('input[data-cy="awardAttrs.name"]')
            .type('{selectall}Bonus Award Name');
        cy.get('[data-cy="timeLimitDays"]')
            .type('{selectall}25');
        cy.get('[data-cy="timeLimitHours"]')
            .type('{selectall}22');
        cy.get('[data-cy="timeLimitMinutes"]')
            .type('{selectall}30');
        cy.clickSaveDialogBtn()

        cy.wait('@saveBadge');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadge1');

        cy.get('[data-cy="btn_edit-badge"]').click();

        cy.get('input[data-cy="awardAttrs.name"]').should('have.value', 'Bonus Award Name');
        cy.get('[data-cy="timeLimitDays"] input').should('have.value', '25');
        cy.get('[data-cy="timeLimitHours"] input').should('have.value', '22');
        cy.get('[data-cy="timeLimitMinutes"] input').should('have.value', '30');
    });

    it('create badge with custom icon', () => {
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/icons/upload',
        })
          .as('uploadIcon');

        cy.visit('/administrator/projects/proj1/badges');
        cy.get('[data-cy="projectInsufficientPoints"]')
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="btn_Badges"]')
          .click();

        cy.get('[data-cy="iconPicker"]').first()
          .click();
        cy.get('[data-pc-name="tabmenu"] [data-pc-section="itemlink"]').contains('Custom').click();
        const filename = 'valid_icon.png';
        cy.get('[data-pc-name="fileupload"] input').attachFile(filename);
        cy.wait('@uploadIcon');
        cy.get('[data-cy="iconPicker"] .proj1-validiconpng');
        cy.get('[data-cy="name"]').type('customIcon');
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="badgeCard-customIconBadge"] .proj1-validiconpng');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/badges');
        cy.get('[data-cy="projectInsufficientPoints"]')
        cy.get('[data-cy="manageBtn_customIconBadge"]')
        cy.get('[data-cy="projectLastReportedSkillValue"]').contains('Never')
        cy.get('[data-cy="badgeCard-customIconBadge"] .proj1-validiconpng');
    });

    it('cancelling delete dialog should return focus to delete button', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);
        cy.createBadge(1, 3);
        cy.createBadge(1, 4);
        cy.createBadge(1, 5);

        cy.intercept('GET', '/admin/projects/proj1/badges')
          .as('getBadges');
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getBadges');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy="deleteBtn"]')
          .click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=closeDialogBtn]')
          .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy="deleteBtn"]')
          .should('have.focus');

    });
});
