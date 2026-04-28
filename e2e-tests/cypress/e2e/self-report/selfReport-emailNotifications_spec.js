/*
 * Copyright 2026 SkillTree
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

describe('Approval Requests Notification Management Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
    });

    it('warn if email service is not configured', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="emailServiceWarning"]')
            .contains('Please note that email notifications are currently disabled');
    });

    it('email service warning should NOT be displayed if there 0 Approval required Self Reporting skills', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .should('not.exist');
    });

    it('no email warning when email service is configured', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                host: 'localhost',
                port: 1025,
                'protocol': 'smtp'
            },
        });

        cy.request({
            method: 'POST',
            url: '/root/saveSystemSettings',
            body: {
                publicUrl: 'http://localhost:8082/',
                resetTokenExpiration: 'PT2H',
                fromEmail: 'noreploy@skilltreeemail.org',
            }
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.defaultUser, vars.defaultPass);
            });

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .should('not.exist');
    });

    it('should be able to unsubscribe from approval request emails', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.reportSkill(2, 1, 'user1Good@skills.org', '2020-09-12 11:00');

        cy.intercept('GET', '/admin/projects/**/approvalEmails/isSubscribed')
            .as('isSubscribed');
        cy.intercept('POST', '/admin/projects/proj1/approvalEmails/unsubscribe')
            .as('unsubscribe');
        cy.intercept('POST', '/admin/projects/proj1/approvalEmails/subscribe')
            .as('subscribe');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill3"]')
        cy.wait('@isSubscribed');
        cy.contains('Subscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
            .should('be.checked');
        cy.get('[data-cy=unsubscribeSwitch]')
            .click();
        cy.wait('@unsubscribe');
        cy.contains('Unsubscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch]')
            .should('not.be.checked');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill3"]')
        cy.wait('@isSubscribed');
        cy.contains('Unsubscribed')
          .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
          .should('not.be.checked');

        //setting should be per project
        cy.visit('/administrator/projects/proj2/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill1"]')
        cy.wait('@isSubscribed');
        cy.contains('Subscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
            .should('be.checked');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill3"]')
        cy.wait('@isSubscribed');
        cy.contains('Unsubscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
            .should('not.be.checked');
    });

    it('approval request email subscription toggle should not be visible if email is not configured', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy=unsubscribeContainer]')
            .should('not.exist');
    });

});

