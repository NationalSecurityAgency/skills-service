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
describe('Metrics Tests', () => {
    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5',
        });
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user2', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0', timestamp: new Date().getTime() - 1000*60*60*24})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0', timestamp: new Date().getTime() - 1000*60*60*24 *2})
    });

    it('markdown features', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');

    })
})
