package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.controller.result.model.TimestampCountItem
import skills.service.datastore.services.AdminUsersService

@RestController
@RequestMapping("/admin")
@Slf4j
class AdminStatsController {

    @Autowired
    AdminUsersService adminUsersService


    @RequestMapping(value = "/projects/{projectId}/usage", method =  RequestMethod.GET, produces = "application/json")
    List<TimestampCountItem> getSystemUsage(@PathVariable("projectId") String projectId, @RequestParam("numDays") Integer numDays){
        assert numDays > 1

        return adminUsersService.getUsage(projectId, numDays)
    }

    enum StatType {
        NUM_ACHIEVED_SKILLS_PIVOTED_BY_SUBJECT,
        NUM_USERS_PER_OVERALL_SKILL_LEVEL
    }

    @RequestMapping(value = "/projects/{projectId}/stats", method =  RequestMethod.GET, produces = "application/json")
    Object getSystemUsageBySubject(@PathVariable("projectId") String projectId,
                                   @RequestParam("type") StatType type){
        Object res
        switch (type) {
            case StatType.NUM_ACHIEVED_SKILLS_PIVOTED_BY_SUBJECT:
                res = adminUsersService.getAchievementCountsPerSubject(projectId)
                break;
            case StatType.NUM_USERS_PER_OVERALL_SKILL_LEVEL:
                res = adminUsersService.getUserCountsPerLevel(projectId)
                break;
        }
        return res
    }
}
