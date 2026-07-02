/*
 * Copyright 2026 SkillTree
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

describe('Project Admin Training Wide Search Tests', () => {

    it('navigate to skills, subjects, and badges using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="manageBtn_subj1"]')
        cy.get('[data-cy="projectLastReportedSkillValue"]').contains('Never')

        // navigate to subject 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Subject 1');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text','SUBJECT: Subject 1')

        // navigate to skill 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Very Great Skill 1');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text', 'SKILL: Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        // navigate to badge 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('badge 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Badge 1');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('BADGE: Badge 1')
    })

    it('navigate from one subject to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 3)

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('SUBJECT: Subject 1')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')

        // navigate to subject 2
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject 2')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Subject 2');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('SUBJECT: Subject 2')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: subj2');
        cy.get('[data-cy="manageSkillLink_skill3Subj2"]')
    })

    it('navigate from one skill to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { pointIncrement: 111 })
        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 3, { pointIncrement: 112 })

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('SKILL: Very Great Skill 1')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: skill1');
        cy.get('[data-cy="skillOverviewTotalpoints"] [data-cy="mediaInfoCardTitle"]').should('have.text', '222 Points');

        // navigate to another skill
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 3')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Skill 3');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('SKILL: Very Great Skill 3 Subj2')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: skill3Subj2');
        cy.get('[data-cy="skillOverviewTotalpoints"] [data-cy="mediaInfoCardTitle"]').should('have.text', '224 Points');
    })

    it('navigate from one badge to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 2);

        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('BADGE: Badge 1')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: badge1');
        cy.get('[data-cy="manage_skill1"]')

        // navigate to badge 2
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('badge 2')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Badge 2');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('BADGE: Badge 2')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: badge2');
        cy.get('[data-cy="manage_skill2"]')
    })

    it('navigate to skills group using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkillsGroup(1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 2)
        cy.addSkillToGroup(1, 1, 1, 3)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text', 'PROJECT: This is project 1');

        // navigate to skills group 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('awesome group 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Awesome Group 1');
        cy.realPress('Enter');
        cy.url().should('include', '/subjects/subj1/groups/group1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('GROUP: Awesome Group 1')
        cy.get('[data-cy="requiredAllSkills"]')
        cy.get('[data-cy="manageSkillLink_skill1"]')
    })

    it('navigate from one skills group to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkillsGroup(1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 2)

        cy.createSkillsGroup(1, 1, 2)
        cy.addSkillToGroup(1, 1, 2, 3)

        cy.visit('/administrator/projects/proj1/subjects/subj1/groups/group1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('GROUP: Awesome Group 1')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: group1');
        cy.get('[data-cy="requiredAllSkills"]')
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="manageSkillLink_skill3"]').should('not.exist')


        // navigate to badge 2
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('group 2')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Group 2');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('GROUP: Awesome Group 2')
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').should('have.text', 'ID: group2');
        cy.get('[data-cy="requiredAllSkills"]')
        cy.get('[data-cy="manageSkillLink_skill3"]')
        cy.get('[data-cy="manageSkillLink_skill1"]').should('not.exist')
        cy.get('[data-cy="manageSkillLink_skill2"]').should('not.exist')
    })

    it('point and skill counts on search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { pointIncrement: 111 })
        cy.createSkill(1, 1, 2, { pointIncrement: 112 })
        cy.createSkill(1, 1, 3, { pointIncrement: 113 })
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 4)

        cy.createSubject(1, 3)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 1);
        cy.assignSkillToBadge(1, 2, 2);

        cy.createBadge(1, 3);

        cy.createSkillsGroup(1, 1, 10)
        cy.addSkillToGroup(1, 1, 10, 11)
        cy.addSkillToGroup(1, 1, 10, 12, { pointIncrement: 3333 })

        cy.createSkillsGroup(1, 1, 20)
        cy.addSkillToGroup(1, 1, 20, 13)

        cy.createSkillsGroup(1, 1, 30)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="manageBtn_subj1"]')
        cy.get('[data-cy="projectLastReportedSkillValue"]').contains('Never')

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')

        // subject counts
        cy.get('input.p-listbox-filter').type('subject')
        cy.get('[data-cy="searchRes-subj1"] [data-cy="points"]').should('have.text', '6 Skills')
        cy.get('[data-cy="searchRes-subj2"] [data-cy="points"]').should('have.text', '1 Skill')
        cy.get('[data-cy="searchRes-subj3"] [data-cy="points"]').should('have.text', '0 Skills')

        // badge counts
        cy.get('input.p-listbox-filter').type('{selectAll}adge')
        cy.get('[data-cy="searchRes-badge1"] [data-cy="points"]').should('have.text', '1 Skill')
        cy.get('[data-cy="searchRes-badge2"] [data-cy="points"]').should('have.text', '2 Skills')
        cy.get('[data-cy="searchRes-badge3"] [data-cy="points"]').should('have.text', '0 Skills')

        // group counts
        cy.get('input.p-listbox-filter').type('{selectAll}ROUP')
        cy.get('[data-cy="searchRes-group10"] [data-cy="points"]').should('have.text', '2 Skills')
        cy.get('[data-cy="searchRes-group20"] [data-cy="points"]').should('have.text', '1 Skill')
        cy.get('[data-cy="searchRes-group30"] [data-cy="points"]').should('have.text', '0 Skills')

        // skills
        // group counts
        cy.get('input.p-listbox-filter').type('{selectAll}sKIll 1')
        cy.get('[data-cy="searchRes-skill1"] [data-cy="points"]').should('have.text', '222 Points')
        cy.get('[data-cy="searchRes-skill11"] [data-cy="points"]').should('have.text', '200 Points')
        cy.get('[data-cy="searchRes-skill12"] [data-cy="points"]').should('have.text', '6,666 Points')
        cy.get('[data-cy="searchRes-skill13"] [data-cy="points"]').should('have.text', '200 Points')
    })

    it('no results after filtering training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text', 'PROJECT: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('li.p-listbox-empty-message').should('not.exist')
        cy.get('input.p-listbox-filter').type('xyz')
        cy.get('li.p-listbox-empty-message').contains('No results found').should('be.visible')
    })

    it('no results for empty project in training-wide search dialog', () => {
        cy.createProject(1)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text', 'PROJECT: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('li.p-listbox-empty-message').contains('No results found').should('be.visible')
    })

    it('navigate using the mouse and keyboard', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text', 'PROJECT: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject')
        cy.get('[data-cy="searchRes-subj1"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('Subject 1')

        cy.realPress(["Control", "k"]);
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Very Great Skill 1');
        cy.realPress('Enter');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').contains('SKILL: Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')
    })

    it('close training-wide search dialog', () => {
        cy.createProject(1)

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="pageHeader"] [data-cy="title"]').should('have.text', 'PROJECT: This is project 1');

        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.realPress('Escape');
        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
    })

    it('search dialog on admin side does not honor custom labels', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'Course',
                setting: 'subject.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Assessment',
                setting: 'skill.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Cluster',
                setting: 'group.displayName',
                projectId: 'proj1',
            },
        ]);

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()

        cy.get('input.p-listbox-filter')
          .invoke('attr', 'placeholder')
          .should('contain', 'Search for Subjects, Skills, Groups or Badges');
        cy.get('[data-cy="subjectName"]').first().should('have.text', "Subject:Subject 1");
    });

})