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
const { rmdirSync } = require('fs');

describe('Navigation in skills-client tests', () => {

  beforeEach(() => {
  })

  it('only display skills up-to the provided version', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1,1 )
    cy.createSkill(1, 1,2, {version: 1 } )
    cy.createSkill(1, 1,3, {version: 2 } )

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '600');
    cy.wrapIframe().find('[data-cy="pointsTillNextLevel"]').should('have.text', '60')
    cy.wrapIframe().find('[data-cy="numTotalSkills"]').should('have.text', '3')
    cy.wrapIframe().find('[data-cy="pointsProgress"]').should('have.text', '0 / 600')
    cy.wrapIframe().find('[data-cy="subjectTile-subj1"] [data-cy="levelProgress"]').should('have.text', '0 / 60')

    cy.visit('/test-skills-client/proj1?skillsVersion=1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '400');
    cy.wrapIframe().find('[data-cy="pointsProgress"]').should('have.text', '0 / 400')

    cy.visit('/test-skills-client/proj1?skillsVersion=0')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wrapIframe().find('[data-cy="totalPoints"]').should('have.text', '200');
    cy.wrapIframe().find('[data-cy="pointsProgress"]').should('have.text', '0 / 200')
  })

  it('report page visits when enablePageVisitReporting=true', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.enablePageVisitReporting = true;
        res.send(conf);
      });
    }).as('loadConfig')

    cy.intercept('PUT', '/api/pageVisit').as('pageVisit')

    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1,1 )
    cy.createSkill(1, 1,2, {version: 1 } )
    cy.createSkill(1, 1,3, {version: 2 } )

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wait('@loadConfig')
    let enterCount = 0
    let numSkillsClientVisits = 0
    const expectPageVisit = () => {
      return cy.wait('@pageVisit').its('request.body').then((body) => {
        enterCount=enterCount+1
        if (body.skillDisplay === true) {
          numSkillsClientVisits++
          expect(body.projectId).to.equal('proj1')
          expect(body.path).to.equal('/')
        }
      })
    }
    Promise.all([expectPageVisit(), expectPageVisit(), expectPageVisit()])
      .then(() => {
        expect(enterCount).to.equal(3)
        expect(numSkillsClientVisits).to.equal(1)
      })


    cy.wrapIframe().find('[data-cy="subjectTileBtn"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wait('@pageVisit').its('request.body').then((body) => {
      expect(body.skillDisplay).to.equal(true)
      expect(body.projectId).to.equal('proj1')
      expect(body.path).to.equal('/subjects/subj1')
    })

    cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Skill Overview')
    cy.wrapIframe().find('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardTitle"]').contains('100 Limit')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    cy.wait('@pageVisit').its('request.body').then((body) => {
      expect(body.skillDisplay).to.equal(true)
      expect(body.projectId).to.equal('proj1')
      expect(body.path).to.equal('/subjects/subj1/skills/skill1')
    })
  })

})