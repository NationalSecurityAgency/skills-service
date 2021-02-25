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

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Conditional
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.repos.LevelDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.nativeSql.DBConditions
import spock.lang.Ignore

import javax.annotation.PostConstruct
import javax.sql.DataSource

@Deprecated
@Ignore
@Slf4j
@SpringBootTest(properties = ['skills.db.startup=false', 'spring.liquibase.enabled=false'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class DataMigrationDBIT extends DefaultIntSpec {

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

    @Autowired
    DbDropper dbDropper

    @Override
    def doSetup() {
        // disable initial db setup in the base class
        dbDropper.dropDb()
    }

    def "validate migration1"() {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()))
        Liquibase liquibase = new Liquibase(StringUtils.removeStart(changeLog.getPath(), "classpath:"), new ClassLoaderResourceAccessor(), database)
        liquibase.update(new Contexts('!migration1'), new LabelExpression())

        insertPreMigrationTestData("migration1.sql")
        skillsService = createService()
        when:
        String projectId = 'TestProject1'
        String userId = 'user1'

        // get skill_definition, settings, and user_achievement rows pre migration1
        def existingSkillDefsPreMigration = getSkillDefinitionFromDB(projectId)
        def existingUserAchievementsPreMigration = getUserAchievementsFromDB(projectId)
        def existingSettingsPreMigration = getSettingsFromDB()

        // apply migration1 changes
        liquibase.update(new Contexts('migration1'), new LabelExpression())

        // get skill_definition, settings, and user_achievement rows post migration1
        def existingSkillDefsPostMigration = getSkillDefinitionFromDB(projectId)
        def existingUserAchievementsPostMigration = getUserAchievementsFromDB(projectId)
        def settingsPostMigration = getSettingsFromDB()

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
        def resetTokensPostMigration = getPasswordResetTokens()

        then:
        existingSkillDefsPreMigration.findAll { it.containsKey('ENABLED') }.size() == 0
        existingUserAchievementsPreMigration.findAll { it.containsKey('NOTIFIED') }.size() == 0
        //make sure existing setting value wasn't changed by column size alteration
        existingSettingsPreMigration.find {it['id'] == 2}.value == settingsPostMigration.find {it['id'] ==2}.value

        existingSkillDefsPreMigration.size() == existingSkillDefsPostMigration.size()
        existingUserAchievementsPreMigration.size() == existingUserAchievementsPreMigration.size()

        existingSkillDefsPostMigration.findAll { it.containsKey('ENABLED') }.size() == existingSkillDefsPostMigration.size()
        existingUserAchievementsPostMigration.findAll { it.containsKey('NOTIFIED') }.size() == existingUserAchievementsPostMigration.size()
        existingSkillDefsPostMigration.findAll { new Boolean(it.ENABLED) == true }.size() == existingSkillDefsPostMigration.size()
        existingUserAchievementsPostMigration.findAll { new Boolean(it.NOTIFIED) == true }.size() == existingUserAchievementsPostMigration.size()
        !resetTokensPostMigration
    }

    private insertPreMigrationTestData(String sqlFilePath) {
        new ClassPathResource(sqlFilePath).getFile().eachLine { sqlStmt ->
            jdbcTemplate.execute(sqlStmt)
        }
    }

    def "validate migration2"() {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()))
        Liquibase liquibase = new Liquibase(StringUtils.removeStart(changeLog.getPath(), "classpath:"), new ClassLoaderResourceAccessor(), database)
        liquibase.update(new Contexts('migration1'), new LabelExpression())

        insertPreMigrationTestData("migration2.sql")
        def resBefore = jdbcTemplate.queryForList("select * from user_achievement")
        when:
        liquibase.update(new Contexts('migration2'), new LabelExpression())
        def resAfter = jdbcTemplate.queryForList("select * from user_achievement")
        then:
        resBefore.findAll({it["achieved_on".toUpperCase()]}).size() == 0
        resAfter.findAll({it["achieved_on".toUpperCase()]}).size() == 16
        resAfter.each {
            assert it["achieved_on".toUpperCase()] == it["created".toUpperCase()]
        }
    }

    private List<Map<String, Object>> getSkillDefinitionFromDB(String projectId) {
        jdbcTemplate.queryForList("select * from skill_definition where project_id='${projectId}'")
    }

    private List<Map<String, Object>> getUserAchievementsFromDB(String projectId) {
        jdbcTemplate.queryForList("select * from user_achievement where project_id='${projectId}'")
    }

    private List<Map<String, Object>> getSettingsFromDB() {
        jdbcTemplate.queryForList("select * from settings");
    }

    private List<Map<String, Object>> getPasswordResetTokens() {
        jdbcTemplate.queryForList("select * from password_reset_token")
    }

    // over-ride beans that interact with the database during spring context initialization
    @ConditionalOnProperty(
            name = "skills.db.startup",
            havingValue = "false",
            matchIfMissing = false)
    @Component
    static class HealthChecker extends skills.HealthChecker {
        @PostConstruct
        @Override
        void checkRequiredServices() { }
    }

    @ConditionalOnProperty(
            name = "skills.db.startup",
            havingValue = "false",
            matchIfMissing = false)
    @Component
    static class EmailSettingsService extends skills.settings.EmailSettingsService {
        @PostConstruct
        @Override
        void init() { }
    }

    static interface DbDropper {
        void dropDb()
    }

    @Conditional(DBConditions.PostgresQL)
    @Component
    static class PostgresqlDropper implements DbDropper {

        @Autowired
        JdbcTemplate jdbcTemplate

        @Override
        void dropDb() {
            jdbcTemplate.execute('drop schema public cascade')
            jdbcTemplate.execute('create schema public')
            log.info("Dropped PostgreSQL database")
        }
    }

    @Conditional(DBConditions.H2_IN_MEMORY)
    @Component
    static class H2Dropper implements DbDropper {

        @Autowired
        JdbcTemplate jdbcTemplate

        @Override
        void dropDb() {
            jdbcTemplate.execute('DROP ALL OBJECTS')
            log.info("Dropped H2 database")
        }
    }
}
