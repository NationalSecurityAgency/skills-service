<template>
  <b-modal id="reportSkillModal"
           :hide-header="true"
           :hide-footer="true"
           size="xl"
           body-class="p-0"
           :no-close-on-backdrop="true"
           v-model="show">
    <quiz-run :quiz-id="skill.selfReporting.quizId"
              @testWasTaken="testWasTaken"
      @cancelled="cancel">
      <template slot="splashPageTitle">
        <div class="mb-2">
          <i class="fas fa-glass-cheers text-info" style="font-size: 1.5rem;"></i> You will earn <b-badge variant="success">{{ skill.points}}</b-badge> points for
          <span class="font-weight-bold text-primary" style="font-size: 1.2rem">{{ skill.skill }}</span>
          skill by passing this test.
        </div>
      </template>
    </quiz-run>
  </b-modal>
</template>

<script>
  import QuizRun from '@/common-components/quiz/QuizRun';

  export default {
    name: 'QuizModal',
    components: { QuizRun },
    props: {
      skill: Object,
      value: Boolean,
    },
    data() {
      return {
        show: this.value,
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    mounted() {
      console.log(this.skill);
    },
    methods: {
      cancel() {
        this.show = false;
      },
      testWasTaken(quizResult) {
        this.show = false;
        this.$emit('testWasTaken', quizResult);
      },
    },
  };
</script>

<style scoped>

</style>
