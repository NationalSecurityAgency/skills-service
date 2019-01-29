package skills.storage.model

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*

@ToString(excludes =['parent', 'child'])
@Entity()
@Table(name='skill_relationship_definition')
@EntityListeners(AuditingEntityListener)
class SkillRelDef {

    enum RelationshipType { RuleSetDefinition, Dependence, BadgeDependence, Recommendation }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    @ManyToOne
    @JoinColumn(name="parentId")
    SkillDef parent

    @ManyToOne
    @JoinColumn(name="childId")
    SkillDef child

    @Enumerated(EnumType.STRING)
    RelationshipType type = RelationshipType.RuleSetDefinition

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
