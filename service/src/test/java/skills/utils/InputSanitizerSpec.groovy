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
import spock.lang.Unroll

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

    def "keep html in codeblocks"() {

        String text = '<remove><some/>\n```\n<some/>\n```\n <not-keep></not-keep>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '''
```
<some/>
```
 '''
    }

    def "keep html in multiple codeblocks"() {

        String text = '''
one  `<some/>` fun
```
<some/>
```
two  `<some/>` fun
```
<some/>
```
three
- one `<some/>` fun
- two `<some/>` fun
```
<some/>
```
more  `<some/>` fun
more  `<some/>` fun
more  `<some/>` fun
more  `<some/>` fun 
```
<some/>
```
'''

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == text
    }


    def "keep html in inline codeblocks"() {

        String text = '<remove>some text `<html/>` more text <not-keep></not-keep>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == 'some text `<html/>` more text '
    }

    def "strip html outside codeblocks but preserve inside"() {
        String text = 'blah <script>alert("xss")</script> Some text `<iframe>safe</iframe>` more text <object data="test">'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == 'blah  Some text `<iframe>safe</iframe>` more text '
    }

    def "preserve javascript in multiline codeblocks"() {
        String text = '<remove>```javascript\nfunction test() {\n  console.log("<div>test</div>");\n  return "<script>alert(1)</script>";\n}\n```</remove> <not-keep></not-keep>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '```javascript\nfunction test() {\n  console.log("<div>test</div>");\n  return "<script>alert(1)</script>";\n}\n``` '
    }

    def "preserve html in multiline codeblocks"() {
        String text = '<remove>```html\n<div class="test">\n  <span>content</span>\n  <script>alert("test")</script>\n</div>\n```</remove> <not-keep></not-keep>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '```html\n<div class="test">\n  <span>content</span>\n  <script>alert("test")</script>\n</div>\n``` '
    }

    def "multiple inline codeblocks in same input"() {
        String text = 'Start `<iframe>first</iframe>` middle `<script>second</script>` end `<object>third</object>` <remove>strip</remove>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == 'Start `<iframe>first</iframe>` middle `<script>second</script>` end `<object>third</object>` strip'
    }

    def "multiple multiline codeblocks in same input"() {
        String text = '''<remove>First block:
```html
<iframe>first</iframe>
<script>alert(1)</script>
```
Middle text <strip>remove</strip>
Second block:
```javascript
console.log("<object>test</object>");
return "<embed>test</embed>";
```
End <remove>strip</remove>'''

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '''First block:
```html
<iframe>first</iframe>
<script>alert(1)</script>
```
Middle text remove
Second block:
```javascript
console.log("<object>test</object>");
return "<embed>test</embed>";
```
End strip'''
    }

    def "mixed inline and multiline codeblocks"() {
        String text = '''<remove>Start</remove> text `<iframe>inline</iframe>` middle <strip>remove</strip>

```html
<object class="multiline">
  <embed>content</embed>
  <script>alert("test")</script>
</object>
```

End text `<script>inline-js</script>` <remove>strip</remove>'''

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '''Start text `<iframe>inline</iframe>` middle remove

```html
<object class="multiline">
  <embed>content</embed>
  <script>alert("test")</script>
</object>
```

End text `<script>inline-js</script>` strip'''
    }

    def "complex nested html in codeblocks"() {
        String text = '''<remove>Before</remove>
`<iframe onclick="alert('test')">click me</iframe>`
Middle <strip>remove</strip>
```html
<!DOCTYPE html>
<html>
<head>
  <title>Test</title>
  <script>
    function test() {
      document.getElementById("demo").innerHTML = "<object>changed</object>";
    }
  </script>
</head>
<body>
  <object id="demo">
    <embed>original</embed>
  </object>
  <button onclick="test()">Click me</button>
</body>
</html>
```
After <remove>strip</remove>'''

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '''Before
`<iframe onclick="alert('test')">click me</iframe>`
Middle remove
```html
<!DOCTYPE html>
<html>
<head>
  <title>Test</title>
  <script>
    function test() {
      document.getElementById("demo").innerHTML = "<object>changed</object>";
    }
  </script>
</head>
<body>
  <object id="demo">
    <embed>original</embed>
  </object>
  <button onclick="test()">Click me</button>
</body>
</html>
```
After strip'''
    }

    def "edge cases - empty codeblocks and special chars"() {
        String text = '<remove>test</remove> `` `<iframe>test</iframe>` `` <not-keep></not-keep> ```\n``` ```<script></script>``` <strip>remove</strip>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == 'test `` `<iframe>test</iframe>` ``  ```\n``` ```<script></script>``` remove'
    }

    def "xss evasion techniques - mixed case and encoding"() {
        String text = '<SCRIPT>alert("xss")</SCRIPT> <Img Src=x OnError=alert(1)> `<script>alert("safe")</script>`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == ' <img> `<script>alert("safe")</script>`'
    }

    def "xss evasion - javascript protocol and data urls"() {
        String text = '<a href="javascript:alert(1)">click</a> <iframe src="data:text/html,<script>alert(2)</script>"> `<a href="javascript:alert(3)">safe</a>`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '<a>click</a>  `<a href="javascript:alert(3)">safe</a>`'
    }

    def "malformed code blocks - unclosed backticks"() {
        String text = '<remove>unclosed `code block <script>alert(1)</script> another `second block</remove> <iframe>outside</iframe>'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == 'unclosed `code block <script>alert(1)</script> another `second block outside'
    }

    def "malformed code blocks - triple backticks with content"() {
        String text = '<remove>```not a real code block <script>alert(1)</script> still not``` <iframe>outside</iframe> `<real>```real code block```</real>`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '```not a real code block <script>alert(1)</script> still not``` outside ````real code block````'
    }

    def "encoding bypass attempts - html entities"() {
        String text = '&lt;script&gt;alert("xss")&lt;/script&gt; <iframe>outside</iframe> `&lt;script&gt;alert("safe")&lt;/script&gt;`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '&lt;script&gt;alert("xss")&lt;/script&gt; outside `&lt;script&gt;alert("safe")&lt;/script&gt;`'
    }

    def "encoding bypass attempts - unicode and hex"() {
        String text = '\\u003cscript\\u003ealert("xss")\\u003c/script\\u003e <iframe>outside</iframe> `\\u003cscript\\u003ealert("safe")\\u003c/script\\u003e`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '\\u003cscript\\u003ealert("xss")\\u003c/script\\u003e outside `\\u003cscript\\u003ealert("safe")\\u003c/script\\u003e`'
    }

    def "nested code blocks - backticks inside code blocks"() {
        String text = '<remove>outside</remove> ```outer `inner` content <script>alert(1)</script>``` <iframe>outside</iframe> `nested ```deep``` content`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == 'outside ```outer `inner` content <script>alert(1)</script>``` outside `nested ```deep``` content`'
    }

    def "code block injection attempts - fake code blocks"() {
        String text = '<script>alert("xss")</script> ```fake code block <script>alert(2)</script>``` <iframe>outside</iframe> `real code block`'

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == ' ```fake code block <script>alert(2)</script>``` outside `real code block`'
    }

    def "large input and performance edge case"() {
        String text = '''<remove>Start</remove> ''' + 'A' * 10000 + ''' <script>alert("large")</script> ```''' + 'B' * 10000 + '''``` <iframe>end</iframe> `''' + 'C' * 10000 + '''`'''

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one.startsWith('Start AAA')
        one.contains('```' + 'B' * 10000 + '```')
        one.contains('`' + 'C' * 10000 + '`')
        !one.contains('<iframe>')
    }

    def "escaped inline block ticks are not honored"() {
        String text = "\\`\\<one>\\`\\n\\n`<two>`"

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == "\\`\\\\`\\n\\n`<two>`"
    }

    def "escaped code block ticks are not honored"() {
        String text = '''
\\`\\`\\`
<one>
\\`\\`\\`

```
<two>
```
'''

        when:
        def one = InputSanitizer.sanitizeDescription(text)

        then:
        one == '''
\\`\\`\\`

\\`\\`\\`

```
<two>
```
'''
    }

    @Unroll
    def "sanitize users' names"() {
        expect:
        InputSanitizer.sanitizeNoSafeList(input) == expected

        where:
        input                                                                                               || expected
        "some@email.com"                                                                                    || "some@email.com"

        // --- Basic Scripting & Code Injection ---
        "<script>blah</script>"                                                                             || ""
        "<script src='http://evil.com'></script>"                                                           || ""

        // --- Common HTML Formatting & Structural Tags ---
        "<b>John</b> Doe"                                                                                   || "John Doe"
        "<i>Jane</i>"                                                                                       || "Jane"
        "<h1>Title</h1>"                                                                                    || "Title"
        "<div>Paragraph</div>"                                                                              || "Paragraph"
        "Line 1<br>Line 2"                                                                                  || "Line 1Line 2" // Adjust to "Line 1 Line 2" if your code replaces tags with spaces

        // --- HTML Attributes & Inline Styles ---
        "<span style='color: red; font-size: 20px;'>Alice</span>"                                           || "Alice"
        "<p class=\"paragraph-style\" id=\"main\">Bob</p>"                                                  || "Bob"
        "<img src='avatar.jpg' alt='Profile Picture' />"                                                    || ""
        "<a href='https://example.com'>Click Here</a>"                                                      || "Click Here"

        // --- Event Handlers (XSS Vectors) ---
        "<body onload=alert('test')>Charlie"                                                                || "Charlie"
        "<img src=x onerror=alert(1)>"                                                                      || ""
        "<div onmouseover='badCode()'>Hover Me</div>"                                                       || "Hover Me"

        // --- Broken, Malformed, or Nested HTML ---
        "Missing <b Closing Tag"                                                                            || "Missing "
        "Nested <div>Tags <b>Everywhere</b></div>"                                                          || "Nested Tags Everywhere"
        "<<script></script>script>nested</script>"                                                          || "&lt;script&gt;nested"
        "<b <script></script>>Complex Bracket</b>"                                                          || "&gt;Complex Bracket"

        // --- Legitimate Name Characters (Should NOT be stripped) ---
        "O'Connor"                                                                                          || "O'Connor"
        "Jean-Luc"                                                                                          || "Jean-Luc"
        "Dr. Smith, Jr."                                                                                    || "Dr. Smith, Jr."
        "Renée"                                                                                             || "Renée" // Unicode / Accented characters

        // --- Edge Cases ---
        ""                                                                                                  || ""
        "   "                                                                                               || "   " // Or "" if your sanitizer auto-trims whitespace
        null                                                                                                || null

        // DNs
        "cn=aaa@email.foo, ou=integration tests, o=skilltree test, c=us"                                    || "cn=aaa@email.foo, ou=integration tests, o=skilltree test, c=us"
        "UID=jdoe123, CN=John Doe, OU=Employees, O=Example Corp, C=US"                                      || "UID=jdoe123, CN=John Doe, OU=Employees, O=Example Corp, C=US"
        "CN=server01.device.example.net, OU=IoT Infrastructure, O=Example Corp, C=US"                       || "CN=server01.device.example.net, OU=IoT Infrastructure, O=Example Corp, C=US"
        "CN=John Doe, E=john.doe@example.com, OU=Some, O=Example Corp, L=Fancy Junction, ST=Maryland, C=US" || "CN=John Doe, E=john.doe@example.com, OU=Some, O=Example Corp, L=Fancy Junction, ST=Maryland, C=US"
    }

}
