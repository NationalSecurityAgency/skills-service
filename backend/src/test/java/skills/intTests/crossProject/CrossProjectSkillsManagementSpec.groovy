package skills.intTests.crossProject

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class CrossProjectSkillsManagementSpec extends DefaultIntSpec {

    SkillsService skillsServiceAdmin2

    def setup() {
        skillsServiceAdmin2 = createService("userNumberTwo")
        skillsServiceAdmin2.deleteAllMyProjects()
    }

    def cleanup() {
        skillsServiceAdmin2.deleteAllMyProjects()
    }

    def "share skill with another project"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == "Test Project#2"
        proj1SharedSkills.get(0).projectId == "TestProject2"
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"
    }

    def "share skill with ALL other projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)

        def proj3 = SkillsFactory.createProject(3)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)

        skillsService.createProject(proj3)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def proj3SharedSkills = skillsService.getSharedSkills(proj3.projectId)
        def proj3SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj3.projectId)

        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == null
        proj1SharedSkills.get(0).projectId == null
        proj1SharedSkills.get(0).sharedWithAllProjects
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"

        !proj3SharedSkills
        proj3SharedWithMeSkills.size() == 1
        proj3SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj3SharedWithMeSkills.get(0).skillId == "skill1"
        proj3SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj3SharedWithMeSkills.get(0).projectId == "TestProject1"
    }

    def "share skill with ALL other projects and then depend on that skill from another project"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        def proj3 = SkillsFactory.createProject(3)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.createProject(proj3)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        def proj3SharedSkills = skillsService.getSharedSkills(proj3.projectId)
        def proj3SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj3.projectId)

        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)


        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == null
        proj1SharedSkills.get(0).projectId == null
        proj1SharedSkills.get(0).sharedWithAllProjects
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"

        !proj3SharedSkills
        proj3SharedWithMeSkills.size() == 1
        proj3SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj3SharedWithMeSkills.get(0).skillId == "skill1"
        proj3SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj3SharedWithMeSkills.get(0).projectId == "TestProject1"

        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"
    }

    def "share skill with another project managed by a different admin"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsServiceAdmin2.createProject(proj2)
        skillsServiceAdmin2.createSubject(proj2_subj)
        skillsServiceAdmin2.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsServiceAdmin2.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsServiceAdmin2.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 1
        proj1SharedSkills.get(0).skillName == "Test Skill 1"
        proj1SharedSkills.get(0).skillId == "skill1"
        proj1SharedSkills.get(0).projectName == "Test Project#2"
        proj1SharedSkills.get(0).projectId == "TestProject2"
        !proj1SharedWithMeSkills

        !proj2SharedSkills
        proj2SharedWithMeSkills.size() == 1
        proj2SharedWithMeSkills.get(0).skillName == "Test Skill 1"
        proj2SharedWithMeSkills.get(0).skillId == "skill1"
        proj2SharedWithMeSkills.get(0).projectName == "Test Project#1"
        proj2SharedWithMeSkills.get(0).projectId == "TestProject1"
    }

    def "only admins of a project should see shared skills"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def proj1SharedSkills = skillsServiceAdmin2.getSharedSkills(proj1.projectId)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("code=403 FORBIDDEN")
    }

    def "remove shared skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(2).skillId, proj2.projectId)

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        skillsService.deleteShared(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 3
        proj2SharedWithMeSkills.size() == 3
        !proj1SharedWithMeSkills
        !proj2SharedSkills

        proj1SharedSkills_afterDelete.size() == 2
        proj1SharedSkills_afterDelete.find { it.skillId == "skill1" }.projectId == "TestProject2"
        proj1SharedSkills_afterDelete.find { it.skillId == "skill3" }.projectId == "TestProject2"

        proj2SharedWithMeSkills_afterDelete.size() == 2
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill1" }.projectId == "TestProject1"
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill3" }.projectId == "TestProject1"
    }

    def "remove shared skill with ALL projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(2).skillId, 'ALL_SKILLS_PROJECTS')

        def proj1SharedSkills = skillsService.getSharedSkills(proj1.projectId)
        def proj1SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj1.projectId)

        def proj2SharedSkills = skillsService.getSharedSkills(proj2.projectId)
        def proj2SharedWithMeSkills = skillsService.getSharedWithMeSkills(proj2.projectId)

        skillsService.deleteShared(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')

        def proj1SharedSkills_afterDelete = skillsService.getSharedSkills(proj1.projectId)
        def proj2SharedWithMeSkills_afterDelete = skillsService.getSharedWithMeSkills(proj2.projectId)

        then:
        proj1SharedSkills.size() == 3
        proj2SharedWithMeSkills.size() == 3
        !proj1SharedWithMeSkills
        !proj2SharedSkills

        proj1SharedSkills_afterDelete.size() == 2
        proj1SharedSkills_afterDelete.find { it.skillId == "skill1" }.projectId == null
        proj1SharedSkills_afterDelete.find { it.skillId == "skill3" }.projectId == null

        proj2SharedWithMeSkills_afterDelete.size() == 2
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill1" }.projectId == "TestProject1"
        proj2SharedWithMeSkills_afterDelete.find { it.skillId == "skill3" }.projectId == "TestProject1"
    }

    def "delete cross-project dependency skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        then:
        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill2"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"

        !sharedSkillInfoAfterDelete.dependencies
    }
    def "delete cross-project dependency skill shared with ALL projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, 'ALL_SKILLS_PROJECTS')
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        then:
        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill2"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"

        !sharedSkillInfoAfterDelete.dependencies
    }

    def "delete cross-project dependency transient skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(4, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        // project 1 internal dependencies
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId, dependentSkillId: proj1_skills.get(1).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId, dependentSkillId: proj1_skills.get(3).skillId])

        // cross project dependency
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])


        def crossProjectSharedInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        def proj1Skill1SharedInfo = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def proj2Skill1SharedInfo = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(1).skillId)
        def proj3Skill1SharedInfo = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(2).skillId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: proj1_skills.get(2).skillId,])

        def crossProjectSharedInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)
        def proj1Skill1SharedInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def proj2Skill1SharedInfoAfterDelete = skillsService.getSkillDependencyInfo("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        crossProjectSharedInfo.dependencies.size() == 1
        crossProjectSharedInfo.dependencies.get(0).dependsOn.skillId == "skill1"
        crossProjectSharedInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"

        proj1Skill1SharedInfo.dependencies.size() == 3
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill1" }.dependsOn.skillId == "skill2"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill1" }.dependsOn.projectId == "TestProject1"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.skillId == "skill3"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.projectId == "TestProject1"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.skillId == "skill4"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.projectId == "TestProject1"

        proj2Skill1SharedInfo.dependencies.size() == 2
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.skillId == "skill3"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill2" }.dependsOn.projectId == "TestProject1"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.skillId == "skill4"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.projectId == "TestProject1"

        proj3Skill1SharedInfo.dependencies.size() == 1
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.skillId == "skill4"
        proj1Skill1SharedInfo.dependencies.find { it.skill.skillId == "skill3" }.dependsOn.projectId == "TestProject1"

        crossProjectSharedInfoAfterDelete.dependencies.size() == 1
        crossProjectSharedInfoAfterDelete.dependencies.get(0).dependsOn.skillId == "skill1"
        crossProjectSharedInfoAfterDelete.dependencies.get(0).dependsOn.projectId == "TestProject1"

        proj1Skill1SharedInfoAfterDelete.dependencies.size() == 1
        proj1Skill1SharedInfoAfterDelete.dependencies.get(0).dependsOn.skillId == "skill2"
        proj1Skill1SharedInfoAfterDelete.dependencies.get(0).dependsOn.projectId == "TestProject1"

        !proj2Skill1SharedInfoAfterDelete.dependencies.size()
    }

    def "delete skill when it has cross-project dependency"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(1).skillId,])

        def sharedSkillInfo = skillsService.getSkillDependencyInfo("user1", proj2.projectId, proj2_skills.get(0).skillId)

        skillsService.deleteSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: proj2_skills.get(0).skillId,])

        then:
        sharedSkillInfo.dependencies.size() == 1
        sharedSkillInfo.dependencies.get(0).dependsOn.skillId == "skill2"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectId == "TestProject1"
        sharedSkillInfo.dependencies.get(0).dependsOn.projectName == "Test Project#1"
    }


    def "dependent skill from another project was achieved by another user - that shouldn't affect me"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)

        when:
        skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])
        def res1 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        def res2 = skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], "phil", new Date())
        def res3 = skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId])
        then:
        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"

        !res3.body.skillApplied
        res3.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."
    }

    def "attempt to depend on skill that was not shared"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        when:
        def res = skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                                  dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])

        then:
        !res.success
        res.body.explanation == "Skill [TestProject1:skill1] is not shared (or does not exist) to [TestProject2] project"
    }

    def "attempt to assign dependency that already exist"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj2.projectId)

        when:
        def res = skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                        dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])
        def res1 = skillsService.assignDependency([projectId         : proj2.projectId, skillId: proj2_skills.get(0).skillId,
                                                  dependentProjectId: proj1.projectId, dependentSkillId: proj1_skills.get(0).skillId,])

        then:
        res.success
        !res1.success
        res1.body.explanation == "Skill dependency [TestProject2:skill1subj2]=>[TestProject1:skill1] already exist."
        res1.body.errorCode == "FailedToAssignDependency"
    }


    def "cannot create project with reserved project id"() {

        def proj1 = SkillsFactory.createProject(1)
        proj1.projectId = 'ALL_SKILLS_PROJECTS'

        when:

        skillsService.createProject(proj1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.httpStatus == HttpStatus.BAD_REQUEST
    }

}
