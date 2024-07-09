/**
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
 *
 * 
 * @history 12/02/2013
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for {@link SRPPotential}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaSRPPotentialTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the SRP potential model
         * 
         * @featureDescription Validate the first derivatives
         * 
         * @coveredRequirements ?????
         */
        VALIDATE_STELA_SRP_POTENTIAL_DV,

        /**
         * @featureTitle Validate the SRP potential model
         * 
         * @featureDescription Validate the second derivatives
         * 
         * @coveredRequirements ?????
         */
        VALIDATE_STELA_SRP_POTENTIAL_DV2,
    }

    /** Space object orbital position (type 8). */
    final double[] pv = { 2.422800000000000000E+07, 5.393598442489803801E+00, 4.592777886100000151E-01,
        -5.665453952400000270E-01, 5.490687833999999962E-02, -6.770144794999999327E-02 };
    /** Distance to Sun's center. */
    final double rSun = 1.499176766493516541E+11;
    /** Sun position. */
    final double[] sunPos = { this.rSun * -9.936502885512082939E-01, this.rSun * -1.032392149731359426E-01,
        this.rSun * -4.472995141771566457E-02 };
    /** Mass of the space object (kg). */
    final double mass = 1000;
    /** Reflectivity area (m<sup>2</sup>). */
    final double surface = 10;
    /** Reflectivity coefficient. */
    final double cr = 2;
    /** Stela ref Orbit */
    final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(this.pv[0], this.pv[2], this.pv[3], this.pv[4],
        this.pv[5], this.pv[1],
        FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
    /** threshold */
    final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
    /** Sun */
    final CelestialBody sun = new CelestialBody(){

        private static final long serialVersionUID = -9079436376500784297L;

        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return new PVCoordinates(new Vector3D(StelaSRPPotentialTest.this.sunPos), Vector3D.ZERO);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Frame getInertiallyOrientedFrame() throws PatriusException {
            return null;
        }

        @Override
        public double getGM() {
            return 0;
        }

        @Override
        public Frame getBodyOrientedFrame() throws PatriusException {
            return null;
        }
    };

    // Expected results
    /** Expected first derivatives of the SRP considered as a potential. */
    final double[] expFirstDeriv = { -5.420305085076107187E-08, 0.000000000000000000E+00, -3.266991746588933765E+00,
        -3.304681854998319790E-01, -1.061497996449227849E-01, -8.595556124324318925E-02 };
    /** Expected second derivatives of the SRP considered as a potential. */
    final double[][] expSecDeriv = {
        { 0.000000000000000000E+00, 0.000000000000000000E+00, -1.348436415134940384E-07, -1.363992840927158497E-08,
            -4.381286100582911564E-09, -3.547777829092091085E-09 },
        { 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
            0.000000000000000000E+00, 0.000000000000000000E+00 },
        { -1.348436415134940384E-07, 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
            4.724099758191775689E-02, -6.327827878297568320E-01 },
        { -1.363992840927158497E-08, 0.000000000000000000E+00, 0.000000000000000000E+00, 0.000000000000000000E+00,
            2.256598352404188268E-01, -3.612552849979074887E-01 },
        { -4.381286100582911564E-09, 0.000000000000000000E+00, 4.724099758191775689E-02, 2.256598352404188268E-01,
            -7.906568026471507693E-01, 3.407062030333043889E+00 },
        { -3.547777829092091085E-09, 0.000000000000000000E+00, -6.327827878297568320E-01,
            -3.612552849979074887E-01, 3.407062030333043889E+00, 6.043556775998468744E+00 } };

    /**
     * @throws PatriusException
     *         if PRS potential computation fails
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_STELA_SRP_POTENTIAL_DV}
     * 
     * @testedMethod {@link SRPPotential#computePerturbation(StelaEquinoctialOrbit)}
     * 
     * @description test computation of perturbations (first derivatives)
     * 
     * @input and orbit and the suns position
     * 
     * @output the derivatives
     * 
     * @testPassCriteria the derivatives are the same as the expected ones. The threshold is 1e-14, as per STELA, on a
     *                   relative scale.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testPerturbations() throws PatriusException {

        final SRPPotential pot = new SRPPotential(this.sun, this.mass, this.surface, this.cr);

        this.assertEquals(this.expFirstDeriv, pot.computePerturbation(this.orbit), this.eps);
    }

    /**
     * @throws PatriusException
     *         if PRS potential computation fails
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_STELA_SRP_POTENTIAL_DV2}
     * 
     * @testedMethod {@link SRPPotential#computePartialDerivatives(StelaEquinoctialOrbit)}
     * 
     * @description test computation of perturbations (second derivatives)
     * 
     * @input and orbit and the suns position
     * 
     * @output the derivatives
     * 
     * @testPassCriteria the derivatives are the same as the expected ones. The threshold is 1e-14, as per STELA, on a
     *                   relative scale.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSecondDerivatives() throws PatriusException {

        final SRPPotential pot = new SRPPotential(this.sun, this.mass, this.surface, this.cr);
        this.assertEquals(this.expSecDeriv, pot.computePartialDerivatives(this.orbit), this.eps);

    }

    /**
     * @throws PatriusException
     *         if PRS potential computation fails
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_STELA_SRP_POTENTIAL_DV}
     * 
     * @testedMethod {@link SRPPotential#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description covergae : method not implemented by STELA
     * 
     * @input and orbit and the suns position
     * 
     * @output zero
     * 
     * @testPassCriteria the derivatives are the same as the expected ones. The threshold is 1e-14, as per STELA, on a
     *                   relative scale.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testShortPeriods() throws PatriusException {

        final SRPPotential pot = new SRPPotential(this.sun, this.mass, this.surface, this.cr);
        this.assertEquals(new double[6], pot.computeShortPeriods(this.orbit), this.eps);

    }

    private void assertEquals(final double[][] exp, final double[][] act, final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            this.assertEquals(exp[i], act[i], eps);
        }
    }

    private void assertEquals(final double[] exp, final double[] act, final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            if (MathLib.abs(exp[i]) < Precision.EPSILON) {
                Assert.assertEquals(0, act[i], eps);
            } else {
                Assert.assertEquals(0, (act[i] - exp[i]) / exp[i], eps);
            }
        }
    }
}
