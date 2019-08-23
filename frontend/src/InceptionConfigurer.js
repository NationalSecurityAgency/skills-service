import { SkillsConfiguration } from '@skills/skills-client-vue';
import store from './store/store';

export default {

  configure() {
    if (store.getters.userInfo) {
      const projectId = 'Inception';
      const serviceUrl = window.location.origin;
      let authenticator;
      if (store.getters.isPkiAuthenticated) {
        authenticator = 'pki';
      } else {
        authenticator = `/app/projects/${encodeURIComponent(projectId)}/users/${encodeURIComponent(store.getters.userInfo.userId)}/token`;
      }

      SkillsConfiguration.configure({
        serviceUrl,
        projectId,
        authenticator,
      });
    }
  },
};
