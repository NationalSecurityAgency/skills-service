<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import { useScrollSkillsIntoViewState } from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'

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
const preferences = useSkillsDisplayPreferencesState()
const skillsDisplayService = useSkillsDisplayService()
const subjectAndSkillsState = useSkillsDisplaySubjectState()
const searchString = ref('')
const showDescriptionsInternal = ref(false)


let filter = () => true
if (props.projectId) {
  filter = (s) => s.projectId === props.projectId
}
const skillsInternal = computed(() => subjectAndSkillsState.subjectSummary.skills)
const subject = computed(() => subjectAndSkillsState.subjectSummary)
onMounted(() => {
  // skillsInternal.skills.forEach((skill) => {
  //   updateMetaCountsForSkillRes(res)
  // })
  scrollToLastViewedSkill(400)
})
watch(() => skillsInternal?.value?.skills, () => {
  skillsInternal.value.skills.forEach((item) => {
    updateMetaCountsForSkillRes(item)
  })
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

const descriptionsLoaded = ref(false)
const loading = ref(false)
const descriptions = ref([])
const updateSkillForLoadedDescription = (skills, desc) => {
  let foundSkill = null;
  for (let i = 0; i < skills.length; i += 1) {
    const skill = skills[i];
    if (desc.skillId === skill.skillId) {
      foundSkill = skill;
    } else if (skill.isSkillsGroupType) {
      foundSkill = skill.children.find((child) => desc.skillId === child.skillId);
    }
    if (foundSkill) {
      break;
    }
  }

  if (foundSkill) {
    foundSkill.description = desc;
    foundSkill.achievedOn = desc.achievedOn;
    foundSkill.videoSummary = desc.videoSummary;
  }
}
const onDetailsToggle = () => {
  if (!descriptionsLoaded.value) {
    loading.value = true;
    skillsDisplayService.getDescriptions(subject.value.subjectId || props.subject.badgeId, props.type)
      .then((res) => {
        descriptions.value = res;
        res.forEach((desc) => {
          updateSkillForLoadedDescription(skillsInternal.value, desc);
          // updateSkillForLoadedDescription(this.skillsInternalOrig, desc);
        });
        descriptionsLoaded.value = true;
      })
      .finally(() => {
        loading.value = false;
      });
  }
}

</script>

<template>
  <Card data-cy="skillsProgressList"
        v-if="(skillsInternal && skillsInternal.length > 0 || searchString || showNoDataMsg)">
    <template #header>
      <div class="flex px-4 pt-3"
           v-if="skillsInternal && skillsInternal.length > 0">
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
        <div class="flex-1">
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
        <div class="" data-cy="skillDetailsToggle">
          <div class="flex align-content-center">
            <span class="text-muted pr-1 align-content-center">{{ preferences.skillDisplayName }} Details:</span>
            <InputSwitch v-model="showDescriptionsInternal"
                         @change="onDetailsToggle"
                         :aria-label="`Show ${preferences.skillDisplayName} Details`"
                         data-cy="toggleSkillDetails"/>
          </div>


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
        </div>
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
            />

<!--            <Divider />-->
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