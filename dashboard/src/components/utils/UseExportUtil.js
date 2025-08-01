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
import axios from 'axios'

export const useExportUtil = () => {
  const ajaxDownload = (url, params) => {
    return axios.get(url, { params, responseType: 'blob' }).then((response) => {
      // response.data
      // create file link in browser's memory
      const href = URL.createObjectURL(response.data);

      // create "a" HTML element with href to file & click
      const link = document.createElement('a');
      link.href = href;
      let filename = "";
      const disposition = response.headers['content-disposition']
      if (disposition && disposition.indexOf('attachment') !== -1) {
        const filenameRegex = /fileName[^;=\n]*=((['"]).*?\2|[^;\n]*)/i;
        const matches = filenameRegex.exec(disposition);
        if (matches != null && matches[1]) {
          filename = matches[1].replace(/['"]/g, '');
        }
      }
      link.setAttribute('download', filename); //or any other extension
      link.click();

      // remove ObjectURL
      URL.revokeObjectURL(href);
    })
  }

  return {
    ajaxDownload
  }
}