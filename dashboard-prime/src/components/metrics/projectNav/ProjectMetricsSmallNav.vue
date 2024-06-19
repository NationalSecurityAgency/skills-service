<script setup>
import { useRoute } from 'vue-router';

defineProps(['navCards']);
const route = useRoute();

</script>

<template>
  <div class="flex gap-1">
    <router-link :to="{ name: navItem.pathName }" v-for="navItem in navCards" :key="navItem.title">
      <SkillsButton class=""
                    :aria-label="`${navItem.title} metrics`"
                    :data-cy="`${navItem.title}-metrics-link`"
                    :class="{
                      'bg-primary' : route.name === navItem.pathName,
                      'text-white' : route.name === navItem.pathName
                    }"> <i :class="navItem.icon"/><span class="sr-only">{{navItem.title}}</span></SkillsButton>
    </router-link>
  </div>
</template>

<style scoped>

</style>