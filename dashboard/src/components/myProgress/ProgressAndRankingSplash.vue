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
  <div class="masthead">
    <div class="masthead-content">
      <div class="container-fluid above-background" >
        <div class="text-center">
          <img src="/static/img/skilltree_logo.svg" alt="SkillTree Logo"/>
        </div>
        <div class="h4 text-center mb-4 text-primary font-italic above-background">A micro-learning <b>gamification</b> training platform</div>
        <div class="row">
          <div class="col-md-6 col-lg mt-2">
            <b-card body-class="text-center" style="border-top: 5px solid #007c49;" class="h-100">
              <div class="h5 text-primary text-uppercase">Gamification</div>
              <div class="row no-gutters text-center">
                <div class="col-auto">
                  <div class="border rounded py-2 px-3 border-success">
                    <i class="fas fa-trophy text-success" style="font-size: 2rem;" aria-hidden="true"></i>
                  </div>
                </div>
                <div class="col text-left px-2">
                  Earn <b>points <i class="far fa-arrow-alt-circle-up skills-color-points"
                                    aria-hidden="true"/></b>, achieve <b>badges <i
                  class="fas fa-award skills-color-badges" aria-hidden="true"/></b> and complete
                  <b>skills <i class="fas fa-graduation-cap skills-color-skills"
                               aria-hidden="true"/></b>
                  all from the comfort of your own chair!
                </div>
              </div>
            </b-card>
          </div>
          <div class="col-md-6 col-lg mt-2">
            <b-card body-class="text-center" style="border-top: 5px solid #ffc42b;" class="h-100">
              <div class="h5 text-primary text-uppercase">Go At your Own Pace</div>
              <div class="row no-gutters text-center">
                <div class="col-auto">
                  <div class="border rounded py-2 px-3 border-success">
                    <i class="fas fa-clock text-warning" style="font-size: 2rem;" aria-hidden="true"></i>
                  </div>
                </div>
                <div class="col text-left px-2">
                  Learn new <b>skills <i class="fas fa-graduation-cap skills-color-skills"
                                         aria-hidden="true"/></b> and effectively master advanced
                  knowledge when it's <b>most convenient <i
                  class="fas fa-hourglass-half text-success" aria-hidden="true"></i></b> for you!
                </div>
              </div>
            </b-card>
          </div>
          <div class="col-lg mt-2">
            <b-card body-class="text-center" style="border-top: 5px solid #e76f51ff;" class="h-100">
              <div class="h5 text-info text-uppercase">Community</div>
              <div class="row no-gutters text-center">
                <div class="col-auto">
                  <div class="border rounded py-2 px-3 border-success">
                    <i class="fas fa-users" style="font-size: 2rem; color: #e76f51ff !important;" aria-hidden="true"></i>
                  </div>
                </div>
                <div class="col text-left px-2">
                  Take the learning journey <b>together <i class="fas fa-users skills-color-users"
                                                           aria-hidden="true"/></b> with your
                  peers and enjoy <b>friendly <i class="fas fa-handshake text-info"
                                                 aria-hidden="true"/></b> competition!
                </div>
              </div>
            </b-card>
          </div>
        </div>
        <div class="text-center mt-5">
          <div class="text-info mb-1 text-uppercase" style="font-size: 1.5rem;">Get Started Now</div>
          <div><b-button size="lg" :to="{ name: 'DiscoverProjectsPage' }" variant="primary" data-cy="manageMyProjsBtnInNoContent" class="animate__bounceIn"><i class="fas fa-tasks" aria-hidden="true"/> Projects Catalog</b-button></div>
        </div>
      </div>
      <div class="bg-circle-1 bg-circle"></div>
      <div class="bg-circle-2 bg-circle"></div>
      <div class="bg-circle-3 bg-circle"></div>
      <div class="bg-circle-4 bg-circle"></div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'ProgressAndRankingSplash',
  };
</script>

<style scoped>
.masthead img {
  position: relative;
  height: 12rem;
  margin-top: 2rem;
  margin-bottom: 1rem;
}

.masthead {
  margin-top: 0px !important;
  position: relative;
  overflow: hidden;
}

.masthead .masthead-content {
  z-index: 1;
  position: relative;
  height: calc(100vh - 10rem) !important;
  min-height: 42rem !important;
}

.masthead .bg-circle {
  z-index: 0;
  position: absolute;
  border-radius: 100%;
  background: linear-gradient(0deg, #2d8779 0%, #f5fffe 100%);
}

.masthead .bg-circle-1 {
  height: 90rem;
  width: 90rem;
  bottom: -55rem;
  left: -55rem;
}

.masthead .bg-circle-2 {
  height: 50rem;
  width: 50rem;
  top: -25rem;
  right: -25rem;
}

.masthead .bg-circle-3 {
  height: 20rem;
  width: 20rem;
  bottom: -10rem;
  right: 5%;
}

.masthead .bg-circle-4 {
  height: 30rem;
  width: 30rem;
  top: -22rem;
  left: -5rem;
}

.above-background {
  z-index: 50 !important;
  position: relative;
}

/* bootstrap lg breakpoint */
@media (max-width: 992px) {
  .masthead .masthead-content {
    min-height: 53rem !important;
  }
}
/* bootstrap md breakpoint */
@media (max-width: 768px) {
  .masthead .masthead-content {
    min-height: 70rem !important;
  }
}
</style>
