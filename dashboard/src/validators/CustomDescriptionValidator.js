/*
 * Copyright 2020 SkillTree
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
import VeeValidate from 'vee-validate';
import store from '../store/store';
import CustomValidatorService from './CustomValidatorsService';

const validator = {
  getMessage: (field) => `${field} - ${store.getters.config.paragraphValidationMessage}.`,
  validate(value) {
    if (!store.getters.config.paragraphValidationRegex) {
      return true;
    }

    return CustomValidatorService.validateDescription(value).then((result) => result.valid);
  },
};

VeeValidate.Validator.extend('customDescriptionValidator', validator, {
  immediate: false,
});

export default validator;
