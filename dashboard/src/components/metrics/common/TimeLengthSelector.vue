/*
Copyright 2020 SkillTree

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
<template>
  <span data-cy="timeLengthSelector">
    <b-badge v-for="(item, index) in options" :key="`${item.length}${item.unit}`"
      class="ml-2" :class="{'can-select' : (index !== selectedIndex) }"
             :variant="getVariant(index)" @click="handleClick(index)">
      {{ item.length }} {{ item.unit }}
    </b-badge>
  </span>
</template>

<script>
  import dayjs from '../../../DayJsCustomizer';

  export default {
    name: 'TimeLengthSelector',
    props: ['options'],
    data() {
      return {
        selectedIndex: 0,
      };
    },
    methods: {
      getVariant(index) {
        return this.selectedIndex === index ? 'primary' : 'secondary';
      },
      handleClick(index) {
        this.selectedIndex = index;
        const selectedItem = this.options[index];
        const start = dayjs()
          .subtract(selectedItem.length, selectedItem.unit);
        const event = {
          durationLength: selectedItem.length,
          durationUnit: selectedItem.unit,
          startTime: start,
        };
        this.$emit('time-selected', event);
      },
    },
  };
</script>

<style scoped>
.can-select {
  cursor: pointer;
}

</style>
