package skills.intTests.cypress

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService

@Slf4j
class CypressIntSpec extends  DefaultIntSpec {

    def setup() {
        SkillsService rootService = createService('root', 'password')
        rootService.grantRoot()
    }

    def cleanup() {

    }

    void runCypress(boolean devMode = false){
        String testName = specificationContext.currentIteration.name
        String testToRun = testName.split("CYPRESS TEST: ")[1]

        String cmd = "npx cypress ${devMode ? 'open' : 'run'} --config baseUrl=http://localhost:${localPort},testFiles=${testToRun}".toString()
        log.info("Executing [${cmd}]")
        String stdoutRes = cmd.execute(null, new File('../frontend')).text
        log.info(stdoutRes)
        if (!stdoutRes.contains("All specs passed!")){
            throw new RuntimeException("Cypress Test Failed: ${testName}")
        }
    }


}
