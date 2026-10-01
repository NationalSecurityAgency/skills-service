/*
 * Copyright 2026 SkillTree
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

describe('Project learning path in skills display', () => {
  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true)
    cy.createProject(1)
    cy.createSubject(1, 1)
  })

  it('only shows a learning path summary when routes exist and counts achieved nodes once', () => {
    cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 4, { numPerformToCompletion: 1 })

    cy.cdVisit('/')
    cy.get('[data-cy="viewLearningPathLink"]').should('not.exist')

    cy.addLearningPathItem(1, 1, 2)
    cy.addLearningPathItem(1, 1, 3)
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/')
    cy.get('[data-cy="numAchievedLearningPathItems"]').should('have.text', '1')
    cy.get('[data-cy="numTotalLearningPathItems"]').should('have.text', '3')
    cy.get('[data-cy="numAchievedLearningPathItems"]').parent().should('contain.text', '1 of 3 items achieved')
    cy.get('[data-cy="learningPathPercent"]').should('have.text', '33%')
    cy.get('[data-cy="learningPathPercent"]').closest('[aria-hidden="true"]').should('exist')
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '1')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '4')
    cy.get('[data-cy="learningPathTitle"]').then(($learningPathTitle) => {
      cy.get('[data-cy="achievedSkillsTitle"]').should(($achievedSkillsTitle) => {
        expect(Math.abs($learningPathTitle[0].getBoundingClientRect().top - $achievedSkillsTitle[0].getBoundingClientRect().top)).to.be.lessThan(1)
      })
    })
    cy.get('[data-cy="viewLearningPathLink"]').should('be.visible').and('have.prop', 'tagName', 'BUTTON').and('have.attr', 'aria-label', 'View project learning path').click()
    cy.location('pathname').should('eq', '/test-skills-display/proj1/learning-path')
    cy.get('[data-cy="skillsTitle"]').contains('Learning Path')
    cy.get('[data-cy="fullDepsSkillsGraph"] [data-cy="graphLegend"]').should('not.exist')
    cy.get('[data-cy="fullDepsSkillsGraph"] [data-cy="learningPathProgressSummary"]').should('be.visible')
    cy.get('[data-cy="learningPathProgressSummary"]').then(($progress) => {
      cy.get('#additionalControls').should(($controls) => {
        expect(Math.abs($progress[0].getBoundingClientRect().top - $controls[0].getBoundingClientRect().top)).to.be.lessThan(1)
      })
    })
    cy.get('[data-cy="learningPathProgressCount"]').should('contain.text', '1 of 3 items achieved')
    cy.get('[data-cy="learningPathProgressPercent"]').should('have.text', '33%')
    cy.get('[data-cy="learningPathProgressBar"] [role="progressbar"]').should('have.attr', 'aria-label', '1 of 3 items achieved')
    cy.get('[data-cy="learningPathProgressBar"] [role="progressbar"]').should('have.attr', 'aria-valuenow', '33')
    cy.get('#dependency-graph canvas').should('be.visible')
    cy.get('[data-cy="learningPathTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')
    cy.get('[data-cy="learningPathTable"] [data-cy="sharedSkillsTable-removeBtn"]').should('not.exist')
    cy.get('[data-cy="learningPathTable"]').contains('View Route').should('not.exist')
  })

  it('reflects completed and incomplete learning path skills in the progress summary', () => {
    cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 4, { numPerformToCompletion: 1 })
    cy.addLearningPathItem(1, 1, 2)
    cy.addLearningPathItem(1, 2, 3)

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/')
    cy.get('[data-cy="numAchievedLearningPathItems"]').should('have.text', '2')
    cy.get('[data-cy="numTotalLearningPathItems"]').should('have.text', '3')
    cy.get('[data-cy="numAchievedLearningPathItems"]').parent().should('contain.text', '2 of 3 items achieved')
    cy.get('[data-cy="learningPathPercent"]').should('have.text', '67%')
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '3')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '4')

    cy.get('[data-cy="viewLearningPathLink"]').click()
    cy.get('[data-cy="learningPathProgressCount"]').should('contain.text', '2 of 3 items achieved')
    cy.get('[data-cy="learningPathProgressPercent"]').should('have.text', '67%')
    cy.get('[data-cy="learningPathProgressBar"] [role="progressbar"]').should('have.attr', 'aria-label', '2 of 3 items achieved')
    cy.get('[data-cy="learningPathProgressBar"] [role="progressbar"]').should('have.attr', 'aria-valuenow', '67')
    cy.get('#dependency-graph canvas').should('be.visible')
    cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill1"]').should('be.visible')
    cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill2"]').should('be.visible')
    cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_skill3"]').should('be.visible')
    cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_skill4"]').should('not.exist')
  })

  it('shows 100% completion on both the home and learning path pages', () => {
    cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
    cy.addLearningPathItem(1, 1, 2)
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/')
    cy.get('[data-cy="numAchievedLearningPathItems"]').should('have.text', '2')
    cy.get('[data-cy="numTotalLearningPathItems"]').should('have.text', '2')
    cy.get('[data-cy="learningPathPercent"]').should('have.text', '100%')
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', '2')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '2')

    cy.get('[data-cy="viewLearningPathLink"]').click()
    cy.get('[data-cy="learningPathProgressCount"]').should('contain.text', '2 of 2 items achieved')
    cy.get('[data-cy="learningPathProgressPercent"]').should('have.text', '100%')
    cy.get('[data-cy="learningPathProgressBar"] [role="progressbar"]').should('have.attr', 'aria-valuenow', '100')
  })

  it('shows the learning path in the embedded skills-client display', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.addLearningPathItem(1, 1, 2)

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="viewLearningPathLink"]').should('be.visible').click()
    cy.wrapIframe().find('[data-cy="skillsTitle"]').contains('Learning Path')
    cy.wrapIframe().find('[data-cy="learningPathProgressCount"]').should('contain.text', '0 of 2 items achieved')
    cy.wrapIframe().find('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill1"]').should('be.visible')
  })

  it('keeps the graph progress in sync with the versioned skills summary', () => {
    cy.createSkill(1, 1, 1, { version: 0, numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 2, { version: 0, numPerformToCompletion: 1 })
    cy.createSkill(1, 1, 3, { version: 1, numPerformToCompletion: 1 })
    cy.addLearningPathItem(1, 1, 2)
    cy.addLearningPathItem(1, 2, 3)
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')

    cy.intercept('GET', '/api/projects/proj1/summary*').as('versionedSummary')
    cy.intercept('GET', '/api/projects/proj1/dependency/graph*').as('versionedGraph')
    cy.visit('/test-skills-display/proj1?skillsVersion=0')
    cy.wait('@versionedSummary').then(({ request, response }) => {
      expect(request.url).to.include('version=0')
      expect(response.body.totalSkills).to.eq(2)
    })
    cy.wait('@versionedGraph').its('request.url').should('include', 'version=0')
    cy.get('[data-cy="numTotalSkills"]').should('have.text', '2')
    cy.get('[data-cy="numAchievedLearningPathItems"]').should('have.text', '1')
    cy.get('[data-cy="numTotalLearningPathItems"]').should('have.text', '2')
    cy.get('[data-cy="viewLearningPathLink"]').click()
    cy.get('[data-cy="learningPathProgressCount"]').should('have.text', '1 of 2 items achieved')
    cy.get('[data-cy="learningPathProgressPercent"]').should('have.text', '50%')
    cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_skill3"]').should('not.exist')
  })

  it('navigates from the learning path graph to skill, badge, and shared-skill details', () => {
    cy.viewport(1280, 1280)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.createBadge(1, 1, { enabled: true })
    cy.createProject(2)
    cy.createSubject(2, 1)
    cy.createSkill(2, 1, 3)
    cy.addLearningPathItem(1, 1, 2, true, false)
    cy.addCrossProjectLearningPathItem(2, 3, 1, 2)

    const clickGraphNode = (skillId, projectId) => {
      cy.cdVisit('/learning-path')
      cy.get('#dependency-graph canvas').should('be.visible')
      cy.get('#dependency-graph').should(($graph) => {
        const position = $graph[0].getNodePosition?.(projectId, skillId)
        expect(position, `${projectId}/${skillId} graph node position`).to.have.property('x').that.is.a('number')
        expect(position).to.have.property('y').that.is.a('number')
      }).then(($graph) => {
        const position = $graph[0].getNodePosition(projectId, skillId)
        cy.get('#dependency-graph canvas').click(position.x, position.y)
      })
    }

    clickGraphNode('skill2', 'proj1')
    cy.location('pathname').should('eq', '/test-skills-display/proj1/subjects/subj1/skills/skill2')
    cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 2')

    clickGraphNode('badge1', 'proj1')
    cy.location('pathname').should('eq', '/test-skills-display/proj1/badges/badge1')
    cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').should('contain.text', 'Badge 1')

    clickGraphNode('skill3', 'proj2')
    cy.location('pathname').should('eq', '/test-skills-display/proj1/subjects/subj1/skills/skill3/crossProject/proj2/skill3')
    cy.get('[data-cy="crossProjAlert"]').should('contain.text', 'This skill is shared from another project')
    cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 3')
  })

  it('uses the Skills Display theme for the learning path graph, progress, controls, and routes', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.addLearningPathItem(1, 1, 2)

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')

    cy.visit('/test-skills-display/proj1/?enableTheme=true')
    cy.get('[data-cy="skillsTitle"]').should('be.visible')
    cy.get('[data-cy="viewLearningPathLink"]').should('be.visible').click()
    cy.get('#dependency-graph canvas').should('be.visible')
    cy.get('[data-cy="learningPathProgressSummary"]').should('have.css', 'background-color', 'rgb(21, 46, 77)')
    cy.get('[data-cy="learningPathProgressCount"]').should('have.css', 'color', 'rgb(255, 255, 255)')
    cy.get('[data-cy="learningPathProgressPercent"]').should('have.css', 'color', 'rgb(255, 255, 255)')
    cy.get('[data-cy="learningPathProgressSummary"] .p-progressbar-value').should('have.css', 'background-color', 'rgb(89, 173, 82)')
    cy.get('[data-cy="learningPathProgressSummary"] .p-progressbar').should('have.css', 'background-color', 'rgb(205, 205, 205)')
    cy.get('#dependency-graph .vis-navigation').should('have.css', 'background-color', 'rgba(0, 0, 0, 0)')
    cy.get('#dependency-graph .vis-navigation .vis-button').first().should('have.css', 'color', 'rgb(204, 231, 243)')
    cy.get('#additionalControls .p-togglebutton').should('have.css', 'color', 'rgb(255, 255, 255)')
      .and('have.css', 'background-color', 'rgb(21, 46, 77)')
    cy.get('[data-cy="learningPathTotalRows"]').should('have.css', 'color', 'rgb(255, 255, 255)')
      .and('contain.text', 'Total Rows: 1')
    cy.get('[data-cy="learningPathTable"] .p-paginator').should('have.css', 'background-color', 'rgb(21, 46, 77)')
    cy.get('#additionalControls .p-togglebutton, [data-cy="learningPathTotalRows"]').each(($label) => {
      const element = $label[0]
      const foreground = getComputedStyle(element).color.match(/\d+/g).slice(0, 3).map(Number)
      const background = getComputedStyle($label.is('.p-togglebutton') ? element : element.closest('.p-paginator'))
        .backgroundColor.match(/\d+/g).slice(0, 3).map(Number)
      const luminance = (rgb) => rgb.map((value) => {
        const channel = value / 255
        return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4
      }).reduce((sum, channel, index) => sum + channel * [0.2126, 0.7152, 0.0722][index], 0)
      const light = Math.max(luminance(foreground), luminance(background))
      const dark = Math.min(luminance(foreground), luminance(background))
      expect((light + 0.05) / (dark + 0.05), `${$label.text().trim()} contrast`).to.be.at.least(4.5)
    })
    cy.get('[data-cy="learningPathTable"] thead th').first().should('have.css', 'color', 'rgb(255, 255, 255)')
    cy.get('[data-cy="learningPathTable"] thead th').first().should('have.css', 'background-color', 'rgb(21, 46, 77)')
    cy.contains('[data-cy="card-header"]', 'Learning Path Routes').should('have.css', 'color', 'rgb(255, 255, 255)')
      .and('have.css', 'background-color', 'rgb(21, 46, 77)')
  })

  it('keeps the graph within its card while zooming and dragging', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.addLearningPathItem(1, 1, 2)

    cy.cdVisit('/learning-path')
    cy.get('#dependency-graph canvas').should('be.visible')
    cy.get('#dependency-graph').should(($graph) => {
      const graph = $graph[0].getBoundingClientRect()
      const card = $graph.closest('[data-cy="fullDepsSkillsGraph"]')[0].getBoundingClientRect()
      expect(graph.bottom).to.be.at.most(card.bottom)
    })

    cy.get('#dependency-graph canvas').trigger('wheel', { deltaY: -600, bubbles: true, cancelable: true })
    cy.get('#dependency-graph canvas').trigger('mousedown', { clientX: 450, clientY: 300, button: 0 })
      .trigger('mousemove', { clientX: 450, clientY: 650, buttons: 1 })
      .trigger('mouseup', { clientX: 450, clientY: 650 })

    cy.get('#dependency-graph').should(($graph) => {
      const graph = $graph[0].getBoundingClientRect()
      const card = $graph.closest('[data-cy="fullDepsSkillsGraph"]')[0].getBoundingClientRect()
      expect(graph.bottom).to.be.at.most(card.bottom)
      expect($graph.css('overflow')).to.eq('hidden')
    })
    cy.get('[data-cy="learningPathTable"]').should('be.visible')
  })

  it('hides focus settings in both graph modes', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.addLearningPathItem(1, 1, 2)

    cy.cdVisit('/learning-path')
    cy.get('#dependency-graph canvas').should('be.visible')
    cy.get('[data-cy="learningPathSettingsMenu"]').click()
    cy.contains('Focus On Select').should('not.exist')
    cy.contains('Smooth Focus').should('not.exist')
    cy.contains('Dynamic Height').should('be.visible')

    cy.get('[data-cy="learningPath-fullScreenButton"]').realClick()
    cy.get('#fullDepsSkillsGraphContainer').should('match', ':fullscreen')
    cy.get('#additionalControls').should('be.visible').within(() => {
      cy.contains('Focus On Select').should('not.exist')
      cy.contains('Smooth Focus').should('not.exist')
    })
  })

  it('shows an empty learning path on a direct visit', () => {
    cy.cdVisit('/learning-path')
    cy.get('[data-cy="skillsTitle"]').contains('Learning Path')
    cy.get('[data-cy="fullDepsSkillsGraph"] [data-cy="learningPathProgressSummary"]').should('not.exist')
    cy.get('[data-cy="fullDepsSkillsGraph"]').contains('No Learning Path Yet')
    cy.get('[data-cy="learningPathTable"]').should('not.exist')
  })

  it('navigates to grouped skills and badges from the learning path table', () => {
    cy.createSkillsGroup(1, 1, 10)
    cy.addSkillToGroup(1, 1, 10, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 3)
    cy.createBadge(1, 1, { enabled: true })
    cy.addLearningPathItem(1, 1, 1, false, true)
    cy.addLearningPathItem(1, 1, 2, true, false)

    cy.cdVisit('/learning-path')
    cy.get('[data-cy="graphLegend"]').should('be.visible')
    cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill1"]')
      .should('have.attr', 'href', '/test-skills-display/proj1/subjects/subj1/groups/group10/skills/skill1')
      .click()
    cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')

    cy.cdVisit('/learning-path')
    cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_badge1"]')
      .should('have.attr', 'href', '/test-skills-display/proj1/badges/badge1')
      .click()
    cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').contains('Badge 1')
  })

  it('opens cross-project skills from the learning path table', () => {
    cy.createSkill(1, 1, 1)
    cy.createProject(2)
    cy.createSubject(2, 1)
    cy.createSkill(2, 1, 2)
    cy.addCrossProjectLearningPathItem(2, 2, 1, 1)

    cy.cdVisit('/learning-path')
    cy.get('[data-cy="graphLegend"]').should('not.exist')
    cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill2"]')
      .should('have.attr', 'href', '/test-skills-display/proj1/subjects/subj1/skills/skill2/crossProject/proj2/skill2')
      .click()
    cy.get('[data-cy="crossProjAlert"]').contains('This skill is shared from another project')
    cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
  })

})
