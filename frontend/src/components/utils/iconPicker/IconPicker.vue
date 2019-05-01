<template>
    <div class="has-text-centered has-text-info">
      <div class="columns is-centered" style="height: 100%;">
        <div class="icon-box column" v-on:click="selectIcon">
          <i
            :class="[selectedIconClass]"
            class="select-icon"/>
        </div>
      </div>

      <!--<b-modal id="icons" size="lg" title="Icon Selection">
          <icon-manager v-bind:customIconHeight="this.customIconHeight"
                        v-bind:customIconWidth="this.customIconWidth"
                        v-on:selected-icon="onSelectedIcon"></icon-manager>
      </b-modal>-->
    </div>

</template>

<script>
  /* import IconManager from './IconManager'; */

  export default {
    name: 'IconPicker',
    components: { /* IconManager */ },
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
    box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
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
