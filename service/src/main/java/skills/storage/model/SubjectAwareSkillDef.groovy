package skills.storage.model

interface SubjectAwareSkillDef {

    SkillDefWithExtra getSkill()
    String getSubjectId()
    String getSubjectName()
    String getProjectName()
}
