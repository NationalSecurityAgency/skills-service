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
import './OptionalNumericValidator';
import './CustomDescriptionValidator';
import './CustomNameValidator';
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
  },
};
