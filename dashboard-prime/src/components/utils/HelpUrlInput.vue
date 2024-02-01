<script setup>
import { ref, computed } from 'vue';
import InlineHelp from '@/components/utils/InlineHelp.vue';
import InputGroup from 'primevue/inputgroup';
import InputGroupAddon from 'primevue/inputgroupaddon';
import {useField} from "vee-validate";
import { projConfig } from '@/components/projects/ProjConfig.js';

const props = defineProps({
  name: String,
  nextFocusEl: HTMLElement,
});
const emit = defineEmits(['shown', 'hidden']);
const config = projConfig();

let internalValue = ref(props.name);
const projConfigRootHelpUrl = config.projConfigRootHelpUrl;

const overrideRootHelpUrl = computed(() => {
  return projConfigRootHelpUrl && internalValue.value && (internalValue.value.startsWith('http://') || internalValue.value.startsWith('https://'));
});

const rootHelpUrl = computed(() => {
  if (!projConfigRootHelpUrl) {
    return projConfigRootHelpUrl;
  }
  if (projConfigRootHelpUrl.endsWith('/')) {
    return projConfigRootHelpUrl.substring(0, projConfigRootHelpUrl.length - 1);
  }
  return projConfigRootHelpUrl;
});

const tooltipShown = (e) => {
  emit('shown', e);
};

const tooltipHidden = (e) => {
  emit('hidden', e);
};

const handleEscape = () => {
  document.activeElement.blur();
  props.nextFocusEl?.focus();
};

const { value, errorMessage } = useField(() => props.name);
</script>

<template>
  <div class="form-group">
    <label for="skillHelpUrl">Help URL/Path
      <inline-help v-if="$route.params.projectId"
                   target-id="helpUrlHelp"
                   :next-focus-el="nextFocusEl"
                   @shown="tooltipShown"
                   @hidden="tooltipHidden"
                   msg="If project level 'Root Help Url' is specified then this path will be relative to 'Root Help Url'"/>
    </label>

    <InputGroup>
      <InputGroupAddon v-if="projConfigRootHelpUrl">
        <i class="fas fa-cogs mr-1"></i>
        <span class="text-primary" :class="{ 'stikethrough' : overrideRootHelpUrl}" data-cy="rootHelpUrlSetting"
              id="rootHelpUrlHelp"
              :aria-label="`Root Help URL was configured in the project's settings. Root Help URL is ${rootHelpUrl}. URLs starting with http or https will not use Root Help URL.`"
              tabindex="0"
              v-tooltip.top="`Root Help URL was configured in the project's settings. URLs starting with http(s) will not use Root Help URL.`"
              @keydown.esc="handleEscape">{{ rootHelpUrl }}</span>
      </InputGroupAddon>
      <InputText v-model="value"></InputText>
    </InputGroup>
<!--    -->

<!--    <ValidationProvider rules="help_url|customUrlValidator" v-slot="{errors}"-->
<!--                        name="Help URL/Path">-->
<!--      <b-input-group>-->
<!--        <template #prepend v-if="projConfigRootHelpUrl">-->
<!--          <b-input-group-text><i class="fas fa-cogs mr-1"></i>-->
<!--            <span class="text-primary" :class="{ 'stikethrough' : overrideRootHelpUrl}" data-cy="rootHelpUrlSetting"-->
<!--                  id="rootHelpUrlHelp"-->
<!--                  :aria-label="`Root Help URL was configured in the project's settings. Root Help URL is ${rootHelpUrl}. URLs starting with http or https will not use Root Help URL.`"-->
<!--                  tabindex="0"-->
<!--                  @keydown.esc="handleEscape">{{ rootHelpUrl }}</span>-->

<!--            <b-tooltip target="rootHelpUrlHelp"-->
<!--                       title="Root Help URL was configured in the project's settings. URLs starting with http(s) will not use Root Help URL."-->
<!--                       @shown="tooltipShown"-->
<!--                       @hidden="tooltipHidden"/>-->

<!--          </b-input-group-text>-->
<!--        </template>-->
<!--        <b-form-input-->
<!--            id="skillHelpUrl"-->
<!--            v-model="internalValue" data-cy="skillHelpUrl"-->
<!--            aria-describedby="skillHelpUrlError"-->
<!--            aria-errormessage="skillHelpUrlError"-->
<!--            :aria-invalid="(errors && errors.length > 0)"></b-form-input>-->
<!--      </b-input-group>-->
<!--      <small role="alert" class="form-text text-danger" id="skillHelpUrlError"-->
<!--             data-cy="skillHelpUrlError">{{ errors[0] }}</small>-->
<!--    </ValidationProvider>-->
  </div>
</template>

<style scoped></style>
