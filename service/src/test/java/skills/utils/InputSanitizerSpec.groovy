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
package skills.utils

import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import spock.lang.Specification

class InputSanitizerSpec extends Specification{

    def "unsanitize url with html ampersand entity"() {
        def input = "http://somewhere.foo?p=1&amp;p2=v&amp;2"

        when:
        def decoded = InputSanitizer.unsanitizeUrl(input)

        then:
        decoded == "http://somewhere.foo?p=1&p2=v&2"
    }

    def "Sanitize javascript: url"() {
        when:
        def sani2 = InputSanitizer.sanitizeUrl("javascript:alert('foo');")

        then:
        SkillException ske = thrown()
        ske.errorCode == ErrorCode.BadParam
        ske.message == "only local urls or http/https protocols are allowed"
    }

    def "Sanitize url with query parameters"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("https://foo.bar?p=1&pp=2&ppp=3")

        then:
        sanitized == "https://foo.bar?p=1&pp=2&ppp=3"
    }

    def "Sanitize url with trailing space"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("https://foo.bar?p=1&pp=2&ppp=3 ")

        then:
        sanitized == "https://foo.bar?p=1&pp=2&ppp=3"
    }

    def "Sanitize url with leading space"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl(" https://foo.bar?p=1&pp=2&ppp=3 ")

        then:
        sanitized == "https://foo.bar?p=1&pp=2&ppp=3"
    }

    def "Sanitize local url"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("/foo?p=1&pp=2&ppp=3")

        then:
        sanitized == "/foo?p=1&pp=2&ppp=3"
    }

    def "unsanitize markdown with gt html entity encoded"() {
        def input = "markdown markdown markdown &gt;blockquote markdown markdown markdown"

        when:
        def sanitized = InputSanitizer.unsanitizeForMarkdown(input)

        then:
        sanitized == "markdown markdown markdown >blockquote markdown markdown markdown"
    }

    def "sanitize url with space in params"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("http://foo.foo?a=b%20c")

        then:
        sanitized == "http://foo.foo?a=b%20c"
        sanitized != "http://foo.foo?a=b c"
    }

    def "un-sanitize url with space in params"() {
        when:
        def sanitized = InputSanitizer.unsanitizeUrl(InputSanitizer.sanitizeUrl("http://foo.foo?a=b%20c"))

        then:
        sanitized == "http://foo.foo?a=b%20c"
        sanitized != "http://foo.foo?a=b c"
    }

    def "sanitize url with space in path"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("http://foo.foo/bar%20baz")

        then:
        sanitized == "http://foo.foo/bar%20baz"
        sanitized != "http://foo.foo/bar bazc"
    }

    def "un-sanitize url with space in path"() {
        when:
        def unsani = InputSanitizer.unsanitizeUrl(InputSanitizer.sanitizeUrl("http://foo.foo/bar%20baz"))

        then:
        unsani == "http://foo.foo/bar%20baz"
        unsani != "http://foo.foo/bar baz"
    }

    def "sanitize url with space in host/authority"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("http://foo%20space%20bar.foo")

        then:
        sanitized == "http://foo%20space%20bar.foo"
    }

    def "un-sanitize url with space in host/authority"() {
        when:
        def sanitized = InputSanitizer.unsanitizeUrl(InputSanitizer.sanitizeUrl("http://foo%20space%20bar.foo"))

        then:
        sanitized == "http://foo%20space%20bar.foo"
    }

    def "sanitize url with space in fragment"() {
        when:
        def sanitized = InputSanitizer.sanitizeUrl("http://foo.foo#bar%20baz")

        then:
        sanitized == "http://foo.foo#bar%20baz"
        sanitized != "http://foo.foo#bar baz"
    }

    def "un-sanitize url with space in fragment"() {
        when:
        def unsani = InputSanitizer.unsanitizeUrl(InputSanitizer.sanitizeUrl("http://foo.foo#bar%20baz"))

        then:
        unsani == "http://foo.foo#bar%20baz"
        unsani != "http://foo.foo#bar baz"
    }

    def "un-sanitize naked ampersand"() {
        when:
        def unsani = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("A & B"))

        then:
        unsani == "A & B"
        unsani != "A &amp; B"
    }

    def "un-sanitize ampersand as part of tag entity"() {
        when:
        def unsani = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("foooo &lt;script type=\"javascript\"&gt;alert('danger');&lt;/script&gt; barrr"))

        then:
        unsani == "foooo &lt;script type=\"javascript\"&gt;alert('danger');&lt;/script&gt; barrr"
    }

    def "sanitize ampersands"() {

        when:
        def one = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("&&&&"))
        def two = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("&amp;&lt;"))
        def three = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("A&B"))
        def four = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("A & B"))
        def five = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("A& B"))
        def six = InputSanitizer.unsanitizeName(InputSanitizer.sanitize("A &B"))

        then:
        one == "&&&&"
        two == "&&lt;"
        three == "A&B"
        four == "A & B"
        five == "A& B"
        six == "A &B"
    }

    def "sanitize text with html"() {

        String text = '<skills-display version="0"></skills-display>\n<em><del>(U) one **two** three</del></em><span style="font-size: 24px;">(U) this is some text</span>'

        when:
        def one = InputSanitizer.sanitize(text)

        then:
        one == '<skills-display version="0"></skills-display>\n<del>(U) one **two** three</del>(U) this is some text'
    }

    def "sanitize markdown with html"() {

        String text = '<skills-display version="0"></skills-display>\n<em><del>(U) one **two** three</del></em><span style="font-size: 24px;">(U) this is some text</span>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == text
    }
}
