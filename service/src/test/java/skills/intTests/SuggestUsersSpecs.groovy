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
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreIf

class SuggestUsersSpecs extends DefaultIntSpec {

    def "suggest on dashboard users"() {
        // make unique enough lookups to avoid clashing with users created by other tests

        def s1 = createService("jdoe@email.foo", "p@ssw0rd", "John", "Doe")
        //we need to actuall make a request. If the test is running pki mode, just creating the service doesn't result in any changes
        s1.getProjects()
        def s2 = createService("jadoe@email.foo","p@ssw0rd", "Jane", "Doe")
        s2.getProjects()
        def s3 = createService("fbar@email.foo","p@ssw0rd", "Foo", "Bar")
        s3.getProjects()

        expect:
        //one of the other tests, when run in a suite, results in extra users not added by this test, thus the change to containsAll
        def res = skillsService.suggestDashboardUsers(query)
        res.collect({ it.userId }).sort().containsAll(userIds)
        where:
        query  | userIds
        // by user id
        "jdoe@email.foo"        | ["jdoe@email.foo"]
        "Adoe"                  | ["jadoe@email.foo"]
        "email"                 | ["jdoe@email.foo", "jadoe@email.foo", "fbar@email.foo"]
        // by last name
        "Bar"                   | ["fbar@email.foo"]
        // by first name
        "Jane"                  | ["jadoe@email.foo"]
        // by nickname
        "Jane Doe"              | ["jadoe@email.foo"]
        // current user - includeSelf should default to true
        "skills@"               | ["skills@skills.org"]
//        this consistently fails in gitlab-ci but not locally (in either h2 or postgres)
//        "" | ["aafirstsuggestusersspecsuser", "bbsecondsuggestusersspecsuser", "ccthirdsuggestusersspecsuser"]
    }

    def "suggest client users for project"() {

        given:
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )
        def users = getRandomUsers(10)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skills.eachWithIndex{ skill, counter ->
            skillsService.addSkill([projectId:proj.projectId, skillId: skill.skillId], users.get(counter), new Date())
        }

        when:

        def results1 = skillsService.suggestClientUsersForProject(proj.projectId, users.get(4)).collect({ it.userId }).sort()
        def results2 = skillsService.suggestClientUsersForProject(proj.projectId, "").collect({ it.userId }).sort()

        then:
        results1
        results1.contains(users.get(4))
        results2
        results2.unique().size() == results2.size()
        users.containsAll(results2)
    }
}
