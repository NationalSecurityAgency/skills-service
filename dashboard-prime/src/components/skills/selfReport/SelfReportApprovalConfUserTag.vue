<script setup>
import { ref, nextTick, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const emit = defineEmits(['conf-added', 'conf-removed']);
const announcer = useSkillsAnnouncer();
const route = useRoute();
const props = defineProps({
  userInfo: Object,
  tagLabel: String,
  tagKey: String,
});

const enteredTag = ref('');
const table = ref({
  items: [],
      options: {
    busy: false,
        bordered: true,
        outlined: true,
        stacked: 'md',
        sortBy: 'updated',
        sortDesc: true,
        emptyText: 'You are the only user',
        tableDescription: 'Configure Approval Workload',
        fields: null,
        pagination: {
      remove: false,
          server: false,
          currentPage: 1,
          totalRows: 1,
          pageSize: 4,
          possiblePageSizes: [4, 10, 15, 20],
    },
  },
});

onMounted(() => {
  const hasTagConf = props.userInfo.tagConf && props.userInfo.tagConf.length > 0;
  if (hasTagConf) {
    table.value.items = props.userInfo.tagConf.map((u) => ({ ...u }));
  }
});
const addTagConf = () => {
  if (enteredTag.value && enteredTag.value !== '') {
    // refs.validationProvider.validate()
    //     .then((validationRes) => {
    //       if (validationRes.valid) {
            SelfReportService.configureApproverForUserTag(route.params.projectId, props.userInfo.userId, props.tagKey, enteredTag.value)
                .then((res) => {
                  table.value.items.push(res);
                  enteredTag.value = '';
                  emit('conf-added', res);
                  nextTick(() => announcer.polite(`Added workload configuration successfully for ${enteredTag.value} ${props.tagLabel}.`));
                });
  //         }
  //       });
  }
};

const removeTagConf = (removedIem) => {
  table.value.items = table.value.items.map((i) => ({ ...i, deleteInProgress: i.id === removedIem.id }));
  SelfReportService.removeApproverConfig(route.params.projectId, removedIem.id)
      .then(() => {
        table.value.items = table.value.items.filter((i) => i.id !== removedIem.id);
        emit('conf-removed', removedIem);
        nextTick(() => announcer.polite(`Removed workload configuration successfully for ${removedIem.userTagValue} ${props.tagLabel}.`));
      });
};
</script>

<template>
<Card>
  <template #content>
    Test
  </template>
</Card>
</template>

<style scoped>

</style>