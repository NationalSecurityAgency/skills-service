import Vue from 'vue';
import { SkillsReporter } from '@skills/skills-client-vue';

// Register a global custom directive
Vue.directive('skills-onMount', {
  bind: (el, binding) => {
    SkillsReporter.reportSkill(binding.value);
  },
});
