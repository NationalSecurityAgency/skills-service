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

describe('Configure Video Tests', () => {

    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/video').as('getVideoProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        Cypress.Commands.add("visitVideoConfPage", (projNum) => {
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
            cy.wait('@getVideoProps')
            cy.wait('@getSkillInfo')
            cy.get('.spinner-border').should('not.exist')
        });
    });

    it('resize video setting', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)
        cy.visitVideoConfPage();

        cy.wait(1000)
        cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
        cy.get('[data-cy="updateVideoSettings"]').should('not.exist')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')

        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get('[data-cy="defaultVideoSize"]').contains('705 x 488')
        cy.get('[data-cy="updateVideoSettings"]').should('exist')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '705px')

        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get('[data-cy="defaultVideoSize"]').contains('687 x 475')
        cy.get('[data-cy="updateVideoSettings"]').should('exist')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '687px')

        cy.get('[data-cy="updateVideoSettings"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="defaultVideoSize"]').contains('687 x 475')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '687')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
    });

    it('resize the video using keyboard', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)
        cy.visitVideoConfPage();

        cy.wait(1000)
        cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
        cy.get('[data-cy="updateVideoSettings"]').should('not.exist')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')

        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="defaultVideoSize"]').contains('672 x 465')
        cy.get('[data-cy="updateVideoSettings"]').should('exist')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '672px')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')

        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="defaultVideoSize"]').contains('622 x 430')
        cy.get('[data-cy="updateVideoSettings"]').should('exist')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '622px')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')

        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{rightArrow}')
        cy.get('[data-cy="defaultVideoSize"]').contains('672 x 464')
        cy.get('[data-cy="updateVideoSettings"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="defaultVideoSize"]').contains('672 x 464')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '672px')
    });

    it('video on skills-display skill page uses configured default unless overridden by the user', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)
        cy.visitVideoConfPage();

        cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="defaultVideoSize"]').contains('622 x 430')
        cy.get('[data-cy="updateVideoSettings"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', '622px')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '622px')

        // user overrides the video size
        cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '572px')

        // user-set new size is used
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '572px')
    });

    it('video on skills-display size is controlled by the user', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#skillVideoFor-proj1-skill1Container')

        cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '771px')

        // user-set new size is used
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '771px')
    });

    it('playing the video removes the resize button', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#skillVideoFor-proj1-skill1Container')

        cy.get('[data-cy="videoResizeHandle"]')
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.get('[data-cy="videoResizeHandle"]').should('not.exist')
        cy.get('[data-cy="videoPlayer"] [title="Pause"]').click()
        cy.get('[data-cy="videoResizeHandle"]')
    });

    it('player will resize after playing then pausing the video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#skillVideoFor-proj1-skill1Container')


        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '886px')

        cy.get('[data-cy="videoResizeHandle"]')
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.get('[data-cy="videoResizeHandle"]').should('not.exist')
        cy.wait(1000)
        cy.get('[data-cy="videoPlayer"] [title="Pause"]').click()
        cy.get('[data-cy="videoResizeHandle"]')

        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '868px')
    });

});
