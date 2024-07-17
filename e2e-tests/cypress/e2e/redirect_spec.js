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
describe('Redirection Tests', () => {

    beforeEach(() => {
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })

        cy.logout();
        const supervisorUser = 'supervisor@skills.org';
        cy.register(supervisorUser, 'password');
        cy.login('root@skills.org', 'password');
        cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
        cy.logout();
        cy.login(supervisorUser, 'password');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });
    });

    it('Old skills client display route is redirected from a subject', () => {
        cy.visit('/progress-and-rankings/projects/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1');

        cy.url().should('include', '/redirect')
        cy.get('[data-cy=redirectExplanation]')
            .should('be.visible')
            .contains('You seem to have followed an old link. You will be redirected to /progress-and-rankings/projects/proj1/subjects/subj1 shortly.');
    });

    it('Old skills client display route is redirected from a skill', () => {
        cy.visit('/progress-and-rankings/projects/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');

        cy.url().should('include', '/redirect')
        cy.get('[data-cy=redirectExplanation]')
            .should('be.visible')
            .contains('You seem to have followed an old link. You will be redirected to /progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1 shortly.');
    });

});
