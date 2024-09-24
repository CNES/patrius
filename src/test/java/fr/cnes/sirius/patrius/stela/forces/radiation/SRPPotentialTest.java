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
 * 
 * @history 12/02/2013
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Ajout des courtes periodes dues a la traînee atmospherique et a la pression de radiation solaire dans STELA
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.IAUPole;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
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
public class SRPPotentialTest {

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

        /** Serializable UID. */
        private static final long serialVersionUID = -9079436376500784297L;

        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return new PVCoordinates(new Vector3D(SRPPotentialTest.this.sunPos), Vector3D.ZERO);
        }

        @Override
        public CelestialBodyEphemeris getEphemeris() {
            return null;
        }
        
        @Override
        public void setEphemeris(CelestialBodyEphemeris ephemerisIn) {
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public CelestialBodyFrame getInertialFrame(IAUPoleModelType iauPole) throws PatriusException {
            return null;
        }

        @Override
        public double getGM() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
            return null;
        }

        @Override
        public CelestialBodyFrame getICRF() throws PatriusException {
            return null;
        }

        @Override
        public CelestialBodyFrame getEME2000() {
            return null;
        }

		@Override
		public BodyShape getShape() {
			return null;
		}

		@Override
		public void setShape(final BodyShape shapeIn) {
		}

		@Override
        public GravityModel getGravityModel() {
			return null;
		}

		@Override
        public void setGravityModel(final GravityModel gravityModelIn) {
		}

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            return null;
        }

        @Override
        public IAUPole getIAUPole() {
            return null;
        }

        @Override
        public void setIAUPole(final IAUPole iauPoleIn) {   
        }

        @Override
        public void setGM(final double gmIn) {
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
    public void testPreturbations() throws PatriusException {

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
        this.assertEquals(new double[6], pot.computeShortPeriods(this.orbit, null), this.eps);

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
