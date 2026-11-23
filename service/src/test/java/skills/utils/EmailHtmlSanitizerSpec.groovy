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
package skills.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import spock.lang.Specification

class EmailHtmlSanitizerSpec extends Specification {

    def "unsafe link schemes cannot survive HTML cleaning"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("<a href=\"${url}\">link text</a>"))

        then:
        document.select('a').text() == 'link text'
        !document.select('a').first().hasAttr('href')

        where:
        url << ['javascript:alert(1)', 'JaVaScRiPt:alert(1)', 'jav&#x61;script:alert(1)',
                'java&#x09;script:alert(1)', 'java&#x0a;script:alert(1)', 'java&#x0d;script:alert(1)',
                'vbscript:msgbox(1)', 'data:text/html,test']
    }

    def "safe absolute and relative links retain their destinations"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("<a href=\"${url}\">link text</a>"))

        then:
        document.select('a').attr('href') == url

        where:
        url << ['http://example.com', 'https://example.com', 'mailto:user@example.com',
                '/administrator/projects/proj1', 'relative/path', '#section', '//example.com/path']
    }

    def "only raster base64 data images are allowed"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("<img src=\"${url}\" onerror=\"alert(1)\">"))

        then:
        document.select('img').first().hasAttr('src') == allowed
        !document.select('img').first().hasAttr('onerror')

        where:
        url                                          | allowed
        'data:image/png;base64,aGVsbG8='              | true
        'data:image/jpeg;base64,aGVsbG8='             | true
        'data:image/gif;base64,aGVsbG8='              | true
        'data:image/webp;base64,aGVsbG8='             | true
        'data:image/svg+xml;base64,aGVsbG8='          | false
        'data:text/html;base64,aGVsbG8='              | false
        'data:image/png;base64,not valid'            | false
        'javascript:alert(1)'                        | false
        '/api/download/image.png'                    | true
        'https://example.com/image.png'              | true
    }

    def "only the editor font size CSS is allowed"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("<span style=\"${style}\">text</span>"))

        then:
        document.select('span').text() == 'text'
        document.select('span').first().hasAttr('style') == allowed

        where:
        style                                                  | allowed
        'font-size: 24px;'                                      | true
        'font-size: 12.5px'                                     | true
        'FONT-SIZE: 18PX;'                                      | true
        'color: red'                                           | false
        'font-size: 24px; background: url(https://example.com)'  | false
        'font-size: expression(alert(1))'                       | false
        'font-size: 24px; position: fixed'                      | false
    }

    def "empty HTML is left unchanged"() {
        expect:
        EmailHtmlSanitizer.sanitize(input) == input

        where:
        input << [null, '']
    }
}
