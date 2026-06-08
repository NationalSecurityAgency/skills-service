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
package skills.utils

import groovy.util.logging.Slf4j
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.net.URLEncodedUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import org.owasp.encoder.Encode
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException

import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class InputSanitizer {

    private static final Pattern ALLOWED_PROTOCOLS = ~/^(?:https?:\/\/|\/).*$/
    public static final Document.OutputSettings print = new Document.OutputSettings().prettyPrint(false)

    private static final Pattern GT = ~/&gt;/
    private static final Pattern LT = ~/&lt;/
    private static final Pattern AMP = ~/&amp;/
    private static final Pattern PURE_AMP = ~/\s&amp;\s/
    private static final Pattern SPACE = ~/\s/
    private static final Pattern CAPTION_ARROW = ~/--&gt;/
    private static final String CODE_BLOCK_TYPE_MULTILINE = "MULTILINE"
    private static final String CODE_BLOCK_TYPE_INLINE = "INLINE"
    private static final Pattern CODE_BLOCK = ~/```([\s\S]*?)```/
    private static final Pattern INLINE_CODE_BLOCK = ~/(?<!\\)(?<!`)(?!___MULTILINE_CODE_BLOCK_)(?!___INLINE_CODE_BLOCK_)`([^`\n]+)`(?!`)/
    private static final SAFE_LIST_FOR_DESC = Safelist.relaxed()
            .addTags('del', 'skills-display')
            .addAttributes('skills-display', 'version')
            .addAttributes('span', 'style')

    private static final SAFE_LIST = Safelist.none()
            .addTags('del', 'skills-display')
            .addAttributes('skills-display', 'version')

    private static final SAFE_LIST_NONE = Safelist.none()

    static String sanitizeNoSafeList(String input) {
        return sanitize(input, SAFE_LIST_NONE)
    }

    static String sanitize(String input, Safelist safelist = SAFE_LIST) {
        if (!input) {
            return input;
        }

        return Jsoup.clean(input, "", safelist, print)
    }

    private static class CodeBlockMatch {
        int start
        int end
        String type
        int index
    }
    static String sanitizeDescription(String input) {
        if (!input) {
            return input;
        }
        log.trace("Input: {}", input)

        // Find all code blocks (both inline and multiline) in order
        List<String> codeBlocks = []
        String processedInput = input
        
        // Collect all code blocks with their positions
        List<CodeBlockMatch> codeBlockMatches = []
        
        // Find multi-line code blocks first
        Matcher multilineMatcher = CODE_BLOCK.matcher(input)
        while (multilineMatcher.find()) {
            codeBlocks.add(multilineMatcher.group(0))
            codeBlockMatches.add(new CodeBlockMatch(
                start: multilineMatcher.start(),
                end: multilineMatcher.end(),
                type: CODE_BLOCK_TYPE_MULTILINE,
                index: codeBlocks.size() - 1
            ))
        }
        
        // Find inline code blocks, avoiding those inside multi-line blocks
        Matcher inlineMatcher = INLINE_CODE_BLOCK.matcher(input)
        while (inlineMatcher.find()) {
            String inlineBlock = inlineMatcher.group(0)
            int inlineStart = inlineMatcher.start()
            int inlineEnd = inlineMatcher.end()
            
            // Check if this inline block is inside any multi-line block
            boolean insideMultiline = codeBlockMatches.any { CodeBlockMatch match ->
                inlineStart >= match.start && inlineEnd <= match.end
            }
            
            if (!insideMultiline) {
                codeBlocks.add(inlineBlock)
                codeBlockMatches.add(new CodeBlockMatch(
                    start: inlineStart,
                    end: inlineEnd,
                    type: CODE_BLOCK_TYPE_INLINE,
                    index: codeBlocks.size() - 1
                ))
            }
        }
        
        // Sort matches by position and replace from end to beginning
        codeBlockMatches.sort { a, b -> a.start <=> b.start }
        Collections.reverse(codeBlockMatches)
        
        // Replace all code blocks with placeholders
        for (CodeBlockMatch match : codeBlockMatches) {
            String placeholder = "___${match.type}_${match.index}___"
            processedInput = processedInput.substring(0, match.start) + placeholder + 
                           processedInput.substring(match.end)
        }
        log.trace("After code blocks replacement: {}", processedInput)

        // Sanitize the content without code blocks
        String sanitized = Jsoup.clean(processedInput, "", SAFE_LIST_FOR_DESC, print)
        log.trace("After sanitization: {}", sanitized)

        // Restore code blocks in original order
        Collections.reverse(codeBlockMatches)
        for (CodeBlockMatch match : codeBlockMatches) {
            String placeholder = "___${match.type}_${match.index}___"
            sanitized = sanitized.replace(placeholder, codeBlocks[match.index])
        }

        log.trace("Result: {}", sanitized)
        return sanitized
    }

    static String unSanitizeCaption(String input) {
        if (!input) {
            return input;
        }

        return CAPTION_ARROW.matcher(input).replaceAll("-->")
    }

    static String sanitizeUrl(String uri) {
        if (!uri) {
            return uri;
        }
        uri = uri.trim()
        Matcher m = ALLOWED_PROTOCOLS.matcher(uri)
        if (!m.matches()) {
            SkillException ske = new SkillException("only local urls or http/https protocols are allowed")
            ske.errorCode = ErrorCode.BadParam
            throw ske
        }

        try {
            URI u = new URI(handleSpacesInUrl(uri))
            String scheme = u.getScheme()
            String authority = u.getAuthority()
            String userInfo = u.getUserInfo()
            String path = u.getPath()
            String queryString = u.getQuery()
            String fragment = u.getFragment()

            StringBuilder reassembled = new StringBuilder()
            if (scheme) {
                reassembled.append(scheme).append("://")
            }
            if (userInfo) {
                reassembled.append(userInfo).append("@")
            }
            if (authority) {
                authority = SPACE.matcher(authority).replaceAll("%20")
                reassembled.append(authority)
            }
            if (path) {
                path = SPACE.matcher(path).replaceAll("%20")
                reassembled.append(path)
            }
            if (queryString) {
                List<NameValuePair> params = URLEncodedUtils.parse(u, Charset.forName("utf-8"))
                Iterator<NameValuePair> itr = params.iterator()
                reassembled.append("?")
                while (itr.hasNext()) {
                    NameValuePair paramPair = itr.next()
                    reassembled.append(paramPair.name).append("=")
                    reassembled.append(Encode.forUriComponent(paramPair.value))
                    if (itr.hasNext()) {
                        reassembled.append("&")
                    }
                }
            }
            if (fragment) {
                fragment = SPACE.matcher(fragment).replaceAll("%20")
                reassembled.append("#").append(fragment)
            }

            return reassembled.toString()
        } catch (URISyntaxException e) {
            SkillException ske = new SkillException("url [$uri] is invalid: ${e.getMessage()}")
            ske.errorCode = ErrorCode.BadParam
            throw ske
        }
    }

    private static String handleSpacesInUrl(String url) {
        String res = url
        if (url.startsWith("http")) {
            // 8 is the index after https:// and http://
            int foundIndex = url.indexOf("/", 8)
            if (foundIndex > 0) {
                String firstPart = url.substring(0, foundIndex)
                String secondPart = url.substring(foundIndex).replaceAll(" ", "%20")
                res = firstPart + secondPart
            }
        } else {
            res = url.replaceAll(" ", "%20")
        }
        return res

    }

    /**
     * JSOUP sanitization replaced all ampersands in a url string with the html encoded entity version
     * this breaks some receiving systems. To maintain backwards compatibility with existing datasets,
     * replace all &amp; in the url with the & literal.
     * @param input
     * @return
     */
    static unsanitizeUrl(String input) {
        if (!input) {
            return input;
        }

        def clean = AMP.matcher(input).replaceAll("&")
        return clean
    }

    /**
     * is #sanitize method is used for markdown before returning the payload back
     * to the client remove sanitizion that breaks proper display of markdown
     */
    static String unsanitizeForMarkdown(String input) {
        if (!input) {
            return input;
        }

        return GT.matcher(input).replaceAll(">")
    }

    /**
     * Replaces a html ampersand entity that has no preceeding or proceeding characters
     * with a plain &
     * @param name
     * @return
     */
    static String unsanitizeName(String input) {
        if (!input) {
            return input
        }

        return AMP.matcher(input).replaceAll("&")
    }

    static String unsanitizeEscapedHtml(String input) {
        if (!input) {
            return input;
        }

        String sanitized = GT.matcher(input).replaceAll(">");
        sanitized = AMP.matcher(sanitized).replaceAll("&");
        sanitized = LT.matcher(sanitized).replaceAll("<");

        return sanitized;
    }
}
