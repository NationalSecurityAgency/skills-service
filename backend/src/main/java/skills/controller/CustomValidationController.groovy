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
