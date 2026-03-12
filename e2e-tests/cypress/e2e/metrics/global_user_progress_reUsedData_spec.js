/*
 * Copyright 2026 SkillTree
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

describe('Global Users Progress With Reused Data', () => {

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        const createProj = (projNum) => {
            cy.createProject(projNum)
            cy.createSubject(projNum, 1)
            const numSkills = 5;
            for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
                cy.createSkill(projNum, 1, skillsCounter)
            }
        }
        createProj(1)
        createProj(2)
        createProj(3)

        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.createBadge(1, 2)
        cy.assignSkillToBadge(1, 2, 2)
        cy.enableBadge(1, 2)

        cy.createGlobalBadge(10)
        cy.assignSkillToGlobalBadge(10, 1, 1 )
        cy.enableGlobalBadge(10)

        const users = ['user1', 'user2']
        cy.reportSkill(1, 1, users[0])

        cy.reportSkill(1, 1, users[1], 'yesterday')
        cy.reportSkill(1, 1, users[1])

        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)
        cy.runQuizForUser(1, users[0], [{selectedIndex: [0]}], true)

        cy.createSurveyDef(2)
        cy.createSurveyMultipleChoiceQuestionDef(2, 1)

        cy.addUserTag([{
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC1']
        }]);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('user progress page', () => {
        cy.visit('/administrator/users-progress');
    });

});
