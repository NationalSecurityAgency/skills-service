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
import skills.storage.repos.ProjectParticipationRepo

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class CommunityAndSkillsCatalogSpecs extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    ProjectParticipationRepo participationRepo

    def "source reports after finalization propagate community member credit but never create nonmember destination credit"() {
        List<String> users = getRandomUsers(3)
        SkillsService owner = createService(users[0])
        SkillsService member = createService(users[1])
        SkillsService nonmember = createService(users[2])
        SkillsService root = createRootSkillService()
        [owner, member].each { SkillsService user ->
            root.saveUserTag(user.userName, 'dragons', ['DivineDragon'])
        }

        Map source = createProject(1)
        List<Map> sourceSkills = createSkills(2, 1, 1, 100, 2)
        owner.createProjectAndSubjectAndSkills(source, createSubject(1, 1), sourceSkills)
        sourceSkills.each { Map skill -> owner.exportSkillToCatalog(source.projectId, skill.skillId) }
        Map destination = createProject(2) + [enableProtectedUserCommunity: true]
        owner.createProject(destination)
        owner.createSubject(createSubject(2, 1))
        owner.bulkImportSkillsFromCatalogAndFinalize(destination.projectId, 'TestSubject1', sourceSkills)
        member.initializeProjectProgress(destination.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Date firstReport = new Date(System.currentTimeMillis() - 2 * 86400000L)
        Date completionReport = new Date(System.currentTimeMillis() - 86400000L)

        when:
        sourceSkills.each { Map skill ->
            owner.addSkill(skill, member.userName, firstReport)
            owner.addSkill(skill, nonmember.userName, firstReport)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map memberSource = member.getSkillsSummaryForCurrentUser(source.projectId)
        Map nonmemberSource = nonmember.getSkillsSummaryForCurrentUser(source.projectId)
        Map memberDestination = member.getSkillsSummaryForCurrentUser(destination.projectId)

        then:
        memberSource.points == 200
        memberSource.skillsAchieved == 0
        nonmemberSource.points == 200
        nonmemberSource.skillsAchieved == 0
        memberDestination.points == 200
        memberDestination.skillsAchieved == 0
        !userPointsRepo.findByProjectIdAndUserId(destination.projectId, nonmember.userName)
        !userAchievedRepo.findAllByUserAndProjectIds(nonmember.userName, [destination.projectId])
        !participationRepo.existsByProjectIdAndUserId(destination.projectId, nonmember.userName)

        when:
        sourceSkills.each { Map skill ->
            owner.addSkill(skill, member.userName, completionReport)
            owner.addSkill(skill, nonmember.userName, completionReport)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map completedMemberSource = member.getSkillsSummaryForCurrentUser(source.projectId)
        Map completedNonmemberSource = nonmember.getSkillsSummaryForCurrentUser(source.projectId)
        Map completedMemberDestination = member.getSkillsSummaryForCurrentUser(destination.projectId)

        then:
        completedMemberSource.points == 400
        completedMemberSource.skillsAchieved == 2
        completedNonmemberSource.points == 400
        completedNonmemberSource.skillsAchieved == 2
        completedMemberDestination.points == 400
        completedMemberDestination.skillsAchieved == 2
        completedMemberDestination.subjects[0].points == 400
        completedMemberDestination.subjects[0].skillsAchieved == 2
        !userPointsRepo.findByProjectIdAndUserId(destination.projectId, nonmember.userName)
        !userAchievedRepo.findAllByUserAndProjectIds(nonmember.userName, [destination.projectId])
        !participationRepo.existsByProjectIdAndUserId(destination.projectId, nonmember.userName)
    }

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
