<script setup>
import { ref } from 'vue';

const props = defineProps(['options']);
const emit = defineEmits(['mode-selected']);

const selectedIndex = ref(0);

const getVariant = (index) => {
  return selectedIndex.value === index ? 'primary' : 'secondary';
};

const handleClick = (index) => {
  selectedIndex.value = index;
  const selectedItem = props.options[index];
  const event = {
    value: selectedItem.value,
  };
  emit('mode-selected', event);
};
</script>

<template>
  <span data-cy="modeSelector" class="mode-selector">
    <Badge v-for="(item, index) in options" :key="`${index}`"
             class="ml-2" :class="{'can-select' : (index !== selectedIndex) }"
             :severity="getVariant(index)" @click="handleClick(index)" @keyup.enter="handleClick(index)" tabindex="0">
      {{ item.label }}
    </Badge>
  </span>
</template>

<style scoped>
.can-select {
  cursor: pointer;
}

.mode-selector {
  padding-right: 8em;
}
</style>