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

    @Column(name="level_ref_id")
    Integer levelRefId

    Integer level

    @Column(name="proj_ref_id")
    Integer projectRefId

    @Column(name="project_id")
    String projectId

    @Column(name="project_name")
    String projectName

    @Column(name="skill_ref_id")
    Integer badgeRefId

    @Column(name="skill_id")
    String badgeId

}
