package skills.storage.model;

import java.util.Date;

public interface SkillDefMin {
    int getId();
    String getProjectId();
    String getSkillId();
    String getName();
    int getPointIncrement();
    int getPointIncrementInterval();
    int getNumMaxOccurrencesIncrementInterval();
    int getTotalPoints();
    SkillDef.ContainerType getType();
    Date getStartDate();
    Date getEndDate();
    String getEnabled();
    SkillDef.SelfReportingType getSelfReportingType();
    Integer getCopiedFrom();
    String getReadOnly();
    String getCopiedFromProjectId();
    String getGroupId();
    int getNumSkillsRequired();
}
