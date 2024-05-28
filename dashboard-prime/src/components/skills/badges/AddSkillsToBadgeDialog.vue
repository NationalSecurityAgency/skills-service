<script setup>
import { computed, onMounted, ref, toRaw } from 'vue'
import { useRoute } from 'vue-router'
import Stepper from 'primevue/stepper'
import StepperPanel from 'primevue/stepperpanel'
import SkillsService from '@/components/skills/SkillsService.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useFocusState } from '@/stores/UseFocusState.js'
import BadgesService from '@/components/badges/BadgesService.js'
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useErrorChecker } from '@/components/utils/errors/UseErrorChecker.js'

const props = defineProps({
  skills: {
    type: Array,
    required: true
  }
})
const model = defineModel()
const route = useRoute()
const pluralSupport = useLanguagePluralSupport()
const focusState = useFocusState()
const errorChecker = useErrorChecker()
const emits = defineEmits(['on-added'])

const addedSkills = ref([])

const onCancel = () => {
  model.value = false

  if (addedSkills.value && addedSkills.value.length > 0) {
    emits('on-added', { moved: toRaw(addedSkills.value), destination: toRaw(selectedDestination.value) })
  }

  handleFocus()
}

const state = ref({
  addedAlready: false,
  inProgress: false,
  errorOnSave: false,
})
const loadingDest = ref(true)
const destinations = ref([])
const selectedDestination = ref({})
const loadDestinations = () => {
  BadgesService.getBadges(route.params.projectId)
    .then((res) => {
      destinations.value = res
    })
    .finally(() => {
      loadingDest.value = false
    })
}
const isLoadingData = computed(() => loadingDest.value)

onMounted(() => {
  loadDestinations()
})

const hasDestinations = computed(() => destinations.value && destinations.value.length > 0)
const showStepper = computed(() => !state.value.addedAlready && hasDestinations.value)

const onVisibleChanged = (isVisible) => {
  if (!isVisible) {
    handleFocus()
  }
}
const handleFocus = () => {
  focusState.focusOnLastElement()
}

const learningPathViolationErr = ref({
  show: false,
  skillName: ''
})

const loadingExistingBadgeSkills = ref(true)
const skillsForBadge = ref({
  available: [],
  alreadyExist: [],
  allAlreadyExist: [],
  skillsWithLearningPathViolations: []
})
const selectDestination = (selection, moveToNextStep) => {
  moveToNextStep()
  const projectId = route.params.projectId
  const badgeId = selection.badgeId
  SkillsService.getBadgeSkills(projectId, badgeId)
    .then((res) => {
      skillsForBadge.value.allAlreadyExist = res
      skillsForBadge.value.alreadyExist = props.skills.filter((skill) => res.find((e) => e.skillId === skill.skillId))
      const availableSkills = props.skills.filter((skill) => !res.find((e) => e.skillId === skill.skillId))
      if (availableSkills.length > 0) {
        availableSkills.forEach((skill) => {
          if (!learningPathViolationErr.value.show) {
            SkillsService.validateDependency(projectId, badgeId, skill.skillId, projectId).then((dependencyRes) => {
              if (!dependencyRes.possible && dependencyRes.failureType !== 'NotEligible') {
                skillsForBadge.value.skillsWithLearningPathViolations.push(skill)
              }
            })
          }
        })
        skillsForBadge.value.available = availableSkills
      }
    })
    .finally(() => {
      loadingExistingBadgeSkills.value = false
    })
}

const addSkillsToBadge = (navToNextStep) => {
  state.value.inProgress = true;
  navToNextStep()
  const skillIds = skillsForBadge.value.available.map((sk) => sk.skillId);
  SkillsService.assignSkillsToBadge(route.params.projectId, selectedDestination.value.badgeId, skillIds, false)
    .then(() => {
      state.value.addSkillsToBadgeInProgress = false;
      state.value.addSkillsToBadgeComplete = true;
      addedSkills.value = skillsForBadge.value.available

      const groupId = skillsForBadge.value.available[0].groupId
      const focusOn = groupId ? `group-${groupId}_newSkillBtn` : 'newSkillBtn'
      focusState.setElementId(focusOn)

      SkillsReporter.reportSkill('AssignGemOrBadgeSkills');
    })
    .catch((e) => {
      if (errorChecker.isLearningPathErrorCode(e)) {
        learningPathViolationErr.value.show = true;
        learningPathViolationErr.value.skillName = skillsForBadge.value.available.find((sk) => sk.skillId === e.response.data.skillId)?.name;
        state.value.errorOnSave = true;
      } else {
        throw e
      }
    })
    .finally(() => {
      state.value.inProgress = false
    })
}
</script>

<template>
  <Dialog
    modal
    @update:visible="onVisibleChanged"
    header="Add Skills to Badge"
    :maximizable="true"
    :close-on-escape="true"
    class="w-11 xl:w-8"
    v-model:visible="model"
    :pt="{ maximizableButton: { 'aria-label': 'Expand to full screen and collapse back to the original size of the dialog' } }"
  >
    <div data-cy="addSkillsToBadgeModalContent">
      <skills-spinner :is-loading="isLoadingData" class="my-8" />

      <div v-if="!isLoadingData" class="w-100">

        <no-content2
          v-if="!hasDestinations"
          class="my-5"
          title="No Badges Available"
          data-cy="noBadgesAvailable"
          message="There are no Badges available. A badge must be created before adding skills to it." />

        <Stepper v-if="showStepper" :linear="true" class="w-100">
          <StepperPanel header="Select Destination">
            <template #content="{ nextCallback }">
              <div data-cy="addSkillsToBadgeModalStep1">
                <Listbox
                  v-model="selectedDestination"
                  :options="destinations"
                  :filter="destinations.length > 4"
                  :filter-fields="['subjectName', 'groupName']"
                  listStyle="max-height:250px"
                  @update:modelValue="selectDestination($event, nextCallback)"
                  class="w-full">
                  <template #option="item">
                    <div :data-cy="`selectDest_${item.option.badgeId}`">
                      <i class="fas fa-cubes" aria-hidden="true" />
                      <span class="font-italic ml-1">Badge:</span>
                      <span class="ml-1 font-semibold text-primary">{{
                          item.option.name
                        }}</span>
                    </div>
                  </template>
                </Listbox>
                <div class="flex pt-4 justify-content-end">
                  <SkillsButton
                    label="Cancel"
                    icon="far fa-times-circle"
                    outlined
                    class="mr-2"
                    severity="warning"
                    data-cy="closeButton"
                    @click="onCancel" />
                  <SkillsButton
                    label="Add"
                    :disabled="true"
                    data-cy="addSkillsToBadgeButton"
                    icon="fas fa-arrow-circle-right"
                    outlined />
                </div>
              </div>
            </template>
          </StepperPanel>
          <StepperPanel header="Preview">
            <template #content="{ nextCallback }">
              <skills-spinner
                v-if="loadingExistingBadgeSkills"
                :is-loading="loadingExistingBadgeSkills" class="my-8" />
              <div
                data-cy="addSkillsToBadgeModalStep2"
                v-else>
                <div
                  class="p-4 border-2 border-dashed surface-border border-round surface-ground flex-auto flex flex-column gap-2 justify-content-center align-items-center font-medium">
                  <div
                    v-if="skillsForBadge.skillsWithLearningPathViolations.length === 0 && skillsForBadge.available.length > 0 && !state.errorOnSave">
                    <Tag>{{ skillsForBadge.available.length }}</Tag>
                    skill{{ pluralSupport.plural(skillsForBadge.available) }} will be added to the
                    <span><span class="text-primary font-semibold">[{{ selectedDestination.name }}]</span> badge.</span>
                  </div>
                  <Message v-else :closable="false" severity="warn">
                    Selected skills can NOT be added to the
                    <span class="text-primary font-semibold">{{ selectedDestination.name }} </span> badge.
                    Please cancel and select different skills.
                  </Message>
                  <div
                    v-if="skillsForBadge.skillsWithLearningPathViolations.length === 0 && skillsForBadge.alreadyExist.length > 0">
                    <Tag severity="warning">{{ skillsForBadge.alreadyExist.length }}</Tag>
                    selected skill{{ pluralSupport.pluralWithHave(skillsForBadge.alreadyExist) }} <span
                    class="text-primary font-weight-bold">already</span> been added to that badge!
                  </div>
                  <Message
                    v-for="(skill) in skillsForBadge.skillsWithLearningPathViolations"
                    :key="skill.skillId"
                    :closable="false"
                    :data-cy="`learningPathErrMsg-${skill.skillId}`"
                    severity="error"
                  >
                    Unable to add <b>{{ skill.name }}</b> skill to the badge.
                    Adding this skill would result in a <b>circular/infinite learning path</b>.
                    Please visit project's
                    <router-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path
                    </router-link>
                    page to review.
                  </Message>
                </div>
                <div class="flex pt-4 justify-content-end">
                  <SkillsButton
                    label="Cancel"
                    icon="far fa-times-circle"
                    outlined
                    class="mr-2"
                    severity="warning"
                    data-cy="closeButton"
                    @click="onCancel" />
                  <SkillsButton
                    label="Add"
                    :disabled="!selectedDestination || state.addSkillsToBadgeInProgress || (skillsForBadge.available && skillsForBadge.available.length === 0) || skillsForBadge.skillsWithLearningPathViolations.length > 0"
                    @click="addSkillsToBadge(nextCallback)"
                    data-cy="addSkillsToBadgeButton"
                    icon="fas fa-arrow-circle-right"
                    outlined />
                </div>
              </div>
            </template>
          </StepperPanel>
          <StepperPanel header="Confirmation">
            <template #content>
              <div data-cy="addSkillsToBadgeModalStep3">
                <skills-spinner :is-loading="state.inProgress" />
                <div v-if="!state.inProgress">
                  <Message
                    v-if="learningPathViolationErr.show"
                    :closable="false"
                    severity="error"
                    data-cy="learningPathErrMsg">
                    Failed to add <b>{{ learningPathViolationErr.skillName }}</b> skill to the badge.
                    Adding this skill would result in a <b>circular/infinite learning path</b>.
                    Please visit project's
                    <router-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path
                    </router-link>
                    page to review.
                  </Message>
                  <div
                    v-if="!learningPathViolationErr.show"
                    class="p-4 border-2 border-dashed surface-border border-round surface-ground flex-auto flex flex-column gap-2 justify-content-center align-items-center font-medium">
                    <div>
                      <span class="text-success">Successfully</span> added
                      <Tag>{{ skillsForBadge.available.length }}</Tag>
                      skill{{ pluralSupport.plural(skillsForBadge.available) }} to the <span>
                    <span class="text-primary font-semibold">[{{ selectedDestination.name }}]</span> badge.</span>
                    </div>
                  </div>
                  <div class="flex pt-4 justify-content-end">
                    <SkillsButton
                      label="OK"
                      icon="fas fa-thumbs-up"
                      @click="onCancel"
                      data-cy="okButton"
                      outlined />
                  </div>
                </div>
              </div>
            </template>
          </StepperPanel>
        </Stepper>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.p-stepper {
  flex-basis: 100rem;
}
</style>