<script setup>
import { computed } from 'vue';

const props = defineProps(['type']);
const types = [
  'Overall',
  'Subject',
  'Skill',
  'Badge',
];

const isUnknownType = computed(() => {
  return types.find((item) => item === props.type) === undefined;
});
</script>

<template>
  <div style="vertical-align: text-top !important;">
    <span class="border-info rounded d-inline-block p-1 text-center icon text-secondary">
      <span v-if="type === types[0]">
        <i class="fa fa-trophy"/>
      </span>
      <span v-if="type === types[1]">
            <i class="fa fa-cubes "/>
      </span>
      <span v-if="type === types[2]">
            <i class="fa fa-graduation-cap "/>
      </span>
      <span v-if="type === types[3]">
            <i class="fa fa-award "/>
      </span>
      <span v-if="isUnknownType">
            <i class="fa fa-award "/>
      </span>
    </span>
    <span style="vertical-align: text-top !important;">
      {{ type }}
    </span>
  </div>
</template>

<style scoped>

</style>