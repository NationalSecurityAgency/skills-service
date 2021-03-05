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
  <div class="card h-100 skills-my-rank" @click.stop="openMyRankDetails()"
       :class="{ 'skills-navigable-item': !isSummaryOnly }" data-cy="myRank">
    <div class="card-header">
      <h3 class="h6 card-title mb-0 text-uppercase">My Rank</h3>
    </div>
    <div class="card-body">
      <span class="fa-stack skills-icon user-rank-stack">
        <i class="fa fa-users fa-stack-2x watermark-icon"/>
        <strong class="fa-stack-1x text-primary user-rank-text">
          <span v-if="displayData.userSkillsRanking">
            {{ displayData.userSkillsRanking.position | number }}
          </span>
          <vue-simple-spinner v-else line-bg-color="#333" line-fg-color="#17a2b8"/>
        </strong>
      </span>
    </div>
  </div>
</template>

<script>
  import Spinner from 'vue-simple-spinner';
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';

  export default {
    mixins: [NavigationErrorMixin],
    components: {
      'vue-simple-spinner': Spinner,
    },
    props: {
      displayData: Object,
    },
    methods: {
      openMyRankDetails() {
        if (!this.isSummaryOnly) {
          this.handlePush({
            name: 'myRankDetails',
            params: {
              subjectId: this.displayData.userSkills.subjectId,
            },
          });
        }
      },
    },
    computed: {
      isSummaryOnly() {
        return this.$store.state.isSummaryOnly;
      },
    },
  };
</script>

<style scoped>

  .skills-my-rank .skills-icon {
    display: inline-block;
    color: #b1b1b1;
    margin: 5px 0;
  }

  .skills-my-rank .skills-icon.user-rank-stack {
    margin: 14px 0;
    font-size: 4.1rem;
    width: 100%;
    color: #0fcc15d1;
  }
  .skills-my-rank .skills-icon.user-rank-stack i{
    opacity: 0.38;
  }

  .skills-my-rank .user-rank-text {
    font-size: 0.5em;
    line-height: 1.2em;
    margin-top: 1.8em;
    background: rgba(255, 255, 255, 0.6);
  }
</style>
