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

const commentInput = ref('')
// const showCommentInput = ref(false)

const hasComments = computed(() => false)


const availableCommentTypes = ref([
  {name: 'Internal', code: 'Internal', title: 'Only visible to you and project admins'},
  {name: 'Public', code: 'Public', title: 'Visible to anyone who can see this skill'},
]);
const commentType = ref(availableCommentTypes.value[0]);

const comments = []

</script>

<template>
  <div class="my-5">
    <div class="p-4 bg-gray-100 rounded-2xl text-right">
      <!--      <markdown-editor />-->
      <Textarea v-model="commentInput" rows="2" fluid
                variant="filled"
                :pt="{ root: { class: '!border-0' } }"
                placeholder="Add a message or comment"/>
      <div class="flex gap-4 text-left">
<!--        <div class="flex-1 flex items-center gap-e3">-->

<!--          <div v-for="available in availableCommentTypes" :key="available.code">-->
<!--            <RadioButton v-model="commentType.code"-->
<!--                         :inputId="available.code"-->
<!--                         name="dynamic"-->
<!--                         :title="available.title"-->
<!--                         :value="available.name"/>-->
<!--            <label :for="available.code" class="ml-1">{{ available.name }}</label>-->
<!--          </div>-->
<!--        </div>-->
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

    <div class="mt-5 px-2 flex flex-col gap-2">
      <div v-for="(comment, index) in comments" :key="comment.id">
        <user-comment :comment="comment" :comment-index="index"/>
      </div>
    </div>

  </div>
</template>

<style scoped>

</style>