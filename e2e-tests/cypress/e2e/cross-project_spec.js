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
            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="shareButton"]')
                .should('be.disabled');
            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="skillsSelector"]')
                .click()
            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="skillsSelector"] input')
                .type(`{selectall}${skillText}`)
            cy.get('[data-pc-section="overlay"] [data-cy="skillsSelector-skillName"]').contains(skillText).first()
                .click()
            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]  [data-cy="shareButton"]')
                .should('be.disabled');

            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="projectSelector"]')
                .click()
            if (projText) {
                cy.get('[data-pc-section="overlay"] [data-pc-name="pcfilter"]')
                  .type(`${projText}`)
                cy.get('[data-pc-section="overlay"] [data-cy="projectSelector-projectName"]').contains(projText).first()
                  .click()
            } else {
                cy.get('[data-pc-section="overlay"] [data-cy="projectSelector-projectName"]').first().click()
            }
            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="shareButton"]')
                .should('be.enabled');

            cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="shareButton"]')
                .click();
        });
    });

    it('loads shared skills after an incompatible sort was saved', () => {
        cy.request('PUT', '/admin/projects/proj1/skills/skill1/shared/projects/proj2');

        cy.visit('/administrator/projects/proj1/learning-path', {
            onBeforeLoad(win) {
                win.localStorage.setItem('skillsTable-sort-sharedSkillsTable', JSON.stringify({
                    sortOrder: 1,
                    sortBy: 'projectName',
                }));
            },
        });

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"] [data-cy="sharedSkillsTable"]')
            .should('contain', 'Very Great Skill # 1');
        cy.window().then((win) => {
            expect(JSON.parse(win.localStorage.getItem('skillsTable-sort-sharedSkillsTable')))
                .to.deep.equal({ sortOrder: -1, sortBy: 'skill.name' });
        });

        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get(sharedWithMeTableSelector).should('contain', 'Very Great Skill # 1');
        cy.window().then((win) => {
            expect(JSON.parse(win.localStorage.getItem('skillsTable-sort-sharedWithMeSkillsTable')))
                .to.deep.equal({ sortOrder: -1, sortBy: 'skill.skillId' });
        });
    });

    it('share skill with another project', () => {
        cy.viewport(1400, 1800)
        cy.intercept('/admin/projects/proj1/skillsAndBadges**').as('skillsAndBadges');
        cy.intercept('GET', '/admin/projects/**/sharedWithMe**').as('loadSharedSkills');
        cy.visit('/administrator/projects/proj1/learning-path');
        cy.wait('@skillsAndBadges');
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
        cy.get('[data-cy="includeShared"]').click();
        cy.wait('@loadSharedSkills')
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(`${sharedSkillSelector} [data-cy="skillsSelector-type"]`).contains('Shared Skill');
        cy.get(`${sharedSkillSelector} [data-cy="skillsSelector-projectName"]`).contains('Project 1')
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
        cy.intercept('GET', '/admin/projects/**/sharedWithMe**').as('loadSharedSkills');

        cy.get('[data-cy="shareSkillsWithOtherProjectsCard"]')
            .contains('Share skills from this project with other projects');
        cy.get('[data-cy="skillsSharedWithMeCard"]')
            .contains('No Skills Available Yet...');

        cy.get('[data-cy="shareButton"]')
            .should('be.disabled');
        cy.get('[data-cy="skillSelector"]')
            .click()
            .type('1{enter}');
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill1"]`).click()
        cy.get('[data-cy="shareButton"]')
            .should('be.disabled');

        cy.get('[data-cy="shareWithAllProjectsCheckbox"]').click()

        cy.get('[data-cy="shareButton"]')
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
        cy.get('[data-cy="includeShared"]').click();
        cy.wait('@loadSharedSkills')
        cy.get(`${sharedSkillSelector} [data-cy="skillsSelector-projectName"]`).contains('Project 1')
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
        cy.get('[data-cy="includeShared"]').click();
        cy.wait('@loadSharedSkills')
        cy.get('[data-cy="learningPathFromSkillSelector"]').click()
        cy.get(`${sharedSkillSelector} [data-cy="skillsSelector-projectName"]`).contains('Project 1')
        cy.get(sharedSkillSelector).contains('Very Great Skill # 1')
    });

    it('remove share', () => {
        cy.intercept('GET', '/admin/projects/**/sharedWithMe**').as('loadSharedSkills');
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

        cy.get('[data-cy="includeShared"]').click();
        cy.wait('@loadSharedSkills')

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

describe('Shared skills pagination and request recovery', () => {
    const outgoingCard = '[data-cy="shareSkillsWithOtherProjectsCard"]';
    const incomingCard = '[data-cy="skillsSharedWithMeCard"]';
    const table = '[data-cy="sharedSkillsTable"]';
    const rows = '[data-pc-section="bodyrow"]';
    const fromInput = '[data-cy="learningPathFromSkillSelector"] input';
    const option = (project, skill) => `[data-cy="skillsSelectionItem-proj${project}-skill${skill}"]`;
    const outgoingSortKey = 'skillsTable-sort-sharedSkillsTable';
    const incomingSortKey = 'skillsTable-sort-sharedWithMeSkillsTable';

    const visit = (project = 1, savedSorts = {}) => {
        cy.visit(`/administrator/projects/proj${project}/learning-path`, {
            onBeforeLoad(win) {
                win.localStorage.setItem('sharedSkillsTable-pageSize', '5');
                win.localStorage.setItem('sharedWithMeSkillsTable-pageSize', '5');
                Object.entries(savedSorts).forEach(([key, value]) => {
                    win.localStorage.setItem(key, JSON.stringify(value));
                });
            },
        });
    };

    const expectFirstSkill = (card, name, rowCount = 5) => {
        cy.get(`${card} ${table} ${rows}`).should('have.length', rowCount)
            .first().should('contain', name);
    };

    const pagingNames = (first, last) => Array.from({ length: Math.abs(last - first) + 1 }, (_, index) =>
        `Paging Skill ${String(first + index * (last >= first ? 1 : -1)).padStart(2, '0')}`);

    const createTwelveShares = () => {
        for (let skill = 1; skill <= 12; skill += 1) {
            cy.createSkill(1, 1, skill, { name: `Paging Skill ${String(skill).padStart(2, '0')}` });
            if (skill > 6) {
                cy.request('PUT', `/admin/projects/proj1/skills/skill${skill}/shared/projects/proj2`);
            }
        }
    };

    const expectSkills = (card, expected, page) => {
        cy.get(`${card} ${rows} td:first-child`).should(($skills) => {
            expect([...$skills].map((skill) => skill.textContent.trim())).to.deep.equal(expected);
        });
        cy.get(`${card} [aria-label="Page ${page}"]`).should('have.attr', 'aria-current', 'page');
    };

    const selectShare = (skill = 1, project = 3) => {
        cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type(`{selectall}Shared Skill ${skill}`);
        // Wait for the debounced search instead of selecting from the initial suggestions.
        cy.get('[data-cy^="skillsSelectionItem-proj1-"]').should('have.length', 1);
        cy.get(option(1, skill)).click();
        cy.get(`${outgoingCard} [data-cy="projectSelector"]`).click();
        cy.get('[data-cy="projectSelector-projectName"]').contains(`Project ${project}`).click();
    };

    const holdNextShareLookup = (fail = false) => {
        const held = { release: null };
        cy.intercept('GET', '/admin/projects/proj1/isSkillShared?*', (request) => {
            if (!held.release && request.query.skillId === 'skill1' && request.query.destinationProjectId === 'proj3') {
                request.alias = 'heldShareLookup';
                return new Cypress.Promise((resolve) => {
                    held.release = () => {
                        if (fail) {
                            request.reply({ statusCode: 500, body: { explanation: 'Share lookup failed' } });
                        } else {
                            request.continue();
                        }
                        resolve();
                    };
                });
            }
        });
        return held;
    };

    const changePageSize = (card, size) => {
        cy.get(`${card} [aria-label="Rows per page"]`).click();
        cy.get('[role="option"]').contains(new RegExp(`^${size}$`)).click();
    };

    const expectProjects = (card, expected) => {
        cy.get(`${card} ${rows} td:nth-child(2) .flex.flex-col > div:first-child`).should(($projects) => {
            expect([...$projects].map((project) => project.textContent.trim())).to.deep.equal(expected);
        });
    };

    beforeEach(() => {
        cy.createProject(1, { name: 'Project 1' });
        cy.createProject(2, { name: 'Project 2' });
        cy.createProject(3, { name: 'Project 3' });
        cy.createSubject(1, 1);
        cy.createSubject(2, 1);
        for (let skill = 1; skill <= 6; skill += 1) {
            cy.createSkill(1, 1, skill, { name: `Shared Skill ${skill}` });
            cy.request('PUT', `/admin/projects/proj1/skills/skill${skill}/shared/projects/proj2`);
        }
        cy.createSkill(2, 1, 1, { name: 'Local destination skill' });
    });

    ['skillName', 'notAField'].forEach((sortBy) => {
        it(`recovers from legacy or invalid outgoing sort ${sortBy}`, () => {
            visit(1, { [outgoingSortKey]: { sortOrder: 1, sortBy } });
            expectFirstSkill(outgoingCard, 'Shared Skill 6');
            cy.reload();
            expectFirstSkill(outgoingCard, 'Shared Skill 6');
        });
    });

    it('recovers from an outgoing query alias saved for the incoming table', () => {
        visit(2, { [incomingSortKey]: { sortOrder: 1, sortBy: 'sharedProject.name' } });
        expectFirstSkill(incomingCard, 'Shared Skill 6');
        cy.reload();
        expectFirstSkill(incomingCard, 'Shared Skill 6');
    });

    it('retains independent valid sort preferences across reloads', () => {
        // Make name ordering differ from ID ordering so the incoming request
        // cannot pass by accidentally using its default skill.skillId sort.
        cy.createSkill(1, 1, 6, { name: 'Aardvark Shared Skill 6' });
        const sorts = {
            [outgoingSortKey]: { sortOrder: 1, sortBy: 'skill.name' },
            [incomingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
        };
        visit(1, sorts);
        expectFirstSkill(outgoingCard, 'Aardvark Shared Skill 6');
        cy.reload();
        expectFirstSkill(outgoingCard, 'Aardvark Shared Skill 6');
        visit(2);
        expectFirstSkill(incomingCard, 'Shared Skill 5');
        cy.reload();
        expectFirstSkill(incomingCard, 'Shared Skill 5');
    });

    [
        { project: 1, card: outgoingCard },
        { project: 2, card: incomingCard },
    ].forEach(({ project, card }) => {
        it(`recovers the ${project === 1 ? 'outgoing' : 'incoming'} table when another session removes the last page`, () => {
            visit(project);
            expectFirstSkill(card, 'Shared Skill 6');
            // Change the dataset without refreshing the current session's paginator.
            cy.request('DELETE', '/admin/projects/proj1/skills/skill1/shared/projects/proj2');
            cy.get(`${card} [aria-label="Page 2"]`).click();
            expectFirstSkill(card, 'Shared Skill 6');
            cy.get(`${card} ${table}`).should('be.visible').and('not.contain', 'Shared Skill 1');
            cy.get(`${card} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
            cy.get(`${card} [aria-label="Page 2"]`).should('not.exist');
            cy.get(`${card} [data-cy="skillsBTableTotalRows"]`).should('have.text', '5');
            cy.get(card).should('not.contain', 'No Skills Available Yet...')
                .and('not.contain', 'Not Selected Yet...');
        });

        it(`shows the empty state when all ${project === 1 ? 'outgoing' : 'incoming'} shares disappear before paging`, () => {
            visit(project);
            expectFirstSkill(card, 'Shared Skill 6');
            for (let skill = 1; skill <= 6; skill += 1) {
                cy.request('DELETE', `/admin/projects/proj1/skills/skill${skill}/shared/projects/proj2`);
            }
            cy.get(`${card} [aria-label="Page 2"]`).click();
            cy.get(card).should('contain', project === 1 ? 'Not Selected Yet...' : 'No Skills Available Yet...');
            cy.get(`${card} ${table}`).should('not.exist');
            cy.get(`${card} [aria-label="Page 1"]`).should('not.exist');
            cy.get(card).should('not.contain', 'Could not load shared skills');
        });

        it(`resets the ${project === 1 ? 'outgoing' : 'incoming'} table to page one when sorting`, () => {
            visit(project);
            expectFirstSkill(card, 'Shared Skill 6');
            cy.get(`${card} [aria-label="Page 2"]`).click();
            expectFirstSkill(card, 'Shared Skill 1', 1);
            cy.get(`${card} th`).contains('Shared Skill').click();
            expectFirstSkill(card, 'Shared Skill 1');
            cy.get(`${card} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
        });

        ['page', 'sort', 'failed page'].forEach((olderRequest) => {
            it(`ignores an older ${olderRequest} response in the ${project === 1 ? 'outgoing' : 'incoming'} table`, () => {
                const endpoint = project === 1 ? 'shared' : 'sharedWithMe';
                let releaseRequest;
                visit(project);
                expectFirstSkill(card, 'Shared Skill 6');

                cy.intercept('GET', `/admin/projects/proj${project}/${endpoint}?*`, (request) => {
                    const isOlderRequest = olderRequest === 'sort'
                        ? request.query.ascending === 'true'
                        : request.query.page === '2';
                    if (isOlderRequest) {
                        request.alias = 'olderTableRequest';
                        return new Cypress.Promise((resolve) => {
                            releaseRequest = () => {
                                if (olderRequest === 'failed page') {
                                    request.reply({ statusCode: 500, body: { explanation: 'Older request failed' } });
                                } else {
                                    request.continue();
                                }
                                resolve();
                            };
                        });
                    }
                    request.alias = 'newerTableRequest';
                });

                if (olderRequest === 'sort') {
                    cy.get(`${card} th`).contains('Shared Skill').click();
                } else {
                    cy.get(`${card} [aria-label="Page 2"]`).click();
                }
                cy.wrap({ ready: () => Boolean(releaseRequest) }).invoke('ready').should('eq', true);

                if (olderRequest === 'sort') {
                    cy.get(`${card} th`).contains('Shared Skill').click();
                    cy.get(`${card} th`).contains('Shared Skill').closest('th')
                        .should('have.attr', 'aria-sort', 'descending');
                } else {
                    cy.get(`${card} [aria-label="Page 1"]`).click();
                }
                cy.wait('@newerTableRequest');
                expectFirstSkill(card, 'Shared Skill 6');
                cy.then(() => releaseRequest());
                cy.wait('@olderTableRequest');
                expectFirstSkill(card, 'Shared Skill 6');
                cy.get(`${card} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
                cy.get(card).should('not.contain', 'Could not load shared skills');
            });
        });

        it(`recovers after the latest ${project === 1 ? 'outgoing' : 'incoming'} table request fails`, () => {
            const endpoint = project === 1 ? 'shared' : 'sharedWithMe';
            visit(project);
            expectFirstSkill(card, 'Shared Skill 6');
            cy.intercept('GET', `/admin/projects/proj${project}/${endpoint}?*`, (request) => {
                if (request.query.page === '2') {
                    request.reply({ statusCode: 500, body: { explanation: 'Latest request failed' } });
                }
            });
            cy.get(`${card} [aria-label="Page 2"]`).click();
            cy.get(card).should('contain', 'Could not load shared skills. Please try again.');
            cy.get(`${card} [aria-label="Page 1"]`).click();
            expectFirstSkill(card, 'Shared Skill 6');
            cy.get(card).should('not.contain', 'Could not load shared skills');
        });
    });

    it('keeps incoming and outgoing page sizes independent across reloads', () => {
        for (let skill = 1; skill <= 6; skill += 1) {
            cy.createSkill(2, 1, skill, { name: `Incoming Skill ${skill}` });
            cy.request('PUT', `/admin/projects/proj2/skills/skill${skill}/shared/projects/proj1`);
        }

        visit();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        expectFirstSkill(incomingCard, 'Incoming Skill 6');

        cy.get(`${outgoingCard} [aria-label="Rows per page"]`).click();
        cy.get('[role="option"]').contains(/^10$/).click();
        expectFirstSkill(outgoingCard, 'Shared Skill 6', 6);
        cy.get(`${incomingCard} [aria-label="Rows per page"]`).should('have.text', '5');
        expectFirstSkill(incomingCard, 'Incoming Skill 6');
        cy.get(`${incomingCard} [aria-label="Page 2"]`).click();
        expectFirstSkill(incomingCard, 'Incoming Skill 1', 1);

        cy.reload();
        cy.get(`${outgoingCard} [aria-label="Rows per page"]`).should('have.text', '10');
        expectFirstSkill(outgoingCard, 'Shared Skill 6', 6);
        cy.get(`${incomingCard} [aria-label="Rows per page"]`).should('have.text', '5');
        expectFirstSkill(incomingCard, 'Incoming Skill 6');
    });

    it('returns to page one after deleting the last row on page two', () => {
        visit();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        cy.get(`${outgoingCard} [aria-label="Page 2"]`).click();
        expectFirstSkill(outgoingCard, 'Shared Skill 1', 1);
        cy.get(`${outgoingCard} [data-cy="sharedSkillsTable-removeBtn"]`).click();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        cy.get(`${outgoingCard} ${table}`).should('not.contain', 'Shared Skill 1');
        cy.get(`${outgoingCard} [aria-label="Page 2"]`).should('not.exist');
    });

    [
        { project: 1, card: outgoingCard },
        { project: 2, card: incomingCard },
    ].forEach(({ project, card }) => {
        it(`keeps all ${project === 1 ? 'outgoing' : 'incoming'} rows and the active page aligned after later-page sorting`, () => {
            createTwelveShares();
            visit(project, {
                [outgoingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
                [incomingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
            });
            expectSkills(card, pagingNames(12, 8), 1);
            cy.get(`${card} [aria-label="Page 3"]`).click();
            expectSkills(card, pagingNames(2, 1), 3);
            cy.get(`${card} th`).contains('Shared Skill').click();
            expectSkills(card, pagingNames(1, 5), 1);
            cy.get(`${card} [aria-label="Page 2"]`).click();
            expectSkills(card, pagingNames(6, 10), 2);
            cy.get(`${card} th`).contains('Shared Skill').click();
            expectSkills(card, pagingNames(12, 8), 1);
            cy.get(`${card} [aria-label="Page 2"]`).click();
            expectSkills(card, pagingNames(7, 3), 2);
        });

        it(`recovers a removed ${project === 1 ? 'outgoing' : 'incoming'} final page while multiple pages remain`, () => {
            createTwelveShares();
            visit(project, {
                [outgoingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
                [incomingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
            });
            expectSkills(card, pagingNames(12, 8), 1);
            // Simulate another session changing the dataset without refreshing this paginator.
            cy.request('DELETE', '/admin/projects/proj1/skills/skill1/shared/projects/proj2');
            cy.request('DELETE', '/admin/projects/proj1/skills/skill2/shared/projects/proj2');
            cy.get(`${card} [aria-label="Page 3"]`).click();
            expectSkills(card, pagingNames(7, 3), 2);
            cy.get(`${card} [data-cy="skillsBTableTotalRows"]`).should('have.text', '10');
            cy.get(`${card} [aria-label="Page 3"]`).should('not.exist');
            cy.get(`${card} [aria-label="Page 1"]`).click();
            expectSkills(card, pagingNames(12, 8), 1);
        });
    });

    it('aligns outgoing rows and the active page after deleting on a later page with multiple pages remaining', () => {
        createTwelveShares();
        visit(1, { [outgoingSortKey]: { sortOrder: -1, sortBy: 'skill.name' } });
        expectSkills(outgoingCard, pagingNames(12, 8), 1);
        cy.get(`${outgoingCard} [aria-label="Page 3"]`).click();
        expectSkills(outgoingCard, pagingNames(2, 1), 3);
        cy.get(`${outgoingCard} ${rows}`).contains('Paging Skill 01').closest('tr')
            .find('[data-cy="sharedSkillsTable-removeBtn"]').click();
        expectSkills(outgoingCard, pagingNames(12, 8), 1);
        cy.get(`${outgoingCard} [data-cy="skillsBTableTotalRows"]`).should('have.text', '11');
        cy.get(`${outgoingCard} [aria-label="Page 3"]`).click();
        expectSkills(outgoingCard, ['Paging Skill 02'], 3);
        cy.get(`${outgoingCard} [data-cy="sharedSkillsTable-removeBtn"]`).click();
        expectSkills(outgoingCard, pagingNames(12, 8), 1);
        cy.get(`${outgoingCard} [aria-label="Page 3"]`).should('not.exist');
        cy.get(`${outgoingCard} [aria-label="Page 2"]`).click();
        expectSkills(outgoingCard, pagingNames(7, 3), 2);
    });

    it('retains the incoming total across pages and page sizes with count left and controls right below the rows', () => {
        createTwelveShares();
        visit(2, { [incomingSortKey]: { sortOrder: -1, sortBy: 'skill.name' } });
        const count = `${incomingCard} [data-cy="skillsBTableTotalRows"]`;
        expectSkills(incomingCard, pagingNames(12, 8), 1);
        cy.get(count).should('be.visible').and('have.text', '12');
        cy.get(`${incomingCard} [aria-label="Page 3"]`).click();
        expectSkills(incomingCard, pagingNames(2, 1), 3);
        cy.get(count).should('have.text', '12');
        changePageSize(incomingCard, 10);
        expectSkills(incomingCard, pagingNames(2, 1), 2);
        cy.get(count).should('have.text', '12');
        cy.get(`${incomingCard} [aria-label="Page 1"]`).click();
        expectSkills(incomingCard, pagingNames(12, 3), 1);
        cy.get(count).should('have.text', '12');
        cy.get(`${incomingCard} ${table}`).should(($table) => {
            const countRect = $table[0].querySelector('[data-pc-section="contentstart"]').getBoundingClientRect();
            const controlsRect = $table[0].querySelector('[aria-label="Pagination Controls for table with id of sharedWithMeSkillsTable"] [data-pc-section="content"]').getBoundingClientRect();
            const tableRect = $table[0].getBoundingClientRect();
            const bodyRect = $table[0].querySelector('tbody').getBoundingClientRect();
            expect(countRect.top).to.be.at.least(bodyRect.bottom);
            expect(controlsRect.top).to.be.at.least(bodyRect.bottom);
            expect(countRect.right).to.be.lessThan(controlsRect.left);
            expect(countRect.left - tableRect.left).to.be.lessThan(32);
            expect(tableRect.right - controlsRect.right).to.be.lessThan(32);
        });
    });

    it('blocks a duplicate share that is not on the visible page', () => {
        visit();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        cy.get(`${outgoingCard} ${table}`).should('not.contain', 'Shared Skill 1');
        selectShare(1, 2);
        cy.get(outgoingCard).should('contain', 'This skill is already shared with project Project 2.');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
        cy.get(`${outgoingCard} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
    });

    it('blocks a targeted share when an off-page share is already global', () => {
        cy.request('DELETE', '/admin/projects/proj1/skills/skill1/shared/projects/proj2');
        cy.request('PUT', '/admin/projects/proj1/skills/skill1/shared/projects/ALL_SKILLS_PROJECTS');
        visit();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        cy.get(`${outgoingCard} ${table}`).should('not.contain', 'Shared Skill 1');
        selectShare();
        cy.get(outgoingCard).should('contain', 'This skill is already shared with All Projects.');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
    });

    it('blocks sharing with all projects when a specific share is off-page', () => {
        visit();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        cy.get(`${outgoingCard} ${table}`).should('not.contain', 'Shared Skill 1');
        cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type('Shared Skill 1');
        cy.get(option(1, 1)).click();
        cy.get(`${outgoingCard} [data-cy="shareWithAllProjectsCheckbox"]`).click();
        cy.get(outgoingCard).should('contain', 'This skill is already shared with project Project 2.');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
    });

    it('disables sharing until the current duplicate-share lookup completes', () => {
        const held = holdNextShareLookup();
        visit();
        selectShare();
        cy.wrap(held).its('release').should('be.a', 'function');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
        cy.then(() => held.release());
        cy.wait('@heldShareLookup');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        cy.get(outgoingCard).should('not.contain', 'already shared');
    });

    it('shows a current lookup failure inline and recovers on a new selection', () => {
        const held = holdNextShareLookup(true);
        visit();
        selectShare();
        cy.wrap(held).its('release').should('be.a', 'function');
        cy.then(() => held.release());
        cy.wait('@heldShareLookup');
        cy.get(outgoingCard).should('contain', 'Could not check whether this skill is already shared.');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
        cy.location('pathname').should('eq', '/administrator/projects/proj1/learning-path');
        cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type('{selectall}Shared Skill 2');
        cy.get(option(1, 2)).click();
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        cy.get(outgoingCard).should('not.contain', 'Could not check whether');
    });

    it('ignores an older failed lookup after a newer selection succeeds', () => {
        const held = holdNextShareLookup(true);
        visit();
        selectShare();
        cy.wrap(held).its('release').should('be.a', 'function');
        cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type('{selectall}Shared Skill 2');
        cy.get(option(1, 2)).click();
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        cy.then(() => held.release());
        cy.wait('@heldShareLookup');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        cy.get(outgoingCard).should('not.contain', 'Could not check whether');
        cy.location('pathname').should('eq', '/administrator/projects/proj1/learning-path');
    });

    ['skill', 'destination'].forEach((selection) => {
        it(`invalidates a pending lookup when the ${selection} is cleared`, () => {
            cy.request('PUT', '/admin/projects/proj1/skills/skill1/shared/projects/proj3');
            const held = holdNextShareLookup();
            visit();
            selectShare();
            cy.wrap(held).its('release').should('be.a', 'function');
            if (selection === 'skill') {
                cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).clear();
            } else {
                cy.get(`${outgoingCard} [data-cy="projectSelector"] [data-pc-section="clearicon"]`).click();
            }
            cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
            cy.then(() => held.release());
            cy.wait('@heldShareLookup');
            cy.get(outgoingCard).should('not.contain', 'already shared');
            cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
            if (selection === 'skill') {
                cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type('Shared Skill 2');
                cy.get(option(1, 2)).click();
            } else {
                cy.get(`${outgoingCard} [data-cy="projectSelector"]`).click();
                cy.get('[data-cy="projectSelector-projectName"]').contains('Project 3').click();
                cy.get(outgoingCard).should('contain', 'This skill is already shared with project Project 3.');
                cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type('{selectall}Shared Skill 2');
                cy.get(option(1, 2)).click();
            }
            cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        });
    });

    it('revalidates the selected skill after its share is deleted', () => {
        visit();
        expectFirstSkill(outgoingCard, 'Shared Skill 6');
        selectShare(6, 2);
        cy.get(outgoingCard).should('contain', 'This skill is already shared with project Project 2.');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.disabled');
        cy.get(`${outgoingCard} ${rows}`).contains('Shared Skill 6').closest('tr')
            .find('[data-cy="sharedSkillsTable-removeBtn"]').click();
        cy.get(`${outgoingCard} ${table}`).should('not.contain', 'Shared Skill 6');
        cy.get(outgoingCard).should('not.contain', 'already shared');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
    });

    it('creates a learning path from a name search outside the initial 200 shared options', () => {
        cy.createSkill(1, 1, 1, { name: 'Nebula prerequisite' });
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);
        for (let skill = 7; skill <= 201; skill += 1) {
            // Keep each subject within its 100-skill limit while preserving sortable IDs.
            cy.createSkill(1, Math.ceil(skill / 100), skill, { skillId: `skill${skill}`, name: `Shared Skill ${skill}` });
            cy.request('PUT', `/admin/projects/proj1/skills/skill${skill}/shared/projects/proj2`);
        }
        visit(2);
        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).click();
        // This option is included but below the visible portion of the scrollable list.
        cy.get(option(1, 2)).should('exist');
        cy.get(option(1, 1)).should('not.exist');
        cy.get(fromInput).type('nEbUlA');
        cy.get(option(1, 1)).should('be.visible').and('contain', 'Nebula prerequisite').click();
        cy.get('[data-cy="learningPathToSkillSelector"] input').click();
        cy.get(option(2, 1)).should('be.visible').click();
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.enabled').click();
        cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill1"]')
            .should('be.visible').and('have.text', 'Nebula prerequisite')
            .and('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="learningPathTable"]').should('contain', 'shared from Project 1');
        cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_skill1"]')
            .should('be.visible').and('have.text', 'Local destination skill')
            .and('have.attr', 'href', '/administrator/projects/proj2/subjects/subj1/skills/skill1');
    });

    [
        { project: 1, card: outgoingCard, otherCard: incomingCard },
        { project: 2, card: incomingCard, otherCard: outgoingCard },
    ].forEach(({ project, card, otherCard }) => {
        it(`changes the ${project === 1 ? 'outgoing' : 'incoming'} page size on a later page without affecting the other table`, () => {
            for (let skill = 1; skill <= 12; skill += 1) {
                cy.createSkill(1, 1, skill, { name: `Paging Skill ${String(skill).padStart(2, '0')}` });
                if (skill > 6) {
                    cy.request('PUT', `/admin/projects/proj1/skills/skill${skill}/shared/projects/proj2`);
                }
            }
            for (let skill = 1; skill <= 6; skill += 1) {
                cy.createSkill(2, 1, skill, { name: `Reverse Skill ${skill}` });
                cy.request('PUT', `/admin/projects/proj2/skills/skill${skill}/shared/projects/proj1`);
            }
            visit(project, {
                [outgoingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
                [incomingSortKey]: { sortOrder: -1, sortBy: 'skill.name' },
            });
            expectFirstSkill(card, 'Paging Skill 12');
            expectFirstSkill(otherCard, 'Reverse Skill 6');
            cy.get(`${card} [aria-label="Page 3"]`).click();
            expectFirstSkill(card, 'Paging Skill 02', 2);
            changePageSize(card, 10);
            cy.get(`${card} [aria-label="Rows per page"]`).should('have.text', '10');
            expectFirstSkill(card, 'Paging Skill 02', 2);
            cy.get(`${card} [aria-label="Page 2"]`).should('have.attr', 'aria-current', 'page');
            cy.get(`${card} [aria-label="Page 3"]`).should('not.exist');
            cy.get(`${card} [aria-label="Page 1"]`).click();
            expectFirstSkill(card, 'Paging Skill 12', 10);
            cy.get(`${otherCard} [aria-label="Rows per page"]`).should('have.text', '5');
            cy.get(`${otherCard} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
            expectFirstSkill(otherCard, 'Reverse Skill 6');
        });
    });

    it('sorts outgoing projects in both directions including All Projects and resets the page', () => {
        cy.updateProject(2, { name: 'Zulu Project' });
        cy.updateProject(3, { name: 'Alpha Project' });
        cy.request('PUT', '/admin/projects/proj1/skills/skill6/shared/projects/proj3');
        cy.createSkill(1, 1, 7, { name: 'Shared Skill 7' });
        cy.request('PUT', '/admin/projects/proj1/skills/skill7/shared/projects/ALL_SKILLS_PROJECTS');
        visit();
        cy.get(`${outgoingCard} [aria-label="Page 2"]`).click();
        cy.get(`${outgoingCard} ${rows}`).should('have.length', 3);
        cy.get(`${outgoingCard} th`).contains(/^Project$/).click();
        cy.get(`${outgoingCard} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
        expectProjects(outgoingCard, ['Alpha Project', ...Array(4).fill('Zulu Project')]);
        changePageSize(outgoingCard, 10);
        expectProjects(outgoingCard, ['Alpha Project', ...Array(6).fill('Zulu Project'), 'All Projects']);
        cy.get(`${outgoingCard} th`).contains(/^Project$/).click();
        expectProjects(outgoingCard, ['All Projects', ...Array(6).fill('Zulu Project'), 'Alpha Project']);
    });

    it('sorts incoming source projects in both directions and resets the page', () => {
        cy.updateProject(1, { name: 'Zulu Project' });
        cy.updateProject(3, { name: 'Alpha Project' });
        cy.createSubject(3, 1);
        cy.createSkill(3, 1, 1, { name: 'Alpha source skill' });
        cy.request('PUT', '/admin/projects/proj3/skills/skill1/shared/projects/proj2');
        visit(2);
        cy.get(`${incomingCard} [aria-label="Page 2"]`).click();
        cy.get(`${incomingCard} ${rows}`).should('have.length', 2);
        cy.get(`${incomingCard} th`).contains(/^Project$/).click();
        cy.get(`${incomingCard} [aria-label="Page 1"]`).should('have.attr', 'aria-current', 'page');
        expectProjects(incomingCard, ['Alpha Project', ...Array(4).fill('Zulu Project')]);
        changePageSize(incomingCard, 10);
        expectProjects(incomingCard, ['Alpha Project', ...Array(6).fill('Zulu Project')]);
        cy.get(`${incomingCard} th`).contains(/^Project$/).click();
        expectProjects(incomingCard, [...Array(6).fill('Zulu Project'), 'Alpha Project']);
    });

    it('ignores an older duplicate-share lookup after a different skill is selected', () => {
        let releaseLookup;
        cy.request('PUT', '/admin/projects/proj1/skills/skill1/shared/projects/proj3');
        cy.intercept('GET', '/admin/projects/proj1/isSkillShared?*', (request) => {
            if (request.query.skillId === 'skill1') {
                request.alias = 'oldLookup';
                return new Cypress.Promise((resolve) => {
                    releaseLookup = () => {
                        request.continue();
                        resolve();
                    };
                });
            }
        });
        visit();
        selectShare();
        cy.wrap({ ready: () => Boolean(releaseLookup) }).invoke('ready').should('eq', true);
        cy.get(`${outgoingCard} [data-cy="skillsSelector"] input`).type('{selectall}Shared Skill 2');
        cy.get(option(1, 2)).click();
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        cy.then(() => releaseLookup());
        cy.wait('@oldLookup');
        cy.get(`${outgoingCard} [data-cy="shareButton"]`).should('be.enabled');
        cy.get(outgoingCard).should('not.contain', 'already shared');
    });

    it('groups prerequisite options only when shared skills are included and hides empty groups', () => {
        const groups = '[data-pc-section="optiongroup"]';
        cy.createBadge(2, 1, { name: 'Local destination badge' });
        cy.assignSkillToBadge(2, 1, 1);
        cy.enableBadge(2, 1, { name: 'Local destination badge' });
        visit(2);
        cy.get(fromInput).click();
        cy.get(option(2, 1)).should('be.visible');
        cy.get('[data-cy="skillsSelectionItem-proj2-badge1"]').should('be.visible');
        cy.get(groups).should('not.exist');
        cy.get('[data-cy^="skillsSelectionItem-proj1-"]').should('not.exist');

        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).click();
        cy.get(option(1, 6)).scrollIntoView().should('be.visible');
        cy.get(groups).should('have.length', 2);
        cy.get(groups).eq(0).should('have.text', 'This Project');
        cy.get(groups).eq(1).should('have.text', 'Shared Skills');

        cy.get(fromInput).type('Local');
        cy.get('[data-cy^="skillsSelectionItem-proj1-"]').should('not.exist');
        cy.get(option(2, 1)).should('be.visible');
        cy.get('[data-cy="skillsSelectionItem-proj2-badge1"]').should('be.visible');
        cy.get(groups).should('have.length', 1).and('have.text', 'This Project');

        cy.get(fromInput).type('{selectall}skill2');
        cy.get(option(1, 2)).should('be.visible');
        cy.get(option(2, 1)).should('not.exist');
        cy.get(groups).should('have.length', 1).and('have.text', 'Shared Skills');

        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).type('{selectall}Local');
        cy.get(option(2, 1)).should('be.visible');
        cy.get('[data-cy="skillsSelectionItem-proj2-badge1"]').should('be.visible');
        cy.get(groups).should('not.exist');
        cy.get('[data-cy^="skillsSelectionItem-proj1-"]').should('not.exist');
    });

    [false, true].forEach((includeShared) => {
        it(`clears search loading for empty and repeated queries and restores options after no matches (shared=${includeShared})`, () => {
            visit(2);
            if (includeShared) {
                cy.get('[data-cy="includeShared"]').click();
            }
            cy.get(fromInput).click();
            cy.get(option(2, 1)).should('be.visible');
            if (includeShared) {
                cy.get(option(1, 6)).should('exist');
            }
            const spinner = '[data-cy="learningPathFromSkillSelector"] [data-pc-section="loader"]';
            cy.get(spinner).should('not.exist');
            // Advance the real autocomplete debounce deterministically, including
            // same-query completions that do not issue another HTTP request.
            cy.clock(Date.now(), ['setTimeout', 'clearTimeout']);
            cy.get(fromInput).type('Local');
            cy.tick(500);
            cy.get(option(2, 1)).should('be.visible');
            cy.get(spinner).should('not.exist');
            cy.get(fromInput).type('{selectall}Local');
            cy.tick(500);
            cy.get(option(2, 1)).should('be.visible');
            cy.get(spinner).should('not.exist');
            cy.get(fromInput).type('{selectall}no matching prerequisite');
            cy.tick(500);
            cy.get('[data-pc-section="emptymessage"]').should('be.visible')
                .and('contain', 'No results found. Consider changing the search query');
            cy.get('[data-cy^="skillsSelectionItem-"]').should('not.exist');
            cy.get(spinner).should('not.exist');
            cy.get(fromInput).clear();
            // Flush the autocomplete's deferred close before reopening the popup.
            cy.tick(0);
            // Clearing closes the popup; reopen it through the selector's dropdown.
            cy.get('[data-cy="learningPathFromSkillSelector"] button[aria-label="click to select a skill"]').click();
            cy.tick(500);
            cy.get(option(2, 1)).should('be.visible');
            if (includeShared) {
                cy.get(option(1, 6)).should('exist');
                cy.get('[data-pc-section="optiongroup"]').should('have.length', 2);
            } else {
                cy.get('[data-pc-section="optiongroup"]').should('not.exist');
            }
            cy.get(spinner).should('not.exist');
            cy.get(fromInput).blur().focus();
            cy.tick(500);
            cy.get(option(2, 1)).should('be.visible');
            cy.get(spinner).should('not.exist');
            cy.clock().invoke('restore');
            cy.get(option(2, 1)).click();
            cy.get('[data-cy="learningPathToSkillSelector"] input').should('be.enabled');
        });
    });

    it('ignores an older shared search after a newer search completes', () => {
        let releaseSearch;
        cy.intercept('GET', '/admin/projects/proj2/sharedWithMe?*', (request) => {
            if (request.query.filter === 'skill1') {
                request.alias = 'oldSearch';
                return new Cypress.Promise((resolve) => {
                    releaseSearch = () => {
                        request.continue();
                        resolve();
                    };
                });
            }
        });
        visit(2);
        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).type('skill1');
        cy.wrap({ ready: () => Boolean(releaseSearch) }).invoke('ready').should('eq', true);
        cy.get(fromInput).type('{selectall}skill2');
        cy.get(option(1, 2)).should('be.visible');
        cy.then(() => releaseSearch());
        cy.wait('@oldSearch');
        cy.get(option(1, 2)).should('be.visible');
        cy.get(option(1, 1)).should('not.exist');
    });

    it('ignores an older shared search failure after a newer search succeeds', () => {
        let releaseSearch;
        cy.intercept('GET', '/admin/projects/proj2/sharedWithMe?*', (request) => {
            if (request.query.filter === 'skill1') {
                request.alias = 'oldFailedSearch';
                return new Cypress.Promise((resolve) => {
                    releaseSearch = () => {
                        request.reply({ statusCode: 500, body: { explanation: 'Older search failed' } });
                        resolve();
                    };
                });
            }
        });
        visit(2);
        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).type('skill1');
        cy.wrap({ ready: () => Boolean(releaseSearch) }).invoke('ready').should('eq', true);
        cy.get(fromInput).type('{selectall}skill2');
        cy.get(option(1, 2)).should('be.visible');
        cy.then(() => releaseSearch());
        cy.wait('@oldFailedSearch');
        cy.location('pathname').should('eq', '/administrator/projects/proj2/learning-path');
        cy.get(option(1, 2)).should('be.visible');
        cy.get(option(1, 1)).should('not.exist');
        cy.get('[data-cy="sharedSkillsSearchError"]').should('not.exist');
        cy.get(option(1, 2)).click();
        cy.get('[data-cy="learningPathToSkillSelector"] input').should('be.enabled');
    });

    it('ignores a shared search failure after shared skills are toggled off', () => {
        let releaseSearch;
        cy.intercept('GET', '/admin/projects/proj2/sharedWithMe?*', (request) => {
            if (request.query.filter === 'skill1') {
                request.alias = 'disabledFailedSearch';
                return new Cypress.Promise((resolve) => {
                    releaseSearch = () => {
                        request.reply({ statusCode: 500, body: { explanation: 'Disabled search failed' } });
                        resolve();
                    };
                });
            }
        });
        visit(2);
        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).type('skill1');
        cy.wrap({ ready: () => Boolean(releaseSearch) }).invoke('ready').should('eq', true);
        cy.get('[data-cy="includeShared"]').click();
        cy.then(() => releaseSearch());
        cy.wait('@disabledFailedSearch');
        cy.location('pathname').should('eq', '/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="includeShared"] input').should('not.be.checked');
        cy.get('[data-cy="sharedSkillsSearchError"]').should('not.exist');
        cy.get(fromInput).type('{selectall}Local');
        cy.get(option(2, 1)).should('be.visible');
        cy.get(option(1, 1)).should('not.exist');
        cy.get(option(2, 1)).click();
        cy.get('[data-cy="learningPathToSkillSelector"] input').should('be.enabled');
    });

    it('shows the current shared search failure inline and recovers on a new search', () => {
        cy.intercept('GET', '/admin/projects/proj2/sharedWithMe?*', (request) => {
            if (request.query.filter === 'Local') {
                request.reply({ statusCode: 500, body: { explanation: 'Current search failed' } });
            }
        });
        visit(2);
        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).type('Local');
        cy.get('[data-cy="sharedSkillsSearchError"]').should('be.visible')
            .and('contain', 'Could not load shared skills. Change the search to try again.');
        cy.location('pathname').should('eq', '/administrator/projects/proj2/learning-path');
        cy.get(option(2, 1)).should('be.visible');
        cy.get(fromInput).type('{selectall}skill2');
        cy.get(option(1, 2)).should('be.visible');
        cy.get('[data-cy="sharedSkillsSearchError"]').should('not.exist');
    });

    it('does not reintroduce shared options when sharing is toggled off during a search', () => {
        let releaseSearch;
        cy.intercept('GET', '/admin/projects/proj2/sharedWithMe?*', (request) => {
            if (request.query.filter === 'skill1') {
                request.alias = 'disabledSearch';
                return new Cypress.Promise((resolve) => {
                    releaseSearch = () => {
                        request.continue();
                        resolve();
                    };
                });
            }
        });
        visit(2);
        cy.get('[data-cy="includeShared"]').click();
        cy.get(fromInput).type('skill1');
        cy.wrap({ ready: () => Boolean(releaseSearch) }).invoke('ready').should('eq', true);
        cy.get('[data-cy="includeShared"]').click();
        cy.then(() => releaseSearch());
        cy.wait('@disabledSearch');
        cy.get(fromInput).click();
        cy.get(option(1, 1)).should('not.exist');
    });
});
