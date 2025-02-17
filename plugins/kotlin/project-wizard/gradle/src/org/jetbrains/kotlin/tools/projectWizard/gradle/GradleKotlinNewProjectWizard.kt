// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.tools.projectWizard.gradle

import com.intellij.ide.projectWizard.NewProjectWizardCollector.BuildSystem.logAddSampleCodeChanged
import com.intellij.ide.projectWizard.NewProjectWizardConstants.BuildSystem.GRADLE
import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.starters.local.StandardAssetsProvider
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.name
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.path
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep.Companion.ADD_SAMPLE_CODE_PROPERTY_NAME
import com.intellij.ide.wizard.chain
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import org.jetbrains.kotlin.tools.projectWizard.BuildSystemKotlinNewProjectWizard
import org.jetbrains.kotlin.tools.projectWizard.BuildSystemKotlinNewProjectWizardData
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizard
import org.jetbrains.kotlin.tools.projectWizard.setupKmpWizardLinkUI
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType.GradleGroovyDsl
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType.GradleKotlinDsl
import org.jetbrains.plugins.gradle.service.project.wizard.GradleNewProjectWizardData.GradleDsl
import org.jetbrains.plugins.gradle.service.project.wizard.GradleNewProjectWizardStep
import java.io.File

internal class GradleKotlinNewProjectWizard : BuildSystemKotlinNewProjectWizard {

    override val name = GRADLE

    override val ordinal = 200

    override fun createStep(parent: KotlinNewProjectWizard.Step) = Step(parent).chain(::AssetsStep)

    class Step(parent: KotlinNewProjectWizard.Step) :
        GradleNewProjectWizardStep<KotlinNewProjectWizard.Step>(parent),
        BuildSystemKotlinNewProjectWizardData by parent {

        private val addSampleCodeProperty = propertyGraph.property(true)
            .bindBooleanStorage(ADD_SAMPLE_CODE_PROPERTY_NAME)

        private val addSampleCode by addSampleCodeProperty

        private fun setupSampleCodeUI(builder: Panel) {
            builder.row {
                checkBox(UIBundle.message("label.project.wizard.new.project.add.sample.code"))
                    .bindSelected(addSampleCodeProperty)
                    .whenStateChangedFromUi { logAddSampleCodeChanged(it) }
            }
        }

        override fun setupSettingsUI(builder: Panel) {
            setupJavaSdkUI(builder)
            setupGradleDslUI(builder)
            setupParentsUI(builder)
            setupSampleCodeUI(builder)
            setupKmpWizardLinkUI(builder)
        }

        override fun setupAdvancedSettingsUI(builder: Panel) {
            setupGroupIdUI(builder)
            setupArtifactIdUI(builder)
        }

        override fun setupProject(project: Project) {
            KotlinNewProjectWizard.generateProject(
                project = project,
                projectPath = "$path/$name",
                projectName = name,
                sdk = sdk,
                buildSystemType = when (gradleDsl) {
                    GradleDsl.KOTLIN -> GradleKotlinDsl
                    GradleDsl.GROOVY -> GradleGroovyDsl
                },
                projectGroupId = groupId,
                artifactId = artifactId,
                version = version,
                addSampleCode = addSampleCode
            )
        }
    }

    private class AssetsStep(parent: NewProjectWizardStep) : AssetsNewProjectWizardStep(parent) {
        override fun setupAssets(project: Project) {
            outputDirectory = "$path/$name"
            addAssets(StandardAssetsProvider().getGradlewAssets())
            addAssets(StandardAssetsProvider().getGradleIgnoreAssets())
        }

        override fun setupProject(project: Project) {
            super.setupProject(project)

            val gradlewFile = File(outputDirectory, "gradlew")
            if (gradlewFile.exists()) {
                gradlewFile.setExecutable(true, false)
            }
        }
    }
}