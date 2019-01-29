package skills.storage.model

import groovy.transform.ToString
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

import javax.persistence.*
/**
 * Created with IntelliJ IDEA.
 * Date: 11/29/18
 * Time: 1:55 PM
 */
@Entity
@Table(name='custom_icons')
@ToString(includeNames = true)
class CustomIcon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId;

    String filename;

    @Lob
    byte[] imageContent;

    @Column(columnDefinition = 'TEXT')
    String dataUri;

    String contentType;

    String url;

    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    Date updated
}
