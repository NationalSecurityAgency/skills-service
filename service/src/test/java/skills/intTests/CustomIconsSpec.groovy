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


import org.springframework.core.io.ClassPathResource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSubject

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
        def clientDisplayRes = skillsService.getCustomIconCssForProject(projId)
        then:
        result == [[filename:'dot2.png', cssClassname:"${projId}-dot2png"]]
        clientDisplayRes.toString().startsWith(".TestProject1-dot2png {\tbackground-image: url(")
    }

    def "get icon usages when not used"() {
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        skillsService.createProject([projectId: projId, name: "Test Icon Project"])
        def file = resource.getFile()
        def iconInfo = skillsService.uploadIcon([projectId:(projId)], file)

        def result = skillsService.getIconUsages(projId, iconInfo.cssClassName)

        then:
        result == []
    }

    def "get icon usages when used"() {
        ClassPathResource resource = new ClassPathResource("/dot2.png")

        when:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        def file = resource.getFile()
        def iconInfo = skillsService.uploadIcon([projectId:(projId)], file)

        def originalSubjectId = p1subj1.subjectId
        p1subj1.iconClass = iconInfo.cssClassName
        skillsService.updateSubject(p1subj1, originalSubjectId)

        def result = skillsService.getIconUsages(p1.projectId, iconInfo.cssClassName)

        then:
        result
        result[0] == p1subj1.name
    }
}
