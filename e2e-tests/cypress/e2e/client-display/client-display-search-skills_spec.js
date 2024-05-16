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
import moment from 'moment-timezone';


describe('Client Display Search Skills Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { pointIncrement: 11, numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 3, { pointIncrement: 22, numPerformToCompletion: 1 });

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 1, { pointIncrement: 33, numPerformToCompletion: 3 });
        cy.createSkill(1, 2, 2, { pointIncrement: 44, numPerformToCompletion: 1 });
        cy.createSkill(1, 2, 3, { pointIncrement: 55, numPerformToCompletion: 1 });

        cy.createSubject(1, 3);
        cy.createSkill(1, 3, 1, { pointIncrement: 66, numPerformToCompletion: 1 });
        cy.createSkill(1, 3, 2, { pointIncrement: 77, numPerformToCompletion: 1 });

        cy.doReportSkill({ project: 1, skill: 1, subjNum: 2 })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 3 })
    });

    it('search across skills', () => {
        cy.cdVisit('/');
        cy.get('[data-cy="searchSkillsAcrossSubjects"]').click()
        cy.get('[data-cy="searchRes-skill1"]')
        cy.get('[data-cy="searchRes-skill1"] [data-cy="subjectName"]').contains('Subject 1')
        cy.get('[data-cy="searchRes-skill1"] [data-cy="skillName"]').contains('Very Great Skill 1')
        cy.get('[data-cy="searchRes-skill1"] [data-cy="points"]').contains('0 / 200')
        cy.get('[data-cy="searchRes-skill1Subj2"]')
        cy.get('[data-cy="searchRes-skill1Subj2"] [data-cy="subjectName"]').contains('Subject 2')
        cy.get('[data-cy="searchRes-skill1Subj2"] [data-cy="skillName"]').contains('Very Great Skill 1 Subj2')
        cy.get('[data-cy="searchRes-skill1Subj2"] [data-cy="points"]').contains('33 / 99')
        cy.get('[data-cy="searchRes-skill1Subj3"]')
        cy.get('[data-cy="searchRes-skill1Subj3"] [data-cy="subjectName"]').contains('Subject 3')
        cy.get('[data-cy="searchRes-skill1Subj3"] [data-cy="skillName"]').contains('Very Great Skill 1 Subj3')
        cy.get('[data-cy="searchRes-skill1Subj3"] [data-cy="points"]').contains('66 / 66')
        cy.get('[data-cy="searchRes-skill2"]')
        cy.get('[data-cy="searchRes-skill2"] [data-cy="subjectName"]').contains('Subject 1')
        cy.get('[data-cy="searchRes-skill2"] [data-cy="skillName"]').contains('Very Great Skill 2')
        cy.get('[data-cy="searchRes-skill2"] [data-cy="points"]').contains('0 / 11')
        cy.get('[data-cy="searchRes-skill2Subj2"]')
        cy.get('[data-cy="searchRes-skill2Subj2"] [data-cy="subjectName"]').contains('Subject 2')
        cy.get('[data-cy="searchRes-skill2Subj2"] [data-cy="skillName"]').contains('Very Great Skill 2 Subj2')
        cy.get('[data-cy="searchRes-skill2Subj2"] [data-cy="points"]').contains('0 / 44')
        cy.get('[data-pc-section="panel"] [data-cy="skillName"]').should('have.length', 5)

        cy.get('[data-cy="searchSkillsAcrossSubjects"]').type('subj3')
        cy.get('[data-cy="searchRes-skill1Subj3"]')
        cy.get('[data-cy="searchRes-skill2Subj3"]')
        cy.get('[data-pc-section="panel"] [data-cy="skillName"]').should('have.length', 2)
    });

    it('navigate to the skill', () => {
        cy.cdVisit('/');
        cy.get('[data-cy="searchSkillsAcrossSubjects"]').click()
        cy.get('[data-cy="searchRes-skill1Subj2"]').click()
        cy.get('[data-cy="skillsTitle"]').contains('Skill Overview')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1 Subj2')
    });

    it('search results in default theme', () => {
        cy.cdVisit('/');
        cy.get('[data-cy="searchSkillsAcrossSubjects"]')
            .click()
        cy.wait(5000)
        cy.matchSnapshotImageForElement('[data-pc-section="panel"]')
    });

    it('search results in configured theme', () => {
        cy.cdVisit('/?enableTheme=true');
        cy.get('[data-cy="searchSkillsAcrossSubjects"]')
            .click()
        cy.wait(5000)
        cy.matchSnapshotImageForElement('[data-pc-section="panel"]')
    });
});
