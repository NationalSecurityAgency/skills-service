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

}
