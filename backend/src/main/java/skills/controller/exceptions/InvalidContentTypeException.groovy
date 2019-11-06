package skills.controller.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Unsupported content-type")
class InvalidContentTypeException extends RuntimeException{

    InvalidContentTypeException(String var1) {
        super(var1)
    }
}
