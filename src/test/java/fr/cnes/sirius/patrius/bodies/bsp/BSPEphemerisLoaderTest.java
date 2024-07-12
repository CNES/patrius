/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BSPCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemerisLoader;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.IAUPole;
import fr.cnes.sirius.patrius.bodies.IAUPoleCoefficients;
import fr.cnes.sirius.patrius.bodies.IAUPoleCoefficients1D;
import fr.cnes.sirius.patrius.bodies.IAUPoleFunction;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.UserIAUPole;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BSPEphemerisLoaderTest {

    /**
     * Tests the {@link CelestialBodyEphemeris#getPVCoordinates(AbsoluteDate, Frame)} method for a type 2 SPK file.
     * Here we check that an exception is thrown if the asked date for the PVCoordinate is not available in the bsp
     * file.
     *
     * @passCriteria An illegal argument exception is thrown
     */
    @Test(expected = PatriusException.class)
    public void testGetPVCoordinatesOutOfRangeDate() throws PatriusException {
        Utils.setDataRoot("bsp");
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        final CelestialBodyEphemeris ephemeris = loader.loadCelestialBodyEphemeris("BORRELLY");

        // Try to get the ephemeris at a date outside the data
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        ephemeris.getPVCoordinates(date, FramesFactory.getGCRF());
    }

    /**
     * Tests the {@link CelestialBodyEphemeris#getPVCoordinates(AbsoluteDate, Frame)} method for a type 2 SPK file.
     * Here we check that an exception is thrown if the asked date for the PVCoordinate is not available in the bsp
     * file.
     * <p>
     * For this test, we read a particular file and verify that the content red by the reader matches the content of the
     * file that we have hard coded in the test.In the conversion test, we check the data contained in the arrays of the
     * SPK kernel, depending on the data type.
     * <p>
     *
     * @objective This method is used to test the {@link BSPEphemerisLoader} conversion
     *            method : toDataModel(), for the specific SPK data type 02.
     *
     * @description The test consists in a numerical comparison between
     *              the expected values (values written in the source test file) and the
     *              actual values that have been red by the reader.
     *
     * @passCriteria All fields red by the reader are equals with the
     *               expected value.
     *
     * @requirement For this test to perform, the right source test file must be
     *              available in the resource directory.
     *
     * @throws PatriusException If an error occur when creating Patrius
     *         objects (Frame for example).
     * @throws SpiceErrorException If an error occurs when initializing Spice
     */
    @Test
    public void testGetPVCoordinates()
        throws PatriusException {
        Utils.setDataRoot("bsp_de");
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // PV check
        final CelestialBodyEphemeris ephemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);

        final PVCoordinates phobosPV = ephemeris.getPVCoordinates(date, CelestialBodyFactory.getMars().getICRF());

        final Vector3D phobosPos = phobosPV.getPosition();

        // Phobos position should be consistent with Phobos standard distance to Mars center
        Assert.assertTrue(phobosPos.getNorm() < 10000E3);
    }

    /**
     * Test load of gravitational coefficients.
     */
    @Test
    public void testGetLoadedGravitationalCoefficient() throws PatriusException {
        Utils.setDataRoot("bsp");
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        org.junit.Assert.assertEquals(1.3271244004127942E+20,
            loader.getLoadedGravitationalCoefficient(EphemerisType.SUN), 1E-14);
        org.junit.Assert.assertEquals(3.2485859200000000E+14,
            loader.getLoadedGravitationalCoefficient(EphemerisType.VENUS), 1E-14);
        org.junit.Assert.assertEquals(4.0350323562548019E+14,
            loader.getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON), 1E-14);
        org.junit.Assert.assertEquals(4.2828375815756102E+13,
            loader.getLoadedGravitationalCoefficient(EphemerisType.MARS), 1E-14);
        org.junit.Assert.assertEquals(1.2671276409999998E+17,
            loader.getLoadedGravitationalCoefficient(EphemerisType.JUPITER), 1E-14);
        org.junit.Assert.assertEquals(3.7940584841799997E+16,
            loader.getLoadedGravitationalCoefficient(EphemerisType.SATURN), 1E-14);
        org.junit.Assert.assertEquals(5.7945563999999985E+15,
            loader.getLoadedGravitationalCoefficient(EphemerisType.URANUS), 1E-14);
        org.junit.Assert.assertEquals(6.8365271005803989E15,
            loader.getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE), 1E-14);
        org.junit.Assert.assertEquals(9.7550000000000000E11,
            loader.getLoadedGravitationalCoefficient(EphemerisType.PLUTO), 1E-14);
        org.junit.Assert.assertEquals(2.2031868551400003E+13,
            loader.getLoadedGravitationalCoefficient(EphemerisType.MERCURY), 1E-14);
        org.junit.Assert.assertEquals(4.9028001184575496E12,
            loader.getLoadedGravitationalCoefficient(EphemerisType.MOON), 1E-14);
        org.junit.Assert.assertEquals(3.9860043550702266E14,
            loader.getLoadedGravitationalCoefficient(EphemerisType.EARTH), 1E-14);
        org.junit.Assert.assertEquals(4.0350323562548019E14 + 2.2031868551400003E13 + 9.7550000000000000E11
                + 6.8365271005803989E15 + 5.7945563999999985E15 + 3.7940584841799997E16 + 1.2671276409999998E17
                + 4.2828375815756102E13 + 3.2485859200000000E14 + 1.3271244004127942E20,
                loader.getLoadedGravitationalCoefficient(EphemerisType.SOLAR_SYSTEM_BARYCENTER), 1E5);
    }

    /**
     * @objective Test propagations using BSP file and JPL files
     *
     * @description Test propagations using BSP file (for Phobos) and JPL files (for planets and Sun)
     *
     * @passCriteria propagation is properly performed (reference: non-regression after thematic validation, threshold: 0)
     */
    @Test
    public void testPropagationJPLBSP() throws PatriusException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        // Build BSP file loader
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Phobos body of type UserCelestialBody

        // Phobos parent frame: this is PATRIUS closest known PATRIUS frame and not necessarily Phobos parent frame in BSP file
        // In BSP file: Phobos parent frame is Mars ICRF barycenter whose parent frame is Mars ICRF. it is not necessary to 
        // link PATRIUS to Mars ICRF barycenter since this is just an internal intermediate BSP frame
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final CelestialBody phobos = new UserCelestialBody("PHOBOS", phobosEphemeris,
                0.7E9, null, phobosEphemeris.getNativeFrame(null, null), null);
        final GravityModel phobosGravityModel = new NewtonianGravityModel(phobos.getICRF(), 0.7E9);
        phobos.setGravityModel(phobosGravityModel);
        
        // Initial state - Orbit around Mars
        final Frame frame = CelestialBodyFactory.getMars().getInertialFrame(IAUPoleModelType.CONSTANT);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                frame, date, CelestialBodyFactory.getMars().getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);
        
        // ====== Propagation with Mars as main body and Earth as third body with JPL ephemeris ======
        final double t0 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator = new NumericalPropagator(integrator, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun().getGravityModel()));
        final SpacecraftState state = propagator.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4430078.346382135, state.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5419815.90723554, state.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-5.161946162523615, state.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t0) + "ms");

        // ====== Propagation with Mars as main body and Phobos as third body with JPL and BSP ephemeris ======
        final double t1 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator2 = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(initialState);
        propagator2.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator2.addForceModel(new ThirdBodyAttraction(phobosGravityModel));
        final SpacecraftState state2 = propagator2.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4416360.517411188, state2.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5433134.736657448, state2.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(50.424476729550214, state2.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t1) + "ms");
    }

    /**
     * @objective Test propagations using BSP file and JPL files
     *
     * @description Test propagations using BSP file (for Phobos) and JPL files (for planets and Sun)
     *
     * @passCriteria propagation is properly performed (reference: non-regression after thematic validation, threshold: 0)
     */
    @Test
    public void testPropagationJPLvsBSP() throws PatriusException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        // Initial state - Orbit around Mars
        final Frame frame = CelestialBodyFactory.getMars().getInertialFrame(IAUPoleModelType.CONSTANT);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                frame, date, CelestialBodyFactory.getMars().getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // ====== Propagation with Mars as main body, Sun as 3rd body with JPL ephemeris ======
        final double t0 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator = new NumericalPropagator(integrator, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun().getGravityModel()));
        final SpacecraftState state = propagator.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4430078.346382135, state.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5419815.90723554, state.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-5.161946162523615, state.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t0) + "ms");

        // ====== Propagation with Mars as main body, Sun as 3rd body with BSP ephemeris ======
        Utils.setDataRoot("bsp");
        final double t1 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator2 = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(initialState);
        propagator2.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator2.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun().getGravityModel()));
        final SpacecraftState state2 = propagator2.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4429848.560761231, state2.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5420008.11701873, state2.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-573.2473669168108, state2.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t1) + "ms");
    }

    /**
     * @objective Test propagations using BSP file only
     *
     * @description Test propagations using BSP file (for Phobos and for planets and Sun)
     *
     * @passCriteria propagation is properly performed (reference: non-regression after thematic validation, threshold: 0)
     */
    @Test
    public void testPropagationBSP() throws PatriusException {
        // Initialization
        Utils.setDataRoot("bsp");

        // Build CelestialBody loader
        final CelestialBodyEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Mars body from factory
        final CelestialBody mars = CelestialBodyFactory.getMars();

        // Phobos body (UserCelestialBody)
        final IAUPole iauPole = new UserIAUPole(new IAUPoleCoefficients(new IAUPoleCoefficients1D(new ArrayList<IAUPoleFunction>()),
                new IAUPoleCoefficients1D(new ArrayList<IAUPoleFunction>()),
                new IAUPoleCoefficients1D(new ArrayList<IAUPoleFunction>())));
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final CelestialBody phobos = new UserCelestialBody("PHOBOS", phobosEphemeris,
                0.7E9, iauPole, phobosEphemeris.getNativeFrame(null, null), null);
        final GravityModel phobosGravityModel = new NewtonianGravityModel(phobos.getInertialFrame(IAUPoleModelType.CONSTANT), 0.7E9);
        phobos.setGravityModel(phobosGravityModel);

        // Sun body from factory
        final CelestialBody sun = CelestialBodyFactory.getSun();

        // Initial state - Orbit around Mars
        final Frame frame = mars.getInertialFrame(IAUPoleModelType.CONSTANT);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                frame, date, mars.getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // ====== Propagation with Mars as main body and Sun as third body with only BSP ephemeris ======
        final double t0 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator = new NumericalPropagator(integrator, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(mars.getGravityModel()));
        propagator.addForceModel(new ThirdBodyAttraction(sun.getGravityModel()));
        
        final SpacecraftState state = propagator.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4430197.26066027, state.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5419718.7062539235, state.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-5.161802276755939, state.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t0) + "ms");

        // ====== Propagation with Mars as main body and Phobos as third body with only BSP ephemeris ======
        final double t1 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator2 = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(initialState);
        propagator2.addForceModel(new DirectBodyAttraction(mars.getGravityModel()));
        propagator2.addForceModel(new ThirdBodyAttraction(phobosGravityModel));
        final SpacecraftState state2 = propagator2.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4416487.551430735, state2.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5433030.149856378, state2.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(50.765554228716255, state2.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t1) + "ms");

        // Other minor checks
        Assert.assertTrue(phobos.getEphemeris().getNativeFrame(null, null).getName().equals("IAU_ICRF_MARS BARYCENTER"));

//        // TODO: currently StackOverflowError. Problème de construction de l'arbre des repères Terre
//        final KeplerianOrbit orbit2 = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
//                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8), Constants.CNES_STELA_MU);
//        final LocalOrbitalFrame frameLOF = new LocalOrbitalFrame(FramesFactory.getGCRF(), LOFType.LVLH, orbit2, "LOF");
//        System.out.println(CelestialBodyFactory.getMars().getEME2000().getPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8), FramesFactory.getGCRF()));    
    }

    /**
     * @objective Test transformations when frame tree is linked between JPL ans BSP ephemeris
     *
     * @description Test transformations when frame tree is linked between JPL ans BSP ephemeris through Mars common body with 3 cases
     *              - Mars ICRF (from JPL) and Mars Barycenter (from BSP): result should be close (~10cm)
     *              - SSB ICRF (from JPL) and SSB (from BSP): result should be about 1.5km
     *              - EMB ICRF (from JPL) and EMB (from BSP): result should be about 1.5km
     *
     * @passCriteria transformation is as expected (reference: non-regression after thematic validation, threshold: 0)
     */
    @Test
    public void testSetFramesTrees() throws PatriusException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        // Mars from JPL ephemeris
        final CelestialBody mars = CelestialBodyFactory.getMars();

        // Phobos from BSP ephemeris
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");

        // Link frames trees (MARS ICRF from JPL and "MARS" body from BSP)
        loader.linkFramesTrees(mars.getICRF(), "MARS");
        
        // Check transform between Mars ICRF and Mars BSP barycenter if small (distance about 20cm)
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final Vector3D res = mars.getICRF().getTransformTo(phobosEphemeris.getNativeFrame(null, null), date).getCartesian().getPosition();
        Assert.assertEquals(0, res.distance(new Vector3D(0.10093404962664373, 0.029617246703695563, -0.04246546587980414)), 0.);

        // Check transform between JPL SSB and BSP SSB
        final CelestialBody ssbJPL = CelestialBodyFactory.getSolarSystemBarycenter();
        final CelestialBodyEphemeris ssbBSP = loader.loadCelestialBodyEphemeris("MARS BARYCENTER");
        final Vector3D res2 = ssbJPL.getICRF().getTransformTo(ssbBSP.getNativeFrame(null, null), date).getCartesian().getPosition();
        Assert.assertEquals(0, res2.distance(new Vector3D(459.2584533691406, -407.49351501464844, 1444.3439254760742)), 0.);

        // Check transform between JPL EMB and BSP EMB
        final CelestialBody embJPL = CelestialBodyFactory.getEarthMoonBarycenter();
        final CelestialBodyEphemeris earthBSP = loader.loadCelestialBodyEphemeris("EARTH");
        final Vector3D res3 = embJPL.getICRF().getTransformTo(earthBSP.getNativeFrame(null, null), date).getCartesian().getPosition();
        Assert.assertEquals(0, res3.distance(new Vector3D(-58.24407958984375, 236.19407653808594, -446.07347869873047)), 0.);
    }

    /**
     * @objective Test parent frame references
     *
     * @description Check Phobos and Deimos have same parent reference frame
     *
     * @passCriteria Phobos and Deimos have same parent reference frame
     */
    @Test
    public void testParent() throws PatriusException {
        // Initialization
        Utils.setDataRoot("bsp");

        // Phobos from BSP ephemeris
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final CelestialBodyEphemeris deimosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");

        Assert.assertEquals(phobosEphemeris.getNativeFrame(null, null), deimosEphemeris.getNativeFrame(null, null));
    }

    /**
     * @objective Test various degraded cases when reading BSP files
     *
     * @description Test various degraded cases when reading BSP files
     *
     * @passCriteria exceptions are raised as expected
     */
    @Test
    public void testFunctional() throws FileNotFoundException, IOException, ParseException {
        Utils.setDataRoot("bsp");

        // Non-existent file
        try {
            new BSPEphemerisLoader("dummyFile.bsp").loadCelestialBodyEphemeris("DUMMY");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Body not in file
        try {
            final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
            loader.loadCelestialBodyEphemeris("DUMMY");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Other minor checks
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        Assert.assertTrue(loader.stillAcceptsData());
        try {
            // Wrong InputStream type
            loader.loadData(new FileInputStream("src" + File.separator + "test" + File.separator + "resources" + File.separator + "bsp" + File.separator + "mar097_20160314_20300101.bsp"), "DUMMY");
            Assert.fail();
        } catch (PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }
}
