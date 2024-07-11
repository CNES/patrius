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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * <p>
 * A variable length {@link DoubleArray} implementation that automatically handles expanding and contracting its
 * internal storage array as elements are added and removed.
 * </p>
 * <h3>Important note: Usage should not assume that this class is thread-safe even though some of the methods are
 * {@code synchronized}. This qualifier will be dropped in the next major release (4.0).</h3>
 * <p>
 * The internal storage array starts with capacity determined by the {@code initialCapacity} property, which can be set
 * by the constructor. The default initial capacity is 16. Adding elements using {@link #addElement(double)} appends
 * elements to the end of the array. When there are no open entries at the end of the internal storage array, the array
 * is expanded. The size of the expanded array depends on the {@code expansionMode} and {@code expansionFactor}
 * properties. The {@code expansionMode} determines whether the size of the array is multiplied by the
 * {@code expansionFactor} ({@link ExpansionMode#MULTIPLICATIVE}) or if the expansion is additive (
 * {@link ExpansionMode#ADDITIVE} -- {@code expansionFactor} storage locations added). The default {@code expansionMode}
 * is {@code MULTIPLICATIVE} and the default {@code expansionFactor} is 2.
 * </p>
 * <p>
 * The {@link #addElementRolling(double)} method adds a new element to the end of the internal storage array and adjusts
 * the "usable window" of the internal array forward by one position (effectively making what was the second element the
 * first, and so on). Repeated activations of this method (or activation of {@link #discardFrontElements(int)}) will
 * effectively orphan the storage locations at the beginning of the internal storage array. To reclaim this storage,
 * each time one of these methods is activated, the size of the internal storage array is compared to the number of
 * addressable elements (the {@code numElements} property) and if the difference is too large, the internal array is
 * contracted to size {@code numElements + 1}. The determination of when the internal storage array is "too large"
 * depends on the {@code expansionMode} and {@code contractionFactor} properties. If the {@code expansionMode} is
 * {@code MULTIPLICATIVE}, contraction is triggered when the ratio between storage array length and {@code numElements}
 * exceeds {@code contractionFactor.} If the {@code expansionMode} is {@code ADDITIVE}, the number of excess storage
 * locations is compared to {@code contractionFactor}.
 * </p>
 * <p>
 * To avoid cycles of expansions and contractions, the {@code expansionFactor} must not exceed the
 * {@code contractionFactor}. Constructors and mutators for both of these properties enforce this requirement, throwing
 * a {@code MathIllegalArgumentException} if it is violated.
 * </p>
 * 
 * @version $Id: ResizableDoubleArray.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class ResizableDoubleArray implements DoubleArray, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -3485529955529426875L;

    /** Default contraction criterion. */
    private static final double DEFAULT_CONTRACTION_CRITERION = 2.5;

    /** Default value for initial capacity. */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    /** Default value for array size modifier. */
    private static final double DEFAULT_EXPANSION_FACTOR = 2.0;
    /**
     * Default value for the difference between {@link #contractionCriterion} and {@link #expansionFactor}.
     */
    private static final double DEFAULT_CONTRACTION_DELTA = 0.5;

    /**
     * The contraction criteria determines when the internal array will be
     * contracted to fit the number of elements contained in the element
     * array + 1.
     */
    private double contractionCriterion = DEFAULT_CONTRACTION_CRITERION;

    /**
     * The expansion factor of the array. When the array needs to be expanded,
     * the new array size will be {@code internalArray.length * expansionFactor} if {@code expansionMode} is set to
     * MULTIPLICATIVE_MODE, or {@code internalArray.length + expansionFactor} if {@code expansionMode} is set to
     * ADDITIVE_MODE.
     */
    private double expansionFactor = 2.0;

    /**
     * Determines whether array expansion by {@code expansionFactor} is additive or multiplicative.
     */
    private ExpansionMode expansionMode = ExpansionMode.MULTIPLICATIVE;

    /**
     * The internal storage array.
     */
    private double[] internalArray;

    /**
     * The number of addressable elements in the array. Note that this
     * has nothing to do with the length of the internal storage array.
     */
    private int numElements = 0;

    /**
     * The position of the first addressable element in the internal storage
     * array. The addressable elements in the array are
     * {@code internalArray[startIndex],...,internalArray[startIndex + numElements - 1]}.
     */
    private int startIndex = 0;

    /**
     * Specification of expansion algorithm.
     * 
     * @since 3.1
     */
    public static enum ExpansionMode {
        /** Multiplicative expansion mode. */
        MULTIPLICATIVE,
        /** Additive expansion mode. */
        ADDITIVE
    }

    /**
     * Creates an instance with default properties.
     * <ul>
     * <li>{@code initialCapacity = 16}</li>
     * <li>{@code expansionMode = MULTIPLICATIVE}</li>
     * <li>{@code expansionFactor = 2.0}</li>
     * <li>{@code contractionCriterion = 2.5}</li>
     * </ul>
     */
    public ResizableDoubleArray() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates an instance with the specified initial capacity.
     * Other properties take default values:
     * <ul>
     * <li>{@code expansionMode = MULTIPLICATIVE}</li>
     * <li>{@code expansionFactor = 2.0}</li>
     * <li>{@code contractionCriterion = 2.5}</li>
     * </ul>
     * 
     * @param initialCapacity
     *        Initial size of the internal storage array.
     * @throws MathIllegalArgumentException
     *         if {@code initialCapacity <= 0}.
     */
    public ResizableDoubleArray(final int initialCapacity) {
        this(initialCapacity, DEFAULT_EXPANSION_FACTOR);
    }

    /**
     * Creates an instance from an existing {@code double[]} with the
     * initial capacity and numElements corresponding to the size of
     * the supplied {@code double[]} array.
     * If the supplied array is null, a new empty array with the default
     * initial capacity will be created.
     * The input array is copied, not referenced.
     * Other properties take default values:
     * <ul>
     * <li>{@code initialCapacity = 16}</li>
     * <li>{@code expansionMode = MULTIPLICATIVE}</li>
     * <li>{@code expansionFactor = 2.0}</li>
     * <li>{@code contractionCriterion = 2.5}</li>
     * </ul>
     * 
     * @param initialArray
     *        initial array
     * @since 2.2
     */
    public ResizableDoubleArray(final double[] initialArray) {
        this(DEFAULT_INITIAL_CAPACITY,
            DEFAULT_EXPANSION_FACTOR,
            DEFAULT_CONTRACTION_DELTA + DEFAULT_EXPANSION_FACTOR,
            ExpansionMode.MULTIPLICATIVE,
            initialArray);
    }

    /**
     * Creates an instance with the specified initial capacity
     * and expansion factor.
     * The remaining properties take default values:
     * <ul>
     * <li>{@code expansionMode = MULTIPLICATIVE}</li>
     * <li>{@code contractionCriterion = 0.5 + expansionFactor}</li>
     * </ul>
     * <br/>
     * Throws IllegalArgumentException if the following conditions are
     * not met:
     * <ul>
     * <li>{@code initialCapacity > 0}</li>
     * <li>{@code expansionFactor > 1}</li>
     * </ul>
     * 
     * @param initialCapacity
     *        Initial size of the internal storage array.
     * @param expansionFactorIn
     *        The array will be expanded based on this
     *        parameter.
     * @throws MathIllegalArgumentException
     *         if parameters are not valid.
     * @since 3.1
     */
    public ResizableDoubleArray(final int initialCapacity,
        final double expansionFactorIn) {
        this(initialCapacity,
            expansionFactorIn,
            DEFAULT_CONTRACTION_DELTA + expansionFactorIn);
    }

    /**
     * Creates an instance with the specified initial capacity,
     * expansion factor, and contraction criteria.
     * The expansion mode will default to {@code MULTIPLICATIVE}. <br/>
     * Throws IllegalArgumentException if the following conditions are
     * not met:
     * <ul>
     * <li>{@code initialCapacity > 0}</li>
     * <li>{@code expansionFactor > 1}</li>
     * <li>{@code contractionCriterion >= expansionFactor}</li>
     * </ul>
     * 
     * @param initialCapacity
     *        Initial size of the internal storage array..
     * @param expansionFactorIn
     *        The array will be expanded based on this
     *        parameter.
     * @param contractionCriterionIn
     *        Contraction criterion.
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid.
     * @since 3.1
     */
    public ResizableDoubleArray(final int initialCapacity,
        final double expansionFactorIn,
        final double contractionCriterionIn) {
        this(initialCapacity,
            expansionFactorIn,
            contractionCriterionIn,
            ExpansionMode.MULTIPLICATIVE,
            null);
    }

    /**
     * Creates an instance with the specified properties. <br/>
     * Throws MathIllegalArgumentException if the following conditions are
     * not met:
     * <ul>
     * <li>{@code initialCapacity > 0}</li>
     * <li>{@code expansionFactor > 1}</li>
     * <li>{@code contractionCriterion >= expansionFactor}</li>
     * </ul>
     * 
     * @param initialCapacity
     *        Initial size of the internal storage array.
     * @param expansionFactorIn
     *        The array will be expanded based on this
     *        parameter.
     * @param contractionCriterionIn
     *        Contraction criteria.
     * @param expansionModeIn
     *        Expansion mode.
     * @param data
     *        Initial contents of the array.
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid.
     */
    public ResizableDoubleArray(final int initialCapacity,
        final double expansionFactorIn,
        final double contractionCriterionIn,
        final ExpansionMode expansionModeIn,
        final double... data) {
        if (initialCapacity <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.INITIAL_CAPACITY_NOT_POSITIVE,
                initialCapacity);
        }
        this.checkContractExpand(contractionCriterionIn, expansionFactorIn);

        this.expansionFactor = expansionFactorIn;
        this.contractionCriterion = contractionCriterionIn;
        this.expansionMode = expansionModeIn;
        this.internalArray = new double[initialCapacity];
        this.numElements = 0;
        this.startIndex = 0;

        if (data != null) {
            this.addElements(data);
        }
    }

    /**
     * Copy constructor. Creates a new ResizableDoubleArray that is a deep,
     * fresh copy of the original. Needs to acquire synchronization lock
     * on original. Original may not be null; otherwise a {@link NullArgumentException} is thrown.
     * 
     * @param original
     *        array to copy
     * @exception NullArgumentException
     *            if original is null
     * @since 2.0
     */
    public ResizableDoubleArray(final ResizableDoubleArray original) {
        MathUtils.checkNotNull(original);
        copy(original, this);
    }

    /**
     * Adds an element to the end of this expandable array.
     * 
     * @param value
     *        Value to be added to end of array.
     */
    @Override
    public synchronized void addElement(final double value) {
        if (this.internalArray.length <= this.startIndex + this.numElements) {
            this.expand();
        }
        this.internalArray[this.startIndex + this.numElements++] = value;
    }

    /**
     * Adds several element to the end of this expandable array.
     * 
     * @param values
     *        Values to be added to end of array.
     * @since 2.2
     */
    @Override
    public synchronized void addElements(final double[] values) {
        final double[] tempArray = new double[this.numElements + values.length + 1];
        System.arraycopy(this.internalArray, this.startIndex, tempArray, 0, this.numElements);
        System.arraycopy(values, 0, tempArray, this.numElements, values.length);
        this.internalArray = tempArray;
        this.startIndex = 0;
        this.numElements += values.length;
    }

    /**
     * <p>
     * Adds an element to the end of the array and removes the first element in the array. Returns the discarded first
     * element. The effect is similar to a push operation in a FIFO queue.
     * </p>
     * <p>
     * Example: If the array contains the elements 1, 2, 3, 4 (in that order) and addElementRolling(5) is invoked, the
     * result is an array containing the entries 2, 3, 4, 5 and the value returned is 1.
     * </p>
     * 
     * @param value
     *        Value to be added to the array.
     * @return the value which has been discarded or "pushed" out of the array
     *         by this rolling insert.
     */
    @Override
    public synchronized double addElementRolling(final double value) {
        final double discarded = this.internalArray[this.startIndex];

        if ((this.startIndex + (this.numElements + 1)) > this.internalArray.length) {
            this.expand();
        }
        // Increment the start index
        this.startIndex += 1;

        // Add the new value
        this.internalArray[this.startIndex + (this.numElements - 1)] = value;

        // Check the contraction criterion.
        if (this.shouldContract()) {
            this.contract();
        }
        return discarded;
    }

    /**
     * Substitutes <code>value</code> for the most recently added value.
     * Returns the value that has been replaced. If the array is empty (i.e.
     * if {@link #numElements} is zero), an IllegalStateException is thrown.
     * 
     * @param value
     *        New value to substitute for the most recently added value
     * @return the value that has been replaced in the array.
     * @throws MathIllegalStateException
     *         if the array is empty
     * @since 2.0
     */
    public synchronized double substituteMostRecentElement(final double value) {
        if (this.numElements < 1) {
            throw new MathIllegalStateException(
                PatriusMessages.CANNOT_SUBSTITUTE_ELEMENT_FROM_EMPTY_ARRAY);
        }

        final int substIndex = this.startIndex + (this.numElements - 1);
        final double discarded = this.internalArray[substIndex];

        this.internalArray[substIndex] = value;

        return discarded;
    }

    /**
     * Checks the expansion factor and the contraction criterion and raises
     * an exception if the contraction criterion is smaller than the
     * expansion criterion.
     * 
     * @param contraction
     *        Criterion to be checked.
     * @param expansion
     *        Factor to be checked.
     * @throws NumberIsTooSmallException
     *         if {@code contraction < expansion}.
     * @throws NumberIsTooSmallException
     *         if {@code contraction <= 1}.
     * @throws NumberIsTooSmallException
     *         if {@code expansion <= 1 }.
     * @since 3.1
     */
    protected void checkContractExpand(final double contraction,
                                       final double expansion) {
        if (contraction < expansion) {
            final NumberIsTooSmallException e = new NumberIsTooSmallException(contraction, 1, true);
            e.getContext().addMessage(PatriusMessages.CONTRACTION_CRITERIA_SMALLER_THAN_EXPANSION_FACTOR,
                contraction, expansion);
            throw e;
        }

        if (contraction <= 1) {
            final NumberIsTooSmallException e = new NumberIsTooSmallException(contraction, 1, false);
            e.getContext().addMessage(PatriusMessages.CONTRACTION_CRITERIA_SMALLER_THAN_ONE,
                contraction);
            throw e;
        }

        if (expansion <= 1) {
            final NumberIsTooSmallException e = new NumberIsTooSmallException(contraction, 1, false);
            e.getContext().addMessage(PatriusMessages.EXPANSION_FACTOR_SMALLER_THAN_ONE,
                expansion);
            throw e;
        }
    }

    /**
     * Clear the array contents, resetting the number of elements to zero.
     */
    @Override
    public synchronized void clear() {
        this.numElements = 0;
        this.startIndex = 0;
    }

    /**
     * Contracts the storage array to the (size of the element set) + 1 - to
     * avoid a zero length array. This function also resets the startIndex to
     * zero.
     */
    public synchronized void contract() {
        final double[] tempArray = new double[this.numElements + 1];

        // Copy and swap - copy only the element array from the src array.
        System.arraycopy(this.internalArray, this.startIndex, tempArray, 0, this.numElements);
        this.internalArray = tempArray;

        // Reset the start index to zero
        this.startIndex = 0;
    }

    /**
     * Discards the <code>i</code> initial elements of the array. For example,
     * if the array contains the elements 1,2,3,4, invoking <code>discardFrontElements(2)</code> will cause the first
     * two elements
     * to be discarded, leaving 3,4 in the array. Throws illegalArgumentException
     * if i exceeds numElements.
     * 
     * @param i
     *        the number of elements to discard from the front of the array
     * @throws MathIllegalArgumentException
     *         if i is greater than numElements.
     * @since 2.0
     */
    public synchronized void discardFrontElements(final int i) {
        this.discardExtremeElements(i, true);
    }

    /**
     * Discards the <code>i</code> last elements of the array. For example,
     * if the array contains the elements 1,2,3,4, invoking <code>discardMostRecentElements(2)</code> will cause the
     * last two elements
     * to be discarded, leaving 1,2 in the array. Throws illegalArgumentException
     * if i exceeds numElements.
     * 
     * @param i
     *        the number of elements to discard from the end of the array
     * @throws MathIllegalArgumentException
     *         if i is greater than numElements.
     * @since 2.0
     */
    public synchronized void discardMostRecentElements(final int i) {
        this.discardExtremeElements(i, false);
    }

    /**
     * Discards the <code>i</code> first or last elements of the array,
     * depending on the value of <code>front</code>.
     * For example, if the array contains the elements 1,2,3,4, invoking <code>discardExtremeElements(2,false)</code>
     * will cause the last two elements
     * to be discarded, leaving 1,2 in the array.
     * For example, if the array contains the elements 1,2,3,4, invoking <code>discardExtremeElements(2,true)</code>
     * will cause the first two elements
     * to be discarded, leaving 3,4 in the array.
     * Throws illegalArgumentException
     * if i exceeds numElements.
     * 
     * @param i
     *        the number of elements to discard from the front/end of the array
     * @param front
     *        true if elements are to be discarded from the front
     *        of the array, false if elements are to be discarded from the end
     *        of the array
     * @throws MathIllegalArgumentException
     *         if i is greater than numElements.
     * @since 2.0
     */
    private synchronized void discardExtremeElements(final int i,
                                                     final boolean front) {
        if (i > this.numElements) {
            throw new MathIllegalArgumentException(
                PatriusMessages.TOO_MANY_ELEMENTS_TO_DISCARD_FROM_ARRAY,
                i, this.numElements);
        } else if (i < 0) {
            throw new MathIllegalArgumentException(
                PatriusMessages.CANNOT_DISCARD_NEGATIVE_NUMBER_OF_ELEMENTS,
                i);
        } else {
            // "Subtract" this number of discarded from numElements
            this.numElements -= i;
            if (front) {
                this.startIndex += i;
            }
        }
        if (this.shouldContract()) {
            this.contract();
        }
    }

    /**
     * Expands the internal storage array using the expansion factor.
     * <p>
     * if <code>expansionMode</code> is set to MULTIPLICATIVE_MODE, the new array size will be
     * <code>internalArray.length * expansionFactor.</code> If <code>expansionMode</code> is set to ADDITIVE_MODE, the
     * length after expansion will be <code>internalArray.length + expansionFactor</code>
     * </p>
     */
    protected synchronized void expand() {
        // notice the use of FastMath.ceil(), this guarantees that we will always
        // have an array of at least currentSize + 1. Assume that the
        // current initial capacity is 1 and the expansion factor
        // is 1.000000000000000001. The newly calculated size will be
        // rounded up to 2 after the multiplication is performed.
        int newSize = 0;
        if (this.expansionMode == ExpansionMode.MULTIPLICATIVE) {
            newSize = (int) MathLib.ceil(this.internalArray.length * this.expansionFactor);
        } else {
            newSize = (int) (this.internalArray.length + MathLib.round(this.expansionFactor));
        }
        final double[] tempArray = new double[newSize];

        // Copy and swap
        System.arraycopy(this.internalArray, 0, tempArray, 0, this.internalArray.length);
        this.internalArray = tempArray;
    }

    /**
     * Expands the internal storage array to the specified size.
     * 
     * @param size
     *        Size of the new internal storage array.
     */
    private synchronized void expandTo(final int size) {
        final double[] tempArray = new double[size];
        // Copy and swap
        System.arraycopy(this.internalArray, 0, tempArray, 0, this.internalArray.length);
        this.internalArray = tempArray;
    }

    /**
     * The contraction criterion defines when the internal array will contract
     * to store only the number of elements in the element array.
     * If the <code>expansionMode</code> is <code>MULTIPLICATIVE_MODE</code>,
     * contraction is triggered when the ratio between storage array length
     * and <code>numElements</code> exceeds <code>contractionFactor</code>.
     * If the <code>expansionMode</code> is <code>ADDITIVE_MODE</code>, the
     * number of excess storage locations is compared to <code>contractionFactor.</code>
     * 
     * @return the contraction criterion used to reclaim memory.
     * @since 3.1
     */
    public double getContractionCriterion() {
        return this.contractionCriterion;
    }

    /**
     * Returns the element at the specified index
     * 
     * @param index
     *        index to fetch a value from
     * @return value stored at the specified index
     * @throws ArrayIndexOutOfBoundsException
     *         if <code>index</code> is less than
     *         zero or is greater than <code>getNumElements() - 1</code>.
     */
    @Override
    public synchronized double getElement(final int index) {
        if (index >= this.numElements) {
            throw new ArrayIndexOutOfBoundsException(index);
        } else if (index >= 0) {
            return this.internalArray[this.startIndex + index];
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    /**
     * Returns a double array containing the elements of this <code>ResizableArray</code>. This method returns a copy,
     * not a
     * reference to the underlying array, so that changes made to the returned
     * array have no effect on this <code>ResizableArray.</code>
     * 
     * @return the double array.
     */
    @Override
    public synchronized double[] getElements() {
        final double[] elementArray = new double[this.numElements];
        System.arraycopy(this.internalArray, this.startIndex, elementArray, 0, this.numElements);
        return elementArray;
    }

    /**
     * Gets the currently allocated size of the internal data structure used
     * for storing elements.
     * This is not to be confused with {@link #getNumElements() the number of
     * elements actually stored}.
     * 
     * @return the length of the internal array.
     * @since 3.1
     */
    public synchronized int getCapacity() {
        return this.internalArray.length;
    }

    /**
     * Returns the number of elements currently in the array. Please note
     * that this is different from the length of the internal storage array.
     * 
     * @return the number of elements.
     */
    @Override
    public synchronized int getNumElements() {
        return this.numElements;
    }

    /**
     * Provides <em>direct</em> access to the internal storage array.
     * Please note that this method returns a reference to this object's
     * storage array, not a copy. <br/>
     * To correctly address elements of the array, the "start index" is
     * required (available via the {@link #getStartIndex() getStartIndex} method. <br/>
     * This method should only be used to avoid copying the internal array.
     * The returned value <em>must</em> be used for reading only; other
     * uses could lead to this object becoming inconsistent. <br/>
     * The {@link #getElements} method has no such limitation since it
     * returns a copy of this array's addressable elements.
     * 
     * @return the internal storage array used by this object.
     * @since 3.1
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    protected double[] getArrayRef() {
        return this.internalArray;
    }

    /**
     * Returns the "start index" of the internal array.
     * This index is the position of the first addressable element in the
     * internal storage array.
     * The addressable elements in the array are at indices contained in
     * the interval [{@link #getStartIndex()}, {@link #getStartIndex()} + {@link #getNumElements()} - 1].
     * 
     * @return the start index.
     * @since 3.1
     */
    protected synchronized int getStartIndex() {
        return this.startIndex;
    }

    /**
     * Performs an operation on the addressable elements of the array.
     * 
     * @param f
     *        Function to be applied on this array.
     * @return the result.
     * @since 3.1
     */
    public synchronized double compute(final MathArrays.Function f) {
        return f.evaluate(this.internalArray, this.startIndex, this.numElements);
    }

    /**
     * Sets the element at the specified index. If the specified index is greater than <code>getNumElements() - 1</code>
     * , the <code>numElements</code> property
     * is increased to <code>index +1</code> and additional storage is allocated
     * (if necessary) for the new element and all (uninitialized) elements
     * between the new element and the previous end of the array).
     * 
     * @param index
     *        index to store a value in
     * @param value
     *        value to store at the specified index
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code index < 0}.
     */
    @Override
    public synchronized void setElement(final int index, final double value) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (index + 1 > this.numElements) {
            this.numElements = index + 1;
        }
        if ((this.startIndex + index) >= this.internalArray.length) {
            this.expandTo(this.startIndex + (index + 1));
        }
        this.internalArray[this.startIndex + index] = value;
    }

    /**
     * This function allows you to control the number of elements contained
     * in this array, and can be used to "throw out" the last n values in an
     * array. This function will also expand the internal array as needed.
     * 
     * @param i
     *        a new number of elements
     * @throws MathIllegalArgumentException
     *         if <code>i</code> is negative.
     */
    public synchronized void setNumElements(final int i) {
        // If index is negative thrown an error.
        if (i < 0) {
            throw new MathIllegalArgumentException(
                PatriusMessages.INDEX_NOT_POSITIVE,
                i);
        }

        // Test the new num elements, check to see if the array needs to be
        // expanded to accommodate this new number of elements.
        final int newSize = this.startIndex + i;
        if (newSize > this.internalArray.length) {
            this.expandTo(newSize);
        }

        // Set the new number of elements to new value.
        this.numElements = i;
    }

    /**
     * Returns true if the internal storage array has too many unused
     * storage positions.
     * 
     * @return true if array satisfies the contraction criteria
     */
    private synchronized boolean shouldContract() {
        if (this.expansionMode == ExpansionMode.MULTIPLICATIVE) {
            return (this.internalArray.length / ((float) this.numElements)) > this.contractionCriterion;
        } else {
            return (this.internalArray.length - this.numElements) > this.contractionCriterion;
        }
    }

    /**
     * <p>
     * Copies source to dest, copying the underlying data, so dest is a new, independent copy of source. Does not
     * contract before the copy.
     * </p>
     * 
     * <p>
     * Obtains synchronization locks on both source and dest (in that order) before performing the copy.
     * </p>
     * 
     * <p>
     * Neither source nor dest may be null; otherwise a {@link NullArgumentException} is thrown
     * </p>
     * 
     * @param source
     *        ResizableDoubleArray to copy
     * @param dest
     *        ResizableArray to replace with a copy of the source array
     * @exception NullArgumentException
     *            if either source or dest is null
     * @since 2.0
     * 
     */
    public static void copy(final ResizableDoubleArray source,
                            final ResizableDoubleArray dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        synchronized (source) {
            synchronized (dest) {
                dest.contractionCriterion = source.contractionCriterion;
                dest.expansionFactor = source.expansionFactor;
                dest.expansionMode = source.expansionMode;
                dest.internalArray = new double[source.internalArray.length];
                System.arraycopy(source.internalArray, 0, dest.internalArray,
                    0, dest.internalArray.length);
                dest.numElements = source.numElements;
                dest.startIndex = source.startIndex;
            }
        }
    }

    /**
     * Returns a copy of the ResizableDoubleArray. Does not contract before
     * the copy, so the returned object is an exact copy of this.
     * 
     * @return a new ResizableDoubleArray with the same data and configuration
     *         properties as this
     * @since 2.0
     */
    public synchronized ResizableDoubleArray copy() {
        final ResizableDoubleArray result = new ResizableDoubleArray();
        copy(this, result);
        return result;
    }

    /**
     * Returns true iff object is a ResizableDoubleArray with the same properties
     * as this and an identical internal storage array.
     * 
     * @param object
     *        object to be compared for equality with this
     * @return true iff object is a ResizableDoubleArray with the same data and
     *         properties as this
     * @since 2.0
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ResizableDoubleArray)) {
            return false;
        }
        synchronized (this) {
            synchronized (object) {
                boolean result = true;
                final ResizableDoubleArray other = (ResizableDoubleArray) object;
                result = result && (other.contractionCriterion == this.contractionCriterion);
                result = result && (other.expansionFactor == this.expansionFactor);
                result = result && (other.expansionMode == this.expansionMode);
                result = result && (other.numElements == this.numElements);
                result = result && (other.startIndex == this.startIndex);
                if (result) {
                    return Arrays.equals(this.internalArray, other.internalArray);
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Returns a hash code consistent with equals.
     * 
     * @return the hash code representing this {@code ResizableDoubleArray}.
     * @since 2.0
     */
    @Override
    public synchronized int hashCode() {
        final int[] hashData = new int[6];
        hashData[0] = Double.valueOf(this.expansionFactor).hashCode();
        hashData[1] = Double.valueOf(this.contractionCriterion).hashCode();
        hashData[2] = this.expansionMode.hashCode();
        hashData[3] = Arrays.hashCode(this.internalArray);
        hashData[4] = this.numElements;
        hashData[5] = this.startIndex;
        return Arrays.hashCode(hashData);
    }

    // CHECKSTYLE: resume CommentRatio check
}
