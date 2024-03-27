<script setup>
import { useSlots, toRef } from 'vue'
import DataTable from 'primevue/datatable'
import { useStorage } from '@vueuse/core'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const sortField = defineModel('sortField')
const sortOrder = defineModel('sortOrder')
const emit = defineEmits(['sort', 'filter', 'page'])
const props = defineProps({
  tableId: {
    type: String,
    required: true
  }
})
const slots = useSlots()
const announcer = useSkillsAnnouncer()

const sortInfo = useStorage(`skillsTable-sort-${props.tableId}`, { sortOrder: sortOrder.value, sortBy: sortField.value })

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

    <template #footer>
      sortField: {{ sortField }}, sortInfo: {{ sortInfo.sortBy }}
    </template>
    <template v-for="(_, name) in slots" v-slot:[name]="slotData">
      <slot :name="name" v-bind="{...slotData}" />
    </template>
  </DataTable>
</template>

<style scoped>

</style>