/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.impl.findParentAnnotated
import com.google.devtools.ksp.symbol.impl.findParentDeclaration
import com.google.devtools.ksp.symbol.impl.getDocString
import com.intellij.psi.PsiElement

abstract class KSDeclarationJavaImpl(private val psi: PsiElement) /*: KSDeclaration*/ {
    open /*override*/ val packageName: KSName by lazy {
        (this as KSDeclaration).containingFile!!.packageName
    }

    override fun toString(): String {
        return (this as KSDeclaration).simpleName.asString()
    }

    open /*override*/ val docString by lazy {
        psi.getDocString()
    }

    open /*override*/ val parentDeclaration: KSDeclaration? by lazy {
        psi.findParentDeclaration()
    }

    open /*override*/ val parent: KSNode? by lazy {
        psi.findParentAnnotated()
    }
}
