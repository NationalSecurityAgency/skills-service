<script setup>
import { useField } from "vee-validate";

const props = defineProps({
  name: {
    type: String,
    required: false,
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
  fontSize: {
    type: String,
    default: '2.1rem',
  },
  isRadioIcon: {
    type: Boolean,
    default: false,
  },
  markIncorrect: {
    type: Boolean,
    default: false,
  },
  answerNumber: {
    type: Number,
  },
})

const { value, errorMessage } = useField(() => props.name, undefined, {syncVModel: true});
const model = defineModel()
function flipSelected(){
  if (!props.readOnly){
    value.value = !value.value
  }
}
</script>

<template>

<!--  # TODO - need custom component to block and allows for custom overlay like `b-overlay` with `#overylay`slot-->
<!--  <BlockUI :blocked="readOnly && markIncorrect">-->
<!--    <div v-if="readOnly && markIncorrect">-->
<!--      <i v-if="model" class="fa fa-ban text-danger" style="font-size: 1.5rem;" data-cy="wrongSelection"></i>-->
<!--      <i v-else class="fa fa-check text-danger" style="font-size: 1rem;" data-cy="missedSelection"></i>-->
<!--    </div>-->
<!--    <div v-else>-->
<!--    </div>-->
<!--  </BlockUI>-->
  <div v-on:keydown.space="flipSelected"
       @click="flipSelected"
       :tabindex="readOnly ? -1 : 0"
       role="checkbox"
       :aria-label="`Select answer number ${answerNumber} as the correct answer`"
       :aria-checked="`${value}`"
       :class="{ 'cursorPointer': !readOnly}"
       data-cy="selectCorrectAnswer">
    <i v-if="!value" data-cy="notSelected" class="far" :class="{ 'fa-square' : !isRadioIcon, 'fa-circle': isRadioIcon }" :style="{ 'font-size': fontSize }"></i>
    <i v-if="value" data-cy="selected" class="far text-primary" :class="{ 'fa-check-square' : !isRadioIcon, 'fa-check-circle': isRadioIcon }" :style="{ 'font-size': fontSize }"></i>
  </div>

</template>

<style scoped>
i {
  color: #b6b5b5;
}
.cursorPointer {
  cursor: pointer;
}
</style>