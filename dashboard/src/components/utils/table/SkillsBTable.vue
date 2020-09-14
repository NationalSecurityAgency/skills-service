<template>
  <div>
    <b-table striped :items="items"
             :busy="options.busy"
             :sort-by.sync="options.sortBy" :sort-desc.sync="options.sortDesc"
             :bordered="true" :outlined="true"
             :fields="options.fields"
             head-variant="light" class="mb-0">
      <template v-slot:table-busy>
        <div class="text-center text-info my-2">
          <b-spinner class="align-middle"></b-spinner>
          <p>
            <strong>Loading...</strong>
          </p>
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
  };
</script>

<style scoped>

</style>
