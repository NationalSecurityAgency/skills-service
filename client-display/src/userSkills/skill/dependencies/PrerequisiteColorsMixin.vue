<script>
  export default {
    name: 'PrerequisiteColorsMixin',
    methods: {
      getBadgeColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.badgeColor;
        if (themeColor) {
          return themeColor;
        }
        return 'indigo';
      },
      getSkillColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.skillColor;
        if (themeColor) {
          return themeColor;
        }
        return 'orange';
      },
      getThisSkillColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.thisSkillColor;
        if (themeColor) {
          return themeColor;
        }
        return '#00a4e8';
      },
      getAchievedColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.achievedColor;
        if (themeColor) {
          return themeColor;
        }
        return 'green';
      },
      getThemeNavButtonsColor() {
        const themeColor = this.$store.state.themeModule?.prerequisites?.navButtonsColor;
        return themeColor;
      },
      getThemeTextPrimaryColor() {
        const themeColor = this.$store.state.themeModule?.textPrimaryColor;
        return themeColor;
      },
    },
  };
</script>
