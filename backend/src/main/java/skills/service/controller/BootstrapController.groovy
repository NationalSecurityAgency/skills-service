package skills.service.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import skills.service.datastore.services.AccessSettingsStorageService

@RestController
@Slf4j
class BootstrapController {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @GetMapping('/')
    ModelAndView handleBootstrap(ModelMap model) {
        if (!accessSettingsStorageService.rootAdminExists()) {
            return new ModelAndView('redirect:/bootstrap/index.html', model)
        }
        return new ModelAndView('/index.html', model)
    }

    @GetMapping('/bootstrap')
    ModelAndView handleBootstrapPart2(ModelMap modelMap) {
        return new ModelAndView('/bootstrap/index.html', modelMap)
    }
}
