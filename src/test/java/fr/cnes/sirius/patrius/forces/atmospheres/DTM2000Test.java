/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:156:24/10/2013:Optimized DTM2000 code by moving an instantiation into the constructor
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.SolarInputs97to05;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class DTM2000Test {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(DTM2000Test.class.getSimpleName(), "DTM2000 atmosphereOrekit");
    }

    /** FT 158 : Reduced computation time by a factor of 2 */
    @Test
    public void testPerformance() throws PatriusException {

        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        final DTM2000 atm = new DTM2000(in, sun, earth);

        final AbsoluteDate date = new AbsoluteDate(2004, 01, 01, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();
        final Vector3D pos = new KeplerianOrbit(6700000, .001, .1, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU).getPVCoordinates().getPosition();

        System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            atm.getDensity(date, pos, gcrf);
        }
    }

    @Test
    public void testWithOriginalTestsCases() throws PatriusException {

        Report.printMethodHeader("testWithOriginalTestsCases", "Density computation", "Unknown", 1e-14,
            ComparisonType.RELATIVE);

        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        new DTM2000(in, sun, earth);
        double roTestCase;
        double tzTestCase;
        double tinfTestCase;

        // Inputs :
        // alt=800.
        // lat=40.
        // day=185.
        // hl=16.
        // xlon=0.
        // fm(1)=150.
        // f(1) =fm(1)
        // fm(2)=0.
        // f(2)=0.
        // akp(1)=0.
        // akp(2)=0.
        // akp(3)=0.
        // akp(4)=0.

        AtmosphereData data;

        // Outputs :
        roTestCase = 3.804370771270033E-15;
        tzTestCase = 926.223575874829;
        tinfTestCase = 926.223602492388;

        // Computation and results (Results are from Patrius v3.3!)
        data =
            computeData(earth, 185, 800 * 1000, 0, MathLib.toRadians(40), 16 * FastMath.PI / 12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase - data.getDensity()) / roTestCase, 1e-14);
        Assert.assertEquals(0, (tzTestCase - data.getLocalTemperature()) / tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase - data.getExosphericTemperature()) / tinfTestCase, 1e-13);

        Report.printToReport("Density at day 185 (altitude: 800km)", roTestCase, data.getDensity());

        // IDEM., day=275 - Results are from Patrius v3.3!

        roTestCase = 1.0652826866093144E-14;
        tzTestCase = 909.5698991997787;
        tinfTestCase = 909.5699121194999;

        data =
            computeData(earth, 275, 800 * 1000, 0, MathLib.toRadians(40), 16 * FastMath.PI / 12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase - data.getDensity()) / roTestCase, 1e-14);
        Assert.assertEquals(0, (tzTestCase - data.getLocalTemperature()) / tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase - data.getExosphericTemperature()) / tinfTestCase, 1e-13);

        Report.printToReport("Density at day 275 (altitude: 800km)", roTestCase, data.getDensity());

        // IDEM., day=355

        roTestCase = 1.7343324462212e-17 * 1000;
        tzTestCase = 1033.0277846356;
        tinfTestCase = 1033.0282703200;

        data =
            computeData(earth, 355, 800 * 1000, 0, MathLib.toRadians(40), 16 * FastMath.PI / 12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase - data.getDensity()) / roTestCase, 2e-14);
        Assert.assertEquals(0, (tzTestCase - data.getLocalTemperature()) / tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase - data.getExosphericTemperature()) / tinfTestCase, 1e-13);

        Report.printToReport("Density at day 355 (altitude: 800km)", roTestCase, data.getDensity());

        // IDEM., day=85

        roTestCase = 2.9983740796297e-17 * 1000;
        tzTestCase = 1169.5405086196;
        tinfTestCase = 1169.5485768345;

        data = computeData(earth, 85, 800 * 1000, 0, MathLib.toRadians(40), 16 * FastMath.PI / 12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase - data.getDensity()) / roTestCase, 3e-14);
        Assert.assertEquals(0, (tzTestCase - data.getLocalTemperature()) / tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase - data.getExosphericTemperature()) / tinfTestCase, 1e-13);

        Report.printToReport("Density at day 85 (altitude: 800km)", roTestCase, data.getDensity());

        // alt=500.
        // lat=-70. NB: the subroutine requires latitude in rad
        // day=15.
        // hl=16. NB: the subroutine requires local time in rad (0hr=0 rad)
        // xlon=0.
        // fm(1)=70.
        // f(1) =fm(1)
        // fm(2)=0.
        // f(2)=0.
        // akp(1)=0.
        // akp(2)=0.
        // akp(3)=0.
        // akp(4)=0.
        // ro= 1.3150282384722D-16
        // tz= 793.65487014559
        // tinf= 793.65549802348
        // roTestCase = 1.3150282384722E-16;
        // tzTestCase= 793.65487014559;
        // tinfTestCase= 793.65549802348;

        computeData(earth, 15, 500 * 1000, 0, MathLib.toRadians(-70), 16 * FastMath.PI / 12, 70, 70, 0, 0);

        // IDEM., alt=800.
        // ro= 1.9556768571305D-18
        // tz= 793.65549797919
        // tinf= 793.65549802348
        data = computeData(earth, 15, 800 * 1000, 0, MathLib.toRadians(-70), 16 * FastMath.PI / 12, 70, 70, 0, 0);

        // Mean atomic mass: reference PATRIUS v4.0 MCA *1000
        Assert.assertEquals(0.0038417536421558557 * 1000, data.getMeanAtomicMass(), 1E-14);
    }

    /**
     * Convert old test cases and its input values to new format.
     */
    private final static AtmosphereData computeData(final OneAxisEllipsoid earth, final double day,
                                                    final double alti, final double lon, final double lat,
                                                    final double hl, final double f, final double fbar,
                                                    final double akp3, final double akp24) throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI()).shiftedBy(86400 * day);
        final Vector3D pos = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), lat, lon, alti, "")
            .getPosition();

        final DTMInputParameters params = new DTMInputParameters(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4712987089211160124L;

            @Override
            public double getThreeHourlyKP(final AbsoluteDate date) {
                return akp3;
            }

            @Override
            public AbsoluteDate getMinDate() {
                return AbsoluteDate.PAST_INFINITY;
            }

            @Override
            public double getMeanFlux(final AbsoluteDate date) {
                return fbar;
            }

            @Override
            public AbsoluteDate getMaxDate() {
                return AbsoluteDate.FUTURE_INFINITY;
            }

            @Override
            public double getInstantFlux(final AbsoluteDate date) {
                return f;
            }

            @Override
            public double get24HoursKp(final AbsoluteDate date) {
                return akp24;
            }

            /** {@inheritDoc} */
            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
                // Nothing to do (test)
            }
        };

        final PVCoordinatesProvider sun = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -143547622504822739L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final Vector3D pCIRF = FramesFactory.getITRF().getTransformTo(FramesFactory.getCIRF(), date)
                    .transformPosition(pos);
                final double c = MathLib.tan(hl - FastMath.PI);
                final double cst = (pCIRF.getX() + c * pCIRF.getY()) / (pCIRF.getY() - c * pCIRF.getX());
                return new PVCoordinates(new Vector3D(cst, 1., 0), new Vector3D(0, 0, 0));
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                                        final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final DTM2000 atmosphere = new DTM2000(params, sun, earth);

        return atmosphere.getData(date, pos, FramesFactory.getITRF());
    }

    /** FT 268 : drag and lift implementation */
    @Test
    public void testGetSpeedOfSound() throws PatriusException {

        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        final DTM2000 atm = new DTM2000(in, sun, earth);

        final AbsoluteDate date = new AbsoluteDate(2004, 01, 01, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();

        final Vector3D position = new Vector3D(6500000., 0., 0.);
        final double speedOfSound = atm.getSpeedOfSound(date, position, gcrf);
        final double ref = 391.1109242375076;
        Assert.assertEquals(ref, speedOfSound, 1E-14);
    }

    /**
     * Test that aims at showing the "cache" process of class {@link #DTM2000} :
     * density is recomputed the method {@link DTM2000#getDensity(AbsoluteDate, Vector3D, Frame)} is successively called
     * with different parameters.
     * 
     * @throws PatriusException
     *         if DTM2000 altitude range is below 120 000 m
     * @referenceVersion 3.2
     */
    @Test
    public void testRecomputed() throws PatriusException {

        final Frame frame = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, frame);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        final DTM2000 atm = new DTM2000(in, sun, earth);

        final AbsoluteDate date = new AbsoluteDate(2004, 01, 01, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();

        setUp();

        // Altitude
        final double alt1 = computeZ(200E3);
        final double alt2 = computeZ(400E3);

        // Geodetic point
        final EllipsoidPoint point1 = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), 0, 0, alt1, "");
        final EllipsoidPoint point2 = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), 0, 0, alt2, "");

        // Positions
        final Vector3D pos1 = point1.getPosition();
        final Vector3D pos2 = point2.getPosition();

        // Check that the density depends on the frame used
        // so it is recomputed and leads to different results

        // When calling the following getDensity methods, the parameters in cache are :
        // (date, pos1, frame)
        final double density1_ITRF = atm.getDensity(date, pos1, frame);

        // Recomputation occur here : parameters in cache are now :
        // (date1, pos1, gcrf), so results are different
        final double density1_GCRF = atm.getDensity(date, pos1, gcrf);
        Assert.assertFalse(density1_GCRF == density1_ITRF);

        // Check also that values are recomputed if the position changes

        // (date1, pos2, frame) are now in cache
        final double density2 = atm.getDensity(date, pos2, frame);

        // Values are updated so different from previous
        Assert.assertFalse(density1_ITRF == density2);

        // Finally, check that changing the dates leads to recomputation in GCRF
        // Same idea about parameters in cache
        final double density_otherDate = atm.getDensity(date.shiftedBy(3600), pos1, gcrf);

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
     * @testType UT
     * 
     * @testedMethod {@link DTM2000#getData(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description test computation of all available atmospheric data
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output {@link AtmosphereData}
     * 
     * @testPassCriteria sum of partial densities equals total density. Non-regression over past versions (threshold:
     *                   1E-16)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testAtmosphereData() throws PatriusException {

        // Initialization
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1 / 0.29825765000000E+03,
            FramesFactory.getGCRF());
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        final DTM2000 atmosModel = new DTM2000(in, sun, earth);
        final DTM2000 atm2 = (DTM2000) atmosModel.copy();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D pos = new Vector3D(new double[] { 8749870.287474481, -976409.8027621375, -1110696.1958653878 });

        // Computation
        final AtmosphereData data = atm2.getData(date, pos, FramesFactory.getGCRF());
        final double density = data.getDensity();
        final double localTemperature = data.getLocalTemperature();
        final double exosphericTemperature = data.getExosphericTemperature();
        final double densityHe = data.getDensityHe();
        final double densityO = data.getDensityO();
        final double densityN2 = data.getDensityN2();
        final double densityO2 = data.getDensityO2();
        final double densityAr = data.getDensityAr();
        final double densityH = data.getDensityH();
        final double densityN = data.getDensityN();
        final double densityAnomalousOxygen = data.getDensityAnomalousOxygen();
        final double meanAtomicMass = data.getMeanAtomicMass();

        // Check density = sum partial densities
        final double expected = densityHe + densityO + densityN2 + densityO2 + densityAr + densityH + densityN
                + densityAnomalousOxygen;
        Assert.assertEquals(0., (expected - density) / expected, 2E-16);

        // Non-regression
        Assert.assertEquals(8.743030418768094E-17, density, 0.);
        Assert.assertEquals(1176.5772892517557, localTemperature, 0.);
        Assert.assertEquals(1176.5772892565271, exosphericTemperature, 0.);
        Assert.assertEquals(4.5195134429592930E-17, densityHe, 0.);
        Assert.assertEquals(1.003251773895717E-22, densityO, 0.);
        Assert.assertEquals(2.5130906021254690E-31, densityN2, 0.);
        Assert.assertEquals(2.4603723554743347E-35, densityO2, 0.);
        Assert.assertEquals(0., densityAr, 0.);
        Assert.assertEquals(4.223506943291037E-17, densityH, 0.);
        Assert.assertEquals(0., densityN, 0.);
        Assert.assertEquals(0., densityAnomalousOxygen, 0.);
        Assert.assertEquals(2.5507999134418067, meanAtomicMass, 0.);
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

}
