<template>
  <div class="row">
    <div class="col-lg-2 border rounded p-3 mb-2 bg-light">
      <h3 class="mb-2">Navigation</h3>
      <ul class="nav flex-column">
        <li class="nav-item skills-nav-item" v-for="(navItem) of navItems" :key="navItem.name">
          <b-link class="nav-link" @click="navigate(`${navItem.name}`)"
             :class="{'bg-primary': menuSelections.get(navItem.name), 'text-light': menuSelections.get(navItem.name)}">
            <i v-bind:class="navItem.iconClass" class="fas fa-w-16" style="min-width: 2rem;"/> {{ navItem.name }}
          </b-link>
        </li>
      </ul>
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
      };
    },
    methods: {
      navigate(selectedKey) {
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
    },
  };
</script>

<style scoped>
  .skills-nav-item {
    font-size: 1.1rem;
  }
</style>
