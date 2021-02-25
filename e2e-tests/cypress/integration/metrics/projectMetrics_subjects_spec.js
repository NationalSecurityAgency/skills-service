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


    it('subjects - num users per level over time - not subjects', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate the chart using controls above!');

        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').contains('Generate').should('be.disabled');

        cy.wait(waitForSnap);
        cy.get('[data-cy=subjectNumUsersPerLevelOverTime]').matchImageSnapshot();
    });

    it('subjects - num users per level over time - subject has no data', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        });
        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level over time - subject has little data', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/metrics/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1')
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
        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})
        // cy.reportHistoryOfEvents('proj1', 'user0Good@skills.org', 1, [], ['skill1', 'skill2', 'skill3']);

        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level over time - multiple levels', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/subjects',
          {
            statusCode: 200,
            body: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');

        cy.intercept('/admin/projects/proj1/metrics/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
          {
            statusCode: 200,
            body: [{
                'level': 1,
                'counts': generateDayWiseTimeSeries(moment.utc('11 Mar 2020', 'DD MMM YYY').valueOf(), 30, 10),
            }, {
                'level': 2,
                'counts': generateDayWiseTimeSeries(moment.utc('26 Mar 2020', 'DD MMM YYY').valueOf(), 15, 11),
            }, {
                'level': 3,
                'counts': generateDayWiseTimeSeries(moment.utc('26 Mar 2020', 'DD MMM YYY').valueOf(), 8, 5),
            }],
        }).as('getLevelsOverTimeData');

        cy.visit('/administrator/projects/proj1/');
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


    it('subjects - num users per level over time - multiple levels with same # of users for all the achieved levels', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/metrics/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1')
            .as('getLevelsOverTimeData');

        cy.intercept('/admin/projects/proj1/subjects',
          {
            statusCode: 200,
            body: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        });
        const numSkills = 6;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Skill ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '1',
            });
        };

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(10, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill4`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})


        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level over time - long history', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/subjects',
          {
            statusCode: 200,
            body: [{
                'subjectId': 'subj1',
                'projectId': 'proj1',
                'name': 'Subject 1',
            }],
        }).as('getSubjects');


        cy.intercept('/admin/projects/proj1/metrics/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
          {
            statusCode: 200,
            body: [{
                'level': 1,
                'counts': generateDayWiseTimeSeries(moment.utc('11 Mar 2020', 'DD MMM YYY').valueOf(), 126, 15),
            }, {
                'level': 2,
                'counts': generateDayWiseTimeSeries(moment.utc('26 Mar 2020', 'DD MMM YYY').valueOf(), 111, 11),
            }, {
                'level': 3,
                'counts': generateDayWiseTimeSeries(moment.utc('2 Apr 2020', 'DD MMM YYY').valueOf(), 104, 5),
            }, {
                'level': 4,
                'counts': generateDayWiseTimeSeries(moment.utc('22 Apr 2020', 'DD MMM YYY').valueOf(), 84, 3),
            }, {
                'level': 5,
                'counts': generateDayWiseTimeSeries(moment.utc('1 June 2020', 'DD MMM YYY').valueOf(), 44, 3),
            }],
        }).as('getLevelsOverTimeData');

        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level over time - many levels', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/subjects',
          {
            statusCode: 200,
            body: [{
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
                    'counts': generateDayWiseTimeSeries(moment.utc('11 Mar 2020', 'DD MMM YYY').valueOf() + 86400000 * i*10, 200-i*10, 40-i*2),
                }
            )
        }

        cy.intercept('/admin/projects/proj1/metrics/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
          {
            statusCode: 200,
            body: response,
        }).as('getLevelsOverTimeData');

        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level over time - higher levels have more users than lower', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/subjects',
          {
            statusCode: 200,
            body: [{
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

        cy.intercept('/admin/projects/proj1/metrics/usersByLevelForSubjectOverTimeChartBuilder?subjectId=subj1',
          {
            statusCode: 200,
            body: response,
        }).as('getLevelsOverTimeData');

        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/metrics/numUsersPerSubjectPerLevelChartBuilder').as('getChartData');

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

        cy.visit('/administrator/projects/proj1/');
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

    it('subjects - num users per level - typical 6 subjects', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.intercept('/admin/projects/proj1/metrics/numUsersPerSubjectPerLevelChartBuilder',
          {
            statusCode: 200,
            body: [
                createSubjectObj('First Cool Subject', [23,293,493,625,293]),
                createSubjectObj('Awesome Subject', [1265,2352,493,625,293]),
                createSubjectObj('Other Subject', [1254,1000,852,625,293]),
                createSubjectObj('Where subjects no go', [856,293,493,625,293]),
                createSubjectObj('Short', [325,293,493,625,293]),
                createSubjectObj('Interesting Subject', [1568,859,493,625,293]),
            ],
        }).as('getChartData');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - many subjects', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        const response = [];
        for (let i=0; i < 15; i+=1) {
            response.push(createSubjectObj(`Subject # ${i}`, [1265,852,493,625,293]))
        }

        cy.intercept('/admin/projects/proj1/metrics/numUsersPerSubjectPerLevelChartBuilder',
          {
            statusCode: 200,
            body: response,
        }).as('getChartData');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - many levels', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        const response = [];
        for (let i=0; i < 6; i+=1) {
            response.push(createSubjectObj(`Subject # ${i}`, [1265,852,493,625,293,392,293,983,1923,1209]))
        }

        cy.intercept('/admin/projects/proj1/metrics/numUsersPerSubjectPerLevelChartBuilder',
          {
            statusCode: 200,
            body: response,
        }).as('getChartData');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.contains("Subject # 5")
        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - empty', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        const body = [];
        cy.intercept('/admin/projects/proj1/metrics/numUsersPerSubjectPerLevelChartBuilder',
          {
            statusCode: 200,
            body,
        }).as('getChartData');

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })


})
