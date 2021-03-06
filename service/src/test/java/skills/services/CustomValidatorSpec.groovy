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
package skills.services


import spock.lang.Specification

class CustomValidatorSpec extends Specification{

    def "Test custom name validation"(){
        CustomValidator validator = new CustomValidator();
        validator.nameValidationRegex = '^A.*$'
        validator.nameValidationMessage = 'fail'
        validator.init()

        when:
        CustomValidationResult result = validator.validateName("Aname")
        CustomValidationResult result2 = validator.validateName("name")

        then:
        result.valid
        !result2.valid
    }

    def "Test custom name validation, no regex configured"(){
        CustomValidator validator = new CustomValidator();
        validator.nameValidationRegex = ''
        validator.nameValidationMessage = 'fail'
        validator.init()

        when:
        CustomValidationResult result = validator.validateName("Aname")

        then:
        result.valid
    }

    def "test custom paragraph validation"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:

        String paragraphs = """A Paragraph one

A Paragraph two

Paragraph three

A paragraph four
"""

        String paragraphs2 = """A Paragraph one

A Paragraph two

A Paragraph three

A paragraph four
"""
        CustomValidationResult result = validator.validateDescription(paragraphs)
        CustomValidationResult result2 = validator.validateDescription(paragraphs2)

        then:
        !result.valid
        result2.valid
    }

    def "test custom paragraph validation, no regex configured"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = ''
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:

        String paragraphs = """A Paragraph one

A Paragraph two

Paragraph three

A paragraph four
"""
        CustomValidationResult result = validator.validateDescription(paragraphs)

        then:
        result.valid
    }


    def "ignore blank values"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'

        validator.nameValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("   ").valid
        validator.validateDescription("").valid
        validator.validateDescription(null).valid

        validator.validateName("   ").valid
        validator.validateName("").valid
        validator.validateName(null).valid
    }

    def "support markdown lists"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'


        when:
        validator.init()

        then:
        validator.validateDescription("""A Paragraph one
* item 1
* item 2

A paragraph two
""").valid

        validator.validateDescription("""A Paragraph one
* item 1
* item 2

A paragraph two
- item 1
- item 2
""").valid

        validator.validateDescription("""A Paragraph one
* item 1
* item 2




A paragraph two
- item 1
- item 2


""").valid

        !validator.validateDescription("""A Paragraph one

* item 1
* item 2

A paragraph two
- item 1
- item 2
""").valid


        validator.validateDescription("""A Paragraph one

A
* item 1
* item 2

A paragraph two
- item 1
- item 2
""").valid
    }

    def "support markdown tables"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'


        when:
        validator.init()

        then:
        validator.validateDescription("""A Paragraph one
| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

A paragraph two
""").valid

        !validator.validateDescription("""A Paragraph one

| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

A paragraph two
""").valid

        validator.validateDescription("""A Paragraph one
A
| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

A paragraph two
""").valid
    }

    def "support markdown headers"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""# A Paragraph one""").valid
        validator.validateDescription("""## A Paragraph one""").valid
        validator.validateDescription("""### A Paragraph one""").valid
        validator.validateDescription("""#### A Paragraph one""").valid
        validator.validateDescription("""#### A ## Paragraph ## one ###""").valid

        !validator.validateDescription("""# Paragraph one""").valid
        !validator.validateDescription("""## Paragraph one""").valid
        !validator.validateDescription("""### Paragraph one""").valid
        !validator.validateDescription("""#### Paragraph one""").valid
    }

    def "ignore markdown separators"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""A Separate me
        ___
        A Separate me
        ---
        A Separate me
        ***""").valid

        !validator.validateDescription("""A Separate me
        ___
        Separate me
        ---
        A Separate me
        ***""").valid

        !validator.validateDescription("""A Separate me
        ___
        A Separate me
        ---
        A Separate me
        ***
        no go""").valid
    }

    def "markdown Blockquotes should be considered during validation"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""> A This is a block quote""").valid
        !validator.validateDescription("""> This is a block quote""").valid
    }

    def "apply paragraph validator to bulleted/numbered lists"() {
        String text = """
        A fish
        A fish

        * A fish
        * Not a fish 

        - A fish
        - Not a fish

        1. A fish
        1. Not a fish

        2. A fish
        3. Not a fish
            - A fish
        """

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^A.*$'
        validator.paragraphValidationMessage = 'fail'

        String shouldFail = """
        A fish
        A fish

        * Not A fish
        * Not a fish 
        """

        when:
        validator.init()

        boolean success = validator.validateDescription(text).valid
        boolean shouldBeInvalid = validator.validateDescription(shouldFail).valid
        then:
        success
        !shouldBeInvalid
    }
}

