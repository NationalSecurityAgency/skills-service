/*
 * Copyright 2025 SkillTree
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
describe('Global Badge on P&R pages', () => {


    beforeEach(() => {
    })

    it('global badge with only levels', function () {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createProject(2);

        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1, 2);
        cy.assignProjectToGlobalBadge(1, 2, 1);
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeDetailsLink_globalBadge1"]').click();
        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectName"]').contains('This is project 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectLevel"]').contains('Level 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="percentComplete"]').should('have.text', '100');

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectName"]').contains('This is project 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectLevel"]').contains('Level 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="percentComplete"]').should('have.text', '0');
    })

    it('global badge only with skills', function () {
        cy.createProject()
        cy.createSubject()
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 3);
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1, )
        cy.assignSkillToGlobalBadge(1, 2, 1, )
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeDetailsLink_globalBadge1"]').click();
        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0');
    })

    it('achieved global badge with only levels', function () {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1, 2);
        cy.assignProjectToGlobalBadge(1, 2, 1);
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))
        cy.reportSkill(2, 1, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="achievedBadges"] [data-cy="achievedBadge-globalBadge1"] [data-cy="badgeName"]').contains('Global Badge 1');
        cy.get('[data-cy="achievedBadges"] [data-cy="earnedBadgeLink_globalBadge1"]').click();

        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('100% Complete')

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectName"]').contains('This is project 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectLevel"]').contains('Level 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="percentComplete"]').should('have.text', '100');

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectName"]').contains('This is project 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectLevel"]').contains('Level 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="percentComplete"]').should('have.text', '100');
    })

    it('earned global badge only with skills', function () {
        cy.createProject()
        cy.createSubject()
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 3);
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1, )
        cy.assignSkillToGlobalBadge(1, 2, 1, )
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))
        cy.reportSkill(1, 2, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="achievedBadges"] [data-cy="achievedBadge-globalBadge1"] [data-cy="badgeName"]').contains('Global Badge 1');
        cy.get('[data-cy="achievedBadges"] [data-cy="earnedBadgeLink_globalBadge1"]').click();
        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('100% Complete')

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100');
    })

    it('Ability to claim points for Global Badge Honor skill from another project - by navigation to the skill', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);


        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')
        cy.get('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.get('[data-cy="claimPointsBtn"]').click()
        cy.get('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.get('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.get('[data-cy="achievementDate"]')

        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1/crossProject/proj1/skill1');
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.get('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.get('[data-cy="achievementDate"]')
    })

    it('Ability to claim points for Global Badge Honor skill from another project - by going directly to the skill', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1/crossProject/proj1/skill2');
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')
        cy.get('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.get('[data-cy="claimPointsBtn"]').click()
        cy.get('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.get('[data-cy="skillCompletedCheck-skill2=globalBadge1"]')
        cy.get('[data-cy="achievementDate"]')

        // reload
        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1/crossProject/proj1/skill2');
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.get('[data-cy="skillCompletedCheck-skill2=globalBadge1"]')
        cy.get('[data-cy="achievementDate"]')
    })

    it('Ability to claim points for Global Badge Honor skill from another project - from the badge page', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1');
        cy.get('[data-cy="toggleSkillDetails"]').click()
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="claimPointsBtn"]').should('be.enabled')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="claimPointsBtn"]').click()
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.get('[data-cy="skillCompletedCheck-skill2=globalBadge1"]')

        // reload
        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1/crossProject/proj1/skill2');
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillProgressTitle-skill2=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')
        cy.get('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.get('[data-cy="skillCompletedCheck-skill2=globalBadge1"]')
        cy.get('[data-cy="achievementDate"]')
    })

    it('Quiz attempt history is displayed for skills in Global Badge from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [1]}])

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.get('[data-cy="approvalHistoryTimeline"]').contains('Failed')

        // refresh and validate
        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1/crossProject/proj1/skill1');
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.get('[data-cy="approvalHistoryTimeline"]').contains('Failed')
    })

    it('can view quiz results for skills in Global Badge from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}])

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.get('[data-cy="quizCompletedMsg"]').contains('You have passed')
        cy.get('[data-cy="viewQuizAttemptInfo"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"]')

        // refresh and validate
        cy.visit('/progress-and-rankings/projects/proj2/badges/global/globalBadge1/crossProject/proj1/skill1');
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.get('[data-cy="quizCompletedMsg"]').contains('You have passed')
        cy.get('[data-cy="viewQuizAttemptInfo"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"]')
    })

    it('skills-client: Ability to claim points for Global Badge Honor skill from another project', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 2')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj2%2Fskill3');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 2')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill3=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')


        // directly load the skill
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj1%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // refresh
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj1%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Ability to claim points for Global Badge Honor skill from same project', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'HonorSystem'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // refresh
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2Fskills%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // directly load the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2Fskills%2Fskill2');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2=globalBadge1"]').contains('Very Great Skill 2')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill2=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="claimPointsBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('You just earned 100 points and completed the skill')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill2=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Ability to request approval for Global Badge skill from another project', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 2')

        cy.wrapIframe().find('[data-cy="requestApprovalBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="requestApprovalBtn"]').click()
        cy.wrapIframe().find('[data-cy="selfReportSubmitBtn"]').click()

        cy.wrapIframe().find('[data-cy="selfReportAlert"]').contains('This skill requires approval from a project administrator')
        cy.wrapIframe().find('[data-cy="skillProgress-ptsOverProgressBard"] [data-cy="approvalPending"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj2%2Fskill3');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"]').contains('Very Great Skill 3')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill3=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 2')

        cy.wrapIframe().find('[data-cy="requestApprovalBtn"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="skillProgress-ptsOverProgressBard"] [data-cy="approvalPending"]')
    })

    it('skills-client: Ability to achieve skill for Global Badge by watching a video from another project', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.wrapIframe().find('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.wrapIframe().find('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.wrapIframe().find('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.wrapIframe().find('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.wrapIframe().find('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.wrapIframe().find('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.wrapIframe().find('[data-cy="viewTranscriptBtn"]')
        cy.wrapIframe().find('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')

        // refresh and validate
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj1%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Ability to achieve skill for Global Badge by taking a quiz from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.wrapIframe().find('[data-cy="takeQuizBtn"]').click();

        cy.wrapIframe().find('[data-cy="title"]').contains('Quiz')
        cy.wrapIframe().find('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

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

        cy.wrapIframe().find('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.wrapIframe().find('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.wrapIframe().find('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj1%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
        cy.wrapIframe().find('[data-cy="skillCompletedCheck-skill1=globalBadge1"]')
        cy.wrapIframe().find('[data-cy="achievementDate"]')
    })

    it('skills-client: Quiz attempt history is displayed for skills in Global Badge from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [1]}])

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="approvalHistoryTimeline"]').contains('Failed')

        // refresh and validate
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj1%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="approvalHistoryTimeline"]').contains('Failed')
    })

    it('skills-client: can view quiz results for skills in Global Badge from another project', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}])

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 3, { numPerformToCompletion: 1, selfReportingType: 'Approval'});

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1 )
        cy.assignSkillToGlobalBadge(1, 2, 1)
        cy.assignSkillToGlobalBadge(1, 3, 2)
        cy.enableGlobalBadge(1);

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);

        // navigate to the skill
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1');
        cy.wrapIframe().find('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="quizCompletedMsg"]').contains('You have passed')
        cy.wrapIframe().find('[data-cy="viewQuizAttemptInfo"]').click()
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"]')

        // refresh and validate
        cy.visit('/test-skills-client/proj2?skillsClientDisplayPath=%2Fbadges%2Fglobal%2FglobalBadge1%2FcrossProject%2Fproj1%2Fskill1');
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"]').contains('Very Great Skill 1')
        cy.wrapIframe().find('[data-cy="skillProgressTitle-skill1=globalBadge1"] [data-cy="skillProjectName"]').contains('This is project 1')

        cy.wrapIframe().find('[data-cy="quizCompletedMsg"]').contains('You have passed')
        cy.wrapIframe().find('[data-cy="viewQuizAttemptInfo"]').click()
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"]')
    })
})