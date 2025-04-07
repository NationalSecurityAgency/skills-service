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
import {computed, ref} from 'vue'
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import UserComment from "@/skills-display/components/communication/UserComment.vue";
import UserComments from "@/skills-display/components/communication/UserComments.vue";

const commentInput = ref('')
</script>

<template>
  <div class="my-5">
    <div class="p-4 bg-gray-100 rounded-2xl text-right">
      <Textarea v-model="commentInput" rows="2" fluid
                variant="filled"
                :pt="{ root: { class: '!border-0' } }"
                placeholder="Add a message or comment"/>
      <div class="flex gap-4 text-left">
        <div class="mt-2 flex-1">
          <SkillsButton label="Submit"
                        :outlined="false"
                        icon="far fa-paper-plane"
                        severity="warn"
                        rounded
                        size="small"/>
        </div>
        <div>
          <small>Visible only to you and training admins</small>
        </div>
      </div>
    </div>

    <user-comments />

  </div>
</template>

<style scoped>

</style>