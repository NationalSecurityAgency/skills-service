/*
Copyright 2024 SkillTree

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
<script setup>

import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import RoleManager from '@/components/access/RoleManager.vue';


const route = useRoute()
const badgeId = computed(() => {
  return route.params.badgeId
});
</script>

<template>
  <div>
    <SubPageHeader title="Access" />

    <role-manager id="add-group-admin-access-user"
                  :badge-id="badgeId"
                  title=""
                  role-description="Global Badge Admin"
                  data-cy="roleManager"
                  :roles="['ROLE_GLOBAL_BADGE_ADMIN']">
    </role-manager>
  </div>
</template>

<style scoped>

</style>