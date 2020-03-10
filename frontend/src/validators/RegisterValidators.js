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
import { Validator } from 'vee-validate';
import './OptionalNumericValidator';
import './CustomDescriptionValidator';
import './CustomNameValidator';
import ValidatorFactory from './ValidatorFactory';
import store from '../store/store';

export default {
  init() {
    Validator.extend('maxDescriptionLength', ValidatorFactory.newCharLengthValidator(store.getters.config.descriptionMaxLength));
    Validator.extend('maxFirstNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxFirstNameLength));
    Validator.extend('maxLastNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxLastNameLength));
    Validator.extend('maxNicknameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxNicknameLength));

    Validator.extend('minUsernameLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minUsernameLength));

    Validator.extend('minPasswordLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minPasswordLength));
    Validator.extend('maxPasswordLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxPasswordLength));

    Validator.extend('minIdLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minIdLength));
    Validator.extend('maxIdLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxIdLength));

    Validator.extend('minNameLength', ValidatorFactory.newCharMinLengthValidator(store.getters.config.minNameLength));

    Validator.extend('maxBadgeNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxBadgeNameLength));
    Validator.extend('maxProjectNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxProjectNameLength));
    Validator.extend('maxSkillNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxSkillNameLength));
    Validator.extend('maxSubjectNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxSubjectNameLength));
    Validator.extend('maxLevelNameLength', ValidatorFactory.newCharLengthValidator(store.getters.config.maxLevelNameLength));

    Validator.extend('maxSkillVersion', ValidatorFactory.newMaxNumValidator(store.getters.config.maxSkillVersion));
    Validator.extend('maxPointIncrement', ValidatorFactory.newMaxNumValidator(store.getters.config.maxPointIncrement));
    Validator.extend('maxNumPerformToCompletion', ValidatorFactory.newMaxNumValidator(store.getters.config.maxNumPerformToCompletion));
    Validator.extend('maxNumPointIncrementMaxOccurrences', ValidatorFactory.newMaxNumValidator(store.getters.config.maxNumPointIncrementMaxOccurrences));

    Validator.extend('userNoSpaceInUserIdInNonPkiMode', ValidatorFactory.newUserObjNoSpacesValidatorInNonPkiMode(store.getters.isPkiAuthenticated));
  },
};
