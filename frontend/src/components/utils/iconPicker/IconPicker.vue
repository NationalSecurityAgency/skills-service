<template>
  <div class="icon-box text-info" v-on:click="selectIcon">
    <i :class="[selectedIconClass]" class="select-icon"/>
  </div>
</template>

<script>
  export default {
    name: 'IconPicker',
    props: {
      startIcon: String,
      customIconHeight: {
        type: Number,
        default: 48,
      },
      customIconWidth: {
        type: Number,
        default: 48,
      },
    },
    data() {
      return {
        hideAvailableIcons: true,
        selectedIconClass: this.startIcon,
      };
    },
    methods: {
      selectIcon() {
        this.$emit('select-icon');
        /* this.$bvModal.show('icons'); */
      },
      onSelectedIcon(selectedIcon) {
        this.selectedIconClass = `${selectedIcon.css}`;
        this.hideAvailableIcons = true;
        this.$emit('on-icon-selected', this.selectedIconClass);
        this.$bvModal.hide('icons');
      },
      close() {
        this.hideAvailableIcons = true;
      },
    },
  };
</script>

<style scoped>
  .icon-box {
    background-color: white;
    border-radius: 6px;
    border: 1px solid #c9c9c9;
    /*box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);*/
    font-size: 3rem;
    width: 6rem;
    height: 5rem;
    cursor: pointer;
    text-align: center;
  }

  .select-icon {
    height: 64px;
    width: 64px;
  }

</style>
