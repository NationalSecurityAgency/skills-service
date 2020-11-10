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
package skills.storage.repos

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.Query

@Service
@Slf4j
class UserAchievedCustomRepoImpl implements UserAchievedCustomRepo {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    List<UserAndLevel> findUsersWithMaxLevelForMultipleProjects(List<ProjectAndLevel> projectAndLevels, int firstResult, int maxResults, boolean userIdSortAsc) {
        assert projectAndLevels.size() > 1
        assert projectAndLevels.size() <= 5

        def projIdsWithIndex = projectAndLevels.withIndex()
        String jpql = createJPQL(projIdsWithIndex, userIdSortAsc)

        Query query = entityManager.createQuery(jpql.toString())
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        List<UserAndLevel> res = []
        query.getResultList().each {
            String userId = it[0]
            List<ProjectAndLevel> levels = projIdsWithIndex.collect { proj, index ->
                new ProjectAndLevel(projectId: proj.projectId, level: it[index + 1])
            }
            res.add(new UserAndLevel(userId: userId, levels: levels))
        }
        return res
    }

    @Override
    Integer countUsersWithMaxLevelForMultipleProjects(List<ProjectAndLevel> projectAndLevels) {
        assert projectAndLevels.size() > 1
        assert projectAndLevels.size() <= 5

        def projIdsWithIndex = projectAndLevels.withIndex()
        String jpql = createJPQL(projIdsWithIndex, false, true)
        Query query = entityManager.createQuery(jpql.toString())
        return query.getSingleResult()
    }

    private String createJPQL(List<Tuple2<ProjectAndLevel, Integer>> projIdsWithIndex, boolean userIdSortAsc, boolean isCount = false) {
        String jpql =
                "select " +
                        (isCount ?
                                "count(distinct ua0.userId) as count\n"
                                :
                                "  ua0.userId as user, ${projIdsWithIndex.collect({ proj, index -> "max(ua${index}.level) as ${proj.projectId}Level" }).join(", ")} \n"
                        ) +
                        "from  ${projIdsWithIndex.collect({ proj, index -> "UserAchievement as ua${index}" }).join(", ")} \n" +
                        "where \n" +
                        "${projIdsWithIndex.collect({ proj, index -> "ua${index}.projectId = '${proj.projectId}' and\nua${index}.skillId is null and\nua${index}.level >= ${proj.level} and\n" }).join("")}" +
                        "${(1..(projIdsWithIndex.size() - 1)).collect({ index -> "ua0.userId = ua${index}.userId" }).join(" and\n")}\n" +
                        (isCount ? "" :
                                (
                                    "group by ua0.userId\n" +
                                    "order by ua0.userId " + (userIdSortAsc ? "asc" : "desc")
                                )
                        );
        log.debug("findUsersWithMaxLevelForMultipleProjects jpql=[{}]", jpql)
        println jpql
        return jpql
    }
}
