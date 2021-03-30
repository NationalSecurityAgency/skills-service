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
                      <i class="fas fa-exclamation-triangle fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center text-secondary">
      <h4 class="">Resource Not Found</h4>
    </div>

    <div class="container-fluid">
      <div class="row justify-content-center text-danger mt-3">
        <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="notFoundExplanation">
          <p v-if="explanation">
            {{ explanation }}
          </p>
          <p v-else-if="isOldLink" data-cy="oldLinkRedirect" class="text-muted">
            It looks like you may have followed an old link. You will be forwarded to <router-link :to="newLinkValue" data-cy="newLink">
            {{ newLinkValue }}</router-link> in {{ timer }} seconds.
          </p>
          <p v-else data-cy="notFoundDefaultExplanation">
            The resource you requested cannot be located.
          </p>
        </div>
      </div>
      <div class="text-center">
        <b-button href="/" variant="outline-primary" class="p-2" data-cy="takeMeHome"><i class="fas fa-home mr-1"/>Take Me Home</b-button>
      </div>
    </div>

  </div>
</template>

<script>

  export default {
    name: 'NotFound',
    props: {
      explanation: String,
    },
    data() {
      return {
        timer: -1,
        isOldLink: false,
        newLinkValue: '',
      };
    },
    mounted() {
      if (this.$route && this.$route.redirectedFrom) {
        const { redirectedFrom } = this.$route;
        if (redirectedFrom.startsWith('/projects') || redirectedFrom.startsWith('/metrics') || redirectedFrom.startsWith('/globalBadges')) {
          if (redirectedFrom === '/projects/' || redirectedFrom === '/projects') {
            this.newLinkValue = '/administrator/';
          } else {
            this.newLinkValue = `/administrator${redirectedFrom}`;
          }
          this.isOldLink = true;
        }
      }
    },
    watch: {
      isOldLink(val) {
        if (val === true) {
          this.timer = 10;
        }
      },
      timer(value) {
        if (value > 0) {
          setTimeout(() => {
            this.timer -= 1;
          }, 1000);
        } else {
          this.$router.replace(this.newLinkValue).catch((error) => {
            this.$router.push('/error');
          });
        }
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  @import "../../styles/palette";

  .error-header {
    background-color: $red-palette-color3;
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

</style>
