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
package skills.intTests.community

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class CommunityAndSkillsCatalogSpecs extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    def "projects with a protected community are not allowed to export skills"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        pristineDragonsUser.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Projects with the community protection are not allowed to export skills to the catalog")
    }

    def "cannot enable community for a project if project has exported skills"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        pristineDragonsUser.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)

        p1.enableProtectedUserCommunity = true
        when:
        pristineDragonsUser.updateProject(p1)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("Has skill(s) that have been exported to the Skills Catalog")
    }

    def "areSkillIdsExportable endpoint - projects with a protected community are not allowed to export skills"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def res = pristineDragonsUser.areSkillIdsExportable(p1.projectId, [p1Skills[0].skillId])
        def res1 = pristineDragonsUser.areSkillIdsExportable(p2.projectId, [p2Skills[0].skillId])
        then:
        res.isUserCommunityRestricted == true
        res1.isUserCommunityRestricted == false
    }

    def "prevent export of skills with invalid descriptions to catalog"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills[2].description = "jabberwocky"
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        jdbcTemplate.execute("delete from settings where project_id='${p1.projectId}' and setting='user_community'")

        when:
        pristineDragonsUser.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill description is invalid")
        e.message.contains("errorCode:ParagraphValidationFailed")
        e.message.contains("skillId:${p1Skills[2].skillId}")
        e.message.contains("projectId:${p1.projectId}")
    }
}
