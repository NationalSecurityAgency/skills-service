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
import { useStorage } from '@vueuse/core'
import dayjs from '@/common-components/DayJsCustomizer'
import { computed } from 'vue'

const props = defineProps({
  id: {
    type: String,
    required: true
  },
  expireAfterMins: {
    type: Number,
    default: 1440, // 1 day
  }
})

const expirationInfo = useStorage(`reminderMessage-${props.id}`, { createdOn: null, toExpireOn: null })
const onClose = () => {
  const createdOn = dayjs()
  const toExpireOn = createdOn.add(Number(props.expireAfterMins), 'minute')
  expirationInfo.value.createdOn = createdOn
  expirationInfo.value.toExpireOn = toExpireOn
}
const shouldShow = computed(() => {
  if (!expirationInfo.value || !expirationInfo.value.toExpireOn) {
    return true
  }
  const expiredOn = dayjs(expirationInfo.value.toExpireOn)
  return dayjs().isAfter(expiredOn)
})
</script>

<template>
  <Message
    v-if="shouldShow"
    @close="onClose">
    <slot/>
  </Message>
</template>

<style scoped>

</style>