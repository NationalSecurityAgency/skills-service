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

describe('Client Display Custom Label Tests', () => {

    it.only('verify custom labels', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 10,
            numPerformToCompletion: 5
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 15,
            numPerformToCompletion: 2
        });
        cy.createSubject(1, 3);
        cy.createSkill(1, 3, 1, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 3, 2, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            justificationRequired: false,
        });
        cy.createSkill(1, 3, 3, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            justificationRequired: true,
        });

        cy.loginAsRootUser();
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignProjectToGlobalBadge(1, 1);
        cy.enableGlobalBadge();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();

                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'Work Role',
                setting: 'project.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Competency',
                setting: 'subject.displayName',
                projectId: 'proj1',
            },
            {
                value: 'KSA',
                setting: 'group.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Course',
                setting: 'skill.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Stage',
                setting: 'level.displayName',
                projectId: 'proj1',
            },
        ]);

        const proxyUser = Cypress.env('proxyUser');
        cy.reportSkill(1, 1, proxyUser, '2021-02-24 10:00');

        cy.cdVisit('/');
        cy.contains('Stage 2 Progress');
        cy.contains('My Stage');
        cy.get('[data-cy="overallLevelDesc"]')
            .contains('Stage 1 out of 5');
        cy.get('[data-cy="subjectTile"]')
            .contains('Next Stage');
        cy.get('[data-cy="subjectTile"]')
            .contains('Stage 2');
        cy.get('[data-cy="subjectTile"]')
            .contains('Stage 0');
        cy.contains('Level')
            .should('not.exist');

        cy.cdClickRank();
        cy.get('[data-cy="myRankLevelStatCard"]')
            .contains('My Stage');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('Stage Breakdown');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('You are Stage 1!');
        cy.contains('Level')
            .should('not.exist');

        cy.get('[data-cy=breadcrumb-Overview]')
            .click();
        cy.cdClickSubj(0);
        cy.contains('Stage 3 Progress');
        cy.contains('My Stage');
        cy.contains('Level')
            .should('not.exist');
        cy.get('[data-cy="skillsSearchInput"]')
            .invoke('attr', 'placeholder')
            .should('contain', 'Search courses');
        cy.get('[data-cy="skillDetailsToggle"]')
            .contains('Course Details');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemValue"]')
          .should('have.text', 'subj1');

        cy.cdClickRank();
        cy.contains('My Stage');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('Stage Breakdown');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('You are Stage 2!');
        cy.contains('Level')
            .should('not.exist');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-subj1]')
            .click();
        cy.cdClickSkill(0, true, 'Course');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-skill1"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Course:');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-Overview]')
            .click();
        cy.cdClickBadges();
        cy.cdVisit('/badges/global/globalBadge1');
        cy.get('[data-cy="gb_proj1"]')
            .contains('Stage 1');
        cy.contains('Level')
            .should('not.exist');
        cy.get('[data-cy="skillsSearchInput"]')
            .invoke('attr', 'placeholder')
            .should('contain', 'Search courses');
        cy.get('[data-cy="skillDetailsToggle"]')
            .contains('Course Details');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-Overview]')
            .click();
        cy.cdClickSubj(1);
        cy.get('[data-cy="noDataYet"]')
            .contains('Courses have not been added yet');
        cy.get('[data-cy="noDataYet"]')
            .contains('Please contact this work role\'s administrator.');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj2"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        // cy.get('[data-cy=breadcrumb-subj2]')
        //     .contains('Competency: subj2');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-Overview]')
            .click();
        cy.cdClickSubj(2);
        // cy.get('[data-cy=breadcrumb-subj3]')
        //     .contains('Competency: subj3');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');

        cy.cdClickSkill(0, true, 'Course');
        // cy.get('[data-cy=breadcrumb-subj3]')
        //     .contains('Competency: subj3');
        // cy.get('[data-cy=breadcrumb-skill1Subj3]')
        //     .contains('Course: skill1Subj3');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-skill1Subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Course:');

        cy.get('[data-cy="claimPointsBtn"]')
            .click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('You just earned 100 points!');

        cy.cdBack('Subject 3');
        cy.cdClickSkill(1, true, 'Course');
        // cy.get('[data-cy=breadcrumb-subj3]')
        //     .contains('Competency: subj3');
        // cy.get('[data-cy=breadcrumb-skill2Subj3]')
        //     .contains('Course: skill2Subj3');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-skill2Subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Course:');

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('This course requires approval from a work role administrator. Now let\'s play the waiting game!');

        cy.cdBack('Subject 3');
        cy.cdClickSkill(2, true, 'Course');
        // cy.get('[data-cy=breadcrumb-subj3]')
        //     .contains('Competency: subj3');
        // cy.get('[data-cy=breadcrumb-skill3Subj3]')
        //     .contains('Course: skill3Subj3');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-skill3Subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Course:');

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportMsgInput"]')
            .type('some val');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('This course requires approval from a work role administrator. Now let\'s play the waiting game!');
    });

    it('custom skill label updates page indicator', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'Course',
                setting: 'skill.displayName',
                projectId: 'proj1',
            },
        ]);

        cy.cdVisit('/subjects/subj1/skills/skill2');
        cy.get('[data-cy="skillOrder"]').should('have.text', "Course 2 of 3")
    });

});