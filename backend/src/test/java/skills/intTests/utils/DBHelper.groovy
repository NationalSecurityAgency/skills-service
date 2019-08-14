package skills.intTests.utils

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import spock.lang.Shared

@Slf4j
class DBHelper {

    static ConfiguredProps props = ConfiguredProps.get()

    def sqlEngine

    DBHelper() {
        def url = props.getProp("db.url")
        def user = props.getProp("db.user")
        def password = props.getProp("db.pass")
        def driver = props.getProp("db.driver")
        log.info("Creating db connection for url=[$url], user=[$user], pass=[$password], driver=[$driver]")
        sqlEngine = Sql.newInstance(url, user, password, driver)
    }

    void query(String sql, Closure closure) {
        log.info(sql)
        this.sqlEngine.eachRow(sql) { row ->
            closure.call(row)
        }
    }

    def firstRow(String sql){
        log.info(sql)
        return sqlEngine.firstRow(sql)
    }

}
