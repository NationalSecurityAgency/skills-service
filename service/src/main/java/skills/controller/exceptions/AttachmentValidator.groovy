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
package skills.controller.exceptions

import org.apache.commons.io.FileUtils
import org.springframework.http.MediaType
import org.springframework.util.unit.DataSize

class AttachmentValidator {

    static void isWithinMaxAttachmentSize(Long fileSize, DataSize maxAttachmentSize) {
        if (fileSize > maxAttachmentSize.toBytes()) {
            throw new SkillException("File size [${FileUtils.byteCountToDisplaySize(fileSize)}] exceeds maximum file size [${FileUtils.byteCountToDisplaySize(maxAttachmentSize.toBytes())}]", ErrorCode.BadParam)
        }
    }

    static void isAllowedAttachmentMimeType(String contentType, List<MediaType> allowedAttachmentMimeTypes) {
        boolean foundAllowedMediaType = false
        try {
            foundAllowedMediaType = allowedAttachmentMimeTypes.contains(MediaType.parseMediaType(contentType))
        } catch (Exception e) { }
        if (!foundAllowedMediaType) {
            throw new SkillException("Invalid media type [${contentType}]", ErrorCode.BadParam)
        }
    }

    static boolean isAllowedAttachmentMimeTypeBoolean(String contentType, List<MediaType> allowedAttachmentMimeTypes) {
        boolean foundAllowedMediaType = false
        try {
            foundAllowedMediaType = allowedAttachmentMimeTypes.contains(MediaType.parseMediaType(contentType))
        } catch (Exception e) { }
        return foundAllowedMediaType
    }
}
