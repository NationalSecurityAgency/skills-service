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
    <badge-details-overview :badge="badge" :icon-color="iconColor">
        <template slot="body-footer" slot-scope="{ props }">
            <router-link :to="{ name: badge.global ? 'globalBadgeDetails' : 'badgeDetails', params: { badgeId: props.badgeId }}" tag="button"
                         class="btn btn-info btn-sm mr-1 text-uppercase">
                View Details
            </router-link>
        </template>
    </badge-details-overview>
</template>

<script>
  import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview';

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
    },
  };
</script>

<style scoped>

</style>
