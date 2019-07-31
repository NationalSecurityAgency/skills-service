package skills.storage.model

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*

@ToString(excludes = ['levelDefinitions', 'projDef'], includeNames = true)
@Entity()
@Table(name='skill_definition')
@EntityListeners(AuditingEntityListener)
class SkillDef {

    static enum ContainerType { Subject, Skill, Badge }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String skillId

    String projectId

    String name

    int pointIncrement
    int pointIncrementInterval
    @Column(name = 'increment_interval_max_occurrences')
    int numMaxOccurrencesIncrementInterval
    int totalPoints

    int version

    String iconClass

    @Enumerated(EnumType.STRING)
    ContainerType type = ContainerType.Skill


    @Lob
    @Column(columnDefinition = "text")
    String description

    @Lob
    @Column(columnDefinition = "text")
    String helpUrl

    int displayOrder


    @Temporal(TemporalType.TIMESTAMP)
    Date startDate // optional, used for "gem" badges only currently

    @Temporal(TemporalType.TIMESTAMP)
    Date endDate  // optional, used for "gem" badges only currently

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="skill_id", insertable = false, updatable = false)
    List<LevelDef> levelDefinitions

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customIconId")
    CustomIcon customIcon

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    ProjDef projDef
}
