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

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.services.attributes.SlidesAttrs
import skills.storage.model.SkillAttributesDef

interface SkillAttributesDefRepo extends CrudRepository<SkillAttributesDef, Long> {

    @Nullable
    List<SkillAttributesDef> findAllByType(SkillAttributesDef.SkillAttributesType type)

    @Nullable
    List<SkillAttributesDef> findAllBySkillRefId(Integer skillRefId)

    @Nullable
    SkillAttributesDef findBySkillRefIdAndType(Integer skillRefId, SkillAttributesDef.SkillAttributesType type)

    int deleteBySkillRefIdAndType(Integer skillRefId, SkillAttributesDef.SkillAttributesType type)

    static interface VideoSummaryAttributes {
        String getUrl()
        @Nullable
        String getType()
        Boolean getHasCaptions()
        Boolean getHasTranscript()
        @Nullable
        Double getWidth()
        @Nullable
        Double getHeight()
    }

    static interface SlidesSummaryAttributes {
        String getUrl()
        String getType()
        Double getWidth()
    }

    @Nullable
    @Query(value = '''select attributes ->> 'videoUrl' as url,
           attributes ->> 'videoType' as type,
           case when attributes ->> 'captions' is not null then true else false end   as hasCaptions,
           case when attributes ->> 'transcript' is not null then true else false end as hasTranscript,
           attributes ->> 'height' as height, attributes ->> 'width' as width
        from skill_attributes_definition
        where type= 'Video' and skill_ref_id = ?1''', nativeQuery = true)
    VideoSummaryAttributes getVideoSummary(Integer skillRefId)


    @Nullable
    @Query(value = '''select attributes ->> 'url' as url,
           attributes ->> 'type' as type,
           attributes ->> 'width' as width
        from skill_attributes_definition
        where type= 'Slides' and skill_ref_id = ?1''', nativeQuery = true)
    SlidesSummaryAttributes `(Integer skillRefId)

    @Nullable
    @Query(value = '''select attributes ->> 'videoUrl' as url
        from skill_attributes_definition
        where type= 'Video' and skill_ref_id = ?1''', nativeQuery = true)
    String getVideoUrlBySkillRefId(Integer skillRefId)

    @Nullable
    @Query(value = '''select sa.attributes ->> 'captions' as captions
        from skill_attributes_definition sa, skill_definition sd
        where sa.type= 'Video'
            and (case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end) = sa.skill_ref_id  
            and sd.project_id = ?1
            and sd.skill_id = ?2
    ''', nativeQuery = true)
    String getVideoCaptionsByProjectAndSkillId(String projectId, String skillId)

    @Nullable
    @Query(value = '''select sa.attributes ->> 'transcript' as captions
        from skill_attributes_definition sa, skill_definition sd
        where sa.type= 'Video'
            and (case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end) = sa.skill_ref_id  
            and sd.project_id = ?1
            and sd.skill_id = ?2
    ''', nativeQuery = true)
    String getVideoTranscriptsByProjectAndSkillId(String projectId, String skillId)


    @Nullable
    @Query(value = '''select sa.*
        from skill_attributes_definition sa, skill_definition sd
        where (case when sd.copied_from_skill_ref is not null then sd.copied_from_skill_ref else sd.id end) = sa.skill_ref_id 
            and sd.project_id = ?1
            and sd.skill_id = ?2
            and sa.type= ?3    
    ''', nativeQuery = true)
    SkillAttributesDef findByProjectIdAndSkillIdAndType(String projectId, String skillId, String type)
}
