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
var moment = require('moment-timezone');

describe('Multiple Project Metrics', () => {
    const waitForSnap = 4000;
    const multiProjSel = '[data-cy=multiProjectUsersInCommon]';
    const trainingProfSel = '[data-cy=trainingProfileComparator]';


    after(() => {
        Cypress.env('disableResetDb', false);
    });

    before(() => {
        Cypress.Commands.add("usrsInCommon", (optionalSelector = null) => {
            return cy.get(`${multiProjSel}${optionalSelector ? (' ' + optionalSelector) : ''}`);
        });
        Cypress.Commands.add("trainingProf", (optionalSelector = null) => {
            return cy.get(`${trainingProfSel}${optionalSelector ? (' ' + optionalSelector) : ''}`);
        });

        Cypress.env('disableResetDb', true);

        cy.resetDb();

        cy.fixture('vars.json').then((vars) => {
            const oauthMode = Cypress.env('oauthMode');
            const projectOwner = oauthMode ? Cypress.env('proxyUser') : vars.defaultUser;
            cy.log(`oauthMode: [${oauthMode}], projectOwner[${projectOwner}]`);
            if (!oauthMode) {
                cy.login(vars.defaultUser, vars.defaultPass);
            } else {
                cy.loginBySingleSignOn()
            }

            const numProj = 6;
            for (let i =0; i < numProj; i+=1) {
                const projId = `proj${i}`
                cy.request('POST', `/app/projects/${projId}`, {
                    projectId: projId,
                    name: `Grand Project ${i}`
                })

                if (i < 2) {
                    cy.request('POST', `/admin/projects/${projId}/levels/next`, {
                        percent: 93,
                        name: `Cool Level`
                    })
                    cy.request('POST', `/admin/projects/${projId}/levels/next`, {
                        percent: 95,
                        name: `Cool Level`
                    })
                }
                else if (i < 3) {
                    cy.request('POST', `/admin/projects/${projId}/levels/next`, {
                        percent: 93,
                        name: `Cool Level`
                    })
                }

                cy.request('POST', `/admin/projects/${projId}/subjects/subj1`, {
                    projectId: 'proj1',
                    subjectId: 'subj1',
                    name: "Interesting Subject 1",
                })

                const numSkills = 3;
                for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
                    cy.request('POST', `/admin/projects/${projId}/subjects/subj1/skills/skill${skillsCounter}`, {
                        projectId: projId,
                        subjectId: 'subj1',
                        skillId: `skill${skillsCounter}`,
                        name: `Very Great Skill # ${skillsCounter}`,
                        pointIncrement: '150',
                        numPerformToCompletion: 1,
                    });
                };

                for (let badgeCount = 0; badgeCount <= i; badgeCount += 1) {
                    cy.request('POST', `/admin/projects/${projId}/badges/badge${badgeCount}`, {
                        projectId: projId,
                        badgeId: `badge${badgeCount}`,
                        name: `Badge ${badgeCount}`,
                        "iconClass": "fas fa-ghost",
                    });
                }

                const numUsers = 9;
                for (let usersCounter = 1; usersCounter <= numUsers; usersCounter += 1) {
                    const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
                    cy.request('POST', `/api/projects/${projId}/skills/skill1`, { userId: `user${usersCounter}@skills.org`,
                        timestamp: m.clone()
                            .subtract(1, 'day')
                            .format('x')
                    })
                    if (i < 3 && usersCounter > 4) {
                        cy.request('POST', `/api/projects/${projId}/skills/skill2`, {
                            userId: `user${usersCounter}@skills.org`,
                            timestamp: m.clone()
                                .subtract(4, 'day')
                                .format('x')
                        });
                    }
                    if (i < 6 && usersCounter > 5) {
                        cy.request('POST', `/api/projects/${projId}/skills/skill3`, {
                            userId: `user${usersCounter}@skills.org`,
                            timestamp: m.clone()
                                .subtract(5, 'day')
                                .format('x')
                        });
                    }
                }
            }

            cy.logout();

            cy.login(vars.rootUser, vars.defaultPass);
            // cy.request('PUT', `/root/users/${vars.rootUser}/roles/ROLE_SUPERVISOR`);
            cy.request('PUT', `/root/users/${projectOwner}/roles/ROLE_SUPERVISOR`);

            cy.logout();
            if (!oauthMode) {
                cy.login(vars.defaultUser, vars.defaultPass);
            } else {
                cy.loginBySingleSignOn()
            }
        });
    });

    it('only root or supervisor should see multiple project metrics', () => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();

            const newUsers = 'otherUser1@skills.org';
            cy.register(newUsers, vars.defaultPass);
            cy.login(newUsers, vars.defaultPass);
            cy.visit('/administrator/');
            // wait for nav to finish loading
            cy.wait(1000);
            cy.get('[data-cy=nav-Metrics]').should('not.exist')

            cy.logout();

            cy.login(vars.rootUser, vars.defaultPass);
            cy.request('PUT', `/root/users/${newUsers}/roles/ROLE_SUPERVISOR`);
            cy.logout();
            cy.login(newUsers, vars.defaultPass);
            cy.visit('/administrator/');
            cy.clickNav('Metrics');
            cy.contains('No Projects Selected')
        });
    });

    it('Project definitions comparison chart loads 4 projects by default', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');

        cy.trainingProf('[data-cy=trainingProfileComparatorProjectSelector] .multiselect__tag').should('have.length', 4).as('selected');
        cy.get('@selected').eq(0).contains('Inception');

        // validate x axis
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 1');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 2');

        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Inception');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=totalAvailablePointsChart').contains('Grand Project 1');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 2');

        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 1');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 2');

        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 1');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 2');

        cy.wait(waitForSnap);
        cy.get(`${trainingProfSel} [data-cy=numOfSkillsChart]`).matchImageSnapshot('Project definitions comparison - Number of Skills chart');
        cy.get(`${trainingProfSel} [data-cy=totalAvailablePointsChart]`).matchImageSnapshot('Project definitions comparison - Total Available Points');
        cy.get(`${trainingProfSel} [data-cy=numOfSubjChart]`).matchImageSnapshot('Project definitions comparison - Number of Subjects chart');
        cy.get(`${trainingProfSel} [data-cy=numOfBadgesChart]`).matchImageSnapshot('Project definitions comparison - Number of Badges chart');
    });

    it('Project definitions comparison generates charts only after 2 projects are selected', () => {
        cy.viewport('macbook-11');
        cy.visit('/administrator/');
        cy.clickNav('Metrics');

        cy.trainingProf('[data-cy=trainingProfileComparatorProjectSelector]  .multiselect__tag-icon').should('have.length', 4).as('removeBtns');
        cy.get('@removeBtns').eq(1).click()
        cy.get('@removeBtns').eq(1).click()
        cy.get('@removeBtns').eq(1).click()

        cy.trainingProf().contains('Need more projects');
    });

    it('Project definitions comparison allows up to 5 projects', () => {
        cy.viewport('macbook-11');
        cy.visit('/administrator/');
        cy.clickNav('Metrics');

        cy.trainingProf('[data-cy=trainingProfileComparatorProjectSelector]').click()
        cy.trainingProf().contains('Grand Project 4').click()
        cy.trainingProf('[data-cy=trainingProfileComparatorProjectSelector]').click()

        cy.trainingProf().contains('Maximum of 5 options selected');
    });

    it('Project definitions comparison - remove project', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');

        cy.trainingProf('[data-cy=trainingProfileComparatorProjectSelector]  .multiselect__tag-icon').should('have.length', 4).as('removeBtns');
        cy.get('@removeBtns').eq(2).click()

        // validate x axis
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 1').should('not.exist');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 2');

        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Inception');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=totalAvailablePointsChart').contains('Grand Project 1').should('not.exist');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 2');

        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 1').should('not.exist');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 2');

        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 1').should('not.exist');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 2');
    });

    it('Project definitions comparison - add project', () => {
        cy.viewport('macbook-11');
        cy.visit('/administrator/');
        cy.clickNav('Metrics');

        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 5').should('not.exist');

        cy.trainingProf('[data-cy=trainingProfileComparatorProjectSelector]').click()
        cy.trainingProf().contains('Grand Project 5').click()

        // validate x axis
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 1');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 2');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 5');

        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Inception');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=totalAvailablePointsChart').contains('Grand Project 1');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 2');
        cy.trainingProf('[data-cy=totalAvailablePointsChart]').contains('Grand Project 5');

        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 1');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 2');
        cy.trainingProf('[data-cy=numOfSubjChart]').contains('Grand Project 5');

        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 0');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 1');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 2');
        cy.trainingProf('[data-cy=numOfBadgesChart]').contains('Grand Project 5');
    });


    it('find button should be disabled until 2 projects are selected', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains('Grand Project 0').click();
        cy.usrsInCommon('[data-cy=findUsersBtn]').should('be.disabled');

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains('Grand Project 3').click();
        cy.usrsInCommon('[data-cy=findUsersBtn]').should('be.enabled');
    });

    it('only support up to five projects', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        for (let i=0; i<5; i+= 1) {
            cy.usrsInCommon('[data-cy=projectSelector]').click();
            cy.usrsInCommon().contains(`Grand Project ${i}`).click();
        }

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains("Maximum of 5 options selected")
        cy.usrsInCommon().contains(`Grand Project 5`).should('not.exist')
    });

    it('sync levels', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        for (let i=0; i<5; i+= 1) {
            cy.usrsInCommon('[data-cy=projectSelector]').click();
            cy.usrsInCommon().contains(`Grand Project ${i}`).click();
        }

        const tableSelector = `${multiProjSel} tbody tr`
        cy.get(tableSelector).should('have.length', 5).as('cyRows');
        cy.get('@cyRows').eq(0).find('td').as('row1');

        for (let i = 0; i < 5; i += 1) {
            cy.get('@cyRows').eq(i).find('td').as('row');
            cy.get('@row').eq(5).find('[data-cy=minLevelSelector]').contains('1')
        }

        cy.get('@row1').eq(5).find('[data-cy=minLevelSelector]').select('2');
        cy.get('@row1').eq(5).find('[data-cy=syncLevelButton]').click();

        for (let i = 0; i < 5; i += 1) {
            cy.get('@cyRows').eq(i).find('td').as('row');
            cy.get('@row').eq(5).find('[data-cy=minLevelSelector]').contains('2')
        }
    });

    it('if sync level higher than max level then just use max level', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        for (let i = 0; i < 5; i += 1) {
            cy.usrsInCommon('[data-cy=projectSelector]').click();
            cy.usrsInCommon().contains(`Grand Project ${i}`).click();
        }

        const tableSelector = `${multiProjSel} tbody tr`
        cy.get(tableSelector)
            .should('have.length', 5)
            .as('cyRows');
        cy.get('@cyRows')
            .eq(1)
            .find('td')
            .as('row1');

        cy.get('@row1')
            .eq(5)
            .find('[data-cy=minLevelSelector]')
            .select('7');
        cy.get('@row1')
            .eq(5)
            .find('[data-cy=syncLevelButton]')
            .click();

        for (let i = 0; i < 5; i += 1) {
            cy.get('@cyRows')
                .eq(i)
                .find('td')
                .as('row');
            cy.get('@row')
                .eq(5)
                .find('[data-cy=minLevelSelector]')
                .contains(i < 2 ? '7' : (i === 2 ? 6 : '5'))
        }
    })


    it('sort and page through the result table', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        for (let i = 0; i < 2; i += 1) {
            cy.usrsInCommon('[data-cy=projectSelector]').click();
            cy.usrsInCommon().contains(`Grand Project ${i}`).click();
        }

        const tableSelector = `${multiProjSel} tbody tr`
        cy.get(tableSelector).should('have.length', 2);

        cy.get('[data-cy=findUsersBtn]').click();

        const resTable = '[data-cy=usersInCommonResultTable]'
        const expected = [
            [{ colIndex: 0,  value: 'user1@skills.org' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }],
            [{ colIndex: 0,  value: 'user6@skills.org' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }],
            [{ colIndex: 0,  value: 'user9@skills.org' }],
        ]
        cy.validateTable(resTable, expected);

        // sort
        cy.get(resTable).contains('User').click();
        const expected1 = [
            [{ colIndex: 0,  value: 'user9@skills.org' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }],
            [{ colIndex: 0,  value: 'user6@skills.org' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }],
        ]
        cy.validateTable(resTable, expected1);
    })

    it('adjust page size of the result table', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        for (let i = 0; i < 2; i += 1) {
            cy.usrsInCommon('[data-cy=projectSelector]').click();
            cy.usrsInCommon().contains(`Grand Project ${i}`).click();
        }

        const tableSelector = `${multiProjSel} tbody tr`
        cy.get(tableSelector).should('have.length', 2);

        cy.get('[data-cy=findUsersBtn]').click();

        cy.get(`${multiProjSel} [data-cy=skillsBTablePageSize]`).select('10');

        const resTable = '[data-cy=usersInCommonResultTable]'
        const expected = [
            [{ colIndex: 0,  value: 'user1@skills.org' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }],
            [{ colIndex: 0,  value: 'user6@skills.org' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }],
            [{ colIndex: 0,  value: 'user9@skills.org' }],
        ]
        cy.validateTable(resTable, expected, 10, true);
    })

    it('filter by level', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 0`).click();
        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 5`).click();

        const tableSelector = `${multiProjSel} tbody tr`
        cy.get(tableSelector).should('have.length', 2).as('inputRows');

        cy.get('[data-cy=findUsersBtn]').click();

        const resTable = '[data-cy=usersInCommonResultTable]'
        // validate 3 columns
        cy.get(`${resTable} th`).should('have.length', 3);
        const expected = [
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 1,  value: 'Level 2' }, { colIndex: 2,  value: 'Level 2' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 1,  value: 'Level 2' }, { colIndex: 2,  value: 'Level 2' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 1,  value: 'Level 2' }, { colIndex: 2,  value: 'Level 2' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 1,  value: 'Level 2' }, { colIndex: 2,  value: 'Level 2' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 1,  value: 'Level 3' }, { colIndex: 2,  value: 'Level 2' }],
            [{ colIndex: 0,  value: 'user6@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user9@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
        ]
        cy.validateTable(resTable, expected);

        cy.get('@inputRows').eq(0).find('td').as('row1');
        cy.get('@row1').eq(5).find('[data-cy=minLevelSelector]').select('3');

        cy.get('[data-cy=findUsersBtn]').click();

        const expected1 = [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 1,  value: 'Level 3' }, { colIndex: 2,  value: 'Level 2' }],
            [{ colIndex: 0,  value: 'user6@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user9@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
        ]
        cy.validateTable(resTable, expected1);

        cy.get('@inputRows').eq(1).find('td').as('row2');
        cy.get('@row2').eq(5).find('[data-cy=minLevelSelector]').select('3');

        cy.get('[data-cy=findUsersBtn]').click();

        const expected2 = [
            [{ colIndex: 0,  value: 'user6@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user9@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
        ]
        cy.validateTable(resTable, expected2);


        cy.get('@inputRows').eq(0).find('td').as('row1');
        cy.get('@row1').eq(5).find('[data-cy=minLevelSelector]').select('7');

        cy.get('[data-cy=findUsersBtn]').click();

        const expected3 = [
            [{ colIndex: 0,  value: 'user6@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user7@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user8@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
            [{ colIndex: 0,  value: 'user9@skills.org' }, { colIndex: 1,  value: 'Level 7' }, { colIndex: 2,  value: 'Level 3' }],
        ]
        cy.validateTable(resTable, expected3);

        cy.get('@inputRows').eq(1).find('td').as('row2');
        cy.get('@row2').eq(5).find('[data-cy=minLevelSelector]').select('5');

        cy.get('[data-cy=findUsersBtn]').click();

        cy.get(resTable).contains('There are no records to show');
    })

    it('number of results columns are derived from input projects', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 0`).click();
        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 5`).click();

        const tableSelector = `${multiProjSel} tbody tr`
        cy.get(tableSelector).should('have.length', 2).as('inputRows');

        cy.get('[data-cy=findUsersBtn]').click();

        const resTable = '[data-cy=usersInCommonResultTable]'
        // validate 3 columns
        cy.get(`${resTable} th`).should('have.length', 3).as('headers');
        cy.get('@headers').eq(0).contains('User');
        cy.get('@headers').eq(1).contains('Grand Project 0');
        cy.get('@headers').eq(2).contains('Grand Project 5');

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 4`).click();

        cy.get('[data-cy=findUsersBtn]').click();
        cy.get(`${resTable} th`).should('have.length', 4).as('headers');
        cy.get('@headers').eq(0).contains('User');
        cy.get('@headers').eq(1).contains('Grand Project 0');
        cy.get('@headers').eq(2).contains('Grand Project 5')
        cy.get('@headers').eq(3).contains('Grand Project 4')

        cy.usrsInCommon('[data-cy=projectSelector] .multiselect__tag-icon').should('have.length', 3).as('removeBtns');
        cy.get('@removeBtns').eq(1).click()

        cy.get('[data-cy=findUsersBtn]').click();
        cy.get(`${resTable} th`).should('have.length', 3).as('headers');
        cy.get('@headers').eq(0).contains('User');
        cy.get('@headers').eq(1).contains('Grand Project 0');
        cy.get('@headers').eq(2).contains('Grand Project 4')
    });

    it('removing project from input should clear the result', () => {
        cy.visit('/administrator/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected');

        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 0`).click();
        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 5`).click();
        cy.usrsInCommon('[data-cy=projectSelector]').click();
        cy.usrsInCommon().contains(`Grand Project 4`).click();

        const tableSelector = `${multiProjSel} [data-cy=multiProjectUsersInCommon-inputProjs] tbody tr`
        cy.get(tableSelector).should('have.length', 3).as('inputRows');

        cy.get('[data-cy=findUsersBtn]').click();

        const resTable = `${multiProjSel} [data-cy=usersInCommonResultTable]`
        cy.get(`${resTable} th`).should('have.length', 4).as('headers');
        cy.get('@headers').eq(0).contains('User');
        cy.get('@headers').eq(1).contains('Grand Project 0');
        cy.get('@headers').eq(2).contains('Grand Project 5')
        cy.get('@headers').eq(3).contains('Grand Project 4')

        cy.get(`${multiProjSel} [data-cy=projectSelector] .multiselect__tag-icon`).should('have.length', 3).as('removeBtns');
        cy.get('@removeBtns').eq(1).click()
        cy.get(resTable).should('not.exist')
    });


})
