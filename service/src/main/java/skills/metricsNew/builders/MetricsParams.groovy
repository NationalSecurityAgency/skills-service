package skills.metricsNew.builders

import skills.controller.exceptions.SkillException

class MetricsParams {
    final static String P_SKILL_ID = "skillId"

    static String getSkillId(String projectId, String chartId, Map<String, String> props) {
        String sortBy = props[P_SKILL_ID]
        if (!sortBy) {
            throw new SkillException("Chart[${chartId}]: Must supply ${P_SKILL_ID} param", projectId)
        }
        return sortBy
    }
}
