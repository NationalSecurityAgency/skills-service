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
import { computed, onMounted, ref } from 'vue'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import { useScrollSkillsIntoViewState } from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import SkillTypeFilter from '@/skills-display/components/skill/SkillTypeFilter.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useRoute } from 'vue-router'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'
import {useMatomoSupport} from "@/stores/UseMatomoSupport.js";
import MatomoEvents from "@/utils/MatomoEvents.js";

const props = defineProps({
  showDescriptions: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: 'subject'
  },
  projectId: {
    type: String,
    default: null,
    required: false
  },
  showNoDataMsg: {
    type: Boolean,
    default: true,
    required: false
  },
  badgeIsLocked: {
    type: Boolean,
    default: false,
    required: false
  }
})

const scrollIntoViewState = useScrollSkillsIntoViewState()
const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const subjectAndSkillsState = useSkillsDisplaySubjectState()
const parentFrame = useSkillsDisplayParentFrameState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const route = useRoute()
const matomo = useMatomoSupport()
const themeHelper = useThemesHelper()
const searchString = ref('')

const skillsInternal = computed(() => subjectAndSkillsState.subjectSummary.skills)
const subject = computed(() => subjectAndSkillsState.subjectSummary)
onMounted(() => {
  scrollToLastViewedSkill(400)
})

const lastViewedButtonDisabled = computed(() => !findLastViewedSkill(skillsToShow.value))
const scrollToLastViewedSkill = (timeout = null) => {
  if (scrollIntoViewState.lastViewedSkillId) {
    const found = skillsInternal.value.find((skill) => {
      return skill.skillId === scrollIntoViewState.lastViewedSkillId ||
        skill.children?.find((childItem) => childItem.skillId === scrollIntoViewState.lastViewedSkillId)
    })
    if (found && !skillsDisplayInfo.isGlobalBadgePage.value) {
      scrollIntoViewState.scrollToLastViewedSkill(timeout)
    }
  } else {
    // set it if backend provided
    skillsInternal.value.forEach((item) => {
      if (item.isLastViewed === true) {
        scrollIntoViewState.setLastViewedSkillId(item.skillId)
      } else if (item.type === 'SkillsGroup') {
        item.children.forEach((childItem) => {
          if (childItem.isLastViewed === true) {
            scrollIntoViewState.setLastViewedSkillId(childItem.skillId)
          }
        })
      }
    })
  }
}
const hasLastViewedSkill = computed(() => findLastViewedSkill(skillsInternal.value))
const findLastViewedSkill = (skillsToCheck) => {
  let lastViewedSkill = null
  if (skillsToCheck) {
    skillsToCheck.forEach((item) => {
      if (item.isLastViewed === true || item.skillId === scrollIntoViewState.lastViewedSkillId) {
        lastViewedSkill = item
      } else if (item.type === 'SkillsGroup' && !lastViewedSkill) {
        lastViewedSkill = item.children.find((childItem) => childItem.isLastViewed === true || childItem.skillId === scrollIntoViewState.lastViewedSkillId)
      }
    })
  }
  return lastViewedSkill
}

const descriptionsLoaded = ref(false)
const loading = ref(false)
const descriptions = ref([])
const onDetailsToggle = () => {
  if (!descriptionsLoaded.value) {
    loading.value = true
    skillsDisplayService.getDescriptions(subject.value.subjectId || route.params.badgeId, props.type)
      .then((res) => {
        descriptions.value = res
        res.forEach((desc) => {
          subjectAndSkillsState.updateDescription(desc)
        })
        descriptionsLoaded.value = true
      })
      .finally(() => {
        loading.value = false
      })
  }
  matomo.trackEvent(MatomoEvents.category.ToggleSwitch, MatomoEvents.action.Toggle, 'Skill Details')
}

const selectedTagFilters = ref([])
const filterId = ref('')
const setFilterId = (newFilterId) => {
  filterId.value = newFilterId
}
const addTagFilter = (tag) => {
  if (!selectedTagFilters.value.find((elem) => elem.tagId === tag.tagId)) {
    selectedTagFilters.value.push(tag)
  }
}
const removeTagFilter = (tag) => {
  selectedTagFilters.value = selectedTagFilters.value.filter((elem) => elem.tagId !== tag.tagId)
}

const skillsToShow = computed(() => {
  let resultSkills = skillsInternal.value ? skillsInternal.value.map((item) => ({ ...item })) : []
  const hasTagSearch = Boolean(selectedTagFilters.value.length)
  if (hasTagSearch || (searchString.value && searchString.value.trim().length > 0)) {
    const searchStrNormalized = searchString.value.trim().toLowerCase()
    const tagFilters = Array.from(selectedTagFilters.value).map((tag) => tag.tagValue.toLowerCase())

    // groups are treated as a single unit (group and child skills shown OR the entire group is removed)
    // group is shown when either a group name matches OR any of the skill names match the search string
    resultSkills = resultSkills.filter((item) => {
      const foundSkill = item.skill?.trim()?.toLowerCase().includes(searchStrNormalized)
      if (item.isSkillsGroupType) {
        // if the group is not a match, find at least 1 child that matches the search string
        const foundGroup = foundSkill ? true : item.children.find((childItem) => childItem.skill?.trim()?.toLowerCase().includes(searchStrNormalized))
        if (foundGroup) {
          // if filtering on tags, we need to make sure the group children (as a unit) contains *all* tags
          if (hasTagSearch) {
            const tagsFound = new Set()
            for (let i = 0; i < item.children.length; i += 1) {
              const child = item.children[i]
              for (let j = 0; j < tagFilters.length; j += 1) {
                const tagFilter = tagFilters[j]
                if (child.tags?.find((tag) => tag?.tagValue?.trim()?.toLowerCase()?.includes(tagFilter))) {
                  tagsFound.add(tagFilter)
                }
                if (tagsFound.size >= tagFilters.length) {
                  break
                }
              }
              if (tagsFound.size >= tagFilters.length) {
                break
              }
            }
            return tagsFound.size >= tagFilters.length
          }
          return true
        }
      }
      if (foundSkill) {
        // if filtering on tags, we need to make sure the skill contains *all* tags
        if (hasTagSearch) {
          return tagFilters.every((tagFilter) => item.tags?.map((tag) => tag?.tagValue?.trim()?.toLowerCase()).includes(tagFilter))
        }
        return true
      }
      return false
    }).map((item) => ({ ...item, children: item.children?.filter((child) => {
      return child.skill?.trim()?.toLowerCase().includes(searchStrNormalized)
    })?.map((child) => ({ ...child })) }))
  }

  if (resultSkills && filterId.value && filterId.value.length > 0) {
    const filteredRes = []
    resultSkills.forEach((item) => {
      if (item.isSkillsGroupType) {
        const copyItem = ({ ...item })
        copyItem.children = copyItem.children.filter((childItem) => childItem.meta[filterId.value] === true)
        if (copyItem.children && copyItem.children.length > 0) {
          filteredRes.push(copyItem)
        }
      } else if (item.meta[filterId.value] === true) {
        filteredRes.push(item)
      }
    })
    resultSkills = filteredRes
  }
  return resultSkills
})

const showDescriptionsInternal = ref(false)
const isLastViewedScrollSupported = computed(() => {
  return !parentFrame.parentFrame || parentFrame.isLastViewedScrollSupported
})

const expandGroups = ref(null)
const hasGroups = computed(() => {
  return !!skillsInternal.value.find(it => it.type === 'SkillsGroup')
})

const expandAllGroups = (() => {
  expandGroups.value = true
})
const collapseAllGroups = (() => {
  expandGroups.value = false
})
const resetGroupExpansion = (() => {
  expandGroups.value = null
})

const items = [
  {
    label: 'Collapse All',
    icon: 'fas fa-minus',
    command: collapseAllGroups
  },
  {
    label: 'Expand All',
    icon: 'fas fa-plus',
    command: expandAllGroups
  },
]

const menu = ref()
const toggle = (event) => {
  menu.value.toggle(event)
}
</script>

<template>
  <Card data-cy="skillsProgressList"
        :class="{'skills-display-test-link': skillsDisplayInfo.isLocalTestPath() && themeHelper.isDarkTheme.value }"
        v-if="(skillsInternal && skillsInternal.length > 0 || searchString || showNoDataMsg)">
    <template #header>
      <h2 class="sr-only">Skills</h2>
      <div class="px-6 pt-4">
        <div class=" flex flex-wrap gap-4 flex-col md:flex-row"
             v-if="skillsInternal && skillsInternal.length > 0">
          <div class="flex-1">
            <div class="flex-col sm:flex-row flex gap-2">
              <div class="">
                <InputGroup class="p-0">
                  <InputText
                    v-model="searchString"
                    :placeholder="`Search ${attributes.skillDisplayName.toLowerCase()}s`"
                    :aria-label="`Search ${attributes.skillDisplayName}s`"
                    data-cy="skillsSearchInput" />
                  <InputGroupAddon class="p-0 m-0">
                    <SkillsButton :pt="{ root: { class: 'border-0!' } }"
                      icon="fas fa-times"
                      text
                      outlined
                      @click="searchString = ''"
                      class="position-absolute skills-theme-btn m-0 h-full"
                      aria-label="clear search input"
                      data-cy="clearSkillsSearchInput" />
                  </InputGroupAddon>
                </InputGroup>
              </div>
              <div class="">
                <skill-type-filter @filter-selected="setFilterId" :skills="skillsInternal"
                                   @clear-filter="filterId = ''" />
              </div>
              <div class="w-min-9rem">
                <SkillsButton
                  v-if="hasLastViewedSkill && isLastViewedScrollSupported && !skillsDisplayInfo.isGlobalBadgePage.value"
                  icon="fas fa-eye"
                  label="Last Viewed"
                  :disabled="lastViewedButtonDisabled"
                  @click.prevent="scrollToLastViewedSkill"
                  class="skills-theme-btn"
                  outlined
                  serverit="info"
                  :aria-label="`Jump to Last Viewed Skill`"
                  data-cy="jumpToLastViewedButton" />
              </div>
            </div>
          </div>


          <div class="" data-cy="skillDetailsToggle">
            <div class="flex flex-row flex-wrap content-center">
              <div class="flex flex-wrap mr-4 gap-2" v-if="!route.params.badgeId && hasGroups">
                <Button
                    outlined
                    raised
                    size="small"
                    @click="toggle"
                    aria-label="Group Controls"
                    aria-haspopup="true"
                    data-cy="groupToggle"
                    aria-controls="group_control_menu">
                  <i class="fas fa-list mr-1" aria-hidden="true"></i>
                  <span>{{ attributes.groupDisplayNamePlural }}</span>
                  <i class="fas fa-caret-down ml-2"></i>
                </Button>
                <div id="group_control_menu">
                  <Menu ref="menu" :model="items" :popup="true"></Menu>
                </div>
              </div>
              <div class="flex">
                <span class="text-muted pr-1 content-center">{{ attributes.skillDisplayName }} Details:</span>
                <ToggleSwitch v-model="showDescriptionsInternal"
                             @change="onDetailsToggle"
                             :aria-label="`Show ${attributes.skillDisplayName} Details`"
                             data-cy="toggleSkillDetails" />
              </div>
            </div>
          </div>

        </div>
        <div v-if="selectedTagFilters.length > 0" class="flex gap-2 mt-2">
            <Chip
              v-for="(tag, index) in selectedTagFilters"
              :label="tag.tagValue"
              icon="fas fa-tag"
              :data-cy="`skillTagFilter-${index}`"
              :key="tag.tagId"
              :pt="{ root: { class: 'p-0!'}}"
              @remove="removeTagFilter(tag)"
              outlined
              removable>
              <span class="bg-primary text-primary-contrast rounded-full w-7 h-7 flex items-center justify-center"><i
                class="fas fa-tag" aria-hidden="true"/></span>
              <span class="font-medium">{{ tag.tagValue }}</span>
            </Chip>
        </div>
      </div>
    </template>
    <template #content>
      <div v-if="skillsToShow.length > 0" class="skills-theme-progress-rows">
        <div v-for="(skill, index) in skillsToShow"
             :key="`skill-${skill.skillId}`"
             :id="`skillRow-${skill.skillId}`"
             class="skills-theme-bottom-border-with-background-color"
        >
          <div class="p-4 pt-6">
            <skill-progress
              :id="`skill-${skill.skillId}`"
              :ref="`skillProgress${skill.skillId}`"
              :skill="skill"
              :type="type"
              :expand-groups="expandGroups"
              @reset-group-expansion="resetGroupExpansion"
              :enable-drill-down="true"
              :show-description="showDescriptionsInternal"
              :data-cy="`skillProgress_index-${index}`"
              :badge-is-locked="badgeIsLocked"
              :child-skill-highlight-string="searchString"
              :video-collapsed-by-default="true"
              @add-tag-filter="addTagFilter"
              :index="index"
            />
          </div>
        </div>
      </div>

      <no-content-2
        v-if="!(skillsToShow && skillsToShow.length > 0) && (searchString || Boolean(selectedTagFilters.length))"
        class="my-8"
        icon="fas fa-search-minus" title="No results">
                      <span v-if="searchString">
                        Please refine [{{ searchString }}] search  <span
                        v-if="filterId || Boolean(selectedTagFilters.length)">and/or clear the selected filter</span>
                      </span>
        <span v-if="!searchString">
                       Please clear selected filters
                      </span>
      </no-content-2>

      <no-content2
        v-if="!(skillsInternal && skillsInternal.length > 0) && showNoDataMsg"
        :title="`${attributes.skillDisplayNamePlural} have not been added yet.`"
        :message="`Please contact this ${attributes.projectDisplayName.toLowerCase()}'s administrator.`" />
    </template>
  </Card>
</template>

<style>
body .skills-display-test-link a,
body .skills-display-test-link a:link,
body .skills-display-test-link a:visited,
body .skills-display-test-link a:focus,
body .skills-display-test-link a:hover,
body .skills-display-test-link a:active {
  color: #005efb !important;
}
</style>

<style scoped>

</style>