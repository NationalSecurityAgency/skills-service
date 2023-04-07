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
describe('Save State Tests', () => {

  beforeEach(() => {
    cy.createProject(1, { name: "proj1" })
    cy.createSubject(1, 1, { name: "Subject 1" })
  });

  it('Saves and discards new badge state', () => {
    cy.intercept('GET', '/admin/projects/proj1/badges').as('loadBadges');
    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');
    cy.clickButton('Badge');

    cy.get('#badgeName').type('New Badge');
    cy.get('[data-cy="markdownEditorInput"]').type('test description');

    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');
    cy.clickButton('Badge');

    cy.get('#badgeName').should('have.value', 'New Badge');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy=closeBadgeButton]').click();
    cy.wait(250);

    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');
    cy.clickButton('Badge');

    cy.get('#badgeName').should('have.value', '');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
  })

  it('Saves and discards edit badge state', () => {
    cy.intercept('GET', '/admin/projects/proj1/badges').as('loadBadges');
    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');
    cy.clickButton('Badge');

    cy.intercept('POST', '/admin/projects/proj1/badges/NewBadgeBadge').as('saveBadge');

    cy.get('#badgeName').type('New Badge');
    cy.get('[data-cy="markdownEditorInput"]').type('test description');
    cy.get('[data-cy=saveBadgeButton]').click();
    cy.wait('@saveBadge');

    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');

    cy.get('[data-cy="editBtn"]').click()
    cy.get('#badgeName').should('have.value', 'New Badge');
    cy.get('#badgeName').type(' Edit');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy="markdownEditorInput"]').type(' edit');
    cy.wait(250);

    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');

    cy.get('[data-cy="editBtn"]').click()
    cy.get('#badgeName').should('have.value', 'New Badge Edit');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description edit');
    cy.get('[data-cy=closeBadgeButton]').click();
    cy.wait(250);

    cy.visit('/administrator/projects/proj1/badges');
    cy.wait('@loadBadges');

    cy.get('[data-cy="editBtn"]').click()
    cy.get('#badgeName').should('have.value', 'New Badge');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
  })

  it('Saves and discards new project state', () => {
    cy.intercept('GET', '/app/projects')
       .as('loadProjects');
    cy.visit('/administrator/projects/');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="newProjectButton"]').click();
    cy.get('[data-cy="projectName"]').type('Test Project')
    cy.get('[data-cy="markdownEditorInput"]').type('test description');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="newProjectButton"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', 'Test Project');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy=closeProjectButton]').click();

    cy.get('[data-cy="newProjectButton"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', '');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
  })

  it('Saves and discards edit project state', () => {
    cy.intercept('GET', '/app/projects')
       .as('loadProjects');
    cy.visit('/administrator/projects/');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="editProjBtn"]').click();
    cy.get('[data-cy="projectName"]').type(' With Edits')
    cy.get('[data-cy="markdownEditorInput"]').type('description with edits');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="editProjBtn"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', 'proj1 With Edits');
    cy.get('[data-cy="markdownEditorInput"]').contains('description with edits');
    cy.get('[data-cy=closeProjectButton]').click();

    cy.get('[data-cy="editProjBtn"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', 'proj1');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
  })

  it('Saves and discards copy project state', () => {
    cy.intercept('GET', '/app/projects')
       .as('loadProjects');
    cy.visit('/administrator/projects/');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
    cy.get('[data-cy="projectName"]').type('Copy Proj With Edits')
    cy.get('[data-cy="markdownEditorInput"]').type('description with edits');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', 'Copy Proj With Edits');
    cy.get('[data-cy="markdownEditorInput"]').contains('description with edits');
    cy.get('[data-cy=closeProjectButton]').click();

    cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', '');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
  })


  it('Saves and discards new skill state', () => {
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy="newSkillButton"]').click();
    cy.get('[data-cy="skillName"]').type('Skill One')
    cy.get('[data-cy="markdownEditorInput"]').type('test description');
    cy.get('[data-cy="skillPointIncrement"]').type('{selectall}11');
    cy.get('[data-cy="numPerformToCompletion"]').type('{selectall}11');
    cy.get('[data-cy=timeWindowCheckbox]').click({force: true});
    cy.get('[data-cy="timeWindowHours"]').type('{selectall}11');
    cy.get('[data-cy="timeWindowMinutes"]').type('{selectall}11');
    cy.get('[data-cy="maxOccurrences"]').type('{selectall}11');
    cy.get('[data-cy="selfReportEnableCheckbox"]').check({ force: true });

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy="newSkillButton"]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Skill One');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy="skillPointIncrement"]').should('have.value', '11');
    cy.get('[data-cy="numPerformToCompletion"]').should('have.value', '11');
    cy.get('[data-cy="timeWindowHours"]').should('have.value', '11');
    cy.get('[data-cy="timeWindowMinutes"]').should('have.value', '11');
    cy.get('[data-cy="maxOccurrences"]').should('have.value', '11');
    cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
    cy.get('[data-cy=closeSkillButton]').click();

    cy.get('[data-cy="newSkillButton"]').click();
    cy.get('[data-cy="skillName"]').should('have.value', '');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
    cy.get('[data-cy="skillPointIncrement"]').should('have.value', '100');
    cy.get('[data-cy="numPerformToCompletion"]').should('have.value', '1');
    cy.get('[data-cy="timeWindowHours"]').should('have.value', '8');
    cy.get('[data-cy="timeWindowMinutes"]').should('have.value', '0');
    cy.get('[data-cy="maxOccurrences"]').should('have.value', '1');
    cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
  })

  it('Saves and discards edit skill state', () => {
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject');

    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/SkillOneSkill').as('saveSkill');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy="newSkillButton"]').click();
    cy.get('[data-cy="skillName"]').type('Skill One')
    cy.get('[data-cy="markdownEditorInput"]').type('test description');
    cy.get('[data-cy="skillPointIncrement"]').type('{selectall}11');
    cy.get('[data-cy="numPerformToCompletion"]').type('{selectall}11');
    cy.get('[data-cy=timeWindowCheckbox]').click({force: true});
    cy.get('[data-cy="timeWindowHours"]').type('{selectall}11');
    cy.get('[data-cy="timeWindowMinutes"]').type('{selectall}11');
    cy.get('[data-cy="maxOccurrences"]').type('{selectall}11');
    cy.get('[data-cy="selfReportEnableCheckbox"]').check({ force: true });
    cy.get('[data-cy=saveSkillButton]').click();
    cy.wait('@saveSkill');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy=editSkillButton_SkillOneSkill]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Skill One');
    cy.get('[data-cy="skillName"]').type(' Two Three')
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy="markdownEditorInput"]').type(' for storage');
    cy.get('[data-cy="skillPointIncrement"]').type('22');
    cy.get('[data-cy="numPerformToCompletion"]').type('22');
    cy.get('[data-cy="timeWindowHours"]').type('22');
    cy.get('[data-cy="timeWindowMinutes"]').type('22');
    cy.get('[data-cy="maxOccurrences"]').type('22');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy=editSkillButton_SkillOneSkill]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Skill One Two Three');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description for storage');
    cy.get('[data-cy="skillPointIncrement"]').should('have.value', '1122');
    cy.get('[data-cy="numPerformToCompletion"]').should('have.value', '1122');
    cy.get('[data-cy="timeWindowHours"]').should('have.value', '1122');
    cy.get('[data-cy="timeWindowMinutes"]').should('have.value', '1122');
    cy.get('[data-cy="maxOccurrences"]').should('have.value', '1122');

    cy.get('[data-cy=closeSkillButton]').click();

    cy.get('[data-cy=editSkillButton_SkillOneSkill]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Skill One');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy="skillPointIncrement"]').should('have.value', '11');
    cy.get('[data-cy="numPerformToCompletion"]').should('have.value', '11');
    cy.get('[data-cy="timeWindowHours"]').should('have.value', '11');
    cy.get('[data-cy="timeWindowMinutes"]').should('have.value', '11');
    cy.get('[data-cy="maxOccurrences"]').should('have.value', '11');
  })

  it('Saves and discards copy skill state', () => {
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject');

    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/SkillOneSkill').as('saveSkill');
    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy="newSkillButton"]').click();
    cy.get('[data-cy="skillName"]').type('Skill One')
    cy.get('[data-cy="markdownEditorInput"]').type('test description');
    cy.get('[data-cy=saveSkillButton]').click();
    cy.wait('@saveSkill');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy=copySkillButton_SkillOneSkill]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Copy of Skill One');
    cy.get('[data-cy="skillName"]').type(' Two Three')
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy="markdownEditorInput"]').type(' for storage');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy=copySkillButton_SkillOneSkill]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Copy of Skill One Two Three');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description for storage');

    cy.get('[data-cy=closeSkillButton]').click();

    cy.get('[data-cy=copySkillButton_SkillOneSkill]').click();
    cy.get('[data-cy="skillName"]').should('have.value', 'Copy of Skill One');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
  })

  it('new skill state should not affect copy skill state', function () {
    cy.createQuizDef(1);
    cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]').click()
    cy.get('[data-cy="skillName"]').type('save')

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="copySkillButton_skill1"]').click()
    cy.get('[data-cy="quizSelected-quiz1"]')
  });

  it('Saves and discards new subject state', () => {
    cy.visit('/administrator/projects/proj1/');

    cy.get('[data-cy=btn_Subjects]').click();
    cy.get('[data-cy="subjectNameInput"]').type('Subject One')
    cy.get('[data-cy="markdownEditorInput"]').type('test description');

    cy.visit('/administrator/projects/proj1/');

    cy.get('[data-cy=btn_Subjects]').click();
    cy.get('[data-cy="subjectNameInput"]').should('have.value', 'Subject One');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.get('[data-cy=closeSubjectButton]').click();

    cy.get('[data-cy=btn_Subjects]').click();
    cy.get('[data-cy="subjectNameInput"]').should('have.value', '');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
  })

  it('Saves and discards edit subject state', () => {
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy=btn_edit-subject]').click();
    cy.get('[data-cy="subjectNameInput"]').should('have.value', 'Subject 1');
    cy.get('[data-cy="subjectNameInput"]').type(' Two Three')
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
    cy.get('[data-cy="markdownEditorInput"]').type('test description for storage');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[data-cy=btn_edit-subject]').click();
    cy.get('[data-cy="subjectNameInput"]').should('have.value', 'Subject 1 Two Three');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description for storage');

    cy.get('[data-cy=closeSubjectButton]').click();

    cy.get('[data-cy=btn_edit-subject]').click();
    cy.get('[data-cy="subjectNameInput"]').should('have.value', 'Subject 1');
    cy.get('[data-cy="markdownEditorInput"]').should('have.value', '');
  })

  it('Redirect to error page does not show discard popup', () => {
    cy.intercept('GET', '/app/projects')
       .as('loadProjects');
    cy.visit('/administrator/projects/');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="newProjectButton"]').click();
    cy.get('[data-cy="projectName"]').type('Test Project')
    cy.get('[data-cy="markdownEditorInput"]').type('test description');

    cy.visit('/administrator/');
    cy.wait('@loadProjects');

    cy.get('[data-cy="newProjectButton"]').click();
    cy.get('[data-cy="projectName"]').should('have.value', 'Test Project');
    cy.get('[data-cy="markdownEditorInput"]').contains('test description');
    cy.visit('/error');

    cy.contains('Discard Changes').should('not.exist');
  })


  it('Saves and discards new quiz state', () => {
    cy.visit('/administrator/quizzes/')
    cy.get('[data-cy="noQuizzesYet"]')

    cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
    cy.get('.modal-title').contains('New Quiz/Survey')

    cy.get('[data-cy="quizName"]').type('My First Quiz')
    cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstQuiz')

    cy.get('[data-cy="quizDescription"]').type('Some cool Description')

    cy.visit('/administrator/quizzes/')

    cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
    cy.get('.modal-title').contains('New Quiz/Survey')

    cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstQuiz')
    cy.get('[data-cy="quizDescription"]').contains('Some cool Description')

    cy.get('[data-cy="closeQuizButton"]').click()
    cy.get('[data-cy="btn_Quizzes And Surveys"]').click()

    cy.get('[data-cy="idInputValue"]').should('have.value', '')
    cy.get('[data-cy="quizDescription"]').should('have.value', '')
  })

  it('Saves and discards edit quiz state', () => {
    cy.createQuizDef(1);

    cy.visit('/administrator/quizzes/')

    cy.get('[data-cy="editQuizButton_quiz1"]').click()
    cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
    cy.get('[data-cy="idInputValue"]').should('have.value', 'quiz1')
    cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
    cy.get('[data-cy="quizTypeSelector"]').should('have.value','Quiz')
    cy.get('[data-cy="quizTypeSelector"]').should('be.disabled')
    cy.get('[data-cy="quizTypeSection"]').contains('Can only be modified for a new quiz/survey')

    cy.get('[data-cy="quizName"]').type(' with edits')
    cy.get('[data-cy="quizDescription"]').type(' with edits')

    cy.visit('/administrator/quizzes/')
    cy.get('[data-cy="editQuizButton_quiz1"]').click()
    cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1 with edits')
    cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it! with edits')

    cy.get('[data-cy="closeQuizButton"]').click()
    cy.get('[data-cy="editQuizButton_quiz1"]').click()

    cy.get('[data-cy="quizName"]').should('have.value','This is quiz 1')
    cy.get('[data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
  })

  it('Saves and discards new question state', () => {
    cy.createQuizDef(1);
    cy.visit('/administrator/quizzes/quiz1/')

    cy.get('[data-cy="btn_Questions"]').click()
    cy.get('.modal-title').contains('New Question')

    cy.get('[data-cy="questionText"]').type('My new quiz question')
    cy.get('[data-cy="answer-0"]').type('Answer One')
    cy.get('[data-cy="answer-1"]').type('Answer Two')

    cy.get('[data-cy="addNewAnswer"]').last().click();
    cy.get('[data-cy="answer-2"]').type('Answer Three')
    cy.get('[data-cy="addNewAnswer"]').last().click();

    cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click();

    cy.visit('/administrator/quizzes/quiz1/')

    cy.get('[data-cy="btn_Questions"]').click()
    cy.get('.modal-title').contains('New Question')

    cy.get('[data-cy="questionText"]').contains('My new quiz question')
    cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Answer One')
    cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'Answer Two')
    cy.get('[data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'Answer Three')
    cy.get('[data-cy="answer-3"] [data-cy="answerText"]').should('have.value', '')
    cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('exist');

    cy.get('[data-cy="closeQuestionBtn"]').click()
    cy.get('[data-cy="btn_Questions"]').click()

    cy.get('[data-cy="questionText"]').should('have.value', '')
    cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('have.value', '')
    cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('have.value', '')
    cy.get('[data-cy="answer-2"] [data-cy="answerText"]').should('not.exist')
    cy.get('[data-cy="answer-3"] [data-cy="answerText"]').should('not.exist')
    cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('not.exist');

  })

  it('Saves and discards edit question state', () => {
    cy.createQuizDef(1);
    cy.visit('/administrator/quizzes/quiz1/')

    cy.get('[data-cy="btn_Questions"]').click()
    cy.get('.modal-title').contains('New Question')

    cy.get('[data-cy="questionText"]').type('My new quiz question')
    cy.get('[data-cy="answer-0"]').type('Answer One')
    cy.get('[data-cy="answer-1"]').type('Answer Two')

    cy.get('[data-cy="addNewAnswer"]').last().click();
    cy.get('[data-cy="answer-2"]').type('Answer Three')
    cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click();
    cy.get('[data-cy="saveQuestionBtn"]').click();

    cy.visit('/administrator/quizzes/quiz1/')

    cy.get('[data-cy="editQuestionButton_1"]').click()
    cy.get('.modal-title').contains('Editing Existing Question')

    cy.get('[data-cy="questionText"]').type(' with edit')
    cy.get('[data-cy="answer-0"]').type(' with edit')
    cy.get('[data-cy="answer-2"]').type(' with edit')
    cy.get('[data-cy="addNewAnswer"]').last().click();
    cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').click();

    cy.visit('/administrator/quizzes/quiz1/')

    cy.get('[data-cy="editQuestionButton_1"]').click()
    cy.get('[data-cy="questionText"]').contains('My new quiz question with edit')
    cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Answer One with edit')
    cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'Answer Two')
    cy.get('[data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'Answer Three with edit')
    cy.get('[data-cy="answer-3"] [data-cy="answerText"]').should('have.value', '')
    cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('exist');
    cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('exist');

    cy.get('[data-cy="closeQuestionBtn"]').click()
    cy.get('[data-cy="editQuestionButton_1"]').click()

    cy.get('[data-cy="questionText"]').contains('My new quiz question')
    cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'Answer One')
    cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'Answer Two')
    cy.get('[data-cy="answer-2"] [data-cy="answerText"]').should('have.value', 'Answer Three')
    cy.get('[data-cy="answer-3"] [data-cy="answerText"]').should('not.exist')
    cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('exist');
  })
});