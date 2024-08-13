describe('Transcript export tests', () => {

  beforeEach(() => {

  })

  const createSubjectAndSkills = (subjNum, numOfSkills) => {
    cy.createSubject(1, subjNum)
    for (let i = 1; i <= numOfSkills; i++) {
      cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
    }
  }

  it('empty project - with no skills, subjects or badges', () => {
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: 'Empty Project' })
    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    // cy.wait(2000)
    const pathToPdf = 'cypress/downloads/Empty Project - Joe Doe (skills@skills.org for display) - Transcript.pdf'
    cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
      // file exists and was successfully read
      cy.log('Transcript Created!')
    })
    cy.task('readPdf', pathToPdf).then((doc) => {
      expect(doc.numpages).to.equal(1)
      expect(doc.text).to.include('SkillTree TRANSCRIPT')
      expect(doc.text).to.include('Empty Project')
      expect(doc.text).to.include('Level: \n0')
      expect(doc.text).to.include('Points: \n0')
      expect(doc.text).to.include('Skills: \n0')
      expect(doc.text).to.not.include('Badges')
    })

  })

  it('transcript with 1 subject', () => {
    const projName = 'Has Subject and Skills'
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: projName })
    cy.createSubject(1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    // cy.wait(2000)
    const pathToPdf = `cypress/downloads/${projName} - Joe Doe (skills@skills.org for display) - Transcript.pdf`
    cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
      // file exists and was successfully read
      cy.log('Transcript Created!')
    })
    cy.task('readPdf', pathToPdf).then((doc) => {
      expect(doc.numpages).to.equal(2)
      expect(doc.text).to.include('SkillTree TRANSCRIPT')
      expect(doc.text).to.include(projName)
      expect(doc.text).to.include('Level: \n1\n / 5')
      expect(doc.text).to.include('Points: \n100\n / 600')
      expect(doc.text).to.include('Skills: \n0\n / 3')
      expect(doc.text).to.not.include('Badges')

      // should be a title on the 2nd page
      expect(doc.text).to.include('Subject: Subject 1'.toUpperCase())
    })
  })

  it('transcript with multiple subject and some progress', () => {
    const projName = 'Many Subj and Progress'
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: projName })
    const createSubjectAndSkills = (subjNum, numOfSkills) => {
      cy.createSubject(1, subjNum)
      for (let i = 1; i <= numOfSkills; i++) {
        cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
      }
    }
    createSubjectAndSkills(1, 12)
    createSubjectAndSkills(2, 6)
    createSubjectAndSkills(3, 8)
    cy.createSubject(1, 4)

    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 5, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 7, subjNum: 3, userId: user, date: 'now' })


    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    // cy.wait(2000)
    const pathToPdf = `cypress/downloads/${projName} - Joe Doe (skills@skills.org for display) - Transcript.pdf`
    cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
      // file exists and was successfully read
      cy.log('Transcript Created!')
    })
    cy.task('readPdf', pathToPdf).then((doc) => {
      expect(doc.numpages).to.equal(4)
      expect(doc.text).to.include('SkillTree TRANSCRIPT')
      expect(doc.text).to.include(projName)
      expect(doc.text).to.include('Level: \n1\n / 5')
      expect(doc.text).to.include('Points: \n600\n / 2,600')
      expect(doc.text).to.include('Skills: \n6\n / 26')
      expect(doc.text).to.not.include('Badges')

      // should be a title on the 2nd-4th pages
      expect(doc.text).to.include('Subject: Subject 1'.toUpperCase())
      expect(doc.text).to.include('Subject: Subject 2'.toUpperCase())
      expect(doc.text).to.include('Subject: Subject 3'.toUpperCase())
      // 4th subject doesn't have any skills so there shouldn't be a page for it
      expect(doc.text).to.not.include('Subject: Subject 4'.toUpperCase())
    })
  })

  it('transcript with multiple subject and some progress and one badge', () => {
    const projName = 'Subj and 1 Badge'
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createProject(1, { name: projName })
    const createSubjectAndSkills = (subjNum, numOfSkills) => {
      cy.createSubject(1, subjNum)
      for (let i = 1; i <= numOfSkills; i++) {
        cy.createSkill(1, subjNum, i, { numPerformToCompletion: 1 })
      }
    }
    createSubjectAndSkills(1, 12)
    createSubjectAndSkills(2, 6)
    createSubjectAndSkills(3, 8)

    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 3, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 5, subjNum: 3, userId: user, date: 'now' })
    cy.doReportSkill({ project: 1, skill: 7, subjNum: 3, userId: user, date: 'now' })

    cy.createBadge(1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.enableBadge(1, 1)

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()

    // cy.wait(2000)
    const pathToPdf = `cypress/downloads/${projName} - Joe Doe (skills@skills.org for display) - Transcript.pdf`
    cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
      // file exists and was successfully read
      cy.log('Transcript Created!')
    })
    cy.task('readPdf', pathToPdf).then((doc) => {
      expect(doc.numpages).to.equal(4)
      expect(doc.text).to.include('SkillTree TRANSCRIPT')
      expect(doc.text).to.include(projName)
      expect(doc.text).to.include('Level: \n1\n / 5')
      expect(doc.text).to.include('Points: \n600\n / 2,600')
      expect(doc.text).to.include('Skills: \n6\n / 26')
      expect(doc.text).to.include('Badges: \n1\n')

      // should be a title on the 2nd page
      expect(doc.text).to.include('Subject: Subject 1'.toUpperCase())
    })
  })

  it.only('badges table is on the second page if there are too many subjects', () => {
    const projName = `Badge Table on the Second Page`
    cy.createProject(1, { name: projName })
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    for (let i = 1; i <= 16; i++) {
      createSubjectAndSkills(i, 1)
    }
    const user = Cypress.env('proxyUser')
    cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })

    cy.createBadge(1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.enableBadge(1, 1)

    cy.cdVisit('/')
    cy.get('[data-cy="downloadTranscriptBtn"]').click()
    const pathToPdf = `cypress/downloads/${projName} - Joe Doe (skills@skills.org for display) - Transcript.pdf`
    cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
      cy.log('Transcript Created!')
    })
    cy.task('readPdf', pathToPdf).then((doc) => {
      expect(doc.numpages).to.equal(18) // 16 subj pages, first page + earned badges page
      expect(doc.text).to.include('SkillTree TRANSCRIPT')
      expect(doc.text).to.include(projName)
    })
  })

})