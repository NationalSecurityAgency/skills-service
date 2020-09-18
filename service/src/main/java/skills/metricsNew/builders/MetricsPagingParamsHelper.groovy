package skills.metricsNew.builders

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException

@Component
class MetricsPagingParamsHelper {
    static String PROP_PAGE_SIZE = "pageSize"
    static String PROP_CURRENT_PAGE = "currentPage"
    @Value('#{"${skills.levels.max:25}"}')
    private int maxLevels

    PageRequest createPageRequest(String projectId, String chartId, Map<String,String> props) {
        int pageSize = props[PROP_PAGE_SIZE] ? Integer.valueOf(props[PROP_PAGE_SIZE]) : 10
        // client's page starts 1, dbs at 0
        int currentPage = props[PROP_CURRENT_PAGE] ? Integer.valueOf(props[PROP_CURRENT_PAGE]) - 1 : 0
        if (pageSize > 100) {
            throw new SkillException("Chart[${chartId}]: page size must not exceed 100. Provided [${pageSize}]", projectId)
        }
        if (pageSize < 1) {
            throw new SkillException("Chart[${chartId}]: page size must not be less than 1. Provided [${pageSize}]", projectId)
        }
        if (currentPage < 0) {
            throw new SkillException("Chart[${chartId}]: current page must not be less than 0. Provided [${currentPage}]", projectId)
        }

        return PageRequest.of(currentPage, pageSize)
    }
}
