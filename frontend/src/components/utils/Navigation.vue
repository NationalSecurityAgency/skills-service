<template>
  <div class="row">
    <div class="col-lg-2 border rounded p-3 mb-2 bg-light">
      <h4 class="mb-2 text-secondary">Navigation
        <span class="d-lg-none float-right">
          <b-button v-b-toggle.menu-collapse-control variant="outline-secondary" size="sm" class="mb-1">
            <i class="fas fa-bars"/>
          </b-button>
        </span>
      </h4>
      <!-- bootstrap didn't handle vertical menus well so rolling out our own-->
      <b-collapse id="menu-collapse-control" :visible="!smallScreenMode">
        <ul class="m-0 p-0" style="list-style: none;">
          <li class="mb-1 p-2 text-primary" v-for="(navItem) of navItems" :key="navItem.name"
              @click="navigate(`${navItem.name}`)"
              :class="{'bg-primary': menuSelections.get(navItem.name), 'text-light': menuSelections.get(navItem.name), 'select-cursor': !menuSelections.get(navItem.name)}">
            <div class="text-truncate">
              <i v-bind:class="navItem.iconClass" class="fas fa-w-16" style="min-width: 1.7rem;"/> {{ navItem.name }}
            </div>
          </li>
        </ul>
      </b-collapse>
    </div>
    <div class="col-lg-10">
      <div v-for="(navItem) of navItems" :key="navItem.name">
        <div v-if="menuSelections.get(navItem.name)">
          <slot :name="navItem.name"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'Navigation',
    props: ['navItems'],
    data() {
      const menuSelectionsTemp = this.buildNewMenuMap(this.navItems[0].name);
      return {
        menuSelections: menuSelectionsTemp,
        windowWidth: 0,
      };
    },
    created() {
      window.addEventListener('resize', this.handleResize);
      this.handleResize();
    },
    destroyed() {
      window.removeEventListener('resize', this.handleResize);
    },
    methods: {
      navigate(selectedKey) {
        if (this.smallScreenMode) {
          this.$root.$emit('bv::toggle::collapse', 'menu-collapse-control');
        }
        const menuSelectionsTemp = this.buildNewMenuMap(selectedKey);
        this.menuSelections = menuSelectionsTemp;
      },
      buildNewMenuMap(selectedKey) {
        const menuSelectionsTemp = new Map();
        this.navItems.forEach((navItem) => {
          menuSelectionsTemp.set(navItem.name, false);
        });
        menuSelectionsTemp.set(selectedKey, true);

        return menuSelectionsTemp;
      },
      handleResize() {
        this.windowWidth = window.innerWidth;
      },
    },
    computed: {
      smallScreenMode() {
        // 992 matches lg in bootstrap
        return this.windowWidth < 992;
      },
    },
  };
</script>

<style scoped>
  .select-cursor {
    cursor: pointer;
  }
</style>
