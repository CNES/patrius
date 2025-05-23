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
 * @history creation 10/07/2012
 * @history A-1004 : 15/10/12
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
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
 * VERSION::FA:318:05/11/2014:anomalies correction for class RediffusedFlux
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:1324:09/05/2018:Add comments for the model limits and the possibility to use the earth as unique source (0,0)
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialBodyIAUOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * 
 * @description <p>
 *              Test class for the rediffused flux computation.
 *              </p>
 * 
 * @author ClaudeD
 * 
 * @version $Id: RediffusedFluxTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class RediffusedFluxTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative model.
         * 
         * @featureDescription Computation of the redistributed solar flux.
         * 
         * @coveredRequirements DV-MOD_290
         */
        REDISTRIBUTED_FLUX
    }

    /** Positions epsilon. */
    private static final double POS_EPSILON = 1e-8;

    /** Angles epsilon. */
    private static final double ANG_EPSILON = 1e-9;

    /** Albedo and infrared values epsilon. */
    private static final double ALBINF_EPSILON = 1e-7;

    /** ITRF reference frame */
    private static CelestialBodyFrame itrf;

    /** GCRF reference frame */
    private static CelestialBodyFrame gcrf;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(RediffusedFluxTest.class.getSimpleName(), "Rediffused flux");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDISTRIBUTED_FLUX}
     * 
     * @testedMethod {@link RediffusedFlux#RediffusedFlux(int, int, Frame, CelestialPoint, PVCoordinatesProvider, AbsoluteDate, IEmissivityModel)}
     * @testedMethod {@link RediffusedFlux#RediffusedFlux(int, int, Frame, CelestialPoint, PVCoordinatesProvider, AbsoluteDate, IEmissivityModel, boolean, boolean)}
     * 
     * @description test the albedo and infrared flux
     * 
     * @input none
     * 
     * @output albedo flux, infrared flux
     * 
     * @testPassCriteria correct flux
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2.1
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testRediffusedFlux() throws PatriusException {

        Report.printMethodHeader("testRediffusedFlux", "Rediffused flux computation", "Unknown",
            ALBINF_EPSILON, ComparisonType.ABSOLUTE);

        int nCorona = 10;
        int nMeridian = 10;

        final PVCoordinatesProvider satProvider = this.getOrbitLEO();
        final AbsoluteDate d = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        IEmissivityModel model = new KnockeRiesModel();

        // Mock object for the sun, which always returns the same PVCoordinates
        final CelestialBody mockSun = new CelestialBody(){

            /** Serializable UID. */
            private static final long serialVersionUID = 8828766113161271470L;

            final CelestialBody sunProvider = CelestialBodyFactory.getSun();

            @Override
            public CelestialBodyEphemeris getEphemeris() {
                return null;
            }

            @Override
            public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                // Reference position for the date d
                final Vector3D position = new Vector3D(-1.4734309100022910E+11,
                    7.6346686461594896E+09, -1.5742957910260618E+10);
                final PVCoordinates pv = new PVCoordinates(position, Vector3D.ZERO);
                return pv;
            }

            @Override
            public String getName() {
                return this.sunProvider.getName();
            }

            @Override
            public double getGM() {
                return this.sunProvider.getGM();
            }

            @Override
            public CelestialBodyFrame getICRF() throws PatriusException {
                return this.sunProvider.getICRF();
            }

            @Override
            public CelestialBodyFrame getEME2000() throws PatriusException {
                return sunProvider.getEME2000();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getInertialFrame(final IAUPoleModelType iauPole) throws PatriusException {
                final CelestialBodyFrame frame;
                switch (iauPole) {
                    case CONSTANT:
                        // Get an inertially oriented, body centered frame taking into account
                        // only constant part of IAU pole data with respect to ICRF frame. The frame
                        // is always bound to the body center, and its axes have a fixed
                        // orientation with respect to other inertial frames.
                        frame = this.sunProvider.getInertialFrame(IAUPoleModelType.CONSTANT);
                        break;
                    case MEAN:
                        // Get an inertially oriented, body centered frame taking into account only
                        // constant and secular part of IAU pole data with respect to ICRF frame.
                        frame = this.sunProvider.getInertialFrame(IAUPoleModelType.MEAN);
                        break;
                    case TRUE:
                        // Get an inertially oriented, body centered frame taking into account
                        // constant, secular and harmonics part of IAU pole data with respect to
                        // ICRF frame.
                        frame = this.sunProvider.getInertialFrame(IAUPoleModelType.TRUE);
                        break;
                    default:
                        // The iauPole given as input is not implemented in this method.
                        throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
                }
                return frame;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CelestialBodyFrame getRotatingFrame(final IAUPoleModelType iauPole) throws PatriusException {
                final CelestialBodyFrame frame;
                switch (iauPole) {
                    case CONSTANT:
                        // Get a body oriented, body centered frame taking into account only constant part
                        // of IAU pole data with respect to inertially-oriented frame. The frame is always
                        // bound to the body center, and its axes have a fixed orientation with respect to
                        // the celestial body.
                        frame = this.sunProvider.getRotatingFrame(IAUPoleModelType.CONSTANT);
                        break;
                    case MEAN:
                        // Get a body oriented, body centered frame taking into account constant and secular
                        // part of IAU pole data with respect to mean equator frame. The frame is always
                        // bound to the body center, and its axes have a fixed orientation with respect to
                        // the celestial body.
                        frame = this.sunProvider.getRotatingFrame(IAUPoleModelType.MEAN);
                        break;
                    case TRUE:
                        // Get a body oriented, body centered frame taking into account constant, secular
                        // and harmonics part of IAU pole data with respect to true equator frame. The frame
                        // is always bound to the body center, and its axes have a fixed orientation with
                        // respect to the celestial body.
                        frame = this.sunProvider.getRotatingFrame(IAUPoleModelType.TRUE);
                        break;
                    default:
                        // The iauPole given as input is not implemented in this method.
                        throw new PatriusException(PatriusMessages.INVALID_IAUPOLEMODELTYPE);
                }
                return frame;
            }

            @Override
            public BodyShape getShape() {
                return this.sunProvider.getShape();
            }

            @Override
            public void setShape(final BodyShape shapeIn) {
                this.sunProvider.setShape(shapeIn);
                ;
            }

            @Override
            public GravityModel getGravityModel() {
                return this.sunProvider.getGravityModel();
            }

            @Override
            public void setGravityModel(final GravityModel gravityModelIn) {
                this.sunProvider.setGravityModel(gravityModelIn);
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

        final Vector3D pvcp = satProvider.getPVCoordinates(d, gcrf).getPosition();
        Assert.assertEquals(2.06479765813527000e+06, pvcp.getX(), POS_EPSILON);
        Assert.assertEquals(-3.89513229411636293e+06, pvcp.getY(), POS_EPSILON);
        Assert.assertEquals(-5.09383750217838865e+06, pvcp.getZ(), POS_EPSILON);

        final CelestialBodyFrame itrfFrame = itrf;
        final PVCoordinates satPV = satProvider.getPVCoordinates(d, itrfFrame);

        // Checking the satellite position...
        double v = satPV.getPosition().getX();
        {
            // obelix reference value = -2.7930686610869779E+06
            final double refVal = -2.7930686610869779E+06;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), POS_EPSILON);
        }

        v = satPV.getPosition().getY();
        {
            // obelix reference value = 3.4121722103315047E+06
            final double refVal = 3.4121722103315047E+06;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), POS_EPSILON);
        }

        v = satPV.getPosition().getZ();
        {
            // obelix reference value = -5.0929827918228116E+06
            final double refVal = -5.0929827918228116E+06;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), POS_EPSILON);
        }

        RediffusedFlux flux = new RediffusedFlux(nCorona, nMeridian, itrfFrame, mockSun,
            satProvider, d, model);
        ElementaryFlux[] fluxElement = flux.getFlux();

        // Checking the fluxes
        // nCorona = 10, nMeridian = 10
        v = fluxElement[0].getAlbedoPressure();
        {
            // obelix value = 3.3678353015882278E-07
            final double refVal = 3.3678353015882278E-07;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), ALBINF_EPSILON);
            Report.printToReport("Albedo pressure", refVal, v);
        }

        v = fluxElement[0].getInfraRedPressure();
        {
            // obelix value = 2.4027324919806277E-07
            final double refVal = 2.4027324919806277E-07;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), ALBINF_EPSILON);
            Report.printToReport("IR pressure", refVal, v);
        }

        // Obelix reference flux direction
        Vector3D refFlux = new Vector3D(-4.1460716543484116E-01, 0.506507795025916,
            -0.7560097564642517);
        Vector3D dirFlux = fluxElement[0].getDirFlux();
        double ad = Vector3D.angle(refFlux, dirFlux);
        Assert.assertEquals(0., ad, ANG_EPSILON);
        Report.printToReport("Flux direction", refFlux, dirFlux);

        // Checking the fluxes
        // nCorona = 1, nMeridian = 1
        nCorona = 1;
        nMeridian = 1;
        // sunProvider = CelestialBodyFactory.getSun();
        // satProvider = this.getOrbitLEO();
        // d = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        model = new KnockeRiesModel();
        flux = new RediffusedFlux(nCorona, nMeridian, itrfFrame, mockSun, satProvider, d, model);
        fluxElement = flux.getFlux();

        v = fluxElement[0].getAlbedoPressure();
        {
            // nCorona = 1, nMeridian = 1
            // obelix value = 1.7007568273020547E-05
            final double refVal = 1.7007568273020547E-05;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), ALBINF_EPSILON);
        }

        v = fluxElement[0].getInfraRedPressure();
        {
            // obelix value = 1.2133799084502170E-05
            final double refVal = 1.2133799084502170E-05;
            Assert.assertEquals(0., subcomputeRelativeDeviation(v, refVal), ALBINF_EPSILON);
        }

        final RediffusedFlux fluxIr = new RediffusedFlux(nCorona, nMeridian, itrfFrame, mockSun,
            satProvider, d, model, false, true);
        final ElementaryFlux[] fluxIrElement = fluxIr.getFlux();

        // Obelix reference flux direction
        refFlux = new Vector3D(-4.1460716543484116E-01, 0.506507795025916, -0.7560097564642517);
        dirFlux = fluxIrElement[0].getDirFlux();
        ad = Vector3D.angle(refFlux, dirFlux);
        Assert.assertEquals(0., ad, ANG_EPSILON);

        this.rediffusedFluxSubcaseZero(satProvider, d, model, mockSun, itrfFrame);

        // create a new rediffused flux using a polar orbit for coverage purposes
        final RediffusedFlux fluxPolarorbit = new RediffusedFlux(11, 17, itrfFrame, mockSun,
            this.getPolarOrbit(), d, model);
        Assert.assertEquals(fluxPolarorbit.getFlux().length, 11 * 17 + 1);

    }

    /**
     * No flux subcase.
     * 
     * @param satProvider satProvider
     * @param d d
     * @param model model
     * @param mockSun mockSun
     * @param itrfFrame itrfFrame
     * @throws PatriusException should not happen
     */
    private void rediffusedFluxSubcaseZero(final PVCoordinatesProvider satProvider,
                                           final AbsoluteDate d, final IEmissivityModel model,
                                           final CelestialPoint mockSun,
                                           final CelestialBodyFrame itrfFrame) throws PatriusException {
        // Without flux
        final RediffusedFlux flux0 = new RediffusedFlux(10, 10, itrfFrame, mockSun, satProvider, d,
            model, false, false);
        final ElementaryFlux[] flux0Element = flux0.getFlux();
        Assert.assertEquals(0., flux0Element[0].getDirFlux().getX(), 0.);
        Assert.assertEquals(0., flux0Element[0].getDirFlux().getY(), 0.);
        Assert.assertEquals(0., flux0Element[0].getDirFlux().getZ(), 0.);
        Assert.assertEquals(0., flux0Element[0].getAlbedoPressure(), 0.);
        Assert.assertEquals(0., flux0Element[0].getInfraRedPressure(), 0.);
        Assert.assertEquals(0., flux0Element[100].getAlbedoPressure(), 0.);
        Assert.assertEquals(0., flux0Element[100].getInfraRedPressure(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDISTRIBUTED_FLUX}
     * 
     * @testedMethod {@link RediffusedFlux#RediffusedFlux(int, int, Frame, CelestialPoint, PVCoordinatesProvider, AbsoluteDate, IEmissivityModel)}
     * 
     * @description test the exception
     * 
     * @input bad value for nCorona or nMeridian
     * 
     * @output exception
     * 
     * @testPassCriteria exception
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2.1
     * 
     * @throws PatriusException should not happen
     */
    @Test
    public void testRediffusedFluxException() throws PatriusException {
        final CelestialPoint sunProvider = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider satProvider = this.getOrbitLEO();
        final AbsoluteDate d = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final IEmissivityModel model = new KnockeRiesModel();
        final CelestialBodyFrame itrfFrame = FramesFactory.getITRF();

        boolean asExpected = false;
        try {
            new RediffusedFlux(-1, 10, itrfFrame, sunProvider, satProvider, d, model);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
            asExpected = true;
        }
        Assert.assertTrue(asExpected);

        asExpected = false;
        try {
            new RediffusedFlux(10, -1, itrfFrame, sunProvider, satProvider, d, model);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
            asExpected = true;
        }

        Assert.assertTrue(asExpected);

        asExpected = false;
        try {
            new RediffusedFlux(0, 1, itrfFrame, sunProvider, satProvider, d, model);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
            asExpected = true;
        }

        Assert.assertTrue(asExpected);

        asExpected = false;
        try {
            new RediffusedFlux(1, 0, itrfFrame, sunProvider, satProvider, d, model);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
            asExpected = true;
        }

        Assert.assertTrue(asExpected);

        asExpected = false;
        try {
            new RediffusedFlux(0, 0, itrfFrame, sunProvider, satProvider, d, model);

        } catch (final IllegalArgumentException e) {
            // expected !
            asExpected = true;
        }
        Assert.assertFalse(asExpected);

    }

    /**
     * get a LEO orbit
     * 
     * @return orbit
     */
    private PVCoordinatesProvider getOrbitLEO() {
        // initial date from ZOOM reference
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0,
            TimeScalesFactory.getTAI());
        // mu from grim4s4_gr potential file
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel from ZOOM ephemeris reference
        final Vector3D pos = new Vector3D(2.06479765813527000e+06, -3.89513229411636293e+06,
            -5.09383750217838865e+06);
        final Vector3D vel = new Vector3D(-2.19214076040873215e+03, 5.39738533682446723e+03,
            -5.01727016544111484e+03);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
        return orbit;
    }

    /**
     * Get a polar orbit
     * 
     * @return a polar orbit
     */
    private PVCoordinatesProvider getPolarOrbit() {
        // initial date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0,
            TimeScalesFactory.getTAI());
        // mu from grim4s4_gr potential file
        final double mu = 0.39860043770442e+15;
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(80000000, 0.01, MathLib.toRadians(90), 0.0, 0.0,
            FastMath.PI / 2.0, PositionAngle.MEAN, referenceFrame, date, mu);
        return orbit;
    }

    /**
     * Compute the relative deviation (sans eps).
     * 
     * @param x first double
     * @param y second double
     * @return x and y relative deviation
     */
    private static Double subcomputeRelativeDeviation(final double x, final double y) {
        Double rez;
        if (MathLib.abs(x) > MathLib.abs(y)) {
            rez = MathLib.abs((x - y) / x);
        } else {
            rez = MathLib.abs((x - y) / y);
        }
        return rez;
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException should not happen
     * 
     */
    @SuppressWarnings("deprecation")
    @BeforeClass
    public static void setUp() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003/de405-ephemerides");
        final String jplf = "unxp2000.405";
        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader(jplf,
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader(jplf,
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER,
            loaderSSB);
        gcrf = FramesFactory.getGCRF();

        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        itrf = FramesFactory.getITRF();

    }

    /**
     * @throws PatriusException problem at the model creation
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        final boolean isIR = true;
        final boolean isAlbedo = false;
        final PVCoordinatesProvider satProvider = this.getOrbitLEO();
        final IEmissivityModel model = new KnockeRiesModel();

        final RediffusedFlux flux = new RediffusedFlux(1, 1, FramesFactory.getGCRF(),
            CelestialBodyFactory.getSun(), satProvider, AbsoluteDate.J2000_EPOCH, model, isIR,
            isAlbedo);

        Assert.assertEquals(isIR, flux.isIr());
        Assert.assertEquals(isAlbedo, flux.isAlbedo());
    }
}
