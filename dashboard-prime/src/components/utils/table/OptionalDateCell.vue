<script setup>
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { computed } from "vue";

const timeUtils = useTimeUtils();
const props = defineProps(['value']);
const isToday = (timestamp) => {
  return timeUtils.isToday(timestamp);
};

const formattedDate = computed(() => {
  return timeUtils.formatDate(props.value);
})
</script>

<template>
    <span v-if="!value">
      never
    </span>
  <span v-else-if="isToday(value)">
      today
    </span>
  <span v-else>
      {{ formattedDate }}
    </span>
</template>

<style scoped>

</style>