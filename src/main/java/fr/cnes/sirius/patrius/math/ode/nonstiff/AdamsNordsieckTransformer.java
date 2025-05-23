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
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.math.fraction.BigFraction;
import fr.cnes.sirius.patrius.math.linear.Array2DRowFieldMatrix;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayFieldVector;
import fr.cnes.sirius.patrius.math.linear.FieldDecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.FieldLUDecomposition;
import fr.cnes.sirius.patrius.math.linear.FieldMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * Transformer to Nordsieck vectors for Adams integrators.
 * <p>
 * This class is used by {@link AdamsBashforthIntegrator Adams-Bashforth} and {@link AdamsMoultonIntegrator
 * Adams-Moulton} integrators to convert between classical representation with several previous first derivatives and
 * Nordsieck representation with higher order scaled derivatives.
 * </p>
 * 
 * <p>
 * We define scaled derivatives s<sub>i</sub>(n) at step n as:
 * 
 * <pre>
 * s<sub>1</sub>(n) = h y'<sub>n</sub> for first derivative
 * s<sub>2</sub>(n) = h<sup>2</sup>/2 y''<sub>n</sub> for second derivative
 * s<sub>3</sub>(n) = h<sup>3</sup>/6 y'''<sub>n</sub> for third derivative
 * ...
 * s<sub>k</sub>(n) = h<sup>k</sup>/k! y<sup>(k)</sup><sub>n</sub> for k<sup>th</sup> derivative
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * With the previous definition, the classical representation of multistep methods uses first derivatives only, i.e. it
 * handles y<sub>n</sub>, s<sub>1</sub>(n) and q<sub>n</sub> where q<sub>n</sub> is defined as:
 * 
 * <pre>
 *   q<sub>n</sub> = [ s<sub>1</sub>(n-1) s<sub>1</sub>(n-2) ... s<sub>1</sub>(n-(k-1)) ]<sup>T</sup>
 * </pre>
 * 
 * (we omit the k index in the notation for clarity).
 * </p>
 * 
 * <p>
 * Another possible representation uses the Nordsieck vector with higher degrees scaled derivatives all taken at the
 * same step, i.e it handles y<sub>n</sub>, s<sub>1</sub>(n) and r<sub>n</sub>) where r<sub>n</sub> is defined as:
 * 
 * <pre>
 * r<sub>n</sub> = [ s<sub>2</sub>(n), s<sub>3</sub>(n) ... s<sub>k</sub>(n) ]<sup>T</sup>
 * </pre>
 * 
 * (here again we omit the k index in the notation for clarity)
 * </p>
 * 
 * <p>
 * Taylor series formulas show that for any index offset i, s<sub>1</sub>(n-i) can be computed from s<sub>1</sub>(n),
 * s<sub>2</sub>(n) ... s<sub>k</sub>(n), the formula being exact for degree k polynomials.
 * 
 * <pre>
 * s<sub>1</sub>(n-i) = s<sub>1</sub>(n) + &sum;<sub>j&gt;1</sub> j (-i)<sup>j-1</sup> s<sub>j</sub>(n)
 * </pre>
 * 
 * The previous formula can be used with several values for i to compute the transform between classical representation
 * and Nordsieck vector at step end. The transform between r<sub>n</sub> and q<sub>n</sub> resulting from the Taylor
 * series formulas above is:
 * 
 * <pre>
 * q<sub>n</sub> = s<sub>1</sub>(n) u + P r<sub>n</sub>
 * </pre>
 * 
 * where u is the [ 1 1 ... 1 ]<sup>T</sup> vector and P is the (k-1)&times;(k-1) matrix built with the j
 * (-i)<sup>j-1</sup> terms:
 * 
 * <pre>
 *        [  -2   3   -4    5  ... ]
 *        [  -4  12  -32   80  ... ]
 *   P =  [  -6  27 -108  405  ... ]
 *        [  -8  48 -256 1280  ... ]
 *        [          ...           ]
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Changing -i into +i in the formula above can be used to compute a similar transform between classical representation
 * and Nordsieck vector at step start. The resulting matrix is simply the absolute value of matrix P.
 * </p>
 * 
 * <p>
 * For {@link AdamsBashforthIntegrator Adams-Bashforth} method, the Nordsieck vector at step n+1 is computed from the
 * Nordsieck vector at step n as follows:
 * <ul>
 * <li>y<sub>n+1</sub> = y<sub>n</sub> + s<sub>1</sub>(n) + u<sup>T</sup> r<sub>n</sub></li>
 * <li>s<sub>1</sub>(n+1) = h f(t<sub>n+1</sub>, y<sub>n+1</sub>)</li>
 * <li>
 * r<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u + P<sup>-1</sup> A P r<sub>n</sub>
 * </li>
 * </ul>
 * where A is a rows shifting matrix (the lower left part is an identity matrix):
 * 
 * <pre>
 *        [ 0 0   ...  0 0 | 0 ]
 *        [ ---------------+---]
 *        [ 1 0   ...  0 0 | 0 ]
 *    A = [ 0 1   ...  0 0 | 0 ]
 *        [       ...      | 0 ]
 *        [ 0 0   ...  1 0 | 0 ]
 *        [ 0 0   ...  0 1 | 0 ]
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * For {@link AdamsMoultonIntegrator Adams-Moulton} method, the predicted Nordsieck vector at step n+1 is computed from
 * the Nordsieck vector at step n as follows:
 * <ul>
 * <li>Y<sub>n+1</sub> = y<sub>n</sub> + s<sub>1</sub>(n) + u<sup>T</sup> r<sub>n</sub></li>
 * <li>S<sub>1</sub>(n+1) = h f(t<sub>n+1</sub>, Y<sub>n+1</sub>)</li>
 * <li>
 * R<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u + P<sup>-1</sup> A P r<sub>n</sub>
 * </li>
 * </ul>
 * From this predicted vector, the corrected vector is computed as follows:
 * <ul>
 * <li>y<sub>n+1</sub> = y<sub>n</sub> + S<sub>1</sub>(n+1) + [ -1 +1 -1 +1 ... &plusmn;1 ] r<sub>n+1</sub></li>
 * <li>s<sub>1</sub>(n+1) = h f(t<sub>n+1</sub>, y<sub>n+1</sub>)</li>
 * <li>r<sub>n+1</sub> = R<sub>n+1</sub> + (s<sub>1</sub>(n+1) - S<sub>1</sub>(n+1)) P<sup>-1</sup> u</li>
 * </ul>
 * where the upper case Y<sub>n+1</sub>, S<sub>1</sub>(n+1) and R<sub>n+1</sub> represent the predicted states whereas
 * the lower case y<sub>n+1</sub>, s<sub>n+1</sub> and r<sub>n+1</sub> represent the corrected states.
 * </p>
 * 
 * <p>
 * We observe that both methods use similar update formulas. In both cases a P<sup>-1</sup>u vector and a P<sup>-1</sup>
 * A P matrix are used that do not depend on the state, they only depend on k. This class handles these transformations.
 * </p>
 * 
 * @version $Id: AdamsNordsieckTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public final class AdamsNordsieckTransformer {

    /** Cache for already computed coefficients. */
    private static final Map<Integer, AdamsNordsieckTransformer> CACHE = new ConcurrentHashMap<>();

    /** Update matrix for the higher order derivatives h<sup>2</sup>/2y'', h<sup>3</sup>/6 y''' ... */
    private final Array2DRowRealMatrix update;

    /** Update coefficients of the higher order derivatives wrt y'. */
    private final double[] c1;

    /**
     * Simple constructor.
     * 
     * @param nSteps
     *        number of steps of the multistep method
     *        (excluding the one being computed)
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private AdamsNordsieckTransformer(final int nSteps) {

        // compute exact coefficients
        final FieldMatrix<BigFraction> bigP = buildP(nSteps);
        final FieldDecompositionSolver<BigFraction> pSolver =
            new FieldLUDecomposition<>(bigP).getSolver();

        final BigFraction[] u = new BigFraction[nSteps];
        Arrays.fill(u, BigFraction.ONE);
        final BigFraction[] bigC1 = pSolver
            .solve(new ArrayFieldVector<>(u, false)).toArray();

        // update coefficients are computed by combining transform from
        // Nordsieck to multistep, then shifting rows to represent step advance
        // then applying inverse transform
        final BigFraction[][] shiftedP = bigP.getData();
        for (int i = shiftedP.length - 1; i > 0; --i) {
            // shift rows
            shiftedP[i] = shiftedP[i - 1];
        }
        shiftedP[0] = new BigFraction[nSteps];
        Arrays.fill(shiftedP[0], BigFraction.ZERO);
        final FieldMatrix<BigFraction> bigMSupdate =
            pSolver.solve(new Array2DRowFieldMatrix<>(shiftedP, false));

        // convert coefficients to double
        this.update = MatrixUtils.bigFractionMatrixToRealMatrix(bigMSupdate);
        this.c1 = new double[nSteps];
        for (int i = 0; i < nSteps; ++i) {
            this.c1[i] = bigC1[i].doubleValue();
        }

    }

    /**
     * Get the Nordsieck transformer for a given number of steps.
     * 
     * @param nSteps
     *        number of steps of the multistep method
     *        (excluding the one being computed)
     * @return Nordsieck transformer for the specified number of steps
     */
    public static AdamsNordsieckTransformer getInstance(final int nSteps) {
        AdamsNordsieckTransformer t = CACHE.get(nSteps);
        if (t == null) {
            t = new AdamsNordsieckTransformer(nSteps);
            CACHE.put(nSteps, t);
        }
        return t;
    }

    /**
     * Get the number of steps of the method
     * (excluding the one being computed).
     * 
     * @return number of steps of the method
     *         (excluding the one being computed)
     */
    public int getNSteps() {
        return this.c1.length;
    }

    /**
     * Build the P matrix.
     * <p>
     * The P matrix general terms are shifted j (-i)<sup>j-1</sup> terms:
     * 
     * <pre>
     *        [  -2   3   -4    5  ... ]
     *        [  -4  12  -32   80  ... ]
     *   P =  [  -6  27 -108  405  ... ]
     *        [  -8  48 -256 1280  ... ]
     *        [          ...           ]
     * </pre>
     * 
     * </p>
     * 
     * @param nSteps
     *        number of steps of the multistep method
     *        (excluding the one being computed)
     * @return P matrix
     */
    private static FieldMatrix<BigFraction> buildP(final int nSteps) {

        // Initialize matrix data
        final BigFraction[][] pData = new BigFraction[nSteps][nSteps];

        for (int i = 0; i < pData.length; ++i) {
            // build the P matrix elements from Taylor series formulas
            final BigFraction[] pI = pData[i];
            final int factor = -(i + 1);
            int aj = factor;
            // build the current row
            for (int j = 0; j < pI.length; ++j) {
                pI[j] = new BigFraction(aj * (j + 2));
                aj *= factor;
            }
        }

        // Create and return matrix from computed data
        return new Array2DRowFieldMatrix<>(pData, false);
    }

    /**
     * Initialize the high order scaled derivatives at step start.
     * 
     * @param h
     *        step size to use for scaling
     * @param t
     *        first steps times
     * @param y
     *        first steps states
     * @param yDot
     *        first steps derivatives
     * @return Nordieck vector at first step (h<sup>2</sup>/2 y''<sub>n</sub>,
     *         h<sup>3</sup>/6 y'''<sub>n</sub> ... h<sup>k</sup>/k! y<sup>(k)</sup><sub>n</sub>)
     */
    public Array2DRowRealMatrix initializeHighOrderDerivatives(final double h, final double[] t,
                                                               final double[][] y,
                                                               final double[][] yDot) {

        // using Taylor series with di = ti - t0, we get:
        // y(ti) - y(t0) - di y'(t0) = di^2 / h^2 s2 + ... + di^k / h^k sk + O(h^(k+1))
        // y'(ti) - y'(t0) = 2 di / h^2 s2 + ... + k di^(k-1) / h^k sk + O(h^k)
        // we write these relations for i = 1 to i= n-1 as a set of 2(n-1) linear
        // equations depending on the Nordsieck vector [s2 ... sk]
        final double[][] a = new double[2 * (y.length - 1)][this.c1.length];
        final double[][] b = new double[2 * (y.length - 1)][y[0].length];
        final double[] y0 = y[0];
        final double[] yDot0 = yDot[0];
        for (int i = 1; i < y.length; ++i) {

            final double di = t[i] - t[0];
            final double ratio = di / h;
            double dikM1Ohk = 1 / h;

            // linear coefficients of equations
            // y(ti) - y(t0) - di y'(t0) and y'(ti) - y'(t0)
            final double[] aI = a[2 * i - 2];
            final double[] aDotI = a[2 * i - 1];
            for (int j = 0; j < aI.length; ++j) {
                dikM1Ohk *= ratio;
                aI[j] = di * dikM1Ohk;
                aDotI[j] = (j + 2) * dikM1Ohk;
            }

            // expected value of the previous equations
            final double[] yI = y[i];
            final double[] yDotI = yDot[i];
            final double[] bI = b[2 * i - 2];
            final double[] bDotI = b[2 * i - 1];
            for (int j = 0; j < yI.length; ++j) {
                bI[j] = yI[j] - y0[j] - di * yDot0[j];
                bDotI[j] = yDotI[j] - yDot0[j];
            }

        }

        // solve the rectangular system in the least square sense
        // to get the best estimate of the Nordsieck vector [s2 ... sk]
        final QRDecomposition decomposition;
        decomposition = new QRDecomposition(new Array2DRowRealMatrix(a, false));
        final RealMatrix x = decomposition.getSolver().solve(new Array2DRowRealMatrix(b, false));
        return new Array2DRowRealMatrix(x.getData(), false);
    }

    /**
     * Update the high order scaled derivatives for Adams integrators (phase 1).
     * <p>
     * The complete update of high order derivatives has a form similar to:
     * 
     * <pre>
     * r<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u + P<sup>-1</sup> A P r<sub>n</sub>
     * </pre>
     * 
     * this method computes the P<sup>-1</sup> A P r<sub>n</sub> part.
     * </p>
     * 
     * @param highOrder
     *        high order scaled derivatives
     *        (h<sup>2</sup>/2 y'', ... h<sup>k</sup>/k! y(k))
     * @return updated high order derivatives
     * @see #updateHighOrderDerivativesPhase2(double[], double[], Array2DRowRealMatrix)
     */
    public Array2DRowRealMatrix updateHighOrderDerivativesPhase1(final Array2DRowRealMatrix highOrder) {
        return this.update.multiply(highOrder);
    }

    /**
     * Update the high order scaled derivatives Adams integrators (phase 2).
     * <p>
     * The complete update of high order derivatives has a form similar to:
     * 
     * <pre>
     * r<sub>n+1</sub> = (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u + P<sup>-1</sup> A P r<sub>n</sub>
     * </pre>
     * 
     * this method computes the (s<sub>1</sub>(n) - s<sub>1</sub>(n+1)) P<sup>-1</sup> u part.
     * </p>
     * <p>
     * Phase 1 of the update must already have been performed.
     * </p>
     * 
     * @param start
     *        first order scaled derivatives at step start
     * @param end
     *        first order scaled derivatives at step end
     * @param highOrder
     *        high order scaled derivatives, will be modified
     *        (h<sup>2</sup>/2 y'', ... h<sup>k</sup>/k! y(k))
     * @see #updateHighOrderDerivativesPhase1(Array2DRowRealMatrix)
     */
    public void updateHighOrderDerivativesPhase2(final double[] start,
                                                 final double[] end,
                                                 final Array2DRowRealMatrix highOrder) {
        final double[][] data = highOrder.getDataRef();
        for (int i = 0; i < data.length; ++i) {
            final double[] dataI = data[i];
            final double c1I = this.c1[i];
            for (int j = 0; j < dataI.length; ++j) {
                dataI[j] += c1I * (start[j] - end[j]);
            }
        }
    }

}
