<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { useAppConfig } from '@/components/utils/UseAppConfig'

const store = useStore()
const router = useRouter()
const appConfig = useAppConfig()

const menu = ref()
let allItemsTmp = [
  {
    label: 'Official Docs',
    icon: 'pi pi-book',
    url: `${appConfig.docsHost.value}`
  },
  {
    separator: true
  },
  {
    label: 'Guides',
    items: [
      {
        label: 'Dashboard',
        icon: 'pi pi-info-circle',
        url: `${appConfig.docsHost.value}/dashboard/user-guide/`
      },
      {
        label: 'Integration',
        icon: 'pi pi-info-circle',
        url: `${appConfig.docsHost.value}/skills-client/`
      }
    ]
  }
]

const configs = store.getters.config
const dupKeys = Object.keys(configs)
  .filter((conf) => conf.startsWith('supportLink'))
  .map((filteredConf) => filteredConf.substr(0, 12))
const keys = dupKeys.filter((v, i, a) => a.indexOf(v) === i)
if (keys && keys.length > 0) {
  const items = keys.map((key) => {
    return {
      label: configs[`${key}Label`],
      icon: configs[`${key}Icon`],
      url: configs[key]
    }
  })
  allItemsTmp.push({
    label: 'Support',
    items
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
      icon="pi pi-question"
      severity="primary"
      rounded
      outlined
      raised
      @click="toggle"
      aria-label="Help Button"
      aria-haspopup="true"
      aria-controls="help_settings_menu" />
    <Menu ref="menu" id="help_settings_menu" :model="items" :popup="true">
      <template #item="{ item, props }">
        <a :href="item.url" target="_blank" v-bind="props.action">
          <span :class="item.icon" />
          <span class="ms-2">{{ item.label }}</span>
        </a>
      </template>
    </Menu>
  </div>
</template>

<style scoped></style>
