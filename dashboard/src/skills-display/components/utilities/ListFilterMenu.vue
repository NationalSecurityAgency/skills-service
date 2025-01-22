/*
Copyright 2024 SkillTree

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
<script setup>

import {Popover, PanelMenu} from "primevue";
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
    <div class="flex content-center">
      <Button
        icon="fas fa-filter"
        @click="toggle"
        outlined
        severity="info"
        data-cy="filterBtn"
        aria-label="Open type filter"
        aria-haspopup="true" />
      <div v-if="filteredSelection?.label" class="ml-2 content-center">
        <Chip :label="filteredSelection.label"
              :icon="filteredSelection.icon"
              @remove="clearSelection"
              class="whitespace-nowrap"
              removable
              data-cy="selectedFilter"/>
      </div>
    </div>
    <Popover id="typeSelectorPanel" ref="menu" @show="focusOnProgressGroup" aria-label="Type Selector Menu">
      <div>
        <PanelMenu :model="filtersInternal" class="w-full md:w-80" v-model:expandedKeys="expandedKeys">
          <template #item="{ item, props, root, active }">
            <div text v-if="root" class="p-4" :id="item.key" :data-cy="`filter_${item.key}`">
              <i v-if="!active" class="far fa-arrow-alt-circle-right"></i>
              <i v-else class="far fa-arrow-alt-circle-down"></i>
              <span class="ml-2" v-html="item.label" />
            </div>
            <div v-else class="flex items-center pl-4 p-2" v-bind="props.action" :data-cy="`filter_${item.key}`">
              <Avatar v-if="item.icon" :icon="item.icon" class="" size="small" />
              <div class="flex-1">
                <span class="ml-2">{{ item.label}}</span>
              </div>
              <Tag data-cy="filterCount">{{ item.count }}</Tag>
            </div>
          </template>
        </PanelMenu>
      </div>
    </Popover>

  </div>
</template>

<style scoped>

</style>