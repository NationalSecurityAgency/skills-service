package skills.service.controller.exceptions

import org.apache.commons.lang3.StringUtils

class SkillsValidator {

    static void isNotBlank(String value, String attrName, String projectId = null, String skillId = null) {
        if (StringUtils.isBlank(value) || value?.trim().equalsIgnoreCase("null")) {
            throw new SkillException("${attrName} was not provided.".toString(), projectId, skillId, ErrorCode.BadParam)
        }
    }

    static void isNotNull(Object value, String attrName, String projectId = null, String skillId = null) {
        if (value == null) {
            throw new SkillException("${attrName} was not provided.".toString(), projectId, skillId, ErrorCode.BadParam)
        }
    }

    static void isFirstOrMustEqualToSecond(String first, String second, String attrName) {
        if (first && second != first) {
            throw new SkillException("${attrName} in the request doesn't equal to ${attrName} in the URL. [${first}]<>[${second}]", null, null, ErrorCode.BadParam)
        }
    }

    static void isTrue(boolean condition, String msg, String projectId = null, String skillId = null) {
        if (!condition) {
            throw new SkillException(msg, projectId, skillId, ErrorCode.BadParam)
        }
    }
}
