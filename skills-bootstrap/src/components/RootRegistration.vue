<template>
    <bootstrap-container title="Welcome! Let's create root account.">
        <form @submit.prevent="validateAndLogin">
            <div class="form-group">
                <label class="label">First Name</label>
                <input
                        class="form-control"
                        type="text"
                        v-model="loginFields.firstName"
                        name="firstName"
                        v-validate="'required'"
                        data-vv-delay="500"
                >
                <p class="help is-danger" v-show="errors.has('firstName')">{{errors.first('firstName')}}</p>
            </div>
            <div class="form-group">
                <label class="label">Last Name</label>
                <input
                        class="form-control"
                        type="text"
                        v-model="loginFields.lastName"
                        name="lastName"
                        v-validate="'required'"
                        data-vv-delay="500"
                >
                <p class="help is-danger" v-show="errors.has('lastName')">{{errors.first('lastName')}}</p>
            </div>
            <div class="form-group">
                <label class="label">Email</label>
                <input
                        class="form-control"
                        type="text"
                        v-model="loginFields.email"
                        name="email"
                        v-validate="'required|email|uniqueEmail'"
                        data-vv-delay="500"
                >
                <p class="help is-danger" v-show="errors.has('email')">{{errors.first('email')}}</p>
            </div>
            <div class="form-group">
                <label class="label">Password</label>
                <input
                        class="form-control"
                        type="password"
                        v-model="loginFields.password"
                        name="password"
                        ref="password"
                        placeholder="At least 8 characters"
                        v-validate="'required|min:8|max:15'"
                        data-vv-delay="500"
                >
                <p class="help is-danger" v-show="errors.has('password')">{{errors.first('password')}}</p>
            </div>
            <div class="form-group">
                <label class="label">Re-enter Password</label>
                <input
                        class="form-control"
                        type="password"
                        v-model="loginFields.password_confirmation"
                        name="password_confirmation"
                        v-validate="'required|confirmed:password'"
                        data-vv-delay="500"
                        data-vv-as="password"
                >
                <p class="help is-danger" v-show="errors.has('password_confirmation')">
                    {{errors.first('password_confirmation')}}</p>
            </div>
            <div>
                <button class="btn btn-outline-primary" :disabled="errors.any() || missingRequiredValues()">
                    Create Account
                    <i class="fas fa-arrow-circle-right"/>
                </button>
            </div>
        </form>
    </bootstrap-container>
</template>

<script>
    import { Validator } from 'vee-validate';
    import BootstrapService from './BootstrapService';
    import BootstrapContainer from './BootstrapContainer';

    const dictionary = {
        en: {
            attributes: {
                password: 'Password',
                email: 'Email',
                firstName: 'First Name',
                lastName: 'Last Name'
            }
        }
    };
    Validator.localize(dictionary);
    Validator.extend(
        'uniqueEmail',
        {
            getMessage: 'The email address is already used for another account.',
            validate(value) {
                return BootstrapService.userWithEmailExists(value)
                    .catch(e => {
                        throw e;
                    });
            }
        },
        {
            immediate: false
        }
    );

    export default {
        name: 'RootRegistration',
        components: { BootstrapContainer },
        data() {
            return {
                loginFields: {
                    firstName: '',
                    lastName: '',
                    email: '',
                    password: '',
                    password_confirmation: '',
                },
            };
        },
        methods: {
            validateAndLogin() {
                this.$validator.validate()
                    .then(valid => {
                        if (valid) {
                            this.$emit('registerUser', this.loginFields);
                        }
                    });
            },
            missingRequiredValues() {
                return (
                    !this.loginFields.firstName ||
                    !this.loginFields.lastName ||
                    !this.loginFields.email ||
                    !this.loginFields.password ||
                    !this.loginFields.password_confirmation
                );
            }
        }
    };
</script>

<style scoped>
</style>
