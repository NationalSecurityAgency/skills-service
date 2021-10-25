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
    <badge-details-overview :badge="badge" :icon-color="iconColor" :display-project-name="displayProjectName">
        <template slot="body-footer" slot-scope="{ props }">
            <router-link :to="badgeRouterLinkGenerator(badge)"
                         class="btn btn-sm btn-outline-info skills-theme-btn mr-1 text-uppercase"
                         :data-cy="`badgeDetailsLink_${props.badgeId}`">
                View Details
            </router-link>
        </template>
    </badge-details-overview>
</template>

<script>
  import BadgeDetailsOverview from './BadgeDetailsOverview';

  export default {
    name: 'BadgeCatalogItem',
    components: {
      BadgeDetailsOverview,
    },
    props: {
      badge: {
        type: Object,
        required: true,
      },
      iconColor: {
        type: String,
        default: 'text-success',
      },
      badgeRouterLinkGenerator: {
        type: Function,
        required: true,
      },
      displayProjectName: {
        type: Boolean,
        required: false,
        default: false,
      },
    },
  };
</script>

<style scoped>

</style>
