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
describe('Client Display Theme Components Tests', () => {

  const visitThemePage = (path, expectPointHistoryData = false) => {
    cy.cdVisit(`${path}${path.includes('?') ? '&' : '?'}disableChartAnimations=true`)
    if (expectPointHistoryData) {
      cy.get('[data-cy="pointHistoryChartWithData"]').should('be.visible')
    }
  }

  const matchComponentSnapshot = (selector, { allowPendingProgress = false, ...options } = {}) => {
    if (!allowPendingProgress) {
      cy.get('[data-pc-name="progressspinner"]').should('not.exist')
    }
    cy.get(selector).should('be.visible').should($element => {
      const loadingMasks = $element.find('[data-pc-section="mask"]').addBack('[data-pc-section="mask"]')
        .add($element.parents('[data-pc-name="blockui"]').children('[data-pc-section="mask"]'))
        .filter((index, mask) => !mask.closest('[data-cy="pointHistoryChartNoData"]'))
      expect(loadingMasks, 'captured loading masks').to.have.length(0)
    })
    return cy.matchSnapshotImageForElement(selector, { waitForResources: true, ...options })
  }

  const waitForPointHistoryChart = () => {
    cy.get('[data-cy="pointHistoryChart-rendered"]').should('have.attr', 'data-render-version', '1')
    cy.get('[data-cy="pointHistoryChartWithData"]').should('have.attr', 'aria-busy', 'false')
    cy.get('[data-cy="pointHistoryChartWithData"] [data-pc-section="mask"]').should('not.exist')
  }

  const waitForCircleProgress = (selector) => {
    cy.get(`${selector} [data-cy="radialChart-rendered"]`).should('exist')
  }

  const waitForEmbeddedDisplay = (maxWidth) => {
    cy.wrapIframe().find('[data-cy="skillTreePoweredBy"]').should('be.visible')
    cy.wrapIframe().find('[data-cy="subjectTileBtn"]').should('have.length', 1)
    cy.wrapIframe().find('[data-cy="myRankPosition"]').should('contain.text', '#1')
    cy.wrapIframe().find('[data-cy="pointHistoryChart-rendered"]').should('have.attr', 'data-render-version', '1')
    cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"]').should('have.attr', 'aria-busy', 'false')
    cy.wrapIframe().find('[data-cy="pointHistoryChartWithData"] [data-pc-section="mask"]').should('not.exist')
    cy.wrapIframe().find('[data-cy="overallPoints"] [data-cy="radialChart-rendered"]').should('exist')
    cy.wrapIframe().find('[data-cy="levelProgress"] [data-cy="radialChart-rendered"]').should('exist')
    cy.wrapIframe().find('[data-pc-name="progressspinner"]').should('not.exist')
    cy.wrapIframe().find('[data-cy="skillsDisplayHome"]').should('have.css', 'max-width', maxWidth)
    cy.wrapIframe().then($body => $body[0].ownerDocument.fonts.ready)
    // The SDK updates the host height asynchronously after the child's ResizeObserver fires.
    cy.get('body iframe').should($iframe => {
      const frame = $iframe[0]
      const display = frame.contentDocument.querySelector('#skills-display-app')
      expect(display, 'embedded display root').not.to.be.null
      const expectedHeight = Math.max(Cypress.$(display).height() + 10, 960)
      expect(frame.getBoundingClientRect().height, 'iframe height').to.be.closeTo(expectedHeight, 1)
    })
  }

  beforeEach(() => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' })
    cy.createSkill(1, 1, 3)
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), '2023-08-12 11:00')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), '2023-08-19 11:00')
  })

  it('pending progress message respects the display theme', () => {
    cy.configureDarkMode(false)
    // Keep the message visible across background checks, independently of worker timing.
    cy.intercept('POST', '/api/projects/proj1/progress/initialize', {
      statusCode: 200,
      body: { ready: false, backfillStatus: 'PENDING' }
    })

    const message = '[data-cy="userProgressLoading"]'
    const captureMessage = (name) => {
      cy.get('[data-cy="skillsTitle"]').should('contain.text', 'Themed User Skills')
      cy.get(message).should('be.visible').and('contain.text', 'Loading your latest progress')
        .and('contain.text', 'You can keep exploring while we sync your points and achievements.')
      cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '200')
      matchComponentSnapshot(message, {
        allowPendingProgress: true,
        name,
        errorThreshold: 0.1
      })
    }

    // Use visit directly: cdVisit expects every spinner to disappear, including the persistent message spinner.
    cy.visit('/test-skills-display/proj1?enableTheme=true&disableChartAnimations=true')
    captureMessage('pending progress message - themed defaults')

    const tiles = encodeURIComponent(JSON.stringify({ backgroundColor: '#152E4d', borderColor: '#ff0000' }))
    cy.visit(`/test-skills-display/proj1?enableTheme=true&disableChartAnimations=true&themeParam=tiles|${tiles}&themeParam=textPrimaryColor|white&themeParam=textSecondaryColor|${encodeURIComponent('#dbeafe')}`)
    captureMessage('pending progress message - themed tile border and text colors')
  })

  it('breadcrumb - linkColor and currentPageColor ', () => {
    const breadcrumb = JSON.stringify({
      linkColor: encodeURIComponent('#ffff69'),
      currentPageColor: encodeURIComponent('#fd70d2'),
      linkHoverColor: encodeURIComponent('#000000')
    })
    const titleBg = 'themeParam=tiles|{"backgroundColor":"gray"}'
    visitThemePage(`/subjects/subj1/skills/skill1?themeParam=breadcrumb|${breadcrumb}&${titleBg}`)

    cy.get('[data-cy="skillsDisplayBreadcrumbBar"]')
    matchComponentSnapshot('[data-cy="skillsDisplayBreadcrumbBar"] nav', { errorThreshold: 0.2 })
  })

  it('disable breadcrumb', () => {
    visitThemePage(`/subjects/subj1/skills/skill1?themeParam=disableBreadcrumb|true`)
    cy.get('[data-cy="skillsTitle"]')
    cy.get('[data-cy="skillsDisplayBreadcrumbBar"]').should('not.exist')
  })

  it('pageTitle config', () => {
    const pageTitle = JSON.stringify({
      textColor: encodeURIComponent('#fd70d2'),
      borderColor: encodeURIComponent('#ec0933'),
      fontSize: '4rem',
      borderStyle: 'none none solid none',
      backgroundColor: encodeURIComponent('#c3fad2'),
      textAlign: 'left',
      padding: '1.6rem 1rem 1.1rem 1rem',
      margin: '3rem'
    })
    const bgColor = encodeURIComponent('#152E4d')
    const titleBg = `themeParam=tiles|{"backgroundColor":"${bgColor}"}`
    const disableBrand = `themeParam=disableSkillTreeBrand|true`
    const disableBackButton = 'disableBackButton=true'
    const disableSearchButton = `themeParam=disableSearchButton|true`
    const breadcrumb = 'themeParam=breadcrumb|{"align": "start"}'
    visitThemePage(`/subjects/subj1/skills/skill1?themeParam=pageTitle|${pageTitle}&${titleBg}&${disableBrand}&${disableBackButton}&${disableSearchButton}&${breadcrumb}`)

    cy.get('[data-cy="skillsTitle"]')
    matchComponentSnapshot('[data-cy="skillsTitle"]', { errorThreshold: 0.1 })
  })

  it('circleProgressInteriorTextColor config', () => {
    const bgColor = encodeURIComponent('#152E4d')
    const titleBg = `themeParam=tiles|{"backgroundColor":"${bgColor}"}`
    const textPrimaryColor = `themeParam=textPrimaryColor|${encodeURIComponent('#fbfbfb')}`
    const circleProgressInteriorTextColor = `themeParam=circleProgressInteriorTextColor|${encodeURIComponent('#ec0933')}`

    // use text primary color
    visitThemePage(`/?${titleBg}&${textPrimaryColor}`)

    cy.get('[data-cy="overallPoints"] [data-cy="circleProgressPts"]').should('have.text', '200')
    cy.get('[data-cy="overallPoints"] [data-cy="circleProgressPercent"]').should('have.text', '33%')
    cy.get('[data-cy="levelProgress"] [data-cy="circleProgressPts"]').should('have.text', '50')
    cy.get('[data-cy="levelProgress"] [data-cy="circleProgressPercent"]').should('have.text', '41%')

    waitForCircleProgress('[data-cy="overallPoints"]')
    waitForCircleProgress('[data-cy="levelProgress"]')
    matchComponentSnapshot('[data-cy="overallPoints"] [data-pc-name="chart"]', {
      name: 'circleProgressInteriorTextColor is not present default to textPrimaryColor',
      errorThreshold: 0.1
    })

    visitThemePage(`/?${titleBg}&${textPrimaryColor}&${circleProgressInteriorTextColor}`)
    cy.get('[data-cy="overallPoints"] [data-cy="circleProgressPts"]').should('have.text', '200')
    waitForCircleProgress('[data-cy="overallPoints"]')
    matchComponentSnapshot('[data-cy="overallPoints"] [data-pc-name="chart"]', {
      name: 'circleProgressInteriorTextColor overrides textPrimaryColor',
      errorThreshold: 0.1
    })
  })

  it('progressIndicators config', () => {
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 5 })
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2023-08-12 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2023-08-19 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')

    cy.createSubject(1, 2, { name: 'Unused' })

    cy.createSubject(1, 3, { name: 'Completed' })
    cy.createSkill(1, 3, 4, { numPerformToCompletion: 1 })

    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })

    const progressIndicators = JSON.stringify({
      beforeTodayColor: encodeURIComponent('#fd70d2'),
      earnedTodayColor: encodeURIComponent('#ec0933'),
      completeColor: encodeURIComponent('#03270f'),
      incompleteColor: encodeURIComponent('#ecec05')
    })

    visitThemePage(`/?themeParam=progressIndicators|${progressIndicators}`, true)
    cy.get('[data-cy="skillsTitle"]')

    cy.get('[data-cy="subjectTile-subj1"]').should('contain.text', 'Subject 1')
    matchComponentSnapshot('[data-cy="subjectTile-subj1"]', {
      name: 'progressIndicators settings - beforeTodayColor earnedTodayColor and incompleteColor',
      errorThreshold: 0.1
    })

    cy.get('[data-cy="subjectTile-subj2"]').should('contain.text', 'Unused')
    matchComponentSnapshot('[data-cy="subjectTile-subj2"]', {
      name: 'progressIndicators settings - incompleteColor',
      errorThreshold: 0.1
    })

    cy.get('[data-cy="subjectTile-subj3"]').should('contain.text', 'Completed')
    matchComponentSnapshot('[data-cy="subjectTile-subj3"]', {
      name: 'progressIndicators settings - completeColor',
      errorThreshold: 0.1
    })


    waitForCircleProgress('[data-cy="overallPoints"]')
    matchComponentSnapshot('[data-cy="overallPoints"] [data-pc-name="chart"]', {
      name: 'beforeTodayColor and  incompleteColor apply to CircleProgress comoponent',
      errorThreshold: 0.1
    })

    matchComponentSnapshot('[data-cy="achievedSkillsProgress"]', {
      name: 'achieved skills progress component',
      errorThreshold: 0.1
    })

    cy.cdClickSubj(0, 'Subject 1', true)
    cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '300 / 500 Points')
    matchComponentSnapshot('[data-cy="skillsProgressList"] [data-pc-section="body"]', {
      name: 'progressIndicators settings - skill list - beforeTodayColor earnedTodayColor and incompleteColor',
      blackout: '[data-cy="skillProgressTitle"]',
      errorThreshold: 0.1
    })

    cy.cdBack()
    cy.cdClickSubj(2, 'Completed', false)
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 100 Points')
    matchComponentSnapshot('[data-cy="skillsProgressList"] [data-pc-section="body"]', {
      name: 'progressIndicators settings - skill list - completeColor',
      blackout: '[data-cy="skillProgressTitle"]',
      errorThreshold: 0.1
    })
  })

  it('stars config', () => {
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 5 })
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2023-08-12 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2023-08-19 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')

    cy.createSubject(1, 2, { name: 'Unused' })

    cy.createSubject(1, 3, { name: 'Completed' })
    cy.createSkill(1, 3, 4, { numPerformToCompletion: 1 })

    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })

    const stars = JSON.stringify({
      earnedColor: encodeURIComponent('#fd70d2'),
      unearnedColor: encodeURIComponent('#ecec05')
    })

    visitThemePage(`/?themeParam=stars|${stars}`, true)
    cy.get('[data-cy="skillsTitle"]')

    matchComponentSnapshot('[data-cy="overallStars"]', {
      name: 'stars config - overall level',
      errorThreshold: 0.1
    })

    matchComponentSnapshot('[data-cy="subjectTile-subj1"] [data-cy="subjectStars"]', {
      name: 'stars config - subject - some levels achieved',
      errorThreshold: 0.1
    })

    matchComponentSnapshot('[data-cy="subjectTile-subj2"] [data-cy="subjectStars"]', {
      name: 'stars config - subject - no levels',
      errorThreshold: 0.1
    })

    matchComponentSnapshot('[data-cy="subjectTile-subj3"] [data-cy="subjectStars"]', {
      name: 'stars config - subject - all levels achieved',
      errorThreshold: 0.1
    })


  })

  it('point history chart - line, label and gradient', () => {
    const charts = JSON.stringify({
      pointHistory: {
        lineColor: 'purple',
        gradientStartColor: 'blue',
        gradientStopColor: 'yellow'
      },
      labelBorderColor: 'black',
      labelBackgroundColor: 'green',
      labelForegroundColor: 'lightgray'
    })

    visitThemePage(`/?themeParam=charts|${charts}`)
    waitForPointHistoryChart()
    matchComponentSnapshot('[data-cy=pointHistoryChart]', { errorThreshold: 0.1 })
  })

  it('point history chart - dark gray background customization', () => {
    visitThemePage('/?themeParam=tiles|{"backgroundColor":"gray"}&themeParam=textPrimaryColor|white&themeParam=charts|{"axisLabelColor":"lightblue"}')
    waitForPointHistoryChart()
    matchComponentSnapshot('[data-cy=pointHistoryChart]', { errorThreshold: 0.1 })
  })

  it('chart labels for all the charts', () => {
    visitThemePage('/?themeParam=charts|{"labelBorderColor":"black","labelBackgroundColor":"purple","labelForegroundColor":"lightblue"}')
    waitForPointHistoryChart()
    matchComponentSnapshot('[data-cy=pointHistoryChart]', { name: 'chartLabels-pointsHistory', errorThreshold: 0.1 })
    cy.cdClickRank()

    cy.get('[data-cy="levelBreakdownChart-rendered"]').should('exist')
    cy.get('[data-cy="levelBreakdownChart"] [data-pc-name="blockui"]').should('have.attr', 'aria-busy', 'false')
    matchComponentSnapshot('[data-cy="levelBreakdownChart"]', { name: 'chartLabels-levelBreakdown', errorThreshold: 0.1 })

  })

  it('buttons customization without changing tile background', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const buttons = JSON.stringify({
      backgroundColor: 'green',
      foregroundColor: 'white',
      borderColor: 'purple',
      disabledColor: 'red',
    })
    const url = `/?themeParam=buttons|${buttons}`
    visitThemePage(url, true)

    cy.cdClickSubj(0, 'Subject 1', true)
    matchComponentSnapshot('[data-cy="back"]', { name: 'buttons-Back', errorThreshold: 0.1 })
    matchComponentSnapshot('[data-cy="filterMenu"] [data-cy="filterBtn"]', { name: 'buttons-skillsFilter', errorThreshold: 0.1 })
    cy.cdClickSkill(1)

    cy.get('[data-cy="claimPointsBtn"]').should('be.enabled')
    matchComponentSnapshot('[data-cy="claimPointsBtn"]', { name: 'buttons-selfReport', errorThreshold: 0.1 })
    visitThemePage(url, true)
    cy.cdClickBadges()
    cy.get('[data-cy="badgeDetailsLink_badge1"] button').should('be.enabled')
    matchComponentSnapshot('[data-cy="badgeDetailsLink_badge1"] [data-pc-name="button"]', { name: 'buttons-viewBadgeDetails', errorThreshold: 0.1 })

    // todo: add back
    // matchComponentSnapshot('[data-cy="filterMenu"] [data-cy="filterBtn"]', { name: 'buttons-badgesFilter' })
  })

  it('buttons customization with changing tile background - subjects', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/subjects/subj1?themeParam=buttons|{"backgroundColor":"green","foregroundColor":"white",%20"borderColor":"purple"}&themeParam=tiles|{"backgroundColor":"black"}'
    visitThemePage(url, true)

    cy.get('[data-cy="filterMenu"]')

    matchComponentSnapshot('[data-cy="back"]', { name: 'buttons-Back-darkTileBackground', errorThreshold: 0.1 })
    matchComponentSnapshot('[data-cy="filterMenu"]', { name: 'buttons-skillsFilter-darkTileBackground', errorThreshold: 0.1 })
  })

  it('buttons customization with changing tile background - skill', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/subjects/subj1/skills/skill2?themeParam=buttons|{"backgroundColor":"green","foregroundColor":"white",%20"borderColor":"purple"}&themeParam=tiles|{"backgroundColor":"black"}'
    visitThemePage(url)

    cy.get('[data-cy="claimPointsBtn"]').should('be.enabled')
    matchComponentSnapshot('[data-cy="claimPointsBtn"]', { name: 'buttons-selfReport-darkTileBackground', errorThreshold: 0.1 })
  })

  it('buttons customization with changing tile background - badge', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/badges?themeParam=buttons|{"backgroundColor":"green","foregroundColor":"white",%20"borderColor":"purple"}&themeParam=tiles|{"backgroundColor":"black"}'
    visitThemePage(url)
    cy.get('[data-cy="badgeDetailsLink_badge1"] button').should('be.enabled')

    matchComponentSnapshot('[data-cy="badgeDetailsLink_badge1"] [data-pc-name="button"]', { name: 'buttons-viewBadgeDetails-darkTileBackground', errorThreshold: 0.1 })
  })

  it('filter menu with dark tile background - skills', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)

    const url = '/subjects/subj1/?themeParam=tiles|{"backgroundColor":"black"}&themeParam=textPrimaryColor|white&themeParam=textSecondaryColor|yellow'
    visitThemePage(url)

    cy.get('[data-cy="clearSkillsSearchInput"]').tab().type('{enter}')

    cy.get('[data-cy="filter_withoutProgress"]').should('be.visible')
    matchComponentSnapshot('[data-pc-name="panelmenu"]', {
      name: 'filterMenu-skills',
      errorThreshold: 0.1
    })
  })

  it('filter menu with dark tile background - badges', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/badges/?themeParam=tiles|{"backgroundColor":"black"}&themeParam=textPrimaryColor|white&themeParam=textSecondaryColor|yellow'
    visitThemePage(url)

    cy.get('[data-cy="badgeDetailsLink_badge1"] button').should('be.enabled')
    cy.get('[data-cy="clearSkillsSearchInput"]').tab().type('{enter}')
    cy.get('[data-cy="filter_projectBadges"]').should('be.visible')
    matchComponentSnapshot('[data-pc-name="panelmenu"]', {
      name: 'filterMenu-badges',
      errorThreshold: 0.1
    })
  })

  it('filter menu with just textPrimaryColor', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/subjects/subj1/?themeParam=textPrimaryColor|purple'
    visitThemePage(url)
    cy.get('[data-cy="clearSkillsSearchInput"]').tab().type('{enter}')
    cy.get('[data-cy="filter_withoutProgress"]').should('be.visible')
    matchComponentSnapshot('[data-pc-name="panelmenu"]', { errorThreshold: 0.1 })
  })

  it('theme info cards', () => {
    const infoCards = encodeURIComponent(JSON.stringify({
      backgroundColor: 'lightgray',
      borderColor: 'green',
      foregroundColor: 'purple',
      iconColors: ['blue', 'red', 'yellow', 'green']
    }))
    const tiles = JSON.stringify({ backgroundColor : encodeURIComponent('#f6dedd')})
    const theme = `/?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|white&themeParam=infoCards|${infoCards}`
    const url = `/subjects/subj1/skills/skill1${theme}`
    visitThemePage(url)

    cy.get('[data-cy="overallPointsEarnedCard"]')
      .contains('200')
    cy.get('[data-cy="pointsAchievedTodayCard"]')
      .contains('0')
    cy.get('[data-cy="pointsPerOccurrenceCard"]')
      .contains('100')
    cy.get('[data-cy="timeWindowPts"]')
      .contains('100')

    matchComponentSnapshot('[data-cy="skillsSummaryCards"]', { name: 'infoCards-skill', errorThreshold: 0.1 })
  })

  it('theme info cards border overrides tile border', () => {
    const tiles = JSON.stringify({
      backgroundColor: 'lightgray',
      borderColor: 'blue'
    })
    const infoCards = JSON.stringify({
      borderColor:'purple'
    })
    const url = `/?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|black&themeParam=infoCards|${infoCards}`
    visitThemePage(url, true)

    cy.cdClickSubj(0, 'Subject 1', true)
    cy.cdClickSkill(0)

    cy.get('[data-cy="overallPointsEarnedCard"]')
      .contains('200')
    cy.get('[data-cy="pointsAchievedTodayCard"]')
      .contains('0')
    cy.get('[data-cy="pointsPerOccurrenceCard"]')
      .contains('100')
    cy.get('[data-cy="timeWindowPts"]')
      .contains('100')

    matchComponentSnapshot('[data-cy=skillsSummaryCards]', { errorThreshold: 0.1 })
  })

  it('ability to configure tile border', () => {
    const tiles = JSON.stringify({
      backgroundColor: 'lightgray',
      borderColor: 'blue'
    })
    const url = `/?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|black}`
    visitThemePage(url)
    waitForPointHistoryChart()
    waitForCircleProgress('[data-cy="overallPoints"]')
    waitForCircleProgress('[data-cy="levelProgress"]')
    cy.get('[data-cy="subjectTile-subj1"]').should('be.visible')
    cy.get('[data-cy="myRankPosition"]').should('contain.text', '#1')
    matchComponentSnapshot('[data-cy="skillsDisplayHome"]', { blackout: '[data-cy=achievementDate]', errorThreshold: 0.1 })
  })

  it('tiles watermarkIconColor config', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const tiles = JSON.stringify({
      backgroundColor: 'lightgray',
      borderColor: 'blue',
      watermarkIconColor: 'purple'
    })
    const url = `/?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|black}`
    visitThemePage(url)
    cy.get('[data-cy="myRankPosition"]').should('contain.text', '#1')
    matchComponentSnapshot('[data-cy="myRank"]', { name: 'watermarkIconColor for my rank', errorThreshold: 0.1 })
    matchComponentSnapshot('[data-cy="myBadges"]', { name: 'watermarkIconColor for my badges', errorThreshold: 0.1 })
  })

  it('tiles subtitle overlay colors', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const tiles = JSON.stringify({
      backgroundColor: encodeURIComponent('#152E4d'),
      watermarkIconColor: 'rgba(243,198,54)',
      subTitleOverlayTextColor: 'rgb(0,255,0)',
      subTitleOverlayBackgroundColor: 'rgba(243,198,54,0.3)'
    })
    const url = `/?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|black}&themeParam=textPrimaryColor|white`
    visitThemePage(url)
    cy.get('[data-cy="myRankPosition"]').should('contain.text', '#1')
    matchComponentSnapshot('[data-cy="myRank"]', { name: 'subtitle overlay colors for my rank', errorThreshold: 0.1 })
    matchComponentSnapshot('[data-cy="myBadges"]', { name: 'subtitle overlay colors for my badges', errorThreshold: 0.1 })
  })

  it('badge settings', () => {
    const badges = JSON.stringify({
      backgroundColor: 'red',
      foregroundColor: 'blue',
      backgroundColorSecondary: 'black'
    })
    const url = `/?themeParam=badges|${badges}`
    visitThemePage(url)
    cy.get('[data-cy="overallLevelDesc"]').should('contain.text', 'Level')
    matchComponentSnapshot('[data-cy="overallLevelDesc"]', { errorThreshold: 0.1 })
  })

  it('subjectTileIconColor settings', () => {
    const subjectTileIconColor = encodeURIComponent('#c3ff4c')
    const url = `/?themeParam=subjectTileIconColor|${subjectTileIconColor}`
    visitThemePage(url)
    cy.get('[data-cy="subjectTile-subj1"]').should('be.visible')
    matchComponentSnapshot('[data-cy="subjectTile"] .sd-theme-subject-tile-icon', { errorThreshold: 0.1 })
  })

  it('trophyIconColor settings', () => {
    const trophyIconColor = encodeURIComponent('#c3ff4c')
    const url = `/?themeParam=trophyIconColor|${trophyIconColor}`
    visitThemePage(url)
    cy.get('[data-cy="overallLevelDesc"]').should('contain.text', 'Level')
    matchComponentSnapshot('[data-cy="overallLevel"] [data-cy="trophyIcon"]', { errorThreshold: 0.1 })
  })

  it('backButton settings', () => {
    const backButton = JSON.stringify({
      padding: '1rem 3rem 4rem 2rem',
      fontSize: '3rem',
      lineHeight: '2rem',
    })
    const url = `/subjects/subj1?themeParam=backButton|${backButton}`
    visitThemePage(url)
    cy.get('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1')
    matchComponentSnapshot('[data-cy="back"]', { errorThreshold: 0.1 })
  })

  it('searchButton settings', () => {
    const searchButton = JSON.stringify({
      padding: '1rem 3rem 4rem 2rem',
      fontSize: '3rem',
      lineHeight: '2rem',
    })
    const url = `/subjects/subj1?themeParam=searchButton|${searchButton}`
    visitThemePage(url)
    cy.get('[data-cy="skillsTitle"]').should('contain.text', 'Subject 1')
    matchComponentSnapshot('[data-cy="skillsDisplaySearchBtn"]', { errorThreshold: 0.1 })
  })

  it('maxWidth setting - 75%', () => {
    cy.ignoreSkillsClientError()
    cy.visit(`/test-skills-client/proj1?disableChartAnimations=true&themeParam=maxWidth|${encodeURIComponent('75%')}`)
    waitForEmbeddedDisplay('75%')
    matchComponentSnapshot('body iframe', {
      errorThreshold: 0.05
    })
  })

  it('maxWidth setting - 900px', () => {
    cy.ignoreSkillsClientError()
    cy.visit(`/test-skills-client/proj1?disableChartAnimations=true&themeParam=maxWidth|${encodeURIComponent('900px')}`)
    waitForEmbeddedDisplay('900px')
    matchComponentSnapshot('body iframe', {
      errorThreshold: 0.05
    })
  })

  it('markdown editor header controls respect background color', () => {
    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval'})

    const tiles = JSON.stringify( {
      backgroundColor:encodeURIComponent('#152E4d')
    })
    const url = `/subjects/subj1/skills/skill5?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|white}`
    visitThemePage(url)

    cy.get('[data-cy="requestApprovalBtn"]').click()
    cy.get('[data-cy="markdownEditorInput"] [aria-label="Headings"]').click()
    cy.get('.toastui-editor-popup [aria-label="Headings"]').should('be.visible')
    matchComponentSnapshot('.toastui-editor-popup [aria-label="Headings"]', {
      errorThreshold: 0.05
    })
  })

  it('markdown display foreground color', () => {
    const markdown = '# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n' +
      '---\n' +
      '# Emphasis\n' +
      'italics: *italicized* or _italicized_\n\n' +
      'bold: **bolded** or __bolded__\n\n' +
      'combination **_bolded & italicized_**\n\n' +
      'strikethrough: ~~struck~~\n\n' +
      '---\n' +
      '# Inline\n' +
      'Inline `code` has `back-ticks around` it\n\n' +
      '---\n' +
      '# Multiline\n' +
      '\n' +
      '\n' +
      '```\n' +
      'import { SkillsDirective } from \'@skilltree/skills-client-vue\';\n' +
      'Vue.use(SkillsDirective);\n' +
      '```\n' +
      '# Lists\n' +
      'Ordered Lists:\n' +
      '1. Item one\n' +
      '1. Item two\n' +
      '1. Item three (actual number does not matter)\n\n' +
      'If List item has multiple lines of text, subsequent lines must be idented four spaces, otherwise list item numbers will reset, e.g.,\n' +
      '1. item one\n' +
      '    paragrah one\n' +
      '1. item two\n' +
      '1. item three\n' +
      '\n' +
      'Unordered Lists\n' +
      '* Item\n' +
      '* Item\n' +
      '* Item\n' +
      '___\n' +
      '# Links\n' +
      '[in line link](https://www.somewebsite.com)\n' +
      '___\n' +
      '# Blockquotes\n' +
      '> Blockquotes are very handy to emulate reply text.\n' +
      '> This line is part of the same quote.\n\n' +
      '# Horizontal rule\n' +
      'Use three or more dashes, asterisks, or underscores to generate a horizontal rule line\n' +
      '\n' +
      'Separate me\n\n' +
      '___\n\n' +
      'Separate me\n\n' +
      '---\n\n' +
      'Separate me\n\n' +
      '***\n\n'

    cy.createSkill(1, 1, 5, {description: markdown })

    const tiles = JSON.stringify( {
      backgroundColor:encodeURIComponent('#152E4d')
    })
    const url = `/subjects/subj1/skills/skill5?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|white}`
    visitThemePage(url)

    cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '100 Increment')
    cy.get('#toastuiViewer-skillDescription-skill5 h1').should('contain.text', 'Title1')
    cy.get('#toastuiViewer-skillDescription-skill5 pre').should('contain.text', 'SkillsDirective')
    cy.get('#toastuiViewer-skillDescription-skill5 blockquote').should('contain.text', 'Blockquotes are very handy')
    cy.get('#toastuiViewer-skillDescription-skill5').should('contain.text', 'Separate me')
    matchComponentSnapshot('#toastuiViewer-skillDescription-skill5', {
      errorThreshold: 0.05
    })
  })



})
