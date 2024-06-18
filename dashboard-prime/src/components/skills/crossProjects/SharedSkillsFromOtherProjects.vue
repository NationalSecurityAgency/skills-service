<script setup>
import { ref, onMounted } from 'vue';
import SkillsShareService from '@/components/skills/crossProjects/SkillsShareService.js';
import SharedSkillsTable from "@/components/skills/crossProjects/SharedSkillsTable.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";

const props = defineProps(['projectId']);

const loading = ref(true);
const sharedSkills = ref([]);

const loadSharedSkills = () => {
  loading.value = true;
  SkillsShareService.getSharedWithmeSkills(props.projectId)
      .then((data) => {
        sharedSkills.value = data;
        loading.value = false;
      });
};

onMounted(() => {
  loadSharedSkills();
})
</script>

<template>
  <Card class="mb-3"
        :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }"
        data-cy="skillsSharedWithMeCard">
    <template #header>
      <SkillsCardHeader title="Available skills from other projects for use as prerequisites"></SkillsCardHeader>
    </template>
    <template #content>
      <div v-if="sharedSkills && sharedSkills.length > 0">
        <shared-skills-table :shared-skills="sharedSkills" :disable-delete="true"></shared-skills-table>
      </div>
      <div v-else>
        <no-content2 title="No Skills Available Yet..." icon="far fa-handshake"
                     class="p-5"
                     message="Coordinate with other projects to share skills with this project."></no-content2>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>