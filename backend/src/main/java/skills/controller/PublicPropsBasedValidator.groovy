package skills.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.PublicProps
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator

@Component
class PublicPropsBasedValidator {

    @Autowired
    PublicProps publicProps

    void validateMaxStrLength(PublicProps.UiProp prop, String fieldName, String value) {
        if (value) {
            int maxLength = publicProps.getInt(prop)
            if (value.length() > maxLength) {
                throw new SkillException("[${fieldName}] must not exceed [${maxLength}] chars.")
            }
        }
    }

    void validateMinStrLength(PublicProps.UiProp prop, String fieldName, String value) {
        SkillsValidator.isNotBlank(value, fieldName)
        int minLen = publicProps.getInt(prop)
        if(value.length() < minLen){
            throw new SkillException("[${fieldName}] must not be less than [${minLen}] chars.")
        }
    }

    void validateMaxIntValue(PublicProps.UiProp prop, String fieldName, int value) {
        int maxVal = publicProps.getInt(prop)
        if (value > maxVal) {
            throw new SkillException("[${fieldName}] must be <= [${maxVal}]")
        }
    }
}
