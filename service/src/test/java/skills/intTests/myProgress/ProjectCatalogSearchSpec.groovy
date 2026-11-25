/**
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
package skills.intTests.myProgress

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class ProjectCatalogSearchSpec extends DefaultIntSpec {

    SkillsService projectAdmin
    String otherUserName

    def setup() {
        List<String> users = getRandomUsers(2)
        projectAdmin = createService(users[0])
        otherUserName = users[1]
    }

    private Map createCatalogProject(int projNum, String name, String description = null, boolean prodMode = true) {
        def proj = SkillsFactory.createProject(projNum)
        proj.name = name
        if (description != null) {
            proj.description = description
        }
        projectAdmin.createProject(proj)
        if (prodMode) {
            projectAdmin.enableProdMode(proj)
        }
        return proj
    }

    private Map createSubject(int projNum, int subjNum, String name, String description = null) {
        def subj = SkillsFactory.createSubject(projNum, subjNum)
        subj.name = name
        if (description != null) {
            subj.description = description
        }
        projectAdmin.createSubject(subj)
        return subj
    }

    private Map createSkill(int projNum, int subjNum, int skillNum, String name, String description, boolean enabled = true) {
        def skill = SkillsFactory.createSkill(projNum, subjNum, skillNum)
        skill.name = name
        skill.description = description
        skill.enabled = enabled
        projectAdmin.createSkill(skill)
        return skill
    }

    def "matches project name, project description, subject name, skill name and skill description"() {
        def p1 = createCatalogProject(1, "Kubernetes Operations")
        createSubject(1, 1, "Basics")
        createSkill(1, 1, 1, "Pods", "Run a pod")

        def p2 = createCatalogProject(2, "Second", "Training on kubernetes clusters")
        createSubject(2, 1, "Basics")
        createSkill(2, 1, 1, "Pods", "Run a pod")

        def p3 = createCatalogProject(3, "Third")
        createSubject(3, 1, "Kubernetes Networking")
        createSkill(3, 1, 1, "Services", "Expose a service")

        def p4 = createCatalogProject(4, "Fourth")
        createSubject(4, 1, "Basics")
        createSkill(4, 1, 1, "Kubernetes Deployments", "Roll out an app")

        def p5 = createCatalogProject(5, "Fifth")
        createSubject(5, 1, "Basics")
        createSkill(5, 1, 1, "Deployments", "Roll out an app to kubernetes")

        def p6 = createCatalogProject(6, "Sixth", "Docker only")
        createSubject(6, 1, "Basics")
        createSkill(6, 1, 1, "Images", "Build an image")

        when:
        def res = skillsService.searchAvailableMyProjects("kubernetes")

        then:
        res.collect { it.projectId } == [p1.projectId, p2.projectId, p3.projectId, p4.projectId, p5.projectId]
        res.find { it.projectId == p1.projectId }.descriptionMatched == false
        res.find { it.projectId == p1.projectId }.matchedSkills == []
        res.find { it.projectId == p1.projectId }.numMatchedSkills == 0
        res.find { it.projectId == p2.projectId }.descriptionMatched == true
        res.find { it.projectId == p3.projectId }.matchedSkills == []
        res.find { it.projectId == p4.projectId }.matchedSkills == ["Kubernetes Deployments"]
        res.find { it.projectId == p4.projectId }.numMatchedSkills == 1
        res.find { it.projectId == p5.projectId }.matchedSkills == ["Deployments"]
        res.find { it.projectId == p5.projectId }.descriptionMatched == false
    }

    def "every word must appear in the project, in any order and in any field"() {
        def p1 = createCatalogProject(1, "Cloud Training", "Hands-on SECURITY labs")
        createSubject(1, 1, "Containers")
        createSkill(1, 1, 1, "Kubernetes Basics", "Run a pod")

        def p2 = createCatalogProject(2, "Cloud Training Two")
        createSubject(2, 1, "Containers")
        createSkill(2, 1, 1, "Kubernetes Basics", "Run a pod")

        def p3 = createCatalogProject(3, "Kubernetes security")
        createSubject(3, 1, "Basics")
        createSkill(3, 1, 1, "Pods", "Run a pod")

        when:
        def res = skillsService.searchAvailableMyProjects("  Security   KUBERNETES ")
        def phrase = skillsService.searchAvailableMyProjects("kubernetes security")
        def reversed = skillsService.searchAvailableMyProjects("security kubernetes")
        def none = skillsService.searchAvailableMyProjects("kubernetes security helm")

        then:
        res.collect { it.projectId } == [p1.projectId, p3.projectId]
        res.find { it.projectId == p1.projectId }.descriptionMatched == true
        res.find { it.projectId == p1.projectId }.matchedSkills == ["Kubernetes Basics"]
        phrase.collect { it.projectId } == [p1.projectId, p3.projectId]
        reversed.collect { it.projectId } == [p1.projectId, p3.projectId]
        none == []
    }

    def "matching skills are listed w/ the most matched words first, at most 5, w/ the total count"() {
        def p1 = createCatalogProject(1, "Secure Development")
        createSubject(1, 1, "Practices")
        createSkill(1, 1, 1, "Code Review", "Review code for secure coding issues")
        (2..7).each { createSkill(1, 1, it, "Coding Exercise ${it}", "Practice") }
        createSkill(1, 1, 8, "Threat Modeling", "Model threats")

        when:
        def res = skillsService.searchAvailableMyProjects("secure coding")

        then:
        res.size() == 1
        res[0].projectId == p1.projectId
        res[0].matchedSkills == ["Code Review", "Coding Exercise 2", "Coding Exercise 3", "Coding Exercise 4", "Coding Exercise 5"]
        res[0].numMatchedSkills == 7
    }

    def "disabled skills are not searched"() {
        def p1 = createCatalogProject(1, "First")
        createSubject(1, 1, "Basics")
        createSkill(1, 1, 1, "Enabled Skill", "Visible")
        createSkill(1, 1, 2, "Hidden Helm Skill", "Not visible yet", false)

        def p2 = createCatalogProject(2, "Second")
        createSubject(2, 1, "Basics")
        createSkill(2, 1, 1, "Helm Charts", "Visible")

        when:
        def res = skillsService.searchAvailableMyProjects("helm")

        then:
        res.collect { it.projectId } == [p2.projectId]
        res[0].matchedSkills == ["Helm Charts"]
    }

    def "only projects in the user's catalog are searched"() {
        def p1 = createCatalogProject(1, "Terraform in production")
        def p2 = createCatalogProject(2, "Terraform not in production", null, false)
        def p3 = createCatalogProject(3, "Terraform hidden", null, false)

        SkillsService otherUser = createService(otherUserName)
        otherUser.addMyHiddenProject(p3.projectId)

        when:
        def res = skillsService.searchAvailableMyProjects("terraform")
        def otherUserRes = otherUser.searchAvailableMyProjects("terraform")

        then:
        res.collect { it.projectId } == [p1.projectId]
        otherUserRes.collect { it.projectId } == [p1.projectId, p3.projectId]
    }

    def "% and _ are matched literally"() {
        def p1 = createCatalogProject(1, "First", "Reach 100% coverage")
        def p2 = createCatalogProject(2, "Second", "Reach 100 coverage")
        def p3 = createCatalogProject(3, "Third")
        createSubject(3, 1, "Basics")
        createSkill(3, 1, 1, "snake_case names", "Naming")
        def p4 = createCatalogProject(4, "Fourth")
        createSubject(4, 1, "Basics")
        createSkill(4, 1, 1, "snakeXcase names", "Naming")

        when:
        def percent = skillsService.searchAvailableMyProjects("100%")
        def underscore = skillsService.searchAvailableMyProjects("snake_case")

        then:
        percent.collect { it.projectId } == [p1.projectId]
        underscore.collect { it.projectId } == [p3.projectId]
    }

    def "ampersand in a name is matched as typed"() {
        def p1 = createCatalogProject(1, "Research and Development")
        createSubject(1, 1, "Basics")
        createSkill(1, 1, 1, "Q&A Sessions", "Ask questions")

        when:
        def res = skillsService.searchAvailableMyProjects("q&a")

        then:
        res.collect { it.projectId } == [p1.projectId]
        res[0].matchedSkills == ["Q&A Sessions"]
    }

    def "blank search returns no results"() {
        createCatalogProject(1, "First")

        expect:
        skillsService.searchAvailableMyProjects("") == []
        skillsService.searchAvailableMyProjects("   ") == []
    }

    def "search is limited in length and number of words"() {
        createCatalogProject(1, "First")

        when:
        skillsService.searchAvailableMyProjects("a" * 101)

        then:
        SkillsClientException e = thrown()
        e.resBody.contains("Search must not exceed 100 characters")

        when:
        skillsService.searchAvailableMyProjects((1..11).collect { "w${it}" }.join(" "))

        then:
        e = thrown()
        e.resBody.contains("Search must not have more than 10 words")

        when:
        def maxRes = skillsService.searchAvailableMyProjects((1..10).collect { "first" }.join(" "))

        then:
        maxRes.size() == 1
    }
}
