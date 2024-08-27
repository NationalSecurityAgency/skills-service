package skills.dbupgrade

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec

class UpgradeInProgressSpecs extends DefaultIntSpec {

    @Autowired
    S3ClientSample s3ClientSample

    def "upgrade in progress"() {
        when:
        s3ClientSample.readFile()

        then:
        true
    }

    def "write to s3"() {
        when:
        s3ClientSample.writeFile()

        then:
        true
    }
}
