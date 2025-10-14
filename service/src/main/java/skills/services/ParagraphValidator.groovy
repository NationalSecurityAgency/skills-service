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
package skills.services

import org.apache.commons.lang3.StringUtils
import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.*
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.commonmark.renderer.text.TextContentRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class ParagraphValidator {

    static class InternalValidationRequest {
        String description
        String prefix // if provided description will be regenerated and prefix will be added to invalid paragraphs
        String projectId = null
        Boolean utilizeUserCommunityParagraphPatternByDefault = false
        String quizId = null

        CustomValidator.ValidationPattern validationPattern
        Pattern forceValidationPattern
    }

    static class InternalValidationResult {
        Boolean isValid
        String newDescription // if prefix is provided
        String validationMsg
        String validationFailedDetails
    }

    private final InternalValidationRequest request

    ParagraphValidator(InternalValidationRequest request) {
        this.request = request
        init()
    }

    private CustomValidator.ValidationPattern validationPattern
    private final AtomicBoolean isValid = new AtomicBoolean(true)
    private final StringBuilder validationFailedDetails = new StringBuilder()
    private MarkdownRenderer markdownRenderer
    private TextContentRenderer textContentRenderer
    private Node document
    private final Map<String, String> customReplacements = [:]

    InternalValidationResult validateMarkdown() {
        InternalValidationResult res = init()
        if (res) {
            return res
        }

        String description = request.description

        AbstractVisitor visitor = new AbstractVisitor() {

            @Override
            void visit(ThematicBreak thematicBreak) {
                 // ignore
            }

            @Override
            void visit(HtmlBlock htmlBlock) {
                String html = htmlBlock.getLiteral()
                Document doc = Jsoup.parse(html)

                AtomicBoolean prefixAdded = new AtomicBoolean(false)
                Closure<Boolean> documentValidationFailure = (Element el, String text) -> {
                    invalidate()
                    Integer lineNum = htmlBlock.sourceSpans ? htmlBlock.sourceSpans?.first()?.lineIndex : null
                    appendToValidationFailedDetails("Failed within an html element for text [${text.substring(0,Math.min(20,text.length()))}] ${lineNum != null ? "after line[${lineNum}]" : ""}")
                    if (shouldAddPrefixToElement(el)) {
                        el.prependText(request.prefix)
                        prefixAdded.set(true)
                    }
                }

                // Process paragraphs
                Elements paragraphs = doc.select("p")
                paragraphs.each { Element element ->
                    String text = element.text().trim()
                    if (text && !validationPattern.pattern.matcher(text).matches()) {
                        documentValidationFailure(element, text)
                    }
                }


                // process lists
                Elements lists = doc.select("ul, ol")
                lists.each { Element list ->
                    Elements items = list.select("li")
                    if (items) {
                        Element firstItem = items.first()
                        if (firstItem) {
                            String text = items.text().trim()
                            if (text && !validationPattern.pattern.matcher(text).matches()) {
                                boolean validatedViaPrevious = false
                                Element previousNonEmptyElement = list.previousElementSiblings().find({ it.text().trim().size() > 0 })
                                if (previousNonEmptyElement) {
                                    String prevElementText = previousNonEmptyElement.text()
                                    validatedViaPrevious = validationPattern.pattern.matcher(prevElementText).matches()
                                }

                                if (!validatedViaPrevious) {
                                    documentValidationFailure(firstItem, text)
                                }
                            }
                        }
                    }
                }

                if (prefixAdded.get()) {
                    htmlBlock.setLiteral(doc.body().html())
                }
            }

            @Override
            void visit(IndentedCodeBlock indentedCodeBlock) {
                blockHandlerValidator(indentedCodeBlock)
            }

            @Override
            void visit(BulletList bulletList) {
                handleList(bulletList)
            }

            @Override
            void visit(OrderedList bulletList) {
                handleList(bulletList)
            }

            void handleList(ListBlock bulletList) {
                Node previousNode = bulletList.previous

                boolean previousNodeValidates = previousNode
                        && linesToPreviousNode(bulletList) < 3
                        && validateParagraph(previousNode)

                if (!previousNodeValidates) {
                    ListItem firstBullet = (ListItem) bulletList.firstChild
                    Node firstBulletContent = firstBullet.firstChild

                    if (!validateParagraph(firstBulletContent)) {
                        if (firstBulletContent == null) {
                            appendToValidationFailedDetails("First bullet is empty")
                        }
                        invalidate()
                        if (shouldAddPrefix(firstBulletContent)) {
                            firstBulletContent.prependChild(new Text(request.prefix))
                        }
                    }
                }

                if (request.forceValidationPattern) {
                    ListItem currentBullet = (ListItem) bulletList.firstChild
                    while (currentBullet) {
                        Node firstBulletValue = currentBullet.firstChild
                        if (firstBulletValue) {
                            String toValidate = textContentRenderer.render(firstBulletValue)
                            toValidate = Jsoup.parse(toValidate).text()
                            boolean shouldForceValidation = request.forceValidationPattern.matcher(toValidate).matches()
                            if (shouldForceValidation && !validateParagraph(firstBulletValue)) {
                                invalidate()
                            }
                        }
                        currentBullet = (ListItem) currentBullet.next
                    }
                }
            }

            @Override
            void visit(Paragraph paragraph) {
                if (!validateParagraph(paragraph) && !(paragraph.firstChild instanceof Image)) {
                    invalidate()
                    if (shouldAddPrefix(paragraph)) {
                        paragraph.prependChild(new Text(request.prefix))
                    }
                }
                visitChildren(paragraph)
            }

            @Override
            void visit(Heading heading) {
                if (!validateParagraph(heading)) {
                    invalidate()
                    if (shouldAddPrefix(heading)) {
                        heading.prependChild(new Text(request.prefix))
                    }
                }
            }

            @Override
            void visit(CustomNode customNode) {
                if (customNode instanceof TableHead) {
                    TableHead tableHead = (TableHead) customNode
                    TableBlock tableBlock = (TableBlock) tableHead.parent
                    blockHandlerValidator(tableBlock)
                } else {
                    visitChildren(customNode)
                }
            }

            @Override
            void visit(FencedCodeBlock fencedCodeBlock) {
                blockHandlerValidator(fencedCodeBlock, 3)
            }

            @Override
            void visit(Image image) {
                blockHandlerValidator(image)
            }
        }
        document.accept(visitor)

        String newDescription = request.prefix ? markdownRenderer.render(document) : description
        customReplacements.each { key, value ->
            newDescription = newDescription.replace(key, value)
        }
        return new InternalValidationResult(
                isValid: isValid.get(),
                newDescription: newDescription,
                validationMsg: validationPattern.message,
                validationFailedDetails: validationFailedDetails?.toString()
        )

    }

    private void invalidate() {
        isValid.set(false)
    }
    private void appendToValidationFailedDetails(String msg) {
        if (!validationFailedDetails.toString().contains(msg)) {
            validationFailedDetails.append(msg)
            validationFailedDetails.append("\n")
        }
    }

    private InternalValidationResult init() {
        String description = request.description
        validationPattern = request.validationPattern
        if (!validationPattern.pattern || StringUtils.isBlank(description)) {
            return new InternalValidationResult(isValid: true)
        }

        List<Extension> extensions = List.of(TablesExtension.create());
        Parser parser = Parser.builder()
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .extensions(extensions)
                .build();
        String descriptionNormalized = normalizeInput(description)
        document = parser.parse(descriptionNormalized);
        markdownRenderer = MarkdownRenderer.builder().extensions(extensions).build();
        textContentRenderer = TextContentRenderer.builder().extensions(extensions).build()


        return null
    }

    private String removeLeadingSeparators(String text) {
        if (text == null || text.isEmpty()) {
            return text
        }
        return text.replaceFirst('^[\\s\\-*_=~`#]+', '')
    }

    private boolean isOnlySeparatorsOrWhitespace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true
        }

        // Check if the text contains only whitespace, newlines, or markdown separators
        return text.matches('^[\\s\\-*_=~`#]*$')
    }

    private boolean validateParagraph(Node node) {
        if (!node) {
            return false
        }
        String toValidate = textContentRenderer.render(node)
        toValidate = Jsoup.parse(toValidate).text()
        if (isOnlySeparatorsOrWhitespace(toValidate)) {
            return true
        }
        toValidate = removeLeadingSeparators(toValidate)

        boolean isValidParagraph = validationPattern.pattern.matcher(toValidate).matches()
        if (isValidParagraph) {
            if (request.forceValidationPattern) {
                String markdown = markdownRenderer.render(node)
                String[] lines = markdown.split('\n')
                for (String line : lines) {
                    String lineToValidate = Jsoup.parse(line).text()
                    boolean forceValidate = request.forceValidationPattern.matcher(lineToValidate).matches()
                    if (forceValidate) {
                        boolean isValidLine = validationPattern.pattern.matcher(lineToValidate).matches()
                        if (!isValidLine) {
                            Integer lineNum = node.sourceSpans ? node.sourceSpans?.first()?.lineIndex : null
                            String msg = "Via forced validation${lineNum != null ? ", after line[${lineNum}] " : " "}[${line?.substring(0, Math.min(20, line?.length()))}]\n"
                            appendToValidationFailedDetails("${msg}")
                            return false
                        }
                    }
                }
            }
        } else {
            appendValidationMsg(node)
        }
        return isValidParagraph
    }

    private void appendValidationMsg(Node node) {
        String asText = textContentRenderer.render(node)
        Integer lineNum = node.sourceSpans ? node.sourceSpans?.first()?.lineIndex : null
        String msg = "${lineNum != null ? "Line[${lineNum}] " : ""}[${asText?.substring(0, Math.min(20, asText?.length()))}]\n"
        appendToValidationFailedDetails("${msg}")
    }

    private static String normalizeInput(String input) {
        return input
                .replaceAll(/(?<=\n)\s*<br\s*\/?>\s*|\s*<br\s*\/?>\s*(?=\n)/, "\n")  // Replace <br> or <br/> when next to newlines
                .replaceAll(/\r\n?/, "\n")  // Normalize line endings
                .replaceAll(/(?<=\S)[ \t]*(?=\n)/, "")  // Remove spaces before newlines
                .replaceAll(/(?<=\n)[ \t]*(?=\n)/, "")  // Remove spaces between newlines
                .replaceAll("&gt;", ">")  // Replace greater than encoding so Block Quotes are handled properly by the parser

    }

    private boolean shouldAddPrefix(Node nodeToAddTo) {
        return request.prefix && !textContentRenderer.render(nodeToAddTo)?.startsWith(request.prefix)
    }

    private boolean shouldAddPrefixToElement(Element e) {
        return request.prefix && !e.text()?.startsWith(request.prefix)
    }

    private static Node lookForPreviousParagraph(Node node) {
        Node previousNode = node.previous

        if (!previousNode && node instanceof Image) {
            if (node.parent instanceof Paragraph && node.parent.firstChild == node) {
                previousNode = node.parent.previous
            }
        }
        while (previousNode && previousNode instanceof SoftLineBreak) {
            previousNode = previousNode.previous
        }
        while (previousNode && previousNode instanceof Text) {
            previousNode = previousNode.parent
        }
        return previousNode
    }

    private static Integer linesToPreviousNode(Node node) {
        Node previousNode = lookForPreviousParagraph(node)
        if (!previousNode || !node.sourceSpans) {
            return -1
        }
        if (!previousNode || !previousNode.sourceSpans) {
            return -1
        }
        int previousEndLine = previousNode.sourceSpans.last().lineIndex
        int currentStartLine = node.sourceSpans.first().lineIndex
        return currentStartLine - previousEndLine
    }

    private void blockHandlerValidator(Node node, int minLinesToPreviousNode = 2) {
        Node previousNode = lookForPreviousParagraph(node)

        boolean prevNodeIsATable = previousNode instanceof TableBlock && node instanceof TableBlock
        if (!previousNode || linesToPreviousNode(node) > minLinesToPreviousNode || prevNodeIsATable) {
            invalidate()

            if (shouldAddPrefix(node)) {
                Paragraph paragraph = new Paragraph()
                paragraph.prependChild(new Text(request.prefix))
                node.insertBefore(paragraph)
            }
        } else if (!validateParagraph(previousNode)) {
            invalidate()
            if (shouldAddPrefix(previousNode)) {
                if (previousNode instanceof HtmlBlock) {
                    String currentHtml = previousNode.getLiteral()
                    String newHtml = request.prefix + (currentHtml ?: "")
                    customReplacements.put(markdownRenderer.render(previousNode), newHtml)
                } else {
                    previousNode.prependChild(new Text(request.prefix))
                }
            }
        }
    }
}
