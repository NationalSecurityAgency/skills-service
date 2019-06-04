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
          <router-link :to="{ name: navItem.page }" tag="li" class="mb-1 p-2 text-primary" v-for="(navItem) of navItems" :key="navItem.name"
              @click.native="navigate(`${navItem.name}`)"
              :class="{'bg-primary': menuSelections.get(navItem.name), 'text-light': menuSelections.get(navItem.name), 'select-cursor': !menuSelections.get(navItem.name)}">
            <div class="text-truncate">
                <i v-bind:class="navItem.iconClass" class="fas fa-w-16" style="min-width: 1.7rem;"/> {{ navItem.name }}
            </div>
          </router-link>
        </ul>
      </b-collapse>
    </div>
    <div class="col-lg-10">
      <div v-for="(navItem) of navItems" :key="navItem.name">
        <div v-if="menuSelections.get(navItem.name)">
<!--          <slot :name="navItem.name"></slot>-->
          <router-view></router-view>
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
      return {
        menuSelections: [],
        windowWidth: 0,
      };
    },
    created() {
      this.buildNewMenuMapWhenPropsChange(this.navItems);
      window.addEventListener('resize', this.handleResize);
      this.handleResize();
    },
    destroyed() {
      window.removeEventListener('resize', this.handleResize);
    },
    watch: {
      // must watch the input because the user is allowed to modify the menu any time
      navItems(newValue) {
        this.buildNewMenuMapWhenPropsChange(newValue);
      },
      $route: function routeChange() {
        this.buildNewMenuMapWhenPropsChange(this.navItems);
      },
    },
    methods: {
      navigate(selectedKey) {
        if (this.smallScreenMode) {
          this.$root.$emit('bv::toggle::collapse', 'menu-collapse-control');
        }
        const menuSelectionsTemp = this.buildNewMenuMap(selectedKey);
        this.menuSelections = menuSelectionsTemp;
      },
      buildNewMenuMapWhenPropsChange(navigationItems) {
        const routeName = this.$route.name;
        const navItem = navigationItems.find(item => item.page === routeName);
        this.menuSelections = this.buildNewMenuMap(navItem ? navItem.name : navigationItems[0].name);
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
