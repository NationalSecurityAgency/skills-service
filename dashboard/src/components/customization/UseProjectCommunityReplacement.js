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
import MsgLogService from '@/common-components/utilities/MsgLogService.js'

export const useProjectCommunityReplacement = () => {

  const communityProjDescriptor = /\{\{\s?community.project.descriptor\s?\}\}/gi;
  const populateProjectCommunity = (value, projCommunityValue, errorPrepend = '') => {
    let result = value;
    if (value) {
      const found = value.match(communityProjDescriptor);
      if (found && !projCommunityValue) {
        const errorMessage = `${errorPrepend} contained {{community.project.descriptor}} property but failed to load [project_community_value] configuration for the replacement.`;
        MsgLogService.log('ERROR', errorMessage);
        throw new Error(errorMessage)
      }
      result = result.replace(communityProjDescriptor, projCommunityValue);
    }
    return result;
  }

  return {
    populateProjectCommunity
  }
}