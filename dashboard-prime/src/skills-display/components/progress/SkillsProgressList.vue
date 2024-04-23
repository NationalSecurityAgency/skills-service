<script setup>
import { onMounted, ref, computed } from 'vue'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import { useScrollSkillsIntoViewState } from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'

const props = defineProps({
  subject: {
    type: Object,
    required: true
  },
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

const searchString = ref('')
const showDescriptionsInternal = ref(false)


let filter = () => true
if (props.projectId) {
  filter = (s) => s.projectId === props.projectId
}
const skillsInternal = ref([])
onMounted(() => {
  skillsInternal.value = props.subject.skills.filter(filter).map((item) => {
    const isSkillsGroupType = item.type === 'SkillsGroup'
    if (isSkillsGroupType) {
      // eslint-disable-next-line no-param-reassign
      item.children = item.children.map((child) => ({ ...child, groupId: item.skillId, isSkillType: true }))
    }
    const res = {
      ...item, subject: props.subject, isSkillsGroupType, isSkillType: !isSkillsGroupType
    }
    updateMetaCountsForSkillRes(res)
    return res
  })

  scrollToLastViewedSkill(400)
})
const updateMetaCountsForSkillRes = (skillRes) => {
  if (skillRes.isSkillsGroupType) {
    skillRes.children.forEach((childItem) => {
      updateMetaCounts(childItem.meta)
    })
  } else {
    updateMetaCounts(skillRes.meta)
  }
}

const metaCounts = ref({
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
})
const updateMetaCounts = (meta) => {
  if (meta.complete) {
    metaCounts.value.complete += 1
  }
  if (meta.withoutProgress) {
    metaCounts.value.withoutProgress += 1
  }
  if (meta.inProgress) {
    metaCounts.value.inProgress += 1
  }
  if (meta.belongsToBadge) {
    metaCounts.value.belongsToBadge += 1
  }
  if (meta.pendingApproval) {
    metaCounts.value.pendingApproval += 1
  }
  if (meta.hasTag) {
    metaCounts.value.hasTag += 1
  }
  if (meta.approval) {
    metaCounts.value.approval += 1
  }
  if (meta.honorSystem) {
    metaCounts.value.honorSystem += 1
  }
  if (meta.quiz) {
    metaCounts.value.quiz += 1
  }
  if (meta.survey) {
    metaCounts.value.survey += 1
  }
  if (meta.video) {
    metaCounts.value.video += 1
  }
}

const lastViewedButtonDisabled = ref(false)
const scrollToLastViewedSkill = (timeout = null) => {
  if (scrollIntoViewState.lastViewedSkillId) {
    const found = skillsInternal.value.find((skill) => skill.skillId === scrollIntoViewState.lastViewedSkillId)
    if (found) {
      scrollIntoViewState.scrollToLastViewedSkill(timeout)
    }
  }
}
const hasLastViewedSkill = computed(() => {
  let lastViewedSkill = null
  if (skillsInternal.value) {
    skillsInternal.value.forEach((item) => {
      if (item.isLastViewed === true) {
        lastViewedSkill = item
      } else if (item.type === 'SkillsGroup' && !lastViewedSkill) {
        lastViewedSkill = item.children.find((childItem) => childItem.isLastViewed === true)
      }
    })
  }
  return lastViewedSkill
})
</script>

<template>
  <Card data-cy="skillsProgressList" v-if="(skillsInternal.length > 0 || searchString || showNoDataMsg)">
    <template #header>
      <div class="row" v-if="skillsInternal && skillsInternal.length > 0">
        <!--        <div class="col-md-auto text-left pr-md-0">-->
        <!--          <div class="d-flex">-->
        <!--            <b-form-input @input="searchSkills" style="padding-right: 2.3rem;"-->
        <!--                          v-model="searchString"-->
        <!--                          :placeholder="`Search ${this.skillDisplayName.toLowerCase()}s`"-->
        <!--                          :aria-label="`Search ${this.skillDisplayName}s`"-->
        <!--                          data-cy="skillsSearchInput"></b-form-input>-->
        <!--            <b-button v-if="searchString && searchString.length > 0" @click="clearSearch"-->
        <!--                      class="position-absolute skills-theme-btn" variant="outline-info" style="right: 0rem;"-->
        <!--                      data-cy="clearSkillsSearchInput">-->
        <!--              <i class="fas fa-times"></i>-->
        <!--              <span class="sr-only">clear search</span>-->
        <!--            </b-button>-->
        <!--          </div>-->
        <!--        </div>-->
        <div class="col-md text-left my-2 my-md-0 ml-md-0 pl-md-0">
          <!--                <skills-filter :counts="metaCounts"-->
          <!--                               :filters="filters"-->
          <!--                               @filter-selected="filterSkills"-->
          <!--                               @clear-filter="clearFilters"/>-->

          <!--                <b-button v-if="!loading.userSkills && isLastViewedScrollSupported && hasLastViewedSkill"-->
          <SkillsButton
            v-if="hasLastViewedSkill"
            icon="fas fa-eye"
            label="Last Viewed"
            :disabled="lastViewedButtonDisabled"
            @click.prevent="scrollToLastViewedSkill"
            class="skills-theme-btn"
            outlined
            size="small"
            serverit="info"
            :aria-label="`Jump to Last Viewed Skill`"
            data-cy="jumpToLastViewedButton" />
        </div>
        <!--        <div class="col-md-auto text-right skill-details-toggle" data-cy="skillDetailsToggle">-->
        <!--          <span class="text-muted pr-1">{{ skillDisplayName }} Details:</span>-->
        <!--          <toggle-button class="" v-model="showDescriptionsInternal" @change="onDetailsToggle"-->
        <!--                         :color="{ checked: '#007c49', unchecked: '#6b6b6b' }"-->
        <!--                         :aria-label="`Show ${this.skillDisplayName} Details`"-->
        <!--                         :labels="{ checked: 'On', unchecked: 'Off' }" data-cy="toggleSkillDetails"/>-->
        <!--        </div>-->
        <!--      </div>-->
        <!--      <div v-if="selectedTagFilters.length > 0" class="row mt-2">-->
        <!--        <div class="col-md-auto text-left pr-md-0">-->
        <!--          <b-badge v-for="(tag, index) in selectedTagFilters"-->
        <!--                   :data-cy="`skillTagFilter-${index}`"-->
        <!--                   :key="tag.tagId"-->
        <!--                   variant="light"-->
        <!--                   class="mx-1 py-1 border-info border selected-filter overflow-hidden">-->
        <!--            <i :class="'fas fa-tag'" class="ml-1"></i> <span v-html="tag.tagValue"></span>-->
        <!--            <button type="button" class="btn btn-link p-0" @click="removeTagFilter(tag)" :data-cy="`clearSelectedTagFilter-${tag.tagId}`">-->
        <!--              <i class="fas fa-times-circle ml-1"></i>-->
        <!--              <span class="sr-only">clear filter</span>-->
        <!--            </button>-->
        <!--          </b-badge>-->
        <!--        </div>-->
      </div>
    </template>
    <template #content>
      <!--      <skills-spinner :loading="loading"/>-->
      <!--      <div v-if="!loading">-->
      <div v-if="skillsInternal && skillsInternal.length > 0">
        <div v-for="(skill, index) in skillsInternal"
             :key="`skill-${skill.skillId}`"
             :id="`skillRow-${skill.skillId}`"
             class="skills-theme-bottom-border-with-background-color"
             :class="{
                 'separator-border-thick' : showDescriptionsInternal,
                 'border-bottom' : (index + 1) !== skillsInternal.length
               }"
        >
          <div class="p-3 pt-4">
            <!--            :show-group-descriptions="showGroupDescriptions"-->
            <!--            @points-earned="onPointsEarned"-->
            <!--            @add-tag-filter="addTagFilter"-->
            <skill-progress
              :id="`skill-${skill.skillId}`"
              :ref="`skillProgress${skill.skillId}`"
              :skill="skill"
              :subjectId="subject.subjectId"
              :badgeId="subject.badgeId"
              :type="type"
              :enable-drill-down="true"
              :show-description="showDescriptionsInternal"
              :data-cy="`skillProgress_index-${index}`"
              :badge-is-locked="badgeIsLocked"
              :child-skill-highlight-string="searchString"
              :video-collapsed-by-default="true"
            />
          </div>
        </div>
      </div>
      <!--        <no-data-yet v-if="!(skillsInternal && skillsInternal.length > 0) && (searchString || Boolean(this.selectedTagFilters.length))" class="my-5"-->
      <!--                     icon="fas fa-search-minus fa-5x" title="No results">-->
      <!--          <span v-if="searchString">-->
      <!--            Please refine [{{searchString}}] search  <span v-if="filterId || Boolean(this.selectedTagFilters.length)">and/or clear the selected filter</span>-->
      <!--          </span>-->
      <!--          <span v-if="!searchString">-->
      <!--           Please clear selected filters-->
      <!--          </span>-->
      <!--        </no-data-yet>-->

      <!--        <no-data-yet v-if="!(skillsInternalOrig && skillsInternalOrig.length > 0) && showNoDataMsg" class="my-5"-->
      <!--                     :title="`${this.skillDisplayName}s have not been added yet.`"-->
      <!--                     :sub-title="`Please contact this ${this.projectDisplayName.toLowerCase()}'s administrator.`"/>-->
      <!--      </div>-->
    </template>
  </Card>
</template>

<style scoped>

</style>