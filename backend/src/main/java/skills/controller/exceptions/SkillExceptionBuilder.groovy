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

class SkillExceptionBuilder {

    private String _msg
    private Throwable _t
    private String _projectId
    private String _skillId
    private String _userId
    private ErrorCode _errorCode = ErrorCode.InternalError
    SkillException.SkillExceptionLogLevel _logLevel = SkillException.SkillExceptionLogLevel.ERROR
    private boolean _printStackTrace = true
    private boolean _doNotRetry = false

    SkillExceptionBuilder logLevel(SkillException.SkillExceptionLogLevel logLevel){
        this._logLevel = logLevel
        return this
    }
    SkillExceptionBuilder doNotRetry(boolean doNotRetry){
        this._doNotRetry = doNotRetry
        return this
    }
    SkillExceptionBuilder printStackTrace(boolean printStackTrace){
        this._printStackTrace = printStackTrace
        return this
    }

    SkillExceptionBuilder msg(String msg){
        this._msg = msg
        return this
    }
    SkillExceptionBuilder throwable(Throwable t){
        this._t = t
        return this
    }
    SkillExceptionBuilder projectId(String projectId){
        this._projectId = projectId
        return this
    }
    SkillExceptionBuilder skillId(String skillId){
        this._skillId = skillId
        return this
    }
    SkillExceptionBuilder userId(String userId){
        this._userId = userId
        return this
    }
    SkillExceptionBuilder errorCode(String errorCode){
        this._errorCode = errorCode
        return this
    }

    SkillException build(){
        assert _msg, "Must supply msg"
        SkillException exception
        if (_msg && _t) {
            exception = new SkillException(_msg, _t)
        } else if (_msg) {
            exception = new SkillException(_msg)
        }

        exception.userId = _userId
        exception.projectId = _projectId
        exception.skillId = _skillId
        exception.errorCode = _errorCode
        exception.logLevel = _logLevel
        exception.doNotRetry = _doNotRetry
        exception.printStackTrace = _printStackTrace
        return exception
    }
}
