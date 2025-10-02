/**
 * Copyright 2025 SkillTree
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
package skills.services


import spock.lang.Specification

class InsertPrefixBasedOnCustomValidatorSpec extends Specification {

    def "paragraph prefix"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:
        String prefix = "(N) "

        then:
        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one

(A) Paragraph two

Paragraph three

(A) paragraph four
""", prefix).newDescription == """(A) Paragraph one

(A) Paragraph two

(N) Paragraph three

(A) paragraph four
"""
        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one

(A) Paragraph two

(A) Paragraph three

(A) paragraph four
""", prefix).newDescription == """(A) Paragraph one

(A) Paragraph two

(A) Paragraph three

(A) paragraph four
"""

        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one

(A) Paragraph two
Paragraph three

(A) paragraph four
still part of it
noe more

(A) now new
""", prefix).newDescription == """(A) Paragraph one

(A) Paragraph two
Paragraph three

(A) paragraph four
still part of it
noe more

(A) now new
"""

        validator.addPrefixToInvalidParagraphs("""Paragraph one

Paragraph two
Paragraph three

paragraph four
still part of it
noe more

now new
""", prefix).newDescription == """(N) Paragraph one

(N) Paragraph two
Paragraph three

(N) paragraph four
still part of it
noe more

(N) now new
"""
    }


    def "support markdown lists"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'


        when:
        validator.init()
        String prefix = "(A) "

        then:
        validator.addPrefixToInvalidParagraphs("""Paragraph one
* item 1
* item 2

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

* item 1
* item 2

(A) paragraph two
"""
        validator.addPrefixToInvalidParagraphs("""* item 1
* item 2
""", prefix).newDescription == """* (A) item 1
* item 2
"""

        validator.addPrefixToInvalidParagraphs("""Paragraph one
- item 1
- item 2

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

- item 1
- item 2

(A) paragraph two
"""
        validator.addPrefixToInvalidParagraphs("""- item 1
- item 2
""", prefix).newDescription == """- (A) item 1
- item 2
"""

        validator.addPrefixToInvalidParagraphs("""Paragraph one
1. item 1
1. item 2

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

1. item 1
2. item 2

(A) paragraph two
"""
        validator.addPrefixToInvalidParagraphs("""1. item 1
1. item 2
""", prefix).newDescription == """1. (A) item 1
2. item 2
"""
    }
}
