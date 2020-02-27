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
  <div class="mt-5">
    <div class="text-center text-secondary">
      <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-heart-broken fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center text-secondary">
      <h4 class="">Tiny-bit of an error!</h4>
    </div>

    <div class="row justify-content-center text-danger m-3">
      <div class="col col-sm-8 col-md-6 col-lg-5 text-center">
        <p>
          Oh no, something went wrong! An error occurred and we are unable to process your request, please try again
          later.
        </p>
        <p><strong>{{ errorMessage }}</strong></p>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      errorMessage: null,
    },
  };
</script>

<style lang="scss" scoped>
  .error-container {
    margin: 0 auto;
    width: 45rem;
  }

  .skills-pad-top-1-rem {
    padding-top: 1rem;
  }

  .error-header {
    background-color: #161616;
    border-top-left-radius: 7px;
    border-top-right-radius: 7px;
    padding: 1rem;
  }
  .error-title {
    color: whitesmoke;
    font-size: 1.5rem;
  }
  .error-body {
    border: 1px solid #ddd;
    border-bottom-left-radius: 7px;
    border-bottom-right-radius: 7px;
    padding: 1rem;
  }

  .error-icon {
    font-size: 5rem;
  };
</style>
