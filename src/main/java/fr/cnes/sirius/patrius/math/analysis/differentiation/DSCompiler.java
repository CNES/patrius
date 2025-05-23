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
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Class holding "compiled" computation rules for derivative structures.
 * <p>
 * This class implements the computation rules described in Dan Kalman's paper <a
 * href="http://www.math.american.edu/People/kalman/pdffiles/mmgautodiff.pdf">Doubly Recursive Multivariate Automatic
 * Differentiation</a>, Mathematics Magazine, vol. 75, no. 3, June 2002. However, in order to avoid performances
 * bottlenecks, the recursive rules are "compiled" once in an unfold form. This class does this recursion unrolling and
 * stores the computation rules as simple loops with pre-computed indirection arrays.
 * </p>
 * <p>
 * This class maps all derivative computation into single dimension arrays that hold the value and partial derivatives.
 * The class does not hold these arrays, which remains under the responsibility of the caller. For each combination of
 * number of free parameters and derivation order, only one compiler is necessary, and this compiler will be used to
 * perform computations on all arrays provided to it, which can represent hundreds or thousands of different parameters
 * kept together with all theur partial derivatives.
 * </p>
 * <p>
 * The arrays on which compilers operate contain only the partial derivatives together with the 0<sup>th</sup>
 * derivative, i.e. the value. The partial derivatives are stored in a compiler-specific order, which can be retrieved
 * using methods {@link #getPartialDerivativeIndex(int...) getPartialDerivativeIndex} and
 * {@link #getPartialDerivativeOrders(int)}. The value is guaranteed to be stored as the first element (i.e. the
 * {@link #getPartialDerivativeIndex(int...) getPartialDerivativeIndex} method returns 0 when called with 0 for all
 * derivation orders and {@link #getPartialDerivativeOrders(int)
 * getPartialDerivativeOrders} returns an array filled with 0 when called with 0 as the index).
 * </p>
 * <p>
 * Note that the ordering changes with number of parameters and derivation order. For example given 2 parameters x and
 * y, df/dy is stored at index 2 when derivation order is set to 1 (in this case the array has three elements: f, df/dx
 * and df/dy). If derivation order is set to 2, then df/dy will be stored at index 3 (in this case the array has six
 * elements: f, df/dx, df/dxdx, df/dy, df/dxdy and df/dydy).
 * </p>
 * <p>
 * Given this structure, users can perform some simple operations like adding, subtracting or multiplying constants and
 * negating the elements by themselves, knowing if they want to mutate their array or create a new array. These simple
 * operations are not provided by the compiler. The compiler provides only the more complex operations between several
 * arrays.
 * </p>
 * <p>
 * This class is mainly used as the engine for scalar variable {@link DerivativeStructure}. It can also be used directly
 * to hold several variables in arrays for more complex data structures. User can for example store a vector of n
 * variables depending on three x, y and z free parameters in one array as follows:
 * 
 * <pre>
 *   // parameter 0 is x, parameter 1 is y, parameter 2 is z
 *   int parameters = 3;
 *   DSCompiler compiler = DSCompiler.getCompiler(parameters, order);
 *   int size = compiler.getSize();
 * 
 *   // pack all elements in a single array
 *   double[] array = new double[n * size];
 *   for (int i = 0; i < n; ++i) {
 * 
 *     // we know value is guaranteed to be the first element
 *     array[i * size] = v[i];
 * 
 *     // we don't know where first derivatives are stored, so we ask the compiler
 *     array[i * size + compiler.getPartialDerivativeIndex(1, 0, 0) = dvOnDx[i][0];
 *     array[i * size + compiler.getPartialDerivativeIndex(0, 1, 0) = dvOnDy[i][0];
 *     array[i * size + compiler.getPartialDerivativeIndex(0, 0, 1) = dvOnDz[i][0];
 * 
 *     // we let all higher order derivatives set to 0
 * 
 *   }
 * </pre>
 * 
 * Then in another function, user can perform some operations on all elements stored in the single array, such as a
 * simple product of all variables:
 * 
 * <pre>
 * // compute the product of all elements
 * double[] product = new double[size];
 * prod[0] = 1.0;
 * for (int i = 0; i &lt; n; ++i) {
 *     double[] tmp = product.clone();
 *     compiler.multiply(tmp, 0, array, i * size, product, 0);
 * }
 * 
 * // value
 * double p = product[0];
 * 
 * // first derivatives
 * double dPdX = product[compiler.getPartialDerivativeIndex(1, 0, 0)];
 * double dPdY = product[compiler.getPartialDerivativeIndex(0, 1, 0)];
 * double dPdZ = product[compiler.getPartialDerivativeIndex(0, 0, 1)];
 * 
 * // cross derivatives (assuming order was at least 2)
 * double dPdXdX = product[compiler.getPartialDerivativeIndex(2, 0, 0)];
 * double dPdXdY = product[compiler.getPartialDerivativeIndex(1, 1, 0)];
 * double dPdXdZ = product[compiler.getPartialDerivativeIndex(1, 0, 1)];
 * double dPdYdY = product[compiler.getPartialDerivativeIndex(0, 2, 0)];
 * double dPdYdZ = product[compiler.getPartialDerivativeIndex(0, 1, 1)];
 * double dPdZdZ = product[compiler.getPartialDerivativeIndex(0, 0, 2)];
 * 
 * </p>
 * 
 * @see DerivativeStructure
 * @version $Id: DSCompiler.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
@SuppressWarnings({ "PMD.NullAssignment", "PMD.AvoidArrayLoops"})
public final class DSCompiler {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 10.0 */
    private static final double C_10 = 10.;

    /** -2.0 */
    private static final double C_N2 = -2.;

    /** Array of all compilers created so far. */
    private static AtomicReference<DSCompiler[][]> compilers = new AtomicReference<>(null);

    /** Number of free parameters. */
    private final int parameters;

    /** Derivation order. */
    private final int order;

    /** Number of partial derivatives (including the single 0 order derivative element). */
    private final int[][] sizes;

    /** Indirection array for partial derivatives. */
    private final int[][] derivativesIndirection;

    /** Indirection array of the lower derivative elements. */
    private final int[] lowerIndirection;

    /** Indirection arrays for multiplication. */
    private final int[][][] multIndirection;

    /** Indirection arrays for function composition. */
    private final int[][][] compIndirection;

    /**
     * Private constructor, reserved for the factory method {@link #getCompiler(int, int)}.
     * 
     * @param parametersIn
     *        number of free parameters
     * @param orderIn
     *        derivation order
     * @param valueCompiler
     *        compiler for the value part
     * @param derivativeCompiler
     *        compiler for the derivative part
     */
    private DSCompiler(final int parametersIn, final int orderIn,
                       final DSCompiler valueCompiler, final DSCompiler derivativeCompiler) {

        this.parameters = parametersIn;
        this.order = orderIn;
        this.sizes = compileSizes(parametersIn, orderIn, valueCompiler);
        this.derivativesIndirection =
            compileDerivativesIndirection(parametersIn, orderIn,
                valueCompiler, derivativeCompiler);
        this.lowerIndirection =
            compileLowerIndirection(parametersIn, orderIn,
                valueCompiler, derivativeCompiler);
        this.multIndirection =
            compileMultiplicationIndirection(parametersIn, orderIn,
                valueCompiler, derivativeCompiler, this.lowerIndirection);
        this.compIndirection =
            compileCompositionIndirection(parametersIn, orderIn,
                valueCompiler, derivativeCompiler,
                this.sizes, this.derivativesIndirection);

    }

    /**
     * Get the compiler for number of free parameters and order.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @return cached rules set
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static DSCompiler getCompiler(final int parameters, final int order) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // get the cached compilers
        final DSCompiler[][] cache = compilers.get();
        if (cache != null && cache.length > parameters && cache[parameters].length > order) {
            if (cache[parameters][order] != null) {
                // the compiler has already been created
                return cache[parameters][order];
            }
        }

        // we need to create more compilers
        final int maxParameters = MathLib.max(parameters, cache == null ? 0 : cache.length);
        final int maxOrder = MathLib.max(order, cache == null ? 0 : cache[0].length);
        final DSCompiler[][] newCache = new DSCompiler[maxParameters + 1][maxOrder + 1];

        if (cache != null) {
            // preserve the already created compilers
            for (int i = 0; i < cache.length; ++i) {
                System.arraycopy(cache[i], 0, newCache[i], 0, cache[i].length);
            }
        }

        // create the array in increasing diagonal order
        for (int diag = 0; diag <= parameters + order; ++diag) {
            for (int o = MathLib.max(0, diag - parameters); o <= MathLib.min(order, diag); ++o) {
                final int p = diag - o;
                if (newCache[p][o] == null) {
                    final DSCompiler valueCompiler = (p == 0) ? null : newCache[p - 1][o];
                    final DSCompiler derivativeCompiler = (o == 0) ? null : newCache[p][o - 1];
                    newCache[p][o] = new DSCompiler(p, o, valueCompiler, derivativeCompiler);
                }
            }
        }

        // atomically reset the cached compilers array
        compilers.compareAndSet(cache, newCache);

        return newCache[parameters][order];

    }

    /**
     * Compile the sizes array.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param valueCompiler
     *        compiler for the value part
     * @return sizes array
     */
    private static int[][] compileSizes(final int parameters, final int order,
                                        final DSCompiler valueCompiler) {

        // initialiaze sizes array
        final int[][] sizes = new int[parameters + 1][order + 1];
        if (parameters == 0) {
            // particular case if parameters is equal to 0
            Arrays.fill(sizes[0], 1);
        } else {
            // copy values from valueCompiler
            System.arraycopy(valueCompiler.sizes, 0, sizes, 0, parameters);
            sizes[parameters][0] = 1;
            for (int i = 0; i < order; ++i) {
                sizes[parameters][i + 1] = sizes[parameters][i] + sizes[parameters - 1][i + 1];
            }
        }

        return sizes;

    }

    /**
     * Compile the derivatives indirection array.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param valueCompiler
     *        compiler for the value part
     * @param derivativeCompiler
     *        compiler for the derivative part
     * @return derivatives indirection array
     */
    private static int[][] compileDerivativesIndirection(final int parameters, final int order,
                                                         final DSCompiler valueCompiler,
                                                         final DSCompiler derivativeCompiler) {

        if (parameters == 0 || order == 0) {
            return new int[1][parameters];
        }

        final int vSize = valueCompiler.derivativesIndirection.length;
        final int dSize = derivativeCompiler.derivativesIndirection.length;
        final int[][] derivativesIndirection = new int[vSize + dSize][parameters];

        // set up the indices for the value part
        for (int i = 0; i < vSize; ++i) {
            // copy the first indices, the last one remaining set to 0
            System.arraycopy(valueCompiler.derivativesIndirection[i], 0,
                derivativesIndirection[i], 0,
                parameters - 1);
        }

        // set up the indices for the derivative part
        for (int i = 0; i < dSize; ++i) {

            // copy the indices
            System.arraycopy(derivativeCompiler.derivativesIndirection[i], 0,
                derivativesIndirection[vSize + i], 0,
                parameters);

            // increment the derivation order for the last parameter
            derivativesIndirection[vSize + i][parameters - 1]++;

        }

        return derivativesIndirection;

    }

    /**
     * Compile the lower derivatives indirection array.
     * <p>
     * This indirection array contains the indices of all elements except derivatives for last derivation order.
     * </p>
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param valueCompiler
     *        compiler for the value part
     * @param derivativeCompiler
     *        compiler for the derivative part
     * @return lower derivatives indirection array
     */
    private static int[] compileLowerIndirection(final int parameters, final int order,
                                                 final DSCompiler valueCompiler,
                                                 final DSCompiler derivativeCompiler) {

        // particular case
        // return int[] with zero value
        if (parameters == 0 || order <= 1) {
            return new int[] { 0 };
        }

        // this is an implementation of definition 6 in Dan Kalman's paper.
        final int vSize = valueCompiler.lowerIndirection.length;
        final int dSize = derivativeCompiler.lowerIndirection.length;
        final int[] lowerIndirection = new int[vSize + dSize];
        // copy lowerIndirection values from valueCompiler
        System.arraycopy(valueCompiler.lowerIndirection, 0, lowerIndirection, 0, vSize);
        for (int i = 0; i < dSize; ++i) {
            lowerIndirection[vSize + i] = valueCompiler.getSize() + derivativeCompiler.lowerIndirection[i];
        }

        return lowerIndirection;

    }

    /**
     * Compile the multiplication indirection array.
     * <p>
     * This indirection array contains the indices of all pairs of elements involved when computing a multiplication.
     * This allows a straightforward loop-based multiplication (see
     * {@link #multiply(double[], int, double[], int, double[], int)}).
     * </p>
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param valueCompiler
     *        compiler for the value part
     * @param derivativeCompiler
     *        compiler for the derivative part
     * @param lowerIndirection
     *        lower derivatives indirection array
     * @return multiplication indirection array
     */
    private static int[][][] compileMultiplicationIndirection(final int parameters, final int order,
                                                              final DSCompiler valueCompiler,
                                                              final DSCompiler derivativeCompiler,
                                                              final int[] lowerIndirection) {

        if ((parameters == 0) || (order == 0)) {
            // Immediate return
            return new int[][][] { { { 1, 0, 0 } } };
        }

        // Generic case

        // this is an implementation of definition 3 in Dan Kalman's paper.
        final int vSize = valueCompiler.multIndirection.length;
        final int dSize = derivativeCompiler.multIndirection.length;
        final int[][][] multIndirection = new int[vSize + dSize][][];

        System.arraycopy(valueCompiler.multIndirection, 0, multIndirection, 0, vSize);

        for (int i = 0; i < dSize; ++i) {
            // Loop on direction size
            final int[][] dRow = derivativeCompiler.multIndirection[i];
            final List<int[]> row = new ArrayList<>();
            for (final int[] element : dRow) {
                row.add(new int[] { element[0], lowerIndirection[element[1]], vSize + element[2] });
                row.add(new int[] { element[0], vSize + element[1], lowerIndirection[element[2]] });
            }

            // combine terms with similar derivation orders
            final List<int[]> combined = new ArrayList<>(row.size());
            for (int j = 0; j < row.size(); ++j) {
                final int[] termJ = row.get(j);
                if (termJ[0] > 0) {
                    for (int k = j + 1; k < row.size(); ++k) {
                        final int[] termK = row.get(k);
                        if (termJ[1] == termK[1] && termJ[2] == termK[2]) {
                            // combine termJ and termK
                            termJ[0] += termK[0];
                            // make sure we will skip termK later on in the outer loop
                            termK[0] = 0;
                        }
                    }
                    combined.add(termJ);
                }
            }

            multIndirection[vSize + i] = combined.toArray(new int[combined.size()][]);

        }

        // Return result
        return multIndirection;
    }

    /**
     * Compile the function composition indirection array.
     * <p>
     * This indirection array contains the indices of all sets of elements involved when computing a composition. This
     * allows a straightforward loop-based composition (see {@link #compose(double[], int, double[], double[], int)}).
     * </p>
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param valueCompiler
     *        compiler for the value part
     * @param derivativeCompiler
     *        compiler for the derivative part
     * @param sizes
     *        sizes array
     * @param derivativesIndirection
     *        derivatives indirection array
     * @return multiplication indirection array
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private static int[][][] compileCompositionIndirection(final int parameters, final int order,
                                                           final DSCompiler valueCompiler,
                                                           final DSCompiler derivativeCompiler,
                                                           final int[][] sizes,
                                                           final int[][] derivativesIndirection) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if ((parameters == 0) || (order == 0)) {
            return new int[][][] { { { 1, 0 } } };
        }

        final int vSize = valueCompiler.compIndirection.length;
        final int dSize = derivativeCompiler.compIndirection.length;
        final int[][][] compIndirection = new int[vSize + dSize][][];

        // the composition rules from the value part can be reused as is
        System.arraycopy(valueCompiler.compIndirection, 0, compIndirection, 0, vSize);

        // the composition rules for the derivative part are deduced by
        // differentiation the rules from the underlying compiler once
        // with respect to the parameter this compiler handles and the
        // underlying one did not handle
        for (int i = 0; i < dSize; ++i) {
            final List<int[]> row = new ArrayList<>();
            for (final int[] term : derivativeCompiler.compIndirection[i]) {

                // handle term p * f_k(g(x)) * g_l1(x) * g_l2(x) * ... * g_lp(x)

                // derive the first factor in the term: f_k with respect to new parameter
                final int[] derivedTermF = new int[term.length + 1];
                // p
                derivedTermF[0] = term[0];
                // f_(k+1)
                derivedTermF[1] = term[1] + 1;
                final int[] orders = new int[parameters];
                orders[parameters - 1] = 1;
                // g_1
                derivedTermF[term.length] = getPartialDerivativeIndex(parameters, order, sizes, orders);
                for (int j = 2; j < term.length; ++j) {
                    // convert the indices as the mapping for the current order
                    // is different from the mapping with one less order
                    derivedTermF[j] = convertIndex(term[j], parameters,
                        derivativeCompiler.derivativesIndirection,
                        parameters, order, sizes);
                }
                Arrays.sort(derivedTermF, 2, derivedTermF.length);
                row.add(derivedTermF);

                // derive the various g_l
                for (int l = 2; l < term.length; ++l) {
                    final int[] derivedTermG = new int[term.length];
                    derivedTermG[0] = term[0];
                    derivedTermG[1] = term[1];
                    for (int j = 2; j < term.length; ++j) {
                        // convert the indices as the mapping for the current order
                        // is different from the mapping with one less order
                        derivedTermG[j] = convertIndex(term[j], parameters,
                            derivativeCompiler.derivativesIndirection,
                            parameters, order, sizes);
                        if (j == l) {
                            // derive this term
                            System.arraycopy(derivativesIndirection[derivedTermG[j]], 0, orders, 0, parameters);
                            orders[parameters - 1]++;
                            derivedTermG[j] = getPartialDerivativeIndex(parameters, order, sizes, orders);
                        }
                    }
                    Arrays.sort(derivedTermG, 2, derivedTermG.length);
                    row.add(derivedTermG);
                }

            }

            // combine terms with similar derivation orders
            final List<int[]> combined = new ArrayList<>(row.size());
            for (int j = 0; j < row.size(); ++j) {
                final int[] termJ = row.get(j);
                if (termJ[0] > 0) {
                    for (int k = j + 1; k < row.size(); ++k) {
                        final int[] termK = row.get(k);
                        boolean equals = termJ.length == termK.length;
                        for (int l = 1; equals && l < termJ.length; ++l) {
                            equals &= termJ[l] == termK[l];
                        }
                        if (equals) {
                            // combine termJ and termK
                            termJ[0] += termK[0];
                            // make sure we will skip termK later on in the outer loop
                            termK[0] = 0;
                        }
                    }
                    combined.add(termJ);
                }
            }

            compIndirection[vSize + i] = combined.toArray(new int[combined.size()][]);

        }

        return compIndirection;

    }

    /**
     * Get the index of a partial derivative in the array.
     * <p>
     * If all orders are set to 0, then the 0<sup>th</sup> order derivative is returned, which is the value of the
     * function.
     * </p>
     * <p>
     * The indices of derivatives are between 0 and {@link #getSize() getSize()} - 1. Their specific order is fixed for
     * a given compiler, but otherwise not publicly specified. There are however some simple cases which have guaranteed
     * indices:
     * </p>
     * <ul>
     * <li>the index of 0<sup>th</sup> order derivative is always 0</li>
     * <li>if there is only 1 {@link #getFreeParameters() free parameter}, then the derivatives are sorted in increasing
     * derivation order (i.e. f at index 0, df/dp at index 1, d<sup>2</sup>f/dp<sup>2</sup> at index 2 ...
     * d<sup>k</sup>f/dp<sup>k</sup> at index k),</li>
     * <li>if the {@link #getOrder() derivation order} is 1, then the derivatives are sorted in incresing free parameter
     * order (i.e. f at index 0, df/dx<sub>1</sub> at index 1, df/dx<sub>2</sub> at index 2 ... df/dx<sub>k</sub> at
     * index k),</li>
     * <li>all other cases are not publicly specified</li>
     * </ul>
     * <p>
     * This method is the inverse of method {@link #getPartialDerivativeOrders(int)}
     * </p>
     * 
     * @param orders
     *        derivation orders with respect to each parameter
     * @return index of the partial derivative
     * @exception DimensionMismatchException
     *            if the numbers of parameters does not
     *            match the instance
     * @exception NumberIsTooLargeException
     *            if sum of derivation orders is larger
     *            than the instance limits
     * @see #getPartialDerivativeOrders(int)
     */
    public int getPartialDerivativeIndex(final int... orders) {

        // safety check
        if (orders.length != this.getFreeParameters()) {
            throw new DimensionMismatchException(orders.length, this.getFreeParameters());
        }

        return getPartialDerivativeIndex(this.parameters, this.order, this.sizes, orders);

    }

    /**
     * Get the index of a partial derivative in an array.
     * 
     * @param parameters
     *        number of free parameters
     * @param order
     *        derivation order
     * @param sizes
     *        sizes array
     * @param orders
     *        derivation orders with respect to each parameter
     *        (the lenght of this array must match the number of parameters)
     * @return index of the partial derivative
     * @exception NumberIsTooLargeException
     *            if sum of derivation orders is larger
     *            than the instance limits
     */
    private static int getPartialDerivativeIndex(final int parameters, final int order,
                                                 final int[][] sizes, final int... orders) {

        // the value is obtained by diving into the recursive Dan Kalman's structure
        // this is theorem 2 of his paper, with recursion replaced by iteration
        int index = 0;
        int m = order;
        int ordersSum = 0;
        for (int i = parameters - 1; i >= 0; --i) {

            // derivative order for current free parameter
            int derivativeOrder = orders[i];

            // safety check
            ordersSum += derivativeOrder;
            if (ordersSum > order) {
                throw new NumberIsTooLargeException(ordersSum, order, true);
            }

            while (derivativeOrder > 0) {
                // as long as we differentiate according to current free parameter,
                // we have to skip the value part and dive into the derivative part
                // so we add the size of the value part to the base index
                index += sizes[i][m--];
                derivativeOrder--;
            }

        }

        return index;

    }

    /**
     * Convert an index from one (parameters, order) structure to another.
     * 
     * @param index
     *        index of a partial derivative in source derivative structure
     * @param srcP
     *        number of free parameters in source derivative structure
     * @param srcDerivativesIndirection
     *        derivatives indirection array for the source
     *        derivative structure
     * @param destP
     *        number of free parameters in destination derivative structure
     * @param destO
     *        derivation order in destination derivative structure
     * @param destSizes
     *        sizes array for the destination derivative structure
     * @return index of the partial derivative with the <em>same</em> characteristics
     *         in destination derivative structure
     */
    private static int convertIndex(final int index,
                                    final int srcP, final int[][] srcDerivativesIndirection,
                                    final int destP, final int destO, final int[][] destSizes) {
        final int[] orders = new int[destP];
        System.arraycopy(srcDerivativesIndirection[index], 0, orders, 0, MathLib.min(srcP, destP));
        return getPartialDerivativeIndex(destP, destO, destSizes, orders);
    }

    /**
     * Get the derivation orders for a specific index in the array.
     * <p>
     * This method is the inverse of {@link #getPartialDerivativeIndex(int...)}.
     * </p>
     * 
     * @param index
     *        of the partial derivative
     * @return orders derivation orders with respect to each parameter
     * @see #getPartialDerivativeIndex(int...)
     */
    public int[] getPartialDerivativeOrders(final int index) {
        return this.derivativesIndirection[index];
    }

    /**
     * Get the number of free parameters.
     * 
     * @return number of free parameters
     */
    public int getFreeParameters() {
        return this.parameters;
    }

    /**
     * Get the derivation order.
     * 
     * @return derivation order
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * Get the array size required for holding partial derivatives data.
     * <p>
     * This number includes the single 0 order derivative element, which is guaranteed to be stored in the first element
     * of the array.
     * </p>
     * 
     * @return array size required for holding partial derivatives data
     */
    public int getSize() {
        return this.sizes[this.parameters][this.order];
    }

    /**
     * Compute linear combination.
     * The derivative structure built will be a1 * ds1 + a2 * ds2
     * 
     * @param a1
     *        first scale factor
     * @param c1
     *        first base (unscaled) component
     * @param offset1
     *        offset of first operand in its array
     * @param a2
     *        second scale factor
     * @param c2
     *        second base (unscaled) component
     * @param offset2
     *        offset of second operand in its array
     * @param result
     *        array where result must be stored (it may be
     *        one of the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void linearCombination(final double a1, final double[] c1, final int offset1,
                                  final double a2, final double[] c2, final int offset2,
                                  final double[] result, final int resultOffset) {
        for (int i = 0; i < this.getSize(); ++i) {
            result[resultOffset + i] =
                MathArrays.linearCombination(a1, c1[offset1 + i], a2, c2[offset2 + i]);
        }
    }

    /**
     * Compute linear combination.
     * The derivative structure built will be a1 * ds1 + a2 * ds2 + a3 * ds3 + a4 * ds4
     * 
     * @param a1
     *        first scale factor
     * @param c1
     *        first base (unscaled) component
     * @param offset1
     *        offset of first operand in its array
     * @param a2
     *        second scale factor
     * @param c2
     *        second base (unscaled) component
     * @param offset2
     *        offset of second operand in its array
     * @param a3
     *        third scale factor
     * @param c3
     *        third base (unscaled) component
     * @param offset3
     *        offset of third operand in its array
     * @param result
     *        array where result must be stored (it may be
     *        one of the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void linearCombination(final double a1, final double[] c1, final int offset1,
                                  final double a2, final double[] c2, final int offset2,
                                  final double a3, final double[] c3, final int offset3,
                                  final double[] result, final int resultOffset) {
        for (int i = 0; i < this.getSize(); ++i) {
            result[resultOffset + i] =
                MathArrays.linearCombination(a1, c1[offset1 + i],
                    a2, c2[offset2 + i],
                    a3, c3[offset3 + i]);
        }
    }

    /**
     * Compute linear combination.
     * The derivative structure built will be a1 * ds1 + a2 * ds2 + a3 * ds3 + a4 * ds4
     * 
     * @param a1
     *        first scale factor
     * @param c1
     *        first base (unscaled) component
     * @param offset1
     *        offset of first operand in its array
     * @param a2
     *        second scale factor
     * @param c2
     *        second base (unscaled) component
     * @param offset2
     *        offset of second operand in its array
     * @param a3
     *        third scale factor
     * @param c3
     *        third base (unscaled) component
     * @param offset3
     *        offset of third operand in its array
     * @param a4
     *        fourth scale factor
     * @param c4
     *        fourth base (unscaled) component
     * @param offset4
     *        offset of fourth operand in its array
     * @param result
     *        array where result must be stored (it may be
     *        one of the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void linearCombination(final double a1, final double[] c1, final int offset1,
                                  final double a2, final double[] c2, final int offset2,
                                  final double a3, final double[] c3, final int offset3,
                                  final double a4, final double[] c4, final int offset4,
                                  final double[] result, final int resultOffset) {
        for (int i = 0; i < this.getSize(); ++i) {
            result[resultOffset + i] =
                MathArrays.linearCombination(a1, c1[offset1 + i],
                    a2, c2[offset2 + i],
                    a3, c3[offset3 + i],
                    a4, c4[offset4 + i]);
        }
    }

    /**
     * Perform addition of two derivative structures.
     * 
     * @param lhs
     *        array holding left hand side of addition
     * @param lhsOffset
     *        offset of the left hand side in its array
     * @param rhs
     *        array right hand side of addition
     * @param rhsOffset
     *        offset of the right hand side in its array
     * @param result
     *        array where result must be stored (it may be
     *        one of the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void add(final double[] lhs, final int lhsOffset,
                    final double[] rhs, final int rhsOffset,
                    final double[] result, final int resultOffset) {
        for (int i = 0; i < this.getSize(); ++i) {
            result[resultOffset + i] = lhs[lhsOffset + i] + rhs[rhsOffset + i];
        }
    }

    /**
     * Perform subtraction of two derivative structures.
     * 
     * @param lhs
     *        array holding left hand side of subtraction
     * @param lhsOffset
     *        offset of the left hand side in its array
     * @param rhs
     *        array right hand side of subtraction
     * @param rhsOffset
     *        offset of the right hand side in its array
     * @param result
     *        array where result must be stored (it may be
     *        one of the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void subtract(final double[] lhs, final int lhsOffset,
                         final double[] rhs, final int rhsOffset,
                         final double[] result, final int resultOffset) {
        for (int i = 0; i < this.getSize(); ++i) {
            result[resultOffset + i] = lhs[lhsOffset + i] - rhs[rhsOffset + i];
        }
    }

    /**
     * Perform multiplication of two derivative structures.
     * 
     * @param lhs
     *        array holding left hand side of multiplication
     * @param lhsOffset
     *        offset of the left hand side in its array
     * @param rhs
     *        array right hand side of multiplication
     * @param rhsOffset
     *        offset of the right hand side in its array
     * @param result
     *        array where result must be stored (for
     *        multiplication the result array <em>cannot</em> be one of
     *        the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void multiply(final double[] lhs, final int lhsOffset,
                         final double[] rhs, final int rhsOffset,
                         final double[] result, final int resultOffset) {
        for (int i = 0; i < this.multIndirection.length; ++i) {
            final int[][] mappingI = this.multIndirection[i];
            double r = 0;
            for (final int[] element : mappingI) {
                r += element[0] *
                        lhs[lhsOffset + element[1]] *
                        rhs[rhsOffset + element[2]];
            }
            result[resultOffset + i] = r;
        }
    }

    /**
     * Perform division of two derivative structures.
     * 
     * @param lhs
     *        array holding left hand side of division
     * @param lhsOffset
     *        offset of the left hand side in its array
     * @param rhs
     *        array right hand side of division
     * @param rhsOffset
     *        offset of the right hand side in its array
     * @param result
     *        array where result must be stored (for
     *        division the result array <em>cannot</em> be one of
     *        the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void divide(final double[] lhs, final int lhsOffset,
                       final double[] rhs, final int rhsOffset,
                       final double[] result, final int resultOffset) {
        final double[] reciprocal = new double[this.getSize()];
        this.pow(rhs, lhsOffset, -1, reciprocal, 0);
        this.multiply(lhs, lhsOffset, reciprocal, 0, result, resultOffset);
    }

    /**
     * Perform remainder of two derivative structures.
     * 
     * @param lhs
     *        array holding left hand side of remainder
     * @param lhsOffset
     *        offset of the left hand side in its array
     * @param rhs
     *        array right hand side of remainder
     * @param rhsOffset
     *        offset of the right hand side in its array
     * @param result
     *        array where result must be stored (it may be
     *        one of the input arrays)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void remainder(final double[] lhs, final int lhsOffset,
                          final double[] rhs, final int rhsOffset,
                          final double[] result, final int resultOffset) {

        // compute k such that lhs % rhs = lhs - k rhs
        final double rem = lhs[lhsOffset] % rhs[rhsOffset];
        final double k = MathLib.rint((lhs[lhsOffset] - rem) / rhs[rhsOffset]);

        // set up value
        result[resultOffset] = rem;

        // set up partial derivatives
        for (int i = 1; i < this.getSize(); ++i) {
            result[resultOffset + i] = lhs[lhsOffset + i] - k * rhs[rhsOffset + i];
        }

    }

    /**
     * Compute power of a double to a derivative structure.
     * 
     * @param a
     *        number to exponentiate
     * @param operand
     *        array holding the power
     * @param operandOffset
     *        offset of the power in its array
     * @param result
     *        array where result must be stored (for
     *        power the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     * @since 3.3
     */
    public void pow(final double a,
                    final double[] operand, final int operandOffset,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        // [a^x, ln(a) a^x, ln(a)^2 a^x,, ln(a)^3 a^x, ... ]
        final double[] function = new double[1 + this.order];
        // particular case with a==0
        if (a == 0) {
            if (operand[operandOffset] == 0) {
                function[0] = 1;
                double infinity = Double.POSITIVE_INFINITY;
                for (int i = 1; i < function.length; ++i) {
                    infinity = -infinity;
                    function[i] = infinity;
                }
            } else if (operand[operandOffset] < 0) {
                // fill function with NaN values
                Arrays.fill(function, Double.NaN);
            }
        } else {
            // fill function array with
            function[0] = MathLib.pow(a, operand[operandOffset]);
            // get log(a)
            final double lnA = MathLib.log(a);
            for (int i = 1; i < function.length; ++i) {
                function[i] = lnA * function[i - 1];
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute power of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param p
     *        power to apply
     * @param result
     *        array where result must be stored (for
     *        power the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void pow(final double[] operand, final int operandOffset, final double p,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        // [x^p, px^(p-1), p(p-1)x^(p-2), ... ]
        final double[] function = new double[1 + this.order];
        double xk = MathLib.pow(operand[operandOffset], p - this.order);
        for (int i = this.order; i > 0; --i) {
            function[i] = xk;
            xk *= operand[operandOffset];
        }
        function[0] = xk;
        double coefficient = p;
        for (int i = 1; i <= this.order; ++i) {
            function[i] *= coefficient;
            coefficient *= p - i;
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute integer power of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param n
     *        power to apply
     * @param result
     *        array where result must be stored (for
     *        power the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void pow(final double[] operand, final int operandOffset, final int n,
                    final double[] result, final int resultOffset) {

        if (n == 0) {
            // special case, x^0 = 1 for all x
            result[resultOffset] = 1.0;
            Arrays.fill(result, resultOffset + 1, resultOffset + this.getSize(), 0);
            return;
        }

        // create the power function value and derivatives
        // [x^n, nx^(n-1), n(n-1)x^(n-2), ... ]
        final double[] function = new double[1 + this.order];

        if (n > 0) {
            // strictly positive power
            final int maxOrder = MathLib.min(this.order, n);
            double xk = MathLib.pow(operand[operandOffset], n - maxOrder);
            for (int i = maxOrder; i > 0; --i) {
                function[i] = xk;
                xk *= operand[operandOffset];
            }
            function[0] = xk;
        } else {
            // strictly negative power
            final double inv = 1.0 / operand[operandOffset];
            double xk = MathLib.pow(inv, -n);
            for (int i = 0; i <= this.order; ++i) {
                function[i] = xk;
                xk *= inv;
            }
        }

        double coefficient = n;
        for (int i = 1; i <= this.order; ++i) {
            function[i] *= coefficient;
            coefficient *= n - i;
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute power of a derivative structure.
     * 
     * @param x
     *        array holding the base
     * @param xOffset
     *        offset of the base in its array
     * @param y
     *        array holding the exponent
     * @param yOffset
     *        offset of the exponent in its array
     * @param result
     *        array where result must be stored (for
     *        power the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void pow(final double[] x, final int xOffset,
                    final double[] y, final int yOffset,
                    final double[] result, final int resultOffset) {
        final double[] logX = new double[this.getSize()];
        this.log(x, xOffset, logX, 0);
        final double[] yLogX = new double[this.getSize()];
        this.multiply(logX, 0, y, yOffset, yLogX, 0);
        this.exp(yLogX, 0, result, resultOffset);
    }

    /**
     * Compute n<sup>th</sup> root of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param n
     *        order of the root
     * @param result
     *        array where result must be stored (for
     *        n<sup>th</sup> root the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void rootN(final double[] operand, final int operandOffset, final int n,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        // [x^(1/n), (1/n)x^((1/n)-1), (1-n)/n^2x^((1/n)-2), ... ]
        final double[] function = new double[1 + this.order];
        double xk;
        // xk calculation from n value
        if (n == 2) {
            function[0] = MathLib.sqrt(operand[operandOffset]);
            xk = HALF / function[0];
        } else if (n == 3) {
            function[0] = MathLib.cbrt(operand[operandOffset]);
            xk = 1.0 / (3.0 * function[0] * function[0]);
        } else {
            function[0] = MathLib.pow(operand[operandOffset], 1.0 / n);
            xk = 1.0 / (n * MathLib.pow(function[0], n - 1));
        }
        final double nReciprocal = 1.0 / n;
        final double xReciprocal = 1.0 / operand[operandOffset];
        for (int i = 1; i <= this.order; ++i) {
            // fill function coefficients with xk
            function[i] = xk;
            xk *= xReciprocal * (nReciprocal - i);
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute exponential of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        exponential the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void exp(final double[] operand, final int operandOffset,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        Arrays.fill(function, MathLib.exp(operand[operandOffset]));

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute exp(x) - 1 of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        exponential the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void expm1(final double[] operand, final int operandOffset,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        function[0] = MathLib.expm1(operand[operandOffset]);
        Arrays.fill(function, 1, 1 + this.order, MathLib.exp(operand[operandOffset]));

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute natural logarithm of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        logarithm the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void log(final double[] operand, final int operandOffset,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        // evaluate first coefficient with log
        function[0] = MathLib.log(operand[operandOffset]);
        if (this.order > 0) {
            final double inv = 1.0 / operand[operandOffset];
            // initialize xk
            double xk = inv;
            for (int i = 1; i <= this.order; ++i) {
                function[i] = xk;
                xk *= -i * inv;
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Computes shifted logarithm of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        shifted logarithm the result array <em>cannot</em> be the input array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void log1p(final double[] operand, final int operandOffset,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        // evaluate first coefficient with log
        function[0] = MathLib.log1p(operand[operandOffset]);
        if (this.order > 0) {
            final double inv = 1.0 / (1.0 + operand[operandOffset]);
            // initialize xk
            double xk = inv;
            for (int i = 1; i <= this.order; ++i) {
                function[i] = xk;
                xk *= -i * inv;
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Computes base 10 logarithm of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        base 10 logarithm the result array <em>cannot</em> be the input array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void log10(final double[] operand, final int operandOffset,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        // evaluate first coefficient with log
        function[0] = MathLib.log10(operand[operandOffset]);
        if (this.order > 0) {
            final double inv = 1.0 / operand[operandOffset];
            // initiliaz xk
            double xk = inv / MathLib.log(C_10);
            for (int i = 1; i <= this.order; ++i) {
                function[i] = xk;
                xk *= -i * inv;
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute cosine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        cosine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void cos(final double[] operand, final int operandOffset,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        function[0] = MathLib.cos(operand[operandOffset]);
        if (this.order > 0) {
            function[1] = -MathLib.sin(operand[operandOffset]);
            for (int i = 2; i <= this.order; ++i) {
                function[i] = -function[i - 2];
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute sine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        sine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void sin(final double[] operand, final int operandOffset,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        function[0] = MathLib.sin(operand[operandOffset]);
        if (this.order > 0) {
            function[1] = MathLib.cos(operand[operandOffset]);
            for (int i = 2; i <= this.order; ++i) {
                function[i] = -function[i - 2];
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute tangent of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        tangent the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void tan(final double[] operand, final int operandOffset,
                    final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double t = MathLib.tan(operand[operandOffset]);
        function[0] = t;

        if (this.order > 0) {

            // the nth order derivative of tan has the form:
            // dn(tan(x)/dxn = P_n(tan(x))
            // where P_n(t) is a degree n+1 polynomial with same parity as n+1
            // P_0(t) = t, P_1(t) = 1 + t^2, P_2(t) = 2 t (1 + t^2) ...
            // the general recurrence relation for P_n is:
            // P_n(x) = (1+t^2) P_(n-1)'(t)
            // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
            final double[] p = new double[this.order + 2];
            p[1] = 1;
            final double t2 = t * t;
            for (int n = 1; n <= this.order; ++n) {

                // update and evaluate polynomial P_n(t)
                double v = 0;
                p[n + 1] = n * p[n];
                for (int k = n + 1; k >= 0; k -= 2) {
                    v = v * t2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (k - 1) * p[k - 1] + (k - 3) * p[k - 3];
                    } else if (k == 2) {
                        p[0] = p[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= t;
                }

                function[n] = v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute arc cosine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        arc cosine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void acos(final double[] operand, final int operandOffset,
                     final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double x = operand[operandOffset];
        function[0] = MathLib.acos(x);
        if (this.order > 0) {
            // the nth order derivative of acos has the form:
            // dn(acos(x)/dxn = P_n(x) / [1 - x^2]^((2n-1)/2)
            // where P_n(x) is a degree n-1 polynomial with same parity as n-1
            // P_1(x) = -1, P_2(x) = -x, P_3(x) = -2x^2 - 1 ...
            // the general recurrence relation for P_n is:
            // P_n(x) = (1-x^2) P_(n-1)'(x) + (2n-3) x P_(n-1)(x)
            // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
            final double[] p = new double[this.order];
            p[0] = -1;
            final double x2 = x * x;
            final double f = 1.0 / (1 - x2);
            double coeff = MathLib.sqrt(f);
            function[1] = coeff * p[0];
            for (int n = 2; n <= this.order; ++n) {

                // update and evaluate polynomial P_n(x)
                double v = 0;
                p[n - 1] = (n - 1) * p[n - 2];
                for (int k = n - 1; k >= 0; k -= 2) {
                    v = v * x2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (k - 1) * p[k - 1] + (2 * n - k) * p[k - 3];
                    } else if (k == 2) {
                        p[0] = p[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= x;
                }

                coeff *= f;
                function[n] = coeff * v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute arc sine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        arc sine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void asin(final double[] operand, final int operandOffset,
                     final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double x = operand[operandOffset];
        function[0] = MathLib.asin(x);
        if (this.order > 0) {
            // the nth order derivative of asin has the form:
            // dn(asin(x)/dxn = P_n(x) / [1 - x^2]^((2n-1)/2)
            // where P_n(x) is a degree n-1 polynomial with same parity as n-1
            // P_1(x) = 1, P_2(x) = x, P_3(x) = 2x^2 + 1 ...
            // the general recurrence relation for P_n is:
            // P_n(x) = (1-x^2) P_(n-1)'(x) + (2n-3) x P_(n-1)(x)
            // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
            final double[] p = new double[this.order];
            p[0] = 1;
            final double x2 = x * x;
            final double f = 1.0 / (1 - x2);
            double coeff = MathLib.sqrt(f);
            function[1] = coeff * p[0];
            for (int n = 2; n <= this.order; ++n) {

                // update and evaluate polynomial P_n(x)
                double v = 0;
                p[n - 1] = (n - 1) * p[n - 2];
                for (int k = n - 1; k >= 0; k -= 2) {
                    v = v * x2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (k - 1) * p[k - 1] + (2 * n - k) * p[k - 3];
                    } else if (k == 2) {
                        p[0] = p[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= x;
                }

                coeff *= f;
                function[n] = coeff * v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute arc tangent of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        arc tangent the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void atan(final double[] operand, final int operandOffset,
                     final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double x = operand[operandOffset];
        function[0] = MathLib.atan(x);
        if (this.order > 0) {
            // the nth order derivative of atan has the form:
            // dn(atan(x)/dxn = Q_n(x) / (1 + x^2)^n
            // where Q_n(x) is a degree n-1 polynomial with same parity as n-1
            // Q_1(x) = 1, Q_2(x) = -2x, Q_3(x) = 6x^2 - 2 ...
            // the general recurrence relation for Q_n is:
            // Q_n(x) = (1+x^2) Q_(n-1)'(x) - 2(n-1) x Q_(n-1)(x)
            // as per polynomial parity, we can store coefficients of both Q_(n-1) and Q_n in the same array
            final double[] q = new double[this.order];
            q[0] = 1;
            final double x2 = x * x;
            final double f = 1.0 / (1 + x2);
            double coeff = f;
            function[1] = coeff * q[0];
            for (int n = 2; n <= this.order; ++n) {

                // update and evaluate polynomial Q_n(x)
                double v = 0;
                q[n - 1] = -n * q[n - 2];
                for (int k = n - 1; k >= 0; k -= 2) {
                    v = v * x2 + q[k];
                    if (k > 2) {
                        q[k - 2] = (k - 1) * q[k - 1] + (k - 1 - 2 * n) * q[k - 3];
                    } else if (k == 2) {
                        q[0] = q[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= x;
                }

                coeff *= f;
                function[n] = coeff * v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute two arguments arc tangent of a derivative structure.
     * 
     * @param y
     *        array holding the first operand
     * @param yOffset
     *        offset of the first operand in its array
     * @param x
     *        array holding the second operand
     * @param xOffset
     *        offset of the second operand in its array
     * @param result
     *        array where result must be stored (for
     *        two arguments arc tangent the result array <em>cannot</em> be the input array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void atan2(final double[] y, final int yOffset,
                      final double[] x, final int xOffset,
                      final double[] result, final int resultOffset) {

        // compute r = sqrt(x^2+y^2)
        final double[] tmp1 = new double[this.getSize()];
        // x^2
        this.multiply(x, xOffset, x, xOffset, tmp1, 0);
        final double[] tmp2 = new double[this.getSize()];
        // y^2
        this.multiply(y, yOffset, y, yOffset, tmp2, 0);
        // x^2 + y^2
        this.add(tmp1, 0, tmp2, 0, tmp2, 0);
        // r = sqrt(x^2 + y^2)
        this.rootN(tmp2, 0, 2, tmp1, 0);

        if (x[xOffset] >= 0) {

            // compute atan2(y, x) = 2 atan(y / (r + x))
            // r + x
            this.add(tmp1, 0, x, xOffset, tmp2, 0);
            // y /(r + x)
            this.divide(y, yOffset, tmp2, 0, tmp1, 0);
            // atan(y / (r + x))
            this.atan(tmp1, 0, tmp2, 0);
            for (int i = 0; i < tmp2.length; ++i) {
                // 2 * atan(y / (r + x))
                result[resultOffset + i] = 2 * tmp2[i];
            }

        } else {

            // compute atan2(y, x) = +/- pi - 2 atan(y / (r - x))
            // r - x
            this.subtract(tmp1, 0, x, xOffset, tmp2, 0);
            // y /(r - x)
            this.divide(y, yOffset, tmp2, 0, tmp1, 0);
            // atan(y / (r - x))
            this.atan(tmp1, 0, tmp2, 0);
            result[resultOffset] =
                // +/-pi - 2 * atan(y / (r - x))
                ((tmp2[0] <= 0) ? -FastMath.PI : FastMath.PI) - 2 * tmp2[0];
            for (int i = 1; i < tmp2.length; ++i) {
                // +/-pi - 2 * atan(y / (r - x))
                result[resultOffset + i] = C_N2 * tmp2[i];
            }

        }

    }

    /**
     * Compute hyperbolic cosine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        hyperbolic cosine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void cosh(final double[] operand, final int operandOffset,
                     final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        function[0] = MathLib.cosh(operand[operandOffset]);
        if (this.order > 0) {
            function[1] = MathLib.sinh(operand[operandOffset]);
            for (int i = 2; i <= this.order; ++i) {
                function[i] = function[i - 2];
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute hyperbolic sine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        hyperbolic sine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void sinh(final double[] operand, final int operandOffset,
                     final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        function[0] = MathLib.sinh(operand[operandOffset]);
        if (this.order > 0) {
            function[1] = MathLib.cosh(operand[operandOffset]);
            for (int i = 2; i <= this.order; ++i) {
                function[i] = function[i - 2];
            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute hyperbolic tangent of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        hyperbolic tangent the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void tanh(final double[] operand, final int operandOffset,
                     final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double t = MathLib.tanh(operand[operandOffset]);
        function[0] = t;

        if (this.order > 0) {

            // the nth order derivative of tanh has the form:
            // dn(tanh(x)/dxn = P_n(tanh(x))
            // where P_n(t) is a degree n+1 polynomial with same parity as n+1
            // P_0(t) = t, P_1(t) = 1 - t^2, P_2(t) = -2 t (1 - t^2) ...
            // the general recurrence relation for P_n is:
            // P_n(x) = (1-t^2) P_(n-1)'(t)
            // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
            final double[] p = new double[this.order + 2];
            p[1] = 1;
            final double t2 = t * t;
            for (int n = 1; n <= this.order; ++n) {

                // update and evaluate polynomial P_n(t)
                double v = 0;
                p[n + 1] = -n * p[n];
                for (int k = n + 1; k >= 0; k -= 2) {
                    v = v * t2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (k - 1) * p[k - 1] - (k - 3) * p[k - 3];
                    } else if (k == 2) {
                        p[0] = p[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= t;
                }

                function[n] = v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute inverse hyperbolic cosine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        inverse hyperbolic cosine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void acosh(final double[] operand, final int operandOffset,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double x = operand[operandOffset];
        function[0] = MathLib.acosh(x);
        if (this.order > 0) {
            // the nth order derivative of acosh has the form:
            // dn(acosh(x)/dxn = P_n(x) / [x^2 - 1]^((2n-1)/2)
            // where P_n(x) is a degree n-1 polynomial with same parity as n-1
            // P_1(x) = 1, P_2(x) = -x, P_3(x) = 2x^2 + 1 ...
            // the general recurrence relation for P_n is:
            // P_n(x) = (x^2-1) P_(n-1)'(x) - (2n-3) x P_(n-1)(x)
            // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
            final double[] p = new double[this.order];
            p[0] = 1;
            final double x2 = x * x;
            final double f = 1.0 / (x2 - 1);
            double coeff = MathLib.sqrt(f);
            function[1] = coeff * p[0];
            for (int n = 2; n <= this.order; ++n) {

                // update and evaluate polynomial P_n(x)
                double v = 0;
                p[n - 1] = (1 - n) * p[n - 2];
                for (int k = n - 1; k >= 0; k -= 2) {
                    v = v * x2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (1 - k) * p[k - 1] + (k - 2 * n) * p[k - 3];
                    } else if (k == 2) {
                        p[0] = -p[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= x;
                }

                coeff *= f;
                function[n] = coeff * v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute inverse hyperbolic sine of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        inverse hyperbolic sine the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void asinh(final double[] operand, final int operandOffset,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double x = operand[operandOffset];
        function[0] = MathLib.asinh(x);
        if (this.order > 0) {
            // the nth order derivative of asinh has the form:
            // dn(asinh(x)/dxn = P_n(x) / [x^2 + 1]^((2n-1)/2)
            // where P_n(x) is a degree n-1 polynomial with same parity as n-1
            // P_1(x) = 1, P_2(x) = -x, P_3(x) = 2x^2 - 1 ...
            // the general recurrence relation for P_n is:
            // P_n(x) = (x^2+1) P_(n-1)'(x) - (2n-3) x P_(n-1)(x)
            // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
            final double[] p = new double[this.order];
            p[0] = 1;
            final double x2 = x * x;
            final double f = 1.0 / (1 + x2);
            double coeff = MathLib.sqrt(f);
            function[1] = coeff * p[0];
            for (int n = 2; n <= this.order; ++n) {

                // update and evaluate polynomial P_n(x)
                double v = 0;
                p[n - 1] = (1 - n) * p[n - 2];
                for (int k = n - 1; k >= 0; k -= 2) {
                    v = v * x2 + p[k];
                    if (k > 2) {
                        p[k - 2] = (k - 1) * p[k - 1] + (k - 2 * n) * p[k - 3];
                    } else if (k == 2) {
                        p[0] = p[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= x;
                }

                coeff *= f;
                function[n] = coeff * v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute inverse hyperbolic tangent of a derivative structure.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param result
     *        array where result must be stored (for
     *        inverse hyperbolic tangent the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void atanh(final double[] operand, final int operandOffset,
                      final double[] result, final int resultOffset) {

        // create the function value and derivatives
        final double[] function = new double[1 + this.order];
        final double x = operand[operandOffset];
        function[0] = MathLib.atanh(x);
        if (this.order > 0) {
            // the nth order derivative of atanh has the form:
            // dn(atanh(x)/dxn = Q_n(x) / (1 - x^2)^n
            // where Q_n(x) is a degree n-1 polynomial with same parity as n-1
            // Q_1(x) = 1, Q_2(x) = 2x, Q_3(x) = 6x^2 + 2 ...
            // the general recurrence relation for Q_n is:
            // Q_n(x) = (1-x^2) Q_(n-1)'(x) + 2(n-1) x Q_(n-1)(x)
            // as per polynomial parity, we can store coefficients of both Q_(n-1) and Q_n in the same array
            final double[] q = new double[this.order];
            q[0] = 1;
            final double x2 = x * x;
            final double f = 1.0 / (1 - x2);
            double coeff = f;
            function[1] = coeff * q[0];
            for (int n = 2; n <= this.order; ++n) {

                // update and evaluate polynomial Q_n(x)
                double v = 0;
                q[n - 1] = n * q[n - 2];
                for (int k = n - 1; k >= 0; k -= 2) {
                    v = v * x2 + q[k];
                    if (k > 2) {
                        q[k - 2] = (k - 1) * q[k - 1] + (2 * n - k + 1) * q[k - 3];
                    } else if (k == 2) {
                        q[0] = q[1];
                    }
                }
                if ((n & 0x1) == 0) {
                    v *= x;
                }

                coeff *= f;
                function[n] = coeff * v;

            }
        }

        // apply function composition
        this.compose(operand, operandOffset, function, result, resultOffset);

    }

    /**
     * Compute composition of a derivative structure by a function.
     * 
     * @param operand
     *        array holding the operand
     * @param operandOffset
     *        offset of the operand in its array
     * @param f
     *        array of value and derivatives of the function at
     *        the current point (i.e. at {@code operand[operandOffset]}).
     * @param result
     *        array where result must be stored (for
     *        composition the result array <em>cannot</em> be the input
     *        array)
     * @param resultOffset
     *        offset of the result in its array
     */
    public void compose(final double[] operand, final int operandOffset, final double[] f,
                        final double[] result, final int resultOffset) {
        // loop on the comIndirection length
        for (int i = 0; i < this.compIndirection.length; ++i) {
            final int[][] mappingI = this.compIndirection[i];
            // initialize r
            double r = 0;
            // loop on the integers in mappingI
            for (final int[] mappingIJ : mappingI) {
                double product = mappingIJ[0] * f[mappingIJ[1]];
                for (int k = 2; k < mappingIJ.length; ++k) {
                    product *= operand[operandOffset + mappingIJ[k]];
                }
                r += product;
            }
            // fill result array with r value
            result[resultOffset + i] = r;
        }
    }

    /**
     * Evaluate Taylor expansion of a derivative structure.
     * 
     * @param ds
     *        array holding the derivative structure
     * @param dsOffset
     *        offset of the derivative structure in its array
     * @param delta
     *        parameters offsets (&Delta;x, &Delta;y, ...)
     * @return value of the Taylor expansion at x + &Delta;x, y + &Delta;y, ...
     */
    public double taylor(final double[] ds, final int dsOffset, final double... delta) {
        // initialize the output value
        double value = 0;
        // loop on the array size of the partial derivatives data
        for (int i = this.getSize() - 1; i >= 0; --i) {
            // get the derivative orders
            final int[] orders = this.getPartialDerivativeOrders(i);
            double term = ds[dsOffset + i];
            for (int k = 0; k < orders.length; ++k) {
                if (orders[k] > 0) {
                    // evaluate each term by order
                    term *= MathLib.pow(delta[k], orders[k]) / ArithmeticUtils.factorial(orders[k]);
                }
            }
            value += term;
        }
        return value;
    }

    /**
     * Check rules set compatibility.
     * 
     * @param compiler
     *        other compiler to check against instance
     * @exception DimensionMismatchException
     *            if number of free parameters or orders are inconsistent
     */
    public void checkCompatibility(final DSCompiler compiler) {
        if (this.parameters != compiler.parameters) {
            throw new DimensionMismatchException(this.parameters, compiler.parameters);
        }
        if (this.order != compiler.order) {
            throw new DimensionMismatchException(this.order, compiler.order);
        }
    }
    
    /**
     * Returns indirection arrays for multiplication.
     * @return indirection arrays for multiplication
     */
    @SuppressWarnings({ "PMD.MethodReturnsInternalArray", "PMD.AvoidProtectedMethodInFinalClassNotExtending" })
    // Reason: performance
    // Reason: unit test of method
    protected int[][][] getMultIndirection() {
        return this.multIndirection;
    }
    
    /**
     * Returns indirection arrays for function composition.
     * @return indirection arrays for function composition
     */
    @SuppressWarnings({ "PMD.MethodReturnsInternalArray", "PMD.AvoidProtectedMethodInFinalClassNotExtending" })
    // Reason: performance
    protected int[][][] getCompIndirection() {
        return this.compIndirection;
    }
}
