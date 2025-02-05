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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Quiz CRUD Tests', () => {

    const quizTableSelector = '[data-cy="quizDefinitionsTable"]';
    beforeEach(() => {

    });

    it('create a quiz and a survey', function () {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('[data-cy="quizName"]').type('My First Quiz')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstQuiz')

        cy.get('[data-cy="quizDescription"]').type('Some cool Description')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').should('have.focus')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="quizName"]').type('My First Survey')
        // cy.get('[data-cy="quizTypeSelector"]').select('Survey')
        cy.get('[data-cy="quizTypeSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="optionlabel"]').contains('Survey').click()
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').should('have.focus')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
            [{
                colIndex: 0,
                value: 'My First Survey'
            }, {
                colIndex: 1,
                value: 'Survey'
            }],
        ], 5);


        // refresh and revalidate
        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
            [{
                colIndex: 0,
                value: 'My First Survey'
            }, {
                colIndex: 1,
                value: 'Survey'
            }],
        ], 5);
    });

    it('Quiz Modal Validation: Name', function () {
        cy.createSurveyDef(1, { name: 'Already Exist' });

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // name is not taken
        cy.get('[data-cy="quizName"]').type('Already Exist')
        cy.get('[data-cy="quizNameError"]').contains('The value for the Quiz/Survey Name is already taken')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // min 3 chars
        cy.get('[data-cy="quizName"]').clear().type('ab')
        cy.get('[data-cy="quizNameError"]').contains('Quiz/Survey Name must be at least 3 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('c')
        cy.get('[data-cy="quizNameError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // max 75 chars
        const longName = new Array(75).join('A');
        cy.get('[data-cy="quizName"]').clear().fill(longName)
        cy.get('[data-cy="quizName"]').type('AA')
        cy.get('[data-cy="quizNameError"]').contains('Quiz/Survey Name must be at most 75 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('{backspace}')
        cy.get('[data-cy="quizNameError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // required field
        cy.get('[data-cy="quizName"]').clear()
        cy.get('[data-cy="quizNameError"]').contains('Quiz/Survey Name is a required field')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        
        // custom validators
        cy.get('[data-cy="quizName"]')
          .type('{selectall}(A) Updated Quiz/Survey Name');
        cy.get('[data-cy="quizNameError"]')
          .contains('names may not contain (A)');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.disabled');

        cy.get('[data-cy="quizName"]')
          .type('{selectall}(B) A Updated Quiz/Survey Name');
        cy.get('[data-cy="quizNameError"]')
          .invoke('text').invoke('trim').should('equal', '');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');
    });

    it('Quiz Modal Validation: Quiz Id', function () {
        cy.createSurveyDef(1);

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('Good')
        cy.get('[data-cy="quizNameError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // id is not taken
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="idInputValue"]').clear().type('quiz1');
        cy.get('[data-cy="idError"]').contains('The value for the Quiz/Survey ID is already taken')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // at least 3 chars
        cy.get('[data-cy="idInputValue"]').clear().type('ab');
        cy.get('[data-cy="idError"]').contains('Quiz/Survey ID must be at least 3 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="idInputValue"]').type('c');
        cy.get('[data-cy="idError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // no more than 100 chars
        const longId = new Array(100).join('A');
        cy.get('[data-cy="idInputValue"]').clear().fill(longId);
        cy.get('[data-cy="idInputValue"]').type('bb');
        cy.get('[data-cy="idError"]').contains('Quiz/Survey ID must be at most 100 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // required field
        cy.get('[data-cy="idInputValue"]').clear()
        cy.get('[data-cy="idError"]').contains('Quiz/Survey ID is a required field')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('Quiz Modal Validation: description', function () {
        cy.createSurveyDef(1);

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('Good')
        cy.get('[data-cy="quizNameError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // custom description validation
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="quizDescription"]').type('a jabberwocky b');
        cy.get('[data-cy="descriptionError"]').contains('Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

    });

    it('quiz name search / filtering', function () {
        cy.createSurveyDef(1, { name: 'a Quiz 1' });
        cy.createQuizDef(2, { name: 'b Survey 1' });
        cy.createSurveyDef(3, { name: 'c Quiz 2' });
        cy.createQuizDef(4, { name: 'd Survey 2' });
        cy.createSurveyDef(5, { name: 'e Quiz 3' });
        cy.createQuizDef(6, { name: 'f Survey 3' });

        cy.visit('/administrator/quizzes/')

        // sort by name
        const headerSelector = `${quizTableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('Name')
            .click();

        cy.get(`${quizTableSelector}`).find('[data-cy="skillsBTableTotalRows"]').should('have.text', 6)
        cy.get('[data-cy="quizNameFilter"]').type('3')
        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 2)

        cy.get(`${quizTableSelector} [data-p-index="0"] [data-cy="managesQuizLink_quiz5"]`)
        cy.get(`${quizTableSelector} [data-p-index="1"] [data-cy="managesQuizLink_quiz6"]`)
        cy.get(`${quizTableSelector} [data-p-index="3"]`).should('not.exist')

        cy.get('[data-cy="quizResetBtn"]').click()
        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
        cy.get('[data-cy="quizNameFilter"]').should('have.value', '')
        cy.get('[data-cy="quizNameFilter"]').type('sUrVeY{enter}')

        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 3)

        cy.get(`${quizTableSelector} [data-p-index="0"] [data-cy="managesQuizLink_quiz2"]`)
        cy.get(`${quizTableSelector} [data-p-index="1"] [data-cy="managesQuizLink_quiz4"]`)
        cy.get(`${quizTableSelector} [data-p-index="2"] [data-cy="managesQuizLink_quiz6"]`)
        cy.get(`${quizTableSelector} [data-p-index="3"]`).should('not.exist')

        cy.get('[data-cy="quizNameFilter"]').type('{backspace}{backspace}{backspace}{backspace}{backspace}{backspace}{enter}')
        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
    });

    it('edit existing quiz', function () {
        cy.createQuizDef(1);
        cy.createSurveyDef(2);

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
        cy.get('[data-cy="quizTypeSection"]').contains('Can only be modified for a new quiz/survey')

        cy.get('[data-cy="quizName"]').type('A')
        cy.get('[data-cy="quizDescription"]').type('A')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')

        cy.get(`${quizTableSelector} [data-p-index="0"]`).contains('This is quiz 1A')
        cy.get('[data-cy="editQuizButton_quiz1"]').should('have.focus')

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1A')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!A')
        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
    });

    it('edit existing quiz\'s id', function () {
        cy.createQuizDef(1);
        cy.createSurveyDef(2);

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="editQuizButton_quiz2"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is survey 2')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz2')
        cy.get('[data-cy="quizDescription"]').contains('What a cool survey #2! Thank you for taking it!')
        cy.get('[data-cy="quizTypeSelector"]').contains('Survey')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
        cy.get('[data-cy="quizTypeSection"]').contains('Can only be modified for a new quiz/survey')

        cy.get('[data-cy="managesQuizLink_quiz2"]').should('exist')
        cy.get('[data-cy="managesQuizBtn_quiz2"]').should('exist')

        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idInputValue"]').type('A');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')

        cy.get('[data-cy="managesQuizLink_quiz2"]').should('not.exist')
        cy.get('[data-cy="managesQuizBtn_quiz2"]').should('not.exist')
        cy.get('[data-cy="managesQuizLink_quiz2A"]')
        cy.get('[data-cy="managesQuizBtn_quiz2A"]')

        cy.get('[data-cy="managesQuizLink_quiz2A"]').click()
        cy.get('[data-cy="pageHeader"]').contains('This is survey 2')

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="managesQuizLink_quiz2A"]')
        cy.get('[data-cy="managesQuizBtn_quiz2A"]')
    });

    it('edit quiz on the quiz page', function () {
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
        cy.get('[data-cy="quizTypeSection"]').contains('Can only be modified for a new quiz/survey')

        cy.get('[data-cy="quizName"]').type('A')
        cy.get('[data-cy="quizDescription"]').type('A')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')

        cy.get('[data-cy="pageHeader"]').contains('This is quiz 1A')
        cy.get('[data-cy="editQuizButton"]').should('have.focus')

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1A')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!A')
        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
    });

    it('edit quiz id on the quiz page', function () {
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
        cy.get('[data-cy="quizTypeSection"]').contains('Can only be modified for a new quiz/survey')

        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idInputValue"]').type('A');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')

        cy.get('[data-cy="pageHeader"]').contains('This is quiz 1')
        cy.get('[data-cy="editQuizButton"]').should('have.focus')

        cy.url()
            .should('include', '/administrator/quizzes/quiz1A');

        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1A')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
    });

    it('quiz id is derived from name', function () {
        const expectedId = 'LotsofspecialPchars';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";

        cy.visit('/administrator/quizzes')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()

        cy.get('[data-cy="quizName"]').type(providedName, { delay: 100 })
        cy.get('[data-cy="idInputValue"]').should('have.value', expectedId)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')

        // id is not derived from name during edit
        cy.get(`[data-cy="editQuizButton_${expectedId}"]`).click()
        cy.get('[data-cy="idInputValue"]').should('have.value', expectedId)
        cy.get('[data-cy="quizName"]').type('More', { delay: 100 })
        cy.get('[data-cy="idInputValue"]').should('have.value', expectedId)
    });

    it('edit quiz modal close and cancel focus on the edit button', function () {
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('exist')

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.get('[data-cy="editQuizButton_quiz1"]').should('have.focus')

        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('exist')

        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.get('[data-cy="editQuizButton_quiz1"]').should('have.focus')
    });

    it('delete quiz', function () {
        cy.createQuizDef(1);
        cy.createSurveyDef(2);
        cy.createQuizDef(3);
        cy.createSurveyDef(4);

        cy.visit('/administrator/quizzes/')

        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 4)
        cy.get('[data-cy="deleteQuizButton_quiz1"]').should('exist')
        cy.get('[data-cy="deleteQuizButton_quiz2"]').should('exist')
        cy.get('[data-cy="deleteQuizButton_quiz3"]').should('exist')
        cy.get('[data-cy="deleteQuizButton_quiz4"]').should('exist')

        cy.get('[data-cy="deleteQuizButton_quiz2"]').click()
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove This is survey 2 Survey')
        cy.get('[data-cy="currentValidationText"]').type('Delete Me', {delay: 0})
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 3)
        cy.get('[data-cy="deleteQuizButton_quiz1"]').should('exist')
        cy.get('[data-cy="deleteQuizButton_quiz2"]').should('not.exist')
        cy.get('[data-cy="deleteQuizButton_quiz3"]').should('exist')
        cy.get('[data-cy="deleteQuizButton_quiz4"]').should('exist')

        // the delete quiz button that was last clicked before closing the modal has focus which I believe is correct
        // cy.get('[data-cy="btn_Quizzes And Surveys"]').should('have.focus')

        cy.get('[data-cy="deleteQuizButton_quiz1"]').click()
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove This is quiz 1 Quiz')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="deleteQuizButton_quiz1"]').should('have.focus')
    });

    it('not allowed to delete quiz if associated to skills', function () {
        cy.createQuizDef(1);
        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="deleteQuizButton_quiz1"]').click()
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('Cannot remove the quiz since it is currently assigned to 1 skill')
        cy.get('[data-cy="currentValidationText"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
    });

    it('closing modal returns focus on the "New Quiz/Survey button"', function () {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="btn_Quizzes And Surveys"]').should("have.focus")

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="btn_Quizzes And Surveys"]').should("have.focus")
    });

    it('edit quiz description with block quotes on the quiz page', function () {
        cy.intercept('POST', '/admin/quiz-definitions/quiz1').as('saveQuiz');
        // ignore warning 'TextSelection endpoint not pointing into a node with inline content (blockQuote)'
        Cypress.env('ignoreConsoleWarnings', true);
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
        cy.get('[data-cy="quizTypeSelector"]').contains('Quiz')
        cy.get('[data-cy="quizTypeSelector"]').should('have.class', 'p-disabled')
        cy.get('[data-cy="quizTypeSection"]').contains('Can only be modified for a new quiz/survey')

        cy.get('button.quote').click({force: true})
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@saveQuiz')

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
        cy.get('[data-cy="quizDescription"] div.ProseMirror.toastui-editor-contents blockquote p').should('contain', 'What a cool quiz #1! Thank you for taking it!')
        cy.get('[data-cy="quizDescription"] div.ProseMirror.toastui-editor-contents blockquote p').should('not.contain', '> What a cool quiz #1! Thank you for taking it!')
    });

    it('sort column and order is saved in local storage', () => {
        cy.createQuizDef(1, { name: 'a Quiz 1' });
        cy.createSurveyDef(2, { name: 'b Survey 1' });
        cy.createQuizDef(3, { name: 'c Quiz 2' });
        cy.createSurveyDef(4, { name: 'd Survey 2' });
        cy.createQuizDef(5, { name: 'e Quiz 3' });

        cy.visit('/administrator/quizzes/')

        // initial sort order
        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'a Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'b Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'c Quiz 2' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'd Survey 2' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'e Quiz 3' }, { colIndex: 1, value: 'Quiz' }],
        ], 5);

        // sort by type
        const headerSelector = `${quizTableSelector} thead tr th`;
        cy.get(headerSelector)
          .contains('Type')
          .click();

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'e Quiz 3' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'c Quiz 2' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'a Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'd Survey 2' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'b Survey 1' }, { colIndex: 1, value: 'Survey' }],
        ], 5);

        cy.get(headerSelector)
          .contains('Type')
          .click();

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'd Survey 2' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'b Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'e Quiz 3' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'c Quiz 2' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'a Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'd Survey 2' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'b Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'e Quiz 3' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'c Quiz 2' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'a Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
        ], 5);
    });

    it('copy an empty quiz and an empty survey', function () {
        cy.createQuizDef(1, { name: 'Quiz 1' });
        cy.createSurveyDef(2, { name: 'Survey 1' });

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Quiz 1')

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'Copy of Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
        ], 5);

        cy.get('[data-cy="copyQuizButton_quiz2"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Survey 1')

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'Copy of Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Copy of Survey 1' }, { colIndex: 1, value: 'Survey' }],
        ], 5);
    });

    it('copy a quiz and a survey with questions', function () {
        cy.createQuizDef(1, { name: 'Quiz 1' });
        cy.createSurveyDef(2, { name: 'Survey 1' });

        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);
        cy.createTextInputQuestionDef(2, 2);
        cy.createRatingQuestionDef(2, 3);

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Quiz 1')

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'Copy of Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
        ], 5);

        cy.get('[data-cy="copyQuizButton_quiz2"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Survey 1')

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'Copy of Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Copy of Survey 1' }, { colIndex: 1, value: 'Survey' }],
        ], 5);

        cy.get('[data-cy="managesQuizLink_Copyofquiz1"]').click()
        cy.get('[data-cy="questionDisplayCard-1"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-2"]').contains('This is a question # 2')

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="managesQuizLink_Copyofquiz2"]').click()

        cy.get('[data-cy="questionDisplayCard-1"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-2"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-3"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="textAreaPlaceHolder"]').should('exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-pc-name="rating"]').should('exist')

    });

    it('copy a quiz and a survey with settings', function () {
        cy.createQuizDef(1, { name: 'Quiz 1' });
        cy.createSurveyDef(2, { name: 'Survey 1' });

        cy.setQuizMaxNumAttempts(1, 3);
        cy.setQuizMultipleTakes(1, true);
        cy.setQuizShowCorrectAnswers(1, true);

        cy.setQuizMultipleTakes(2, true);

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Quiz 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="copyQuizButton_quiz2"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Survey 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'Copy of Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Copy of Survey 1' }, { colIndex: 1, value: 'Survey' }],
        ], 5);

        cy.visit('/administrator/quizzes/Copyofquiz1/settings')
        cy.get('[data-cy="numAttemptsInput"] input').should('have.value', 3);
        cy.get('[data-cy="multipleTakesSwitch"] input').should('be.checked');
        cy.get('[data-cy="alwaysShowCorrectAnswersSwitch"] input').should('be.checked');

        cy.visit('/administrator/quizzes/Copyofquiz2/settings')
        cy.get('[data-cy="multipleTakesSwitch"] input').should('be.checked');

    });

    it('focus is returned to button', function () {
        cy.createQuizDef(1, { name: 'Quiz 1' });
        cy.createSurveyDef(2, { name: 'Survey 1' });

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Quiz 1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="copyQuizButton_quiz1"]').should('have.focus');

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="copyQuizButton_quiz1"]').should('have.focus');

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="closeDialogBtn"]').type('{esc}');
        cy.get('[data-cy="copyQuizButton_quiz1"]').should('have.focus');

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click()
        cy.get('[data-cy="copyQuizButton_quiz1"]').should('have.focus');

    });

    it('copying a quiz validates appropriately', function () {
        cy.createQuizDef(1, { name: 'Quiz 1' });
        cy.createSurveyDef(2, { name: 'Survey 1' });

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Quiz 1')

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.validateTable(quizTableSelector, [
            [{ colIndex: 0, value: 'Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
            [{ colIndex: 0, value: 'Survey 1' }, { colIndex: 1, value: 'Survey' }],
            [{ colIndex: 0, value: 'Copy of Quiz 1' }, { colIndex: 1, value: 'Quiz' }],
        ], 5);

        cy.get('[data-cy="copyQuizButton_quiz1"]').click()
        cy.get('[data-cy="quizName"]').should('have.value', 'Copy of Quiz 1')
        cy.get('[data-cy="saveDialogBtn"]').should('not.be.enabled')
        cy.get('[data-cy="quizNameError"]').should('exist');
        cy.get('[data-cy="quizNameError"]').contains('The value for the Quiz/Survey Name is already taken');
    });

    it('created quiz has a created date', function () {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.p-dialog-header').contains('New Quiz/Survey')

        cy.get('[data-cy="quizName"]').type('My First Quiz')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstQuiz')

        cy.get('[data-cy="quizDescription"]').type('Some cool Description')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').should('have.focus')
        cy.get(quizTableSelector).should('contain', 'a few seconds ago')
        cy.get(quizTableSelector).should('not.contain','Invalid Date')
    });
});

