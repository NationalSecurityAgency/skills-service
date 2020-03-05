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
    <div class="container mt-2" v-if="showNewVersionAlert">
        <div class="alert alert-warning alert-dismissible fade show" role="alert">
            New Skills Software Version is Available!! Please refresh the page.
            <br/>
            {{ JSON.stringify(events)}}
        </div>
    </div>
</template>

<script>
    export default {
        name: 'NewSoftwareVersionComponent',
        data() {
            return {
                showNewVersionAlert: false,
                currentLibVersion: undefined,
                events: [],
            };
        },
        mounted() {
            this.updateStorageIfNeeded();
        },
        computed: {
            libVersion() {
                return this.$store.state.softwareVersion;
            },
        },
        watch: {
            libVersion() {
                this.events.push({
                    type: 'Consider Display',
                    before: localStorage.skillsClientDisplayLibVersion,
                    after: this.libVersion,
                });
                if (localStorage.skillsClientDisplayLibVersion !== undefined && this.libVersion.localeCompare(localStorage.skillsClientDisplayLibVersion) > 0) {
                    this.events.push({
                        type: 'Displayed',
                        before: localStorage.skillsClientDisplayLibVersion,
                        after: this.libVersion,
                    });
                    this.showNewVersionAlert = true;
                }
                this.updateStorageIfNeeded();
            },
        },
        methods: {
            updateStorageIfNeeded() {
                const storedVal = localStorage.skillsClientDisplayLibVersion;
                const currentVersion = this.libVersion;
                this.events.push({
                    type: 'Consider Updated',
                    before: storedVal,
                    after: currentVersion,
                });
                if (currentVersion !== undefined && (storedVal === undefined || currentVersion.localeCompare(storedVal) > 0)) {
                    this.events.push({
                        type: 'Updated',
                        before: storedVal,
                        after: currentVersion,
                    });
                    localStorage.skillsClientDisplayLibVersion = currentVersion;
                }
            },
        },
    };
</script>

<style scoped>
  .newVersionAlert {
    /*max-width: 70rem;*/
  }
</style>
