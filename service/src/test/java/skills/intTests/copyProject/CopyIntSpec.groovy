/**
 * Copyright 2024 SkillTree
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
package skills.intTests.copyProject

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import skills.intTests.utils.DefaultIntSpec
import skills.storage.repos.AttachmentRepo
import skills.storage.repos.LevelDefRepo
import skills.utils.GroovyToJavaByteUtils

class CopyIntSpec extends DefaultIntSpec {
    @Autowired
    LevelDefRepo levelDefRepo

    @Autowired
    AttachmentRepo attachmentRepo

    def attachFileAndReturnHref(String projectId, String contents = 'Test is a test') {
        String filename = 'test-pdf.pdf'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)
        def result = skillsService.uploadAttachment(resource, projectId, null, null)
        String attachmentHref = result.href
        return attachmentHref
    }
}
