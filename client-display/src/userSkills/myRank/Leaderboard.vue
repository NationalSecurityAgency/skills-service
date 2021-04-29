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
  <div class="card">
    <div class="card-body p-0">
      <div class="row p-4">
        <div class="col-auto text-left" style="font-size: 1rem;">
          <span class="h4 text-uppercase">Leaderboard</span>
          <span class="ml-2">|</span>
          <badge-based-selector class="ml-2"
                                :options="badgesSelector.options"
                                @value-changed="loadData"
                                v-model="badgesSelector.selected"/>
        </div>
        <div class="col pl-0">
          <hr/>
        </div>
      </div>

      <b-table :items="items" :fields="fields"
               stacked="sm"
               :busy="loading"
               :tbody-tr-class="rowClass">
        <template #table-busy>
          <div class="text-center text-danger my-2">
            <skills-spinner :loading="true"/>
          </div>
        </template>

        <template #head(rank)="data">
            <i class="fas fa-sort-amount-up"></i> {{ data.label }}
        </template>
        <template v-slot:cell(rank)="data">
          <div class="mt-2 bigger-text text-left text-sm-center"  style="min-width: 4rem;">
            <b-badge class="font-weight-bold" style="font-size: 0.8rem;">#{{ data.value }}</b-badge>
          </div>
        </template>

        <template #head(user)="data">
          <div class="text-left">
            <i class="far fa-user"></i> {{ data.label }}
          </div>
        </template>
        <template v-slot:cell(user)="data">
          <div class="text-left mt-2 bigger-text">
            <i class="fas fa-user-circle" style="font-size: 1.8rem;"></i> <span class="align-text-bottom text-info">{{ data.item.userId }}</span>
            <i v-if="data.item.rank <=3" class="fas fa-medal ml-2" :class="medalClass(data.item)"></i>
          </div>
        </template>

        <i class="fas fa-running"></i>
        <template #head(progress)="data">
          <div class="text-left">
            <i class="fas fa-running"></i> {{ data.label }}
          </div>
        </template>
        <template v-slot:cell(progress)="data">
          <div class="text-left mt-1">
            <div>
              <span style="width: 50rem;">
                <span class="h5">{{ data.item.points | number }}</span> <span class="font-italic">Points</span>
              </span>
              <b-progress :value="data.item.points" :max="totalProjPoints" class="mb-3" height="4px" ></b-progress>
            </div>
          </div>
        </template>

        <template #head(firstVisit)="data">
          <i class="far fa-clock"></i> {{ data.label }}
        </template>
        <template v-slot:cell(firstVisit)="data">
          <div class="mt-1 text-left text-sm-center">
            <div>{{ data.item.userFirstSeenTimestamp | relativeTime }}</div>
            <div class="text-secondary">{{ data.item.userFirstSeenTimestamp | formatDate('MM/DD/YYYY')}}</div>
          </div>
        </template>

      </b-table>

    </div>
  </div>
</template>

<script>
  import UserSkillsService from '../service/UserSkillsService';
  import BadgeBasedSelector from '../../common/utilities/BadgeBasedSelector';
  import SkillsSpinner from '../../common/utilities/SkillsSpinner';

  export default {
    name: 'Leaderboard',
    components: { SkillsSpinner, BadgeBasedSelector },
    data() {
      return {
        loading: true,
        totalProjPoints: 0,
        items: [],
        fields: [
          {
            key: 'rank',
            label: 'Rank',
            sortable: false,
          },
          {
            key: 'user',
            label: 'User',
            sortable: false,
          },
          {
            key: 'progress',
            label: 'Progress',
            sortable: false,
          },
          {
            key: 'firstVisit',
            label: 'User Since',
            sortable: false,
          },
        ],
        badgesSelector: {
          selected: 'topTen',
          options: [{
            value: 'topTen',
            label: 'Top 10',
          }, {
            value: 'tenAroundMe',
            label: '10 Around Me',
          }, {
            value: 'bottomTen',
            label: 'Bottom 10',
          }],
        },
      };
    },
    mounted() {
      this.loadData(this.badgesSelector.selected);
    },
    methods: {
      loadData(type) {
        this.loading = true;
        UserSkillsService.getLeaderboard(this.$route.params.subjectId, type)
          .then((result) => {
            this.totalProjPoints = result.totalProjPoints;
            this.items = result.rankedUsers;
          })
          .finally(() => {
            this.loading = false;
          });
      },
      medalClass(item) {
        if (item.rank === 1) {
          return 'skills-color-gold';
        }
        if (item.rank === 2) {
          return 'skills-color-silver';
        }
        if (item.rank === 3) {
          return 'skills-color-bronze';
        }
        return null;
      },
      rowClass(item, type) {
        if (!item || type !== 'row') {
          return;
        }
        if (item.isItYou) {
          // eslint-disable-next-line consistent-return
          return 'highlight-row';
        }
      },
    },
  };
</script>

<style scoped>
.bigger-text {
  font-size: 1rem;
}
.fa-medal {
  font-size: 1.2rem;
}
</style>

<style>
.highlight-row {
  border-style: solid;
  border-width: 3px;
  border-color: #007c49;
}
</style>
