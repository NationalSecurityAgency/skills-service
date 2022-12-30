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
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <span slot="right-of-header">
        <i v-if="badge && badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </span>
      <div slot="subSubTitle" v-if="badge">
        <b-button-group class="mb-3" size="sm">
          <b-button @click="displayEditBadge"
                    ref="editBadgeButton"
                    class="btn btn-outline-primary"
                    size="sm"
                    variant="outline-primary"
                    data-cy="btn_edit-badge"
                    :aria-label="'edit Badge '+badge.badgeId">
            <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
          <b-button :disabled="badge.enabled === 'true'"
                    @click.stop="handlePublish"
                    class="btn btn-outline-primary"
                    size="sm"
                    variant="outline-primary"
                    aria-label="Go Live"
                    data-cy="goLive">Go Live
          </b-button>
        </b-button-group>
        <div style="height: 2rem;">
          <div class="col">
            <div v-if="badge.enabled === 'false'" data-cy="badgeStatus" style="">
              <span class="text-secondary font-italic small">Status: </span>
              <span class="text-secondary small text-uppercase">Disabled <span class="far fa-stop-circle text-warning" aria-hidden="true"/></span>
            </div>
            <div v-else data-cy="badgeStatus" style="">
              <span class="text-secondary font-italic small">Status: </span> <span class="small text-primary text-uppercase align-middle">Live <span class="far fa-check-circle text-success" aria-hidden="true"/></span>
            </div>
          </div>
        </div>
      </div>
    </page-header>

    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'GlobalBadgeSkills'},
          {name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'GlobalBadgeLevels'},
        ]">
    </navigation>
    <edit-badge v-if="showEdit" v-model="showEdit" :id="badge.badgeId" :badge="badge" :is-edit="true"
                :global="true" @badge-updated="badgeEdited" @hidden="handleHidden"></edit-badge>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import Navigation from '../../utils/Navigation';
  import PageHeader from '../../utils/pages/PageHeader';
  import EditBadge from '../EditBadge';
  import GlobalBadgeService from './GlobalBadgeService';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('badges');

  export default {
    name: 'GlobalBadgePage',
    mixins: [MsgBoxMixin],
    components: {
      PageHeader,
      Navigation,
      EditBadge,
    },
    data() {
      return {
        isLoading: true,
        badgeId: '',
        showEdit: false,
      };
    },
    created() {
      this.badgeId = this.$route.params.badgeId;
    },
    mounted() {
      this.loadBadge();
    },
    computed: {
      ...mapGetters([
        'badge',
      ]),
      headerOptions() {
        if (!this.badge) {
          return {};
        }
        return {
          icon: 'fas fa-globe-americas skills-color-badges',
          title: `BADGE: ${this.badge.name}`,
          subTitle: `ID: ${this.badge.badgeId}`,
          stats: [{
            label: 'Skills',
            count: this.badge.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Levels',
            count: this.badge.requiredProjectLevels.length,
            icon: 'fas fa-trophy skills-color-levels',
          }, {
            label: 'Projects',
            count: this.badge.uniqueProjectCount,
            icon: 'fas fa-project-diagram skills-color-projects',
          }],
        };
      },
    },
    methods: {
      ...mapActions([
        'loadGlobalBadgeDetailsState',
      ]),
      ...mapMutations([
        'setBadge',
      ]),
      displayEditBadge() {
        this.showEdit = true;
      },
      loadBadge() {
        this.isLoading = false;
        if (this.$route.params.badge) {
          this.setBadge(this.$route.params.badge);
          this.isLoading = false;
        } else {
          this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
      badgeEdited(editedBadge) {
        GlobalBadgeService.saveBadge(editedBadge).then((resp) => {
          const origId = this.badge.badgeId;
          this.setBadge(resp);
          if (origId !== resp.badgeId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, badgeId: resp.badgeId } });
            this.badgeId = resp.badgeId;
          }
        }).finally(() => {
          this.handleFocus();
        });
      },
      handleHidden(e) {
        if (!e || !e.saved) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          const ref = this.$refs.editBadgeButton;
          if (ref) {
            ref.focus();
          }
        });
      },
      handlePublish() {
        if (this.canPublish()) {
          const msg = `While this Badge is disabled, user's cannot see the Badge or achieve it. Once the Badge is live, it will be visible to users.
        Please note that once the badge is live, it cannot be disabled.`;
          this.msgConfirm(msg, 'Please Confirm!', 'Yes, Go Live!')
            .then((res) => {
              if (res) {
                this.badge.enabled = 'true';
                const toSave = { ...this.badge };
                if (!toSave.originalBadgeId) {
                  toSave.originalBadgeId = toSave.badgeId;
                }
                toSave.startDate = this.toDate(toSave.startDate);
                toSave.endDate = this.toDate(toSave.endDate);
                this.badgeEdited(toSave);
              }
            });
        } else {
          this.msgOk(this.getNoPublishMsg(), 'Empty Badge!');
        }
      },
      canPublish() {
        return this.badge.numSkills > 0 || this.badge.requiredProjectLevels.length > 0;
      },
      getNoPublishMsg() {
        let msg = 'This Global Badge has no assigned Skills or Project Levels. A Global Badge cannot be published without at least one Skill or Project Level.';

        return msg;
      },
      toDate(value) {
        let dateVal = value;
        if (value && !(value instanceof Date)) {
          dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
        }
        return dateVal;
      },
    },
  };
</script>

<style scoped>

</style>
