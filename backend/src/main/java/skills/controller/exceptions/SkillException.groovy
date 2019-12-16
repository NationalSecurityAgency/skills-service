package skills.controller.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class SkillException extends RuntimeException {
    public static final String NA = 'N/A'

    String projectId
    String skillId
    String userId
    ErrorCode errorCode = ErrorCode.InternalError
    boolean printStackTrace = true
    static enum SkillExceptionLogLevel { ERROR, WARN, INFO }

    // will be used by skills.controller.exceptions.RestExceptionHandler
    SkillExceptionLogLevel logLevel = SkillExceptionLogLevel.ERROR
    // only applicable is exception is caught in skills.utils.RetryUtil
    boolean doNotRetry = false

    SkillException(String msg) {
        this(msg, NA, NA)
    }

    SkillException(String msg, Throwable t) {
        this(msg, t, NA, NA)
    }

    SkillException(String msg, String projectId) {
        this(msg, projectId, NA)
    }

    SkillException(String msg, String projectId, String skillId) {
        this(msg, projectId, skillId, ErrorCode.InternalError)
    }

    SkillException(String msg, String projectId, String skillId, ErrorCode errorCode) {
        super(msg)
        this.projectId = projectId
        this.skillId = skillId
        this.errorCode = errorCode
    }

    SkillException(String msg, Throwable var2, String projectId, String skillId) {
        this(msg, var2, projectId, skillId, ErrorCode.InternalError)
    }

    SkillException(String msg, Throwable var2, String projectId, String skillId, ErrorCode errorCode) {
        super(msg, var2)
        this.projectId = projectId
        this.skillId = skillId
        this.errorCode = errorCode
    }

    SkillException(Throwable msg, String projectId, String skillId) {
        super(msg)
        this.projectId = projectId
        this.skillId = skillId
    }

    SkillException(String msg, Throwable var2, boolean var3, boolean var4, String projectId, String skillId) {
        this(msg, var2, var3, var4, projectId, skillId, ErrorCode.InternalError)
    }

    SkillException(String msg, Throwable var2, boolean var3, boolean var4, String projectId, String skillId, ErrorCode errorCode) {
        super(msg, var2, var3, var4)
        this.projectId = projectId
        this.skillId = skillId
        this.errorCode = errorCode
    }
}
