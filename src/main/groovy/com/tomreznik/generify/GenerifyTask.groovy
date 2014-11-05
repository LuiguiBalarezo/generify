package com.tomreznik.generify

import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class GenerifyTask extends DefaultTask {
  def final static String ATTR_GENERIC_TYPE = "{http://schemas.android.com/apk/res-auto}generic_type"

  // Output directory to generate Java files into
  @OutputDirectory File sourceOutputDir

  // Output directory to generate XML layout files into
  @OutputDirectory File layoutsOutputDir

  // Output directory to generate XML value files into
  @OutputDirectory File valuesOutputDir

  // Collection of input files to read XML resources from
  @InputFiles FileCollection resInputDirs

  // Fully qualified package name of the project
  @Input String packageName

  // Mapping of fully qualified view class names to injections
  HashMap<String, Injection> injections

  @TaskAction void execute(IncrementalTaskInputs inputs) {
    injections = new HashMap<>()

    // Iterate through all input resource folders
    resInputDirs.each { dir ->
      // Process XML layouts
      def file = new File("${dir}/layout")
      if (file.exists()) {
        file.eachFileRecurse(FileType.FILES, { layout -> processLayout(layout) })
      }

      // Inject custom XML attribute
      injectGenericTypeAttr()
    }
  }

  def injectGenericTypeAttr() {
    def attrs = new File("${valuesOutputDir}/attrs.xml")
    if (attrs.createNewFile()) {
      attrs.withWriter { writer ->
        writer.write('''<?xml version="1.0" encoding="utf-8"?>
<resources>
  <declare-styleable name="View">
    <attr name="generic_type" format="string" />
  </declare-styleable>
</resources>''')
      }
    }
  }

  def processLayout(File layout) {
    def xml = new XmlParser().parse(layout)

    // Iterate through all XML nodes
    xml.each { Node node -> processNode(node) }

    if (injections.size() > 0) {
      // Replace original nodes with injected classes also
      // removing the 'generic_type' plugin from the cloned XML node
      injections.values().each { Injection inject ->
        def attrs = inject.node.attributes()
        attrs.collect {
          def key = it.getKey()
          if (key.toString().contains(ATTR_GENERIC_TYPE)) {
            return key
          }
        }.each { attrs.remove(it) }

        inject.node.replaceNode new Node(inject.node, inject.fqClassName, attrs)
      }

      def fileName = layout.getAbsoluteFile().getName()
      def file = new File("${layoutsOutputDir}/${fileName}")
      file.createNewFile()
      file.withWriter { writer -> new XmlNodePrinter(new PrintWriter(writer)).print(xml) }
    }
  }

  def processNode(Node node) {
    def String attrKey
    def String attrValue

    // Collect type attribute
    node.attributes().each { attr ->
      if ((attr.getKey() as String).contains(
          "{http://schemas.android.com/apk/res-auto}generic_type")) {
        attrKey = attr.getKey() as String
        attrValue = attr.getValue()
      }
    }

    if (attrKey != null && attrValue != null) {
      // Remove type attribute from XML
      injectClass(node, attrValue)
      node.attributes().remove(attrKey)
    }

    // Iterate through all children recursively
    node.children().each { Node child -> processNode(child) }
  }

  def injectClass(Node node, String genericType) {
    def fqClassName = "${node.name()}${genericType}"
    def inject = injections.get(fqClassName)

    if (inject != null) {
      return
    }

    def nodeName = node.name() as String
    def classNameShort = fqClassName.substring(fqClassName.lastIndexOf('.') + 1)
    def parentNameShort = nodeName.substring(nodeName.lastIndexOf('.') + 1)
    def file = new File("${sourceOutputDir}/${classNameShort}.java")
    if (file.createNewFile()) {
      file.withWriter { writer ->
        writer.write("// GENERATED CODE DO NOT MODIFY")
        writer.write("""
package ${packageName};

import android.content.Context;
import android.util.AttributeSet;
import ${node.name()};

public class ${classNameShort} extends ${parentNameShort}<${genericType}> {
  public ${classNameShort}(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ${classNameShort}(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }
}
""" as String)
      }

      inject = new Injection()
      inject.fqClassName = fqClassName
      inject.parentClassName = node.name()
      inject.genericsTypeExpression = genericType
      inject.node = node
      injections.put(fqClassName, inject)
    }
  }

  class Injection {
    String fqClassName
    String parentClassName
    String genericsTypeExpression
    Node node
  }
}
