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
      expect(doc.text).to.include('Level:0')
      expect(doc.text).to.include('Points:0')
      expect(doc.text).to.include('Skills:0')
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
      expect(doc.text).to.include('Level:1/ 5')
      expect(doc.text).to.include('Points:100/ 600')
      expect(doc.text).to.include('Skills:0/ 3')
      expect(doc.text).to.not.include('Badges')

      // should be a title on the 2nd page
      expect(doc.text).to.include('Subject: Subject 1')
    })
  })

  it('transcript with multiple subject and some pgoress', () => {
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
      expect(doc.text).to.include('Level:1/ 5')
      expect(doc.text).to.include('Points:600/ 2,600')
      expect(doc.text).to.include('Skills:6/ 26')
      expect(doc.text).to.not.include('Badges')

      // should be a title on the 2nd page
      expect(doc.text).to.include('Subject: Subject 1')
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
      expect(doc.text).to.include('Level:1/ 5')
      expect(doc.text).to.include('Points:600/ 2,600')
      expect(doc.text).to.include('Skills:6/ 26')
      expect(doc.text).to.include('Badges:1')

      // should be a title on the 2nd page
      expect(doc.text).to.include('Subject: Subject 1')
    })
  })

  it('badges table placement is determined based on number of subjects', () => {
    cy.createProject(1)
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    for (let i = 1; i <= 10; i++) {
      const projName = `Badge Table Placement ${i}`
      cy.updateProject(1, { name: projName })

      createSubjectAndSkills(i, 1)
      if (i === 1) {
        const user = Cypress.env('proxyUser')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })

        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)
      }

      cy.cdVisit('/')
      cy.get('[data-cy="downloadTranscriptBtn"]').click()
      const pathToPdf = `cypress/downloads/${projName} - Joe Doe (skills@skills.org for display) - Transcript.pdf`
      cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
        cy.log('Transcript Created!')
      })
      cy.task('readPdf', pathToPdf).then((doc) => {
        expect(doc.numpages).to.equal(i + 1)
        expect(doc.text).to.include('SkillTree TRANSCRIPT')
        expect(doc.text).to.include(projName)
      })
    }
    // for (let i = 1; i <= 10; i++) {
    //   const projName = `Badge Table Placement ${i}`
    //   cy.createProject(1, { name: projName })
    //
    //   for (let j = 0; j < i; i++) {
    //     createSubjectAndSkills(j+1, 1)
    //   }
    //
    //   const user = Cypress.env('proxyUser')
    //   cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: user, date: 'now' })
    //
    //   cy.createBadge(1)
    //   cy.assignSkillToBadge(1, 1, 1);
    //   cy.enableBadge(1, 1);
    //
    //   cy.cdVisit('/')
    //   cy.get('[data-cy="downloadTranscriptBtn"]').click()
    //
    //   // cy.wait(2000)
    //   const pathToPdf = `cypress/downloads/${projName} - Joe Doe (skills@skills.org for display) - Transcript.pdf`
    //   cy.readFile(pathToPdf, { timeout: 10000 }).then(() => {
    //     // file exists and was successfully read
    //     cy.log('Transcript Created!')
    //   })
    //   cy.task('readPdf', pathToPdf).then((doc) => {
    //     expect(doc.numpages).to.equal(4)
    //     expect(doc.text).to.include('SkillTree TRANSCRIPT')
    //     expect(doc.text).to.include(projName)
    //     expect(doc.text).to.include('Level:1/ 5')
    //     expect(doc.text).to.include('Points:600/ 2,600')
    //     expect(doc.text).to.include('Skills:6/ 26')
    //     expect(doc.text).to.include('Badges:1')
    //
    //     // should be a title on the 2nd page
    //     expect(doc.text).to.include('Subject: Subject 1')
    //   })
    // }
  })

  it('badges table is on the second page if there are too many subjects', () => {
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
      expect(doc.numpages).to.equal(18) // 16 subj pages, first page + eanrned badges page
      expect(doc.text).to.include('SkillTree TRANSCRIPT')
      expect(doc.text).to.include(projName)
    })
  })

})