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

// subject: {
//   type: Object,
//     required: true
// },
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
const searchString = ref('')

let filter = () => true
if (props.projectId) {
  filter = (s) => s.projectId === props.projectId
}
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
    if (found) {
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
// const updateSkillForLoadedDescription = (skills, desc) => {
//   let foundSkill = null
//   console.log(desc)
//   for (let i = 0; i < skills.length; i += 1) {
//     const skill = skills[i]
//     if (desc.skillId === skill.skillId) {
//       foundSkill = skill
//     } else if (skill.isSkillsGroupType) {
//       foundSkill = skill.children.find((child) => desc.skillId === child.skillId)
//     }
//     if (foundSkill) {
//       break
//     }
//   }
//
//   console.log(foundSkill)
//   console.log('----------------------')
//   if (foundSkill) {
//     foundSkill.description = desc
//     foundSkill.achievedOn = desc.achievedOn
//     foundSkill.videoSummary = desc.videoSummary
//   }
// }
const onDetailsToggle = () => {
  if (!descriptionsLoaded.value) {
    loading.value = true
    skillsDisplayService.getDescriptions(subject.value.subjectId || props.subject.badgeId, props.type)
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
    }).map((item) => ({ ...item, children: item.children?.map((child) => ({ ...child })) }))

    // resultSkills = foundItems.map((item) => {
    //   const skillHtml = searchStrNormalized ? StringHighlighter.highlight(item.skill, searchStrNormalized) : null
    //   return skillHtml ? ({ ...item, skillHtml }) : item
    // })
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
  // this.skillsInternal = resultSkills
  return resultSkills
})

const showDescriptionsInternal = ref(false)
const isLastViewedScrollSupported = computed(() => {
  return !parentFrame.parentFrame || parentFrame.isLastViewedScrollSupported
})
// this.lastViewedButtonDisabled = resultSkills.findIndex((i) => i.isLastViewed || (i.children && i.children.findIndex((c) => c.isLastViewed) >= 0)) < 0

</script>

<template>
  <Card data-cy="skillsProgressList"
        :class="{'skills-display-test-link': skillsDisplayInfo.isLocalTestPath()}"
        v-if="(skillsInternal && skillsInternal.length > 0 || searchString || showNoDataMsg)">
    <template #header>
      <div class="px-4 pt-3">
        <div class="flex"
             v-if="skillsInternal && skillsInternal.length > 0">
          <div class="flex-1">
            <div class="flex">
              <div class="">
                <InputGroup>
                  <InputText
                    v-model="searchString"
                    :placeholder="`Search ${attributes.skillDisplayName.toLowerCase()}s`"
                    :aria-label="`Search ${attributes.skillDisplayName}s`"
                    data-cy="skillsSearchInput" />
                  <InputGroupAddon class="p-0 m-0">
                    <SkillsButton
                      icon="fas fa-times"
                      text
                      outlined
                      @click="searchString = ''"
                      class="position-absolute skills-theme-btn" variant="outline-info" style="right: 0rem;"
                      data-cy="clearSkillsSearchInput" />
                  </InputGroupAddon>
                </InputGroup>
              </div>
              <div class="ml-2">
                <skill-type-filter @filter-selected="setFilterId" :skills="skillsInternal"
                                   @clear-filter="filterId = ''" />
              </div>
                <SkillsButton
                  v-if="hasLastViewedSkill && isLastViewedScrollSupported"
                  icon="fas fa-eye"
                  label="Last Viewed"
                  :disabled="lastViewedButtonDisabled"
                  @click.prevent="scrollToLastViewedSkill"
                  class="skills-theme-btn ml-2"
                  outlined
                  size="small"
                  serverit="info"
                  :aria-label="`Jump to Last Viewed Skill`"
                  data-cy="jumpToLastViewedButton" />
            </div>
          </div>

          <div class="" data-cy="skillDetailsToggle">
            <div class="flex align-content-center">
              <span class="text-muted pr-1 align-content-center">{{ attributes.skillDisplayName }} Details:</span>
              <InputSwitch v-model="showDescriptionsInternal"
                           @change="onDetailsToggle"
                           :aria-label="`Show ${attributes.skillDisplayName} Details`"
                           data-cy="toggleSkillDetails" />
            </div>
          </div>

        </div>
        <div v-if="selectedTagFilters.length > 0" class="flex mt-2">
          <div class="">
            <Chip
              v-for="(tag, index) in selectedTagFilters"
              :label="tag.tagValue"
              icon="fas fa-tag"
              :data-cy="`skillTagFilter-${index}`"
              :key="tag.tagId"
              class="py-0 pl-0 pr-3 mr-2"
              @remove="removeTagFilter(tag)"
              outlined
              removable>
              <span class="bg-primary border-circle w-2rem h-2rem flex align-items-center justify-content-center"><i
                class="fas fa-tag" /></span>
              <span class="ml-2 font-medium">{{ tag.tagValue }}</span>
            </Chip>
          </div>
        </div>
      </div>
    </template>
    <template #content>
      <!--      <skills-spinner :loading="loading"/>-->
      <!--      <div v-if="!loading">-->
      <div v-if="skillsToShow.length > 0">
        <div v-for="(skill, index) in skillsToShow"
             :key="`skill-${skill.skillId}`"
             :id="`skillRow-${skill.skillId}`"
             class="skills-theme-bottom-border-with-background-color"
        >
          <div class="p-3 pt-4">
            <!--            :show-group-descriptions="showGroupDescriptions"-->
            <!--            @points-earned="onPointsEarned"-->
            <!--            @add-tag-filter="addTagFilter"-->
            <!--            :subjectId="subject.subjectId"-->
            <!--            :badgeId="subject.badgeId"-->

            <skill-progress
              :id="`skill-${skill.skillId}`"
              :ref="`skillProgress${skill.skillId}`"
              :skill="skill"
              :type="type"
              :enable-drill-down="true"
              :show-description="showDescriptionsInternal"
              :data-cy="`skillProgress_index-${index}`"
              :badge-is-locked="badgeIsLocked"
              :child-skill-highlight-string="searchString"
              :video-collapsed-by-default="true"
              @add-tag-filter="addTagFilter"
            />
          </div>
        </div>
      </div>

      <no-content-2
        v-if="!(skillsToShow && skillsToShow.length > 0) && (searchString || Boolean(selectedTagFilters.length))"
        class="my-5"
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
        :title="`${attributes.skillDisplayName}s have not been added yet.`"
        :message="`Please contact this ${attributes.projectDisplayName.toLowerCase()}'s administrator.`" />
    </template>
  </Card>
</template>

<style>
.skills-display-test-link a {
  color: #295bac !important;
}
</style>

<style scoped>

</style>