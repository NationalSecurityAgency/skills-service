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
import org.jsoup.nodes.Element
import spock.lang.Specification
import spock.lang.Timeout

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
        '  font-size : 24px  ;  '                               | true
        'font-size: 24px  '                                     | true
        'color: red'                                           | false
        'font-size: 24px; background: url(https://example.com)'  | false
        'font-size: expression(alert(1))'                       | false
        'font-size: 24px; position: fixed'                      | false
    }

    def "font size styles have a small maximum length"() {
        given:
        String declaration = 'font-size: 1px'
        String style = declaration + (' ' * (length - declaration.length()))

        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("<span style=\"${style}\">text</span>"))

        then:
        document.select('span').text() == 'text'
        document.select('span').first().hasAttr('style') == allowed

        where:
        length | allowed
        128    | true
        129    | false
        100000 | false
    }

    @Timeout(5)
    def "long almost-valid font size styles are rejected without expensive matching"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("<span style=\"${style}\">text</span>"))

        then:
        document.select('span').text() == 'text'
        !document.select('span').first().hasAttr('style')

        where:
        style << [
                'font-size: 1px' + (' ' * 100000) + '!',
                'font-size: 1px;' + (' ' * 100000) + '!',
                (' ' * 100000) + 'font-size: 1px!',
                'font-size: ' + ('1' * 100000) + 'px!',
        ]
    }

    def "horizontal rules survive without executable attributes"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize('<p>before</p><hr onclick="alert(1)"><p>after</p>'))

        then:
        document.select('hr').size() == 1
        !document.select('hr').first().hasAttr('onclick')
        document.select('p').collect { Element paragraph -> paragraph.text() } == ['before', 'after']
    }

    def "only supported table column alignments are preserved"() {
        when:
        Document document = Jsoup.parse(EmailHtmlSanitizer.sanitize("""<table>
<thead><tr><th align="${alignment}">header</th></tr></thead>
<tbody><tr><td align="${alignment}" onclick="alert(1)">cell</td></tr></tbody>
</table>"""))

        then:
        document.select('th').text() == 'header'
        document.select('td').text() == 'cell'
        document.select('th, td').every { Element cell -> cell.hasAttr('align') == allowed }
        document.select('th, td').collect { Element cell -> cell.attr('align') } == (allowed ? [alignment, alignment] : ['', ''])
        !document.select('td').first().hasAttr('onclick')

        where:
        alignment            | allowed
        'left'               | true
        'center'             | true
        'right'              | true
        'LEFT'               | true
        'justify'            | false
        'javascript:alert(1)' | false
        'center; color: red'  | false
        ''                   | false
    }

    def "empty HTML is left unchanged"() {
        expect:
        EmailHtmlSanitizer.sanitize(input) == input

        where:
        input << [null, '']
    }
}
