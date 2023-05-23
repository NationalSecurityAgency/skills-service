/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
    <div>
        <skills-spinner :loading="isLoading" />

        <div v-if="!isLoading">
            <skills-title>Badge Details</skills-title>

            <div class="card">
                <div class="card-body">
                    <badge-details-overview :badge="badgeOverview"></badge-details-overview>

                    <div v-if="locked" class="text-center text-muted locked-text">
                      *** Badge has <b>{{ badge.dependencyInfo.numDirectDependents}}</b> direct prerequisite(s).
                      <span>Please see its prerequisites below.</span>
                      ***
                    </div>
                </div>
                <div v-if="badge.helpUrl" class="card-footer text-left">
                  <a :href="badge.helpUrl" target="_blank" rel="noopener" class="btn btn-sm btn-outline-info skills-theme-btn">
                    Learn More <i class="fas fa-external-link-alt"></i>
                  </a>
                </div>
            </div>

            <skills-progress-list @points-earned="refreshHeader" v-if="badge" :subject="badge" :show-descriptions="showDescriptions" type="badge"
                                  @scrollTo="scrollToLastViewedSkill" :badge-is-locked="locked"/>

            <skill-dependencies class="mt-2" v-if="dependencies && dependencies.length > 0" :dependencies="dependencies"
                                :skill-id="$route.params.badgeId"></skill-dependencies>
        </div>
    </div>
</template>

<script>
  import BadgeDetailsOverview from '@/common-components/badges/BadgeDetailsOverview';
  import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import SkillsTitle from '@/common/utilities/SkillsTitle';
  import ScrollSkillIntoViewMixin from '@/userSkills/utils/ScrollSkillIntoViewMixin';

  export default {
    components: {
      SkillsTitle,
      SkillsProgressList,
      BadgeDetailsOverview,
      SkillsSpinner,
      'skill-dependencies': () => import(/* webpackChunkName: 'skillDependencies' */'@/userSkills/skill/dependencies/SkillDependencies'),
    },
    mixins: [ScrollSkillIntoViewMixin],
    beforeRouteEnter(to, from, next) {
      if (to.name === 'badgeDetails' && from.name === 'badgeSkillDetails') {
        next((vm) => {
          // eslint-disable-next-line no-param-reassign
          vm.jumpToLastViewed = true;
        });
      } else {
        next();
      }
    },
    data() {
      return {
        loading: {
          badge: true,
          prerequisites: true,
        },
        badge: null,
        badgeOverview: null,
        initialized: false,
        showDescriptions: false,
        dependencies: [],
      };
    },
    watch: {
      $route: 'reloadData',
    },
    mounted() {
      this.reloadData();
    },
    computed: {
      locked() {
        return this.badge.dependencyInfo && !this.badge.dependencyInfo.achieved;
      },
      isLoading() {
        return this.loading.badge || this.loading.prerequisites;
      },
    },
    methods: {
      reloadData() {
        this.loadDependencies().then(() => {
          this.fetchData().then(() => this.handleScroll());
        });
      },
      handleScroll() {
        const foundLastViewedSkill = this.badge.skills.find((item) => item.isLastViewed === true);
        this.lastViewedSkillId = foundLastViewedSkill ? foundLastViewedSkill.skillId : null;
        this.autoScrollToLastViewedSkill();
      },
      loadDependencies() {
        this.loading.prerequisites = true;
        return UserSkillsService.getSkillDependencies(this.$route.params.badgeId)
          .then((res) => {
            this.dependencies = res.dependencies;
          }).finally(() => {
            this.loading.prerequisites = false;
          });
      },
      fetchData() {
        this.loading.badge = true;
        return UserSkillsService.getBadgeSkills(this.$route.params.badgeId)
          .then((badgeSummary) => {
            this.badge = badgeSummary;
            this.badgeOverview = badgeSummary;
          }).finally(() => {
            this.loading.badge = false;
          });
      },
      refreshHeader(event) {
        if (event.badgeId && event.badgeId === this.badge.badgeId) {
          UserSkillsService.getBadgeSkills(this.$route.params.badgeId, false, false)
            .then((badgeSummary) => {
              this.badgeOverview = badgeSummary;
            });
        }
      },
    },
  };
</script>

<style scoped>

</style>
