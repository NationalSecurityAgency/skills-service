/**
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
package skills.services.inception

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import skills.controller.request.model.BadgeRequest

@Component
@Slf4j
class InceptionBadges {

    static class BadgeInfo {
        BadgeRequest badgeRequest
        List<String> skillIds
    }

    List<BadgeInfo> getBadges() {
        return [
                new BadgeInfo(
                        badgeRequest: new BadgeRequest(
                                badgeId: "CommunitySuperHero",
                                name: "Community Super Hero",
                                iconClass: "fas fa-mask",
                                description: '''> **“Live as if you were to die tomorrow. Learn as if you were to live forever.”**
> *\\- Mahatma Gandhi*

**Community Super Hero** badge of honor is awarded to the advocates of the SkillTree platform - only to the individuals with the domain expertise in the SkillTree ecosystem who use their knowledge and the *big heart* **to educate and lead** their peers into the next generation of learning!
'''
                        ),
                        skillIds: ['VisitContactUsers',
                                   'ProjectAccessOptions',
                                   'ExporttoCatalog',
                                   'SuggestFeature',
                                   'SuggestToolIntegration',
                                   'SpreadtheWordorTeach',
                                   'ShareSkillTreeSuccessStory',
                                   'CreateBadge',
                                   'CreateProject',
                        ]
                ),
                new BadgeInfo(
                        badgeRequest: new BadgeRequest(
                                badgeId: "AdminStarter",
                                name: "Admin Starter",
                                iconClass: "fas fa-fish",
                                description: '''> **“Give a man a fish and you feed him for a day; teach a man to fish and you feed him for a lifetime.”**
> *\\- Maimonides*

Congrats on being SkillTree <strong>project administrator</strong>! Your powers include building gamified training profiles as you masterfully create SkillTree projects, subjects and skills!
'''
                        ),
                        skillIds: ['CreateProject',
                                   'CreateSubject',
                                   'VisitSkillOverview',
                        ]
                ),
                new BadgeInfo(
                        badgeRequest: new BadgeRequest(
                                badgeId: "MetricsNinja",
                                name: "Metrics Ninja",
                                iconClass: "fas fa-user-ninja",
                                description: '''> **“The whole purpose of education is to turn mirrors into windows”**
> *\\- Sydney J\\. Harris*

After jumping over tall barbed wired fences, flying into skyscrapers though ceiling-to-floor mirrors and of course defeating thousands of bad guys you took a break and mastered SkillTree metrics - which is the true self-discipline if you ask us!
'''
                        ),
                        skillIds: ['VisitSubjectMetrics',
                                   'VisitSkillStats',
                                   'VisitProjectStats',
                                   'VisitProjectUserAchievementMetrics',
                                   'VisitProjectSubjectMetrics',
                                   'VisitProjectSkillMetrics',
                        ]
                ),
                new BadgeInfo(
                        badgeRequest: new BadgeRequest(
                                badgeId: "CatalogUser",
                                name: "Catalog User",
                                iconClass: "fas fa-book-reader",
                                description: '''> Don't reinvent the wheel

Someone once told us that sharing is caring so then using SkillTree's Skills Catalog is caring, caring a lot!
''',
                                helpUrl: "/dashboard/user-guide/skills-catalog.html"
                        ),
                        skillIds: ['ExporttoCatalog',
                                   'ImportSkillfromCatalog',
                        ]
                ),
                new BadgeInfo(
                        badgeRequest: new BadgeRequest(
                                badgeId: "SkillsOrganizer",
                                name: "Skills Organizer",
                                iconClass: "fas fa-shapes",
                                description: '''> **Out of clutter, find simplicity. From discord, find harmony. In the middle of difficulty lies opportunity.**
>
> *\\- Albert Einstein*

Sometimes we just have to continue tweaking and molding until the end product is just <strong>perfect</strong>! Just *one* more edit.. and.. PERFECTION!
''',
                        ),
                        skillIds: ['CreateSkillDependencies',
                                   'CopySkill',
                                   'CreateSkillGroup',
                                   'ReuseSkill',
                                   'MoveSkill',
                        ]
                ),
                new BadgeInfo(
                        badgeRequest: new BadgeRequest(
                                badgeId: "Promoter",
                                name: "Promoter",
                                iconClass: "fas fa-ad",
                                description: '''> **“A man’s mind, stretched by new ideas, may never return to its original dimensions.“**
> *\\- Oliver Wendell Holmes Jr\\.*

SkillTree is forever in debt to you for your generous efforts!
''',
                        ),
                        skillIds: ['ShareSkillTreeSuccessStory',
                                   'SpreadtheWordorTeach',
                                   'SuggestToolIntegration',
                        ]
                ),
        ]
    }
}
