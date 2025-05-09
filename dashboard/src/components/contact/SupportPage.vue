/*
Copyright 2025 SkillTree

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

import ContactSupportInfo from "@/components/contact/ContactSupportInfo.vue";
import {useRouter} from "vue-router";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
const appConfig = useAppConfig()

const router = useRouter()

const navBack = () => {
  router.back()
}
</script>

<template>
  <div class="pt-5 flex justify-center">
    <Message v-if="!appConfig.contactSupportEnabled"
             severity="danger"
             data-cy="featureDisabled"
             :closable="false">Contact Support Feature is not enabled</Message>
    <Card v-if="appConfig.contactSupportEnabled" class="w-[50rem]">
      <template #content>
        <div class="p-5">
          <contact-support-info/>
        </div>
      </template>
      <template #footer>
        <hr class="w-[100%] mx-auto"/>

        <div class="flex mt-3 gap-3 justify-center">
          <SkillsButton
              label="Navigate Back"
              icon="fa-solid fa-backward-step"
              @click="navBack"
              severity="warn"
              data-cy="navBack"/>
          <router-link to="/">
            <SkillsButton
                label="Take Me Home"
                icon="fa-solid fa-home"
                severity="info"
                data-cy="takeMeHome"/>
          </router-link>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>