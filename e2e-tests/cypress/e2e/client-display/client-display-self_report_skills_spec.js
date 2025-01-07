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
import moment from 'moment-timezone';

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Self Report Skills Tests', () => {

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.createProject(1);
        cy.createSubject(1, 1);

        Cypress.Commands.add('approveRequest', (requestNum = 0) => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    cy.request('POST', '/admin/projects/proj1/approvals/approve', {
                        skillApprovalIds: [response.body.data[requestNum].id],
                    });
                });
        });

        Cypress.Commands.add('rejectRequest', (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    cy.request('POST', '/admin/projects/proj1/approvals/reject', {
                        skillApprovalIds: [response.body.data[requestNum].id],
                        rejectionMessage: rejectionMsg,
                    });
                });
        });

        Cypress.Commands.add('submitForApproval', (skillNum = '1') => {
            // cy.request('POST', `/api/projects/proj1/skills/${skillId}`)
            const m = moment.utc();
            cy.request('POST', `/api/projects/proj1/skills/skill${skillNum}`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .subtract(5, 'day')
                    .format('x')
            });
        });
    });

    it('only show self-report button if enabled', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/?internalBackButton=true');
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.cdClickSubj(0);
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="requestApprovalBtn"]')
            .should('exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="requestApprovalAlert"]')
            .contains('This skill requires approval. Request 100 points once you\'ve completed the skill.')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="claimPointsBtn"]')
            .should('exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="honorSystemAlert"]')
            .contains('This skill can be submitted under the Honor System, claim 100 points once you\'ve completed the skill')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="claimPointsBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalAlert"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="honorSystemAlert"]')
            .should('not.exist');

        cy.cdClickSkill(0);
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('exist');
        cy.get('[data-cy="requestApprovalAlert"]')
            .contains('This skill requires approval. Request 100 points once you\'ve completed the skill.')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="claimPointsBtn"]')
            .should('exist');
        cy.get('[data-cy="honorSystemAlert"]')
            .contains('This skill can be submitted under the Honor System, claim 100 points once you\'ve completed the skill')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]')
            .should('not.exist');
        cy.get('[data-cy="requestApprovalAlert"]')
            .should('not.exist');
        cy.get('[data-cy="honorSystemAlert"]')
            .should('not.exist');
    });

    it('do not show report if skill is completed', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem',  numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem',  numPerformToCompletion: 1 });
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval',  numPerformToCompletion: 1 });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="selfReportHonorSystemTag"]')
        cy.get('[data-cy="requestApprovalBtn"]').should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist');

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);

        cy.get('[data-cy="selfReportApprovalTag"]')
        cy.get('[data-cy="requestApprovalBtn"]').should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist');
    });

    it('do not show report if skill has uncompleted dependencies', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem', numPerformToCompletion: 2 });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval', numPerformToCompletion: 1 });
        cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill2`);
        cy.request('POST', `/admin/projects/proj1/skill3/prerequisite/proj1/skill2`);
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/?internalBackButton=true');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]').should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist');

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="requestApprovalBtn"]').should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]').should('exist');

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="requestApprovalBtn"]').should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist');
    });

    it('self report honor skill', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="claimPointsBtn"]')
            .click();

        cy.get('[data-cy="selfReportAlert"]')
            .contains('You just earned 50 points!');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('50 / 100 Points');

        cy.matchSnapshotImageForElement('[data-cy="skillProgressBar"]', 'Halfway_Progress');

        cy.get('[data-cy="claimPointsBtn"]')
            .click();

        cy.get('[data-cy="selfReportAlert"]')
            .contains('You just earned 50 points and completed the skill!');
        cy.get('[data-cy="claimPointsBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('100');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('100');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 100 Points');

        cy.matchSnapshotImageForElement('[data-cy="skillProgressBar"]', 'Full_Progress');

        cy.contains('Achieved on ');
    });

    it('self report honor skill from subject page', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="claimPointsBtn"]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportAlert"]')
            .contains('You just earned 50 points!');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('50 / 100 Points');

        cy.matchSnapshotImageForElement('[data-cy="skillProgress_index-1"] [data-cy="skillProgressBar"]', 'Halfway_Progress_On_Subj_Page');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="claimPointsBtn"]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportAlert"]')
            .contains('You just earned 50 points and completed the skill!');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="claimPointsBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('100');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('100');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 100 Points');

        cy.matchSnapshotImageForElement('[data-cy="skillProgress_index-1"] [data-cy="skillProgressBar"]', 'Full_Progress_On_Subj_Page');

        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Achieved on ');
    });

    it('self report approval-required skill - skill gets approved', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSkillMsg"]')
            .contains('Submit with an optional justification and it will enter an approval queue.');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();

        cy.get('[data-cy="selfReportAlert"]')
            .contains('This skill requires approval from a project administrator. Now let\'s play the waiting game! ');
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="selfReportAlert"]')
            .contains('requires approval');
        cy.get('[data-cy="selfReportAlert"]')
            .contains('Submitted successfully!');

        // refresh the page and validate that submit button is disabled and approval status is still displayed
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approval Requested')

        // approve and then visit page again
        cy.approveRequest();
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approved')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(1)
            .should('contain.text', 'Approval Requested')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('50 / 100 Points');
    });

    it('self report approval justification required', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            justificationRequired: true,
        });
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSkillMsg"]')
            .contains('Submit with a justification and it will enter an approval queue');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="selfReportMsgInput"]')
            .type('some val');
        cy.get('[data-cy="descriptionError"]')
            .should('not.be.visible');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();

        cy.get('[data-cy="selfReportAlert"]')
            .contains('This skill requires approval from a project administrator. Now let\'s play the waiting game! ');
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');

        // cy.get('[data-cy="approvalHistoryTimeline"]').should('not.exist')
        cy.get('[data-cy="selfReportAlert"] [data-pc-section="closebutton"]').click()
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approval Requested')
            .should('contain.text', 'a few seconds ago');
        cy.get('[data-cy="selfReportAlert"]').should('not.exist')

        // refresh the page and validate that submit button is disabled and approval status is still displayed
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approval Requested')
            .should('contain.text', 'a few seconds ago');

        // approve and then visit page again
        cy.approveRequest();
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approved')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(1)
            .should('contain.text', 'Approval Requested')

        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('50 / 100 Points');
    });

    it('self report - skill was submitted for approval', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.submitForApproval();

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approval Requested')
            .should('contain.text', '5 days ago');
    });

    it('self report - skill was submitted for approval - on subject page', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });
        cy.submitForApproval();

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="pendingApprovalStatus"]')
            .contains('pending approval');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="pendingApprovalStatus"]')
            .contains('Submitted 5 days ago');
    });

    it('self report - approval-based submission was cancelled - on subject page', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]').click()
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="selfReportSubmitBtn"]').should('be.enabled')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]').should('not.exist')

        cy.get('[data-cy="skillDescription-skill1"] [data-cy="selfReportApprovalCancelBtn"]').click()
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="selfReportApprovalCancelBtn"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]').should('be.focused')
    });

    it('self report - skill was submitted for approval - on subject page skill header', () => {
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="approvalPending"]').should('not.exist');

        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });
        cy.submitForApproval();

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="approvalPending"]').should('exist');

        cy.approveRequest();
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="approvalPending"]').should('not.exist');
    });

    it('self report - skill was rejected', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.submitForApproval();
        cy.rejectRequest();

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="approvalPending"]').contains('Request Rejected')
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="selfReportRejectedAlert"]')
            .contains('The reason is: "Skill was rejected"');
        cy.get('[data-cy="clearRejectionMsgBtn"]').should('be.enabled')
    });

    it('self report - skill was rejected - on subject page', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 200,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: 200,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 3);
        cy.submitForApproval(2);
        cy.rejectRequest();

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="clearRejectionMsgBtn"]')
            .should('be.enabled');


        cy.get('[data-cy="skillProgress_index-1"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="approvalPending"]').contains('Request Rejected')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportRejectedAlert"]')
            .contains('The reason is: "Skill was rejected"');
    });

    it('self report - resubmit rejected skill', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.submitForApproval();
        cy.rejectRequest();

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="selfReportRejectedAlert"]')
            .contains('The reason is: "Skill was rejected"');
        cy.get('[data-cy="clearRejectionMsgBtn"]').click()

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSkillMsg"]')
            .contains('Submit with an optional justification and it will enter an approval queue.');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();

        cy.get('[data-cy="selfReportAlert"]')
            .contains('This skill requires approval from a project administrator. Now let\'s play the waiting game! ');
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="selfReportAlert"]')
            .should('not.exist');
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Approval Requested')
            .should('contain.text', 'a few seconds ago');
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(1)
            .should('contain.text', 'Rejected')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(2)
            .should('contain.text', 'Approval Requested')
    });

    it('self report - resubmit rejected skill - on subject page', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3);
        cy.submitForApproval(2);
        cy.rejectRequest();

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="clearRejectionMsgBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="approvalPending"]').contains('Request Rejected')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportRejectedAlert"]')
            .contains('The reason is: "Skill was rejected"');


        cy.get('[data-cy="skillProgress_index-1"] [data-cy="clearRejectionMsgBtn"]').click()

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportSkillMsg"]')
            .contains('Submit with an optional justification and it will enter an approval queue.');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportSubmitBtn"]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportAlert"]')
            .contains('This skill requires approval from a project administrator. Now let\'s play the waiting game! ');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('0');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="pendingApprovalStatus"]')
            .contains('pending approval');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="pendingApprovalStatus"]')
            .contains('Submitted a few seconds ago');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="approvalPending"]')
            .contains('Pending Approval');
    });

    it('validate approval message if custom validator is configured', () => {

        cy.intercept('POST', '/api/projects/proj1/skills/skill1', (req) => {
            expect(req.body.approvalRequestedMsg)
                .to
                .include('some val jabberwock');
        })
            .as('reportSkill');

        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSkillMsg"]')
            .contains('Submit with an optional justification and it will enter an approval queue.');
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');

        cy.get('[data-cy="selfReportMsgInput"]')
            .type('some val');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="descriptionError"]')
            .should('not.be.visible');

        cy.get('[data-cy="selfReportMsgInput"]')
            .type(' jabberwocky ok');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="descriptionError"]')
            .contains('paragraphs may not contain jabberwocky');

        cy.get('[data-cy="selfReportMsgInput"]')
            .type('{backspace}{backspace}{backspace}');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="descriptionError"]')
            .contains('paragraphs may not contain jabberwocky');

        cy.get('[data-cy="selfReportMsgInput"]')
            .type('{backspace}');
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="descriptionError"]')
            .should('not.be.visible');

        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();
        cy.wait('@reportSkill');
    });

    it('clearly indicate which skills are self reportable', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3, { selfReportingType: 'HonorSystem' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="selfReportApprovalTag"]').contains('Approval');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportApprovalTag"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportHonorSystemTag"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="selfReportHonorSystemTag"]').contains('Honor');

        cy.matchSnapshotImageForElement('[data-cy="skillProgress_index-0"]', {
            name: 'Self_Reportable Label',
            blackout: '[data-cy="skillProgressTitle"]',
            errorThreshold: 0.05
        });

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.matchSnapshotImageForElement('[data-cy="skillProgress_index-0"]', {
            name: 'Self_Reportable Label - Themed',
            errorThreshold: 0.05
        });
    });

    it('clearly indicate on the skill overview whether skill is self reportable', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3, { selfReportingType: 'HonorSystem' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="selfReportApprovalTag"]').contains('Approval')
        cy.matchSnapshotImageForElement('[data-cy="skillProgressTitle-skill1"]', 'Self_Reportable Label on Skill Overview');

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="selfReportApprovalTag"]') .should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="selfReportHonorSystemTag"]') .should('not.exist');

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.matchSnapshotImageForElement('[data-cy="skillProgressTitle-skill1"]', 'Self_Reportable Label on Skill Overview - Themed');
    });

    it('attempt to report skill with insufficient project points', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 10
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 10
        });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();
        cy.get('[data-cy="selfReportError"]')
            .contains('Insufficient project points, skill achievement is disallowed');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy="claimPointsBtn"]')
            .click();
        cy.get('[data-cy="selfReportError"]')
            .contains('Insufficient project points, skill achievement is disallowed');
    });

    it('attempt to report skill with insufficient subject points', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 10
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 10
        });

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 1, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 200
        });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();
        cy.get('[data-cy="selfReportError"]')
            .contains('Insufficient Subject points, skill achievement is disallowed');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy="claimPointsBtn"]')
            .click();
        cy.get('[data-cy="selfReportError"]')
            .contains('Insufficient Subject points, skill achievement is disallowed');
    });

    it('self report honor system updates overall progress and points', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="overallPoints"] .apexcharts-canvas')
            .contains('0 Points');
        cy.get('[data-cy="overallPoints"] .apexcharts-canvas')
          .contains('0%');
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]')
          .should('have.text', '0');
        cy.get('[data-cy="overallPoints"] [data-cy="pointsEarnedToday"]')
          .should('have.text', '0');
        cy.get('[data-cy="overallPoints"] [data-cy="totalPoints"]')
          .should('have.text', '400');

        cy.get('[data-cy="levelProgress"] .apexcharts-canvas')
          .contains('0 Points');
        cy.get('[data-cy="levelProgress"] .apexcharts-canvas')
          .contains('0%');
        cy.get('[data-cy="levelProgress"] [data-cy="pointsTillNextLevel"]').should('have.text', '40')
        cy.get('[data-cy="levelProgress"]').contains('40 Points to Level 1')
        cy.get('[data-cy="levelProgress"]').contains('Level 1 Progress')
        cy.get('[data-cy="overallLevelDesc"]').contains('Level 0 out of 5')
        cy.get('[data-cy="overallStars"]').find('[data-p-active="true"]').should('have.length', 0)
        cy.get('[data-cy="overallStars"]').find('[data-p-active="false"]').should('have.length', 5)

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="claimPointsBtn"]')
            .click();

        cy.get('[data-cy="overallPoints"] .apexcharts-canvas')
          .contains('50 Points');
        cy.get('[data-cy="overallPoints"] .apexcharts-canvas')
          .contains('12%');
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]')
          .should('have.text', '50');
        cy.get('[data-cy="overallPoints"] [data-cy="pointsEarnedToday"]')
          .should('have.text', '50');
        cy.get('[data-cy="overallPoints"] [data-cy="totalPoints"]')
          .should('have.text', '400');

        cy.get('[data-cy="levelProgress"] .apexcharts-canvas')
          .contains('10 Points');
        cy.get('[data-cy="levelProgress"] .apexcharts-canvas')
          .contains('16%');
        cy.get('[data-cy="levelProgress"] [data-cy="pointsTillNextLevel"]').should('have.text', '50')
        cy.get('[data-cy="levelProgress"]').contains('50 Points to Level 2')
        cy.get('[data-cy="levelProgress"]').contains('Level 2 Progress')
        cy.get('[data-cy="overallLevelDesc"]').contains('Level 1 out of 5')
        cy.get('[data-cy="overallStars"]').find('[data-p-active="true"]').should('have.length', 1)
        cy.get('[data-cy="overallStars"]').find('[data-p-active="false"]').should('have.length', 4)
    });
});
