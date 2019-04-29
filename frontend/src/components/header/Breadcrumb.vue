<template>
  <!-- TODO: Consider implementing our own breadcrumb
  In order to implement dynamic names in the breadcrumb path
  vue-2-crumbs puts a lot of burden on each component to specify a list of its parents as well as params;
  Let's consider implementing out own breadcrumb impl, perhaps using vuex
  -->

  <!--    <nav class="breadcrumb" aria-label="breadcrubms">-->
  <!-- To support icons in the current item we'd need to patch the existing lib, TODO: submit bug to the lib

    THERE IS A BUG IN vue-2-crumbs, the bug is with slot=current, to fix change frontend/node_modules/vue-2-crumbs/index.js the following:
      <slot name="current" :label="getRouteLabel(currentRoute)"

      to

      <slot name="current" :label="getRouteLabel(currentRoute)" :utils="currentRoute.meta.breadcrumb.utils">
  -->

  <nav aria-label="breadcrumb">
    <app-breadcrumbs container="ol" class="breadcrumb">
      <li slot-scope="{to, label, utils}" class="breadcrumb-item">
        <router-link :to="to" class="" :itemprop="utils && utils.itemprop">
          <span class="">{{ label }}</span>
        </router-link>
      </li>

      <span slot-scope="{label}" slot="current">
         <a class="breadcrumb-item active">
          <!--<span class="icon is-small">-->
           <!--<i :class="utils && utils.iconClass"/>-->
           <!--</span>-->
          <span>{{ label }}</span>
         </a>
        </span>

    </app-breadcrumbs>
  </nav>
</template>

<script>
  export default {
    name: 'Breadcrumb',
  };
</script>

<style lang="scss" scoped>
  @import "../../styles/palette";

  .breadcrumbContainer {
    border-color: #E8E8E8;
    border-width: 1px 0px 1px 0px;
    border-style: solid;
    padding: 8px 10px 8px 40px;

    background-image: linear-gradient(to right, $blue-palette-color5, lightgray);
  }

</style>
