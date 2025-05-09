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
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import ReuseOrMovePreview from '@/components/skills/reuseSkills/ReuseOrMovePreview.vue'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useFocusState } from '@/stores/UseFocusState.js'
import {useDialogUtils} from "@/components/utils/inputForm/UseDialogUtils.js";

const props = defineProps({
  skills: {
    type: Array,
    required: true
  },
  isReuseType: {
    type: Boolean,
    default: false
  }
})
const model = defineModel()
const route = useRoute()
const pluralSupport = useLanguagePluralSupport()
const focusState = useFocusState()
const emits = defineEmits(['on-moved'])

const textCustomization = props.isReuseType ?
  { actionName: 'Reuse', actionDirection: 'in' } :
  { actionName: 'Move', actionDirection: 'to' }

const actionNameLowerCase = computed(() => textCustomization.actionName.toLowerCase())
const actionNameInPast = computed(() => `${actionNameLowerCase.value}d`)
const actionDirection = computed(() => textCustomization.actionDirection)

const onCancel = () => {
  model.value = false

  if (movedOrReusedSkills.value && movedOrReusedSkills.value.length > 0) {
    emits('on-moved', { moved: toRaw(movedOrReusedSkills.value), destination: toRaw(selectedDestination.value) })
  }

  handleFocus()
}

const state = ref({
  skillsWereMovedOrReusedAlready: false,
  reUseInProgress: false,
})
const loadingDest = ref(true)
const destinations = ref([])
const selectedDestination = ref({})
const loadDestinations = () => {
  const skillId = props.skills[0].skillId
  SkillsService.getSkillInfo(route.params.projectId, skillId)
    .then((skillInfo) => {
      if (skillInfo.subjectId !== route.params.subjectId) {
        state.value.skillsWereMovedOrReusedAlready = true
        loadingDest.value = false
      } else {
        SkillsService.getReuseDestinationsForASkill(route.params.projectId, skillId)
          .then((res) => {
            destinations.value = res
            // this.updateDestinationPage(this.destinations.currentPageNum);
          })
          .finally(() => {
            loadingDest.value = false
          })
      }
    })
}
const loadingFinalizeInfo = ref(true)
const finalizeInfo = ref({})
const loadFinalizeInfo = () => {
  CatalogService.getCatalogFinalizeInfo(route.params.projectId)
    .then((res) => {
      finalizeInfo.value = res
    })
    .finally(() => {
      loadingFinalizeInfo.value = false
    })
}
const isLoadingData = computed(() => loadingDest.value || loadingFinalizeInfo.value)

onMounted(() => {
  loadDestinations()
  loadFinalizeInfo()
})

const movedOrReusedSkills = ref([])
const onReuseOrMove = (changedSkills) => {
  // after reuse/move action button will be disabled so need to focus on another button
  const groupId = props.skills[0].groupId
  const focusOn = groupId ? `group-${groupId}_newSkillBtn` : 'newSkillBtn'
  focusState.setElementId(focusOn)

  movedOrReusedSkills.value = changedSkills
}
const hasDestinations = computed(() => destinations.value && destinations.value.length > 0)
const showStepper = computed(() => !state.value.skillsWereMovedOrReusedAlready && hasDestinations.value && !importFinalizePending.value && (!hasDisabledSkillSelected.value || !props.isReuseType))
const importFinalizePending = computed(() => finalizeInfo.value.numSkillsToFinalize && finalizeInfo.value.numSkillsToFinalize > 0)
const hasDisabledSkillSelected = computed(() => !!props.skills.find(skill => skill.enabled === false))

const onVisibleChanged = (isVisible) => {
  if (!isVisible) {
    handleFocus()
  }
}
const handleFocus = () => {
  focusState.focusOnLastElement()
}
const dialogUtils = useDialogUtils()
</script>

<template>
  <Dialog
    modal
    @update:visible="onVisibleChanged"
    :header="`${textCustomization.actionName} Skills in this Project`"
    :maximizable="true"
    :close-on-escape="true"
    class="w-11/12 xl:w-8/12"
    v-model:visible="model"
    :pt="{ pcMaximizeButton: dialogUtils.getMaximizeButtonPassThrough() }"
  >
    <div data-cy="reuseOrMoveDialog">
      <skills-spinner :is-loading="isLoadingData" class="my-20" />
      <div v-if="!isLoadingData" data-cy="reuseModalContent" class="w-full">
        <no-content2
          class="mt-8 mb-6"
          v-if="state.skillsWereMovedOrReusedAlready"
          title="Please Refresh"
          :show-refresh-action="true"
          message="Skills were moved or reused in another browser tab OR modified by another project administrator." />
        <no-content2
          class="mt-8 mb-6"
          v-if="!hasDestinations && !state.skillsWereMovedOrReusedAlready"
          title="No Destinations Available"
          :message="`There are no Subjects or Groups that this skill can be ${actionNameInPast} ${actionDirection}. Please create additional subjects and/or groups if you want to ${actionNameLowerCase} skills.`" />
        <no-content2
            class="mt-8 mb-6"
            v-if="importFinalizePending && hasDestinations"
            :title="`Cannot ${textCustomization.actionName}`"
            :message="`Cannot initiate skill ${actionNameLowerCase} while skill finalization is pending.`"/>
        <no-content2
            class="mt-8 mb-6"
            v-if="hasDisabledSkillSelected && props.isReuseType && hasDestinations && !importFinalizePending"
            :title="`Cannot ${textCustomization.actionName}`"
            :message="`Cannot ${actionNameLowerCase} a disabled skill.`"/>

        <Stepper v-if="showStepper"  value="1" :linear="true" class="w-full">
          <StepList>
            <Step value="1">Select Destination</Step>
            <Step value="2">Preview</Step>
            <Step value="3">Confirmation</Step>
          </StepList>
          <StepPanels>
            <StepPanel value="1" v-slot="{ activateCallback }">
              <div data-cy="reuseSkillsModalStep1">
                  <Listbox
                    v-model="selectedDestination"
                    :options="destinations"
                    :filter="destinations.length > 4"
                    :filter-fields="['subjectName', 'groupName']"
                    listStyle="max-height:250px"
                    @update:modelValue="activateCallback('2')"
                    class="w-full">
                    <template #empty>
                      There are no <b>Subjects</b> or <b>Groups</b> that this skill can be {{actionNameInPast}} {{actionDirection}}.
                      Please create additional subjects and/or groups if you want to {{actionNameLowerCase}} skills.
                    </template>
                    <template #option="item">
                      <div class="flex" :data-cy="`selectDest_subj${item.option.subjectId}${item.option.groupId || ''}`">
                        <div class="mr-2">
                          <i v-if="item.option.groupId" class="fas fa-layer-group" aria-hidden="true" />
                          <i v-else class="fas fa-cubes" aria-hidden="true" />
                        </div>

                        <div v-if="!item.option.groupId">
                          <span class="italic">Subject:</span>
                          <span class="ml-1 font-semibold text-primary">{{
                              item.option.subjectName
                            }}</span>
                        </div>
                        <div v-if="item.option.groupId">
                          <div>
                            <span class="italic">Group:</span>
                            <span class="ml-1 font-semibold text-primary">{{
                                item.option.groupName
                              }}</span>
                          </div>
                          <div>
                            <span class="italic">In subject:</span> {{ item.option.subjectName }}
                          </div>
                        </div>
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
                      :label="textCustomization.actionName"
                      :disabled="true"
                      data-cy="reuseButton"
                      icon="fas fa-shipping-fast"
                      outlined />
                  </div>
                </div>
            </StepPanel>
            <StepPanel value="2" v-slot="{ activateCallback }">
              <reuse-or-move-preview
                  v-if="selectedDestination?.groupId || selectedDestination?.subjectId"
                  data-cy="reuseSkillsModalStep2"
                  :skills="skills"
                  :destination="selectedDestination"
                  @on-cancel="onCancel"
                  @on-changed="onReuseOrMove"
                  :is-reuse-type="isReuseType"
                  :action-direction="actionDirection"
                  :action-name="textCustomization.actionName"
                  :next-step-nav-function="() => activateCallback('3')"
                />
            </StepPanel>
            <StepPanel value="3">
              <div data-cy="reuseSkillsModalStep3" role="alert">
              <div class="flex flex-col h-48">
                <div
                  class="border-2 border-dashed border-surface rounded-border bg-surface-50 dark:bg-surface-950 flex-auto flex justify-center items-center font-medium">
                <span><span class="text-primary">Successfully</span> {{ actionNameInPast }}
                <Tag severity="info">{{ movedOrReusedSkills.length }}</Tag>
                skill{{ pluralSupport.plural(movedOrReusedSkills) }}.</span>
                </div>
              </div>
              <div class="flex pt-6 justify-end">
                <SkillsButton
                  label="OK"
                  icon="fas fa-shipping-fast"
                  @click="onCancel"
                  data-cy="okButton"
                  outlined />
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