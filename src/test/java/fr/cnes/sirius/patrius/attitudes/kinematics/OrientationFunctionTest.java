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
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.kinematics;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.analysis.function.Cos;
import fr.cnes.sirius.patrius.math.analysis.function.Power;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.Tan;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Tests for the {@link AbstractOrientationFunction} class.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: OrientationFunctionTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 1.3
 * 
 */
public class OrientationFunctionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Validation of the orientation function
         * 
         * @featureDescription test the orientation function features
         * 
         * @coveredRequirements DV-ATT_10, DV-ATT_20, DV-ATT_120, DV-ATT_130, DV-ATT_170
         */
        ORIENTATION_FUNCTION_VALIDATION;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ORIENTATION_FUNCTION_VALIDATION}
     * 
     * @testedMethod {@link AbstractOrientationFunction#getOrientation(AbsoluteDate)}
     * @testedMethod {@link AbstractOrientationFunction#getZeroDate()}
     * @testedMethod {@link AbstractOrientationFunction#getDifferentiator()}
     * @testedMethod {@link AbstractOrientationFunction#value(double)}
     * @testedMethod {@link AbstractOrientationFunction#estimateRateFunction(double)}
     * @testedMethod {@link AbstractOrientationFunction#estimateRate(AbsoluteDate, double)}
     * @testedMethod {@link AbstractOrientationFunction#computeSpinFunction()}
     * @testedMethod {@link AbstractOrientationFunction#computeSpin(AbsoluteDate)}
     * @testedMethod {@link AbstractOrientationFunction#derivative()}
     * 
     * @description test the abstract orientation function methods using a function
     * 
     * @input an orientation function implementing AbstractRotationFunction
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
        final OrientationTestFunction function = new OrientationTestFunction(zero);
        final double dt = FastMath.PI / 7;

        // test getOrientation(AbsoluteDate) method:
        final Rotation result1 = function.getOrientation(AbsoluteDate.J2000_EPOCH.shiftedBy(dt));
        Assert.assertEquals(MathLib.cos(dt), result1.getQi()[0], 0.0);
        Assert.assertEquals(MathLib.sin(dt), result1.getQi()[1], 0.0);
        Assert.assertEquals(MathLib.tan(dt), result1.getQi()[2], 0.0);
        Assert.assertEquals(MathLib.pow(dt, 0.5), result1.getQi()[3], 0.0);

        // test getZeroDate() method:
        Assert.assertEquals(zero, function.getZeroDate());

        // test the value(double) method:
        final Quaternion expected = new Quaternion(MathLib.cos(13.89), MathLib.sin(13.89), MathLib.tan(13.89),
            MathLib.pow(13.89, 0.5));
        Assert.assertEquals(expected.getQ0(), function.value(13.89)[0], 0.0);
        Assert.assertEquals(expected.getQ1(), function.value(13.89)[1], 0.0);
        Assert.assertEquals(expected.getQ2(), function.value(13.89)[2], 0.0);
        Assert.assertEquals(expected.getQ3(), function.value(13.89)[3], 0.0);

        // test the getDifferentiator() method:
        Assert.assertEquals(FiniteDifferencesDifferentiator.class, function.getDifferentiator().getClass());

        // test the estimateRateFunction(double) method:
        final AbsoluteDate testDate = AbsoluteDate.J2000_EPOCH.shiftedBy(86400);
        final Vector3DFunction result2 = function.estimateRateFunction(0.002, AbsoluteDateInterval.INFINITY);
        Vector3D actualSpin = result2.getVector3D(testDate);
        // we re-implement the method:
        final Rotation start = function.getOrientation(testDate.shiftedBy(-0.001));
        final Rotation end = function.getOrientation(testDate.shiftedBy(+0.001));
        final Vector3D expectedSpin = KinematicsToolkit.estimateSpin(start, end, 0.002);
        Assert.assertEquals(expectedSpin, actualSpin);

        // test the estimateRate(AbsoluteDate) method:
        actualSpin = function.estimateRate(testDate, 0.002, AbsoluteDateInterval.INFINITY);
        Assert.assertEquals(expectedSpin, actualSpin);

        // test the derivative(double) method:
        Assert.assertEquals(-MathLib.sin(0.235), function.derivative().value(0.235)[0], 1E-10);
        Assert.assertEquals(MathLib.cos(0.235), function.derivative().value(0.235)[1], 1E-10);
        Assert.assertEquals((1. + MathLib.tan(0.235) * MathLib.tan(0.235)), function.derivative().value(0.235)[2],
            1E-10);
        Assert.assertEquals(0.5 * MathLib.pow(0.235, 0.5 - 1), function.derivative().value(0.235)[3], 1E-10);
    }

    /**
     * Time orientation function used for test purposes.
     */
    private class OrientationTestFunction extends AbstractOrientationFunction {

        /** q0 quaternion component. */
        private final UnivariateFunction q0 = new Cos();
        /** q1 quaternion component. */
        private final UnivariateFunction q1 = new Sin();
        /** q2 quaternion component. */
        private final UnivariateFunction q2 = new Tan();
        /** q3 quaternion component. */
        private final UnivariateFunction q3 = new Power(0.5);

        /**
         * Default constructor.
         * 
         * @param zeroDate
         */
        public OrientationTestFunction(final AbsoluteDate zeroDate) {
            super(zeroDate);
        }

        @Override
        public Rotation getOrientation(final AbsoluteDate date) {
            final AbsoluteDate zero = this.getZeroDate();
            final double dt = date.durationFrom(zero);
            return new Rotation(false, this.q0.value(dt), this.q1.value(dt), this.q2.value(dt), this.q3.value(dt));
        }
    }
}
