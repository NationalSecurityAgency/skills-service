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

const moment = require('moment-timezone');

describe('Skill Reuse Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
    });

    it('reuse skill into a group under the same subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.createSkillsGroup(1, 1, 11);
        cy.createSkillsGroup(1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group11"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 11 Subj1] group');
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills');
        cy.get('[data-cy="okButton"]')
            .click();
    });
});