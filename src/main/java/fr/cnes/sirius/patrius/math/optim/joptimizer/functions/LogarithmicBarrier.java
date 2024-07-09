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
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.functions;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Default barrier function for the barrier method algorithm. <br>
 * If f_i(x) are the inequalities of the problem, theh we have: <br>
 * <i>&Phi;</i> = - Sum_i[log(-f_i(x))]
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.2.1"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public class LogarithmicBarrier implements BarrierFunction {

    /** Scalar */
    private static final double SCALAR = -0.5;
    /** Convex multivariate function */
    private final ConvexMultivariateRealFunction[] fi;
    /** Dimension */
    private final int dim;

    /**
     * Create the logarithmic barrier function.
     * @param f convex multivariate function
     * @param dimension dimension
     * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.2.1"
     */
    public LogarithmicBarrier(final ConvexMultivariateRealFunction[] f,
            final int dimension) {
        this.fi = f.clone();
        this.dim = dimension;
    }

    /**
     * Evaluation of the function at point X.
     */
    @Override
    public double value(final double[] val) {
        double psi = 0;
        for (int j = 0; j < fi.length; j++) {
            final double ineqValuejX = fi[j].value(val);
            if (ineqValuejX >= 0 || Double.isNaN(ineqValuejX)) {
                return Double.NaN;
            }
            psi -= MathLib.log(-ineqValuejX);
        }
        return psi;
    }

    /**
     * Function gradient at point X
     */
    @Override
    public double[] gradient(final double[] value) {
        RealVector gradFiSum = new ArrayRealVector(getDim());
        for (int j = 0; j < fi.length; j++) {
            final double ineqValuejX = fi[j].value(value);
            final RealVector ineqGradjX = new ArrayRealVector(fi[j].gradient(value));
            // gradFiSum.assign(ineqGradjX.assign(Mult.mult(-1./ineqValuejX)), Functions.plus);
            gradFiSum = gradFiSum.add(ineqGradjX.mapMultiply(-1. / ineqValuejX));
        }
        return gradFiSum.toArray();
    }

    /**
     * Function hessian at point X.
     */
    @Override
    public double[][] hessian(final double[] value) {
        RealMatrix hessSum = new BlockRealMatrix(new double[getDim()][getDim()]);
        RealMatrix gradSum = new BlockRealMatrix(new double[getDim()][getDim()]);
        for (int j = 0; j < fi.length; j++) {
            final double ineqValuejX = fi[j].value(value);  // inequality value
            final double[][] fijHessianX = fi[j].hessian(value);  // hessian value array
            final RealMatrix ineqHessjX;
            if (fijHessianX != null) {
                ineqHessjX = new BlockRealMatrix(fijHessianX);
            }else {
                ineqHessjX = null;
            }
            final RealVector ineqGradjX = new ArrayRealVector(fi[j].gradient(value));  // gradient value vector
            if (ineqHessjX != null) {
                hessSum = hessSum.add(ineqHessjX.scalarMultiply(-1. / ineqValuejX));
            }
            //  gradsum = gradsum + ineqGradjX.ineqGradjX * 1/ineqValuejX^2
            gradSum = gradSum.add(ineqGradjX.outerProduct(ineqGradjX).scalarMultiply(1. / MathLib.pow(ineqValuejX, 2)));
        }
        return hessSum.add(gradSum).getData(false);
    }

    /**
     * Get dimension
     */
    @Override
    public int getDim() {
        return this.dim;
    }

    /**
     * Calculates the duality gap for a barrier method build with this barrier function
     */
    @Override
    public double getDualityGap(final double t) {
        return (fi.length) / t;
    }

    /**
     * Create the barrier function for the Phase I.
     * It is a LogarithmicBarrier for the constraints: <br>
     * fi(X)-s, i=1,...,n
     */
    @Override
    public BarrierFunction createPhase1BarrierFunction() {

        final int dimPh1 = dim + 1;
        final ConvexMultivariateRealFunction[] inequalitiesPh1 = new ConvexMultivariateRealFunction[this.fi.length];
        for (int i = 0; i < inequalitiesPh1.length; i++) {

            final ConvexMultivariateRealFunction originalFi = this.fi[i];

            final ConvexMultivariateRealFunction f = new ConvexMultivariateRealFunction() {

                /**
                 * Evaluation of the function at point X.
                 */
                @Override
                public double value(final double[] val) {
                    final RealVector y = new ArrayRealVector(val);
                    final RealVector x = y.getSubVector(0, dim);
                    return originalFi.value(x.toArray()) - y.getEntry(dimPh1 - 1);
                }

                /**
                 * Function gradient at point X
                 */
                @Override
                public double[] gradient(final double[] value) {
                    final RealVector y = new ArrayRealVector(value);
                    final RealVector x = y.getSubVector(0, dim);
                    final RealVector origGrad = new ArrayRealVector(originalFi.gradient(x.toArray()));
                    RealVector ret = new ArrayRealVector(1, -1);
                    ret = origGrad.append(ret);
                    return ret.toArray();
                }

                /**
                 * Function hessian at point X.
                 */
                @Override
                public double[][] hessian(final double[] value) {
                    final RealVector y = new ArrayRealVector(value);
                    final RealVector x = y.getSubVector(0, dim);
                    final RealMatrix origHess;
                    final double[][] origFiHessX = originalFi.hessian(x.toArray()); // original hessian array
                    if (origFiHessX == null) {
                        return null;  // return an array of zeros
                    } else {
                        origHess = new BlockRealMatrix(origFiHessX);
                        final RealMatrix[][] parts = 
                                new RealMatrix[][] {{origHess, null},{null, new BlockRealMatrix(1, 1)}};
                        return AlgebraUtils.composeMatrix(parts).getData(); // return composed matrix
                    }
                }

                /**
                 * Get dimension
                 */
                @Override
                public int getDim() {
                    return dimPh1;
                }
            };
            inequalitiesPh1[i] = f;
        }

        return new LogarithmicBarrier(inequalitiesPh1, dimPh1);
    }

    /**
     * Calculates the initial value for the s parameter in Phase I.
     * Return s = max(fi(x))
     * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.6.2"
     */
    @Override
    public double calculatePhase1InitialFeasiblePoint(final double[] originalNotFeasiblePoint,
            final double tolerance) {
        final RealVector fiX0NF = new ArrayRealVector(fi.length);
        for (int i = 0; i < fi.length; i++) {
            fiX0NF.setEntry(i, this.fi[i].value(originalNotFeasiblePoint));
        }

        // lucky strike?
        final int maxIneqIndex = fiX0NF.getMaxIndex();
        if (fiX0NF.getEntry(maxIneqIndex) < 0) {
            // the given notFeasible starting point is in fact already feasible
            return -1;
        }

        double s = MathLib.pow(tolerance, SCALAR);
        for (int i = 0; i < fiX0NF.getDimension(); i++) {
            // Calculates initial value for the s parameter in Phase I.
            s = MathLib.max(s, fiX0NF.getEntry(i) * MathLib.pow(tolerance, SCALAR));
        }

        return s;
    }
}
