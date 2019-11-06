package skills.intTests


import org.springframework.core.io.ClassPathResource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import spock.lang.Specification

class CustomIconsSpec extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    def setup() {
        skillsService.deleteProjectIfExist(projId)
    }

    def "upload icon"(){
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        skillsService.createProject([projectId: projId, name: "Test Icon Project"])
        def file = resource.getFile()
        def result = skillsService.uploadIcon([projectId:(projId)], file)

        then:
        result
        result.success
        result.cssClassName == "${projId}-dot2png"
        result.name == "dot2.png"
    }

    def "delete icon"(){
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        skillsService.createProject([projectId: projId, name: "Test Icon Project"])
        def file = resource.getFile()
        skillsService.uploadIcon([projectId:(projId)], file)
        skillsService.deleteIcon([projectId:(projId), filename: "dot2.png"])
        def result = skillsService.getIconCssForProject([projectId:(projId)])

        then:
        !result
    }

    def "get css for project"(){
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        skillsService.createProject([projectId: projId, name: "Test Icon Project"])
        def file = resource.getFile()
        skillsService.uploadIcon([projectId:(projId)], file)
        def result = skillsService.getIconCssForProject([projectId:(projId)])
        def clientDisplayRes = skillsService.getCustomClientDisplayCss(projId)
        then:
        result == [[filename:'dot2.png', cssClassname:"${projId}-dot2png"]]
        clientDisplayRes.toString().startsWith(".TestProject1-dot2png {\tbackground-image: url(")
    }

}
