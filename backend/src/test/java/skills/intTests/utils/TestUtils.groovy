package skills.intTests.utils

import groovy.time.TimeCategory

class TestUtils {

    List<Date> getLastNDays(int numDays) {
        use(TimeCategory) {
            return (0..numDays - 1).collect { it.days.ago }.sort()
        }
    }
}
