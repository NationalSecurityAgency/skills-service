import { ref } from 'vue'
import { defineStore } from 'pinia'
import SettingService from '@/components/settings/SettingsService';

export const useProjConfig = defineStore('projConfig', () => {
   const projConfig = ref(null);
   const loadingProjConfig = ref(true);

   function loadProjConfigState(params) {
        const { projectId } = params;
        const { updateLoadingVar = true } = params;
       console.log('Function called');
        return new Promise((resolve, reject) => {
            if (updateLoadingVar) {
                loadingProjConfig.value = true;
            }
            SettingService.getSettingsForProject(projectId)
                .then((response) => {
                    console.log('Getting settings for project...');
                    if (response && typeof response.reduce === 'function') {
                        const newProjConfig = response.reduce((map, obj) => {
                            // eslint-disable-next-line no-param-reassign
                            map[obj.setting] = obj.value;
                            return map;
                        }, {});
                        projConfig.value = newProjConfig
                    }
                    resolve(response);
                })
                .finally(() => {
                    if (updateLoadingVar) {
                        loadingProjConfig.value = false;
                    }
                })
                .catch((error) => reject(error));
        });
    }

    function afterProjConfigStateLoaded({ state }) {
        return new Promise((resolve) => {
            (function waitForProjConfig() {
                if (!state.loadingProjConfig) return resolve(state.projConfig);
                setTimeout(waitForProjConfig, 100);
                return state.projConfig;
            }());
        });
    }

    return { projConfig, loadingProjConfig, loadProjConfigState, afterProjConfigStateLoaded };
});