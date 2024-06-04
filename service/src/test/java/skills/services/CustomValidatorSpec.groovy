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

class CustomValidatorSpec extends Specification {

    def "Test custom name validation"(){
        CustomValidator validator = new CustomValidator();
        validator.nameValidationRegex = '^\\(A\\).*$'
        validator.nameValidationMessage = 'fail'
        validator.init()

        when:
        CustomValidationResult result = validator.validateName("(A)name")
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
        CustomValidationResult result = validator.validateName("(A)name")

        then:
        result.valid
    }

    def "Test custom name validation value between parens"(){
        CustomValidator validator = new CustomValidator();
        validator.nameValidationRegex = '(?i)^(?!\\s*[(].*?(?:BD|BAD)[^()]*?[)]).*$'
        validator.nameValidationMessage = 'fail'

        when:
        validator.init()

        then:

        validator.validateName("(B)name").valid
        validator.validateName("(A) name").valid
        !validator.validateName("(BD)name").valid
        !validator.validateName(" (BD)name").valid
        !validator.validateName("(BAD)name").valid
        !validator.validateName("  (BAD)name").valid
        validator.validateName("(A) name (C)").valid
        validator.validateName("(A) name BAD (C)").valid
        !validator.validateName("(A) name (BAD) (C)").valid
    }

    def "test custom paragraph validation"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:

        String paragraphs = """(A) Paragraph one

(A) Paragraph two

Paragraph three

(A) paragraph four
"""

        String paragraphs2 = """(A) Paragraph one

(A) Paragraph two

(A) Paragraph three

(A) paragraph four
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

        String paragraphs = """(A) Paragraph one

(A) Paragraph two

Paragraph three

(A) paragraph four
"""
        CustomValidationResult result = validator.validateDescription(paragraphs)

        then:
        result.valid
    }

    def "ignore blank values"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'

        validator.nameValidationRegex = '^\\(A\\).*$'

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
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'


        when:
        validator.init()

        then:
        validator.validateDescription("""(A) Paragraph one
* item 1
* item 2

(A) paragraph two
""").valid

        validator.validateDescription("""(A) Paragraph one
* item 1
* item 2

(A) paragraph two
- item 1
- item 2
""").valid

        validator.validateDescription("""(A) Paragraph one
* item 1
* item 2




(A) paragraph two
- item 1
- item 2


""").valid

        validator.validateDescription("""(A) Paragraph one

* item 1
* item 2

(A) paragraph two
- item 1
- item 2
""").valid


        validator.validateDescription("""(A) Paragraph one

(A)
* item 1
* item 2

(A) paragraph two
- item 1
- item 2
""").valid
    }

    def "support markdown tables"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'


        when:
        validator.init()

        then:
        validator.validateDescription("""(A) Paragraph one
| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

(A) paragraph two
""").valid

        validator.validateDescription("""(A) Paragraph one

| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

(A) paragraph two
""").valid

        !validator.validateDescription("""(A) Paragraph one


| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

(A) paragraph two
""").valid

        validator.validateDescription("""(A) Paragraph one
(A)
| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |

(A) paragraph two
""").valid

        validator.validateDescription("""(A)

| heading 1 | heading 2 | heading 3 |
| --------- | :-------: | --------- |
| row 1-A | row 1-B | row 1-C |
| row 2-A | lots of text centering in the middle | row 2-C<br><br><br>a few newlines |


<br>
<br>
<br>
<br>
(A) new sentence after a few new lines
```""").valid

        validator.validateDescription("""(A) dsfdsdf

|  |  |
| --- | --- |
|  | <br> |


<br>
""").valid
    }

    def "support markdown codeblocks"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'


        when:
        validator.init()

        then:
        validator.validateDescription("""(A) Paragraph one
```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""").valid

        !validator.validateDescription("""(A) Paragraph one
```
if (a == true) {
  (B) println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""").valid

        validator.validateDescription("""(A) Paragraph one
```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""").valid

        !validator.validateDescription("""(A) Paragraph one

if (a == true) {
  println 'Hello <br> <br /> World'
}

(A) paragraph two
""").valid

        !validator.validateDescription("""(A) Paragraph one


```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""").valid

        validator.validateDescription("""(A) Paragraph one
(A)
```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""").valid

        validator.validateDescription("""(A)

```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```


<br>
<br>
<br>
<br>
(A) new sentence after a few new lines
```""").valid

        validator.validateDescription("""(A)
```
<template>
</template>
```

""").valid

        validator.validateDescription("""(A)

```
line one

line two
```

""").valid

        !validator.validateDescription("""(A)

```
line one

(B)
line two
```

""").valid

        validator.validateDescription("""(A) empty
```

```""").valid
    }

    def "support markdown headers"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""# (A) Paragraph one""").valid
        validator.validateDescription("""## (A) Paragraph one""").valid
        validator.validateDescription("""### (A) Paragraph one""").valid
        validator.validateDescription("""#### (A) Paragraph one""").valid
        validator.validateDescription("""#### (A) ## Paragraph ## one ###""").valid

        !validator.validateDescription("""# Paragraph one""").valid
        !validator.validateDescription("""## Paragraph one""").valid
        !validator.validateDescription("""### Paragraph one""").valid
        !validator.validateDescription("""#### Paragraph one""").valid
    }

    def "ignore markdown separators"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""(A) Separate me
        ___
        (A) Separate me
        ---
        (A) Separate me
        ***""").valid

        !validator.validateDescription("""(A) Separate me
        ___
        Separate me
        ---
        (A) Separate me
        ***""").valid

        !validator.validateDescription("""(A) Separate me
        ___
        (A) Separate me
        ---
        (A) Separate me
        ***
        no go""").valid

        validator.validateDescription("""(A) Separate me
___
        (A) Separate me
---
        (A) Separate me
***
        
        
        ___
        
        (A) Separate me
        
        
---
        
        
        (A) Separate me
        
        
        ***
(A)

```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```


<br>
<br>
<br>
<br>
(A) new sentence after a few new lines
```""").valid

        validator.validateDescription("""(A) this is text\n\n(A)\n\n***\n\n<br>\n""").valid
        validator.validateDescription("""***""").valid
    }

    def "markdown Blockquotes should be considered during validation"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""> (A) This is a block quote""").valid
        validator.validateDescription("""(A) hello world\n" +
                "\n" +
                "\n" +
                "<br>\n" +
                "> \n" +
                "> \n" +
                "> (A) quote<em>s</em>""").valid
        !validator.validateDescription("""> This is a block quote""").valid
    }

    def "apply paragraph validator to bulleted/numbered lists"() {
        String text = """
        (A) fish
        (A) fish

        * (A) fish
        * Not a fish 

        - (A) fish
        - Not a fish

        1. (A) fish
        1. Not a fish

        2. (A) fish
        3. Not a fish
            - (A) fish
        """

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String shouldFail = """
        (A) fish
        (A) fish

        * Not (A) fish
        * Not a fish 
        """

        String invalidListItem = """
        (A) fish
        (A) fish

        * (A) fish
        * (B) Not a fish 

        - (A) fish
        - Not a fish

        1. (A) fish
        1. (B) Not a fish

        2. (A) fish
        3. Not a fish
            - (A) fish
        """

        when:
        validator.init()

        boolean success = validator.validateDescription(text).valid
        boolean shouldBeInvalid = validator.validateDescription(shouldFail).valid
        boolean shouldBeInvalid2 = validator.validateDescription(invalidListItem).valid
        boolean shouldBeInvalid3 = validator.validateDescription("""
                (A) fish
                (B) fish""").valid
        then:
        success
        !shouldBeInvalid
        !shouldBeInvalid2
        !shouldBeInvalid3
    }

    def "force single newline paragraph validation" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()
        boolean shouldBeValid = validator.validateDescription("""
                (A) fish
                (B) fish""").valid

        validator.forceValidationRegex = '^\\(.+\\).*$'
        validator.init()
        boolean shouldBeInvalid = validator.validateDescription("""
                (A) fish
                (B) fish""").valid
        then:
        shouldBeValid
        !shouldBeInvalid
    }

    def "ignore bold/italics at the beginning of a line"() {
        String text = """

*(A)* an italic at the beginning of a line

**(A)** bold at the  beginning of a line

***(A)*** bold and italic at the  beginning of a line

*(A) an italic sentence*

**(A) bold sentence**

***(A) bold and italic sentence***

*(A) an italic words* not preceded by spaces

**(A) bold words** not preceded by spaces

***(A) bold and italic words** sentence not preceded by spaces

        *(A)* an italic at the beginning of a line preceded by spaces
        
        **(A)** bold at the  beginning of a line preceded by spaces
        
        ***(A)*** bold and italic at the  beginning of a line preceded by spaces
        
        *(A) an italic sentence preceded by spaces*
        
        **(A) bold sentence preceded by spaces**
        
        ***(A) bold and italic sentence preceded by spaces***
        
        *(A) an italic words* preceded by spaces
        
        **(A) bold words** preceded by spaces
        
        ***(A) bold and italic words** sentence preceded by spaces
        """

        String invalidText = """(A) simple lined

***another***"""

        String styledBlockQuote = """> **(A)** this is a quote<em>s</em>"""

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        boolean success = validator.validateDescription(text).valid
        boolean fail = validator.validateDescription(invalidText).valid
        then:

        success
        !fail
        validator.validateDescription(styledBlockQuote).valid
    }

    def "ignore extra html markdown"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("""(A) this is some normal text

<em>(A) <del>cool</del>, not cools</em>""").valid

        validator.validateDescription("""<em>(A) <del>cool</del>, not cools</em>""").valid

        validator.validateDescription("""*(A) cool, not cool*""").valid

        validator.validateDescription("""<em>(A) <strong>cool</strong>, not cools</em>""").valid

        validator.validateDescription("""<span style="font-size: 24px;">(A) this is some text</span>""").valid

        validator.validateDescription("""(A) ok

<strong>(A) should ~~work~~ yes</strong>""").valid

        validator.validateDescription("""(A) normal

<em>(A) <del>cool</del>, not cool</em>""").valid

        validator.validateDescription('<span style=\"font-size:16.0pt;\nline-height:107%\">(A) fancy formatting</span>').valid
        validator.validateDescription("""<span style="font-size:16.0pt;
line-height:107%">(A) fancy formatting</span>""").valid

        validator.validateDescription("""### (A) this is a heading""").valid
        validator.validateDescription("""### <em class="some-class" unknown>(A)</em> this is a heading""").valid
        validator.validateDescription("""### <strong><em>(A)</em></strong> this is a heading""").valid
        validator.validateDescription("""### <strong blah href="something" class="xyz" dah>(A)</strong> this is a heading""").valid
        validator.validateDescription("""### <em><strong>(A)</strong></em> this is a heading""").valid
        validator.validateDescription("""### <EM><strong>(A)</strong></em> this is a heading""").valid
        validator.validateDescription("""### <EM><unknown>(A)</unkNoWn></em> this is a heading""").valid
        validator.validateDescription('''**<span style="font-size:18.0pt;line-height:107%;font-family:'Adobe Song Std L', serif;\\ncolor:red">(A) THIS IS (A) TEST</span>**''').valid
        validator.validateDescription('''**<span style=\\"font-size:18.0pt;line-height:107%;font-family:'Adobe Song Std L', serif;\\ncolor:red\\">(A) THIS IS (A) TEST</span>**''').valid
        validator.validateDescription("""<span style="font-size: 14px;">(A) line with </span>[<span style="font-size: 14px;">a link</span>](http://link.com)<span style="font-size: 14px;"> in the middle</span>

<span style="font-size: 14px;">(A) line with a new line above</span>""").valid
        validator.validateDescription('''### (A) [Lorem ipsum dolor sit amet, consectetur adipiscing elit (div, p, span ... - BlahBlah](https://www.blahblah.com/en/kb/htmlcss/how-to-do-stuff-link.html)

<span class="dsfgfdsgsdg" style="color: rgb(00, 11, 22); font-size: 14px; max-width: 200px; display: block; line-height: 20px; white-space: nowrap;">(A) arclab.com</span>
(A) [https://www.blahblah.com<span class="dgfdsgdsd dfgdsfg" role="text" style="color: rgb(33, 44, 55);"> › htmlcss › how-to-do-stuff-...</span>](https://www.blahblah.com/en/kb/htmlcss/how-to-do-stuff-link.html)
<span>quis nostrud exercitation ullamco laboris nisi ut aliquip ex </span><em><span>ea commodo consequat</span></em><span>. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur " ...</span>''').valid
    }

    def "test html paragraph validation"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:

        String paragraphs = "<p>(A) Paragraph 1</p><p><br></p><p>Paragraph2</p>"
        CustomValidationResult result = validator.validateDescription(paragraphs)

        then:
        !result.valid
    }
}

