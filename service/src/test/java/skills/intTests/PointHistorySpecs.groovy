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
        def res2 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(1))
        skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(2))
        def res5 = skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(3))
        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId)

        then:
        res2.body.completed.find { it.type == "Subject" }.level == 1
        res2.body.completed.find { it.type == "Overall" }.level == 1
        res5.body.completed.find { it.type == "Subject" }.level == 2
        res5.body.completed.find { it.type == "Overall" }.level == 2

        res1.pointsHistory.size() == 4
        res1.pointsHistory.get(0).points == 10
        parseDate(res1.pointsHistory.get(0).dayPerformed) ==  dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) ==  dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 40
        parseDate(res1.pointsHistory.get(2).dayPerformed) ==  dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 50
        parseDate(res1.pointsHistory.get(3).dayPerformed) ==  dates.get(3).clearTime()

        res1.achievements.size() == 2
        res1.achievements.find { it.name == "Level 1" }.points == res1.pointsHistory.get(1).points
        res1.achievements.find { it.name == "Level 1" }.achievedOn == res1.pointsHistory.get(1).dayPerformed

        res1.achievements.find { it.name == "Level 2" }.points == res1.pointsHistory.get(3).points
        res1.achievements.find { it.name == "Level 2" }.achievedOn == res1.pointsHistory.get(3).dayPerformed
    }


    def "empty dates should carry points from previous day"() {
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
        parseDate(res1.pointsHistory.get(0).dayPerformed) == dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) == dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 40
        parseDate(res1.pointsHistory.get(2).dayPerformed) == dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 50
        parseDate(res1.pointsHistory.get(3).dayPerformed) == dates.get(3).clearTime()

        res1.achievements.size() == 2
        res1.achievements.find { it.name == "Level 1" }.points == res1.pointsHistory.get(1).points
        res1.achievements.find { it.name == "Level 1" }.achievedOn == res1.pointsHistory.get(1).dayPerformed

        res1.achievements.find { it.name == "Level 2" }.points == res1.pointsHistory.get(3).points
        res1.achievements.find { it.name == "Level 2" }.achievedOn == res1.pointsHistory.get(3).dayPerformed
    }

    def "SUBJECTS: few days of history - multiple subjects"() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        List<Map> skills = SkillsFactory.createSkills(2)
        skills = skills.collect { it.numPerformToCompletion = 10; return it; }
        def subject = SkillsFactory.createSubject()
        def subject2 = SkillsFactory.createSubject(1, 2)
        List<Map> skills2 = SkillsFactory.createSkills(2, 1, 2, 100)
        skills2 = skills2.collect { it.numPerformToCompletion = 3; return it; }

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        List<Date> dates
        use(TimeCategory) {
            dates = [new Date(), 1.day.ago, 2.days.ago, 3.days.ago].sort()
        }
        when:
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(0))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId], userId, dates.get(1))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(1))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(2))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills.get(1).skillId], userId, dates.get(3))

        println "--------------------------------"

        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills2.get(0).skillId], userId, dates.get(1))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills2.get(0).skillId], userId, dates.get(2))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills2.get(1).skillId], userId, dates.get(0))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills2.get(1).skillId], userId, dates.get(1))
        println skillsService.addSkill([projectId: SkillsFactory.defaultProjId, skillId: skills2.get(1).skillId], userId, dates.get(2))

        def res1 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills.get(0).subjectId)
        def res2 = skillsService.getPointHistory(userId, SkillsFactory.defaultProjId, skills2.get(0).subjectId)

        then:
        res1.pointsHistory.size() == 4
        res1.pointsHistory.get(0).points == 10
        parseDate(res1.pointsHistory.get(0).dayPerformed) == dates.get(0).clearTime()

        res1.pointsHistory.get(1).points == 30
        parseDate(res1.pointsHistory.get(1).dayPerformed) == dates.get(1).clearTime()

        res1.pointsHistory.get(2).points == 40
        parseDate(res1.pointsHistory.get(2).dayPerformed) == dates.get(2).clearTime()

        res1.pointsHistory.get(3).points == 50
        parseDate(res1.pointsHistory.get(3).dayPerformed) == dates.get(3).clearTime()

        res1.achievements.size() == 2
        res1.achievements.find { it.name == "Level 1" }.points == res1.pointsHistory.get(1).points
        res1.achievements.find { it.name == "Level 1" }.achievedOn == res1.pointsHistory.get(1).dayPerformed

        res1.achievements.find { it.name == "Level 2" }.points == res1.pointsHistory.get(3).points
        res1.achievements.find { it.name == "Level 2" }.achievedOn == res1.pointsHistory.get(3).dayPerformed

        res2.pointsHistory.size() == 4
        parseDate(res2.pointsHistory.get(0).dayPerformed) == dates.get(0).clearTime()
        res2.pointsHistory.get(0).points == 100
        parseDate(res2.pointsHistory.get(1).dayPerformed) == dates.get(1).clearTime()
        res2.pointsHistory.get(1).points == 300
        parseDate(res2.pointsHistory.get(2).dayPerformed) == dates.get(2).clearTime()
        res2.pointsHistory.get(2).points == 500
        parseDate(res2.pointsHistory.get(3).dayPerformed) == dates.get(3).clearTime()
        res2.pointsHistory.get(3).points == 500

        res2.achievements.size() == 2

        parseDate(res2.achievements.find { it.name == "Level 1" }.achievedOn) == dates.get(1)
        res2.achievements.find { it.name == "Level 1" }.points == 300

        parseDate(res2.achievements.find { it.name == "Levels 2, 3, 4" }.achievedOn) == dates.get(2)
        res2.achievements.find { it.name == "Levels 2, 3, 4" }.points == 500
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
