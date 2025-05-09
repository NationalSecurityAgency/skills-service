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
import { computed, onMounted, ref, toRaw } from 'vue'
import { useRoute } from 'vue-router'
import Stepper from 'primevue/stepper'
import StepList from 'primevue/steplist';
import StepPanels from 'primevue/steppanels';
import StepPanel from 'primevue/steppanel';
import Step from 'primevue/step';
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
const showStepper = computed(() => !state.value.addedAlready && hasDestinations.value && !hasDisabledSkillSelected.value)
const hasDisabledSkillSelected = computed(() => !!props.skills.find(skill => skill.enabled === false))

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
    class="w-11/12 xl:w-8/12"
    v-model:visible="model"
    :pt="{ maximizableButton: { 'aria-label': 'Expand to full screen and collapse back to the original size of the dialog' } }"
  >
    <div data-cy="addSkillsToBadgeModalContent">
      <skills-spinner :is-loading="isLoadingData" class="my-20" />

      <div v-if="!isLoadingData" class="w-full">

        <no-content2
          v-if="!hasDestinations"
          class="my-8"
          title="No Badges Available"
          data-cy="noBadgesAvailable"
          message="There are no Badges available. A badge must be created before adding skills to it." />

        <no-content2
            v-if="hasDisabledSkillSelected && hasDestinations"
            class="my-8"
            title="Cannot Add"
            data-cy="hasDisabledSkillSelected"
            message="Disabled skills cannot be added to a badge." />

        <Stepper v-if="showStepper" :linear="true" class="w-full" value="1">
          <StepList>
            <Step value="1">Select Destination</Step>
            <Step value="2">Preview</Step>
            <Step value="3">Confirmation</Step>
          </StepList>
          <StepPanels>
            <StepPanel value="1" v-slot="{ activateCallback }">
              <div data-cy="addSkillsToBadgeModalStep1">
                  <Listbox
                    v-model="selectedDestination"
                    :options="destinations"
                    :filter="destinations.length > 4"
                    :filter-fields="['subjectName', 'groupName']"
                    listStyle="max-height:250px"
                    @update:modelValue="selectDestination($event, () => activateCallback('2'))"
                    class="w-full">
                    <template #option="item">
                      <div :data-cy="`selectDest_${item.option.badgeId}`">
                        <i class="fas fa-cubes" aria-hidden="true" />
                        <span class="italic ml-1">Badge:</span>
                        <span class="ml-1 font-semibold text-primary">{{
                            item.option.name
                          }}</span>
                      </div>
                    </template>
                  </Listbox>
                  <div class="flex pt-6 justify-end">
                    <SkillsButton
                      label="Cancel"
                      icon="far fa-times-circle"
                      outlined
                      class="mr-2"
                      severity="warn"
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
            </StepPanel>
            <StepPanel value="2" v-slot="{ activateCallback }">
              <skills-spinner
                v-if="loadingExistingBadgeSkills"
                :is-loading="loadingExistingBadgeSkills" class="my-20" />
              <div
                  data-cy="addSkillsToBadgeModalStep2"
                  v-else>
                  <div
                    class="p-6 border-2 border-dashed border-surface rounded-border bg-surface-50 dark:bg-surface-950 flex-auto flex flex-col gap-2 justify-center items-center font-medium">
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
                      <Tag severity="warn">{{ skillsForBadge.alreadyExist.length }}</Tag>
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
                  <div class="flex pt-6 justify-end">
                    <SkillsButton
                      label="Cancel"
                      icon="far fa-times-circle"
                      outlined
                      class="mr-2"
                      severity="warn"
                      data-cy="closeButton"
                      @click="onCancel" />
                    <SkillsButton
                      label="Add"
                      :disabled="!selectedDestination || state.addSkillsToBadgeInProgress || (skillsForBadge.available && skillsForBadge.available.length === 0) || skillsForBadge.skillsWithLearningPathViolations.length > 0"
                      @click="addSkillsToBadge(() => activateCallback('3'))"
                      data-cy="addSkillsToBadgeButton"
                      icon="fas fa-arrow-circle-right"
                      outlined />
                  </div>
                </div>
            </StepPanel>
            <StepPanel value="3">
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
                    class="p-6 border-2 border-dashed border-surface rounded-border bg-surface-50 dark:bg-surface-950 flex-auto flex flex-col gap-2 justify-center items-center font-medium">
                    <div>
                      <span class="text-success">Successfully</span> added
                      <Tag>{{ skillsForBadge.available.length }}</Tag>
                      skill{{ pluralSupport.plural(skillsForBadge.available) }} to the <span>
                    <span class="text-primary font-semibold">[{{ selectedDestination.name }}]</span> badge.</span>
                    </div>
                  </div>
                  <div class="flex pt-6 justify-end">
                    <SkillsButton
                      label="OK"
                      icon="fas fa-thumbs-up"
                      @click="onCancel"
                      data-cy="okButton"
                      outlined />
                  </div>
                </div>
              </div>
            </StepPanel>
          </StepPanels>
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