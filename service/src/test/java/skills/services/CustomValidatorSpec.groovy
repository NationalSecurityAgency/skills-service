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

        String paragraphs3 = """(A) Paragraph one

(A) Paragraph two
Paragraph three

(A) paragraph four
still part of it
noe more

(A) now new
"""

        CustomValidationResult result = validator.validateDescription(paragraphs)
        CustomValidationResult result2 = validator.validateDescription(paragraphs2)
        CustomValidationResult result3 = validator.validateDescription(paragraphs3)

        then:
        !result.valid
        result2.valid
        result3.valid
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

        validator.validateDescription("""(A) one
* item 1
* item 2
""").valid
        validator.validateDescription("""(A) one
- item 1
- item 2
""").valid
        validator.validateDescription("""(A) one
1. item 1
1. item 2
""").valid
        !validator.validateDescription("""- item 1
- item 2
""").valid
        !validator.validateDescription("""* item 1
* item 2
""").valid
        !validator.validateDescription("""1. item 1
1. item 2
""").valid
        validator.validateDescription("""- (A) item 1
- item 2
""").valid
        validator.validateDescription("""* (A) item 1
* item 2
""").valid
        validator.validateDescription("""1. (A) item 1
1. item 2
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

        !validator.validateDescription("""| header 1 | header 2 | header 3 |
| ---      |  ------  |---------:|
| cell 1   | cell 2   | cell 3   |
| cell 4 | cell 5 is longer | cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size. |
| cell 7   |          | cell <br> 9 |
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

        validator.validateDescription("""(A) empty
```

```""").valid

        !validator.validateDescription("""(A) Paragraph one


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
  println 'Hello <br> <br /> World'
}
```
""").valid

        !validator.validateDescription("""```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```
""").valid
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
(A) one
(A) two

* (A) three
* four 

- (A) five
- six

1. (A) seven
1. eight

2. (A) nine
3. ten
    - (A) eleven
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

        !validator.validateDescription("""- (A) item 1
- (B) item 2
""").valid
        !validator.validateDescription("""* (A) item 1
* (B) item 2
""").valid
        !validator.validateDescription("""1. (A) item 1
1. (B) item 2
""").valid
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

***(A) bold and italic words*** sentence not preceded by spaces

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

    def "multiple tables markdown"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:

        validator.validateDescription("""(A)

| First | Second |
| ----- | ------ |
| Third | Fourth |

(A)

| Fifth | Sixth |
| ----- | ------ |
| Seventh | Eighth |

(A)

| <br> | <br> |
| --- | --- |
| <br> | <br> |

""").valid

    }

    def "ignore extra html markdown"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:

        validator.validateDescription("""(A)

| **<span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column1</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column2</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column3</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column4</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column5</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column6</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column7</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column8</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;">Column9</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(255, 255, 255);"> </span> |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; background-repeat: repeat-x; background-position: left bottom; background-image: var(--urlSpellingErrorV2, url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB3aWR0aD0iNXB4IiBoZWlnaHQ9IjRweCIgdmlld0JveD0iMCAwIDUgNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIj4KICAgIDwhLS0gR2VuZXJhdG9yOiBTa2V0Y2ggNTYuMiAoODE2NzIpIC0gaHR0cHM6Ly9za2V0Y2guY29tIC0tPgogICAgPHRpdGxlPnNwZWxsaW5nX3NxdWlnZ2xlPC90aXRsZT4KICAgIDxkZXNjPkNyZWF0ZWQgd2l0aCBTa2V0Y2guPC9kZXNjPgogICAgPGcgaWQ9IkZsYWdzIiBzdHJva2U9Im5vbmUiIHN0cm9rZS13aWR0aD0iMSIgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj4KICAgICAgICA8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTAxMC4wMDAwMDAsIC0yOTYuMDAwMDAwKSIgaWQ9InNwZWxsaW5nX3NxdWlnZ2xlIj4KICAgICAgICAgICAgPGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAxMC4wMDAwMDAsIDI5Ni4wMDAwMDApIj4KICAgICAgICAgICAgICAgIDxwYXRoIGQ9Ik0wLDMgQzEuMjUsMyAxLjI1LDEgMi41LDEgQzMuNzUsMSAzLjc1LDMgNSwzIiBpZD0iUGF0aCIgc3Ryb2tlPSIjRUIwMDAwIiBzdHJva2Utd2lkdGg9IjEiPjwvcGF0aD4KICAgICAgICAgICAgICAgIDxyZWN0IGlkPSJSZWN0YW5nbGUiIHg9IjAiIHk9IjAiIHdpZHRoPSI1IiBoZWlnaHQ9IjQiPjwvcmVjdD4KICAgICAgICAgICAgPC9nPgogICAgICAgIDwvZz4KICAgIDwvZz4KPC9zdmc+)); border-bottom: 1px solid transparent;">eafe</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(0, 0, 0);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; background-repeat: repeat-x; background-position: left bottom; background-image: var(--urlSpellingErrorV2, url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB3aWR0aD0iNXB4IiBoZWlnaHQ9IjRweCIgdmlld0JveD0iMCAwIDUgNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIj4KICAgIDwhLS0gR2VuZXJhdG9yOiBTa2V0Y2ggNTYuMiAoODE2NzIpIC0gaHR0cHM6Ly9za2V0Y2guY29tIC0tPgogICAgPHRpdGxlPnNwZWxsaW5nX3NxdWlnZ2xlPC90aXRsZT4KICAgIDxkZXNjPkNyZWF0ZWQgd2l0aCBTa2V0Y2guPC9kZXNjPgogICAgPGcgaWQ9IkZsYWdzIiBzdHJva2U9Im5vbmUiIHN0cm9rZS13aWR0aD0iMSIgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj4KICAgICAgICA8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTAxMC4wMDAwMDAsIC0yOTYuMDAwMDAwKSIgaWQ9InNwZWxsaW5nX3NxdWlnZ2xlIj4KICAgICAgICAgICAgPGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAxMC4wMDAwMDAsIDI5Ni4wMDAwMDApIj4KICAgICAgICAgICAgICAgIDxwYXRoIGQ9Ik0wLDMgQzEuMjUsMyAxLjI1LDEgMi41LDEgQzMuNzUsMSAzLjc1LDMgNSwzIiBpZD0iUGF0aCIgc3Ryb2tlPSIjRUIwMDAwIiBzdHJva2Utd2lkdGg9IjEiPjwvcGF0aD4KICAgICAgICAgICAgICAgIDxyZWN0IGlkPSJSZWN0YW5nbGUiIHg9IjAiIHk9IjAiIHdpZHRoPSI1IiBoZWlnaHQ9IjQiPjwvcmVjdD4KICAgICAgICAgICAgPC9nPgogICAgICAgIDwvZz4KICAgIDwvZz4KPC9zdmc+)); border-bottom: 1px solid transparent;">eafe</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(0, 0, 0);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; background-repeat: repeat-x; background-position: left bottom; background-image: var(--urlSpellingErrorV2, url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB3aWR0aD0iNXB4IiBoZWlnaHQ9IjRweCIgdmlld0JveD0iMCAwIDUgNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIj4KICAgIDwhLS0gR2VuZXJhdG9yOiBTa2V0Y2ggNTYuMiAoODE2NzIpIC0gaHR0cHM6Ly9za2V0Y2guY29tIC0tPgogICAgPHRpdGxlPnNwZWxsaW5nX3NxdWlnZ2xlPC90aXRsZT4KICAgIDxkZXNjPkNyZWF0ZWQgd2l0aCBTa2V0Y2guPC9kZXNjPgogICAgPGcgaWQ9IkZsYWdzIiBzdHJva2U9Im5vbmUiIHN0cm9rZS13aWR0aD0iMSIgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj4KICAgICAgICA8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTAxMC4wMDAwMDAsIC0yOTYuMDAwMDAwKSIgaWQ9InNwZWxsaW5nX3NxdWlnZ2xlIj4KICAgICAgICAgICAgPGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAxMC4wMDAwMDAsIDI5Ni4wMDAwMDApIj4KICAgICAgICAgICAgICAgIDxwYXRoIGQ9Ik0wLDMgQzEuMjUsMyAxLjI1LDEgMi41LDEgQzMuNzUsMSAzLjc1LDMgNSwzIiBpZD0iUGF0aCIgc3Ryb2tlPSIjRUIwMDAwIiBzdHJva2Utd2lkdGg9IjEiPjwvcGF0aD4KICAgICAgICAgICAgICAgIDxyZWN0IGlkPSJSZWN0YW5nbGUiIHg9IjAiIHk9IjAiIHdpZHRoPSI1IiBoZWlnaHQ9IjQiPjwvcmVjdD4KICAgICAgICAgICAgPC9nPgogICAgICAgIDwvZz4KICAgIDwvZz4KPC9zdmc+)); border-bottom: 1px solid transparent;">eafe</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(0, 0, 0);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; background-repeat: repeat-x; background-position: left bottom; background-image: var(--urlSpellingErrorV2, url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB3aWR0aD0iNXB4IiBoZWlnaHQ9IjRweCIgdmlld0JveD0iMCAwIDUgNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIj4KICAgIDwhLS0gR2VuZXJhdG9yOiBTa2V0Y2ggNTYuMiAoODE2NzIpIC0gaHR0cHM6Ly9za2V0Y2guY29tIC0tPgogICAgPHRpdGxlPnNwZWxsaW5nX3NxdWlnZ2xlPC90aXRsZT4KICAgIDxkZXNjPkNyZWF0ZWQgd2l0aCBTa2V0Y2guPC9kZXNjPgogICAgPGcgaWQ9IkZsYWdzIiBzdHJva2U9Im5vbmUiIHN0cm9rZS13aWR0aD0iMSIgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj4KICAgICAgICA8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTAxMC4wMDAwMDAsIC0yOTYuMDAwMDAwKSIgaWQ9InNwZWxsaW5nX3NxdWlnZ2xlIj4KICAgICAgICAgICAgPGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAxMC4wMDAwMDAsIDI5Ni4wMDAwMDApIj4KICAgICAgICAgICAgICAgIDxwYXRoIGQ9Ik0wLDMgQzEuMjUsMyAxLjI1LDEgMi41LDEgQzMuNzUsMSAzLjc1LDMgNSwzIiBpZD0iUGF0aCIgc3Ryb2tlPSIjRUIwMDAwIiBzdHJva2Utd2lkdGg9IjEiPjwvcGF0aD4KICAgICAgICAgICAgICAgIDxyZWN0IGlkPSJSZWN0YW5nbGUiIHg9IjAiIHk9IjAiIHdpZHRoPSI1IiBoZWlnaHQ9IjQiPjwvcmVjdD4KICAgICAgICAgICAgPC9nPgogICAgICAgIDwvZz4KICAgIDwvZz4KPC9zdmc+)); border-bottom: 1px solid transparent;">eafe</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(0, 0, 0);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; background-repeat: repeat-x; background-position: left bottom; background-image: var(--urlSpellingErrorV2, url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB3aWR0aD0iNXB4IiBoZWlnaHQ9IjRweCIgdmlld0JveD0iMCAwIDUgNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIj4KICAgIDwhLS0gR2VuZXJhdG9yOiBTa2V0Y2ggNTYuMiAoODE2NzIpIC0gaHR0cHM6Ly9za2V0Y2guY29tIC0tPgogICAgPHRpdGxlPnNwZWxsaW5nX3NxdWlnZ2xlPC90aXRsZT4KICAgIDxkZXNjPkNyZWF0ZWQgd2l0aCBTa2V0Y2guPC9kZXNjPgogICAgPGcgaWQ9IkZsYWdzIiBzdHJva2U9Im5vbmUiIHN0cm9rZS13aWR0aD0iMSIgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj4KICAgICAgICA8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTAxMC4wMDAwMDAsIC0yOTYuMDAwMDAwKSIgaWQ9InNwZWxsaW5nX3NxdWlnZ2xlIj4KICAgICAgICAgICAgPGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAxMC4wMDAwMDAsIDI5Ni4wMDAwMDApIj4KICAgICAgICAgICAgICAgIDxwYXRoIGQ9Ik0wLDMgQzEuMjUsMyAxLjI1LDEgMi41LDEgQzMuNzUsMSAzLjc1LDMgNSwzIiBpZD0iUGF0aCIgc3Ryb2tlPSIjRUIwMDAwIiBzdHJva2Utd2lkdGg9IjEiPjwvcGF0aD4KICAgICAgICAgICAgICAgIDxyZWN0IGlkPSJSZWN0YW5nbGUiIHg9IjAiIHk9IjAiIHdpZHRoPSI1IiBoZWlnaHQ9IjQiPjwvcmVjdD4KICAgICAgICAgICAgPC9nPgogICAgICAgIDwvZz4KICAgIDwvZz4KPC9zdmc+)); border-bottom: 1px solid transparent;">eafe</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(0, 0, 0);"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> |
| <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif;"> </span> | <span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; background-repeat: repeat-x; background-position: left bottom; background-image: var(--urlSpellingErrorV2, url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB3aWR0aD0iNXB4IiBoZWlnaHQ9IjRweCIgdmlld0JveD0iMCAwIDUgNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIj4KICAgIDwhLS0gR2VuZXJhdG9yOiBTa2V0Y2ggNTYuMiAoODE2NzIpIC0gaHR0cHM6Ly9za2V0Y2guY29tIC0tPgogICAgPHRpdGxlPnNwZWxsaW5nX3NxdWlnZ2xlPC90aXRsZT4KICAgIDxkZXNjPkNyZWF0ZWQgd2l0aCBTa2V0Y2guPC9kZXNjPgogICAgPGcgaWQ9IkZsYWdzIiBzdHJva2U9Im5vbmUiIHN0cm9rZS13aWR0aD0iMSIgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj4KICAgICAgICA8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTAxMC4wMDAwMDAsIC0yOTYuMDAwMDAwKSIgaWQ9InNwZWxsaW5nX3NxdWlnZ2xlIj4KICAgICAgICAgICAgPGcgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAxMC4wMDAwMDAsIDI5Ni4wMDAwMDApIj4KICAgICAgICAgICAgICAgIDxwYXRoIGQ9Ik0wLDMgQzEuMjUsMyAxLjI1LDEgMi41LDEgQzMuNzUsMSAzLjc1LDMgNSwzIiBpZD0iUGF0aCIgc3Ryb2tlPSIjRUIwMDAwIiBzdHJva2Utd2lkdGg9IjEiPjwvcGF0aD4KICAgICAgICAgICAgICAgIDxyZWN0IGlkPSJSZWN0YW5nbGUiIHg9IjAiIHk9IjAiIHdpZHRoPSI1IiBoZWlnaHQ9IjQiPjwvcmVjdD4KICAgICAgICAgICAgPC9nPgogICAgICAgIDwvZz4KICAgIDwvZz4KPC9zdmc+)); border-bottom: 1px solid transparent;">eafe</span><span style="margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 11pt; line-height: 18.3458px; font-family: Verdana Pro, Verdana Pro_EmbeddedFont, Verdana Pro_MSFontService, sans-serif; color: rgb(0, 0, 0);"> </span> |
""").valid

//        the below test was originally added in #2564. this examples has newlines between columns likely due to all the extra formatting.
//        the new lines causes the TABLE_FIX to no longer match, so it was fixed by adding the DOTALL flag, but that was too aggressive
//        and would continue to match treating two or more tables as a single table.  it was later determined that the user can strip the formatting
//        from the MS Word table to fix the problem.
//        validator.validateDescription("""(A) some words
//
//| **<span style="font-family:'Arial',sans-serif;
//  mso-fareast-font-family:'Times New Romain';color:black;">Heading 1</span>** | **<span style="font-family:'Arial',sans-serif;
//  mso-fareast-font-family:'Times New Romain';color:black;">Heading 2</span>** | **<span style="font-family:'Arial',sans-serif;
//  mso-fareast-font-family:'Times New Romain';color:black;">Heading 1</span>** |
//| ----- | ---------- | ---- |
//| <span style="font-family:'Arial',sans-serif;mso-fareast-font-family:
//  'Times New Romain';color:black;">Row 1: Value 1</span> | <span style="font-family:'Arial',sans-serif;mso-fareast-font-family:
//  'Times New Romain';color:black;">Row 1: Value 2</span> | <span style="font-family:'Arial',sans-serif;mso-fareast-font-family:
//  'Times New Romain';color:black;">Row 1: Value 3</span> |
//| <span style="font-family:'Arial',sans-serif;mso-fareast-font-family:
//  'Times New Romain';color:black;">Row 1: Value 1</span> | <span style="font-family:'Arial',sans-serif;mso-fareast-font-family:
//  'Times New Romain';color:black;">Row 1: Value 2</span> | <span style="font-family:'Arial',sans-serif;mso-fareast-font-family:
//  'Times New Romain';color:black;">Row 1: Value 3</span> |""").valid

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

    def "test list validation with html items"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:

        String validList = "* <span class=\"SomeFancyText\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) One</span><span class=\"SomeFont\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"FancyFont\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Two</span><span class=\"someclass\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"Text\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(U) Three</span><span class=\"Some\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\""
        String notValidList = "* <span class=\"SomeFancyText\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">One</span><span class=\"SomeFont\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"FancyFont\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Two</span><span class=\"someclass\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"Text\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(U) Three</span><span class=\"Some\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\""

        then:
        validator.validateDescription(validList)
        validator.validateDescription(notValidList)
    }


}

