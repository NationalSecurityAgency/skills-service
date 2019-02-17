<template>
    <div class="icon-box has-text-centered has-text-info" v-on:click="selectIcon">
      <div class="columns is-centered" style="height: 100%">
        <div class="column">
          <i
            :class="[selectedIconClass]"
            class="skills-icon"/>
        </div>
      </div>
    </div>
</template>

<script>
  import IconManager from './IconManager';

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
        this.$modal.open({
          parent: this,
          width: '900px',
          component: IconManager,
          hasModalCard: true,
          props: {
            customIconHeight: this.customIconHeight,
            customIconWidth: this.customIconWidth,
          },
          events: {
            'selected-icon': this.onSelectedIcon,
          },
        });
      },
      onSelectedIcon(selectedIcon) {
        this.selectedIconClass = `${selectedIcon.css}`;
        this.hideAvailableIcons = true;
        this.$emit('on-icon-selected', this.selectedIconClass);
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
    box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
    font-size: 3rem;
    width: 6rem;
    height: 5rem;
    cursor: pointer;
  }

</style>
