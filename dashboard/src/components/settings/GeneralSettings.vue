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
import { ref, onMounted } from 'vue';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import { useUserInfo } from '@/components/utils/UseUserInfo'
import { object, string } from 'yup'
import SettingsService from '@/components/settings/SettingsService.js'
import {useForm} from "vee-validate";
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const userInfo = useUserInfo()
const appConfig = useAppConfig();

const schema = object({
  firstName: string().required().min(2).label('First Name'),
  lastName: string().required().min(2).max(30).label('Last Name'),
  nickName: string().label('Primary Name')
})

const { defineField, meta } = useForm({
  validationSchema: schema,
})

const [firstName] = defineField('firstName');
const [lastName] = defineField('lastName');
const [nickname] = defineField('nickname');

const isLoading = ref(true);
const isSaving = ref(false);
const pkiAuthenticated = ref(false);
const saveMessage = ref('');

onMounted(() => {
  loadData();
  pkiAuthenticated.value = appConfig.isPkiAuthenticated.value;
})

function loadData() {
  const currentUser = userInfo.userInfo.value;
  if (currentUser) {
    firstName.value = currentUser.first;
    lastName.value = currentUser.last;
    nickname.value = currentUser.nickname;
  }
  isLoading.value = false;
}

function updateUserInfo() {
  saveMessage.value = '';
  const currentUser = userInfo.userInfo.value;
  currentUser.first = firstName.value;
  currentUser.last = lastName.value;
  currentUser.nickname = nickname.value;
  isSaving.value = true;

  SettingsService.saveUserInfo(currentUser).then(() => {
    saveMessage.value = 'Updated User Info Successfully!';
  }).catch(() => {
    saveMessage.value = 'Failed to Update User Info Settings!';
  }).finally(() => {
    isSaving.value = false;
  });
}
</script>

<template>
  <sub-page-header title="Profile"/>

  <Card>
    <template #content v-if="!isLoading">
      <Message v-if="saveMessage" :sticky="false" :life="10000" severity="success">{{saveMessage}}</Message>
      <SkillsTextInput name="firstName" label="First Name" is-required :initialValue="firstName" v-if="!pkiAuthenticated"/>
      <SkillsTextInput name="lastName" label="Last Name" is-required :initialValue="lastName" v-if="!pkiAuthenticated" class="my-2"/>
      <SkillsTextInput name="nickname" label="Primary Name" :initialValue="nickname" class="mb-3"/>

      <SkillsButton label="Save" icon="fas fa-arrow-circle-right" @click="updateUserInfo" :disabled="!meta.valid || !meta.dirty" data-cy="generalSettingsSave" />
    </template>
  </Card>
</template>

<style scoped></style>
