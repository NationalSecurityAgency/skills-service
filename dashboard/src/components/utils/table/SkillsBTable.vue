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
             :fields="this.fieldsInternal">
      <colgroup v-if="options.rowDetailsControls"><col style="width: 2rem;"><col></colgroup>
      <template v-if="options.rowDetailsControls" v-slot:cell(b_table_controls)="data">
        <b-button size="sm" @click="data.toggleDetails" class="mr-2">
          <i v-if="data.detailsShowing" class="fa fa-minus-square" />
          <i v-else class="fa fa-plus-square" />
        </b-button>
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
      </div>
      <div class="col">
        <b-pagination v-model="options.pagination.currentPage" :total-rows="options.pagination.totalRows"
                      :per-page="options.pagination.perPage" slot-scope=""
                      pills align="center" size="sm" variant="info" class="customPagination m-0 p-0">
        </b-pagination>
      </div>
      <div class="col text-right">
        <span class="text-muted">Rows:</span>
        <b-form-select v-model="options.pagination.perPage" :options="options.pagination.possiblePageSizes"
                       size="sm" class="mx-2" style="width: 4rem;"/>
        <b-button size="sm" v-b-tooltip.hover title="Download CSV" variant="outline-info"><i
          class="fas fa-download"></i></b-button>
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
  };
</script>

<style scoped>
.skills-b-table /deep/ .control-column {
  max-width: 3rem !important;
  /*margin-right: 0px !important;*/
  /*padding-right: 0px !important;*/
}
</style>
