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
  <b-card body-class="p-0 mt-3">
    <div class="row px-3 mb-3">
      <div class="col">
        <b-button variant="outline-info" @click="changeSelectionForAll(true)" data-cy="users-filterBtn" class="mr-2 mt-1"><i class="fa fa-check-square"/> Select All</b-button>
        <b-button variant="outline-info" @click="changeSelectionForAll(false)" data-cy="users-filterBtn" class="mt-1"><i class="far fa-square"></i> Clear All</b-button>
      </div>
      <div class="col text-right">
        <b-button variant="outline-danger" @click="changeSelectionForAll(false)" data-cy="users-filterBtn" class="mt-1" :disabled="actionsDisabled"><i class="fa fa-times-circle"/> Reject</b-button>
        <b-button variant="outline-success" @click="changeSelectionForAll(false)" data-cy="users-filterBtn" class="mt-1 ml-2" :disabled="actionsDisabled"><i class="fa fa-check"/> Approve</b-button>
      </div>
    </div>

    <skills-b-table :options="table.options" :items="table.items"
                    @page-changed="pageChanged"
                    @page-size-changed="pageSizeChanged"
                    @sort-changed="sortTable"
                    data-cy="usersTable">

      <template v-slot:cell(userId)="data">
        <b-form-checkbox
          :id="`${data.item.userId}-${data.item.skillId}`"
          v-model="data.item.selected"
          :name="`checkbox--${data.item.skillId}`"
          :value="true"
          :unchecked-value="false"
          :inline="true"
          v-on:input="updateActionsDisableStatus"
        >
          {{ data.value }}
        </b-form-checkbox>
      </template>

      <template v-slot:cell(request)="data">
        <div>{{ data.item.skillName }}</div>
        <div class="small text-secondary">ID: {{ data.item.skillId }}</div>
        <div class="mt-2" style="font-size: 0.9rem;"><span class="text-secondary">Note:</span>
          <span v-if="data.item.note && data.item.note.length > 0"> {{ data.item.note }}</span>
          <span class="text-muted"> Not supplied</span>
        </div>
      </template>

      <template v-slot:cell(reportedOn)="data">
        <date-cell :value="data.value" />
      </template>

    </skills-b-table>
  </b-card>
</template>

<script>
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import DateCell from '../../utils/table/DateCell';

  export default {
    name: 'SelfReportApproval',
    components: { DateCell, SkillsBTable },
    data() {
      return {
        actionsDisabled: true,
        table: {
          items: [{
            userId: 'flyingGoon',
            skillId: 'InterestingSkill',
            skillName: 'What a skill',
            reportedOn: new Date().getTime(),
            note: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sodales faucibus justo non scelerisque. '
              + 'Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Fusce et laoreet massa, '
              + 'in condimentum augue. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi molestie venenatis nisi id '
              + 'venenatis. Sed aliquet erat purus, sed interdum ipsum aliquet et. '
              + 'Pellentesque venenatis felis nisl, ac feugiat neque commodo quis. Nulla ut tellus sit amet odio aliquet consectetur aliquet quis mauris. Nunc imperdiet id turpis nec tincidunt. ',
          }, {
            userId: 'flyingGoon',
            skillId: 'InterestingSkill1',
            skillName: 'What a skill',
            reportedOn: new Date().getTime(),
            note: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sodales faucibus justo non scelerisque. '
              + 'Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Fusce et laoreet massa, '
              + 'in condimentum augue. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi molestie venenatis nisi id '
              + 'venenatis. Sed aliquet erat purus, sed interdum ipsum aliquet et. '
              + 'Pellentesque venenatis felis nisl, ac feugiat neque commodo quis. Nulla ut tellus sit amet odio aliquet consectetur aliquet quis mauris. Nunc imperdiet id turpis nec tincidunt. ',
          }, {
            userId: 'flyingGoon',
            skillId: 'InterestingSkill2',
            skillName: 'What a skill',
            reportedOn: new Date().getTime(),
            note: '',
          }],
          options: {
            busy: true,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'userId',
            sortDesc: false,
            fields: [
              {
                key: 'userId',
                label: 'User Id',
                sortable: true,
              },
              {
                key: 'request',
                label: 'Requested',
                sortable: true,
              },
              {
                key: 'reportedOn',
                label: 'Reported On',
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
      this.table.options.busy = false;
      console.log(this.table.items);
      this.table.items = this.table.items.map((item) => ({ selected: false, ...item }));
      console.log(this.table.items);
    },
    methods: {
      changeSelectionForAll(selectedValue) {
        this.table.items.forEach((item) => {
          // eslint-disable-next-line no-param-reassign
          item.selected = selectedValue;
        });
        this.updateActionsDisableStatus();
      },
      updateActionsDisableStatus() {
        if (this.table.items.find((item) => item.selected) !== undefined) {
          this.actionsDisabled = false;
        } else {
          this.actionsDisabled = true;
        }
      },
    },
  };
</script>

<style scoped>

</style>
