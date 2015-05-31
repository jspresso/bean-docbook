/*
 * Copyright (c) 2005-2015 Vincent Vandenschrick. All rights reserved.
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
 * A simple doclet to generate markdown from javadoc information.
 *
 * @author Vincent Vandenschrick
 * @version $LastChangedRevision: 3701 $
 */
public class BeanMarkdownDoclet extends AbstractBeanDoclet {

  /**
   * Generate markdown part documenting beans.
   *
   * @param root
   *     the root doc.
   * @return true if succesful.
   */
  public static boolean start(RootDoc root) {
    AbstractBeanDoclet.setThreadInstance(new BeanMarkdownDoclet());
    return AbstractBeanDoclet.start(root);
  }

  /**
   * Write section.
   *
   * @param rootClassTree
   *     the root class tree
   * @throws IOException
   *     the iO exception
   */
  @Override
  protected void writeRootSection(ClassTree rootClassTree) throws IOException {
    writeLine("## " + rootClassTree.getRoot().name());
    writeLine("");
    processClassTree(rootClassTree);
  }

  /**
   * Write class section.
   *
   * @param classTree
   *     the class tree
   * @param classDoc
   *     the class doc
   * @throws IOException
   *     the iO exception
   */
  @Override
  protected void writeClassSection(ClassTree classTree, ClassDoc classDoc) throws IOException {
    processClassDoc(classTree);
  }

  /**
   * Close class section.
   *
   * @throws IOException
   *     the iO exception
   */
  @Override
  protected void closeClassSection() throws IOException {
    writeLine("");
    writeLine("");
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
    String dbSource = source.replaceAll("\\{@code ([^\\}]*)}", "<code>$1</code>");
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
    writeLine("#### " + "<a name=\"" + classDoc.qualifiedTypeName() + "\"></a>" + classDoc.name());
    writeLine("");
    writeLine("+ **Full name** : " + createLink(classDoc.qualifiedTypeName(), computeJavadocUrl(
        classDoc.qualifiedTypeName())));
    if (classDoc.superclassType().qualifiedTypeName().startsWith("org.jspresso")) {
      if (!isInternalOrDeprecated(classDoc.superclassType().asClassDoc())) {
        writeLine("+ **Super-type** : " + createLink(classDoc.superclass().name(),
            "#" + classDoc.superclassType().qualifiedTypeName()));
      } else {
        writeLine("+ **Super-type** : `" + classDoc.superclass().name() + "`");
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
          buff.append(createLink(subclassTree.getRoot().name(), "#" + subclassTree.getRoot().qualifiedTypeName()));
        }
      }
      writeLine("+ **Sub-types** : " + buff.toString());
    }
    writeLine("");
    writeLine("");
    writeLine("");
    writeLine(javadocToDoc(classDoc.commentText()));
    writeLine("");
    writeLine("");
    writeLine("");
    writeLine("<table>");
    writeLine("<caption>" + classDoc.name() + " properties</caption>");
    writeLine("<colgroup>");
    writeLine("<col width=\"33%\" />");
    writeLine("<col width=\"66%\" />");
    writeLine("</colgroup>");
    writeLine("<thead>");
    writeLine("<tr class=\"header\">");
    writeLine("<th align=\"left\">Property</th>");
    writeLine("<th align=\"left\">Description</th>");
    writeLine("</tr>");
    writeLine("</thead>");
    writeLine("<tbody>");
    boolean atleastOneRow = false;
    Map<String, MethodDoc> propertiesMap = new TreeMap<String, MethodDoc>();
    for (MethodDoc methodDoc : classDoc.methods()) {
      if (isSetterForRefDoc(methodDoc)) {
        atleastOneRow = true;
        propertiesMap.put(getProperty(methodDoc), methodDoc);
      }
    }
    int row = 0;
    for (Map.Entry<String, MethodDoc> propEntry : propertiesMap.entrySet()) {
      row++;
      writeLine("<tr class=\"" + (row % 2 == 0 ? "even" : "odd") + "\">");
      Parameter param = propEntry.getValue().parameters()[0];
      ParameterizedType pType = param.type().asParameterizedType();
      StringBuilder typeBuff = new StringBuilder();
      if (pType != null) {
        Type[] typeArguments = pType.asParameterizedType().typeArguments();
        if (param.type().qualifiedTypeName().startsWith("org.jspresso")) {
          typeBuff.append(createHtmlLink(hyphenateCamelCase(param.type().simpleTypeName()), computeJavadocUrl(
              param.type().qualifiedTypeName())));
        } else {
          typeBuff.append(hyphenateCamelCase(param.type().simpleTypeName()));
        }
        typeBuff.append("&#x200B;&lt;&#x200B;");
        for (int i = 0; i < typeArguments.length; i++) {
          if (typeArguments[i].qualifiedTypeName().startsWith("org.jspresso")) {
            typeBuff.append(createHtmlLink(hyphenateCamelCase(typeArguments[i].simpleTypeName()), computeJavadocUrl(
                typeArguments[i].qualifiedTypeName())));
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
          typeBuff.append(createHtmlLink(hyphenateCamelCase(param.type().simpleTypeName()), computeJavadocUrl(
              param.type().qualifiedTypeName())));
        } else {
          typeBuff.append(hyphenateCamelCase(param.type().simpleTypeName()));
        }
      }
      writeLine("<td align=\"left\"><p><strong>" + propEntry.getKey() + "</strong></p><p><code>" + typeBuff.toString()
              + "</code></p></td>");
      writeLine("<td><p>" + javadocToDoc(propEntry.getValue().commentText()) + "</p></td>");
      writeLine("</tr>");
    }
    if (!atleastOneRow) {
      writeLine("<tr>");
      writeLine("<td align=\"left\">This class does not have any specific property.</td>");
      writeLine("<td align=\"left\"></td>");
      writeLine("</tr>");
    }
    writeLine("</tbody>");
    writeLine("</table>");
    writeLine("");
    writeLine("---");
  }

  private String createLink(String linkText, String linkEnd) {
    return "[`" + linkText + "`](" + linkEnd + ")";
  }

  private String createHtmlLink(String linkText, String linkEnd) {
    return "<a href=\"" + linkEnd + "\">" + linkText + "</a>";
  }

  /**
   * Gets output extension.
   *
   * @return the output extension
   */
  @Override
  protected String getOutputExtension() {
    return ".md";
  }

}
