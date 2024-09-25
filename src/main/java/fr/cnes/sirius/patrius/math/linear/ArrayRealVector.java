/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

// CHECKSTYLE: stop CommentRatio check
// Reason: model - Commons-Math code kept as such

/**
 * This class implements the {@link RealVector} interface with a double array.
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @version $Id: ArrayRealVector.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class ArrayRealVector extends RealVector implements Serializable {
     /** Serializable UID. */
    private static final long serialVersionUID = -1097961340710804027L;
    /** Default format. */
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
    /** 9 */
    private static final int C_9 = 9;

    /** Entries of the vector. */
    private final double[] data;

    /**
     * Build a 0-length vector.
     * Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the
     * {@link #ArrayRealVector(ArrayRealVector, ArrayRealVector)} constructor
     * or one of the {@code append} method ({@link #append(double)}, {@link #append(ArrayRealVector)}) to gather data
     * into this vector.
     */
    public ArrayRealVector() {
        super();
        this.data = new double[0];
    }

    /**
     * Construct a vector of zeroes.
     * 
     * @param size
     *        Size of the vector.
     */
    public ArrayRealVector(final int size) {
        super();
        this.data = new double[size];
    }

    /**
     * Construct a vector with preset values.
     * 
     * @param size
     *        Size of the vector
     * @param preset
     *        All entries will be set with this value.
     */
    public ArrayRealVector(final int size, final double preset) {
        super();
        this.data = new double[size];
        Arrays.fill(this.data, preset);
    }

    /**
     * Construct a vector from an array, copying the input array.
     * 
     * @param d
     *        Array.
     */
    public ArrayRealVector(final double[] d) {
        super();
        this.data = d.clone();
    }

    /**
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the {@code copyArray} may be
     * set to {@code false}. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * 
     * @param d
     *        Data for the new vector.
     * @param copyArray
     *        if {@code true}, the input array will be copied,
     *        otherwise it will be referenced.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @see #ArrayRealVector(double[])
     */
    public ArrayRealVector(final double[] d, final boolean copyArray) {
        super();
        MathUtils.checkNotNull(d);
        this.data = copyArray ? d.clone() : d;
    }

    /**
     * Construct a vector from part of a array.
     * 
     * @param d
     *        Array.
     * @param pos
     *        Position of first entry.
     * @param size
     *        Number of entries to copy.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws NumberIsTooLargeException
     *         if the size of {@code d} is less
     *         than {@code pos + size}.
     */
    public ArrayRealVector(final double[] d, final int pos, final int size) {
        super();
        MathUtils.checkNotNull(d);
        if (d.length < pos + size) {
            throw new NumberIsTooLargeException(pos + size, d.length, true);
        }
        this.data = new double[size];
        System.arraycopy(d, pos, this.data, 0, size);
    }

    /**
     * Construct a vector from an array.
     * 
     * @param d
     *        Array of {@code Double}s.
     */
    public ArrayRealVector(final Double[] d) {
        super();
        this.data = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            this.data[i] = d[i].doubleValue();
        }
    }

    /**
     * Construct a vector from part of an array.
     * 
     * @param d
     *        Array.
     * @param pos
     *        Position of first entry.
     * @param size
     *        Number of entries to copy.
     * @throws NullArgumentException
     *         if {@code d} is {@code null}.
     * @throws NumberIsTooLargeException
     *         if the size of {@code d} is less
     *         than {@code pos + size}.
     */
    public ArrayRealVector(final Double[] d, final int pos, final int size) {
        super();
        MathUtils.checkNotNull(d);
        if (d.length < pos + size) {
            throw new NumberIsTooLargeException(pos + size, d.length, true);
        }
        this.data = new double[size];
        for (int i = pos; i < pos + size; i++) {
            this.data[i - pos] = d[i].doubleValue();
        }
    }

    /**
     * Construct a vector from another vector, using a deep copy.
     * 
     * @param v
     *        vector to copy.
     * @throws NullArgumentException
     *         if {@code v} is {@code null}.
     */
    public ArrayRealVector(final RealVector v) {
        super();
        MathUtils.checkNotNull(v);
        this.data = new double[v.getDimension()];
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
    public ArrayRealVector(final ArrayRealVector v) {
        this(v, true);
    }

    /**
     * Construct a vector from another vector.
     * 
     * @param v
     *        Vector to copy.
     * @param deep
     *        If {@code true} perform a deep copy, otherwise perform a
     *        shallow copy.
     */
    public ArrayRealVector(final ArrayRealVector v, final boolean deep) {
        super();
        this.data = deep ? v.data.clone() : v.data;
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     */
    public ArrayRealVector(final ArrayRealVector v1, final ArrayRealVector v2) {
        super();
        this.data = new double[v1.data.length + v2.data.length];
        System.arraycopy(v1.data, 0, this.data, 0, v1.data.length);
        System.arraycopy(v2.data, 0, this.data, v1.data.length, v2.data.length);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     */
    public ArrayRealVector(final ArrayRealVector v1, final RealVector v2) {
        super();
        final int l1 = v1.data.length;
        final int l2 = v2.getDimension();
        this.data = new double[l1 + l2];
        System.arraycopy(v1.data, 0, this.data, 0, l1);
        for (int i = 0; i < l2; ++i) {
            this.data[l1 + i] = v2.getEntry(i);
        }
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     */
    public ArrayRealVector(final RealVector v1, final ArrayRealVector v2) {
        super();
        final int l1 = v1.getDimension();
        final int l2 = v2.data.length;
        this.data = new double[l1 + l2];
        for (int i = 0; i < l1; ++i) {
            this.data[i] = v1.getEntry(i);
        }
        System.arraycopy(v2.data, 0, this.data, l1, l2);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     */
    public ArrayRealVector(final ArrayRealVector v1, final double[] v2) {
        super();
        final int l1 = v1.getDimension();
        final int l2 = v2.length;
        this.data = new double[l1 + l2];
        System.arraycopy(v1.data, 0, this.data, 0, l1);
        System.arraycopy(v2, 0, this.data, l1, l2);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        First vector (will be put in front of the new vector).
     * @param v2
     *        Second vector (will be put at back of the new vector).
     */
    public ArrayRealVector(final double[] v1, final ArrayRealVector v2) {
        super();
        final int l1 = v1.length;
        final int l2 = v2.getDimension();
        this.data = new double[l1 + l2];
        System.arraycopy(v1, 0, this.data, 0, l1);
        System.arraycopy(v2.data, 0, this.data, l1, l2);
    }

    /**
     * Construct a vector by appending one vector to another vector.
     * 
     * @param v1
     *        first vector (will be put in front of the new vector)
     * @param v2
     *        second vector (will be put at back of the new vector)
     */
    public ArrayRealVector(final double[] v1, final double[] v2) {
        super();
        final int l1 = v1.length;
        final int l2 = v2.length;
        this.data = new double[l1 + l2];
        System.arraycopy(v1, 0, this.data, 0, l1);
        System.arraycopy(v2, 0, this.data, l1, l2);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector copy() {
        return new ArrayRealVector(this, true);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector add(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            final int dim = vData.length;
            this.checkVectorDimensions(dim);
            final ArrayRealVector result = new ArrayRealVector(dim);
            final double[] resultData = result.data;
            for (int i = 0; i < dim; i++) {
                resultData[i] = this.data[i] + vData[i];
            }
            return result;
        } else {
            this.checkVectorDimensions(v);
            final double[] out = this.data.clone();
            final Iterator<Entry> it = v.iterator();
            while (it.hasNext()) {
                final Entry e = it.next();
                out[e.getIndex()] += e.getValue();
            }
            return new ArrayRealVector(out, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector subtract(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            final int dim = vData.length;
            this.checkVectorDimensions(dim);
            final ArrayRealVector result = new ArrayRealVector(dim);
            final double[] resultData = result.data;
            for (int i = 0; i < dim; i++) {
                resultData[i] = this.data[i] - vData[i];
            }
            return result;
        } else {
            this.checkVectorDimensions(v);
            final double[] out = this.data.clone();
            final Iterator<Entry> it = v.iterator();
            while (it.hasNext()) {
                final Entry e = it.next();
                out[e.getIndex()] -= e.getValue();
            }
            return new ArrayRealVector(out, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector map(final UnivariateFunction function) {
        return this.copy().mapToSelf(function);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector mapToSelf(final UnivariateFunction function) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = function.value(this.data[i]);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector mapAddToSelf(final double d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] += d;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector mapSubtractToSelf(final double d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] -= d;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector mapMultiplyToSelf(final double d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] *= d;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector mapDivideToSelf(final double d) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] /= d;
        }
        return this;
    }
    
    /** {@inheritDoc} */
    @Override
    public ArrayRealVector ebeMultiply(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            final int dim = vData.length;
            checkVectorDimensions(dim);
            final ArrayRealVector result = new ArrayRealVector(dim);
            final double[] resultData = result.data;
            for (int i = 0; i < dim; i++) {
                resultData[i] = data[i] * vData[i];
            }
            return result;
        } else {
            checkVectorDimensions(v);
            final double[] out = data.clone();
            for (int i = 0; i < data.length; i++) {
                out[i] *= v.getEntry(i);
            }
            return new ArrayRealVector(out, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector ebeDivide(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            final int dim = vData.length;
            checkVectorDimensions(dim);
            final ArrayRealVector result = new ArrayRealVector(dim);
            final double[] resultData = result.data;
            for (int i = 0; i < dim; i++) {
                resultData[i] = data[i] / vData[i];
            }
            return result;
        } else {
            checkVectorDimensions(v);
            final double[] out = data.clone();
            for (int i = 0; i < data.length; i++) {
                out[i] /= v.getEntry(i);
            }
            return new ArrayRealVector(out, false);
        }
    }

    /**
     * Get a reference to the underlying data array.
     * This method does not make a fresh copy of the underlying data.
     * 
     * @return the array of entries.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getDataRef() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public double dotProduct(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            this.checkVectorDimensions(vData.length);
            double dot = 0;
            for (int i = 0; i < this.data.length; i++) {
                dot += this.data[i] * vData[i];
            }
            return dot;
        }
        return super.dotProduct(v);
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm() {
        double sum = 0;
        for (final double a : this.data) {
            sum += a * a;
        }
        return MathLib.sqrt(sum);
    }

    /** {@inheritDoc} */
    @Override
    public double getL1Norm() {
        double sum = 0;
        for (final double a : this.data) {
            sum += MathLib.abs(a);
        }
        return sum;
    }

    /** {@inheritDoc} */
    @Override
    public double getLInfNorm() {
        double max = 0;
        for (final double a : this.data) {
            max = MathLib.max(max, MathLib.abs(a));
        }
        return max;
    }

    /** {@inheritDoc} */
    @Override
    public double getDistance(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            this.checkVectorDimensions(vData.length);
            double sum = 0;
            for (int i = 0; i < this.data.length; ++i) {
                final double delta = this.data[i] - vData[i];
                sum += delta * delta;
            }
            return MathLib.sqrt(sum);
        } else {
            this.checkVectorDimensions(v);
            double sum = 0;
            for (int i = 0; i < this.data.length; ++i) {
                final double delta = this.data[i] - v.getEntry(i);
                sum += delta * delta;
            }
            return MathLib.sqrt(sum);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getL1Distance(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            this.checkVectorDimensions(vData.length);
            double sum = 0;
            for (int i = 0; i < this.data.length; ++i) {
                final double delta = this.data[i] - vData[i];
                sum += MathLib.abs(delta);
            }
            return sum;
        } else {
            this.checkVectorDimensions(v);
            double sum = 0;
            for (int i = 0; i < this.data.length; ++i) {
                final double delta = this.data[i] - v.getEntry(i);
                sum += MathLib.abs(delta);
            }
            return sum;
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLInfDistance(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            this.checkVectorDimensions(vData.length);
            double max = 0;
            for (int i = 0; i < this.data.length; ++i) {
                final double delta = this.data[i] - vData[i];
                max = MathLib.max(max, MathLib.abs(delta));
            }
            return max;
        } else {
            this.checkVectorDimensions(v);
            double max = 0;
            for (int i = 0; i < this.data.length; ++i) {
                final double delta = this.data[i] - v.getEntry(i);
                max = MathLib.max(max, MathLib.abs(delta));
            }
            return max;
        }
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix outerProduct(final RealVector v) {
        if (v instanceof ArrayRealVector) {
            final double[] vData = ((ArrayRealVector) v).data;
            final int m = this.data.length;
            final int n = vData.length;
            final RealMatrix out = MatrixUtils.createRealMatrix(m, n);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    out.setEntry(i, j, this.data[i] * vData[j]);
                }
            }
            return out;
        } else {
            final int m = this.data.length;
            final int n = v.getDimension();
            final RealMatrix out = MatrixUtils.createRealMatrix(m, n);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    out.setEntry(i, j, this.data[i] * v.getEntry(j));
                }
            }
            return out;
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public double getEntry(final int index) {
        try {
            return this.data[index];
        } catch (final IndexOutOfBoundsException e) {
            throw new OutOfRangeException(PatriusMessages.INDEX, index, 0,
                this.getDimension() - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return this.data.length;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector append(final RealVector v) {
        try {
            return new ArrayRealVector(this, (ArrayRealVector) v);
        } catch (final ClassCastException cce) {
            return new ArrayRealVector(this, v);
        }
    }

    /**
     * Construct a vector by appending a vector to this vector.
     * 
     * @param v
     *        Vector to append to this one.
     * @return a new vector.
     */
    public ArrayRealVector append(final ArrayRealVector v) {
        return new ArrayRealVector(this, v);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector append(final double v) {
        final double[] out = new double[this.data.length + 1];
        System.arraycopy(this.data, 0, out, 0, this.data.length);
        out[this.data.length] = v;
        return new ArrayRealVector(out, false);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getSubVector(final int index, final int n) {
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.NUMBER_OF_ELEMENTS_SHOULD_BE_POSITIVE, n);
        }
        final ArrayRealVector out = new ArrayRealVector(n);
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
    public void setEntry(final int index, final double value) {
        try {
            this.data[index] = value;
        } catch (final IndexOutOfBoundsException e) {
            this.checkIndex(index);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void addToEntry(final int index, final double increment) {
        try {
            this.data[index] += increment;
        } catch (final IndexOutOfBoundsException e) {
            throw new OutOfRangeException(PatriusMessages.INDEX,
                index, 0, this.data.length - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSubVector(final int index, final RealVector v) {
        if (v instanceof ArrayRealVector) {
            this.setSubVector(index, ((ArrayRealVector) v).data);
        } else {
            try {
                for (int i = index; i < index + v.getDimension(); ++i) {
                    this.data[i] = v.getEntry(i - index);
                }
            } catch (final IndexOutOfBoundsException e) {
                this.checkIndex(index);
                this.checkIndex(index + v.getDimension() - 1);
            }
        }
    }

    /**
     * Set a set of consecutive elements.
     * 
     * @param index
     *        Index of first element to be set.
     * @param v
     *        Vector containing the values to set.
     * @throws OutOfRangeException
     *         if the index is inconsistent with the vector
     *         size.
     */
    public void setSubVector(final int index, final double[] v) {
        try {
            System.arraycopy(v, 0, this.data, index, v.length);
        } catch (final IndexOutOfBoundsException e) {
            this.checkIndex(index);
            this.checkIndex(index + v.length - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final double value) {
        Arrays.fill(this.data, value);
    }

    /** {@inheritDoc} */
    @Override
    public double[] toArray() {
        return this.data.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return DEFAULT_FORMAT.format(this);
    }

    /**
     * Check if instance and specified vectors have the same dimension.
     * 
     * @param v
     *        Vector to compare instance with.
     * @throws DimensionMismatchException
     *         if the vectors do not
     *         have the same dimension.
     */
    @Override
    protected void checkVectorDimensions(final RealVector v) {
        this.checkVectorDimensions(v.getDimension());
    }

    /**
     * Check if instance dimension is equal to some expected value.
     * 
     * @param n
     *        Expected dimension.
     * @throws DimensionMismatchException
     *         if the dimension is
     *         inconsistent with vector size.
     */
    @Override
    protected void checkVectorDimensions(final int n) {
        if (this.data.length != n) {
            throw new DimensionMismatchException(this.data.length, n);
        }
    }

    /**
     * Check if any coordinate of this vector is {@code NaN}.
     * 
     * @return {@code true} if any coordinate of this vector is {@code NaN}, {@code false} otherwise.
     */
    @Override
    public boolean isNaN() {
        for (final double v : this.data) {
            if (Double.isNaN(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether any coordinate of this vector is infinite and none
     * are {@code NaN}.
     * 
     * @return {@code true} if any coordinate of this vector is infinite and
     *         none are {@code NaN}, {@code false} otherwise.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public boolean isInfinite() {
        // CHECKSTYLE: resume ReturnCount check
        if (this.isNaN()) {
            return false;
        }

        for (final double v : this.data) {
            if (Double.isInfinite(v)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RealVector)) {
            return false;
        }

        final RealVector rhs = (RealVector) other;
        if (this.data.length != rhs.getDimension()) {
            return false;
        }

        if (rhs.isNaN()) {
            return this.isNaN();
        }

        for (int i = 0; i < this.data.length; ++i) {
            if (this.data[i] != rhs.getEntry(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc} All {@code NaN} values have the same hash code.
     */
    @Override
    public int hashCode() {
        if (this.isNaN()) {
            return C_9;
        }
        return MathUtils.hash(this.data);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector combine(final double a, final double b, final RealVector y) {
        return this.copy().combineToSelf(a, b, y);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayRealVector combineToSelf(final double a, final double b, final RealVector y) {
        if (y instanceof ArrayRealVector) {
            final double[] yData = ((ArrayRealVector) y).data;
            this.checkVectorDimensions(yData.length);
            for (int i = 0; i < this.data.length; i++) {
                this.data[i] = a * this.data[i] + b * yData[i];
            }
        } else {
            this.checkVectorDimensions(y);
            for (int i = 0; i < this.data.length; i++) {
                this.data[i] = a * this.data[i] + b * y.getEntry(i);
            }
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public double walkInDefaultOrder(final RealVectorPreservingVisitor visitor) {
        visitor.start(this.data.length, 0, this.data.length - 1);
        for (int i = 0; i < this.data.length; i++) {
            visitor.visit(i, this.data[i]);
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInDefaultOrder(final RealVectorPreservingVisitor visitor,
                                     final int start, final int end) {
        this.checkIndices(start, end);
        visitor.start(this.data.length, start, end);
        for (int i = start; i <= end; i++) {
            visitor.visit(i, this.data[i]);
        }
        return visitor.end();
    }

    /**
     * {@inheritDoc}
     * 
     * In this implementation, the optimized order is the default order.
     */
    @Override
    public double walkInOptimizedOrder(final RealVectorPreservingVisitor visitor) {
        return this.walkInDefaultOrder(visitor);
    }

    /**
     * {@inheritDoc}
     * 
     * In this implementation, the optimized order is the default order.
     */
    @Override
    public double walkInOptimizedOrder(final RealVectorPreservingVisitor visitor,
                                       final int start, final int end) {
        return this.walkInDefaultOrder(visitor, start, end);
    }

    /** {@inheritDoc} */
    @Override
    public double walkInDefaultOrder(final RealVectorChangingVisitor visitor) {
        visitor.start(this.data.length, 0, this.data.length - 1);
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = visitor.visit(i, this.data[i]);
        }
        return visitor.end();
    }

    /** {@inheritDoc} */
    @Override
    public double walkInDefaultOrder(final RealVectorChangingVisitor visitor,
                                     final int start, final int end) {
        this.checkIndices(start, end);
        visitor.start(this.data.length, start, end);
        for (int i = start; i <= end; i++) {
            this.data[i] = visitor.visit(i, this.data[i]);
        }
        return visitor.end();
    }

    /**
     * {@inheritDoc}
     * 
     * In this implementation, the optimized order is the default order.
     */
    @Override
    public double walkInOptimizedOrder(final RealVectorChangingVisitor visitor) {
        return this.walkInDefaultOrder(visitor);
    }

    /**
     * {@inheritDoc}
     * 
     * In this implementation, the optimized order is the default order.
     */
    @Override
    public double walkInOptimizedOrder(final RealVectorChangingVisitor visitor,
                                       final int start, final int end) {
        return this.walkInDefaultOrder(visitor, start, end);
    }

    // CHECKSTYLE: resume CommentRatio check
}
