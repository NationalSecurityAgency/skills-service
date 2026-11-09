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
package skills.intTests.catalog

import org.springframework.test.annotation.DirtiesContext
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.Setting
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement

import static skills.intTests.utils.SkillsFactory.*

/** Temporary pre-5.1 Fix acceptance for reused credit; retire after remediation. */
// Close this context's scheduler before the dedicated IT context installs worker barriers.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationFixReuseSpecs extends CatalogCleanupIntSpec {
    def "targeted Fix repairs #sourceCredit source credit and levels while preserving valid awards for #qualification"() {
        List<String> users = getRandomUsers(3)
        Map setup = reuseFixture(['TestProject2': [users[0]], 'TestProject3': [users[0], users[1]]], 1, true)
        Date earned = new Date().clearTime() - 7
        skillsService.addSkill(setup.source, users[1], earned)
        if (sourceCredit == 'positive') { skillsService.addSkill(setup.source, users[0], earned) }
        setup.destinations.each { Map destination ->
            users.drop(1).each { String user -> createService(user).initializeProjectProgress(destination.projectId) }
            if (qualification == 'COMPLETED participant') { createService(users[0]).initializeProjectProgress(destination.projectId) }
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        users.each { String user -> seedGlobal(user) }
        Map target = setup.destinations[0]
        String user = users[0]
        Map original = snapshot(target.projectId, user)
        int projectLevel = sourceCredit == 'positive' ? 3 : 1
        int subjectLevel = sourceCredit == 'positive' ? 2 : 1
        List<String> replacedIds = [target.imported.skillId, target.importedCopies[1].skillId,
                                    target.importedGroup.skillId, target.badge.badgeId, 'TestSubject4']
        List<Map> validAwards = original.awards.findAll { Map award ->
            !(award.skillId in replacedIds) &&
                    (award.skillId != null || award.level <= projectLevel) &&
                    (award.skillId != 'TestSubject1' || award.level <= subjectLevel)
        }
        List<String> stableSkillIds = [target.nativeSkill.skillId, target.nativeCopy.skillId] +
                (sourceCredit == 'positive' ? [target.importedCopies[0].skillId] : [])
        List<Map> stablePoints = original.points.findAll { Map point -> point.skillId in stableSkillIds }
        assert validAwards.find { Map award -> award.skillId == target.nativeGroup.skillId }
        assert validAwards.find { Map award -> award.skillId == target.nativeBadge.badgeId }
        assert validAwards.find { Map award -> award.skillId == null && award.level == 1 }

        if (sourceCredit == 'positive') {
            // Leave one valid imported reuse intact; lose the original and the grouped copy.
            [target.imported, target.importedCopies[1]].each { SkillDef skill ->
                userPointsRepo.delete(userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, skill.skillId))
                userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, target.projectId, skill.skillId))
            }
            [target.importedGroup.skillId, target.badge.badgeId, 'TestSubject4'].each { String skillId ->
                userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, target.projectId, skillId))
            }
        } else {
            // Legacy full imported credit and awards must disappear when current source credit is empty.
            ([target.imported] + target.importedCopies).each { SkillDef skill ->
                seedPoints(target.projectId, user, skill, 100, earned)
                seedAward(target.projectId, user, skill, null, earned)
            }
            seedAward(target.projectId, user, target.importedGroup, null, earned)
            seedAward(target.projectId, user, skillDefRepo.findByProjectIdAndSkillId(target.projectId, target.badge.badgeId), null, earned)
        }
        (target.subjects + [target.nativeGroup, target.importedGroup]).each { SkillDef container -> seedPoints(target.projectId, user, container, 999) }
        seedPoints(target.projectId, user, null, 999)
        seedLevels(target.projectId, user, null, (1..5).toList())
        target.subjects.each { SkillDef subject -> seedLevels(target.projectId, user, subject, (1..5).toList()) }
        // Different, deliberately stale values outside the target expose accidental broad repair SQL.
        seedPoints('TestProject3', users[0], setup.destinations[1].subject, 713)
        seedPoints('TestProject3', users[0], null, 817)
        seedPoints('TestProject2', users[1], target.subject, 619)
        seedPoints('TestProject2', users[1], null, 923)
        Map before = projectUserSnapshots(setup.destinations, users)
        Map memberships = projectMembershipSnapshots(setup.destinations, users)
        Map sources = users.collectEntries { String learner -> [(learner): snapshot(setup.source.projectId, learner)] }
        Integer sourceId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        Map sourceEvents = users.collectEntries { String learner -> [(learner): events(sourceId, learner)] }
        Map globals = users.collectEntries { String learner -> [(learner): globalSnapshot(learner)] }
        SkillsService root = createRootSkillService()

        when:
        root.fixImportedSkillProgress(target.projectId, user)

        then:
        assertDisplayedProgress(target, user, sourceCredit == 'positive' ? [200, 100, 100, 100] : [100, 0, 100, 0],
                sourceCredit == 'positive' ? 500 : 200, sourceCredit == 'positive' ? [2, 5, 5, 5] : [1, 0, 5, 0],
                projectLevel, true, sourceCredit == 'positive')
        assertLevels(target.projectId, user, null, projectLevel)
        target.subjects.eachWithIndex { SkillDef subject, int index ->
            assertLevels(target.projectId, user, subject.skillId, (sourceCredit == 'positive' ? [2, 5, 5, 5] : [1, 0, 5, 0])[index])
            assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, subject.skillId).points ==
                    (sourceCredit == 'positive' ? [200, 100, 100, 100] : [100, 0, 100, 0])[index]
        }
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, null).points == (sourceCredit == 'positive' ? 500 : 200)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, target.nativeGroup.skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, target.importedGroup.skillId).points == (sourceCredit == 'positive' ? 100 : 0)
        snapshot(target.projectId, user).points.findAll { Map point -> point.skillId in stableSkillIds } == stablePoints
        snapshot(target.projectId, user).awards.findAll { Map award -> award.id in validAwards*.id } == validAwards
        ([target.imported] + target.importedCopies).each { SkillDef skill ->
            List<UserAchievement> awards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, target.projectId, skill.skillId)
            assert awards.size() == (sourceCredit == 'positive' ? 1 : 0)
            if (sourceCredit == 'positive') {
                assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, skill.skillId).points == 100
                assert awards.first().achievedOn == earned
            } else {
                assert !userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, skill.skillId)
            }
        }
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, target.projectId, target.importedGroup.skillId).size() == (sourceCredit == 'positive' ? 1 : 0)
        projectMembershipSnapshots(setup.destinations, users) == memberships
        before.findAll { String key, Map rows -> key != "${target.projectId}/${user}" }.every { String key, Map rows ->
            List<String> pair = key.split('/') as List<String>
            snapshot(pair[0], pair[1]) == rows
        }
        users.every { String learner -> snapshot(setup.source.projectId, learner) == sources[learner] &&
                events(sourceId, learner) == sourceEvents[learner] && globalSnapshot(learner) == globals[learner] }
        !backfillTasks()

        when:
        Map repaired = projectUserSnapshots(setup.destinations, users)
        root.fixImportedSkillProgress(target.projectId, user)

        then:
        projectUserSnapshots(setup.destinations, users) == repaired
        projectMembershipSnapshots(setup.destinations, users) == memberships
        users.every { String learner -> globalSnapshot(learner) == globals[learner] }

        where:
        sourceCredit | qualification
        'positive'   | 'local-event-only user'
        'positive'   | 'COMPLETED participant'
        'empty'      | 'local-event-only user'
        'empty'      | 'COMPLETED participant'
    }

    def "Fix removes stale original and reused imported points but preserves achievement-only skill group and badge awards"() {
        String user = getRandomUsers(1)[0]
        Map setup = reuseFixture(['TestProject2': [user], 'TestProject3': [user]])
        Map target = setup.destinations[0]
        Date earned = new Date().clearTime() - 7
        SkillDef source = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId)
        // Valid source completion survives without points or performed history. Destination
        // awards match it, but legacy points and their derived levels are stale.
        seedAward(source.projectId, user, source, null, earned)
        setup.destinations.each { Map destination -> seedReusedProgress(destination, user, 100, 100, earned) }
        participationRepo.save(new ProjectParticipation(projectId: target.projectId, userId: user,
                backfillStatus: BackfillStatus.COMPLETED))
        seedGlobal(user)
        Map before = snapshot(target.projectId, user)
        Map otherProject = snapshot('TestProject3', user)
        Map sourceBefore = snapshot(source.projectId, user)
        List<Map> sourceEvents = events(source.id, user)
        Integer nativeRefId = skillDefRepo.findByProjectIdAndSkillId(target.projectId, target.nativeSkill.skillId).id
        List<Map> nativeEvents = events(nativeRefId, user)
        Map participant = membership(target.projectId, user)
        List<Map> global = globalSnapshot(user)
        List<String> nativeIds = [target.nativeSkill.skillId, target.nativeCopy.skillId]
        List<String> importedIds = [target.imported.skillId] + target.importedCopies*.skillId
        List<Map> nativePoints = before.points.findAll { Map point -> point.skillId in nativeIds }
        Map<String, Integer> retainedLevels = [(null): 2, 'TestSubject1': 3, 'TestSubject2': 0,
                                               'TestSubject3': 5, 'TestSubject4': 0]
        List<Map> validAwards = before.awards.findAll { Map award ->
            award.level == null || award.level <= retainedLevels[award.skillId]
        }
        assert sourceBefore.points == []
        assert sourceBefore.awards.size() == 1 && sourceBefore.awards.first().achievedOn == earned
        assert sourceEvents == []
        assert nativePoints.size() == 2
        assert before.points.findAll { Map point -> point.skillId in importedIds }*.points == [100, 100, 100]
        assert validAwards.findAll { Map award -> award.skillId in importedIds }.size() == 3
        assert validAwards.find { Map award -> award.skillId == target.importedGroup.skillId }
        assert validAwards.find { Map award -> award.skillId == target.badge.badgeId }
        SkillsService root = createRootSkillService()

        when:
        root.fixImportedSkillProgress(target.projectId, user)

        then:
        SkillsService learner = createService(user)
        Map summary = learner.getSkillsSummaryForCurrentUser(target.projectId)
        summary.points == 200
        summary.skillsLevel == 2
        target.subjects.eachWithIndex { SkillDef subject, int index ->
            Map subjectSummary = learner.getSubjectSummaryForCurrentUser(target.projectId, subject.skillId)
            assert subjectSummary.points == [100, 0, 100, 0][index]
            assert subjectSummary.skillsLevel == [3, 0, 5, 0][index]
        }
        learner.getSkillsGroupSummary(target.projectId, target.nativeGroup.skillId).skillsAchieved == 1
        // Rendered group child completion is points-based. Its stored group award and
        // the badge remain valid despite zero points; exact award snapshots below protect them.
        learner.getSkillsGroupSummary(target.projectId, target.importedGroup.skillId).skillsAchieved == 0
        learner.getBadgeSummary(null, target.projectId, target.nativeBadge.badgeId).badgeAchieved
        learner.getBadgeSummary(null, target.projectId, target.badge.badgeId).badgeAchieved
        snapshot(target.projectId, user).awards == validAwards
        snapshot(target.projectId, user).points.findAll { Map point -> point.skillId in nativeIds } == nativePoints
        importedIds.each { String skillId ->
            assert !userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, skillId)
            List<UserAchievement> awards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, target.projectId, skillId)
            assert awards.size() == 1 && awards.first().achievedOn == earned
        }
        target.subjects.eachWithIndex { SkillDef subject, int index ->
            assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, subject.skillId).points == [100, 0, 100, 0][index]
            assertLevels(target.projectId, user, subject.skillId, [3, 0, 5, 0][index])
        }
        assertLevels(target.projectId, user, null, 2)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, null).points == 200
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, target.nativeGroup.skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, target.importedGroup.skillId).points == 0
        membership(target.projectId, user) == participant
        globalSnapshot(user) == global
        snapshot(source.projectId, user) == sourceBefore
        events(source.id, user) == sourceEvents
        events(nativeRefId, user) == nativeEvents
        snapshot('TestProject3', user) == otherProject
        !backfillTasks()

        when:
        Map repaired = snapshot(target.projectId, user)
        root.fixImportedSkillProgress(target.projectId, user)

        then:
        snapshot(target.projectId, user) == repaired
        membership(target.projectId, user) == participant
        globalSnapshot(user) == global
    }

    def "Fix sums all earned optional-group subject credit while preserving rendered group progress and valid awards"() {
        String user = getRandomUsers(1)[0]
        Map setup = reuseFixture(['TestProject2': [user]], 1, true)
        Map destination = setup.destinations[0]
        Map optionalChild = createSkill(2, 3, 32, 0, 1, 0, 100)
        skillsService.assignSkillToSkillsGroup(destination.nativeGroup.skillId, optionalChild)
        Map groupRequest = createSkillsGroup(2, 3, 31) + [numSkillsRequired: 1]
        skillsService.updateSkill(groupRequest)
        skillsService.addSkill(optionalChild, user, new Date() - 3)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        // Explicitly emulate a stale legacy aggregate; current reuse correctly credits both children.
        seedPoints(destination.projectId, user, destination.subjects[2], 100)
        Map before = snapshot(destination.projectId, user)
        List<Map> nativeAwards = before.awards.findAll { Map award -> award.skillId in
                [destination.nativeSkill.skillId, destination.nativeCopy.skillId, optionalChild.skillId, destination.nativeGroup.skillId, destination.nativeBadge.badgeId] }
        Map participant = membership(destination.projectId, user)
        Map subject = createService(user).getSubjectSummaryForCurrentUser(destination.projectId, 'TestSubject3')
        // The drilldown caps rendered group progress at one required child. Stored subject totals
        // sum both earned child rows; Fix repairs the fixture's incomplete 100-point aggregate.
        assert subject.points == 100
        assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, 'TestSubject3').points == 100
        assert subject.skills.find { Map skill -> skill.skillId == destination.nativeGroup.skillId }.points == 100
        assert subject.skills.first().children*.points == [100, 100]

        when:
        createRootSkillService().fixImportedSkillProgress(destination.projectId, user)

        then:
        Map repairedSubject = createService(user).getSubjectSummaryForCurrentUser(destination.projectId, 'TestSubject3')
        repairedSubject.points == 100
        repairedSubject.skills.find { Map skill -> skill.skillId == destination.nativeGroup.skillId }.points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, 'TestSubject3').points == 200
        createService(user).getSkillsSummaryForCurrentUser(destination.projectId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, null).points == 300
        snapshot(destination.projectId, user).awards.findAll { Map award -> award.id in nativeAwards*.id } == nativeAwards
        membership(destination.projectId, user) == participant
    }

    def "community-excluded Fix preserves mixed former-member native and reused credit and valid native awards on first repair"() {
        List<String> users = getRandomUsers(2)
        Map setup = reuseFixture()
        Map target = setup.destinations[0]
        Date earned = new Date().clearTime() - 7
        SkillsService root = createRootSkillService()
        root.saveUserTag(root.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])
        users.each { String user ->
            root.saveUserTag(user, 'dragons', ['DivineDragon'])
            setup.destinations.each { Map destination -> skillsService.addSkill(destination.nativeSkill, user, earned) }
            skillsService.addSkill(setup.source, user, earned)
            createService(user).initializeProjectProgress(target.projectId)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        users.each { String user -> seedGlobal(user) }
        // Earned progress precedes the policy change. Remove only this former member's
        // community tag; the root/admin callers and the control learner remain authorized.
        settingRepo.save(new Setting(projectId: target.projectId, type: Setting.SettingType.Project,
                setting: 'user_community', value: 'true'))
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(users[1], 'dragons'))
        String user = users[1]
        Map before = projectUserSnapshots(setup.destinations, users)
        Map memberships = projectMembershipSnapshots(setup.destinations, users)
        Map sources = users.collectEntries { String learner -> [(learner): snapshot(setup.source.projectId, learner)] }
        Integer sourceRefId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        Map sourceEvents = users.collectEntries { String learner -> [(learner): events(sourceRefId, learner)] }
        Integer nativeRefId = skillDefRepo.findByProjectIdAndSkillId(target.projectId, target.nativeSkill.skillId).id
        List<Map> nativeEvents = events(nativeRefId, user)
        Map globals = users.collectEntries { String learner -> [(learner): globalSnapshot(learner)] }
        Map original = before["${target.projectId}/${user}".toString()]
        List<String> nativeIds = [target.nativeSkill.skillId, target.nativeCopy.skillId]
        List<String> nativeAwardIds = nativeIds + [target.nativeGroup.skillId, target.nativeBadge.badgeId]
        List<String> importedIds = [target.imported.skillId] + target.importedCopies*.skillId
        Map<String, Integer> retainedLevels = [(null): 2, 'TestSubject1': 3, 'TestSubject2': 0,
                                               'TestSubject3': 5, 'TestSubject4': 0]
        List<Map> nativePoints = original.points.findAll { Map point -> point.skillId in nativeIds }
        List<Map> validAwards = original.awards.findAll { Map award ->
            award.skillId in nativeAwardIds ||
                    (award.level != null && award.level <= retainedLevels[award.skillId])
        }
        assert nativePoints.size() == 2 && nativePoints*.points == [100, 100]
        assert validAwards.findAll { Map award -> award.level == null }*.skillId.toSet() == nativeAwardIds.toSet()
        assert validAwards.find { Map award -> award.skillId == null && award.level == 2 }
        assert original.awards.findAll { Map award -> award.skillId in importedIds }.size() == 3
        assert original.awards.find { Map award -> award.skillId == target.importedGroup.skillId }
        assert original.awards.find { Map award -> award.skillId == target.badge.badgeId }
        assert memberships["${target.projectId}/${user}".toString()].status == BackfillStatus.COMPLETED
        assert skillsService.getSkillSummary(user, target.projectId).points == 500

        when:
        root.fixImportedSkillProgress(target.projectId, user)

        then:
        Map summary = skillsService.getSkillSummary(user, target.projectId)
        summary.points == 200
        summary.skillsLevel == 2
        target.subjects.eachWithIndex { SkillDef subject, int index ->
            Map subjectSummary = skillsService.getSkillSummary(user, target.projectId, subject.skillId)
            assert subjectSummary.points == [100, 0, 100, 0][index]
            assert subjectSummary.skillsLevel == [3, 0, 5, 0][index]
            assertLevels(target.projectId, user, subject.skillId, [3, 0, 5, 0][index])
        }
        assertLevels(target.projectId, user, null, 2)
        skillsService.getSkillsGroupSummary(target.projectId, target.nativeGroup.skillId, user).skillsAchieved == 1
        skillsService.getSkillsGroupSummary(target.projectId, target.importedGroup.skillId, user).skillsAchieved == 0
        skillsService.getBadgeSummary(user, target.projectId, target.nativeBadge.badgeId).badgeAchieved
        !skillsService.getBadgeSummary(user, target.projectId, target.badge.badgeId).badgeAchieved
        snapshot(target.projectId, user).points.findAll { Map point -> point.skillId in nativeIds } == nativePoints
        snapshot(target.projectId, user).awards == validAwards
        importedIds.every { String skillId ->
            !userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, user, skillId) &&
                    !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, target.projectId, skillId)
        }
        projectMembershipSnapshots(setup.destinations, users) == memberships
        before.findAll { String key, Map rows -> key != "${target.projectId}/${user}" }.every { String key, Map rows ->
            List<String> pair = key.split('/') as List<String>
            snapshot(pair[0], pair[1]) == rows
        }
        users.every { String learner -> snapshot(setup.source.projectId, learner) == sources[learner] &&
                events(sourceRefId, learner) == sourceEvents[learner] && globalSnapshot(learner) == globals[learner] }
        events(nativeRefId, user) == nativeEvents
        !backfillTasks()

        when:
        Map repaired = snapshot(target.projectId, user)
        root.fixImportedSkillProgress(target.projectId, user)

        then:
        snapshot(target.projectId, user) == repaired
        projectMembershipSnapshots(setup.destinations, users) == memberships
        users.every { String learner -> globalSnapshot(learner) == globals[learner] }
    }
}
