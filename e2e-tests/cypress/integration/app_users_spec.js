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
describe('App Users Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        const createUser = (length) => {
            var result           = '';
            var characters       = 'abcdefghijklmnopqrstuvwxyz';
            var charactersLength = characters.length;
            for ( var i = 0; i < length; i++ ) {
                result += characters.charAt(Math.floor(Math.random() * charactersLength));
            }
            return result;
        }

        for (let i = 0; i < 2000; i++) {
            const randomUser = createUser(8); Math.random().toString(36).substring(7);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: randomUser, timestamp: new Date().getTime()})
            // cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: `user${i}`, timestamp: new Date().getTime() - 1000*60*60*24})
        }
    });

    it('filter user table by username', () => {
        cy.visit("/projects/proj1/users")
        // cy.server().route('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill');
        // cy.server().route('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill');
        //
        // const selectorOccurrencesToCompletion = '[data-vv-name="numPerformToCompletion"]';
        // const selectorSkillsRowToggle = 'table .VueTables__child-row-toggler';
        // cy.visit('/projects/proj1/subjects/subj1');
        // cy.clickButton('Skill')
        // cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        // cy.get('#skillName').type('Skill 1')
        //
        // cy.clickSave()
        // cy.wait('@postNewSkill');
        //
        //
        // cy.get(selectorSkillsRowToggle).click()
        // cy.contains('50 Points')
        //
        // cy.get('table .control-column .fa-edit').click()
        // cy.wait('@getSkill')
        //
        // // close toast
        // cy.get('.toast-header button').click({ multiple: true })
        // cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        // cy.get(selectorOccurrencesToCompletion).type('{backspace}10')
        // cy.get(selectorOccurrencesToCompletion).should('have.value', '10')
        //
        // cy.clickSave()
        // cy.wait('@postNewSkill');
        //
        // cy.get(selectorSkillsRowToggle).click()
        // cy.contains('100 Points')
    });



})
