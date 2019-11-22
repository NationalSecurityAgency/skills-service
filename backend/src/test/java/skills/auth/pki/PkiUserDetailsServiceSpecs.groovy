package skills.auth.pki

import org.springframework.beans.factory.annotation.Autowired
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.intTests.utils.DefaultIntSpec
import skills.storage.repos.UserAttrsRepo

import java.util.concurrent.atomic.AtomicInteger

class PkiUserDetailsServiceSpecs extends DefaultIntSpec {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserAttrsRepo userAttrsRepo

    def "handle loading users concurrently"() {

        PkiUserLookup pkiUserLookup = Mock(PkiUserLookup)
        pkiUserLookup.lookupUserDn(_) >> { String dn ->
            new UserInfo(
                    firstName: "First",
                    lastName: "Last",
                    nickname: "",
                    username: dn,
                    usernameForDisplay: dn,
                    userDn: dn,
            )
        }
        int numThreads = 8
        AtomicInteger exceptioncount = new AtomicInteger()
        List<String> userIdsToSave = (0..100).collect { "User${it}".toString() }
        when:
        List<Thread> threads = (1..numThreads).collect {
            Thread.start {
                PkiUserDetailsService pkiUserDetailsService = new PkiUserDetailsService(userAuthService: userAuthService, pkiUserLookup: pkiUserLookup)
                userIdsToSave.each {
                    try {
                        pkiUserDetailsService.loadUserByUsername(it)
                    } catch (Throwable t){
                        exceptioncount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        then:
        List<String> allDbUserIds = userAttrsRepo.findAll().collect({it.userId})
        userIdsToSave.each { String userIdToSearch ->
            assert allDbUserIds.contains(userIdToSearch.toLowerCase()), "[$userIdToSearch] userId does not exist in UserAttrs table"
            List<String> foundsIds = allDbUserIds.findAll({ userIdToSearch.equalsIgnoreCase(it)})
            assert foundsIds.size() == 1, "[$userIdToSearch] userId has more than 1 entry"
        }

        exceptioncount.get() == 0
    }
}
