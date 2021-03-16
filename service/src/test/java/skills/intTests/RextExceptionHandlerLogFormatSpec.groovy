/**
 * Copyright 2021 SkillTree
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
package skills.intTests


import skills.controller.exceptions.RestExceptionHandler
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.utils.LoggerHelper
import skills.utils.WaitFor

class RextExceptionHandlerLogFormatSpec extends DefaultIntSpec {

    def "SkillException log formatting"() {
        LoggerHelper loggerHelper = new LoggerHelper(RestExceptionHandler.class)

        when:
        skillsService.createProject([projectId: (1..51).collect({"a"}).join(""), name: 'name'])

        WaitFor.wait {loggerHelper.hasError()}

        then:
        SkillsClientException exception = thrown()
        loggerHelper.getLogEvents().find() { it.message.contains ("POST uri=/app/projects/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")}

        cleanup:
        loggerHelper.stop()
    }

    def "MessageNotReadable log formatting"() {
        LoggerHelper loggerHelper = new LoggerHelper(RestExceptionHandler.class)
        def proj = SkillsFactory.createProject()
        def badge = SkillsFactory.createBadge()
        skillsService.createProject(proj)
        skillsService.createBadge(badge)

        when:
        StringWriter stringWriter = new StringWriter()
        Writable writable = "FooBarBaz123445".getBytes().encodeBase64()
        stringWriter.write(writable)
        String base64 = stringWriter.toString()

        skillsService.wsHelper.adminPatch(skillsService.getBadgeUrl(proj.projectId, badge.badgeId)+"?param=val", base64)

        WaitFor.wait {loggerHelper.hasError()}

        then:
        SkillsClientException exception = thrown()
        loggerHelper.getLogEvents().find() { it.message.contains ("PATCH uri=/admin/projects/TestProject1/badges/badge1, params=[param=val]")}

        cleanup:
        loggerHelper.stop()
    }
}
