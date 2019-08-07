package skills.storage.model

import groovy.transform.ToString
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import javax.persistence.*
/**
 * Created with IntelliJ IDEA.
 * Date: 11/29/18
 * Time: 1:55 PM
 */
@Entity
@Table(name='custom_icons')
@ToString(includeNames = true, excludes = 'projDef')
@EntityListeners(AuditingEntityListener)
class CustomIcon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId;

    String filename;

    @Lob
    @Column(columnDefinition = "text")
    String dataUri;

    String contentType;

    Integer width;
    Integer height;

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="projRefId")
    ProjDef projDef

    void setProjDef(ProjDef project) {
        this.projDef = project;
        this.projDef.customIcons.add(this);
    }
}
