/**
 * Copyright 2026 SkillTree
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
package skills.intTests.video

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import spock.lang.Unroll

import static skills.intTests.utils.SkillsFactory.*

// Small limits exercise both database-chunk and blob-stream paths with a real video fixture.
@SpringBootTest(properties = [
        'skills.config.videoStreamDefaultChunkSize=1024',
        'skills.config.videoStreamMaxOptimizedDbFetchSize=2048',
        'skills.h2.port=9098'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class VideoStreamRangeIT extends DefaultIntSpec {

    byte[] videoBytes
    String videoUrl

    def setup() {
        def project = createProject(1)
        def subject = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(project, subject, skills)
        def video = new ClassPathResource('/testVideos/create-project.webm')
        videoBytes = video.inputStream.withCloseable { it.readAllBytes() }
        assert videoBytes.length > 8192
        skillsService.saveSkillVideoAttributes(project.projectId, skills[0].skillId, [file: video])
        videoUrl = skillsService.getSkillVideoAttributes(project.projectId, skills[0].skillId).videoUrl
    }

    def 'no Range returns the complete video with download headers'() {
        when:
        def response = download(null)

        then:
        assertFullVideo(response)
    }

    @Unroll
    def 'explicit range #range returns exact bytes across fetch boundaries'() {
        when:
        def response = download(range)

        then:
        assertPartialVideo(response, start, end)

        where:
        range            | start | end
        'bytes=0-0'      | 0     | 0
        'bytes=123-123'  | 123   | 123
        'bytes=0-1023'   | 0     | 1023
        'bytes=123-2169' | 123   | 2169
        'bytes=123-2170' | 123   | 2170
        'bytes=123-2171' | 123   | 2171
        'bytes=0-4095'   | 0     | 4095
        'bytes=123-4218' | 123   | 4218
    }

    @Unroll
    def 'range ending at or beyond EOF is clamped: #endKind'() {
        given:
        long end = endKind == 'exact' ? videoBytes.length - 1L :
                endKind == 'past EOF' ? videoBytes.length + 100L : Long.MAX_VALUE
        int start = videoBytes.length - 100

        when:
        def response = download("bytes=${start}-${end}")

        then:
        assertPartialVideo(response, start, videoBytes.length - 1)

        where:
        endKind << ['exact', 'past EOF', 'Long.MAX_VALUE']
    }

    @Unroll
    def 'open range uses configured chunk size and stops at EOF: #position'() {
        given:
        int start = position == 'beginning' ? 0 : position == 'middle' ? 123 : videoBytes.length - 100

        when:
        def response = download("bytes=${start}-")

        then:
        assertPartialVideo(response, start, Math.min(start + 1023, videoBytes.length - 1))

        where:
        position << ['beginning', 'middle', 'near EOF']
    }

    @Unroll
    def 'suffix range returns the requested tail: #suffix'() {
        given:
        long length = suffix == 'larger than file' ? videoBytes.length + 100L : Long.parseLong(suffix)

        when:
        def response = download("bytes=-${length}")

        then:
        assertPartialVideo(response, (int) Math.max(0L, videoBytes.length - length), videoBytes.length - 1)

        where:
        suffix << ['1', '100', '2048', '2049', 'larger than file', '9223372036854775807']
    }

    @Unroll
    def 'unsatisfiable range returns 416 with representation size: #kind'() {
        given:
        String range = [
                'at EOF': "bytes=${videoBytes.length}-",
                'past EOF': "bytes=${videoBytes.length + 100}-",
                'explicit past EOF': "bytes=${videoBytes.length + 100}-${videoBytes.length + 200}",
                'overflowing open range': 'bytes=9223372036854775807-',
                'huge explicit start': 'bytes=9223372036854775807-9223372036854775807',
                'zero suffix': 'bytes=-0'
        ][kind]

        when:
        def response = download(range)

        then:
        response.statusCode == HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE
        response.headers.getFirst(HttpHeaders.CONTENT_RANGE) == "bytes */${videoBytes.length}"
        response.body != videoBytes

        where:
        kind << ['at EOF', 'past EOF', 'explicit past EOF', 'overflowing open range', 'huge explicit start', 'zero suffix']
    }

    // Policy: ignore malformed ranges with a full 200 response, matching the existing numeric fallback.
    // These assertions expose malformed inputs that currently throw or are partially accepted as 206.
    @Unroll
    def 'malformed range is consistently ignored: #range'() {
        when:
        def response = download(range)

        then:
        assertFullVideo(response)

        where:
        range << ['bytes=abc-def', 'bytes=0-nope', 'bytes=', 'bytes=-', 'bytes=--',
                  'bytes=100-50', 'bytes=0-1-2', 'bytes=0-1-', 'bytes=0', 'bytes=+1-10',
                  'bytes=9223372036854775808-', 'bytes=0-9223372036854775808']
    }

    @Unroll
    def 'unsupported range unit or multiple ranges falls back to full video: #range'() {
        when:
        def response = download(range)

        then:
        assertFullVideo(response)

        where:
        range << ['items=0-10', 'bytes=0-1,4-5', 'bytes=0-1, 4-5']
    }

    private ResponseEntity<byte[]> download(String range) {
        HttpHeaders headers = new HttpHeaders()
        if (range != null) {
            headers.set(HttpHeaders.RANGE, range)
        }
        def client = skillsService.wsHelper.restTemplateWrapper
        // Use the authenticated delegate so cookies, PKI, and the non-throwing error handler are retained.
        client.restTemplate.exchange("${skillsService.wsHelper.skillsService}${videoUrl}".toString(),
                HttpMethod.GET, client.getAuthEntity(byte[].class, new HttpEntity<>(headers)), byte[].class)
    }

    private void assertVideoHeaders(ResponseEntity<byte[]> response) {
        assert response.headers.getFirst(HttpHeaders.ACCEPT_RANGES) == 'bytes'
        assert response.headers.getFirst(HttpHeaders.CONTENT_TYPE) == 'video/webm'
        assert response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION) == 'inline; filename="create-project.webm"'
    }

    private void assertFullVideo(ResponseEntity<byte[]> response) {
        assert response.statusCode == HttpStatus.OK
        assertVideoHeaders(response)
        assert response.headers.getFirst(HttpHeaders.CONTENT_RANGE) == null
        assert response.headers.contentLength == videoBytes.length
        assert response.body == videoBytes
    }

    private void assertPartialVideo(ResponseEntity<byte[]> response, int start, int end) {
        assert response.statusCode == HttpStatus.PARTIAL_CONTENT
        assertVideoHeaders(response)
        assert response.headers.getFirst(HttpHeaders.CONTENT_RANGE) == "bytes ${start}-${end}/${videoBytes.length}"
        assert response.headers.contentLength == end - start + 1
        assert response.body == Arrays.copyOfRange(videoBytes, start, end + 1)
    }
}
