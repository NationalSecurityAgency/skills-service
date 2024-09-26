/*
Copyright 2024 SkillTree

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
<script setup> 

import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import InputGroup from "primevue/inputgroup";
import InputGroupAddon from "primevue/inputgroupaddon";
import Logo1 from "@/components/brand/Logo1.vue";

const appConfig = useAppConfig()
const authState = useAuthState()

const saml2Login = () => {
  authState.saml2Login(appConfig.saml2RegistrationId);
}
  
</script>

<template>


  <div class="">
    <div class="text-center mt-8">
      <div class="mt-5 justify-content-center">
        <logo1 />
      </div>
      <div class="grid ">
        <div class="col-12 sm:col-8 sm:col-offset-2 md:col-6 md:col-offset-3 lg:col-4 lg:col-offset-4">
          <Card class="mt-3">
            <template #content>
              <p class="text-center">
                <i class="far fa-check-square text-success"></i> You have been logged out of SkillTree
              </p>
              <Divider />
              <Button
                  class="w-full text-center"
                  outlined
                  icon="far fa-arrow-alt-circle-left"
                  label="Sign back in"
                  @click="saml2Login()" />

            </template>
          </Card>


        </div>
      </div>

    </div>
  </div>
</template>

<style scoped></style>
