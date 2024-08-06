/*
 * Copyright 2020 SkillTree
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
import axios from 'axios';

const levelLookup = new Map();
levelLookup.set('TRACE', 1);
levelLookup.set('DEBUG', 2);
levelLookup.set('INFO', 3);
levelLookup.set('WARN', 5);
levelLookup.set('ERROR', 8);
export default {
  log(logLevel, msg) {
    const levelInt = levelLookup.get(logLevel);
    if (!levelInt) {
      throw new Error(`Unknown log level [${logLevel}]`);
    }
    const postBody = {
      message: msg,
      level: {
        name: logLevel,
        value: levelInt,
      },
    };

    return axios.post('/public/log', postBody, { handleError: false });
  },
};
