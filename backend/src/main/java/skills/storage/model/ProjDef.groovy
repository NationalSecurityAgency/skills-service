package skills.storage.model

import groovy.transform.ToString
import org.hibernate.annotations.Where

import javax.persistence.*

@Entity
@Table(name = 'project_definition')
@ToString(includeNames = true, excludes = ['levelDefinitions', 'subjects', 'badges'])
class ProjDef implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId

    String name

    // need to be denormalized so levels can be efficiently calculated (without the need of loading all of the rules)
    int totalPoints

    int displayOrder

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="projectId")
    List<LevelDef> levelDefinitions

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    @Where(clause = "type = 'Subject'")
    List<SkillDef> subjects

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    @Where(clause = "type = 'Badge'")
    List<SkillDef> badges
}
