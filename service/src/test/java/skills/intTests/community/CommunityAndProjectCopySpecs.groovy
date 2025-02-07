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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.*

class CommunityAndProjectCopySpecs extends DefaultIntSpec {

    def "copying project should copy the community"() {
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
        def newProj = SkillsFactory.createProject(50)
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        def copiedProject = pristineDragonsUser.getProject(newProj.projectId)
        then:
        copiedProject.projectId == newProj.projectId
        copiedProject.userCommunity == 'Divine Dragon'
    }

    def "cannot disable community when copying a project"() {
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
        def newProj = SkillsFactory.createProject(50)
        p1.enableProtectedUserCommunity = false
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        def copiedProject = pristineDragonsUser.getProject(newProj.projectId)
        then:
        copiedProject.projectId == newProj.projectId
        copiedProject.userCommunity == 'Divine Dragon'
    }

    def "enable protected community during copy"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def newProj = SkillsFactory.createProject(50)
        newProj.enableProtectedUserCommunity = true
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        def copiedProject = pristineDragonsUser.getProject(newProj.projectId)
        def originalProject = pristineDragonsUser.getProject(p1.projectId)
        then:
        copiedProject.projectId == newProj.projectId
        copiedProject.userCommunity == 'Divine Dragon'

        originalProject.userCommunity == 'All Dragons'
    }

    def "enable protected community during copy and description has jabberwocky that's not allowed in non-uc projects"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.description = "divinedragon is not allowed in uc projects"
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def newProj = SkillsFactory.createProject(50)
        newProj.enableProtectedUserCommunity = true
        newProj.description = "This is a jabberwocky description that is not allowed in non-uc projects"
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        def copiedProject = pristineDragonsUser.getProject(newProj.projectId)
        def originalProject = pristineDragonsUser.getProject(p1.projectId)
        def copiedProjectDesc = pristineDragonsUser.getProjectDescription(newProj.projectId)
        def origProjectDesc = pristineDragonsUser.getProjectDescription(p1.projectId)
        then:
        copiedProject.projectId == newProj.projectId
        copiedProject.userCommunity == 'Divine Dragon'
        copiedProjectDesc.description == "This is a jabberwocky description that is not allowed in non-uc projects"

        originalProject.userCommunity == 'All Dragons'
        origProjectDesc.description == "divinedragon is not allowed in uc projects"
    }

    def "enable protected community during copy and description has divinedragon that's not allowed in uc projects"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.description = "divinedragon is not allowed in uc projects"
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def newProj = SkillsFactory.createProject(50)
        newProj.enableProtectedUserCommunity = true
        newProj.description = "divinedragon is not allowed in uc projects"
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "keeping community disabled during copy and description has divinedragon that's not allowed in uc projects"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.description = "divinedragon is not allowed in uc projects"
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def newProj = SkillsFactory.createProject(50)
        newProj.description = "divinedragon is not allowed in uc projects"
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        def copiedProject = pristineDragonsUser.getProject(newProj.projectId)
        def originalProject = pristineDragonsUser.getProject(p1.projectId)
        def copiedProjectDesc = pristineDragonsUser.getProjectDescription(newProj.projectId)
        def origProjectDesc = pristineDragonsUser.getProjectDescription(p1.projectId)
        then:
        copiedProject.projectId == newProj.projectId
        copiedProject.userCommunity == 'All Dragons'
        copiedProjectDesc.description == "divinedragon is not allowed in uc projects"

        originalProject.userCommunity == 'All Dragons'
        origProjectDesc.description == "divinedragon is not allowed in uc projects"
    }

    def "copy non-protected community project"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        when:
        def newProj = SkillsFactory.createProject(50)
        pristineDragonsUser.copyProject(p1.projectId, newProj)
        def copiedProject = pristineDragonsUser.getProject(newProj.projectId)
        def originalProject = pristineDragonsUser.getProject(p1.projectId)
        then:
        copiedProject.projectId == newProj.projectId
        copiedProject.userCommunity == 'All Dragons'

        originalProject.userCommunity == 'All Dragons'
    }

}
