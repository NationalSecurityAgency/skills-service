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

import skills.intTests.utils.DefaultIntSpec

import static skills.intTests.utils.SkillsFactory.*

class HelpUrlSpecs extends DefaultIntSpec {

    String url1Before = "http://thisShouldBeFine.com/this one has spaces/ok/yes to spaces"
    String url1After = "http://thisShouldBeFine.com/this%20one%20has%20spaces/ok/yes%20to%20spaces"
    String url2Before = "/some other with spaces/ok/yes to spaces"
    String url2After = "/some%20other%20with%20spaces/ok/yes%20to%20spaces"

    def "subject - spaces in help url are encoded"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        p1subj1.helpUrl = url1Before
        def p1subj2 = createSubject(1, 2)
        p1subj2.helpUrl = url2Before

        skillsService.createProject(p1)
        when:
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        def subj1 = skillsService.getSubject(p1subj1)
        def subj2 = skillsService.getSubject(p1subj2)
        then:
        subj1.helpUrl == url1After
        subj2.helpUrl == url2After
    }


    def "badges - spaces in help url are encoded"() {
        def p1 = createProject(1)
        def p1Badge1 = createBadge(1, 1)
        p1Badge1.helpUrl = url1Before
        def p1Badge2 = createBadge(1, 2)
        p1Badge2.helpUrl = url2Before

        skillsService.createProject(p1)
        when:
        skillsService.createBadge(p1Badge1)
        skillsService.createBadge(p1Badge2)

        def badge1 = skillsService.getBadge(p1.projectId, p1Badge1.badgeId)
        def badge2 = skillsService.getBadge(p1.projectId, p1Badge2.badgeId)
        then:
        badge1.helpUrl == url1After
        badge2.helpUrl == url2After
    }

    def "skills - spaces in help url are encoded"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        def skill2 = createSkill(1, 1, 23, 0, 12, 512, 18,)
        skill1.helpUrl = url1Before
        skill2.helpUrl = url2Before

        skillsService.createProject(p1)
        when:
        skillsService.createSubject(p1subj1)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        def skill1Res = skillsService.getSkill(skill1)
        def skill2Res = skillsService.getSkill(skill2)
        then:
        skill1Res.helpUrl == url1After
        skill2Res.helpUrl == url2After
    }

}
