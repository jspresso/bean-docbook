/*
 * Copyright (c) 2005-2010 Vincent Vandenschrick. All rights reserved.
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

import java.util.LinkedHashSet;
import java.util.Set;

import com.sun.javadoc.ClassDoc;

/**
 * Data structure to represent a class tree.
 * 
 * @version $LastChangedRevision$
 * @author Vincent Vandenschrick
 */
public class ClassTree implements Comparable<ClassTree> {

  private ClassDoc       root;
  private Set<ClassTree> subclasses;

  /**
   * Constructs a new <code>ClassTree</code> instance.
   * 
   * @param root
   *          the root of this class tree.
   */
  public ClassTree(ClassDoc root) {
    this.root = root;
    this.subclasses = new LinkedHashSet<ClassTree>();
  }

  /**
   * Gets the root.
   * 
   * @return the root.
   */
  public ClassDoc getRoot() {
    return root;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return root.toString();
  }

  /**
   * Gets the subclasses.
   * 
   * @return the subclasses.
   */
  public Set<ClassTree> getSubclasses() {
    return subclasses;
  }

  /**
   * Comparison based on root simple class name.
   * <p>
   * {@inheritDoc}
   */
  public int compareTo(ClassTree another) {
    return getRoot().simpleTypeName().compareToIgnoreCase(
        another.getRoot().simpleTypeName());
  }
}
