/**
 * Copyright 2021 SkillTree
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.storage.repos;

import org.apache.commons.lang3.Validate;
import org.springframework.data.jpa.domain.Specification;
import skills.storage.model.UserAchievement;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

public class UserAchievementSpecifications {

    public static Specification<UserAchievement> achievedSkill(String skillId) {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
                criteriaBuilder.equal(root.get("skillId"), skillId);
    }

    public static Specification<UserAchievement> notAchievedSkill(String skillId) {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("skillId"), skillId);
    }

    public static Specification<UserAchievement> notAchieved(List<String> skilIds) {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
                root.get("skillId").in(skilIds).not();
    }

    public static Specification<UserAchievement> hasAchieved(List<String> skillIds) {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
                root.get("skillId").in(skillIds);
    }

    public static Specification<UserAchievement> hasProjectId(String projectId){
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
                criteriaBuilder.equal(root.get("projectId"), projectId);
    }

    public static Specification<UserAchievement> hasProjectLevel(final String projectId, final Integer level) {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            Predicate p = criteriaBuilder.equal(root.get("projectId"), projectId);
            Predicate l = criteriaBuilder.greaterThanOrEqualTo(root.get("level"), level);
            Predicate noSkillId = criteriaBuilder.isNull(root.get("skillId"));

            return criteriaBuilder.and(p, l, noSkillId);
        };
    }

    public static Specification<UserAchievement> hasSubjectLevel(String projectId, String subjectId, Integer level) {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            Predicate pId = criteriaBuilder.equal(root.get("projectId"), projectId);
            Predicate lev = criteriaBuilder.greaterThanOrEqualTo(root.get("level"), level);
            Predicate subject = criteriaBuilder.equal(root.get("skillId"), subjectId);

            return criteriaBuilder.and(pId, lev, subject);
        };
    }

    public static Specification<UserAchievement> groupByDistinctUser() {
        return (Root<UserAchievement> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
          query.groupBy(root.get("userId")).distinct(true);
    }
}
