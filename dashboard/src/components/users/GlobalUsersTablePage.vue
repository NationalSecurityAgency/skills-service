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
import UsersTable from './UsersTable.vue';
import {useBadgeState} from "@/stores/UseBadgeState.js";
import {onMounted, ref} from "vue";
import {storeToRefs} from "pinia";
import {useRoute} from "vue-router";
import NoContent2 from "@/components/utils/NoContent2.vue";

const route = useRoute();
const isLoading = ref(true);
const badgeId = ref(route.params.badgeId);
const badgeState = useBadgeState();
const { badge } = storeToRefs(badgeState);

onMounted(() => {
  loadGlobalBadge();
});

const loadGlobalBadge = () => {
  badgeState.loadGlobalBadgeDetailsState(badgeId.value).finally(() => {
    badge.value = badgeState.badge;
    isLoading.value = false;
  });
}
</script>

<template>
  <div class="usersTable">
    <Card :pt="{ body: { class: 'p-0!' } }" v-if="!isLoading">
      <template #content>
        <NoContent2 v-if="!isLoading && badge.enabled !== 'true'"
                    title="This Badge Is Not Active"
                    class="py-20 px-6"
                    data-cy="badgeNotActive">
          <div>
            <p>
              Users can only be viewed once the badge has gone live.
            </p>
          </div>
        </NoContent2>
        <UsersTable v-else />
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
