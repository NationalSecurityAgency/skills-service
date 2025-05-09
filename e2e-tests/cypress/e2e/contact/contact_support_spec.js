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
describe('Contact Support Specs', () => {

    beforeEach(() => {

    });

    it('contact support can disabled', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.contactSupportEnabled = false;
                res.send(conf);
            });
        })

        cy.visit('/support')
        cy.get('[data-cy="featureDisabled"]')
        cy.get('[data-cy="visitSupportCenterBtn"]').should('not.exist')

        // check footer
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Email Us"]')
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Contact"]').should('not.exist')

        // check help button
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Integration"]')
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').should('not.exist')
    });

    it('support page should use configuration', () => {
        cy.visit('/support')
        cy.get('[data-cy="visitSupportCenterBtn"]')
            .should('have.attr', 'href')
            .and('include', 'https://skilltreeplatform.dev/');
        cy.contains('f you have a feature request, need to report a bug, or ')
        cy.get('h1').contains('Support Center')
        cy.get('[data-cy="visitSupportCenterBtn"]').contains('Visit Support Center')

        cy.contains('As an alternative, you can reach out to the SkillTree team by sending an email to')
        cy.get('[data-cy="supportEmailLink"]')
            .should('have.attr', 'href')
            .and('include', 'mailto:someSupport@someDomain.com');
    });

    it('navigate back and home from support page', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')

        cy.get('[data-cy="navBack"]').click()
        cy.url().should('include', '/administrator');

        cy.visit('/support')
        cy.get('[data-cy="takeMeHome"]').click()
        cy.url().should('include', '/progress-and-rankings');

    });

    it('Contact support controls navigate to support page from admin and settings pages', () => {
        cy.createProject(1)

        cy.visit('/administrator')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')

        cy.visit('/administrator')
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')

        cy.visit('/settings')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')

        cy.visit('/settings')
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')
    });

    it('navigate to support page from the home page when user has 0 projects added', () => {
        cy.visit('/progress-and-rankings')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')
        cy.url().should('include', '/support');
        cy.get('[data-cy="visitSupportCenterBtn"]')
    });

    it('on p&r page for user that a single project contact form will default to that project', () => {
        cy.createProject(1)
        cy.enableProdMode(1)
        cy.addToMyProjects(1)

        cy.visit('/progress-and-rankings')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.get('[data-p="modal"]').contains('Send your message to the administrators of This is project 1 training')
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]')
        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="myProjectSelector"]').should('not.exist')
        cy.url().should('not.include', '/support');

        cy.visit('/progress-and-rankings')
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Contact"]').click()
        cy.get('[data-p="modal"]').contains('Send your message to the administrators of This is project 1 training')
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]')
        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="myProjectSelector"]').should('not.exist')
        cy.url().should('not.include', '/support');
    });

    it('on p&r page with multiple projects', () => {
        cy.intercept('POST', '/api/projects/proj1/contact').as('contactProj1')
        cy.intercept('POST', '/api/projects/proj2/contact').as('contactProj2')
        cy.createProject(1)
        cy.enableProdMode(1)
        cy.addToMyProjects(1)
        cy.createProject(2)
        cy.enableProdMode(2)
        cy.addToMyProjects(2)

        cy.visit('/progress-and-rankings')
        cy.get('[data-cy="helpButton"]').click()
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Contact"]').click()
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]').should('not.exist')

        cy.get('[data-p="modal"] [data-cy="myProjectSelector"] [data-pc-section="dropdown"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="This is project 1"]').click()

        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]').type('This is a message')
        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').click()
        cy.wait('@contactProj1')

        cy.get('[data-p="modal"] [data-cy="myProjectSelector"]').should('not.exist')
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]').should('not.exist')


        cy.visit('/progress-and-rankings')
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Contact"]').click()
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]').should('not.exist')

        cy.get('[data-p="modal"] [data-cy="myProjectSelector"] [data-pc-section="dropdown"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="This is project 1"]').click()

        cy.get('[data-p="modal"] [data-cy="myProjectSelector"] [data-pc-section="dropdown"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="This is project 2"]').click()

        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]').type('This is a message')
        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-p="modal"] [data-cy="saveDialogBtn"]').click()
        cy.wait('@contactProj2')

        cy.get('[data-p="modal"] [data-cy="myProjectSelector"]').should('not.exist')
        cy.get('[data-p="modal"] [data-cy="contactOwnersMsgInput"]').should('not.exist')
    });

});

