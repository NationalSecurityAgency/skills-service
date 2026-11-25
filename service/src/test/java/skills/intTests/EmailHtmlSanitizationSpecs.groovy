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
package skills.intTests

import org.jsoup.Jsoup
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.utils.WaitFor
import spock.lang.Unroll

class EmailHtmlSanitizationSpecs extends DefaultIntSpec {

    private static final String RASTER_IMAGE = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aF9sAAAAASUVORK5CYII='
    private static final String EMAIL_BODY = '''<h1>Safe title</h1>
<p><strong>bold</strong> <em>italic</em> <del>deleted</del> <span style="font-size: 24px;">large</span></p>
<ul><li>list item</li></ul>
<hr>
<table><thead><tr><th align="left">left header</th><th align="center">center header</th><th align="right">right header</th></tr></thead>
<tbody><tr><td align="left">left cell</td><td align="center">center cell</td><td align="right">right cell</td></tr></tbody></table>
<a href="https://example.com/path?one=1&amp;two=2" target="_blank" rel="noopener noreferrer">safe link</a>
<a href="mailto:user@example.com">mail link</a>
<img src="https://example.com/image.png" alt="safe image" onerror="alert(1)">
<pre><code>&lt;script&gt;literal example&lt;/script&gt;</code></pre>
<a href="javascript:alert(1)">javascript link</a>
<a href="JaVaScRiPt:alert(1)">mixed case link</a>
<a href="jav&#x61;script:alert(1)">encoded link</a>
<a href="java&#x09;script:alert(1)">tab link</a>
<a href="vbscript:msgbox(1)">vbscript link</a>
<a href="data:text/html,test">data link</a>
<img src="javascript:alert(1)">
<img src="data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9ImFsZXJ0KDEpIj48L3N2Zz4=">
<span style="background-image: url(https://example.com/tracker)">unsafe CSS</span>
<span style="font-size: 24px; background-image: url(https://example.com/tracker)">mixed CSS</span>
<p onclick="alert(1)">event handler</p>
<script>alert(1)</script><iframe src="https://example.com"></iframe>
<svg onload="alert(1)"></svg><object data="https://example.com"></object>
`<a href="javascript:alert(1)">inline backticks</a>`
```
<a href="javascript:alert(1)">fenced backticks</a>
```
'''

    def setup() {
        startEmailServer()
    }

    @Unroll
    def "#emailPath sanitizes rendered HTML without losing supported formatting"() {
        given:
        List<String> users = getRandomUsers(2)
        SkillsService admin = createService(users[0])
        SkillsService learner = createService(users[1])
        SkillsService root = createRootSkillService()
        Map project = SkillsFactory.createProject(1)
        Map subject = SkillsFactory.createSubject(1, 1)
        Map skill = SkillsFactory.createSkill(1, 1, 1, 0, 10, 480, 100)
        admin.createProject(project)
        admin.createSubject(subject)
        admin.createSkill(skill)
        learner.addSkill(skill)
        String emailBody = EMAIL_BODY + "<img src=\"${RASTER_IMAGE}\" alt=\"pasted image\">" +
                "<span style=\"font-size: 1px${' ' * 10000}!\">almost valid font size</span>"
        int expectedMessages = emailPath == 'root broadcast' ? root.countAllProjectAdminsWithEmail() : 1
        String expectedRecipient = emailPath == 'project broadcast' ? users[1] :
                emailPath == 'project preview' ? users[0] : DEFAULT_ROOT_USER_ID

        when:
        switch (emailPath) {
            case 'project broadcast':
                admin.contactProjectUsers(project.projectId, 'Safe email subject', emailBody, true)
                break
            case 'project preview':
                admin.previewEmail(project.projectId, 'Safe email subject', emailBody)
                break
            case 'root broadcast':
                root.contactAllProjectAdmins('Safe email subject', emailBody)
                break
            case 'root preview':
                root.previewEmail('Safe email subject', emailBody)
                break
        }
        assert WaitFor.wait { greenMail.receivedMessages.length == expectedMessages }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        emails.size() == expectedMessages
        emails.any { EmailUtils.EmailRes email -> email.recipients.any { String recipient -> recipient.containsIgnoreCase(expectedRecipient) } }
        emails.each { EmailUtils.EmailRes email ->
            assert email.subj == 'Safe email subject'
            assertSafeEmailHtml(email.html)
        }

        where:
        emailPath << ['project broadcast', 'project preview', 'root broadcast', 'root preview']
    }

    private static void assertSafeEmailHtml(String html) {
        Document document = Jsoup.parse(html)
        assert document.select('script, iframe, object, embed, svg, math, form, input').isEmpty()
        document.select('a').each { Element link ->
            if (!(link.text() in ['safe link', 'mail link'])) {
                assert !link.hasAttr('href')
            }
        }
        document.getAllElements().each { Element element ->
            element.attributes().each { Attribute attribute ->
                assert !attribute.key.startsWith('on')
            }
        }
        assert document.select('h1').text() == 'Safe title'
        assert document.select('strong').text() == 'bold'
        assert document.select('em').text() == 'italic'
        assert document.select('del').text() == 'deleted'
        assert document.select('li').text() == 'list item'
        assert document.select('hr').size() == 1
        assert document.select('th').collect { Element cell -> cell.text() } == ['left header', 'center header', 'right header']
        assert document.select('td').collect { Element cell -> cell.text() } == ['left cell', 'center cell', 'right cell']
        assert document.select('th').collect { Element cell -> cell.attr('align') } == ['left', 'center', 'right']
        assert document.select('td').collect { Element cell -> cell.attr('align') } == ['left', 'center', 'right']
        assert document.select('code').text() == '<script>literal example</script>'
        assert document.select('a').find { Element link -> link.text() == 'safe link' }.attr('href') == 'https://example.com/path?one=1&two=2'
        assert document.select('a').find { Element link -> link.text() == 'mail link' }.attr('href') == 'mailto:user@example.com'
        assert document.select('span[style]').size() == 1
        assert document.select('span[style]').attr('style') == 'font-size: 24px;'
        assert !document.select('span').find { Element span -> span.text() == 'almost valid font size' }.hasAttr('style')
        assert document.select('img[src]').collect { Element image -> image.attr('src') } == ['https://example.com/image.png', RASTER_IMAGE]
    }

    @Unroll
    def "#emailPath removes unsafe destinations after rendering Markdown"() {
        given:
        String user = getRandomUsers(1)[0]
        SkillsService admin = createService(user)
        SkillsService root = createRootSkillService()
        Map project = SkillsFactory.createProject(1)
        admin.createProject(project)
        String markdown = '**formatted text** and [unsafe](javascript:alert(1)) and [safe](https://example.com)\n\n---\n\nafter rule'

        when:
        if (emailPath == 'project preview') {
            admin.previewEmail(project.projectId, 'Markdown preview', markdown)
        } else {
            root.previewEmail('Markdown preview', markdown)
        }
        assert WaitFor.wait { greenMail.receivedMessages.length == 1 }
        EmailUtils.EmailRes email = EmailUtils.getEmail(greenMail)
        Document document = Jsoup.parse(email.html)

        then:
        email.subj == 'Markdown preview'
        document.select('strong').text() == 'formatted text'
        document.select('hr').size() == 1
        document.select('a').find { Element link -> link.text() == 'unsafe' }.attr('href') == ''
        document.select('a').find { Element link -> link.text() == 'safe' }.attr('href') == 'https://example.com'
        email.plainText.replace('\r\n', '\n').contains(markdown)

        where:
        emailPath << ['project preview', 'root preview']
    }
}
