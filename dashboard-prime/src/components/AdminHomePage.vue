<script setup>
import Navigation from "@/components/utils/Navigation.vue";
import {ref} from "vue";

const items = ref([
  {
    label: 'Projects',
    icon: 'pi pi-list',
    route: '/unstyled'
  },
  {
    label: 'Quizzes & Surveys',
    icon: 'pi pi-check-square',
    command: () => {
      this.$router.push('/introduction');
    }
  }
]);
</script>

<template>
  <div>
    <navigation :nav-items="items"
        data-cy="navigationmenu"
        role="navigation">
    </navigation>

  </div>
</template>

<style scoped></style>
