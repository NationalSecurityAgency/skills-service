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
import { computed, ref } from 'vue'
import { boolean, object, string, ValidationError } from 'yup'
import { useDebounceFn } from '@vueuse/core'
import { SkillsReporter } from '@skilltree/skills-client-js'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';
import CommunityProtectionControls from '@/components/projects/CommunityProtectionControls.vue';
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js';

const model = defineModel()
const props = defineProps({
  adminGroup: Object,
  isEdit: {
    type: Boolean,
    default: false,
  }
})

const communityLabels = useCommunityLabels()
const initialValueForEnableProtectedUserCommunity = communityLabels.isRestrictedUserCommunity(props.adminGroup.userCommunity)
const enableProtectedUserCommunity = ref(initialValueForEnableProtectedUserCommunity)

const emit = defineEmits(['admin-group-saved'])
const loadingComponent = ref(false)

const modalTitle = computed(() => {
  return props.isEdit ? 'Editing Existing Admin Group' : 'New Admin Group'
})
const modalId = props.isEdit ? `editAdminGroupDialog${props.adminGroup.adminGroupId}` : 'newAdminGroupDialog'
const appConfig = useAppConfig()


const checkAdminGroupNameUnique = useDebounceFn((value) => {
  if (!value || value.length === 0) {
    return true
  }
  const origName = props.adminGroup.name
  if (props.isEdit && (origName === value || origName.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true
  }
  return AdminGroupsService.checkIfAdminGroupNameExist(value).then((remoteRes) => !remoteRes)
}, appConfig.formFieldDebounceInMs)
const checkAdminGroupIdUnique = useDebounceFn((value) => {
  if (!value || value.length === 0 || (props.isEdit && props.adminGroup.adminGroupId === value)) {
    return true
  }
  return AdminGroupsService.checkIfAdminGroupIdExist(value)
      .then((remoteRes) => !remoteRes)

}, appConfig.formFieldDebounceInMs)

const checkUserCommunityRequirements = (value, testContext) => {
  if (!value || !props.isEdit) {
    return true;
  }
  return AdminGroupsService.validateAdminGroupForEnablingCommunity(props.adminGroup.adminGroupId).then((result) => {
    if (result.isAllowed) {
      return true;
    }
    if (result.unmetRequirements) {
      const errors = result.unmetRequirements.map((req) => {
        return testContext.createError({ message: `${req}` })
      })
      return new ValidationError(errors)
    }
    return true
  });
}

const schema = object({
  'adminGroupName': string()
      .trim()
      .required()
      .min(appConfig.minNameLength)
      .max(appConfig.maxAdminGroupNameLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'The value for the Admin Group Name is already taken', (value) => checkAdminGroupNameUnique(value))
      .customNameValidator()
      .label('Admin Group Name'),
  'adminGroupId': string()
      .required()
      .min(appConfig.minIdLength)
      .max(appConfig.maxIdLength)
      .idValidator()
      .nullValueNotAllowed()
      .test('uniqueId', 'Admin Group ID already exists', (value) => checkAdminGroupIdUnique(value))
      .label('Admin Group ID'),
  'enableProtectedUserCommunity': boolean()
      .test('communityReqValidation', 'Unmet community requirements', (value, testContext) => checkUserCommunityRequirements(value, testContext))
      .label('Enable Protected User Community'),
})

const initialAdminGroupData = ref({
  adminGroupId: props.adminGroup.adminGroupId || '',
  adminGroupName: props.adminGroup.name || '',
  enableProtectedUserCommunity: false,
})
const close = () => { model.value = false }

const saveAdminGroup = (values) => {
  const adminGroupToSave = {
    ...values,
    originalAdminGroupId: props.adminGroup.adminGroupId,
    originalAdminGroupName: props.adminGroup.name,
    name: InputSanitizer.sanitize(values.adminGroupName),
    adminGroupId: InputSanitizer.sanitize(values.adminGroupId),
  }

  if (initialValueForEnableProtectedUserCommunity) {
    adminGroupToSave.enableProtectedUserCommunity = initialValueForEnableProtectedUserCommunity
  }

  return AdminGroupsService.updateAdminGroupDef(adminGroupToSave)
    .then((updatedAdminGroup) => {
      return {
        ...updatedAdminGroup,
        originalAdminGroupName: props.adminGroup.name,
      }
    })
}
const onSaved = (savedAdminGroup) => {
  emit('admin-group-saved', savedAdminGroup)
  if (!props.isEdit) {
    SkillsReporter.reportSkill('CreateAdminGroup')
  }
  close()
}

</script>

<template>
  <SkillsInputFormDialog
      :id="modalId"
      v-model="model"
      :is-edit="isEdit"
      :should-confirm-cancel="true"
      :header="modalTitle"
      :loading="loadingComponent"
      :validation-schema="schema"
      :initial-values="initialAdminGroupData"
      :save-data-function="saveAdminGroup"
      @saved="onSaved"
      @close="close"
  >
    <template #default>

      <SkillsNameAndIdInput
          name-label="Group Name"
          name-field-name="adminGroupName"
          id-label="Group ID"
          id-field-name="adminGroupId"
          :name-to-id-sync-enabled="!isEdit"
          :show-id-field="false"
      />

      <community-protection-controls
          v-model:enable-protected-user-community="enableProtectedUserCommunity"
          :admin-group="adminGroup"
          :is-edit="isEdit"
          :is-copy="false" />

    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>