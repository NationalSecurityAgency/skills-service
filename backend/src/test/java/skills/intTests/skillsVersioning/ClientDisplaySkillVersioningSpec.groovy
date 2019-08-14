package skills.intTests.skillsVersioning

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientDisplaySkillVersioningSpec extends DefaultIntSpec {

    def "create skills with different versions; only the correct skills are returned when filtered by version 1"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId, dependentSkillId: skills.get(0).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())

        when:
        def skillSummary0 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 0)
        def skillSummary1 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 1)
        def skillSummary2 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 2)

        then:
        skillSummary0.skills.size() == 2
        skillSummary0.totalPoints == 20
        skillSummary0.skills.collect({ it.skillId }).sort() == ["skill1", "skill2"]

        skillSummary1.skills.size() == 5
        skillSummary1.totalPoints == 50
        skillSummary1.skills.collect({ it.skillId }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5"]

        def skill1 = skillSummary1.skills.find { it.skillId == "skill1" }
        skill1
        !skill1.dependencyInfo

        def skill2 = skillSummary1.skills.find { it.skillId == "skill2" }
        skill2.dependencyInfo.numDirectDependents == 1
        skill2.dependencyInfo.achieved

        skillSummary2.skills.size() == 6
        skillSummary2.totalPoints == 60
        skillSummary2.skills.collect({
            it.skillId
        }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"]
    }

    def "user points DO NOT respect the version - if user ends those points they are proudly displayed in all versions"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId], userId, new Date() - 3)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date() - 2)

        when:
        def skillSummary0 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 0)
        def skillSummary1 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 1)
        def skillSummary2 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 2)

        then:
        skillSummary0.skills.size() == 2
        skillSummary0.totalPoints == 20
        skillSummary0.skills.collect({ it.skillId }).sort() == ["skill1", "skill2"]
        skillSummary0.points == 60
        skillSummary0.todaysPoints == 30

        skillSummary1.skills.size() == 5
        skillSummary1.totalPoints == 50
        skillSummary1.skills.collect({ it.skillId }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        skillSummary1.points == 60
        skillSummary1.todaysPoints == 30

        skillSummary2.skills.size() == 6
        skillSummary2.totalPoints == 60
        skillSummary2.skills.collect({
            it.skillId
        }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"]
        skillSummary2.points == 60
        skillSummary2.todaysPoints == 30
    }

    def "user points to NOT respect the version (skills with numPerformToCompletion > 1) - if user ends those points they are proudly displayed in all versions"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills.each {
            it.numPerformToCompletion = 5
        }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 3)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 4)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date())

        when:
        def skillSummary0 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 0)
        def skillSummary1 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 1)
        def skillSummary2 = skillsService.getSkillSummary(userId, SkillsFactory.defaultProjId, subject.subjectId, 2)

        then:
        skillSummary0.skills.size() == 2
        skillSummary0.totalPoints == 100
        skillSummary0.skills.collect({ it.skillId }).sort() == ["skill1", "skill2"]
        skillSummary0.points == 80
        skillSummary0.todaysPoints == 20

        skillSummary1.skills.size() == 5
        skillSummary1.totalPoints == 250
        skillSummary1.skills.collect({ it.skillId }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        skillSummary1.points == 80
        skillSummary1.todaysPoints == 20

        skillSummary2.skills.size() == 6
        skillSummary2.totalPoints == 300
        skillSummary2.skills.collect({
            it.skillId
        }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"]
        skillSummary2.points == 80
        skillSummary2.todaysPoints == 20
    }


    def "badge summary must respect the version - skills with numPerformToCompletion > 1"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills.each {
            it.numPerformToCompletion = 5
        }
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        def badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)
        skills.each { skillItem ->
            skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: skillItem.skillId])
        }

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 3)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, new Date() - 4)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date() - 2)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date() - 1)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, new Date())

        when:
        def skillSummary0 = skillsService.getBadgeSummary(userId, SkillsFactory.defaultProjId, badge.badgeId, 0)
        def skillSummary1 = skillsService.getBadgeSummary(userId, SkillsFactory.defaultProjId, badge.badgeId, 1)
        def skillSummary2 = skillsService.getBadgeSummary(userId, SkillsFactory.defaultProjId, badge.badgeId, 2)

        then:
        skillSummary0.skills.size() == 2
        skillSummary0.skills.collect({ it.skillId }).sort() == ["skill1", "skill2"]

        skillSummary1.skills.size() == 5
        skillSummary1.skills.collect({ it.skillId }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5"]

        skillSummary2.skills.size() == 6
        skillSummary2.skills.collect({
            it.skillId
        }).sort() == ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"]
    }

    def "point history should respect skill versions"() {
        String userId = "user1"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills = skills.collect { it.numPerformToCompletion = 10; return it; }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        List<Date> dates
        use(TimeCategory) {
            dates = [new Date(), 1.day.ago, 2.days.ago, 3.days.ago].sort()
        }
        when:
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(0))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(3))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(0))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(3))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, dates.get(2))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, dates.get(3))

        def resVersion0 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, null, 0)
        def resVersion1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, null, 1)
        def resVersion2 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, null, 2)

        then:
        resVersion0.pointsHistory.size() == 4
        resVersion0.pointsHistory.get(0).points == 10
        parseDate(resVersion0.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        resVersion0.pointsHistory.get(1).points == 30
        parseDate(resVersion0.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        resVersion0.pointsHistory.get(2).points == 40
        parseDate(resVersion0.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        resVersion0.pointsHistory.get(3).points == 50
        parseDate(resVersion0.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
        // -------------------------------------------
        resVersion1.pointsHistory.size() == 4
        resVersion1.pointsHistory.get(0).points == 20
        parseDate(resVersion1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        resVersion1.pointsHistory.get(1).points == 60
        parseDate(resVersion1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        resVersion1.pointsHistory.get(2).points == 90
        parseDate(resVersion1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        resVersion1.pointsHistory.get(3).points == 110
        parseDate(resVersion1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
        // -------------------------------------------
        resVersion2.pointsHistory.size() == 4
        resVersion2.pointsHistory.get(0).points == 20
        parseDate(resVersion2.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        resVersion2.pointsHistory.get(1).points == 60
        parseDate(resVersion2.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        resVersion2.pointsHistory.get(2).points == 100
        parseDate(resVersion2.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        resVersion2.pointsHistory.get(3).points == 130
        parseDate(resVersion2.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
    }

    def "point history should respect skill versions - subjects"() {
        String userId = "user1"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 1, 1, 2])
        skills = skills.collect { it.numPerformToCompletion = 10; return it; }

        List<Map> skills_subj1 = SkillsFactory.createSkills(5, 1, 2)
        skills_subj1.eachWithIndex { it, index ->
            it.numPerformToCompletion = 10;
            it.version = index;
        }

        def subject = SkillsFactory.createSubject()
        def subject1 = SkillsFactory.createSubject(1, 2)

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.createSubject(subject1)
        skillsService.createSkills(skills_subj1)

        List<Date> dates
        use(TimeCategory) {
            dates = [new Date(), 1.day.ago, 2.days.ago, 3.days.ago].sort()
        }
        when:
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(0))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(3))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(0))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(2).skillId], userId, dates.get(3))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(4).skillId], userId, dates.get(2))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId], userId, dates.get(3))

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills_subj1.get(4).skillId], userId, dates.get(2))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills_subj1.get(4).skillId], userId, dates.get(3))

        def resVersion0 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject.subjectId, 0)
        def resVersion1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject.subjectId, 1)
        def resVersion2 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject.subjectId, 2)

        def resVersion0_subj1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject1.subjectId, 0)
        def resVersion1_subj1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject1.subjectId, 1)
        def resVersion2_subj1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject1.subjectId, 2)
        def resVersion3_subj1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject1.subjectId, 3)
        def resVersion4_subj1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject1.subjectId, 4)
        def resVersion5_subj1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, subject1.subjectId, 5)

        then:
        resVersion0.pointsHistory.size() == 4
        resVersion0.pointsHistory.get(0).points == 10
        parseDate(resVersion0.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        resVersion0.pointsHistory.get(1).points == 30
        parseDate(resVersion0.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        resVersion0.pointsHistory.get(2).points == 40
        parseDate(resVersion0.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        resVersion0.pointsHistory.get(3).points == 50
        parseDate(resVersion0.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
        // -------------------------------------------
        resVersion1.pointsHistory.size() == 4
        resVersion1.pointsHistory.get(0).points == 20
        parseDate(resVersion1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        resVersion1.pointsHistory.get(1).points == 60
        parseDate(resVersion1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        resVersion1.pointsHistory.get(2).points == 90
        parseDate(resVersion1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        resVersion1.pointsHistory.get(3).points == 110
        parseDate(resVersion1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
        // -------------------------------------------
        resVersion2.pointsHistory.size() == 4
        resVersion2.pointsHistory.get(0).points == 20
        parseDate(resVersion2.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        resVersion2.pointsHistory.get(1).points == 60
        parseDate(resVersion2.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        resVersion2.pointsHistory.get(2).points == 100
        parseDate(resVersion2.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        resVersion2.pointsHistory.get(3).points == 130
        parseDate(resVersion2.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()

        // ---------------------------------------
        // ---------------------------------------
        !resVersion0_subj1.pointsHistory
        !resVersion1_subj1.pointsHistory
        !resVersion2_subj1.pointsHistory
        !resVersion3_subj1.pointsHistory
        resVersion4_subj1.pointsHistory.size() == 2
        resVersion5_subj1.pointsHistory.size() == 2
    }

    private Date parseDate(String str) {
        Date.parse("yyyy-MM-dd'T'HH:mm:ss", str)
    }
}
