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
package skills.intTests.reportSkills

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.controller.request.model.SkillEventRequest
import skills.services.events.SkillEventResult

@RestController
@RequestMapping("/api")
class ReportSkillsTransactionController {

    @Autowired
    ReportSkillsTransactionalService service

    @RequestMapping(value = "/projects/{projectId}/skills/{skillId}/throwException/{shouldThrow}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    @CompileStatic
    SkillEventResult addSkill(@PathVariable("projectId") String projectId,
                              @PathVariable("skillId") String skillId,
                              @PathVariable("shouldThrow") Boolean shouldThrow,
                              @RequestBody(required = false) SkillEventRequest skillEventRequest) {
        assert skillEventRequest.userId
        assert skillEventRequest.timestamp
        return service.reportSkill(projectId, skillId, skillEventRequest, shouldThrow)
    }





}
