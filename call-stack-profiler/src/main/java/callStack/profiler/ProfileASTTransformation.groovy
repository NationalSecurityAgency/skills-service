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
package callStack.profiler

import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.util.concurrent.atomic.AtomicLong

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@Slf4j
public class ProfileASTTransformation extends AbstractASTTransformation {
    final static AtomicLong counter = new AtomicLong(0)
    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        if (!nodes) return
        if (!nodes[0]) return
        if (!nodes[1]) return
        if (!nodes[0] instanceof AnnotatedNode) return
        if (!nodes[1] instanceof MethodNode) return

        MethodNode annotatedMethod = nodes[1]
        List<AnnotationNode> annotationNodeList = annotatedMethod.getAnnotations(new ClassNode(Profile))
        if (!annotationNodeList) {
            return
        }
        String profileKey = getMemberStringValue(annotationNodeList,  "name")
        boolean aggregateIntoSingleEvent = getMemberBooleanValue(annotationNodeList, "aggregateIntoSingleEvent", true)

        if (!profileKey) {
            ClassNode declaringClass = annotatedMethod.declaringClass
            profileKey = declaringClass.nameWithoutPackage + "." + annotatedMethod.name

            // add the parameter types to the profile key if more than one method exists with the same name
            if (declaringClass.getMethods(annotatedMethod.name).size() > 1) {
                for (Parameter parameter : annotatedMethod.parameters) {
                    profileKey += '_' + parameter.type.nameWithoutPackage
                }
            }
        }
        if(!aggregateIntoSingleEvent){
            profileKey = profileKey+counter.getAndIncrement()
        }

        log.info('profile key is {}', profileKey)
        Statement startMessage = createProfileCallAst("start", profileKey)
        Statement endMessage = createProfileCallAst("stop", profileKey, aggregateIntoSingleEvent)
        wrapWithTryFinally(annotatedMethod, startMessage, endMessage)
    }

    private static void wrapWithTryFinally(MethodNode methodNode, Statement startProf, Statement stopProf) {
        BlockStatement code = (BlockStatement) methodNode.getCode()
        BlockStatement newCode = new BlockStatement()
        newCode.addStatement(startProf)

        TryCatchStatement tryCatchStatement = new TryCatchStatement(code, new BlockStatement())
        newCode.addStatement(tryCatchStatement)
        methodNode.setCode(newCode)
        tryCatchStatement.setFinallyStatement(stopProf)
    }

    private Statement createProfileCallAst(String method, String message) {
        return new ExpressionStatement(
                new StaticMethodCallExpression(
                        ClassHelper.make(CProf),
                        method,
                        new ArgumentListExpression(
                                new ConstantExpression(message)
                        )
                )
        )
    }
    private Statement createProfileCallAst(String method, String message, boolean aggregateIntoSingleEvent) {
        return new ExpressionStatement(
                new StaticMethodCallExpression(
                        ClassHelper.make(CProf),
                        method,
                        new ArgumentListExpression(
                                new ConstantExpression(message),
                                new ConstantExpression(aggregateIntoSingleEvent)
                        )
                )
        )
    }

    protected String getMemberStringValue(List<AnnotationNode> annotationNodeList, String name){
        annotationLoop: for (AnnotationNode annotationNode : annotationNodeList) {
            String res = getMemberStringValue(annotationNode, name)
            if(res){
                return res
            }
        }
        return null
    }

    protected Boolean getMemberBooleanValue(List<AnnotationNode> annotationNodeList, String name, boolean defaultVal){
        annotationLoop: for (AnnotationNode annotationNode : annotationNodeList) {
            Boolean res = getMemberBooleanValue(annotationNode, name)
            if(res!=null){
                return res
            }
        }
        defaultVal
    }

    protected Boolean getMemberBooleanValue(AnnotationNode node, String name) {
        final Expression member = node.getMember(name);
        if (member != null && member instanceof ConstantExpression) {
            Object result = ((ConstantExpression) member).getValue();
            if (result != null) return (boolean)result;
        }
        return null;
    }
}