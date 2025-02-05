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
import { computed } from 'vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import ListFilterMenu from '@/skills-display/components/utilities/ListFilterMenu.vue'

const attributes = useSkillsDisplayAttributesState()
const skillDisplayInfo = useSkillsDisplayInfo()
const props = defineProps({
  skills: {
    type: Array,
    required: true
  }
})
const emit = defineEmits(['filter-selected', 'clear-filter'])

const metaCounts = computed(() => {
  const res = {
    complete: 0,
    withoutProgress: 0,
    inProgress: 0,
    belongsToBadge: 0,
    pendingApproval: 0,
    hasTag: 0,
    approval: 0,
    honorSystem: 0,
    quiz: 0,
    survey: 0,
    video: 0
  }

  props.skills.forEach((skillRes) => {
    if (skillRes.isSkillsGroupType) {
      skillRes.children.forEach((childItem) => {
        updateMetaCounts(res, childItem.meta)
      })
    } else {
      updateMetaCounts(res, skillRes.meta)
    }
  })

  return res
})

const updateMetaCounts = (metaCounts, meta) => {
  if (meta.complete) {
    metaCounts.complete += 1
  }
  if (meta.withoutProgress) {
    metaCounts.withoutProgress += 1
  }
  if (meta.inProgress) {
    metaCounts.inProgress += 1
  }
  if (meta.belongsToBadge) {
    metaCounts.belongsToBadge += 1
  }
  if (meta.pendingApproval) {
    metaCounts.pendingApproval += 1
  }
  if (meta.hasTag) {
    metaCounts.hasTag += 1
  }
  if (meta.approval) {
    metaCounts.approval += 1
  }
  if (meta.honorSystem) {
    metaCounts.honorSystem += 1
  }
  if (meta.quiz) {
    metaCounts.quiz += 1
  }
  if (meta.survey) {
    metaCounts.survey += 1
  }
  if (meta.video) {
    metaCounts.video += 1
  }
}

const createAttributeFilterItems = () => {
  const res = [
    {
      icon: 'fas fa-check',
      key: 'pendingApproval',
      label: 'Pending Approval',
      count: 0,
    },
    {
      icon: 'fas fa-tag',
      key: 'hasTag',
      label: 'Has a Tag',
      count: 0,
    }
  ]
  if (skillDisplayInfo.isSubjectPage.value) {
    res.push({
      icon: 'fas fa-award',
      key: 'belongsToBadge',
      label: 'Belongs to a Badge',
      count: 0,
    })
  }
  return res
}

const filters = computed(() => {
  const filters = [
    {
      key: 'progressGroup',
      label: `${attributes.skillDisplayName} Progress Filter`,
      items: [
        {
          icon: 'fas fa-battery-empty',
          key: 'withoutProgress',
          label: 'Without Progress',
          count: 0,
        },
        {
          icon: 'far fa-check-circle',
          key: 'complete',
          label: 'Completed',
          count: 0,
        },
        {
          icon: 'fas fa-running',
          key: 'inProgress',
          label: 'In Progress',
          count: 0,
        }
      ]
    },
    {
      key: 'attributeGroups',
      label: `${attributes.skillDisplayName} Attribute Filter`,
      items: createAttributeFilterItems()
    },
    {
      key: 'selfReportGroups',
      label: `${attributes.skillDisplayName} Self Reporting Filter`,
      items: [
        {
          icon: 'fas fa-user-check',
          key: 'approval',
          label: 'Approval',
          count: 0,
        },
        {
          icon: 'fas fa-person-booth',
          key: 'honorSystem',
          label: 'Honor System',
          count: 0,
        },
        {
          icon: 'fas fa-spell-check',
          key: 'quiz',
          label: 'Quiz',
          count: 0,
        },
        {
          icon: 'fas fa-file-contract',
          key: 'survey',
          label: 'Survey',
          count: 0,
        },
        {
          icon: 'fas fa-play-circle',
          key: 'video',
          label: 'Media',
          count: 0,
        }
      ]
    }
  ]
  updateFiltersWithMeta(filters, metaCounts.value)
  return filters
})

const updateFiltersWithMeta = (filters, meta) => {
  const keys = Object.keys(meta)
  const flattenedFilters = filters.map((group) => group.items).flat()
  keys.map((key) => {
    const filter = flattenedFilters.find((item) => item.key === key)
    // some of the filters are optionally configured based on the page
    if (filter) {
      filter.count = meta[key]
    }
    return key
  })
}


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