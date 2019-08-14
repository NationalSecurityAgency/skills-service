package skills.storage.model

import groovy.transform.CompileStatic
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table
import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity()
@Table(name = 'skill_definition')
@EntityListeners(AuditingEntityListener)
@CompileStatic
class SkillDef extends SkillDefParent {
    static enum ContainerType {
        Subject, Skill, Badge, GlobalBadge
    }

}
