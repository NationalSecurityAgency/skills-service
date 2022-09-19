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
      <div class="row px-3 pt-3">
        <div class="col-12">
          <b-form-group label="User Id Filter" label-class="text-muted">
            <b-input v-model="filters.userId" v-on:keydown.enter="applyFilters" data-cy="users-skillIdFilter" aria-label="user id filter"/>
          </b-form-group>
        </div>
        <div class="col-md">
        </div>
      </div>

      <div class="row pl-3 mb-3">
        <div class="col">
          <b-button variant="outline-info" @click="applyFilters" data-cy="users-filterBtn"><i class="fa fa-filter" aria-hidden="true" /> Filter</b-button>
          <b-button variant="outline-info" @click="reset" class="ml-1" data-cy="users-resetBtn"><i class="fa fa-times" aria-hidden="true" /> Reset</b-button>
        </div>
      </div>

      <skills-b-table :options="table.options" :items="table.items"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      @sort-changed="sortTable"
                      data-cy="usersTable">
        <template #head(userId)="data">
          <span class="text-primary"><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(totalPoints)="data">
          <span class="text-primary"><i class="far fa-arrow-alt-circle-up skills-color-points" aria-hidden="true"></i> {{ data.label }}</span>
        </template>
        <template #head(lastUpdated)="data">
          <span class="text-primary"><i class="far fa-clock skills-color-events" aria-hidden="true"></i> {{ data.label }}</span>
        </template>

        <template v-slot:cell(userId)="data">
          {{ getUserDisplay(data.item) }}

          <b-button-group class="float-right">
            <b-button :to="calculateClientDisplayRoute(data.item)"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover="'View User Details'"
                      :aria-label="`View details for user ${getUserDisplay(data.item)}`"
                      data-cy="usersTable_viewDetailsBtn"><i class="fa fa-user-alt" aria-hidden="true"/><span class="sr-only">view user details</span>
            </b-button>
          </b-button-group>
        </template>
        <template v-slot:cell(userTag)="data">
          <router-link
            v-if="showUserTagColumn && data.item.userTag"
            :to="{ name: 'UserTagMetrics', params: { projectId: projectId, tagKey: tagKey, tagFilter: data.item.userTag } }"
            class="text-info mb-0 pb-0 preview-card-title"
            :aria-label="`View metrics for ${data.item.userTag}`"
            role="link"
            data-cy="usersTable_viewUserTagMetricLink">
            {{ data.item.userTag }}
          </router-link>
        </template>
        <template v-slot:cell(totalPoints)="data">
          <div :data-cy="`usr_progress-${data.item.userId}`">
            <div class="row">
              <div class="col-auto">
                <span class="font-weight-bold text-primary"
                      :aria-label="`${calcPercent(data.value)} percent completed`"
                      data-cy="progressPercent">{{ calcPercent(data.value) }}%</span>
              </div>
              <div class="col text-right">
                <span class="text-primary font-weight-bold"
                      :aria-label="`${data.value} out of ${totalPoints} total points`"
                      data-cy="progressCurrentPoints">{{ data.value | number }}</span> / <span class="font-italic" data-cy="progressTotalPoints">{{ totalPoints | number }}</span>
              </div>
            </div>
            <b-progress :max="totalPoints" class="mb-3" height="5px" variant="info">
              <b-progress-bar :value="data.value"  :aria-label="`Progress for ${data.item.userId} user`"></b-progress-bar>
            </b-progress>
            <div v-if="data.item.userMaxLevel || data.item.userMaxLevel === 0" class="row" data-cy="progressLevels">
              <div class="col">
                <i class="fas fa-trophy skills-color-levels" aria-hidden="true" /> <span class="font-italic">Current Level: </span>
                <span v-if="data.item.userMaxLevel === 0" data-cy="progressCurrentLevel">None</span>
                <span v-else class="font-weight-bold" data-cy="progressCurrentLevel">{{ data.item.userMaxLevel }}</span>
              </div>
            </div>
          </div>
        </template>
        <template v-slot:cell(lastUpdated)="data">
          <date-cell :value="data.value" />
        </template>
      </skills-b-table>
    </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import UsersService from './UsersService';
  import DateCell from '../utils/table/DateCell';

  export default {
    name: 'Users',
    components: {
      DateCell,
      SkillsBTable,
    },
    data() {
      return {
        loading: true,
        initialLoad: true,
        inviteOnlyProject: false,
        data: [],
        filters: {
          userId: '',
        },
        totalPoints: 0,
        table: {
          items: [],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'lastUpdated',
            sortDesc: true,
            tableDescription: 'Users',
            fields: [
              {
                key: 'userId',
                label: 'User Id',
                sortable: true,
              },
              {
                key: 'totalPoints',
                label: 'Progress',
                sortable: true,
              },
              {
                key: 'lastUpdated',
                label: 'Points Last Earned',
                sortable: true,
              },
            ],
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
          },
        },
      };
    },
    mounted() {
      if (this.showUserTagColumn) {
        this.table.options.fields.splice(1, 0, {
          key: 'userTag',
          label: this.$store.getters.config.usersTableAdditionalUserTagLabel,
          sortable: true,
        });
      }
      this.loadData();
    },
    computed: {
      showUserTagColumn() {
        return !!(this.$store.getters.config.usersTableAdditionalUserTagKey && this.$store.getters.config.usersTableAdditionalUserTagLabel);
      },
      projectId() {
        return this.$route.params.projectId;
      },
      tagKey() {
        return this.$store.getters.config.usersTableAdditionalUserTagKey;
      },
    },
    methods: {
      calcPercent(userPoints) {
        if (!this.totalPoints) {
          return 'N/A';
        }
        return Math.trunc((userPoints / this.totalPoints) * 100);
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      applyFilters() {
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite(`Users table has been filtered by ${this.filters.userId}`));
        });
      },
      reset() {
        this.filters.userId = '';
        this.table.options.pagination.currentPage = 1;
        this.loadData().then(() => {
          this.$nextTick(() => this.$announcer.polite('Users table filters have been removed'));
        });
      },
      loadData() {
        this.table.options.busy = true;
        const url = this.getUrl();
        return UsersService.ajaxCall(url, {
          query: this.filters.userId,
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          byColumn: 0,
          orderBy: this.table.options.sortBy,
        }).then((res) => {
          this.table.items = res.data;
          this.table.options.pagination.totalRows = res.count;
          this.totalPoints = res.totalPoints;
          this.table.options.busy = false;
        });
      },
      calculateClientDisplayRoute(props) {
        const hasSubject = this.$route.params.subjectId || false;
        const hasSkill = this.$route.params.skillId || false;
        const hasBadge = this.$route.params.badgeId || false;

        let routeObj = {
          name: 'ClientDisplayPreview',
          params: {
            projectId: this.$route.params.projectId,
            userId: props.userId,
            dn: props.dn,
          },
        };

        if (hasSkill) {
          routeObj = {
            name: 'ClientDisplayPreviewSkill',
            params: {
              projectId: this.$route.params.projectId,
              subjectId: this.$route.params.subjectId,
              skillId: this.$route.params.skillId,
              userId: props.userId,
              dn: props.dn,
            },
          };
        } else if (hasSubject) {
          routeObj = {
            name: 'ClientDisplayPreviewSubject',
            params: {
              projectId: this.$route.params.projectId,
              subjectId: this.$route.params.subjectId,
              userId: props.userId,
              dn: props.dn,
            },
          };
        } else if (hasBadge) {
          routeObj = {
            name: 'ClientDisplayPreviewBadge',
            params: {
              projectId: this.$route.params.projectId,
              badgeId: this.$route.params.badgeId,
              userId: props.userId,
              dn: props.dn,
            },
          };
        }

        return routeObj;
      },
      getUrl() {
        let url = `/admin/projects/${encodeURIComponent(this.$route.params.projectId)}`;
        if (this.$route.params.skillId) {
          url += `/skills/${encodeURIComponent(this.$route.params.skillId)}`;
        } else if (this.$route.params.badgeId) {
          url += `/badges/${encodeURIComponent(this.$route.params.badgeId)}`;
        } else if (this.$route.params.subjectId) {
          url += `/subjects/${encodeURIComponent(this.$route.params.subjectId)}`;
        } else if (this.$route.params.tagKey && this.$route.params.tagFilter) {
          url += `/userTags/${encodeURIComponent(this.$route.params.tagKey)}/${encodeURIComponent(this.$route.params.tagFilter)}`;
        }
        url += '/users';
        return url;
      },
      getUserDisplay(props) {
        const userDisplay = props.userIdForDisplay ? props.userIdForDisplay : props.userId;
        const { oAuthProviders } = this.$store.getters.config;
        if (oAuthProviders) {
          const indexOfDash = userDisplay.lastIndexOf('-');
          if (indexOfDash > 0) {
            const provider = userDisplay.substr(indexOfDash + 1);
            if (oAuthProviders.includes(provider)) {
              return userDisplay.substr(0, indexOfDash);
            }
          }
        }
        return userDisplay;
      },
    },
  };
</script>

<style>

</style>
