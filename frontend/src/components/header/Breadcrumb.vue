
<!--
 Please note that we heavily rely on routes' naming convention to build the breadcrumb
 Generally we expect pattern of '/entity/id/entity2/id2' which then will map to
 'entity:id / entity2:id2' breadcrumb; If the number of entities is even then the last item
 will not have entity/label, for example '/entity/id/entity2/id2/last' will produce:
 'entity:id / entity2:id2 / last'

 You can optionally override the last items display in the router config:
 meta: { breadcrumb: 'Add Skill Event' },
-->
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
            } else {
              value = value
                // insert a space before all caps
                .replace(/([A-Z])/g, ' $1')
                // uppercase the first character
                .replace(/^./, str => str.toUpperCase());
            }

            if (key) {
              newItems.push(this.buildResItem(key, value, res, index));
              key = null;
            } else {
              // must exclude items in the path because each page with navigation
              // doesn't have a sub-route in the url, for example:
              // '/projects/projectId' will conceptually map to '/projects/projectId/subjects'
              // but there is no '/project/projectId/subjects' route configured so when parsing something like
              // '/projects/projectId/subjects/subjectId/stats we must end up with:
              //    'projects / project:projectId / subject:subjectId / stats'
              // notice that 'subjects' is missing
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

<style scoped>
  .breadcrumb-item-label {
    font-size: 0.9rem;
  }

  .breadcrumb li {
    display: inline;
    max-width: 15rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>
