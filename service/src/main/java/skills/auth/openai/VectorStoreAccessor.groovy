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
package skills.auth.openai

import groovy.util.logging.Slf4j
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
@Slf4j
class VectorStoreAccessor {

    @Autowired
    VectorStore vectorStore;

    @Autowired
    ChatModel chatModel;

    void addSkillDesc(String projectId, String skillId, String description) {
        Document document = new Document(description, Map.of("projectId", projectId, "skillId", skillId))

        vectorStore.add([document]);
    }

    void deleteSkillDesc(String projectId, String skillId) {
        Filter.Expression expression = buildFilterExpression(projectId, skillId)
        vectorStore.delete(expression)
    }

    private static Filter.Expression buildFilterExpression(String projectId, String skillId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = new FilterExpressionBuilder()
                .and(b.eq("projectId", projectId), b.eq("skillId", skillId))
                .build();
        return expression
    }

    String suggestNextSkills(String completedSkillId, String projectId, String model, double modelTemperature = 0.5) {
        // Get the completed skill's content
        List<Document> completedSkillDocs = this.vectorStore.similaritySearch(
                SearchRequest.builder().query("skillId:${completedSkillId} projectId:${projectId}").topK(1).build()
        )
        if (!completedSkillDocs) {
            log.warn("No completed skill found for skillId: ${completedSkillId} and projectId: ${projectId}")
            return []
        }

        Document completedSkill = completedSkillDocs[0]
        String completedContent = completedSkill.text

        // Search for related skills (excluding the completed one)
        List<Document> candidateSkills = this.vectorStore.similaritySearch(
                SearchRequest.builder().query(completedContent).topK(10).build()
        )

        log.info("Found {} candidate skills for skillId: {} and projectId: {}", candidateSkills.size(), completedSkillId, projectId)
        // Use RAG to generate personalized suggestions
        return generatePersonalizedSuggestions(completedSkill, candidateSkills, model, modelTemperature)
    }

    private String generatePersonalizedSuggestions(Document completedSkill, List<Document> candidateSkills, String model, double modelTemperature = 0.5) {
        if (!candidateSkills) {
            return []
        }

        // Build context for RAG
        String context = """
            COMPLETED SKILL:
            Skill ID: ${completedSkill.metadata.skillId}
            Content: ${completedSkill.text}
            
            CANDIDATE SKILLS:
            ${candidateSkills.collect { doc ->
            """Skill ID: ${doc.metadata.skillId}
Content: ${doc.getFormattedContent()}
Similarity Score: ${doc.score}"""
        }.join('\n---\n')}
        """

        // Generate suggestions using LLM
        String promptStr = context + """

            Based on the completed skill and candidate skills above, suggest the top 3 next skills for a personalized learning path.
            
Consider:
1. Logical progression and prerequisites
2. Topic similarity and skill building
3. Difficulty progression
4. Learning relevance
            
Return the result as a JSON array with objects containing "skillId" and "reason" fields. Format:
[
  {"skillId": "skill_id_1", "reason": "explanation why this skill is recommended"},
  {"skillId": "skill_id_2", "reason": "explanation why this skill is recommended"},
  {"skillId": "skill_id_3", "reason": "explanation why this skill is recommended"}
]

Reason should be an encouraging explanation to the trainee why this skill is recommended.
        """

        log.info("Prompt: {}", promptStr)
        List<Message> messages = [
                new UserMessage(promptStr)
        ]
        Prompt prompt = new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(modelTemperature)
                        .build()
        )
        ChatResponse chatResponse = chatModel.call(prompt)

        List<Generation> genList = chatResponse.getResults()
        if (!genList) {
            return ""
        }
        String res = (String) genList.get(0).getOutput().getText()

        return res
    }


}
