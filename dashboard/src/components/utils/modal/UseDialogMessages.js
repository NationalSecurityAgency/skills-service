/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useConfirm } from 'primevue/useconfirm'
import { ref } from 'vue'
import {useFocusState} from "@/stores/UseFocusState.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

export const useDialogMessages = () => {
  const confirm = useConfirm()
  const focusState = useFocusState()
  const isConfirmVisible = ref(false);
  const announcer = useSkillsAnnouncer()

  const msgOk = ({message, header = 'Message!', okButtonTitle = 'Ok'}) => {
    confirm.require({
      message,
      header,
      onShow: () => {
        isConfirmVisible.value = true;
        announcer.polite(message);
      },
      onHide: () => {
        isConfirmVisible.value = false;
        focusState.focusOnLastElement()
      },
      reject: () => {
        focusState.focusOnLastElement()
      },
      accept: () => {
        focusState.focusOnLastElement()
      },
      rejectClass: 'hidden!',
      acceptLabel: okButtonTitle,
    });
  }

  const msgConfirm = ({
                        message,
                        header = 'Message!',
                        accept = null,
                        reject = null,
                        acceptLabel = 'OK',
                        rejectLabel = 'Cancel',
                        targetElement = undefined,
                        onShowHandler = () => {},
                        onHideHandler = () => {},
                      }) => {
    confirm.require({
      target: targetElement,
      message,
      header,
      position: 'center',
      icon: 'fas fa-exclamation-triangle',
      acceptLabel: acceptLabel,
      acceptClass: 'p-button-success p-button-outlined',
      acceptIcon: 'far fa-check-circle',
      rejectLabel: rejectLabel,
      rejectClass: 'p-button-warn p-button-outlined',
      rejectIcon: 'far fa-times-circle',
      onShow: () => {
        announcer.polite(message);
        onShowHandler.call()
      },
      accept: () => {
        if(accept) {
          accept();
        }
        focusState.focusOnLastElement();
      },
      reject: () => {
        if(reject) {
          reject();
        }
        focusState.focusOnLastElement();
      },
      onHide: () => {
        focusState.focusOnLastElement()
        onHideHandler.call()
      },
      defaultFocus: 'accept',
    });
  }


  return {
    msgOk,
    msgConfirm
  }
}