package skills.storage.model

interface SkillDefPartial extends SkillDefSkinny{
    Integer getPointIncrement()
    Integer getPointIncrementInterval()
    Integer getNumMaxOccurrencesIncrementInterval()
    String getIconClass()
    SkillDef.ContainerType getSkillType()
    Date getUpdated()
    SkillDef.SelfReportingType getSelfReportingType()
    String getEnabled()
    Integer getNumSkillsRequired()
    Integer getCopiedFrom()
    String getCopiedFromProjectId()
    Boolean getReadOnly()
    String getCopiedFromProjectName()
    Boolean getSharedToCatalog()
}
