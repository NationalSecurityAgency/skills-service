package skills.controller.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Maximum Icon size exceeded")
class MaxIconSizeExceeded extends RuntimeException{

    MaxIconSizeExceeded(String var1) {
        super(var1)
    }
}
