/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.controller

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import skills.PublicProps
import skills.auth.UserInfoService
import skills.metrics.MetricsService
import skills.profile.EnableCallStackProf
import skills.services.AdminUsersService
import skills.skillLoading.SkillsLoader
import skills.skillLoading.model.MyProgressSummary

import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api")
@Slf4j
@EnableCallStackProf
class MyProgressController {

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    MetricsService metricsServiceNew

    @Autowired
    private SkillsLoader skillsLoader;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private PublicProps publicProps;

    @RequestMapping(value = "/metrics/{metricsId}", method =  RequestMethod.GET, produces = "application/json")
    def getChartData(@PathVariable("metricsId") String metricsId,
                     @RequestParam Map<String,String> metricsProps) {
        return metricsServiceNew.loadGlobalMetrics(metricsId, metricsProps)
    }

    @RequestMapping(value = "/myProgressSummary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @CompileStatic
    @Profile
    MyProgressSummary getMyProgressSummary(HttpServletRequest request,
                                           @RequestParam(name = "version", required = false) Integer version) {
        String userId = userInfoService.getCurrentUserId();
        return skillsLoader.loadMyProgressSummary(userId, getProvidedVersionOrReturnDefault(version));
    }

    private int getProvidedVersionOrReturnDefault(Integer versionParam) {
        if (versionParam != null) {
            return versionParam;
        }
        return publicProps.getInt(PublicProps.UiProp.maxSkillVersion);
    }
}
