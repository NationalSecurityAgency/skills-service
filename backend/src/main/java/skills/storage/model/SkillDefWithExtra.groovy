package skills.storage.model

import groovy.transform.CompileStatic
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*

@Entity()
@Table(name='skill_definition')
@EntityListeners(AuditingEntityListener)
@CompileStatic
class SkillDefWithExtra extends SkillDefParent {

    @Lob
    @Column(columnDefinition = "text")
    String description

    @Lob
    @Column(columnDefinition = "text")
    String helpUrl
}
