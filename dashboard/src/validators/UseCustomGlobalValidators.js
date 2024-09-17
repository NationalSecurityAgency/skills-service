/*
 * Copyright 2024 SkillTree
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
import CustomValidatorsService from '@/validators/CustomValidatorsService.js'
import { addMethod, string } from 'yup'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useDebounceFn } from '@vueuse/core'
import { useDescriptionValidatorService } from '@/common-components/validators/UseDescriptionValidatorService.js'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useCustomGlobalValidators = () => {

  const descriptionValidatorService = useDescriptionValidatorService()
  const log = useLog()

  function customNameValidator(fieldName = '') {
    const appConfig = useAppConfig()
    const validateName = useDebounceFn((value, context) => {
      if (!value || value.trim().length === 0 || !appConfig.nameValidationRegex) {
        return true;
      }
      return CustomValidatorsService.validateName(value).then((result) => {
        if (!result.valid) {
          return context.createError({ message: `${fieldName ? `${fieldName} - ` : ''}${result.msg}` });
        }
        return true;
      });
    }, appConfig.formFieldDebounceInMs)

    return this.test("customNameValidator", null, (value, context) => validateName(value, context));
  }

  function customDescriptionValidator(fieldName = '', enableProjectIdParam = true, useProtectedCommunityValidator = null, fieldNameFunction = null) {
    const appConfig = useAppConfig()
    const validateName = useDebounceFn((value, context) => {
      if (!value || value.trim().length === 0 || !appConfig.paragraphValidationRegex) {
        return true
      }
      return descriptionValidatorService.validateDescription(value, enableProjectIdParam, useProtectedCommunityValidator).then((result) => {
        if (result.valid) {
          return true
        }
        let fieldNameToUse = fieldName ? fieldName : '';
        if (!fieldNameToUse && fieldNameFunction) {
          fieldNameToUse = fieldNameFunction(context);
        }
        if (result.msg) {
          return context.createError({ message: `${fieldNameToUse ? `${fieldNameToUse} - ` : ''}${result.msg}` })
        }
        return context.createError({ message: `${fieldNameToUse || 'Field'} is invalid` })
      })
    }, appConfig.formFieldDebounceInMs)

    return this.test("customDescriptionValidator", null, (value, context) => validateName(value, context));
  }

  function nullValueNotAllowed() {
    return this.test('nullValueNotAllowed', 'Null value is not allowed', (value) => !value || value.trim().toLowerCase() !== 'null');
  }

  function urlValidator(fieldName = 'Help URL/Path') {
    const appConfig = useAppConfig()
    const validateName = useDebounceFn((value, context) => {
      if (!value || value.trim().length === 0) {
        return true;
      }

      const properStart = value.startsWith('http') || value.startsWith('https') || value.startsWith('/')
      if (!properStart) {
        return context.createError({ message:`${fieldName} must start with "/" or "http(s)"`})
      }

      return CustomValidatorsService.validateUrl(value).then((result) => {
        if (!result.valid) {
          return context.createError({ message: result.msg });
        }
        return true;
      });
    }, appConfig.formFieldDebounceInMs)

    return this.test("urlValidator", null, (value, context) => validateName(value, context));
  }

  function idValidator() {
    return this.matches(/^\w+$/, (fieldProps) => `${fieldProps.label} may only contain alpha-numeric characters`)
  }

  function noScript() {
    const scriptRegex = /<[^>]*script/i;
    return this.test('noScript', 'Script tags are not allowed', (value) => {
      if (value) {
        return value.match(scriptRegex) === null;
      }
      return true;
    })
  }

  const addValidators = () => {
    log.trace('Adding custom global validators')
    addMethod(string, "customNameValidator", customNameValidator);
    addMethod(string, "nullValueNotAllowed", nullValueNotAllowed);
    addMethod(string, 'customDescriptionValidator', customDescriptionValidator)
    addMethod(string, 'urlValidator', urlValidator)
    addMethod(string, 'idValidator', idValidator)
    addMethod(string, 'noScript', noScript)
  }

  return {
    addValidators
  }
}