<script setup>

import OverlayPanel from 'primevue/overlaypanel'
import PanelMenu from 'primevue/panelmenu'
import { nextTick, onMounted, ref } from 'vue'

const props = defineProps({
  filters: Array,
})
const emit = defineEmits(['filter-selected', 'clear-filter'])

const firstGroup = props.filters[0]
const firstGroupKey = firstGroup.key
const firstGroupLabel = firstGroup.label

const filteredSelection = ref({})
const menu = ref()
const filtersInternal = ref([])


const expandedKeysDefault = {}
expandedKeysDefault[firstGroupKey] = true
const expandedKeys = ref(expandedKeysDefault)

const toggle = (event) => {
  menu.value.toggle(event)
}

const onSelected = (id) => {
  emit('filter-selected', id)
  menu.value.hide()
  const flattenedFilters = filtersInternal.value.map((group) => group.items).flat()
  const filter = flattenedFilters.find((item) => item.key === id)
  filteredSelection.value = filter
}
onMounted(() => {
  const newFilters = props.filters.map(group => ({ ...group }))
  newFilters.forEach(group => {
    group.items.forEach(item => {
      item.command = () => {
        onSelected(item.key)
      }
      item.disabled = item.count === 0
    })
  });

  filtersInternal.value = newFilters
})


const clearSelection = () => {
  filteredSelection.value = {}
  emit('clear-filter')
}

const focusOnProgressGroup = () => {
  nextTick(() => {
    const element = document.querySelector(`[aria-label="${firstGroupLabel}"]`)
    if (element) {
      element.focus()
    }
  })
}

</script>

<template>
  <div v-if="filtersInternal" class="skills-theme-filter-menu" data-cy="filterMenu">
    <div class="flex align-content-center">
      <Button
        icon="fas fa-filter"
        @click="toggle"
        outlined
        severity="info"
        data-cy="filterBtn"
        aria-haspopup="true"
        aria-controls="overlay_menu" />
      <div v-if="filteredSelection?.label" class="ml-2 align-content-center">
        <Chip :label="filteredSelection.label"
              :icon="filteredSelection.icon"
              @remove="clearSelection"
              class="white-space-nowrap"
              removable
              data-cy="selectedFilter"/>
      </div>
    </div>
    <OverlayPanel ref="menu" @show="focusOnProgressGroup">
      <div>
        <PanelMenu :model="filtersInternal" class="w-full md:w-20rem" v-model:expandedKeys="expandedKeys">
          <template #item="{ item, props, root, active }">
            <div text v-if="root" class="p-3" :id="item.key" :data-cy="`filter_${item.key}`">
              <i v-if="!active" class="far fa-arrow-alt-circle-right"></i>
              <i v-else class="far fa-arrow-alt-circle-down"></i>
              <span class="ml-2" v-html="item.label" />
            </div>
            <div v-else class="flex align-items-center pl-3 p-2" v-bind="props.action" :data-cy="`filter_${item.key}`">
              <Avatar v-if="item.icon" :icon="item.icon" class="" size="small" />
              <div class="flex-1">
                <span class="ml-2">{{ item.label}}</span>
              </div>
              <Tag data-cy="filterCount">{{ item.count }}</Tag>
            </div>
          </template>
        </PanelMenu>
      </div>
    </OverlayPanel>

  </div>
</template>

<style scoped>

</style>