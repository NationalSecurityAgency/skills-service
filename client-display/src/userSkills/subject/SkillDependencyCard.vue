<template>
    <div class="card">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">{{ title }}</h6>
        </div>
        <div class="card-body text-left">
            <div class="row">
                <div class="col-md-9 text-left">
                    {{ progress.currentPoints }} / {{ progress.totalPoints }} Points
                </div>
                <div class="col-md-3 text-right">
                    <span class="text-muted">{{ progress.percentComplete }}%</span>
                </div>
            </div>
            <div>
                <progress-bar bar-color="lightgreen" :val="progress.percentComplete"></progress-bar>
            </div>
            <div>
                <p class="skill-description"><small>{{ skill.description.description }}</small></p>
            </div>
        </div>
        <div class="card-footer">
            <div class="row">
                <div v-show="skill.description.href" class="col text-left">
                    <span>Need help?</span>
                    <a :href="skill.description.href" target="_blank">
                        Click here!
                    </a>
                </div>
                <div v-if="hasOkButton" class="col text-right">
                    <button class="btn btn-info pull-right" v-on:click="close">OK</button>
                </div>
            </div>


        </div>
    </div>
</template>

<script>
    import ProgressBar from 'vue-simple-progress';

    export default {
        name: 'SkillDependencyCard',
        components: {
            ProgressBar,
        },
        props: {
            skill: Object,
            hasOkButton: {
                type: Boolean,
                required: false,
                default: true,
            },
        },
        methods: {
            close() {
                this.$emit('close');
            },
        },
        computed: {
            title() {
                return this.skill.skill;
            },
            progress() {
                return {
                    currentPoints: this.skill.points,
                    totalPoints: this.skill.totalPoints,
                    percentComplete: (this.skill.points / this.skill.totalPoints) * 100,
                };
            },
        },
    };
</script>

<style scoped>

</style>
