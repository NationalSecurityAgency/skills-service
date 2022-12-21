<template>
    <div class="answer-row"
         v-on:keydown.space="flipSelected"
         @click="flipSelected"
         tabindex="0"
         :class="{ 'selected-answer': selected }"
         aria-label="Select as the correct answer">
      <div class="row no-gutters">
        <div class="col-auto">
          <span class="checkmark">
            <i :class="{
              'text-success' : selected,
              'far fa-square' : squareSelector && !selected,
              'far fa-check-square' : squareSelector && selected,
              'far fa-circle' : !squareSelector && !selected,
              'far fa-check-circle' : !squareSelector && selected,
            }" />
          </span>
        </div>
        <div class="col ml-2">
          <span class="answerText">{{ a.answerOption }}</span>
        </div>
      </div>
    </div>
</template>

<script>

  export default {
    name: 'QuizRunAnswer',
    props: {
      a: Object,
      readOnly: {
        type: Boolean,
        default: false,
      },
      squareSelector: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        selected: this.value,
      };
    },
    watch: {
      selected(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      flipSelected() {
        if (!this.readOnly) {
          this.selected = !this.selected;
          this.$emit('selected', this.selected);
        }
      },
    },
  };
</script>

<style scoped>
.answer-row {
  padding: 0.2rem 1rem 0rem 1rem  !important;
  margin-bottom: 0.1rem;
  border: 1px dotted transparent;
}

.answer-row:hover {
  border: 1px dotted #007c49;
  border-radius: 5px;
  cursor: pointer;
}

i {
  color: #b6b5b5;
}
.checkmark {
  font-size: 1.2rem;
}

.selected-answer {
  background-color: lightgray;
  border: 1px dotted #007c49;
  border-radius: 5px;
  font-weight: bold;
}

.answerText {
  font-size: 0.8rem;
}
</style>
