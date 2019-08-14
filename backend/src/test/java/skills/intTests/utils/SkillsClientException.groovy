package skills.intTests.utils

import org.springframework.http.HttpStatus

class SkillsClientException extends RuntimeException {

    String url

    HttpStatus httpStatus

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
}
