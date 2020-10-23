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
  import moment from 'moment';

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
        const start = moment()
          .subtract(selectedItem.length, selectedItem.unit);
        this.$emit('time-selected', start);
      },
    },
  };
</script>

<style scoped>
.can-select {
  cursor: pointer;
}

</style>
