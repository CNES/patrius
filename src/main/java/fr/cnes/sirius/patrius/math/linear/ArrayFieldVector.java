/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * This class implements the {@link FieldVector} interface with a {@link FieldElement} array.
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @param <T>
 *        the type of the field elements
 * @version $Id: ArrayFieldVector.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.PreserveStackTrace")
public class ArrayFieldVector<T extends FieldElement<T>> implements FieldVector<T>, Serializable {
    /** Serializable version identifier. */
    private static final long serialVersionUID = 7648186910365927050L;

    /** Hashcode generator. */
    private static final int HASH_CODE_GENERATOR = 3542;

    /** Entries of the vector. */
    private T[] data;

    /** Field to which the elements belong. */
    private final Field<T> field;

    /**
     * Build a 0-length vector.
     * Zero-length vectors may be used to initialize construction of vectors
     * by data gathering. We start with zero-length and use either the
     * {@link #ArrayFieldVector(ArrayFieldVector, ArrayFieldVector)} constructor
     * or one of the {@code append} methods ({@link #add(FieldVector)} or {@link #append(ArrayFieldVector)}) to gather
     * data into this vector.
     * 
     * @param fieldIn
     *        field to which the elements belong
     */
    public ArrayFieldVector(final Field<T> fieldIn) {
        this(fieldIn, 0);
    }

    /**
     * Construct a vector of zeroes.
     * 
     * @param fieldIn
     *        Field to which the elements belong.
     * @param size
     *        Size of the vector.
     */
    public ArrayFieldVector(final Field<T> fieldIn, final int size) {
        this.field = fieldIn;
        this.data = MathArrays.buildArray(field, size);
    }

    /**
     * Construct a vector with preset values.
     * 
     * @param size
     *        Size of the vector.
     * @param preset
     *        All entries will be set with this value.
     */
    public ArrayFieldVector(final int size, final T preset) {
        this(preset.getField(), size);
        Arrays.fill(this.data, preset);
    }

    /**
     * Construct a vector from an array, copying the input array.
     * This constructor needs a non-empty {@code d} array to retrieve
     * the field from its first element. This implies it cannot build
     * 0 length vectors. To build vectors from any size, one should
     * use the {@link #ArrayFieldVector(Field, FieldElement[])} constructor.
     * 
     * @param d
     *        Array.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws ZeroException
     *         if {@code d} is empty.
     * @see #ArrayFieldVector(Field, FieldElement[])
     */
    public ArrayFieldVector(final T[] d) {
        MathUtils.checkNotNull(d);
        try {
            this.field = d[0].getField();
            this.data = d.clone();
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new ZeroException(PatriusMessages.VECTOR_MUST_HAVE_AT_LEAST_ONE_ELEMENT);
        }
    }

    /**
     * Construct a vector from an array, copying the input array.
     * 
     * @param fieldIn
     *        Field to which the elements belong.
     * @param d
     *        Array.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @see #ArrayFieldVector(FieldElement[])
     */
    public ArrayFieldVector(final Field<T> fieldIn, final T[] d) {
        MathUtils.checkNotNull(d);
        this.field = fieldIn;
        this.data = d.clone();
    }

    /**
     * Create a new ArrayFieldVector using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * ArrayFieldVector and not used directly, the {@code copyArray} may be
     * set to {@code false}. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * This constructor needs a non-empty {@code d} array to retrieve
     * the field from its first element. This implies it cannot build
     * 0 length vectors. To build vectors from any size, one should
     * use the {@link #ArrayFieldVector(Field, FieldElement[], boolean)} constructor.
     * 
     * @param d
     *        Data for the new vector.
     * @param copyArray
     *        If {@code true}, the input array will be copied,
     *        otherwise it will be referenced.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws ZeroException
     *         if {@code d} is empty.
     * @see #ArrayFieldVector(FieldElement[])
     * @see #ArrayFieldVector(Field, FieldElement[], boolean)
     */
    public ArrayFieldVector(final T[] d, final boolean copyArray) {
        MathUtils.checkNotNull(d);
        if (d.length == 0) {
            throw new ZeroException(PatriusMessages.VECTOR_MUST_HAVE_AT_LEAST_ONE_ELEMENT);
        }
        this.field = d[0].getField();
        this.data = copyArray ? d.clone() : d;
    }

    /**
     * Create a new ArrayFieldVector using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * ArrayFieldVector and not used directly, the {@code copyArray} may be
     * set to {@code false}. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * 
     * @param fieldIn
     *        Field to which the elements belong.
     * @param d
     *        Data for the new vector.
     * @param copyArray
     *        If {@code true}, the input array will be copied,
     *        otherwise it will be referenced.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @see #ArrayFieldVector(FieldElement[], boolean)
     */
    public ArrayFieldVector(final Field<T> fieldIn, final T[] d, final boolean copyArray) {
        MathUtils.checkNotNull(d);
        this.field = fieldIn;
        this.data = copyArray ? d.clone() : d;
    }

    /**
     * Construct a vector from part of a array.
     * 
     * @param d
     *        Array.
     * @param pos
     *        Position of the first entry.
     * @param size
     *        Number of entries to copy.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws NumberIsTooLargeException
     *         if the size of {@code d} is less
     *         than {@code pos + size}.
     */
    public ArrayFieldVector(final T[] d, final int pos, final int size) {
        MathUtils.checkNotNull(d);
        if (d.length < pos + size) {
            throw new NumberIsTooLargeException(pos + size, d.length, true);
        }
        this.field = d[0].getField();
        this.data = MathArrays.buildArray(this.field, size);
        System.arraycopy(d, pos, this.data, 0, size);
    }

    /**
     * Construct a vector from part of a array.
     * 
     * @param fieldIn
     *        Field to which the elements belong.
     * @param d
     *        Array.
     * @param pos
     *        Position of the first entry.
     * @param size
     *        Number of entries to copy.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws NumberIsTooLargeException
     *         if the size of {@code d} is less
     *         than {@code pos + size}.
     */
    public ArrayFieldVector(final Field<T> fieldIn, final T[] d, final int pos, final int size) {
        MathUtils.checkNotNull(d);
        if (d.length < pos + size) {
            throw new NumberIsTooLargeException(pos + size, d.length, true);
        }
        this.field = fieldIn;
        this.data = MathArrays.buildArray(this.field, size);
        System.arraycopy(d, pos, this.data, 0, size);
    }

    /**
     * Construct a vector from another vector, using a deep copy.
     * 
     * @param v
     *        Vector to copy.
     * @throws NullArgumentException
     *         if {@code v} is {@code null}.
     */
    public ArrayFieldVector(final FieldVector<T> v) {
        MathUtils.checkNotNull(v);
        this.field = v.getField();
        this.data = MathArrays.buildArray(this.field, v.getDimension());
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = v.getEntry(i);
        }
    }

    /**
     * Construct a vector from another vector, using a deep copy.
     * 
     * @param v
     *        Vector to copy.
     * @throws NullArgumentException
     *         if {@code v} is {@code null}.
     */
    public ArrayFieldVector(final ArrayFieldVector<T> v) {
        MathUtils.checkNotNull(v);
        this.field = v.getField();
        this.data = v.data.clone();
    }

    /**
     * Construct a vector from another vector.
     * 
     * @param v
     *        Vector to copy.
     * @param deep
     *        If {@code true} perform a deep copy, otherwise perform
     *        a shallow copy
     * @throws NullArgumentException
     *         if {@code v} is {@code null}.
     */
    public ArrayFieldVector(final ArrayFieldVector<T> v, final boolean deep) {
        MathUtils.checkNotNull(v);
        this.field = v.getField();
        this.data = deep ? v.data.clone() : v.data;
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     * @throws NullArgumentException
     *         if {@code v1} or {@code v2} is {@code null}.
     * @deprecated as of 4.5, replaced by {@link #ArrayFieldVector(FieldVector, FieldVector)}
     */
    @Deprecated
    public ArrayFieldVector(final ArrayFieldVector<T> v1, final ArrayFieldVector<T> v2) {
        this((FieldVector<T>) v1, (FieldVector<T>) v2);
    }
    
    /**
     * Construct a vector by appending one vector to another vector.
     *
     * @param v1 First vector (will be put in front of the new vector).
     * @param v2 Second vector (will be put at back of the new vector).
     * @throws NullArgumentException if {@code v1} or {@code v2} is
     * {@code null}.
     */
    public ArrayFieldVector(final FieldVector<T> v1, final FieldVector<T> v2) {
        MathUtils.checkNotNull(v1);
        MathUtils.checkNotNull(v2);
        this.field = v1.getField();
        final T[] v1Data =
                (v1 instanceof ArrayFieldVector) ? ((ArrayFieldVector<T>) v1).data : v1.toArray();
        final T[] v2Data =
                (v2 instanceof ArrayFieldVector) ? ((ArrayFieldVector<T>) v2).data : v2.toArray();
        this.data = MathArrays.buildArray(field, v1Data.length + v2Data.length);
        System.arraycopy(v1Data, 0, data, 0, v1Data.length);
        System.arraycopy(v2Data, 0, data, v1Data.length, v2Data.length);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     * @throws NullArgumentException
     *         if {@code v1} or {@code v2} is {@code null}.
     * @deprecated as of 4.5, replaced by {@link #ArrayFieldVector(FieldVector, FieldElement[])}
     */
    @Deprecated
    public ArrayFieldVector(final ArrayFieldVector<T> v1, final T[] v2) {
        this((FieldVector<T>) v1, v2);
    }
    
    /**
     * Construct a vector by appending one vector to another vector.
     *
     * @param v1 First vector (will be put in front of the new vector).
     * @param v2 Second vector (will be put at back of the new vector).
     * @throws NullArgumentException if {@code v1} or {@code v2} is
     * {@code null}.
     */
    public ArrayFieldVector(final FieldVector<T> v1, final T[] v2) {
        MathUtils.checkNotNull(v1);
        MathUtils.checkNotNull(v2);
        this.field = v1.getField();
        final T[] v1Data =
                (v1 instanceof ArrayFieldVector) ? ((ArrayFieldVector<T>) v1).data : v1.toArray();
        this.data = MathArrays.buildArray(this.field, v1Data.length + v2.length);
        System.arraycopy(v1Data, 0, this.data, 0, v1Data.length);
        System.arraycopy(v2, 0, this.data, v1Data.length, v2.length);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     * @throws NullArgumentException
     *         if {@code v1} or {@code v2} is {@code null}.
     * @deprecated as of 4.5, replaced by {@link #ArrayFieldVector(FieldElement[], FieldVector)}
     */
    @Deprecated
    public ArrayFieldVector(final T[] v1, final ArrayFieldVector<T> v2) {
        this(v1, (FieldVector<T>) v2);
    }
    
    /**
     * Construct a vector by appending one vector to another vector.
     *
     * @param v1 First vector (will be put in front of the new vector).
     * @param v2 Second vector (will be put at back of the new vector).
     * @throws NullArgumentException if {@code v1} or {@code v2} is
     * {@code null}.
     */
    public ArrayFieldVector(final T[] v1, final FieldVector<T> v2) {
        MathUtils.checkNotNull(v1);
        MathUtils.checkNotNull(v2);
        this.field = v2.getField();
        final T[] v2Data =
                (v2 instanceof ArrayFieldVector) ? ((ArrayFieldVector<T>) v2).data : v2.toArray();
        this.data = MathArrays.buildArray(this.field, v1.length + v2Data.length);
        System.arraycopy(v1, 0, this.data, 0, v1.length);
        System.arraycopy(v2Data, 0, this.data, v1.length, v2Data.length);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * This constructor needs at least one non-empty array to retrieve
     * the field from its first element. This implies it cannot build
     * 0 length vectors. To build vectors from any size, one should
     * use the {@link #ArrayFieldVector(Field, FieldElement[], FieldElement[])} constructor.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     * @throws NullArgumentException
     *         if {@code v1} or {@code v2} is {@code null}.
     * @throws ZeroException
     *         if both arrays are empty.
     * @see #ArrayFieldVector(Field, FieldElement[], FieldElement[])
     */
    public ArrayFieldVector(final T[] v1, final T[] v2) {
        MathUtils.checkNotNull(v1);
        MathUtils.checkNotNull(v2);
        if (v1.length + v2.length == 0) {
            throw new ZeroException(PatriusMessages.VECTOR_MUST_HAVE_AT_LEAST_ONE_ELEMENT);
        }
        this.data = MathArrays.buildArray(v1[0].getField(), v1.length + v2.length);
        System.arraycopy(v1, 0, this.data, 0, v1.length);
        System.arraycopy(v2, 0, this.data, v1.length, v2.length);
        this.field = this.data[0].getField();
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param fieldIn
     *        Field to which the elements belong.
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     * @throws NullArgumentException
     *         if {@code v1} or {@code v2} is {@code null}.
     * @throws ZeroException
     *         if both arrays are empty.
     * @see #ArrayFieldVector(FieldElement[], FieldElement[])
     */
    public ArrayFieldVector(final Field<T> fieldIn, final T[] v1, final T[] v2) {
        MathUtils.checkNotNull(v1);
        MathUtils.checkNotNull(v2);
        if (v1.length + v2.length == 0) {
            throw new ZeroException(PatriusMessages.VECTOR_MUST_HAVE_AT_LEAST_ONE_ELEMENT);
        }
        this.data = MathArrays.buildArray(fieldIn, v1.length + v2.length);
        System.arraycopy(v1, 0, this.data, 0, v1.length);
        System.arraycopy(v2, 0, this.data, v1.length, v2.length);
        this.field = fieldIn;
    }

    /** {@inheritDoc} */
    @Override
    public Field<T> getField() {
        return this.field;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> copy() {
        return new ArrayFieldVector<T>(this, true);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> add(final FieldVector<T> v) {
        try {
            return this.add((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            this.checkVectorDimensions(v);
            final T[] out = MathArrays.buildArray(this.field, this.data.length);
            for (int i = 0; i < this.data.length; i++) {
                out[i] = this.data[i].add(v.getEntry(i));
            }
            return new ArrayFieldVector<T>(this.field, out, false);
        }
    }

    /**
     * Compute the sum of {@code this} and {@code v}.
     * 
     * @param v
     *        vector to be added
     * @return {@code this + v}
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this}
     */
    public ArrayFieldVector<T> add(final ArrayFieldVector<T> v) {
        this.checkVectorDimensions(v.data.length);
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].add(v.data[i]);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> subtract(final FieldVector<T> v) {
        try {
            return this.subtract((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            this.checkVectorDimensions(v);
            final T[] out = MathArrays.buildArray(this.field, this.data.length);
            for (int i = 0; i < this.data.length; i++) {
                out[i] = this.data[i].subtract(v.getEntry(i));
            }
            return new ArrayFieldVector<T>(this.field, out, false);
        }
    }

    /**
     * Compute {@code this} minus {@code v}.
     * 
     * @param v
     *        vector to be subtracted
     * @return {@code this - v}
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this}
     */
    public ArrayFieldVector<T> subtract(final ArrayFieldVector<T> v) {
        this.checkVectorDimensions(v.data.length);
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].subtract(v.data[i]);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapAdd(final T d) {
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].add(d);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapAddToSelf(final T d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = this.data[i].add(d);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapSubtract(final T d) {
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].subtract(d);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapSubtractToSelf(final T d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = this.data[i].subtract(d);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapMultiply(final T d) {
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].multiply(d);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapMultiplyToSelf(final T d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = this.data[i].multiply(d);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapDivide(final T d) {
        MathUtils.checkNotNull(d);
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].divide(d);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapDivideToSelf(final T d) {
        MathUtils.checkNotNull(d);
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = this.data[i].divide(d);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapInv() {
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        final T one = this.field.getOne();
        for (int i = 0; i < this.data.length; i++) {
            try {
                out[i] = one.divide(this.data[i]);
            } catch (final MathArithmeticException e) {
                throw new MathArithmeticException(PatriusMessages.INDEX, i);
            }
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> mapInvToSelf() {
        final T one = this.field.getOne();
        for (int i = 0; i < this.data.length; i++) {
            try {
                this.data[i] = one.divide(this.data[i]);
            } catch (final MathArithmeticException e) {
                throw new MathArithmeticException(PatriusMessages.INDEX, i);
            }
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> ebeMultiply(final FieldVector<T> v) {
        try {
            return this.ebeMultiply((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            this.checkVectorDimensions(v);
            final T[] out = MathArrays.buildArray(this.field, this.data.length);
            for (int i = 0; i < this.data.length; i++) {
                out[i] = this.data[i].multiply(v.getEntry(i));
            }
            return new ArrayFieldVector<T>(this.field, out, false);
        }
    }

    /**
     * Element-by-element multiplication.
     * 
     * @param v
     *        vector by which instance elements must be multiplied
     * @return a vector containing {@code this[i] * v[i]} for all {@code i}
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this}
     */
    public ArrayFieldVector<T> ebeMultiply(final ArrayFieldVector<T> v) {
        this.checkVectorDimensions(v.data.length);
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            out[i] = this.data[i].multiply(v.data[i]);
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public FieldVector<T> ebeDivide(final FieldVector<T> v) {
        try {
            return this.ebeDivide((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            this.checkVectorDimensions(v);
            final T[] out = MathArrays.buildArray(this.field, this.data.length);
            for (int i = 0; i < this.data.length; i++) {
                try {
                    out[i] = this.data[i].divide(v.getEntry(i));
                } catch (final MathArithmeticException e) {
                    throw new MathArithmeticException(PatriusMessages.INDEX, i);
                }
            }
            return new ArrayFieldVector<T>(this.field, out, false);
        }
    }

    /**
     * Element-by-element division.
     * 
     * @param v
     *        vector by which instance elements must be divided
     * @return a vector containing {@code this[i] / v[i]} for all {@code i}
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this}
     * @throws MathArithmeticException
     *         if one entry of {@code v} is zero.
     */
    public ArrayFieldVector<T> ebeDivide(final ArrayFieldVector<T> v) {
        this.checkVectorDimensions(v.data.length);
        final T[] out = MathArrays.buildArray(this.field, this.data.length);
        for (int i = 0; i < this.data.length; i++) {
            try {
                out[i] = this.data[i].divide(v.data[i]);
            } catch (final MathArithmeticException e) {
                throw new MathArithmeticException(PatriusMessages.INDEX, i);
            }
        }
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /**
     * Returns the vector data (copy).
     * 
     * @return the vector data
     */
    public T[] getData() {
        return this.data.clone();
    }

    /**
     * Returns a reference to the underlying data array.
     * <p>
     * Does not make a fresh copy of the underlying data.
     * </p>
     * 
     * @return array of entries
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public T[] getDataRef() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public T dotProduct(final FieldVector<T> v) {
        try {
            return this.dotProduct((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            this.checkVectorDimensions(v);
            T dot = this.field.getZero();
            for (int i = 0; i < this.data.length; i++) {
                dot = dot.add(this.data[i].multiply(v.getEntry(i)));
            }
            return dot;
        }
    }

    /**
     * Compute the dot product.
     * 
     * @param v
     *        vector with which dot product should be computed
     * @return the scalar dot product of {@code this} and {@code v}
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this}
     */
    public T dotProduct(final ArrayFieldVector<T> v) {
        this.checkVectorDimensions(v.data.length);
        T dot = this.field.getZero();
        for (int i = 0; i < this.data.length; i++) {
            dot = dot.add(this.data[i].multiply(v.data[i]));
        }
        return dot;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> projection(final FieldVector<T> v) {
        return v.mapMultiply(this.dotProduct(v).divide(v.dotProduct(v)));
    }

    /**
     * Find the orthogonal projection of this vector onto another vector.
     * 
     * @param v
     *        vector onto which {@code this} must be projected
     * @return projection of {@code this} onto {@code v}
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this}
     * @throws MathArithmeticException
     *         if {@code v} is the null vector.
     */
    public ArrayFieldVector<T> projection(final ArrayFieldVector<T> v) {
        return (ArrayFieldVector<T>) v.mapMultiply(this.dotProduct(v).divide(v.dotProduct(v)));
    }

    /** {@inheritDoc} */
    @Override
    public FieldMatrix<T> outerProduct(final FieldVector<T> v) {
        try {
            return this.outerProduct((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            final int m = this.data.length;
            final int n = v.getDimension();
            final FieldMatrix<T> out = new Array2DRowFieldMatrix<T>(this.field, m, n);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    out.setEntry(i, j, this.data[i].multiply(v.getEntry(j)));
                }
            }
            return out;
        }
    }

    /**
     * Compute the outer product.
     * 
     * @param v
     *        vector with which outer product should be computed
     * @return the matrix outer product between instance and v
     */
    public FieldMatrix<T> outerProduct(final ArrayFieldVector<T> v) {
        final int m = this.data.length;
        final int n = v.data.length;
        final FieldMatrix<T> out = new Array2DRowFieldMatrix<T>(this.field, m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                out.setEntry(i, j, this.data[i].multiply(v.data[j]));
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public T getEntry(final int index) {
        return this.data[index];
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return this.data.length;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> append(final FieldVector<T> v) {
        try {
            return this.append((ArrayFieldVector<T>) v);
        } catch (final ClassCastException cce) {
            return new ArrayFieldVector<T>(this, new ArrayFieldVector<T>(v));
        }
    }

    /**
     * Construct a vector by appending a vector to this vector.
     * 
     * @param v
     *        vector to append to this one.
     * @return a new vector
     */
    public ArrayFieldVector<T> append(final ArrayFieldVector<T> v) {
        return new ArrayFieldVector<T>(this, v);
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector<T> append(final T v) {
        final T[] out = MathArrays.buildArray(this.field, this.data.length + 1);
        System.arraycopy(this.data, 0, out, 0, this.data.length);
        out[this.data.length] = v;
        return new ArrayFieldVector<T>(this.field, out, false);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public FieldVector<T> getSubVector(final int index, final int n) {
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.NUMBER_OF_ELEMENTS_SHOULD_BE_POSITIVE, n);
        }
        final ArrayFieldVector<T> out = new ArrayFieldVector<T>(this.field, n);
        try {
            System.arraycopy(this.data, index, out.data, 0, n);
        } catch (final IndexOutOfBoundsException e) {
            this.checkIndex(index);
            this.checkIndex(index + n - 1);
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void setEntry(final int index, final T value) {
        try {
            this.data[index] = value;
        } catch (final IndexOutOfBoundsException e) {
            this.checkIndex(index);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSubVector(final int index, final FieldVector<T> v) {
        try {
            try {
                this.set(index, (ArrayFieldVector<T>) v);
            } catch (final ClassCastException cce) {
                for (int i = index; i < index + v.getDimension(); ++i) {
                    this.data[i] = v.getEntry(i - index);
                }
            }
        } catch (final IndexOutOfBoundsException e) {
            this.checkIndex(index);
            this.checkIndex(index + v.getDimension() - 1);
        }
    }

    /**
     * Set a set of consecutive elements.
     * 
     * @param index
     *        index of first element to be set.
     * @param v
     *        vector containing the values to set.
     * @throws OutOfRangeException
     *         if the index is invalid.
     */
    public void set(final int index, final ArrayFieldVector<T> v) {
        try {
            System.arraycopy(v.data, 0, this.data, index, v.data.length);
        } catch (final IndexOutOfBoundsException e) {
            this.checkIndex(index);
            this.checkIndex(index + v.data.length - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final T value) {
        Arrays.fill(this.data, value);
    }

    /** {@inheritDoc} */
    @Override
    public T[] toArray() {
        return this.data.clone();
    }

    /**
     * Check if instance and specified vectors have the same dimension.
     * 
     * @param v
     *        vector to compare instance with
     * @exception DimensionMismatchException
     *            if the vectors do not
     *            have the same dimensions
     */
    protected void checkVectorDimensions(final FieldVector<T> v) {
        this.checkVectorDimensions(v.getDimension());
    }

    /**
     * Check if instance dimension is equal to some expected value.
     * 
     * @param n
     *        Expected dimension.
     * @throws DimensionMismatchException
     *         if the dimension is not equal to the
     *         size of {@code this} vector.
     */
    protected void checkVectorDimensions(final int n) {
        if (this.data.length != n) {
            throw new DimensionMismatchException(this.data.length, n);
        }
    }

    /**
     * Test for the equality of two vectors.
     * 
     * @param other
     *        Object to test for equality.
     * @return {@code true} if two vector objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            // May fail, but we ignore ClassCastException
            final FieldVector<T> rhs = (FieldVector<T>) other;
            if (this.data.length != rhs.getDimension()) {
                return false;
            }

            for (int i = 0; i < this.data.length; ++i) {
                if (!this.data[i].equals(rhs.getEntry(i))) {
                    return false;
                }
            }
            return true;
        } catch (final ClassCastException ex) {
            // ignore exception
            return false;
        }
    }

    /**
     * Get a hashCode for the real vector.
     * <p>
     * All NaN values have the same hash code.
     * </p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int h = HASH_CODE_GENERATOR;
        for (final T a : this.data) {
            h ^= a.hashCode();
        }
        return h;
    }

    /**
     * Check if an index is valid.
     * 
     * @param index
     *        Index to check.
     * @exception OutOfRangeException
     *            if the index is not valid.
     */
    private void checkIndex(final int index) {
        if (index < 0 || index >= this.getDimension()) {
            throw new OutOfRangeException(PatriusMessages.INDEX,
                index, 0, this.getDimension() - 1);
        }
    }
    
    /**
     * Checks that the indices of a subvector are valid.
     *
     * @param start the index of the first entry of the subvector
     * @param end the index of the last entry of the subvector (inclusive)
     * @throws OutOfRangeException if {@code start} of {@code end} are not valid
     * @throws NumberIsTooSmallException if {@code end < start}
     * @since 3.3
     */
    private void checkIndices(final int start, final int end) {
        final int dim = getDimension();
        if ((start < 0) || (start >= dim)) {
            throw new OutOfRangeException(PatriusMessages.INDEX, start, 0, dim - 1);
        }
        if ((end < 0) || (end >= dim)) {
            throw new OutOfRangeException(PatriusMessages.INDEX, end, 0, dim - 1);
        }
        if (end < start) {
            throw new NumberIsTooSmallException(PatriusMessages.INITIAL_ROW_AFTER_FINAL_ROW, end, start, false);
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
