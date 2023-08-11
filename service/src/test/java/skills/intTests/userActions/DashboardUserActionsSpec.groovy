/**
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
package skills.intTests.userActions

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.catalog.CatalogIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.model.UserActionsHistory
import skills.storage.repos.UserActionsHistoryRepo

import static skills.intTests.utils.SkillsFactory.*

class DashboardUserActionsSpec extends CatalogIntSpec {

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    def "track project, subject and skills CRUD"() {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        p1.description = "this is a description allright"
        def p1subj1 = createSubject(1, 1)
        p1subj1.description = "this is a description allright"
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            it.description = "this is a description all right"
        }

        when:
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.updateProject(p1)
        skillsService.updateSubject(p1subj1)
        skillsService.updateSkill(p1Skills[0])
        p1Skills.each {
            skillsService.deleteSkill(it)
        }
        skillsService.deleteSubject(p1subj1)
        skillsService.deleteProject(p1.projectId)

        def res = rootService.getUserActionsForEverything()
        println JsonOutput.prettyPrint(JsonOutput.toJson(res))

        Long idToLoad = res.data.find { it.action == "Edit" }.id
        def singleActionAttributes = rootService.getUserActionAttributes(idToLoad)
        println JsonOutput.prettyPrint(JsonOutput.toJson(singleActionAttributes))

//        List<UserActionsHistory> history = userActionsHistoryRepo.findAll()
//        println JsonOutput.prettyPrint(JsonOutput.toJson(history))

        then:
        true
    }
}
