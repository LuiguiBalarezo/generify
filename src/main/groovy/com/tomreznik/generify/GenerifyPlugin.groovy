package com.tomreznik.generify

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.ide.common.res2.ResourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

class GenerifyPlugin implements Plugin<Project> {
  def static final DIR_JAVA = "java"
  def static final DIR_LAYOUT = "layout"
  def static final DIR_RES = "res"
  def static final DIR_VALUES = "values"
  def static final TASK_NAME = "generify"

  @Override void apply(Project project) {
    project.afterEvaluate {
      if (project.getPlugins().hasPlugin(AppPlugin.class)) {
        project.getExtensions().findByType(AppExtension.class).getApplicationVariants().each {
          setup(project, it);
        }
      }
    }
  }

  def static setup(Project project, BaseVariant variant) {
    def mergeResourcesTask = variant.getMergeResources()
    def inputResourceSets = mergeResourcesTask.getInputResourceSets();
    def resourceSet = new ResourceSet("${variant.getName()}${TASK_NAME.capitalize()}")
    resourceSet.addSource(new File(project.getBuildDir(), getXmlBasePath(variant)))
    inputResourceSets.add(resourceSet)
    mergeResourcesTask.setInputResourceSets(inputResourceSets)
    mergeResourcesTask.dependsOn createGenerifyTask(project, variant)
  }

  def static Task createGenerifyTask(Project project, BaseVariant variant) {
    def sOutputDir = new File(project.getBuildDir(), getJavaPath(variant))
    def task = project.task("${TASK_NAME.toLowerCase()}${variant.getName().capitalize()}", type: GenerifyTask) {
      sourceOutputDir = sOutputDir
      layoutsOutputDir = new File(project.getBuildDir(), getXmlFolderPath(variant, DIR_LAYOUT))
      valuesOutputDir = new File(project.getBuildDir(), getXmlFolderPath(variant, DIR_VALUES))
      resInputDirs = getResDirectories(project, variant)
      packageName = project.android.defaultConfig.applicationId
    }

    variant.registerJavaGeneratingTask(task, sOutputDir)
    variant.addJavaSourceFoldersToModel(sOutputDir)

    return task
  }

  def static FileCollection getResDirectories(Project project, BaseVariant variant) {
    project.files(variant.sourceSets*.resDirectories.flatten())
  }

  def static String getXmlBasePath(BaseVariant variant) {
    return "generify/${variant.getName()}/${DIR_RES}"
  }

  def static String getXmlFolderPath(BaseVariant variant, String folder) {
    return "${getXmlBasePath(variant)}/${folder}"
  }

  def static String getJavaPath(BaseVariant variant) {
    return "generated/source/generify/${variant.getName()}/${DIR_JAVA}"
  }
}