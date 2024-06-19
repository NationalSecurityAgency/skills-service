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

describe('Dashboard User Actions Tests', () => {

    let adminUserIdForDisplay;
    beforeEach(() => {
        cy.fixture('vars.json')
          .then((vars) => {
              adminUserIdForDisplay = vars.defaultUser;
              if (Cypress.env('oauthMode')) {
                  const projAdminUser = vars.oauthUser;
                  adminUserIdForDisplay = vars.oauthUser.substring(0, projAdminUser.indexOf('@'));
              }
          });
    });

    it('Display user activity history on multiple pages', () => {
        cy.createProject(1)
        cy.createProject(2)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)
        cy.createSkill(1, 1, 5)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '13')

        cy.get('[data-cy="row0-userId"]').contains(adminUserIdForDisplay)
        cy.get('[data-cy="row0-action"]').contains('Edit')
        cy.get('[data-cy="row0-item"]').contains('Skill')
        cy.get('[data-cy="row0-itemId"]').contains('skill5')
        cy.get('[data-cy="row0-projectId"]').contains('proj1')
        cy.get('[data-cy="row0-quizId"]').should('be.empty')

        cy.get('[data-cy="row3-userId"]').contains(adminUserIdForDisplay)
        cy.get('[data-cy="row3-action"]').contains('Create')
        cy.get('[data-cy="row3-item"]').contains('Skill')
        cy.get('[data-cy="row3-itemId"]').contains('skill4')
        cy.get('[data-cy="row3-projectId"]').contains('proj1')
        cy.get('[data-cy="row3-quizId"]').should('be.empty')

        cy.get('[data-cy="row9-userId"]')
        cy.get('[data-cy="row10-userId"]').should('not.exist')

        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="row0-expandedDetails"').contains('Skill Id:')
        cy.get('[data-cy="row0-expandedDetails"').contains('skill5')

        cy.get('[data-p-index="2"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="row2-expandedDetails"').contains('Skill Id:')
        cy.get('[data-cy="row2-expandedDetails"').contains('skill4')

        // testing pagination
        cy.get('[data-pc-section="pagebutton"]').contains('2').click();
        cy.get('[data-cy="row0-userId"]').contains(adminUserIdForDisplay)
        cy.get('[data-cy="row0-action"]').contains('Create')
        cy.get('[data-cy="row0-item"]').contains('Project')
        cy.get('[data-cy="row0-itemId"]').contains('proj2')
        cy.get('[data-cy="row0-projectId"]').contains('proj2')
        cy.get('[data-cy="row0-quizId"]').should('be.empty')
        cy.get('[data-cy="row2-userId"]')
        cy.get('[data-cy="row3-userId"]').should('not.exist')

        // test different page size
        cy.get('[data-cy="dashboardActionsForEverything"] [data-pc-name="rowperpagedropdown"]').click().get('[data-pc-section="item"]').contains('25').click();
        cy.get('[data-cy="row12-userId"]')
        cy.get('[data-cy="row13-userId"]').should('not.exist')
    });

    it('Filter by user id', () => {
        cy.createProject(1)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
        cy.get('[data-cy="row0-userId"]').contains(adminUserIdForDisplay)
        cy.get('[data-cy="row1-userId"]').contains('root@skills.org')

        cy.get('[data-cy="userFilter"]').type('OoT')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-userId"]').contains('root@skills.org')
        cy.get('[data-cy="row1-userId"]').should('not.exist')

        // reset and filter via enter
        cy.get('[data-pc-section="filterclearicon"]').eq(0).click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
        cy.get('[data-cy="userFilter"]').should('be.empty')
        cy.get('[data-cy="userFilter"]').type('OoT{enter}')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-userId"]').contains('root@skills.org')
        cy.get('[data-cy="row1-userId"]').should('not.exist')
    })

    it('Filter by action', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSubject(1, 1)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '4')
        cy.get('[data-cy="row0-action"]').contains('Edit')
        cy.get('[data-cy="row1-action"]').contains('Create')

        cy.get('[data-cy="actionFilter"]').click()
        cy.get('[data-pc-section="itemlabel"]').contains('Edit').click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-action"]').contains('Edit')
        cy.get('[data-cy="row1-action"]').should('not.exist')

        // reset
        cy.get('[data-pc-section="filterclearicon"]').eq(1).click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '4')
    })

    it('Filter by item', () => {
        cy.createProject(1)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
        cy.get('[data-cy="row0-item"]').contains('Project')
        cy.get('[data-cy="row1-item"]').contains('Settings')

        cy.get('[data-cy="itemFilter"]').click()
        cy.get('[data-pc-section="itemlabel"]').contains('Settings').click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-item"]').contains('Settings')
        cy.get('[data-cy="row1-item"]').should('not.exist')

        // reset
        cy.get('[data-pc-section="filterclearicon"]').eq(2).click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
    })

    it('Filter by item id', () => {
        cy.createProject(1)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
        cy.get('[data-cy="row0-itemId"]').contains('proj1')
        cy.get('[data-cy="row1-itemId"]').contains('EmailSettings')

        cy.get('[data-cy="itemIdFilter"]').type('seTT')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-itemId"]').contains('EmailSettings')
        cy.get('[data-cy="row1-itemId"]').should('not.exist')

        // reset and filter via enter
        cy.get('[data-pc-section="filterclearicon"]').eq(3).click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
        cy.get('[data-cy="itemIdFilter"]').should('be.empty')
        cy.get('[data-cy="itemIdFilter"]').type('seTT{enter}')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-itemId"]').contains('EmailSettings')
        cy.get('[data-cy="row1-itemId"]').should('not.exist')
    })

    it('Filter by project id', () => {
        cy.createProject(1)
        cy.createProject(2)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')
        cy.get('[data-cy="row0-projectId"]').contains('proj2')
        cy.get('[data-cy="row1-projectId"]').contains('proj1')
        cy.get('[data-cy="row2-projectId"]').should('be.empty')

        cy.get('[data-cy="projectIdFilter"]').type('2')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-projectId"]').contains('proj2')
        cy.get('[data-cy="row1-projectId"]').should('not.exist')

        // reset and filter via enter
        cy.get('[data-pc-section="filterclearicon"]').eq(4).click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')
        cy.get('[data-cy="projectIdFilter"]').should('be.empty')
        cy.get('[data-cy="projectIdFilter"]').type('2{enter}')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-projectId"]').contains('proj2')
        cy.get('[data-cy="row1-projectId"]').should('not.exist')
    })

    it('Filter by quiz id', () => {
        cy.createQuizDef(1)
        cy.createQuizDef(2)
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
        });

        cy.visit('/administrator/userActions')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')
        cy.get('[data-cy="row0-projectId"]').should('be.empty')
        cy.get('[data-cy="row1-projectId"]').should('be.empty')
        cy.get('[data-cy="row2-projectId"]').should('be.empty')
        cy.get('[data-cy="row0-quizId"]').contains('quiz2')
        cy.get('[data-cy="row1-quizId"]').contains('quiz1')
        cy.get('[data-cy="row2-quizId"]').should('be.empty')

        cy.get('[data-cy="quizIdFilter"]').type('2')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-quizId"]').contains('quiz2')
        cy.get('[data-cy="row1-quizId"]').should('not.exist')

        // reset and filter via enter
        cy.get('[data-pc-section="filterclearicon"]').eq(5).click()
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')
        cy.get('[data-cy="quizIdFilter"]').should('be.empty')
        cy.get('[data-cy="quizIdFilter"]').type('2{enter}')
        cy.get('[data-cy="dashboardActionsForEverything"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="row0-quizId"]').contains('quiz2')
        cy.get('[data-cy="row1-quizId"]').should('not.exist')
    })
});
