import { useStore } from 'vuex'

export const useAppCommonConfig = () => {
  const store = useStore()

  const allowedAttachmentFileTypes = store.getters.config.allowedAttachmentFileTypes
  const maxAttachmentSize = store.getters.config.maxAttachmentSize ? Number(store.getters.config.maxAttachmentSize) : 0;
  const attachmentWarningMessage = store.getters.config.attachmentWarningMessage
  const allowedAttachmentMimeTypes = store.getters.config.allowedAttachmentMimeTypes
  const docsHost = store.getters.config.docsHost
  return {
    allowedAttachmentFileTypes,
    maxAttachmentSize,
    attachmentWarningMessage,
    allowedAttachmentMimeTypes,
    docsHost
  }
}
