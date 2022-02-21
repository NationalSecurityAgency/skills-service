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
package skills.profile

import callStack.profiler.CProf
import callStack.profiler.ProfileEvent
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice


@ControllerAdvice
@Slf4j
@CompileStatic
class ReportServetrTimingResponseBodyAdvice implements ResponseBodyAdvice {

    @Value('#{"${skills.prof.serverTimingAPI.enabled:false}"}')
    boolean enabled

    @Value('#{"${skills.prof.enabled:false}"}')
    boolean profEnabled

    @Override
    boolean supports(MethodParameter returnType, Class converterType) {
        return enabled && profEnabled;
    }

    @Override
    Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                           Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        if (enabled && profEnabled && response instanceof ServletServerHttpResponse) {
            ProfileEvent profileEvent = CProf.getRootEvent()
            if (profileEvent) {
                String[] profIdSplit = profileEvent.getName().split("profId=")
                if (profIdSplit?.length > 1) {
                    String profId = profileEvent.getName().split("profId=")[1]

                    ServletServerHttpResponse res = (ServletServerHttpResponse) (response);

                    String headerValue = "cprof;desc=cprof${profId};dur=${profileEvent.getRuntimeInMillis()}".toString()
                    res.getHeaders().set("Server-Timing", headerValue);
                }
            }
        }

        return body;
    }
}
