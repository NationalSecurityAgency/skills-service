package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.controller.result.model.ValidationResult
import skills.profile.EnableCallStackProf
import skills.services.CustomValidationResult
import skills.services.CustomValidator

@RestController
@RequestMapping("/app/validation")
@Slf4j
@EnableCallStackProf
class CustomValidationController {

    @Autowired
    CustomValidator customValidator

    @RequestMapping(value = "/description", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    ValidationResult validateDescription(@RequestBody Map<String,String> body){
        log.info("validating description");
        CustomValidationResult vr = customValidator.validateDescription(body.value)
        ValidationResult validationResult = new ValidationResult(vr.valid, vr.msg)
        return validationResult
    }

    @RequestMapping(value = "/name", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    ValidationResult validateName(@RequestBody Map<String,String> body){
        CustomValidationResult vr = customValidator.validateName(body.value)
        ValidationResult validationResult = new ValidationResult(vr.valid, vr.msg)
        return validationResult
    }

}
