<template>
  <div class="columns">
    <div class="column is-narrow" style="height: 800px">
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

<style lang="scss" scoped>
  @import "../../styles/palette";

  i {
    width: 2rem;
  }

  li {
    margin-bottom: 0.75rem;
    font-size: 1.1rem;
  }

  .columns .is-narrow{
    padding: 20px;
    border: 1px solid #ddd;
    box-shadow: 0 22px 35px -16px rgba(0,0,0,0.1);
    margin-bottom: 2rem;
    border-radius: 5px;
  }

  /*.columns h3 {*/
    /*border-color: lightgrey;*/
    /*border-width: 0 0 1px 0px;*/
    /*border-style: inset;*/
  /*}*/

</style>
