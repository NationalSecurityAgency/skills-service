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
describe('Resource Not Found Tests', () => {

    beforeEach(() => {
      cy.logout();
      const supervisorUser = 'supervisor@skills.org';
      cy.register(supervisorUser, 'password');
      cy.login('root@skills.org', 'password');
      cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
      cy.logout();
      cy.login(supervisorUser, 'password');

      cy.request('POST', '/app/projects/proj1', {
          projectId: 'proj1',
          name: "proj1"
      });
      cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
          projectId: 'proj1',
          subjectId: 'subj1',
          name: "Subject 1"
      });
    });

    it('invalid subject results in not found page', () => {
      cy.intercept('GET', '/api/myProgressSummary').as('loadProgress');
      cy.visit('/administrator/projects/proj1/subjects/fooo');

      cy.get('[data-cy=notFoundExplanation]').should('be.visible').should('have.text', ' Subject [fooo] doesn\'t exist in project [proj1] ');
      cy.get('[data-cy=notFoundExplanation]');
      cy.get('[data-cy=takeMeHome]').click();
      cy.wait('@loadProgress');
      cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
    });

    it('invalid skill results in not found page', () => {
      cy.intercept('GET', '/api/myProgressSummary').as('loadProgress');
      cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skillFooo');

      cy.get('[data-cy=notFoundExplanation]').should('be.visible').should('have.text', ' Skill [skillFooo] doesn\'t exist. ');
      cy.get('[data-cy=notFoundExplanation]');
      cy.get('[data-cy=takeMeHome]').click();
      cy.wait('@loadProgress');
      cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
    });

    it('redirects from old /projects link to /administrator/', ()=> {
      cy.intercept('GET', '/api/myProgressSummary').as('loadProgress');
      cy.intercept('GET', '/app/projects').as('loadProjects');
      cy.intercept('GET', '/admin/projects/proj1').as('loadProject');
      cy.visit('/projects');
      cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
      cy.get('[data-cy=newLink]').should('be.visible');
      cy.wait(11*1000); //wait for redirect timeout
      cy.wait('@loadProjects');
      cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');

      cy.visit('/projects/');
      cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
      cy.get('[data-cy=newLink]').click();
      cy.wait('@loadProjects');
      cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');

      cy.visit('/projects/');
      cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
      cy.get('[data-cy=takeMeHome]').click();
      cy.wait('@loadProgress');
      cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

      cy.visit('/projects/proj1');
      cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
      cy.get('[data-cy=newLink]').click();
      cy.wait('@loadProject');
      cy.get('[data-cy=breadcrumb-proj1]').should('be.visible');
    });

  it('redirects from old subjects link to /administrator/', ()=> {
    cy.intercept('GET', '/api/myProgressSummary').as('loadProgress');
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills').as('loadSkills');
    cy.visit('/projects/proj1/subjects/subj1');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').should('be.visible');
    cy.wait(11*1000); //wait for redirect timeout
    cy.wait('@loadSkills');
    cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');
    cy.get('[data-cy=breadcrumb-subj1]').should('be.visible');

    cy.visit('/projects/proj1/subjects/subj1');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').click();
    cy.wait('@loadSkills');
    cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');
    cy.get('[data-cy=breadcrumb-subj1]').should('be.visible');

    cy.visit('/projects/proj1/subjects/subjNope');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').click(); //should try to direct to new /administrator path and then return a not-found as the subj doesn't actually exist
    cy.get('[data-cy=notFoundExplanation]').should('be.visible');
    cy.get('[data-cy=oldLinkRedirect]').should('not.exist');

    cy.visit('/projects/proj1/subjects/subj1');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=takeMeHome]').click();
    cy.wait('@loadProgress');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
  });

  it('redirects from old skills link to /administrator/', ()=> {
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: `skill1`,
      name: `Very Great Skill # 1`,
      pointIncrement: '1500',
      numPerformToCompletion: '10',
    });

    cy.intercept('GET', '/api/myProgressSummary').as('loadProgress');
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('loadSkill');
    cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').should('be.visible');
    cy.wait(11*1000); //wait for redirect timeout
    cy.wait('@loadSkill');
    cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');
    cy.get('[data-cy=breadcrumb-skill1]').should('be.visible');

    cy.visit('/projects/proj1/subjects/subj1/skills/skill1/');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').click();
    cy.wait('@loadSkill');
    cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');
    cy.get('[data-cy=breadcrumb-skill1]').should('be.visible');

    cy.visit('/projects/proj1/subjects/subj1/skills/skillNope/');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').click();
    cy.wait('@loadSkill');
    cy.get('[data-cy=notFoundExplanation]').should('be.visible');
    cy.get('[data-cy=oldLinkRedirect]').should('not.exist');

    cy.visit('/projects/proj1/subjects/subj1/skills/skill1/');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=takeMeHome]').click();
    cy.wait('@loadProgress');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
  });

  it('redirects from old global badges link to /administrator/', ()=> {
    cy.intercept('GET', '/api/myProgressSummary').as('loadProgress');
    cy.intercept('GET', '/supervisor/badges').as('loadBadges');
    cy.intercept('GET', '/supervisor/badges/asdfBadge').as('loadBadge');
    cy.visit('/globalBadges');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').should('be.visible');
    cy.wait(11*1000); //wait for redirect timeout
    cy.wait('@loadBadges');
    cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');
    cy.get('[data-cy=breadcrumb-GlobalBadges]').should('be.visible');

    cy.visit('/globalBadges');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').click();
    cy.wait('@loadBadges');
    cy.get('[data-cy=breadcrumb-Projects]').should('be.visible');
    cy.get('[data-cy=breadcrumb-GlobalBadges]').should('be.visible');

    cy.visit('/globalBadges/asdfBadge/');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=newLink]').click();
    cy.wait('@loadBadge');
    cy.get('[data-cy=notFoundExplanation]').should('be.visible');
    cy.get('[data-cy=oldLinkRedirect]').should('not.exist');

    cy.visit('/globalBadges');
    cy.get('[data-cy=oldLinkRedirect]').should('be.visible');
    cy.get('[data-cy=takeMeHome]').click();
    cy.wait('@loadProgress');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
  });


});
