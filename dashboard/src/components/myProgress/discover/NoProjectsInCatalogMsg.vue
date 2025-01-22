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
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const conf = useAppConfig()
</script>

<template>
  <div>
    <no-content2 title="No Discoverable Projects"
                 :message="`Projects can be created and managed from the Project Admin view.`" />

    <div class="flex justify-center mt-8 text-xl">
      <div class="">
        <Card>
          <template #subtitle>Instructions:</template>
          <template #content>
            <p>
              <Tag>1.</Tag>
              click on the
              <img src="@/assets/img/screenshot_settingsButton.png" alt="Settings button screenshot" />
              button on the top right and select
              <img src="@/assets/img/screenshot_projAdminNav.png" alt="Admin navigation button screenshot" />
            </p>
            <p>
              <Tag>2.</Tag>
              Create or edit a project and then navigate to the <span class="text-primary">Settings</span> tab
            </p>
            <p>
              <Tag>3.</Tag>
              Change <span class="text-primary">Project Discoverability</span> to <span class="text-primary">Add to the Project Catalog</span>
            </p>

            <p class="mt-12">
              To learn more please visit <a class="font-bold" target="_blank"
              :href="`${conf.docsHost}/dashboard/user-guide/projects.html#setting-project-discoverability`">Setting:
              Project Discoverability</a> section in the documentation.
            </p>
          </template>
        </Card>
      </div>
    </div>

  </div>
</template>

<style scoped>

</style>