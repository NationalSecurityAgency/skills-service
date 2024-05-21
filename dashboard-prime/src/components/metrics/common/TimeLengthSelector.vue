<script setup>
import { ref } from 'vue';
import dayjs from 'dayjs';

const props = defineProps(['options']);
const emit = defineEmits(['time-selected']);

const selectedIndex = ref(0);

const getVariant = (index) => {
  return selectedIndex.value === index ? 'success' : 'secondary';
};

const handleClick = (index) => {
  selectedIndex.value = index;
  const selectedItem = props.options[index];
  const start = dayjs().subtract(selectedItem.length, selectedItem.unit);
  const event = {
    durationLength: selectedItem.length,
    durationUnit: selectedItem.unit,
    startTime: start,
  };
  emit('time-selected', event);
};
</script>

<template>
  <span data-cy="timeLengthSelector" class="time-length-selector">
    <Badge v-for="(item, index) in options" :key="`${item.length}${item.unit}`"
             class="ml-2" :class="{'can-select' : (index !== selectedIndex) }"
             :severity="getVariant(index)" @click="handleClick(index)" @keyup.enter="handleClick(index)" tabindex="0">
      {{ item.length }} {{ item.unit }}
    </Badge>
  </span>
</template>

<style scoped>
.can-select {
  cursor: pointer;
}
</style>