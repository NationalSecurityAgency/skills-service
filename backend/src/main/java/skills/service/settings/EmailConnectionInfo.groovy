package skills.service.settings

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(excludes = ['password'])
class EmailConnectionInfo {
    String host = 'localhost'
    int port = 25
    String protocol = 'smtp'
    String username
    String password
    boolean authEnabled = false
    boolean tlsEnabled = false
}
