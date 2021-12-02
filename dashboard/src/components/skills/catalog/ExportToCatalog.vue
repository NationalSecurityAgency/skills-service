<template>
  <b-modal :id="id" size="md" :title="`Export ${exportType} to the Catalog`" v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="publishHidden"
           :aria-label="isSkill?'Export Skill to the Catalog':'Export Subject to the Catalog'">
    <p>
      This will export {{ exportType }} with id <b>[{{ id }}]</b> to the SkillTree Catalog <i class="fas fa-book" aria-hidden="true" />.
      Other project administrators will then be able to import a read-only version of this {{ exportType }}.
    </p>

    <hr/>
    <div class="h6">Visibility:
      <b-form-checkbox v-model="visibilityToAllProjects" @change="onVisibilityToAllProjects" class="mt-2 d-inline"
                       data-cy="shareWithAllProjectsCheckbox">
        <small>Share With All Projects </small><inline-help msg="Select this checkbox to share the skill with ALL projects."/>
      </b-form-checkbox>
    </div>
    <project-selector :project-id="$route.params.projectId" :selected="selectedProject"
                      v-on:selected="onSelectedProject"
                      v-on:unselected="onUnSelectedProject"
                      :only-single-selected-value="true"
                      :disabled="visibilityToAllProjects">
    </project-selector>

    <div slot="modal-footer" class="w-100">
      <b-button variant="success" size="sm" class="float-right"
                @click="handleExport"
                data-cy="exportToCatalogButton">
        Export
      </b-button>
      <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="closeButton">
        Cancel
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import ProjectSelector from '../crossProjects/ProjectSelector';

  export default {
    name: 'ExportToCatalog',
    components: { ProjectSelector },
    props: {
      id: String,
      exportType: {
        type: String,
        default: 'Skill',
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        visibilityToAllProjects: true,
        selectedProject: null,
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      isSkill() {
        return this.exportType === 'Skill';
      },
    },
    methods: {
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        this.$emit('hidden', { id: this.id, exportType: this.exportType, ...e });
      },
      handleExport() {

      },
      onVisibilityToAllProjects() {

      },
      onSelectedProject() {

      },
      onUnSelectedProject() {

      },
    },
  };
</script>

<style scoped>

</style>
