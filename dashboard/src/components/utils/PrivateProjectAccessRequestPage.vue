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
  <div class="mt-5">
    <div class="text-center text-secondary">
      <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-shield-alt fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center text-secondary">
      <div class="h4">Invite Only Project</div>
    </div>

    <div class="row justify-content-center text-danger mt-3">
      <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="notAuthorizedExplanation">
        <p>
          This Project is configured for Invite Only access.
        </p>
        <p v-if="isEmailEnabled">
          <b-button variant="outline-primary"
                    @click="showContactOwner" data-cy="contactOwnerBtn">
            Contact Project <i aria-hidden="true" class="fas fas fa-mail-bulk"/>
          </b-button>
        </p>
      </div>
    </div>
    <contact-owners-dialog v-if="showContact" v-model="showContact" :project-id="projectId"/>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex';
  import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog';

  export default {
    name: 'PrivateProjectAccessRequestPage',
    components: {
      ContactOwnersDialog,
    },
    props: {
      explanation: String,
      projectId: String,
    },
    data() {
      return {
        showContact: false,
      };
    },
    computed: {
      ...mapGetters([
        'isEmailEnabled',
      ]),
    },
    methods: {
      showContactOwner() {
        this.showContact = true;
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  @import "../../styles/palette";

  .error-header {
    background-color: $red-palette-color3;
    border-top-left-radius: 7px;
    border-top-right-radius: 7px;
    padding: 1rem;
  }

  .error-title {
    color: whitesmoke;
    font-size: 1.5rem;
  }

  .error-body {
    border: 1px solid #ddd;
    border-bottom-left-radius: 7px;
    border-bottom-right-radius: 7px;
    padding: 1rem;
  }

</style>
