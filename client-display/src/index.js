import UserSkills from './userSkills/UserSkills.vue';
import UserSkillsHeader from './userSkills/UserSkillsHeader.vue';

const plugin = {
  install(Vue) {
    Vue.component('UserSkills', UserSkills);
    Vue.component('UserSkillsHeader', UserSkillsHeader);
  },
};

export default plugin;

export { UserSkills, UserSkillsHeader };
