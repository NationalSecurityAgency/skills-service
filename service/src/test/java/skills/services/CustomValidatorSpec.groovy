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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
        validator.forceValidationRegex = '^\\(.+\\).*$'
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

        String paragraphs4 = """(A) Paragraph one **bold** then *italic* then ~~crossed out~~ and we are done

(A) Paragraph two **bold** then *italic* then ~~crossed out~~ and we are done

Paragraph three **bold** then *italic* then ~~crossed out~~ and we are done

(A) paragraph four **bold** then *italic* then ~~crossed out~~ and we are done
"""



        then:
        CustomValidationResult result = validator.validateDescription(paragraphs)
        !result.valid
        CustomValidationResult result2 = validator.validateDescription(paragraphs2)
        result2.valid
        CustomValidationResult result3 = validator.validateDescription(paragraphs3)
        result3.valid
        CustomValidationResult result4 = validator.validateDescription(paragraphs4)
        !result4.valid
        result4.validationFailedDetails == "Line[4] [Paragraph three bold]\n\n"
    }

    def "test indented block"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        when:
        validator.init()
        then:
        validator.validateDescription("\t(A) ok").valid
        CustomValidationResult res1 = validator.validateDescription("\t(A ok")
        !res1.valid
        res1.validationFailedDetails == "Line[0] [(A ok]\n\n"

        validator.validateDescription("(A) ok\n\t indented").valid
        validator.validateDescription("(A) ok\n\n\t indented").valid

        CustomValidationResult res2 = validator.validateDescription("(A) ok\n\n\n\t indented")
        !res2.valid
        res2.validationFailedDetails == "Line[3] [ indented]\n\n"

        validator.validateDescription("\t(A) ok\n\n\t- item 1\n\t- item-2").valid
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
        validator.forceValidationRegex = '^\\(.+\\).*$'


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

        validator.validateDescription("""(A) \n\n| (A) Locate and explain the Skills. |\n| -------------------------------------------------------------- |\n|\n""").valid

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

        validator.validateDescription("## **(**A) ok").valid
        validator.validateDescription("## **(**A)&nbsp;**S**some&nbsp;").valid
        validator.validateDescription("## \\*\\*(\\*\\*A) **S**ome").valid
        validator.validateDescription("## *(*A) great").valid
        validator.validateDescription('## *~~(~~*A) great').valid
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

        def sepRes = validator.validateDescription("""(A) Separate me

___

Separate me

---

(A) Separate me
***""")
        !sepRes.valid
        sepRes.validationFailedDetails == "Line[4] [Separate me]\n\n"

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
        validator.forceValidationRegex = '^\\(.+\\).*$'

        when:
        validator.init()

        then:
        validator.validateDescription("&gt; (A) This is a test quote").valid
        validator.validateDescription("""> (A) This is a block quote""").valid
        validator.validateDescription("(A) hello world\n" +
                "\n" +
                "\n" +
                "<br>\n" +
                "> \n" +
                "> \n" +
                "> (A) quote<em>s</em>").valid
        !validator.validateDescription("""> This is a block quote""").valid

        validator.validateDescription("""> (A) **this is ok**:
> * one
> * two """).valid

        validator.validateDescription("""> (A) **this is ok**:
>
> * one
> * two ***and** three also ***four*** and five""").valid

        validator.validateDescription("""> (A) **this is ok**:
>
> * one
> * two ***and** three also ***four*** and ***<span>five</span>*** and six""").valid

        validator.validateDescription("""> (A) **this is ok**:
>
> * one
> * two ***and** three also ***four*** and ***<span>five</span>*** and six
>
> ![This is Image](data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAD0pRVz/2Q==)""").valid

        validator.validateDescription("""> (A) **this is ok**:
>
> * one
> * two ***and** three also ***four*** and ***<span>five</span>*** and six
>
> ![This is Image](data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAD0pRVz/2Q==)![This is Image](data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAD0pRVz/2Q==)""").valid

        validator.validateDescription("""### (A) title

* item 1
* item 2\\*
* item 3

> (A) **this is ok**:
>
> * one
> * two ***and** three also ***four*** and five""").valid


        !validator.validateDescription("""> (A) this is not ok:
>
> **List 1**
>
> * one
> * two
>
> **List 2**
>
> * three
> * four""").valid

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

        then:
        validator.validateDescription(text).valid
        !validator.validateDescription(shouldFail).valid
        !validator.validateDescription(invalidListItem).valid
        !validator.validateDescription("""
                (A) fish
                (B) fish""").valid

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
        def failRes = validator.validateDescription(invalidText)
        then:

        success
        !failRes.valid
        failRes.validationFailedDetails == "Line[2] [another]\n\n"
        validator.validateDescription(styledBlockQuote).valid
    }

    def "multiple tables markdown"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

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

        validator.validateDescription("""(A) some

| First | Second |
| ----- | ------ |
| Third | Fourth |

| Fifth | Sixth |
| ----- | ------ |
| Seventh | Eighth |
""").valid

        validator.validateDescription("""<span>(A) some</span>

| First | Second |
| ----- | ------ |
| Third | Fourth |

| Fifth | Sixth |
| ----- | ------ |
| Seventh | Eighth |
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
        validator.forceValidationRegex = '^\\(.+\\).*$'

        when:
        validator.init()

        then:
        String paragraphs = "<p>(A) Paragraph 1</p><p><br></p><p>Paragraph2</p>"
        CustomValidationResult res1 = validator.validateDescription(paragraphs)
        !res1.valid
        res1.validationFailedDetails == "Failed within an html element for text [Paragraph2] after line[0]\n"

        validator.validateDescription("<span style=\\\"box-sizing: border-box; font-style: normal;\\\">(A) Very Cool Message (VCM)</span>\n<span style=\\\"box-sizing: border-box; font-style: normal;\\\">(A) message 2</span>").valid
        def res2 = validator.validateDescription("""<span style="box-sizing: border-box; font-style: normal;">(A) Items:</span>
<span style="box-sizing: border-box; font-style: normal;">          (t) (A) Item 1</span>
<span style="box-sizing: border-box; font-style: normal;">          (f) (A) Item 2</span>""")
        !res2.valid
        res2.validationFailedDetails == "Via forced validation, after line[1] [          (t) (A) It]\n\n" +
                "Via forced validation, after line[2] [          (f) (A) It]\n\n"
    }

    def "test list validation with html items"(){
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        when:

        String validList = "* <span class=\"SomeFancyText\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) One</span><span class=\"SomeFont\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"FancyFont\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Two</span><span class=\"someclass\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"Text\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Three</span><span class=\"Some\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\""
        String notValidList = "* <span class=\"SomeFancyText\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">One</span><span class=\"SomeFont\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"FancyFont\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Two</span><span class=\"someclass\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"Text\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Three</span><span class=\"Some\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\""
        String notValidList1 = "(A) one\n\n(A) two\n\n\n\n* <span class=\"SomeFancyText\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">One</span><span class=\"SomeFont\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"FancyFont\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Two</span><span class=\"someclass\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\\n\\n\\n* <span class=\"Text\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent;\">(A) Three</span><span class=\"Some\" data-ccp-props=\"{}\" style=\"margin: 0px; padding: 0px; user-select: text; -webkit-user-drag: none; -webkit-tap-highlight-color: transparent; font-size: 12pt; line-height: 20.925px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, sans-serif;\"> </span>\""

        then:
        validator.validateDescription(validList).valid
        def res = validator.validateDescription(notValidList)
        !res.valid
        res.validationFailedDetails == "Line[0] [<span class=\"SomeFan]\n\n"

        def res1 = validator.validateDescription(notValidList1)
        !res1.valid
        res1.validationFailedDetails == "Line[6] [<span class=\"SomeFan]\n\n"
    }

    def "support images" () {
        String table = """|First|Second|\n|---|---|\n|Third|Fourth|"""
        String imgStr =
                "![This is Image](data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAD0pRVz/2Q==)"

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        !validator.validateDescription(imgStr).valid
        validator.validateDescription("(A) ok\n${imgStr}").valid
        validator.validateDescription("(A) ok\n\n${imgStr}").valid
        !validator.validateDescription("(A) not\n\n\n${imgStr}").valid

        validator.validateDescription("(A) ok\n\n${imgStr}\n\n(A) ok\n\n${imgStr}\n\n(A) ok\n\n${imgStr}").valid
        !validator.validateDescription("(A) ok\n\n${imgStr}\n\n Negative\n\n${imgStr}\n\n(A) ok\n\n${imgStr}").valid
        validator.validateDescription("(A) ok\n\n${imgStr}\n\n(A) ok\n\n${table}\n\n(A) ok\n\n${imgStr}").valid
        def imageRes = validator.validateDescription("(A) ok\n\n${imgStr}\n\n(A) ok\n\n${table}\n\n(A NOT\n\n${imgStr}")
        !imageRes.valid
        imageRes.validationFailedDetails == "Line[10] [(A NOT]\n\n"
        validator.validateDescription("(A) ok\n\n${table}\n\n(A) ok\n\n${imgStr}\n\n(A) ok\n\n${table}").valid
        !validator.validateDescription("(A) ok\n\n${table}\n\n(A notok\n\n${imgStr}\n\n(A) ok\n\n${table}").valid

        validator.validateDescription("""** (A) This is the title:**
![image.png](data:image/png;base64,XXXXXXXXXXXX)""").valid

        validator.validateDescription("(A) val 1\n\n${imgStr}\n${imgStr}").valid

        validator.validateDescription("(A) **some text - image: cool image:**\n${imgStr}").valid
        def imageRes2= validator.validateDescription("(A **some text - image: cool image:**\n${imgStr}")
        !imageRes2.valid
        imageRes2.validationFailedDetails == "Line[0] [(A some text - image]\n\n"

        validator.validateDescription("(A) great\n\n\n\n**(A) some **<span sytle=\"color: 'blue'\"> - </span>**image:**\n${imgStr}").valid

        // image via external link
        validator.validateDescription("(A)\n![This is Image](https://some.path.some.png)").valid
        // image via external link wrapped with a link
        validator.validateDescription("(A)\n[![This is Image](https://some.path.some.png)](https://some.url.com)").valid

        validator.validateDescription("(A) SOME - (Ok 'OK') / [ https://some.web.com/some/page](https://some.web.com/some/page)\n${imgStr}").valid

        validator.validateDescription("""(A) ok:

| one | two |
| ----- | ------ |
| three | four |
| five | six """ + imgStr + """|
| seven | eight |
""").valid

        validator.validateDescription("(A)\n**${imgStr}**").valid
        validator.validateDescription("(A)\n\n${imgStr}").valid
        validator.validateDescription("(A)\n\n**${imgStr}**").valid
        !validator.validateDescription("(A\n\n**${imgStr}**").valid
    }

    def "support links" () {
        String url = "https://www.some.com"

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        then:
        validator.validateDescription("(A) ${url}").valid
        def invalidLink = validator.validateDescription("(A)somet\n\n${url}")
        !invalidLink.valid
        invalidLink.validationFailedDetails == "Line[2] [https://www.some.com]\n\n"

        validator.validateDescription("(A) <p>value</p>\n<p><a href=\"${url}\">${url}</a></p>").valid

        validator.validateDescription("(A)&nbsp;[A link] (http://linky.com").valid
        validator.validateDescription("(A) [A link] (http://linky.com").valid
    }

    def "support mixed html br and newline chars" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        String p1 = "**(A) First W's:**\n\n* [okd](http://ljelaj.com/dljl/dljl\n* [some (doje)](http://dlj.com/dlj)\n* [another](http://dlj.com/dlj.pdf)'\n* [eafeafe](http://dljel.com/dljld/dljdlj.pdf)"
        String p2 = "<strong>(A) Something &nbsp; &nbsp; very st'rong:</strong>\n\n* [dlaj (dljdl)](http://dlj.com/dlj/)\n"
        when:
        validator.init()
        then:
        validator.validateDescription("<strong>(A) one <br/>two<br>three </strong>").valid
        validator.validateDescription("${p1}\n\n\n<br>\n${p2}").valid
    }

    def "bad list" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String input = "*\n\n* \n* \n* one"
        when:
        validator.init()
        def res = validator.validateDescription(input)
        then:
        !res.valid
        res.validationFailedDetails == "First bullet is empty\n"
    }

    def "list under a heading" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String input = """### (A) This is a list

* first
* second
* third"""
        when:
        validator.init()
        def res = validator.validateDescription(input)
        then:
        res.valid
    }

    def "first bullet point in the list has html" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String input = "* <div>blah<span>ok</span></div>\n* two"
        String input2 = "* <div>(A) blah<span>ok</span></div>\n* two"
        when:
        validator.init()
        def res = validator.validateDescription(input)
        def res2 = validator.validateDescription(input2)
        then:
        !res.valid
        res.validationFailedDetails == "Line[0] [<div>blah<span>ok</s]\n\n"

        res2.valid
    }

    def "html list" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String input = """<ol>
    <li>(A) First item with <strong>bold text</strong></li>
    <li>Second item with <strong>another bold text</strong></li>
    <li>Third item with <strong>final bold text</strong></li>
    </ol>"""
        String input1 = """<ol>
    <li>First item with <strong>bold text</strong></li>
    <li>Second item with <strong>another bold text</strong></li>
    <li>Third item with <strong>final bold text</strong></li>
    </ol>"""
        String input2 = """<p></p><p><ol>
    <li>First item with <strong>bold text</strong></li>
    <li>Second item with <strong>another bold text</strong></li>
    <li>Third item with <strong>final bold text</strong></li>
    </ol></p>"""
        String input3 = """<p>(A) some</p><p><ol>
    <li>First item with <strong>bold text</strong></li>
    <li>Second item with <strong>another bold text</strong></li>
    <li>Third item with <strong>final bold text</strong></li>
    </ol></p>"""
        when:
        validator.init()
        def res = validator.validateDescription(input)
        def res1 = validator.validateDescription(input1)
        def res2 = validator.validateDescription(input2)
        def res3 = validator.validateDescription(input3)
        then:
        res.valid
        !res1.valid
        res1.validationFailedDetails == "Failed within an html element for text [First item with bold] after line[0]\n"
        !res2.valid
        res2.validationFailedDetails == "Failed within an html element for text [First item with bold] after line[0]\n"
        res3.valid
    }

    def "html sub lists" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String input = """(A) <p><span>Title</span></p>
<ul>
<li><span>Level 1</span>
<ul>
<li><span>Sub List Item 1</span></li>
<li><span>Sub List Item 2</span></li>
<li><span>Sub List Item 3</span>
<ul>
<li><span>Sub-sub List Item 1</span></li>
</ul>
</li>
</ul>
</li>
</ul>"""

        when:
        validator.init()
        def res = validator.validateDescription(input)
        println res.validationFailedDetails
        then:
        res.valid
    }


    def "lists and its parent" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String input = '''(A)\n<strong>Fake Heading</strong>
<li>one</li>
<li>two</li>
<strong>Fake Heading</strong>
<ol>\n<li>three</li></ol>
<strong>Fake Heading</strong>\\n<p>four</p> 
<ol> \n<li>Five</li> \n
<li>Six</li>\n
<li>Seven</li>\n
</ol>'''

        String inputB = '''(A)\n<strong>Fake Heading</strong>
<li>one</li>
<li>two</li>
<strong>Fake Heading</strong>
<ol>\n<li>(B) three</li></ol>
<strong>Fake Heading</strong>\\n<p>four</p> 
<ol> \n<li>Five</li> \n
<li>Six</li>\n
<li>Seven</li>\n
</ol>'''

        String inputC = '''(A)\n<strong>Fake Heading</strong>
<li>one</li>
<li>two</li>
<strong>Fake Heading</strong>
<ol>
    <li>three</li>
</ol>
<strong>Fake Heading</strong>\\n<p>four</p> 
<ol>
  <li>Five</li>
  <li>Six</li>
  <li>(B) Seven</li>
</ol>'''

        when:
        validator.init()
        then:
        def res = validator.validateDescription(input)
        def res1 = validator.validateDescription(inputB)
        def res2 = validator.validateDescription(inputC)
        res.valid
        !res1.valid
        res1.validationFailedDetails == "Failed within an html element for text [(B) three] after line[2]\n"
        !res2.valid
        res2.validationFailedDetails == "Failed within an html element for text [(B) Seven] after line[2]\n"


        validator.validateDescription('''(A) some text
1. item
2. item
''').valid

        validator.validateDescription('''(A) some text
1. item
2. item
- some text
''').valid

        validator.validateDescription('''(A) some text

1. item
2. item
- some text
''').valid

        validator.validateDescription('''(A) some text

1. item
2. item

- some text
''').valid

        def res4 = validator.validateDescription('''(A some text
1. item
2. item
- some text
''')
        !res4.valid
        res4.validationFailedDetails == "Line[0] [(A some text]\n" +
                "\n" +
                "Line[1] [item]\n" +
                "\n" +
                "Line[3] [some text]\n\n"

        validator.validateDescription('''(A)
some text
1. item
2. item
3. item
*some text*
- one
-two''').valid

        def res5 = validator.validateDescription('''(A)
some text
1. item
2. item
3. item

*some text*
- one
-two''')
        !res5.valid
        res5.validationFailedDetails == """Line[6] [some text]

Line[7] [one
-two]

"""

        validator.validateDescription('''(A) some text:
1.\t (A) item
2.\t (A) item
1.\t (A) item
2.\t (A) item
(A) some text
-\tone
-\ttwo''').valid

        def res6 = validator.validateDescription('''(A) some text:\n\n* ((BLAH: DLJD-LDJD))''')
        !res6.valid
        res6.validationFailedDetails == "Line[2] [((BLAH: DLJD-LDJD))]\n\n"
    }

    def "support text with multiple paragraphs and lists - test concurrency"() {
        CustomValidator validator = new CustomValidator()
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.init()

        String desc = """## (A) Main Heading\\n\\n### (A) Subheading 1\\n\\n* (A) First list item.\\n* Second list item.\\n    \\\\*data: Third list item with indentation.\\n* \\n* Fourth list item \\n\\n\\n* (A) Fifth list item.\\n* Sixth list item.\\n* Seventh list item.\\n\\n### (A) Subheading 2\\n\\n* (A) Eighth list item.\\n* Ninth list item.\\n* Tenth list item.\\n\\n### (A) Subheading 3\\n\\n* (A) Eleventh list item.\\n* Twelfth list item.\\n* Thirteenth list item.\\n\\n### (A) Subheading 4\\n\\n* (A) Fourteenth list item.\\n* Fifteenth list item.\\n* Sixteenth list item.\\n\\n(A)"""

        int threadCount = 5
        int numSubmissions = 10000
        def executor = Executors.newFixedThreadPool(threadCount)
        def results = Collections.synchronizedList([])
        def latch = new CountDownLatch(numSubmissions)

        when:
        (1..numSubmissions).each { threadNum ->
            executor.submit({
                try {
                    boolean isValid = validator.validateDescription(desc).valid
                    results.push("Thread $threadNum: ${isValid ? 'PASSED' : 'FAILED'}")
                } catch (Exception e) {
                    results.push("Thread $threadNum: ERROR - ${e.message}")
                } finally {
                    latch.countDown()
                }
            } as Runnable)
        }
        latch.await(20, TimeUnit.SECONDS) // Wait for all threads to complete with a timeout
        executor.shutdown()

        then:
        results.size() == numSubmissions
        results.every { it.endsWith('PASSED') }

    }


    def "Test force validation"(){
        CustomValidator validator = new CustomValidator();
        validator.nameValidationRegex = '^\\(A\\).*$'
        validator.nameValidationMessage = 'fail'
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'


        when:
        validator.init()
        then:
        validator.validateName("(A) some value This Cool Acronym (ACA) some parans (\"keep hydrated\"). More interesting info (but more stuff could happen)").valid
        validator.validateName("(A) **one** (AB)").valid
        validator.validateDescription("(A) one (AB)").valid
        validator.validateDescription("(A) **one** (AB)").valid
        !validator.validateDescription("(A one (AB)").valid
        !validator.validateDescription("(A **one** (AB)").valid
    }



}

