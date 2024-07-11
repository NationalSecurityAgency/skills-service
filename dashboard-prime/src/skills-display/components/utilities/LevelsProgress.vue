<script setup>
import { ref } from 'vue'

const props = defineProps({
  level: Number,
  totalLevels: Number
})
const levelInternal = ref(props.level)
</script>

<template>
  <Rating
    v-model="levelInternal"
    :stars="totalLevels"
    readonly
    disabled
    :cancel="false"
    class="override-readonly-opacity" />
</template>

<style>
.override-readonly-opacity.p-rating {
  opacity: 1 !important;
}
</style>