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

describe('Transcript export in skills-client tests', () => {

  const deleteDownloadDir = () => {
    try {
      rmdirSync('cypress/downloads', { maxRetries: 5, recursive: true });
    } catch (error) {
      throw Error(`Failed to remove cypress/downloads with ${error}`);
    } finally {
      return null;
    }
  };

  let user
  beforeEach(() => {
    deleteDownloadDir()
    const userInfo = {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    }
    cy.request('POST', '/app/userInfo', userInfo)
    user = Cypress.env('proxyUser')
    const userIdInFileName = Cypress.env('oauthMode') ? 'foo' : user
    const buildPath = (projName) => {
      const cleanUpRegex = /[^a-zA-Z0-9@.()\s]/g
      return `cypress/downloads/${projName.replace(cleanUpRegex, '')} - ${userInfo.nickname} (${userIdInFileName}) - Transcript.pdf`
    }
    Cypress.Commands.add("readTranscript", (projName) => {
      const pathToPdf = buildPath(projName)
      return cy.readPdf(pathToPdf)
    });
  })

  const expectedHeaderAndFooter = 'For All Dragons enjoyment'
  const expectedHeaderAndFooterCommunityProtected = 'For Divine Dragon enjoyment'

  const clean = (text) => {
    return text.replace(/\n/g, '')
  }

  it('ability to export transcript', () => {
    const projName = 'Has Subject and Skills'
    cy.createProject(1, { name: projName })
    cy.createSubject(1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.reportSkill(1, 1, user, 'now')

    cy.ignoreSkillsClientError()
    cy.intercept('/api/projects/proj1/pointHistory').as('getProjPointsHistory')
    cy.visit('/test-skills-client/proj1')
    cy.wait('@getProjPointsHistory')
    cy.wrapIframe().find('[data-cy="downloadTranscriptBtn"]').click()

    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(2)
      expect(clean(doc.text)).to.include('SkillTree Transcript')
      expect(clean(doc.text)).to.include(projName)
      expect(clean(doc.text)).to.include('Level: 1 / 5 ')
      expect(clean(doc.text)).to.include('Points: 100 / 600 ')
      expect(clean(doc.text)).to.include('Skills: 0 / 3 ')
      expect(clean(doc.text)).to.not.include('Badges')

      // should be a title on the 2nd page
      expect(clean(doc.text)).to.include('Subject: Subject 1'.toUpperCase())

      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)
      expect(clean(doc.text)).to.not.include('null')
    })
  })

  it('ability to configure footer and header text', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.exportHeaderAndFooter = 'For {{community.project.descriptor}} enjoyment';
        res.send(conf);
      });
    })
    const projName = `Footer and Header`
    cy.createProject(1, { name: projName })
    cy.request('POST', '/app/userInfo', {
      'first': 'Joe',
      'last': 'Doe',
      'nickname': 'Joe Doe'
    })
    cy.createSubject(1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.visit('/test-skills-client/proj1')
    cy.wrapIframe().find('[data-cy="downloadTranscriptBtn"]').click()
    const numExpectedPages = 2
    cy.readTranscript(projName).then((doc) => {
      expect(doc.numpages).to.equal(numExpectedPages)
      expect(clean(doc.text)).to.include('SkillTree Transcript')
      expect(clean(doc.text)).to.include(projName)
      expect((clean(doc.text).match(new RegExp(expectedHeaderAndFooter, 'g')) || []).length).to.equal(numExpectedPages*2)
      expect(clean(doc.text)).to.not.include(expectedHeaderAndFooterCommunityProtected)

      expect(clean(doc.text)).to.not.include('null')
    })
  })

  if (!Cypress.env('oauthMode')) {
    it('ability to configure footer and header text - community protected', () => {
      cy.intercept('GET', '/public/config', (req) => {
        req.reply((res) => {
          const conf = res.body;
          conf.exportHeaderAndFooter = 'For {{community.project.descriptor}} enjoyment';
          res.send(conf);
        });
      })
      const allDragonsUser = 'allDragons@email.org'
      cy.fixture('vars.json').then((vars) => {
        cy.logout();
        cy.login(vars.rootUser, vars.defaultPass, true);
        cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
        cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
        cy.logout();

        cy.register(allDragonsUser, vars.defaultPass);
        cy.logout();

        cy.login(vars.defaultUser, vars.defaultPass);
      });

      const projName = `Footer and Header for User Community`
      cy.createProject(1, { name: projName, enableProtectedUserCommunity: true })
      cy.request('POST', '/app/userInfo', {
        'first': 'Joe',
        'last': 'Doe',
        'nickname': 'Joe Doe'
      })
      cy.createSubject(1)
      cy.createSkill(1, 1, 1)
      cy.createSkill(1, 1, 2)
      cy.createSkill(1, 1, 3)

      cy.visit('/test-skills-client/proj1')
      cy.wrapIframe().find('[data-cy="downloadTranscriptBtn"]').click()
      const numExpectedPages = 2
      cy.readTranscript(projName).then((doc) => {
        expect(doc.numpages).to.equal(numExpectedPages)
        expect(clean(doc.text)).to.include('SkillTree Transcript')
        expect(clean(doc.text)).to.include(projName)
        expect(clean(doc.text)).to.not.include(expectedHeaderAndFooter)
        expect((clean(doc.text).match(new RegExp(expectedHeaderAndFooterCommunityProtected, 'g')) || []).length).to.equal(numExpectedPages * 2)

        expect(clean(doc.text)).to.not.include('null')
      })
    })
  }
})