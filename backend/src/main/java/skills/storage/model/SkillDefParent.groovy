package skills.storage.model

import groovy.transform.CompileStatic
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

import javax.persistence.*

@CompileStatic
@MappedSuperclass
class SkillDefParent {

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
    SkillDef.ContainerType type = SkillDef.ContainerType.Skill

    int displayOrder

    @Temporal(TemporalType.TIMESTAMP)
    Date startDate // optional, used for "gem" badges only currently

    @Temporal(TemporalType.TIMESTAMP)
    Date endDate  // optional, used for "gem" badges only currently

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="skillRefId", insertable = false, updatable = false)
    List<LevelDef> levelDefinitions

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    ProjDef projDef

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
