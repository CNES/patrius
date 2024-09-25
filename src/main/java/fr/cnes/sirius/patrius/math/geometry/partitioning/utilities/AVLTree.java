/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.partitioning.utilities;

//CHECKSTYLE: stop ModifiedControlVariable check
//CHECKSTYLE: stop CommentRatio check
//Reason: Commons-Math code kept as such

/**
 * This class implements AVL trees.
 * 
 * <p>
 * The purpose of this class is to sort elements while allowing duplicate elements (i.e. such that {@code a.equals(b)}
 * is true). The {@code SortedSet} interface does not allow this, so a specific class is needed. Null elements are not
 * allowed.
 * </p>
 * 
 * <p>
 * Since the {@code equals} method is not sufficient to differentiate elements, the {@link #delete delete} method is
 * implemented using the equality operator.
 * </p>
 * 
 * <p>
 * In order to clearly mark the methods provided here do not have the same semantics as the ones specified in the
 * {@code SortedSet} interface, different names are used ({@code add} has been replaced by {@link #insert insert} and
 * {@code remove} has been replaced by {@link #delete
 * delete}).
 * </p>
 * 
 * <p>
 * This class is based on the C implementation Georg Kraml has put in the public domain. Unfortunately, his <a
 * href="www.purists.org/georg/avltree/index.html">page</a> seems not to exist any more.
 * </p>
 * 
 * @param <T>
 *        the type of the elements
 * 
 * @version $Id: AVLTree.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class AVLTree<T extends Comparable<T>> {

    /** Top level node. */
    private Node top;

    /**
     * Build an empty tree.
     */
    public AVLTree() {
        this.top = null;
    }

    /**
     * Insert an element in the tree.
     * 
     * @param element
     *        element to insert (silently ignored if null)
     */
    public void insert(final T element) {
        if (element != null) {
            if (this.top == null) {
                this.top = new Node(element, null);
            } else {
                this.top.insert(element);
            }
        }
    }

    /**
     * Delete an element from the tree.
     * <p>
     * The element is deleted only if there is a node {@code n} containing exactly the element instance specified, i.e.
     * for which {@code n.getElement() == element}. This is purposely <em>different</em> from the specification of the
     * {@code java.util.Set} {@code remove} method (in fact, this is the reason why a specific class has been
     * developed).
     * </p>
     * 
     * @param element
     *        element to delete (silently ignored if null)
     * @return true if the element was deleted from the tree
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public boolean delete(final T element) {
        // CHECKSTYLE: resume ReturnCount check
        if (element != null) {
            for (Node node = this.getNotSmaller(element); node != null; node = node.getNext()) {
                // loop over all elements neither smaller nor larger
                // than the specified one
                if (node.element == element) {
                    node.delete();
                    return true;
                } else if (node.element.compareTo(element) > 0) {
                    // all the remaining elements are known to be larger,
                    // the element is not in the tree
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Check if the tree is empty.
     * 
     * @return true if the tree is empty
     */
    public boolean isEmpty() {
        return this.top == null;
    }

    /**
     * Get the number of elements of the tree.
     * 
     * @return number of elements contained in the tree
     */
    public int size() {
        return (this.top == null) ? 0 : this.top.size();
    }

    /**
     * Get the node whose element is the smallest one in the tree.
     * 
     * @return the tree node containing the smallest element in the tree
     *         or null if the tree is empty
     * @see #getLargest
     * @see #getNotSmaller
     * @see #getNotLarger
     * @see Node#getPrevious
     * @see Node#getNext
     */
    public Node getSmallest() {
        return (this.top == null) ? null : this.top.getSmallest();
    }

    /**
     * Get the node whose element is the largest one in the tree.
     * 
     * @return the tree node containing the largest element in the tree
     *         or null if the tree is empty
     * @see #getSmallest
     * @see #getNotSmaller
     * @see #getNotLarger
     * @see Node#getPrevious
     * @see Node#getNext
     */
    public Node getLargest() {
        return (this.top == null) ? null : this.top.getLargest();
    }

    /**
     * Get the node whose element is not smaller than the reference object.
     * 
     * @param reference
     *        reference object (may not be in the tree)
     * @return the tree node containing the smallest element not smaller
     *         than the reference object or null if either the tree is empty or
     *         all its elements are smaller than the reference object
     * @see #getSmallest
     * @see #getLargest
     * @see #getNotLarger
     * @see Node#getPrevious
     * @see Node#getNext
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public Node getNotSmaller(final T reference) {
        // CHECKSTYLE: resume ReturnCount check
        Node candidate = null;
        for (Node node = this.top; node != null;) {
            if (node.element.compareTo(reference) < 0) {
                if (node.right == null) {
                    return candidate;
                }
                node = node.right;
            } else {
                candidate = node;
                if (node.left == null) {
                    return candidate;
                }
                node = node.left;
            }
        }
        return null;
    }

    /**
     * Get the node whose element is not larger than the reference object.
     * 
     * @param reference
     *        reference object (may not be in the tree)
     * @return the tree node containing the largest element not larger
     *         than the reference object (in which case the node is guaranteed
     *         not to be empty) or null if either the tree is empty or all its
     *         elements are larger than the reference object
     * @see #getSmallest
     * @see #getLargest
     * @see #getNotSmaller
     * @see Node#getPrevious
     * @see Node#getNext
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public Node getNotLarger(final T reference) {
        // CHECKSTYLE: resume ReturnCount check
        Node candidate = null;
        for (Node node = this.top; node != null;) {
            if (node.element.compareTo(reference) > 0) {
                if (node.left == null) {
                    return candidate;
                }
                node = node.left;
            } else {
                candidate = node;
                if (node.right == null) {
                    return candidate;
                }
                node = node.right;
            }
        }
        return null;
    }

    /** Enum for tree skew factor. */
    private static enum Skew {
        /** Code for left high trees. */
        LEFT_HIGH,

        /** Code for right high trees. */
        RIGHT_HIGH,

        /** Code for Skew.BALANCED trees. */
        BALANCED;
    }

    /**
     * This class implements AVL trees nodes.
     * <p>
     * AVL tree nodes implement all the logical structure of the tree. Nodes are created by the {@link AVLTree AVLTree}
     * class.
     * </p>
     * <p>
     * The nodes are not independant from each other but must obey specific balancing constraints and the tree structure
     * is rearranged as elements are inserted or deleted from the tree. The creation, modification and tree-related
     * navigation methods have therefore restricted access. Only the order-related navigation, reading and delete
     * methods are public.
     * </p>
     * 
     * @see AVLTree
     */
    @SuppressWarnings("PMD.ShortClassName")
    public class Node {

        /** Element contained in the current node. */
        private T element;

        /** Left sub-tree. */
        private Node left;

        /** Right sub-tree. */
        private Node right;

        /** Parent tree. */
        private Node parent;

        /** Skew factor. */
        private Skew skew;

        /**
         * Build a node for a specified element.
         * 
         * @param elementIn
         *        element
         * @param parentIn
         *        parent node
         */
        Node(final T elementIn, final Node parentIn) {
            this.element = elementIn;
            this.left = null;
            this.right = null;
            this.parent = parentIn;
            this.skew = Skew.BALANCED;
        }

        /**
         * Get the contained element.
         * 
         * @return element contained in the node
         */
        public T getElement() {
            return this.element;
        }

        /**
         * Get the number of elements of the tree rooted at this node.
         * 
         * @return number of elements contained in the tree rooted at this node
         */
        private int size() {
            return 1 + ((this.left == null) ? 0 : this.left.size()) + ((this.right == null) ? 0 : this.right.size());
        }

        /**
         * Get the node whose element is the smallest one in the tree
         * rooted at this node.
         * 
         * @return the tree node containing the smallest element in the
         *         tree rooted at this node or null if the tree is empty
         * @see #getLargest
         */
        private Node getSmallest() {
            Node node = this;
            while (node.left != null) {
                node = node.left;
            }
            return node;
        }

        /**
         * Get the node whose element is the largest one in the tree
         * rooted at this node.
         * 
         * @return the tree node containing the largest element in the
         *         tree rooted at this node or null if the tree is empty
         * @see #getSmallest
         */
        private Node getLargest() {
            Node node = this;
            while (node.right != null) {
                node = node.right;
            }
            return node;
        }

        /**
         * Get the node containing the next smaller or equal element.
         * 
         * @return node containing the next smaller or equal element or
         *         null if there is no smaller or equal element in the tree
         * @see #getNext
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        public Node getPrevious() {
            // CHECKSTYLE: resume ReturnCount check

            if (this.left != null) {
                final Node node = this.left.getLargest();
                if (node != null) {
                    return node;
                }
            }

            for (Node node = this; node.parent != null; node = node.parent) {
                if (node != node.parent.left) {
                    return node.parent;
                }
            }

            return null;

        }

        /**
         * Get the node containing the next larger or equal element.
         * 
         * @return node containing the next larger or equal element (in
         *         which case the node is guaranteed not to be empty) or null if
         *         there is no larger or equal element in the tree
         * @see #getPrevious
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        public Node getNext() {
            // CHECKSTYLE: resume ReturnCount check

            if (this.right != null) {
                final Node node = this.right.getSmallest();
                if (node != null) {
                    return node;
                }
            }

            for (Node node = this; node.parent != null; node = node.parent) {
                if (node != node.parent.right) {
                    return node.parent;
                }
            }

            return null;

        }

        /**
         * Insert an element in a sub-tree.
         * 
         * @param newElement
         *        element to insert
         * @return true if the parent tree should be re-Skew.BALANCED
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        private boolean insert(final T newElement) {
            // CHECKSTYLE: resume ReturnCount check
            if (newElement.compareTo(this.element) < 0) {
                // the inserted element is smaller than the node
                if (this.left == null) {
                    this.left = new Node(newElement, this);
                    return this.rebalanceLeftGrown();
                }
                return this.left.insert(newElement) ? this.rebalanceLeftGrown() : false;
            }

            // the inserted element is equal to or greater than the node
            if (this.right == null) {
                this.right = new Node(newElement, this);
                return this.rebalanceRightGrown();
            }
            return this.right.insert(newElement) ? this.rebalanceRightGrown() : false;

        }

        /**
         * Delete the node from the tree.
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Commons-Math code kept as such
        public void delete() {
            // CHECKSTYLE: resume CyclomaticComplexity check
            if ((this.parent == null) && (this.left == null) && (this.right == null)) {
                // this was the last node, the tree is now empty
                this.element = null;
                AVLTree.this.top = null;
            } else {

                Node node;
                final Node child;
                boolean leftShrunk;
                if ((this.left == null) && (this.right == null)) {
                    node = this;
                    this.element = null;
                    leftShrunk = node == node.parent.left;
                    child = null;
                } else {
                    node = (this.left == null) ? this.right.getSmallest() : this.left.getLargest();
                    this.element = node.element;
                    leftShrunk = node == node.parent.left;
                    child = (node.left == null) ? node.right : node.left;
                }

                node = node.parent;
                if (leftShrunk) {
                    node.left = child;
                } else {
                    node.right = child;
                }
                if (child != null) {
                    child.parent = node;
                }

                while (leftShrunk ? node.rebalanceLeftShrunk() : node.rebalanceRightShrunk()) {
                    if (node.parent == null) {
                        return;
                    }
                    leftShrunk = node == node.parent.left;
                    node = node.parent;
                }

            }
        }

        /**
         * Re-balance the instance as left sub-tree has grown.
         * 
         * @return true if the parent tree should be reSkew.BALANCED too
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        private boolean rebalanceLeftGrown() {
            // CHECKSTYLE: resume ReturnCount check
            switch (this.skew) {
                case LEFT_HIGH:
                    if (this.left.skew == Skew.LEFT_HIGH) {
                        this.rotateCW();
                        this.skew = Skew.BALANCED;
                        this.right.skew = Skew.BALANCED;
                    } else {
                        final Skew s = this.left.right.skew;
                        this.left.rotateCCW();
                        this.rotateCW();
                        switch (s) {
                            case LEFT_HIGH:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.RIGHT_HIGH;
                                break;
                            case RIGHT_HIGH:
                                this.left.skew = Skew.LEFT_HIGH;
                                this.right.skew = Skew.BALANCED;
                                break;
                            default:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.BALANCED;
                                break;
                        }
                        this.skew = Skew.BALANCED;
                    }
                    return false;
                case RIGHT_HIGH:
                    this.skew = Skew.BALANCED;
                    return false;
                default:
                    this.skew = Skew.LEFT_HIGH;
                    return true;
            }
        }

        /**
         * Re-balance the instance as right sub-tree has grown.
         * 
         * @return true if the parent tree should be reSkew.BALANCED too
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        private boolean rebalanceRightGrown() {
            // CHECKSTYLE: resume ReturnCount check
            switch (this.skew) {
                case LEFT_HIGH:
                    this.skew = Skew.BALANCED;
                    return false;
                case RIGHT_HIGH:
                    if (this.right.skew == Skew.RIGHT_HIGH) {
                        this.rotateCCW();
                        this.skew = Skew.BALANCED;
                        this.left.skew = Skew.BALANCED;
                    } else {
                        final Skew s = this.right.left.skew;
                        this.right.rotateCW();
                        this.rotateCCW();
                        switch (s) {
                            case LEFT_HIGH:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.RIGHT_HIGH;
                                break;
                            case RIGHT_HIGH:
                                this.left.skew = Skew.LEFT_HIGH;
                                this.right.skew = Skew.BALANCED;
                                break;
                            default:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.BALANCED;
                                break;
                        }
                        this.skew = Skew.BALANCED;
                    }
                    return false;
                default:
                    this.skew = Skew.RIGHT_HIGH;
                    return true;
            }
        }

        /**
         * Re-balance the instance as left sub-tree has shrunk.
         * 
         * @return true if the parent tree should be reSkew.BALANCED too
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        private boolean rebalanceLeftShrunk() {
            // CHECKSTYLE: resume ReturnCount check
            switch (this.skew) {
                case LEFT_HIGH:
                    this.skew = Skew.BALANCED;
                    return true;
                case RIGHT_HIGH:
                    if (this.right.skew == Skew.RIGHT_HIGH) {
                        this.rotateCCW();
                        this.skew = Skew.BALANCED;
                        this.left.skew = Skew.BALANCED;
                        return true;
                    } else if (this.right.skew == Skew.BALANCED) {
                        this.rotateCCW();
                        this.skew = Skew.LEFT_HIGH;
                        this.left.skew = Skew.RIGHT_HIGH;
                        return false;
                    } else {
                        final Skew s = this.right.left.skew;
                        this.right.rotateCW();
                        this.rotateCCW();
                        switch (s) {
                            case LEFT_HIGH:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.RIGHT_HIGH;
                                break;
                            case RIGHT_HIGH:
                                this.left.skew = Skew.LEFT_HIGH;
                                this.right.skew = Skew.BALANCED;
                                break;
                            default:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.BALANCED;
                                break;
                        }
                        this.skew = Skew.BALANCED;
                        return true;
                    }
                default:
                    this.skew = Skew.RIGHT_HIGH;
                    return false;
            }
        }

        /**
         * Re-balance the instance as right sub-tree has shrunk.
         * 
         * @return true if the parent tree should be reSkew.BALANCED too
         */
        // CHECKSTYLE: stop ReturnCount check
        // Reason: Commons-Math code kept as such
        private boolean rebalanceRightShrunk() {
            // CHECKSTYLE: resume ReturnCount check
            switch (this.skew) {
                case RIGHT_HIGH:
                    this.skew = Skew.BALANCED;
                    return true;
                case LEFT_HIGH:
                    if (this.left.skew == Skew.LEFT_HIGH) {
                        this.rotateCW();
                        this.skew = Skew.BALANCED;
                        this.right.skew = Skew.BALANCED;
                        return true;
                    } else if (this.left.skew == Skew.BALANCED) {
                        this.rotateCW();
                        this.skew = Skew.RIGHT_HIGH;
                        this.right.skew = Skew.LEFT_HIGH;
                        return false;
                    } else {
                        final Skew s = this.left.right.skew;
                        this.left.rotateCCW();
                        this.rotateCW();
                        switch (s) {
                            case LEFT_HIGH:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.RIGHT_HIGH;
                                break;
                            case RIGHT_HIGH:
                                this.left.skew = Skew.LEFT_HIGH;
                                this.right.skew = Skew.BALANCED;
                                break;
                            default:
                                this.left.skew = Skew.BALANCED;
                                this.right.skew = Skew.BALANCED;
                                break;
                        }
                        this.skew = Skew.BALANCED;
                        return true;
                    }
                default:
                    this.skew = Skew.LEFT_HIGH;
                    return false;
            }
        }

        /**
         * Perform a clockwise rotation rooted at the instance.
         * <p>
         * The skew factor are not updated by this method, they <em>must</em> be updated by the caller
         * </p>
         */
        private void rotateCW() {

            final T tmpElt = this.element;
            this.element = this.left.element;
            this.left.element = tmpElt;

            final Node tmpNode = this.left;
            this.left = tmpNode.left;
            tmpNode.left = tmpNode.right;
            tmpNode.right = this.right;
            this.right = tmpNode;

            if (this.left != null) {
                this.left.parent = this;
            }
            if (this.right.right != null) {
                this.right.right.parent = this.right;
            }

        }

        /**
         * Perform a counter-clockwise rotation rooted at the instance.
         * <p>
         * The skew factor are not updated by this method, they <em>must</em> be updated by the caller
         * </p>
         */
        private void rotateCCW() {

            final T tmpElt = this.element;
            this.element = this.right.element;
            this.right.element = tmpElt;

            final Node tmpNode = this.right;
            this.right = tmpNode.right;
            tmpNode.right = tmpNode.left;
            tmpNode.left = this.left;
            this.left = tmpNode;

            if (this.right != null) {
                this.right.parent = this;
            }
            if (this.left.left != null) {
                this.left.left.parent = this.left;
            }

        }

    }

    // CHECKSTYLE: resume ModifiedControlVariable check
    // CHECKSTYLE: resume CommentRatio check
}
