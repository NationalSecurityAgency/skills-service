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
import org.springframework.web.bind.annotation.*
import skills.controller.result.model.ModifiedDescription
import skills.controller.result.model.ValidationCheckResult
import skills.controller.result.model.ValidationResult
import skills.dbupgrade.DBUpgradeSafe
import skills.profile.EnableCallStackProf
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.utils.InputSanitizer

@CrossOrigin(allowCredentials = "true", originPatterns = ['*'])
@RestController
@RequestMapping("/api/validation")
@Slf4j
@EnableCallStackProf
class CustomValidationController {

    @Autowired
    CustomValidator customValidator

    @DBUpgradeSafe
    @RequestMapping(value = "/description", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    ValidationResult validateDescription(@RequestBody Map<String,String> body){
        CustomValidationResult vr = customValidator.validateDescription(body.value, body.projectId, shouldUseProtectedCommunityValidator(body), body.quizId)
        ValidationResult validationResult = new ValidationResult(vr.valid, vr.msg)
        return validationResult
    }


    @DBUpgradeSafe
    @RequestMapping(value = "/addPrefixToInvalidParagraphs", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    ModifiedDescription addPrefixToInvalidParagraphs(@RequestBody Map<String,String> body){
        ModifiedDescription result = customValidator.addPrefixToInvalidParagraphs(body.value, body.prefix, body.projectId, shouldUseProtectedCommunityValidator(body), body.quizId)
        return result
    }

    private static Boolean shouldUseProtectedCommunityValidator(Map<String,String> body) {
        Boolean useProtectedCommunityValidator = body.useProtectedCommunityValidator ? Boolean.valueOf(body.useProtectedCommunityValidator) : false
        return useProtectedCommunityValidator
    }

    @DBUpgradeSafe
    @RequestMapping(value = "/name", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    ValidationResult validateName(@RequestBody Map<String,String> body){
        CustomValidationResult vr = customValidator.validateName(body.value)
        ValidationResult validationResult = new ValidationResult(vr.valid, vr.msg)
        return validationResult
    }

    @DBUpgradeSafe
    @RequestMapping(value="/url", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    ValidationResult validateUrl(@RequestBody Map<String, String> body) {
        ValidationResult validationResult = new ValidationResult()
        try {
            InputSanitizer.sanitizeUrl(body.value)
            validationResult.valid = true
        } catch (Exception e) {
            validationResult.msg = e.getMessage();
            validationResult.valid = false
        }

        return validationResult
    }

}
