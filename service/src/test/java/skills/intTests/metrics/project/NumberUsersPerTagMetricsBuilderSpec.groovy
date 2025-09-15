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
package skills.intTests.metrics.project

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserTag
import skills.storage.repos.UserTagRepo

import java.text.SimpleDateFormat

class NumberUsersPerTagMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "numUsersPerTagBuilder"

    @Autowired
    UserTagRepo userTagRepo

    def "empty res"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: "someVal", currentPage: 1, pageSize: 5, sortDesc: true])
        then:
        res.totalNumItems == 0
        !res.items
    }

    def "count users for each tag value"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[2], key: key, value: "blah" ))

        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[3], key: key, value: "blah1" ))

        userTagRepo.save(new UserTag(userId: users[4], key: key, value: "blah2" ))

        // other key
        userTagRepo.save(new UserTag(userId: users[0], key: key + "a", value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key + "a", value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[2], key: key + "a", value: "blah" ))


        Map props = [
                tagKey: key,
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        skillsService.archiveUsers([users[2]], proj.projectId)
        def resultAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.totalNumItems == 3
        res.items.size() == 3
        res.items.find { it.value == "blah"}.count == 3
        res.items.find { it.value == "blah1"}.count == 2
        res.items.find { it.value == "blah2"}.count == 1

        resultAfterArchive.totalNumItems == 3
        resultAfterArchive.items.size() == 3
        resultAfterArchive.items.find { it.value == "blah"}.count == 2
        resultAfterArchive.items.find { it.value == "blah1"}.count == 2
        resultAfterArchive.items.find { it.value == "blah2"}.count == 1
    }

    def "count users for each tag value - paging"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true])
        def res_pg2 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 2, pageSize: 5, sortDesc: true])
        def res_pg3 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 3, pageSize: 5, sortDesc: true])
        def res_pg4 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 4, pageSize: 5, sortDesc: true])
        then:
        res.totalNumItems == 19
        res.items.size() == 5
        res.items.collect { it.count } == [19, 18, 17, 16, 15]

        res_pg2.totalNumItems == 19
        res_pg2.items.size() == 5
        res_pg2.items.collect { it.count } == [14, 13, 12, 11, 10]

        res_pg3.totalNumItems == 19
        res_pg3.items.size() == 5
        res_pg3.items.collect { it.count } == [9, 8, 7, 6, 5]

        res_pg4.totalNumItems == 19
        res_pg4.items.size() == 4
        res_pg4.items.collect { it.count } == [4, 3, 2, 1]
    }

    def "count users for each tag value - paging - sort asc"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 5, sortDesc: false])
        def res_pg2 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 2, pageSize: 5, sortDesc: false])
        def res_pg3 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 3, pageSize: 5, sortDesc: false])
        def res_pg4 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 4, pageSize: 5, sortDesc: false])
        then:
        res.totalNumItems == 19
        res.items.size() == 5
        res.items.collect { it.count } == [1, 2, 3, 4, 5]

        res_pg2.totalNumItems == 19
        res_pg2.items.size() == 5
        res_pg2.items.collect { it.count } == [6, 7, 8, 9, 10]

        res_pg3.totalNumItems == 19
        res_pg3.items.size() == 5
        res_pg3.items.collect { it.count } == [11, 12, 13, 14, 15]

        res_pg4.totalNumItems == 19
        res_pg4.items.size() == 4
        res_pg4.items.collect { it.count } == [16, 17, 18, 19]
    }

    def "count users for each tag value - filter"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String tagFilter = "LAh1"
        def resAll = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])
        def filterdRes = resAll.items.findAll({ it.value.toLowerCase().contains(tagFilter.toLowerCase())}).sort({ it.count }).reverse()

        when:

        def res = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true, tagFilter: tagFilter])
        def res_pg2 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 2, pageSize: 5, sortDesc: true, tagFilter: tagFilter])
        def res1 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true, tagFilter: "13"])
        then:

        filterdRes.size() == 10
        res.totalNumItems == 10
        res.items.collect { it.value } == filterdRes.subList(0, 5).collect { it.value }
        res.items.collect { it.count } == filterdRes.subList(0, 5).collect { it.count }

        res_pg2.totalNumItems == 10
        res_pg2.items.collect { it.value } == filterdRes.subList(5, 10).collect { it.value }
        res_pg2.items.collect { it.count } == filterdRes.subList(5, 10).collect { it.count }

        res1.totalNumItems == 1
        res1.items.collect { it.value } == ["blah13"]
    }

    def "count users for each tag value - sort by tag asc"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String sortBy = "tag"
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 5, sortDesc: false, sortBy: sortBy])
        def res_pg2 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 2, pageSize: 5, sortDesc: false, sortBy: sortBy])
        def res_pg3 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 3, pageSize: 5, sortDesc: false, sortBy: sortBy])
        def res_pg4 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 4, pageSize: 5, sortDesc: false, sortBy: sortBy])
        then:
        res.totalNumItems == 19
        res.items.size() == 5
        res.items.collect { it.value } == ["blah0", "blah1", "blah10", "blah11", "blah12"]

        res_pg2.totalNumItems == 19
        res_pg2.items.size() == 5
        res_pg2.items.collect { it.value } == ["blah13", "blah14", "blah15", "blah16", "blah17"]

        res_pg3.totalNumItems == 19
        res_pg3.items.size() == 5
        res_pg3.items.collect { it.value } == ["blah18", "blah2", "blah3", "blah4", "blah5"]

        res_pg4.totalNumItems == 19
        res_pg4.items.size() == 4
        res_pg4.items.collect { it.value } == ["blah6", "blah7", "blah8", "blah9"]
    }

    def "count users for each tag value - sort by tag desc"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String sortBy = "tag"
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true, sortBy: sortBy])
        def res_pg2 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 2, pageSize: 5, sortDesc: true, sortBy: sortBy])
        def res_pg3 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 3, pageSize: 5, sortDesc: true, sortBy: sortBy])
        def res_pg4 = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 4, pageSize: 5, sortDesc: true, sortBy: sortBy])
        then:
        res.totalNumItems == 19
        res.items.size() == 5
        res.items.collect { it.value } == ["blah9", "blah8", "blah7", "blah6", "blah5"]

        res_pg2.totalNumItems == 19
        res_pg2.items.size() == 5
        res_pg2.items.collect { it.value } == ["blah4", "blah3", "blah2", "blah18", "blah17"]

        res_pg3.totalNumItems == 19
        res_pg3.items.size() == 5
        res_pg3.items.collect { it.value } == ["blah16", "blah15", "blah14", "blah13", "blah12"]

        res_pg4.totalNumItems == 19
        res_pg4.items.size() == 4
        res_pg4.items.collect { it.value } == ["blah11", "blah10", "blah1", "blah0"]
    }

    def "count users for each tag value in the project with catalog imported skills"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[2], key: key, value: "blah" ))

        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[3], key: key, value: "blah1" ))

        userTagRepo.save(new UserTag(userId: users[4], key: key, value: "blah2" ))

        // other key
        userTagRepo.save(new UserTag(userId: users[0], key: key + "a", value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key + "a", value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[2], key: key + "a", value: "blah" ))


        Map props = [
                tagKey: key,
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)

        skills.each {skillsService.exportSkillToCatalog(it.projectId, it.skillId)}
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        def res_proj2 = skillsService.getMetricsData(proj2.projectId, metricsId, props)
        then:
        res.totalNumItems == 3
        res.items.size() == 3
        res.items.find { it.value == "blah"}.count == 3
        res.items.find { it.value == "blah1"}.count == 2
        res.items.find { it.value == "blah2"}.count == 1

        res_proj2.totalNumItems == 3
        res_proj2.items.size() == 3
        res_proj2.items.find { it.value == "blah"}.count == 3
        res_proj2.items.find { it.value == "blah1"}.count == 2
        res_proj2.items.find { it.value == "blah2"}.count == 1
    }

    def "disabled skills must not produce counts"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[2], key: key, value: "blah" ))

        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[3], key: key, value: "blah1" ))

        userTagRepo.save(new UserTag(userId: users[4], key: key, value: "blah2" ))

        // other key
        userTagRepo.save(new UserTag(userId: users[0], key: key + "a", value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key + "a", value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[2], key: key + "a", value: "blah" ))


        Map props = [
                tagKey: key,
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)

        skills.each {skillsService.exportSkillToCatalog(it.projectId, it.skillId)}
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        def res_proj2 = skillsService.getMetricsData(proj2.projectId, metricsId, props)
        then:
        res.totalNumItems == 3
        res.items.size() == 3
        res.items.find { it.value == "blah"}.count == 3
        res.items.find { it.value == "blah1"}.count == 2
        res.items.find { it.value == "blah2"}.count == 1

        res_proj2.totalNumItems == 0
        !res_proj2.items
    }

    def "count users for each tag value - filter by date"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        List<String> users = getRandomUsers(20)
        List<Date> dates = (1..20).collect{ Date.parse("yyyy-MM-dd HH:mm:ss", '2020-08-01 00:00:00').plus(it).toLocalDateTime().toDate()}

        users.eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, dates[index])
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String today = format.format(format.parse('2020-08-21 00:00:00'))
        String fiveDaysAgo = format.format(format.parse('2020-08-16 00:00:00'))
        String tenDaysAgo = format.format(format.parse('2020-08-11 00:00:00'))
        String twentyDaysAgo = format.format(format.parse('2020-08-01 00:00:00'))
        def resAll = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])

        when:
        def res_5days = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: fiveDaysAgo, toDayFilter: today])
        def res_10days = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: tenDaysAgo, toDayFilter: today])
        def res_20days = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: twentyDaysAgo, toDayFilter: today])

        then:
        resAll.items.size() == 19
        res_5days.items.size() == 19
        res_5days.items == [
            [value:'blah0', count:6],
            [value:'blah1', count:6],
            [value:'blah10', count:6],
            [value:'blah11', count:6],
            [value:'blah12', count:6],
            [value:'blah13', count:6],
            [value:'blah5', count:6],
            [value:'blah6', count:6],
            [value:'blah7', count:6],
            [value:'blah8', count:6],
            [value:'blah9', count:6],
            [value:'blah2', count:6],
            [value:'blah3', count:6],
            [value:'blah4', count:6],
            [value:'blah14', count:5],
            [value:'blah15', count:4],
            [value:'blah16', count:3],
            [value:'blah17', count:2],
            [value:'blah18', count:1]
        ]

        res_10days.items.size() == 19
        res_10days.items == [
                [value:'blah2', count:11],
                [value:'blah1', count:11],
                [value:'blah0', count:11],
                [value:'blah8', count:11],
                [value:'blah7', count:11],
                [value:'blah6', count:11],
                [value:'blah5', count:11],
                [value:'blah4', count:11],
                [value:'blah3', count:11],
                [value:'blah9', count:10],
                [value:'blah10', count:9],
                [value:'blah11', count:8],
                [value:'blah12', count:7],
                [value:'blah13', count:6],
                [value:'blah14', count:5],
                [value:'blah15', count:4],
                [value:'blah16', count:3],
                [value:'blah17', count:2],
                [value:'blah18', count:1]
        ]

        res_20days.items.size() == 19
        res_20days.items == [
                [value:'blah0', count:19],
                [value:'blah1', count:18],
                [value:'blah2', count:17],
                [value:'blah3', count:16],
                [value:'blah4', count:15],
                [value:'blah5', count:14],
                [value:'blah6', count:13],
                [value:'blah7', count:12],
                [value:'blah8', count:11],
                [value:'blah9', count:10],
                [value:'blah10', count:9],
                [value:'blah11', count:8],
                [value:'blah12', count:7],
                [value:'blah13', count:6],
                [value:'blah14', count:5],
                [value:'blah15', count:4],
                [value:'blah16', count:3],
                [value:'blah17', count:2],
                [value:'blah18', count:1]
        ]
    }

    def "count users for each tag value - filter by date and tag"() {

        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        List<String> users = getRandomUsers(20)
        List<Date> dates = (1..20).collect{ Date.parse("yyyy-MM-dd HH:mm:ss", '2020-08-01 00:00:00').plus(it).toLocalDateTime().toDate()}

        users.eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj.projectId, skillId: skills.get(0).skillId], it, dates[index])
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String today = format.format(format.parse('2020-08-21 00:00:00'))
        String fiveDaysAgo = format.format(format.parse('2020-08-16 00:00:00'))
        String tenDaysAgo = format.format(format.parse('2020-08-11 00:00:00'))
        String tagFilter = 'blah11'
        def resAll = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])

        when:
        def res_5days = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: fiveDaysAgo, toDayFilter: today])
        def res_10days = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: tenDaysAgo, toDayFilter: today])
        def res_5days_filter = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: fiveDaysAgo, toDayFilter: today, tagFilter: tagFilter])
        def res_10days_filter = skillsService.getMetricsData(proj.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: tenDaysAgo, toDayFilter: today, tagFilter: tagFilter])

        then:
        resAll.items.size() == 19
        res_5days.items.size() == 19
        res_10days.items.size() == 19
        res_5days_filter.items.size() == 1
        res_10days_filter.items.size() == 1

    }

    def "count users for each tag value - multiple projects and subjects"() {

        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def proj1subj1 = SkillsFactory.createSubject(1, 1)
        def proj1subj2 = SkillsFactory.createSubject(1, 2)
        def proj2subj1 = SkillsFactory.createSubject(2, 1)
        def proj2subj2 = SkillsFactory.createSubject(2, 2)
        List<Map> proj1subj1skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> proj1subj2skills = SkillsFactory.createSkills(3, 1, 2)
        List<Map> proj2subj1skills = SkillsFactory.createSkills(3, 2, 1)
        List<Map> proj2subj2skills = SkillsFactory.createSkills(3, 2, 2)

        proj1subj1skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }
        proj1subj2skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }
        proj2subj1skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }
        proj2subj2skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(proj1subj1)
        skillsService.createSubject(proj1subj2)
        skillsService.createSubject(proj2subj1)
        skillsService.createSubject(proj2subj2)
        skillsService.createSkills(proj1subj1skills)
        skillsService.createSkills(proj1subj2skills)
        skillsService.createSkills(proj2subj1skills)
        skillsService.createSkills(proj2subj2skills)

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        List<String> users = getRandomUsers(20)
        List<Date> dates = (1..20).collect{ Date.parse("yyyy-MM-dd HH:mm:ss", '2020-08-01 00:00:00').plus(it).toLocalDateTime().toDate()}

        users.eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1subj1skills.get(0).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1subj1skills.get(1).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1subj1skills.get(2).skillId], it, dates[index])
        }
        users[0..4].eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1subj2skills.get(0).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1subj2skills.get(1).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1subj2skills.get(2).skillId], it, dates[index])
        }
        users[5..9].eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj2.projectId, skillId: proj2subj1skills.get(0).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj2.projectId, skillId: proj2subj1skills.get(1).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj2.projectId, skillId: proj2subj1skills.get(2).skillId], it, dates[index])
        }
        users[10..14].eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj2.projectId, skillId: proj2subj2skills.get(0).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj2.projectId, skillId: proj2subj2skills.get(1).skillId], it, dates[index])
            skillsService.addSkill([projectId: proj2.projectId, skillId: proj2subj2skills.get(2).skillId], it, dates[index])
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String today = format.format(format.parse('2020-08-21 00:00:00'))
        String fiveDaysAgo = format.format(format.parse('2020-08-16 00:00:00'))
        String tenDaysAgo = format.format(format.parse('2020-08-11 00:00:00'))
        String twentyDaysAgo = format.format(format.parse('2020-08-01 00:00:00'))
        def proj1_resAll = skillsService.getMetricsData(proj1.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])
        def proj2_resAll = skillsService.getMetricsData(proj2.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])

        when:
        def proj1_res_5days = skillsService.getMetricsData(proj1.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: fiveDaysAgo, toDayFilter: today])
        def proj1_res_10days = skillsService.getMetricsData(proj1.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: tenDaysAgo, toDayFilter: today])
        def proj2_res_10days = skillsService.getMetricsData(proj2.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: tenDaysAgo, toDayFilter: today])
        def proj2_res_20days = skillsService.getMetricsData(proj2.projectId, metricsId, [tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: twentyDaysAgo, toDayFilter: tenDaysAgo])

        then:
        proj1_resAll.items.size() == 19
        proj1_res_5days.items.size() == 19
        proj1_res_10days.items.size() == 19
        proj2_resAll.items.size() == 14
        proj2_res_10days.items.size() == 0
        proj2_res_20days.items.size() == 14
        proj1_resAll.items.collect { it.count }.max() == 19
        proj2_resAll.items.collect{ it.count }.max() == 10

    }
}
