<script setup>
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';

defineProps({
  show: {
    type: Boolean,
    required: true,
  },
  showSpinner: {
    type: Boolean,
    default: true,
  },
  opacity: {
    type: String,
    default: '100',
  },
});
</script>

<template>
  <BlockUI :blocked="show" :auto-z-index="false"
           :pt:mask:class="`opacity-${opacity}`"
  >
    <slot></slot>
    <div v-if="show" class="text-center overlay-content">
      <slot name="overlay">
        <div v-if="show && showSpinner">
          <SkillsSpinner :is-loading="true"></SkillsSpinner>
        </div>
      </slot>
    </div>
  </BlockUI>
</template>

<style scoped>

.overlay-content {
  position: absolute !important;
  z-index: 100;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%)
}
</style>
