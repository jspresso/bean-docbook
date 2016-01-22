/*
 * Copyright (c) 2005-2016 Vincent Vandenschrick. All rights reserved.
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
import com.sun.javadoc.RootDoc;

/**
 * A simple doclet to generate documentation from javadoc information.
 *
 * @author Vincent Vandenschrick
 * @version $LastChangedRevision: 3701 $
 */
public abstract class AbstractBeanDoclet {

  private static ThreadLocal<AbstractBeanDoclet> threadInstance = new ThreadLocal<AbstractBeanDoclet>();
  private String             rootClassName;
  private Collection<String> excludedSubtrees;
  private Collection<String> includedPackages;
  private int                maxDepth;
  private String             apidocUrl;
  private String             outputDir;
  private int                treeDepth;
  private Map<String, Map<String, String>> configSets = new HashMap<String, Map<String, String>>();
  /**
   * The Writer.
   */
  protected Writer writer;

  /**
   * Sets config set.
   *
   * @param configSet
   *     the config set
   */
  protected void setupConfigSet(Map<String, String> configSet) {
    rootClassName = configSet.get("rootClassName");
    if (configSet.containsKey("maxDepth")) {
      maxDepth = Integer.parseInt(configSet.get("maxDepth"));
    } else {
      maxDepth = -1;
    }
    if (configSet.containsKey("includedPackages")) {
      includedPackages = new HashSet<String>(Arrays.asList(configSet.get("includedPackages").split(":")));
    } else {
      includedPackages = null;
    }
    if (configSet.containsKey("excludedSubtrees")) {
      excludedSubtrees = new HashSet<String>(Arrays.asList(configSet.get("excludedSubtrees").split(":")));
    } else {
      excludedSubtrees = new HashSet<String>();
    }
    treeDepth = 0;
    writer = null;
  }

  /**
   * Generate docbook part documenting beans.
   *
   * @param root
   *     the root doc.
   * @return true if successful.
   */
  protected static boolean start(RootDoc root) {
    AbstractBeanDoclet doclet = threadInstance.get();
    doclet.readOptions(root.options());
    for (Map<String, String> configSet : doclet.configSets.values()) {
      doclet.setupConfigSet(configSet);
      Map<String, ClassTree> classTrees = new LinkedHashMap<String, ClassTree>();
      ClassTree rootClassTree = null;
      ClassDoc[] classes = root.classes();
      try {
        File f = new File(doclet.outputDir, doclet.rootClassName.substring(doclet.rootClassName.lastIndexOf(".") + 1)
            + doclet.getOutputExtension());
        f.getParentFile().mkdirs();
        doclet.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        doclet.writeHeader();
        for (ClassDoc classDoc : classes) {
          if (classDoc.isPublic() && classDoc.isClass()) {
            ClassTree classTree = new ClassTree(classDoc);
            if (doclet.rootClassName.equals(classDoc.qualifiedName())) {
              rootClassTree = classTree;
            }
            classTrees.put(classDoc.qualifiedTypeName(), classTree);
          }
        }
        if (rootClassTree != null) {
          for (Map.Entry<String, ClassTree> entry : classTrees.entrySet()) {
            ClassTree parent = classTrees.get(entry.getValue().getRoot().superclassType().qualifiedTypeName());
            if (parent != null) {
              parent.getSubclasses().add(entry.getValue());
            }
          }
          doclet.writeRootSection(rootClassTree);
        }
        doclet.writer.flush();
        doclet.writer.close();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return true;
  }

  /**
   * Gets output extension.
   *
   * @return the output extension
   */
  protected abstract String getOutputExtension();

  /**
   * Write root section.
   *
   * @param rootClassTree
   *     the root class tree
   * @throws IOException
   *     the iO exception
   */
  protected abstract void writeRootSection(ClassTree rootClassTree) throws IOException;

  /**
   * Write header.
   *
   * @throws IOException
   *     the iO exception
   */
  protected void writeHeader() throws IOException {
    // Empty default implementation
  }

  /**
   * Process class tree.
   *
   * @param classTree
   *     the class tree
   * @throws IOException
   *     the iO exception
   */
  protected void processClassTree(ClassTree classTree) throws IOException {
    ClassDoc classDoc = classTree.getRoot();
    // boolean childInSection = classTree.getSubclasses().size() > 1;
    boolean childInSection = false;
    if (!isInternalOrDeprecated(classDoc)) {
      writeClassSection(classTree, classDoc);
      if (!childInSection) {
        closeClassSection();
      }
    }
    if (maxDepth < 0 || treeDepth < maxDepth) {
      treeDepth++;
      List<ClassTree> children = new ArrayList<ClassTree>(classTree.getSubclasses());
      Collections.sort(children);
      for (ClassTree subclassTree : children) {
        if (shouldTreeBeDocumented(subclassTree.getRoot())) {
          processClassTree(subclassTree);
        }
      }
      treeDepth--;
    }
    if (!isInternalOrDeprecated(classDoc)) {
      if (childInSection) {
        closeClassSection();
      }
    }
  }

  /**
   * Close class section.
   *
   * @throws IOException
   *     the iO exception
   */
  protected void closeClassSection() throws IOException {
    // Empty default implementation
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
  protected abstract void writeClassSection(ClassTree classTree, ClassDoc classDoc)
      throws IOException;

  /**
   * Is internal or deprecated.
   *
   * @param classDoc the class doc
   * @return the boolean
   */
  protected boolean isInternalOrDeprecated(ClassDoc classDoc) {
    return classDoc.tags("@internal").length > 0 || classDoc.tags("@deprecated").length > 0;
  }

  /**
   * Should tree be documented.
   *
   * @param classDoc the class doc
   * @return the boolean
   */
  protected boolean shouldTreeBeDocumented(ClassDoc classDoc) {
    // handled individually for each class.
    // if (isInternalOrDeprecated(classDoc)) {
    // return false;
    // }
    if (excludedSubtrees.contains(classDoc.qualifiedTypeName())) {
      return false;
    }
    if (includedPackages == null) {
      return true;
    }
    String pack = classDoc.containingPackage().name();
    for (String includedPackage : includedPackages) {
      if (pack.contains(includedPackage)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Process class doc.
   *
   * @param classTree
   *     the class tree
   * @throws IOException
   *     the iO exception
   */
  protected abstract void processClassDoc(ClassTree classTree) throws IOException;

  /**
   * Is setter for ref doc.
   *
   * @param methodDoc
   *     the method doc
   * @return the boolean
   */
  protected boolean isSetterForRefDoc(MethodDoc methodDoc) {
    return methodDoc.isPublic() && isSetter(methodDoc) && methodDoc.tags("@internal").length == 0 && methodDoc.tags(
        "@deprecated").length == 0;
  }

  /**
   * Hyphenate dotted string.
   *
   * @param source
   *     the source
   * @return the string
   */
  protected String hyphenateDottedString(String source) {
    return source.replace(".", "&#x200B;.");
  }

  /**
   * Hyphenate camel case.
   *
   * @param source
   *     the source
   * @return the string
   */
  protected String hyphenateCamelCase(String source) {
    StringBuilder buff = new StringBuilder();
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

  /**
   * Compute javadoc url.
   *
   * @param qualifiedName
   *     the qualified name
   * @return the string
   */
  protected String computeJavadocUrl(String qualifiedName) {
    return apidocUrl + "/" + qualifiedName.replace(".", "/") + ".html";
  }

  /**
   * Javadoc to doc.
   *
   * @param source
   *     the source
   * @return the string
   */
  protected String javadocToDoc(String source) {
    return source;
  }

  /**
   * Gets property.
   *
   * @param methodDoc
   *     the method doc
   * @return the property
   */
  protected String getProperty(MethodDoc methodDoc) {
    return methodDoc.name().substring(3, 4).toLowerCase() + methodDoc.name().substring(4);
  }

  /**
   * Is setter.
   *
   * @param methodDoc
   *     the method doc
   * @return the boolean
   */
  protected boolean isSetter(MethodDoc methodDoc) {
    return methodDoc.name().startsWith("set");
  }

  /**
   * Write line.
   *
   * @param content
   *     the content
   * @throws IOException
   *     the iO exception
   */
  protected void writeLine(String content) throws IOException {
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

  private void readOptions(String[][] options) {
    for (String[] opt : options) {
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
   * Mandatory for custom options.
   *
   * @param option
   *     the custom option.
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

  /**
   * Sets thread instance.
   *
   * @param doclet
   *     the doclet
   */
  protected static void setThreadInstance(AbstractBeanDoclet doclet) {
    threadInstance.set(doclet);
  }
}
