package skills.intTests.utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@Slf4j
class WaitForAsyncTasksCompletion {

    @Autowired
    JdbcTemplate jdbcTemplate

    boolean waitForAllScheduleTasks(long waitInMs = 30000) {
        log.info("Waiting for all scheduled task to complete")
        Closure<Integer> numRecords = {
            return jdbcTemplate.queryForObject("select count(*) from scheduled_tasks", Integer.class)
        }
        long start = System.currentTimeMillis()
        int lastNumRecords
        while ( (lastNumRecords = numRecords.call()) > 0) {
            Thread.sleep(250)
            log.trace("Still waiting on [{}] tasks to finish", lastNumRecords)
            if ( System.currentTimeMillis() - start > waitInMs ){
                throw new IllegalStateException("Scheduled tasks took too long to complete. Still [${lastNumRecords}] tasks left!")
            }
        }
        log.info("All scheduled task completed!")
    }

    void clearScheduledTaskTable() {
        jdbcTemplate.execute("delete from scheduled_tasks")
    }
}
