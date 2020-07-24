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
  <div class="row justify-content-center text-center" data-cy="resetNotSupported">
    <div class="col col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem;">
      <div class="mt-5">
        <i class="fa fa-users fa-4x text-secondary"></i>
        <h2 class="mt-4 text-info">Password Reset not currently enabled</h2>
      </div>
      <div class="card text-left">
        <div class="card-body p-4">
          Password Reset is not currently enabled on this system. Please contact your SkillTree administrator. Return to the <router-link to="/skills-login" data-cy="loginPage">login page</router-link>?
        </div>
      </div>

    </div>
  </div>
</template>

<script>

  export default {
    name: 'ResetNotSupportedPage',
  };
</script>
