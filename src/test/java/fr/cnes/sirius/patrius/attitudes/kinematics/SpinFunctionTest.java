/**
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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.kinematics;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateVectorFunctionDifferentiator;
import fr.cnes.sirius.patrius.math.analysis.function.Cos;
import fr.cnes.sirius.patrius.math.analysis.function.Exp;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.integration.TrapezoidIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.UnivariateIntegrator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Tests for the {@link AbstractVector3DFunction} class.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: SpinFunctionTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 1.3
 * 
 */
public class SpinFunctionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Validation of the spin function
         * 
         * @featureDescription test the spin function features
         * 
         * @coveredRequirements DV-ATT_160, DV-ATT_170, DV-ATT_180, DV-ATT_190, DV-ATT_200, DV-ATT_210
         */
        SPIN_FUNCTION_VALIDATION;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPIN_FUNCTION_VALIDATION}
     * 
     * @testedMethod {@link AbstractVector3DFunction#AbstractVector3DFunction(AbsoluteDate)}
     * @testedMethod {@link AbstractVector3DFunction#AbstractVector3DFunction(AbsoluteDate, UnivariateVectorFunctionDifferentiator)}
     * @testedMethod {@link AbstractVector3DFunction#AbstractVector3DFunction(AbsoluteDate, UnivariateVectorFunctionDifferentiator, UnivariateIntegrator)}
     * @testedMethod {@link AbstractVector3DFunction#getZeroDate()}
     * @testedMethod {@link AbstractVector3DFunction#value(double)}
     * @testedMethod {@link AbstractVector3DFunction#getVector3D(AbsoluteDate)}
     * @testedMethod {@link AbstractVector3DFunction#nthDerivative(int)}
     * @testedMethod {@link AbstractVector3DFunction#integral(double, double)}
     * 
     * @description test the abstract spin function methods using a function
     * 
     * @input a spin function implementing AbstractVector3DFunction
     * 
     * @output the output of the methods
     * 
     * @testPassCriteria the output of the methods are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAllMethods() throws PatriusException {
        final AbsoluteDate zero = AbsoluteDate.J2000_EPOCH;
        final SpinTestFunction function = new SpinTestFunction(zero);
        final double dt = 1.5;

        // test getSpin(AbsoluteDate) method:
        final Vector3D result1 = function.getVector3D(AbsoluteDate.J2000_EPOCH.shiftedBy(dt));
        Assert.assertEquals(2. * dt, result1.getX(), 0.0);
        Assert.assertEquals(dt - 1, result1.getY(), 0.0);
        Assert.assertEquals(dt * dt, result1.getZ(), 0.0);

        // test getZeroDate() method:
        Assert.assertEquals(zero, function.getZeroDate());

        // test the value(double) method:
        final double x = 9.5681;
        final Vector3D expected = new Vector3D(2 * x, x - 1, x * x);
        Assert.assertEquals(expected.getX(), function.value(x)[0], 0.0);
        Assert.assertEquals(expected.getY(), function.value(x)[1], 0.0);
        Assert.assertEquals(expected.getZ(), function.value(x)[2], 0.0);

        // test the getDifferentiator() method:
        Assert.assertEquals(FiniteDifferencesDifferentiator.class, function.getDifferentiator().getClass());

        // test the getIntegrator() method:
        Assert.assertEquals(TrapezoidIntegrator.class, function.getIntegrator().getClass());

        // test the nthDerivative() method:
        final AbsoluteDate dateTest = zero.shiftedBy(4.5);
        final Vector3DFunction actual1stDer = function.nthDerivative(1);
        final Vector3DFunction actual2ndDer = function.nthDerivative(2);
        final Vector3D expected1stDer = new Vector3D(2., 1., 2. * 4.5);
        final Vector3D expected2ndDer = new Vector3D(0., 0., 2.);
        // we use a numerical method to compute the derivative, the precision is not high:
        Assert.assertEquals(expected1stDer.getX(), actual1stDer.getVector3D(dateTest).getX(), 1E-8);
        Assert.assertEquals(expected1stDer.getY(), actual1stDer.getVector3D(dateTest).getY(), 1E-8);
        Assert.assertEquals(expected1stDer.getZ(), actual1stDer.getVector3D(dateTest).getZ(), 1E-8);
        Assert.assertEquals(expected2ndDer.getX(), actual2ndDer.getVector3D(dateTest).getX(), 1E-8);
        Assert.assertEquals(expected2ndDer.getY(), actual2ndDer.getVector3D(dateTest).getY(), 1E-8);
        Assert.assertEquals(expected2ndDer.getZ(), actual2ndDer.getVector3D(dateTest).getZ(), 1E-8);

        // test the integral(double,double) method:
        final Vector3D actualIntegral = function.integral(0, 5.);
        final Vector3D expectedIntegral = new Vector3D(5. * 5., 5. * 5. / 2. - 5., 5. * 5. * 5. / 3.);
        // we use a numerical method to compute the integral, the precision is not high:
        Assert.assertEquals(expectedIntegral.getX(), actualIntegral.getX(), 1E-6);
        Assert.assertEquals(expectedIntegral.getY(), actualIntegral.getY(), 1E-6);
        Assert.assertEquals(expectedIntegral.getZ(), actualIntegral.getZ(), 1E-5);

        // cover the AbstractVector3DFunction(AbsoluteDate, UnivariateVectorFunctionDifferentiator) and
        // the AbstractVector3DFunction(AbsoluteDate, UnivariateVectorFunctionDifferentiator, UnivariateIntegrator)
        // constructors:
        final UnivariateVectorFunctionDifferentiator diff = new FiniteDifferencesDifferentiator(4, 0.001);
        final UnivariateIntegrator integ = new TrapezoidIntegrator();
        final AbstractVector3DFunction function2 = new AbstractVector3DFunction(zero, diff){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) {
                return null;
            }

        };
        final AbstractVector3DFunction function3 = new AbstractVector3DFunction(zero, diff, integ){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) {
                return null;
            }
        };
        Assert.assertEquals(diff, function2.getDifferentiator());
        Assert.assertEquals(diff, function3.getDifferentiator());
        Assert.assertEquals(integ, function3.getIntegrator());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPIN_FUNCTION_VALIDATION}
     * 
     * @testedMethod {@link AbstractVector3DFunction#nthDerivative(int)}
     * 
     * @description test the n-th derivative computation method for an analytical function
     * 
     * @input a spin analytical function implementing AbstractVector3DFunction
     * 
     * @output the output of the nthDerivative method
     * 
     * @testPassCriteria the output of the nthDerivative method are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testNthDerivative() throws PatriusException {
        final AbsoluteDate zero = AbsoluteDate.J2000_EPOCH;
        final AnalyticalSpinTestFunction function = new AnalyticalSpinTestFunction(zero);
        // test the nthDerivative() method:
        final double value = 1.5;
        final AbsoluteDate dateTest = zero.shiftedBy(value);
        final Vector3DFunction actual1stDer = function.nthDerivative(1);
        final Vector3DFunction actual2ndDer = function.nthDerivative(2);
        final Vector3DFunction actual5ndDer = function.nthDerivative(5);
        final Vector3D expected1stDer = new Vector3D(MathLib.cos(value), -MathLib.sin(value), MathLib.exp(value));
        final Vector3D expected2ndDer = new Vector3D(-MathLib.sin(value), -MathLib.cos(value), MathLib.exp(value));
        final Vector3D expected5ndDer = new Vector3D(MathLib.cos(value), -MathLib.sin(value), MathLib.exp(value));

        // we use a exact method to compute the derivative, the epsilon is zero:
        Assert.assertEquals(expected1stDer.getX(), actual1stDer.getVector3D(dateTest).getX(), 0.0);
        Assert.assertEquals(expected1stDer.getY(), actual1stDer.getVector3D(dateTest).getY(), 0.0);
        Assert.assertEquals(expected1stDer.getZ(), actual1stDer.getVector3D(dateTest).getZ(), 0.0);
        Assert.assertEquals(expected2ndDer.getX(), actual2ndDer.getVector3D(dateTest).getX(), 0.0);
        Assert.assertEquals(expected2ndDer.getY(), actual2ndDer.getVector3D(dateTest).getY(), 0.0);
        Assert.assertEquals(expected2ndDer.getZ(), actual2ndDer.getVector3D(dateTest).getZ(), 0.0);
        Assert.assertEquals(expected5ndDer.getX(), actual5ndDer.getVector3D(dateTest).getX(), 0.0);
        Assert.assertEquals(expected5ndDer.getY(), actual5ndDer.getVector3D(dateTest).getY(), 0.0);
        Assert.assertEquals(expected5ndDer.getZ(), actual5ndDer.getVector3D(dateTest).getZ(), 0.0);
    }

    /**
     * Time spin function used for test purposes.
     */
    private class SpinTestFunction extends AbstractVector3DFunction {

        /**
         * Default constructor.
         * 
         * @param zeroDate
         *        zero
         */
        public SpinTestFunction(final AbsoluteDate zeroDate) {
            super(zeroDate);
        }

        @Override
        public Vector3D getVector3D(final AbsoluteDate date) {
            final AbsoluteDate zero = this.getZeroDate();
            final double x = date.durationFrom(zero);
            return new Vector3D(2.0 * x, x - 1.0, x * x);
        }
    }

    /**
     * Time spin analytical function used for test purposes.
     */
    private class AnalyticalSpinTestFunction extends AbstractVector3DFunction {

        /** x component. */
        private final UnivariateDifferentiableFunction vx = new Sin();
        /** y component. */
        private final UnivariateDifferentiableFunction vy = new Cos();
        /** z component. */
        private final UnivariateDifferentiableFunction vz = new Exp();

        /**
         * Default constructor.
         * 
         * @param zeroDate
         *        zero
         */
        public AnalyticalSpinTestFunction(final AbsoluteDate zeroDate) {
            super(zeroDate);
        }

        @Override
        public Vector3D getVector3D(final AbsoluteDate date) {
            final AbsoluteDate zero = this.getZeroDate();
            final double t = date.durationFrom(zero);
            return new Vector3D(this.vx.value(t), this.vy.value(t), this.vz.value(t));
        }

        @Override
        public Vector3DFunction nthDerivative(final int order) {
            // re-implement the nth-derivative method to get the exact derivatives of the analytical functions:
            return new AbstractVector3DFunction(this.getZeroDate()){
                @Override
                public Vector3D getVector3D(final AbsoluteDate date) {
                    final double dt = date.durationFrom(this.getZeroDate());
                    // Compute the nth derivative using the DerivativeStructure:
                    final DerivativeStructure s = new DerivativeStructure(1, order, 0, dt);
                    // Create the new Vector3D from the derivatives value:
                    return new Vector3D(AnalyticalSpinTestFunction.this.vx.value(s).getPartialDerivative(order),
                        AnalyticalSpinTestFunction.this.vy.value(s).getPartialDerivative(order),
                        AnalyticalSpinTestFunction.this.vz.value(s).getPartialDerivative(order));
                }
            };
        }
    }
}
