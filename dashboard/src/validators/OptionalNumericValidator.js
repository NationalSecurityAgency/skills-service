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
