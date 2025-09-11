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
import {useProjConfig} from "@/stores/UseProjConfig.js";
import {useRoute} from "vue-router";
import ReminderMessage from "@/components/utils/misc/ReminderMessage.vue";

const projConfig = useProjConfig()
const route = useRoute()

const expireAfter30Days = 60 * 24 * 30
</script>

<template>
<reminder-message id="pointsBasedLevelsWarning" v-if="projConfig.isPointsLevelManagementEnabled" severity="warn" :expireAfterMins="expireAfter30Days">
  <div>A friendly reminder that this project uses <b>Point-Based Level Management</b>. You are responsible for defining point ranges for each level.
    <ul class="list-disc ml-8">
      <li class="leading-relaxed">As new skills increase the total available points, update <router-link class="underline" :to="{ name:'ProjectLevels', params: { projectId: route.params.projectId }}">level thresholds</router-link> to prevent users from reaching the maximum level prematurely.</li>
      <li class="leading-relaxed">To disable this feature, go to <router-link class="underline" :to="{ name:'ProjectSettings', params: { projectId: route.params.projectId }}">Project Settings</router-link>.</li>
    </ul>
  </div>
</reminder-message>
</template>

<style scoped>

</style>