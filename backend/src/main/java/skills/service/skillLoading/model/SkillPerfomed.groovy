package skills.service.skillLoading.model

import groovy.transform.Canonical

@Canonical
class SkillPerfomed {
    String id
    String skillId
    Date performedOn
}
