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

import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import org.owasp.encoder.Encode
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException

import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

class InputSanitizer {

    private static final Pattern ALLOWED_PROTOCOLS = ~/^(?:https?:\/\/|\/).*$/
    public static final Document.OutputSettings print = new Document.OutputSettings().prettyPrint(false)

    private static final Pattern GT = ~/&gt;/
    private static final Pattern AMP = ~/&amp;/

    static String sanitize(String input) {
        if (!input) {
            return input;
        }

        return Jsoup.clean(input, "", Whitelist.basic(), print)
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
            URI u = new URI(uri)
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
                reassembled.append(authority)
            }
            if (path) {
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
                reassembled.append("#").append(fragment)
            }

            return reassembled.toString()
        } catch (URISyntaxException e) {
            SkillException ske = new SkillException("url [$uri] is invalid: ${e.getMessage()}")
            ske.errorCode = ErrorCode.BadParam
            throw ske
        }
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

        return AMP.matcher(input).replaceAll("&")
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
}
