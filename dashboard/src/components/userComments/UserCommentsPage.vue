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
import {ref} from "vue";

import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import NewCommentDialog from "@/components/userComments/NewCommentDialog.vue";
import UserCommentsTable from "@/components/userComments/UserCommentsTable.vue";
import UserComments from "@/skills-display/components/communication/UserComments.vue";


const showNewCommentDialog = ref(false)
const doShowNewCommentDialog = () => {
  showNewCommentDialog.value = true
}
</script>

<template>
  <div>
    <sub-page-header title="User Comments"
                     action="New Comment"
                     @add-action="doShowNewCommentDialog"/>
    <Card>
      <template #content>
        <user-comments />
        <user-comments-table />
      </template>
    </Card>

    <new-comment-dialog
        v-model="showNewCommentDialog"
    />
  </div>
</template>

<style scoped>

</style>