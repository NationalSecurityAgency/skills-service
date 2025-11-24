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
import org.commonmark.ext.gfm.tables.TableCell
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
    private final Set<Node> validNodes = new HashSet<>()
    private final Set<Node> invalidNodes = new HashSet<>()

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
            void visit(Link link) {
                blockHandlerValidator(link)
            }

            @Override
            void visit(BlockQuote blockQuote) {
                boolean isValid = blockHandlerValidator(blockQuote, 2, false);
                if (!isValid) {
                    visitChildren(blockQuote);
                }
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

                Closure<Boolean> checkIfPreviousParagraphCloseAndValid = {
                    Node previous = htmlBlock.previous
                    if (previous && previous instanceof Paragraph) {
                        int prevLine = previous.sourceSpans.first().lineIndex
                        int currentLine = previous.sourceSpans.first().lineIndex
                        if ( currentLine - prevLine <= 1) {
                            if (validateNode (previous).isValid) {
                                return true
                            }
                        }
                    }
                    return false
                }
                Closure<Boolean> shouldForceValidation = { String text ->
                    return request.forceValidationPattern && request.forceValidationPattern?.matcher(text)?.matches()
                }
                Closure<Boolean> doValidate = { String text ->
                    return validationPattern.pattern.matcher(text).matches()
                }
                Closure<Boolean> isPrevElementValid = { Element list ->
                    boolean validatedViaPrevious = false
                    if (list) {
                        Element previousNonEmptyElement = list.previousElementSiblings().find({ it.text().trim().size() > 0 })
                        if (previousNonEmptyElement) {
                            String prevElementText = previousNonEmptyElement.text()
                            validatedViaPrevious = validationPattern.pattern.matcher(prevElementText).matches()
                            if (!validatedViaPrevious) {
                                validatedViaPrevious = true
                            }
                        }
                    }

                    return validatedViaPrevious
                }
                boolean isPrevParagraphValid = checkIfPreviousParagraphCloseAndValid()

                // Process paragraphs
                Elements paragraphs = doc.select("p")
                pLoop: for (int i = 0; i < paragraphs.size(); i++) {
                    Element element = paragraphs.get(i);
                    String text = element.text().trim()
                    if (text && (!isPrevParagraphValid || shouldForceValidation(text))) {
                        if (!doValidate(text)) {
                            documentValidationFailure(element, text)
                        }
                    }
                }

                // process lists
                Elements lists = doc.select("ul, ol")
                listLoop:
                for (int i = 0; i < lists.size(); i++) {
                    Element list = lists.get(i)
                    Elements items = list.select("li")
                    if (items) {
                        Element firstItem = items.first()
                        if (firstItem) {
                            String text = items.text().trim()
                            if (text && !isPrevParagraphValid && !isPrevElementValid(list)) {
                                if (!doValidate(text)) {
                                    documentValidationFailure(firstItem, text)
                                }
                            }
                        }

                        for (Element litItem : items) {
                            String text = litItem.text().trim()
                            if (text && shouldForceValidation(text)) {
                                if (!doValidate(text)) {
                                    documentValidationFailure(litItem, text)
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
                if (!indentedCodeBlock.firstChild && indentedCodeBlock.literal) {
                    ValidateNodeRes validateNodeRes = validateNode (indentedCodeBlock)
                    if (validateNodeRes.isValid) {
                        if (request.forceValidationPattern) {
                            String textToCheck = textContentRenderer.render(indentedCodeBlock)
                            List<String> modifiedLInes = []
                            boolean failedForceValidation = false
                            textToCheck?.split("\n")?.each { String line ->
                                String toCheck = line?.trim()
                                boolean shouldForceValidation = request.forceValidationPattern.matcher(toCheck).matches()
                                if (shouldForceValidation) {
                                    StringValidationRes validationRes = validateString(line)
                                    if (!validationRes.isValid) {
                                        invalidate()
                                        // insert prefix before the first non space character
                                        int firstNonSpace = line.findIndexOf { it != ' ' && it != '\t' }
                                        if (firstNonSpace >= 0) {
                                            String newLine = line.substring(0, firstNonSpace) + request.prefix + line.substring(firstNonSpace)
                                            modifiedLInes.add(newLine)
                                        }
                                        failedForceValidation = true
                                    } else {
                                        modifiedLInes.add(line)
                                    }
                                } else {
                                    modifiedLInes.add(line)
                                }
                            }
                            if (failedForceValidation) {
                                indentedCodeBlock.setLiteral(modifiedLInes.join("\n"))
                            }
                        }

                        return
                    }
                }
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
                boolean isValidList = true
                Node previousNode = bulletList.previous

                boolean previousNodeValidates = false
                if (previousNode) {
                    Integer linesToPrevNode = linesToPreviousNode(bulletList)
                    boolean prevNodeCloseEnough = linesToPrevNode > -1 && linesToPrevNode < 3

                    boolean isPreviousNodeListBlock = previousNode instanceof ListBlock
                    boolean amIPartOfAnotherList = prevNodeCloseEnough && isPreviousNodeListBlock
                    boolean isPrevNodeAlreadyValidated = validNodes.contains(previousNode)

                    if (amIPartOfAnotherList && isPrevNodeAlreadyValidated) {
                        previousNodeValidates = true
                    } else if (prevNodeCloseEnough && !isPreviousNodeListBlock && validateNode(previousNode).isValid) {
                        previousNodeValidates = true
                    }
                }

                if (!previousNodeValidates) {
                    ListItem firstBullet = (ListItem) bulletList.firstChild
                    Node firstBulletContent = firstBullet.firstChild

                    ValidateNodeRes validationRes = validateNode(firstBulletContent)
                    if (!validationRes.isValid) {
                        if (firstBulletContent == null) {
                            appendToValidationFailedDetails("First bullet is empty")
                        }
                        invalidate()
                        isValidList = false
                        addPrefixIfNeeded(validationRes)
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
                            if (shouldForceValidation && !validateNode(firstBulletValue).isValid) {
                                invalidate()
                                addPrefixIfNeeded(firstBulletValue)
                                isValidList = false
                            }
                        }
                        currentBullet = (ListItem) currentBullet.next
                    }
                }

                if (isValidList) {
                    validNodes.add(bulletList)
                }
            }

            @Override
            void visit(Paragraph paragraph) {
                boolean isImage = paragraph.firstChild instanceof Image || (paragraph.firstChild instanceof StrongEmphasis && paragraph.firstChild?.firstChild instanceof Image)
                boolean isLink = paragraph.firstChild instanceof Link && paragraph.firstChild == paragraph.lastChild
                if (!isImage && !isLink) {
                    ValidateNodeRes validateRes = validateNode(paragraph)
                    if (!validateRes.isValid) {
                        invalidate()
                        addPrefixIfNeeded(validateRes)
                    }
                }
                visitChildren(paragraph)
            }

            @Override
            void visit(Heading heading) {
                ValidateNodeRes validateRes = validateNode(heading)
                if (!validateRes.isValid) {
                    invalidate()
                    addPrefixIfNeeded(validateRes)
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
                // in case there is more than 1 image in a row, find the very first one
                Node previousNode = image.previous
                Node lastLocatedImage = null
                while (previousNode && (previousNode instanceof SoftLineBreak || previousNode instanceof Image)) {
                    if (previousNode instanceof Image) {
                        lastLocatedImage = previousNode
                    }
                    previousNode = previousNode.previous
                }
                if (lastLocatedImage && lastLocatedImage instanceof Image) {
                    blockHandlerValidator(lastLocatedImage)
                } else {
                    boolean isWithinTable = image.parent instanceof TableCell
                    if (!isWithinTable) {
                        blockHandlerValidator(image)
                    }
                }
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

    private static  String removeLeadingSeparators(String text) {
        if (text == null || text.isEmpty()) {
            return text
        }
        return text.replaceFirst('^[\\[\\s\\-*_=~`#]+', '')
    }

    private static  boolean isOnlySeparatorsOrWhitespace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true
        }

        // Check if the text contains only whitespace, newlines, or markdown separators
        return text.matches('^[\\s\\-*_=~`#]*$')
    }

    private static boolean isOnlySpecialChars(String str) {
        return str && str.trim().matches(/^[^a-zA-Z0-9]+$/)
    }
    private static String removeNumberingPrefix(String text) {
        if (text == null || text.isEmpty()) {
            return text
        }
        return text.replaceFirst('^\\s*\\d+\\.\\s*', '')
    }
    private static String stripStyleMarkerPairs(String input) {
        if (!input) {
            return input
        }

        String res = input

        if (res.contains("~~")) {
            // Handle double tildes (strikethrough)
            res = res.replaceAll(/(?<!\~)\~\~([^~]+)\~\~(?!\~)/, '$1')
        }
        if (res.contains("**")) {
            // Handle double asterisks (bold)
            res = res.replaceAll(/(?<!\*)\*\*([^*]+)\*\*(?!\*)/, '$1')
        }
        if (res.contains("*")) {
            // Handle single asterisks (italic)
            res = res.replaceAll(/(?<!\*)\*([^*]+)\*(?!\*)/, '$1')
        }
        return res
    }

    private String allNodesOnSameLineToString(Node node) {
        List<Node> prevNodesOnSameLine = [node]
        if (node.sourceSpans) {
            Integer nodeCurrentLine = node.sourceSpans?.first()?.lineIndex
            Node currentNode = node.previous
            while (isNodeOnThisLine(currentNode, nodeCurrentLine)) {
                prevNodesOnSameLine.add(currentNode)
                currentNode = currentNode.previous
            }
        }

        StringBuilder res = new StringBuilder()
        prevNodesOnSameLine.reverse().each { Node resNode ->
            res.append(textContentRenderer.render(resNode))
        }
        return res.toString()
    }

    private String nodeToText(Node node) {
        String res = textContentRenderer.render(node)
        return Jsoup.parse(res).text()
    }

    static class ValidateNodeRes {
        Node closestNode = null
        boolean isValid = false
        boolean alreadyHandledInvalidationProcess = false

        static final ValidateNodeRes VALID = new ValidateNodeRes(isValid: true)
    }

    static int maxIteration = 900000
    private void checkChildForForceValidation(Node node, int iteration) {
        if (iteration > maxIteration) {
            throw new IllegalArgumentException("Too many iterations")
        }
        if (node?.firstChild) {
            checkSiblingsForForceValidation(node.firstChild, iteration + 1)
        }
    }
    private void checkSiblingsForForceValidation(Node node, int iteration) {
        if (iteration > maxIteration) {
            throw new IllegalArgumentException("Too many iterations")
        }
        int originalNodeLine = -1
        int lastValidatedLineNum = -1
        if (node?.sourceSpans) {
            originalNodeLine = node.sourceSpans.first().lineIndex
        }

        Closure<Boolean> isTextOnAnotherLine = { Node nodeToCheck ->
            if (!(nodeToCheck instanceof Text)) {
                return false
            }
            if (nodeToCheck.sourceSpans) {
                int currentLine = nodeToCheck.sourceSpans.first().lineIndex
                if (originalNodeLine == currentLine || lastValidatedLineNum == currentLine) {
                    return false
                }
            }
            return true
        }

        Node currentNode = node.next

        siblingsCheckLoop: while (currentNode) {
            if (isTextOnAnotherLine(currentNode)) {
                Text textNode = (Text)currentNode
                String textToCheck = textNode.literal?.trim()
                if (textToCheck) {
                    boolean forceValidate = request.forceValidationPattern.matcher(textToCheck).matches()
                    if (forceValidate) {
                        boolean isValidLine = validationPattern.pattern.matcher(textToCheck).matches()
                        if (!isValidLine) {
                            invalidate()
                            if (request.prefix) {
                                textNode.setLiteral("${request.prefix}${textNode.literal}".toString())
                            }
                            Integer lineNum = textNode.sourceSpans ? textNode.sourceSpans?.first()?.lineIndex : null
                            String msg = "Via forced validation${lineNum != null ? ", after line[${lineNum}] " : " "}[${textNode.literal?.substring(0, Math.min(20, textNode.literal?.length()))}]\n"
                            appendToValidationFailedDetails("${msg}")
                        } else {
                            if (textNode?.sourceSpans) {
                                lastValidatedLineNum = textNode?.sourceSpans?.first()?.lineIndex
                            }
                        }
                    }
                }
            }
            checkChildForForceValidation(currentNode, iteration + 1)
            currentNode = currentNode.next
        }
    }

    private ValidateNodeRes validateNode(Node node) {
        if (!node) {
            invalidNodes.add(node)
            return new ValidateNodeRes(isValid: false, closestNode: node)
        }
        String toValidate = allNodesOnSameLineToString(node)
        StringValidationRes stringValidationRes = validateString(toValidate)
        if (stringValidationRes.isValid && !stringValidationRes.shouldContinueValidation) {
            return ValidateNodeRes.VALID
        }
        boolean isValidParagraph = stringValidationRes.isValid
        if (isValidParagraph) {
            if (request.forceValidationPattern) {
                checkChildForForceValidation(node, 0)
            }
        } else {
            appendValidationMsg(node)
        }
        if (!isValidParagraph) {
            invalidNodes.add(node)
        } else {
            validNodes.add(node)
        }
        return new ValidateNodeRes(isValid: isValidParagraph, closestNode: node)
    }

    static class StringValidationRes {
        boolean isValid = false
        boolean shouldContinueValidation = true
    }
    private StringValidationRes validateString(String toValidate) {
        toValidate = Jsoup.parse(toValidate).text()
        if (isOnlySeparatorsOrWhitespace(toValidate)) {
            return new StringValidationRes(isValid: true, shouldContinueValidation: false)
        }
        toValidate = stripStyleMarkerPairs(toValidate)
        toValidate = removeLeadingSeparators(toValidate)
        if (isOnlySpecialChars(toValidate)) {
            return new StringValidationRes(isValid: true, shouldContinueValidation: false)
        }
        toValidate = removeNumberingPrefix(toValidate)
        boolean isValid = validationPattern.pattern.matcher(toValidate).matches()
        new StringValidationRes(isValid: isValid, shouldContinueValidation: true)
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
                .replaceAll("\\*\\*\n\\*\\*", "\n")
                .replaceAll("\\*\n\\*", "\n")
                .replaceAll("~~\n~~", "\n")
    }

    private boolean shouldAddPrefix(Node nodeToAddTo) {
        return request.prefix && !textContentRenderer.render(nodeToAddTo)?.startsWith(request.prefix)
    }

    private boolean addPrefixIfNeeded(ValidateNodeRes validateNodeRes) {
        if (validateNodeRes.alreadyHandledInvalidationProcess) {
            return false
        }
        return addPrefixIfNeeded ((Node)validateNodeRes.closestNode)
    }

    private boolean addPrefixIfNeeded(Node nodeToAddTo) {
        boolean shouldAdd = shouldAddPrefix(nodeToAddTo)
        if (shouldAdd) {
            if (nodeToAddTo instanceof Text) {
                Text text = (Text)nodeToAddTo
                text.setLiteral("${request.prefix}${text.literal}".toString())
            } else {
                nodeToAddTo.prependChild(new Text(request.prefix))
            }
        }
        return shouldAdd
    }

    private boolean shouldAddPrefixToElement(Element e) {
        return request.prefix && !e.text()?.startsWith(request.prefix)
    }

    private static Node getPreviousForImageOrLink(Node currentNode, Node previousNode) {
        Node res = previousNode
        Node parent = currentNode.parent
        if ((parent instanceof Paragraph || parent instanceof Link) && parent.firstChild == currentNode) {
            res = parent.previous
        } else if (parent instanceof StrongEmphasis) {
            if (parent?.parent instanceof Paragraph && parent?.parent?.firstChild instanceof Text) {
                res = parent.parent
            } else if (parent?.parent?.previous instanceof Paragraph &&
                    (parent?.parent?.previous?.firstChild instanceof Text || parent?.parent?.previous?.firstChild instanceof HtmlInline)) {
                res = parent.parent.previous
            }
        }
        return res
    }

    private static boolean isSoftLine(Node node) {
        return node && node instanceof SoftLineBreak
    }

    private static boolean isNodeOnThisLine(Node node, Integer lineNum) {
        return lineNum != null && node?.sourceSpans && node.sourceSpans.first().lineIndex == lineNum
    }

    private static Node lookForPreviousParagraph(Node node) {
        Node previousNode = node.previous

        if (!previousNode && (node instanceof Image || node instanceof Link)) {
            previousNode = getPreviousForImageOrLink(node, previousNode)
        }

        // find first non-softline node
        while (previousNode && isSoftLine(previousNode)) {
            previousNode = previousNode.previous
        }

        // find first node on the same line
        Integer lineToStayOn = previousNode?.sourceSpans ? previousNode.sourceSpans.first().lineIndex : null
        Node currentNode = previousNode?.previous
        while (currentNode && (isSoftLine(currentNode) || isNodeOnThisLine(currentNode, lineToStayOn))) {
            previousNode = currentNode
            currentNode = currentNode.previous
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

    private static List<Class<? extends Node>> chainableNodeClasses =
            [TableBlock.class, Image.class, BulletList.class, IndentedCodeBlock.class, FencedCodeBlock.class]
    private boolean blockHandlerValidator(Node node, int minLinesToPreviousNode = 2, boolean mutateValidationState = true) {
        Node previousNode = lookForPreviousParagraph(node)

        boolean isNodeChainableBlock = chainableNodeClasses.find { it.isInstance(node) }
        boolean isPrevNodeChainableBlock = chainableNodeClasses.find { it.isInstance(previousNode) }
        if (isNodeChainableBlock && isPrevNodeChainableBlock && linesToPreviousNode(node) <= minLinesToPreviousNode) {
            // validation is delegated to the previous table
            return true
        }

        if (!previousNode || linesToPreviousNode(node) > minLinesToPreviousNode) {
            if (mutateValidationState) {
                invalidate()

                if (shouldAddPrefix(node)) {
                    if (node instanceof Link) {
                        addPrefixIfNeeded (node)
                    } else {
                        Paragraph paragraph = new Paragraph()
                        paragraph.prependChild(new Text(request.prefix))
                        node.insertBefore(paragraph)
                    }
                }
            }
            return false
        }

        ValidateNodeRes validRes = validateNode(previousNode)
        if (!validRes.isValid) {
            if (mutateValidationState) {
                invalidate()
                if (shouldAddPrefix(validRes.closestNode)) {
                    if (validRes.closestNode instanceof HtmlBlock) {
                        HtmlBlock htmlBlock = (HtmlBlock) validRes.closestNode
                        String currentHtml = htmlBlock.getLiteral()
                        String newHtml = request.prefix + (currentHtml ?: "")
                        customReplacements.put(markdownRenderer.render(previousNode), newHtml)
                    } else if (validRes.closestNode instanceof BulletList) {
                        addPrefixIfNeeded(node)
                    } else {
                        addPrefixIfNeeded(validRes)
                    }
                }
            }
            return false
        }

        return true
    }
}
