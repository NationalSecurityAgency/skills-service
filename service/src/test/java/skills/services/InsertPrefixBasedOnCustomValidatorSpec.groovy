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

        validator.addPrefixToInvalidParagraphs("""(A) some text
1. item
2. item


- some text
""", prefix).newDescription == """(A) some text

1. item
2. item

- (A) some text
"""

        validator.addPrefixToInvalidParagraphs("""(A some text
1. item
2. item
- some text
""", prefix).newDescription == """(A) (A some text

1. item
2. item

- some text
"""

    }

    def "support markdown tables"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()
        String prefix = "(A) "

        then:
        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one


|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

(A) 

|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|

(A) paragraph two
"""


        validator.addPrefixToInvalidParagraphs("""|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|
""", prefix).newDescription == """(A) 

|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|
"""

        validator.addPrefixToInvalidParagraphs("""ok
|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|
""", prefix).newDescription == """(A) ok

|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|
"""

        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one

ok
|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

(A) ok

|header 1|header 2|header 3|
|---|---|---:|
|cell 1|cell 2|cell 3|
|cell 4|cell 5 is longer|cell 6 is much longer than the others, but that's ok. It will eventually wrap the text when the cell is too large for the display size.|
|cell 7||cell <br> 9|

(A) paragraph two
"""
    }

    def "multiple tables markdown"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("""| First | Second |
| ----- | ------ |
| Third | Fourth |

| Fifth | Sixth |
| ----- | ------ |
| Seventh | Eighth |

| <br> | <br> |
| --- | --- |
| <br> | <br> |

""", prefix).newDescription == """(A) 

|First|Second|
|---|---|
|Third|Fourth|

(A) 

|Fifth|Sixth|
|---|---|
|Seventh|Eighth|

(A) 

|<br>|<br>|
|---|---|
|<br>|<br>|
"""
    }

    def "support markdown codeblocks"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("""```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```
""", prefix).newDescription == """(A) 

```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```
"""

        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one



```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

(A) 

```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
"""


        validator.addPrefixToInvalidParagraphs("""(A) Paragraph one

ok
```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
""", prefix).newDescription == """(A) Paragraph one

(A) ok

```
if (a == true) {
  println 'Hello <br> <br /> World'
}
```

(A) paragraph two
"""
    }

    def "support headings"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("""# one""", prefix).newDescription == """# (A) one
"""
        validator.addPrefixToInvalidParagraphs("""## one""", prefix).newDescription == """## (A) one
"""
        validator.addPrefixToInvalidParagraphs("""### one""", prefix).newDescription == """### (A) one
"""
        validator.addPrefixToInvalidParagraphs("""#### one""", prefix).newDescription == """#### (A) one
"""


        validator.addPrefixToInvalidParagraphs("""# one

(A) some text 1

(A) some text 2

## two

(A) some text 3

(A) some text 4

### three

(A) some text 5

(A) some text 6

# four

(A) some text 7

(A) some text 8

## five

(A) some text 9

(A) some text 10

#### six

(A) some text 11

(A) some text 12

""", prefix).newDescription == """# (A) one

(A) some text 1

(A) some text 2

## (A) two

(A) some text 3

(A) some text 4

### (A) three

(A) some text 5

(A) some text 6

# (A) four

(A) some text 7

(A) some text 8

## (A) five

(A) some text 9

(A) some text 10

#### (A) six

(A) some text 11

(A) some text 12
"""

    }

    def "ignore markdown separators"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("""Separate me

___

Separate me

---

(A) Separate me
        
***
""", prefix).newDescription == """(A) Separate me

___

(A) Separate me

---

(A) Separate me

***
"""
    }

    def "handle blockquotes"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("""> This is a block quote""", prefix).newDescription == """> (A) This is a block quote
"""

        validator.addPrefixToInvalidParagraphs("""> This is a block quote

(A) some text 

> another one

***

""", prefix).newDescription == """> (A) This is a block quote

(A) some text

> (A) another one

***
"""
    }

    def "ignore bold/italics at the beginning of a line"() {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("""*(A)* an italic at the beginning of a line""", prefix).newDescription == """*(A)* an italic at the beginning of a line
"""
        validator.addPrefixToInvalidParagraphs("""*italic* at the beginning of a line""", prefix).newDescription == """(A) *italic* at the beginning of a line
"""
        validator.addPrefixToInvalidParagraphs("""***bold and italic at the  beginning*** of a line""", prefix).newDescription == """(A) ***bold and italic at the  beginning*** of a line
"""

    }

    def "support images" () {
        String table = """|First|Second|\n|---|---|\n|Third|Fourth|"""
        String imgStr =
                "![This is Image](data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAD0pRVz/2Q==)"

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("ok\n${imgStr}", prefix).newDescription == "(A) ok\n${imgStr}\n"
        validator.addPrefixToInvalidParagraphs("ok\n\n${imgStr}", prefix).newDescription == "(A) ok\n\n${imgStr}\n"
        validator.addPrefixToInvalidParagraphs("(A) ok\n\n${imgStr}\n\nok\n\n${imgStr}\n\n(A) ok\n\n${imgStr}", prefix)
                .newDescription == "(A) ok\n\n${imgStr}\n\n(A) ok\n\n${imgStr}\n\n(A) ok\n\n${imgStr}\n"
        validator.addPrefixToInvalidParagraphs("ok\n\n${imgStr}\n\n(A) ok\n\n${table}\n\n(A) ok\n\n${imgStr}", prefix)
                .newDescription == "(A) ok\n\n${imgStr}\n\n(A) ok\n\n${table}\n\n(A) ok\n\n${imgStr}\n"
        validator.addPrefixToInvalidParagraphs("(A) ok\n\n${table}\n\nok\n\n${imgStr}\n\n(A) ok\n\n${table}", prefix)
                .newDescription == "(A) ok\n\n${table}\n\n(A) ok\n\n${imgStr}\n\n(A) ok\n\n${table}\n"
    }

    def "support url" () {
        String url = "https://www.some.com"

        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        String prefix = "(A) "
        when:
        validator.init()

        then:
        validator.addPrefixToInvalidParagraphs("${url}", prefix).newDescription == "(A) ${url}\n"
    }

    def "support html paragraphs" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'

        when:
        validator.init()

        String prefix = "(B) "
        String text = "<p>paragraph 1</p><p><br></p><p>(A) paragraph 2 </p><p><br></p><p>paragraph 3</p><p><br></p><p>(A) paragraph 4</p><p><br></p><p>paragraph 5</p>"
        String expect = '''<p>(B) paragraph 1</p>
<p><br></p>
<p>(A) paragraph 2</p>
<p><br></p>
<p>(B) paragraph 3</p>
<p><br></p>
<p>(A) paragraph 4</p>
<p><br></p>
<p>(B) paragraph 5</p>
'''

        String newDesc = validator.addPrefixToInvalidParagraphs(text, prefix).newDescription
        then:
        newDesc == expect
    }

    def "support mixed html br and newline chars" () {
        String prefix = "(A) "
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        when:
        validator.init()
        then:
        validator.addPrefixToInvalidParagraphs("<strong>one <br/>two<br>three </strong>", prefix).newDescription == "(A) <strong>one <br/>two<br>three </strong>\n"
        validator.addPrefixToInvalidParagraphs("One\n\n<br/>\ntwo", prefix).newDescription == "(A) One\n\n(A) two\n"
    }

    def "html list" () {
        CustomValidator validator = new CustomValidator();
        validator.paragraphValidationRegex = '^\\(A\\).*$'
        validator.paragraphValidationMessage = 'fail'
        validator.forceValidationRegex = '^\\(.+\\).*$'

        String prefix = "(B) "

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
        when:
        validator.init()
        def res1 = validator.addPrefixToInvalidParagraphs(input1, prefix)
        def res2 = validator.addPrefixToInvalidParagraphs(input2, prefix)
        then:
        res1.newDescription == """<ol>
 <li>(B) First item with <strong>bold text</strong></li>
 <li>Second item with <strong>another bold text</strong></li>
 <li>Third item with <strong>final bold text</strong></li>
</ol>\n"""
        res2.newDescription == """<p></p>
<p></p>
<ol>
 <li>(B) First item with <strong>bold text</strong></li>
 <li>Second item with <strong>another bold text</strong></li>
 <li>Third item with <strong>final bold text</strong></li>
</ol>
<p></p>\n"""
    }

//    def "force validation"() {
//        CustomValidator validator = new CustomValidator();
//        validator.paragraphValidationRegex = '^\\(A\\).*$'
//        validator.paragraphValidationMessage = 'fail'
//        validator.forceValidationRegex = '^\\(.+\\).*$'
//
//        String prefix = "(B) "
//        when:
//        validator.init()
//
//        then:
//        validator.addPrefixToInvalidParagraphs("""(A) some\n(some text)""", prefix).newDescription == """(A) some\n(B) (some text)"""
//    }
}
