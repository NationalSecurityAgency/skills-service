package skills.storage.model

import groovy.transform.ToString

import javax.persistence.*

@Entity
@Table(name='global_badge_level_definition')
@ToString(includeNames = true)
class GlobalBadgeLevelDef implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    int level

    @Column(name="project_id")
    Integer projectId

    @Column(name="project_name")
    String projectName

    @Column(name="skill_id")
    Integer badgeId
}
