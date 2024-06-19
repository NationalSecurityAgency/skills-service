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
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { computed, ref } from 'vue'
import dayjs from '@/common-components/DayJsCustomizer.js'
import ProjectService from '@/components/projects/ProjectService.js'

const props = defineProps(['project'])
const emits = defineEmits(['extended'])
const appConfig = useAppConfig()

const cancellingExpiration = ref(false);
const fromExpirationDate = computed(() => {
  if (!props.project.expiring) {
    return '';
  }
  const gracePeriodInDays = appConfig.expirationGracePeriod;
  const expires = dayjs(props.project.expirationTriggered).add(gracePeriodInDays, 'day').startOf('day');
  return  dayjs().startOf('day').to(expires);
});

const keepIt = () => {
  cancellingExpiration.value = true;
  ProjectService.cancelUnusedProjectDeletion(props.project.projectId)
    .then(() => {
      emits('extended');
    })
    .finally(() => {
      cancellingExpiration.value = false;
    });
};

</script>

<template>
  <Message
    v-if="project.expiring"
    data-cy="projectExpiration"
    severity="error"
    :closable="false"
    class="mt-2">
    <div class="flex gap-2 flex-column md:flex-row align-items-center">
      <div class="flex-1">
        Project has not been used in over <Tag severity="danger">{{ appConfig.expireUnusedProjectsOlderThan }} days</Tag> and will be
        deleted <Tag severity="danger">{{ fromExpirationDate }}</Tag>.
      </div>
      <div class="text-right">
        <SkillsButton
          @click="keepIt"
          data-cy="keepIt"
          size="sm"
          label="Keep It"
          icon="fas fa-shield-alt"
          :loading="cancellingExpiration"
          :aria-label="'Keep Project '+ project.name" />
      </div>
    </div>
  </Message>
</template>

<style scoped>

</style>