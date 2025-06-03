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
<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useTimeoutFn} from "@vueuse/core";
import Popover from "primevue/popover";

const notifications = ref();
const toggle = (event) => {
  notifications.value.toggle(event);
}

const alertNewNotif = ref(false)

onMounted(() => {
  alertNewNotif.value = true
  const {isPending, start, stop} = useTimeoutFn(() => {
    alertNewNotif.value = false
  }, 5000)
})
</script>

<template>
  <div class="d-inline relative">
    <!-- New tag element -->
    <div v-if="alertNewNotif" class="absolute -top-1 -right-1 z-10 animate-ping">
      <span class="bg-orange-700 text-white text-xs font-bold px-1 py-0.5 rounded">
        new
      </span>
    </div>
    <Button
        icon="fa-solid fa-bell"
        severity="warn"
        badge="1"
        badgeSeverity="green"
        outlined
        raised
        @click="toggle"
        aria-label="User Settings Button"
        aria-haspopup="true"
        aria-controls="user_settings_menu"/>
    <Popover ref="notifications">
      <div class="flex flex-col w-[25rem] skills-notification">
        <div class="flex gap-2 align-items-center border-b-1 border-b-gray-200 mb-2">
          <div class="font-bold flex-1">Notifications</div>
<!--          <div>-->
<!--            <SkillsButton label="Clear All" severity="danger" icon="fa-solid fa-xmark" size="small"/>-->
<!--          </div>-->
        </div>
        <div class="py-2">
          <div class="flex gap-2 items-center">
            <div class="font-bold flex-1 text-orange-800">SkillTree 3.6 Version Released</div>
            <div class="text-sm text-gray-600">2023-08-28</div>
          </div>
          <div class="flex pl-6 mt-2 border-l-2 border-l-gray-400-400">
            <div class="flex-1">
              <ul class="list-disc">
                <li>Draft mode for subject/skill creation</li>
                <li>Audio/video in quizzes and surveys</li>
                <li>Context-aware contact system</li>
                <li>Keyboard shortcuts for training navigation</li>
                <li>Screen reader-friendly headings</li>
                <li><a href="">Learn more...</a></li>
              </ul>
            </div>
            <div>
              <SkillsButton severity="danger" icon="fa-solid fa-xmark" size="small"/>
            </div>
          </div>
          <!--          <div class="text-right">-->
          <!--            <SkillsButton size="small"  label="Learn More" />-->
          <!--          </div>-->
        </div>
      </div>
    </Popover>
  </div>
</template>

<style scoped>
.skills-notification a {
  text-decoration: underline !important;
}
</style>