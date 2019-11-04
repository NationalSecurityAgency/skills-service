package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class SubjectsOrderSpecs extends DefaultIntSpec {

    def "move subject down"() {
        def proj = SkillsFactory.createProject()
        int numSubjects = 5

        skillsService.createProject(proj)
        List subjects = (1..numSubjects).collect {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
            return subject
        }

        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectDown(subjects.first())
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject1", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "should not be able to move down the last subject"() {
        def proj = SkillsFactory.createProject()
        int numSubjects = 5

        skillsService.createProject(proj)
        List subjects = (1..numSubjects).collect {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
            return subject
        }

        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectDown(subjects.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        skillsService.getSubjects(proj.projectId).collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "move subject up"() {
        def proj = SkillsFactory.createProject()
        int numSubjects = 5

        skillsService.createProject(proj)
        List subjects = (1..numSubjects).collect {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
            return subject
        }

        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectUp(subjects.get(1))
        def afterMove = skillsService.getSubjects(proj.projectId)
        then:
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        afterMove.collect({it.subjectId}) == ["TestSubject2", "TestSubject1", "TestSubject3", "TestSubject4", "TestSubject5"]
    }

    def "should not be able to move the first subject up"() {
        def proj = SkillsFactory.createProject()
        int numSubjects = 5

        skillsService.createProject(proj)
        List subjects = (1..numSubjects).collect {
            def subject = SkillsFactory.createSubject(1, it)
            skillsService.createSubject(subject)
            return subject
        }

        when:
        def beforeMove = skillsService.getSubjects(proj.projectId)
        skillsService.moveSubjectUp(subjects.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
        skillsService.getSubjects(proj.projectId).collect({it.subjectId}) == ["TestSubject1", "TestSubject2", "TestSubject3", "TestSubject4", "TestSubject5"]
    }
}
