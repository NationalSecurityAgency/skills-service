import VeeValidate from 'vee-validate';

const numericRegex = /^[0-9]+$/;

const validator = {
  getMessage: field => `The ${field} field may only contain numeric characters.`,
  validate(value) {
    const testValue = (val) => {
      const strValue = String(val);

      if (strValue) {
        return numericRegex.test(strValue);
      }
      return true;
    };

    if (Array.isArray(value)) {
      return value.every(testValue);
    }

    return testValue(value);
  },
};

VeeValidate.Validator.extend('optionalNumeric', validator, {
  immediate: false,
});

export default validator;
