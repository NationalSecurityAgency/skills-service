<script setup>
import { useRoute } from 'vue-router';

defineProps(['navCards']);
const route = useRoute();

</script>

<template>
  <ButtonGroup>
    <router-link :to="{ name: navItem.pathName }" v-for="navItem in navCards" :key="navItem.title">
      <SkillsButton class="skill-nav-button"
                    :aria-label="`${navItem.title} metrics`"
                    :data-cy="`${navItem.title}-metrics-link`"
                    :class="{'bg-primary' : route.name === navItem.pathName, 'skills-white-text' : route.name === navItem.pathName}"
                    variant="outline-info"> <i :class="navItem.icon"/><span class="sr-only">{{navItem.title}}</span></SkillsButton>
    </router-link>
  </ButtonGroup>
</template>

<style scoped>
.skills-white-text i {
  color: white !important;
}

.skill-nav-button:hover i {
  color: white !important;
}
</style>