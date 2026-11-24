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
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist

import java.util.regex.Pattern

class EmailHtmlSanitizer {

    private static final int MAX_FONT_SIZE_STYLE_LENGTH = 128
    private static final Pattern FONT_SIZE = ~/(?i)\A\s*+font-size\s*+:\s*+\d++(?:\.\d++)?px\s*+(?:;\s*+)?\z/
    private static final Pattern RASTER_IMAGE = ~/(?i)^data:image\/(?:png|jpeg|gif|webp);base64,[a-z0-9+\/]+={0,2}$/
    private static final Set<String> TABLE_ALIGNMENTS = Set.of('left', 'center', 'right')
    private static final Safelist EMAIL_SAFELIST = new EmailSafelist()
    // Used only for protocol validation; relative URLs retain their original values.
    private static final String RELATIVE_LINK_BASE = 'https://relative-link.invalid/'

    static String sanitize(String html) {
        if (!html) {
            return html
        }
        // This is rendered HTML, not Markdown: never restore unsanitized code blocks.
        return Jsoup.clean(html, RELATIVE_LINK_BASE, EMAIL_SAFELIST, new Document.OutputSettings().prettyPrint(false))
    }

    private static class EmailSafelist extends Safelist {
        EmailSafelist() {
            super(Safelist.relaxed()
                    .addTags('del', 'span', 'hr')
                    .addAttributes('a', 'target', 'rel')
                    .addAttributes('span', 'style')
                    .addAttributes('th', 'align')
                    .addAttributes('td', 'align')
                    .preserveRelativeLinks(true))
        }

        @Override
        boolean isSafeAttribute(String tagName, Element element, Attribute attribute) {
            // Keep the editor's font-size formatting, but not arbitrary inline CSS.
            if (tagName == 'span' && attribute.key == 'style') {
                // Notification rendering holds a shared lock; bound work before matching.
                return attribute.value.length() <= MAX_FONT_SIZE_STYLE_LENGTH && FONT_SIZE.matcher(attribute.value).matches()
            }
            if (tagName in ['th', 'td'] && attribute.key == 'align') {
                return TABLE_ALIGNMENTS.contains(attribute.value.toLowerCase(Locale.ROOT))
            }
            // Pasted images use data URLs; do not allow SVG or other active content.
            if (tagName == 'img' && attribute.key == 'src' && RASTER_IMAGE.matcher(attribute.value).matches()) {
                return true
            }
            return super.isSafeAttribute(tagName, element, attribute)
        }
    }
}
