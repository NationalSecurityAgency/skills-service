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
    <div class="flex-fill card">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left text-uppercase">
                Badges Catalog
            </h6>
        </div>
        <div class="card-body">
            <div class="" v-for="(badge, index) in badges" v-bind:key="badge.badgeId">
                <badge-catalog-item :badge="badge" class="pb-3" :badgeRouterLinkGenerator="badgeRouterLinkGenerator"></badge-catalog-item>
                <div v-if="index !== badges.length - 1">
                    <hr/>
                </div>
            </div>
            <div v-if="!badges || badges.length === 0" class="skills-no-data-yet text-primary text-center" data-cy="badge-catalog_no-badges">
                {{ noBadgesMessage }}
            </div>
        </div>
    </div>
</template>

<script>
  import BadgeCatalogItem from '@/common/badges/BadgeCatalogItem';

  export default {
    name: 'BadgesCatalog',
    components: { BadgeCatalogItem },
    props: {
      badges: {
        type: Array,
        required: true,
      },
      badgeRouterLinkGenerator: {
        type: Function,
        required: true,
      },
      noBadgesMessage: {
        type: String,
        required: false,
        default: 'No Badges left to earn!',
      },
    },
    data() {
      return {
        colors: ['text-success', 'text-warning', 'text-danger', 'text-info'],
      };
    },
    methods: {
      getIconColor(index) {
        const colorIndex = index % this.colors.length;
        const color = this.colors[colorIndex];
        return color;
      },
    },
  };
</script>

<style scoped>

</style>
