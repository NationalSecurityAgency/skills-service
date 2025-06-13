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


    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.loginAsDefaultUser()

        cy.createProject(1, { name: 'With Custom Labels'});
        cy.createSubject(1, 1, {name: 'Fancy'});
        cy.createSkill(1, 1, 1, { name: 'Expertise 1'});
        cy.createSubject(1, 2, {name: 'Comedy'});
        cy.createSkillsGroup(1, 1, 1, {name: 'Bunch'});
        cy.addSkillToGroup(1, 1, 1, 2, {
            name: 'Expertise 2',
            pointIncrement: 10,
            numPerformToCompletion: 5
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            name: 'Expertise 3',
            pointIncrement: 15,
            numPerformToCompletion: 2
        });
        cy.createSubject(1, 3, {name: 'Magic'});
        cy.createSkill(1, 3, 1, { selfReportingType: 'HonorSystem', name: 'Expertise 1a' });
        cy.createSkill(1, 3, 2, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            justificationRequired: false,
            name: 'Expertise 2a'
        });
        cy.createSkill(1, 3, 3, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            justificationRequired: true,
            name: 'Expertise 3a'
        });

        cy.loginAsRootUser();
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignProjectToGlobalBadge(1, 1);
        cy.enableGlobalBadge();
        cy.loginAsDefaultUser()

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
            {
                value: 'Unit',
                setting: 'point.displayName',
                projectId: 'proj1',
            },
        ]);

        const proxyUser = Cypress.env('proxyUser');
        cy.reportSkill(1, 1, proxyUser, '2021-02-24 10:00');
    });
    after(() => {
        Cypress.env('disableResetDb', false);
    });

    it('verify custom labels - project page', () => {
        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Work Role: With Custom Labels');
        cy.get('[data-cy="overallPoints"]').contains('Overall Units');
        cy.get('[data-cy="levelProgress"]').contains('Stage 2 Progress');
        cy.get('[data-cy="pointsTillNextLevelSubtitle"]').contains('70 Units to Stage 2');
        cy.get('[data-cy="overallLevel"]').contains('My Stage');
        cy.get('[data-cy="overallLevelDesc"]')
            .contains('Stage 1 out of 5');
        cy.get('[data-cy="subjectTile"]')
            .contains('Next Stage');
        cy.get('[data-cy="subjectTile"]')
            .contains('Stage 2');
        cy.get('[data-cy="subjectTile"]')
            .contains('Stage 0');
        cy.get('[data-cy="achievedSkillsProgress"]').contains('Achieved Courses');
        cy.get('[data-cy="downloadTranscriptCard"]').contains('You have Completed 0 out of 6 Courses!')
        cy.get('[data-cy="searchSkillsAcrossSubjects"] input')
            .should('have.attr', 'placeholder', 'Search for a Course across Competencies...');

        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('be.visible');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - rank page', () => {
        cy.visit('/progress-and-rankings/projects/proj1/rank')
        cy.get('[data-cy="levelBreakdownChart-animationEnded"]')
        cy.get('[data-cy="myRankLevelStatCard"]')
            .contains('My Stage');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('Stage Breakdown');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('You are Stage 1!');
        cy.contains('Level')
            .should('not.exist');

        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).filter('[data-cy="userColumn"]').should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - subject page', () => {
        cy.cdVisit('/subjects/subj1');

        cy.get('[data-cy="overallPoints"]').contains('Overall Units');
        cy.get('[data-cy="levelProgress"]').contains('Stage 3 Progress');
        cy.get('[data-cy="pointsTillNextLevelSubtitle"]').contains('26 Units to Stage 3');
        cy.get('[data-cy="overallLevel"]').contains('My Stage');
        cy.get('[data-cy="overallLevelDesc"]').contains('Stage 2 out of 5');
        cy.get('[data-cy="achievedSkillsProgress"]').contains('Achieved Courses');

        cy.get('[data-cy="groupToggle"]').contains('KSAS')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 2 Competencies');

        cy.get('[data-cy="skillsSearchInput"]')
            .invoke('attr', 'placeholder')
            .should('contain', 'Search courses');
        cy.get('[data-cy="skillDetailsToggle"]')
            .contains('Course Details');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemLabel"]')
            .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemValue"]')
            .should('have.text', 'subj1');

        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).not('[data-pc-section="header"] h2.sr-only').should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - subject rank page', () => {
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/rank');
        cy.get('[data-cy="levelBreakdownChart-animationEnded"]')
        cy.get('[data-cy="myRankLevelStatCard"]')
            .contains('My Stage');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('Stage Breakdown');
        cy.get('[data-cy="levelBreakdownChart"]')
            .contains('You are Stage 2!');
        cy.contains('Level')
            .should('not.exist');

        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).filter('[data-cy="userColumn"]').should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - single skill', () => {
        // TODO: stopped here
        cy.cdClickSkill(0, true, 'Course');
        cy.get('[data-cy="skillProgressTitle-skill1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemLabel"]')
            .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-skill1"] [data-cy="breadcrumbItemLabel"]')
            .should('have.text', 'Course:');
    })

    it('verify custom labels', () => {

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-Overview]')
            .click();
        cy.get('[data-cy="title"]').contains('User Skills')
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3).should('be.visible');

        cy.cdClickBadges();
        cy.get('[data-cy="badge_globalBadge1"]')

        cy.get('[data-cy="badgeDetailsLink_globalBadge1"]').click()
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
        cy.get('[data-cy="title"]').contains('User Skills')
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3).should('be.visible');

        cy.cdClickSubj(1);
        cy.get('[data-cy="noContent"]')
            .contains('Courses have not been added yet');
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="noContent"]')
            .contains('Please contact this work role\'s administrator.');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj2"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-Overview]')
            .click();
        cy.get('[data-cy="title"]').contains('User Skills')
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3).should('be.visible');

        cy.cdClickSubj(2, 'Subject 3', false);
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="pointHistoryChartNoData"]')

        cy.cdClickSkill(0, true, 'Course');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-skill1Subj3"] [data-cy="breadcrumbItemLabel"]')
          .should('have.text', 'Course:');

        cy.get('[data-cy="claimPointsBtn"]')
            .click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('You just earned 100 points!');

        cy.cdBack('Subject 3');
        cy.get('[data-cy="skillProgress_index-0"]')
        cy.get('[data-cy="pointHistoryChartNoData"]')

        cy.cdClickSkill(1, true, 'Course');
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
        cy.get('[data-cy="skillProgress_index-0"]')
        cy.get('[data-cy="pointHistoryChartNoData"]')

        cy.cdClickSkill(2, true, 'Course');
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