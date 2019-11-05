package skills.intTests.utils

class SkillsFactory {

    static String DEFAULT_PROJ_NAME = "Test Project"
    static String DEFAULT_PROJ_ID_PREPEND = DEFAULT_PROJ_NAME.replaceAll(" ", "")

    static String getDefaultProjId(int projNum = 1) {
        DEFAULT_PROJ_ID_PREPEND + "${projNum}"
    }

    static String getDefaultProjName(int projNum = 1) {
        DEFAULT_PROJ_NAME + "#${projNum}"
    }

    static String getSubjectId(int subjNumber = 1) {
        return "TestSubject${subjNumber}".toString()
    }

    static String getSubjectName(int subjNumber = 1) {
        return "Test Subject #${subjNumber}".toString()
    }

    static Map createSkill(int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10) {
        return [projectId: getDefaultProjId(projNumber), subjectId: getSubjectId(subjNumber),
                skillId: "skill${skillNumber}${subjNumber > 1 ? "subj" + subjNumber : ""}".toString(),
                name     : "Test Skill ${skillNumber}${subjNumber > 1 ? " Subject" + subjNumber : "" }".toString(),
                type     : "Skill", pointIncrement: pointIncrement, numPerformToCompletion: numPerformToCompletion,
                pointIncrementInterval: pointIncrementInterval, numMaxOccurrencesIncrementInterval: 1,
                description: "This skill [skill${skillNumber}] belongs to project [${getDefaultProjId(projNumber)}]".toString(),
                helpUrl: "http://veryhelpfulwebsite-${skillNumber}".toString(),
                version  : version]
    }

    static List<Map> createSkills(int numSkills, int projNumber = 1, int subjNumer = 1) {
        return (1..numSkills).collect { createSkill(projNumber, subjNumer, it) }
    }

    static List<Map> createSkillsWithDifferentVersions(List<Integer> skillVersions, int projNumber = 1, int subjNumber = 1) {
        int num = 1
        return skillVersions.collect { createSkill(projNumber, subjNumber, num++, it)}
    }

    static createProject(int projNumber = 1) {
        return [projectId: getDefaultProjId(projNumber), name: getDefaultProjName(projNumber)]
    }

    static createSubject(int projNumber = 1, int subjNumber = 1) {
        return [projectId: getDefaultProjId(projNumber), subjectId: getSubjectId(subjNumber), name: getSubjectName(subjNumber)]
    }

    static createBadge(int projNumber = 1, int badgeNumber = 1) {
        return [projectId: getDefaultProjId(projNumber), badgeId: "badge${badgeNumber}".toString(), name: "Test Badge ${badgeNumber}".toString()]
    }

}
