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
import moment from 'moment-timezone'

const dateFormatter = value => moment.utc(value)
  .format('YYYY-MM-DD[T]HH:mm:ss[Z]')

describe('Client Display Theme Components Tests', () => {

  beforeEach(() => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' })
    cy.createSkill(1, 1, 3)
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), '2019-09-12 11:00')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), '2019-09-19 11:00')
  })

  it('breadcrumb - linkColor and currentPageColor ', () => {
    const breadcrumb = JSON.stringify({
      linkColor: encodeURIComponent('#ffff69'),
      currentPageColor: encodeURIComponent('#fd70d2'),
      linkHoverColor: encodeURIComponent('#000000')
    })
    const titleBg = 'themeParam=tiles|{"backgroundColor":"gray"}'
    cy.cdVisit(`/subjects/subj1/skills/skill1?themeParam=breadcrumb|${breadcrumb}&${titleBg}`)

    cy.get('[data-cy="skillsDisplayBreadcrumbBar"]')
    cy.matchSnapshotImageForElement('[data-cy="skillsDisplayBreadcrumbBar"] nav')
  })

  it('disable breadcrumb', () => {
    cy.cdVisit(`/subjects/subj1/skills/skill1?themeParam=disableBreadcrumb|true`)
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
    const breadcrumb = 'themeParam=breadcrumb|{"align": "start"}'
    cy.cdVisit(`/subjects/subj1/skills/skill1?themeParam=pageTitle|${pageTitle}&${titleBg}&${disableBrand}&${disableBackButton}&${breadcrumb}`)

    cy.get('[data-cy="skillsTitle"]')
    cy.matchSnapshotImageForElement('[data-cy="skillsTitle"]')
  })

  it('circleProgressInteriorTextColor config', () => {
    const bgColor = encodeURIComponent('#152E4d')
    const titleBg = `themeParam=tiles|{"backgroundColor":"${bgColor}"}`
    const textPrimaryColor = `themeParam=textPrimaryColor|${encodeURIComponent('#fbfbfb')}`
    const circleProgressInteriorTextColor = `themeParam=circleProgressInteriorTextColor|${encodeURIComponent('#ec0933')}`

    // use text primary color
    cy.cdVisit(`/?${titleBg}&${textPrimaryColor}`)

    cy.get('[data-cy="overallPoints"] .vue-apexcharts')
    cy.matchSnapshotImageForElement('[data-cy="overallPoints"] .vue-apexcharts', {
      name: 'circleProgressInteriorTextColor is not present default to textPrimaryColor'
    })

    cy.cdVisit(`/?${titleBg}&${textPrimaryColor}&${circleProgressInteriorTextColor}`)
    cy.matchSnapshotImageForElement('[data-cy="overallPoints"] .vue-apexcharts', {
      name: 'circleProgressInteriorTextColor overrides textPrimaryColor'
    })
  })

  it('progressIndicators config', () => {
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 5 })
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2019-09-12 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2019-09-19 11:00')
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

    cy.cdVisit(`/?themeParam=progressIndicators|${progressIndicators}`, true)
    cy.get('[data-cy="skillsTitle"]')

    cy.matchSnapshotImageForElement('[data-cy="subjectTile-subj1"]', {
      name: 'progressIndicators settings - beforeTodayColor earnedTodayColor and incompleteColor'
    })

    cy.matchSnapshotImageForElement('[data-cy="subjectTile-subj2"]', {
      name: 'progressIndicators settings - incompleteColor'
    })

    cy.matchSnapshotImageForElement('[data-cy="subjectTile-subj3"]', {
      name: 'progressIndicators settings - completeColor'
    })

    cy.matchSnapshotImageForElement('[data-cy="overallPoints"] .vue-apexcharts', {
      name: 'beforeTodayColor and  incompleteColor apply to CircleProgress comoponent'
    })

    cy.cdClickSubj(0, 'Subject 1', true)
    cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"] [data-pc-section="body"]', {
      name: 'progressIndicators settings - skill list - beforeTodayColor earnedTodayColor and incompleteColor'
    })

    cy.cdBack()
    cy.cdClickSubj(2, 'Completed', false)
    cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"] [data-pc-section="body"]', {
      name: 'progressIndicators settings - skill list - completeColor'
    })
  })

  it('stars config', () => {
    cy.createSkill(1, 1, 3, { numPerformToCompletion: 5 })
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2019-09-12 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2019-09-19 11:00')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')

    cy.createSubject(1, 2, { name: 'Unused' })

    cy.createSubject(1, 3, { name: 'Completed' })
    cy.createSkill(1, 3, 4, { numPerformToCompletion: 1 })

    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })

    const stars = JSON.stringify({
      earnedColor: encodeURIComponent('#fd70d2'),
      unearnedColor: encodeURIComponent('#ecec05')
    })

    cy.cdVisit(`/?themeParam=stars|${stars}`, true)
    cy.get('[data-cy="skillsTitle"]')

    cy.matchSnapshotImageForElement('[data-cy="overallStars"]', {
      name: 'stars config - overall level'
    })

    cy.matchSnapshotImageForElement('[data-cy="subjectTile-subj1"] [data-cy="subjectStars"]', {
      name: 'stars config - subject - some levels achieved'
    })

    cy.matchSnapshotImageForElement('[data-cy="subjectTile-subj2"] [data-cy="subjectStars"]', {
      name: 'stars config - subject - no levels'
    })

    cy.matchSnapshotImageForElement('[data-cy="subjectTile-subj3"] [data-cy="subjectStars"]', {
      name: 'stars config - subject - all levels achieved'
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

    cy.cdVisit(`/?themeParam=charts|${charts}`)

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('point history chart - dark gray background customization', () => {
    cy.cdVisit('/?themeParam=tiles|{"backgroundColor":"gray"}&themeParam=textPrimaryColor|white&themeParam=charts|{"axisLabelColor":"lightblue"}')

    // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]')
  })

  it('chart labels for all the charts', () => {
    cy.cdVisit('/?themeParam=charts|{"labelBorderColor":"black","labelBackgroundColor":"purple","labelForegroundColor":"lightblue"}')

    // // let's wait for animation to complete
    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy=pointHistoryChart]', 'chartLabels-pointsHistory')
    cy.cdClickRank()

    cy.get('[data-cy="levelBreakdownChart-animationEnded"]')
    cy.matchSnapshotImageForElement('[data-cy="levelBreakdownChart"]', 'chartLabels-levelBreakdown')

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
    cy.cdVisit(url, true)
    cy.matchSnapshotImageForElement('[data-cy="pointProgressChart-resetZoomBtn"]', 'buttons-resetZoom')

    cy.cdClickSubj(0, 'Subject 1', true)
    cy.matchSnapshotImageForElement('[data-cy="back"]', 'buttons-Back')
    cy.matchSnapshotImageForElement('[data-cy="filterMenu"] [data-cy="filterBtn"]', 'buttons-skillsFilter')
    cy.cdClickSkill(1)

    cy.matchSnapshotImageForElement('[data-cy="claimPointsBtn"]', 'buttons-selfReport')
    cy.cdVisit(url, true)
    cy.cdClickBadges()
    cy.matchSnapshotImageForElement('[data-cy="badgeDetailsLink_badge1"] [data-pc-name="button"]', 'buttons-viewBadgeDetails')

    // todo: add back
    // cy.matchSnapshotImageForElement('[data-cy="filterMenu"] [data-cy="filterBtn"]', 'buttons-badgesFilter')
  })

  it('buttons customization with changing tile background', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/?themeParam=buttons|{"backgroundColor":"green","foregroundColor":"white",%20"borderColor":"purple"}&themeParam=tiles|{"backgroundColor":"black"}'
    cy.cdVisit(url, true)

    cy.cdClickSubj(0, 'Subject 1', true)
    cy.matchSnapshotImageForElement('[data-cy="back"]', 'buttons-Back-darkTileBackground')
    cy.matchSnapshotImageForElement('[data-cy="filterMenu"]', 'buttons-skillsFilter-darkTileBackground')
    cy.cdClickSkill(1)
    cy.matchSnapshotImageForElement('[data-cy="claimPointsBtn"]', 'buttons-selfReport-darkTileBackground')

    cy.cdVisit(url)
    cy.cdClickBadges()
    cy.matchSnapshotImageForElement('[data-cy="badgeDetailsLink_badge1"] [data-pc-name="button"]', 'buttons-viewBadgeDetails-darkTileBackground')
    // todo: add back
    // cy.matchSnapshotImageForElement('[data-cy="filterMenu"]', 'buttons-badgesFilter-darkTileBackground')
  })

  it('filter menu with dark tile background', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.enableBadge(1, 1)

    const url = '/subjects/subj1/?themeParam=tiles|{"backgroundColor":"black"}&themeParam=textPrimaryColor|white&themeParam=textSecondaryColor|yellow'
    cy.cdVisit(url)

    cy.get('[data-cy="clearSkillsSearchInput"]').tab().type('{enter}')
    // cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();

    cy.matchSnapshotImageForElement('[data-pc-name="panelmenu"]', {
      name: 'filterMenu-skills'
    })

    cy.cdBack()
    cy.cdClickBadges()
    // cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
    cy.get('[data-cy="clearSkillsSearchInput"]').tab().type('{enter}')
    cy.matchSnapshotImageForElement('[data-pc-name="panelmenu"]', {
      name: 'filterMenu-badges'
    })
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
    cy.cdVisit(url)

    cy.get('[data-cy="overallPointsEarnedCard"]')
      .contains('200')
    cy.get('[data-cy="pointsAchievedTodayCard"]')
      .contains('0')
    cy.get('[data-cy="pointsPerOccurrenceCard"]')
      .contains('100')
    cy.get('[data-cy="timeWindowPts"]')
      .contains('100')

    cy.matchSnapshotImageForElement('[data-cy="skillsSummaryCards"]', 'infoCards-skill')
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
    cy.cdVisit(url, true)

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

    cy.matchSnapshotImageForElement('[data-cy=skillsSummaryCards]')
  })

  it('ability to configure tile border', () => {
    const tiles = JSON.stringify({
      backgroundColor: 'lightgray',
      borderColor: 'blue'
    })
    const url = `/?themeParam=tiles|${tiles}&themeParam=textPrimaryColor|black}`
    cy.cdVisit(url)

    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.wait(1111)
    cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', { blackout: '[data-cy=achievementDate]' })
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
    cy.cdVisit(url)

    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.wait(1111)
    cy.matchSnapshotImageForElement('[data-cy="myRank"]', 'watermarkIconColor for my rank')
    cy.matchSnapshotImageForElement('[data-cy="myBadges"]', 'watermarkIconColor for my badges')
  })


  it('badge settings', () => {
    const badges = JSON.stringify({
      backgroundColor: 'red',
      foregroundColor: 'blue',
      backgroundColorSecondary: 'black'
    })
    const url = `/?themeParam=badges|${badges}`
    cy.cdVisit(url)

    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.wait(1111)
    cy.matchSnapshotImageForElement('[data-cy="overallLevelDesc"]')
  })

  it('subjectTileIconColor settings', () => {
    const subjectTileIconColor = encodeURIComponent('#c3ff4c')
    const url = `/?themeParam=subjectTileIconColor|${subjectTileIconColor}`
    cy.cdVisit(url)

    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.wait(1111)
    cy.matchSnapshotImageForElement('[data-cy="subjectTile"] .sd-theme-subject-tile-icon')
  })

  it('trophyIconColor settings', () => {
    const trophyIconColor = encodeURIComponent('#c3ff4c')
    const url = `/?themeParam=trophyIconColor|${trophyIconColor}`
    cy.cdVisit(url)

    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.wait(1111)
    cy.matchSnapshotImageForElement('[data-cy="overallLevel"] .trophy-stack')
  })


  it('backButton settings', () => {
    const backButton = JSON.stringify({
      padding: '1rem 3rem 4rem 2rem',
      fontSize: '3rem',
      lineHeight: '2rem',
    })
    const url = `/subjects/subj1?themeParam=backButton|${backButton}`
    cy.cdVisit(url)

    cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    cy.wait(1111)
    cy.matchSnapshotImageForElement('[data-cy="back"]')
  })




})

