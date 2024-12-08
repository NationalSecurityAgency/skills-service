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
package skills.auth

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

class SecurityMode {

    static class PkiAuth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.PKI
        }
    }

    static class FormAuth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.FORM
        }
    }

    static class SAML2Auth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.SAML2
        }
    }

    static class NonSAML2Auth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode !== AuthMode.SAML2
        }
    }

    static class FormOrSAML2Auth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.FORM || AuthMode.SAML2
        }
    }



}
