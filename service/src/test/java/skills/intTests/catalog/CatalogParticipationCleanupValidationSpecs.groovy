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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardItem

/** Temporary pre-5.1 cleanup/Fix request validation; retire after remediation. */
// Close this context's scheduler before the dedicated IT context installs worker barriers.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationCleanupValidationSpecs extends CatalogCleanupIntSpec {
    def "cleanup rejects unsafe or ambiguous scope #selection before changing credit"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        seedResidue(setup.destinations[0], user)
        Map before = snapshot('TestProject2', user)
        SkillsService root = createRootSkillService()

        when:
        root.cleanupImportedSkillProgress(selection)

        then:
        SkillsClientException failure = thrown()
        failure.httpStatus.value() == 400
        failure.message.contains('BadParam')
        snapshot('TestProject2', user) == before
        root.getUserActionsForEverything(10, 1, 'created', false, '', DashboardItem.SkillsBatch,
                '', '', 'cleanupImportedSkillProgress').data.isEmpty()

        where:
        selection << [null, [:], [projectId: 'TestProject2'], [allProjects: false], [allProjects: 'true'],
                      [allProjects: null], [projectIds: ['TestProject2'], allProjects: true],
                      [projectIds: ['TestProject2'], typo: true], [allProjects: true, typo: true],
                      [projectIds: []], [projectIds: 'TestProject2'], [projectIds: [123]], [projectIds: [' ']]]
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
}
