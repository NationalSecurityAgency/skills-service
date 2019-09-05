import VeeValidate from 'vee-validate';
import store from '../store/store';
import CustomValidatorService from './CustomValidatorsService';

const validator = {
  getMessage: field => `${field} - ${store.getters.config.paragraphValidationMessage}.`,
  validate(value) {
    if (!store.getters.config.paragraphValidationRegex) {
      return true;
    }

    console.log(`validating [${value}] against endpoint`);
    return CustomValidatorService.validateDescription(value).then(result => result.valid);
  },
};

VeeValidate.Validator.extend('customDescriptionValidator', validator, {
  immediate: false,
});

export default validator;
