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
package skills.intTests.skillExpiration

import groovy.util.logging.Slf4j
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.SkillAttributesDef
import skills.storage.repos.SkillAttributesDefRepo

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillExpirationConfigSpecs extends DefaultIntSpec {

    DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    def "cannot configure skill expiration for skills imported from catalog"() {
        def user = getRandomUsers(1)[0]
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 2)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 1, 2, 0, 100)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill1)

        when:
        skillsService.exportSkillToCatalog(proj1.projectId, skill1.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId, true)

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(proj2.projectId, skill1.skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Cannot configure expiration attribute on skills imported from the catalog")
    }

    def "cannot configure skill expiration for skills re-used in the same project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        when:

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(p1.projectId, 'skill1STREUSESKILLST0', [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Cannot configure expiration attribute on skills that are reused")
    }

    def "save and get expiration settings" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        when:
        def attributes = skillsService.getSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.expirationType == ExpirationAttrs.YEARLY
        attributes.every == 1
        attributes.monthlyDay == expirationDate.dayOfMonth.toString()
        attributes.nextExpirationDate == DTF.print(expirationDate.toDate().time)
    }

    def "delete expiration attributes" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])


        when:

        def t1_attributes = skillsService.getSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId)
        SkillAttributesDef t1_skillAttributes = skillAttributesDefRepo.findByProjectIdAndSkillIdAndType(p1.projectId, p1Skills[0].skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration.toString())

        skillsService.deleteSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId)

        def t2_attributes = skillsService.getSkillExpirationAttributes(p1.projectId, p1Skills[0].skillId)
        SkillAttributesDef t2_skillAttributes = skillAttributesDefRepo.findByProjectIdAndSkillIdAndType(p1.projectId, p1Skills[0].skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration.toString())

        then:

        t1_attributes.expirationType == ExpirationAttrs.YEARLY
        t1_attributes.every == 1
        t1_attributes.monthlyDay == expirationDate.dayOfMonth.toString()
        t1_attributes.nextExpirationDate == DTF.print(expirationDate.toDate().time)
        t1_skillAttributes

        t2_attributes.expirationType == ExpirationAttrs.NEVER
        t2_attributes.every == null
        t2_attributes.monthlyDay == null
        t2_attributes.nextExpirationDate == null
        !t2_skillAttributes
    }

    def "can only set expiration attributes for a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup])
        def p1Skills = createSkills(6, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        when:
        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(p1.projectId, p1skillsGroup.skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Failed to find skillId")
    }
}