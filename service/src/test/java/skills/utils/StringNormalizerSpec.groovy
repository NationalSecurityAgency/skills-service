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

import spock.lang.Specification

class StringNormalizerSpec extends Specification {

    def "trim JavaScript whitespace character #codePoint only at the edges"() {
        String whitespace = Character.toString((char) codePoint)

        expect:
        StringNormalizer.trimUnicodeWhitespace("${whitespace}Text${whitespace}") == 'Text'
        StringNormalizer.trimUnicodeWhitespace(whitespace + whitespace) == ''
        StringNormalizer.trimUnicodeWhitespace("First${whitespace}Second") == "First${whitespace}Second"

        where:
        codePoint << ((0x0009..0x000D).toList() + [0x0020, 0x00A0, 0x1680] +
                (0x2000..0x200A).toList() + [0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF])
    }

    def "trimming preserves null empty strings casing HTML and non-JavaScript whitespace"() {
        expect:
        StringNormalizer.trimUnicodeWhitespace(input) == expected

        where:
        input                         | expected
        null                          | null
        ''                            | ''
        ' \t\u00A0Text\uFEFF\n '      | 'Text'
        '  <b>Mixed CASE</b>  '        | '<b>Mixed CASE</b>'
        '\u0085Text\u0085'            | '\u0085Text\u0085'
        'Text \u0085'                 | 'Text \u0085'
        '\u180EText\u180E'            | '\u180EText\u180E'
        '\u200BText\u200B'            | '\u200BText\u200B'
    }
}
