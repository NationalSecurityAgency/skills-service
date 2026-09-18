--
-- Copyright 2026 SkillTree
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- This function/trigger combination validates that attachment.skill_id was
-- correctly updated before skill_definition.skill_id is changed.
-- It works around PostgreSQL’s inability to cascade updates of the composite
-- foreign key (attachments.fk_attachments_proj_id_skill_id) when one of the
-- key columns contains NULL.
CREATE OR REPLACE FUNCTION f_check_skillId_was_updated_in_attachments()
    RETURNS trigger AS $$
BEGIN
    -- Only enforce when project_id is NULL
    IF NEW.project_id IS NULL AND OLD.skill_id <> NEW.skill_id THEN
        PERFORM 1
        FROM   attachments attachment
        WHERE  attachment.skill_id = OLD.skill_id and attachment.project_id is null;

        IF FOUND THEN
            RAISE EXCEPTION
                'Violation - cannot update skill_id[% -> %] because attachments.skill_id was not updated first: skill.id=[%], skill.type=[%], attachment.uuid=[%]',
                OLD.skill_id, NEW.skill_id, OLD.id, OLD.type,
                (SELECT uuid FROM attachments
                 WHERE skill_id = OLD.skill_id AND project_id IS NULL LIMIT 1);
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_attachments_skillId_validation
    BEFORE UPDATE OF skill_id ON skill_definition
    FOR EACH ROW EXECUTE FUNCTION f_check_skillId_was_updated_in_attachments();
