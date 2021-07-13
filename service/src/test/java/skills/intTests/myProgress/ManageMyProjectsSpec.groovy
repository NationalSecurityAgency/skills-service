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
package skills.intTests.myProgress


import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreRest

@Slf4j
class ManageMyProjectsSpec extends DefaultIntSpec {
    SkillsService anotherUser

    def setup() {
        String anotherUserName = getRandomUsers(1)[0]
        log.info("This user will crate projects: [${anotherUserName}")
        anotherUser = createService(anotherUserName)
    }

    def "projects in a prod-mode are available"() {
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            anotherUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                anotherUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                anotherUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            anotherUser.createSkills(skills)
            return proj;
        }
        // 2nd project is NOT in the production mode
        anotherUser.enableProdMode(projs[0])
        anotherUser.enableProdMode(projs[2])

        when:
        def forMyProjects = skillsService.getAvailableMyProjects()

        then:
        forMyProjects.size() == 2

        forMyProjects[0].projectId == projs[0].projectId
        forMyProjects[0].name == projs[0].name
        forMyProjects[0].totalPoints == 10
        forMyProjects[0].numSubjects == 1
        forMyProjects[0].numSkills == 1
        forMyProjects[0].numBadges == 3
        !forMyProjects[0].isMyProject

        forMyProjects[1].projectId == projs[2].projectId
        forMyProjects[1].name == projs[2].name
        forMyProjects[1].totalPoints == 30
        forMyProjects[1].numSubjects == 3
        forMyProjects[1].numSkills == 3
        forMyProjects[1].numBadges == 1
        !forMyProjects[1].isMyProject
    }

    def "available projects are sorted by projectId"() {
        List projects = (1..15).collect {
            SkillsFactory.createProject(it)
        }
        List reverseProjects = projects.reverse()
        reverseProjects.each {
            anotherUser.createProject(it)
            anotherUser.enableProdMode(it)
        }

        when:
        def forMyProjects = skillsService.getAvailableMyProjects()

        println JsonOutput.prettyPrint(JsonOutput.toJson(forMyProjects))
        then:
        forMyProjects.collect { it.projectId } == projects.collect { it.projectId }.sort()
    }

    def "projects should not be available if prod-mode is disabled"() {
        List projects = (1..3).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            skillsService.addMyProject(proj.projectId)
            return proj
        }

        when:
        def forMyProjects = skillsService.getAvailableMyProjects()

        anotherUser.disableProdMode(projects[1])
        def forMyProjects1 = skillsService.getAvailableMyProjects()

        anotherUser.disableProdMode(projects[0])
        anotherUser.disableProdMode(projects[2])
        def forMyProjects2 = skillsService.getAvailableMyProjects()

        then:
        forMyProjects.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        forMyProjects1.collect { it.projectId }  == [projects[0].projectId, projects[2].projectId]
        !forMyProjects2
    }

    def "add and remove my project - single project"() {
        def proj1 = SkillsFactory.createProject()
        anotherUser.createProject(proj1)
        anotherUser.enableProdMode(proj1)

        when:
        def forMyProjects = skillsService.getAvailableMyProjects()

        skillsService.addMyProject(proj1.projectId)
        def forMyProjects1 = skillsService.getAvailableMyProjects()

        skillsService.removeMyProject(proj1.projectId)
        def forMyProjects2 = skillsService.getAvailableMyProjects()

        then:
        !forMyProjects[0].isMyProject
        forMyProjects1[0].isMyProject
        !forMyProjects2[0].isMyProject
    }

    def "add and remove my project - many projects"() {

        List projs = (1..6).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            return proj
        }

        when:
        def forMyProjects = skillsService.getAvailableMyProjects().sort { it.projectId }

        skillsService.addMyProject(projs[1].projectId)
        def forMyProjects1 = skillsService.getAvailableMyProjects()

        skillsService.addMyProject(projs[2].projectId)
        def forMyProjects2 = skillsService.getAvailableMyProjects()

        skillsService.addMyProject(projs[4].projectId)
        def forMyProjects3 = skillsService.getAvailableMyProjects()

        skillsService.removeMyProject(projs[1].projectId)
        def forMyProjects4 = skillsService.getAvailableMyProjects()

        skillsService.removeMyProject(projs[2].projectId)
        def forMyProjects5 = skillsService.getAvailableMyProjects()

        then:
        forMyProjects.collect { it.isMyProject }  == [false, false, false, false, false, false]
        forMyProjects1.collect { it.isMyProject }  == [false, true, false, false, false, false]
        forMyProjects2.collect { it.isMyProject }  == [false, true, true, false, false, false]
        forMyProjects3.collect { it.isMyProject }  == [false, true, true, false, true, false]
        forMyProjects4.collect { it.isMyProject }  == [false, false, true, false, true, false]
        forMyProjects5.collect { it.isMyProject }  == [false, false, false, false, true, false]
    }

    def "add and remove my project - all projects"() {
        List projs = (1..6).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            return proj
        }

        when:
        def forMyProjects = skillsService.getAvailableMyProjects().sort { it.projectId }

        projs.each {
            skillsService.addMyProject(it.projectId)
        }
        def forMyProjects1 = skillsService.getAvailableMyProjects()

        projs.each {
            skillsService.removeMyProject(it.projectId)
        }
        def forMyProjects2 = skillsService.getAvailableMyProjects()

        then:
        forMyProjects.collect { it.isMyProject }  == [false, false, false, false, false, false]
        forMyProjects1.collect { it.isMyProject }  == [true, true, true, true, true, true]
        forMyProjects2.collect { it.isMyProject }  == [false, false, false, false, false, false]
    }
}

