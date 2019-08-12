package skills.controller.result.model

import com.fasterxml.jackson.annotation.JsonFormat
import groovy.transform.Canonical

@Canonical
class BadgeResult {

    String badgeId

    String projectId

    String name

    int totalPoints

    String description

    int numSkills

    int displayOrder

    String iconClass

    @JsonFormat(pattern = "MM-dd-yyyy")
    Date startDate
    @JsonFormat(pattern = "MM-dd-yyyy")
    Date endDate

    List<SkillDefRes> requiredSkills = []
}
