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

describe('Community Quiz Description Validation Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();
            cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${Cypress.env('proxyUser')}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.loginAsAdminUser();
        });
    });

    it('quiz description is validated against custom validators', () => {
        cy.viewport(1200, 1400)
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions').as('loadQuestions');
        cy.intercept('GET', '/admin/quiz-definitions/quiz1').as('loadQuizDef');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuestions');

        cy.get('[data-cy="editQuizButton"]').click()
        cy.wait('@loadQuizDef');

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'ldkj aljdl aj\n\njabberwocky')
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}')
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'ldkj aljdl aj\n\ndivinedragon')
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').contains('Quiz/Survey Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{backspace}')
        cy.wait('@validateDescription');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('question text is validated against custom validators', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})
        cy.createQuizDef(2, {enableProtectedUserCommunity: false})

        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions').as('loadQuiz1Questions');
        cy.intercept('POST', '/admin/quiz-definitions/quiz1/create-question').as('saveQuiz1Question');
        cy.intercept('GET', '/admin/quiz-definitions/quiz2/questions').as('loadQuiz2Questions');
        cy.intercept('POST', '/admin/quiz-definitions/quiz2/create-question').as('saveQuiz2Question');

        // navigate directly to quiz 1
        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuiz1Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Question - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz1Question');

        // navigate using ui to quiz 2
        cy.get('[data-cy=breadcrumb-Quizzes]').click();
        cy.get('[data-cy="managesQuizBtn_quiz2"]').click()
        cy.wait('@loadQuiz2Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()


        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').contains('Question - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz2Question');

        // navigate directly back to quiz 1
        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuiz1Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('5')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Question - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz1Question');

        // navigate directly back to quiz 2
        cy.visit('/administrator/quizzes/quiz2');
        cy.wait('@loadQuiz2Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('5')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').contains('Question - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz2Question');

        // finally, navigate back to quiz 1 again using ui controls
        cy.get('[data-cy=breadcrumb-Quizzes]').click();
        cy.get('[data-cy="managesQuizBtn_quiz1"]').click()
        cy.wait('@loadQuiz1Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('6')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('8')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Question - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz1Question');
    });

    it('answer hint text is validated against custom validators', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})
        cy.createQuizDef(2, {enableProtectedUserCommunity: false})

        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions').as('loadQuiz1Questions');
        cy.intercept('POST', '/admin/quiz-definitions/quiz1/create-question').as('saveQuiz1Question');
        cy.intercept('GET', '/admin/quiz-definitions/quiz2/questions').as('loadQuiz2Questions');
        cy.intercept('POST', '/admin/quiz-definitions/quiz2/create-question').as('saveQuiz2Question');

        // navigate directly to quiz 1
        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuiz1Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()


        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHintEnableCheckbox"]').click()

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="answerHintError"]').contains('Answer Hint - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="answerHint"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz1Question');

        // navigate using ui to quiz 2
        cy.get('[data-cy=breadcrumb-Quizzes]').click();
        cy.get('[data-cy="managesQuizBtn_quiz2"]').click()
        cy.wait('@loadQuiz2Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHintEnableCheckbox"]').click()

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="answerHintError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="answerHintError"]').contains('Answer Hint - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="answerHint"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz2Question');

        // navigate directly back to quiz 1
        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuiz1Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('5')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHintEnableCheckbox"]').click()

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="answerHintError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="answerHintError"]').contains('Answer Hint - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="answerHint"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz1Question');

        // navigate directly back to quiz 2
        cy.visit('/administrator/quizzes/quiz2');
        cy.wait('@loadQuiz2Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('5')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHintEnableCheckbox"]').click()

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="answerHintError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="answerHintError"]').contains('Answer Hint - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="answerHint"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz2Question');

        // finally, navigate back to quiz 1 again using ui controls
        cy.get('[data-cy=breadcrumb-Quizzes]').click();
        cy.get('[data-cy="managesQuizBtn_quiz1"]').click()
        cy.wait('@loadQuiz1Questions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('6')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('8')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHintEnableCheckbox"]').click()

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="answerHintError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="answerHint"]').type('{selectall}{backspace}')
        cy.get('[data-cy="answerHint"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="answerHintError"]').contains('Answer Hint - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="answerHint"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.clickSaveDialogBtn()
        cy.wait('@saveQuiz1Question');
    });

    it('Input Text answer is validated against custom validators', () => {
        cy.intercept('GET', '/api/projects/*/pointHistory').as('getPointHistory');

        cy.createQuizDef(1, {enableProtectedUserCommunity: true})
        cy.createTextInputQuestionDef(1, 1)
        cy.createQuizDef(2, {enableProtectedUserCommunity: false})
        cy.createTextInputQuestionDef(2, 1)

        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.request('POST', '/admin/projects/proj1/settings', [{
            setting: 'production.mode.enabled',
            value: 'true',
            projectId: 'proj1'
        }]);
        cy.request('POST', '/api/myprojects/proj1')

        cy.createProject(2, {enableProtectedUserCommunity: false})
        cy.createSubject(2,1)
        cy.createSkill(2, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz2',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.request('POST', '/admin/projects/proj2/settings', [{
            setting: 'production.mode.enabled',
            value: 'true',
            projectId: 'proj2'
        }]);
        cy.request('POST', '/api/myprojects/proj2')

        // navigate directly to quiz 1
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - May not contain divinedragon word');

        cy.clickCompleteQuizBtn()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - May not contain divinedragon word');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled');

        // navigate to quiz2 using ui controls
        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.get('[data-cy="project-link-proj2"]').click()
        cy.wait('@getPointHistory');
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click()
        cy.get('#skillProgressTitleLink-skill1').click()

        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #2! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}jabberwocky')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\n');
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky');

        cy.clickCompleteQuizBtn()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled');

        // navigate to quiz1 using ui controls
        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.get('[data-cy="project-link-proj1"]').click()
        cy.wait('@getPointHistory');
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click()
        cy.get('#skillProgressTitleLink-skill1').click()

        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - May not contain divinedragon word');

        cy.clickCompleteQuizBtn()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - May not contain divinedragon word');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled');

        // navigate directly back to quiz 2
        cy.visit('/test-skills-display/proj2/subjects/subj1/skills/skill1');
        cy.validatePoweredBy()
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}jabberwocky')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\n');
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky');

        cy.clickCompleteQuizBtn()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled');
    });

    it('Input Text grader response is validated against custom validators', () => {
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions').as('loadQuiz1Questions');
        cy.intercept('GET', '/admin/quiz-definitions/quiz2/questions').as('loadQuiz2Questions');

        cy.createQuizDef(1, {enableProtectedUserCommunity: true})
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)
        cy.createQuizDef(2, {enableProtectedUserCommunity: false})
        cy.createTextInputQuestionDef(2, 1)
        cy.createTextInputQuestionDef(2, 2)
        cy.createTextInputQuestionDef(2, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(2, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        // navigate directly to quiz1
        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('hi jabberwocky')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{selectall}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - May not contain divinedragon word')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markCorrectBtn"]').should('be.enabled')

        // navigate directly to quiz2
        cy.visit('/administrator/quizzes/quiz2/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('hi divinedragon')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{selectall}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markCorrectBtn"]').should('be.enabled')

        // navigate back to quiz1 using ui controls
        cy.get('[data-cy=breadcrumb-Quizzes]').click();
        cy.get('[data-cy="managesQuizBtn_quiz1"]').click()
        cy.wait('@loadQuiz1Questions');
        cy.get('[data-cy="nav-Grading"]').click()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('hi jabberwocky')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{selectall}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - May not contain divinedragon word')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')

        // reload, then complete grading for quiz1
        cy.reload()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{selectall}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - May not contain divinedragon word')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{moveToEnd}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]')

        // all 3 are graded
        cy.get('[data-cy="attemptGradedFor_user1"]')

        // navigate back to quiz2 using ui controls
        cy.get('[data-cy=breadcrumb-Quizzes]').click();
        cy.get('[data-cy="managesQuizBtn_quiz2"]').click()
        cy.wait('@loadQuiz2Questions');
        cy.get('[data-cy="nav-Grading"]').click()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('hi divinedragon')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{selectall}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')

        // reload, then complete grading for quiz2
        cy.reload()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{selectall}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - paragraphs may not contain jabberwocky')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{moveToEnd}{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]')

        // all 3 are graded
        cy.get('[data-cy="attemptGradedFor_user1"]')
    });

    it('video transcript is validated against custom validators', () => {
        cy.intercept('GET', '/api/projects/*/pointHistory').as('getPointHistory');

        cy.createQuizDef(1)
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/administrator/quizzes/quiz1/')
        cy.get('[data-cy="add-video-question-1"]').click()

        const videoFile = 'create-subject.webm';

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/${videoFile}`,  { force: true })

        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').type('{selectall}{backspace}')
        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled');

        cy.get('[data-cy="videoTranscript"]').type('{backspace}');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled');

    });

    it('video transcript is validated against custom validators - uc protected', () => {
        cy.intercept('GET', '/api/projects/*/pointHistory').as('getPointHistory');

        cy.createQuizDef(1, {enableProtectedUserCommunity: true})
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/administrator/quizzes/quiz1/')
        cy.get('[data-cy="add-video-question-1"]').click()

        const videoFile = 'create-subject.webm';

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/${videoFile}`,  { force: true })

        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').type('{selectall}{backspace}')
        cy.get('[data-cy="videoTranscript"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript - May not contain divinedragon word');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled');

        cy.get('[data-cy="videoTranscript"]').type('{backspace}');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled');

    });

});
