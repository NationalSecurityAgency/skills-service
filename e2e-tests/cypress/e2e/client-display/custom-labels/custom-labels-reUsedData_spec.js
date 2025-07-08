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

const moment = require("moment-timezone");
describe('Custom Label with Reused Data Tests', () => {


    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.loginAsDefaultUser()

        cy.createProject(1, { name: 'With Custom Labels'});
        cy.createSubject(1, 1, {name: 'Fancy'});
        cy.createSkill(1, 1, 1, { name: 'Expertise 1', skillId: 'expertise1' });
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
        const m = moment.utc().add(1, 'day');
        const nextExpirationDate = m.format('x');
        cy.request('POST', `/admin/projects/proj1/skills/expertise1/expiration`, {
            expirationType: 'YEARLY',
            every: 1,
            nextExpirationDate
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
        cy.request('POST', `/supervisor/badges/globalBadge1/projects/proj1/skills/expertise1`);
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
        cy.reportSkill(1, 'expertise1', proxyUser, '2021-02-24 10:00');
    });
    after(() => {
        Cypress.env('disableResetDb', false);
    });

    it('verify custom labels - project page in dashboard', () => {
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
        cy.get('[data-cy="downloadTranscriptCard"]').contains('You have Completed 0 out of 6 courses!')
        cy.get('[data-cy="searchSkillsAcrossSubjects"] input')
            .should('have.attr', 'placeholder', 'Search for a Course across Competencies...');

        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('be.visible');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - project page on default skills display', () => {
        cy.cdVisit();
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('User Courses');
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
        cy.get('[data-cy="downloadTranscriptCard"]').contains('You have Completed 0 out of 6 courses!')
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
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 2 Courses');

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

    it('verify custom labels - subject 2 page - empty', () => {
        cy.cdVisit('/subjects/subj2');
        cy.get('[data-cy="pointHistoryChartNoData"]')

        cy.get('[data-cy="noContent"]')
            .contains('Courses have not been added yet');
        cy.get('[data-cy="noContent"]')
            .contains('Please contact this work role\'s administrator.');

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj2"] [data-cy="breadcrumbItemLabel"]')
            .should('have.text', 'Competency:');

        cy.get('[data-cy="overallPoints"]').contains('Overall Units');
        cy.get('[data-cy="levelProgress"]').contains('Stage 1 Progress');
        cy.get('[data-cy="pointsTillNextLevelSubtitle"]').contains('0 Units to Stage 1');
        cy.get('[data-cy="overallLevel"]').contains('My Stage');
        cy.get('[data-cy="overallLevelDesc"]').contains('Stage 0 out of 5');
        cy.get('[data-cy="achievedSkillsProgress"]').contains('Achieved Courses');

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

    it('verify custom labels - global badge', () => {
        cy.cdVisit('/badges/global/globalBadge1')
        cy.get('[data-cy="gb_proj1"]')
            .contains('Stage 1');
        cy.get('[data-cy="skillsSearchInput"]')
            .invoke('attr', 'placeholder')
            .should('contain', 'Search courses');
        cy.get('[data-cy="skillDetailsToggle"]')
            .contains('Course Details');


        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).not('[data-pc-section="header"] h2.sr-only').should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - single skill', () => {
        cy.cdVisit('/subjects/subj1/skills/expertise1');
        cy.get('[data-cy="skillsTitle"]').contains('Course Overview')
        cy.get('[data-cy="skillProgressTitle-expertise1"]').contains('Expertise 1')
        cy.get('[data-cy="skillOrder"]').should('have.text', "Course 1 of 3")

        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Units')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('Units will expire on')

        // should use units instead of points
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Overall Units Earned')
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Units Achieved Today')
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Units per Occurrence')
        cy.get('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardSubTitle"]').contains('Up-to 100 units within 8 hrs')

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"] [data-cy="breadcrumbItemLabel"]')
            .should('have.text', 'Competency:');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-expertise1"] [data-cy="breadcrumbItemLabel"]')
            .should('have.text', 'Course:');

        cy.get('[data-cy="skillsDisplayHome"]').contains('Level', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Skill', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Subject', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Project', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Point', { matchCase: false }).should('not.exist');
        cy.get('[data-cy="skillsDisplayHome"]').contains('Group', { matchCase: false }).should('not.exist');
    })

    it('verify custom labels - single skill - earn points', () => {
        cy.cdVisit('/subjects/subj3/skills/skill1Subj3');
        cy.get('[data-cy="skillsTitle"]').contains('Course Overview')
        cy.get('[data-cy="skillProgressTitle-skill1Subj3"]').contains('Expertise 1a')
        cy.get('[data-cy="skillOrder"]').should('have.text', "Course 1 of 3")

        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 200 Units')

        cy.get('[data-cy="honorSystemAlert"]').contains('This course can be submitted under the Honor System, claim 100 units once you\'ve completed the course');
        cy.get('[data-cy="claimPointsBtn"]').contains('Claim Units')
        cy.get('[data-cy="claimPointsBtn"]').click();
        cy.get('[data-cy="selfReportAlert"]').contains('You just earned 100 units!');
    })

    it('verify custom labels - single skill - request points', () => {
        cy.cdVisit('/subjects/subj3/skills/skill2Subj3');
        cy.get('[data-cy="skillsTitle"]').contains('Course Overview')
        cy.get('[data-cy="skillProgressTitle-skill2Subj3"]').contains('Expertise 2a')
        cy.get('[data-cy="skillOrder"]').should('have.text', "Course 2 of 3")

        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Units')

        cy.get('[data-cy="requestApprovalAlert"]').contains('This course requires approval. Request 50 units once you\'ve completed the course.');
        cy.get('[data-cy="requestApprovalBtn"]').click();
        cy.get('[data-cy="selfReportSubmitBtn"]').click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('Submitted successfully! This course requires approval from a work role administrator. Now let\'s play the waiting game!');
    })


});