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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Add;
import fr.cnes.sirius.patrius.math.analysis.function.Divide;
import fr.cnes.sirius.patrius.math.analysis.function.Multiply;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class defining a real-valued vector with basic algebraic operations.
 * <p>
 * vector element indexing is 0-based -- e.g., {@code getEntry(0)} returns the first element of the vector.
 * </p>
 * <p>
 * The {@code code map} and {@code mapToSelf} methods operate on vectors element-wise, i.e. they perform the same
 * operation (adding a scalar, applying a function ...) on each element in turn. The {@code map} versions create a new
 * vector to hold the result and do not change the instance. The {@code mapToSelf} version uses the instance itself to
 * store the results, so the instance is changed by this method. In all cases, the result vector is returned by the
 * methods, allowing the <i>fluent API</i> style, like this:
 * </p>
 * 
 * <pre>
 * RealVector result = v.mapAddToSelf(3.4).mapToSelf(new Tan()).mapToSelf(new Power(2.3));
 * </pre>
 * 
 * @version $Id: RealVector.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.1
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.ConstructorCallsOverridableMethod"})
public abstract class RealVector {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * Returns the size of the vector.
     * 
     * @return the size of this vector.
     */
    public abstract int getDimension();

    /**
     * Return the entry at the specified index.
     * 
     * @param index
     *        Index location of entry to be fetched.
     * @return the vector entry at {@code index}.
     * @throws OutOfRangeException
     *         if the index is not valid.
     * @see #setEntry(int, double)
     */
    public abstract double getEntry(int index);

    /**
     * Set a single element.
     * 
     * @param index
     *        element index.
     * @param value
     *        new value for the element.
     * @throws OutOfRangeException
     *         if the index is not valid.
     * @see #getEntry(int)
     */
    public abstract void setEntry(int index, double value);

    /**
     * Change an entry at the specified index.
     * 
     * @param index
     *        Index location of entry to be set.
     * @param increment
     *        Value to add to the vector entry.
     * @throws OutOfRangeException
     *         if the index is not valid.
     * @since 3.0
     */
    public void addToEntry(final int index, final double increment) {
        this.setEntry(index, this.getEntry(index) + increment);
    }

    /**
     * Construct a new vector by appending a vector to this vector.
     * 
     * @param v
     *        vector to append to this one.
     * @return a new vector.
     */
    public abstract RealVector append(RealVector v);

    /**
     * Construct a new vector by appending a double to this vector.
     * 
     * @param d
     *        double to append.
     * @return a new vector.
     */
    public abstract RealVector append(double d);

    /**
     * Get a subvector from consecutive elements.
     * 
     * @param index
     *        index of first element.
     * @param n
     *        number of elements to be retrieved.
     * @return a vector containing n elements.
     * @throws OutOfRangeException
     *         if the index is not valid.
     * @throws NotPositiveException
     *         if the number of elements is not positive.
     */
    public abstract RealVector getSubVector(int index, int n);

    /**
     * Set a sequence of consecutive elements.
     * 
     * @param index
     *        index of first element to be set.
     * @param v
     *        vector containing the values to set.
     * @throws OutOfRangeException
     *         if the index is not valid.
     */
    public abstract void setSubVector(int index, RealVector v);

    /**
     * Check whether any coordinate of this vector is {@code NaN}.
     * 
     * @return {@code true} if any coordinate of this vector is {@code NaN}, {@code false} otherwise.
     */
    public abstract boolean isNaN();

    /**
     * Check whether any coordinate of this vector is infinite and none are {@code NaN}.
     * 
     * @return {@code true} if any coordinate of this vector is infinite and
     *         none are {@code NaN}, {@code false} otherwise.
     */
    public abstract boolean isInfinite();

    /**
     * Check if instance and specified vectors have the same dimension.
     * 
     * @param v
     *        Vector to compare instance with.
     * @throws DimensionMismatchException
     *         if the vectors do not
     *         have the same dimension.
     */
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
     *         inconsistent with the vector size.
     */
    protected void checkVectorDimensions(final int n) {
        final int d = this.getDimension();
        if (d != n) {
            throw new DimensionMismatchException(d, n);
        }
    }

    /**
     * Check if an index is valid.
     * 
     * @param index
     *        Index to check.
     * @exception OutOfRangeException
     *            if {@code index} is not valid.
     */
    protected void checkIndex(final int index) {
        if (index < 0 || index >= this.getDimension()) {
            throw new OutOfRangeException(PatriusMessages.INDEX, index, 0, this.getDimension() - 1);
        }
    }

    /**
     * Checks that the indices of a subvector are valid.
     * 
     * @param start
     *        the index of the first entry of the subvector
     * @param end
     *        the index of the last entry of the subvector (inclusive)
     * @throws OutOfRangeException
     *         if {@code start} of {@code end} are not valid
     * @throws NumberIsTooSmallException
     *         if {@code end < start}
     * @since 3.1
     */
    protected void checkIndices(final int start, final int end) {
        final int dim = this.getDimension();
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

    /**
     * Compute the sum of this vector and {@code v}.
     * Returns a new vector. Does not change instance data.
     * 
     * @param v
     *        Vector to be added.
     * @return {@code this} + {@code v}.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     */
    public RealVector add(final RealVector v) {
        this.checkVectorDimensions(v);
        final RealVector result = v.copy();
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            final int index = e.getIndex();
            result.setEntry(index, e.getValue() + result.getEntry(index));
        }
        return result;
    }

    /**
     * Subtract {@code v} from this vector.
     * Returns a new vector. Does not change instance data.
     * 
     * @param v
     *        Vector to be subtracted.
     * @return {@code this} - {@code v}.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     */
    public RealVector subtract(final RealVector v) {
        this.checkVectorDimensions(v);
        final RealVector result = v.mapMultiply(-1d);
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            final int index = e.getIndex();
            result.setEntry(index, e.getValue() + result.getEntry(index));
        }
        return result;
    }

    /**
     * Add a value to each entry.
     * Returns a new vector. Does not change instance data.
     * 
     * @param d
     *        Value to be added to each entry.
     * @return {@code this} + {@code d}.
     */
    public RealVector mapAdd(final double d) {
        return this.copy().mapAddToSelf(d);
    }

    /**
     * Add a value to each entry.
     * The instance is changed in-place.
     * 
     * @param d
     *        Value to be added to each entry.
     * @return {@code this}.
     */
    public RealVector mapAddToSelf(final double d) {
        if (d != 0) {
            return this.mapToSelf(FunctionUtils.fix2ndArgument(new Add(), d));
        }
        return this;
    }

    /**
     * Returns a (deep) copy of this vector.
     * 
     * @return a vector copy.
     */
    public abstract RealVector copy();

    /**
     * Compute the dot product of this vector with {@code v}.
     * 
     * @param v
     *        Vector with which dot product should be computed
     * @return the scalar dot product between this instance and {@code v}.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     */
    public double dotProduct(final RealVector v) {
        this.checkVectorDimensions(v);
        double d = 0;
        final int n = this.getDimension();
        for (int i = 0; i < n; i++) {
            d += this.getEntry(i) * v.getEntry(i);
        }
        return d;
    }

    /**
     * Computes the cosine of the angle between this vector and the
     * argument.
     * 
     * @param v
     *        Vector.
     * @return the cosine of the angle between this vector and {@code v}.
     * @throws MathArithmeticException
     *         if {@code this} or {@code v} is the null
     *         vector
     * @throws DimensionMismatchException
     *         if the dimensions of {@code this} and {@code v} do not match
     */
    public double cosine(final RealVector v) {
        final double norm = this.getNorm();
        final double vNorm = v.getNorm();

        if (norm == 0 ||
            vNorm == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }
        return this.dotProduct(v) / (norm * vNorm);
    }

    /**
     * Element-by-element division.
     *
     * @param v Vector by which instance elements must be divided.
     * @return a vector containing this[i] / v[i] for all i.
     * @throws DimensionMismatchException if {@code v} is not the same size as
     * {@code this} vector.
     */
    public abstract RealVector ebeDivide(RealVector v)
        throws DimensionMismatchException;

    /**
     * Element-by-element multiplication.
     *
     * @param v Vector by which instance elements must be multiplied
     * @return a vector containing this[i] * v[i] for all i.
     * @throws DimensionMismatchException if {@code v} is not the same size as
     * {@code this} vector.
     */
    public abstract RealVector ebeMultiply(RealVector v)
        throws DimensionMismatchException;
    
    /**
     * Distance between two vectors.
     * <p>
     * This method computes the distance consistent with the L<sub>2</sub> norm, i.e. the square root of the sum of
     * element differences, or Euclidean distance.
     * </p>
     * 
     * @param v
     *        Vector to which distance is requested.
     * @return the distance between two vectors.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     * @see #getL1Distance(RealVector)
     * @see #getLInfDistance(RealVector)
     * @see #getNorm()
     */
    public double getDistance(final RealVector v) {
        this.checkVectorDimensions(v);
        double d = 0;
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            final double diff = e.getValue() - v.getEntry(e.getIndex());
            d += diff * diff;
        }
        return MathLib.sqrt(d);
    }

    /**
     * Returns the L<sub>2</sub> norm of the vector.
     * <p>
     * The L<sub>2</sub> norm is the root of the sum of the squared elements.
     * </p>
     * 
     * @return the norm.
     * @see #getL1Norm()
     * @see #getLInfNorm()
     * @see #getDistance(RealVector)
     */
    public double getNorm() {
        double sum = 0;
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            final double value = e.getValue();
            sum += value * value;
        }
        return MathLib.sqrt(sum);
    }

    /**
     * Returns the L<sub>1</sub> norm of the vector.
     * <p>
     * The L<sub>1</sub> norm is the sum of the absolute values of the elements.
     * </p>
     * 
     * @return the norm.
     * @see #getNorm()
     * @see #getLInfNorm()
     * @see #getL1Distance(RealVector)
     */
    public double getL1Norm() {
        double norm = 0;
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            norm += MathLib.abs(e.getValue());
        }
        return norm;
    }

    /**
     * Returns the L<sub>&infin;</sub> norm of the vector.
     * <p>
     * The L<sub>&infin;</sub> norm is the max of the absolute values of the elements.
     * </p>
     * 
     * @return the norm.
     * @see #getNorm()
     * @see #getL1Norm()
     * @see #getLInfDistance(RealVector)
     */
    public double getLInfNorm() {
        double norm = 0;
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            norm = MathLib.max(norm, MathLib.abs(e.getValue()));
        }
        return norm;
    }

    /**
     * Distance between two vectors.
     * <p>
     * This method computes the distance consistent with L<sub>1</sub> norm, i.e. the sum of the absolute values of the
     * elements differences.
     * </p>
     * 
     * @param v
     *        Vector to which distance is requested.
     * @return the distance between two vectors.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     */
    public double getL1Distance(final RealVector v) {
        this.checkVectorDimensions(v);
        double d = 0;
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            d += MathLib.abs(e.getValue() - v.getEntry(e.getIndex()));
        }
        return d;
    }

    /**
     * Distance between two vectors.
     * <p>
     * This method computes the distance consistent with L<sub>&infin;</sub> norm, i.e. the max of the absolute values
     * of element differences.
     * </p>
     * 
     * @param v
     *        Vector to which distance is requested.
     * @return the distance between two vectors.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     * @see #getDistance(RealVector)
     * @see #getL1Distance(RealVector)
     * @see #getLInfNorm()
     */
    public double getLInfDistance(final RealVector v) {
        this.checkVectorDimensions(v);
        double d = 0;
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            d = MathLib.max(MathLib.abs(e.getValue() - v.getEntry(e.getIndex())), d);
        }
        return d;
    }

    /**
     * Get the index of the minimum entry.
     * 
     * @return the index of the minimum entry or -1 if vector length is 0
     *         or all entries are {@code NaN}.
     */
    public int getMinIndex() {
        int minIndex = -1;
        double minValue = Double.POSITIVE_INFINITY;
        final Iterator<Entry> iterator = this.iterator();
        while (iterator.hasNext()) {
            final Entry entry = iterator.next();
            if (entry.getValue() <= minValue) {
                minIndex = entry.getIndex();
                minValue = entry.getValue();
            }
        }
        return minIndex;
    }

    /**
     * Get the value of the minimum entry.
     * 
     * @return the value of the minimum entry or {@code NaN} if all
     *         entries are {@code NaN}.
     */
    public double getMinValue() {
        final int minIndex = this.getMinIndex();
        return minIndex < 0 ? Double.NaN : this.getEntry(minIndex);
    }

    /**
     * Get the index of the maximum entry.
     * 
     * @return the index of the maximum entry or -1 if vector length is 0
     *         or all entries are {@code NaN}
     */
    public int getMaxIndex() {
        int maxIndex = -1;
        double maxValue = Double.NEGATIVE_INFINITY;
        final Iterator<Entry> iterator = this.iterator();
        while (iterator.hasNext()) {
            final Entry entry = iterator.next();
            if (entry.getValue() >= maxValue) {
                maxIndex = entry.getIndex();
                maxValue = entry.getValue();
            }
        }
        return maxIndex;
    }

    /**
     * Get the value of the maximum entry.
     * 
     * @return the value of the maximum entry or {@code NaN} if all
     *         entries are {@code NaN}.
     */
    public double getMaxValue() {
        final int maxIndex = this.getMaxIndex();
        return maxIndex < 0 ? Double.NaN : this.getEntry(maxIndex);
    }

    /**
     * Multiply each entry by the argument. Returns a new vector.
     * Does not change instance data.
     * 
     * @param d
     *        Multiplication factor.
     * @return {@code this} * {@code d}.
     */
    public RealVector mapMultiply(final double d) {
        return this.copy().mapMultiplyToSelf(d);
    }

    /**
     * Multiply each entry.
     * The instance is changed in-place.
     * 
     * @param d
     *        Multiplication factor.
     * @return {@code this}.
     */
    public RealVector mapMultiplyToSelf(final double d) {
        return this.mapToSelf(FunctionUtils.fix2ndArgument(new Multiply(), d));
    }

    /**
     * Subtract a value from each entry. Returns a new vector.
     * Does not change instance data.
     * 
     * @param d
     *        Value to be subtracted.
     * @return {@code this} - {@code d}.
     */
    public RealVector mapSubtract(final double d) {
        return this.copy().mapSubtractToSelf(d);
    }

    /**
     * Subtract a value from each entry.
     * The instance is changed in-place.
     * 
     * @param d
     *        Value to be subtracted.
     * @return {@code this}.
     */
    public RealVector mapSubtractToSelf(final double d) {
        return this.mapAddToSelf(-d);
    }

    /**
     * Divide each entry by the argument. Returns a new vector.
     * Does not change instance data.
     * 
     * @param d
     *        Value to divide by.
     * @return {@code this} / {@code d}.
     */
    public RealVector mapDivide(final double d) {
        return this.copy().mapDivideToSelf(d);
    }

    /**
     * Divide each entry by the argument.
     * The instance is changed in-place.
     * 
     * @param d
     *        Value to divide by.
     * @return {@code this}.
     */
    public RealVector mapDivideToSelf(final double d) {
        return this.mapToSelf(FunctionUtils.fix2ndArgument(new Divide(), d));
    }

    /**
     * Compute the outer product.
     * 
     * @param v
     *        Vector with which outer product should be computed.
     * @return the matrix outer product between this instance and {@code v}.
     */
    public RealMatrix outerProduct(final RealVector v) {
        // Get instance dimension
        final int m = this.getDimension();
        // Get v dimension
        final int n = v.getDimension();
        final RealMatrix product = new Array2DRowRealMatrix(m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                // Compute product for each entry (i,j)
                product.setEntry(i, j, this.getEntry(i) * v.getEntry(j));
            }
        }
        return product;
    }

    /**
     * Find the orthogonal projection of this vector onto another vector.
     * 
     * @param v
     *        vector onto which instance must be projected.
     * @return projection of the instance onto {@code v}.
     * @throws DimensionMismatchException
     *         if {@code v} is not the same size as {@code this} vector.
     * @throws MathArithmeticException
     *         if {@code this} or {@code v} is the null
     *         vector
     */
    public RealVector projection(final RealVector v) {
        final double norm2 = v.dotProduct(v);
        if (norm2 == 0.0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }
        return v.mapMultiply(this.dotProduct(v) / v.dotProduct(v));
    }

    /**
     * Set all elements to a single value.
     * 
     * @param value
     *        Single value to set for all elements.
     */
    public void set(final double value) {
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            e.setValue(value);
        }
    }

    /**
     * Convert the vector to an array of {@code double}s.
     * The array is independent from this vector data: the elements
     * are copied.
     * 
     * @return an array containing a copy of the vector elements.
     */
    public double[] toArray() {
        final int dim = this.getDimension();
        final double[] values = new double[dim];
        for (int i = 0; i < dim; i++) {
            values[i] = this.getEntry(i);
        }
        return values;
    }

    /**
     * Creates a unit vector pointing in the direction of this vector.
     * The instance is not changed by this method.
     * 
     * @return a unit vector pointing in direction of this vector.
     * @throws MathArithmeticException
     *         if the norm is zero.
     */
    public RealVector unitVector() {
        final double norm = this.getNorm();
        if (norm == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }
        return this.mapDivide(norm);
    }

    /**
     * Converts this vector into a unit vector.
     * The instance itself is changed by this method.
     * 
     * @throws MathArithmeticException
     *         if the norm is zero.
     */
    public void unitize() {
        final double norm = this.getNorm();
        if (norm == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }
        this.mapDivideToSelf(this.getNorm());
    }

    /**
     * Generic dense iterator. Iteration is in increasing order
     * of the vector index.
     * 
     * <p>
     * Note: derived classes are required to return an {@link Iterator} that returns non-null {@link Entry} objects as
     * long as {@link Iterator#hasNext()} returns {@code true}.
     * </p>
     * 
     * @return a dense iterator.
     */
    public Iterator<Entry> iterator() {
        final int dim = this.getDimension();
        return new Iterator<Entry>(){

            /** Current index. */
            private int i = 0;

            /** Current entry. */
            private final Entry e = new Entry();

            /** {@inheritDoc} */
            @Override
            public boolean hasNext() {
                return this.i < dim;
            }

            /** {@inheritDoc} */
            @Override
            public Entry next() {
                if (this.i < dim) {
                    this.e.setIndex(this.i++);
                    return this.e;
                } else {
                    throw new NoSuchElementException();
                }
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all circumstances.
             */
            @Override
            public void remove() {
                throw new MathUnsupportedOperationException();
            }
        };
    }

    /**
     * Acts as if implemented as:
     * 
     * <pre>
     * return copy().mapToSelf(function);
     * </pre>
     * 
     * Returns a new vector. Does not change instance data.
     * 
     * @param function
     *        Function to apply to each entry.
     * @return a new vector.
     */
    public RealVector map(final UnivariateFunction function) {
        return this.copy().mapToSelf(function);
    }

    /**
     * Acts as if it is implemented as:
     * 
     * <pre>
     * Entry e = null;
     * for (Iterator&lt;Entry&gt; it = iterator(); it.hasNext(); e = it.next()) {
     *     e.setValue(function.value(e.getValue()));
     * }
     * </pre>
     * 
     * Entries of this vector are modified in-place by this method.
     * 
     * @param function
     *        Function to apply to each entry.
     * @return a reference to this vector.
     */
    public RealVector mapToSelf(final UnivariateFunction function) {
        final Iterator<Entry> it = this.iterator();
        while (it.hasNext()) {
            final Entry e = it.next();
            e.setValue(function.value(e.getValue()));
        }
        return this;
    }

    /**
     * Returns a new vector representing {@code a * this + b * y}, the linear
     * combination of {@code this} and {@code y}.
     * Returns a new vector. Does not change instance data.
     * 
     * @param a
     *        Coefficient of {@code this}.
     * @param b
     *        Coefficient of {@code y}.
     * @param y
     *        Vector with which {@code this} is linearly combined.
     * @return a vector containing {@code a * this[i] + b * y[i]} for all {@code i}.
     * @throws DimensionMismatchException
     *         if {@code y} is not the same size as {@code this} vector.
     */
    public RealVector combine(final double a, final double b, final RealVector y) {
        return this.copy().combineToSelf(a, b, y);
    }

    /**
     * Updates {@code this} with the linear combination of {@code this} and {@code y}.
     * 
     * @param a
     *        Weight of {@code this}.
     * @param b
     *        Weight of {@code y}.
     * @param y
     *        Vector with which {@code this} is linearly combined.
     * @return {@code this}, with components equal to {@code a * this[i] + b * y[i]} for all {@code i}.
     * @throws DimensionMismatchException
     *         if {@code y} is not the same size as {@code this} vector.
     */
    public RealVector combineToSelf(final double a, final double b, final RealVector y) {
        this.checkVectorDimensions(y);
        for (int i = 0; i < this.getDimension(); i++) {
            final double xi = this.getEntry(i);
            final double yi = y.getEntry(i);
            this.setEntry(i, a * xi + b * yi);
        }
        return this;
    }

    /**
     * Visits (but does not alter) all entries of this vector in default order
     * (increasing index).
     * 
     * @param visitor
     *        the visitor to be used to process the entries of this
     *        vector
     * @return the value returned by {@link RealVectorPreservingVisitor#end()} at the end of the walk
     * @since 3.1
     */
    public double walkInDefaultOrder(final RealVectorPreservingVisitor visitor) {
        final int dim = this.getDimension();
        visitor.start(dim, 0, dim - 1);
        for (int i = 0; i < dim; i++) {
            visitor.visit(i, this.getEntry(i));
        }
        return visitor.end();
    }

    /**
     * Visits (but does not alter) some entries of this vector in default order
     * (increasing index).
     * 
     * @param visitor
     *        visitor to be used to process the entries of this vector
     * @param start
     *        the index of the first entry to be visited
     * @param end
     *        the index of the last entry to be visited (inclusive)
     * @return the value returned by {@link RealVectorPreservingVisitor#end()} at the end of the walk
     * @throws NumberIsTooSmallException
     *         if {@code end < start}.
     * @throws OutOfRangeException
     *         if the indices are not valid.
     * @since 3.1
     */
    public double walkInDefaultOrder(final RealVectorPreservingVisitor visitor,
                                     final int start, final int end) {
        this.checkIndices(start, end);
        visitor.start(this.getDimension(), start, end);
        for (int i = start; i <= end; i++) {
            visitor.visit(i, this.getEntry(i));
        }
        return visitor.end();
    }

    /**
     * Visits (but does not alter) all entries of this vector in optimized
     * order. The order in which the entries are visited is selected so as to
     * lead to the most efficient implementation; it might depend on the
     * concrete implementation of this abstract class.
     * 
     * @param visitor
     *        the visitor to be used to process the entries of this
     *        vector
     * @return the value returned by {@link RealVectorPreservingVisitor#end()} at the end of the walk
     * @since 3.1
     */
    public double walkInOptimizedOrder(final RealVectorPreservingVisitor visitor) {
        return this.walkInDefaultOrder(visitor);
    }

    /**
     * Visits (but does not alter) some entries of this vector in optimized
     * order. The order in which the entries are visited is selected so as to
     * lead to the most efficient implementation; it might depend on the
     * concrete implementation of this abstract class.
     * 
     * @param visitor
     *        visitor to be used to process the entries of this vector
     * @param start
     *        the index of the first entry to be visited
     * @param end
     *        the index of the last entry to be visited (inclusive)
     * @return the value returned by {@link RealVectorPreservingVisitor#end()} at the end of the walk
     * @throws NumberIsTooSmallException
     *         if {@code end < start}.
     * @throws OutOfRangeException
     *         if the indices are not valid.
     * @since 3.1
     */
    public double walkInOptimizedOrder(final RealVectorPreservingVisitor visitor,
                                       final int start, final int end) {
        return this.walkInDefaultOrder(visitor, start, end);
    }

    /**
     * Visits (and possibly alters) all entries of this vector in default order
     * (increasing index).
     * 
     * @param visitor
     *        the visitor to be used to process and modify the entries
     *        of this vector
     * @return the value returned by {@link RealVectorChangingVisitor#end()} at the end of the walk
     * @since 3.1
     */
    public double walkInDefaultOrder(final RealVectorChangingVisitor visitor) {
        final int dim = this.getDimension();
        visitor.start(dim, 0, dim - 1);
        for (int i = 0; i < dim; i++) {
            this.setEntry(i, visitor.visit(i, this.getEntry(i)));
        }
        return visitor.end();
    }

    /**
     * Visits (and possibly alters) some entries of this vector in default order
     * (increasing index).
     * 
     * @param visitor
     *        visitor to be used to process the entries of this vector
     * @param start
     *        the index of the first entry to be visited
     * @param end
     *        the index of the last entry to be visited (inclusive)
     * @return the value returned by {@link RealVectorChangingVisitor#end()} at the end of the walk
     * @throws NumberIsTooSmallException
     *         if {@code end < start}.
     * @throws OutOfRangeException
     *         if the indices are not valid.
     * @since 3.1
     */
    public double walkInDefaultOrder(final RealVectorChangingVisitor visitor,
                                     final int start, final int end) {
        this.checkIndices(start, end);
        visitor.start(this.getDimension(), start, end);
        for (int i = start; i <= end; i++) {
            this.setEntry(i, visitor.visit(i, this.getEntry(i)));
        }
        return visitor.end();
    }

    /**
     * Visits (and possibly alters) all entries of this vector in optimized
     * order. The order in which the entries are visited is selected so as to
     * lead to the most efficient implementation; it might depend on the
     * concrete implementation of this abstract class.
     * 
     * @param visitor
     *        the visitor to be used to process the entries of this
     *        vector
     * @return the value returned by {@link RealVectorChangingVisitor#end()} at the end of the walk
     * @since 3.1
     */
    public double walkInOptimizedOrder(final RealVectorChangingVisitor visitor) {
        return this.walkInDefaultOrder(visitor);
    }

    /**
     * Visits (and possibly change) some entries of this vector in optimized
     * order. The order in which the entries are visited is selected so as to
     * lead to the most efficient implementation; it might depend on the
     * concrete implementation of this abstract class.
     * 
     * @param visitor
     *        visitor to be used to process the entries of this vector
     * @param start
     *        the index of the first entry to be visited
     * @param end
     *        the index of the last entry to be visited (inclusive)
     * @return the value returned by {@link RealVectorChangingVisitor#end()} at the end of the walk
     * @throws NumberIsTooSmallException
     *         if {@code end < start}.
     * @throws OutOfRangeException
     *         if the indices are not valid.
     * @since 3.1
     */
    public double walkInOptimizedOrder(final RealVectorChangingVisitor visitor,
                                       final int start, final int end) {
        return this.walkInDefaultOrder(visitor, start, end);
    }

    /**
     * <p>
     * Test for the equality of two real vectors. If all coordinates of two real vectors are exactly the same, and none
     * are {@code NaN}, the two real vectors are considered to be equal. {@code NaN} coordinates are considered to
     * affect globally the vector and be equals to each other - i.e, if either (or all) coordinates of the real vector
     * are equal to {@code NaN}, the real vector is equal to a vector with all {@code NaN} coordinates.
     * </p>
     * <p>
     * This method <em>must</em> be overriden by concrete subclasses of {@link RealVector} (the current implementation
     * throws an exception).
     * </p>
     * 
     * @param other
     *        Object to test for equality.
     * @return {@code true} if two vector objects are equal, {@code false} if {@code other} is null, not an instance of
     *         {@code RealVector}, or
     *         not equal to this {@code RealVector} instance.
     * @throws MathUnsupportedOperationException
     *         if this method is not
     *         overridden.
     */
    @Override
    public boolean equals(final Object other) {
        throw new MathUnsupportedOperationException();
    }

    /**
     * {@inheritDoc}. This method <em>must</em> be overriden by concrete
     * subclasses of {@link RealVector} (current implementation throws an
     * exception).
     * 
     * @throws MathUnsupportedOperationException
     *         if this method is not
     *         overridden.
     */
    @Override
    public int hashCode() {
        throw new MathUnsupportedOperationException();
    }

    /**
     * Returns an unmodifiable view of the specified vector.
     * The returned vector has read-only access. An attempt to modify it will
     * result in a {@link MathUnsupportedOperationException}. However, the
     * returned vector is <em>not</em> immutable, since any modification of {@code v} will also change the returned
     * view.
     * For example, in the following piece of code
     * 
     * <pre>
     * RealVector v = new ArrayRealVector(2);
     * RealVector w = RealVector.unmodifiableRealVector(v);
     * v.setEntry(0, 1.2);
     * v.setEntry(1, -3.4);
     * </pre>
     * 
     * the changes will be seen in the {@code w} view of {@code v}.
     * 
     * @param v
     *        Vector for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of {@code v}.
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    public static RealVector unmodifiableRealVector(final RealVector v) {
        // CHECKSTYLE: resume MethodLength check

        /**
         * This anonymous class is an implementation of {@link RealVector} with read-only access.
         * It wraps any {@link RealVector}, and exposes all methods which
         * do not modify it. Invoking methods which should normally result
         * in the modification of the calling {@link RealVector} results in
         * a {@link MathUnsupportedOperationException}. It should be noted
         * that {@link UnmodifiableVector} is <em>not</em> immutable.
         */
        return new RealVector(){
            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all circumstances.
             */
            @Override
            public RealVector mapToSelf(final UnivariateFunction function) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector map(final UnivariateFunction function) {
                return v.map(function);
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<Entry> iterator() {
                final Iterator<Entry> i = v.iterator();
                return new Iterator<Entry>(){
                    /** The current entry. */
                    private final UnmodifiableEntry e = new UnmodifiableEntry();

                    /** {@inheritDoc} */
                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    /** {@inheritDoc} */
                    @Override
                    public Entry next() {
                        this.e.setIndex(i.next().getIndex());
                        return this.e;
                    }

                    /**
                     * {@inheritDoc}
                     * 
                     * @throws MathUnsupportedOperationException
                     *         in all
                     *         circumstances.
                     */
                    @Override
                    public void remove() {
                        throw new MathUnsupportedOperationException();
                    }
                };
            }

            /** {@inheritDoc} */
            @Override
            public RealVector copy() {
                return v.copy();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector add(final RealVector w) {
                return v.add(w);
            }

            /** {@inheritDoc} */
            @Override
            public RealVector subtract(final RealVector w) {
                return v.subtract(w);
            }

            /** {@inheritDoc} */
            @Override
            public RealVector mapAdd(final double d) {
                return v.mapAdd(d);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public RealVector mapAddToSelf(final double d) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector mapSubtract(final double d) {
                return v.mapSubtract(d);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public RealVector mapSubtractToSelf(final double d) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector mapMultiply(final double d) {
                return v.mapMultiply(d);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public RealVector mapMultiplyToSelf(final double d) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector mapDivide(final double d) {
                return v.mapDivide(d);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public RealVector mapDivideToSelf(final double d) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector ebeMultiply(final RealVector w) {
                return v.ebeMultiply(w);
            }

            /** {@inheritDoc} */
            @Override
            public RealVector ebeDivide(final RealVector w) {
                return v.ebeDivide(w);
            }

            /** {@inheritDoc} */
            @Override
            public double dotProduct(final RealVector w) {
                return v.dotProduct(w);
            }

            /** {@inheritDoc} */
            @Override
            public double cosine(final RealVector w) {
                return v.cosine(w);
            }

            /** {@inheritDoc} */
            @Override
            public double getNorm() {
                return v.getNorm();
            }

            /** {@inheritDoc} */
            @Override
            public double getL1Norm() {
                return v.getL1Norm();
            }

            /** {@inheritDoc} */
            @Override
            public double getLInfNorm() {
                return v.getLInfNorm();
            }

            /** {@inheritDoc} */
            @Override
            public double getDistance(final RealVector w) {
                return v.getDistance(w);
            }

            /** {@inheritDoc} */
            @Override
            public double getL1Distance(final RealVector w) {
                return v.getL1Distance(w);
            }

            /** {@inheritDoc} */
            @Override
            public double getLInfDistance(final RealVector w) {
                return v.getLInfDistance(w);
            }

            /** {@inheritDoc} */
            @Override
            public RealVector unitVector() {
                return v.unitVector();
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public void unitize() {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public RealMatrix outerProduct(final RealVector w) {
                return v.outerProduct(w);
            }

            /** {@inheritDoc} */
            @Override
            public double getEntry(final int index) {
                return v.getEntry(index);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public void setEntry(final int index, final double value) {
                throw new MathUnsupportedOperationException();
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public void addToEntry(final int index, final double value) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public int getDimension() {
                return v.getDimension();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector append(final RealVector w) {
                return v.append(w);
            }

            /** {@inheritDoc} */
            @Override
            public RealVector append(final double d) {
                return v.append(d);
            }

            /** {@inheritDoc} */
            @Override
            public RealVector getSubVector(final int index, final int n) {
                return v.getSubVector(index, n);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public void setSubVector(final int index, final RealVector w) {
                throw new MathUnsupportedOperationException();
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public void set(final double value) {
                throw new MathUnsupportedOperationException();
            }

            /** {@inheritDoc} */
            @Override
            public double[] toArray() {
                return v.toArray();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isNaN() {
                return v.isNaN();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isInfinite() {
                return v.isInfinite();
            }

            /** {@inheritDoc} */
            @Override
            public RealVector combine(final double a, final double b, final RealVector y) {
                return v.combine(a, b, y);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws MathUnsupportedOperationException
             *         in all
             *         circumstances.
             */
            @Override
            public RealVector combineToSelf(final double a, final double b, final RealVector y) {
                throw new MathUnsupportedOperationException();
            }

            /** An entry in the vector. */
            class UnmodifiableEntry extends Entry {
                /** {@inheritDoc} */
                @Override
                public double getValue() {
                    return v.getEntry(this.getIndex());
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @throws MathUnsupportedOperationException
                 *         in all
                 *         circumstances.
                 */
                @Override
                public void setValue(final double value) {
                    throw new MathUnsupportedOperationException();
                }
            }
        };
    }

    /** An entry in the vector. */
    protected class Entry {
        /** Index of this entry. */
        private int index;

        /** Simple constructor. */
        public Entry() {
            this.setIndex(0);
        }

        /**
         * Get the value of the entry.
         * 
         * @return the value of the entry.
         */
        public double getValue() {
            return RealVector.this.getEntry(this.getIndex());
        }

        /**
         * Set the value of the entry.
         * 
         * @param value
         *        New value for the entry.
         */
        public void setValue(final double value) {
            RealVector.this.setEntry(this.getIndex(), value);
        }

        /**
         * Get the index of the entry.
         * 
         * @return the index of the entry.
         */
        public int getIndex() {
            return this.index;
        }

        /**
         * Set the index of the entry.
         * 
         * @param indexIn
         *        New index for the entry.
         */
        public void setIndex(final int indexIn) {
            this.index = indexIn;
        }
    }
}
