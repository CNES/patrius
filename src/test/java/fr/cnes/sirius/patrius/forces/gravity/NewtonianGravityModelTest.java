/**
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
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for parameter methods
 */
public class NewtonianGravityModelTest {

    /**
     * Test for coverage purpose
     */
    @Test
    public void testGetEventsDetectors() {
        final NewtonianGravityModel force = new NewtonianGravityModel(5);
        final EventDetector[] detectors = (new DirectBodyAttraction(force)).getEventsDetectors();
        Assert.assertEquals(0, detectors.length);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NewtonianGravityModel#NewtonianAttraction(double, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link NewtonianGravityModel}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // Instance
        final String nameMu = "central attraction coefficient";
        final Parameter mu = new Parameter(nameMu, 5);
        final NewtonianGravityModel force = new NewtonianGravityModel(FramesFactory.getGCRF(), mu, false);

        // Spacecraft
        final KeplerianOrbit orbit = new KeplerianOrbit(6.7e6, .1, .2, .3, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(),
            new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        final SpacecraftState sp = new SpacecraftState(orbit);

        // Compute partial derivatives
        final double[][] dAccdPos = force.computeDAccDPos(sp.getPVCoordinates().getPosition(),
            sp.getDate());

        // Check that all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], Precision.DOUBLE_COMPARISON_EPSILON);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NewtonianGravityModel#computeAcceleration(SpacecraftState)}
     * 
     * @description compute acceleration with multiplicative factor k
     * 
     * @testPassCriteria acceleration with k = 5 = 5 * acceleration with k = 1
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testMultiplicativeFactor() throws PatriusException {
        final double mu = Constants.WGS84_EARTH_MU;
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,mu));
        final NewtonianGravityModel actualModel = new NewtonianGravityModel(FramesFactory.getGCRF(),
            new Parameter("", mu), false);
        final DirectBodyAttraction forceModel = new DirectBodyAttraction(actualModel, true);
        forceModel.setMultiplicativeFactor(5.);
        final NewtonianGravityModel expectedModel = new NewtonianGravityModel(FramesFactory.getGCRF(),
            new Parameter("", mu), false);
        
        // Acceleration
        final Vector3D actual = forceModel.computeAcceleration(state);
        final Vector3D expected = expectedModel.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate()).scalarMultiply(5.);
        Assert.assertEquals(expected, actual);
        // Partial derivatives
        final double[][] dAccdPosActual = new double[3][3];
        forceModel.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(), dAccdPosActual);
        final double[][] dAccdPosExpected = expectedModel.computeDAccDPos(state.getPVCoordinates().getPosition(),
            state.getDate());
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(dAccdPosActual[i][j], dAccdPosExpected[i][j] * 5.,
                    Precision.DOUBLE_COMPARISON_EPSILON);
            }
        }
        // K value
        Assert.assertEquals(5., forceModel.getMultiplicativeFactor(), 0.);
    }
    
    /**
     * @testType UT
     * 
     * @testedMethod {@link NewtonianGravityModel#computeAcceleration(SpacecraftState)}
     * 
     * @description compute acceleration with frame transformation
     * 
     * @testPassCriteria accelerations are the same
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testMultiplicativeFactorWithTransfo() throws PatriusException {
        
        // Set configuration
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        
        // Instantiate newtonian models
        final double mu = Constants.WGS84_EARTH_MU;
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,mu));
        final NewtonianGravityModel expectedModel = new NewtonianGravityModel(FramesFactory.getGCRF(),
            new Parameter("", mu), false);
        final NewtonianGravityModel actualModel = new NewtonianGravityModel(FramesFactory.getCIRF(),
            new Parameter("", mu), false);
        
        // Acceleration
        final Vector3D expected = expectedModel.computeAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D actual = actualModel
            .computeAcceleration(state.getPVCoordinates().getPosition(), state.getDate());
        
        // Assertions
        Assert.assertEquals(expected.getX(), actual.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expected.getY(), actual.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expected.getZ(), actual.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    double sum(final double[] d) {
        double temp = 0;
        for (final double dd : d) {
            temp += dd;
        }
        return temp;
    }

    /**
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
    public void testGetters() {
        // Build model with default GCRF
        NewtonianGravityModel model = new NewtonianGravityModel(Constants.CNES_STELA_MU);
        Assert.assertEquals(true, model.getBodyFrame().getName().equals("GCRF"));

        // Build model with EME2000 (not a body frame but for test purpose)
        model = new NewtonianGravityModel(FramesFactory.getEME2000(), Constants.CNES_STELA_MU);
        Assert.assertEquals(true, model.getBodyFrame().getName().equals("EME2000"));

    }

}
