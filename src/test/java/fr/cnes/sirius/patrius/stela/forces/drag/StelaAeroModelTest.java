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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Changed ConstanSolarActivity class
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:399:09/03/2015:remove C_D parameter
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:605:07/12/2016:gathered Meeus models
 * VERSION::FA:704:07/12/2016: write DM 605 in "HISTORY"
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.drag;

import java.io.InputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.stela.forces.atmospheres.MSIS00Adapter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for {@link StelaAeroModel}
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaAeroModelTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Stela atmospheric drag aero model
         * 
         * @featureDescription Computation of atmospheric drag perturbations, and partial derivatives.
         * 
         * @coveredRequirements NA
         */
        STELA_ATMOSPHERIC_DRAG_MODEL
    }

    /**
     * Doubles comparison epsilon
     */
    private static final double EPS = 1e-14;

    /**
     * The date.
     */
    private static AbsoluteDate date;

    /**
     * MOD frame
     */
    private static Frame frame;
    /**
     * The &mu;.
     */
    private static double mu;
    /**
     * The spacecraft state.
     */
    private static SpacecraftState state;

    /**
     * The Stela Cd value.
     */
    private static StelaCd cd;
    /**
     * The Stela aero model.
     */
    private static StelaAeroModel sp;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link StelaAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description tests the computation of atmospheric drag acceleration
     * 
     * @input a StelaEquinoctialOrbit and a spherical spacecraft
     * 
     * @output atmospheric drag acceleration
     * 
     * @testPassCriteria results close from Stela v2.6 (relative tolerance: 1E-14)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputeDragAcceleration() throws PatriusException {

        // TT - UT1: 66.184s
        date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22370 * 86400 + 5357.372296380345 + 34);

        // Orbit initialization :
        final double a = 2.438081044132939E+7;
        final double LambdaEq = 0.8912662641557962;
        final double eX = 0.36558135959697824;
        final double eY = 0.6310194520173773;
        final double iX = 0.09051408946022774;
        final double iY = 0.05232313195160007;
        mu = 398600441449820.000;
        final StelaEquinoctialOrbit pv8 = new StelaEquinoctialOrbit(a, eX, eY, iX, iY, LambdaEq,
            FramesFactory.getCIRF(), date, mu);
        final double mass = 114.907;
        sp = new StelaAeroModel(mass, new StelaCd(2.2), 1.07);
        /** Default attitude provider. */
        final AttitudeProvider DEFAULT_LAW = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        state = new SpacecraftState(pv8, DEFAULT_LAW.getAttitude(pv8, date, FramesFactory.getCIRF()));
        final double density = 1.218243023748619E-16;

        // Actual results:
        final double[] result = sp.dragAcceleration(state, density, null).toArray();

        // Comparison with expected results:
        final double[] expected = { 2.991580342763402E-11, -7.231246781249255E-11, -1.7591964453276326E-11 };
        for (int i = 0; i < 3; i++) {
            if (MathLib.abs(expected[i]) < Precision.EPSILON) {
                Assert.assertEquals(expected[i], result[i], EPS);
            } else {
                Assert.assertEquals(0.0, (expected[i] - result[i]) / expected[i], EPS);
            }
        }

        Assert.assertEquals(0, sp.getJacobianParameters().size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link StelaAeroModel#StelaAeroModel(double, StelaCd, double, Atmosphere, double)}
     * @testedMethod {@link StelaAeroModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D)}
     * 
     * @description tests the computation of partial derivatives of drag acceleration with respect to position and
     *              velocity
     *              using the full finite differences method.
     * 
     * @input a StelaEquinoctialOrbit and a spherical spacecraft
     * 
     * @output partial derivatives of drag acceleration with respect to position and velocity
     * 
     * @testPassCriteria references from Stela v2.6 (relative tolerance: 1E-6 on position, 1E-13 on velocity)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputeDDragAccDState() throws PatriusException {

        // Initialization
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];

        // earth - stela values
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;

        // Celestial bodies:
        final CelestialBody sun = new MeeusSun(MODEL.STELA);

        // Constant solar activity:
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);

        // Atmosphere:
        final Atmosphere atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f, sun);

        sp = new StelaAeroModel(114.907, cd, 1.07, atmosphere, 50);
        date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22369.928419882603 * 86400 + 35);
        mu = 398600441449820.000;
        state = new SpacecraftState(new StelaEquinoctialOrbit(2.4380867358428184E7, 0.36558117618098135,
            0.6310190803726696,
            0.09051402937824331, 0.05232309052333157, 0.8912661042616996, FramesFactory.getCIRF(), date, mu));

        // Actual results:
        final Vector3D vect0 = new Vector3D(2.926536218293603E-11, -7.074008271845892E-11, -1.7209439873177355E-11);
        final double density0 = 1.191758242605813E-16;
        sp.addDDragAccDState(state, dAccdPos, dAccdVel, density0, vect0, null, true, true);

        // Expected result
        final double[][] expectedPos = {
            { 1.393399688073637E-16, 4.82720547071102E-18, 1.693957101687806E-18 },
            { -1.8266780779716133E-17, -3.333374472018368E-20, -2.570298968171376E-19 },
            { -1.845499314840159E-17, -5.434255935225275E-19, -6.036121139914157E-19 },
        };
        final double[][] expectedVel = {
            { 7.279555338502928E-15, 7.386489236157807E-16, -3.5887759000454154E-17 },
            { -1.76566545958288E-14, -1.0650800629215988E-16, 2.845492335924544E-17 },
            { -4.283857252392849E-15, -3.768481993798372E-17, -8.259205082455991E-16 },
        };

        // Check result
        for (int i = 0; i < expectedPos.length; i++) {
            for (int j = 0; j < expectedPos[i].length; j++) {
                Assert.assertEquals(0, (dAccdPos[i][j] - expectedPos[i][j]) / expectedPos[i][j], 1E-6);
                Assert.assertEquals(0, (dAccdVel[i][j] - expectedVel[i][j]) / expectedVel[i][j], 1E-13);
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link StelaAeroModel#StelaAeroModel(double, StelaCd, double, Atmosphere, double, GeodPosition)}
     * @testedMethod {@link StelaAeroModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D)}
     * 
     * @description tests the computation of partial derivatives of drag acceleration with respect to position and
     *              velocity
     *              using the altitude finite differences method.
     * 
     * @input a StelaEquinoctialOrbit and a spherical spacecraft
     * 
     * @output partial derivatives of drag acceleration with respect to position and velocity
     * 
     * @testPassCriteria references from Stela v2.6 (relative tolerance: 1E-6 on position, 1E-13 on velocity)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputeDDragAccDState2() throws PatriusException {

        // Initialization
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];

        // earth - stela values
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;

        // Celestial bodies:
        final CelestialBody sun = new MeeusSun(MODEL.STELA);

        // Constant solar activity:
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);

        // Atmosphere:
        final Atmosphere atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f, sun);

        final GeodPosition geodPosition = new GeodPosition(ae, 1 / f);
        sp = new StelaAeroModel(114.907, cd, 1.07, atmosphere, -10, geodPosition);
        date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22369.928419882603 * 86400 + 35);
        mu = 398600441449820.000;
        state = new SpacecraftState(new StelaEquinoctialOrbit(2.4380867358428184E7, 0.36558117618098135,
            0.6310190803726696,
            0.09051402937824331, 0.05232309052333157, 0.8912661042616996, FramesFactory.getCIRF(), date, mu));

        // Actual results:
        final Vector3D vect0 = new Vector3D(2.926536218293603E-11, -7.074008271845892E-11, -1.7209439873177355E-11);
        final double density0 = 1.191758242605813E-16;
        sp.addDDragAccDState(state, dAccdPos, dAccdVel, density0, vect0, null, true, true);

        // Expected result
        final double[][] expectedPos = {
            // {-7.792238015471068E-12, -2.846544992272027E-13, -1.0452561200619168E-13},
            // {8.693472690378032E-13, 3.175831159254878E-14, 1.1661451998223225E-14},
            // {0.0, 1.3074426239136814E-19, -3.560559924918857E-19},
            };
        final double[][] expectedVel = {
            { 7.279555338502928E-15, 7.386489236157807E-16, -3.5887759000454154E-17 },
            { -1.76566545958288E-14, -1.0650800629215988E-16, 2.845492335924544E-17 },
            { -4.283857252392849E-15, -3.768481993798372E-17, -8.259205082455991E-16 },
        };

        // Check result
        for (int i = 0; i < expectedPos.length; i++) {
            for (int j = 0; j < expectedPos[i].length; j++) {
                // if (expectedPos[i][j] == 0) {
                // Assert.assertEquals(0, (dAccdPos[i][j] - expectedPos[i][j]), 1E-6);
                // } else {
                // Assert.assertEquals(0, (dAccdPos[i][j] - expectedPos[i][j]) / expectedPos[i][j], 1E-6);
                // }
                Assert.assertEquals(0, (dAccdVel[i][j] - expectedVel[i][j]) / expectedVel[i][j], 1E-13);
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG_MODEL}
     * 
     * @testedMethod {@link StelaAeroModel#StelaAeroModel(double, StelaCd, double, Atmosphere, double, GeodPosition)}
     * @testedMethod {@link StelaAeroModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D)}
     * 
     * @description coverage test.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */

    @Test
    public void coverageTest() throws PatriusException {

        final DateComponents datec = new DateComponents(2003, 11, 15);
        final TimeComponents timec = new TimeComponents(18, 37, 0.0);
        date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());

        // earth - stela values
        final double f = Double.NaN;
        final double ae = Double.NaN;

        // Celestial bodies:
        final CelestialBody sun = new MeeusSun(MODEL.STELA);

        // Constant solar activity:
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);

        // Atmosphere:
        final Atmosphere atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f, sun);

        try {
            atmosphere.getDensity(date, new Vector3D(0., 10., 0.), FramesFactory.getITRF());
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        sp = new StelaAeroModel(1000, cd, 10);
        try {
            sp.addDDragAccDParam(null, new Parameter("coucou", 0.), 0.1, null, null);
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MSIS2000_ADAPTER}
     * 
     * @testedMethod {@link MSIS00Adapter#getSpeedOfSound(date, position, frame)}
     * 
     * @description the test computes the speed of sound for given date, positions and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output speed of sound
     * 
     * @testPassCriteria if speed of sound is as expected
     * 
     * @referenceVersion 2.4
     * 
     * @nonRegressionVersion 2.4
     * 
     */
    @Test
    public void testGetSpeedOfSound() throws PatriusException {

        // Initialization
        this.testSetup();
        final CelestialBody sun = new MeeusSun(MODEL.STELA);
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);
        final double f = 0.29825765000000E+03;
        final double ae = Constants.CNES_STELA_AE;
        final Atmosphere atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f, sun);
        final Atmosphere refAtmosphere = new MSISE2000(new ClassicalMSISE2000SolarData(solarActivity),
            new OneAxisEllipsoid(ae, 1 / f, FramesFactory.getTIRF()), sun);
        final Vector3D position = new Vector3D(6478000., 0., 0.);

        // Compute actual and reference
        final double speedOfSound = atmosphere.getSpeedOfSound(AbsoluteDate.J2000_EPOCH, position,
            FramesFactory.getMOD(false));
        final double ref = refAtmosphere.getSpeedOfSound(AbsoluteDate.J2000_EPOCH, position,
            FramesFactory.getMOD(false));

        // Check
        Assert.assertEquals((speedOfSound - ref) / ref, 0., 5.4E-7);
    }

    /**
     * Test that aims at showing the "cache" process of class {@link #MSIS00Adapter} :
     * density is recomputed the method {@link MSIS00Adapter#getDensity(AbsoluteDate, Vector3D, Frame)} is successively
     * called with different parameters.
     * 
     * @throws PatriusException
     *         if MSIS00Adapter altitude range is not in range 0 to 1000 km
     * @referenceVersion 3.2
     */
    @Test
    public void testRecomputed() throws PatriusException {

        // Initialization
        this.testSetup();
        // Thu Mar 31 22:16:55 GMT 2011
        date = new AbsoluteDate(2011, 03, 31, 22, 16, 55.4778569, TimeScalesFactory.getUTC());
        frame = FramesFactory.getITRF();
        final CelestialBody sun = new MeeusSun(MODEL.STELA);
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);
        final double f = 0.29825765000000E+03;
        final double ae = Constants.CNES_STELA_AE;
        final MSIS00Adapter atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f,
            sun);

        // Altitude
        final double alt1 = this.computeZ(20E3);
        final double alt2 = this.computeZ(40E3);

        // Geodetic point
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, 1 / f, FramesFactory.getGCRF());
        final EllipsoidPoint ep1 = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), 0, 0, alt1, "");
        final EllipsoidPoint ep2 = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), 0, 0, alt2, "");

        // Positions
        final Vector3D pos1 = ep1.getPosition();
        final Vector3D pos2 = ep2.getPosition();

        // GCRF
        final Frame gcrf = FramesFactory.getGCRF();

        // Check that the density depends on the frame used
        // so it is recomputed and leads to different results

        // When calling the following getDensity methods, the parameters in cache are :
        // (date, pos1, frame)
        final double density1_ITRF = atmosphere.getDensity(date, pos1, frame);

        // Recomputation occur here : parameters in cache are now :
        // (date1, pos1, gcrf), so results are different
        final double density1_GCRF = atmosphere.getDensity(date, pos1, gcrf);
        Assert.assertFalse(density1_GCRF == density1_ITRF);

        // Check also that values are recomputed if the position changes

        // (date1, pos2, frame) are now in cache
        final double density2 = atmosphere.getDensity(date, pos2, frame);

        // Values are updated so different from previous
        Assert.assertFalse(density1_ITRF == density2);

        // Finally, check that changing the dates leads to recomputation in GCRF
        // Same idea about parameters in cache
        final double density_otherDate = atmosphere.getDensity(date.shiftedBy(3600), pos1, gcrf);

        Assert.assertFalse(density1_GCRF == density_otherDate);

    }

    /**
     * Geodetic altitude from geopotential altitude
     * 
     * @param h
     *        Geopotential altitude
     * @return altitude
     */
    public double computeZ(final double h) {
        final double AE = 6356766;
        return AE * h / (AE - h);

    }

    /**
     * Set up method.
     * 
     * @throws PatriusException
     */
    @Before
    public void testSetup() {

        // Next line clears data set by other tests,
        // are overriden later
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Cd value:
        cd = new StelaCd(2.2);
        // UTC-TAI leap seconds:
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) {
                // nothing to do
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<>();
                for (int i = 1969; i < 2010; i++) {
                    // constant value:
                    map.put(new DateComponents(i, 11, 13), 35);
                }
                return map;
            }

            @Override
            public String getSupportedNames() {
                return "No name";
            }
        });
    }
}
