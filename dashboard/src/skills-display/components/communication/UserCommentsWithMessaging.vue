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
import {onMounted, ref} from 'vue'
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import UserComments from "@/skills-display/components/communication/UserComments.vue";
import {useSkillsDisplayService} from "@/skills-display/services/UseSkillsDisplayService.js";
import {useRoute} from "vue-router";

const commentInput = ref('')

const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()

const isLoadingComments = ref(true)
const comments = ref([])
const loadComments = () => {
  skillsDisplayService.getUserComments(route.params.skillId)
      .then(res => comments.value = res.comments)
      .finally(() => isLoadingComments.value = false)
}

onMounted(() => {
  loadComments()
})

</script>

<template>
  <div class="my-5">
    <BlockUI :blocked="isLoadingComments">
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

      <user-comments :comments="comments"/>
    </BlockUI>
  </div>
</template>

<style scoped>

</style>