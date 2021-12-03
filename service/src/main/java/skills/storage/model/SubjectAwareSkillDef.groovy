package skills.storage.model

interface SubjectAwareSkillDef {

    SkillDef getSkill()
    String getSubjectId()
    String getSubjectName()
    String getProjectName()
}
