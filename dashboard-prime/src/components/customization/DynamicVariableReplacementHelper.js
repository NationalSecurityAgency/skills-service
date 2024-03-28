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