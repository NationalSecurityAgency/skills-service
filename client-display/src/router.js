import Vue from 'vue';
import VueRouter from 'vue-router';
import VueBreadcrumbs from 'vue-2-breadcrumbs';

import SkillsEntry from '@/SkillsEntry.vue';
// import SubjectDetails from '@/userSkills/subject/SubjectDetails.vue';
import MyRankDetails from '@/userSkills/myRank/MyRankDetails.vue';


Vue.use(VueRouter);
Vue.use(VueBreadcrumbs);

const router = new VueRouter({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: SkillsEntry,
    },
    // {
    //   path: '/subject/:subjectId',
    //   component: SubjectDetails,
    //   name: 'subjectDetails',
    //   props: true,
    //   meta: {
    //     breadcrumb: {
    //       label: 'Subject Details',
    //     },
    //   },
    // },
    {
      path: '/myrank',
      component: MyRankDetails,
      name: 'myRankDetails',
      props: true,
      meta: {
        breadcrumb: {
          label: 'My Rank',
        },
      },
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
