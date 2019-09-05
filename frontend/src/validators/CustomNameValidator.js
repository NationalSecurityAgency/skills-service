import VeeValidate from 'vee-validate';
import store from '../store/store';
import CustomValidatorService from './CustomValidatorsService';

const validator = {
  getMessage: field => `${field} - ${store.getters.config.nameValidationMessage}.`,
  validate(value) {
    if (!store.getters.config.nameValidationRegex) {
      return true;
    }

    return CustomValidatorService.validateName(value).then(result => result.valid);
  },
};

VeeValidate.Validator.extend('customNameValidator', validator, {
  immediate: false,
});

export default validator;
