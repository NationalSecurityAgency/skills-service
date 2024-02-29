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