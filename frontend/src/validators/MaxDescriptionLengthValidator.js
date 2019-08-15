import store from '../store/store';
import VeeValidate from 'vee-validate';

const validator = {
  getMessage: field => `${field} cannot exceed ${store.getters.config.descriptionMaxLength} characters.`,
  validate(value) {
    if (value.length > store.getters.config.descriptionMaxLength) {
      return false;
    }
    return true;
  },
};

VeeValidate.Validator.extend('maxDescriptionLength', validator, {
  immediate: false,
});

export default validator;
