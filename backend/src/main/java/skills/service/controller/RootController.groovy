package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.service.datastore.services.AccessSettingsStorageService
import skills.storage.model.auth.UserRole

import java.security.Principal

@RestController
@RequestMapping('/root')
@Slf4j
class RootController {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @GetMapping('/rootUsers')
    @ResponseBody
    List<UserRole> getRootUsers() {
        return accessSettingsStorageService.getRootUsers()
    }

    @GetMapping('/users/{query}')
    @ResponseBody
    List<String> getNonRootUsers(@PathVariable('query') String query) {
        query = query.toLowerCase()
        return accessSettingsStorageService.getNonRootUsers().findAll {
            it.userId.toLowerCase().contains(query)
        }.collect {it.userId}.unique()
    }

    @GetMapping('/isRoot')
    boolean isRoot(Principal principal) {
        return accessSettingsStorageService.isRoot(principal.name)
    }

    @PutMapping('/addRoot/{userId}')
    UserRole addRoot(@PathVariable('userId') String userId) {
        return accessSettingsStorageService.addRoot(userId)
    }

    @DeleteMapping('/deleteRoot/{userId}')
    void deleteRoot(@PathVariable('userId') String userId) {
        assert accessSettingsStorageService.getRootAdminCount() > 1
        accessSettingsStorageService.deleteRoot(userId)
    }
}
