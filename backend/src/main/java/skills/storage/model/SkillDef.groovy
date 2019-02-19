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
    int totalPoints

    String iconClass

    @Enumerated(EnumType.STRING)
    ContainerType type = ContainerType.Skill

    @Lob
    @Column(name="description", columnDefinition = "TEXT")
    String description

    @Lob
    @Column(name="helpUrl", columnDefinition = "TEXT")
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
    @JoinColumn(name="skillId")
    List<LevelDef> levelDefinitions = new ArrayList<LevelDef>()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customIconId")
    CustomIcon customIcon

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    ProjDef projDef

    public void addLevel(LevelDef level) {
        if (level == null) {
            throw new IllegalArgumentException("cannot add null level")
        }
        if (level.getSkillDef() != null) {
            level.getSkillDef().getLevelDefinitions().remove(level)
        }
        levelDefinitions.add(level)
        level.setSkillDef(this)
    }
}
