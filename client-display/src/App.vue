<template>
  <div id="app" style="width: 1000px; margin-left:auto; margin-right: auto;">
    <user-skills
      :service-url="serviceUrl"
      :project-id="projectId"
      :token="token"/>
  </div>
</template>

<script>
  import UserSkills from '@/userSkills/UserSkills.vue';

  import '@fortawesome/fontawesome-free/css/all.css';
  import 'bootstrap/dist/css/bootstrap.css';

  export default {
    name: 'app',
    components: {
      UserSkills,
    },
    data() {
      return {
        serviceUrl: 'http://localhost:8080',
        projectId: 'MyProject',
        // eslint-disable-next-line
        token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsic2tpbGxzLXNlcnZpY2Utb2F1dGgiXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sInByb3h5X3VzZXIiOiJtQG0uY29tIiwiZXhwIjoxNTUyNDU4NTg2LCJhdXRob3JpdGllcyI6WyJST0xFX1RSVVNURURfQ0xJRU5UIl0sImp0aSI6ImFlNWIzMDQzLTJlMDYtNGJmNy1iOGIxLWJiNzRiOTBlZjNhNiIsImNsaWVudF9pZCI6Ik15UHJvamVjdCJ9.fiWVkJEQYBjCJXhU9SbWiOQ61Q5UAd-TOqMPgxdHUNOAPXfXto6cdVg5T-Lc9bkDj9TMJ-o5o5uqT5Kdh_jNI0LU1Bpk1yjt1XD4gTnsaSZTh-_IZzAs8581D-8EhxZlp6T_MR8GuIqX6dZVnNvUzu0pKCq2pEA21UVe1FMOzUsbPMh132Fw3IcreUeluyU7cWmJmjpbNamDqhlxAa9dkpXeryY6_sBwvS_HINPw615rS76DpGAJIU6qu2Ix1icZs2dS1O9mpNDCsGoPcbHTfZ5H2CqUQ00kljXEDPZHduS0ohXmEm8HyhbqbAcRN_trKrjUYdNZUvZBVRQ2Dp0M5g'
      };
    },
  };
</script>

