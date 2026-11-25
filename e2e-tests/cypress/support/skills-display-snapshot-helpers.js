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

export const visitSkillsDisplaySnapshotPage = (path = '/', expectPointHistoryData = false) => {
  cy.cdVisit(`${path}${path.includes('?') ? '&' : '?'}disableChartAnimations=true`)
  if (expectPointHistoryData) {
    cy.get('[data-cy="pointHistoryChartWithData"]').should('be.visible')
  }
}

const assertSnapshotContentReady = (root) => {
  // cy.get() yields jQuery collections, while cy.wrapIframe() can yield a raw body element.
  const $root = Cypress.$(root)
  const findIncludingRoot = selector => $root.find(selector).addBack(selector)
  expect(findIncludingRoot('[data-pc-name="progressspinner"]'), 'loading spinners').to.have.length(0)

  const loadingMasks = findIncludingRoot('[data-pc-section="mask"]')
    .add($root.parents('[data-pc-name="blockui"]').children('[data-pc-section="mask"]'))
    .filter((index, mask) => !mask.closest('[data-cy="pointHistoryChartNoData"]'))
    // Graded quiz answers keep a transparent mask behind their feedback icons, not a loading state.
    // Only inspect this mask's sibling overlay so enclosing loading masks are still checked.
    .filter((index, mask) => Cypress.$(mask).siblings('.overlay-content')
      .find('[data-cy="wrongSelection"], [data-cy="missedSelection"]').length === 0)
  expect(loadingMasks, 'captured loading masks').to.have.length(0)

  // Subject cards also use levelProgress for text; only CircleProgress wrappers contain radial charts.
  findIncludingRoot('.progress-circle-wrapper[data-cy="overallPoints"], .progress-circle-wrapper[data-cy="levelProgress"]').each((index, circle) => {
    expect(circle.querySelector('[data-cy="radialChart-rendered"]'), 'circle chart final draw').not.to.be.null
  })
  findIncludingRoot('[data-cy="pointHistoryChart"]').each((index, chart) => {
    expect(chart.querySelector('[data-cy="pointHistoryChart-rendered"]'), 'point history final draw').not.to.be.null
  })
  findIncludingRoot('[data-cy="levelBreakdownChart"]').each((index, chart) => {
    expect(chart.querySelector('[data-cy="levelBreakdownChart-rendered"]'), 'level breakdown final draw').not.to.be.null
  })
  findIncludingRoot('[data-cy="prerequisitesGraph"]').each((index, graph) => {
    expect(graph.getAttribute('data-render-ready'), 'prerequisites graph final draw').to.equal('true')
  })
}

export const matchSkillsDisplaySnapshot = (selector, nameOrOptions = {}) => {
  const options = typeof nameOrOptions === 'string' ? { name: nameOrOptions } : nameOrOptions
  cy.get('[data-pc-name="progressspinner"]').should('not.exist')
  cy.get(selector).should('be.visible').should(assertSnapshotContentReady)
  return cy.matchSnapshotImageForElement(selector, options)
}

export const waitForPrerequisitesGraph = () => {
  cy.get('[data-cy="prerequisitesGraph"]')
    .should('have.attr', 'data-render-ready', 'true')
  return cy.get('[data-cy="prerequisitesGraph"] canvas').should('be.visible')
}

export const clickPrerequisiteNode = (projectId, skillId) => {
  waitForPrerequisitesGraph()
  cy.get('[data-cy="prerequisitesGraph"]').should($graph => {
    const position = $graph[0].getNodePosition?.(projectId, skillId)
    expect(position, `rendered position for ${projectId}/${skillId}`).to.exist
  }).then($graph => {
    const position = $graph[0].getNodePosition(projectId, skillId)
    return cy.wrap($graph).find('canvas').click(position.x, position.y)
  })
}

export const waitForIframeSnapshot = () => {
  cy.wrapIframe().should(assertSnapshotContentReady)
  cy.wrapIframe().then(body => Cypress.$(body)[0].ownerDocument.fonts.ready)
  // Wait for the SDK's asynchronous height update rather than a fixed post-navigation delay.
  return cy.get('body iframe').should($iframe => {
    const frame = $iframe[0]
    // Match the content box observed by UseIframeInit, including its configured min-height.
    const display = frame.contentDocument.querySelector('#skilltree-main-container')
    expect(display, 'embedded display root').not.to.be.null
    const expectedHeight = Cypress.$(display).height() + 10
    expect(frame.getBoundingClientRect().height, 'iframe height').to.be.closeTo(expectedHeight, 1)
  })
}
