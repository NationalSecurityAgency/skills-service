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
var moment = require('moment-timezone');

describe('Skills Group Modal Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });
    const tableSelector = '[data-cy="skillsTable"]';

    it('Skills Group modal - id is auto generated based on name - special chars', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('Great !@#$% Name %^&*(+_)(');
        // cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').should('have.value','GreatNameGroup');

        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');
    });

    it('Skills Group modal - id must not be auto generated based on name when id input is enabled', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="idInputValue"]').should('be.disabled');
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').should('be.enabled');

        cy.get('[data-cy="groupName"]').type('Great');
        cy.get('[data-cy="idInputValue"]').should('have.value','');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
    });

    it('Skills Group modal - input validation - min/max length', () => {
        cy.intercept('/public/config', {
            body: {
                artifactBuildTimestamp: "2022-01-17T14:39:38Z",
                authMode: "FORM",
                buildTimestamp: "2022-01-17T14:39:38Z",
                dashboardVersion: "1.9.0-SNAPSHOT",
                defaultLandingPage: "progress",
                descriptionMaxLength: "2000",
                docsHost: "https://code.nsa.gov/skills-docs",
                expirationGracePeriod: 7,
                expireUnusedProjectsOlderThan: 180,
                maxBadgeNameLength: "50",
                maxBadgesPerProject: "25",
                maxDailyUserEvents: "30",
                maxFirstNameLength: "30",
                maxIdLength: "50",
                maxLastNameLength: "30",
                maxLevelNameLength: "50",
                maxNicknameLength: "70",
                maxNumPerformToCompletion: "10000",
                maxNumPointIncrementMaxOccurrences: "999",
                maxPasswordLength: "40",
                maxPointIncrement: "10000",
                maxProjectNameLength: "50",
                maxProjectsPerAdmin: "25",
                maxSelfReportMessageLength: "250",
                maxSelfReportRejectionMessageLength: "250",
                maxSkillNameLength: "100",
                maxSkillVersion: "999",
                maxSkillsPerSubject: "5",
                maxSubjectNameLength: "50",
                maxSubjectsPerProject: "25",
                maxTimeWindowInMinutes: "43200",
                minIdLength: "3",
                minNameLength: "3",
                minPasswordLength: "8",
                minUsernameLength: "5",
                minimumProjectPoints: "100",
                minimumSubjectPoints: "100",
                nameValidationMessage: "",
                nameValidationRegex: "",
                needToBootstrap: false,
                numProjectsForTableView: "10",
                oAuthOnly: false,
                paragraphValidationMessage: "paragraphs may not contain jabberwocky",
                paragraphValidationRegex: "^(?i)(?s)((?!jabberwocky).)*$",
                pointHistoryInDays: "1825",
                projectMetricsTagCharts: "[{\"key\":\"dutyOrganization\",\"type\":\"pie\",\"title\":\"Users by Org\"},{\"key\":\"adminOrganization\",\"type\":\"bar\",\"title\":\"Users by Agency\"}]",
                rankingAndProgressViewsEnabled: "true",
                userSuggestOptions: "ONE,TWO,THREE",
                verifyEmailAddresses: false,
            }
        });
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('12');
        cy.get('[data-cy="groupNameError"]').contains('Group Name cannot be less than 3 characters.');
        cy.get('[data-cy="groupName"]').type('3');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // so id doesn't change anymore
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();

        // max value
        // Group Name cannot exceed 100 characters.
        const invalidName = Array(101).fill('a').join('');
        cy.get('[data-cy=groupName]').clear()
        cy.get('[data-cy=groupName]').fill(invalidName);
        cy.get('[data-cy=groupNameError]').contains('Group Name cannot exceed 100 characters.').should('be.visible');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy=groupName]').type('{backspace}');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // now let's validate id
        // min value
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('12');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');
        cy.get('[data-cy="idError"]').contains('Group ID cannot be less than 3 characters.');
        cy.get('[data-cy="idInputValue"]').type('3');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // max value
        const invalidId = Array(51).fill('a').join('');
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').fill(invalidId);
        cy.get('[data-cy="idError"]').contains('Group ID cannot exceed 50 characters.');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy=idInputValue]').type('{backspace}');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');
    });

    it('Skills Group modal - input validation - name or id already exist', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');
        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');

        // validate against skill
        cy.get('[data-cy="groupName"]').type('Very Great Skill 1');
        cy.get('[data-cy=groupNameError]').contains('The value for the Group Name is already taken.').should('be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('a');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        cy.get('[data-cy="groupName"]').clear()
        // validate against group
        cy.get('[data-cy="groupName"]').type('Awesome Group 1');
        cy.get('[data-cy=groupNameError]').contains('The value for the Group Name is already taken.').should('be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('a');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // now let's test id field
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('skill1');
        cy.get('[data-cy="idError"]').contains('The value for the Group ID is already taken.');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy="idInputValue"]').type('a');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('group1');
        cy.get('[data-cy="idError"]').contains('The value for the Group ID is already taken.');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy="idInputValue"]').type('a');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');
    });

    it('Skills Group modal - input validation - description custom validation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();

        cy.get('[data-cy="groupName"]').type('Awesome Group 1');
        cy.get('[data-cy="groupDescription"]').type('ldkj aljdl aj\n\njabberwocky');

        cy.get('[data-cy="groupDescriptionError"]').contains('Group Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');
    });

    it('Skills Group modal - edit existing group', () => {
        cy.createSkillsGroup(1, 1, 1, { description: 'first group description' })
        cy.createSkillsGroup(1, 1, 2, { description: 'second group description' })
        cy.createSkillsGroup(1, 1, 3, { description: 'third group description' })
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="expandDetailsBtn_group2"]').click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group2"] [data-cy="description"]').contains('second group description');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy="groupDescription"] [data-cy="markdownEditorInput"]').should('have.value', 'second group description');

        cy.get('[data-cy="groupDescription"] [data-cy="markdownEditorInput"]').clear().type('another value');
        cy.get('[data-cy="groupName"]').clear().type('Updated Group Name');
        cy.get('[data-cy="saveGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');

        cy.get('[data-cy="nameCell_group2"]').contains('Updated Group Name')
        cy.get('[data-cy="expandDetailsBtn_group2"]').click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group2"] [data-cy="description"]').contains('another value');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy="groupName"]').should('have.value', 'Updated Group Name');
        cy.get('[data-cy="groupDescription"] [data-cy="markdownEditorInput"]').should('have.value', 'another value');
    });

    it('Skills Group modal - edit id of an existing group', () => {
        cy.createSkillsGroup(1, 1, 1, { description: 'first group description' })
        cy.createSkillsGroup(1, 1, 2, { description: 'second group description' })
        cy.createSkillsGroup(1, 1, 3, { description: 'third group description' })
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').should('have.value','group2');
        cy.get('[data-cy="idInputValue"]').clear().type('newId');
        cy.get('[data-cy="saveGroupButton"]').click();

        cy.get('[data-cy="editSkillButton_newId"]').should('exist');
        cy.get('[data-cy="editSkillButton_group2"]').should('not.exist');

        cy.get('[data-cy="editSkillButton_newId"]').click();
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').should('have.value','newId');

    });
});