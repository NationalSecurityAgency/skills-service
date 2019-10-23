package skills.storage.model

import groovy.transform.ToString
import org.hibernate.annotations.Where

import javax.persistence.*

@Entity
@Table(name = 'skills_db_locks')
class SkillsDBLock implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String lock
}
