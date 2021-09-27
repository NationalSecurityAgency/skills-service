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
  <div class="container-fluid">
    <sub-page-header title="My Badges" class="pt-4">
    </sub-page-header>

    <skills-spinner :is-loading="loading" />
    <b-row v-if="!loading" class="my-4">
      <b-col class="my-summary-card">
        <my-badges-details class="myBadges mb-4"
                           :displayBadgeProject="true"
                           :badges="this.achievedBadges"
                           :badgeRouterLinkGenerator="generateBadgeRouterLink" />
        <badges-catalog
          :noBadgesMessage="noCatalogMsg"
          :badges="this.unachievedBadges"
          :badgeRouterLinkGenerator="generateBadgeRouterLink"
          :displayBadgeProject="true"
        />
      </b-col>
    </b-row>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import MyBadgesDetails from '@/common-components/badges/MyBadgesDetails';
  import BadgesCatalog from '@/common-components/badges/BadgesCatalog';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import MyProgressService from '../MyProgressService';

  const { mapActions, mapGetters } = createNamespacedHelpers('myProgress');

  export default {
    name: 'MyBadges',
    components: {
      MyBadgesDetails,
      BadgesCatalog,
      SubPageHeader,
      SkillsSpinner,
    },
    data() {
      return {
        unachievedBadges: [],
        achievedBadges: [],
        projectIds: [],
        loading: true,
      };
    },
    mounted() {
      if (this.myProjects) {
        this.loadBadges();
      } else {
        this.loadMyProgressSummary()
          .then(() => {
            this.loadBadges();
          });
      }
    },
    computed: {
      noCatalogMsg() {
        if (this.achievedBadges.length > 0 && this.unachievedBadges.length === 0) {
          return 'No Badges left to earn!';
        }
        return 'No Badges available';
      },
      ...mapGetters([
        'myProgress',
        'myProjects',
      ]),
    },
    methods: {
      ...mapActions(['loadMyProgressSummary']),
      generateBadgeRouterLink(badge) {
        if (badge.projectId) {
          const navlink = { path: `/progress-and-rankings/projects/${badge.projectId}/`, query: { skillsClientDisplayPath: `/badges/${badge.badgeId}` } };
          return navlink;
        }

        if (badge.projectLevelsAndSkillsSummaries) {
          const summary = badge.projectLevelsAndSkillsSummaries.find((summ) => this.projectIds.includes(summ.projectId));
          if (summary) {
            const globalNavLink = { path: `/progress-and-rankings/projects/${summary.projectId}/`, query: { skillsClientDisplayPath: `/badges/global/${badge.badgeId}` } };
            return globalNavLink;
          }
        }
        return {};
      },
      loadBadges() {
        this.loading = true;
        this.projectIds = this.myProjects.map((p) => p.projectId);
        MyProgressService.loadMyBadges().then((res) => {
          this.unachievedBadges = res.filter((badge) => badge.badgeAchieved === false);
          this.achievedBadges = res.filter((badge) => badge.badgeAchieved === true);
        }).finally(() => {
          this.loading = false;
        });
      },
    },
  };
</script>

<style scoped>
  /deep/ div.earned-badge {
    text-align: center;
  }

  /deep/ div.skills-badge-icon {
    text-align: center;
  }

  /deep/ .skills-no-data-yet {
    text-align: center;
  }
</style>
