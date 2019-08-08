package skills.storage.model

import groovy.transform.ToString
import org.hibernate.annotations.Where

import javax.persistence.*

@Entity
@Table(name = 'project_definition')
@ToString(includeNames = true, excludes = ['subjects', 'badges', 'customIcons'])
class ProjDef implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId

    String name

    String clientSecret

    // need to be denormalized so levels can be efficiently calculated (without the need of loading all of the rules)
    int totalPoints

    @OneToMany(fetch = FetchType.LAZY, cascade = [])
    @JoinColumn(name="projRefId", insertable = false, updatable = false)
    @Where(clause = "type = 'Subject'")
    List<SkillDef> subjects

    @OneToMany(fetch = FetchType.LAZY, cascade = [])
    @JoinColumn(name="projRefId", insertable = false, updatable = false)
    @Where(clause = "type = 'Badge'")
    List<SkillDef> badges

    @OneToMany(fetch = FetchType.LAZY, cascade = [])
    @JoinColumn(name="projRefId", insertable = false, updatable = false)
    List<CustomIcon> customIcons
}
