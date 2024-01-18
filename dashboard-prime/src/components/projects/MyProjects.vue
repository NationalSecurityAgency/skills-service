<script setup>
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import { useStore } from 'vuex';

const store = useStore();
const isRootUser = store.getters['access/isRoot'];

const editNewProject = () => {};
const addProjectDisabled = false;
</script>

<template>
  <div>
    <SubPageHeader title="Projects" action="Project">
      <Button v-if="isRootUser" variant="outline-primary" ref="pinProjectsButton"
                @click="showSearchProjectModal=true"
                aria-label="Pin projects to your Project page"
                role="button"
                size="sm"
                class="mr-2">
        <span class="d-none d-sm-inline">Pin</span> <i class="fas fa-thumbtack" aria-hidden="true"/>
      </Button>
      <Button id="newProjectBtn" ref="newProjButton" @click="editNewProject()"
                variant="outline-primary" size="sm"
                :disabled="addProjectDisabled"
                data-cy="newProjectButton" aria-label="Create new Project" role="button">
        <span class="d-none d-sm-inline">Project</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
      </Button>
    </SubPageHeader>
  </div>
</template>

<style scoped></style>
