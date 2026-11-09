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
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

/** Temporary pre-5.1 cleanup acceptance for reused credit; retire after remediation. */
// Close this context's scheduler before the dedicated IT context installs worker barriers.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationCleanupReuseSpecs extends CatalogCleanupIntSpec {
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
}
