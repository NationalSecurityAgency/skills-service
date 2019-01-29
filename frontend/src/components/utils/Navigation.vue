<template>
  <div class="columns">
    <div class="column is-narrow">
      <div class="skills-pad-right-2-rem">
        <h3 class="title is-4 logo">Navigation</h3>
        <aside class="menu">
          <ul class="menu-list">
            <li v-for="(navItem) of navItems" :key="navItem.name">
              <a v-on:click="navigate(`${navItem.name}`)" v-bind:class="{'is-active': menuSelections.get(navItem.name)}">
                <i v-bind:class="navItem.iconClass" class="fas fa-w-16"/> {{ navItem.name }}</a>
            </li>
          </ul>
      </aside>
      </div>
    </div>
    <div class="column">
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
  /** THESE CAN BE TAKEN OUT AFTER SASS BASED CUSTOMIZATION **/

  i {
    width: 2rem;
  }

  li {
    margin-bottom: 0.75rem;
    font-size: 1.1rem;

  }

  a {
    color: #3273dc;
  }

</style>
