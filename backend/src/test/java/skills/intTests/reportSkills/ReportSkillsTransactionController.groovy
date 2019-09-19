package skills.intTests.reportSkills

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
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
                              @RequestBody(required = false) skills.controller.request.model.SkillEventRequest skillEventRequest) {
        assert skillEventRequest.userId
        assert skillEventRequest.timestamp
        return service.reportSkill(projectId, skillId, skillEventRequest, shouldThrow)
    }



}
