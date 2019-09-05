import NumConvertUtil from './NumConvertUtil';

export default {
  newCharLengthValidator(maxLength) {
    return {
      getMessage: field => `${field} cannot exceed ${maxLength} characters.`,
      validate(value) {
        if (value.length > NumConvertUtil.toInt(maxLength)) {
          return false;
        }
        return true;
      },
    };
  },
  newCharMinLengthValidator(maxLength) {
    return {
      getMessage: field => `${field} cannot be less than ${maxLength} characters.`,
      validate(value) {
        if (value.length < NumConvertUtil.toInt(maxLength)) {
          return false;
        }
        return true;
      },
    };
  },
  newMaxNumValidator(maxNum) {
    return {
      getMessage: field => `${field} cannot exceed ${maxNum}.`,
      validate(value) {
        if (NumConvertUtil.toInt(value) > NumConvertUtil.toInt(maxNum)) {
          return false;
        }
        return true;
      },
    };
  },
};
