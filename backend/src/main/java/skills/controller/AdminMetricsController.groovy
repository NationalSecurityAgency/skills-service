package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.services.AdminUsersService
import skills.metrics.ChartParams
import skills.metrics.MetricsService
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.profile.EnableCallStackProf

@RestController
@RequestMapping("/admin")
@Slf4j
@EnableCallStackProf
class AdminMetricsController {

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    MetricsService metricsService

    @RequestMapping(value = "/projects/{projectId}/{section}/{sectionId}/metrics", method =  RequestMethod.GET, produces = "application/json")
    List<MetricsChart> getAllSectionMetricsCharts(@PathVariable("projectId") String projectId,
                                                  @PathVariable("section") Section section,
                                                  @PathVariable(ChartParams.SECTION_ID) String sectionId,
                                                  @RequestParam Map<String,String> chartProps) {
        chartProps.put(ChartParams.SECTION_ID, sectionId)
        return metricsService.loadChartsForSection(section, projectId, chartProps)
    }

    @RequestMapping(value = "/projects/{projectId}/{section}/{sectionId}/metrics/{chartBuilderId}", method =  RequestMethod.GET, produces = "application/json")
    MetricsChart getSectionMetricsChart(@PathVariable("projectId") String projectId,
                                        @PathVariable("section") Section section,
                                        @PathVariable(ChartParams.SECTION_ID) String sectionId,
                                        @PathVariable(ChartParams.CHART_BUILDER_ID) String chartBuilderId,
                                        @RequestParam Map<String,String> chartProps) {
        chartProps.put(ChartParams.SECTION_ID, sectionId)
        return metricsService.loadChartForSection(chartBuilderId, section, projectId, chartProps)
    }
}
