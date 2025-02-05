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
import { ref, computed, watch, onMounted } from 'vue';
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue';
import InputText from 'primevue/inputtext';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import Stepper from 'primevue/stepper'
import StepList from 'primevue/steplist';
import StepPanels from 'primevue/steppanels';
import StepPanel from 'primevue/steppanel';
import Step from 'primevue/step';
import AccessService from "@/components/access/AccessService.js";
import projectService from "@/components/projects/ProjectService.js";

const focusState = useFocusState()

const emit = defineEmits(['hidden', 'do-remove']);

const props = defineProps({
  itemName: {
    type: String,
    required: true,
  },
  itemType: {
    type: String,
    required: false,
  },
  validationText: {
    type: String,
    required: false,
    default: 'Delete This Project',
  },
  removeButtonLabel: {
    type: String,
    required: false,
    default: 'Yes, Do Remove!',
  },
  removalNotAvailable: {
    type: Boolean,
    required: false,
    default: false,
  },
  focusOnCloseId: {
    type: String,
    required: false,
  },
  removalTextPrefix: {
    type: String,
    required: false,
    default: 'This will delete the',
  },
  project: {
    type: Object,
    required: true,
  }
});

const model = defineModel()

const adminCount = ref(0);
const userCount = ref(0);
const loading = ref(false);

onMounted(() => {
  const roles = ['ROLE_PROJECT_ADMIN', 'ROLE_PROJECT_APPROVER'];
  const countAdmins = AccessService.countUserRolesForProject(props.project.projectId, roles);
  const countUsers = projectService.countProjectUsers(props.project.projectId)
  const promiseCounts = [countAdmins, countUsers]

  loading.value = true
  Promise.all(promiseCounts).then((result) => {
    adminCount.value = result[0];
    userCount.value = result[1];
    loading.value = false
  })
})

let currentValidationText = ref('');

const removeDisabled = computed(() => {
  return currentValidationText.value !== props.validationText;
});
const announcer = useSkillsAnnouncer()
watch(removeDisabled, (newValue) => {
  if(!newValue) {
    announcer.polite(`Removal operation successfully enabled. Please click on ${props.removeButtonLabel} button`)
  }
})

const publishHidden = (e) => {
  close()
  emit('hidden', { ...e });
};

const removeAction = () => {
  if (props.focusOnCloseId) {
    focusState.setElementId(props.focusOnCloseId);
  }
  close()
  emit('do-remove');
};

const close = () => {
  model.value = false
  handleFocus()
}

const confirmStepOne = ref(false);
const confirmStepTwo = ref(false);

const clearSettings = () => {
  confirmStepOne.value = false;
  confirmStepTwo.value = false;
  currentValidationText.value = '';
}

const handleFocus = () => {
  focusState.focusOnLastElement()
}

const hasContent = computed(() => {
  return props.project.numSubjects > 0 || props.project.numSkills || props.project.numBadges
})

const hasUsers = computed(() => {
  return userCount.value > 0 || adminCount.value > 1;
})
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      header="Removal Safety Check"
      cancel-button-severity="secondary"
      :show-ok-button="false"
      :show-cancel-button="false"
      @on-ok="removeAction"
      @on-cancel="publishHidden"
      :enable-return-focus="true"
      :style="{ width: '40rem !important' }">
    <skills-spinner v-if="loading" :is-loading="loading" class="my-6"/>
    <div v-if="!loading" class="px-2">
      <Stepper linear @step-change="clearSettings" value="1">
        <StepList>
          <Step value="1"></Step>
          <Step value="2"></Step>
          <Step value="3"></Step>
        </StepList>
        <StepPanels>
          <StepPanel value="1" v-slot="{ activateCallback }">
            <div data-cy="removalSafetyCheckMsg">
                <div v-if="!removalNotAvailable">
                  {{ removalTextPrefix }} <span class="font-bold text-primary">{{ itemName }}</span><span v-if="itemType">&nbsp;{{ itemType }}</span>.
                </div>
                <Message severity="warn" :closable="false">
                  Deletion <b>cannot</b> be undone and permanently removes all skill subject definitions, skill
                  definitions and users' performed skills for this Project.

                  <div class="mt-6" v-if="hasContent">
                    This will delete:
                    <ul>
                      <li v-if="project.numSubjects > 0">{{ project.numSubjects }} Subject(s)</li>
                      <li v-if="project.numSkills > 0">{{ project.numSkills }} Skill(s)</li>
                      <li v-if="project.numBadges > 0">{{ project.numBadges }} Badge(s)</li>
                    </ul>
                  </div>
                </Message>
                <div class="flex">
                  <Checkbox inputId="stepOneCheck" :binary="true" name="Confirm" v-model="confirmStepOne" data-cy="confirmCheckbox" />
                  <label for="stepOneCheck" class="ml-2">I understand that this is permanent and cannot be undone</label>
                </div>
                <div class="flex mt-2 w-full justify-end">
                  <SkillsButton label="Cancel" icon="far fa-times-circle" outlined class="mr-2" severity="secondary" data-cy="closeButton" @click="publishHidden" />
                  <SkillsButton label="Next" icon="fas fa-arrow-circle-right float-right" @click="activateCallback('2')" :disabled="!confirmStepOne" data-cy="firstNextButton"/>
                </div>
              </div>
          </StepPanel>
          <StepPanel value="2" v-slot="{ activateCallback }">
            <div data-cy="userRemovalMsg">
                <Message severity="warn" :closable="false">
                  Deleting this project will also delete it for all administrators and users associated with it.

                  <div class="mt-6" v-if="hasUsers">
                    This will remove the project for:
                    <ul>
                      <li v-if="adminCount > 0">{{ adminCount }} Administrator(s)</li>
                      <li v-if="userCount > 0">{{ userCount }} User(s)</li>
                    </ul>
                  </div>
                </Message>
                <div class="flex flex-1 mt-2">
                  <Checkbox
                      inputId="stepTwoCheck"
                      :binary="true"
                      name="Confirm"
                      data-cy="confirmCheckbox"
                      v-model="confirmStepTwo"
                  />
                  <label for="stepTwoCheck" class="ml-2">I understand that this project will be deleted for ALL users.</label>
                </div>
                <div class="flex mt-2 w-full justify-end">
                  <SkillsButton label="Cancel" icon="far fa-times-circle" outlined class="mr-2" severity="secondary" data-cy="closeButton" @click="publishHidden" />
                  <SkillsButton label="Next" icon="fas fa-arrow-circle-right" @click="activateCallback('3')" :disabled="!confirmStepTwo" data-cy="secondNextButton" />
                </div>
              </div>
          </StepPanel>
          <StepPanel value="3">
            <Message severity="warn" :closable="false">
              Are you SURE you want to delete <span class="font-bold text-primary">{{ itemName }}</span>? Remember: This action is permanent and can not be recovered!
            </Message>
            <div v-if="!removalNotAvailable" class="mb-6">
              <p
                  :aria-label="`Please type ${validationText} in the input box to permanently remove the record. To complete deletion press 'Yes, Do Remove' button!`">
                Please type <span class="italic font-bold text-primary">{{ validationText }}</span> to permanently
                remove the record.
              </p>
              <InputText v-model="currentValidationText" data-cy="currentValidationText" aria-required="true" style="width: 100%"
                         aria-label="Type 'Delete This Project' text here to enable the removal operation. Please make sure that 'D' and 'M' are uppercase." />
            </div>
            <div class="flex mt-2 w-full justify-end">
              <SkillsButton label="Cancel" icon="far fa-times-circle" outlined class="mr-2" severity="secondary" data-cy="closeButton" @click="publishHidden" />
              <SkillsButton label="Delete" icon="fas fa-trash-alt" @click="removeAction" :disabled="removeDisabled" data-cy="deleteProjectButton" severity="danger"/>
            </div>
          </StepPanel>
        </StepPanels>
      </Stepper>

    </div>
  </SkillsDialog>
</template>

<style scoped></style>
