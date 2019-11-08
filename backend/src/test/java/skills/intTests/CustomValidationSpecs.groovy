package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory


class CustomValidationSpecs extends DefaultIntSpec {

    def "project name custom validation"(){

        when:
        skillsService.createProject([projectId: "Proj42", name: "Jabberwocky project"])

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "subject name custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def subj = SkillsFactory.createSubject()
        subj.name = "sneaky_Jabberwocky_name"
        skillsService.createSubject(subj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "subject paragraph custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def subj = SkillsFactory.createSubject()
        subj.name = "acceptable name"
        subj.description = """paragaraph

paragraph

jabberwocky

paragraph"""

        skillsService.createSubject(subj)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "skill name custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        def subj = SkillsFactory.createSubject()
        skillsService.createSubject(subj)

        when:
        def skill = SkillsFactory.createSkill()
        skill.name = "has a jabberwocky in it"
        skillsService.createSkill(skill)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "skill paragraph custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)
        def subj = SkillsFactory.createSubject()
        skillsService.createSubject(subj)

        when:
        def skill = SkillsFactory.createSkill()
        skill.name = "acceptable name"
        skill.description = """paragaraph

paragraph

jabberwocky

paragraph"""

        skillsService.createSkill(skill)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "badge name custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def badge = SkillsFactory.createBadge()
        badge.name = "has a jabberwocky in it"
        skillsService.createBadge(badge)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("names may not contain jabberwocky")
    }

    def "badge paragraph custom validation"(){
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        when:
        def badge = SkillsFactory.createBadge()
        badge.name = "acceptable name"
        badge.description = """paragaraph

paragraph

jabberwocky

paragraph"""

        skillsService.createBadge(badge)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "check against description validation endpoint"() {
        when:
        def res = skillsService.checkCustomDescriptionValidation("should not jabberwocky have")
        def resGood = skillsService.checkCustomDescriptionValidation("this one is fine")
        then:
        !res.body.valid
        res.body.msg == "paragraphs may not contain jabberwocky"

        resGood.body.valid
    }

    def "check against name validation endpoint"() {
        when:
        def res = skillsService.checkCustomNameValidation("should not jabberwocky have")
        def resGood = skillsService.checkCustomNameValidation("this one is fine")
        then:
        !res.body.valid
        res.body.msg == "names may not contain jabberwocky"

        resGood.body.valid
    }

    def "create badge with empty description"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        subj.description = ""
        def badge = SkillsFactory.createBadge()
        badge.description = ""

        skillsService.createProject(proj)
        when:
        skillsService.createSubject(subj)
        skillsService.createBadge(badge)

        then:
        true

    }
}
