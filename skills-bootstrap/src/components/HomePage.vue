<template>
    <div>
        <error-page v-if="error"/>
        <div v-else>
            <div v-if="initialized && !accountCreated && !initializing">
                <root-registration v-if="!isPki" v-on:registerUser="onRegisterUser"/>
                <root-pki v-else v-on:grantRoot="onGrantRoot"/>
            </div>
            <initializing v-if="initializing"/>
            <div v-if="(initialized && accountCreated)">
                <success @proceed="onProceed"/>
            </div>
        </div>
    </div>
</template>

<script>
    import BootstrapService from './BootstrapService';
    import RootPki from './RootPki';
    import RootRegistration from './RootRegistration';
    import Success from './Success';
    import Initializing from './Initializing';
    import ErrorPage from './ErrorPage';

    export default {
        name: 'HomePage',
        components: { ErrorPage, Initializing, RootPki, RootRegistration, Success },
        data() {
            return {
                initialized: false,
                initializing: false,
                isPki: false,
                accountCreated: false,
                error: false,
            };
        },
        created() {
            if (!this.initialized) {
                BootstrapService.isLoggedIn()
                    .then((response) => {
                        if (response) {
                            this.isPki = true;
                        }
                    })
                    .catch(() => {
                        this.error = true;
                    })
                    .finally(() => {
                        this.initialized = true;
                    });
            }
        },
        methods: {
            onRegisterUser(loginFields) {
                this.initializing = true;
                BootstrapService.registerUser(loginFields)
                    .then(() => {
                        this.accountCreated = true;
                    })
                    .catch(() => {
                        this.error = true;
                    })
                    .finally(() => {
                        this.initializing = false;
                    });
            },
            onGrantRoot() {
                this.initializing = true;
                BootstrapService.grantRoot()
                    .then(() => {
                        this.accountCreated = true;
                    })
                    .catch(() => {
                        this.error = true;
                    })
                    .finally(() => {
                        this.initializing = false;
                    });
            },
            onProceed() {
                window.location = '/';
            },
        },
    };
</script>

<style scoped>

</style>
