package skills.stressTests

import spock.lang.Specification

class StressTestRunner extends Specification{

    def "run stress test"(){
        HitSkillsHard hitSkillsHard = new HitSkillsHard()
        hitSkillsHard.execute()
        when:
        true
        then:
        true
    }
}
