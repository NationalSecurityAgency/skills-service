<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li v-for="(item, index) of items" :key="item.label" class="breadcrumb-item">
         <span v-if="index === items.length-1">
           <span v-if="item.label" class="breadcrumb-item-label text-uppercase">{{ item.label }}: </span><span>{{ item.value }}</span>
         </span>
         <span v-else>
           <router-link :to="item.url">
             <span v-if="item.label" class="breadcrumb-item-label text-uppercase">{{ item.label }}: </span>
             <span>{{ item.value }}</span>
           </router-link>
         </span>
      </li>
    </ol>
  </nav>
</template>

<script>
  export default {
    name: 'Breadcrumb',
    data() {
      return {
        items: [],
        idsToExcludeFromPath: ['subjects', 'skills', 'projects'],
      };
    },
    mounted() {
      this.build();
    },
    watch: {
      $route: function routeChange() {
        this.build();
      },
    },
    methods: {
      build() {
        const newItems = [this.buildHomeResItem()];
        let res = this.$route.path.split('/');
        res = res.slice(1, res.length);
        let key = null;

        const lastItemInPathCustomName = this.$route.meta.breadcrumb;

        res.forEach((item, index) => {
          let value = item;
          if (value) {
            if (index === res.length - 1 && lastItemInPathCustomName) {
              key = null;
              value = lastItemInPathCustomName;
            }

            if (key) {
              newItems.push(this.buildResItem(key, value, res, index));
              key = null;
            } else {
              if (!this.shouldExclude(value)) {
                newItems.push(this.buildResItem(key, value, res, index));
              }
              key = value;
            }
          }
        });

        this.items = newItems;
      },
      buildResItem(key, item, res, index) {
        const decodedItem = decodeURIComponent(item);
        return {
          label: key ? this.prepKey(key) : null,
          value: !key ? this.capitalize(decodedItem) : decodedItem,
          url: this.getUrl(res, index + 1),
        };
      },
      buildHomeResItem() {
        return {
          label: null,
          value: 'Home',
          url: '/',
        };
      },
      getUrl(arr, endIndex) {
        return `/${arr.slice(0, endIndex).join('/')}`;
      },
      prepKey(key) {
        const res = key.endsWith('s') ? key.substring(0, key.length - 1) : key;
        return this.capitalize(res);
      },
      capitalize(value) {
        return value.charAt(0).toUpperCase() + value.slice(1);
      },
      shouldExclude(item) {
        return this.idsToExcludeFromPath.some(searchForMe => item === searchForMe);
      },
    },
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

  .breadcrumb-item-label {
    /*font-style:  oblique;*/
    font-size: 0.9rem;
  }

</style>
