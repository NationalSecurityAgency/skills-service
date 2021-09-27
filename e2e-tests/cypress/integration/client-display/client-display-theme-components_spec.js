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
const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Theme Components Tests', () => {

    const snapshotOptions = {
        blackout: ['[data-cy=achievementDate]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' })
        cy.createSkill(1, 1, 3)
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '2019-09-12 11:00')
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '2019-09-19 11:00')
    })

    it('point history chart - line, label and gradient', () => {
        cy.cdVisit('/?themeParam=charts|{"pointHistory":{"lineColor":"purple","gradientStartColor":"blue","gradientStopColor":"yellow"},"labelBorderColor":"black","labelBackgroundColor":"green","labelForegroundColor":"lightgray"}');

        // let's wait for animation to complete
        cy.get('[data-cy="pointHistoryChart-animationEnded"]')
        cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]');
    })

    it('point history chart - dark gray background customization', () => {
        cy.cdVisit('/?themeParam=tiles|{"backgroundColor":"gray"}&themeParam=textPrimaryColor|white&themeParam=charts|{"axisLabelColor":"lightblue"}');

        // let's wait for animation to complete
        cy.get('[data-cy="pointHistoryChart-animationEnded"]')
        cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]');
    })

    it('chart labels for all the charts', () => {
        cy.cdVisit('/?themeParam=charts|{"labelBorderColor":"black","labelBackgroundColor":"purple","labelForegroundColor":"lightblue"}');

        // // let's wait for animation to complete
        cy.get('[data-cy="pointHistoryChart-animationEnded"]')
        cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]', 'chartLabels-pointsHistory');
        cy.cdClickRank();

        cy.get('[data-cy="levelBreakdownChart-animationEnded"]')
        cy.matchSnapshotImageForElement('[data-cy="levelBreakdownChart"]', 'chartLabels-levelBreakdown');

    })


    it('buttons customization without changing tile background', () => {
        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)

        const url = '/?themeParam=buttons|{"backgroundColor":"green","foregroundColor":"white",%20"borderColor":"purple"}'
        cy.cdVisit(url);
        cy.matchSnapshotImageForElement('[data-cy="pointProgressChart-resetZoomBtn"]', 'buttons-resetZoom');

        cy.cdClickSubj(0);
        cy.matchSnapshotImageForElement('[data-cy="back"]', 'buttons-Back');
        cy.matchSnapshotImageForElement('[data-cy="filterMenu"] .dropdown', 'buttons-skillsFilter');
        cy.cdClickSkill(1);
        cy.matchSnapshotImageForElement('[data-cy="selfReportBtn"]', 'buttons-selfReport');

        cy.cdVisit(url);
        cy.cdClickBadges();
        cy.matchSnapshotImageForElement('[data-cy="filterMenu"] .dropdown', 'buttons-badgesFilter');
        cy.matchSnapshotImageForElement('[data-cy="badgeDetailsLink_badge1"]', 'buttons-viewBadgeDetails');
    })


    it('buttons customization with changing tile background', () => {
        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)

        const url = '/?themeParam=buttons|{"backgroundColor":"green","foregroundColor":"white",%20"borderColor":"purple"}&themeParam=tiles|{"backgroundColor":"black"}'
        cy.cdVisit(url);

        cy.cdClickSubj(0);
        cy.matchSnapshotImageForElement('[data-cy="back"]', 'buttons-Back-darkTileBackground');
        cy.matchSnapshotImageForElement('[data-cy="filterMenu"] .dropdown', 'buttons-skillsFilter-darkTileBackground');
        cy.cdClickSkill(1);
        cy.matchSnapshotImageForElement('[data-cy="selfReportBtn"]', 'buttons-selfReport-darkTileBackground');

        cy.cdVisit(url);
        cy.cdClickBadges();
        cy.matchSnapshotImageForElement('[data-cy="filterMenu"] .dropdown', 'buttons-badgesFilter-darkTileBackground');
        cy.matchSnapshotImageForElement('[data-cy="badgeDetailsLink_badge1"]', 'buttons-viewBadgeDetails-darkTileBackground');
    })


    it('filter menu with dark tile background', () => {
        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)

        const url = '/?themeParam=tiles|{"backgroundColor":"black"}&themeParam=textPrimaryColor|white&themeParam=textSecondaryColor|yellow'
        cy.cdVisit(url);
        cy.cdClickSubj(0);``
        cy.get('[data-cy="filterMenu"] .dropdown').click();
        cy.matchSnapshotImageForElement('[data-cy="filterMenu"] .dropdown-menu.show', 'filterMenu-skills', snapshotOptions);

        cy.cdBack()
        cy.cdClickBadges();
        cy.get('[data-cy="filterMenu"] .dropdown').click();
        cy.matchSnapshotImageForElement('[data-cy="filterMenu"] .dropdown-menu.show', 'filterMenu-badges', snapshotOptions);
    })



    it('theme info cards', () => {
        const url = '/?themeParam=tiles|{%22backgroundColor%22:%22black%22}&themeParam=textPrimaryColor|white&themeParam=infoCards|{%22backgroundColor%22:%22lightgray%22,%22borderColor%22:%22green%22,%22foregroundColor%22:%22purple%22,%22iconColors%22:[%22blue%22,%22red%22,%20%22yellow%22,%22green%22]}';
        cy.cdVisit(url);

        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="overallPointsEarnedCard"]').contains('200');
        cy.get('[data-cy="pointsAchievedTodayCard"]').contains('0');
        cy.get('[data-cy="pointsPerOccurrenceCard"]').contains('100');
        cy.get('[data-cy="timeWindowPts"]').contains('100');

        cy.matchSnapshotImageForElement('[data-cy="skillsSummaryCards"]', 'infoCards-skill');

        cy.cdVisit(url);
        cy.cdClickRank();
        cy.matchSnapshotImageForElement('[data-cy="encouragementCards"]', 'infoCards-rank');
    });


    it('theme info cards border overrides tile border', () => {
        const url = '/?themeParam=tiles|{%22backgroundColor%22:%22lightgray%22,%22borderColor%22:%22blue%22}&themeParam=textPrimaryColor|black&themeParam=infoCards|{%22borderColor%22:%22purple%22}';
        cy.cdVisit(url);

        cy.cdClickSubj(0);
        cy.cdClickSkill(0);

        cy.get('[data-cy="overallPointsEarnedCard"]').contains('200');
        cy.get('[data-cy="pointsAchievedTodayCard"]').contains('0');
        cy.get('[data-cy="pointsPerOccurrenceCard"]').contains('100');
        cy.get('[data-cy="timeWindowPts"]').contains('100');

        cy.matchSnapshotImage();
    });


    it('ability to configure tile border', () => {
        const url = '/?themeParam=tiles|{%22backgroundColor%22:%22lightgray%22,%22borderColor%22:%22blue%22}&themeParam=textPrimaryColor|black}';
        cy.cdVisit(url);

        cy.get('[data-cy="pointHistoryChart-animationEnded"]')
        cy.wait(1111);
        cy.matchSnapshotImage();
    });


})

