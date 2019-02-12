package skills.service.icons

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class IconCssNameUtil {
    static final Pattern CLEAN = Pattern.compile("(?i)[^a-z0-9]")

    public static String getCssClass(String projectId, String filename) {
        return "${projectId}-"+CLEAN.matcher(filename).replaceAll("")
    }
}
