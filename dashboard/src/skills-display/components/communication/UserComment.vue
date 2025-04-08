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
import {onMounted, ref} from "vue";

import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import {computed} from "vue";

const timeUtils = useTimeUtils()
const colors = useColors()

const props = defineProps({
  comment: Object,
  commentIndex: Number,
  showReply: {
    type: Boolean,
    default: true
  },
  replyLabel: {
    type: String,
    default: 'Comment'
  },
  canEdit: {
    type: Boolean,
    default: true
  },
  hasParent: Boolean,
})
const emit = defineEmits(['reportLocation'])

const avatarBgColor = `!${colors.getBgClass(props.commentIndex, 200)}`
const hasResponses = computed(() => props.comment.responses && props.comment.responses.length > 0)

const userAvatar = ref(null)
const avatarLocInfo = ref(null)
const childLocations = ref(new Map())
const updateChildLocation = (loc) => {
  childLocations.value.set(loc.id, loc)
}
const connectorLengths = computed(() => {
  if (!avatarLocInfo.value) {
    return []
  }
  return Array.from(childLocations.value.values()).map(value => {
    return value.y - avatarLocInfo.value.y - (avatarLocInfo.value.height/2);
  });
})
onMounted(() => {
  if (hasResponses.value || props.hasParent) {
    const handleResize = () => {
      const clientRect = userAvatar.value.getBoundingClientRect()
      if (hasResponses.value) {
        avatarLocInfo.value = {
          x: clientRect.x,
          y: clientRect.y,
          width: clientRect.width,
          height: clientRect.height
        }
      } else if(props.hasParent) {
        emit('reportLocation', {id: props.comment.id, x: clientRect.x, y: clientRect.y})
      }
    }
    window.addEventListener('resize', handleResize);
    handleResize()
  }
})
</script>

<template>
  <div>
    <div class="flex gap-2">
      <div class="flex flex-col items-center relative">
        <div ref="userAvatar">
          <Avatar
            :label="comment.userInitials || 'U'"
            :class="avatarBgColor"
            shape="circle"
            :pt="{icon: {class: 'text-blue-800 dark:text-blue-400'}}"/>
        </div>
        <div
            v-if="hasResponses && childLocations && childLocations.size > 0"
            v-for="(length, index) in connectorLengths"
            class="absolute inset-0" :style="`top:${avatarLocInfo.height}px; left:${avatarLocInfo.width/2}px`">
          <div :key="index" :style="`height: ${length}px; width: 34px;`"
               class="border-l-2 border-b-2 rounded-bl-md"></div>
        </div>
      </div>

      <div class=" flex-1">
        <div class="flex gap-2">
          <div class="flex-1 flex gap-2 items-center">
            <div class="font-bold">{{ comment.userIdForDisplay }}</div>
            <div class="text-gray-600">{{ timeUtils.relativeTime(comment.time) }}</div>
          </div>
        </div>
        <div class="mt-3">{{ comment.comment }}</div>

        <div class="flex">
          <div class="flex-1 flex gap-1">
            <Button v-if="showReply" text icon="far fa-comment" :label="replyLabel" severity="info" class=""
                    size="small"></Button>

            <Button v-if="canEdit" text icon="far fa-edit" label="Edit" severity="secondary" class="" size="small"></Button>
          </div>
        </div>

        <div class="mt-5 px-2 flex flex-col gap-5">
          <div v-if="hasResponses">
            <div v-for="(comment, index) in comment.responses" :key="comment.id">
              <user-comment
                  :comment="comment"
                  @report-location="updateChildLocation"
                  :show-reply="false"
                  :comment-index="index"
                  :has-parent="true"/>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>