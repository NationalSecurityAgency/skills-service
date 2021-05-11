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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('User Tests', () => {
  beforeEach(() => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'proj1'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1'
    });

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/greatSkill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: `greatSkill1`,
      name: `Very Great Skill #1`,
      pointIncrement: '150',
      numPerformToCompletion: 1,
    });

    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/mediocreSkill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: `mediocreSkill1`,
      name: `Mediocre Skill #1`,
      pointIncrement: '150',
      numPerformToCompletion: 1,
    });

  });

  it('skill id filter applied on enter', () => {
    cy.intercept('/admin/projects/proj1/performedSkills/user1**').as('loadUserSkills');
    cy.reportSkill('proj1', 'greatSkill1', 'user1', 'now', false);
    cy.reportSkill('proj1', 'mediocreSkill1', 'user1', 'now', false);
    cy.visit('/administrator/projects/proj1/users/user1/skillEvents');
    cy.wait('@loadUserSkills');

    cy.get('.skills-b-table tbody tr').should('have.length', 2);
    cy.get('[data-cy=performedSkills-skillIdFilter]').type('mediocre{enter}');
    cy.wait('@loadUserSkills');
    cy.get('.skills-b-table tbody tr').should('have.length', 2);
  });


});



/*
https://ip-10-113-80-245.evoforge.org/administrator/projects/movies/users/user8/skillEvents
 */
