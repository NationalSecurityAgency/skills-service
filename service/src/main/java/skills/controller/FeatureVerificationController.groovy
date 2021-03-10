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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import skills.profile.EnableCallStackProf
import skills.services.FeatureService

@RestController
@RequestMapping("/public")
@Slf4j
@EnableCallStackProf
class FeatureVerificationController {

    @Autowired
    FeatureService featureService

    @GetMapping("/isFeatureSupported")
    public boolean isFeatureSupported(@RequestParam("feature") String feature) {
        if ("passwordreset" == feature?.toLowerCase()) {
            return featureService.isPasswordResetFeatureEnabled()
        } else if ("emailservice" == feature?.toLowerCase()) {
            return featureService.isEmailServiceFeatureEnabled()
        } else if (feature) {
            log.warn("Unrecognized feature requested [${feature}]")
        }

        return false
    }
}
