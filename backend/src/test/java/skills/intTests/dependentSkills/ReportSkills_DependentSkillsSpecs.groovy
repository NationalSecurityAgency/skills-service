package skills.intTests.dependentSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ReportSkills_DependentSkillsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup(){
        skillsService.deleteProjectIfExist(SkillsFactory.defaultProjId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "do not allow to create circular dependencies"(){
        // don't allow circular dependency of skill3 -> skill2 -> skill1 -> skill3
        List<Map> skills = SkillsFactory.createSkills(3)

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skills.get(0))
        skillsService.createSkill(skills.get(1))
        skillsService.createSkill(skills.get(2))

        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId, dependentSkillId: skills.get(1).skillId])

        when:
        def res = skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(2).skillId])

        then:
        !res.success
        res.body.explanation == "Discovered circular dependency [TestProject1:skill1 -> TestProject1:skill3 -> TestProject1:skill2 -> TestProject1:skill1]"
    }

    def "do not give credit if dependency was not fulfilled"(){

        List<Map> skills = SkillsFactory.createSkills(2)

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])

        def res = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])

        then:
        !res.body.skillApplied
        res.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."
    }

    def "do not give credit if only some dependencies were fulfilled"(){
        List<Map> skills = SkillsFactory.createSkills(4)
        skills.each{
            it.pointIncrement = 25
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(0).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(2).skillId])

        def resSkill1 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def resSkill3 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId])
        def resSkill4 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId])

        then:
        resSkill1.body.skillApplied
        resSkill3.body.skillApplied

        !resSkill4.body.skillApplied
        resSkill4.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 3. Waiting on completion of [TestProject1:skill2]."
    }

    def "give credit if dependency was fulfilled"(){
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])

        def res0 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def res = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])

        then:
        res0.body.skillApplied
        res0.body.explanation == "Skill event was applied"

        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "make sure that other users achievements don't affect requested users"(){
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.each{
            it.pointIncrement = 50
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])


        def res0 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], sampleUserIds.get(0), new Date())
        def res1 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId],sampleUserIds.get(1), new Date())
        def res2 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], sampleUserIds.get(0), new Date())

        then:
        res0.body.skillApplied
        res0.body.explanation == "Skill event was applied"

        !res1.body.skillApplied
        res1.body.explanation == "Not all dependent skills have been achieved. Missing achievements for 1 out of 1. Waiting on completion of [TestProject1:skill1]."

        res2.body.skillApplied
        res2.body.explanation == "Skill event was applied"
    }


    def "give credit if all dependencies were fulfilled"(){
        List<Map> skills = SkillsFactory.createSkills(4)
        skills.each{
            it.pointIncrement = 25
        }

        when:
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(0).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(2).skillId])

        def resSkill1 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId])
        def resSkill3 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId])
        def resSkill2 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId])
        def resSkill4 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId])

        then:
        resSkill1.body.skillApplied
        resSkill3.body.skillApplied
        resSkill2.body.skillApplied
        resSkill4.body.skillApplied
    }

    @Override
    void setMetaClass(MetaClass metaClass) {
        super.setMetaClass(metaClass)
    }

    def 'retrieve skills available for dependencies'() {
        setup:
        def project = skillsService.createProject(SkillsFactory.createProject(1)).body
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 1, 0))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 4, 1))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 2, 2))
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 3, 0))

        when:
        def v0Result = skillsService.getSkillsAvailableForDependency(SkillsFactory.getDefaultProjId(1))

        then:
        // verify all 4 skills are returned regardless of version #
        v0Result.size() == 4
    }
}
