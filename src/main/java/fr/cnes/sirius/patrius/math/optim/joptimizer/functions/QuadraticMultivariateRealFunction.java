/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.functions;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;

/**
 * Represent a function in the form of <br>
 * f(x) := 1/2 x.P.x + q.x + r <br>
 * where x, q &#8712; R<sup>n</sup>, P is a symmetric nXn matrix and r &#8712; R.
 * 
 * <br>
 * NOTE1: remember the two following propositions hold:
 * <ol>
 * <li>A function f(x) is a quadratic form if and only if it can be written as f(x) = x.P.x for a
 * symmetric matrix P (f can even be written as x.P1.x with P1 not symmetric, for example <br>
 * f = x^2 + 2 x y + y^2 we can written with P={{1, 1}, {1, 1}} symmetric or with P1={{1, -1}, {3,
 * 1}} not symmetric, but here we are interested in symmetric matrices for they convexity
 * properties).</li>
 * <li>Let f(x) = x.P.x be a quadratic form with associated symmetric matrix P, then we have:
 * <ul>
 * <li>f is convex <=> P is positive semidefinite</li>
 * <li>f is concave <=> P is negative semidefinite</li>
 * <li>f is strictly convex <=> P is positive definite</li>
 * <li>f is strictly concave <=> P is negative definite</li>
 * </ul>
 * </li>
 * </ol>
 * 
 * NOTE2: precisely speaking, this class should have been named "PolynomialOfDegreeTwo", because
 * by definition a quadratic form in the variables x1,x2,...,xn is a polynomial function where all
 * terms
 * in the functional expression have order two. A general polynomial function f(x) of degree two can
 * be written
 * as the sum of a quadratic form Q = x.P.x and a linear form L = q.x (plus a constant term r): <br>
 * f(x) = Q + L + r <br>
 * Because L is convex, f is convex if so is Q.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * @see "Eivind Eriksen, Quadratic Forms and Convexity"
 * 
 * @since 4.6
 */
public class QuadraticMultivariateRealFunction implements
        TwiceDifferentiableMultivariateRealFunction {

    /** Message */
    private static final String IMPOSSIBLE_FUNCT = "Impossible to create the function";
    /**
     * Quadratic factor.
     */
    protected final RealMatrix p;

    /**
     * Dimension of the function argument.
     */
    private final int dim;

    /**
     * Linear factor.
     */
    private final RealVector q;

    /**
     * Constant factor.
     */
    private final double r;

    /**
     * For the special case of a quadratic form, the Hessian is independent of X, namely it is P.
     */
    private final double[][] hessianValue;

    /**
     * Constructor
     * 
     * @param pMatrix matrix P
     * @param qVector vector Q
     * @param rValue value
     * @param checkSymmetry check the symmetry?
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: complex JOptimizr code kept as such
    public QuadraticMultivariateRealFunction(final double[][] pMatrix,
            final double[] qVector,
            final double rValue,
            final boolean checkSymmetry) {
        if (pMatrix != null) {
            this.p = new BlockRealMatrix(pMatrix);
        } else {
            this.p = null;
        }
        if (qVector != null) {
            this.q = new ArrayRealVector(qVector);
        } else {
            this.q = null;
        }
        this.r = rValue;

        if (p == null && q == null) {
            throw new IllegalArgumentException(IMPOSSIBLE_FUNCT);
        }
        if (p != null && !p.isSquare()) {
            throw new IllegalArgumentException("P is not square");
        }
        if (p != null && checkSymmetry && !p.isSymmetric()) {
            throw new IllegalArgumentException("P is not symmetric");
        }

        if (p != null) {
            this.dim = p.getColumnDimension();
        } else {
            this.dim = q.getDimension();
        }

        hessianValue = hessianSlow();
    }

    /**
     * Constructor
     * 
     * @param pMatrix matrix P
     * @param qVector vector Q
     * @param rValue value
     */
    public QuadraticMultivariateRealFunction(final double[][] pMatrix,
            final double[] qVector,
            final double rValue) {
        this(pMatrix, qVector, rValue, false);
    }

    /**
     * Evaluation of the function at point X
     */
    @Override
    public final double value(final double[] xValue) {
        final RealVector x = new ArrayRealVector(xValue);
        double ret = r;
        if (p != null) {
            ret += 0.5 * x.dotProduct(p.operate(x));
        }
        if (q != null) {
            ret += q.dotProduct(x);
        }
        return ret;
    }

    /**
     * Function gradient at point X
     */
    @Override
    public final double[] gradient(final double[] xValue) {
        final RealVector x = new ArrayRealVector(xValue);
        RealVector ret = null;
        if (p != null) {
            if (q != null) {
                // P.x + q
                ret = AlgebraUtils.zMult(p, x, q, 1);
            } else {
                ret = p.operate(x);
            }
        } else {
            ret = q;
        }
        return ret.toArray();

    }

    /**
     * Function hessian at point X.
     */
    @Override
    public final double[][] hessian(final double[] xValue) {
        if (hessianValue == null) {
            return null;
        } else {
            return (double[][]) hessianValue.clone();
        }
    }

    /**
     * 
     * @return hessian
     */
    private final double[][] hessianSlow() {
        RealMatrix ret = null;
        if (p != null) {
            ret = p;
        } else {
            return null;
        }
        return ret.getData();
    }

    /**
     * Get dimension
     */
    @Override
    public int getDim() {
        return this.dim;
    }

}
