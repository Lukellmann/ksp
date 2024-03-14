/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.toKSModifiers
import com.google.devtools.ksp.impl.symbol.java.KSAnnotationJavaImpl
import com.google.devtools.ksp.impl.symbol.util.toKSModifiers
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJvmModifiersOwner
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtModifierListOwner

abstract class AbstractKSDeclarationImpl(val ktDeclarationSymbol: KtDeclarationSymbol) : /*KSDeclaration,*/ Deferrable {
    abstract fun asKSDeclaration(): KSDeclaration

    open /*override*/ val origin: Origin by lazy {
        mapAAOrigin(ktDeclarationSymbol)
    }

    open /*override*/ val location: Location by lazy {
        ktDeclarationSymbol.psi.toLocation()
    }

    open /*override*/ val simpleName: KSName by lazy {
        KSNameImpl.getCached((ktDeclarationSymbol as? KtNamedSymbol)?.name?.asString() ?: "")
    }

    open /*override*/ val annotations: Sequence<KSAnnotation> by lazy {
        originalAnnotations
    }

    open /*override*/ val modifiers: Set<Modifier> by lazy {
        if (origin == Origin.JAVA_LIB || origin == Origin.KOTLIN_LIB || origin == Origin.SYNTHETIC) {
            when (ktDeclarationSymbol) {
                is KtPropertySymbol -> ktDeclarationSymbol.toModifiers()
                is KtClassOrObjectSymbol -> ktDeclarationSymbol.toModifiers()
                is KtFunctionLikeSymbol -> ktDeclarationSymbol.toModifiers()
                is KtJavaFieldSymbol -> ktDeclarationSymbol.toModifiers()
                else -> throw IllegalStateException("Unexpected symbol type ${ktDeclarationSymbol.javaClass}")
            }
        } else {
            when (val psi = ktDeclarationSymbol.psi) {
                is KtModifierListOwner -> psi.toKSModifiers()
                is PsiModifierListOwner -> psi.toKSModifiers()
                else -> throw IllegalStateException("Unexpected symbol type ${ktDeclarationSymbol.psi?.javaClass}")
            }
        }
    }

    open /*override*/ val containingFile: KSFile? by lazy {
        ktDeclarationSymbol.toContainingFile()
    }

    open /*override*/ val packageName: KSName by lazy {
        // source
        containingFile?.packageName
            // top level declaration
            ?: when (ktDeclarationSymbol) {
                is KtClassLikeSymbol -> ktDeclarationSymbol.classIdIfNonLocal?.packageFqName?.asString()
                is KtCallableSymbol -> ktDeclarationSymbol.callableIdIfNonLocal?.packageName?.asString()
                else -> null
            }?.let { KSNameImpl.getCached(it) }
            //  null -> non top level declaration, find in parent
            ?: ktDeclarationSymbol.getContainingKSSymbol()?.packageName
            ?: throw IllegalStateException("failed to find package name for $this")
    }

    open /*override*/ val typeParameters: List<KSTypeParameter> by lazy {
        ktDeclarationSymbol.typeParameters.map { KSTypeParameterImpl.getCached(it) }
    }

    open /*override*/ val parentDeclaration: KSDeclaration? by lazy {
        parent as? KSDeclaration
    }

    open /*override*/ val parent: KSNode? by lazy {
        analyze {
            ktDeclarationSymbol.getContainingSymbol()?.let {
                ktDeclarationSymbol.getContainingKSSymbol()
            } ?: (ktDeclarationSymbol.psi?.parentOfType<PsiClass>())?.getNamedClassSymbol()?.let {
                KSClassDeclarationImpl.getCached(it)
            } ?: ktDeclarationSymbol.toContainingFile()
        }
    }

    override fun toString(): String {
        return simpleName.asString()
    }

    open /*override*/ val docString: String?
        get() = ktDeclarationSymbol.toDocString()

    internal val originalAnnotations = if (ktDeclarationSymbol.psi is KtElement || ktDeclarationSymbol.psi == null) {
        ktDeclarationSymbol.annotations(this.asKSDeclaration())
    } else {
        (ktDeclarationSymbol.psi as PsiJvmModifiersOwner)
            .annotations.map { KSAnnotationJavaImpl.getCached(it, this.asKSDeclaration()) }.asSequence()
    }
}
