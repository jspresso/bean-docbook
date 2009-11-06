/*
 * Copyright (c) 2005-2009 Vincent Vandenschrick. All rights reserved.
 *
 *  This file is part of the Jspresso framework.
 *
 *  Jspresso is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Jspresso is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Jspresso.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jspresso.beandocbook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

/**
 * A simple doclet to generate docbook from javadoc information.
 * 
 * @version $LastChangedRevision$
 * @author Vincent Vandenschrick
 */
public class BeanDocbookDoclet {

  private static String rootClassName;
  private static String apidocUrl;
  private static String outputDir;
  private static int    indent = 0;
  private static Writer writer;

  /**
   * Generate docbook part documenting beans.
   * 
   * @param root
   *          the root doc.
   * @return true if succesful.
   */
  public static boolean start(RootDoc root) {
    Map<String, ClassTree> classTrees = new LinkedHashMap<String, ClassTree>();
    ClassTree rootClassTree = null;
    readOptions(root.options());
    ClassDoc[] classes = root.classes();
    try {
      File f = new File(outputDir, rootClassName.substring(rootClassName
          .lastIndexOf(".") + 1)
          + ".xml");
      f.getParentFile().mkdirs();
      writer = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(f), "UTF-8"));
      writeHeader();
      for (ClassDoc classDoc : classes) {
        if (classDoc.isPublic() && classDoc.isClass()) {
          ClassTree classTree = new ClassTree(classDoc);
          if (rootClassName.equals(classDoc.qualifiedName())) {
            rootClassTree = classTree;
          }
          classTrees.put(classDoc.qualifiedTypeName(), classTree);
        }
      }
      if (rootClassTree != null) {
        for (Map.Entry<String, ClassTree> entry : classTrees.entrySet()) {
          ClassTree parent = classTrees.get(entry.getValue().getRoot()
              .superclassType().qualifiedTypeName());
          if (parent != null) {
            parent.getSubclasses().add(entry.getValue());
          }
        }
        processClassTree(rootClassTree);
      }
      writer.flush();
      writer.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return true;
  }

  private static void writeHeader() throws IOException {
    writeLine("<?xml version='1.0' encoding='UTF-8'?>");
    writeLine("<!DOCTYPE chapter PUBLIC '-//OASIS//DTD DocBook XML V4.4//EN'");
    writeLine("  'http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd'>");
  }

  private static void processClassTree(ClassTree classTree) throws IOException {
    ClassDoc classDoc = classTree.getRoot();
    writeLine("<section id='" + classDoc.qualifiedTypeName() + "'>");
    indent++;
    processClassDoc(classDoc);
    boolean childInSection = classTree.getSubclasses().size() > 1;
    if (!childInSection) {
      writeLine("<para></para>");
      writeLine("<para></para>");
      indent--;
      writeLine("</section>");
    }
    for (ClassTree subclassTree : classTree.getSubclasses()) {
      processClassTree(subclassTree);
    }
    if (childInSection) {
      writeLine("<para></para>");
      writeLine("<para></para>");
      indent--;
      writeLine("</section>");
    }
  }

  private static void processClassDoc(ClassDoc classDoc) throws IOException {
    writeLine("<title>" + classDoc.name() + "</title>");
    // writeLine("<para>");
    // writeLine("<?dbfo keep-with-next='always'?>");
    // writeLine("</para>");
    writeLine("<para>" + classDoc.commentText() + "</para>");
    writeLine("<itemizedlist>");
    indent++;
    writeLine("<listitem>");
    indent++;
    writeLine("<para><emphasis role='bold'>Full name</emphasis> : <code><ulink url='"
        + computeJavadocUrl(classDoc)
        + "'>"
        + hyphenateDottedString(classDoc.qualifiedTypeName())
        + "</ulink></code></para>");
    indent--;
    writeLine("</listitem>");
    writeLine("<listitem>");
    indent++;
    if (classDoc.superclassType().qualifiedTypeName()
        .startsWith("org.jspresso")) {
      writeLine("<para><emphasis role='bold'>Inherits</emphasis> : <code><link linkend='"
          + classDoc.superclassType().qualifiedTypeName()
          + "'>"
          + classDoc.superclass().name() + "</link></code></para>");
    }
    indent--;
    writeLine("</listitem>");
    indent--;
    writeLine("</itemizedlist>");
    writeLine("<para></para>");
    writeLine("<table>");
    indent++;
    writeLine("<title>" + classDoc.name() + " properties</title>");
    writeLine("<tgroup cols='3'>");
    indent++;
    writeLine("<colspec colname='name' colwidth='1*' />");
    writeLine("<colspec colname='type' colwidth='1*' />");
    writeLine("<colspec colname='description' colwidth='3*' />");
    writeLine("<thead>");
    indent++;
    writeLine("<row>");
    indent++;
    writeLine("<entry align='center'>Name</entry>");
    writeLine("<entry align='center'>Type</entry>");
    writeLine("<entry align='center'>Description</entry>");
    indent--;
    writeLine("</row>");
    indent--;
    writeLine("</thead>");
    writeLine("<tbody>");
    indent++;
    boolean atleastOneRow = false;
    for (MethodDoc methodDoc : classDoc.methods()) {
      if (methodDoc.isPublic() && isSetter(methodDoc)) {
        atleastOneRow = true;
        writeLine("<row>");
        indent++;
        String property = getProperty(methodDoc);
        writeLine("<entry>" + property + "</entry>");
        Parameter param = methodDoc.parameters()[0];
        ParameterizedType pType = param.type().asParameterizedType();
        if (pType != null) {
          Type[] typeArguments = pType.asParameterizedType().typeArguments();
          StringBuffer buff = new StringBuffer();
          buff.append(param.type().simpleTypeName());
          buff.append("&lt;");
          for (int i = 0; i < typeArguments.length; i++) {
            buff.append(typeArguments[i].simpleTypeName());
            if (i < typeArguments.length - 1) {
              buff.append(",");
            }
          }
          buff.append("&gt;");
          writeLine("<entry>" + buff.toString() + "</entry>");
        } else {
          writeLine("<entry>" + param.type().simpleTypeName() + "</entry>");
        }
        writeLine("<entry>" + methodDoc.commentText() + "</entry>");
        indent--;
        writeLine("</row>");
      }
    }
    if (!atleastOneRow) {
      writeLine("<row>");
      indent++;
      writeLine("<entry namest='name' nameend='description'>This class does not have any specific property.</entry>");
      indent--;
      writeLine("</row>");
    }
    indent--;
    writeLine("</tbody>");
    indent--;
    writeLine("</tgroup>");
    indent--;
    writeLine("</table>");
  }

  private static String hyphenateDottedString(String source) {
    return source.replace(".", "&#x200B;.");
  }

  private static String computeJavadocUrl(ClassDoc classDoc) {
    String name = classDoc.qualifiedTypeName();
    return apidocUrl + "/" + name.replace(".", "/") + ".html";
  }

  private static String getProperty(MethodDoc methodDoc) {
    return methodDoc.name().substring(3, 4).toLowerCase()
        + methodDoc.name().substring(4);
  }

  private static boolean isSetter(MethodDoc methodDoc) {
    return methodDoc.name().startsWith("set");
  }

  private static void writeLine(String content) throws IOException {
    for (int i = 0; i < indent; i++) {
      writer.write("  ");
    }
    writer.write(content);
    writer.write("\n");
  }

  /**
   * Indicates this doclet supports 1.5 sources.
   * 
   * @return LanguageVersion.JAVA_1_5
   */
  public static LanguageVersion languageVersion() {
    return LanguageVersion.JAVA_1_5;
  }

  private static void readOptions(String[][] options) {
    for (int i = 0; i < options.length; i++) {
      String[] opt = options[i];
      if (opt[0].equals("-rootClassName")) {
        rootClassName = opt[1];
      } else if (opt[0].equals("-outputDir")) {
        outputDir = opt[1];
      } else if (opt[0].equals("-apidocUrl")) {
        apidocUrl = opt[1];
      }
    }
  }

  /**
   * Mandatory for custom options
   * 
   * @param option
   *          the custom option.
   * @return the length of the option including the option itself.
   */
  public static int optionLength(String option) {
    if (option.equals("-rootClassName")) {
      return 2;
    } else if (option.equals("-outputDir")) {
      return 2;
    } else if (option.equals("-apidocUrl")) {
      return 2;
    }
    return 0;
  }

}
