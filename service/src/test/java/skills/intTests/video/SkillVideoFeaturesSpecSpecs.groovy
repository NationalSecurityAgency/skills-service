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
package skills.intTests.video

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillVideoFeaturesSpecSpecs extends DefaultIntSpec {

    def "copy project with skill that has video configured" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        when:
        def copyProj = SkillsFactory.createProject(50)
        skillsService.copyProject(p1.projectId, copyProj)
        def attributes = skillsService.getSkillVideoAttributes(copyProj.projectId, p1Skills[0].skillId)
        def skill = skillsService.getSkill([projectId: copyProj.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        then:
        attributes.videoUrl == "http://some.url"
        attributes.videoType == "video"
        attributes.captions == "captions"
        attributes.transcript == "transcript"
        skill.selfReportingType == SkillDef.SelfReportingType.Video.toString()
    }
}
