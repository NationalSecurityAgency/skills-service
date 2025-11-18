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
var moment = require('moment-timezone')

describe('Client Display Point History Tests', () => {

  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true)
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'proj1'
    })
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1'
    })
  })

  it('multiple achievements in the middle', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 100
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-13T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-14T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-15T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-16T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-17T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-18T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-19T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-20T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-21T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-22T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-12T00:00:00.000+00:00',
        'points': 500,
        'name': 'Levels 1, 2, 3'
      }]
    }

    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')
    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.injectAxe()
    cy.wait(1000)
    cy.customA11y()

    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('point history with data from server', () => {
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: `This is 1`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    })

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill2',
      name: `This is 2`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      version: 0
    })
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill3',
      name: `This is 3`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 2,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      version: 0
    })

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill4`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill4',
      name: `This is 4`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 2,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      version: 0
    })

    const m = moment()
    const orig = m.clone()
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      timestamp: m.format('x')
    })
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      timestamp: m.subtract(4, 'day')
        .format('x')
    })
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      timestamp: m.subtract(3, 'day')
        .format('x')
    })
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      timestamp: m.subtract(2, 'day')
        .format('x')
    })
    cy.log(`proxy user ${Cypress.env('proxyUser')}`)
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      timestamp: m.subtract(1, 'day')
        .format('x')
    })

    cy.intercept('/api/projects/proj1/pointHistory')
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')

    cy.get('[data-cy="pointHistoryChart"]')
  })

  it('multiple achievements at the last date', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 100
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-12T00:00:00.000+00:00',
        'points': 500,
        'name': 'Levels 1, 2, 3'
      }]
    }

    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('multiple achievements on first date', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-02T00:00:00.000+00:00',
        'points': 400,
        'name': 'Levels 1, 2, 3'
      }]
    }

    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('single achievements on the first date', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 500
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-02T00:00:00.000+00:00',
        'points': 400,
        'name': 'Level 1'
      }]
    }

    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('single achievement on the last date', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 100
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-12T00:00:00.000+00:00',
        'points': 500,
        'name': 'Level 1'
      }]
    }

    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('achievements throughout time', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 100
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-02T00:00:00.000+00:00',
        'points': 100,
        'name': 'Level 1'
      }, {
        'achievedOn': '2020-09-05T00:00:00.000+00:00',
        'points': 300,
        'name': 'Level 2'
      }, {
        'achievedOn': '2020-09-08T00:00:00.000+00:00',
        'points': 400,
        'name': 'Level 3'
      }, {
        'achievedOn': '2020-09-11T00:00:00.000+00:00',
        'points': 400,
        'name': 'Level 4'
      }, {
        'achievedOn': '2020-09-12T00:00:00.000+00:00',
        'points': 500,
        'name': 'Level 5'
      }]
    }

    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.injectAxe()
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('levels achieved on subsequent days', () => {
    const data = {
      'pointsHistory': [{
        'dayPerformed': '2020-09-02T00:00:00.000+00:00',
        'points': 100
      }, {
        'dayPerformed': '2020-09-03T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-04T00:00:00.000+00:00',
        'points': 200
      }, {
        'dayPerformed': '2020-09-05T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-06T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-07T00:00:00.000+00:00',
        'points': 300
      }, {
        'dayPerformed': '2020-09-08T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-09T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-10T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-11T00:00:00.000+00:00',
        'points': 400
      }, {
        'dayPerformed': '2020-09-12T00:00:00.000+00:00',
        'points': 500
      }],
      'achievements': [{
        'achievedOn': '2020-09-02T00:00:00.000+00:00',
        'points': 100,
        'name': 'Level 1'
      }, {
        'achievedOn': '2020-09-05T00:00:00.000+00:00',
        'points': 300,
        'name': 'Level 2'
      }, {
        'achievedOn': '2020-09-06T00:00:00.000+00:00',
        'points': 300,
        'name': 'Level 3'
      }, {
        'achievedOn': '2020-09-11T00:00:00.000+00:00',
        'points': 400,
        'name': 'Level 4'
      }, {
        'achievedOn': '2020-09-12T00:00:00.000+00:00',
        'points': 500,
        'name': 'Level 5'
      }]
    }
    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  function createTimeline(start, numDays, startScore, increaseBy, increaseEvery, stopIncreasingAfterDays = -1) {
    const m = moment.utc(start, 'YYYY-MM-DD HH')
    const pointHistory = []
    let score = startScore
    for (let i = 0; i < numDays; i += 1) {
      if ((i % increaseEvery === 0) && (stopIncreasingAfterDays === -1 || stopIncreasingAfterDays > i)) {
        score += increaseBy
      }
      pointHistory.push({
        'dayPerformed': m.clone()
          .add(i, 'day')
          .tz('UTC')
          .format(),
        'points': score
      })
    }
    return pointHistory
  }

  it('levels achieved on subsequent days with many days in the timeline', () => {
    const pointHistory = createTimeline('2019-09-12', 120, 10, 10, 10)
    pointHistory.forEach((value) => {
      cy.log(value)
    })
    const data = {
      'pointsHistory': pointHistory,
      'achievements': [{
        'achievedOn': pointHistory[50].dayPerformed,
        'points': pointHistory[50].points,
        'name': 'Level 1'
      }, {
        'achievedOn': pointHistory[51].dayPerformed,
        'points': pointHistory[51].points,
        'name': 'Level 2'
      }]
    }
    cy.intercept('/api/projects/proj1/pointHistory', data)
      .as('getPointHistory')

    cy.cdVisit('/', true)
    cy.wait('@getPointHistory')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')

    cy.wait(4000)
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('empty point history', () => {
    cy.intercept('/api/projects/proj1/pointHistory')
      .as('getPointHistory')
    cy.cdVisit('/')
    cy.wait('@getPointHistory')
    cy.get('[data-cy="pointHistoryChartNoData"]').contains('Your Progress Awaits')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]', { errorThreshold: 0.1 })
  })


})

