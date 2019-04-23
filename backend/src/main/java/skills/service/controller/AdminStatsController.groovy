package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
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

    @RequestMapping(value = "/projects/{projectId}/metrics", method =  RequestMethod.GET, produces = "application/json")
    List<MetricsChart> getAllProjectMetricsCharts(@PathVariable("projectId") String projectId,
                                                  @RequestParam Map<String,String> chartProps){
        return metricsService.loadChartsForSection(Section.Projects, projectId, chartProps)
    }

    @RequestMapping(value = "/projects/{projectId}/metrics/{chartBuilderId}", method =  RequestMethod.GET, produces = "application/json")
    MetricsChart getProjectMetricsChart(@PathVariable("projectId") String projectId,
                                        @PathVariable(ChartParams.CHART_BUILDER_ID) String chartBuilderId,
                                        @RequestParam Map<String,String> chartProps){
        return metricsService.loadChartForSection(chartBuilderId, Section.Projects, projectId, chartProps)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/metrics", method =  RequestMethod.GET, produces = "application/json")
    List<MetricsChart> getAllBadgeMetricsCharts(@PathVariable("projectId") String projectId,
                                                @PathVariable("badgeId") String badgeId,
                                                @RequestParam Map<String,String> chartProps){
        chartProps.put(ChartParams.BADGE_ID, badgeId)
        return metricsService.loadChartsForSection(Section.Badges, projectId, chartProps)
    }

    @RequestMapping(value = "/projects/{projectId}/badges/{badgeId}/metrics/{chartBuilderId}", method =  RequestMethod.GET, produces = "application/json")
    MetricsChart getBadgeMetricsChart(@PathVariable("projectId") String projectId,
                                      @PathVariable("badgeId") String badgeId,
                                      @PathVariable(ChartParams.CHART_BUILDER_ID) String chartBuilderId,
                                      @RequestParam Map<String,String> chartProps) {
        chartProps.put(ChartParams.BADGE_ID, badgeId)
        return metricsService.loadChartForSection(chartBuilderId, Section.Projects, projectId, chartProps)
    }
}
