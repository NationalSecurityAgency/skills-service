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
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const appConfig = useAppConfig()
</script>

<template>
  <div class="flex items-center justify-center mb-6">
    <div class="flex gap-6 mx-auto ">
      <Card :pt="{ content: { class: 'h-full' }, body: { class: 'h-full' }}">
        <template #content>
          <div class="flex items-center justify-center h-full">
            <i class="fa-solid fa-headset text-[6rem] text-blue-700 dark:text-blue-400" aria-hidden="true"></i>
          </div>
        </template>
      </Card>
      <div class="flex flex-col">
        <div class="flex-1">
          <h1 class="text-2xl text-blue-900 dark:text-blue-400 uppercase">{{ appConfig.contactSupportExternalTitle }}</h1>
          <div class="max-w-[30rem]">{{ appConfig.contactSupportExternalDescription }}</div>
        </div>
        <div class="pt-4">
          <a :href="appConfig.contactSupportExternalUrl" data-cy="visitSupportCenterBtn" target="_blank">
            <SkillsButton
                :label="`Visit ${ appConfig.contactSupportExternalTitle}`"
                icon="fa-solid fa-square-arrow-up-right"
            />
          </a>
        </div>
      </div>
    </div>

  </div>

  <hr
      class="h-px border-t-0 bg-transparent bg-gradient-to-r from-transparent via-neutral-500 to-transparent opacity-25 dark:via-neutral-400" />


  <div class="text-center mt-3 mb-6">

    <p>
      {{ appConfig.contactSupportExternalEmailDescription }} <a :href="`mailto:${appConfig.contactSupportExternalEmail}`" data-cy="supportEmailLink"
                                                                                               class="underline">{{ appConfig.contactSupportExternalEmail }}</a>
    </p>
  </div>
</template>

<style scoped>

</style>