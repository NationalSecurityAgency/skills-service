import { useConfirm } from 'primevue/useconfirm'
import { ref } from 'vue'


export const useDialogMessages = () => {
  const confirm = useConfirm()
  const isConfirmVisible = ref(false);

  const msgOk = (message, header = 'Message!', okButtonTitle = 'Ok') => {
    confirm.require({
      message,
      header,
      onShow: () => {
        isConfirmVisible.value = true;
      },
      onHide: () => {
        isConfirmVisible.value = false;
      },
      rejectClass: 'hidden',
      acceptLabel: okButtonTitle,
    });
  }

  const msgConfirm = (message, header = 'Message!', accept = null) => {
    confirm.require({
      message,
      header,
      accept,
    });
  }


  return {
    msgOk,
    msgConfirm
  }
}