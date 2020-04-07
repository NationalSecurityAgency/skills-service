/*
 * Copyright 2020 SkillTree
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
import Vue from 'vue';
import VueRouter from 'vue-router';

import { debounce } from 'lodash';

import SkillsEntry from '@/SkillsEntry.vue';
import SubjectDetails from '@/userSkills/subject/SubjectDetails.vue';
import MyRankDetails from '@/userSkills/myRank/MyRankDetails.vue';
import SkillDetails from '@/userSkills/skill/SkillDetails.vue';
import BadgesDetails from '@/userSkills/badge/BadgesDetails.vue';
import GlobalBadgeDetails from '@/userSkills/badge/GlobalBadgeDetails.vue';
import BadgeDetails from '@/userSkills/badge/BadgeDetails.vue';
import ErrorPage from '@/userSkills/ErrorPage.vue';
import store from '@/store';

Vue.use(VueRouter);

const router = new VueRouter({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: SkillsEntry,
      props: true,
    },
    {
      path: '',
      name: 'error',
      component: ErrorPage,
      props: true,
    },
    {
      path: '/subjects/:subjectId',
      component: SubjectDetails,
      name: 'subjectDetails',
      props: true,
    },
    {
      path: '/badges',
      component: BadgesDetails,
      name: 'badges',
      props: true,
    },
    {
      path: '/badges/:badgeId',
      component: BadgeDetails,
      name: 'badgeDetails',
      props: true,
      meta: { setPreviousRoute: true },
    },
    {
      path: '/badges/global/:badgeId',
      component: GlobalBadgeDetails,
      name: 'globalBadgeDetails',
      props: true,
      meta: { setPreviousRoute: true },
    },
    {
      path: '/skills/:skillId',
      component: SkillDetails,
      name: 'skillDetails',
      meta: { setPreviousRoute: true },
    },
    {
      path: '/skills/crossProject/:crossProjectId/:skillId',
      component: SkillDetails,
      name: 'crossProjectSkillDetails',
      meta: { setPreviousRoute: true },
    },
    {
      path: '/rank',
      component: MyRankDetails,
      name: 'myRankDetails',
      props: true,
    },
    {
      path: '*',
      component: SkillsEntry,
    },
  ],
});

router.beforeEach((to, from, next) => {
  if (to.meta.setPreviousRoute && !to.params.previousRoute) {
    const previousRoute = { ...from };
    const params = { ...to.params, ...{ previousRoute } };
    const updatedTo = { ...to, ...{ params } };
    next(updatedTo);
  } else {
    next();
  }
});

router.afterEach(debounce(() => {
  if (process.env.NODE_ENV !== 'development' && store.state.parentFrame) {
    store.state.parentFrame.emit('route-changed');
  }
}, 250));

export default router;
