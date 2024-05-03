<script setup>
import { computed, nextTick, ref } from 'vue'
import PanelMenu from 'primevue/panelmenu'
import OverlayPanel from 'primevue/overlaypanel'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

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
      command: () => {
        onSelected('pendingApproval')
      }
    },
    {
      icon: 'fas fa-tag',
      key: 'hasTag',
      label: 'Has a Tag',
      count: 0,
      command: () => {
        onSelected('hasTag')
      }
    }
  ]
  if (skillDisplayInfo.isSubjectPage.value) {
    res.push({
      icon: 'fas fa-award',
      key: 'belongsToBadge',
      label: 'Belongs to a Badge',
      count: 0,
      command: () => {
        onSelected('belongsToBadge')
      }
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
          command: () => {
            onSelected('withoutProgress')
          }
        },
        {
          icon: 'far fa-check-circle',
          key: 'complete',
          label: 'Completed',
          count: 0,
          command: () => {
            onSelected('complete')
          }
        },
        {
          icon: 'fas fa-running',
          key: 'inProgress',
          label: 'In Progress',
          count: 0,
          command: () => {
            onSelected('inProgress')
          }
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
          command: () => {
            onSelected('approval')
          }
        },
        {
          icon: 'fas fa-person-booth',
          key: 'honorSystem',
          label: 'Honor System',
          count: 0,
          command: () => {
            onSelected('honorSystem')
          }
        },
        {
          icon: 'fas fa-spell-check',
          key: 'quiz',
          label: 'Quiz',
          count: 0,
          command: () => {
            onSelected('quiz')
          }
        },
        {
          icon: 'fas fa-file-contract',
          key: 'survey',
          label: 'Survey',
          count: 0,
          command: () => {
            onSelected('survey')
          }
        },
        {
          icon: 'fas fa-video',
          key: 'video',
          label: 'Video',
          count: 0,
          command: () => {
            onSelected('video')
          }
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

  flattenedFilters.forEach((filter) => {
    filter.disabled = filter.count === 0
  })
}

const toggle = (event) => {
  menu.value.toggle(event)
}

const filteredSelection = ref({})
const menu = ref()
const expandedKeys = ref({ progressGroup: true })
const onSelected = (id) => {
  emit('filter-selected', id)
  menu.value.hide()
  const flattenedFilters = filters.value.map((group) => group.items).flat()
  const filter = flattenedFilters.find((item) => item.key === id)
  filteredSelection.value = filter
}
const clearSelection = () => {
  filteredSelection.value = {}
  emit('clear-filter')
}
const focusOnProgressGroup = () => {
  nextTick(() => {
    const element = document.querySelector('[aria-label="Skill Progress Filter"]')
    if (element) {
      element.focus()
    }
  })
}

</script>

<template>
  <div class="skills-theme-filter-menu" data-cy="filterMenu">
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
              removable
              data-cy="selectedFilter"/>
      </div>
    </div>
    <OverlayPanel ref="menu" @show="focusOnProgressGroup">
      <div>
        <PanelMenu :model="filters" class="w-full md:w-20rem" v-model:expandedKeys="expandedKeys">
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


    <!--    <Menu ref="menu" id="overlay_menu" :model="filters" :popup="true">-->
    <!--      <template #submenuheader="{ item }">-->
    <!--        <span class="text-primary font-bold">{{ item.label }}</span>-->
    <!--      </template>-->
    <!--      <template #item="{ item, props }">-->
    <!--        <a class="flex align-items-center" v-bind="props.action">-->
    <!--&lt;!&ndash;          <i :class="item.icon" aria-hidden="true"/>&ndash;&gt;-->
    <!--          <Avatar :icon="item.icon" class="" size="small"  />-->
    <!--&lt;!&ndash;          <span class="ml-2">{{ item.label }}</span>&ndash;&gt;-->
    <!--          <span class="ml-2" v-html="item.label" />-->
    <!--&lt;!&ndash;          <Badge v-if="item.badge" class="ml-auto" :value="item.badge" />&ndash;&gt;-->
    <!--&lt;!&ndash;          <span v-if="item.shortcut" class="ml-auto border-1 surface-border border-round surface-100 text-xs p-1">{{ item.shortcut }}</span>&ndash;&gt;-->
    <!--        </a>-->
    <!--      </template>-->
    <!--    </Menu>-->

  </div>
</template>

<style scoped>

</style>