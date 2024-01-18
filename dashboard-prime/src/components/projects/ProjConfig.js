import { computed } from 'vue';
import { useStore } from 'vuex'
import UserRolesUtil from '@/components/utils/UserRolesUtil';

export function projConfig() {
    const store = useStore();

    const isLoadingProjConfig = computed(() => {
        return store.getters.loadingProjConfig === undefined || store.getters.loadingProjConfig === null || store.getters.loadingProjConfig;
    });

    const projConfig = computed(() => {
        return store.getters.projConfig;
    });

    const isProjConfigInviteOnly = computed(() => {
        return store.getters.projConfig && store.getters.projConfig.invite_only === 'true';
    });

    const isProjConfigDiscoverable = computed(() => {
        return store.getters.projConfig && store.getters.projConfig['production.mode.enabled'] === 'true';
    });

    const projConfigRootHelpUrl = computed(() => {
        return store.getters.projConfig && store.getters.projConfig['help.url.root'];
    });

    const isReadOnlyProj = computed(() => {
        return this.isReadOnlyProjMethod();
    });

    const userProjRole = computed(() => {
        return store.getters.projConfig && store.getters.projConfig.user_project_role;
    });

    function loadProjectConfig() {
        return store.dispatch('afterProjConfigStateLoaded').then((projConfig) => projConfig);
    }

    function isReadOnlyProjMethod() {
        return store.getters.projConfig && UserRolesUtil.isReadOnlyProjRole(store.getters.projConfig.user_project_role);
    }

    return { isLoadingProjConfig, projConfig, isProjConfigInviteOnly, isProjConfigDiscoverable, projConfigRootHelpUrl, isReadOnlyProj, userProjRole };
}