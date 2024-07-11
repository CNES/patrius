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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.InertiaComputedModel;
import fr.cnes.sirius.patrius.assembly.models.InertiaSimpleModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel.MassEquation;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * 
 * @version $Id: SimpleMassModelTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class SimpleMassModelTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Validation of simple mass model
         * 
         * @featureDescription test the simple mass model
         * 
         * @coveredRequirements
         */
        SIMPLE_MASS_MODEL;
    }

    /** Mass model. */
    private MassProvider massModel;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIMPLE_MASS_MODEL}
     * 
     * @testedMethod {@link SimpleMassModel#updateMass(final String partName, final double mass)}
     * 
     * @description try to update the mass of a non-existing part and throw an exception
     * 
     * @input a SimpleMassModel
     * 
     * @output an OrekitExceptionWrapper exception
     * 
     * @testPassCriteria throw an OrekitExceptionWrapper exception
     * 
     * @throws PatriusException
     *         if the mass becomes negative (PatriusMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE)
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test(expected = PatriusExceptionWrapper.class)
    public void testUpdateMass() throws PatriusException {
        new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        // Try to update the mass with an incorrect part name:
        this.massModel.updateMass("No Name", 500.0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SIMPLE_MASS_MODEL}
     * 
     * @testedMethod {@link MassEquation#computeDerivatives(SpacecraftState, TimeDerivativesEquations)}
     * 
     * @description try to set a negative mass and throw an exception
     * 
     * @input a SimpleMassModel with a negative mass
     * 
     * @output a PropagationException exception
     * 
     * @testPassCriteria throw a PropagationException exception
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMass() {
        new SimpleMassModel(-1000., "DEFAULT");
    }

    /**
     * @throws PatriusException
     *         in case of a frame error
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}, {@link features#INERTIA_COMPUTED_MODEL}
     * 
     * @testedMethod {@link InertiaSimpleModel#setMassDerivativeZero()}
     * @testedMethod {@link InertiaComputedModel#setMassDerivativeZero()}
     * 
     * @description check mass derivative is properly set to 0 in the corresponding equation
     * 
     * @testPassCriteria mass derivative value is 0
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void setMassDerivativeZeroTest() throws PatriusException {

        // Models creation
        final String partName = "tank";
        final SimpleMassModel model1 = new SimpleMassModel(50.0, partName);

        // ================== Set mass derivative to -5, then to zero ==================

        // Initialization
        final AccelerationRetriever retriever = new AccelerationRetriever();

        // Model 1
        SpacecraftState state1 = new SpacecraftState(null);
        state1 = state1.addAdditionalState("MASS_" + partName, new double[] { 50. });

        model1.addMassDerivative(partName, -5);
        model1.getAdditionalEquation(partName).computeDerivatives(state1, retriever);
        Assert.assertEquals(retriever.getDerivative(), -5, 0.);

        model1.setMassDerivativeZero(partName);
        model1.getAdditionalEquation(partName).computeDerivatives(state1, retriever);
        Assert.assertEquals(retriever.getDerivative(), 0., 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}
     * 
     * @testedMethod {@link SimpleMassModel#getTotalMass(SpacecraftState)}
     * 
     * @description check the mass is the expected one.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetTotalMassSpacecraftState() {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);

        // Simple inertia model case

        final MassProvider massModel1 = new SimpleMassModel(1000, "Main");
        final MassProvider massModel2 = new SimpleMassModel(900, "Main");

        // Check without mass provider
        Assert.assertEquals(1000., massModel1.getTotalMass(new SpacecraftState(orbit)), 0.);

        // Check with mass provider
        Assert.assertEquals(900., massModel1.getTotalMass(new SpacecraftState(orbit, massModel2)), 0.);
    }

    /**
     * Acceleration retriever for testing purpose.
     */
    private static class AccelerationRetriever implements TimeDerivativesEquations {

        private static final long serialVersionUID = -4616792058307814184L;
        private double derivative;

        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit) {
            // nothing to do
        }

        @Override
        public void addKeplerContribution(final double mu) {
            // nothing to do
        }

        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            // nothing to do
        }

        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame) {
            // nothing to do
        }

        public double getDerivative() {
            return this.derivative;
        }

        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
            this.derivative = pDot[0];
        }

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            this.massModel = new SimpleMassModel(1000., "DEFAULT");

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }
}
