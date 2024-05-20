<script setup>
import { computed } from 'vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import ListFilterMenu from '@/skills-display/components/utilities/ListFilterMenu.vue'

const attributes = useSkillsDisplayAttributesState()
const props = defineProps({
  badges: {
    type: Array,
    required: true
  }
})
const emit = defineEmits(['filter-selected', 'clear-filter'])

const filters = computed(() => {
  const projectBadges =  {
    icon: 'fas fa-list-alt',
    key: 'projectBadges',
    label: `${attributes.projectDisplayName} Badges`,
    count: 0,
  }
  const gems = {
    icon: 'fas fa-gem',
    key: 'gems',
    label: 'Gems',
    count: 0,
  }
  const globalBadges = {
    icon: 'fas fa-globe',
    key: 'globalBadges',
    label: 'Global Badges',
    count: 0,
  }
  const items = [projectBadges, gems, globalBadges]
  props.badges.forEach((badge) => {
    items.forEach((item) => {
      if (badge.badgeTypes.includes(item.key)){
        item.count += 1
      }
    })
  });

  const filters = [
    {
      key: 'progressGroup',
      label: 'Badge Filters',
      items
    },
  ]

  return filters
})


const onSelected = (id) => { emit('filter-selected', id) }
const clearSelection = () => { emit('clear-filter') }

</script>

<template>
  <list-filter-menu
    :filters="filters"
    @filter-selected="onSelected"
    @clear-filter="clearSelection"/>
</template>

<style scoped>

</style>