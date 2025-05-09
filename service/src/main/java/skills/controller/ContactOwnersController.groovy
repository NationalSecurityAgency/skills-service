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
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ContactOwnerRequest
import skills.controller.result.model.RequestResult
import skills.profile.EnableCallStackProf
import skills.services.ContactOwnerService
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.utils.InputSanitizer

@RestController
@RequestMapping("/api")
@Slf4j
@EnableCallStackProf
class ContactOwnersController {

    @Autowired
    CustomValidator customValidator

    @Autowired
    ContactOwnerService contactOwnersService

    @RequestMapping(value="/projects/{projectId}/contact", method = RequestMethod.POST, produces = "application/json")
    public RequestResult contactOwners(@PathVariable("projectId") String projectId, @RequestBody ContactOwnerRequest msg) {
        SkillsValidator.isNotBlank(msg?.message, "message", projectId)
        //TODO: some form of RateLimiting should be applied to each user since this end point exposes all projects to contact emails
        CustomValidationResult result = customValidator.validateDescription(msg.message, projectId)

        if (!result.valid) {
            throw new SkillException("Message is invalid: ${result.msg}", projectId)
        }

        String clean = InputSanitizer.sanitize(msg.message)

        contactOwnersService.contactProjectOwner(projectId, clean)

        return RequestResult.success()
    }

    @PostMapping(value="/projects/{projectId}/newInviteRequest", produces = "application/json")
    RequestResult newInviteRequest(@PathVariable("projectId") String projectId) {
        contactOwnersService.sendInviteRequest(projectId)
        return RequestResult.success()
    }

}
