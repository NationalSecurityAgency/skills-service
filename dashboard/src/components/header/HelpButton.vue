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
import {ref} from 'vue'
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import {usePagePath} from "@/components/utils/UsePageLocation.js";
import ContactProjectAdminsDialog from "@/components/contact/ContactProjectAdminsDialog.vue";
import {useRouter} from "vue-router";
import {useSupportLinksUtil} from "@/components/contact/UseSupportLinksUtil.js";

const appConfig = useAppConfig()
const pagePath = usePagePath()
const router = useRouter()
const supportLinksUtil = useSupportLinksUtil()

const menu = ref()
let allItemsTmp = [
  {
    label: 'Official Docs',
    icon: 'fas fa-book',
    url: `${appConfig.docsHost}`
  },
  {
    separator: true
  },
  {
    label: 'Guides',
    items: [
      {
        label: 'Dashboard',
        icon: 'fas fa-info-circle',
        url: `${appConfig.docsHost}/dashboard/user-guide/`
      },
      {
        label: 'Integration',
        icon: 'fas fa-hands-helping',
        url: `${appConfig.docsHost}/skills-client/`
      }
    ]
  }
]

const additionalSupportLinks = supportLinksUtil.supportLinks
if (additionalSupportLinks && additionalSupportLinks.length > 0) {
  allItemsTmp.push({
    label: 'Support',
    items: additionalSupportLinks
  })
}

const items = ref(allItemsTmp)

const toggle = (event) => {
  menu.value.toggle(event)
}
</script>

<template>
  <div class="d-inline">
    <Button
      icon="fas fa-question"
      severity="success"
      outlined
      raised
      @click="toggle"
      aria-label="Help Button"
      aria-haspopup="true"
      data-cy="helpButton"
      aria-controls="help_settings_menu" />
    <div id="help_settings_menu">
      <Menu ref="menu" :model="items" :popup="true" role="navigation">
        <template #item="{ item, props }">
          <a :href="item.url" target="_blank" v-bind="props.action">
            <span class="w-7 border text-center rounded-sm text-green-800 bg-green-50 dark:bg-gray-900 dark:text-green-500 dark:border-green-700"><i :class="item.icon"/></span>
            <span class="">{{ item.label }}</span>
          </a>
        </template>
      </Menu>
    </div>
    <contact-project-admins-dialog v-if="supportLinksUtil.showContactProjectAdminsDialog" v-model="supportLinksUtil.showContactProjectAdminsDialog"/>
  </div>
</template>

<style scoped></style>
