/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';

export const useAdminGroupState = defineStore('adminGroupState', () => {
  const adminGroup = ref(null);
  const loadingAdminGroup = ref(true)

  function loadAdminGroup(adminGroupId) {
    return new Promise((resolve, reject) => {
      loadingAdminGroup.value = true
      AdminGroupsService.getAdminGroupDef(adminGroupId)
        .then((response) => {
          adminGroup.value = response;
          resolve(response);
        })
        .catch((error) => reject(error))
        .finally(() => {
          loadingAdminGroup.value = false
        });
    });
  }

  const afterAdminGroupLoaded = () => {
    return new Promise((resolve) => {
      (function waitForAdminGroup() {
        if (!loadingAdminGroup.value) return resolve(adminGroup.value);
        setTimeout(waitForAdminGroup, 100);
        return adminGroup.value;
      }());
    });
  }

  return {
    adminGroup,
    loadAdminGroup,
    loadingAdminGroup,
    afterAdminGroupLoaded
  }
});