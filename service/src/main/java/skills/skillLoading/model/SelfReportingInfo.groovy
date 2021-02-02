package skills.skillLoading.model

import skills.storage.model.SkillDef

class SelfReportingInfo {

    boolean enabled
    SkillDef.SelfReportingType type

    // only applicable to when selfReportingType == SelfReportingType.Approval
    Long requestedOn
    Long rejectedOn
    String rejectionMsg
}
