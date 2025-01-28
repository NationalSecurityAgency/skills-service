describe('Discard confirmation tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
    });

    it('Discard new project creation', () => {
        cy.visit('/administrator/');

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('.p-dialog').contains('New Project').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('New Project').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard project edit', () => {
        cy.visit('/administrator/');

        cy.get('#editProjBtnproj1').click()
        cy.get('.p-dialog').contains('Editing Existing Project').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('#editProjBtnproj1').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('Editing Existing Project').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard new subject creation', () => {
        cy.visit('/administrator/projects/proj1/');

        cy.get('[data-cy=btn_Subjects]').click()
        cy.get('.p-dialog').contains('New Subject').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy=btn_Subjects]').click()
        cy.get('[data-cy="subjectName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('New Subject').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard subject edit', () => {
        cy.visit('/administrator/projects/proj1/');

        cy.get('#editBtnsubj1').click()
        cy.get('.p-dialog').contains('Editing Existing Subject').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('#editBtnsubj1').click()
        cy.get('[data-cy="subjectName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('Editing Existing Subject').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard new skill creation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy=newSkillButton]').click()
        cy.wait(100);
        cy.get('.p-dialog').contains('New Skill').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy=newSkillButton]').click()
        cy.wait(100);
        cy.get('[data-cy="skillName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('New Skill').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard skill edit', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.wait(100);
        cy.get('.p-dialog').contains('Edit Skill').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.wait(100);
        cy.get('[data-cy="skillName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('Edit Skill').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard new skill group creation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');
        cy.realPress('Escape');
        cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="name"]').type('abc');
        cy.get('[data-cy="idInputValue"]').should('have.value','abcGroup');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('[data-cy="EditSkillGroupModal"]').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    });

    it('Discard edit skill group creation', () => {
        cy.createSkillsGroup(1,1,2)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy="name"]').should('have.value','Awesome Group 2 Subj1');
        cy.get('[data-cy="EditSkillGroupModal"]').contains('Edit Skills Group');
        cy.realPress('Escape');
        cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('Edit Skills Group');
        cy.get('[data-cy="name"]').type('a');
        cy.get('[data-cy="name"]').should('have.value','Awesome Group 2 Subj1a');
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('[data-cy="EditSkillGroupModal"]').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    });

    it('Discard new badge creation', () => {
        cy.visit('/administrator/projects/proj1/badges/');
        cy.get('[data-cy="badgeCard-badge1"]')

        cy.get('[data-cy="btn_Badges"]').click()
        cy.wait(100);
        cy.get('.p-dialog').contains('New Badge').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy="btn_Badges"]').click()
        cy.wait(100);
        cy.get('[data-cy="name"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('New Badge').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard badge edit', () => {
        cy.visit('/administrator/projects/proj1/badges/');

        cy.get('#editBtnbadge1').click()
        cy.wait(100);
        cy.get('.p-dialog').contains('Editing Existing Badge').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('#editBtnbadge1').click()
        cy.wait(100);
        cy.get('[data-cy="name"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('Editing Existing Badge').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard new quiz creation', () => {
        cy.visit('/administrator/quizzes/');

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.wait(100);
        cy.get('.p-dialog').contains('New Quiz').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.wait(100);
        cy.get('[data-cy="quizName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('New Quiz').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })

    it('Discard quiz edit', () => {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/');

        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.wait(100);
        cy.get('.p-dialog').contains('Editing Existing Quiz').should('be.visible')
        cy.realPress('Escape');
        cy.get('.p-dialog').should('not.exist')

        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.wait(100);
        cy.get('[data-cy="quizName"]').type('one')
        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').should('have.focus')
        cy.get('[data-pc-name="rejectbutton"]').click()
        cy.get('.p-dialog').contains('You have unsaved changes').should('not.exist')
        cy.get('.p-dialog').contains('Editing Existing Quiz').should('be.visible')

        cy.realPress('Escape');
        cy.get('.p-dialog').contains('You have unsaved changes').should('exist')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('.p-dialog').should('not.exist')
    })
})