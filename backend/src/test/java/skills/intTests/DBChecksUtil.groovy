package skills.intTests

import skills.intTests.utils.ConfiguredProps

class DBChecksUtil {
    static String ENABLE_DB_CHECKS = "enable.db.checks"

    static boolean isEnabled() {
        String prop = ConfiguredProps.get().getProp("enable.db.checks")
        return prop ? Boolean.valueOf(prop) : false
    }
}
