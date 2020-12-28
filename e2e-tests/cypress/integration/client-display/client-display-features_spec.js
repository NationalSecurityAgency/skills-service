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

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Features Tests', () => {
  const snapshotOptions = {
    blackout: ['[data-cy=pointHistoryChart]'],
    failureThreshold: 0.03, // threshold for entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
  };

  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true);
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'proj1'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1',
      helpUrl: 'http://doHelpOnThisSubject.com',
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    });

    Cypress.Commands.add("createSkill", (num) => {
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${num}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${num}`,
        name: `This is ${num}`,
        type: 'Skill',
        pointIncrement: 50,
        numPerformToCompletion: 2,
        pointIncrementInterval: 0,
        numMaxOccurrencesIncrementInterval: -1,
        description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        version: 0,
        helpUrl: 'http://doHelpOnThisSkill.com'
      });
    });
  })

  it('display new version banner when software is updated', () => {
    cy.intercept({
      method: '*',
      path: '/api/projects/proj1/subjects/subj1/summary',
    }, {
      // statusCode: 200,
      body: {
        'subject': 'Subject 1',
        'subjectId': 'subj1',
        'description': 'Description',
        'skillsLevel': 0,
        'totalLevels': 5,
        'points': 0,
        'totalPoints': 0,
        'todaysPoints': 0,
        'levelPoints': 0,
        'levelTotalPoints': 0,
        'skills': [],
        'iconClass': 'fa fa-question-circle',
        'helpUrl': 'http://doHelpOnThisSubject.com'
      },
      headers: {
        'skills-client-lib-version': dateFormatter(new Date()),
        'skills-client-lib-version-2': dateFormatter(new Date())
      },
    }).as('getSubjectSummary');
    cy.intercept('GET', '/api/projects/proj1/pointHistory').as('pointHistoryChart');

    cy.cdVisit('/');
    cy.injectAxe();
    cy.contains('Overall Points');
    cy.contains('New Skills Software Version is Available').should('not.exist')
    cy.cdClickSubj(0, 'Subject 1');

    cy.wait('@getSubjectSummary')
    cy.wait('@pointHistoryChart');

    cy.contains('New Skills Software Version is Available')

    cy.wait(500) //need to wait on the pointHistoryChart to complete rendering before running a11y
    cy.customA11y();

    cy.cdVisit('/');
    cy.contains('New Skills Software Version is Available').should('not.exist')
  });

  it('do not display new version banner if lib version in headers is older than lib version in local storage', () => {
    const mockedLibVersion = dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 5);
    cy.intercept({
      path: '/api/projects/proj1/subjects/subj1/summary',
      statusCode: 200,
    }, {
      body: {
        'subject': 'Subject 1',
        'subjectId': 'subj1',
        'description': 'Description',
        'skillsLevel': 0,
        'totalLevels': 5,
        'points': 0,
        'totalPoints': 0,
        'todaysPoints': 0,
        'levelPoints': 0,
        'levelTotalPoints': 0,
        'skills': [],
        'iconClass': 'fa fa-question-circle',
        'helpUrl': 'http://doHelpOnThisSubject.com'
      },
      headers: {
        'skills-client-lib-version': mockedLibVersion,
      },
    }).as('getSubjectSummary');

    cy.intercept({
      path: '/api/projects/proj1/subjects/subj1/rank',
    }, {
      statusCode: 200,
      body: {
        'numUsers': 1,
        'position': 1
      },
      headers: {
        'skills-client-lib-version': mockedLibVersion
      },
    }).as('getRank');

    cy.intercept({
      url: '/api/projects/proj1/subjects/subj1/pointHistory',
    }, {
      statusCode: 200,
      body: { 'pointsHistory': [] },
      headers: {
        'skills-client-lib-version': mockedLibVersion
      },
    }).as('getPointHistory');

    cy.cdVisit('/');
    cy.contains('Overall Points');
    cy.contains('New Skills Software Version is Available').should('not.exist')

    cy.cdClickSubj(0, 'Subject 1');
    cy.wait('@getSubjectSummary')
    cy.wait('@getRank')
    cy.wait('@getPointHistory')

    cy.contains('New Skills Software Version is Available').should('not.exist')
  });

  it('achieve level 5, then add new skill', () => {
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: `This is 1`,
      type: 'Skill',
      pointIncrement: 50,
      numPerformToCompletion: 2,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });

    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
    })
    cy.intercept('GET', '/api/projects/proj1/pointHistory').as('pointHistoryChart');

    cy.cdVisit('/');
    cy.injectAxe();

    cy.contains('Overall Points');

    cy.get('[data-cy=subjectTile]').eq(0).contains('Subject 1')
    cy.get('[data-cy=subjectTile]').eq(0).contains('Level 5')

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill2',
      name: `This is 2`,
      type: 'Skill',
      pointIncrement: 50,
      numPerformToCompletion: 2,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: -1,
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      version: 0,
      helpUrl: 'http://doHelpOnThisSkill.com'
    });

    cy.wait('@pointHistoryChart');
    cy.customA11y();
    cy.cdVisit('/');

    cy.contains('Overall Points');

    cy.get('[data-cy=subjectTile]').eq(0).contains('Subject 1')
    cy.get('[data-cy=subjectTile]').eq(0).contains('Level 5')
  });

  it('deps are added to partially achieved skill', () => {
    cy.createSkill(1);
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })
    cy.createSkill(2);
    cy.createSkill(3);
    cy.request('POST', `/admin/projects/proj1/skills/skill1/dependency/skill2`)
    cy.request('POST', `/admin/projects/proj1/skills/skill2/dependency/skill3`)

    cy.cdVisit('/');

    cy.cdClickSubj(0, 'Subject 1');

    cy.matchImageSnapshot(`Subject-WithLockedSkills-ThatWerePartiallyAchieved`, snapshotOptions);

    cy.cdClickSkill(0);
    cy.contains('This is 1');
    const expectedMsg = 'You were able to earn partial points before the dependencies were added';
    cy.contains(expectedMsg);
    // should render dependencies section
    cy.contains('Dependencies');

    cy.matchImageSnapshot(`LockedSkill-ThatWasPartiallyAchieved`, snapshotOptions);

    // make sure the other locked skill doesn't contain the same message
    cy.cdBack('Subject 1');
    cy.cdClickSkill(1);
    cy.contains('This is 2');
    cy.contains(expectedMsg).should('not.exist');

    // make sure the skill without deps doesn't have the message
    cy.cdBack('Subject 1');
    cy.cdClickSkill(2);
    cy.contains('This is 3');
    cy.contains(expectedMsg).should('not.exist');
  });

  it('deps are added to fully achieved skill', () => {
    cy.createSkill(1);
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 24
    })
    cy.createSkill(2);
    cy.request('POST', `/admin/projects/proj1/skills/skill1/dependency/skill2`)

    cy.cdVisit('/');
    cy.cdClickSubj(0, 'Subject 1');

    cy.matchImageSnapshot(`Subject-WithLockedSkills-ThatWereFullyAchieved`, snapshotOptions);

    cy.cdClickSkill(0);
    cy.contains('This is 1');
    const msg = "Congrats! You completed this skill before the dependencies were added";
    cy.contains(msg);

    cy.matchImageSnapshot(`LockedSkill-ThatWasFullyAchieved`, snapshotOptions);

    // other skill should not have the message
    cy.cdBack('Subject 1');
    cy.cdClickSkill(1);
    cy.contains('This is 2');
    cy.contains(msg).should('not.exist');

    // now let's achieve the dependent skill
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime()
    })
    cy.request('POST', `/api/projects/proj1/skills/skill2`, {
      userId: Cypress.env('proxyUser'),
      timestamp: new Date().getTime() - 1000 * 60 * 24
    })
    cy.cdBack('Subject 1');
    cy.cdClickSkill(0);
    cy.contains('This is 1');
    cy.contains(msg).should('not.exist');
  });

})
