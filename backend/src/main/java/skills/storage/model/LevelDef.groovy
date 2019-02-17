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
@ToString(includeNames = true, excludes = ['projDef', 'skillDef'])
class LevelDef implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    int level
    Integer percent

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="id", insertable = false, updatable = false)
    ProjDef projDef

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name="id", insertable = false, updatable = false)
    SkillDef skillDef

    Integer pointsFrom
    Integer pointsTo

    @Column(name="logical_name")
    String name

    String iconClass
}
