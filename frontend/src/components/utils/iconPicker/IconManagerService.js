import CustomIconService from '@/components/icons/CustomIconService';

import axios from 'axios';

const createCustomIconStyleElementIfNotExist = () => {
  const head = document.getElementsByTagName('head')[0];

  let existingStyles = document.getElementById('skill-custom-icons');
  if (!existingStyles) {
    existingStyles = document.createElement('style');
    existingStyles.id = 'skill-custom-icons';
    existingStyles.type = 'text/css';
    head.appendChild(existingStyles);
  }

  return existingStyles;
};

export default {
  getIconIndex(projectId) {
    return axios.get(`/app/projects/${projectId}/customIcons`).then(response => response.data);
  },
  deleteIcon(iconName, projectId) {
    return axios.delete(`/admin/projects/${projectId}/icons/${iconName}`);
  },
  addCustomIconCSS(css) {
    const existingStyles = createCustomIconStyleElementIfNotExist();
    existingStyles.innerText += css;
  },
  refreshCustomIconCss(projectId) {
    const existingStyles = createCustomIconStyleElementIfNotExist();

    CustomIconService.getCustomIconCss(projectId)
      .then((response) => {
        existingStyles.innerText = response;
    });
  },
};
