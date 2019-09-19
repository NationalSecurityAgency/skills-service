package skills.intTests.reportSkills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class ReportSkillsTransactionSpecs extends DefaultIntSpec {

    @Autowired
    JdbcTemplate jdbcTemplate

    def "multi threaded insert of the same skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1, )
        skills[0].numPerformToCompletion=1000
        skills[0].pointIncrementInterval=0 // disable
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        AtomicInteger atomicInteger = new AtomicInteger()
        int numThreads = 8
        when:
        numThreads.times {
            Thread.start {
                200.times {
                    skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
                    atomicInteger.incrementAndGet()
                }
            }
        }

        boolean res = waitFor(atomicInteger, numThreads*200, 60)
        then:
        res
    }

    private boolean waitFor(AtomicInteger atomicInteger, int expectedCount, int waitForSecs) {
        log.info("Wait for the counter to reach {}", expectedCount)
        long start = System.currentTimeMillis()
        while (atomicInteger < expectedCount && (System.currentTimeMillis() - start) < (waitForSecs * 1000)) {
            Thread.sleep(1000)
            log.info("Wait for the counter to reach {}, current is {}", expectedCount, atomicInteger.get())
        }
        log.info("Done waiting. Current={}, Expected={}",atomicInteger.get(), expectedCount)

        return atomicInteger.get() == expectedCount
    }

    def "test transaction when reporting skill - no rollback"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1, )
        skills[0].numPerformToCompletion=1
        skills[0].pointIncrement=500
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String user = "usera"

        when:
//        SkillEventResult skillEventResult = reportSkillsTransactionWrapper.reportSkill1(proj.projectId, skills[0].skillId, user, new Date())
        def skillEventResult = skillsService.addSkillAndOptionallyThrowExceptionAtTheEnd([projectId: proj.projectId, skillId: skills[0].skillId], user, new Date(), false)
        List res = getUserPointsFromDB(proj.projectId, skills)
        then:
        skillEventResult.body.skillApplied
        res.size() == 2
    }

    @Rollback(false)
    def "test transaction when reporting skill - rollback"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1, )
        skills[0].numPerformToCompletion=1
        skills[0].pointIncrement=500
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String user = "usera"

        when:
//        SkillEventResult skillEventResult = reportSkillsTransactionWrapper.reportSkill1(proj.projectId, skills[0].skillId, user, new Date())
        skillsService.addSkillAndOptionallyThrowExceptionAtTheEnd([projectId: proj.projectId, skillId: skills[0].skillId], user, new Date(), true)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        getUserPointsFromDB(proj.projectId, skills).size() == 0
        exception.httpStatus == HttpStatus.INTERNAL_SERVER_ERROR
    }

    private List<Map<String, Object>> getUserPointsFromDB(String projectId, List<Map> skills) {
        jdbcTemplate.queryForList("select * from user_points where project_id='${projectId}' and skill_id='${skills[0].skillId}'")
    }

}
