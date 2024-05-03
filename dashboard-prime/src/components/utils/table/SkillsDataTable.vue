<script setup>
import { useSlots, toRef } from 'vue'
import { useStorage } from '@vueuse/core'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import DataTable from 'primevue/datatable'
import Column from 'primevue/column';

const sortField = defineModel('sortField')
const sortOrder = defineModel('sortOrder')
const emit = defineEmits(['sort', 'filter', 'page'])
const props = defineProps({
  tableStoredStateId: {
    type: String,
    required: true
  },
  expander: {
    type: Boolean,
    required: false,
    default: false,
  },
  expanderLabel: {
    type: String,
    required: false,
    default: 'Expand Rows',
  },
  expanderPt: {
    type: Object,
    required: false,
    default: null,
  }
})
const slots = useSlots()
const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()

const sortInfo = useStorage(`skillsTable-sort-${props.tableStoredStateId}`, { sortOrder: sortOrder.value, sortBy: sortField.value })

sortField.value = sortInfo.value.sortBy
sortInfo.value.sortBy = toRef(() => sortField.value)

sortOrder.value = sortInfo.value.sortOrder
sortInfo.value.sortOrder = toRef(() => sortOrder.value)

const onColumnSort = (sortEvent) => {
  emit('sort', sortEvent)
  announcer.polite(`Sorted by ${sortEvent.sortField} in ${sortEvent.sortOrder === 1 ? 'ascending' : 'descending'} order`)
}

const onFilter = (filterEvent) => {
  emit('filter', filterEvent)
  if (filterEvent.filters?.global?.value) {
    announcer.polite(`Filtered by ${filterEvent.filters?.global?.value} and returned ${filterEvent.filteredValue.length} results`)
  }
}

const onPage = (pageEvent) => {
  emit('page', pageEvent)
  announcer.polite(`Showing up to ${pageEvent.rows} rows on page ${pageEvent.page + 1}`)
}

</script>

<template>
  <DataTable
    v-model:sort-field="sortField"
    v-model:sort-order="sortOrder"
    @sort="onColumnSort"
    @filter="onFilter"
    @page="onPage"
  >
    <template v-for="(_, name) in slots" v-slot:[name]="slotData">
      <Column v-if="expander"
              expander
              :pt="expanderPt"
              :class="{'flex': responsive.md.value }">
        <template #header>
          <span class="mr-1 lg:mr-0 md:hidden"><i class="fas fa-expand-arrows-alt" aria-hidden="true"></i> {{ expanderLabel }}</span>
          <span class="mr-1 lg:mr-0 hidden md:block" :aria-label="expanderLabel">&#8203;</span>
        </template>
      </Column>

      <slot v-if="slotData" :name="name" v-bind="slotData" />
      <slot v-else :name="name" />
    </template>
  </DataTable>
</template>

<style scoped>

</style>