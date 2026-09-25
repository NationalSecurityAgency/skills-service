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


describe('Inception Tests', () => {

    beforeEach(() => {
    });

    it('level button updates when project navigation earns the next Inception level', function () {
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="inception-button"]').should('be.visible').and('contain.text', 'Level 0');

        // CreateSkill earns 10 points per report. Leave the user within one
        // VisitProjectLevels event (10 points) of the current level 1 threshold.
        cy.request('/api/projects/Inception/summary').then(({ body }) => {
            expect(body.skillsLevel).to.eq(0);
            const reports = Math.ceil((body.levelTotalPoints - body.levelPoints) / 10) - 1;
            Cypress._.times(reports, (index) => {
                cy.doReportSkill({
                    project: 'Inception',
                    skill: 'CreateSkill',
                    userId: null,
                    date: `${index + 1} days ago`,
                });
            });
        });

        // Verify the seeded progress is still below level 1 before the UI action.
        cy.request('/api/projects/Inception/summary').then(({ body }) => {
            expect(body.skillsLevel).to.eq(0);
            expect(body.levelTotalPoints - body.levelPoints).to.be.within(1, 10);
        });
        cy.get('[data-cy="inception-button"]').should('contain.text', 'Level 0');

        cy.get('[data-cy="nav-Levels"]').click();
        cy.get('[data-cy="levelsTable"]').should('be.visible');
        cy.get('[data-cy="inception-button"]').should('be.visible').and('contain.text', 'Level 1');
    });

    it('navigation to dashboard skills', function () {
        cy.suppressChartInitError()
        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard Skills');
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3)
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // navigate to subject
        cy.get('[data-cy="subjectTileBtn"]').eq(2).click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard');
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // navigate to skill
        cy.get('[data-cy="skillProgressTitle-VisitDashboardSkills"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Skill Overview');

        // back to subject
        cy.go('back')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard');
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // to subject's rank
        cy.get('[data-cy="myRankBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('My Rank');
        cy.get('[data-cy="leaderboardTable"]')

        // back to subject
        cy.go('back')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard');
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // up the breadcrumb to dashboard skill
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard Skills');
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3)
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // to project's rank
        cy.get('[data-cy="myRankBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('My Rank');
        cy.get('[data-cy="leaderboardTable"]')

        // back to dashboard skills
        cy.go('back')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard Skills');
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3)
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // to badges
        cy.get('[data-cy="myBadgesBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('My Badges');

        // to a specific badge
        cy.get('[data-cy="badgeDetailsLink_CommunitySuperHero"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Badge Details');
    });


    it('navigate to inception then to a catalog-based project then back to inception', function () {
        cy.createProject(1)
        cy.enableProdMode(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })
        cy.addToMyProjects(1);

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard Skills');
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3)
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="subjectTile-Projects"]')

        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.get('[data-cy="project-link-proj1"]').click()
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 1)
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="subjectTile-subj1"]')

        cy.get('[data-cy="settings-button"]').click()
        cy.get('[data-pc-name="menu"] [aria-label="Project Admin"] [data-pc-section="itemlink"]').click()

        cy.get('[data-cy="inception-button"]').click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard Skills');
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3)
        cy.get('[data-cy="subjectTile-Projects"]')
    })

    it('config state is loaded for inception', function () {
        cy.createProject(1)
        cy.enableProdMode(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })
        cy.addToMyProjects(1);

        cy.intercept('GET', '/public/clientDisplay/config?projectId=Inception').as('getConfig');

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard Skills');
        cy.get('[data-cy="subjectTileBtn"]').should('have.length', 3)
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="subjectTile-Projects"]')
        cy.wait('@getConfig').then((interception) => {
            // You can make additional assertions on the request or response
            expect(interception.response.statusCode).to.eq(200)
        })

        // navigate to subject
        cy.get('[data-cy="subjectTileBtn"]').eq(2).click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Dashboard');
        cy.get('[data-cy="pointHistoryChartNoData"]')

        // navigate to skill
        cy.get('[data-cy="skillProgressTitle-ShareSkillTreeSuccessStory"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains('Skill Overview');

        // no description error should be visible
        cy.get('[data-cy="requestApprovalBtn"]').click();
        cy.get('[data-cy="descriptionError"]').should('not.be.visible');
        cy.get('[data-cy="selfReportMsgInput"]').type('some val');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible');
    })

});
