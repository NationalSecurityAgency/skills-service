package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.controller.result.model.TimestampCountItem
import skills.service.datastore.services.AdminUsersService
import skills.service.metrics.ChartParams
import skills.service.metrics.MetricsService
import skills.service.metrics.model.MetricsChart
import skills.service.metrics.model.Section

@RestController
@RequestMapping("/admin")
@Slf4j
class AdminStatsController {

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    MetricsService metricsService


    @RequestMapping(value = "/projects/{projectId}/usage", method =  RequestMethod.GET, produces = "application/json")
    List<TimestampCountItem> getSystemUsage(@PathVariable("projectId") String projectId, @RequestParam(ChartParams.NUM_DAYS) Integer numDays){
        assert numDays > 1

        return adminUsersService.getUsage(projectId, numDays)
    }

    @RequestMapping(value = "/projects/{projectId}/metrics", method =  RequestMethod.GET, produces = "application/json")
    List<MetricsChart> getAllProjectMetricsCharts(@PathVariable("projectId") String projectId, @RequestParam Map<String,String> chartProps){
        return metricsService.loadChartsForSection(Section.Projects, projectId, chartProps)
    }

    @RequestMapping(value = "/projects/{projectId}/metrics/{chartBuilderId}", method =  RequestMethod.GET, produces = "application/json")
    MetricsChart getProjectMetricsChart(@PathVariable("projectId") String projectId, @PathVariable(ChartParams.CHART_BUILDER_ID) String chartBuilderId, @RequestParam Map<String,String> chartProps){
        return metricsService.loadChartsForSection(chartBuilderId, Section.Projects, projectId, chartProps)
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
