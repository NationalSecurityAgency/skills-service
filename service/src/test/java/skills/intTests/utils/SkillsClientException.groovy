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
package skills.intTests.utils

import org.springframework.http.HttpStatus

class SkillsClientException extends RuntimeException {

    String url

    HttpStatus httpStatus

    String resBody

    SkillsClientException(String message, String url, HttpStatus httpStatus) {
        super(message)
        this.url = url
        this.httpStatus = httpStatus
    }

    SkillsClientException(String message, Throwable cause, String url, HttpStatus httpStatus) {
        super(message, cause)
        this.url = url
        this.httpStatus = httpStatus
    }

    SkillsClientException(Throwable cause, String url, HttpStatus httpStatus) {
        super(cause)
        this.url = url
        this.httpStatus = httpStatus
    }

    @Override
    String toString() {
        String res = super.toString()
        if (resBody){
            res += " resBody=[${resBody}]"
        }
        return res
    }
}
