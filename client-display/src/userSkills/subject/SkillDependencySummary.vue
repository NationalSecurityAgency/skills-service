<template>
    <div class="skills-dep-container container" style="max-width: 18rem;">
        <!--see if can make something like this https://themes.getbootstrap.com/preview/?theme_id=19889&show_new= (under analytics -->
        <div class="row">
            <div class="col-md-9 text-left">
                <strong>{{ numDependencies }}</strong> Dependencies
            </div>
            <div class="col-md-3">
                <span class="text-muted">{{ percentComplete }}%</span>
            </div>
        </div>
        <progress-bar bar-color="lightgreen" :val="percentComplete"></progress-bar>
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
    .skills-dep-container {
        border: #e3e3e3 solid 1px;
        border-radius: 5px;
        padding: 15px 17px 15px 15px;
    }
    .skills-dep-container strong {
        font-size: 1.4rem;
    }
</style>
