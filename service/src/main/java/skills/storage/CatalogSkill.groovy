package skills.storage

import skills.storage.model.SubjectAwareSkillDef

interface CatalogSkill extends SubjectAwareSkillDef{
    Date getExportedOn()
}
