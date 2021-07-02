/**
 * Copyright 2021 SkillTree
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
package skills.intTests.adminDisplayOrder

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class ProjectOrderSpecs extends DefaultIntSpec {

    String rootOne = "jh@dojo.com"
    String rootTwo = "bob@email.foo"
    SkillsService rootServiceOne
    SkillsService rootServiceTwo

    def setup() {
        rootServiceOne = createService(rootOne, "aaaaaaaaaaaaaaaa")
        if (!rootServiceOne.isRoot()) {
            rootServiceOne.grantRoot()
        }
        rootServiceTwo = createService(rootTwo, "bbbbbbbbbbbbbbbbb")
        if (!rootServiceTwo.isRoot()) {
            rootServiceOne.grantRootRole(rootTwo)
            assert rootServiceTwo.isRoot()
        }
    }

    def "root user change order of project created by another user" () {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj3 = SkillsFactory.createProject(3)

        rootServiceTwo.createProject(proj1)
        rootServiceTwo.pinProject(proj1.projectId)

        rootServiceOne.createProject(proj2)
        rootServiceOne.createProject(proj3)

        rootServiceTwo.pinProject(proj3.projectId)

        when:
        rootServiceTwo.moveProjectUp(proj3)

        def projects = rootServiceTwo.getProjects()

        then:
        projects.size() == 2
        projects.sort() { it.order }[0].projectId == proj3.projectId
        projects.sort() { it.order }[1].projectId == proj1.projectId

    }

}
