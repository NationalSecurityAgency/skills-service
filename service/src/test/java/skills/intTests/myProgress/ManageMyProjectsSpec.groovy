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


import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreRest

@Slf4j
class ManageMyProjectsSpec extends DefaultIntSpec {
    SkillsService anotherUser
    List<String> randomUsers
    def setup() {
        randomUsers = getRandomUsers(10)
        String anotherUserName = randomUsers.remove(0)
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

        then:
        forMyProjects.collect { it.projectId } == projects.collect { it.projectId }.sort()
    }

    def "projects should be available if prod-mode is disabled but still saved to my projects"() {
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
        forMyProjects1.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        forMyProjects2.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
    }

    def "projects should not be available if prod-mode is disabled and not still saved to my projects"() {
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
        skillsService.removeMyProject(projects[1].projectId)
        def forMyProjects1 = skillsService.getAvailableMyProjects()

        anotherUser.disableProdMode(projects[0])
        anotherUser.disableProdMode(projects[2])
        skillsService.removeMyProject(projects[0].projectId)
        skillsService.removeMyProject(projects[2].projectId)
        def forMyProjects2 = skillsService.getAvailableMyProjects()

        then:
        forMyProjects.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        forMyProjects1.collect { it.projectId }  == [projects[0].projectId, projects[2].projectId]
        !forMyProjects2
    }

    def "hidden projects should not be available to other users"() {
        SkillsService user1 = createService(randomUsers[0])
        SkillsService user2 = createService(randomUsers[1])
        SkillsService user3 = createService(randomUsers[2])

        List projects = (1..3).collect {
            def proj = SkillsFactory.createProject(it)
            skillsService.createProject(proj)
            skillsService.disableProdMode(proj)
            user1.addMyHiddenProject(proj.projectId)
            return proj
        }

        when:
        def user1Projects = user1.getAvailableMyProjects()
        def user2Projects = user2.getAvailableMyProjects()
        def user3Projects = user3.getAvailableMyProjects()

        skillsService.enableProdMode(projects[0])
        user3.addMyHiddenProject(projects[1].projectId)

        def user1Projects2 = user1.getAvailableMyProjects()
        def user2Projects2 = user2.getAvailableMyProjects()
        def user3Projects2 = user3.getAvailableMyProjects()

        then:
        user1Projects.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        user2Projects.collect { it.projectId } == []
        user3Projects.collect { it.projectId } == []
        user1Projects2.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        user2Projects2.collect { it.projectId } == [projects[0].projectId]
        user3Projects2.collect { it.projectId }  == [projects[0].projectId, projects[1].projectId]
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


    def "Summaries sort order - newly added project always appears first"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)

            return proj
        }

        when:
        skillsService.addMyProject(projects[2].projectId)
        def summary = skillsService.getMyProgressSummary()

        skillsService.addMyProject(projects[5].projectId)
        def summary1 = skillsService.getMyProgressSummary()

        skillsService.addMyProject(projects[1].projectId)
        def summary2 = skillsService.getMyProgressSummary()

        skillsService.addMyProject(projects[6].projectId)
        def summary3 = skillsService.getMyProgressSummary()

        then:
        summary.projectSummaries.collect { it.projectId } == [projects[2].projectId]
        summary1.projectSummaries.collect { it.projectId } == [projects[5].projectId, projects[2].projectId]
        summary2.projectSummaries.collect { it.projectId } == [projects[1].projectId, projects[5].projectId, projects[2].projectId]
        summary3.projectSummaries.collect { it.projectId } == [projects[6].projectId, projects[1].projectId, projects[5].projectId, projects[2].projectId]
    }

    def "Summaries sort order - removing my project doesn't break the order"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            skillsService.addMyProject(proj.projectId)
            return proj
        }

        when:
        def summary = skillsService.getMyProgressSummary()
        skillsService.removeMyProject(projects[2].projectId)
        skillsService.removeMyProject(projects[6].projectId)

        def summary1 = skillsService.getMyProgressSummary()

        skillsService.addMyProject(projects[2].projectId)

        def summary2 = skillsService.getMyProgressSummary()
        then:
        summary.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        summary1.projectSummaries.collect {
            it.projectId
        } == [ projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[1].projectId, projects[0].projectId]

        summary2.projectSummaries.collect {
            it.projectId
        } == [ projects[2].projectId, projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[1].projectId, projects[0].projectId]
    }

    def "Summaries sort order - change sort order of an existing project"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            skillsService.addMyProject(proj.projectId)
            return proj
        }

        when:
        def summary = skillsService.getMyProgressSummary()

        skillsService.moveMyProject(projects[6].projectId, 1)
        def summary1 = skillsService.getMyProgressSummary()

        skillsService.moveMyProject(projects[0].projectId, 2)
        def summary2 = skillsService.getMyProgressSummary()

        skillsService.moveMyProject(projects[5].projectId, 6)
        def summary3 = skillsService.getMyProgressSummary()

        then:
        summary.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        summary1.projectSummaries.collect {
            it.projectId
        } == [projects[5].projectId, projects[6].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        summary2.projectSummaries.collect {
            it.projectId
        } == [projects[5].projectId, projects[6].projectId, projects[0].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId]

        summary3.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[0].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[5].projectId]
    }

    def "Summaries sort order - change sort order outsid of existing range"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            skillsService.addMyProject(proj.projectId)
            return proj
        }

        when:
        def summary = skillsService.getMyProgressSummary()

        skillsService.moveMyProject(projects[6].projectId, 20)
        def summary1 = skillsService.getMyProgressSummary()

        then:
        summary.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        summary1.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

    }

    def "Summaries sort order - do not fail if project was removed from My Projects (ex. other browser's tab)"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            skillsService.addMyProject(proj.projectId)
            return proj
        }

        when:
        def summary = skillsService.getMyProgressSummary()
        skillsService.removeMyProject(projects[3].projectId)
        def summary1 = skillsService.getMyProgressSummary()

        skillsService.moveMyProject(projects[3].projectId, 1)
        def summary2 = skillsService.getMyProgressSummary()

        then:
        summary.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[5].projectId, projects[4].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        summary1.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[5].projectId, projects[4].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        summary2.projectSummaries.collect {
            it.projectId
        } == [projects[6].projectId, projects[3].projectId, projects[5].projectId, projects[4].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

    }

    def "Summaries sort order - error is emitted if project is moved to index below 0"() {
        List projects = (1..3).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            skillsService.addMyProject(proj.projectId)
            return proj
        }

        when:
        assert skillsService.getMyProgressSummary().projectSummaries.collect {
            it.projectId
        } == [projects[2].projectId, projects[1].projectId, projects[0].projectId]

        skillsService.moveMyProject(projects[0].projectId, 0)
        assert skillsService.getMyProgressSummary().projectSummaries.collect {
            it.projectId
        } == [projects[0].projectId, projects[2].projectId, projects[1].projectId]

        skillsService.moveMyProject(projects[2].projectId, -1)


        then:
        SkillsClientException e = thrown()
        e.getMessage().contains("Provided [newSortIndex=-1] is less than 0")
    }

    def "Summaries sort order - multiple users - move projects"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            return proj
        }

        SkillsService user1 = createService(randomUsers[0])
        SkillsService user2 = createService(randomUsers[1])
        SkillsService user3 = createService(randomUsers[2])

        projects.each {
            user1.addMyProject(it.projectId)
        }
        projects.reverse().each {
            user2.addMyProject(it.projectId)
        }

        user3.addMyProject(projects[2].projectId)
        user3.addMyProject(projects[4].projectId)
        user3.addMyProject(projects[0].projectId)

        when:
        def user1Sum1 = user1.getMyProgressSummary()
        def user2Sum1 = user2.getMyProgressSummary()
        def user3Sum1 = user3.getMyProgressSummary()

        user1.moveMyProject(projects[4].projectId, 0)
        def user1Sum2 = user1.getMyProgressSummary()

        user2.moveMyProject(projects[0].projectId, 6)
        def user2Sum2 = user2.getMyProgressSummary()

        user3.moveMyProject(projects[4].projectId, 2)
        def user3Sum2 = user3.getMyProgressSummary()

        then:
        user1Sum1.projectSummaries.collect { it.projectId } == projects.reverse().collect { it.projectId }
        user2Sum1.projectSummaries.collect { it.projectId } == projects.collect { it.projectId }
        user3Sum1.projectSummaries.collect { it.projectId } == [projects[0].projectId, projects[4].projectId, projects[2].projectId]

        user1Sum2.projectSummaries.collect {
            it.projectId
        } == [ projects[4].projectId, projects[6].projectId, projects[5].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        user2Sum2.projectSummaries.collect {
            it.projectId
        } == [ projects[1].projectId, projects[2].projectId, projects[3].projectId, projects[4].projectId, projects[5].projectId, projects[6].projectId, projects[0].projectId,]

        user3Sum2.projectSummaries.collect { it.projectId } == [projects[0].projectId, projects[2].projectId, projects[4].projectId]
    }

    def "Summaries sort order - multiple users - remove from My Projects"() {
        List projects = (1..7).collect {
            def proj = SkillsFactory.createProject(it)
            anotherUser.createProject(proj)
            anotherUser.enableProdMode(proj)
            return proj
        }

        SkillsService user1 = createService(randomUsers[0])
        SkillsService user2 = createService(randomUsers[1])
        SkillsService user3 = createService(randomUsers[2])

        projects.each {
            user1.addMyProject(it.projectId)
        }
        projects.reverse().each {
            user2.addMyProject(it.projectId)
        }

        user3.addMyProject(projects[2].projectId)
        user3.addMyProject(projects[4].projectId)
        user3.addMyProject(projects[0].projectId)

        when:
        def user1Sum1 = user1.getMyProgressSummary()
        def user2Sum1 = user2.getMyProgressSummary()
        def user3Sum1 = user3.getMyProgressSummary()

        user1.removeMyProject(projects[4].projectId)
        def user1Sum2 = user1.getMyProgressSummary()

        user2.removeMyProject(projects[0].projectId)
        def user2Sum2 = user2.getMyProgressSummary()

        user3.removeMyProject(projects[4].projectId)
        def user3Sum2 = user3.getMyProgressSummary()

        then:
        user1Sum1.projectSummaries.collect { it.projectId } == projects.reverse().collect { it.projectId }
        user2Sum1.projectSummaries.collect { it.projectId } == projects.collect { it.projectId }
        user3Sum1.projectSummaries.collect { it.projectId } == [projects[0].projectId, projects[4].projectId, projects[2].projectId]

        user1Sum2.projectSummaries.collect {
            it.projectId
        } == [ projects[6].projectId, projects[5].projectId, projects[3].projectId, projects[2].projectId, projects[1].projectId, projects[0].projectId]

        user2Sum2.projectSummaries.collect {
            it.projectId
        } == [ projects[1].projectId, projects[2].projectId, projects[3].projectId, projects[4].projectId, projects[5].projectId, projects[6].projectId]

        user3Sum2.projectSummaries.collect { it.projectId } == [projects[0].projectId, projects[2].projectId]
    }

    def "available projects with empty description return hasDescription of false"() {
        def proj1 = SkillsFactory.createProject(1)
        proj1.description = '';
        def proj2 = SkillsFactory.createProject(2)
        proj2.description = "foooo"

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.enableProdMode(proj1)
        skillsService.enableProdMode(proj2)

        when:
        def available = skillsService.getAvailableMyProjects()

        then:
        available[0].projectId == "TestProject1"
        !available[0].hasDescription
        available[1].projectId == "TestProject2"
        available[1].hasDescription
    }

}

