/**
 * Copyright 2021 SkillTree
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
package skills.storage.repos.nativeSql


import skills.storage.model.QueryUsersCriteria
import skills.storage.model.SubjectLevelCriteria

import javax.persistence.Query

class QueryUserCriteriaHelper {

    static String generateCountSql(QueryUsersCriteria queryUsersCriteria) {
        if (queryUsersCriteria.empty()) {
            return null
        }

        if (queryUsersCriteria.allProjectUsers) {
            return '''SELECT COUNT(DISTINCT user_id) FROM user_events WHERE project_id = :projectId '''
        }

        //handle special case for only notAchievedSkills
        if (isOnlyNotAcheived(queryUsersCriteria)) {
            return '''SELECT COUNT(DISTINCT ue.user_id) FROM user_events ue 
                        WHERE ue.project_id = :projectId 
                        AND ue.user_id NOT IN (SELECT DISTINCT nsk.user_id FROM user_achievement nsk WHERE nsk.skill_id IN (:notSkillIds))'''
        }

        String sql = 'SELECT COUNT(DISTINCT ua.user_id) FROM user_achievement ua '
        if (queryUsersCriteria.projectLevel) {
            sql +='''
               JOIN user_achievement plj ON ua.user_id = plj.user_id
               AND plj.project_id = :projectId 
               AND plj.level >= :projectLevel 
               AND plj.skill_id is null 
            '''
        }
        if (queryUsersCriteria.achievedSkillIds || queryUsersCriteria.badgeIds) {
            List<String> allIds = []
            nullSafeAddAll(allIds, queryUsersCriteria.achievedSkillIds)
            nullSafeAddAll(allIds, queryUsersCriteria.badgeIds)
            allIds.eachWithIndex { String skillId, int i ->
                final String tableAlias = "ua_${skillId}_${i}"
                sql += """
                JOIN user_achievement ${tableAlias} ON ${tableAlias}.user_id = ua.user_id
                AND ${tableAlias}.skill_id = '${skillId}'
                AND ${tableAlias}.project_id = :projectId
                """
            }
        }
        if (queryUsersCriteria.subjectLevels) {
            queryUsersCriteria.subjectLevels.eachWithIndex{ SubjectLevelCriteria entry, int i ->
                String joinAlias = "sl${i}"
                sql += """
                JOIN user_achievement ${joinAlias}  on ua.user_id = ${joinAlias}.user_id 
                AND ${joinAlias}.skill_id = '${entry.subjectId}' AND ${joinAlias}.level >= ${entry.level}
                """
            }
        }
        sql += " WHERE ua.project_id = :projectId "
        if (queryUsersCriteria.notAchievedSkillIds) {
            sql += 'AND ua.user_id NOT IN (SELECT DISTINCT nsk.user_id FROM user_achievement nsk WHERE nsk.skill_id IN (:notSkillIds))'
        }

        return sql
    }

    static String generateSelectUserIdsSql(QueryUsersCriteria queryUsersCriteria) {
        if (queryUsersCriteria.empty()) {
            return null
        }

        if (queryUsersCriteria.allProjectUsers) {
            return '''SELECT DISTINCT user_id FROM user_events WHERE project_id = :projectId '''
        }

        //handle special case for only notAchievedSkills
        if (isOnlyNotAcheived(queryUsersCriteria)) {
            return '''SELECT DISTINCT ue.user_id FROM user_events ue 
                        WHERE ue.project_id = :projectId 
                        AND ue.user_id NOT IN (SELECT DISTINCT nsk.user_id FROM user_achievement nsk WHERE nsk.skill_id IN (:notSkillIds))'''
        }

        String sql = 'SELECT DISTINCT ua.user_id FROM user_achievement ua '
        if (queryUsersCriteria.projectLevel) {
            sql +='''
           JOIN user_achievement plj ON ua.user_id = plj.user_id
           AND plj.project_id = :projectId 
           AND plj.level >= :projectLevel 
           AND plj.skill_id is null 
        '''
        }
        if (queryUsersCriteria.achievedSkillIds || queryUsersCriteria.badgeIds) {
            List<String> allIds = []
            nullSafeAddAll(allIds, queryUsersCriteria.achievedSkillIds)
            nullSafeAddAll(allIds, queryUsersCriteria.badgeIds)
            allIds.each { String skillId ->
                final String tableAlias = "ua_${skillId}"
                sql += """
                JOIN user_achievement ${tableAlias} ON ${tableAlias}.user_id = ua.user_id
                AND ${tableAlias}.skill_id = '${skillId}'
                AND ${tableAlias}.project_id = :projectId
                """
            }
        }
        if (queryUsersCriteria.subjectLevels) {
            queryUsersCriteria.subjectLevels.eachWithIndex{ SubjectLevelCriteria entry, int i ->
                String joinAlias = "sl${i}"
                sql += """
            JOIN user_achievement ${joinAlias}  on ua.user_id = ${joinAlias}.user_id 
            AND ${joinAlias}.skill_id = '${entry.subjectId}' AND ${joinAlias}.level >= ${entry.level}
            """
            }
        }
        sql += " WHERE ua.project_id = :projectId "
        if (queryUsersCriteria.notAchievedSkillIds) {
            sql += 'AND ua.user_id NOT IN (SELECT DISTINCT nsk.user_id FROM user_achievement nsk WHERE nsk.skill_id IN (:notSkillIds))'
        }

        return sql
    }

    static boolean isOnlyNotAcheived(QueryUsersCriteria queryUsersCriteriaRequest) {
        boolean anythingElsePopulated = false

        anythingElsePopulated |= queryUsersCriteriaRequest.projectLevel != null
        anythingElsePopulated |= !!queryUsersCriteriaRequest.achievedSkillIds
        anythingElsePopulated |= !!queryUsersCriteriaRequest.badgeIds
        anythingElsePopulated |= !!queryUsersCriteriaRequest.subjectLevels
        anythingElsePopulated |= !!queryUsersCriteriaRequest.allProjectUsers

        return !anythingElsePopulated && queryUsersCriteriaRequest.notAchievedSkillIds
    }

    static void setCountParams(Query query, QueryUsersCriteria queryUsersCriteria) {
        setParams(query, queryUsersCriteria)
    }

    static void setSelectUserIdParams(Query query, QueryUsersCriteria queryUsersCriteria) {
        setParams(query, queryUsersCriteria)
    }

    private static void setParams(Query query, QueryUsersCriteria queryUsersCriteria) {
        if (queryUsersCriteria.empty()) {
            return
        }
        if (queryUsersCriteria.allProjectUsers) {
            query.setParameter("projectId", queryUsersCriteria.projectId)
            return
        }

        query.setParameter("projectId", queryUsersCriteria.projectId)
        if (queryUsersCriteria.projectLevel) {
            query.setParameter("projectLevel", queryUsersCriteria.projectLevel)
        }

        if (queryUsersCriteria.notAchievedSkillIds) {
            query.setParameter("notSkillIds", queryUsersCriteria.notAchievedSkillIds)
        }
    }

    static void nullSafeAddAll(Collection addTo, Collection addFrom) {
        if (addFrom) {
            addTo?.addAll(addFrom)
        }
    }

}
