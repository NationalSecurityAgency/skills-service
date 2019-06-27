<template>
  <div class="card" @click="selectIcon">
    <div class="card-body text-info" style="min-height: 4rem;">
      <i
        :class="[selectedIconClass]" />
    </div>
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
  i {
    font-size: 3rem;
  }
</style>
