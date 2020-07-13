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

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillRelDefRepo

import javax.annotation.PostConstruct
import javax.sql.DataSource

@SpringBootTest(properties = ['skills.db.startup=false', 'spring.liquibase.enabled=false'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DataMigrationSpecs extends DefaultIntSpec {

    @Autowired
    LevelDefRepo levelDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillRelDefRepo relDefRepo

    @Autowired
    DataSource dataSource

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    @Value('${spring.liquibase.change-log}')
    ClassPathResource changeLog

    @Override
    def doSetup() {
        // disable initial db setup in the base class
    }

    def "validate migration1"() {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()))
        Liquibase liquibase = new Liquibase(StringUtils.removeStart(changeLog.getPath(), "classpath:"), new ClassLoaderResourceAccessor(), database)
        liquibase.update(new Contexts('!migration1'), new LabelExpression())

        insertPreMigration1TestData()
        skillsService = createService()
        when:
        String projectId = 'TestProject1'
        String userId = 'user1'

        // get skill_definition and user_achievement rows pre migration1
        def existingSkillDefsPreMigration = getSkillDefinitionFromDB(projectId)
        def existingUserAchievementsPreMigration = getUserAchievementsFromDB(projectId)

        // apply migration1 changes
        liquibase.update(new Contexts('migration1'), new LabelExpression())

        // get skill_definition and user_achievement rows post migration1
        def existingSkillDefsPostMigration = getSkillDefinitionFromDB(projectId)
        def existingUserAchievementsPostMigration = getUserAchievementsFromDB(projectId)

        // also, create a new subject, skill and user_achievement post migration
        // and make sure everything works as expected
        def subject2 = SkillsFactory.createSubject(1, 2)
        List<Map> subjectSkills2 = SkillsFactory.createSkills(1, 1, 2)
        subjectSkills2.each {
            it.pointIncrement = 100
        }
        def expectedSkill2 = subjectSkills2.get(0)
        skillsService.createSubject(subject2)
        skillsService.createSkills(subjectSkills2)
        def res = skillsService.addSkill([projectId: projectId, skillId: expectedSkill2.skillId], userId, new Date())

        then:
        existingSkillDefsPreMigration.findAll { it.containsKey('ENABLED') }.size() == 0
        existingUserAchievementsPreMigration.findAll { it.containsKey('NOTIFIED') }.size() == 0

        existingSkillDefsPreMigration.size() == existingSkillDefsPostMigration.size()
        existingUserAchievementsPreMigration.size() == existingUserAchievementsPreMigration.size()

        existingSkillDefsPostMigration.findAll { it.containsKey('ENABLED') }.size() == existingSkillDefsPostMigration.size()
        existingUserAchievementsPostMigration.findAll { it.containsKey('NOTIFIED') }.size() == existingUserAchievementsPostMigration.size()
        existingSkillDefsPostMigration.findAll { new Boolean(it.ENABLED) == true }.size() == existingSkillDefsPostMigration.size()
        existingUserAchievementsPostMigration.findAll { new Boolean(it.NOTIFIED) == true }.size() == existingUserAchievementsPostMigration.size()

        res.body.skillApplied
        res.body.explanation == "Skill event was applied"

        res.body.completed.size() == 6
        res.body.completed.find({ it.type == "Skill" }).id == expectedSkill2.skillId
        res.body.completed.find({ it.type == "Skill" }).name == expectedSkill2.name

        res.body.completed.findAll({ it.type == "Subject" && it.id == subject2.subjectId }).size() == 5
        res.body.completed.findAll({ it.type == "Subject" && it.name == subject2.name }).size() == 5
        res.body.completed.findAll({ it.type == "Subject" && it.level == 1 })
        res.body.completed.findAll({ it.type == "Subject" && it.level == 2 })
        res.body.completed.findAll({ it.type == "Subject" && it.level == 3 })
        res.body.completed.findAll({ it.type == "Subject" && it.level == 4 })
        res.body.completed.findAll({ it.type == "Subject" && it.level == 5 })
    }

    private insertPreMigration1TestData() {
        new ClassPathResource("migration1.sql").getFile().eachLine { sqlStmt ->
            jdbcTemplate.execute(sqlStmt)
        }
    }

    private List<Map<String, Object>> getSkillDefinitionFromDB(String projectId) {
        jdbcTemplate.queryForList("select * from skill_definition where project_id='${projectId}'")
    }

    private List<Map<String, Object>> getUserAchievementsFromDB(String projectId) {
        jdbcTemplate.queryForList("select * from user_achievement where project_id='${projectId}'")
    }

    // over-ride beans that interact with the database during spring context initialization
    @Component
    static class HealthChecker extends skills.HealthChecker {
        @PostConstruct
        @Override
        void checkRequiredServices() { }
    }

    @Component
    static class EmailSettingsService extends skills.settings.EmailSettingsService {
        @PostConstruct
        @Override
        void init() { }
    }
}
