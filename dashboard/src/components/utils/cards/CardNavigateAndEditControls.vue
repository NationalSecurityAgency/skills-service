/*
Copyright 2020 SkillTree

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
<template>
  <div class="row">
    <div class="col-auto">
      <b-button
        :to="options.navTo"
        variant="outline-primary" size="sm" class="mr-2"
        :aria-label="`Manage ${options.type} ${options.name}`"
        :data-cy="`manageBtn_${options.id}`">
        Manage <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
      </b-button>
    </div>

    <div class="col text-right">
      <b-button-group size="sm" class="buttons">
        <b-button v-if="options.showShare === true"
                  ref="shareBtn"
                  size="sm"
                  variant="outline-primary"
                  @click="handleShareClick"
                  :title="this.shareTitle"><i :class="shareBtnIcon" aria-hidden="true"/></b-button>
        <b-button ref="editBtn"
                  size="sm"
                  variant="outline-primary"
                  @click="$emit('edit')"
                  :title="`Edit ${options.type}`"
                  data-cy="editBtn"><i class="fas fa-edit" aria-hidden="true"/></b-button>

        <span v-b-tooltip.hover="options.deleteDisabledText">
          <b-button variant="outline-primary"
                    class="last-right-group-btn"
                    size="sm"
                    @click="$emit('delete')"
                    :disabled="options.isDeleteDisabled"
                    :title="`Delete ${options.type}`"
                    data-cy="deleteBtn"><i class="text-warning fas fa-trash" aria-hidden="true"/></b-button>
        </span>
      </b-button-group>

    </div>
  </div>
</template>

<script>
  export default {
    name: 'CardNavigateAndEditControls',
    props: {
      options: Object,
    },
    computed: {
      shareBtnIcon() {
        return this.options?.shareEnabled === true ? 'fas fa-hands-helping' : 'fas fa-handshake-alt-slash';
      },
      shareTitle() {
        return this.options?.shareEnabled === true ? `Share ${this.options?.type}` : `Unshare ${this.options?.type}`;
      },
    },
    methods: {
      handleShareClick() {
        let eventName = 'share';
        if (this.options.shareEnabled === false) {
          eventName = 'unshare';
        }
        this.$emit(eventName);
      },
      focusOnEdit() {
        this.$refs.editBtn.focus();
      },
    },
  };
</script>

<style scoped>
.last-right-group-btn {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-left: none;
}
</style>
