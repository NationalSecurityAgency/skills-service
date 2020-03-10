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
export const SECTION = {
  GLOBAL: 'global',
  PROJECTS: 'projects',
  BADGES: 'badges',
  SKILLS: 'skills',
  SUBJECTS: 'subjects',
  DEPENDENCIES: 'dependencies',
  USERS: 'users',
};

export class SectionParams {
  constructor(builder) {
    this.section = builder.section;
    this.projectId = builder.projectId;
    this.sectionIdParam = builder.sectionIdParam;
    this.chartBuilderId = builder.chartBuilderId;
    this.numMonths = builder.numMonths || 6;
    this.numDays = builder.numDays || 120;
    this.loadDataForFirst = builder.loadDataForFirst || 3;
  }

  static get Builder() {
    class Builder {
      constructor(section, projectId) {
        this.validateSection(section);
        this.section = section;
        this.projectId = projectId;
      }

      withSectionIdParam(sectionIdParam) {
        this.sectionIdParam = sectionIdParam;
        return this;
      }

      withChartBuilderId(chartBuilderId) {
        this.chartBuilderId = chartBuilderId;
        return this;
      }

      withNumMonths(numMonths) {
        this.numMonths = numMonths;
        return this;
      }

      withNumDays(numDays) {
        this.numDays = numDays;
        return this;
      }

      withLoadDataForFirst(loadDataForFirst) {
        this.loadDataForFirst = loadDataForFirst;
        return this;
      }

      validateSection = (section) => {
        switch (section) {
          case SECTION.PROJECTS:
            break;
          case SECTION.BADGES:
            break;
          case SECTION.SKILLS:
            break;
          case SECTION.SUBJECTS:
            break;
          case SECTION.DEPENDENCIES:
            break;
          case SECTION.USERS:
            break;
          case SECTION.GLOBAL:
            break;
          default:
            throw new Error(`Invalid Section [${section}]`);
        }
      }

      build() {
        return new SectionParams(this);
      }
    }
    return Builder;
  }
}
