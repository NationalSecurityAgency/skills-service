/**
 * Copyright 2021 SkillTree
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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillTests extends DefaultIntSpec {

    def "add skill to catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1)
        then:
        res
        res.totalCount == 1
        res.data[0].skillId == skill.skillId
        res.data[0].projectId == skill.projectId
        res.data[0].projectName == project1.name
        res.data[0].description == skill.description
        res.data[0].pointIncrement == skill.pointIncrement
        res.data[0].numPerformToCompletion == skill.numPerformToCompletion
    }

    def "bulk export skills to catalog"() {

    }

    def "update skill that has been exported to catalog"() {
        //changes should be reflected across all copies
    }

    def "update skill imported from catalog"() {
        //should fail
    }

    def "remove skill from catalog"() {
        // should result in deletion of all copies from any projects that have imported the skill
        false
    }

    def "bulk import skills from catalog"() {

    }

    def "import skill from catalog"() {
        //validate that totalPoints for subject and project are properly updated
    }

    def "import skill from catalog twice"() {
        //should result in error
    }

    def "remove imported skill, should have no impact on original skill" () {

    }

    def "report skill event on imported skill, should be reflected in all copies"() {

    }

    def "report skill event on original exported skill, should be reflected on all copies"() {

    }

    def "report skill event on original exported skill when original project has insufficient points"() {

    }

    def "report skill event on imported skill when importing project has insufficient points"() {

    }

    def "export skill to catalog when project has insufficient points"() {

    }

    def "report self-report approval request on skill imported from catalog"() {
        //only one copy of approval request should be generated and should be
        //generated only on original catalog skill to that project's approver
    }

    def "delete user skill event for skill imported from catalog"() {
        //should be replicated to all copies
    }

    def "delete user skill event for skill exported to catalog"() {
        //should be replicated to all copies
    }

    def "get all skills exported by project"() {
        //test paging
    }

    def "get all skills imported to project"() {
        //test paging
    }

    def "get exported to catalog stats for project"() {

    }

    def "get imported from catalog stats for project"() {

    }

    def "get exported skill usage stats"() {

    }

    def "get all catalog skills available to project"() {
        //should not include skills exported to catalog by this project
        //or skills already imported from the catalog by this project
        //helpUrl and description should be populated
        //filtering on projectName, subjectName, skillName all need to be validated as well
        //paging needs to be tested

    }

    //TBD
    //skills imported from catalog should have readOnly attribute as true when loading skills or individual skill
    //that has been imported from the catalog
    //need to add tests to verify where and when copiedFromProjectId/copiedFromProjectName are set
    //need to add tests to verify the sharedToCatalog attribute is set
    //need to add tests to verify the subjectId and subjectName attributes are set
    //getting skills for subject should properly populate sharedToCatalogAttribute
    //getting skills for subject should properly populate projectName attribute when a skill has been imported from the catalog
    // --- copiedFrom attribute should also not be present
}
