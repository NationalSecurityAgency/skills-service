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
             :stacked="options.stacked"
             :per-page="options.pagination.server ? 0 : pageSizeInternal"
             :current-page="options.pagination.server ? null : currentPageInternal"
             :hide-goto-end-buttons="options.pagination.server ? true : false"
             @sort-changed="sortingChanged"
             :no-sort-reset="true"
             :no-local-sorting="options.pagination.server"
             thead-class="accessible"
             show-empty>
      <colgroup v-if="options.rowDetailsControls"><col style="width: 2rem;"><col></colgroup>
      <template v-if="options.rowDetailsControls" v-slot:cell(b_table_controls)="data">
        <b-button size="sm" @click="data.toggleDetails" class="mr-2" :aria-label="`Expand details`">
          <i v-if="data.detailsShowing" class="fa fa-minus-square" />
          <i v-else class="fa fa-plus-square" />
        </b-button>
      </template>

      <template v-slot:table-busy>
        <div class="text-center text-info my-5" style="min-height: 15rem; z-index: 9999">
          <div>
            <b-spinner class="align-middle"></b-spinner>
          </div>
          <div class="mt-1" style="color:darkslategray">
            <strong>Loading...</strong>
          </div>
        </div>
      </template>

      <template v-slot:empty="scope">
        <div class="text-center text-info mt-5" style="min-height: 12rem" data-cy="emptyTable">
          <div class="mb-2">
            <i class="fas fa-dragon fa-3x border border-info rounded p-4 bg-light text-muted" />
          </div>
          <h4 class="align-middle">{{ options.emptyText ? options.emptyText : scope.emptyText }}</h4>
        </div>
      </template>

      <!-- use named slots with b-table component -->
      <slot v-for="slot in Object.keys($slots)" :name="slot" :slot="slot"/>

      <!-- use scoped slots to the b-table component -->
      <template v-for="slot in Object.keys($scopedSlots)" :slot="slot" slot-scope="scope">
        <slot :name="slot" v-bind="scope"/>
      </template>

    </b-table>
    <div v-if="!options.busy && !options.pagination.remove" class="row m-1 p-0 align-items-center">
      <div class="col-md text-center text-md-left">
        <span class="text-muted">Total Rows:</span> <strong data-cy="skillsBTableTotalRows">{{ totalRows | number }}</strong>
      </div>
      <div class="col-md my-3 my-md-0">
        <span v-if="!options.pagination.remove">
          <b-pagination v-model="currentPageInternal" :total-rows="totalRows"
                        :per-page="pageSizeInternal" slot-scope=""
                        pills align="center" size="sm" variant="info" class="customPagination m-0 p-0"
                        :disabled="disabled" data-cy="skillsBTablePaging" aria-label="table pagination">
          </b-pagination>
        </span>
      </div>
      <div class="col-md text-center text-md-right">
        <span v-if="!options.pagination.remove">
          <label :for="`pagination_select_${uid}`" class="text-muted">Per page:</label>
          <b-form-select :id="`pagination_select_${uid}`" v-model="pageSizeInternal" :options="options.pagination.possiblePageSizes"
                         size="sm" class="mx-2" style="width: 4rem;" :disabled="disabledPaging"
                         data-cy="skillsBTablePageSize" />
        </span>
      </div>
    </div>
  </div>
</template>

<script>
  let uid = 0;

  export default {
    name: 'SkillsBTable',
    props: ['items', 'options'],
    beforeCreate() {
      this.uid = uid.toString();
      uid += 1;
    },
    mounted() {
      this.updateColumns();
    },
    data() {
      return {
        fieldsInternal: [],
        currentPageInternal: this.options.pagination.currentPage,
        pageSizeInternal: this.options.pagination.pageSize,
      };
    },
    computed: {
      disabled() {
        return this.isDisabled();
      },
      disabledPaging() {
        const minPageSizeAvailable = Math.min(...this.options.pagination.possiblePageSizes);
        return this.isDisabled() || (this.options.pagination.totalRows <= minPageSizeAvailable);
      },
      totalRows() {
        return this.options.pagination.server ? this.options.pagination.totalRows : this.items.length;
      },
    },
    methods: {
      isDisabled() {
        return !this.items || this.items.length === 0;
      },
      sortingChanged(ctx) {
        // ctx.sortBy   ==> Field key for sorting by (or null for no sorting)
        // ctx.sortDesc ==> true if sorting descending, false otherwise
        this.currentPageInternal = 1;
        this.$emit('sort-changed', ctx);
      },
      updateColumns() {
        const newFields = [];
        if (this.options.rowDetailsControls) {
          newFields.push({
            key: 'b_table_controls',
            sortable: false,
            label: '',
            class: 'control-column',
            headerTitle: 'Expand for additional details',
          });
        }

        this.options.fields.forEach((item) => {
          newFields.push(item);
        });
        this.fieldsInternal = newFields;
      },
    },
    watch: {
      currentPageInternal() {
        this.$emit('page-changed', this.currentPageInternal);
      },
      pageSizeInternal() {
        this.currentPageInternal = 1;
        this.$emit('page-size-changed', this.pageSizeInternal);
      },
      'options.fields': function updateColumns() {
        this.updateColumns();
      },
    },
  };
</script>

<style scoped>
.skills-b-table /deep/ .control-column {
  max-width: 3rem !important;
}

.skills-b-table /deep/ .accessible th {
  color: #264653 !important;
}
</style>
