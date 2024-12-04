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
import {useStorage} from "@vueuse/core";
const props = defineProps({
  storageKey: String,
})
const msgClosedForGood = useStorage(`${props.storageKey}-closed`, false)

const close = () => {
  msgClosedForGood.value = true
}
</script>

<template>
  <Message v-if="!msgClosedForGood" severity="success">
    <template #container>
      <div class="px-2 py-3 flex align-items-center gap-3">
        <div>
          <slot name="icon" />
        </div>
        <div class="text-center text-xl flex-1">
          <slot name="content" />
        </div>
        <div>
          <SkillsButton
              @click="close"
              class="text-xl"
              icon="fas fa-times"
              text
              data-cy="closeCelebrationMsgBtn"
              aria-label="Close celebration message"
          />
        </div>

      </div>
    </template>
  </Message>
</template>

<style scoped>

</style>