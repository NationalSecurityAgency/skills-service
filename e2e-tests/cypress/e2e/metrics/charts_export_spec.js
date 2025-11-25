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
var moment = require('moment-timezone');

describe('Metrics Tests - Achievements', () => {

    beforeEach(() => {
        cy.cleanupDownloadsDir()

        Cypress.Commands.add("validateCsvFile", (csvText) => {
            cy.task('listFiles', 'cypress/downloads').then(files => {
                const csvFiles = files.filter(file => file.endsWith('.csv'))
                cy.log('CSV files in downloads folder:', csvFiles)
                expect(csvFiles).to.have.length(1, 'Expected exactly one CSV file')
                const filepath = `cypress/downloads/${csvFiles[0]}`

                cy.readFile(filepath).then((content) => {
                    expect(content).to.not.be.null
                    expect(content).to.have.length.gt(0)

                    expect(content).to.eq(csvText)
                })
            })
        });
    });

    it('download multi series time chart csv', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.reportSkill(1, 1, "user1", '2025-11-19 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-21 11:00');
        cy.reportSkill(1, 1, "user2", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user3", '2025-11-20 11:00');

        cy.visit('/administrator/projects/proj1/metrics')

        cy.get('[data-cy="distinctNumUsersOverTime"] [data-cy="chartDownloadMenu"]').click()
        cy.get('[data-p="popup"] [aria-label="Export to CSV"]').click()

        // Wait for the file to be downloaded
const expectedCsv = `Date,Users,New Users
2025-10-26,0,0
2025-10-27,0,0
2025-10-28,0,0
2025-10-29,0,0
2025-10-30,0,0
2025-10-31,0,0
2025-11-01,0,0
2025-11-02,0,0
2025-11-03,0,0
2025-11-04,0,0
2025-11-05,0,0
2025-11-06,0,0
2025-11-07,0,0
2025-11-08,0,0
2025-11-09,0,0
2025-11-10,0,0
2025-11-11,0,0
2025-11-12,0,0
2025-11-13,0,0
2025-11-14,0,0
2025-11-15,0,0
2025-11-16,0,0
2025-11-17,0,0
2025-11-18,0,0
2025-11-19,1,1
2025-11-20,3,2
2025-11-21,1,0
2025-11-22,0,0
2025-11-23,0,0
2025-11-24,0,0
2025-11-25,0,0
`
        cy.validateCsvFile(expectedCsv)
    });

    it('download chart jpg', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.reportSkill(1, 1, "user1", '2025-11-19 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-21 11:00');
        cy.reportSkill(1, 1, "user2", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user3", '2025-11-20 11:00');

        cy.visit('/administrator/projects/proj1/metrics')

        cy.get('[data-cy="distinctNumUsersOverTime"] [data-cy="chartDownloadMenu"]').click()
        cy.get('[data-p="popup"] [aria-label="Export to JPG"]').click()

        cy.task('listFiles', 'cypress/downloads').then(files => {
            const jpgFiles = files.filter(file => file.endsWith('.jpg'))
            expect(jpgFiles).to.have.length(1, 'Expected exactly one jpg file')
            const filepath = `cypress/downloads/${jpgFiles[0]}`

            cy.readFile(filepath).then((content) => {
                expect(content).to.not.be.null
                expect(content).to.have.length.gt(0)
            })
        })

    });

    it('download bar chart csv', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.reportSkill(1, 1, "user1", '2025-11-19 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-21 11:00');
        cy.reportSkill(1, 1, "user2", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user3", '2025-11-20 11:00');

        cy.visit('/administrator/projects/proj1/metrics/achievements')

        cy.get('[data-cy="levelsChart"] [data-cy="chartDownloadMenu"]').click()
        cy.get('[data-p="popup"] [aria-label="Export to CSV"]').click()

        // Wait for the file to be downloaded
        const expectedCsv = `Category,Number of Users
Level 5,0
Level 4,0
Level 3,0
Level 2,1
Level 1,2
`
        cy.validateCsvFile(expectedCsv)

    });

    it('download multi series bar chart csv', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 1);
        cy.createSkill(1, 2, 2);
        cy.createSkill(1, 2, 3);

        cy.reportSkill(1, 1, "user1", '2025-11-19 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user1", '2025-11-21 11:00');
        cy.reportSkill(1, 1, "user2", '2025-11-20 11:00');
        cy.reportSkill(1, 1, "user3", '2025-11-20 11:00');

        cy.doReportSkill({
            project: 1,
            skill: 1,
            subjNum: 2,
            userId: "user1",
            date: '2025-11-19 11:00'
        })
        cy.doReportSkill({
            project: 1,
            skill: 2,
            subjNum: 2,
            userId: "user1",
            date: '2025-11-19 11:00'
        })
        cy.doReportSkill({
            project: 1,
            skill: 3,
            subjNum: 2,
            userId: "user1",
            date: '2025-11-19 11:00'
        })

        cy.visit('/administrator/projects/proj1/metrics/subjects')

        cy.get('[data-cy="userCountsBySubjectMetric"] [data-cy="chartDownloadMenu"]').click()
        cy.get('[data-p="popup"] [aria-label="Export to CSV"]').click()

        // Wait for the file to be downloaded
        const expectedCsv = `Category,Level 1,Level 2,Level 3,Level 4,Level 5
Subject 1,2,1,0,0,0
Subject 2,0,0,1,0,0
`
        cy.validateCsvFile(expectedCsv)

    });

    it('download pie chart csv', () => {
        cy.createProject(1, 1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {pointIncrement: '1000', numPerformToCompletion: '1'})

        cy.reportSkill(1, 1, "user1", '2025-10-19 11:00');
        cy.reportSkill(1, 1, "user1", '2025-10-20 11:00');
        cy.reportSkill(1, 1, "user2", '2025-10-20 11:00');
        cy.reportSkill(1, 1, "user3", '2025-10-20 11:00');

        cy
            .intercept('/admin/projects/proj1/metrics/binnedUsagePostAchievementMetricsBuilder**')
            .as('binnedUsagePostAchievementMetricsBuilder');

        cy
            .intercept('/admin/projects/proj1/metrics/usagePostAchievementMetricsBuilder**')
            .as('usagePostAchievementMetricsBuilder');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@binnedUsagePostAchievementMetricsBuilder');
        cy.wait('@usagePostAchievementMetricsBuilder');

        cy.get('[data-cy="numUsersPostAchievement"] [data-cy="chartDownloadMenu"]').click()
        cy.get('[data-p="popup"] [aria-label="Export to CSV"]').click()

        // Wait for the file to be downloaded
        const expectedCsv = `Category,Number of Users
stopped after achieving,2
performed Skill at least once after achieving,1
`
        cy.validateCsvFile(expectedCsv)

    });
});
