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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.springframework.test.annotation.DirtiesContext
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef
import skills.storage.model.UserPoints
import skills.storage.model.UserAchievement
import skills.storage.model.Setting

import static skills.intTests.utils.SkillsFactory.*

/** Temporary pre-5.1 cleanup/Fix endpoint acceptance; retire after remediation. */
// Close this context's scheduler before the dedicated IT context installs worker barriers.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationCleanupSpecs extends CatalogCleanupIntSpec {
    def "cleanup and Fix isolate two source projects and distinguish partial mixed group and badge requirements"() {
        List<String> users = getRandomUsers(4)
        Map setup = reuseFixture(['TestProject2': users.take(2), 'TestProject3': users.take(2)])
        Date earned = new Date().clearTime() - 7
        Map secondSource = createSkill(4, 1, 12, 0, 2, 0, 100)
        // The independent source also contains the first source's skill ID, but it is not imported.
        Map sameIdNative = createSkill(4, 1, 11, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(4), createSubject(4, 1), [secondSource, sameIdNative])
        skillsService.exportSkillToCatalog(secondSource.projectId, secondSource.skillId)
        setup.destinations.eachWithIndex { Map destination, int index ->
            skillsService.bulkImportSkillsIntoGroupFromCatalog(destination.projectId, 'TestSubject3', destination.nativeGroup.skillId,
                    [[projectId: secondSource.projectId, skillId: secondSource.skillId]])
            skillsService.updateSkill(createSkillsGroup(index + 2, 3, 31) + [numSkillsRequired: 2])
            skillsService.finalizeSkillsImportFromCatalog(destination.projectId)
            skillsService.assignSkillToBadge(destination.projectId, destination.nativeBadge.badgeId, secondSource.skillId)
        }
        [users[0], users[2], users[3]].each { String user -> skillsService.addSkill(setup.source, user, earned) }
        users.each { String user -> skillsService.addSkill(sameIdNative, user, earned) }
        [users[0], users[1], users[3]].each { String user -> skillsService.addSkill(secondSource, user, earned) }
        [users[0], users[3]].each { String user -> skillsService.addSkill(secondSource, user, earned + 1) }
        createService(users[2]).initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        SkillsService root = createRootSkillService()
        // Settle changed group/badge requirements before taking the preservation baseline.
        setup.destinations.each { Map destination -> users.take(2).each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }
        root.fixImportedSkillProgress('TestProject2', users[2])
        setup.destinations.eachWithIndex { Map destination, int index ->
            List<String> nonmembers = index == 0 ? [users[3]] : users.drop(2)
            SkillDef extraImport = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, secondSource.skillId)
            nonmembers.each { String user ->
                seedReusedProgress(destination, user, 0, 100, earned)
                seedPoints(destination.projectId, user, extraImport, 200, earned)
                seedAward(destination.projectId, user, extraImport, null, earned)
                seedAward(destination.projectId, user, destination.nativeGroup, null, earned)
                seedAward(destination.projectId, user, skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.nativeBadge.badgeId), null, earned)
            }
        }
        users.each { String user -> seedGlobal(user) }
        Map before = projectUserSnapshots(setup.destinations, users)
        Map memberships = projectMembershipSnapshots(setup.destinations, users)
        Map sources = [setup.source.projectId, secondSource.projectId].collectEntries { String projectId ->
            [(projectId): users.collectEntries { String user -> [(user): snapshot(projectId, user)] }]
        }
        List<SkillDef> sourceSkills = [setup.source, secondSource, sameIdNative].collect { Map skill -> skillDefRepo.findByProjectIdAndSkillId(skill.projectId, skill.skillId) }
        Map sourceEvents = sourceSkills.collectEntries { SkillDef skill ->
            [(skill.id): users.collectEntries { String user -> [(user): events(skill.id, user)] }]
        }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }

        when:
        Map cleaned = root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        cleaned.projects*.status == ['CLEANED', 'CLEANED']
        setup.destinations.eachWithIndex { Map destination, int index ->
            users.eachWithIndex { String user, int userIndex ->
                Map original = before["${destination.projectId}/${user}".toString()]
                boolean eligible = userIndex < 2 || (index == 0 && userIndex == 2)
                List<String> retainedIds = destination.importedCopies*.skillId
                assert snapshot(destination.projectId, user) == (eligible ? original :
                        [points: original.points.findAll { Map row -> row.skillId in retainedIds },
                         awards: original.awards.findAll { Map row -> row.skillId in retainedIds }])
            }
            assert createService(users[0]).getSkillsSummaryForCurrentUser(destination.projectId).points == 700
            assert createService(users[1]).getSkillsSummaryForCurrentUser(destination.projectId).points == 300
            assert createService(users[0]).getBadgeSummary(null, destination.projectId, destination.nativeBadge.badgeId).badgeAchieved
            assert !createService(users[1]).getBadgeSummary(null, destination.projectId, destination.nativeBadge.badgeId).badgeAchieved
            assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], destination.projectId, destination.nativeGroup.skillId).size() == 1
            assert !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[1], destination.projectId, destination.nativeGroup.skillId)
        }
        createService(users[2]).getSkillsSummaryForCurrentUser('TestProject2').points == 300
        createService(users[2]).getSkillsSummaryForCurrentUser('TestProject3').points == 0
        projectMembershipSnapshots(setup.destinations, users) == memberships
        sources.every { String projectId, Map rows -> users.every { String user -> snapshot(projectId, user) == rows[user] } }
        sourceSkills.every { SkillDef skill -> users.every { String user -> events(skill.id, user) == sourceEvents[skill.id][user] } }
        users.every { String user -> globalSnapshot(user) == globals[user] }

        when:
        Map target = setup.destinations[0]
        String partialUser = users[1]
        Map isolated = projectUserSnapshots(setup.destinations, users)
        Map valid = snapshot(target.projectId, partialUser)
        SkillDef partialImport = skillDefRepo.findByProjectIdAndSkillId(target.projectId, secondSource.skillId)
        ([target.imported] + target.importedCopies).each { SkillDef skill ->
            seedPoints(target.projectId, partialUser, skill, 100, earned)
            seedAward(target.projectId, partialUser, skill, null, earned)
        }
        [target.nativeGroup, target.importedGroup].each { SkillDef group ->
            seedPoints(target.projectId, partialUser, group, 999, earned)
            seedAward(target.projectId, partialUser, group, null, earned)
        }
        [target.nativeBadge, target.badge].each { Map badge ->
            seedAward(target.projectId, partialUser, skillDefRepo.findByProjectIdAndSkillId(target.projectId, badge.badgeId), null, earned)
        }
        target.subjects.each { SkillDef subject -> seedPoints(target.projectId, partialUser, subject, 999, earned) }
        seedPoints(target.projectId, partialUser, null, 999, earned)
        seedPoints(target.projectId, partialUser, partialImport, 999, earned)
        seedAward(target.projectId, partialUser, partialImport, null, earned)
        seedLevels(target.projectId, partialUser, null, (1..5).toList())
        target.subjects.each { SkillDef subject -> seedLevels(target.projectId, partialUser, subject, (1..5).toList()) }
        root.fixImportedSkillProgress(target.projectId, partialUser)

        then:
        SkillsService learner = createService(partialUser)
        learner.getSkillsSummaryForCurrentUser(target.projectId).points == 300
        learner.getSkillsSummaryForCurrentUser(target.projectId).skillsLevel == 2
        target.subjects.eachWithIndex { SkillDef subject, int index ->
            assert learner.getSubjectSummaryForCurrentUser(target.projectId, subject.skillId).points == [100, 0, 200, 0][index]
            assert userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, partialUser, subject.skillId).points == [100, 0, 200, 0][index]
            assertLevels(target.projectId, partialUser, subject.skillId, [3, 0, 3, 0][index])
        }
        assertLevels(target.projectId, partialUser, null, 2)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, partialUser, null).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, partialUser, target.nativeGroup.skillId).points == 200
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, partialUser, target.importedGroup.skillId).points == 0
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, partialUser, partialImport.skillId).points == 100
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(partialUser, target.projectId, partialImport.skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(partialUser, target.projectId, target.nativeGroup.skillId)
        !learner.getBadgeSummary(null, target.projectId, target.nativeBadge.badgeId).badgeAchieved
        !learner.getBadgeSummary(null, target.projectId, target.badge.badgeId).badgeAchieved
        snapshot(target.projectId, partialUser).awards.findAll { Map row -> row.id in valid.awards*.id } == valid.awards
        List<String> nativeIds = [target.nativeSkill.skillId, target.nativeCopy.skillId]
        snapshot(target.projectId, partialUser).points.findAll { Map row -> row.skillId in nativeIds } == valid.points.findAll { Map row -> row.skillId in nativeIds }
        isolated.findAll { String key, Map rows -> key != "${target.projectId}/${partialUser}" }.every { String key, Map rows ->
            List<String> pair = key.split('/') as List<String>
            snapshot(pair[0], pair[1]) == rows
        }
        projectMembershipSnapshots(setup.destinations, users) == memberships
        sources.every { String projectId, Map rows -> users.every { String user -> snapshot(projectId, user) == rows[user] } }
        sourceSkills.every { SkillDef skill -> users.every { String user -> events(skill.id, user) == sourceEvents[skill.id][user] } }
        users.every { String user -> globalSnapshot(user) == globals[user] }

        when:
        Map repaired = snapshot(target.projectId, partialUser)
        root.fixImportedSkillProgress(target.projectId, partialUser)

        then:
        snapshot(target.projectId, partialUser) == repaired
        root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId]).deletedRecords == 0
    }

    def "cleanup and Fix neither grant an absent qualifying global badge nor mutate an existing award"() {
        List<String> users = getRandomUsers(2)
        Map setup = reuseFixture(['TestProject2': users, 'TestProject3': users])
        Date earned = new Date().clearTime() - 7
        users.each { String user -> skillsService.addSkill(setup.source, user, earned) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map target = setup.destinations[0]
        SkillsService root = createRootSkillService()
        // Native reuse predates the catalog reports; settle its missing aggregate rows before
        // asserting that a subsequent no-op Fix leaves the existing-award learner unchanged.
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }
        userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectId(users[0], null))
        seedGlobal(users[1])
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        assert globals[users[0]] == []
        assert globals[users[1]].size() == 1
        Map before = projectUserSnapshots(setup.destinations, users)
        Map memberships = projectMembershipSnapshots(setup.destinations, users)

        when:
        root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        projectUserSnapshots(setup.destinations, users) == before
        users.every { String user -> globalSnapshot(user) == globals[user] }

        when:
        userPointsRepo.delete(userPointsRepo.findByProjectIdAndUserIdAndSkillId(target.projectId, users[0], target.imported.skillId))
        userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], target.projectId, target.imported.skillId))
        userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], target.projectId, null))
        seedPoints(target.projectId, users[0], null, 17)
        users.each { String user -> root.fixImportedSkillProgress(target.projectId, user) }

        then:
        assertDisplayedProgress(target, users[0], [200, 100, 100, 100], 500, [5, 5, 5, 5], 5, true, true)
        // The source skill and repaired destination level satisfy both global requirements.
        createService(users[0]).getSkillsSummaryForCurrentUser(setup.source.projectId).points == 100
        users.every { String user -> globalSnapshot(user) == globals[user] }
        projectMembershipSnapshots(setup.destinations, users) == memberships
        snapshot(target.projectId, users[1]) == before["${target.projectId}/${users[1]}".toString()]
        users.every { String user -> snapshot('TestProject3', user) == before["TestProject3/${user}".toString()] }
        !backfillTasks()
    }

    def "cleanup scopes local and explicit membership to each destination while retaining reused credit and all derived achievements"() {
        List<String> users = getRandomUsers(10)
        Map setup = reuseFixture(['TestProject2': [users[0], users[8]], 'TestProject3': [users[1]]], 2)
        Date earned = new Date().clearTime() - 5
        users.findAll { String user -> user != users[8] }.each { String user -> skillsService.addSkill(setup.source, user, earned) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map<String, List<String>> eligible = ['TestProject2': [users[0], users[2]] + users[4..8],
                                             'TestProject3': [users[1], users[3]] + users[4..7]]
        setup.destinations.eachWithIndex { Map destination, int index ->
            participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: users[index + 2],
                    backfillStatus: BackfillStatus.COMPLETED))
            BackfillStatus.values().eachWithIndex { BackfillStatus status, int offset ->
                participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: users[offset + 4], backfillStatus: status))
            }
            users.each { String user ->
                if (user in eligible[destination.projectId]) {
                    int nativePoints = user in [users[index], users[8]] && (index == 0 || user != users[8]) ? 100 : 0
                    seedReusedProgress(destination, user, nativePoints, user == users[8] ? 0 : 100, earned)
                } else {
                    seedDerivedResidue(destination, user)
                    seedPoints(destination.projectId, user, destination.imported, 100, earned)
                    seedAward(destination.projectId, user, destination.imported, null, earned)
                }
            }
            // Native-only evidence is incomplete and has no explicit membership or skill achievement.
            assert !membership(destination.projectId, users[index])
        }
        assert !membership('TestProject2', users[8])
        assert !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[8], 'TestProject2', setup.destinations[0].nativeSkill.skillId)
        assert !userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', users[8], setup.source.skillId)
        Map before = projectUserSnapshots(setup.destinations, users)
        Map memberships = projectMembershipSnapshots(setup.destinations, users)
        Map sourceBefore = users.collectEntries { String user -> [(user): snapshot(setup.source.projectId, user)] }
        Integer sourceId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        Map sourceEvents = users.collectEntries { String user -> [(user): events(sourceId, user)] }
        users.each { String user -> seedGlobal(user) }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        Map available = setup.destinations.collectEntries { Map destination -> [(destination.projectId): projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints] }
        SkillsService root = createRootSkillService()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        result.projects*.status == ['CLEANED', 'CLEANED']
        result.deletedPointRows == 56
        result.deletedAchievementRows == 210
        result.deletedRecords == 266
        setup.destinations.every { Map destination ->
            users.every { String user ->
                String key = "${destination.projectId}/${user}"
                snapshot(destination.projectId, user) == (user in eligible[destination.projectId] ? before[key] : [points: [], awards: []]) &&
                        membership(destination.projectId, user) == memberships[key]
            } && projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints == available[destination.projectId]
        }
        users.every { String user -> snapshot(setup.source.projectId, user) == sourceBefore[user] &&
                events(sourceId, user) == sourceEvents[user] && globalSnapshot(user) == globals[user] }
        setup.destinations.eachWithIndex { Map destination, int index ->
            assertDisplayedProgress(destination, users[index], [200, 100, 100, 100], 500, [3, 5, 3, 5], 4, false, true)
            assertDisplayedProgress(destination, users[index + 2], [100, 100, 0, 100], 300, [2, 5, 0, 5], 2, false, true)
            assertDisplayedProgress(destination, users[1 - index], [0, 0, 0, 0], 0, [0, 0, 0, 0], 0, false, false)
            assertDisplayedProgress(destination, users[3 - index], [0, 0, 0, 0], 0, [0, 0, 0, 0], 0, false, false)
        }
        createService(users[8]).getSkillsSummaryForCurrentUser('TestProject2').points == 200
        root.cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId]).deletedRecords == 0
        projectMembershipSnapshots(setup.destinations, users) == memberships
    }

    def "cleanup preserves native and same-project reuse points and achievements even without membership protection"() {
        Map setup = reuseFixture()
        List<String> users = getRandomUsers(2)
        setup.destinations.each { Map destination ->
            participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: users[0], backfillStatus: BackfillStatus.PENDING))
            users.each { String user -> seedReusedProgress(destination, user, 100, 100, new Date() - 5) }
        }
        users.each { String user -> skillsService.addSkill(setup.source, user); seedGlobal(user) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map before = projectUserSnapshots(setup.destinations, users)
        Map sources = users.collectEntries { String user -> [(user): snapshot(setup.source.projectId, user)] }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        Map memberships = projectMembershipSnapshots(setup.destinations, users)
        Map retained = setup.destinations.collectEntries { Map destination ->
            List<String> ids = [destination.nativeSkill.skillId, destination.nativeCopy.skillId] + destination.importedCopies*.skillId
            Map original = before["${destination.projectId}/${users[1]}".toString()]
            [(destination.projectId): [points: original.points.findAll { Map row -> row.skillId in ids },
                                       awards: original.awards.findAll { Map row -> row.skillId in ids }]]
        }
        assert retained.values().every { Map rows -> rows.points.size() == 4 && rows.awards.size() == 4 }

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        result.projects*.status == ['CLEANED', 'CLEANED']
        setup.destinations.every { Map destination -> snapshot(destination.projectId, users[0]) == before["${destination.projectId}/${users[0]}".toString()] &&
                snapshot(destination.projectId, users[1]) == retained[destination.projectId] }
        result.deletedPointRows == before.findAll { String key, Map rows -> key.endsWith("/${users[1]}") }.values().sum { Map rows -> rows.points.size() - 4 }
        result.deletedAchievementRows == before.findAll { String key, Map rows -> key.endsWith("/${users[1]}") }.values().sum { Map rows -> rows.awards.size() - 4 }
        projectMembershipSnapshots(setup.destinations, users) == memberships
        users.every { String user -> snapshot(setup.source.projectId, user) == sources[user] && globalSnapshot(user) == globals[user] }
        setup.destinations.each { Map destination ->
            SkillsService learner = createService(users[1])
            assert learner.getSkillsSummaryForCurrentUser(destination.projectId).points == 0
            // Drilldowns still expose protected native/reused skill credit; the invalid stored
            // subject/project aggregates are gone. Do not turn cleanup into a native-credit filter.
            destination.subjects.eachWithIndex { SkillDef subject, int index ->
                assert learner.getSubjectSummaryForCurrentUser(destination.projectId, subject.skillId).points == [200, 0, 100, 100][index]
            }
            [destination.nativeGroup, destination.importedGroup].each { SkillDef group ->
                assert !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[1], destination.projectId, group.skillId)
            }
            assert !learner.getBadgeSummary(null, destination.projectId, destination.badge.badgeId).badgeAchieved
            assert !learner.getBadgeSummary(null, destination.projectId, destination.nativeBadge.badgeId).badgeAchieved
        }
    }

    def "cleanup removes derived-only tails and disabled imported residue without requiring an enabled imported credit row"() {
        Map setup = reuseFixture()
        Map extraSource = createSkill(1, 1, 12, 0, 1, 0, 100)
        skillsService.createSkill(extraSource)
        skillsService.exportSkillToCatalog(extraSource.projectId, extraSource.skillId)
        List<String> users = getRandomUsers(3)
        setup.destinations.each { Map destination ->
            skillsService.importSkillFromCatalog(destination.projectId, 'TestSubject2', extraSource.projectId, extraSource.skillId)
            skillsService.finalizeSkillsImportFromCatalog(destination.projectId)
            SkillDef disabledImport = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, extraSource.skillId)
            // Historical definition state, while another cross-project import remains enabled.
            disabledImport.enabled = 'false'
            skillDefRepo.save(disabledImport)
            participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: users[0], backfillStatus: BackfillStatus.NOT_REQUESTED))
            users.each { String user -> seedDerivedResidue(destination, user) }
            [users[0], users[2]].each { String user ->
                seedPoints(destination.projectId, user, disabledImport, 100)
                seedAward(destination.projectId, user, disabledImport)
            }
            assert users.every { String user -> !userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId) &&
                    !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId) }
        }
        Map before = projectUserSnapshots(setup.destinations, users)
        Map memberships = projectMembershipSnapshots(setup.destinations, users)

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress([projectIds: setup.destinations*.projectId])

        then:
        result.projects*.status == ['CLEANED', 'CLEANED']
        result.deletedPointRows == 30
        result.deletedAchievementRows == 118
        result.deletedRecords == 148
        setup.destinations.every { Map destination -> snapshot(destination.projectId, users[0]) == before["${destination.projectId}/${users[0]}".toString()] &&
                users.drop(1).every { String user -> snapshot(destination.projectId, user) == [points: [], awards: []] } }
        projectMembershipSnapshots(setup.destinations, users) == memberships
        setup.destinations.each { Map destination ->
            users.drop(1).each { String user -> assertDisplayedProgress(destination, user, [0, 0, 0, 0], 0, [0, 0, 0, 0], 0, false, false) }
        }
    }

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

    def "default cleanup has no projects to process when there are no enabled catalog imports"() {
        Map nativeSkill = createSkill(1, 1, 11, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [nativeSkill])
        String user = getRandomUsers(1)[0]
        skillsService.addSkill(nativeSkill, user)
        Map before = snapshot(nativeSkill.projectId, user)

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress()

        then:
        result.projects == []
        result.deletedRecords == 0
        result.deletedPointRows == 0
        result.deletedAchievementRows == 0
        snapshot(nativeSkill.projectId, user) == before
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

    def "Fix enforces current community restrictions while preserving former-member participation and global achievements"() {
        Map setup = fixture(1)
        List<String> users = getRandomUsers(2)
        SkillsService root = createRootSkillService()
        root.saveUserTag(root.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(users[0], 'dragons', ['DivineDragon'])
        skillsService.reuseSkillInAnotherSubject('TestProject2', setup.source.skillId, 'TestSubject1')
        users.each { String user -> skillsService.addSkill(setup.source, user); createService(user).initializeProjectProgress('TestProject2') }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        users.each { String user -> seedGlobal(user) }
        Map memberships = users.collectEntries { String user -> [(user): membership('TestProject2', user)] }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        // Scoped historical policy fixture: emulate credit earned before community restrictions changed.
        settingRepo.save(new Setting(projectId: 'TestProject2', type: Setting.SettingType.Project,
                setting: 'user_community', value: 'true'))

        when:
        users.each { String user -> root.fixImportedSkillProgress('TestProject2', user) }

        then:
        skillsService.getSkillSummary(users[0], 'TestProject2').points == 200
        skillsService.getSkillSummary(users[1], 'TestProject2').points == 0
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', users[1], setup.source.skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[1], 'TestProject2', setup.source.skillId)
        users.every { String user -> membership('TestProject2', user) == memberships[user] && globalSnapshot(user) == globals[user] &&
                skillsService.getSkillSummary(user, setup.source.projectId).points == 100 }
    }

    def "malformed top-level selection is rejected with structured validation"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        seedResidue(setup.destinations[0], user)
        Map before = snapshot('TestProject2', user)

        when:
        createRootSkillService().cleanupImportedSkillProgress(['TestProject2'])

        then:
        SkillsClientException failure = thrown()
        failure.message.contains('BadParam')
        failure.message.contains('JSON object')
        snapshot('TestProject2', user) == before
    }

    def "Fix reconciles positive and empty source state for every participation status without changing membership"() {
        Map setup = fixture()
        List<String> users = getRandomUsers(8)
        Date earned = new Date().clearTime() - 7
        users.take(4).each { String user -> skillsService.addSkill(setup.source, user, earned) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup.destinations.each { Map destination ->
            users.eachWithIndex { String user, int index ->
                participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: user,
                        backfillStatus: BackfillStatus.values()[index % 4]))
                seedResidue(destination, user)
                UserPoints credit = userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)
                credit.points = 999
                userPointsRepo.save(credit)
                if (index < 4) {
                    userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, destination.badge.badgeId))
                }
                seedGlobal(user)
            }
        }
        Map memberships = [:]
        setup.destinations.each { Map destination -> users.each { String user -> memberships["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user) } }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        SkillsService root = createRootSkillService()

        when:
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }

        then:
        setup.destinations.every { Map destination -> users.every { String user ->
            int expectedPoints = user in users.take(4) ? 100 : 0
            skillsService.getSkillSummary(user, destination.projectId).points == expectedPoints &&
                    membership(destination.projectId, user) == memberships["${destination.projectId}/${user}".toString()] &&
                    (userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)?.points ?: 0) == expectedPoints &&
                    userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId).size() == (expectedPoints ? 1 : 0) &&
                    userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, destination.badge.badgeId).size() == (expectedPoints ? 1 : 0) &&
                    (!expectedPoints || userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, destination.badge.badgeId).first().achievedOn == earned)
        } }
        users.every { String user -> globalSnapshot(user) == globals[user] }
        !backfillTasks()
    }

    def "group points and awards and inactive local badges are removed with exact row preservation"() {
        Map setup = fixture(1)
        Map destination = setup.destinations[0]
        Map source = createSkill(1, 1, 12, 0, 1, 0, 100)
        skillsService.createSkill(source)
        skillsService.exportSkillToCatalog(source.projectId, source.skillId)
        Map group = createSkillsGroup(2, 2, 30)
        skillsService.createSkill(group)
        skillsService.bulkImportSkillsIntoGroupFromCatalog('TestProject2', 'TestSubject2', group.skillId, [[projectId: source.projectId, skillId: source.skillId]])
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        List<String> users = getRandomUsers(2)
        participationRepo.save(new ProjectParticipation(projectId: 'TestProject2', userId: users[0], backfillStatus: BackfillStatus.PENDING))
        SkillDef imported = skillDefRepo.findByProjectIdAndSkillId('TestProject2', source.skillId)
        SkillDef groupDef = skillDefRepo.findByProjectIdAndSkillId('TestProject2', group.skillId)
        SkillDef badge = skillDefRepo.findByProjectIdAndSkillId('TestProject2', destination.badge.badgeId)
        badge.enabled = 'false'
        badge.startDate = new Date() - 10
        badge.endDate = new Date() - 5
        skillDefRepo.save(badge)
        users.each { String user ->
            seedResidue(destination, user)
            [imported, groupDef].each { SkillDef definition ->
                Date date = new Date() - 3
                userPointsRepo.save(new UserPoints(projectId: 'TestProject2', userId: user, skillId: definition.skillId,
                        skillRefId: definition.id, points: 100, created: date, updated: date))
                userAchievedRepo.save(new UserAchievement(projectId: 'TestProject2', userId: user, skillId: definition.skillId,
                        skillRefId: definition.id, achievedOn: date, pointsWhenAchieved: 100))
            }
        }
        Map protectedRows = snapshot('TestProject2', users[0])

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress()

        then:
        result.deletedRecords == 11
        result.projects*.projectId == ['TestProject2']
        result.deletedPointRows == 5
        result.deletedAchievementRows == 6
        snapshot('TestProject2', users[1]) == [points: [], awards: []]
        snapshot('TestProject2', users[0]) == protectedRows
    }

    def "no-import pending-only and reuse-only projects are skipped without touching residue"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        SkillsService root = createRootSkillService()
        List<Map> nativeSkills = (3..5).collect { int number -> createSkill(number, 1, 21, 0, 1, 0, 100) }
        nativeSkills.eachWithIndex { Map skill, int index ->
            skillsService.createProjectAndSubjectAndSkills(createProject(index + 3), createSubject(index + 3, 1), [skill])
            skillsService.addSkill(skill, user)
        }
        skillsService.importSkillFromCatalog('TestProject4', 'TestSubject1', setup.source.projectId, setup.source.skillId)
        skillsService.createSubject(createSubject(5, 2))
        skillsService.reuseSkillInAnotherSubject('TestProject5', nativeSkills[2].skillId, 'TestSubject2')
        Map before = (3..5).collectEntries { int number -> [("TestProject${number}".toString()): snapshot("TestProject${number}", user)] }
        ListAppender<ILoggingEvent> logs = captureCleanupLogs()

        when:
        Map automatic = root.cleanupImportedSkillProgress()
        Map result = root.cleanupImportedSkillProgress([projectIds: ['TestProject3', 'TestProject4', 'TestProject5']])

        then:
        automatic.projects*.projectId == ['TestProject2']
        automatic.projects[0].status == 'SKIPPED'
        automatic.projects[0].reason == 'No removable destination credit'
        result.projects*.status == ['SKIPPED', 'SKIPPED', 'SKIPPED']
        result.projects.every { it.reason == 'No enabled cross-project catalog imports' }
        before.every { String project, Map rows -> snapshot(project, user) == rows }
        cleanupOutcomeLogs(logs).size() == 4
        (2..5).every { int number ->
            List<ILoggingEvent> projectLogs = cleanupOutcomeLogs(logs).findAll { ILoggingEvent event -> event.formattedMessage.contains("projectId=[TestProject${number}]") }
            projectLogs.size() == 1 && projectLogs[0].level == Level.WARN &&
                    projectLogs[0].formattedMessage.contains('status=[SKIPPED]') &&
                    projectLogs[0].formattedMessage.contains(number == 2 ? 'reason=[No removable destination credit]' : 'reason=[No enabled cross-project catalog imports]')
        }
    }

    def "old disabled local evidence protects all credit but native residue alone does not enroll a user"() {
        List<String> users = getRandomUsers(2)
        Map setup = fixture(2, [users[0]])
        setup.destinations.each { Map destination ->
            SkillDef local = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.nativeSkill.skillId)
            local.enabled = 'false'
            skillDefRepo.save(local)
            seedResidue(destination, users[0], true, false)
            seedResidue(destination, users[1])
            Date date = new Date() - 30
            // Native credit without local events is protected credit, not membership evidence.
            userPointsRepo.save(new UserPoints(projectId: destination.projectId, userId: users[1], skillId: local.skillId,
                    skillRefId: local.id, points: 40, created: date, updated: date))
        }
        Map protectedRows = setup.destinations.collectEntries { Map destination -> [(destination.projectId): snapshot(destination.projectId, users[0])] }
        Map nativeRows = setup.destinations.collectEntries { Map destination -> [(destination.projectId): snapshot(destination.projectId, users[1]).points.findAll { it.skillId == destination.nativeSkill.skillId }] }

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress()

        then:
        result.deletedRecords == 14
        setup.destinations.every { Map destination ->
            snapshot(destination.projectId, users[0]) == protectedRows[destination.projectId] &&
                    snapshot(destination.projectId, users[1]) == [points: nativeRows[destination.projectId], awards: []] &&
                    !membership(destination.projectId, users[0]) && !membership(destination.projectId, users[1])
        }
    }

    def "later qualification backfills cleaned users while ordinary repeat initialization does not repair"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        skillsService.addSkill(setup.source, user)
        seedResidue(setup.destinations[0], user)
        seedGlobal(user)
        SkillsService root = createRootSkillService()
        SkillsService learner = createService(user)
        List<Map> global = globalSnapshot(user)

        when:
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2']])
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        skillsService.getSkillSummary(user, 'TestProject2').points == 100
        membership('TestProject2', user).status == BackfillStatus.COMPLETED

        when:
        userPointsRepo.delete(userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId))
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId)
        !backfillTasks()

        when:
        Map participant = membership('TestProject2', user)
        root.fixImportedSkillProgress('TestProject2', user)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId).points == 100
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global
    }

    def "multi-user multi-project cleanup preserves local and explicit members and deletes only destination residue"() {
        List<String> users = getRandomUsers(9)
        Map setup = fixture(2, users.take(2))
        SkillsService root = createRootSkillService()
        users.each { String user -> skillsService.addSkill(setup.source, user); seedGlobal(user) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup.destinations.each { Map destination ->
            assert !membership(destination.projectId, users[0])
            assert !membership(destination.projectId, users[1])
            // Local-event-only users have no explicit row; imports are deliberately in another subject.
            users.take(2).each { String user ->
                if (!userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)) {
                    seedResidue(destination, user, true, false)
                }
            }
            BackfillStatus.values().eachWithIndex { BackfillStatus status, int index ->
                participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: users[index + 2], backfillStatus: status))
                seedResidue(destination, users[index + 2])
            }
            seedResidue(destination, users[6])
            seedResidue(destination, users[7], false, false)
            seedResidue(destination, users[8], true, false)
            skillsService.archiveUsers([users[5], users[6]], destination.projectId)
        }
        Map protectedRows = [:]
        Map memberships = [:]
        Map archives = [:]
        Map sources = [:]
        Map sourceEvents = [:]
        Map globals = [:]
        setup.destinations.each { Map destination ->
            [users[5], users[6]].each { String user -> archives["${destination.projectId}/${user}".toString()] = archiveSnapshot(destination.projectId, user) }
            users.take(6).each { String user ->
                protectedRows["${destination.projectId}/${user}".toString()] = snapshot(destination.projectId, user)
                memberships["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user)
            }
        }
        Integer sourceRefId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        users.each { String user ->
            sources[user] = snapshot(setup.source.projectId, user)
            sourceEvents[user] = events(sourceRefId, user)
            globals[user] = globalSnapshot(user)
        }
        Map available = setup.destinations.collectEntries { Map destination -> [(destination.projectId): projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints] }

        when:
        Map result = root.cleanupImportedSkillProgress()

        then:
        result.projects.findAll { it.status == 'CLEANED' }*.projectId == setup.destinations*.projectId
        result.deletedRecords == 30
        result.deletedPointRows == 10
        result.deletedAchievementRows == 20
        setup.destinations.every { Map destination ->
            users.drop(6).every { String user -> snapshot(destination.projectId, user) == [points: [], awards: []] } &&
                    users.take(6).every { String user ->
                        snapshot(destination.projectId, user) == protectedRows["${destination.projectId}/${user}".toString()] &&
                                membership(destination.projectId, user) == memberships["${destination.projectId}/${user}".toString()]
                    } && projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints == available[destination.projectId]
        }
        users.every { String user -> snapshot(setup.source.projectId, user) == sources[user] &&
                events(sourceRefId, user) == sourceEvents[user] && globalSnapshot(user) == globals[user] }
        setup.destinations.every { Map destination ->
            skillsService.getSkillSummary(users[6], destination.projectId).points == 0 &&
                    skillsService.getSkillSummary(users[0], destination.projectId).points == 200
        }
        setup.destinations.every { Map destination ->
            [users[5], users[6]].every { String user -> archiveSnapshot(destination.projectId, user) == archives["${destination.projectId}/${user}".toString()] } &&
                    skillsService.getProjectUsers(destination.projectId).data*.userId.toSet() == users.take(5).toSet() &&
                    skillsService.getLeaderboard(users[0], destination.projectId).rankedUsers*.userId.toSet() == users.take(5).collect { String user -> userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay }.toSet() &&
                    exportedUsers(destination.projectId).toSet() == users.take(5).collect { String user -> userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay }.toSet()
        }
        root.cleanupImportedSkillProgress([:]).deletedRecords == 0
        // Archive records survived: restoring an event-free protected learner still finds their row.
        skillsService.restoreArchivedUser(users[5], setup.destinations[0].projectId)
        skillsService.getProjectUsers(setup.destinations[0].projectId).data*.userId.contains(users[5])
    }

    def "selection is canonical deduplicated and validated entirely before any commit"() {
        Map setup = fixture()
        String user = getRandomUsers(1)[0]
        SkillsService root = createRootSkillService()
        setup.destinations.each { Map destination -> seedResidue(destination, user) }
        Map unselected = snapshot('TestProject3', user)

        when:
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2', 'unknown']])

        then:
        SkillsClientException invalid = thrown()
        invalid.message.contains('Unknown project')
        snapshot('TestProject2', user).points.size() == 3
        snapshot('TestProject3', user) == unselected

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: [' testproject2 ', 'TESTPROJECT2']])

        then:
        result.projects*.projectId == ['TestProject2']
        result.deletedRecords == 7
        snapshot('TestProject3', user) == unselected
    }

    def "invalid selection #selection is a structured BadParam with no deletion"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        seedResidue(setup.destinations[0], user)
        Map before = snapshot('TestProject2', user)

        when:
        createRootSkillService().cleanupImportedSkillProgress([projectIds: selection])

        then:
        SkillsClientException failure = thrown()
        failure.message.contains('BadParam')
        snapshot('TestProject2', user) == before

        where:
        selection << [null, [], 'TestProject2', [null], [' '], [123]]
    }

    def "cleanup skips observed #state and targeted retry succeeds after recovery"() {
        Map setup = fixture()
        String user = getRandomUsers(1)[0]
        setup.destinations.each { Map destination -> seedResidue(destination, user) }
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: state])
        Map before = snapshot('TestProject2', user)
        SkillsService root = createRootSkillService()
        ListAppender<ILoggingEvent> logs = captureCleanupLogs()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: ['TestProject2', 'TestProject3']])

        then:
        result.projects*.status == ['SKIPPED', 'CLEANED']
        result.projects[0].reason.contains('RUNNING or FAILED')
        snapshot('TestProject2', user) == before
        cleanupOutcomeLogs(logs).size() == 2
        cleanupOutcomeLogs(logs).find { ILoggingEvent event -> event.level == Level.WARN }.formattedMessage.contains('projectId=[TestProject2] status=[SKIPPED] reason=[Catalog finalization is RUNNING or FAILED; retry after recovery]')
        cleanupOutcomeLogs(logs).find { ILoggingEvent event -> event.level == Level.INFO }.formattedMessage.contains('projectId=[TestProject3] status=[CLEANED]')

        when:
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: 'COMPLETED'])

        then:
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2']]).deletedRecords == 7

        where:
        state << ['RUNNING', 'FAILED']
    }

    def "set-based cleanup removes achievement-only residue for more than one hundred users"() {
        Map setup = fixture(1)
        List<String> users = getRandomUsers(105)
        users.each { String user -> seedResidue(setup.destinations[0], user, false, false) }
        SkillsService root = createRootSkillService()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: ['TestProject2']])

        then:
        result.deletedRecords == 315
        result.deletedAchievementRows == 315
        users.every { String user -> snapshot('TestProject2', user) == [points: [], awards: []] }
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2']]).deletedAchievementRows == 0
    }

    def "root Fix restores missing and stale credit regardless of status without membership or global side effects"() {
        List<String> users = getRandomUsers(2)
        Map setup = fixture(2, [users[1]])
        Date earned = new Date().clearTime() - 7
        users.each { String user -> skillsService.addSkill(setup.source, user, earned); seedGlobal(user) }
        setup.destinations.each { Map destination -> createService(users[0]).initializeProjectProgress(destination.projectId) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        SkillsService root = createRootSkillService()
        Map memberships = [:]
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        Map nativeCredit = [:]
        setup.destinations.each { Map destination ->
            users.each { String user ->
                memberships["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user)
                UserPoints credit = userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)
                if (credit) { userPointsRepo.delete(credit) }
                userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId))
            }
            nativeCredit[destination.projectId] = snapshot(destination.projectId, users[1]).points.findAll { it.skillId == destination.nativeSkill.skillId }
            UserPoints total = userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, users[0], null)
            total.points = 17
            userPointsRepo.save(total)
        }

        when:
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }

        then:
        setup.destinations.every { Map destination ->
            skillsService.getSkillSummary(users[0], destination.projectId).points == 100 &&
                    skillsService.getSkillSummary(users[1], destination.projectId).points == 200 &&
                    users.every { String user ->
                                membership(destination.projectId, user) == memberships["${destination.projectId}/${user}".toString()] &&
                                userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId).points == 100 &&
                                userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId).size() == 1 &&
                                userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId).first().achievedOn == earned
                    } && snapshot(destination.projectId, users[1]).points.findAll { it.skillId == destination.nativeSkill.skillId } == nativeCredit[destination.projectId]
        }
        users.every { String user -> globalSnapshot(user) == globals[user] }
        !membership('TestProject2', users[1])

        when:
        Map repaired = [:]
        setup.destinations.each { Map destination -> users.each { String user -> repaired["${destination.projectId}/${user}".toString()] = snapshot(destination.projectId, user) } }
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }

        then:
        users.every { String user -> globalSnapshot(user) == globals[user] }
        skillsService.getSkillSummary(users[0], 'TestProject2').points == 100
        skillsService.getSkillSummary(users[1], 'TestProject3').points == 200
        setup.destinations.every { Map destination -> users.every { String user -> snapshot(destination.projectId, user) == repaired["${destination.projectId}/${user}".toString()] } }
    }

    def "project admin cannot cleanup or Fix and root cannot enroll an ineligible user through Fix"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        seedResidue(setup.destinations[0], user)
        Map before = snapshot('TestProject2', user)

        when:
        skillsService.cleanupImportedSkillProgress()

        then:
        SkillsClientException cleanupFailure = thrown()
        cleanupFailure.httpStatus.value() == 403
        snapshot('TestProject2', user) == before

        when:
        skillsService.fixImportedSkillProgress('TestProject2', user)

        then:
        SkillsClientException fixFailure = thrown()
        fixFailure.httpStatus.value() == 403
        snapshot('TestProject2', user) == before

        when:
        createRootSkillService().fixImportedSkillProgress('TestProject2', user)

        then:
        SkillsClientException eligibilityFailure = thrown()
        eligibilityFailure.message.contains('does not enroll')
        !membership('TestProject2', user)
        snapshot('TestProject2', user) == before
    }

    def "Fix rejects #state without touching participation credit global awards or tasks"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        createService(user).initializeProjectProgress('TestProject2')
        seedGlobal(user)
        Map before = snapshot('TestProject2', user)
        Map participant = membership('TestProject2', user)
        List<Map> global = globalSnapshot(user)
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: state])

        when:
        createRootSkillService().fixImportedSkillProgress('TestProject2', user)

        then:
        SkillsClientException failure = thrown()
        failure.message.contains('retry Fix')
        snapshot('TestProject2', user) == before
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global
        !backfillTasks()

        where:
        state << ['RUNNING', 'FAILED']
    }

    private Map projectUserSnapshots(List<Map> destinations, List<String> users) {
        Map result = [:]
        destinations.each { Map destination -> users.each { String user -> result["${destination.projectId}/${user}".toString()] = snapshot(destination.projectId, user) } }
        result
    }

    private Map projectMembershipSnapshots(List<Map> destinations, List<String> users) {
        Map result = [:]
        destinations.each { Map destination -> users.each { String user -> result["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user) } }
        result
    }

    private void seedDerivedResidue(Map destination, String user) {
        destination.subjects.each { SkillDef subject ->
            seedPoints(destination.projectId, user, subject, 100)
            seedLevels(destination.projectId, user, subject, (1..5).toList())
        }
        seedPoints(destination.projectId, user, null, 400)
        seedLevels(destination.projectId, user, null, (1..5).toList())
        [destination.nativeGroup, destination.importedGroup].each { SkillDef group ->
            seedPoints(destination.projectId, user, group, 100)
            seedAward(destination.projectId, user, group)
        }
        [destination.badge, destination.nativeBadge].each { Map badge ->
            seedAward(destination.projectId, user, skillDefRepo.findByProjectIdAndSkillId(destination.projectId, badge.badgeId))
        }
    }

    private List<String> exportedUsers(String projectId) {
        SkillsService.FileAndHeaders exported = skillsService.getUserProgressExcelExport(projectId)
        Workbook workbook = WorkbookFactory.create(exported.file)
        try {
            Sheet sheet = workbook.getSheetAt(0)
            (2..<sheet.physicalNumberOfRows - 1).collect { int index -> sheet.getRow(index).getCell(0).stringCellValue }
        } finally { workbook.close() }
    }
}
