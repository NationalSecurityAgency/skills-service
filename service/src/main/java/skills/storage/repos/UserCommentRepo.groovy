/**
 * Copyright 2025 SkillTree
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

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import skills.storage.model.UserComment

interface UserCommentRepo extends JpaRepository<UserComment, Long> {

    static interface SimpleUserComment {
        Integer getUserCommentThreadId()
        String getUserIdForDisplay()
        String getComment()
        Date getCreated()
        String getSkillId()
        String getSkillName()
        Boolean getNeedsResponse()
    }

    @Query('''SELECT comment.userCommentThreadId as userCommentThreadId, userAttrs.userIdForDisplay as userIdForDisplay, comment.comment as comment, comment.created as created
        from  UserComment  comment, UserAttrs userAttrs 
        where
           userAttrs.userId = comment.fromUserId
           and (comment.fromUserId = :userId or comment.toUserId = :userId)
           and comment.userCommentThreadId in (:threadIds)
            ''')
    List<SimpleUserComment> findAllByUserIdAndThreadIdIn(String userId, List<Integer> threadIds)

    @Query('''SELECT 
            comment.userCommentThreadId as userCommentThreadId, 
            userAttrs.userIdForDisplay as userIdForDisplay, 
            comment.comment as comment, 
            comment.created as created,
            commentThread.skillId as skillId,
            skill.name as skillName,
            commentThread.needsResponse as needsResponse
        from  UserCommentThread commentThread, UserComment  comment, UserAttrs userAttrs, SkillDef skill
        where
           userAttrs.userId = comment.fromUserId
           and commentThread.id = comment.userCommentThreadId
           and commentThread.projectId = skill.projectId and commentThread.skillId = skill.skillId
           and comment.userCommentThreadId in (:threadIds)
            ''')
    List<SimpleUserComment> findAllByThreadIdIn(List<Integer> threadIds)

}
