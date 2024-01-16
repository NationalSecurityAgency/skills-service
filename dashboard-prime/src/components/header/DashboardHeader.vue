<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SettingsButton from '@/components/header/SettingsButton.vue'
import HelpButton from '@/components/header/HelpButton.vue'
import SkillsBreadcrumb from '@/components/header/SkillsBreadcrumb.vue'

const route = useRoute()
const isAdminPage = computed(() => {
  return route && route.meta && route.meta.requiresAuth && !route.meta.nonAdmin
})
</script>

<template>
  <div class="header">
    <!--  see usage of preSkipToContentPlaceholder in main.js  -->
    <!--    <span id="preSkipToContentPlaceholder" tabindex="-1" aria-hidden="true" data-cy="preSkipToContentPlaceholder"/>-->
    <!--    <a-->
    <!--        class="skip-main btn btn-primary"-->
    <!--        @click="focusOnMainContent"-->
    <!--        @keydown.prevent.enter="focusOnMainContent"-->
    <!--        tabindex="0"-->
    <!--        data-cy="skipToContentButton">Skip to content</a>-->
    <!--    <div v-if="isUpgradeInProgress" class="container-fluid p-3 text-center bg-warning mb-1" data-cy="upgradeInProgressWarning">-->
    <!--      <span class="fa-stack fa-2x" style="vertical-align: middle; font-size:1em;">-->
    <!--        <i class="fas fa-circle fa-stack-2x"></i>-->
    <!--        <i class="fas fa-hammer fa-stack-1x fa-inverse"></i>-->
    <!--      </span>-->
    <!--      <span class="pl-1">An upgrade is currently in process. Please note that no changes will be permitted until the upgrade is complete.-->
    <!--      Any reported skills will be queued for application once the upgrade has completed.</span>-->
    <!--    </div>-->
    <div class="py-3">
      <div class="row">
        <div class="col-sm">
          <div class="text-center text-sm-start">
            <router-link data-cy="skillTreeLogo" class="h2 text-primary ml-2" to="/">
              <img
                ref="skillTreeLogo"
                src="@/assets/img/skilltree_logo_v1.png"
                alt="skilltree logo" />
            </router-link>
            <span v-if="isAdminPage" ref="adminStamp" class="skills-stamp">ADMIN</span>
          </div>
        </div>

        <div class="col-sm-auto text-center text-sm-end pt-sm-2 mt-3 mt-sm-0">
          <!--          <inception-button v-if="isAdminPage" class="mr-2" data-cy="inception-button"></inception-button>-->
          <settings-button data-cy="settings-button" />
          <help-button data-cy="help-button" class="ms-2" />
        </div>
      </div>
    </div>
    <skills-breadcrumb></skills-breadcrumb>
  </div>
</template>

<style scoped>
.skills-stamp {
  margin-left: 0.5rem;

  box-shadow:
    0 0 0 3px #8b6d6d,
    0 0 0 2px #8b6d6d inset;
  color: #722b2b;
  border: 2px solid transparent;
  border-radius: 4px;
  display: inline-block;
  padding: 5px 2px;
  line-height: 22px;
  font-size: 24px;
  font-family: 'Black Ops One', cursive;
  text-transform: uppercase;
  text-align: center;
  opacity: 0.8;
  width: 155px;
  transform: rotate(-17deg);
}

@media (max-width: 675px) {
  .skills-stamp {
    max-width: 9rem;
    line-height: 12px;
    font-size: 14px;
    width: 85px;
  }
}

@media (max-width: 563px) {
  .skills-stamp {
    max-width: 9rem;
    line-height: 12px;
    font-size: 14px;
    width: 85px;
  }
}

.skip-main {
  position: absolute !important;
  overflow: hidden !important;
  z-index: -999 !important;
}

.skip-main:focus,
.skip-main:active {
  left: 5px !important;
  top: 5px !important;
  font-size: 1.2em !important;
  z-index: 999 !important;
}
</style>
