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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

  private static String                           rootClassName;
  private static Collection<String>               excludedSubtrees;
  private static Collection<String>               includedPackages;
  private static int                              maxDepth;
  private static String                           apidocUrl;
  private static String                           outputDir;
  private static int                              indent;
  private static int                              treeDepth;
  private static Writer                           writer;

  private static Map<String, Map<String, String>> configSets = new HashMap<String, Map<String, String>>();

  private static void setupConfigSet(Map<String, String> configSet) {
    rootClassName = configSet.get("rootClassName");
    if (configSet.containsKey("maxDepth")) {
      maxDepth = Integer.parseInt(configSet.get("maxDepth"));
    } else {
      maxDepth = -1;
    }
    if (configSet.containsKey("includedPackages")) {
      includedPackages = new HashSet<String>(Arrays.asList(configSet.get(
          "includedPackages").split(":")));
    } else {
      includedPackages = null;
    }
    if (configSet.containsKey("excludedSubtrees")) {
      excludedSubtrees = new HashSet<String>(Arrays.asList(configSet.get(
          "excludedSubtrees").split(":")));
    } else {
      excludedSubtrees = new HashSet<String>();
    }
    indent = 0;
    treeDepth = 0;
    writer = null;
  }

  /**
   * Generate docbook part documenting beans.
   * 
   * @param root
   *          the root doc.
   * @return true if succesful.
   */
  public static boolean start(RootDoc root) {
    readOptions(root.options());
    for (Map<String, String> configSet : configSets.values()) {
      setupConfigSet(configSet);
      Map<String, ClassTree> classTrees = new LinkedHashMap<String, ClassTree>();
      ClassTree rootClassTree = null;
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
          writeLine("<section>");
          writeLine("<title>Reference for " + rootClassTree.getRoot().name()
              + " hierarchy</title>");
          indent++;
          writeLine("<para></para>");
          processClassTree(rootClassTree);
          indent--;
          writeLine("</section>");
        }
        writer.flush();
        writer.close();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
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
    processClassDoc(classTree);
    // boolean childInSection = classTree.getSubclasses().size() > 1;
    boolean childInSection = false;
    if (!childInSection) {
      writeLine("<para></para>");
      writeLine("<para></para>");
      indent--;
      writeLine("</section>");
    }
    if (maxDepth < 0 || treeDepth < maxDepth) {
      treeDepth++;
      List<ClassTree> children = new ArrayList<ClassTree>(classTree
          .getSubclasses());
      Collections.sort(children);
      for (ClassTree subclassTree : children) {
        if (shouldBeDocumented(subclassTree.getRoot())) {
          processClassTree(subclassTree);
        }
      }
      treeDepth--;
    }
    if (childInSection) {
      writeLine("<para></para>");
      writeLine("<para></para>");
      indent--;
      writeLine("</section>");
    }
  }

  private static boolean shouldBeDocumented(ClassDoc classDoc) {
    if (excludedSubtrees.contains(classDoc.qualifiedTypeName())) {
      return false;
    }
    if (includedPackages == null) {
      return true;
    }
    String pack = classDoc.containingPackage().name();
    for (String includedPackage : includedPackages) {
      if (pack.indexOf(includedPackage) > 0) {
        return true;
      }
    }
    return false;
  }

  private static void processClassDoc(ClassTree classTree) throws IOException {
    ClassDoc classDoc = classTree.getRoot();
    writeLine("<title>" + classDoc.name() + "</title>");
    // writeLine("<para>");
    // writeLine("<?dbfo keep-with-next='always'?>");
    // writeLine("</para>");
    writeLine("<para>" + javadocToDocbook(classDoc.commentText()) + "</para>");
    writeLine("<itemizedlist>");
    indent++;
    writeLine("<listitem>");
    indent++;
    writeLine("<para><emphasis role='bold'>Full name</emphasis> : <code><ulink url='"
        + computeJavadocUrl(classDoc.qualifiedTypeName())
        + "'>"
        + hyphenateDottedString(classDoc.qualifiedTypeName())
        + "</ulink></code></para>");
    indent--;
    writeLine("</listitem>");
    if (classDoc.superclassType().qualifiedTypeName()
        .startsWith("org.jspresso")) {
      writeLine("<listitem>");
      indent++;
      writeLine("<para><emphasis role='bold'>Supertype</emphasis> : <code><link linkend='"
          + classDoc.superclassType().qualifiedTypeName()
          + "'>"
          + classDoc.superclass().name() + "</link></code></para>");
      indent--;
      writeLine("</listitem>");
    }
    if (classTree.getSubclasses().size() > 0) {
      writeLine("<listitem>");
      indent++;
      StringBuffer buff = new StringBuffer();
      List<ClassTree> children = new ArrayList<ClassTree>(classTree
          .getSubclasses());
      Collections.sort(children);
      int i = 0;
      for (ClassTree subclassTree : children) {
        i++;
        buff.append("<code><link linkend='"
            + subclassTree.getRoot().qualifiedTypeName() + "'>"
            + subclassTree.getRoot().name() + "</link></code>");
        if (i < children.size()) {
          buff.append(", ");
        }
      }
      writeLine("<para><emphasis role='bold'>Subtypes</emphasis> : "
          + buff.toString() + "</para>");
      indent--;
      writeLine("</listitem>");
    }
    indent--;
    writeLine("</itemizedlist>");
    writeLine("<para></para>");
    writeLine("<table colsep='0' rowsep='1' tabstyle='splitable' frame='topbot'>");
    writeLine("<?dbfo keep-together='auto'?>");
    indent++;
    writeLine("<title>" + classDoc.name() + " properties</title>");
    writeLine("<tgroup cols='3'>");
    indent++;
    writeLine("<colspec colname='name' colwidth='1*' />");
    writeLine("<colspec colname='type' colwidth='1*' />");
    writeLine("<colspec colname='description' colwidth='2*' />");
    writeLine("<thead>");
    indent++;
    writeLine("<row>");
    indent++;
    writeLine("<entry align='left'>Name</entry>");
    writeLine("<entry align='left'>Type</entry>");
    writeLine("<entry align='left'>Description</entry>");
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
          if (param.type().qualifiedTypeName().startsWith("org.jspresso")) {
            buff.append("<ulink url='"
                + computeJavadocUrl(param.type().qualifiedTypeName()) + "'>"
                + hyphenateCamelCase(param.type().simpleTypeName())
                + "</ulink>");
          } else {
            buff.append(hyphenateCamelCase(param.type().simpleTypeName()));
          }
          buff.append("&#x200B;&lt;&#x200B;");
          for (int i = 0; i < typeArguments.length; i++) {
            if (typeArguments[i].qualifiedTypeName().startsWith("org.jspresso")) {
              buff.append("<ulink url='"
                  + computeJavadocUrl(typeArguments[i].qualifiedTypeName())
                  + "'>"
                  + hyphenateCamelCase(typeArguments[i].simpleTypeName())
                  + "</ulink>");
            } else {
              buff.append(typeArguments[i].simpleTypeName());
            }
            if (i < typeArguments.length - 1) {
              buff.append("&#x200B;,");
            }
          }
          buff.append("&#x200B;&gt;&#x200B;");
          writeLine("<entry><code>" + buff.toString() + "</code></entry>");
        } else {
          if (param.type().qualifiedTypeName().startsWith("org.jspresso")) {
            writeLine("<entry><code><ulink url='"
                + computeJavadocUrl(param.type().qualifiedTypeName()) + "'>"
                + hyphenateCamelCase(param.type().simpleTypeName())
                + "</ulink></code></entry>");
          } else {
            writeLine("<entry><code>"
                + hyphenateCamelCase(param.type().simpleTypeName())
                + "</code></entry>");
          }
        }
        writeLine("<entry><para>" + javadocToDocbook(methodDoc.commentText())
            + "</para></entry>");
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

  private static String hyphenateCamelCase(String source) {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < source.length() - 1; i++) {
      char c1 = source.charAt(i);
      char c2 = source.charAt(i + 1);
      buff.append(c1);
      if (Character.isLowerCase(c1) && Character.isUpperCase(c2)) {
        buff.append("&#x200B;");
      }
    }
    buff.append(source.charAt(source.length() - 1));
    return buff.toString();
  }

  private static String computeJavadocUrl(String qualifiedName) {
    return apidocUrl + "/" + qualifiedName.replace(".", "/") + ".html";
  }

  private static String javadocToDocbook(String source) {
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
    return dbSource;
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
      String[] splittedOpt = opt[0].split("_");
      String optionName = splittedOpt[0];

      Map<String, String> configSet = null;
      if (splittedOpt.length > 1) {
        String configName = splittedOpt[1];
        configSet = configSets.get(configName);
        if (configSet == null) {
          configSet = new HashMap<String, String>();
          configSets.put(configName, configSet);
        }
      }
      if (optionName.equals("-outputDir")) {
        outputDir = opt[1];
      } else if (optionName.equals("-apidocUrl")) {
        apidocUrl = opt[1];
      } else if (configSet != null) {
        if (optionName.equals("-rootClassName")) {
          configSet.put("rootClassName", opt[1]);
        } else if (optionName.equals("-maxDepth")) {
          configSet.put("maxDepth", opt[1]);
        } else if (optionName.equals("-excludedSubtrees")) {
          configSet.put("excludedSubtrees", opt[1]);
        } else if (optionName.equals("-includedPackages")) {
          configSet.put("includedPackages", opt[1]);
        }
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
    if (option.startsWith("-rootClassName")) {
      return 2;
    } else if (option.startsWith("-maxDepth")) {
      return 2;
    } else if (option.startsWith("-excludedSubtrees")) {
      return 2;
    } else if (option.startsWith("-includedPackages")) {
      return 2;
    } else if (option.equals("-outputDir")) {
      return 2;
    } else if (option.equals("-apidocUrl")) {
      return 2;
    }
    return 0;
  }

}
