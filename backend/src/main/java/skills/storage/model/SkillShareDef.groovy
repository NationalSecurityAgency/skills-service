package skills.storage.model

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*

@ToString(excludes =['skill', 'sharedToProject'])
@Entity()
@Table(name='skill_share_definition')
@EntityListeners(AuditingEntityListener)
class SkillShareDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    @ManyToOne
    @JoinColumn(name="skill_id")
    SkillDef skill

    @ManyToOne
    @JoinColumn(name="shared_to_project_id")
    ProjDef sharedToProject

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
