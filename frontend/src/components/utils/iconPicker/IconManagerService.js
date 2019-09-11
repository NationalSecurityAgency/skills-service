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
    let url = `/app/projects/${projectId}/customIcons`;
    if (!projectId) {
      url = '/supervisor/icons/customIcons';
    }
    return axios.get(url).then(response => response.data);
  },
  deleteIcon(iconName, projectId) {
    let url = `/admin/projects/${projectId}/icons/${iconName}`;
    if (!projectId) {
      url = `/supervisor/icons/${iconName}`;
    }
    return axios.delete(url);
  },
  addCustomIconCSS(css) {
    const existingStyles = createCustomIconStyleElementIfNotExist();
    existingStyles.innerText += css;
  },
  refreshCustomIconCss(projectId, isSupervisor) {
    const existingStyles = createCustomIconStyleElementIfNotExist();

    CustomIconService.getCustomIconCss(projectId, isSupervisor)
      .then((response) => {
        if (response) {
          existingStyles.innerText = response;
        }
    });
  },
};
