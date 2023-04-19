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
// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

import { addMatchImageSnapshotCommand } from 'cypress-image-snapshot/command';
import "cypress-audit/commands";
import './cliend-display-commands';
import 'cypress-file-upload';
import 'cypress-wait-until';
var moment = require('moment-timezone');

function terminalLog(violations) {
    violations = violations || { length: 0 };
    const { length } = violations;

    cy.task(
      'log',
      `${length} accessibility violation${
        length === 1 ? '' : 's'
      } ${length === 1 ? 'was' : 'were'} detected`
    )
    if (length > 0 ) {
        violations.forEach((v) => {
            cy.log(`Accessibility violation [${v.id}]: ${v.description}`);
        });

        // pluck specific keys to keep the table readable
        const violationData = violations.map(
          ({ id, impact, description, nodes }) => ({
              id,
              impact,
              description,
              nodes: nodes.length
          })
        )

        cy.task('table', violationData)
    }
}

addMatchImageSnapshotCommand();

Cypress.Commands.add("matchSnapshotImageForElement", (selector, maybeNameOtherwiseCommandOptions, commandOptions) => {
    cy.doMatchSnapshotImage(maybeNameOtherwiseCommandOptions, commandOptions, selector)
})

Cypress.Commands.add("matchSnapshotImage", (maybeNameOtherwiseCommandOptions, commandOptions) => {
    cy.doMatchSnapshotImage(maybeNameOtherwiseCommandOptions, commandOptions, null)
})

Cypress.Commands.add("doMatchSnapshotImage", (maybeNameOtherwiseCommandOptions, commandOptions, selector) => {
    cy.closeToasts();
    cy.wait(500);

    let options = commandOptions ? commandOptions :
        ((maybeNameOtherwiseCommandOptions && typeof maybeNameOtherwiseCommandOptions === 'object') ? maybeNameOtherwiseCommandOptions : null);
    const namePresent = maybeNameOtherwiseCommandOptions && typeof maybeNameOtherwiseCommandOptions === 'string'

    const snapDir = Cypress.env('customSnapshotsDir');
    if (snapDir) {
        options = {...options, customSnapshotsDir: snapDir }
    }

    if (namePresent) {
        if (selector) {
            cy.get(selector).matchImageSnapshot(maybeNameOtherwiseCommandOptions, options);
        } else {
            cy.matchImageSnapshot(maybeNameOtherwiseCommandOptions, options);
        }
    } else {
        if (selector) {
            cy.get(selector).matchImageSnapshot(options);
        } else {
            cy.matchImageSnapshot(options);
        }
    }
})

Cypress.Commands.add("enableProdMode", (projNum) => {
    cy.request('POST', `/admin/projects/proj${projNum}/settings/production.mode.enabled`, {
        projectId: `proj${projNum}`,
        setting: 'production.mode.enabled',
        value: 'true'
    });
});

Cypress.Commands.add("addToMyProjects", (projNum) => {
    cy.request('POST', `/api/myprojects/proj${projNum}`, {});
});
Cypress.Commands.add("removeFromMyProjects", (projNum) => {
    cy.request('DELETE', `/api/myprojects/proj${projNum}`, {});
});

Cypress.Commands.add("register", (user, pass, grantRoot, usernameForDisplay = null) => {
    return cy.request(`/app/users/validExistingDashboardUserId/${user}`)
        .then((response) => {
            if (response.body !== true) {
                if (grantRoot) {
                    cy.log(`Creating root user [${user}]`)
                    cy.request('PUT', '/createRootAccount', {
                        firstName: 'Firstname',
                        lastName: 'LastName',
                        email: user,
                        password: pass,
                        usernameForDisplay,
                    });
                    // cy.request('POST', '/grantFirstRoot');
                } else {
                    cy.log(`Creating app user [${user}]`)
                    cy.request('PUT', '/createAccount', {
                        firstName: 'Firstname',
                        lastName: 'LastName',
                        email: user,
                        password: pass,
                        usernameForDisplay,
                    });
                }
                cy.request('POST', '/logout');
            } else {
                cy.log(`User [${user}] already exist`)
            }
        });
});

Cypress.Commands.add("login", (user, pass) => {
    cy.log(`logging in as [${user}] with [${pass}]`);
    cy.request( {
        method: 'POST',
        url: '/performLogin',
        body: {
            username: user,
            password: pass
        },
        form: true,
    })
});

Cypress.Commands.add("resetEmail", () => {
    cy.request({
       method: "DELETE",
       url: "http://localhost:1081/api/emails"
    });
});

Cypress.Commands.add("createQuizDef", (quizNum = 1, overrideProps = {}) => {
    cy.request('POST', `/app/quiz-definitions/quiz${quizNum}`, Object.assign({
        quizId: `quizId${quizNum}`,
        name: `This is quiz ${quizNum}`,
        type: 'Quiz',
        description: `What a cool quiz #${quizNum}! Thank you for taking it!`
    }, overrideProps));
});

Cypress.Commands.add("createSurveyDef", (surveyNum = 1, overrideProps = {}) => {
    cy.request('POST', `/app/quiz-definitions/quiz${surveyNum}`, Object.assign({
        quizId: `quiz${surveyNum}`,
        name: `This is survey ${surveyNum}`,
        type: 'Survey',
        description: `What a cool survey #${surveyNum}! Thank you for taking it!`
    }, overrideProps));
});

Cypress.Commands.add("setQuizMaxNumAttempts", (quizNum = 1, numAttemps) => {
    cy.request('POST', `/admin/quiz-definitions/quiz${quizNum}/settings`, [{
        setting: 'quizNumberOfAttempts',
        value: `${numAttemps}`
    }]);
});
Cypress.Commands.add("setMinNumQuestionsToPass", (quizNum = 1, numQuestions) => {
    cy.request('POST', `/admin/quiz-definitions/quiz${quizNum}/settings`, [{
        setting: 'quizPassingReq',
        value: `${numQuestions}`
    }]);
});

Cypress.Commands.add("runQuizForUser", (quizNum = 1, userIdOrUserNumber, quizAttemptInfo, shouldComplete = true, userAnswerTxt = null) => {
    const userId =  Number.isInteger(userIdOrUserNumber) ? `user${userIdOrUserNumber}` : userIdOrUserNumber;
    cy.register(userId, 'password');

    cy.fixture('vars.json').then((vars) => {
        cy.logout()
        if (!Cypress.env('oauthMode')) {
            cy.log('NOT in oauthMode, using form login')
            cy.login(vars.defaultUser, vars.defaultPass);
        } else {
            cy.log('oauthMode, using loginBySingleSignOn')
            cy.loginBySingleSignOn()
        }
        cy.runQuiz(quizNum, userId, quizAttemptInfo, shouldComplete, userAnswerTxt)
    });
});

Cypress.Commands.add('runQuizForTheCurrentUser', (quizNum = 1, quizAttemptInfo) => {
    cy.fixture('vars.json')
        .then((vars) => {
            const userId = vars.defaultUser;
            cy.runQuiz(quizNum, userId, quizAttemptInfo)
        });
});

Cypress.Commands.add('runQuiz', (quizNum = 1, userId, quizAttemptInfo, shouldComplete = true, userAnswerTxt = null) => {
    const quizId = `quiz${quizNum}`;
    cy.request(`/admin/quiz-definitions/${quizId}/questions`)
        .then((response) => {
            // cy.log(JSON.stringify(response.body, null, 2));
            const questionAnswers = response.body.questions.slice(0, quizAttemptInfo.length).map((qDef, questionIndex) => {
                // cy.log(`qDef=${JSON.stringify(qDef)}, questionIndex=${questionIndex}`);
                expect(quizAttemptInfo, 'should never happen as the code selects sublist based on [quizAttemptInfo] parameter!').to.have.length.greaterThan(questionIndex)
                const answerIndexes = quizAttemptInfo[questionIndex];
                // cy.log(JSON.stringify(answerIndexes, null, 2));
                const { answers } = qDef;
                const selectedAnswerIds = answerIndexes.selectedIndex.map((aIndex) => {
                    const foundAnswer = answers[aIndex];
                    return foundAnswer.id;
                });
                const isTextInputQuestion = qDef.questionType === 'TextInput'
                let answerText = null;
                if (isTextInputQuestion) {
                    answerText = userAnswerTxt ? userAnswerTxt : `This is answer for question # ${questionIndex}`;
                }
                return { answerIds: selectedAnswerIds, isTextInputQuestion , answerText };
            }).flat();

            // cy.log(JSON.stringify(questionAnswers, null, 2));

            cy.request('POST', `/admin/quiz-definitions/${quizId}/users/${userId}/attempt`)
                .then((response) => {
                    const attemptId = response.body.id;

                    questionAnswers.forEach((answer) => {
                            answer.answerIds.forEach((answerId) => {
                                cy.request('POST', `/admin/quiz-definitions/${quizId}/users/${userId}/attempt/${attemptId}/answers/${answerId}`, { isSelected: true, answerText: answer.answerText });
                            });
                        })
                    if (shouldComplete) {
                        cy.request('POST', `/admin/quiz-definitions/${quizId}/users/${userId}/attempt/${attemptId}/complete`);
                    }
                });
        });
});

Cypress.Commands.add("createQuizQuestionDef", (quizNum = 1, questionNum = 1, overrideProps = {}) => {
    cy.request('POST', `/admin/quiz-definitions/quiz${quizNum}/create-question`, Object.assign({
        quizId: `quizId${quizNum}`,
        question: `This is a question # ${questionNum}`,
        questionType: 'SingleChoice',
        answers: [{
            answer: `Question ${questionNum} - First Answer`,
            isCorrect: questionNum == 1 || questionNum > 3 ? true : false,
        }, {
            answer: `Question ${questionNum} - Second Answer`,
            isCorrect: questionNum == 2 ? true : false,
        }, {
            answer: `Question ${questionNum} - Third Answer`,
            isCorrect: questionNum == 3 ? true : false,
        }],
    }, overrideProps));
});

Cypress.Commands.add("createQuizMultipleChoiceQuestionDef", (quizNum = 1, questionNum = 1, overrideProps = {}) => {
    cy.request('POST', `/admin/quiz-definitions/quiz${quizNum}/create-question`, Object.assign({
        quizId: `quizId${quizNum}`,
        question: `This is a question # ${questionNum}`,
        questionType: 'MultipleChoice',
        answers: [{
            answer: 'First Answer',
            isCorrect: true,
        }, {
            answer: 'Second Answer',
            isCorrect: false,
        }, {
            answer: 'Third Answer',
            isCorrect: true,
        }, {
            answer: 'Fourth Answer',
            isCorrect: false,
        }],
    }, overrideProps));
});

Cypress.Commands.add("createSurveyMultipleChoiceQuestionDef", (quizNum = 1, questionNum = 1, overrideProps = {}) => {
    cy.request('POST', `/admin/quiz-definitions/quiz${quizNum}/create-question`, Object.assign({
        quizId: `quizId${quizNum}`,
        question: `This is a question # ${questionNum}`,
        questionType: 'MultipleChoice',
        answers: [{
            answer: `Question ${questionNum} - First Answer`,
            isCorrect: false,
        }, {
            answer: `Question ${questionNum} - Second Answer`,
            isCorrect: false,
        }, {
            answer: `Question ${questionNum} - Third Answer`,
            isCorrect: false,
        }],
    }, overrideProps));
});

Cypress.Commands.add("createTextInputQuestionDef", (quizNum = 1, questionNum = 1, overrideProps = {}) => {
    cy.request('POST', `/admin/quiz-definitions/quiz${quizNum}/create-question`, Object.assign({
        quizId: `quizId${quizNum}`,
        question: `This is a question # ${questionNum}`,
        questionType: 'TextInput',
    }, overrideProps));
});


Cypress.Commands.add("createProject", (projNum = 1, overrideProps = {}) => {
    cy.request('POST', `/app/projects/proj${projNum}`, Object.assign({
        projectId: `proj${projNum}`,
        name: `This is project ${projNum}`
    }, overrideProps));
});

Cypress.Commands.add("createSubject", (projNum = 1, subjNum = 1, overrideProps = {}) => {
    cy.request('POST', `/admin/projects/proj${projNum}/subjects/subj${subjNum}`, Object.assign({
        projectId: `proj${projNum}`,
        subjectId: `subj${subjNum}`,
        name: `Subject ${subjNum}`
    }, overrideProps));
});

Cypress.Commands.add("exportSkillToCatalog", (projNum = 1, subjNum = 1, skillNum = 1) => {
    const skillId = `skill${skillNum}${subjNum > 1 ? `Subj${subjNum}` : ''}`;
    const url = `/admin/projects/proj${projNum}/skills/${skillId}/export`;
    cy.request('POST', url);
});

Cypress.Commands.add("importSkillFromCatalog", (projNum = 2, subjNum = 1, fromProjNum = 1, fromSkillNum = 1) => {
    cy.bulkImportSkillFromCatalog(projNum, subjNum, [{ projNum: fromProjNum, skillNum: fromSkillNum}])
});

Cypress.Commands.add('bulkImportSkillFromCatalog', (projNum = 2, subjNum = 1, projNumAndSkillsNumList) => {
    const url = `/admin/projects/proj${projNum}/subjects/subj${subjNum}/import`;
    const params = projNumAndSkillsNumList.map((item) => {
        return {
            projectId: `proj${item.projNum}`,
            skillId: `skill${item.skillNum}`
        };
    });
    cy.request('POST', url, params);
});

Cypress.Commands.add('bulkImportSkillsIntoGroupFromCatalog', (projNum = 2, subjNum = 1, groupNum = 1, projNumAndSkillsNumList) => {
    const url = `/admin/projects/proj${projNum}/subjects/subj${subjNum}/groups/group${groupNum}/import`;
    const params = projNumAndSkillsNumList.map((item) => {
        return {
            projectId: `proj${item.projNum}`,
            skillId: `skill${item.skillNum}`
        };
    });
    cy.request('POST', url, params);
});

Cypress.Commands.add('bulkImportSkillFromCatalogAndFinalize', (projNum = 2, subjNum = 1, projNumAndSkillsNumList) => {
    cy.bulkImportSkillFromCatalog(projNum, subjNum, projNumAndSkillsNumList);
    cy.finalizeCatalogImport(projNum);
});

Cypress.Commands.add('finalizeCatalogImportWithoutWaiting', (projNum = 1) => {
    const url = `/admin/projects/proj${projNum}/catalog/finalize`;
    cy.request('POST', url);
});

Cypress.Commands.add("finalizeCatalogImport", (projNum = 1) => {
    cy.finalizeCatalogImportWithoutWaiting(projNum);
    cy.waitUntil(() => cy.request(`/admin/projects/proj${projNum}/settings/catalog.finalize.state`).then((response) => response.body.value === "COMPLETED"), {
        timeout: 60000, // waits up to 1 minutes
        interval: 500 // performs the check every 500 ms, default to 200
    });
});


Cypress.Commands.add("acceptRemovalSafetyCheck", () => {
    cy.contains('Delete Action CANNOT be undone');
    cy.get('[data-cy="currentValidationText"]').type('Delete Me')
    cy.get('[data-cy="removeButton"]').click();
    cy.get('[data-cy="removeButton"]').should('not.exist')
});

Cypress.Commands.add("discardChanges", () => {
    cy.contains('Discard Changes').click();
});

Cypress.Commands.add("addTagToSkills", (projNum = 1, skillIds = ['skill1'], tagNum=1, overrideProps = {}) => {
    cy.request('POST', `/admin/projects/proj${projNum}/skills/tag`, Object.assign({
        tagId: `tag${tagNum}`,
        tagValue: `TAG ${tagNum}`,
        skillIds: skillIds,
    }, overrideProps));
});

const constructSkills = (projNum = 1, subjNum = 1, skillNum = 1, overrideProps = {}) => {
    const skillId = `skill${skillNum}${subjNum > 1 ? `Subj${subjNum}` : ''}`;
    const skillName = `Very Great Skill ${skillNum}${subjNum > 1 ? ` Subj${subjNum}` : ''}`;
    return Object.assign({
        projectId: `proj${projNum}`,
        subjectId: `subj${subjNum}`,
        skillId: skillId,
        name: skillName,
        pointIncrement: '100',
        numPerformToCompletion: '2',
        type: 'Skill',
    }, overrideProps);
}

Cypress.Commands.add("createSkill", (projNum = 1, subjNum = 1, skillNum = 1, overrideProps = {}) => {
    const skill = constructSkills(projNum, subjNum, skillNum, overrideProps);
    cy.request('POST', `/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/skills/${skill.skillId}`,
        constructSkills(projNum, subjNum, skillNum, overrideProps));
});

Cypress.Commands.add("createSkillsGroup", (projNum = 1, subjNum = 1, groupNum = 1, overrideProps = {}) => {
    const skillId = `group${groupNum}${subjNum > 1 ? `Subj${subjNum}` : ''}`;
    const skillName = `Awesome Group ${groupNum}${groupNum > 1 ? ` Subj${subjNum}` : ''}`;
    const payload = Object.assign({
        projectId: `proj${projNum}`,
        subjectId: `subj${subjNum}`,
        skillId: skillId,
        name: skillName,
        type: 'SkillsGroup',
    }, overrideProps);
    cy.request('POST', `/admin/projects/proj${projNum}/subjects/subj${subjNum}/skills/${skillId}`, payload);
});

Cypress.Commands.add("addSkillToGroup", (projNum = 1, subjNum = 1, groupNum = 1, skillNum = 1, overrideProps = {}) => {
    const groupId = `group${groupNum}${subjNum > 1 ? `Subj${subjNum}` : ''}`;
    const skill = constructSkills(projNum, subjNum, skillNum, overrideProps);
    cy.request('POST', `/admin/projects/${skill.projectId}/subjects/${skill.subjectId}/groups/${groupId}/skills/${skill.skillId}`,
        constructSkills(projNum, subjNum, skillNum, overrideProps));
});

Cypress.Commands.add("createBadge", (projNum = 1, badgeNum = 1, overrideProps = {}) => {
    cy.request('POST', `/admin/projects/proj${projNum}/badges/badge${badgeNum}`, Object.assign({
        projectId: `proj${projNum}`,
        badgeId: `badge${badgeNum}`,
        name: `Badge ${badgeNum}`,
        "iconClass":"fas fa-ghost",
        description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
    }, overrideProps));
});
Cypress.Commands.add("enableBadge", (projNum = 1, badgeNum = 1, overrideProps = {}) => {
    let badgeId = overrideProps.badgeId ? overrideProps.badgeId : `badge${badgeNum}`
    cy.request('POST', `/admin/projects/proj${projNum}/badges/${badgeId}`, Object.assign({
        projectId: `proj${projNum}`,
        badgeId: `badge${badgeNum}`,
        name: `Badge ${badgeNum}`,
        "iconClass":"fas fa-ghost",
        description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        enabled: 'true',
    }, overrideProps));
});

Cypress.Commands.add("assignSkillToBadge", (projNum = 1, badgeNum = 1, skillNum = 1, subjNum = 1) => {
    let skillId = `skill${skillNum}`;
    if (subjNum > 1){
        skillId = `${skillId}Subj${subjNum}`;
    }
    cy.request('POST', `/admin/projects/proj${projNum}/badge/badge${badgeNum}/skills/${skillId}`)
});

Cypress.Commands.add("createGlobalBadge", (badgeNum = 1, overrideProps = {}) => {
    cy.request('PUT', `/supervisor/badges/globalBadge${badgeNum}`, Object.assign({
        badgeId: `globalBadge${badgeNum}`,
        isEdit: false,
        name: `Global Badge ${badgeNum}`,
        originalBadgeId: '',
        iconClass: 'fas fa-award',
        description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
    }, overrideProps));
});
Cypress.Commands.add("enableGlobalBadge", (badgeNum = 1, overrideProps = {}) => {
    cy.request('PUT', `/supervisor/badges/globalBadge${badgeNum}`, Object.assign({
        badgeId: `globalBadge${badgeNum}`,
        isEdit: false,
        name: `Global Badge ${badgeNum}`,
        originalBadgeId: '',
        iconClass: 'fas fa-award',
        description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        enabled: 'true',
    }, overrideProps));
});
Cypress.Commands.add("assignSkillToGlobalBadge", (badgeNum = 1, skillNum = 1, projNum = 1) => {
    cy.request('POST', `/supervisor/badges/globalBadge${badgeNum}/projects/proj${projNum}/skills/skill${skillNum}`)
});
Cypress.Commands.add("assignProjectToGlobalBadge", (badgeNum = 1, projNum = 1, level = 1) => {
    cy.request('POST', `/supervisor/badges/globalBadge${badgeNum}/projects/proj${projNum}/level/${level}`)
});


Cypress.Commands.add("assignDep", (projNum, skillNum1, skillNum2, subj2Num=null) => {
    let skill2Id = `skill${skillNum2}`
    if (subj2Num) {
        skill2Id = `${skill2Id}Subj2`;
    }
    cy.request('POST', `/admin/projects/proj${projNum}/skills/skill${skillNum1}/dependency/${skill2Id}`);
});

Cypress.Commands.add("assignCrossProjectDep", (proj1Num, skillNum1, proj2Num, skillNum2, share=true, subj2Num=null) => {
    let skill2Id = `skill${skillNum2}`
    if (subj2Num) {
        skill2Id = `${skill2Id}Subj2`;
    }
    if (share) {
        cy.request('PUT', `/admin/projects/proj${proj2Num}/skills/${skill2Id}/shared/projects/proj${proj1Num}`);
    }
    cy.request('POST', `/admin/projects/proj${proj1Num}/skills/skill${skillNum1}/dependency/projects/proj${proj2Num}/skills/${skill2Id}`);
});


Cypress.Commands.add("doReportSkill", ({project = 1, skill = 1, subjNum = 1, userId = 'user@skills.org', date = '2020-09-12 11:00', failOnError=true, approvalRequestedMsg=null} = {}) => {
    let timestamp = null
    if (Number.isInteger(date)) {
        timestamp = date;
    } else {
        let m = moment.utc(date, 'YYYY-MM-DD HH:mm');
        if (date === 'now') {
            m = moment.utc()
        }
        if (date === 'yesterday') {
            m = moment.utc().subtract(1, 'day')
        }
        if (date === '2 days ago') {
            m = moment.utc().subtract(2, 'day')
        }
        timestamp = m.clone().format('x')
    }
    let proj = '';
    if (!isNaN(parseFloat(project))) {
        proj = `proj${project}`;
    } else {
        proj = project;
    }
    let skillId = '';
    if (!isNaN(parseFloat(skill))) {
        skillId = `skill${skill}`;
        if (subjNum > 1){
            skillId = `${skillId}Subj${subjNum}`;
        }
    } else {
        skillId = skill;
    }
    const url = `/api/projects/${proj}/skills/${skillId}`;
    const body = {userId, timestamp, approvalRequestedMsg}
    cy.log(`Report Skill Event: url=[${url}], failOnStatusCode=[${failOnError}], body: ${JSON.stringify(body)}`)
    cy.request({
        method: 'POST',
        url,
        failOnStatusCode: failOnError,
        body });
});

// deprecated, pease use doReportSkill
Cypress.Commands.add("reportSkill", (project = 1, skill = 1, userId = 'user@skills.org', date = '2020-09-12 11:00', failOnError=true, approvalRequestedMsg=null) => {
    cy.doReportSkill({ project, skill, userId, date, failOnError, approvalRequestedMsg } );
});

Cypress.Commands.add("approveRequest", (projNum = 1, requestNum = 0) => {
    cy.request(`/admin/projects/proj${projNum}/approvals?limit=10&ascending=true&page=1&orderBy=userId`)
        .then((response) => {
            cy.request('POST', `/admin/projects/proj${projNum}/approvals/approve`, {
                skillApprovalIds: [response.body.data[requestNum].id],
            });
        });
});

Cypress.Commands.add("configureApproverForSkillId", (projNum, approverUserId, skillNum) => {
    const skillId = `skill${skillNum}`;
    cy.request('POST', `/admin/projects/proj${projNum}/approverConf/${approverUserId}`, {
        skillId
    });
});
Cypress.Commands.add("configureApproverForUserTag", (projNum, approverUserId, userTagKey, userTagValue) => {
    cy.request('POST', `/admin/projects/proj${projNum}/approverConf/${approverUserId}`, {
        userTagKey,
        userTagValue
    });
});
Cypress.Commands.add("configureApproverForUser", (projNum, approverUserId, userId) => {
    cy.request('POST', `/admin/projects/proj${projNum}/approverConf/${approverUserId}`, {
        userId
    });
});

Cypress.Commands.add("getLinkFromEmail", () => {
    cy.request({
        "method":"GET",
        "url": "http://localhost:1081/api/emails"
    }).then((response) => {
        if (response.isOkStatusCode && response.body) {
            const localPart = /[http(?:s)?:\/\/^[:]+:\d+\/([^"]+)]/
            const match = response.body[0].text.match(localPart)
            if(match) {
                return match[1]
            }
            return '';
        } else {
            return '';
        }
    });
});

Cypress.Commands.add("getEmails", (expectAtLeastNumEmails = 1) => {
    const emailUrl = 'http://localhost:1081/api/emails';
    cy.waitUntil(() => cy.request(emailUrl).then((response) => response.body && response.body.length >= expectAtLeastNumEmails), {
        errorMsg: `Timed out after 2 minutes while attempting to find at least ${expectAtLeastNumEmails} emails in the test SMTP server (${emailUrl}).`,
        timeout: 120000, // waits up to 2 minutes
        interval: 1000 // performs the check every 1 second, default to 200ms
    });

    cy.request(emailUrl)
        .then((response) => {
            if (response.body) {
                return cy.wrap(response.body);
            }
            return '';
        });
});

Cypress.Commands.add('customLighthouse', () => {
    if (Cypress.env('enableLighthouse')) {
        cy.closeToasts();
        cy.wait(500);

        const lighthouseOptions = {
            extends: 'lighthouse:default',
            settings: {
                emulatedFormFactor: 'desktop',
                maxWaitForFcp: 35 * 1000,
                maxWaitForLoad: 45 * 1000,
                formFactor: 'desktop',
                screenEmulation: {
                    mobile: false,
                    disable: false,
                    width: Cypress.config('viewportWidth'),
                    height: Cypress.config('viewportHeight'),
                    deviceScaleRatio: 1,
                },
            },
        }
        cy.lighthouse({
            "performance": 0,
            "accessibility": 90,
            "best-practices": 80,
            "seo": 0,
            "pwa": 0
        }, {}, lighthouseOptions);
    }
})

Cypress.Commands.add('customPa11y', (optsObj) => {
    cy.closeToasts();
    cy.wait(500);

    // ignore heading-order for now
    // ignore multi-select plugin elements, there are a11y improvements pending for the library
    // ignore visualizations for now as those come from a 3rd party library
    // ignore datepicker a11y issues until we can identify a different library
    // ignore vue-pagination, doesn't label nav element which causes non-unique landmark regions

    let opts = {
        standard: 'Section508',
        threshold: '2',
        hideElements: '#SvgjsSvg1001, .multiselect__placeholder, .multiselect__input, .vis-network, .vdp-datepicker input, .VuePagination',
        ignore: [
            'heading-order'
        ]
    };

    if (optsObj) {
        opts = {...opts, ...optsObj};
    }

    cy.pa11y(opts);
})

Cypress.Commands.add('customA11y', ()=> {
    // ignore heading-order for now
    // ignore multi-select plugin elements, there are a11y improvements pending for the library
    // ignore visualizations for now as those come from a 3rd party library
    // ignore datepicker a11y issues until we can identify a different library
    // ignore bootstrap vue datepicker for now, doesn't meet accessibility requirements (icon creates button with no text and can't configure an aria-label)
    // have validated .accessible and .skillsBTableTotalRows with numerous a11y browser plugins, not sure why cypress axe is complaining about it
    //      but color contrast for those classes has been verified using 3rd party contrast tools
    // we can't really do anything about the apex chart a11y issues
    cy.checkA11y({
        exclude:[
            ['#SvgjsSvg1001'],
            ['#apexcharts-radialbarTrack-0'],
            ['.multiselect__placeholder'],
            ['.multiselect__input'],
            ['.multiselect__tags'],
            ['.vis-network'],
            ['.vdp-datepicker'],
            ['.VuePagination'],
            ['.b-form-datepicker'],
            ['.thead-light div'],
            ['.skillsBTableTotalRows'],
            ['.rank-detail-card'],
            ['.apex-chart-container'],
        ]}, {
            rules:{
                "landmark-no-duplicate-banner": {enabled:false},
                'landmark-no-duplicate-contentinfo': {enabled:false},
                'heading-order': {enabled:false},
                'landmark-unique': {enabled:false},
                'aria-dialog-name': {enabled:false},
            }
    }, terminalLog);
});


Cypress.Commands.add("logout", () => {
    cy.request('POST', '/logout');
    cy.log('Logged out')
});

Cypress.Commands.add("clickSave", () => {
    cy.get("button:contains('Save')").click();
});

Cypress.Commands.add("clickButton", (label) => {
    cy.get(`button:contains('${label}')`).click();
});

Cypress.Commands.add("clickManageSubject", (subjId) => {
    cy.get(`[data-cy=manageBtn_${subjId}]`).click();
});




Cypress.Commands.add("getIdField", () => {
    return cy.get("#idInput");
});


Cypress.Commands.add("setResolution", (size) => {
    if (size !== 'default') {
        if (Cypress._.isArray(size)) {
            cy.viewport(size[0], size[1]);
        } else {
            cy.viewport(size);
        }
        cy.log(`Set viewport to ${size}`);
    } else {
        cy.log(`Using default viewport`);
    }
});

Cypress.Commands.add('vuex', () => {
    cy.window().should('have.property','vm');
    return cy.window().its('vm.$store');
});

//see cypress-io #7306
Cypress.Commands.add('get$', (selector) => {
   return cy.wrap(Cypress.$(selector)).should('have.length.gte', 1);
});

Cypress.Commands.add('resetDb', () => {
    // first call to npm fails, looks like this may be the bug: https://github.com/cypress-io/cypress/issues/6081
    cy.exec('npm version', {failOnNonZeroExit: false})
    cy.exec('npm run backend:resetDb')
    cy.log('reset postgres db')
});

Cypress.Commands.add('clearDb', () => {
    // first call to npm fails, looks like this may be the bug: https://github.com/cypress-io/cypress/issues/6081
    cy.exec('npm version', {failOnNonZeroExit: false})
    cy.exec('npm run backend:clearDb')
});

Cypress.Commands.add('createInviteOnly', () => {
    // first call to npm fails, looks like this may be the bug: https://github.com/cypress-io/cypress/issues/6081
    cy.exec('npm version', {failOnNonZeroExit: false})
    cy.exec('npm run backend:setupInviteOnly', {failOnNonZeroExit: false}).then((res) => {
        cy.log(res.stdout);
        cy.log(res.stderr);
        cy.log(res.code);
    });
})

Cypress.Commands.add('waitForBackendAsyncTasksToComplete', () => {
    const waitConf = {
        timeout: 60000, // waits up to 1 minutes
        interval: 500 // performs the check every 500 ms, default to 200
    };

    // first call to npm fails, looks like this may be the bug: https://github.com/cypress-io/cypress/issues/6081
    cy.exec('npm version', {failOnNonZeroExit: false})
    cy.waitUntil(() => cy.exec('npm run backend:countScheduledTasks').then((result) => result.stdout.match(/.*------\s+(\d+)\s+\(/)[1] === '0'), waitConf);
});


Cypress.Commands.add('clickNav', (navName) => {
    cy.get(`[data-cy="nav-${navName}"]`).click()
});

Cypress.Commands.add('violationLoggingFunction', () => {
    return (violations) => {
        cy.task(
          'log',
          `${violations.length} accessibility violation${
            violations.length === 1 ? '' : 's'
          } ${violations.length === 1 ? 'was' : 'were'} detected`
        )
        // pluck specific keys to keep the table readable
        const violationData = violations.map(
          ({ id, impact, description, nodes }) => ({
              id,
              impact,
              description,
              nodes: nodes.length
          })
        )

        cy.task('table', violationData)
    };
});


const baseUrl = Cypress.config().baseUrl;
Cypress.Commands.add('loginBySingleSignOn', (projId = 'proj1') => {
    Cypress.log({
        name: 'loginBySingleSignOn',
    })

    cy.fixture('vars.json').then((vars) => {
        // first try to get a skills token,
        cy.request({
            url: `http://localhost:8080/api/projects/${projId}/token`,
            failOnStatusCode: false,
        }).then((tokenResp) => {
            if (tokenResp.status === 401) {
                cy.log('Skills token request failed, authenticating with OAuth provider...');
                cy.request({
                    url: 'http://localhost:8080/oauth2/authorization/hydra',
                    qs: { skillsRedirectUri: baseUrl, },
                    // qs: { skillsRedirectUri: `${baseUrl}${homePage}` },
                }).then((resp) => {
                    expect(resp.status).to.eq(200)

                    // parse out the authenticity_token
                    const $html = Cypress.$(resp.body)
                    const authenticityToken = $html.find('input[name=_csrf]').val()
                    const challenge = $html.find('input[name=challenge]').val()

                    if (Cypress.env('hydraAuthenticated')) {
                        cy.log('already authenticated with OAuth provider');
                    } else {
                        const options = {
                            method: 'POST',
                            url: 'http://localhost:3000/login',
                            form: true, // we are submitting a regular form body
                            body: {
                                _csrf: authenticityToken,
                                challenge,
                                email: vars.oauthUser,
                                password: vars.oauthPass,
                                submit: 'Log in',
                                remember: '1',
                            },
                        };

                        cy.request(options).then((resp2) => {
                            expect(resp2.status).to.eq(200)

                            if (resp2.redirects[resp2.redirects.length - 1].includes('/consent?consent_challenge')) {
                                cy.log('Granting consent with OAuth provider...');
                                const $html = Cypress.$(resp2.body)
                                const authenticityToken = $html.find('input[name=_csrf]').val()
                                const challenge = $html.find('input[name=challenge]').val()
                                // const consentUrl = resp2.redirects.filter(r => r.includes('/consent?consent_challenge'))[0].split(' ')[1]
                                const options = {
                                    method: 'POST',
                                    url: 'http://localhost:3000/consent',
                                    form: true, // we are submitting a regular form body
                                    qs: {consent_challenge: challenge},
                                    body: {
                                        _csrf: authenticityToken,
                                        challenge,
                                        grant_scope: 'openid',
                                        // grant_scope: 'offline',
                                        submit: 'Allow access',
                                        remember: '1',
                                    },
                                    failOnStatusCode: false,
                                };

                                cy.request(options).then((resp3) => {
                                    expect(resp3.status).to.eq(200)
                                })
                            }
                        })
                        Cypress.env('hydraAuthenticated', true)
                    }
                })
            } else {
                cy.log('Received Skills token, already authenticated with OAuth provider.');
            }
        })
    });
});

Cypress.Commands.add("loginAsRootUser", () => {
    cy.fixture('vars.json').then((vars) => {
        cy.request('POST', '/logout');
        cy.login(vars.rootUser, vars.defaultPass);
    });
})

Cypress.Commands.add("loginAsDefaultUser", () => {
    cy.fixture('vars.json').then((vars) => {
        cy.request('POST', '/logout');
        cy.login(vars.defaultUser, vars.defaultPass);
    });
})

Cypress.Commands.add("loginAsAdminUser", () => {
    cy.fixture('vars.json').then((vars) => {
        cy.request('POST', '/logout');
        if (!Cypress.env('oauthMode')) {
            cy.log('NOT in oauthMode, using form login')
            cy.login(vars.defaultUser, vars.defaultPass);
        } else {
            cy.log('oauthMode, using loginBySingleSignOn')
            cy.loginBySingleSignOn()
        }
    });
})

Cypress.Commands.add("loginAsProxyUser", () => {
    cy.fixture('vars.json')
        .then((vars) => {
            cy.request('POST', '/logout');
            if (!Cypress.env('oauthMode')) {
                cy.log('NOT in oauthMode, using form login')
                cy.login(Cypress.env('proxyUser'), vars.defaultPass);
            } else {
                cy.log('oauthMode, using loginBySingleSignOn')
                cy.loginBySingleSignOn()
            }
        })
});

Cypress.Commands.add('fill', {
    prevSubject: 'element',
}, ($subject, value) => {
    const el = $subject[0];
    el.value = value;
    return cy.wrap($subject).type('t{backspace}');
});


Cypress.Commands.add('reportHistoryOfEvents', (projId, user, numDays=10, skipWeeDays = [5,6], availableSkillIds=['skill1', 'skill2', 'skill3']) => {
    let skipDays = [...skipWeeDays];
    for(let daysCounter=0; daysCounter < numDays; daysCounter++) {
        cy.log(`user: ${user}, day: ${daysCounter}, skipDays=${skipDays}, skills=${availableSkillIds}`)
        let toSkip = false;
        skipDays.forEach((skipNum, index) => {
            if(daysCounter === skipNum) {
                toSkip = true;
                skipDays[index] += 7;
            }
        });
        if(toSkip) {
            cy.log(`skipping: ${skipDays}`);
            continue;
        }

        const time = new Date().getTime() - (daysCounter)*1000*60*60*24;
        const numSkillsToReport = Math.random() * (availableSkillIds.length);
        cy.log(numSkillsToReport);
        for(let skillsCounter=0; skillsCounter < numSkillsToReport; skillsCounter++) {
            const skillId = availableSkillIds[skillsCounter];
            cy.log(user);
            cy.request('POST', `/api/projects/${projId}/skills/${skillId}`, {userId: user, timestamp: time})
        }
    }
});

Cypress.Commands.add('validateTable', (tableSelector, expected, pageSize = 5, onlyVisiblePage = false, numRowsParam = null, validateTotalRows = true, sortColumnName = null) => {
    cy.get(tableSelector).contains('Loading...').should('not.exist')
    cy.get(tableSelector).contains('There are no records to show').should('not.exist')
    const rowSelector = `${tableSelector} tbody tr`
    const numRows =  numRowsParam ? numRowsParam : expected.length;

    if (sortColumnName) {
        cy.get(`${tableSelector} th`).contains(sortColumnName).should('exist').click();
    }

    if (validateTotalRows) {
        cy.get(`${tableSelector} [data-cy=skillsBTableTotalRows]`)
            .should('have.text', numRows);
    }

    cy.get(rowSelector).should('have.length', Math.min(pageSize, numRows)).as('cyRows');

    const numIterations = onlyVisiblePage ? Math.min(pageSize, numRows) : numRows
    for (let i = 0; i < numIterations; i += 1) {
        let rowIndex = i;
        if (i + 1 >= pageSize) {
            rowIndex = i - (pageSize * (Math.trunc(i / pageSize)));
        }
        if (i > 0 && i % pageSize === 0) {
            const nextPage = (i / pageSize) + 1;
            const nextPageSize = (i + pageSize <= numRows) ? pageSize : (numRows % pageSize);
            cy.log(`Going to the next page #${nextPage}, next page size is [${nextPageSize}]`);
            cy.get(tableSelector).get('[data-cy=skillsBTablePaging]').contains(nextPage).click();
            cy.get(tableSelector).contains('Loading...').should('not.exist')
            cy.get(rowSelector).should('have.length', nextPageSize).as('cyRows');
        }

        cy.get('@cyRows').eq(rowIndex).find('td').as('row1');
        const toValidate = expected[i];
        toValidate.forEach((item) => {
            cy.get('@row1').eq(item.colIndex).should('contain.text', item.value);
        })
    }

});

Cypress.Commands.add('wrapIframe', () => {
    return cy.get('iframe')
      .its('0.contentDocument.body').should('not.be.empty')
      .then(cy.wrap)
});

Cypress.Commands.add('closeToasts', () => {
    cy.get('body').then((body) => {
        if (body.find('header.toast-header').length > 0) {
            cy.get('button.close').click({ multiple: true });
        }
    });
});


Cypress.Commands.add('dragAndDrop', { prevSubject: 'element' }, (sourceElement, destSelector) => {
    const dataTransfer = new DataTransfer()

    cy.get(destSelector).then((destProject) => {
        cy.wrap(sourceElement.get(0))
            .trigger('pointerdown', { eventConstructor: 'PointerEvent' })
            .trigger('dragstart', { dataTransfer, eventConstructor: 'DragEvent' })
            .then(() => {
                cy.wrap(destProject.get(0))
                    .trigger('dragover', { dataTransfer, eventConstructor: 'DragEvent' })
                    .wait(1000)
                    .trigger('drop', {
                        dataTransfer,
                        eventConstructor: 'DragEvent',
                    })
                    .wait(1000)
            });
    })
});
Cypress.Commands.add("validateElementsOrder", (selector, containsValues) => {
    cy.get(selector).should('have.length', containsValues.length).as('elements');
    for (const [i, value] of containsValues.entries()) {
        cy.get('@elements').eq(i).contains(value);
    }
});

Cypress.Commands.add('formRequest', (method, url, formData, onComplete) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url)
    xhr.onload = function () { onComplete(xhr) }
    xhr.onerror = function () { onComplete(xhr) }
    xhr.send(formData)
})

Cypress.Commands.add("uploadCustomIcon", (fileName, url) => {
    const method = 'POST';
    const fileType = 'image/png';
    cy.fixture(fileName, 'binary')
        .then((excelBin) => {
            const blob = Cypress.Blob.binaryStringToBlob(excelBin, fileType);
            const formData = new FormData();
            formData.set('customIcon', blob, fileName);
            cy.formRequest(method, url, formData, function (response) {
                expect(response.status)
                    .to
                    .eq(200);
            });
        });
});

Cypress.Commands.add('reuseSkillIntoAnotherSubject', (projNum, skillNum, toSubjNum) => {
    const url = `/admin/projects/proj${projNum}/skills/reuse`;
    cy.request('POST', url, {
        subjectId: `subj${toSubjNum}`,
        skillIds: [`skill${skillNum}`]
    });
});

Cypress.Commands.add('reuseSkillIntoAnotherGroup', (projNum, skillNum, toSubjNum, groupNum) => {
    cy.log(groupNum);
    const groupId = `group${groupNum}${toSubjNum > 1 ? `Subj${toSubjNum}` : ''}`;
    const url = `/admin/projects/proj${projNum}/skills/reuse`;
    cy.request('POST', url, {
        subjectId: `subj${toSubjNum}`,
        groupId,
        skillIds: [`skill${skillNum}`]
    });
});

Cypress.Commands.add('moveSkillIntoAnotherSubject', (projNum, skillNum, toSubjNum) => {
    const url = `/admin/projects/proj${projNum}/skills/move`;
    cy.request('POST', url, {
        subjectId: `subj${toSubjNum}`,
        skillIds: [`skill${skillNum}`]
    });
});

Cypress.Commands.add('addUserTag', (userTags) => {
    cy.fixture('vars.json')
        .then((vars) => {

            const userIdsWithAssociatedTags = []
            userTags.forEach((element, index) => {
                const userId =  `user${index + 1}`;
                cy.register(userId, 'password');
                cy.log(`Registered [${userId}] user`);
                userIdsWithAssociatedTags.push({
                    userId,
                    tagKey: element.tagKey,
                    tags: element.tags
                })
            });


            cy.logout();
            cy.register(vars.rootUser, vars.defaultPass, true);
            cy.login(vars.rootUser, vars.defaultPass);

            userIdsWithAssociatedTags.forEach((tagInfo) => {
                cy.log(`Create tags user=[${tagInfo.userId}], tagKey=[${tagInfo.tagKey}], tags=[${tagInfo.tags}]`);
                const url = `/root/users/${tagInfo.userId}/tags/${tagInfo.tagKey}`;
                cy.request('POST', url, { tags: tagInfo.tags });
            });

            cy.logout();
            cy.login(vars.defaultUser, vars.defaultPass);
        });
});

Cypress.Commands.add('moveSkillIntoAnotherGroup', (projNum, skillNum, toSubjNum, groupNum) => {
    cy.log(groupNum);
    const groupId = `group${groupNum}${toSubjNum > 1 ? `Subj${toSubjNum}` : ''}`;
    const url = `/admin/projects/proj${projNum}/skills/move`;
    cy.request('POST', url, {
        subjectId: `subj${toSubjNum}`,
        groupId,
        skillIds: [`skill${skillNum}`]
    });
});

Cypress.Commands.add('beforeTestSuiteThatReusesData', () => {
    Cypress.env('disableResetDb', true);
    cy.resetDb();
    cy.resetEmail();

    cy.logout();
    cy.fixture('vars.json')
        .then((vars) => {
            if (!Cypress.env('oauthMode')) {
                cy.log('NOT in oauthMode, using form login');
                cy.login(vars.defaultUser, vars.defaultPass);
            } else {
                cy.log('oauthMode, using loginBySingleSignOn');
                cy.loginBySingleSignOn();
            }
        });
});

Cypress.Commands.add('afterTestSuiteThatReusesData', () => {
    Cypress.env('disableResetDb', false);
});
