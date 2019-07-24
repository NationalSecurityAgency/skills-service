package skills.storage.model

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*

@Entity
@ToString(includeNames = true)
@EntityListeners(AuditingEntityListener)
class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String userId

    // denormalize for performance and convenience
    String projectId

    // null subject will represent overall points
    String skillId

    // fk to SkillDef
    Integer skillRefId

    int points

    @Temporal(TemporalType.DATE)
    Date day // documents which day points are for; null indicates overall points rather than a single day

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
