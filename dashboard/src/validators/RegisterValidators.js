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
import { extend, localize } from 'vee-validate';
import { required } from 'vee-validate/dist/rules';
import './OptionalNumericValidator';
import './CustomDescriptionValidator';
import './CustomNameValidator';
import './IdValidator';
import ValidatorFactory from './ValidatorFactory';
import store from '../store/store';

export default {
  init() {
    extend('maxDescriptionLength', ValidatorFactory.newCharLengthValidator(store.getters.config.descriptionMaxLength));
    extend('maxFirstNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxFirstNameLength));
    extend('maxLastNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxLastNameLength));
    extend('maxNicknameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxNicknameLength));

    extend('minUsernameLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minUsernameLength));

    extend('minPasswordLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minPasswordLength));
    extend('maxPasswordLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxPasswordLength));

    extend('minIdLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minIdLength));
    extend('maxIdLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxIdLength));

    extend('minNameLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minNameLength));

    extend('maxBadgeNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxBadgeNameLength));
    extend('maxProjectNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxProjectNameLength));
    extend('maxSkillNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxSkillNameLength));
    extend('maxSubjectNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxSubjectNameLength));
    extend('maxLevelNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxLevelNameLength));

    extend('maxSkillVersion', ValidatorFactory.newMaxNumValidator(store.getters.config.maxSkillVersion));
    extend('maxPointIncrement', ValidatorFactory.newMaxNumValidator(store.getters.config.maxPointIncrement));
    extend('maxNumPerformToCompletion', ValidatorFactory.newMaxNumValidator(store.getters.config.maxNumPerformToCompletion));
    extend('maxNumPointIncrementMaxOccurrences', ValidatorFactory.newMaxNumValidator(store.getters.config.maxNumPointIncrementMaxOccurrences));

    extend('userNoSpaceInUserIdInNonPkiMode', ValidatorFactory.newUserObjNoSpacesValidatorInNonPkiMode(store.getters.isPkiAuthenticated));

    extend('required', required);

    localize({
      en: {
        messages: {
          alpha: '{_field_} may only contain alphabetic characters',
          alpha_num: '{_field_} may only contain alpha-numeric characters',
          alpha_dash: '{_field_} may contain alpha-numeric characters as well as dashes and underscores',
          alpha_spaces: '{_field_} may only contain alphabetic characters as well as spaces',
          between: '{_field_} must be between {min} and {max}',
          confirmed: '{_field_} confirmation does not match',
          digits: '{_field_} must be numeric and exactly contain {length} digits',
          dimensions: '{_field_} must be {width} pixels by {height} pixels',
          email: '{_field_} must be a valid email',
          excluded: '{_field_} is not a valid value',
          ext: '{_field_} is not a valid file',
          image: '{_field_} must be an image',
          integer: '{_field_} must be an integer',
          length: '{_field_} must be {length} long',
          max_value: '{_field_} must be {max} or less',
          max: '{_field_} may not be greater than {length} characters',
          mimes: '{_field_} must have a valid file type',
          min_value: '{_field_} must be {min} or more',
          min: '{_field_} must be at least {length} characters',
          numeric: '{_field_} may only contain numeric characters',
          oneOf: '{_field_} is not a valid value',
          regex: '{_field_} format is invalid',
          required_if: '{_field_} is required',
          required: '{_field_} is required',
          size: '{_field_} size must be less than {size}KB',
        },
      },
    });
  },
};
