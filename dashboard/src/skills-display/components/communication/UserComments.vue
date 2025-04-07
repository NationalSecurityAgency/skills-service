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
import {ref} from 'vue'
import UserComment from "@/skills-display/components/communication/UserComment.vue";


const availableCommentTypes = ref([
  {name: 'Internal', code: 'Internal', title: 'Only visible to you and project admins'},
  {name: 'Public', code: 'Public', title: 'Visible to anyone who can see this skill'},
]);

const comments = []

</script>

<template>
  <div class="mt-5 px-2 flex flex-col gap-2">
    <div v-for="(comment, index) in comments" :key="comment.id">
      <user-comment :comment="comment" :comment-index="index"/>
    </div>
  </div>
</template>

<style scoped>

</style>