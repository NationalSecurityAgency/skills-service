/**
 * Copyright 2026 SkillTree
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
package skills.intTests.crossProject

import groovy.json.JsonSlurper
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.intTests.utils.SystemSSLConfiguration
import spock.lang.IgnoreIf

class SharedSkillsPaginationSpec extends DefaultIntSpec {

    def "shared skills support pages and accurate totals - incoming=#incoming"() {
        setup:
        createShares()

        when:
        Map first = loadShares(incoming, 2, 1, 'skill.name')
        Map second = loadShares(incoming, 2, 2, 'skill.name')
        Map beyondLast = loadShares(incoming, 2, 3, 'skill.name')
        Map maximumSize = loadShares(incoming, 200, 1, 'skill.name')

        then:
        first.totalCount == 4
        second.totalCount == 4
        beyondLast.totalCount == 4
        first.data*.skillName == firstNames
        second.data*.skillName == secondNames
        beyondLast.data == []
        maximumSize.data*.skillName == firstNames + secondNames

        where:
        incoming | firstNames         | secondNames
        false    | ['Alpha', 'Alpha'] | ['Beta', 'Gamma']
        true     | ['Alpha', 'Beta']  | ['Delta', 'Gamma']
    }

    def "shared skills sort by #orderBy ascending=#ascending - incoming=#incoming"() {
        setup:
        createShares()

        expect:
        loadShares(incoming, 200, 1, orderBy, ascending).data.collect { Map row -> row[resultField] } == expected

        where:
        incoming | orderBy              | ascending | resultField   | expected
        false    | 'skill.name'         | true      | 'skillName'   | ['Alpha', 'Alpha', 'Beta', 'Gamma']
        false    | 'skill.name'         | false     | 'skillName'   | ['Gamma', 'Beta', 'Alpha', 'Alpha']
        false    | 'skill.skillId'      | true      | 'skillId'     | ['skill1', 'skill1', 'skill2', 'skill3']
        false    | 'skill.skillId'      | false     | 'skillId'     | ['skill3', 'skill2', 'skill1', 'skill1']
        false    | 'sharedProject.name' | true      | 'projectName' | ['Alpha Project', 'Zulu Project', 'Zulu Project', 'Zulu Project']
        false    | 'sharedProject.name' | false     | 'projectName' | ['Zulu Project', 'Zulu Project', 'Zulu Project', 'Alpha Project']
        true     | 'skill.name'         | true      | 'skillName'   | ['Alpha', 'Beta', 'Delta', 'Gamma']
        true     | 'skill.name'         | false     | 'skillName'   | ['Gamma', 'Delta', 'Beta', 'Alpha']
        true     | 'skill.name'         | true      | 'skillName'   | ['Alpha', 'Beta', 'Delta', 'Gamma']
        true     | 'projectName'        | true      | 'projectName' | ['Alpha Project', 'Source Project', 'Source Project', 'Source Project']
        true     | 'projectName'        | false     | 'projectName' | ['Source Project', 'Source Project', 'Source Project', 'Alpha Project']
        true     | 'skill.skillId'      | true      | 'skillId'     | ['skill1', 'skill1', 'skill2', 'skill3']
        true     | 'skill.skillId'      | false     | 'skillId'     | ['skill3', 'skill2', 'skill1', 'skill1']
    }

    def "shared skills filtering updates totals and pages - incoming=#incoming filter=#filter"() {
        setup:
        createShares()

        when:
        Map result = loadShares(incoming, 1, 1, 'skill.name', true, filter)
        Map second = loadShares(incoming, 1, 2, 'skill.name', true, filter)

        then:
        result.totalCount == expectedCount
        second.totalCount == expectedCount
        result.data*.skillName == firstNames
        second.data*.skillName == secondNames

        where:
        incoming | filter            | expectedCount | firstNames | secondNames
        false    | 'SKILL1'          | 2             | ['Alpha']  | ['Alpha']
        true     | 'SKILL1'          | 2             | ['Alpha']  | ['Delta']
        true     | 'aLpH'            | 1             | ['Alpha']  | []
        true     | 'no such skill'   | 0             | []         | []
        false    | 'no such skill'   | 0             | []         | []
        true     | "' OR 1=1 --"     | 0             | []         | []
        false    | "' OR 1=1 --"     | 0             | []         | []
    }

    def "invalid paging receives structured BadParam - incoming=#incoming limit=#limit page=#page"() {
        setup:
        createShares()

        when:
        loadShares(incoming, limit, page)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        new JsonSlurper().parseText(exception.resBody).errorCode == 'BadParam'
        new JsonSlurper().parseText(exception.resBody).explanation.contains(message)

        where:
        [incoming, limit, page, message] << [false, true].collectMany { Boolean incoming ->
            [[incoming, 0, 1, 'Limit must be greater than 0'],
             [incoming, -1, 1, 'Limit must be greater than 0'],
             [incoming, 201, 1, 'Cannot ask for more than 200 items'],
             [incoming, 5, 0, 'Page must be 1 or greater'],
             [incoming, 5, -1, 'Page must be 1 or greater']]
        }
    }

    def "invalid sort fields receive structured BadParam - incoming=#incoming orderBy=#orderBy"() {
        setup:
        createShares()

        when:
        loadShares(incoming, 5, 1, orderBy)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        new JsonSlurper().parseText(exception.resBody).errorCode == 'BadParam'
        new JsonSlurper().parseText(exception.resBody).explanation

        where:
        [incoming, orderBy] << [false, true].collectMany { Boolean incoming ->
            ['', 'notAField', 'lower(skill.name)', 'skill.name desc; select 1'].collect { String field -> [incoming, field] }
        } + [[false, 'skillName'], [false, 'projectName'], [true, 'sharedProject.name']]
    }

    def "paging with tied sort values neither loses nor repeats shares - incoming=#incoming"() {
        setup:
        createShares()

        when:
        List<Map> rows = (1..4).collectMany { int page -> loadShares(incoming, 1, page, orderBy).data }
        List<Map> repeated = (1..4).collectMany { int page -> loadShares(incoming, 1, page, orderBy).data }

        then:
        rows.collect { Map row -> [row.skillId, row.projectId] }.sort() == expected.sort()
        repeated.collect { Map row -> [row.skillId, row.projectId] } == rows.collect { Map row -> [row.skillId, row.projectId] }

        where:
        incoming | orderBy         | expected
        false    | 'skill.name'    | [['skill1', 'TestProject2'], ['skill1', 'TestProject3'], ['skill2', 'TestProject2'], ['skill3', 'TestProject2']]
        true     | 'skill.skillId' | [['skill1', 'TestProject1'], ['skill1', 'TestProject3'], ['skill2', 'TestProject1'], ['skill3', 'TestProject1']]
    }

    def "paging breaks sort ties by ascending share ID - incoming=#incoming orderBy=#orderBy ascending=#ascending pageSize=#pageSize"() {
        setup:
        createShares()
        Map globallyShared = SkillsFactory.createSkill(3, 1, 1)
        globallyShared.name = 'Alpha'
        skillsService.updateSkill(globallyShared)

        // Recreating this share puts the oldest skill last in share-ID order.
        skillsService.deleteShared('TestProject1', 'skill1', 'TestProject2')
        skillsService.shareSkill('TestProject1', 'skill1', 'TestProject2')

        List<List<String>> sharesByCreationOrder = incoming
                ? [['skill2', 'TestProject1'], ['skill3', 'TestProject1'], ['skill1', 'TestProject3'], ['skill1', 'TestProject1']]
                : [['skill2', 'TestProject2'], ['skill3', 'TestProject2'], ['skill1', 'TestProject3'], ['skill1', 'TestProject2']]
        List<List<String>> expected = expectedOrder.collect { int index -> sharesByCreationOrder[index] }

        when:
        int pageCount = (int) Math.ceil(4 / (double) pageSize)
        List<Map> pages = (1..pageCount).collect { int page -> loadShares(incoming, pageSize, page, orderBy, ascending) }
        Map beyondLast = loadShares(incoming, pageSize, pageCount + 1, orderBy, ascending)
        Map allShares = loadShares(incoming, 200, 1, orderBy, ascending)

        then:
        pages.every { Map result -> result.totalCount == 4 }
        pages*.data*.size() == (pageSize == 1 ? [1, 1, 1, 1] : [3, 1])
        pages.collectMany { Map result -> result.data.collect { Map row -> [row.skillId, row.projectId] } } == expected
        allShares.data.collect { Map row -> [row.skillId, row.projectId] } == expected
        beyondLast.totalCount == 4
        beyondLast.data == []

        where:
        incoming | orderBy              | ascending | expectedOrder
        false    | 'skill.name'         | true      | [2, 3, 1, 0]
        false    | 'skill.name'         | false     | [0, 1, 2, 3]
        false    | 'skill.skillId'      | true      | [2, 3, 0, 1]
        false    | 'skill.skillId'      | false     | [1, 0, 2, 3]
        false    | 'sharedProject.name' | true      | [2, 0, 1, 3]
        false    | 'sharedProject.name' | false     | [0, 1, 3, 2]
        true     | 'skill.name'         | true      | [2, 3, 1, 0]
        true     | 'skill.name'         | false     | [0, 1, 2, 3]
        true     | 'skill.skillId'      | true      | [2, 3, 0, 1]
        true     | 'skill.skillId'      | false     | [1, 0, 2, 3]
        true     | 'projectName'        | true      | [2, 0, 1, 3]
        true     | 'projectName'        | false     | [0, 1, 3, 2]

        combined:
        pageSize << [1, 3]
    }

    def "outgoing global shares retain exact identities across partial pages - ascending=#ascending"() {
        setup:
        createShares()
        skillsService.deleteShared('TestProject1', 'skill3', 'TestProject2')
        skillsService.shareSkill('TestProject1', 'skill3', 'ALL_SKILLS_PROJECTS')

        when:
        Map first = loadShares(false, 3, 1, 'sharedProject.name', ascending)
        Map last = loadShares(false, 3, 2, 'sharedProject.name', ascending)
        Map filtered = loadShares(false, 3, 1, 'sharedProject.name', ascending, 'SKILL3')

        then:
        first.totalCount == 4
        last.totalCount == 4
        first.data.size() == 3
        last.data.size() == 1
        (first.data + last.data).collect { Map row -> [row.skillId, row.projectId, row.sharedWithAllProjects] } == expected
        filtered.totalCount == 1
        filtered.data.collect { Map row -> [row.skillId, row.projectId, row.sharedWithAllProjects] } == [['skill3', null, true]]

        where:
        ascending | expected
        true      | [['skill1', 'TestProject3', false], ['skill1', 'TestProject2', false], ['skill2', 'TestProject2', false], ['skill3', null, true]]
        false     | [['skill3', null, true], ['skill1', 'TestProject2', false], ['skill2', 'TestProject2', false], ['skill1', 'TestProject3', false]]
    }

    def "unrelated admins and ordinary users cannot read sharing endpoints - role=#role endpoint=#endpoint"() {
        setup:
        createShares()
        SkillsService otherUser = createService(getRandomUsers(1)[0])
        if (role == 'unrelated admin') {
            otherUser.createProject(SkillsFactory.createProject(4))
        }

        when:
        readEndpoint(otherUser, endpoint)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.FORBIDDEN

        where:
        [role, endpoint] << ['unrelated admin', 'ordinary user'].collectMany { String role ->
            ['shared', 'sharedWithMe', 'isSkillShared'].collect { String endpoint -> [role, endpoint] }
        }
    }

    @IgnoreIf({ SystemSSLConfiguration.isPki() })
    def "anonymous callers cannot read sharing endpoints - #endpoint"() {
        setup:
        createShares()

        when:
        new RestTemplate().getForEntity("http://localhost:${localPort}/admin/projects/TestProject1/${endpoint}?limit=5&page=1&orderBy=skill.name&ascending=true&skillId=skill1".toString(), String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]

        where:
        endpoint << ['shared', 'sharedWithMe', 'isSkillShared']
    }

    def "sharing lookup rejects invalid skill or destination - skillId=#skillId destination=#destination"() {
        setup:
        createShares()

        when:
        skillsService.isSkillShared('TestProject1', skillId, destination)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == expectedStatus
        new JsonSlurper().parseText(exception.resBody).explanation.contains(message)

        where:
        skillId        | destination      | expectedStatus         | message
        ''             | 'TestProject2'   | HttpStatus.BAD_REQUEST | 'Skill Id'
        'missingSkill' | 'TestProject2'   | HttpStatus.BAD_REQUEST | 'missingSkill'
        'skill1'       | 'missingProject' | HttpStatus.NOT_FOUND   | 'missingProject'
    }

    private void createShares() {
        Map source = SkillsFactory.createProject(1)
        source.name = 'Source Project'
        Map destination = SkillsFactory.createProject(2)
        destination.name = 'Zulu Project'
        Map other = SkillsFactory.createProject(3)
        other.name = 'Alpha Project'
        [source, destination, other].each { Map project -> skillsService.createProject(project) }
        skillsService.createSubject(SkillsFactory.createSubject(1, 1))
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1)
        ['Alpha', 'Gamma', 'Beta'].eachWithIndex { String name, int index -> skills[index].name = name }
        skillsService.createSkills(skills)
        skills.each { Map skill -> skillsService.shareSkill(source.projectId, skill.skillId, destination.projectId) }
        skillsService.shareSkill(source.projectId, 'skill1', other.projectId)
        skillsService.createSubject(SkillsFactory.createSubject(3, 1))
        Map globallyShared = SkillsFactory.createSkill(3, 1, 1)
        globallyShared.name = 'Delta'
        skillsService.createSkill(globallyShared)
        skillsService.shareSkill(other.projectId, globallyShared.skillId, 'ALL_SKILLS_PROJECTS')
    }

    private Map loadShares(boolean incoming, int limit = 5, int page = 1, String orderBy = 'skill.name', boolean ascending = true, String filter = '') {
        return incoming
                ? skillsService.getSharedWithMeSkills('TestProject2', limit, page, orderBy, ascending, filter)
                : skillsService.getSharedSkills('TestProject1', limit, page, orderBy, ascending, filter)
    }

    private void readEndpoint(SkillsService client, String endpoint) {
        if (endpoint == 'isSkillShared') {
            client.isSkillShared('TestProject1', 'skill1', 'TestProject2')
        } else if (endpoint == 'sharedWithMe') {
            client.getSharedWithMeSkills('TestProject2')
        } else {
            client.getSharedSkills('TestProject1')
        }
    }
}
