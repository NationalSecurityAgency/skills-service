package skills.service.icons

import groovy.transform.CompileStatic

import java.util.regex.Pattern

/**
 * Created with IntelliJ IDEA.
 * Date: 11/30/18
 * Time: 12:29 PM
 */
@CompileStatic
class IconCssNameUtil {
    static final Pattern CLEAN = Pattern.compile("(?i)[^a-z0-9]")

    public static String getCssClass(String projectId, String filename) {
        return "${projectId}-"+CLEAN.matcher(filename).replaceAll("")
    }
}
