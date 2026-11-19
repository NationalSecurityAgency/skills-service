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

export const visitMetricsSnapshotPage = (path) => cy.visit(path, {
  onBeforeLoad(win) {
    win.__skillsMetricsChartSnapshot = true
  },
})

export const matchMetricsSnapshot = (selector, { noDataMessage, ...options } = {}) => {
  cy.get(selector).should('be.visible').should($root => {
    // A chart-only capture still needs its enclosing metrics overlay to finish loading.
    const overlays = $root.find('[data-metrics-loading]').addBack('[data-metrics-loading]')
      .add($root.parents('[data-metrics-loading]'))
    expect(overlays.length, 'metrics overlays').to.be.greaterThan(0)
    overlays.each((index, overlay) => {
      expect(overlay.getAttribute('data-metrics-loading'), 'metrics data loaded').to.equal('false')
      expect(overlay.getAttribute('data-metrics-has-data'), 'expected chart data state').to.equal(noDataMessage ? 'false' : 'true')
      if (!noDataMessage) {
        expect(overlay.querySelector('[data-pc-section="mask"]'), 'data chart loading mask').to.be.null
      } else {
        // Keep the intentional no-data / not-generated overlay in the snapshot.
        expect(Cypress.$(overlay).find('.overlay-content .p-tag').text(), 'no-data message').to.contain(noDataMessage)
      }
    })
    expect($root.find('[data-pc-name="progressspinner"]').add(overlays.find('[data-pc-name="progressspinner"]')).length, 'loading spinners').to.equal(0)
    expect($root.find('canvas').length, 'chart canvases').to.be.greaterThan(0)
    $root.find('canvas').each((index, canvas) => {
      expect(canvas.getAttribute('data-render-ready'), 'static chart final draw').to.equal('true')
    })
    expect($root.find('[data-pc-section="mask"]').filter((index, mask) =>
      !mask.closest('[data-metrics-has-data="false"]')).length, 'other loading masks').to.equal(0)
  })
  cy.get('[data-pc-section="overlay"]').should('not.exist')
  return cy.matchSnapshotImageForElement(selector, { waitForResources: true, ...options })
}
