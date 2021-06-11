/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div class="d-flex justify-content-center" data-cy="breadcrumb-bar">
    <nav aria-label="breadcrumb" role="navigation">
      <ol class="breadcrumb bg-transparent m-0 p-0">
        <li v-for="(item, index) of items" :key="item.label" class="breadcrumb-item" data-cy="breadcrumb-item">
         <span v-if="index === items.length-1" class="text-muted" :data-cy="`breadcrumb-${item.value}`">
           <span v-if="item.label" class="breadcrumb-item-label text-uppercase" aria-current="page">{{ item.label }}: </span><span>{{ item.value }}</span>
         </span>
          <span v-else>
           <router-link :to="item.url" :data-cy="`breadcrumb-${item.value}`">
             <span v-if="item.label" class="breadcrumb-item-label text-uppercase">{{ item.label }}: </span>
             <span class="">{{ item.value }}</span>
           </router-link>
         </span>
        </li>
      </ol>
    </nav>
  </div>

</template>

<script>
  export default {
    name: 'Breadcrumb',
    data() {
      return {
        items: [],
        homeLabel: 'Overview',
        idsToExcludeFromPath: ['subjects', 'skills', 'crossProject', 'dependency', 'global'],
        keysToExcludeFromPath: [], // will be unnecessary key for global badges
        ignoreNext: false,
      };
    },
    mounted() {
      this.build();
    },
    methods: {
      build() {
        const res = this.$route.path.split('/').filter((item) => item);
        res.unshift('');
        let key = null;
        const newItems = [];
        if (res) {
          res.forEach((item, index) => {
            if (!this.ignoreNext && item !== 'global') {
              const value = item === '' ? 'Overview' : item;
              // treat crossProject as a special case
              if (value === 'crossProject') {
                this.ignoreNext = true;
                key = 'Dependency';
                return;
              }
              if (key) {
                if (!this.shouldExcludeKey(key)) {
                  newItems.push(this.buildResItem(key, value, res, index));
                }
                key = null;
              } else {
                // must exclude items in the path because each page with navigation
                // when parsing something like '/subjects/subj1/skills/skill1' we must end up with:
                // 'Overview / subjects:subj1/ skills:skill1'
                if (!this.shouldExcludeValue(value)) {
                  newItems.push(this.buildResItem(key, value, res, index));
                }
                if (value !== 'Overview') {
                  key = value;
                }
              }
            } else {
              this.ignoreNext = false;
            }
          });
        }
        this.items = newItems;
      },
      buildResItem(key, item, res, index) {
        const decodedItem = decodeURIComponent(item);
        return {
          label: key ? this.prepKey(key) : null,
          value: !key ? this.capitalize(this.hyphenToCamelCase(decodedItem)) : decodedItem,
          url: this.getUrl(res, index + 1),
        };
      },
      getUrl(arr, endIndex) {
        const prefix = endIndex === 1 ? '/' : '';
        return `${prefix}${arr.slice(0, endIndex).join('/')}`;
      },
      prepKey(key) {
        const res = key.endsWith('s') ? key.substring(0, key.length - 1) : key;
        return this.capitalize(res);
      },
      hyphenToCamelCase(value) {
        return value.replace(/-([a-z])/g, (g) => ` ${g[1].toUpperCase()}`);
      },
      capitalize(value) {
        return value.charAt(0).toUpperCase() + value.slice(1);
      },
      shouldExcludeValue(item) {
        return this.idsToExcludeFromPath.some((searchForMe) => item === searchForMe);
      },
      shouldExcludeKey(key) {
        return this.keysToExcludeFromPath.some((searchForMe) => key === searchForMe);
      },
    },
  };
</script>
