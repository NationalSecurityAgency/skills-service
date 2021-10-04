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
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
dayjs.extend(relativeTimePlugin);

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
const timeFromNowFormatter = (value) => dayjs(value).startOf('seconds').fromNow();

const testTime = new Date().getTime()
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24)

describe('Navigation Tests', () => {

  beforeEach(() => {
    cy.createProject(1);
    cy.enableProdMode(1);

    cy.createSubject(1, 1);
    cy.createSubject(1, 2);
    cy.createSubject(1, 3);

    cy.addToMyProjects(1);

  });

  it('self report modals must render at the same level of the button that initiated the modal', function () {
    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });

    cy.viewport(1200, 1000)
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=%2Fsubjects%2Fsubj1');
    cy.dashboardCd().find('[data-cy="toggleSkillDetails"]').click();
    cy.dashboardCd().find('[data-cy="selfReportBtn"]').should('have.length', 4)
    cy.dashboardCd().find('[data-cy="skillProgress_index-3"] [data-cy="selfReportBtn"]').click();
    cy.dashboardCd().find('[data-cy="selfReportSkillMsg"]').contains('This skill requires approval');

    cy.wait(5000);
    cy.matchSnapshotImageForElement('iframe', 'self reporting modal positioning');
  });

  it('self report rejection modals must render at the same level of the button that initiated the modal', () => {
    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });

    // request
    cy.viewport(1200, 1000)
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=%2Fsubjects%2Fsubj1');
    cy.dashboardCd().find('[data-cy="toggleSkillDetails"]').click();
    cy.dashboardCd().find('[data-cy="selfReportBtn"]').should('have.length', 4)
    cy.dashboardCd().find('[data-cy="skillProgress_index-3"] [data-cy="selfReportBtn"]').click();
    cy.dashboardCd().find('[data-cy="selfReportSubmitBtn"]').click();
    cy.dashboardCd().find('[data-cy="selfReportAlert"]').contains('This skill requires approval');

    cy.visit('/administrator/projects/proj1/self-report');
    cy.get('[data-cy="selectPageOfApprovalsBtn"]').click();
    cy.get('[data-cy="rejectBtn"]').click();
    cy.get('[data-cy="rejectionTitle"]').contains('This will permanently reject user\'s request(s) to get points')
    cy.get('[data-cy="confirmRejectionBtn"]').click();
    cy.get('[data-cy="skillsReportApprovalTable"]').contains('Nothing to approve');

    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=%2Fsubjects%2Fsubj1');
    cy.dashboardCd().find('[data-cy="toggleSkillDetails"]').click();
    cy.dashboardCd().find('[data-cy="clearRejectionMsgBtn"]').click();
    cy.dashboardCd().find('[data-cy="clearRejectionMsgDialog"]').contains('This action will permanently remove the rejection');

    cy.wait(5000);
    const snapshotOptions = {
      blackout: ['[data-cy="selfReportRejectedAlert"]'],
      failureThreshold: 0.03, // threshold for entire image
      failureThresholdType: 'percent', // percent of image or number of pixels
      customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
      capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };
    cy.matchSnapshotImageForElement('iframe', 'self reporting rejection modal positioning', snapshotOptions);
  });

  it( 'ability to enable theme on project Skills Display', function () {
    cy.visit('/progress-and-rankings/projects/proj1?enableTheme=true');
    cy.dashboardCd().contains('powered by');

    cy.visit('/progress-and-rankings/projects/proj1?enableTheme=false');
    cy.dashboardCd().contains('Overall Points');
    cy.dashboardCd().contains('powered by').should('not.exist');
  });

  it( 'ability to enable classic look on project Skills Display', function () {
    cy.visit('/progress-and-rankings/projects/proj1?classicSkillsDisplay=true');
    cy.dashboardCd().contains('powered by');

    cy.visit('/progress-and-rankings/projects/proj1?classicSkillsDisplay=false');
    cy.dashboardCd().contains('Overall Points');
    cy.dashboardCd().contains('powered by').should('not.exist');
  });

  it('Browser back button works in Skills Display', function () {
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);

    cy.intercept('GET', '/api/projects/proj1/pointHistory').as('pointHistoryChart');

    cy.visit('/');

    cy.get('[data-cy=inception-button]').should('not.exist');

    cy.get('[data-cy=project-link-proj1]').click()
    cy.wait('@pointHistoryChart');
    cy.dashboardCd().contains('Overall Points');
    cy.dashboardCd().contains('Earn up to 800 points');

    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
    cy.get('[data-cy=breadcrumb-proj1]').should('be.visible');
    cy.get('[data-cy=breadcrumb-projects]').should('not.exist');

    cy.dashboardCd().find('[data-cy=back]').should('not.exist');
    cy.dashboardCd().contains('PROJECT: This is project 1');

    // to subject page
    cy.dashboardCdClickSubj(0, 'Subject 1');

    // navigate to Rank Overview and that it does NOT contains the internal back button
    cy.dashboardCd().find('[data-cy=myRank]').click();
    cy.dashboardCd().contains('Rank Overview');
    cy.dashboardCd().find('[data-cy=back]').should('not.exist');

    // click the browser back button and verify that we are still in the
    // client display (Subject page)
    cy.go('back')  // browser back button
    cy.dashboardCd().contains('Subject 1');

    // then back one more time and we should be back on the client display home page
    cy.go('back')  // browser back button
    cy.dashboardCd().contains('PROJECT: This is project 1');

    // finally back one more time and we should be back on the my progress page
    cy.go('back')  // browser back button
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').contains('Progress And Rankings').should('be.visible');
  });


});

