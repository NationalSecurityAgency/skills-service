/*
 * Copyright 2024 SkillTree
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


describe('Dark Mode Accessibility Tests for Admin pages', () => {

  beforeEach(() => {
    cy.request('POST', '/app/userInfo/settings', [{
      'settingGroup': 'user.prefs',
      'value': true,
      'setting': 'enable_dark_mode',
      'lastLoadedValue': 'true',
      'dirty': true
    }]);

  });

  it('learning path', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill2`);
    cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill3`);

    cy.visit('/administrator/projects/proj1/learning-path')
    cy.injectAxe();
    cy.get('[data-cy="addLearningPathItemBtn"]')

    cy.customLighthouse();
    cy.customA11y();
  })

})

