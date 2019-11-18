package skills.storage.model

import groovy.transform.ToString

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name='level_definition')
@ToString(includeNames = true)
class LevelDef implements Serializable, LevelDefInterface{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    int level
    Integer percent

    // either belongs to a project or to skill
    Integer projectRefId
    Integer skillRefId

    Integer pointsFrom
    Integer pointsTo

    @Column(name="logical_name")
    String name

    String iconClass
}
