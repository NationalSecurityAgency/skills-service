import * as emoji from 'node-emoji';
export const useCommonMarkdownOptions = () => {
  const markdownOptions = {
    linkAttributes: {
      target: '_blank',
        rel: 'noopener noreferrer',
    },
    customHTMLRenderer: {
      link(node, context) {
        const { origin, entering } = context;
        const result = origin();
        if (!entering) {
          const faIcon = node?.destination?.startsWith('/api/download/') ? 'fas fa-download' : 'fas fa-external-link-alt';
          return {
            type: 'html',
            content: ` <span class="${faIcon}" style="font-size: 0.8rem"></span></a>`,
          };
        }
        return result;
      },
      text(node, context) {
        const { origin, entering } = context;
        const result = origin();
        const onMissing = (name) => name;
        const emojified = emoji.emojify(result.content, onMissing);
        if (entering) {
          return {
            type: 'text',
            content: emojified,
          };
        }
        return result;
      },
    },
  }

  const getMenuItem = () => document.querySelector('.toastui-editor-popup-body [aria-role="menu"]')
  const getMenuPopup = () => document.querySelector('.toastui-editor-popup-body')
  const getMarkdownEditor = () => document.getElementById('toastuiEditor')
  const getHeaderButton = () => getMarkdownEditor()?.querySelector('.heading')
  const getMouseEvent = () => new MouseEvent('click', { view: window, bubbles: true, cancelable: true })

  return {
    markdownOptions,
    getMenuItem,
    getMenuPopup,
    getMarkdownEditor,
    getHeaderButton,
    getMouseEvent
  }
}