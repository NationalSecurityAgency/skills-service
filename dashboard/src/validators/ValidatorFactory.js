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
import NumConvertUtil from './NumConvertUtil';

export default {
  newCharLengthValidator(maxLength) {
    return {
      message: (field) => `${field} cannot exceed ${maxLength} characters.`,
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
      message: (field) => `${field} cannot be less than ${maxLength} characters.`,
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
      message: (field) => `${field} cannot exceed ${maxNum}.`,
      validate(value) {
        if (NumConvertUtil.toInt(value) > NumConvertUtil.toInt(maxNum)) {
          return false;
        }
        return true;
      },
    };
  },
  newUserObjNoSpacesValidatorInNonPkiMode(isPkiMode) {
    return {
      message: (field) => `The ${field} field may not contain spaces`,
      validate(value) {
        if (isPkiMode || !value.userId) {
          return true;
        }
        const hasSpaces = value.userId.indexOf(' ') >= 0;
        return !hasSpaces;
      },
    };
  },
};
