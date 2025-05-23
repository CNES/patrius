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
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
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
 * VERSION::DM:523:08/02/2016: add solid tides effects in STELA PATRIUS
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyIAUOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link SolidTidesAcc}.
 * 
 * @author Emmanuel Bignon
 * @version $Id$
 * @since 3.2
 */
public class SolidTidesAccTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle STELA solid tides contribution test
         * 
         * @featureDescription test solid tides contribution
         * 
         * @coveredRequirements
         */
        STELA_SOLID_TIDES_CONTRIBUTION
    }

    /** Expected result Sun tides. */
    final double[] expected_Sun_Tides = new double[] { -7.760106847936439E-20, -6.316101811430334E-12,
        -1.0058478763403014E-12, -3.352905274989754E-13, 3.938115930917572E-13, 1.8989501990592597E-13 };
    /** Expected result Moon tides. */
    final double[] expected_Moon_tides = new double[] { -1.6456907347794966E-19, -7.576455609892355E-12,
        -1.2537119524863565E-12, -4.2647947903666463E-13, 1.553638267121558E-12, 4.5263559313608947E-13 };

    /** Relative tolerance. */
    private final double tol_rel = 1e-13;
    /** Absolute tolerance (semi-major axis only). */
    private final double tol_abs = 1e-19;

    /** Sun. */
    private CelestialPoint sun;
    /** Moon. */
    private CelestialPoint moon;
    /** Orbit. */
    private StelaEquinoctialOrbit stelaOrbit;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SolidTidesAccTest.class.getSimpleName(), "STELA solid tides force");
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_SOLID_TIDES_CONTRIBUTION}
     * @testedMethod {@link SolidTidesAcc#computePerturbation(StelaEquinoctialOrbit)}
     * @description tests computation of solid tides perturbation (Sun + Moon contribution)
     * @input orbit
     * @output solid tides perturbation
     * @testPassCriteria perturbation is the same as STELA-LOS 3.0 reference at 1E-13 (relative tol).
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testTotalContribution() throws PatriusException {
        Report.printMethodHeader("testTotalContribution", "Perturbation computation (Sun + Moon)", "STELA 3.0", 1E-13,
            ComparisonType.RELATIVE);
        final SolidTidesAcc tideForces = new SolidTidesAcc(this.sun, this.moon);
        this.runTestWithGivenEntries(this.stelaOrbit, tideForces,
            this.vectAdd(this.expected_Sun_Tides, this.expected_Moon_tides));
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_SOLID_TIDES_CONTRIBUTION}
     * @testedMethod {@link SolidTidesAcc#computePerturbation(StelaEquinoctialOrbit)}
     * @description tests computation of solid tides perturbation (Sun contribution only)
     * @input orbit
     * @output solid tides perturbation
     * @testPassCriteria perturbation is the same as STELA-LOS 3.0 reference at 1E-13 (relative tol).
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testSunOnly() throws PatriusException {
        Report.printMethodHeader("testSunOnly", "Perturbation computation (Sun)", "STELA 3.0", 1E-13,
            ComparisonType.RELATIVE);
        final SolidTidesAcc tideForces = new SolidTidesAcc(true, false, this.sun, this.moon);
        Assert.assertEquals(tideForces.getSun(), this.sun);
        this.runTestWithGivenEntries(this.stelaOrbit, tideForces, this.expected_Sun_Tides);
    }

    /**
     * @throws PatriusException
     *         thrown if computation fails
     * @testType UT
     * @testedFeature {@link features#STELA_SOLID_TIDES_CONTRIBUTION}
     * @testedMethod {@link SolidTidesAcc#computePerturbation(StelaEquinoctialOrbit)}
     * @description tests computation of solid tides perturbation (Moon contribution only)
     * @input orbit
     * @output solid tides perturbation
     * @testPassCriteria perturbation is the same as STELA-LOS 3.0 reference at 1E-13 (relative tol).
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMoonOnly() throws PatriusException {
        Report.printMethodHeader("testMoonOnly", "Perturbation computation (Moon)", "STELA 3.0", 1E-13,
            ComparisonType.RELATIVE);
        final SolidTidesAcc tideForces = new SolidTidesAcc(false, true, this.sun, this.moon);
        Assert.assertEquals(tideForces.getMoon(), this.moon);
        this.runTestWithGivenEntries(this.stelaOrbit, tideForces, this.expected_Moon_tides);
    }

    /**
     * Perform generic test.
     * 
     * @param orbit
     *        orbit
     * @param tideForces
     *        tide force
     * @param expected
     *        expected result
     */
    private void runTestWithGivenEntries(final StelaEquinoctialOrbit orbit, final SolidTidesAcc tideForces,
                                         final double[] expected) throws PatriusException {
        // Compute perturbation
        final double[] perts = tideForces.computePerturbation(orbit);
        // Compare results
        Assert.assertEquals(0, MathLib.abs(expected[0] - perts[0]), this.tol_abs);
        Assert.assertEquals(0, MathLib.abs((expected[1] - perts[1]) / expected[1]), this.tol_rel);
        Assert.assertEquals(0, MathLib.abs((expected[2] - perts[2]) / expected[2]), this.tol_rel);
        Assert.assertEquals(0, MathLib.abs((expected[3] - perts[3]) / expected[3]), this.tol_rel);
        Assert.assertEquals(0, MathLib.abs((expected[4] - perts[4]) / expected[4]), this.tol_rel);
        Assert.assertEquals(0, MathLib.abs((expected[5] - perts[5]) / expected[5]), this.tol_rel);

        // Other checks: short periods and partial derivatives
        final double[] sp = tideForces.computeShortPeriods(orbit, null);
        final double[][] derPar = tideForces.computePartialDerivatives(orbit);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(0., sp[i], 0.);
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(0., derPar[i][j], 0.);
            }
        }

        Report.printToReport("Perturbation", expected, perts);
    }

    /**
     * Set up.
     */
    @Before
    public void setUp() {
        // Values
        this.sun = new CelestialBody(){

            /** Serializable UID. */
            private static final long serialVersionUID = -3130725311101633808L;

            @Override
            public CelestialBodyEphemeris getEphemeris() {
                return null;
            }

            @Override
            public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(new double[] {
                    5.177E+10, 1.307E+11, 5.676E+10 }), Vector3D.ZERO);
            }

            @Override
            public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
                return null;
            }

            @Override
            public String getName() {
                return "sun";
            }

            @Override
            public double getGM() {
                return 1.32712440018E+20;
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
            public CelestialBodyFrame getEclipticJ2000() {
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
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyIAUOrientation getOrientation() {
                return null;
            }

            @Override
            public void setOrientation(final CelestialBodyOrientation celestialBodyOrientationIn) {
            }

            @Override
            public void setGM(final double gmIn) {
            }
        };

        this.moon = new CelestialBody(){

            /** Serializable UID. */
            private static final long serialVersionUID = -6909894905212108872L;

            @Override
            public CelestialBodyEphemeris getEphemeris() {
                return null;
            }

            @Override
            public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(new double[] {
                    -2.521E+08, -2.831E+08, -9.785E+07 }), Vector3D.ZERO);
            }

            @Override
            public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
                return null;
            }

            @Override
            public String getName() {
                return "moon";
            }

            @Override
            public double getGM() {
                return 4.902777900000E12;
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
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return null;
            }

            @Override
            public CelestialBodyIAUOrientation getOrientation() {
                return null;
            }

            @Override
            public void setOrientation(final CelestialBodyOrientation celestialBodyOrientation) {
            }

            @Override
            public void setGM(final double gmIn) {
            }

            @Override
            public CelestialBodyFrame getEclipticJ2000() throws PatriusException {
                // nothing to do
                return null;
            }
        };

        this.stelaOrbit = new StelaEquinoctialOrbit(new StelaEquinoctialParameters(1.e7, 0.1, -0.3, -0.5, 0.05, 3.,
            Constants.CNES_STELA_MU, false), FramesFactory.getCIRF(), AbsoluteDate.J2000_EPOCH);
    }

    /**
     * Returns a vector corresponding to the addition of the two input vectors.
     * 
     * @param u0
     *        1st vector
     * @param u1
     *        2nd vector
     * @return u0 + u1
     */
    private double[] vectAdd(final double[] u0, final double[] u1) {
        final double[] res = new double[6];
        for (int i = 0; i < 6; i++) {
            res[i] = u0[i] + u1[i];
        }
        return res;
    }
}
