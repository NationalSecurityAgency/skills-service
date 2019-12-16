package skills.intTests

import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import spock.lang.Specification

class LevelsSpec extends  DefaultIntSpec{

    String projId = "levelProj"
    String subject = "subject1"
    String skill1 = "skill1"
    String skill2 = "skill2"
    String skill3 = "skill3"

    String projectPointsSetting = "level.points.enabled"


    def setup(){
        skillsService.deleteProjectIfExist(projId)

        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        skillsService.createSkill(
                [
                        projectId: projId,
                        subjectId: subject,
                        skillId: skill1,
                        name: 'Test Skill 1',
                        pointIncrement: 50,
                        numPerformToCompletion: 1,
                        pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1
                ]
        )

        skillsService.createSkill(
                [
                        projectId: projId,
                        subjectId: subject,
                        skillId: skill2,
                        name: 'Test Skill 2',
                        pointIncrement: 50,
                        numPerformToCompletion: 1,
                        pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1
                ]
        )

        skillsService.createSkill(
                [
                        projectId: projId,
                        subjectId: subject,
                        skillId: skill3,
                        name: 'Test Skill 3',
                        pointIncrement: 50,
                        numPerformToCompletion: 1,
                        pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1
                ]
        )
    }

    def "change levels from percentage to points"(){
        when:
        def result = skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        def levelBeforeChange  = result.body.completed.findAll { it.type.toString().equalsIgnoreCase("Overall") }.collect { it.level }.max()

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])

        def userLevelAfterChange = skillsService.getSkillSummary("thing1", projId).skillsLevel

        def projectLevelsAfterChange = skillsService.getLevels(projId, null)
        projectLevelsAfterChange.sort {it.level}

        then:
        levelBeforeChange == userLevelAfterChange
        validateLevels(projectLevelsAfterChange)
    }

    def "change levels from percentage to points and back to percentage"(){
        when:
        def projectLevelsBeforeChange = skillsService.getLevels(projId)
        projectLevelsBeforeChange.sort { it.level}

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false"])


        def projectLevelsAfterChange = skillsService.getLevels(projId, null)
        projectLevelsAfterChange.sort {it.level}

        then:
        projectLevelsAfterChange[0].percent == projectLevelsBeforeChange[0].percent
        projectLevelsAfterChange[1].percent == projectLevelsBeforeChange[1].percent
        projectLevelsAfterChange[2].percent == projectLevelsBeforeChange[2].percent
        projectLevelsAfterChange[3].percent == projectLevelsBeforeChange[3].percent
        projectLevelsAfterChange[4].percent == projectLevelsBeforeChange[4].percent
        validateLevels(projectLevelsAfterChange)
    }

    def "change levels from points to percentage"(){
        when:
        def settingResult = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert settingResult.body.success == true
        settingResult = skillsService.getSetting(projId, projectPointsSetting)
        def props = [:]
        props.points = 15000
        props.name = "TwoEagles"
        props.iconClass = "fas fa-two-eagles"
        def result = skillsService.addLevel(projId, null, props)

        def levelsBeforeChange = skillsService.getLevels(projId, null).sort {it.level}
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false", id: settingResult.id])
        def levelsAfterChange = skillsService.getLevels(projId, null).sort {it.level}

        then:
        levelsBeforeChange.size() == levelsAfterChange.size()
        validateLevels(levelsAfterChange)
    }

    def validateLevels(def after){
        def previous = null
        after.each{
            if(previous != null){
                assert it.percent > previous.percent
                assert it.pointsFrom > previous.pointsFrom
            }

            previous = it
        }
        assert !previous.pointsTo
        return true
    }

    def "user retains achieved level if points range changes"(){
        when:

        def settingResult = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort {it.level}

        skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())

        def userLevelBeforeEdit = skillsService.getSkillSummary("thing1", projId).skillsLevel
        //should be level 3 or 4?


        def props = [:]
        props.projectId = levels.last().projectId
        props.skillId = levels.last().skillId
        props.level = levels.last().level
        props.percent = levels.last().percent
        props.pointsFrom = levels.last().pointsFrom+50
        props.pointsTo = levels.last().pointsTo
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels.get(levels.size()-2).projectId
        props.skillId = levels.get(levels.size()-2).skillId
        props.level = levels.get(levels.size()-2).level
        props.percent = levels.get(levels.size()-2).percent
        props.pointsFrom = levels.get(levels.size()-2).pointsFrom+50
        props.pointsTo = levels.get(levels.size()-2).pointsTo+50
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels.get(levels.size()-3).projectId
        props.skillId = levels.get(levels.size()-3).skillId
        props.level = levels.get(levels.size()-3).level
        props.percent = levels.get(levels.size()-3).percent
        props.pointsFrom = levels.get(levels.size()-3).pointsFrom+50
        props.pointsTo = levels.get(levels.size()-3).pointsTo+50
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        def skillSummary = skillsService.getSkillSummary("thing1", projId)
        def userLevelAfterEdit = skillSummary.skillsLevel
        def levelsAfterEdit = skillsService.getLevels(projId, null).sort {it.level}

        then:
        userLevelBeforeEdit == userLevelAfterEdit
    }

    def "switch to points based, edit level points, user should not achieve based on old percentage"(){
        when:

        def settingResult = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort {it.level}

        def props = [:]
        props.projectId = levels.last().projectId
        props.skillId = levels.last().skillId
        props.level = levels.last().level
        props.percent = levels.last().percent
        props.pointsFrom = levels.last().pointsFrom+50
        props.pointsTo = levels.last().pointsTo
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels.get(levels.size()-2).projectId
        props.skillId = levels.get(levels.size()-2).skillId
        props.level = levels.get(levels.size()-2).level
        props.percent = levels.get(levels.size()-2).percent
        props.pointsFrom = levels.get(levels.size()-2).pointsFrom+50
        props.pointsTo = levels.get(levels.size()-2).pointsTo+50
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels.get(levels.size()-3).projectId
        props.skillId = levels.get(levels.size()-3).skillId
        props.level = levels.get(levels.size()-3).level
        props.percent = levels.get(levels.size()-3).percent
        props.pointsFrom = levels.get(levels.size()-3).pointsFrom+50
        props.pointsTo = levels.get(levels.size()-3).pointsTo+50
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())

        def userLevel = skillsService.getSkillSummary("thing1", projId).skillsLevel

        then:
        userLevel == 2
    }

    def "edit existing level"(){
        when:
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def levelToEdit = levels[0]
        levelToEdit.name = "TwoEagles"
        levelToEdit.iconClass = "two-eagles"
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levelToEdit.pointsFrom
        props.pointsTo = levelToEdit.pointsTo
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"

        skillsService.editLevel(projId, null, props.level as String, props)

        def levelsPostEdit = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levelsPostEdit[0].level == 1
        levelsPostEdit[0].name == "TwoEagles"
        levelsPostEdit[0].iconClass == "two-eagles"
        levelsPostEdit[0].percent == levelToEdit.percent
        levelsPostEdit[0].pointsFrom == levelToEdit.pointsFrom
        levelsPostEdit[0].pointsTo == levelToEdit.pointsTo
    }


    def "edit existing subject level"(){
        when:
        def levels = skillsService.getLevels(projId, subject).sort(){it.level}
        def levelToEdit = levels[0]
        levelToEdit.name = "TwoEagles"
        levelToEdit.iconClass = "two-eagles"
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levelToEdit.pointsFrom
        props.pointsTo = levelToEdit.pointsTo
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"

        skillsService.editLevel(projId, subject, props.level as String, props)

        def levelsPostEdit = skillsService.getLevels(projId, subject).sort(){it.level}

        then:
        levelsPostEdit[0].level == 1
        levelsPostEdit[0].name == "TwoEagles"
        levelsPostEdit[0].iconClass == "two-eagles"
        levelsPostEdit[0].percent == levelToEdit.percent
        levelsPostEdit[0].pointsFrom == levelToEdit.pointsFrom
        levelsPostEdit[0].pointsTo == levelToEdit.pointsTo
    }

    def "edit existing level with invalid parameters for points"(){
        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def levelToEdit = levels[0]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levelToEdit.pointsFrom
        props.pointsTo = 100000
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, null, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }

    def "edit existing subject level with invalid parameters for points"(){
        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, subject).sort(){it.level}
        def levelToEdit = levels[0]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levelToEdit.pointsFrom
        props.pointsTo = 100000
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, subject, props.level as String, props)

        then:
        thrown(SkillsClientException)
    }


    def "edit existing level with points overlapping previous level"(){
        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def levelToEdit = levels[1]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levels[0].pointsTo-5
        props.pointsTo = levels[2].pointsFrom
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, null, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }

    def "edit existing subject level with points overlapping previous level"(){
        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, subject).sort(){it.level}
        def levelToEdit = levels[1]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levels[0].pointsTo-5
        props.pointsTo = levels[2].pointsFrom
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, subject, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }

    def "edit existing level with points overlapping next level"(){
        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def levelToEdit = levels[1]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levels[0].pointsTo
        props.pointsTo = levels[2].pointsFrom+5
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, null, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }


    def "edit existing subject level with points overlapping next level"(){
        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, subject).sort(){it.level}
        def levelToEdit = levels[1]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = levels[0].pointsTo
        props.pointsTo = levels[2].pointsFrom+5
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, subject, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }

    def "edit existing level with invalid parameters for percent"(){
        when:

        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def levelToEdit = levels[0]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = 1000
        props.pointsFrom = levelToEdit.pointsFrom
        props.pointsTo = levelToEdit.pointsTo
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, null, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }


    def "edit existing subject level with invalid parameters for percent"(){
        when:

        def levels = skillsService.getLevels(projId, subject).sort(){it.level}
        def levelToEdit = levels[0]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = 1000
        props.pointsFrom = levelToEdit.pointsFrom
        props.pointsTo = levelToEdit.pointsTo
        props.name = levelToEdit.name
        props.iconClass = levelToEdit.iconClass

        ResponseEntity response = skillsService.editLevel(projId, subject, props.level as String, props)
        assert response

        then:
        thrown(SkillsClientException)
    }


    def "add next level"(){
        when:
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def props = [:]
        props.percent = 98
        props.name = "TwoEagles"
        props.iconClass = "fas fa-two-eagles"

        skillsService.addLevel(projId, null, props)
        def afterAdd = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        afterAdd.size() > levels.size()
        afterAdd.last().percent == 98
        afterAdd.last().name == "TwoEagles"
        afterAdd.last().iconClass == "fas fa-two-eagles"
    }

    def "add next level after changing to points"() {
        when:
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def props = [:]
        props.points = 1000000
        props.name = "TwoEagles"
        props.iconClass = "fas fa-two-eagles"

        skillsService.addLevel(projId, null, props)
        def afterAdd = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        afterAdd.size() > levels.size()
        afterAdd.last().pointsFrom == 1000000
        afterAdd.last().name == "TwoEagles"
        afterAdd.last().iconClass == "fas fa-two-eagles"
    }

    def "change to points, add skills, change back to percentages, verify sane percentages"() {
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true
        setting = skillsService.getSetting(projId, projectPointsSetting)
        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        createSkill("skillz1", "oneone", 100)
        createSkill("skillz2", "twotwo", 100)
        createSkill("skillz3", "threethree", 100)
        createSkill("skillz4", "fourfour", 100)

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false", id: setting.id])

        def levelsAfterChangeBack = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levelsAfterChangeBack[0].percent >= levels[0].percent
        levelsAfterChangeBack[0].pointsFrom >= levels[0].pointsFrom
        levelsAfterChangeBack.last().percent >= levels.last().percent
        levelsAfterChangeBack.last().pointsFrom >= levels.last().pointsFrom
    }

    def "change to points, delete skills, change back to percentages, verify sane percentages"(){
        when:

        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true
        setting = skillsService.getSetting(projId, projectPointsSetting)
        def levelsBefore = skillsService.getLevels(projId, null).sort(){it.level}
        skillsService.deleteSkill([subjectId: subject, projectId: projId, skillId: skill1])
        skillsService.deleteSkill([subjectId: subject, projectId: projId, skillId: skill2])
        skillsService.deleteSkill([subjectId: subject, projectId: projId, skillId: skill3])
        createSkill(skill1, "skill1", 10)
        createSkill(skill2, "skill2", 10)
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false", id: setting.id])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levels.every { it.percent > 0 && it.percent < 100 }
    }

    def "delete level"(){
        when:
        def levelsBefore = skillsService.getLevels(projId, null).sort(){it.level}
        skillsService.deleteLevel(projId, null)
        def levelsAfter = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levelsAfter.size() < levelsBefore.size()
        !levelsAfter.find() { it.level == levelsBefore.last().level}
    }

    def "delete subject's level"(){
        when:
        def levelsBefore = skillsService.getLevels(projId, subject).sort(){it.level}
        skillsService.deleteLevel(projId, subject )
        def levelsAfter = skillsService.getLevels(projId, subject).sort(){it.level}

        then:
        levelsAfter.size() < levelsBefore.size()
        !levelsAfter.find() { it.level == levelsBefore.last().level}
    }


    def "delete level after changing to points"(){
        when:
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levelsBefore = skillsService.getLevels(projId, null).sort(){it.level}

        skillsService.deleteLevel(projId, null)
        def levelsAfter = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levelsAfter.size() < levelsBefore.size()
        !levelsAfter.find() { it.level == levelsBefore.last().level}
        !levelsAfter.last().pointsTo
    }

    def "try to delete a user-achieved level"() {
        when:
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 25)
        createSkill(skill2, 'Test SKill 2', 25)
        createSkill(skill3, 'Test SKill 3', 25)
        createSkill('skill4', 'Test SKill 4', 17)
        createSkill('skill5', 'Test SKill 6', 7)

        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def props = [:]
        props.percent = 98
        props.name = "TwoEagles"
        props.iconClass = "fas fa-two-eagles"

        skillsService.addLevel(projId, null, props)

        def result = skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        def r2 = skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())
        def r3 = skillsService.addSkill([projectId: projId, skillId: skill3], "thing1", new Date())
        def r4 = skillsService.addSkill([projectId: projId, skillId: "skill4"], "thing1", new Date())
        def r5 = skillsService.addSkill([projectId: projId, skillId: "skill5"], "thing1", new Date())

        def userLevelBeforeDelete = skillsService.getSkillSummary("thing1", projId).skillsLevel
        //shouldn't be allowed since a user achieved this level
        skillsService.deleteLevel(projId, null)

        then:
        thrown(SkillsClientException)
    }

    def "edit level ranges after user achieves level"() {
        when:
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 10)
        createSkill(skill2, 'Test SKill 2', 10)
        createSkill(skill3, 'Test SKill 3', 10)
        createSkill('skill4', 'Test SKill 4', 10)
        createSkill('skill5', 'Test SKill 5', 7)
        createSkill('skills6', 'Test SKill 6', 2)
        createSkill('skills7', 'Test SKill 7', 51)

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        //L3 = 45 - 67, L2 25 - 45, L4 = 67 - 92, L5 = 92+
        skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        def r2 = skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())
        def r3 = skillsService.addSkill([projectId: projId, skillId: skill3], "thing1", new Date())
        def r4 = skillsService.addSkill([projectId: projId, skillId: "skill4"], "thing1", new Date())
        def r5 = skillsService.addSkill([projectId: projId, skillId: "skill5"], "thing1", new Date())

        //user should be level 3 now
        def userBeforeEdit = skillsService.getSkillSummary("thing1", projId).skillsLevel

        levels = skillsService.getLevels(projId, null).sort(){it.level}
        def props = [:]
        props.projectId = levels[4].projectId
        props.skillId = levels[4].skillId
        props.level = levels[4].level
        props.percent = levels[4].percent
        props.pointsFrom = 98
        props.pointsTo = null
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels[3].projectId
        props.skillId = levels[3].skillId
        props.level = levels[3].level
        props.percent = levels[3].percent
        props.pointsFrom = 90
        props.pointsTo = 98
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels[2].projectId
        props.skillId = levels[2].skillId
        props.level = levels[2].level
        props.percent = levels[2].percent
        props.pointsFrom = 80
        props.pointsTo = 90
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        def userAfterEdit = skillsService.getSkillSummary("thing1", projId).skillsLevel

        skillsService.addSkill([projectId: projId, skillId: "skills6"], "thing1", new Date())
        def userAfterEditAfterSkill = skillsService.getSkillSummary("thing1", projId).skillsLevel

        then:
        userBeforeEdit == 3
        userAfterEdit  == 3
        userAfterEditAfterSkill == 3
    }

    def "edit level causing user to become eligible for a two level jump"() {
        when:
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 5)
        createSkill(skill2, 'Test SKill 2', 4)
        createSkill(skill3, 'Test SKill 3', 20)
        createSkill('skill4', 'Test SKill 4', 11)
        createSkill('skill5', 'Test SKill 5', 7)
        createSkill('skills6', 'Test SKill 6', 2)
        createSkill('skills7', 'Test SKill 7', 51)

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        //L1 10 - 25, L2 25 - 45, L3 = 45 - 67, L4 = 67 - 92, L5 = 92+
        skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())
        //user now has 9 points, not yet level 1

        def userSummary = skillsService.getSkillSummary("thing1", projId)
        def userBeforeEdit = userSummary.skillsLevel

        levels = skillsService.getLevels(projId, null).sort(){it.level}
        def props = [:]
        props.projectId = levels[4].projectId
        props.skillId = levels[4].skillId
        props.level = levels[4].level
        props.percent = levels[4].percent
        props.pointsFrom = 98
        props.pointsTo = null
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels[3].projectId
        props.skillId = levels[3].skillId
        props.level = levels[3].level
        props.percent = levels[3].percent
        props.pointsFrom = 90
        props.pointsTo = 98
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels[0].projectId
        props.skillId = levels[0].skillId
        props.level = levels[0].level
        props.percent = levels[0].percent
        props.pointsFrom = 5
        props.pointsTo = 10
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels[1].projectId
        props.skillId = levels[1].skillId
        props.level = levels[1].level
        props.percent = levels[1].percent
        props.pointsFrom = 10
        props.pointsTo = 20
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        props = [:]
        props.projectId = levels[2].projectId
        props.skillId = levels[2].skillId
        props.level = levels[2].level
        props.percent = levels[2].percent
        props.pointsFrom = 20
        props.pointsTo = 90
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"
        skillsService.editLevel(projId, null, props.level as String, props)

        userSummary = skillsService.getSkillSummary("thing1", projId)
        def userAfterEdit = userSummary.skillsLevel

        skillsService.addSkill([projectId: projId, skillId: "skill4"], "thing1", new Date())
        def userAfterEditAfterSkill = skillsService.getSkillSummary("thing1", projId).skillsLevel

        then:
        userBeforeEdit == 0
        //the user qualified for a new level after the level ranges were edited, this is reflected in the summary that is created
        userAfterEdit  == 1
        //after adding a new skill, the user jumps to level 3 as their points now qualify them for the new level
        userAfterEditAfterSkill == 3
    }

    def "delete skills causing change in total points and level calculation"() {
        //["White Belt":10, "Blue Belt":25, "Purple Belt":45, "Brown Belt":67, "Black Belt":92]
        when:
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 10)
        createSkill(skill2, 'Test SKill 2', 10)
        createSkill(skill3, 'Test SKill 3', 10)
        createSkill('skill4', 'Test SKill 4', 10)
        createSkill('skill5', 'Test SKill 5', 5)
        createSkill('skills6', 'Test SKill 6', 25)
        createSkill('skills7', 'Test SKill 7', 30)

        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())
        //user now has 20 points, should be level 1

        def userSummary = skillsService.getSkillSummary("thing1", projId)

        def userBeforeDelete = userSummary.skillsLevel

        skillsService.deleteSkill([projectId: projId, subjectId: subject, skillId:'skills7'])
        skillsService.deleteSkill([projectId: projId, subjectId: subject, skillId:'skills6'])
        skillsService.deleteSkill([projectId: projId, subjectId: subject, skillId:'skill5'])
        skillsService.deleteSkill([projectId: projId, subjectId: subject, skillId:'skill4'])
        skillsService.deleteSkill([projectId: projId, subjectId: subject, skillId:'skill3'])

        userSummary = skillsService.getSkillSummary("thing1", projId)
        def userAfterDelete = userSummary.skillsLevel

        createSkill('skills3', 'Test SKill 3', 80)
        //we need to trigger achievement
        skillsService.addSkill([projectId: projId, skillId: 'skills3'], "thing1", new Date())
        userSummary = skillsService.getSkillSummary("thing1", projId)
        def userAfterAchievement = userSummary.skillsLevel

        then:
        userBeforeDelete == 1
        userAfterDelete  == 5 //
        userAfterAchievement == 5
        userSummary.subjects[0].skillsLevel == 5
    }

    def "delete level after users has achieved it"() {
        when:
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 10)
        createSkill(skill2, 'Test SKill 2', 10)
        createSkill(skill3, 'Test SKill 3', 10)
        createSkill('skill4', 'Test SKill 4', 10)
        createSkill('skill5', 'Test SKill 5', 7)
        createSkill('skills6', 'Test SKill 6', 2)
        createSkill('skills7', 'Test SKill 7', 51)

        skillsService.addSkill([projectId: projId, skillId: skill1], "thing1", new Date())
        def r2 = skillsService.addSkill([projectId: projId, skillId: skill2], "thing1", new Date())
        def r3 = skillsService.addSkill([projectId: projId, skillId: skill3], "thing1", new Date())
        def r4 = skillsService.addSkill([projectId: projId, skillId: "skill4"], "thing1", new Date())
        def r5 = skillsService.addSkill([projectId: projId, skillId: "skill5"], "thing1", new Date())

        //user should be level 3 now
        def userLevelBeforeDelete = skillsService.getSkillSummary("thing1", projId).skillsLevel

        skillsService.deleteLevel(projId, null)
        skillsService.deleteLevel(projId, null)
        skillsService.deleteLevel(projId, null)

        then:
        thrown(SkillsClientException)
    }

    def "try to delete all levels"(){
        when:
        def levelsBefore = skillsService.getLevels(projId, null).sort(){it.level}

        skillsService.deleteLevel(projId, null)
        skillsService.deleteLevel(projId, null)
        skillsService.deleteLevel(projId, null)
        skillsService.deleteLevel(projId, null)
        def result = skillsService.deleteLevel(projId, null)
        log.info(result.toString())

        then:
        thrown(SkillsClientException)
    }

    def "add too many levels"(){
        when:
        (1..21).each{
            def props = [:]
            props.percent = 92+it
            props.name = "TwoEagles_$it"
            props.iconClass = "fas fa-two-eagles"

            skillsService.addLevel(projId, null, props)
        }
        then:
        thrown(SkillsClientException)
    }

    def "change to points, delete skill, change back to percentage. levels must be <= 100%"(){
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true
        setting = skillsService.getSetting(projId, projectPointsSetting)
        //project has 3 skills for a total of 150 points, level 5 should be ~138 pointsFrom
        def levelsBeforeDelete = skillsService.getLevels(projId, subject)

        skillsService.deleteSkill([projectId: projId, subjectId: subject, skillId:skill3])
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false", id: setting.id])

        def levelsAfterDeleteAndChange = skillsService.getLevels(projId, subject)

        then:
        levelsBeforeDelete.last().pointsFrom == 138
        levelsAfterDeleteAndChange.last().percent < 100

    }

    def "change to points, add empty subject, change back to percentages"(){
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true

        skillsService.createSubject([projectId: projId, subjectId: "subj99", name: "Test Subject 99"])


        setting = skillsService.getSetting(projId, projectPointsSetting)
        setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false", id: setting.id])
        assert setting.body.success == true

        def levels = skillsService.getLevels(projId, "subj99")

        then:
        levels.last().percent < 100
        levels.first().percent > 0
    }

    def "change to points, add empty subject, change back to percentages, change back to points"(){
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true

        skillsService.createSubject([projectId: projId, subjectId: "subj99", name: "Test Subject 99"])


        def settingData = skillsService.getSetting(projId, projectPointsSetting)
        setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "false", id: settingData.id])
        assert setting.body.success == true

        setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true", id: settingData.id])
        assert setting.body.success == true

        def levels = skillsService.getLevels(projId, "subj99")

        then:
        levels.last().pointsFrom > 0
        !levels.last().pointsTo
        levels.first().pointsFrom > 0
        levels.first().pointsTo > 0
    }

    def "automatically prevent gaps when editing level points"(){
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true

        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        def levelToEdit = levels[1]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = 40 //was 37
        props.pointsTo = 60 //was 67
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"

        skillsService.editLevel(projId, null, props.level as String, props)
        levels = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levels[0].pointsTo == 40
        levels[1].pointsFrom == 40
        levels[1].pointsTo == 60
        levels[2].pointsFrom == 60
    }

    def "automatically prevent gaps when editing first level points"(){
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true

        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        def levelToEdit = levels[0]
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = 5
        props.pointsTo = 10
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"

        skillsService.editLevel(projId, null, props.level as String, props)
        levels = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levels[0].pointsTo == 10
        levels[1].pointsFrom == 10
    }

    def "automatically prevent gaps when editing last level points"(){
        when:
        def setting = skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        assert setting.body.success == true

        def levels = skillsService.getLevels(projId, null).sort(){it.level}

        def levelToEdit = levels.last()
        def props = [:]
        props.projectId = levelToEdit.projectId
        props.skillId = levelToEdit.skillId
        props.level = levelToEdit.level
        props.percent = levelToEdit.percent
        props.pointsFrom = 1500
        props.pointsTo = 2500
        props.name = "TwoEagles"
        props.iconClass = "two-eagles"

        skillsService.editLevel(projId, null, props.level as String, props)
        levels = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levels[levels.size()-1].pointsFrom == 1500
        levels[levels.size()-2].pointsTo == 1500
    }

    def "can switch to points based once project has 100 points"() {
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 100)

        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def setting = skillsService.getSetting(projId, projectPointsSetting)

        then:
        setting
        setting.value == "true"
    }

    def "cannot switch to points based until project has 100 points"() {
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 99)

        when:

        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])

        then:
        thrown(SkillsClientException)
    }

    def "single level with from points greater then project total points"(){
        skillsService.deleteProjectIfExist(projId)
        skillsService.createProject([projectId: projId, name: "Level Project"])
        skillsService.createSubject([projectId: projId, subjectId: subject, name: "Test Subject 1"])
        createSkill(skill1, 'Test SKill 1', 200)

        skillsService.deleteLevel(projId)
        skillsService.deleteLevel(projId)
        skillsService.deleteLevel(projId)
        skillsService.deleteLevel(projId)

        when:
        skillsService.changeSetting(projId, projectPointsSetting, [projectId: projId, setting: projectPointsSetting, value: "true"])
        def levels = skillsService.getLevels(projId, null).sort(){it.level}
        def levelToEdit = levels[0]
        levelToEdit.pointsFrom = 999999

        skillsService.editLevel(projId, null, levelToEdit.level as String, levelToEdit)

        levels = skillsService.getLevels(projId, null).sort(){it.level}

        then:
        levels
        levels.size() == 1
        levels[0].pointsFrom == 999999
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

    private void createSkill(String skillId, String skillName, int pointIncrement){
        skillsService.createSkill([
                projectId: projId,
                subjectId: subject,
                skillId: skillId,
                name: skillName,
                pointIncrement: pointIncrement,
                numPerformToCompletion: 1,
                pointIncrementInterval: 60, numMaxOccurrencesIncrementInterval: 1
        ])
    }
}
