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

describe('Global/Overall Metrics', () => {

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

        cy.reportSkill(1, 1, 'user1@skills.org')

        cy.reportSkill(1, 1, 'user2@skills.org', 'yesterday')
        cy.reportSkill(1, 1, 'user2@skills.org')

    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('user progress page', () => {
        cy.visit('/administrator/overall-metrics');
    });

});
