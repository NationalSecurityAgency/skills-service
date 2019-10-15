package skills.intTests

import org.apache.commons.lang3.RandomStringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.Specification

class UserPointsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds = ['haNson', 'haRry', 'tom']
    List<String> subjects
    List<List<String>> allSkillIds
    String badgeId

    def setup(){
        skillsService.deleteProjectIfExist(projId)

        subjects = ['testSubject1', 'testSubject2', 'testSubject3']
        allSkillIds = setupProjectWithSkills(subjects)
        badgeId = 'badge1'

        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(0).get(0)], sampleUserIds.get(0), new Date())
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(0), new Date())
        skillsService.addSkill(['projectId': projId, skillId: allSkillIds.get(1).get(0)], sampleUserIds.get(1), new Date())

        skillsService.addBadge([projectId: projId, badgeId: badgeId, name: 'Badge 1'])
        skillsService.assignSkillToBadge([projectId: projId, badgeId: badgeId, skillId: allSkillIds.get(0).get(0)])
    }

    def 'get project users when project exists'() {
        when:
        def results = skillsService.getProjectUsers(projId)

        then:
        results
        results.count == 2
        results.totalCount == 2
        results.data.size() == 2
        results.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results.data.get(0).totalPoints == 20
        results.data.get(1).userId == sampleUserIds.get(1)?.toLowerCase()
        results.data.get(1).totalPoints == 10
    }

    def 'get project users with paging'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 1, 1)
        def results2 = skillsService.getProjectUsers(projId, 1, 2)

        then:
        results1
        results1.count == 2
        results1.totalCount == 2
        results1.data.size() == 1
        results1.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results1.data.get(0).totalPoints == 20
        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId == sampleUserIds.get(1)?.toLowerCase()
        results2.data.get(0).totalPoints == 10
    }

    def 'get project users with paging and query'() {
        when:
        def results1 = skillsService.getProjectUsers(projId, 1, 1, "userId", true, "h")
        def results2 = skillsService.getProjectUsers(projId, 1, 2, "userId", true, "h")

        then:
        results1
        results1.count == 2 // result count
        results1.totalCount == 2  // total user count
        results1.data.size() == 1
        results1.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results1.data.get(0).totalPoints == 20
        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId == sampleUserIds.get(1)?.toLowerCase()
        results2.data.get(0).totalPoints == 10
    }

    def 'get subject users when project exists'() {
        when:
        def results1 = skillsService.getSubjectUsers(projId, subjects.get(0))
        def results2 = skillsService.getSubjectUsers(projId, subjects.get(1))
        def results3 = skillsService.getSubjectUsers(projId, subjects.get(2))

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results1.data.get(0).totalPoints == 10

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2
        results2.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results2.data.get(0).totalPoints == 10
        results2.data.get(1).userId == sampleUserIds.get(1)?.toLowerCase()
        results2.data.get(1).totalPoints == 10

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get skill users when project exists'() {
        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0))
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(0))
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(1))

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results1.data.get(0).totalPoints == 10

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 2
        results2.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results2.data.get(0).totalPoints == 10
        results2.data.get(1).userId == sampleUserIds.get(1)?.toLowerCase()
        results2.data.get(1).totalPoints == 10

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get skill users with paging when project exists'() {
        when:
        def results1 = skillsService.getSkillUsers(projId, allSkillIds.get(0).get(0), 1, 1, "userId", true, "h")
        def results2 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(0), 1, 1, "userId", true, "h")
        def results3 = skillsService.getSkillUsers(projId, allSkillIds.get(1).get(1), 1, 1, "userId", true, "h")

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results1.data.get(0).totalPoints == 10

        results2
        results2.count == 2
        results2.totalCount == 2
        results2.data.size() == 1
        results2.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results2.data.get(0).totalPoints == 10

        results3
        results3.count == 0
        results3.totalCount == 0
        !results3.data
    }

    def 'get badge users when project exists'() {
        when:
        def results1 = skillsService.getBadgeUsers(projId, badgeId)

        then:
        results1
        results1.count == 1
        results1.totalCount == 1
        results1.data.size() == 1
        results1.data.get(0).userId == sampleUserIds.get(0)?.toLowerCase()
        results1.data.get(0).totalPoints == 10
    }



    private List<List<String>> setupProjectWithSkills(List<String> subjects = ['testSubject1', 'testSubject2']) {
        List<List<String>> skillIds = []
        skillsService.createProject([projectId: projId, name: "Test Project"])
        subjects.eachWithIndex { String subject, int index ->
            skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject $index".toString()])
            skillIds << addDependentSkills(subject, 3)
        }
        return skillIds
    }

    private List<String> addDependentSkills(String subject, int dependencyLevels = 1, int skillsAtEachLevel = 1) {
        List<String> parentSkillIds = []
        List<String> allSkillIds = []

        for (int i = 0; i < dependencyLevels; i++) {
            parentSkillIds = addSkillsForSubject(subject, skillsAtEachLevel, parentSkillIds)
            allSkillIds.addAll(parentSkillIds)
        }
        return allSkillIds
    }

    private List<String> addSkillsForSubject(String subject, int numSkills = 1, List<String> dependentSkillIds = Collections.emptyList()) {
        List<String> skillIds = []
        for (int i = 0; i < numSkills; i++) {
            String skillId = 'skill' + RandomStringUtils.randomAlphabetic(5)
            skillsService.createSkill(
                    [
                            projectId: projId,
                            subjectId: subject,
                            skillId: skillId,
                            name: 'Test Skill ' + RandomStringUtils.randomAlphabetic(8),
                            pointIncrement: 10,
                            numPerformToCompletion: 1,
                            pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                            dependenctSkillsIds: dependentSkillIds
                    ]
            )
            skillIds << skillId
        }
        return skillIds
    }
}
