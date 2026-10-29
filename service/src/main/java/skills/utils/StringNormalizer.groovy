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

import java.util.regex.Pattern

class StringNormalizer {

    // ECMAScript WhiteSpace and LineTerminator characters used by String.prototype.trim().
    private static final String TRIM_CHARACTERS = '\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF'
    private static final Pattern SURROUNDING_WHITESPACE = Pattern.compile('\\A[' + TRIM_CHARACTERS + ']+|[' + TRIM_CHARACTERS + ']+\\z')

    /**
     * Removes surrounding whitespace using JavaScript trim semantics. Preserves null,
     * internal whitespace, casing, and HTML; this method is not an HTML sanitizer.
     */
    static String trimUnicodeWhitespace(String input) {
        return input == null ? null : SURROUNDING_WHITESPACE.matcher(input).replaceAll('')
    }
}
