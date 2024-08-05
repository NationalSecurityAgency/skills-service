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
import dayjs from 'dayjs';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useDynamicVariableReplacementHelper = () => {
    const appConfig = useAppConfig();

    const releaseVersion = /\{\{\s?release.version\s?\}\}/gi;
    const buildDate = /\{\{\s?build.date\s?\}\}/gi;
    const communityDescriptor = /\{\{\s?community.descriptor\s?\}\}/gi;
    const populateDynamicVariables = (text) => {
        let result = text;
        if (result) {
            const version = appConfig.dashboardVersion;
            if (version) {
                result = result.replace(releaseVersion, version);
            }
            const timestamp = appConfig.buildTimestamp;
            if (timestamp) {
                const dateString = dayjs(timestamp).format('ll');
                result = result.replace(buildDate, dateString);
            }
            const communityHeaderDescriptor = appConfig.currentUsersCommunityDescriptor;
            if (communityHeaderDescriptor) {
                result = result.replace(communityDescriptor, communityHeaderDescriptor);
            }
        }
        return result;
    };

    return {
        populateDynamicVariables
    }
}