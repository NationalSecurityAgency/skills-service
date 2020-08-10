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
describe('Metrics Specs', () => {
  beforeEach(() => {
    cy.server()
      .route('GET', '/metrics/global').as('getMetrics')
      .route('GET', '/app/userInfo').as('getUserInfo')
  });

  it('global metrics page loads', function () {
    cy.visit('/metrics');
    cy.contains('No Metrics Yet').should('be.visible');
  });

});
