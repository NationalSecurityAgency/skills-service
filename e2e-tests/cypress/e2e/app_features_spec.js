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
import moment from 'moment';

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
describe('App Features Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
    });

    const upgradeMsg = 'New SkillTree Software Version is Available'

    it('display new version banner when software is updated', () => {
        cy.intercept('/admin/projects/proj1/subjects', (req) => {
            req.reply((res) => {
                res.send(200, [], { 'skills-client-lib-version': dateFormatter(new Date()) });
            });
        }).as('getSubjects');

        cy.visit('/administrator/');
        /* cy.injectAxe();
         cy.violationLoggingFunction().then((loggingFunc) => {
             cy.checkA11y(null, null, loggingFunc);
         });*/
        cy.get('[data-cy=subPageHeader]')
            .contains('Projects');
        cy.contains(upgradeMsg)
            .should('not.exist');
        cy.get('[data-cy=projectCard]')
            .last()
            .contains('Manage')
            .click();
        cy.wait('@getSubjects');

        cy.contains(upgradeMsg);

        cy.get('[data-cy="newSoftwareVersionReload"]')
            .click();
        cy.contains(upgradeMsg)
            .should('not.exist');
    });

    it('do not display new version banner if lib version in headers is older than lib version in local storage', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=subPageHeader]').contains('Projects');

        cy.intercept('/admin/projects/proj1/subjects', (req) => {
            req.reply((res) => {
                res.send(200, [], { 'skills-client-lib-version': dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 500) });
            });
        }).as('getSubjects');

        cy.contains(upgradeMsg)
          .should('not.exist');

        cy.get('[data-cy="projCard_proj1_manageLink"]').click()
        cy.wait('@getSubjects');


        cy.contains(upgradeMsg)
          .should('not.exist');


    });

    it('access denied should show authorization failure page not error page', () => {

        cy.createSubject(1,1)

        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })

        cy.intercept({
            method: 'GET',
            path: '/admin/projects/proj1/subjects/subj1',
        }, {
            statusCode: 403,
            body: {
                errorCode: 'NotAuthorized',
                explanation: 'Not authorized to view this resource'
            }
        })
            .as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        /*cy.injectAxe();*/
        cy.wait('@loadSubject');
        cy.url()
            .should('include', '/error');
        cy.contains('User Not Authorized')
            .should('be.visible');
        /*cy.violationLoggingFunction().then((loggingFunc) => {
            cy.checkA11y(null, null, loggingFunc);
        });*/
    });

    it('show support and contact information in the help dropdown', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    supportLink1: 'mailto:skilltree@someemail.com',
                    supportLink1Label: 'Email Us',
                    supportLink1Icon: 'fas fa-envelope-open-text',
                    supportLink2: 'https://skilltreesupport.com',
                    supportLink2Label: 'Support Center',
                    supportLink2Icon: 'fas fa-ambulance',
                },
            });
        })
            .as('getConfig');

        cy.visit('/administrator');
        cy.wait('@getConfig');
        cy.get('[data-cy="helpButton"]')
            .click();

        // validate help button
        cy.get('[data-pc-name="menu"] [aria-label="Email Us"]')
            .contains('Email Us');
        cy.get('[data-pc-name="menu"] [aria-label="Email Us"] a')
            .should('have.attr', 'href', 'mailto:skilltree@someemail.com');
        cy.get('[data-pc-name="menu"] [aria-label="Support Center"]')
            .contains('Support Center');
        cy.get('[data-pc-name="menu"] [aria-label="Support Center"] a')
            .should('have.attr', 'href', 'https://skilltreesupport.com');

        // validate footer
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Email Us"]')
            .contains('Email Us');
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Email Us"]')
            .should('have.attr', 'href', 'mailto:skilltree@someemail.com');
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Support Center"]')
            .contains('Support Center');
        cy.get('[data-cy="dashboardFooter"] [data-cy="supportLink-Support Center"]')
            .should('have.attr', 'href', 'https://skilltreesupport.com');
    });

    it('Validate Help Links for Guides', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="helpButton"]').click();

        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Official Docs"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev');

        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Training"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev/training-participation/');
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Admin"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev/dashboard/user-guide/');
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Integration"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev/skills-client/');
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Accessibility"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev/training-participation/accessibility.html');
    });

    it('Accessibility Guide link direct to training vs admin accessibility guide based on current page', () => {
        cy.enableProdMode(1);
        cy.addToMyProjects(1);

        const trainingAccessibilityLink = 'https://skilltreeplatform.dev/training-participation/accessibility.html'
        const adminAccessibilityLink = 'https://skilltreeplatform.dev/dashboard/user-guide/accessibility.html'
        const validateLink = (expectedLink) => {
            cy.get('[data-cy="helpButton"]').click();
            cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Accessibility"] a')
                .should('have.attr', 'href', expectedLink);
            cy.get('[data-p="popup"][data-pc-name="menu"]').should('be.visible');
            cy.wait(500)
            cy.get('[data-cy="dashboardVersion"]').click(); // close it by clicking away
            cy.get('[data-p="popup"][data-pc-name="menu"]').should('not.exist');
        }

        cy.visit('/progress-and-rankings');
        validateLink(trainingAccessibilityLink);

        cy.get('[data-cy="manageMyProjsBtn"]').click();
        cy.get('[data-cy="discoverProjectsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1');
        validateLink(trainingAccessibilityLink)

        cy.get('[data-cy="backToProgressAndRankingBtn"]').click()
        cy.get('[data-cy="project-link-proj1"]')
        validateLink(trainingAccessibilityLink)

        cy.get('[data-cy="project-link-proj1"]').click()
        cy.get('[data-cy="myRankPosition"]')
        cy.get('[data-cy="pointHistoryChart-animationEnded"]')
        validateLink(trainingAccessibilityLink)

        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-p="popup"][data-pc-name="menu"]  [aria-label="Project Admin"]').click()
        cy.get('[data-cy="projCard_proj1_manageBtn"]')
        validateLink(adminAccessibilityLink)

        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.get('[data-cy="projectLastReportedSkillValue"]').contains('Never')
        validateLink(adminAccessibilityLink)

        cy.get('[data-cy="settings-button"]').click();
        cy.get('[data-p="popup"][data-pc-name="menu"] [aria-label="Settings"]').click()
        cy.get('[data-cy="nickname"]')
        validateLink(adminAccessibilityLink)
    });


    it('ability to enable page visits reporting to the backend', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    enablePageVisitReporting: 'true',
                },
            });
        })
            .as('getConfig');
        cy.intercept('PUT', '/api/pageVisit', (req) => {
            expect(req.body.path)
                .to
                .include('/administrator');
            req.reply({
                body: {
                    'success': true,
                    'explanation': null
                },
            });
        })
            .as('pageVisit');

        cy.visit('/administrator');
        cy.wait('@getConfig');
        cy.wait('@pageVisit');
    });

    it('by default page visits reporting to the backend must not happen', () => {
        cy.intercept('GET', '/public/config')
            .as('getConfig');
        cy.intercept('PUT', '/api/pageVisit', cy.spy()
            .as('pageVisit'));
        cy.visit('/administrator');
        cy.wait('@getConfig');
        cy.wait(5000);
        cy.get('@pageVisit')
            .should('not.have.been.called');
    });

});
