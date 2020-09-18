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
    <b-table striped head-variant="light" class="skills-b-table mb-0"
             :items="items"
             :busy="options.busy"
             :sort-by.sync="options.sortBy"
             :sort-desc.sync="options.sortDesc"
             :bordered="options.bordered"
             :outlined="options.outlined"
             :fields="this.fieldsInternal"
             show-empty>
      <colgroup v-if="options.rowDetailsControls"><col style="width: 2rem;"><col></colgroup>
      <template v-if="options.rowDetailsControls" v-slot:cell(b_table_controls)="data">
        <b-button size="sm" @click="data.toggleDetails" class="mr-2">
          <i v-if="data.detailsShowing" class="fa fa-minus-square" />
          <i v-else class="fa fa-plus-square" />
        </b-button>
      </template>

      <template v-slot:table-busy>
        <div class="text-center text-info my-5" style="min-height: 15rem; z-index: 9999">
          <div>
            <b-spinner class="align-middle"></b-spinner>
          </div>
          <div class="mt-1">
            <strong>Loading...</strong>
          </div>
        </div>
      </template>

      <template v-slot:empty="scope">
        <div class="text-center text-info my-5" style="min-height: 15rem">
          <div class="mb-2">
            <i class="fas fa-dragon fa-3x border border-info rounded p-4 bg-light text-muted" />
          </div>
          <h4 class="align-middle">{{ scope.emptyText }}</h4>
        </div>
      </template>

      <!-- use named slots with b-table component -->
      <slot v-for="slot in Object.keys($slots)" :name="slot" :slot="slot"/>

      <!-- use scoped slots to the b-table component -->
      <template v-for="slot in Object.keys($scopedSlots)" :slot="slot" slot-scope="scope">
        <slot :name="slot" v-bind="scope"/>
      </template>

    </b-table>
    <div v-if="!options.busy" class="row m-1 p-0 align-items-center">
      <div class="col">
        <span class="text-muted">Total Rows:</span> <strong>{{options.pagination.totalRows}}</strong>
      </div>
      <div class="col">
        <b-pagination v-model="options.pagination.currentPage" :total-rows="options.pagination.totalRows"
                      :per-page="options.pagination.pageSize" slot-scope=""
                      pills align="center" size="sm" variant="info" class="customPagination m-0 p-0"
                      :disabled="disabled">
        </b-pagination>
      </div>
      <div class="col text-right">
        <span class="text-muted">Page Size:</span>
        <b-form-select v-model="options.pagination.pageSize" :options="options.pagination.possiblePageSizes"
                       size="sm" class="mx-2" style="width: 4rem;" :disabled="disabled"/>
        <b-button size="sm" v-b-tooltip.hover title="Download CSV" variant="outline-info" :disabled="disabled">
          <i class="fas fa-download"></i>
        </b-button>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'SkillsBTable',
    props: ['items', 'options'],
    mounted() {
      this.fieldsInternal = [];
      if (this.options.rowDetailsControls) {
        this.fieldsInternal.push({
          key: 'b_table_controls',
          sortable: false,
          label: '',
          class: 'control-column',
        });
      }

      this.options.fields.forEach((item) => {
        this.fieldsInternal.push(item);
      });
    },
    data() {
      return {
        fieldsInternal: [],
      };
    },
    computed: {
      disabled() {
        return !this.items || this.items.length === 0;
      },
    },
  };
</script>

<style scoped>
.skills-b-table /deep/ .control-column {
  max-width: 3rem !important;
}
</style>
