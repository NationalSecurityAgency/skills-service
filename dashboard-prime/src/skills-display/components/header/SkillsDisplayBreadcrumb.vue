<script setup>
import { computed, onMounted, ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useRoute } from 'vue-router'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SkillsBreadcrumbItem from '@/components/header/SkillsBreadcrumbItem.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const themeState = useSkillsDisplayThemeState()
const displayPreferences = useSkillsDisplayPreferencesState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const route = useRoute()
const disabled = computed(() => themeState.theme.disableBreadcrumb)
const items = ref([])

onMounted(() => {
  build()
})

const build = () => {
  let ignoreNext = false;
  const path = skillsDisplayInfo.cleanPath(route.path)
  const res = path.split('/').filter((item) => item);
  res.unshift('');
  let key = null;
  const newItems = [];
  if (res) {
    res.forEach((item, index) => {
      if (!ignoreNext && item !== 'global') {
        const value = item === '' ? 'Overview' : item;
        // treat crossProject as a special case
        if (value === 'crossProject') {
          ignoreNext = true;
          key = 'Dependency';
          return;
        }
        if (key) {
          if (!shouldExcludeKey(key)) {
            newItems.push(buildResItem(key, value, res, index));
          }
          key = null;
        } else {
          // must exclude items in the path because each page with navigation
          // when parsing something like '/subjects/subj1/skills/skill1' we must end up with:
          // 'Overview / subjects:subj1/ skills:skill1'
          if (!shouldExcludeValue(value)) {
            newItems.push(buildResItem(key, value, res, index));
          }
          if (value !== 'Overview') {
            key = value;
          }
        }
      } else {
        ignoreNext = false;
      }
    });
  }
  items.value = newItems;
}

const idsToExcludeFromPath = ['subjects', 'skills', 'crossProject', 'dependency', 'global', 'quizzes']
const keysToExcludeFromPath = []
const shouldExcludeKey = (key) => {
   keysToExcludeFromPath.some((searchForMe) => key === searchForMe);
}
const shouldExcludeValue = (item) => {
  return idsToExcludeFromPath.some((searchForMe) => item === searchForMe);
}

const buildResItem =(key, item, res, index) => {
  const decodedItem = decodeURIComponent(item);
  return {
    label: key ? prepKey(key) : null,
    value: !key ? capitalize(hyphenToCamelCase(decodedItem)) : decodedItem,
    url: getUrl(res, index + 1),
  };
}
const getUrl = (arr, endIndex) => {
  const prefix = endIndex === 1 ? '/' : '';
  return `${prefix}${arr.slice(0, endIndex).join('/')}`;
}

const prepKey = (key) => {
  let res = key;
  if (key.endsWith('zes')) {
    res = key.substring(0, key.length - 3);
  } else {
    res = key.endsWith('s') ? key.substring(0, key.length - 1) : key;
  }
  return capitalize(substituteCustomLabels(res));
}

const capitalize = (value) => {
  return value.charAt(0).toUpperCase() + value.slice(1);
}
const hyphenToCamelCase = (value) => {
  return value.replace(/-([a-z])/g, (g) => ` ${g[1].toUpperCase()}`);
}
const substituteCustomLabels = (label) => {
  if (label.toLowerCase() === 'project') {
    return displayPreferences.projectDisplayName;
  }
  if (label.toLowerCase() === 'subject') {
    return displayPreferences.subjectDisplayName;
  }
  if (label.toLowerCase() === 'group') {
    return displayPreferences.groupDisplayName;
  }
  if (label.toLowerCase() === 'skill') {
    return displayPreferences.skillDisplayName;
  }
  return label;
}
const home = ref({
  icon: 'pi pi-home'
});


</script>

<template>
  <div v-if="!disabled"
       class="skills-theme-breadcrumb-container flex justify-content-center" data-cy="breadcrumb-bar">
<!--    <nav aria-label="breadcrumb" role="navigation" class="skills-theme-breadcrumb">-->
<!--      <ol class="breadcrumb bg-transparent m-0 p-0">-->
<!--        <li v-for="(item, index) of items" :key="item.label" class="breadcrumb-item theme-link" data-cy="breadcrumb-item">-->
<!--         <span v-if="index === items.length-1" class="text-muted skills-theme-breadcrumb-current-page" :data-cy="`breadcrumb-${item.value}`">-->
<!--           <span v-if="item.label" class="breadcrumb-item-label text-uppercase" aria-current="page">{{ item.label }}: </span><span>{{ item.value }}</span>-->
<!--         </span>-->
<!--          <span v-else>-->
<!--           <router-link :to="item.url" :data-cy="`breadcrumb-${item.value}`" class="skills-page-title-text-color">-->
<!--             <span v-if="item.label" class="breadcrumb-item-label text-uppercase">{{ item.label }}: </span>-->
<!--             <span class="">{{ item.value }}</span>-->
<!--           </router-link>-->
<!--         </span>-->
<!--        </li>-->
<!--      </ol>-->
<!--    </nav>-->
    <Breadcrumb :model="items" :pt="{ root: { class: 'border-none px-0 py-1' } }">
      <template #item="{ item, props }">
        <router-link
          v-if="!item.isLast"
          v-slot="{ href, navigate }"
          :to="item.url"
          custom>
          <a :href="href" v-bind="props.action" @click="navigate">
            <skills-breadcrumb-item
              :icon="item.icon"
              :label="item.label"
              :value="item.value"
              value-css="text-primary" />
          </a>
        </router-link>
        <div v-else>
          <skills-breadcrumb-item
            :icon="item.icon"
            :label="item.label"
            :value="item.value" />
        </div>
      </template>
    </Breadcrumb>
  </div>
</template>

<style scoped>

</style>