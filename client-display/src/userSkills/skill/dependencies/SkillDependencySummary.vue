<template>
    <div class="card">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">Progress</h6>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-8 text-left">
                    <strong>{{ numDependencies }}</strong> Dependencies
                </div>
                <div class="col-4">
                    <span class="text-muted">{{ percentComplete }}%</span>
                </div>
            </div>
            <progress-bar bar-color="lightgreen" :val="percentComplete"></progress-bar>
        </div>

    </div>
</template>

<script>
    import ProgressBar from 'vue-simple-progress';

    export default {
        components: {
            ProgressBar,
        },
        name: 'SkillDependencySummary',
        props: {
            dependencies: {
                type: Array,
                required: true,
            },
        },
        data() {
            return {
                numDependencies: 0,
                percentComplete: 0,
            };
        },
        mounted() {
            this.numDependencies = this.dependencies.length;
            const numCompleted = this.dependencies.filter(item => item.achieved).length;
            if (this.numDependencies > 0 && numCompleted > 0) {
                this.percentComplete = Math.floor((numCompleted / this.numDependencies) * 100);
            }
        },
    };
</script>

<style scoped>
</style>
