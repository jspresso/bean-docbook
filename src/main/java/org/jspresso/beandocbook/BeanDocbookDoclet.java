/*
 * Copyright (c) 2005-2011 Vincent Vandenschrick. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

/**
 * A simple doclet to generate docbook from javadoc information.
 *
 * @version $LastChangedRevision: 3701 $
 * @author Vincent Vandenschrick
 */
public class BeanDocbookDoclet extends AbstractBeanDoclet {

  private int indent;

  /**
   * Generate docbook part documenting beans.
   *
   * @param root
   *          the root doc.
   * @return true if succesful.
   */
  public static boolean start(RootDoc root) {
    AbstractBeanDoclet.setThreadInstance(new BeanDocbookDoclet());
    return AbstractBeanDoclet.start(root);
  }

  @Override
  protected void writeHeader() throws IOException {
    writeLine("<?xml version='1.0' encoding='UTF-8'?>");
    writeLine("<!DOCTYPE chapter PUBLIC '-//OASIS//DTD DocBook XML V4.4//EN'");
    writeLine("  'http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd'>");
  }

  /**
   * Write line.
   *
   * @param content
   *     the content
   * @throws IOException
   *     the iO exception
   */
  @Override
  protected void writeLine(String content) throws IOException {
    for (int i = 0; i < indent; i++) {
      writer.write("  ");
    }
    super.writeLine(content);
  }

  @Override
  protected void setupConfigSet(Map<String, String> configSet) {
    indent = 0;
    super.setupConfigSet(configSet);
  }

  /**
   * Write section.
   *
   * @param rootClassTree the root class tree
   * @throws IOException the iO exception
   */
  @Override
  protected void writeRootSection(ClassTree rootClassTree) throws IOException {
    writeLine("<section>");
    writeLine("<title>Reference for " + rootClassTree.getRoot().name() + " hierarchy</title>");
    indent++;
    writeLine("<para></para>");
    processClassTree(rootClassTree);
    indent--;
    writeLine("</section>");
  }

  /**
   * Write class section.
   *
   * @param classTree the class tree
   * @param classDoc the class doc
   * @throws IOException the iO exception
   */
  @Override
  protected void writeClassSection(ClassTree classTree, ClassDoc classDoc) throws IOException {
    writeLine("<section id='" + classDoc.qualifiedTypeName() + "'>");
    indent++;
    processClassDoc(classTree);
  }

  /**
   * Close class section.
   *
   * @throws IOException the iO exception
   */
  @Override
  protected void closeClassSection() throws IOException {
    writeLine("<para></para>");
    writeLine("<para></para>");
    indent--;
    writeLine("</section>");
  }

  /**
   * Javadoc to doc.
   *
   * @param source
   *     the source
   * @return the string
   */
  @Override
  protected String javadocToDoc(String source) {
    String dbSource = source.replaceAll("<p>", "</para><para>");
    dbSource = dbSource.replaceAll("<br>", "</para><para>");
    dbSource = dbSource.replaceAll("<i>", "<emphasis>");
    dbSource = dbSource.replaceAll("</i>", "</emphasis>");
    dbSource = dbSource.replaceAll("<b>", "<emphasis role='bold'>");
    dbSource = dbSource.replaceAll("</b>", "</emphasis>");
    dbSource = dbSource.replaceAll("<ul>", "<itemizedlist>");
    dbSource = dbSource.replaceAll("</ul>", "</itemizedlist>");
    dbSource = dbSource.replaceAll("<ol>", "<orderedlist>");
    dbSource = dbSource.replaceAll("</ol>", "</orderedlist>");
    dbSource = dbSource.replaceAll("<li>", "<listitem><para>");
    dbSource = dbSource.replaceAll("</li>", "</para></listitem>");
    dbSource = dbSource.replaceAll("<pre>", "<programlisting>");
    dbSource = dbSource.replaceAll("</pre>", "</programlisting>");
    dbSource = dbSource.replaceAll("\\{@code ([^\\}]*)}", "<code>$1</code>");
    return dbSource;
  }

  /**
   * Process class doc.
   *
   * @param classTree
   *     the class tree
   * @throws IOException
   *     the iO exception
   */
  @Override
  protected void processClassDoc(ClassTree classTree) throws IOException {
    ClassDoc classDoc = classTree.getRoot();
    writeLine("<title>" + classDoc.name() + "</title>");
    // writeLine("<para>");
    // writeLine("<?dbfo keep-with-next='always'?>");
    // writeLine("</para>");
    writeLine("<itemizedlist>");
    indent++;
    writeLine("<listitem><para><emphasis role='bold'>Full name</emphasis> : <code><ulink url='" + computeJavadocUrl(
        classDoc.qualifiedTypeName()) + "'>" + hyphenateDottedString(classDoc.qualifiedTypeName())
        + "</ulink></code></para></listitem>");
    if (classDoc.superclassType().qualifiedTypeName().startsWith("org.jspresso")) {
      if (!isInternalOrDeprecated(classDoc.superclassType().asClassDoc())) {
        writeLine("<listitem><para><emphasis role='bold'>Super-type</emphasis> : <code><link linkend='" + classDoc
            .superclassType().qualifiedTypeName() + "'>" + classDoc.superclass().name()
            + "</link></code></para></listitem>");
      } else {
        writeLine("<listitem><para><emphasis role='bold'>Super-type</emphasis> : <code>" + classDoc.superclass().name()
            + "</code></para></listitem>");
      }
    }
    if (classTree.getSubclasses().size() > 0) {
      StringBuffer buff = new StringBuffer();
      List<ClassTree> children = new ArrayList<ClassTree>(classTree.getSubclasses());
      Collections.sort(children);
      boolean first = true;
      for (ClassTree subclassTree : children) {
        if (!isInternalOrDeprecated(subclassTree.getRoot())) {
          if (!first) {
            buff.append(", ");
          }
          first = false;
          buff.append(
              "<code><link linkend='" + subclassTree.getRoot().qualifiedTypeName() + "'>" + subclassTree.getRoot()
                                                                                                        .name()
                  + "</link></code>");
        }
      }
      writeLine(
          "<listitem><para><emphasis role='bold'>Sub-types</emphasis> : " + buff.toString() + "</para></listitem>");
    }
    indent--;
    writeLine("</itemizedlist>");
    writeLine("<para></para>");
    writeLine("<para></para>");
    writeLine("<para>" + javadocToDoc(classDoc.commentText()) + "</para>");
    writeLine("<para></para>");
    writeLine("<para></para>");
    writeLine("<table colsep='0' rowsep='1' tabstyle='splitable' frame='topbot'>");
    writeLine("<?dbfo keep-together='auto'?>");
    indent++;
    writeLine("<title>" + classDoc.name() + " properties</title>");
    writeLine("<tgroup cols='2'>");
    indent++;
    writeLine("<colspec colname='property' colwidth='1*' />");
    writeLine("<colspec colname='description' colwidth='2*' />");
    writeLine("<thead>");
    indent++;
    writeLine("<row>");
    indent++;
    writeLine("<entry align='left'>Property</entry>");
    writeLine("<entry align='left'>Description</entry>");
    indent--;
    writeLine("</row>");
    indent--;
    writeLine("</thead>");
    writeLine("<tbody>");
    indent++;
    boolean atleastOneRow = false;
    Map<String, MethodDoc> propertiesMap = new TreeMap<String, MethodDoc>();
    for (MethodDoc methodDoc : classDoc.methods()) {
      if (isSetterForRefDoc(methodDoc)) {
        atleastOneRow = true;
        propertiesMap.put(getProperty(methodDoc), methodDoc);
      }
    }
    for (Map.Entry<String, MethodDoc> propEntry : propertiesMap.entrySet()) {
      writeLine("<row>");
      indent++;
      Parameter param = propEntry.getValue().parameters()[0];
      ParameterizedType pType = param.type().asParameterizedType();
      StringBuilder typeBuff = new StringBuilder();
      if (pType != null) {
        Type[] typeArguments = pType.asParameterizedType().typeArguments();
        if (param.type().qualifiedTypeName().startsWith("org.jspresso")) {
          typeBuff.append("<ulink url='" + computeJavadocUrl(param.type().qualifiedTypeName()) + "'>"
              + hyphenateCamelCase(param.type().simpleTypeName()) + "</ulink>");
        } else {
          typeBuff.append(hyphenateCamelCase(param.type().simpleTypeName()));
        }
        typeBuff.append("&#x200B;&lt;&#x200B;");
        for (int i = 0; i < typeArguments.length; i++) {
          if (typeArguments[i].qualifiedTypeName().startsWith("org.jspresso")) {
            typeBuff.append("<ulink url='" + computeJavadocUrl(typeArguments[i].qualifiedTypeName()) + "'>"
                + hyphenateCamelCase(typeArguments[i].simpleTypeName()) + "</ulink>");
          } else {
            typeBuff.append(typeArguments[i].simpleTypeName());
          }
          if (i < typeArguments.length - 1) {
            typeBuff.append("&#x200B;,");
          }
        }
        typeBuff.append("&#x200B;&gt;&#x200B;");
      } else {
        if (param.type().qualifiedTypeName().startsWith("org.jspresso")) {
          typeBuff.append("<ulink url='" + computeJavadocUrl(param.type().qualifiedTypeName()) + "'>"
              + hyphenateCamelCase(param.type().simpleTypeName()) + "</ulink>");
        } else {
          typeBuff.append(hyphenateCamelCase(param.type().simpleTypeName()));
        }
      }
      writeLine(
          "<entry valign='middle'><para><emphasis role='bold'>" + propEntry.getKey() + "</emphasis></para><para><code>"
              + typeBuff.toString() + "</code></para></entry>");
      writeLine("<entry><para>" + javadocToDoc(propEntry.getValue().commentText()) + "</para></entry>");
      indent--;
      writeLine("</row>");
    }
    if (!atleastOneRow) {
      writeLine("<row>");
      indent++;
      writeLine(
          "<entry namest='property' nameend='description'>This class does not have any specific property.</entry>");
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

  /**
   * Gets output extension.
   *
   * @return the output extension
   */
  @Override
  protected String getOutputExtension() {
    return ".xml";
  }

}
