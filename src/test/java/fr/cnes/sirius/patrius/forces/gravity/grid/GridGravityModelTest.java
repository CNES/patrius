/**
 * 
 * Copyright 2021-2021 CNES
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
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3113:10/05/2022:[PATRIUS] Probleme avec le modele de gravite sous forme de grille spherique 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2861:18/05/2021:Optimisation du calcul des derivees partielles de EmpiricalForce 
 * VERSION:4.7:DM:DM-2687:18/05/2021:Traitement de modèles de gravité, autres que les harmoniques sphériques
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.grid;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.BalminoGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TriLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TricubicSplineInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SphericalCoordinates;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link GridGravityModel} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class GridGravityModelTest {

    /**
     * @testType UT
     * 
     * @description check computation of acceleration on grid knots (with cartesian file)
     * 
     * @testPassCriteria acceleration is as expected (reference: from file, relative threshold: 1E-15, due to round-off errors)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void accelerationCartesianOnKnotsTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is line 1000 in file)
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(-0.1000000000E+02, -0.5000000000E+01, -0.1000000000E+02).scalarMultiply(1000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, force.getMu()));
        
        // Check
        final Vector3D actual = force.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D expected = new Vector3D(+0.2691503164E01, +0.1506732218E01, +0.3404867886E01).scalarMultiply(force.getMu() / 1E9);
        Assert.assertEquals(0, actual.distance(expected), 1E-15);
    }

    /**
     * @testType UT
     * 
     * @description check computation of acceleration within grid knots (with cartesian file and spline interpolation)
     * 
     * @testPassCriteria acceleration is as expected (reference: from file, relative threshold: 1E-4, due to manuel approximation of file values)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void accelerationCartesianWithinKnotsTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TricubicSplineInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is exactly between line 1000 and 1001 in file)
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(-0.1000000000E+02, -0.5000000000E+01, -0.7500000000E+01).scalarMultiply(1000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, force.getMu()));
        
        // Check
        final Vector3D actual = force.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D expected1 = new Vector3D(+0.2691503164E01, +0.1506732218E01, +0.3404867886E01);
        final Vector3D expected2 = new Vector3D(+0.5780718374E01, +0.3028712927E01, +0.4077843169E01);
        final Vector3D expected = new Vector3D(0.5, expected1, 0.5, expected2).scalarMultiply(force.getMu() / 1E9);
        Assert.assertEquals(0, actual.distance(expected), 3E-4);
    }

    /**
     * @testType UT
     * 
     * @description check computation of acceleration on grid knots (with spherical file)
     * 
     * @testPassCriteria acceleration is as expected (reference: from file, relative threshold: 2E-14, due to round-off errors)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void accelerationSphericalOnKnotsTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_sphere.txt";
        final SphericalGridAttractionLoader loader = new SphericalGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is line 1000 in file)
        final SphericalCoordinates coords = new SphericalCoordinates(MathLib.toRadians(5), MathLib.toRadians(10), 9000);
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                coords.getCartesianCoordinates(), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));
        
        // Check
        final Vector3D actual = force.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D expected = new Vector3D(-7.856589059157337600e-03, -1.441903035720318200e-03, -7.508503790460136100e-04).scalarMultiply(force.getMu() / 1E9 * 1000);
        Assert.assertEquals(0, actual.distance(expected), 2E-14);
    }

    /**
     * @testType UT
     * 
     * @description check that results is independent of modulo along longitude
     * 
     * @testPassCriteria result is identical if longitude = 190deg or -170deg
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void accelerationSphericalCoordsNormalizedLongitudeTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_sphere.txt";
        final SphericalGridAttractionLoader loader = new SphericalGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State with longitude = 190deg
        final SphericalCoordinates coords1 = new SphericalCoordinates(MathLib.toRadians(5), MathLib.toRadians(190), 9000);
        final SpacecraftState state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                coords1.getCartesianCoordinates(), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));

        // State with longitude = -170deg
        final SphericalCoordinates coords2 = new SphericalCoordinates(MathLib.toRadians(5), MathLib.toRadians(-170), 9000);
        final SpacecraftState state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                coords2.getCartesianCoordinates(), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));

        // Check
        final Vector3D actual1 = force.computeAcceleration(state1.getPVCoordinates().getPosition(),
            state1.getDate());
        final Vector3D actual2 = force.computeAcceleration(state2.getPVCoordinates().getPosition(),
            state2.getDate());
        Assert.assertEquals(0, actual1.distance(actual2), 1E-16);
    }

    /**
     * @testType UT
     * 
     * @description check computation of acceleration within grid knots (with spherical file and spline interpolation)
     * 
     * @testPassCriteria acceleration is as expected (reference: from file, relative threshold: 1E-4, due to manuel approximation of file values)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void accelerationSphericalWithinKnotsTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_sphere.txt";
        final SphericalGridAttractionLoader loader = new SphericalGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is exactly between line 1000 and 1001 in file)
        final SphericalCoordinates coords = new SphericalCoordinates(MathLib.toRadians(7.5), MathLib.toRadians(10), 9000);
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                coords.getCartesianCoordinates(), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, force.getMu()));
        
        // Check
        final Vector3D actual = force.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D expected1 = new Vector3D(-7.856589059157337600e00, -1.441903035720318200e00, -7.508503790460136100e-01);
        final Vector3D expected2 = new Vector3D(-7.766784778628123400e00, -1.425421447623912600e00, -1.495986333331622400e00);
        final Vector3D expected = new Vector3D(0.5, expected1, 0.5, expected2).scalarMultiply(force.getMu() / 1E9);
        Assert.assertEquals(0, actual.distance(expected), 1E-4);
    }

    /**
     * @testType UT
     * 
     * @description check computation of potential on grid knots (with cartesian file)
     * 
     * @testPassCriteria potential is as expected (reference: from file, relative threshold: 1E-15, due to round-off errors)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void potentialCartesianOnKnotsTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is line 1000 in file)
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(-0.1000000000E+02, -0.5000000000E+01, -0.1000000000E+02).scalarMultiply(1000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));
        
        // Check
        final double actual = force.computePotential(state.getPVCoordinates().getPosition(), state.getFrame(),
            state.getDate());
        final double expected = +0.6722012383E-01 * +0.712699979843467446E-03 * 1E9 / 1E3;
        Assert.assertEquals(0, (expected - actual) / expected, 1E-15);
    }

    /**
     * @testType UT
     * 
     * @description check computation of potential on grid knots (with spherical file)
     * 
     * @testPassCriteria potential is as expected (reference: from file, relative threshold: 2E-15, due to round-off errors)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void potentialSphericalOnKnotsTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_sphere.txt";
        final SphericalGridAttractionLoader loader = new SphericalGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is line 1000 in file)
        final SphericalCoordinates coords = new SphericalCoordinates(MathLib.toRadians(5), MathLib.toRadians(10), 9000);
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                coords.getCartesianCoordinates(), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));
        
        // Check
        final double actual = force.computePotential(state.getPVCoordinates().getPosition(), state.getFrame(),
            state.getDate());
        final double expected = 8.489861727499203900e-02 * 7.126999798434674500e-04 * 1E9 / 1E3;
        Assert.assertEquals(0, (expected - actual) / expected, 2E-15);
    }

    /**
     * @testType UT
     * 
     * @description check computation of acceleration outside grid
     * 
     * @testPassCriteria acceleration is as expected (reference: back-up model, relative threshold: 0)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void accelerationOutsideGridTest() throws PatriusException {
        // Back-up model
        final double[][] cnorm = { { 1.0 }, { 0.0 }, { -4.84165270522E-4 } };
        final double[][] snorm = { { 0.0 }, { 0.0 }, { 0.0 } };
        final BalminoGravityModel backupModel = new BalminoGravityModel(FramesFactory.getGCRF(), 20000,
            0.712699979843467446E-03 * 1E9, cnorm, snorm);
        backupModel.setCentralTermContribution(false);
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                backupModel, FramesFactory.getGCRF());

        // State outside grid
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(100000, 200000, 300000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));
        
        // Check
        final Vector3D actual = force.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D expected = backupModel.computeNonCentralTermsAcceleration(
            state.getPVCoordinates().getPosition(), state.getDate());
        Assert.assertEquals(0, actual.distance(expected), 0);
    }

    /**
     * @testType UT
     * 
     * @description check computation of potential outside grid
     * 
     * @testPassCriteria potential is close to potential on grid border (reference: potential on grid border, relative threshold: 2E-3, limited due to central body potential approximation)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void potentialOutsideGridTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
                null, FramesFactory.getGCRF());
        
        // State (position is line 17 in file)
        final SpacecraftState state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(-0.4000000000E+02, -0.4000000000E+02, -0.2000000000E+02).scalarMultiply(1000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));
        // State shifted by 1 micrometer (outside grid)
        final SpacecraftState state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(-0.40000000001E+02, -0.4000000000E+02, -0.2000000000E+02).scalarMultiply(1000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU));
        
        // Check
        final double actual = force.computePotential(state2.getPVCoordinates().getPosition(), state2.getFrame(),
            state2.getDate());
        final double expected = force.computePotential(state1.getPVCoordinates().getPosition(), state1.getFrame(),
            state1.getDate());
        Assert.assertEquals(0, (expected - actual) / expected, 2E-3);
    }

    /**
     * @testType UT
     * 
     * @description check basic functional features of {@link GridGravityModel} are as expected:
     *              <ul>
     *              <li>No event detectors</li>
     *              <li>No partial derivatives</li>
     *              <li>Value of Mu</li>
     *              </ul>
     * 
     * @testPassCriteria read data is as expected (reference: from file)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void functionalTest() throws PatriusException {
        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final BalminoGravityModel gravityModel = new BalminoGravityModel(FramesFactory.getTIRF(),
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU, new double[2][2], new double[2][2]);
        gravityModel.setCentralTermContribution(false);
        final GridGravityModel force = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
            gravityModel, FramesFactory.getGCRF());
        final DirectBodyAttraction forceModel = new DirectBodyAttraction(force);
        forceModel.setMultiplicativeFactor(5.);

        // Factor k check
        final GridGravityModel force2 = new GridGravityModel(loader, new TriLinearIntervalsInterpolator(),
            new BalminoGravityModel(FramesFactory.getTIRF(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_MU, new double[2][2], new double[2][2]), FramesFactory.getGCRF());
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(
                new Vector3D(-0.1000000000E+02, -0.5000000000E+01, -0.1000000000E+02).scalarMultiply(1000), 
                new Vector3D(0, 0, 0)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, force.getMu()));
        Assert.assertEquals(forceModel.computeAcceleration(state),
            force2.computeAcceleration(state.getPVCoordinates().getPosition(), state.getDate())
            .scalarMultiply(5.));

        // Functional Checks
        Assert.assertEquals(0, forceModel.getEventsDetectors().length);

        final double[][] dAccdPos = new double[3][3];
        try {
            forceModel.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(), dAccdPos);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        for (int i = 0; i < dAccdPos.length; i++) {
            for (int j = 0; j < dAccdPos[i].length; j++) {
                Assert.assertEquals(0., dAccdPos[i][j], 0.);
            }
        }
        try {
			forceModel.addDAccDParam(state.getPVCoordinates().getPosition(),
					state.getFrame(), state.getDate(), new Parameter("", 0),
					new double[3]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        Assert.assertEquals(+0.712699979843467446E-03 * 1E9, force.getMu(), 0.);
        forceModel.checkData(null, null);
        Assert.assertEquals(5., forceModel.getMultiplicativeFactor());
    }

    /**
     * @testType UT
     * 
     * @description integration test: compare final bulletin of propagation with cartesian grid attraction force (and spline interpolation)
     *              vs propagation with simple Newtonian attraction force over a 1h propagation
     * 
     * @testPassCriteria final position are close (threshold: 1km)
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void integrationTest() throws PatriusException {

        // Propagation with grid model

        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force1 = new GridGravityModel(loader, new TricubicSplineInterpolator(),
                null, FramesFactory.getGCRF());

        // Initial state within grid
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(20000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, force1.getMu()));
        final AbsoluteDate finalDate = state.getDate().shiftedBy(3600.);

        // Build propagator without grid attraction
        final NumericalPropagator propagator1 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.),
            state.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator1.setInitialState(state);
        propagator1.addForceModel(new DirectBodyAttraction(force1));

        // Propagation
        final SpacecraftState finalState1 = propagator1.propagate(finalDate);
        
        // Propagation with Balmino model 8x8

        // Build propagator
        final NumericalPropagator propagator2 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.),
            state.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(state);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(state.getMu())));

        // Propagation
        final SpacecraftState finalState2 = propagator2.propagate(finalDate);

        // Compare final bulletin (threshold: 1.1km)
        Assert.assertEquals(0, finalState1.getPVCoordinates().getPosition().distance(finalState2.getPVCoordinates().getPosition()), 1100);
    }

    /**
     * @testType UT
     * 
     * @description performance test: compare duration of propagation with cartesian grid attraction force (and spline interpolation)
     *              vs propagation with Balmino 8x8 attraction force over a 10 days propagation
     * 
     * @testPassCriteria durations are of same order of magnitude
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void performanceTest() throws PatriusException, IOException, ParseException {

        // Propagation with grid model
        final double t01 = System.currentTimeMillis();

        // Build attraction model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel force1 = new GridGravityModel(loader, new TricubicSplineInterpolator(),
                null, FramesFactory.getGCRF());

        // Initial state within grid
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(20000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, force1.getMu()));
        final AbsoluteDate finalDate = state.getDate().shiftedBy(10. * Constants.JULIAN_DAY);

        // Build propagator without Newtonian attraction
        final NumericalPropagator propagator1 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.),
            state.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator1.setInitialState(state);
        propagator1.addForceModel(new DirectBodyAttraction(force1));

        // Propagation
        propagator1.propagate(finalDate);
        final double tf1 = (System.currentTimeMillis() - t01) / 1000.;
        
        // Propagation with Balmino model 8x8
        final double t02 = System.currentTimeMillis();

        // Build attraction model
        Utils.setDataRoot("regular-dataPBASE");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();
        final double[][] c = data.getC(8, 8, true);
        final double[][] s = data.getS(8, 8, true);
        final BalminoGravityModel force2 = new BalminoGravityModel(FramesFactory.getGCRF(), 10000, force1.getMu(), c, s);

        // Build propagator
        final NumericalPropagator propagator2 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.),
            state.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
        propagator2.setInitialState(state);
        propagator2.addForceModel(new DirectBodyAttraction(force2));

        // Propagation
        propagator2.propagate(finalDate);
        final double tf2 = (System.currentTimeMillis() - t02) / 1000.;

        // Check computation time
        // "3" threshold is here to prevent from machine unexpected slowdown, in practise delta is much lower
        System.out.println(tf1 + " " + tf2);
        Assert.assertTrue(tf1 < 3. * tf2);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testGetters() throws PatriusException {
        // Build model
        final String filename = "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "grid" + File.separator + "GRA_grille_cube.txt";
        final CartesianGridAttractionLoader loader = new CartesianGridAttractionLoader(filename);
        final GridGravityModel model = new GridGravityModel(loader, new TricubicSplineInterpolator(),
            null, FramesFactory.getGCRF());
        Assert.assertEquals(true, model.getBodyFrame().getName().equals("GCRF"));

    }

}
