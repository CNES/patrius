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
 * VERSION:4.16:OPENFD-547:25/04/2025:[PATRIUS] Mauvaise gestion de computeSpinDerivatives dans BSPEphemerisLoader
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-379:25/04/2025:[PATRIUS] Ajout d'une implementation basique de OrbitalCovarianceProvider
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.15:OPENFD-380:21/11/2024:Prise en compte des NEW_MODELS dans les tests
 * VERSION:4.15:OPENFD-428:21/11/2024:[PATRIUS] Robustesse aux bsp contenant l'échelle TDB
 * VERSION:4.14:OPENFD-:22/08/2024:
 * VERSION:4.14:OPENFD-141:22/08/2024: Isolation des algorithmes de somme et produit precis
 * VERSION:4.14:OPENFD-161:22/08/2024:[PATRIUS] Adaptation de l'interface CelestialBody
 * car l'orientation n'est pas forcement IAU
 * VERSION:4.14:OPENFD-172:22/08/2024:[PATRIUS] Harmonisation de la gestion
 * des reperes predefinis et des corps predefinis
 * VERSION:4.14:OPENFD-258:22/08/2024:[PATRIUS] Ephemerides des barycentres planetaires
 * dans les fichiers JPL historiques
 * VERSION:4.14:OPENFD-171:22/08/2024: [PATRIUS] Lecture d'un corps celeste quelconque dans un fichier bsp
 * VERSION:4.14:OPENFD-259:22/08/2024:[PATRIUS] Echelle TDB pour evaluer
 * les polynemes de Chebyshev des fichiers JPL historiques
 * VERSION:4.13.1:FA:FA-170:17/01/2024:[PATRIUS] Impossible d'utiliser le corps racine d'un bsp comme corps pivot
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BSPCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.BasicCelestialPoint;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemerisLoader;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialBodyIAUOrientation;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.IAUCelestialBody;
import fr.cnes.sirius.patrius.bodies.IAUPoleCoefficients;
import fr.cnes.sirius.patrius.bodies.IAUPoleCoefficients1D;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.PredefinedEphemerisType;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.UserIAUCelestialBody;
import fr.cnes.sirius.patrius.bodies.UserIAUPole;
import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceBody;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceKernelManager;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
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

/**
 * Test class for {@link BSPEphemerisLoader} class.
 */
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
     * @throws PatriusException
     *         If an error occur when creating Patrius
     *         objects (Frame for example).
     * @throws URISyntaxException
     * @throws IOException
     * @throws SpiceErrorException
     *         If an error occurs when initializing Spice
     */
    @Test
    public void testGetPVCoordinates()
        throws PatriusException, URISyntaxException, IOException {
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

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
     * Test de validation OPENFD-253 : the aim of the test is to avoid a StackOverflow and to ensure a good management
     * of the ICRF/GCRF root chains in the frame transforms for both BSP and JPL historical ephemeris file.
     *
     * @throws PatriusException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testBspJplRootChainsFrame()
        throws PatriusException, IOException, URISyntaxException {

        // BPS data
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // StackOverflowError
        CelestialBodyFactory.getMars().getEME2000()
            .getPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8), FramesFactory.getGCRF());

    }

    /**
     * This tests verifies that in case a bsp file contains ephemeris not included in default BSP supported names, the
     * method {@link BSPEphemerisLoader#addSpiceBodyMapping(Map)} allows to add a new code-body so that the loading
     * becomes possible.
     *
     * @throws PatriusException
     *         if problems when loading the SpiceKernel
     * @throws URISyntaxException
     *         if problems when switching to URI
     * @throws IOException
     *         if problems when loading the SpiceKernel
     */
    @Test
    public void testAddSpiceBodyMappingAndLoader()
        throws PatriusException, URISyntaxException, IOException {

        // Configure the data root for the file to be used in the test
        Utils.setDataRoot("bsp_failLoad");
        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_failLoad" + File.separator + "spk3ForPatrius.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Create a loader
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        final String expMessage = "Body RANDOM_BODY1 is not available in BSP ephemeris file ^.*\\.bsp$.";
        // Try load body (non-existent: error expected)
        try {
            loader.loadCelestialBodyEphemeris("RANDOM_BODY1");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertEquals("Exception message is not equal!", expMessage, e.getMessage());
        }

        // System.out.println("Before addSpiceBodyMapping : " + SpiceBody.getBodycodenamemapping().toString());
        // Add user mapping with the ephemeris RANDOM_BODY1 contained in the bsp file
        final Map<Integer, String> mapping = new HashMap<>();
        mapping.put(987654321, "RANDOM_BODY1");
        BSPEphemerisLoader.addSpiceBodyMapping(mapping);
        // System.out.println("After addSpiceBodyMapping : " + SpiceBody.getBodycodenamemapping().toString());

        // Make a load of the celestial body ephemeris with the added body and show that it is correctly performed
        loader.loadCelestialBodyEphemeris("RANDOM_BODY1");
    }

    /**
     * This tests verifies the functionality of the method {@link BSPEphemerisLoader#clearSpiceBodyMapping()} : a
     * mapping is created and a random body is added. The clear method is called : the map must be empty after the
     * execution of the clear operation.
     */
    @Test
    public void testClearSpiceBodyMapping() {
        // System.out.println("Before addSpiceBodyMapping : " + SpiceBody.getBodycodenamemapping().toString());
        // Add user mapping with the ephemeris RANDOM_BODY1 contained in the bsp file
        final Map<Integer, String> mapping = new ConcurrentHashMap<>();
        mapping.put(987654321, "RANDOM_BODY1");
        BSPEphemerisLoader.addSpiceBodyMapping(mapping);
        // System.out.println("After addSpiceBodyMapping : " + SpiceBody.getBodycodenamemapping().toString());
        BSPEphemerisLoader.clearSpiceBodyMapping();
        // System.out.println("After clearSpiceBodyMapping : " + SpiceBody.getBodycodenamemapping().toString());
        Assert.assertTrue(SpiceBody.getBodycodenamemapping().isEmpty());
    }

    /**
     * Test load of gravitational coefficients.
     */
    @Test
    public void testGetLoadedGravitationalCoefficient() throws PatriusException {
        Utils.setDataRoot("bsp");
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        org.junit.Assert.assertEquals(1.3271244004127942E+20,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.SUN), 1E-14);
        org.junit.Assert.assertEquals(3.2485859200000000E+14,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.VENUS), 1E-14);
        org.junit.Assert.assertEquals(4.0350323562548019E+14,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.EARTH_MOON), 1E-14);
        org.junit.Assert.assertEquals(4.2828375815756102E+13,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.MARS), 1E-14);
        org.junit.Assert.assertEquals(4.2828375815756102E+13,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.MARS_BARY), 1E-14);
        org.junit.Assert.assertEquals(1.2671276409999998E+17,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.JUPITER), 1E-14);
        org.junit.Assert.assertEquals(1.2671276409999998E+17,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.JUPITER_BARY), 1E-14);
        org.junit.Assert.assertEquals(3.7940584841799997E+16,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.SATURN), 1E-14);
        org.junit.Assert.assertEquals(3.7940584841799997E+16,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.SATURN_BARY), 1E-14);
        org.junit.Assert.assertEquals(5.7945563999999985E+15,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.URANUS), 1E-14);
        org.junit.Assert.assertEquals(5.7945563999999985E+15,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.URANUS_BARY), 1E-14);
        org.junit.Assert.assertEquals(6.8365271005803989E15,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.NEPTUNE), 1E-14);
        org.junit.Assert.assertEquals(6.8365271005803989E15,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.NEPTUNE_BARY), 1E-14);
        org.junit.Assert.assertEquals(9.7550000000000000E11,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.PLUTO), 1E-14);
        org.junit.Assert.assertEquals(9.7550000000000000E11,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.PLUTO_BARY), 1E-14);
        org.junit.Assert.assertEquals(2.2031868551400003E+13,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.MERCURY), 1E-14);
        org.junit.Assert.assertEquals(4.9028001184575496E12,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.MOON), 1E-14);
        org.junit.Assert.assertEquals(3.9860043550702266E14,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.EARTH), 1E-14);
        org.junit.Assert.assertEquals(4.0350323562548019E14 + 2.2031868551400003E13 + 9.7550000000000000E11
                + 6.8365271005803989E15 + 5.7945563999999985E15 + 3.7940584841799997E16 + 1.2671276409999998E17
                + 4.2828375815756102E13 + 3.2485859200000000E14 + 1.3271244004127942E20,
            loader.getLoadedGravitationalCoefficient(PredefinedEphemerisType.SOLAR_SYSTEM_BARYCENTER), 1E5);
    }

    /**
     * @throws IOException
     * @throws URISyntaxException
     * @objective Test propagations using BSP file and JPL files
     *
     * @description Test propagations using BSP file (for Phobos) and JPL files (for planets and Sun)
     *
     * @passCriteria propagation is properly performed (reference: non-regression after thematic validation, threshold:
     *               0)
     */
    @Test
    public void testPropagationJPLBSP() throws PatriusException, IOException, URISyntaxException {

        // Initialization
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Build BSP file loader
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Phobos body of type UserCelestialBody

        // Phobos parent frame: this is PATRIUS closest known PATRIUS frame and not necessarily Phobos parent frame in
        // BSP file
        // In BSP file: Phobos parent frame is Mars ICRF barycenter whose parent frame is Mars ICRF. it is not necessary
        // to
        // link PATRIUS to Mars ICRF barycenter since this is just an internal intermediate BSP frame
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final CelestialBody phobos = new UserCelestialBody("PHOBOS", phobosEphemeris,
            0.7E9, null, phobosEphemeris.getNativeFrame(null), null);
        final GravityModel phobosGravityModel = new NewtonianGravityModel(phobos.getICRF(), 0.7E9);
        phobos.setGravityModel(phobosGravityModel);

        // Initial state - Orbit around Mars
        final Frame frame = ((IAUCelestialBody) CelestialBodyFactory.getMars())
            .getInertialFrame(IAUPoleModelType.CONSTANT);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            frame, date, CelestialBodyFactory.getMars().getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // ====== Propagation with Mars as main body and Earth as third body with JPL ephemeris ======
        final double t0 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator =
            new NumericalPropagator(integrator, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun().getGravityModel()));
        final SpacecraftState state = propagator.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4430078.346381924, state.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5419815.9072357435, state.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-5.161946162326498, state.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t0) + "ms");

        // ====== Propagation with Mars as main body and Phobos as third body with JPL and BSP ephemeris ======
        final double t1 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator2 = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator2 =
            new NumericalPropagator(integrator2, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(initialState);
        propagator2.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator2.addForceModel(new ThirdBodyAttraction(phobosGravityModel));
        final SpacecraftState state2 = propagator2.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4416360.545260159, state2.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5433134.710130642, state2.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(50.424383863753796, state2.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t1) + "ms");
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test propagations using BSP file and JPL files
     *
     * @description Test propagations using BSP file (for Phobos) and JPL files (for planets and Sun)
     *
     * @passCriteria propagation is properly performed (reference: non-regression after thematic validation, threshold:
     *               0)
     */
    @Test
    public void testPropagationJPLvsBSP() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Initial state - Orbit around Mars
        final Frame frame = ((IAUCelestialBody) CelestialBodyFactory.getMars())
            .getInertialFrame(IAUPoleModelType.CONSTANT);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            frame, date, CelestialBodyFactory.getMars().getGM());
        final SpacecraftState initialState = new SpacecraftState(orbit);

        // ====== Propagation with Mars as main body, Sun as 3rd body with JPL ephemeris ======
        final double t0 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator =
            new NumericalPropagator(integrator, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun().getGravityModel()));
        final SpacecraftState state = propagator.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4430078.346381924, state.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5419815.9072357435, state.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-5.161946162326498, state.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t0) + "ms");

        // ====== Propagation with Mars as main body, Sun as 3rd body with BSP ephemeris ======
        Utils.setDataRoot("bsp");

        final String file1 = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file1);
        final double t1 = System.currentTimeMillis();
        final FirstOrderIntegrator integrator2 = new ClassicalRungeKuttaIntegrator(100.);
        final NumericalPropagator propagator2 =
            new NumericalPropagator(integrator2, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(initialState);
        propagator2.addForceModel(new DirectBodyAttraction(CelestialBodyFactory.getMars().getGravityModel()));
        propagator2.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun().getGravityModel()));
        final SpacecraftState state2 = propagator2.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4429869.262628789, state2.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5419990.74861964, state2.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(-573.4236548540046, state2.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t1) + "ms");
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test propagations using BSP file only
     *
     * @description Test propagations using BSP file (for Phobos and for planets and Sun)
     *
     * @passCriteria propagation is properly performed (reference: non-regression after thematic validation, threshold:
     *               0)
     */
    @Test
    public void testPropagationBSP() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Build CelestialBody loader
        final CelestialBodyEphemerisLoader loader =
            new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Mars body from factory
        final IAUCelestialBody mars = (IAUCelestialBody) CelestialBodyFactory.getMars();

        // Phobos body (UserCelestialBody)
        final CelestialBodyIAUOrientation iauPole =
            new UserIAUPole(new IAUPoleCoefficients(new IAUPoleCoefficients1D(new ArrayList<>()),
                new IAUPoleCoefficients1D(new ArrayList<>()),
                new IAUPoleCoefficients1D(new ArrayList<>())));
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final IAUCelestialBody phobos = new UserIAUCelestialBody("PHOBOS", phobosEphemeris,
            0.7E9, iauPole, phobosEphemeris.getNativeFrame(null), null);
        final GravityModel phobosGravityModel =
            new NewtonianGravityModel(phobos.getInertialFrame(IAUPoleModelType.CONSTANT), 0.7E9);
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
        final NumericalPropagator propagator =
            new NumericalPropagator(integrator, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
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
        final NumericalPropagator propagator2 =
            new NumericalPropagator(integrator2, frame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(initialState);
        propagator2.addForceModel(new DirectBodyAttraction(mars.getGravityModel()));
        propagator2.addForceModel(new ThirdBodyAttraction(phobosGravityModel));
        final SpacecraftState state2 = propagator2.propagate(date.shiftedBy(86400));
        Assert.assertEquals(4416487.551430735, state2.getPVCoordinates().getPosition().getX(), 0.);
        Assert.assertEquals(-5433030.149856378, state2.getPVCoordinates().getPosition().getY(), 0.);
        Assert.assertEquals(50.765554228716255, state2.getPVCoordinates().getPosition().getZ(), 0.);
        System.out.println((System.currentTimeMillis() - t1) + "ms");

        // Other minor checks
        Assert
            .assertTrue(phobos.getEphemeris().getNativeFrame(null).getName().equals("MARS BARYCENTER ICRF ^.*\\.bsp$"));

        // // TODO: currently StackOverflowError. Problème de construction de l'arbre des repères Terre
        // final KeplerianOrbit orbit2 = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
        // FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8), Constants.CNES_STELA_MU);
        // final LocalOrbitalFrame frameLOF = new LocalOrbitalFrame(FramesFactory.getGCRF(), LOFType.LVLH, orbit2,
        // "LOF");
        // System.out.println(CelestialBodyFactory.getMars().getEME2000().getPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8),
        // FramesFactory.getGCRF()));
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test transformations when frame tree is linked between JPL ans BSP ephemeris
     *
     * @description Test transformations when frame tree is linked between JPL ans BSP ephemeris through Mars common
     *              body with 3 cases
     *              - Mars ICRF (from JPL) and Mars Barycenter (from BSP): result should be close (~10cm)
     *              - SSB ICRF (from JPL) and SSB (from BSP): result should be about 1.5km
     *              - EMB ICRF (from JPL) and EMB (from BSP): result should be about 1.5km
     *
     * @passCriteria transformation is as expected (reference: non-regression after thematic validation, threshold: 0)
     */
    @Test
    public void testSetFramesTrees() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Mars from JPL ephemeris
        final CelestialPoint mars = CelestialBodyFactory.getMars();

        // Phobos from BSP ephemeris
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");

        // Link frames trees (MARS ICRF from JPL and "MARS" body from BSP)
        loader.linkFramesTrees(mars.getICRF(), "MARS");
        Assert.assertEquals(loader.getBodyLink(), "MARS");

        // Check transform between Mars ICRF and Mars BSP barycenter if small (distance about 20cm)
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final Vector3D res =
            mars.getICRF().getTransformTo(phobosEphemeris.getNativeFrame(null), date).getCartesian().getPosition();
        Assert.assertEquals(0,
            res.distance(new Vector3D(0.10093404962664373, 0.029617246703695563, -0.04246546587980414)), 0.);

        // Check transform between JPL SSB and BSP SSB
        final CelestialPoint ssbJPL = CelestialBodyFactory.getSolarSystemBarycenter();
        final CelestialBodyEphemeris ssbBSP = loader.loadCelestialBodyEphemeris("MARS BARYCENTER");
        final Vector3D res2 =
            ssbJPL.getICRF().getTransformTo(ssbBSP.getNativeFrame(null), date).getCartesian().getPosition();
        Assert.assertEquals(0, res2.distance(new Vector3D(459.92401123046875, -408.4702606201172, 1443.8779220581055)),
            0.);

        // Check transform between JPL EMB and BSP EMB
        final CelestialPoint embJPL = CelestialBodyFactory.getEarthMoonBarycenter();
        final CelestialBodyEphemeris earthBSP = loader.loadCelestialBodyEphemeris("EARTH");
        final Vector3D res3 =
            embJPL.getICRF().getTransformTo(earthBSP.getNativeFrame(null), date).getCartesian().getPosition();
        Assert.assertEquals(0, res3.distance(new Vector3D(401.8907165527344, -170.76209449768066, 998.4608144760132)),
            0.);
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Verify the resolution of issue OPENFD-547 where, when the spin derivatives are to be computed for a
     *            transformation, a null pointer was raised.
     *
     * @description Test transformations when frame tree is linked between JPL ans BSP ephemeris through Mars common
     *              body. If the spin derivatives of the transformation are to be computed, the rotation acceleration
     *              should not be null
     *
     * @passCriteria a not null value for the rotation acceleration is returned when the spin derivatives of the
     *               transformation are computed
     */
    @Test
    public void testCompSpinDerInTrans() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Mars from JPL ephemeris
        final CelestialPoint mars = CelestialBodyFactory.getMars();

        // Phobos from BSP ephemeris
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");

        // Link frames trees (MARS ICRF from JPL and "MARS" body from BSP)
        loader.linkFramesTrees(mars.getICRF(), "MARS");

        // Check transform between Mars ICRF and Mars BSP barycenter if small (distance about 20cm)
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);

        // Check that if the transformation is computed without spin derivatives, a null is returned for the rotation
        // acceleration
        final Transform transWoSpinDer =
            mars.getICRF().getTransformTo(phobosEphemeris.getNativeFrame(null), date, false);

        Assert.assertNull(transWoSpinDer.getRotationAcceleration());

        // Check that if the transformation is computed with spin derivatives, a not null is returned for the rotation
        // acceleration
        final Transform transWithSpinDer =
            mars.getICRF().getTransformTo(phobosEphemeris.getNativeFrame(null), date, true);

        Assert.assertNotNull(transWithSpinDer.getRotationAcceleration());

    }

    /**
     * FA-112
     *
     * @throws URISyntaxException
     * @throws IOException
     * @objective Check frame tree is correct when Earth ICRF is the pivot frame (for EARTH BSP body)
     *
     * @description Check that frame tree between Phobos ICRF and ICRF SSB is correct
     *
     * @passCriteria frame tree between Phobos ICRF and ICRF SSB is correct (functional test)
     */
    @Test
    public void testSetFramesTreesEarth() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Mars from JPL ephemeris
        final CelestialPoint earth = CelestialBodyFactory.getEarth();

        // Phobos from BSP ephemeris
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");

        // Link frames trees (EARTH ICRF from JPL and "EARTH" body from BSP)
        loader.linkFramesTrees(earth.getICRF(), "EARTH");

        // Check frame tree
        Frame frame = phobosEphemeris.getNativeFrame(null);
        final String[] expectedTree = { "MARS BARYCENTER ICRF ^.*\\.bsp$",
            "SOLAR SYSTEM BARYCENTER ICRF ^.*\\.bsp$",
            "EARTH BARYCENTER ICRF ^.*\\.bsp$",
            "GCRF",
            "Earth-Moon barycenter ICRF frame",
            "ICRF" };

        int i = 0;
        while (frame != null) {
            Assert.assertEquals(expectedTree[i], frame.getName());
            frame = frame.getParent();
            i++;
        }
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test parent frame references
     *
     * @description Check Phobos and Deimos have same parent reference frame
     *
     * @passCriteria Phobos and Deimos have same parent reference frame
     */
    @Test
    public void testParent() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Phobos from BSP ephemeris
        final BSPEphemerisLoader loader = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        final CelestialBodyEphemeris phobosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");
        final CelestialBodyEphemeris deimosEphemeris = loader.loadCelestialBodyEphemeris("PHOBOS");

        Assert.assertEquals(phobosEphemeris.getNativeFrame(null), deimosEphemeris.getNativeFrame(null));
    }

    /**
     * @throws URISyntaxException
     * @objective Test various degraded cases when reading BSP files
     *
     * @description Test various degraded cases when reading BSP files
     *
     * @passCriteria exceptions are raised as expected
     */
    @Test
    public void testFunctional() throws FileNotFoundException, IOException, PatriusException, URISyntaxException {
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Non-existent file
        try {
            new BSPEphemerisLoader("dummyFile.bsp").loadCelestialBodyEphemeris("DUMMY");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Body not in file
        try {
            final BSPEphemerisLoader loader =
                new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
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
            loader.loadData(new FileInputStream("src" + File.separator + "test" + File.separator + "resources"
                    + File.separator + "bsp" + File.separator + "mar097_20160314_20300101.bsp"),
                "DUMMY");
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }
        // Check toString
        Assert.assertNotNull(CelestialBodyFactory.getSun().toString());

        // Convention
        final BSPEphemerisLoader loader2 = new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        loader2.setSPICEJ2000Convention(SpiceJ2000ConventionEnum.EME2000);
        Assert.assertEquals(SpiceJ2000ConventionEnum.EME2000, loader2.getConvention());

    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test various functional cases when reading BSP files
     *
     * @description Test various functional cases when reading BSP files
     *
     * @passCriteria exceptions are raised as expected, bodies are built as expected
     */
    @Test
    public void testCelestialBodyPointLoad() throws PatriusException, URISyntaxException, IOException {
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        final BSPCelestialBodyLoader loader = new BSPCelestialBodyLoader(
            BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        // Load Celestial body

        // Test exception in case of loading barycenter as CelestialBody
        try {
            loader.loadCelestialBody("EARTH BARYCENTER");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Test exception in case of loading non-existent ephemeris type
        try {
            loader.loadCelestialBody("MARS BARYCENTER");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Load Celestial point
        Assert.assertTrue(loader.loadCelestialPoint("EARTH BARYCENTER") instanceof BasicCelestialPoint);
        Assert.assertTrue(loader.loadCelestialPoint("MARS BARYCENTER") instanceof BasicCelestialPoint);

        // LoadCelestialBody method

        // Load barycenter as CelestialBody
        loader.declareAsCelestialPoint("MARS BARYCENTER");
        // Exception since barycenter as been declared as such
        try {
            loader.loadCelestialBody("MARS BARYCENTER");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test SSB can be loaded from BSP ephemeris (although it is not present in BSP file)
     *
     * @description load SSB and performs frame transformation from SSB to Earth
     *
     * @passCriteria SSB is loaded without exception and transformation is as expected
     */
    @Test
    public void testSSB() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // SSB from factory
        final CelestialPoint ssb = CelestialBodyFactory.getSolarSystemBarycenter();

        // Earth from factory
        final CelestialPoint earth = CelestialBodyFactory.getEarth();

        // Check translation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final Vector3D translation = ssb.getICRF().getTransformTo(earth.getICRF(), date).getTranslation();
        Assert.assertEquals(-1.47251842782079E11, translation.getX(), 0.);
        Assert.assertEquals(1.5421833722702797E10, translation.getY(), 0.);
        Assert.assertEquals(6.660210875203937E9, translation.getZ(), 0.);
    }

    /**
     * @throws URISyntaxException
     * @throws IOException
     * @objective Test SSB can be defined as link between JPL dans BSP
     *
     * @description Test SSB can be defined as link between JPL dans BSP
     *
     * @passCriteria SSB is linked without exception, transformation between Earth JPL ans Earth BSP is properly
     *               computed
     */
    @Test
    public void testFramessTreesSSB() throws PatriusException, URISyntaxException, IOException {
        // Initialization
        Utils.setDataRoot("bsp_de");

        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp_de" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        // Earth/SSB from JPL ephemeris
        final CelestialPoint earthJPL = CelestialBodyFactory.getEarth();
        final CelestialPoint ssbJPL = CelestialBodyFactory.getSolarSystemBarycenter();

        // Link frames trees (SSB ICRF from JPL and "SOLAR SYSTEM BARYCENTER" body from BSP)
        final BSPCelestialBodyLoader loader =
            new BSPCelestialBodyLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        ((BSPEphemerisLoader) loader.getEphemerisLoader()).linkFramesTrees(FramesFactory.getICRF(),
            "SOLAR SYSTEM BARYCENTER");

        // Earth/SSB from BSP ephemeris
        final CelestialPoint embBSP = loader.loadCelestialPoint("EARTH");
        final CelestialPoint ssbBSP = loader.loadCelestialPoint("SOLAR SYSTEM BARYCENTER");

        // Check transform between EMB (JPL) and EMB (BSP) - Small distance expected
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5.111860E8);
        final Vector3D res = earthJPL.getICRF().getTransformTo(embBSP.getICRF(), date).getCartesian().getPosition();
        Assert.assertEquals(0, res.distance(new Vector3D(-57.795074462890625, 238.12741088867188, -445.35020542144775)),
            0.);

        // Check transform between SSB (JPL) and SSB (BSP) - Should be 0
        final Vector3D res2 = ssbJPL.getICRF().getTransformTo(ssbBSP.getICRF(), date).getCartesian().getPosition();
        Assert.assertEquals(0, res2.getNorm(), 0.);
    }

    /**
     * @throws PatriusException
     * @throws IOException
     * @throws URISyntaxException
     *
     * @description This test is added as part of FA-253. It aims at testing the use of several BSP files in the same
     *              method.
     */
    @Test
    public void testSeveralBSP() throws PatriusException, IOException, URISyntaxException {
        Utils.setDataRoot("bsp");
        final String file = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "mar097_20160314_20300101.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);
        final BSPCelestialBodyLoader loader =
            new BSPCelestialBodyLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);

        final String file1 = new File(ClassLoader
            .getSystemResource(
                "bsp" + File.separator + "spk3ForPatrius.bsp")
            .toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file1);
        final BSPCelestialBodyLoader loader1 =
            new BSPCelestialBodyLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
    }

    /**
     * This test validates the loading operations of a bsp file using TT-TDB tabulation with empty
     * observer/target ID
     * name.
     *
     * @throws PatriusException
     *         if problems when loading the spice kernel or loading celestial body
     *         ephemeris
     * @throws URISyntaxException
     *         if problems when converting toURI
     * @throws IOException
     *         if problems when loading the spice kernel
     */
    @Test
    public void testTDBtimescaleManagement()
        throws PatriusException, URISyntaxException, IOException {
        Utils.setDataRoot("bsp");
        final String file = new File(ClassLoader
            .getSystemResource("bsp" + File.separator + "de432t-TRUNCATED.bsp").toURI())
                .getAbsolutePath();
        SpiceKernelManager.loadSpiceKernel(file);

        final BSPEphemerisLoader loader =
            new BSPEphemerisLoader(BSPCelestialBodyLoader.DEFAULT_BSP_SUPPORTED_NAMES);
        try {
            loader.loadCelestialBodyEphemeris("");
            Assert.fail("Loading operations should fail at this point!");
        } catch (final PatriusException e) {
            Assert.assertEquals("Body is present in the BPS ephemeris file!",
                "Body  is not available in BSP ephemeris file ^.*\\.bsp$.", e.getMessage());
        }
        try {
            final String keyToLoad = "EARTH";
            final CelestialBodyEphemeris earthEphem = loader.loadCelestialBodyEphemeris(keyToLoad);
        } catch (final PatriusException e) {
            fail("Une exception a été levée " + e.getMessage());
        }

    }

}
