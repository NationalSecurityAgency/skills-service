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

describe('Metrics Tests', () => {

    const waitForSnap = 4000;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('end-to-end metrics with real user data', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Some other subject",
        })

        const numSkills =3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Skill ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '25',
            });
        };

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'This is a cool badge',
            "iconClass":"fas fa-jedi",
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        cy.reportHistoryOfEvents('proj1', 'user0Good@skills.org', 25, [6,7], ['skill1', 'skill2', 'skill3']);
        cy.reportHistoryOfEvents('proj1', 'user1Long0@skills.org', 12, [3], ['skill1', 'skill2', 'skill3']);
        cy.reportHistoryOfEvents('proj1', 'user2Smith0@skills.org', 20, [5,6], ['skill1', 'skill2', 'skill3']);

        cy.reportHistoryOfEvents('proj1', 'user3Some0@skills.org', 25, [], ['skill1']);

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
    })

    it('projects - Distinct number of users over time', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject',
            status: 200,
            response: [{
                'value': 1600128000000,
                'count': 2
            }, {
                'value': 1600214400000,
                'count': 2
            }, {
                'value': 1600300800000,
                'count': 2
            }, {
                'value': 1600387200000,
                'count': 1
            }, {
                'value': 1600473600000,
                'count': 1
            }, {
                'value': 1600560000000,
                'count': 2
            }, {
                'value': 1600646400000,
                'count': 3
            }, {
                'value': 1600732800000,
                'count': 3
            }, {
                'value': 1600819200000,
                'count': 3
            }, {
                'value': 1600905600000,
                'count': 3
            }, {
                'value': 1600992000000,
                'count': 2
            }, {
                'value': 1601078400000,
                'count': 1
            }, {
                'value': 1601164800000,
                'count': 2
            }, {
                'value': 1601251200000,
                'count': 4
            }, {
                'value': 1601337600000,
                'count': 3
            }, {
                'value': 1601424000000,
                'count': 4
            }, {
                'value': 1601510400000,
                'count': 4
            }, {
                'value': 1601596800000,
                'count': 3
            }, {
                'value': 1601683200000,
                'count': 12
            }, {
                'value': 1601769600000,
                'count': 10
            }, {
                'value': 1601856000000,
                'count': 60
            }, {
                'value': 1601942400000,
                'count': 45
            }, {
                'value': 1602028800000,
                'count': 2
            }, {
                'value': 1602115200000,
                'count': 52
            }, {
                'value': 1602201600000,
                'count': 72
            }],
        }).as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

    it('projects - Distinct number of users over time - empty project', () => {
        cy.server().route('/admin/projects/proj1/charts/distinctUsersOverTimeForProject').as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.get('[data-cy=distinctNumUsersOverTime]').contains('This chart needs at least 2 days of user activity.');

        cy.get('[data-cy=distinctNumUsersOverTime]').get('.apexcharts-svg').get('line');

        // verify there is no chart
        cy.get('[data-cy=distinctNumUsersOverTime]').get('.apexcharts-svg .apexcharts-area-series')
            .should('be.visible')
            .and(chart => {
                expect(chart.height()).to.be.lessThan(200)
            });

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

    it('projects - Distinct number of users over time - two days with real data', () => {
        cy.server().route('/admin/projects/proj1/charts/distinctUsersOverTimeForProject').as('distinctUsersOverTimeForProject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })
        const numSkills =1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Skill ${skillsCounter}`,
                pointIncrement: '200',
                numPerformToCompletion: '25',
            });
        };

        const m = moment.utc();
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        // make sure a line i rendered on a chart
        cy.get('[data-cy=distinctNumUsersOverTime]').get('.apexcharts-svg .apexcharts-area-series')
            .should('be.visible')
            .and(chart => {
                expect(chart.height()).to.be.greaterThan(200)
            });
    })

    it('projects - Distinct number of users over time - two days', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject',
            status: 200,
            response: [ {
                'value': 1602115200000,
                'count': 52
            }, {
                'value': 1602201600000,
                'count': 82
            }],
        }).as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

    it('projects - Distinct number of users over time - one days', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject',
            status: 200,
            response: [ {
                'value': 1602115200000,
                'count': 52
            }],
        }).as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]').contains('This chart needs at least 2 days of user activity.');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })



    it('subjects - num users per level over time - not subjects', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').should('be.disabled');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('subjects - num users per level over time - subject has no data', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        });
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').should('be.disabled');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime-subjectSelector]').select('Subject 1');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').should('not.be.disabled');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').click();
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Zero users achieved levels for this subject!');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('subjects - num users per level over time - subject has little data', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1')
            .as('getLevelsOverTimeData');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        });
        const numSkills = 3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Skill ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '3',
            });
        };
        cy.reportHistoryOfEvents('proj1', 'user0Good@skills.org', 1, [], ['skill1', 'skill2', 'skill3']);

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime-subjectSelector]').select('Subject 1');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').click();

        cy.wait('@getLevelsOverTimeData')
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Level 1');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    function generateDayWiseTimeSeries(xValStart, count, increaseBy) {
        let baseXVal = xValStart;
        let baseYVal = 0;
        let i = 0;
        const series = [];
        while (i < count) {
            const x = baseXVal;
            const y = baseYVal;
            series.push({
                'value': baseXVal,
                'count': baseYVal
            });

            baseXVal += 86400000;
            baseYVal += increaseBy;
            i += 1;
        }
        return series;
    }

    it('subjects - num users per level over time - multiple levels', () => {
        cy.server().route({
            url: '/admin/projects/proj1/subjects',
            status: 200,
            response: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');

        cy.server().route({
            url: '/admin/projects/proj1/charts/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
            status: 200,
            response: [{
                'level': 1,
                'counts': generateDayWiseTimeSeries(new Date('11 Mar 2020').getTime(), 30, 10),
            }, {
                'level': 2,
                'counts': generateDayWiseTimeSeries(new Date('26 Mar 2020').getTime(), 15, 11),
            }, {
                'level': 3,
                'counts': generateDayWiseTimeSeries(new Date('2 Apr 2020').getTime(), 8, 5),
            }],
        }).as('getLevelsOverTimeData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getSubjects');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime-subjectSelector]').select('Subject 1');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').click();

        cy.wait('@getLevelsOverTimeData');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Level 1');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('subjects - num users per level over time - long history', () => {
        cy.server().route({
            url: '/admin/projects/proj1/subjects',
            status: 200,
            response: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');

        cy.server().route({
            url: '/admin/projects/proj1/charts/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
            status: 200,
            response: [{
                'level': 1,
                'counts': generateDayWiseTimeSeries(new Date('11 Mar 2020').getTime(), 126, 15),
            }, {
                'level': 2,
                'counts': generateDayWiseTimeSeries(new Date('26 Mar 2020').getTime(), 111, 11),
            }, {
                'level': 3,
                'counts': generateDayWiseTimeSeries(new Date('2 Apr 2020').getTime(), 104, 5),
            }, {
                'level': 4,
                'counts': generateDayWiseTimeSeries(new Date('22 Apr 2020').getTime(), 84, 3),
            }, {
                'level': 5,
                'counts': generateDayWiseTimeSeries(new Date('1 June 2020').getTime(), 44, 3),
            }],
        }).as('getLevelsOverTimeData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getSubjects');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime-subjectSelector]').select('Subject 1');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').click();

        cy.wait('@getLevelsOverTimeData');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Level 1');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('subjects - num users per level over time - many levels', () => {
        cy.server().route({
            url: '/admin/projects/proj1/subjects',
            status: 200,
            response: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');

        const numLevels = 10
        const response = [];
        for (let i = 1 ; i <= numLevels; i+= 1) {
            response.push(
                {
                    'level': i,
                    'counts': generateDayWiseTimeSeries(new Date('11 Mar 2020').getTime() + 86400000 * i*10, 200-i*10, 40-i*2),
                }
            )
        }

        cy.server().route({
            url: '/admin/projects/proj1/charts/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
            status: 200,
            response,
        }).as('getLevelsOverTimeData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getSubjects');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime-subjectSelector]').select('Subject 1');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').click();

        cy.wait('@getLevelsOverTimeData');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Level 1');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('subjects - num users per level over time - higher levels have more users than lower', () => {
        cy.server().route({
            url: '/admin/projects/proj1/subjects',
            status: 200,
            response: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');

        const numLevels = 5
        const response = [];
        for (let i = 1 ; i <= numLevels; i+= 1) {
            response.push(
                {
                    'level': i,
                    'counts': generateDayWiseTimeSeries(new Date('11 Mar 2020').getTime() + 86400000 * i*10, 200-i*10, i),
                }
            )
        }

        cy.server().route({
            url: '/admin/projects/proj1/charts/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
            status: 200,
            response,
        }).as('getLevelsOverTimeData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getSubjects');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime-subjectSelector]').select('Subject 1');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').click();

        cy.wait('@getLevelsOverTimeData');
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Level 1');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('achievements table', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills =3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '1',
            });
        };

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'This is a cool badge',
            "iconClass":"fas fa-jedi",
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1Long0@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user1Long0@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})

        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user2Smith0@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user2Smith0@skills.org', timestamp: m.clone().subtract(6, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
    })

    it('subjects - num users per level', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerSubjectPerLevelChartBuilder',
            status: 200,
        }).as('getChartData');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Interesting Subject 2",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
            projectId: 'proj1',
            subjectId: 'subj3',
            name: "Interesting Subject 3",
        })

        const numSkills =3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '1',
            });
        };

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'This is a cool badge',
            "iconClass":"fas fa-jedi",
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1Long0@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user1Long0@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})

        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user2Smith0@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user2Smith0@skills.org', timestamp: m.clone().subtract(6, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    function createSubjectObj(name, numUsers) {
        const res = {
            subject: name,
            numUsersPerLevels: [],
        }
        for (let i = 1; i <= numUsers.length; i+= 1) {
            res.numUsersPerLevels.push({
                level: i,
                numberUsers: numUsers[i-1],
            })
        }
        return res;
    }

    it('subjects - num users per level - typical 6 subjects', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response: [
                createSubjectObj('First Cool Subject', [23,293,493,625,293]),
                createSubjectObj('Awesome Subject', [1265,2352,493,625,293]),
                createSubjectObj('Other Subject', [1254,1000,852,625,293]),
                createSubjectObj('Where subjects no go', [856,293,493,625,293]),
                createSubjectObj('Short', [325,293,493,625,293]),
                createSubjectObj('Interesting Subject', [1568,859,493,625,293]),
            ],
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - many subjects', () => {
        const response = [];
        for (let i=0; i < 15; i+=1) {
            response.push(createSubjectObj(`Subject # ${i}`, [1265,852,493,625,293]))
        }

        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response,
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - many levels', () => {
        const response = [];
        for (let i=0; i < 6; i+=1) {
            response.push(createSubjectObj(`Subject # ${i}`, [1265,852,493,625,293,392,293,983,1923,1209]))
        }

        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response,
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.contains("Subject # 5")
        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - empty', () => {
        const response = [];
        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response,
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })


    it('overall levels - empty', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.contains("Level 5: 0 users");
        // we have to wait for background animation to complete
        cy.wait(waitForSnap);
        cy.get('[data-cy=projectOverallLevelsChart]').matchImageSnapshot('projectOverallLevelsChart-empty');
    });

    it('overall levels - users in all levels', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerLevelChartBuilder',
            status: 200,
            response: [{
                'value': 'Level 1',
                'count': 6251
            }, {
                'value': 'Level 2',
                'count': 4521
            }, {
                'value': 'Level 3',
                'count': 3525
            }, {
                'value': 'Level 4',
                'count': 1254
            }, {
                'value': 'Level 5',
                'count': 754
            }],
        }).as('getLevels');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.contains("Level 5: 754 users");
        cy.contains("Level 4: 1,254 users");
        cy.contains("Level 3: 3,525 users");
        cy.contains("Level 2: 4,521 users");
        cy.contains("Level 1: 6,251 users");

        // we have to wait for background animation to complete
        cy.wait(waitForSnap);
        cy.get('[data-cy=projectOverallLevelsChart]').matchImageSnapshot('projectOverallLevelsChart-allLevels');
    });

})
