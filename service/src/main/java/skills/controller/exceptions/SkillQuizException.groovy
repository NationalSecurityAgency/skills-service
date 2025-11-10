/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.controller.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class SkillQuizException extends RuntimeException {
    public static final String NA = 'N/A'

    String quizId
    Integer questionId
    String userId
    ErrorCode errorCode = ErrorCode.InternalError
    boolean printStackTrace = true
    static enum SkillExceptionLogLevel { ERROR, WARN, INFO }

    // will be used by skills.controller.exceptions.RestExceptionHandler
    SkillExceptionLogLevel logLevel = SkillExceptionLogLevel.ERROR
    // only applicable is exception is caught in skills.utils.RetryUtil
    boolean doNotRetry = false

    SkillQuizException(String msg) {
        this(msg, NA, ErrorCode.InternalError)
    }

    SkillQuizException(String msg, Throwable t) {
        this(msg, t, NA, ErrorCode.InternalError)
    }

    SkillQuizException(String msg, String quizId) {
        this(msg, quizId, ErrorCode.InternalError)
    }

    SkillQuizException(String msg, ErrorCode errorCode) {
        this(msg, NA, errorCode)
    }


    SkillQuizException(String msg, String quizId, ErrorCode errorCode) {
        super(msg)
        this.quizId = quizId
        this.errorCode = errorCode
    }

    SkillQuizException(String msg, Throwable var2, String quizId, ErrorCode errorCode) {
        super(msg, var2)
        this.quizId = quizId
        this.errorCode = errorCode
    }

    SkillQuizException(String msg, Throwable var2, String quizId, Integer questionId, ErrorCode errorCode) {
        super(msg, var2)
        this.quizId = quizId
        this.questionId = questionId
        this.errorCode = errorCode
    }

    SkillQuizException(String msg, Throwable var2, boolean var3, boolean var4, String quizId, ErrorCode errorCode) {
        super(msg, var2, var3, var4)
        this.quizId = quizId
        this.errorCode = errorCode
    }
}
