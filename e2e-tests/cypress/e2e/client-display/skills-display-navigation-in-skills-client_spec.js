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

  it('browser back and forward operations', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1,1 )
    cy.createSkill(1, 1,2 )

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')

    cy.wrapIframe().find('[data-cy="subjectTileBtn"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Skill Overview')
    cy.wrapIframe().find('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardTitle"]').contains('100 Limit')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.wrapIframe().find('[data-cy="nextSkill"]').click()
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')

    cy.go('back')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.go('back')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.go('back')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
  })

  it.only('browser back after reload', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1,1 )
    cy.createSkill(1, 1,2 )

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')

    cy.wrapIframe().find('[data-cy="subjectTileBtn"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Skill Overview')
    cy.wrapIframe().find('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardTitle"]').contains('100 Limit')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.wrapIframe().find('[data-cy="nextSkill"]').click()
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')

    cy.reload();
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Skill Overview')
    cy.wrapIframe().find('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardTitle"]').contains('100 Limit')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')

    cy.go('back')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.go('back')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.go('back')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
  })

  it('breadcrumb based navigation', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1,1 )
    cy.createSkill(1, 1,2 )

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')

    cy.wrapIframe().find('[data-cy="subjectTileBtn"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Skill Overview')
    cy.wrapIframe().find('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardTitle"]').contains('100 Limit')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.wrapIframe().find('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumbLink-subj1"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.wrapIframe().find('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumbLink-Overview"]').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
  })

  it('deep link into various pages', () => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1,1 )
    cy.createSkill(1, 1,2 )

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('User Skills')

    cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Subject 1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartNoData"]')
    cy.wrapIframe().find('[data-cy="myRankPosition"]')

    cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Skill Overview')
    cy.wrapIframe().find('[data-cy="timeWindowPts"] [data-cy="mediaInfoCardTitle"]').contains('100 Limit')
    cy.wrapIframe().find('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Frank')
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('My Rank')
  })

})