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

describe('Cross-project Skills Tests', () => {

    const sharedWithOtherTableSelector = '[data-cy="sharedSkillsTable"]';
    const sharedWithMeTableSelector = '[data-cy="skillsSharedWithMeCard"] [data-cy="sharedSkillsTable"]';

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'Project 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `Very Great Skill # 1`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'Project 2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj2',
            name: 'Interesting Subject 2',
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj2/skills/skill3`, {
            projectId: 'proj2',
            subjectId: 'subj2',
            skillId: `skill3`,
            name: `Very Great Skill # 3`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'Project 3'
        });
        cy.request('POST', '/admin/projects/proj3/subjects/subj3', {
            projectId: 'proj3',
            subjectId: 'subj3',
            name: 'Interesting Subject 3',
        });
        cy.request('POST', `/admin/projects/proj3/subjects/subj3/skills/skill4`, {
            projectId: 'proj3',
            subjectId: 'subj3',
            skillId: `skill4`,
            name: `Very Great Skill # 4`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        Cypress.Commands.add('shareSkill', (skillText, projText) => {
            cy.get('[data-cy="shareButton"')
                .should('be.disabled');
            cy.get('[data-cy="skillSelector"]')
                .click()
            cy.get('[data-cy="skillSelector"]')
                .type(`{selectall}${skillText}`)
            cy.get('[data-cy="skillsSelector-skillName"]').contains(skillText).first()
                .click()
            cy.get('[data-cy="shareButton"]')
                .should('be.disabled');

            cy.get('[data-cy="projectSelector"]')
                .click()
            if (projText) {
                cy.get('[data-pc-name="pcfilter"]')
                  .type(`${projText}`)
                cy.get('[data-cy="projectSelector-projectName"]').contains(projText).first()
                  .click()
            } else {
                cy.get('[data-cy="projectSelector-projectName"]').first().click()
            }
            cy.get('[data-cy="shareButton"')
                .should('be.enabled');

            cy.get('[data-cy="shareButton"')
                .click();
        });
    });

    it('share skill with another project', () => {
        cy.visit('/administrator/projects/proj1');
        cy.clickNav('Learning Path');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.shareSkill('1', '2');

        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        // -------------------------
        // Project 2 should see the skill
        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        const sharedSkillSelector = '[data-cy="skillsSelectionItem-proj1-skill1"]'
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).contains('Shared Skill:');
        cy.get(sharedSkillSelector).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')

        // -------------------------
        // Project 3 should not see the shared skill
        cy.visit('/administrator/projects/proj3/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).should('not.exist')
    });

    it('share with all projects', () => {
        cy.visit('/administrator/projects/proj1/learning-path');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.get('[data-cy="shareButton"')
            .should('be.disabled');
        cy.get('[data-cy="skillSelector"]')
            .click()
            .type('1{enter}');
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill1"]`).click()
        cy.get('[data-cy="shareButton"')
            .should('be.disabled');

        cy.get('[data-cy="shareWithAllProjectsCheckbox"]').click()

        cy.get('[data-cy="shareButton"')
            .click();

        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'All Projects'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        // -------------------------
        // Project 2 should see the skill
        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        cy.contains('No Learning Path Yet');
        const sharedSkillSelector = '[data-cy="skillsSelectionItem-proj1-skill1"]'
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).contains('Shared Skill:');
        cy.get(sharedSkillSelector).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')

        // -------------------------
        // Project 3 should see the shared skill
        cy.visit('/administrator/projects/proj3');
        cy.clickNav('Learning Path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        cy.contains('No Learning Path Yet');
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(sharedSkillSelector).contains('Shared Skill:');
        cy.get(sharedSkillSelector).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')
    });

    it('remove share', () => {
        cy.visit('/administrator/projects/proj1/learning-path');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.shareSkill('1', '2');
        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false);

        cy.shareSkill('2', null);
        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 2'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false, 'Shared Skill');

        cy.get(`${sharedWithOtherTableSelector} [data-cy="sharedSkillsTable-removeBtn"]`)
            .first()
            .click();
        cy.validateTable(sharedWithOtherTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 2'
            }, {
                colIndex: 1,
                value: 'Project 2'
            }],
        ], 5, true, null, false);

        // -------------------------
        // Project 2 should see the skill2 but not skill1
        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');

        cy.validateTable(sharedWithMeTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 2'
            }, {
                colIndex: 1,
                value: 'Project 1'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get('[data-cy="skillsSelectionItem-proj1-skill2"]')
        cy.get('[data-cy="skillsSelectionItem-proj1-skill1"]').should('not.exist')
    });

    it('Ability to claim honor points for cross-project skill from another project', function () {
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillLink-proj2-skill3"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.get('[data-cy="crossProjAlert"]').should('not.exist')

        cy.get('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.get('[data-cy="claimPointsBtn"]').click()
        cy.get('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.get('[data-cy="skillCompletedCheck-skill3"]')
        cy.get('[data-cy="achievementDate"]')

        // refresh and validate
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1/crossProject/proj2/skill3');
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.get('[data-cy="skillCompletedCheck-skill3"]')
        cy.get('[data-cy="achievementDate"]')
    })

    it('Ability to request approval points for cross-project skill from another project', function () {
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillLink-proj2-skill3"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="requestApprovalBtn"]').should('be.enabled')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.get('[data-cy="selfReportSubmitBtn"]').click()
        cy.get('[data-cy="selfReportAlert"]').contains('This skill requires approval from a project administrator')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"] [data-cy="approvalPending"]')

        // refresh and validate
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1/crossProject/proj2/skill3');
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.get('[data-cy="requestApprovalBtn"]').should('not.exist')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"] [data-cy="approvalPending"]')
    })

    it('Ability to claim points by watching a Video for cross-project skill from another project', function () {
        cy.createSubject(2, 1)

        cy.createSkill(2, 1, 3, {numPerformToCompletion : 1})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(2, 3, vidAttr)
        cy.createSkill(2, 1, 3, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillLink-proj2-skill3"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')

        // refresh and validate
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1/crossProject/proj2/skill3');
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="skillCompletedCheck-skill3"]')
        cy.get('[data-cy="achievementDate"]')
    })

    it('Ability to claim points by completing a quiz for cross-project skill from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillLink-proj2-skill3"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 3 skill by passing this quiz')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="completeQuizBtn"]').click()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 3 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
        cy.get('[data-cy="skillCompletedCheck-skill3"]')
        cy.get('[data-cy="achievementDate"]')

        // refresh and validate
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1/crossProject/proj2/skill3');
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
        cy.get('[data-cy="skillCompletedCheck-skill3"]')
        cy.get('[data-cy="achievementDate"]')
    })

    it('Quiz attempt history is displayed for cross-project skill from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [1]}])

        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillLink-proj2-skill3"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="approvalHistoryTimeline"]').contains('Failed')
    })

    it('can view quiz results for cross-project skill from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}])

        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');;
        cy.get('[data-cy="skillLink-proj2-skill3"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.get('[data-cy="quizCompletedMsg"]').contains('You have passed')
        cy.get('[data-cy="viewQuizAttemptInfo"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"]')
    })

    it('skills-client: Ability to claim honor points for cross-project skill from another project', function () {
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillLink-proj2-skill3"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="crossProjAlert"]').should('not.exist')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1%2FcrossProject%2Fproj2%2Fskill3');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Ability to request approval points for cross-project skill from another project', function () {
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillLink-proj2-skill3"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="requestApprovalBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="requestApprovalBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportSubmitBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('This skill requires approval from a project administrator')
        cy.wrapIframe().find('[data-cy="skillProgress-ptsOverProgressBard"] [data-cy="approvalPending"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1%2FcrossProject%2Fproj2%2Fskill3');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="requestApprovalBtn"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="skillProgress-ptsOverProgressBard"] [data-cy="approvalPending"]')
    })

    it('skills-client: Ability to claim points by watching a Video for cross-project skill from another project', function () {
        cy.createSubject(2, 1)

        cy.createSkill(2, 1, 3, {numPerformToCompletion : 1})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(2, 3, vidAttr)
        cy.createSkill(2, 1, 3, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillLink-proj2-skill3"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.wrapIframe().find('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.wrapIframe().find('[data-cy="skillVideo-skill3"] [data-cy="viewTranscriptBtn"]')
        cy.wrapIframe().find('[data-cy="skillVideo-skill3"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.wrapIframe().find('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.wrapIframe().find('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.wrapIframe().find('[data-cy="viewTranscriptBtn"]')
        cy.wrapIframe().find('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')

        // refresh and validate
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1%2FcrossProject%2Fproj2%2Fskill3');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Ability to claim points by completing a quiz for cross-project skill from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillLink-proj2-skill3"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.wrapIframe().find('[data-cy="takeQuizBtn"]').click();

        cy.wrapIframe().find('[data-cy="title"]').contains('Quiz')
        cy.wrapIframe().find('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 3 skill by passing this quiz')

        cy.wrapIframe().find('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.wrapIframe().find('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.wrapIframe().find('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.wrapIframe().find('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.wrapIframe().find('[data-cy="startQuizAttempt"]').click()

        cy.wrapIframe().find('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.wrapIframe().find('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.wrapIframe().find('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.wrapIframe().find('[data-cy="completeQuizBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="completeQuizBtn"]').click()

        cy.wrapIframe().find('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 3 skill by passing the quiz.')

        cy.wrapIframe().find('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.wrapIframe().find('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1%2FcrossProject%2Fproj2%2Fskill3');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Quiz attempt history is displayed for cross-project skill from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [1]}])

        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillLink-proj2-skill3"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="approvalHistoryTimeline"]').contains('Failed')
    })

    it('skills-client: can view quiz results for cross-project skill from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}])

        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addCrossProjectLearningPathItem(2, 3, 1, 1)

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillLink-proj2-skill3"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3')

        cy.wrapIframe().find('[data-cy="quizCompletedMsg"]').contains('You have passed')
        cy.wrapIframe().find('[data-cy="viewQuizAttemptInfo"]').click()
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"]')
    })
});
