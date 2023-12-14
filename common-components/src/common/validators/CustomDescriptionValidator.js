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
import { extend } from 'vee-validate';
import store from '../../store/store';
import DescriptionValidatorService from './DescriptionValidatorService';

const validator = {
  validate(value, args) {
    if (!store.getters.config.paragraphValidationRegex) {
      return true;
    }
    const enableProjectIdParam = !(args && args.length > 0 && args[0] === 'false');
    return DescriptionValidatorService.validateDescription(value, enableProjectIdParam).then((result) => {
      if (result.valid) {
        return true;
      }
      if (result.msg) {
        return `{_field_} - ${result.msg}.`;
      }
      return '{_field_} is invalid.';
    });
  },
};

extend('customDescriptionValidator', validator);

export default validator;
