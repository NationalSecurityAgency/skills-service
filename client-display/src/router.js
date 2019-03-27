import Vue from 'vue';
import VueRouter from 'vue-router';

import SkillsEntry from '@/SkillsEntry.vue';
import SubjectDetails from '@/userSkills/subject/SubjectDetails.vue';

Vue.use(VueRouter);

const router = new VueRouter({
  mode: 'history',
  routes: [
    {
      path: '/',
      component: SkillsEntry,
    },
    {
      path: '/subject/:subjectId',
      component: SubjectDetails,
      name: 'subjectDetails',
      props: true,
    },
    {
      path: '*',
      component: SkillsEntry,
    },
  ],
});

router.afterEach(() => {
  window.parent.postMessage('skills::route-changed::{}', '*');
});

export default router;
