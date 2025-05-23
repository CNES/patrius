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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Arrays utilities.
 * 
 * @since 3.0
 * @version $Id: MathArrays.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class MathArrays {
    /** Factor used for splitting double numbers: n = 2^27 + 1 (i.e. {@value} ). */
    private static final int SPLIT_FACTOR = 0x8000001;

    /**
     * Private constructor.
     */
    private MathArrays() {
    }

    /**
     * Creates an array whose contents will be the element-by-element
     * addition of the arguments.
     * 
     * @param a
     *        First term of the addition.
     * @param b
     *        Second term of the addition.
     * @return a new array {@code r} where {@code r[i] = a[i] + b[i]}.
     * @throws DimensionMismatchException
     *         if the array lengths differ.
     * @since 3.1
     */
    public static double[] ebeAdd(final double[] a,
                                  final double[] b) {
        if (a.length != b.length) {
            throw new DimensionMismatchException(a.length, b.length);
        }

        final double[] result = a.clone();
        for (int i = 0; i < a.length; i++) {
            result[i] += b[i];
        }
        return result;
    }

    /**
     * Creates an array whose contents will be the element-by-element
     * subtraction of the second argument from the first.
     * 
     * @param a
     *        First term.
     * @param b
     *        Element to be subtracted.
     * @return a new array {@code r} where {@code r[i] = a[i] - b[i]}.
     * @throws DimensionMismatchException
     *         if the array lengths differ.
     * @since 3.1
     */
    public static double[] ebeSubtract(final double[] a,
                                       final double[] b) {
        if (a.length != b.length) {
            throw new DimensionMismatchException(a.length, b.length);
        }

        final double[] result = a.clone();
        for (int i = 0; i < a.length; i++) {
            result[i] -= b[i];
        }
        return result;
    }

    /**
     * Creates an array whose contents will be the element-by-element
     * multiplication of the arguments.
     * 
     * @param a
     *        First factor of the multiplication.
     * @param b
     *        Second factor of the multiplication.
     * @return a new array {@code r} where {@code r[i] = a[i] * b[i]}.
     * @throws DimensionMismatchException
     *         if the array lengths differ.
     * @since 3.1
     */
    public static double[] ebeMultiply(final double[] a,
                                       final double[] b) {
        if (a.length != b.length) {
            throw new DimensionMismatchException(a.length, b.length);
        }

        final double[] result = a.clone();
        for (int i = 0; i < a.length; i++) {
            result[i] *= b[i];
        }
        return result;
    }

    /**
     * Creates an array whose contents will be the element-by-element
     * division of the first argument by the second.
     * 
     * @param a
     *        Numerator of the division.
     * @param b
     *        Denominator of the division.
     * @return a new array {@code r} where {@code r[i] = a[i] / b[i]}.
     * @throws DimensionMismatchException
     *         if the array lengths differ.
     * @since 3.1
     */
    public static double[] ebeDivide(final double[] a,
                                     final double[] b) {
        if (a.length != b.length) {
            throw new DimensionMismatchException(a.length, b.length);
        }

        final double[] result = a.clone();
        for (int i = 0; i < a.length; i++) {
            result[i] /= b[i];
        }
        return result;
    }

    /**
     * Calculates the L<sub>1</sub> (sum of abs) distance between two points.
     * 
     * @param p1
     *        the first point
     * @param p2
     *        the second point
     * @return the L<sub>1</sub> distance between the two points
     */
    public static double distance1(final double[] p1, final double[] p2) {
        double sum = 0;
        for (int i = 0; i < p1.length; i++) {
            sum += MathLib.abs(p1[i] - p2[i]);
        }
        return sum;
    }

    /**
     * Calculates the L<sub>1</sub> (sum of abs) distance between two points.
     * 
     * @param p1
     *        the first point
     * @param p2
     *        the second point
     * @return the L<sub>1</sub> distance between the two points
     */
    public static int distance1(final int[] p1, final int[] p2) {
        int sum = 0;
        for (int i = 0; i < p1.length; i++) {
            sum += MathLib.abs(p1[i] - p2[i]);
        }
        return sum;
    }

    /**
     * Calculates the L<sub>2</sub> (Euclidean) distance between two points.
     * 
     * @param p1
     *        the first point
     * @param p2
     *        the second point
     * @return the L<sub>2</sub> distance between the two points
     */
    public static double distance(final double[] p1, final double[] p2) {
        double sum = 0;
        for (int i = 0; i < p1.length; i++) {
            final double dp = p1[i] - p2[i];
            sum += dp * dp;
        }
        return MathLib.sqrt(sum);
    }

    /**
     * Calculates the L<sub>2</sub> (Euclidean) distance between two points.
     * 
     * @param p1
     *        the first point
     * @param p2
     *        the second point
     * @return the L<sub>2</sub> distance between the two points
     */
    public static double distance(final int[] p1, final int[] p2) {
        double sum = 0;
        for (int i = 0; i < p1.length; i++) {
            final double dp = p1[i] - p2[i];
            sum += dp * dp;
        }
        return MathLib.sqrt(sum);
    }

    /**
     * Calculates the L<sub>&infin;</sub> (max of abs) distance between two points.
     * 
     * @param p1
     *        the first point
     * @param p2
     *        the second point
     * @return the L<sub>&infin;</sub> distance between the two points
     */
    public static double distanceInf(final double[] p1, final double[] p2) {
        double max = 0;
        for (int i = 0; i < p1.length; i++) {
            max = MathLib.max(max, MathLib.abs(p1[i] - p2[i]));
        }
        return max;
    }

    /**
     * Calculates the L<sub>&infin;</sub> (max of abs) distance between two points.
     * 
     * @param p1
     *        the first point
     * @param p2
     *        the second point
     * @return the L<sub>&infin;</sub> distance between the two points
     */
    public static int distanceInf(final int[] p1, final int[] p2) {
        int max = 0;
        for (int i = 0; i < p1.length; i++) {
            max = MathLib.max(max, MathLib.abs(p1[i] - p2[i]));
        }
        return max;
    }

    /**
     * Specification of ordering direction.
     */
    public static enum OrderDirection {
        /** Constant for increasing direction. */
        INCREASING,
        /** Constant for decreasing direction. */
        DECREASING
    }

    /**
     * Check that an array is monotonically increasing or decreasing.
     * 
     * @param <T>
     *        the type of the elements in the specified array
     * @param val
     *        Values.
     * @param dir
     *        Ordering direction.
     * @param strict
     *        Whether the order should be strict.
     * @return {@code true} if sorted, {@code false} otherwise.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static <T extends Comparable<? super T>> boolean isMonotonic(final T[] val,
                                                                        final OrderDirection dir,
                                                                        final boolean strict) {
        // CHECKSTYLE: resume ReturnCount check
        T previous = val[0];
        final int max = val.length;
        for (int i = 1; i < max; i++) {
            final int comp;
            switch (dir) {
                case INCREASING:
                    comp = previous.compareTo(val[i]);
                    if (strict) {
                        if (comp >= 0) {
                            return false;
                        }
                    } else {
                        if (comp > 0) {
                            return false;
                        }
                    }
                    break;
                case DECREASING:
                    comp = val[i].compareTo(previous);
                    if (strict) {
                        if (comp >= 0) {
                            return false;
                        }
                    } else {
                        if (comp > 0) {
                            return false;
                        }
                    }
                    break;
                default:
                    // Should never happen.
                    throw new MathInternalError();
            }

            previous = val[i];
        }
        return true;
    }

    /**
     * Check that an array is monotonically increasing or decreasing.
     * 
     * @param val
     *        Values.
     * @param dir
     *        Ordering direction.
     * @param strict
     *        Whether the order should be strict.
     * @return {@code true} if sorted, {@code false} otherwise.
     */
    public static boolean isMonotonic(final double[] val,
                                      final OrderDirection dir,
                                      final boolean strict) {
        return checkOrder(val, dir, strict, false);
    }

    /**
     * Check that the given array is sorted.
     * 
     * @param val
     *        Values.
     * @param dir
     *        Ordering direction.
     * @param strict
     *        Whether the order should be strict.
     * @param abort
     *        Whether to throw an exception if the check fails.
     * @return {@code true} if the array is sorted.
     * @throws NonMonotonicSequenceException
     *         if the array is not sorted
     *         and {@code abort} is {@code true}.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static boolean checkOrder(final double[] val, final OrderDirection dir,
                                     final boolean strict, final boolean abort) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        double previous = val[0];
        final int max = val.length;

        int index;
        ITEM: for (index = 1; index < max; index++) {
            switch (dir) {
                case INCREASING:
                    if (strict) {
                        if (val[index] <= previous) {
                            break ITEM;
                        }
                    } else {
                        if (val[index] < previous) {
                            break ITEM;
                        }
                    }
                    break;
                case DECREASING:
                    if (strict) {
                        if (val[index] >= previous) {
                            break ITEM;
                        }
                    } else {
                        if (val[index] > previous) {
                            break ITEM;
                        }
                    }
                    break;
                default:
                    // Should never happen.
                    throw new MathInternalError();
            }

            previous = val[index];
        }

        if (index == max) {
            // Loop completed.
            return true;
        }

        // Loop early exit means wrong ordering.
        if (abort) {
            throw new NonMonotonicSequenceException(val[index], previous, index, dir, strict);
        }
        return false;
    }

    /**
     * Check that the given array is sorted.
     * 
     * @param val
     *        Values.
     * @param dir
     *        Ordering direction.
     * @param strict
     *        Whether the order should be strict.
     * @throws NonMonotonicSequenceException
     *         if the array is not sorted.
     * @since 2.2
     */
    public static void checkOrder(final double[] val, final OrderDirection dir,
                                  final boolean strict) {
        checkOrder(val, dir, strict, true);
    }

    /**
     * Check that the given array is sorted in strictly increasing order.
     * 
     * @param val
     *        Values.
     * @throws NonMonotonicSequenceException
     *         if the array is not sorted.
     * @since 2.2
     */
    public static void checkOrder(final double[] val) {
        checkOrder(val, OrderDirection.INCREASING, true);
    }

    /**
     * Throws DimensionMismatchException if the input array is not rectangular.
     * 
     * @param matrix
     *        array to be tested
     * @throws NullArgumentException
     *         if input array is null
     * @throws DimensionMismatchException
     *         if input array is not rectangular
     * @since 3.1
     */
    public static void checkRectangular(final long[][] matrix) {
        MathUtils.checkNotNull(matrix);
        for (int i = 1; i < matrix.length; i++) {
            if (matrix[i].length != matrix[0].length) {
                throw new DimensionMismatchException(
                    PatriusMessages.DIFFERENT_ROWS_LENGTHS,
                    matrix[i].length, matrix[0].length);
            }
        }
    }

    /**
     * Check that all entries of the input array are strictly positive.
     * 
     * @param matrix
     *        Array to be tested
     * @throws NotStrictlyPositiveException
     *         if any entries of the array are not
     *         strictly positive.
     * @since 3.1
     */
    public static void checkPositive(final double[] matrix) {
        for (final double element : matrix) {
            if (element <= 0) {
                throw new NotStrictlyPositiveException(element);
            }
        }
    }

    /**
     * Check that all entries of the input array are >= 0.
     * 
     * @param matrix
     *        Array to be tested
     * @throws NotPositiveException
     *         if any array entries are less than 0.
     * @since 3.1
     */
    public static void checkNonNegative(final long[] matrix) {
        for (final long element : matrix) {
            if (element < 0) {
                throw new NotPositiveException(element);
            }
        }
    }

    /**
     * Check all entries of the input array are >= 0.
     * 
     * @param matrix
     *        Array to be tested
     * @throws NotPositiveException
     *         if any array entries are less than 0.
     * @since 3.1
     */
    public static void checkNonNegative(final long[][] matrix) {
        for (final long[] element : matrix) {
            for (int j = 0; j < element.length; j++) {
                if (element[j] < 0) {
                    throw new NotPositiveException(element[j]);
                }
            }
        }
    }

    /**
     * Returns the Cartesian norm (2-norm), handling both overflow and underflow.
     * Translation of the minpack enorm subroutine.
     * 
     * The redistribution policy for MINPACK is available
     * <a href="http://www.netlib.org/minpack/disclaimer">here</a>, for
     * convenience, it is reproduced below.</p>
     * 
     * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
     * <tr>
     * <td>
     * Minpack Copyright Notice (1999) University of Chicago. All rights reserved</td>
     * </tr>
     * <tr>
     * <td>
     * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
     * following conditions are met:
     * <ol>
     * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the
     * following disclaimer.</li>
     * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
     * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
     * <li>The end-user documentation included with the redistribution, if any, must include the following
     * acknowledgment: {@code This product includes software developed by the University of
     * Chicago, as Operator of Argonne National Laboratory.} Alternately, this acknowledgment may appear in the software
     * itself, if and wherever such third-party acknowledgments normally appear.</li>
     * <li><strong>WARRANTY DISCLAIMER. THE SOFTWARE IS SUPPLIED "AS IS" WITHOUT WARRANTY OF ANY KIND. THE COPYRIGHT
     * HOLDER, THE UNITED STATES, THE UNITED STATES DEPARTMENT OF ENERGY, AND THEIR EMPLOYEES: (1) DISCLAIM ANY
     * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS
     * FOR A PARTICULAR PURPOSE, TITLE OR NON-INFRINGEMENT, (2) DO NOT ASSUME ANY LEGAL LIABILITY OR RESPONSIBILITY FOR
     * THE ACCURACY, COMPLETENESS, OR USEFULNESS OF THE SOFTWARE, (3) DO NOT REPRESENT THAT USE OF THE SOFTWARE WOULD
     * NOT INFRINGE PRIVATELY OWNED RIGHTS, (4) DO NOT WARRANT THAT THE SOFTWARE WILL FUNCTION UNINTERRUPTED, THAT IT IS
     * ERROR-FREE OR THAT ANY ERRORS WILL BE CORRECTED.</strong></li>
     * <li><strong>LIMITATION OF LIABILITY. IN NO EVENT WILL THE COPYRIGHT HOLDER, THE UNITED STATES, THE UNITED STATES
     * DEPARTMENT OF ENERGY, OR THEIR EMPLOYEES: BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL OR
     * PUNITIVE DAMAGES OF ANY KIND OR NATURE, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS OR LOSS OF DATA, FOR ANY
     * REASON WHATSOEVER, WHETHER SUCH LIABILITY IS ASSERTED ON THE BASIS OF CONTRACT, TORT (INCLUDING NEGLIGENCE OR
     * STRICT LIABILITY), OR OTHERWISE, EVEN IF ANY OF SAID PARTIES HAS BEEN WARNED OF THE POSSIBILITY OF SUCH LOSS OR
     * DAMAGES.</strong></li>
     * <ol></td>
     * </tr>
     * </table>
     * 
     * @param v
     *        Vector of doubles.
     * @return the 2-norm of the vector.
     * @since 2.2
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static double safeNorm(final double[] v) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final double rdwarf = 3.834e-20;
        final double rgiant = 1.304e+19;
        double s1 = 0;
        double s2 = 0;
        double s3 = 0;
        double x1max = 0;
        double x3max = 0;
        final double floatn = v.length;
        final double agiant = rgiant / floatn;
        for (final double element : v) {
            final double xabs = Math.abs(element);
            if (xabs < rdwarf || xabs > agiant) {
                if (xabs > rdwarf) {
                    if (xabs > x1max) {
                        final double r = x1max / xabs;
                        s1 = 1 + s1 * r * r;
                        x1max = xabs;
                    } else {
                        final double r = xabs / x1max;
                        s1 += r * r;
                    }
                } else {
                    if (xabs > x3max) {
                        final double r = x3max / xabs;
                        s3 = 1 + s3 * r * r;
                        x3max = xabs;
                    } else {
                        if (xabs != 0) {
                            final double r = xabs / x3max;
                            s3 += r * r;
                        }
                    }
                }
            } else {
                s2 += xabs * xabs;
            }
        }
        final double norm;
        if (s1 == 0) {
            if (s2 == 0) {
                norm = x3max * Math.sqrt(s3);
            } else {
                if (s2 >= x3max) {
                    norm = Math.sqrt(s2 * (1 + (x3max / s2) * (x3max * s3)));
                } else {
                    norm = Math.sqrt(x3max * ((s2 / x3max) + (x3max * s3)));
                }
            }
        } else {
            norm = x1max * Math.sqrt(s1 + (s2 / x1max) / x1max);
        }
        return norm;
    }

    /**
     * Sort an array in ascending order in place and perform the same reordering
     * of entries on other arrays. For example, if {@code x = [3, 1, 2], y = [1, 2, 3]} and {@code z = [0, 5, 7]}, then
     * {@code sortInPlace(x, y, z)} will update {@code x} to {@code [1, 2, 3]}, {@code y} to {@code [2, 3, 1]} and
     * {@code z} to {@code [5, 7, 0]}.
     * 
     * @param x
     *        Array to be sorted and used as a pattern for permutation
     *        of the other arrays.
     * @param yList
     *        Set of arrays whose permutations of entries will follow
     *        those performed on {@code x}.
     * @throws DimensionMismatchException
     *         if any {@code y} is not the same
     *         size as {@code x}.
     * @throws NullArgumentException
     *         if {@code x} or any {@code y} is null.
     * @since 3.0
     */
    public static void sortInPlace(final double[] x, final double[]... yList) {
        sortInPlace(x, OrderDirection.INCREASING, yList);
    }

    /**
     * Sort an array in place and perform the same reordering of entries on
     * other arrays. This method works the same as the other {@link #sortInPlace(double[], double[][]) sortInPlace}
     * method, but
     * allows the order of the sort to be provided in the {@code dir} parameter.
     * 
     * @param x
     *        Array to be sorted and used as a pattern for permutation
     *        of the other arrays.
     * @param dir
     *        Order direction.
     * @param yList
     *        Set of arrays whose permutations of entries will follow
     *        those performed on {@code x}.
     * @throws DimensionMismatchException
     *         if any {@code y} is not the same
     *         size as {@code x}.
     * @throws NullArgumentException
     *         if {@code x} or any {@code y} is null
     * @since 3.0
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public static void sortInPlace(final double[] x,
                                   final OrderDirection dir,
                                   final double[]... yList) {
        if (x == null) {
            throw new NullArgumentException();
        }

        final int len = x.length;
        final List<Pair<Double, double[]>> list = new ArrayList<>(len);

        final int yListLen = yList.length;
        for (int i = 0; i < len; i++) {
            final double[] yValues = new double[yListLen];
            for (int j = 0; j < yListLen; j++) {
                final double[] y = yList[j];
                if (y == null) {
                    throw new NullArgumentException();
                }
                if (y.length != len) {
                    throw new DimensionMismatchException(y.length, len);
                }
                yValues[j] = y[i];
            }
            list.add(new Pair<>(x[i], yValues));
        }

        final Comparator<Pair<Double, double[]>> comp = new Comparator<Pair<Double, double[]>>(){
            /** {@inheritDoc} */
            @Override
            public int compare(final Pair<Double, double[]> o1,
                               final Pair<Double, double[]> o2) {
                final int val;
                switch (dir) {
                    case INCREASING:
                        val = o1.getKey().compareTo(o2.getKey());
                        break;
                    case DECREASING:
                        val = o2.getKey().compareTo(o1.getKey());
                        break;
                    default:
                        // Should never happen.
                        throw new MathInternalError();
                }
                return val;
            }
        };

        Collections.sort(list, comp);

        for (int i = 0; i < len; i++) {
            final Pair<Double, double[]> e = list.get(i);
            x[i] = e.getKey();
            final double[] yValues = e.getValue();
            for (int j = 0; j < yListLen; j++) {
                yList[j][i] = yValues[j];
            }
        }
    }

    /**
     * Creates a copy of the {@code source} array.
     * 
     * @param source
     *        Array to be copied.
     * @return the copied array.
     */
    public static int[] copyOf(final int[] source) {
        return copyOf(source, source.length);
    }

    /**
     * Creates a copy of the {@code source} array.
     * 
     * @param source
     *        Array to be copied.
     * @return the copied array.
     */
    public static double[] copyOf(final double[] source) {
        return copyOf(source, source.length);
    }

    /**
     * Creates a copy of the {@code source} array.
     * 
     * @param source
     *        Array to be copied.
     * @param len
     *        Number of entries to copy. If smaller then the source
     *        length, the copy will be truncated, if larger it will padded with
     *        zeroes.
     * @return the copied array.
     */
    public static int[] copyOf(final int[] source, final int len) {
        final int[] output = new int[len];
        System.arraycopy(source, 0, output, 0, MathLib.min(len, source.length));
        return output;
    }

    /**
     * Creates a copy of the {@code source} array.
     * 
     * @param source
     *        Array to be copied.
     * @param len
     *        Number of entries to copy. If smaller then the source
     *        length, the copy will be truncated, if larger it will padded with
     *        zeroes.
     * @return the copied array.
     */
    public static double[] copyOf(final double[] source, final int len) {
        final double[] output = new double[len];
        System.arraycopy(source, 0, output, 0, MathLib.min(len, source.length));
        return output;
    }

    /**
     * Compute a linear combination accurately.
     * This method computes the sum of the products <code>a<sub>i</sub> b<sub>i</sub></code> to high accuracy.
     * It does so by using specific multiplication and addition algorithms to
     * preserve accuracy and reduce cancellation effects. <br/>
     * It is based on the 2005 paper
     * <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
     * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump,
     * and Shin'ichi Oishi published in SIAM J. Sci. Comput.
     * 
     * @param a
     *        Factors.
     * @param b
     *        Factors.
     * @return <code>&Sigma;<sub>i</sub> a<sub>i</sub> b<sub>i</sub></code>.
     * @throws DimensionMismatchException
     *         if arrays dimensions don't match
     */
    public static double linearCombination(final double[] a, final double[] b) {
        final int len = a.length;
        if (len != b.length) {
            throw new DimensionMismatchException(len, b.length);
        }

        final double[] prodHigh = new double[len];
        double prodLowSum = 0;

        for (int i = 0; i < len; i++) {
            final double ai = a[i];
            final double ca = SPLIT_FACTOR * ai;
            final double aHigh = ca - (ca - ai);
            final double aLow = ai - aHigh;

            final double bi = b[i];
            final double cb = SPLIT_FACTOR * bi;
            final double bHigh = cb - (cb - bi);
            final double bLow = bi - bHigh;
            prodHigh[i] = ai * bi;
            final double prodLow = aLow * bLow - (((prodHigh[i] -
                aHigh * bHigh) -
                aLow * bHigh) -
                aHigh * bLow);
            prodLowSum += prodLow;
        }

        final double prodHighCur = prodHigh[0];
        double prodHighNext = prodHigh[1];
        double sHighPrev = prodHighCur + prodHighNext;
        double sPrime = sHighPrev - prodHighNext;
        double sLowSum = (prodHighNext - (sHighPrev - sPrime)) + (prodHighCur - sPrime);

        final int lenMinusOne = len - 1;
        for (int i = 1; i < lenMinusOne; i++) {
            prodHighNext = prodHigh[i + 1];
            final double sHighCur = sHighPrev + prodHighNext;
            sPrime = sHighCur - prodHighNext;
            sLowSum += (prodHighNext - (sHighCur - sPrime)) + (sHighPrev - sPrime);
            sHighPrev = sHighCur;
        }

        double result = sHighPrev + (prodLowSum + sLowSum);

        if (Double.isNaN(result)) {
            // either we have split infinite numbers or some coefficients were NaNs,
            // just rely on the naive implementation and let IEEE754 handle this
            result = 0;
            for (int i = 0; i < len; ++i) {
                result += a[i] * b[i];
            }
        }

        return result;
    }

    /**
     * Compute a linear combination accurately.
     * <p>
     * This method computes a<sub>1</sub>&times;b<sub>1</sub> + a<sub>2</sub>&times;b<sub>2</sub> to high accuracy. It
     * does so by using specific multiplication and addition algorithms to preserve accuracy and reduce cancellation
     * effects. It is based on the 2005 paper <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
     * Accurate Sum and Dot Product</a> by Takeshi Ogita, Siegfried M. Rump, and Shin'ichi Oishi published in SIAM J.
     * Sci. Comput.
     * </p>
     * 
     * @param a1
     *        first factor of the first term
     * @param b1
     *        second factor of the first term
     * @param a2
     *        first factor of the second term
     * @param b2
     *        second factor of the second term
     * @return a<sub>1</sub>&times;b<sub>1</sub> +
     *         a<sub>2</sub>&times;b<sub>2</sub>
     * @see #linearCombination(double, double, double, double, double, double)
     * @see #linearCombination(double, double, double, double, double, double, double, double)
     */
    public static double linearCombination(final double a1, final double b1,
                                           final double a2, final double b2) {

        // the code below is split in many additions/subtractions that may
        // appear redundant. However, they should NOT be simplified, as they
        // use IEEE754 floating point arithmetic rounding properties.
        // as an example, the expression "ca1 - (ca1 - a1)" is NOT the same as "a1"
        // The variable naming conventions are that xyzHigh contains the most significant
        // bits of xyz and xyzLow contains its least significant bits. So theoretically
        // xyz is the sum xyzHigh + xyzLow, but in many cases below, this sum cannot
        // be represented in only one double precision number so we preserve two numbers
        // to hold it as long as we can, combining the high and low order bits together
        // only at the end, after cancellation may have occurred on high order bits

        // split a1 and b1 as two 26 bits numbers
        final double ca1 = SPLIT_FACTOR * a1;
        final double a1High = ca1 - (ca1 - a1);
        final double a1Low = a1 - a1High;
        final double cb1 = SPLIT_FACTOR * b1;
        final double b1High = cb1 - (cb1 - b1);
        final double b1Low = b1 - b1High;

        // accurate multiplication a1 * b1
        final double prod1High = a1 * b1;
        final double prod1Low = a1Low * b1Low - (((prod1High - a1High * b1High) - a1Low * b1High) - a1High * b1Low);

        // split a2 and b2 as two 26 bits numbers
        final double ca2 = SPLIT_FACTOR * a2;
        final double a2High = ca2 - (ca2 - a2);
        final double a2Low = a2 - a2High;
        final double cb2 = SPLIT_FACTOR * b2;
        final double b2High = cb2 - (cb2 - b2);
        final double b2Low = b2 - b2High;

        // accurate multiplication a2 * b2
        final double prod2High = a2 * b2;
        final double prod2Low = a2Low * b2Low - (((prod2High - a2High * b2High) - a2Low * b2High) - a2High * b2Low);

        // accurate addition a1 * b1 + a2 * b2
        final double s12High = prod1High + prod2High;
        final double s12Prime = s12High - prod2High;
        final double s12Low = (prod2High - (s12High - s12Prime)) + (prod1High - s12Prime);

        // final rounding, s12 may have suffered many cancellations, we try
        // to recover some bits from the extra words we have saved up to now
        double result = s12High + (prod1Low + prod2Low + s12Low);

        if (Double.isNaN(result)) {
            // either we have split infinite numbers or some coefficients were NaNs,
            // just rely on the naive implementation and let IEEE754 handle this
            result = a1 * b1 + a2 * b2;
        }

        return result;
    }

    /**
     * Compute a linear combination accurately.
     * <p>
     * This method computes a<sub>1</sub>&times;b<sub>1</sub> + a<sub>2</sub>&times;b<sub>2</sub> +
     * a<sub>3</sub>&times;b<sub>3</sub> to high accuracy. It does so by using specific multiplication and addition
     * algorithms to preserve accuracy and reduce cancellation effects. It is based on the 2005 paper <a
     * href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547"> Accurate Sum and Dot Product</a> by
     * Takeshi Ogita, Siegfried M. Rump, and Shin'ichi Oishi published in SIAM J. Sci. Comput.
     * </p>
     * 
     * @param a1
     *        first factor of the first term
     * @param b1
     *        second factor of the first term
     * @param a2
     *        first factor of the second term
     * @param b2
     *        second factor of the second term
     * @param a3
     *        first factor of the third term
     * @param b3
     *        second factor of the third term
     * @return a<sub>1</sub>&times;b<sub>1</sub> +
     *         a<sub>2</sub>&times;b<sub>2</sub> + a<sub>3</sub>&times;b<sub>3</sub>
     * @see #linearCombination(double, double, double, double)
     * @see #linearCombination(double, double, double, double, double, double, double, double)
     */
    public static double linearCombination(final double a1, final double b1,
                                           final double a2, final double b2,
                                           final double a3, final double b3) {

        // the code below is split in many additions/subtractions that may
        // appear redundant. However, they should NOT be simplified, as they
        // do use IEEE754 floating point arithmetic rounding properties.
        // as an example, the expression "ca1 - (ca1 - a1)" is NOT the same as "a1"
        // The variables naming conventions are that xyzHigh contains the most significant
        // bits of xyz and xyzLow contains its least significant bits. So theoretically
        // xyz is the sum xyzHigh + xyzLow, but in many cases below, this sum cannot
        // be represented in only one double precision number so we preserve two numbers
        // to hold it as long as we can, combining the high and low order bits together
        // only at the end, after cancellation may have occurred on high order bits

        // split a1 and b1 as two 26 bits numbers
        final double ca1 = SPLIT_FACTOR * a1;
        final double a1High = ca1 - (ca1 - a1);
        final double a1Low = a1 - a1High;
        final double cb1 = SPLIT_FACTOR * b1;
        final double b1High = cb1 - (cb1 - b1);
        final double b1Low = b1 - b1High;

        // accurate multiplication a1 * b1
        final double prod1High = a1 * b1;
        final double prod1Low = a1Low * b1Low - (((prod1High - a1High * b1High) - a1Low * b1High) - a1High * b1Low);

        // split a2 and b2 as two 26 bits numbers
        final double ca2 = SPLIT_FACTOR * a2;
        final double a2High = ca2 - (ca2 - a2);
        final double a2Low = a2 - a2High;
        final double cb2 = SPLIT_FACTOR * b2;
        final double b2High = cb2 - (cb2 - b2);
        final double b2Low = b2 - b2High;

        // accurate multiplication a2 * b2
        final double prod2High = a2 * b2;
        final double prod2Low = a2Low * b2Low - (((prod2High - a2High * b2High) - a2Low * b2High) - a2High * b2Low);

        // split a3 and b3 as two 26 bits numbers
        final double ca3 = SPLIT_FACTOR * a3;
        final double a3High = ca3 - (ca3 - a3);
        final double a3Low = a3 - a3High;
        final double cb3 = SPLIT_FACTOR * b3;
        final double b3High = cb3 - (cb3 - b3);
        final double b3Low = b3 - b3High;

        // accurate multiplication a3 * b3
        final double prod3High = a3 * b3;
        final double prod3Low = a3Low * b3Low - (((prod3High - a3High * b3High) - a3Low * b3High) - a3High * b3Low);

        // accurate addition a1 * b1 + a2 * b2
        final double s12High = prod1High + prod2High;
        final double s12Prime = s12High - prod2High;
        final double s12Low = (prod2High - (s12High - s12Prime)) + (prod1High - s12Prime);

        // accurate addition a1 * b1 + a2 * b2 + a3 * b3
        final double s123High = s12High + prod3High;
        final double s123Prime = s123High - prod3High;
        final double s123Low = (prod3High - (s123High - s123Prime)) + (s12High - s123Prime);

        // final rounding, s123 may have suffered many cancellations, we try
        // to recover some bits from the extra words we have saved up to now
        double result = s123High + (prod1Low + prod2Low + prod3Low + s12Low + s123Low);

        if (Double.isNaN(result)) {
            // either we have split infinite numbers or some coefficients were NaNs,
            // just rely on the naive implementation and let IEEE754 handle this
            result = a1 * b1 + a2 * b2 + a3 * b3;
        }

        return result;
    }

    /**
     * Compute a linear combination accurately.
     * <p>
     * This method computes a<sub>1</sub>&times;b<sub>1</sub> + a<sub>2</sub>&times;b<sub>2</sub> +
     * a<sub>3</sub>&times;b<sub>3</sub> + a<sub>4</sub>&times;b<sub>4</sub> to high accuracy. It does so by using
     * specific multiplication and addition algorithms to preserve accuracy and reduce cancellation effects. It is based
     * on the 2005 paper <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547"> Accurate Sum and Dot
     * Product</a> by Takeshi Ogita, Siegfried M. Rump, and Shin'ichi Oishi published in SIAM J. Sci. Comput.
     * </p>
     * 
     * @param a1
     *        first factor of the first term
     * @param b1
     *        second factor of the first term
     * @param a2
     *        first factor of the second term
     * @param b2
     *        second factor of the second term
     * @param a3
     *        first factor of the third term
     * @param b3
     *        second factor of the third term
     * @param a4
     *        first factor of the third term
     * @param b4
     *        second factor of the third term
     * @return a<sub>1</sub>&times;b<sub>1</sub> +
     *         a<sub>2</sub>&times;b<sub>2</sub> + a<sub>3</sub>&times;b<sub>3</sub> +
     *         a<sub>4</sub>&times;b<sub>4</sub>
     * @see #linearCombination(double, double, double, double)
     * @see #linearCombination(double, double, double, double, double, double)
     */
    public static double linearCombination(final double a1, final double b1,
                                           final double a2, final double b2,
                                           final double a3, final double b3,
                                           final double a4, final double b4) {

        // the code below is split in many additions/subtractions that may
        // appear redundant. However, they should NOT be simplified, as they
        // do use IEEE754 floating point arithmetic rounding properties.
        // as an example, the expression "ca1 - (ca1 - a1)" is NOT the same as "a1"
        // The variables naming conventions are that xyzHigh contains the most significant
        // bits of xyz and xyzLow contains its least significant bits. So theoretically
        // xyz is the sum xyzHigh + xyzLow, but in many cases below, this sum cannot
        // be represented in only one double precision number so we preserve two numbers
        // to hold it as long as we can, combining the high and low order bits together
        // only at the end, after cancellation may have occurred on high order bits

        // split a1 and b1 as two 26 bits numbers
        final double ca1 = SPLIT_FACTOR * a1;
        final double a1High = ca1 - (ca1 - a1);
        final double a1Low = a1 - a1High;
        final double cb1 = SPLIT_FACTOR * b1;
        final double b1High = cb1 - (cb1 - b1);
        final double b1Low = b1 - b1High;

        // accurate multiplication a1 * b1
        final double prod1High = a1 * b1;
        final double prod1Low = a1Low * b1Low - (((prod1High - a1High * b1High) - a1Low * b1High) - a1High * b1Low);

        // split a2 and b2 as two 26 bits numbers
        final double ca2 = SPLIT_FACTOR * a2;
        final double a2High = ca2 - (ca2 - a2);
        final double a2Low = a2 - a2High;
        final double cb2 = SPLIT_FACTOR * b2;
        final double b2High = cb2 - (cb2 - b2);
        final double b2Low = b2 - b2High;

        // accurate multiplication a2 * b2
        final double prod2High = a2 * b2;
        final double prod2Low = a2Low * b2Low - (((prod2High - a2High * b2High) - a2Low * b2High) - a2High * b2Low);

        // split a3 and b3 as two 26 bits numbers
        final double ca3 = SPLIT_FACTOR * a3;
        final double a3High = ca3 - (ca3 - a3);
        final double a3Low = a3 - a3High;
        final double cb3 = SPLIT_FACTOR * b3;
        final double b3High = cb3 - (cb3 - b3);
        final double b3Low = b3 - b3High;

        // accurate multiplication a3 * b3
        final double prod3High = a3 * b3;
        final double prod3Low = a3Low * b3Low - (((prod3High - a3High * b3High) - a3Low * b3High) - a3High * b3Low);

        // split a4 and b4 as two 26 bits numbers
        final double ca4 = SPLIT_FACTOR * a4;
        final double a4High = ca4 - (ca4 - a4);
        final double a4Low = a4 - a4High;
        final double cb4 = SPLIT_FACTOR * b4;
        final double b4High = cb4 - (cb4 - b4);
        final double b4Low = b4 - b4High;

        // accurate multiplication a4 * b4
        final double prod4High = a4 * b4;
        final double prod4Low = a4Low * b4Low - (((prod4High - a4High * b4High) - a4Low * b4High) - a4High * b4Low);

        // accurate addition a1 * b1 + a2 * b2
        final double s12High = prod1High + prod2High;
        final double s12Prime = s12High - prod2High;
        final double s12Low = (prod2High - (s12High - s12Prime)) + (prod1High - s12Prime);

        // accurate addition a1 * b1 + a2 * b2 + a3 * b3
        final double s123High = s12High + prod3High;
        final double s123Prime = s123High - prod3High;
        final double s123Low = (prod3High - (s123High - s123Prime)) + (s12High - s123Prime);

        // accurate addition a1 * b1 + a2 * b2 + a3 * b3 + a4 * b4
        final double s1234High = s123High + prod4High;
        final double s1234Prime = s1234High - prod4High;
        final double s1234Low = (prod4High - (s1234High - s1234Prime)) + (s123High - s1234Prime);

        // final rounding, s1234 may have suffered many cancellations, we try
        // to recover some bits from the extra words we have saved up to now
        double result = s1234High + (prod1Low + prod2Low + prod3Low + prod4Low + s12Low + s123Low + s1234Low);

        if (Double.isNaN(result)) {
            // either we have split infinite numbers or some coefficients were NaNs,
            // just rely on the naive implementation and let IEEE754 handle this
            result = a1 * b1 + a2 * b2 + a3 * b3 + a4 * b4;
        }

        return result;
    }

    /**
     * Returns true iff both arguments are null or have same dimensions and all
     * their elements are equal as defined by {@link Precision#equals(float,float)}.
     * 
     * @param x
     *        first array
     * @param y
     *        second array
     * @return true if the values are both null or have same dimension
     *         and equal elements.
     */
    public static boolean equals(final float[] x, final float[] y) {
        if ((x == null) || (y == null)) {
            return !((x == null) ^ (y == null));
        }
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; ++i) {
            if (!Precision.equals(x[i], y[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true iff both arguments are null or have same dimensions and all
     * their elements are equal as defined by {@link Precision#equalsIncludingNaN(double,double) this method}.
     * 
     * @param x
     *        first array
     * @param y
     *        second array
     * @return true if the values are both null or have same dimension and
     *         equal elements
     * @since 2.2
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static boolean equalsIncludingNaN(final float[] x, final float[] y) {
        // CHECKSTYLE: resume ReturnCount check
        if ((x == null) || (y == null)) {
            return !((x == null) ^ (y == null));
        }
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; ++i) {
            if (!Precision.equalsIncludingNaN(x[i], y[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} iff both arguments are {@code null} or have same
     * dimensions and all their elements are equal as defined by {@link Precision#equals(double,double)}.
     * 
     * @param x
     *        First array.
     * @param y
     *        Second array.
     * @return {@code true} if the values are both {@code null} or have same
     *         dimension and equal elements.
     */
    public static boolean equals(final double[] x, final double[] y) {
        if ((x == null) || (y == null)) {
            return !((x == null) ^ (y == null));
        }
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; ++i) {
            if (!Precision.equals(x[i], y[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} iff both arguments are {@code null} or have same
     * dimensions and all their elements are equal as defined by {@link Precision#equalsIncludingNaN(double,double) this
     * method}.
     * 
     * @param x
     *        First array.
     * @param y
     *        Second array.
     * @return {@code true} if the values are both {@code null} or have same
     *         dimension and equal elements.
     * @since 2.2
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static boolean equalsIncludingNaN(final double[] x, final double[] y) {
        // CHECKSTYLE: resume ReturnCount check
        if ((x == null) || (y == null)) {
            return !((x == null) ^ (y == null));
        }
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; ++i) {
            if (!Precision.equalsIncludingNaN(x[i], y[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes an array to make it sum to a specified value.
     * Returns the result of the transformation
     * 
     * <pre>
     *    x |-> x * normalizedSum / sum
     * </pre>
     * 
     * applied to each non-NaN element x of the input array, where sum is the
     * sum of the non-NaN entries in the input array.</p>
     * 
     * <p>
     * Throws IllegalArgumentException if {@code normalizedSum} is infinite or NaN and ArithmeticException if the input
     * array contains any infinite elements or sums to 0.
     * </p>
     * 
     * <p>
     * Ignores (i.e., copies unchanged to the output array) NaNs in the input array.
     * </p>
     * 
     * @param values
     *        Input array to be normalized
     * @param normalizedSum
     *        Target sum for the normalized array
     * @return the normalized array.
     * @throws MathArithmeticException
     *         if the input array contains infinite
     *         elements or sums to zero.
     * @throws MathIllegalArgumentException
     *         if the target sum is infinite or {@code NaN}.
     * @since 2.1
     */
    public static double[] normalizeArray(final double[] values, final double normalizedSum) {
        if (Double.isInfinite(normalizedSum)) {
            throw new MathIllegalArgumentException(PatriusMessages.NORMALIZE_INFINITE);
        }
        if (Double.isNaN(normalizedSum)) {
            throw new MathIllegalArgumentException(PatriusMessages.NORMALIZE_NAN);
        }
        double sum = 0d;
        final int len = values.length;
        for (int i = 0; i < len; i++) {
            if (Double.isInfinite(values[i])) {
                throw new MathIllegalArgumentException(PatriusMessages.INFINITE_ARRAY_ELEMENT, values[i], i);
            }
            if (!Double.isNaN(values[i])) {
                sum += values[i];
            }
        }
        if (sum == 0) {
            throw new MathArithmeticException(PatriusMessages.ARRAY_SUMS_TO_ZERO);
        }
        final double[] out = new double[len];
        for (int i = 0; i < len; i++) {
            if (Double.isNaN(values[i])) {
                out[i] = Double.NaN;
            } else {
                out[i] = values[i] * normalizedSum / sum;
            }
        }
        return out;
    }

    /**
     * Build an array of elements.
     * <p>
     * Arrays are filled with field.getZero()
     * 
     * @param <T>
     *        the type of the field elements
     * @param field
     *        field to which array elements belong
     * @param length
     *        of the array
     * @return a new array
     * @since 3.2
     */
    public static <T> T[] buildArray(final Field<T> field, final int length) {
        @SuppressWarnings("unchecked")
        // OK because field must be correct class
        final T[] array = (T[]) Array.newInstance(field.getRuntimeClass(), length);
        Arrays.fill(array, field.getZero());
        return array;
    }

    /**
     * Build a double dimension array of elements.
     * <p>
     * Arrays are filled with field.getZero()
     * 
     * @param <T>
     *        the type of the field elements
     * @param field
     *        field to which array elements belong
     * @param rows
     *        number of rows in the array
     * @param columns
     *        number of columns (may be negative to build partial
     *        arrays in the same way <code>new Field[rows][]</code> works)
     * @return a new array
     * @since 3.2
     */
    @SuppressWarnings("unchecked")
    public static <T> T[][] buildArray(final Field<T> field, final int rows, final int columns) {
        final T[][] array;
        if (columns < 0) {
            final T[] dummyRow = buildArray(field, 0);
            array = (T[][]) Array.newInstance(dummyRow.getClass(), rows);
        } else {
            array = (T[][]) Array.newInstance(field.getRuntimeClass(),
                new int[] { rows, columns });
            for (int i = 0; i < rows; ++i) {
                Arrays.fill(array[i], field.getZero());
            }
        }
        return array;
    }

    /**
     * Returns an array representing the natural number {@code n}.
     * 
     * @param n
     *        Natural number.
     * @return an array whose entries are the numbers 0, 1, ..., {@code n}-1.
     *         If {@code n == 0}, the returned array is empty.
     */
    public static int[] natural(final int n) {
        return sequence(n, 0, 1);
    }

    /**
     * Returns an array of {@code size} integers starting at {@code start},
     * skipping {@code stride} numbers.
     * 
     * @param size
     *        Natural number.
     * @param start
     *        Natural number.
     * @param stride
     *        Natural number.
     * @return an array whose entries are the numbers {@code start, start + stride, ..., start + (size - 1) * stride}.
     *         If {@code size == 0}, the returned array is empty.
     * 
     * @since 3.4
     */
    public static int[] sequence(final int size,
                                 final int start,
                                 final int stride) {
        final int[] a = new int[size];
        for (int i = 0; i < size; i++) {
            a[i] = start + i * stride;
        }
        return a;
    }


    /**
     * Real-valued function that operate on an array or a part of it.
     * 
     * @since 3.1
     */
    public interface Function {
        /**
         * Operates on an entire array.
         * 
         * @param array
         *        Array to operate on.
         * @return the result of the operation.
         */
        double evaluate(double[] array);

        /**
         * @param array
         *        Array to operate on.
         * @param startIndex
         *        Index of the first element to take into account.
         * @param numElements
         *        Number of elements to take into account.
         * @return the result of the operation.
         */
        double evaluate(double[] array,
                        int startIndex,
                        int numElements);
    }

    // CHECKSTYLE: resume CommentRatio check
}
