<script setup>
import { useAttrs } from 'vue'
import { useFocusState } from '@/stores/UseFocusState.js'

const props = defineProps({
  label: String,
  icon: String,
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  trackForFocus: {
    type: Boolean,
    default: false
  },
  outlined: {
    type: Boolean,
    default: true
  }
})
const emit = defineEmits(['click'])
const attrs = useAttrs()
const focusState = useFocusState()
const onClick = (event) =>{
  if (props.trackForFocus) {
    if (!attrs.id) {
      throw 'SkillsButton component is labeled to track focus but does not have an id attribute'
    }
    focusState.setElementId(attrs.id)
  }
  emit('click', event)
}

</script>

<template>
  <Button :disabled="disabled || loading" @click="onClick" role="button" :outlined="outlined">
    <slot>
      <span v-if="label">{{ label }}</span>
      <i v-if="!loading && icon && icon.trim().length > 0" class="ml-1" :class="icon" aria-hidden="true" style="width: 0.9rem; height: 0.9rem;"></i>
      <ProgressSpinner v-if="loading" style="width: 0.9rem; height: 0.9rem;" class="ml-1" />
    </slot>
  </Button>
</template>

<style scoped>

</style>