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

    before(() => {
        cy.disableUILogin();
    });

    after(function () {
        cy.enableUILogin();
    });

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
    })

    it('display new version banner when software is updated', () => {
        cy.server().route({
            url: '/api/projects/proj1/subjects/subj1/summary',
            status: 200,
            response: {
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
                'skills-client-lib-version': dateFormatter(new Date())
            },
        }).as('getSubjectSummary');
        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.contains('New Skills Software Version is Available').should('not.exist')
        cy.cdClickSubj(0, 'Subject 1');
        cy.wait('@getSubjectSummary')

        cy.contains('New Skills Software Version is Available')

        cy.cdVisit('/');
        cy.contains('New Skills Software Version is Available').should('not.exist')
    });

    it('do not display new version banner if lib version in headers is older than lib version in local storage', () => {
        cy.server().route({
            url: '/api/projects/proj1/subjects/subj1/summary',
            status: 200,
            response: {
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
                'skills-client-lib-version': dateFormatter(new Date() - 1000 * 60 * 60 * 24)
            },
        }).as('getSubjectSummary');

        cy.server().route({
            url: '/api/projects/proj1/subjects/subj1/rank',
            status: 200,
            response: {
                'numUsers': 1,
                'position': 1
            },
            headers: {
                'skills-client-lib-version': dateFormatter(new Date() - 1000 * 60 * 60 * 24)
            },
        }).as('getRank');

        cy.server().route({
            url: '/api/projects/proj1/subjects/subj1/pointHistory',
            status: 200,
            response: { 'pointsHistory': [] },
            headers: {
                'skills-client-lib-version': dateFormatter(new Date() - 1000 * 60 * 60 * 24)
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

})
