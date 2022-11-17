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
  <metrics-card title="Users that Achieved this Skill" data-cy="postAchievementUserList">
    <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="No achievements yet for this skill.">
      <skills-b-table :items="postAchievementUsers" :options="tableOptions" data-cy="postAchievementUsers-table" tableStoredStateId="postAchievementUsers-table"
                          @page-changed="pageChanged"
                          @page-size-changed="pageSizeChanged"
                          @sort-changed="sortTable">
        <template v-slot:cell(userId)="data">
          {{ data.item.userId }}

          <b-button-group class="float-right">
            <b-button :to="calculateClientDisplayRoute(data.item)"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover="'View User Details'"
                      :aria-label="`View details for user ${data.item}`"
                      data-cy="usersTable_viewDetailsBtn"><i class="fa fa-user-alt" aria-hidden="true"/><span class="sr-only">view user details</span>
            </b-button>
          </b-button-group>
        </template>
      </skills-b-table>
    </metrics-overlay>
  </metrics-card>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'PostAchievementUsersTable',
    components: { MetricsOverlay, MetricsCard, SkillsBTable },
    props: ['skillName'],
    data() {
      return {
        postAchievementUsers: [],
        tableOptions: {
          busy: false,
          sortBy: 'userId',
          sortDesc: true,
          bordered: true,
          outlined: true,
          rowDetailsControls: false,
          stacked: 'md',
          fields: [
            {
              key: 'userId',
              label: 'User',
              sortable: true,
            },
            {
              key: 'count',
              label: 'Times Used',
              sortable: true,
            },
            {
              key: 'date',
              label: 'Date Last Used',
              sortable: true,
            },
          ],
          pagination: {
            currentPage: 1,
            totalRows: 1,
            pageSize: 5,
            server: true,
            possiblePageSizes: [5, 10, 15, 20, 50],
          },
          tableDescription: 'Skill Metrics',
        },
        loading: true,
        hasData: false,
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.loading = true;
        MetricsService.loadChart(this.$route.params.projectId, 'usagePostAchievementUsersBuilder', {
          skillId: this.$route.params.skillId, page: this.tableOptions.pagination.currentPage, pageSize: this.tableOptions.pagination.pageSize, sortDesc: this.tableOptions.sortDesc, sortBy: this.tableOptions.sortBy,
        })
          .then((dataFromServer) => {
            if (dataFromServer) {
              this.hasData = true;
              this.tableOptions.pagination.totalRows = dataFromServer.totalCount;
              this.postAchievementUsers = dataFromServer.users;
            }
            this.loading = false;
          });
      },
      pageChanged(pageNum) {
        this.tableOptions.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.tableOptions.pagination.pageSize = newSize;
        this.loadData();
      },
      sortTable(sortContext) {
        const sortBy = sortContext.sortBy === 'userId' ? 'user_id' : sortContext.sortBy;
        this.tableOptions.sortBy = sortBy;
        this.tableOptions.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.tableOptions.pagination.currentPage = 1;
        this.loadData();
      },
      calculateClientDisplayRoute(props) {
        return {
          name: 'ClientDisplayPreviewSkill',
          params: {
            projectId: this.$route.params.projectId,
            subjectId: this.$route.params.subjectId,
            skillId: this.$route.params.skillId,
            userId: props.userId,
            dn: props.dn,
          },
        };
      },
    },
  };
</script>

<style scoped>

</style>
