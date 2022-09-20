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
        <skills-spinner :loading="loading" />

        <div v-if="!loading">
            <skills-title>Badge Details</skills-title>

            <div class="card">
                <div class="card-body">
                    <badge-details-overview :badge="badgeOverview"></badge-details-overview>
                </div>
                <div v-if="badge.helpUrl" class="card-footer text-left">
                  <a :href="badge.helpUrl" target="_blank" rel="noopener" class="btn btn-sm btn-outline-info skills-theme-btn">
                    Learn More <i class="fas fa-external-link-alt"></i>
                  </a>
                </div>
            </div>

            <skills-progress-list @points-earned="refreshHeader" v-if="badge" :subject="badge" :show-descriptions="showDescriptions" type="badge"
                                  @scrollTo="scrollToLastViewedSkill" />
        </div>
    </div>
</template>

<script>
  import BadgeDetailsOverview from '@/common-components/badges/BadgeDetailsOverview';
  import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import SkillsTitle from '@/common/utilities/SkillsTitle';

  export default {
    components: {
      SkillsTitle,
      SkillsProgressList,
      BadgeDetailsOverview,
      SkillsSpinner,
    },
    beforeRouteEnter(to, from, next) {
      // console.log(`to.name: ${to.name}, from: ${from.name}`);
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
        loading: true,
        badge: null,
        badgeOverview: null,
        initialized: false,
        showDescriptions: false,
        jumpToLastViewed: false,
        lastViewedSkillId: null,
      };
    },
    watch: {
      $route: 'fetchData',
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        UserSkillsService.getBadgeSkills(this.$route.params.badgeId)
          .then((badgeSummary) => {
            this.badge = badgeSummary;
            this.badgeOverview = badgeSummary;
            this.loading = false;
            const foundLastViewedSkill = badgeSummary.skills.find((item) => item.isLastViewed === true);
            this.lastViewedSkillId = foundLastViewedSkill ? foundLastViewedSkill.skillId : null;
            setTimeout(() => { this.autoScrollToLastViewedSkill(); }, 400);
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
      autoScrollToLastViewedSkill() {
        if (this.jumpToLastViewed) {
          this.$nextTick(() => {
            this.scrollToLastViewedSkill();
          });
        }
      },
      scrollToLastViewedSkill() {
        if (this.lastViewedSkillId) {
          const element = document.getElementById(`skillProgressTitle-${this.lastViewedSkillId}`);
          if (element) {
            this.$nextTick(() => {
              element.scrollIntoView({ behavior: 'smooth' });
              this.$nextTick(() => {
                const elementFocusOn = document.getElementById(`skillProgressTitle-${this.lastViewedSkillId}`);
                elementFocusOn.focus({ preventScroll: true });
              });
            });
          }
        }
      },
    },
  };
</script>

<style scoped>

</style>
