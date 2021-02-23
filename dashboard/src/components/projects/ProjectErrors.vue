/*
Copyright 2021 SkillTree

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
  <div id="projectErrorsPanel">
    <sub-page-header title="Project Issues">
      <div class="row">
        <div class="col">
          <b-tooltip target="remove-button" title="Remove all project errors." :disabled="errors.length < 1"></b-tooltip>
          <span id="remove-button" class="mr-2">
            <b-button variant="outline-primary" ref="removeAllErrors" @click="removeAllErrors" :disabled="errors.length < 1" size="sm"
                      data-cy="removeAllErrors">
              <span class="d-none d-sm-inline">Remove</span> All <i class="text-warning fas fa-trash-alt" aria-hidden="true"/>
            </b-button>
          </span>
        </div>
      </div>
    </sub-page-header>

    <b-card body-class="p-0">
      <skills-spinner :is-loading="loading" />

      <skills-b-table v-if="!loading" :options="table.options" :items="errors" data-cy="projectErrorsTable">
        <template v-slot:cell(reportedSkillId)="data">
          {{ data.value }}
        </template>

        <template v-slot:cell(created)="data">
          {{ data.value }}
        </template>

        <template v-slot:cell(lastSeen)="data">
          {{ data.value }}
        </template>

        <template v-slot:cell(count)="data">
          {{ data.value }}
        </template>

        <template #cell(edit)="data">
          <b-button :ref="`delete_${data.item.reportedSkillId}`" @click="removeError(data.item)" variant="outline-info" size="sm"
                    data-cy="deleteErrorButton">
            <i class="text-warning fas fa-trash-alt"/> Delete
          </b-button>
        </template>

      </skills-b-table>
    </b-card>
  </div>

</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SkillsBTable from '../utils/table/SkillsBTable';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import ProjectService from './ProjectService';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'ProjectErrors',
    components: { SkillsBTable, SkillsSpinner, SubPageHeader },
    mixins: [MsgBoxMixin],
    props: [],
    data() {
      return {
        loading: true,
        errors: [],
        table: {
          options: {
            pagination: {
              remove: true,
            },
            fields: [
              {
                key: 'reportedSkillId',
                label: 'Non-existent Skill ID',
                sortable: false,
              }, {
                key: 'created',
                label: 'First Seen',
                sortable: false,
              }, {
                key: 'lastSeen',
                label: 'Last Seen',
                sortable: false,
              }, {
                key: 'count',
                label: 'Times Seen',
                sortable: false,
              }, {
                key: 'edit',
                label: 'Delete',
                sortable: false,
              },

            ],
          },
        },
      };
    },
    mounted() {
      this.loadErrors();
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      loadErrors() {
        this.loading = true;
        ProjectService.getProjectErrors(this.$route.params.projectId).then((res) => {
          this.errors = res;
        }).finally(() => {
          this.loading = false;
        });
      },
      removeAllErrors() {
        const msg = 'Are you absolutely sure you want to remove all Project issues?';
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.loading = true;
              ProjectService.deleteAllProjectErrors(this.$route.params.projectId).then(() => {
                this.loadErrors();
                this.loadProjectDetailsState({ projectId: this.$route.params.projectId });
              });
            }
          });
      },
      removeError(projectError) {
        const msg = `Are you absolutely sure you want to remove issue related to ${projectError.reportedSkillId}?`;
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.loading = true;
              ProjectService.deleteProjectError(projectError.projectId, projectError.reportedSkillId).then(() => {
                this.loadErrors();
                this.loadProjectDetailsState({ projectId: this.$route.params.projectId });
              });
            }
          });
      },
    },
  };
</script>
