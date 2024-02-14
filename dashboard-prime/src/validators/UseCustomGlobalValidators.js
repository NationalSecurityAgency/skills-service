import CustomValidatorsService from '@/validators/CustomValidatorsService.js'
import { addMethod, string } from 'yup'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import { useDebounceFn } from '@vueuse/core'
import DescriptionValidatorService from '@/common-components/validators/DescriptionValidatorService.js'

export const useCustomGlobalValidators = () => {

  function customNameValidator(message) {
    const appConfig = useAppConfig()
    const validateName = useDebounceFn((value, context) => {
      if (!value || value.trim().length === 0 || !appConfig.nameValidationRegex) {
        return true;
      }
      return CustomValidatorsService.validateName(value).then((result) => {
        if (!result.valid) {
          return context.createError({ message: result.msg });
        }
        return true;
      });
    }, appConfig.formFieldDebounceInMs)

    return this.test("customNameValidator", message, (value, context) => validateName(value, context));
  }

  function customDescriptionValidator(fieldName = '', enableProjectIdParam = true, useProtectedCommunityValidator = null) {
    const appConfig = useAppConfig()
    const validateName = useDebounceFn((value, context) => {
      if (!value || value.trim().length === 0 || !appConfig.paragraphValidationRegex) {
        return true
      }
      return DescriptionValidatorService.validateDescription(value, enableProjectIdParam, useProtectedCommunityValidator).then((result) => {
        if (result.valid) {
          return true
        }
        if (result.msg) {
          return context.createError({ message: `${fieldName ? `${fieldName} - ` : ''}${result.msg}` })
        }
        return context.createError({ message: `${fieldName || 'Field'} is invalid` })
      })
    }, appConfig.formFieldDebounceInMs)

    return this.test("customDescriptionValidator", null, (value, context) => validateName(value, context));
  }



  function nullValueNotAllowed() {
    return this.test('nullValueNotAllowed', 'Null value is not allowed', (value) => !value || value.trim().toLowerCase() !== 'null');
  }

  const addValidators = () => {

    addMethod(string, "customNameValidator", customNameValidator);
    addMethod(string, "nullValueNotAllowed", nullValueNotAllowed);
    addMethod(string, 'customDescriptionValidator', customDescriptionValidator)
  }

  return {
    addValidators
  }
}