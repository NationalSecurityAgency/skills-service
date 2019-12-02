package skills.intTests

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class PointHistorySpecs extends DefaultIntSpec {
    String userId = "user1"

    def "user has no points"(){
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        then:
        !res.pointsHistory
    }

    def "history only appears after 2 days of usage"() {
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1,50)
        skills = skills.collect { it.numPerformToCompletion = 2; return it; }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        Date today = new Date()
        Date yesterday
        use (TimeCategory) {
            yesterday = 1.day.ago
        }
        when:
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, today)
        def res = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, yesterday)
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        then:
        !res.pointsHistory

        res1.pointsHistory.size() == 2
        res1.pointsHistory.get(0).points == 50
        parseDate(res1.pointsHistory.get(0).dayPerformed) == yesterday.clearTime()

        res1.pointsHistory.get(1).points == 100
        parseDate(res1.pointsHistory.get(1).dayPerformed) == today.clearTime()
    }


    def "add up points from different skills"() {
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1, 50)
        skills = skills.collect { it.numPerformToCompletion = 2; return it; }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        Date today = new Date()
        Date yesterday
        use (TimeCategory) {
            yesterday = 1.day.ago
        }
        when:
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, today)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, yesterday)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, yesterday)
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        then:
        res1.pointsHistory.size() == 2
        res1.pointsHistory.get(0).points == 100 // 2 skills added together
        parseDate(res1.pointsHistory.get(0).dayPerformed) == yesterday.clearTime()

        res1.pointsHistory.get(1).points == 150
        parseDate(res1.pointsHistory.get(1).dayPerformed) == today.clearTime()
    }

    def "few days of history"() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkills(2)
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
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        then:
        res1.pointsHistory.size() == 4
        res1.pointsHistory.get(0).points == 10
        parseDate(res1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 40
        parseDate(res1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 50
        parseDate(res1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
    }

    def "empty dates should carry points from prevous day"() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkills(2)
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
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        then:
        res1.pointsHistory.size() == 4
        res1.pointsHistory.get(0).points == 10
        parseDate(res1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 30
        parseDate(res1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 30
        parseDate(res1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
    }

    def "SUBJECTS: user has no points"(){
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)

        then:
        !res.pointsHistory
    }

    def "SUBJECTS: history only appears after 2 days of usage"() {
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1, 50)
        skills = skills.collect { it.numPerformToCompletion = 2; return it; }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        Date today = new Date()
        Date yesterday
        use (TimeCategory) {
            yesterday = 1.day.ago
        }
        when:
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, today)
        def res = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)

        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, yesterday)
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)

        then:
        !res.pointsHistory

        res1.pointsHistory.size() == 2
        res1.pointsHistory.get(0).points == 50
        parseDate(res1.pointsHistory.get(0).dayPerformed) == yesterday.clearTime()

        res1.pointsHistory.get(1).points == 100
        parseDate(res1.pointsHistory.get(1).dayPerformed) == today.clearTime()
    }



    def "SUBJECTS: add up points from different skills"() {
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1, 50)
        skills = skills.collect { it.numPerformToCompletion = 2; return it; }
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        Date today = new Date()
        Date yesterday
        use (TimeCategory) {
            yesterday = 1.day.ago
        }
        when:
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, today)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, yesterday)
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, yesterday)
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)

        then:
        res1.pointsHistory.size() == 2
        res1.pointsHistory.get(0).points == 100 // 2 skills added together
        parseDate(res1.pointsHistory.get(0).dayPerformed) == yesterday.clearTime()

        res1.pointsHistory.get(1).points == 150
        parseDate(res1.pointsHistory.get(1).dayPerformed) == today.clearTime()
    }

    def "SUBJECTS: few days of history"() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkills(2)
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
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)

        then:
        res1.pointsHistory.size() == 4
        res1.pointsHistory.get(0).points == 10
        parseDate(res1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 40
        parseDate(res1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 50
        parseDate(res1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
    }

    def "SUBJECTS: empty dates should carry points from prevous day"() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkills(2)
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
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)

        then:
        res1.pointsHistory.size() == 4
        res1.pointsHistory.get(0).points == 10
        parseDate(res1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 30
        parseDate(res1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 30
        parseDate(res1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()
    }

    private Date parseDate(String str) {
        Date.parse("yyyy-MM-dd'T'HH:mm:ss", str)
    }
}
