import CustomValidatorsService from '@/validators/CustomValidatorsService.js'
import { addMethod, string } from 'yup'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'

export const useCustomGlobalValidators = () => {
  const appConfig = useAppConfig()
  function customNameValidator(message) {
    return this.test("noNo", message, function(value) {
      const { path, createError } = this;
      if (!appConfig.nameValidationRegex) {
        return true;
      }
      return CustomValidatorsService.validateName(value).then((result) => {
        if (!result.valid) {
          return createError({ path, message: result.msg });
        }
        return true;
      });
    });
  }

  function nullValueNotAllowed() {
    return this.test('nullValueNotAllowed', 'Null value is not allowed', (value) => !value || value.trim().toLowerCase() !== 'null');
  }

  const addValidators = () => {
    addMethod(string, "customNameValidator", customNameValidator);
    addMethod(string, "nullValueNotAllowed", nullValueNotAllowed);
  }

  return {
    addValidators
  }
}