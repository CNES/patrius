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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:706:13/12/2016: synchronisation problem with the Assemby mass
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Validation tests for additional states tolerances in {@link NumericalPropagator}.
 * 
 * @author cardosop
 * 
 * @version $Id: AdditionalStatesValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 */
public class AdditionalStatesValTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Tolerance values for additional states
         * 
         * @featureDescription Support for tolerance values for additional states in the numerical propagator
         * 
         * @coveredRequirements DV-PROPAG_61, DV-PROPAG_62
         */
        ADDITIONAL_STATES_TOLERANCE_VALUES
    }

    private static Validate VAL;

    private static final double AWFUL_EPSILON_LIN = 60.;
    private static final double BETTER_EPSILON_LIN = 1e-7;

    private static final double AWFUL_EPSILON_LINTRI = 100.;
    private static final double BETTER_EPSILON_LINTRI = 1e-8;

    private double mu;
    private AbsoluteDate initDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private AddStatesStepHandler adsHandler;

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ADDITIONAL_STATES_TOLERANCE_VALUES}
     * 
     * @testedMethod {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)}
     * @testedMethod {@link NumericalPropagator#setInitialAdditionalState(String, double[])}
     * 
     * @description Validation test with one custom additional state and no tolerances
     * 
     * @input an additional state : temperature and mass, linear fusion modelization
     * 
     * @output additional state at the end of the propagation
     * 
     * @testPassCriteria propagation without tolerances for the additional state, expected unaccurate, with epsilon
     *                   AWFUL_EPSILON_LIN - no external references for this test.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateLinearOne() throws PatriusException {
        final AdditionalEquations aeq = new TemperatureAndMassEquationsLinear();
        final double startTemp = 0.;
        final double startMass = 0.;

        final double[] addState = { startTemp, startMass };

        // duration after which the fusion is supposed to start :
        final double fusionDuration = TemperatureAndMassEquationsLinear.referenceFusionStart(startTemp);
        // State expected at end of propagation
        final double refDuration = fusionDuration + 200.;
        final double refTemp = TemperatureAndMassEquationsLinear.referenceTemp(startTemp, refDuration);
        final double refMass = TemperatureAndMassEquationsLinear.referenceMass(startTemp, startMass, refDuration);

        // Add the equation to the propagator
        this.propagator.addAdditionalEquations(aeq);
        // Initialize the additional state
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), addState);
        // Propagation without custom tolerances
        this.propagator.setInitialState(this.initialState);
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));
        // Get the final additional state
        final double[] ads = this.adsHandler.saveState;

        // Since there are no custom tolerances for the additional state,
        // the propagation will not become more accurate around the "fusion start" date.
        // Therefore, the mass and temperature will NOT be accurate at all!

        // System.out.println("Temp : " + ads[7] + " - Mass : " + ads[8]);
        // System.out.println("RefTemp : " + refTemp + " - RefMass : " + refMass);

        VAL.assertEquals(ads[0], refTemp, AWFUL_EPSILON_LIN, refTemp, AWFUL_EPSILON_LIN, "unaccurate temperature");
        VAL.assertEquals(ads[1], refMass, AWFUL_EPSILON_LIN, refMass, AWFUL_EPSILON_LIN, "unaccurate mass");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ADDITIONAL_STATES_TOLERANCE_VALUES}
     * 
     @testedMethod {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)}
     * 
     * @description Validation test with one custom additional state and tolerances
     * 
     * @input an additional state : temperature and mass, linear fusion modelization
     * 
     * @output additional state at the end of the propagation
     * 
     * @testPassCriteria propagation with tolerances for the additional state, expected accurate, with epsilon
     *                   BETTER_EPSILON_LIN - no external references for this test.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateLinearTwo() throws PatriusException {
        final AdditionalEquations aeq = new TemperatureAndMassEquationsLinear();
        final double startTemp = 0.;
        final double startMass = 0.;

        final double[] addState = { startTemp, startMass };

        // duration after which the fusion is supposed to start :
        final double fusionDuration = TemperatureAndMassEquationsLinear.referenceFusionStart(startTemp);
        // State expected at end of propagation
        final double refDuration = fusionDuration + 200.;
        final double refTemp = TemperatureAndMassEquationsLinear.referenceTemp(startTemp, refDuration);
        final double refMass = TemperatureAndMassEquationsLinear.referenceMass(startTemp, startMass, refDuration);

        // Add the equation to the propagator
        this.propagator.addAdditionalEquations(aeq);
        // Initialize the additional state with tolerances
        final double[] absTols = { 1e-10, 1e-10 };
        final double[] relTols = { 1e-12, 1e-12 };
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), addState);
        // Propagation without custom tolerances
        this.propagator.setAdditionalStateTolerance(aeq.getName(), absTols, relTols);
        // Propagation with custom tolerances
        this.propagator.setInitialState(this.initialState);
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));
        // Get the final additional state
        final double[] ads = this.adsHandler.saveState;

        // Since there are custom tolerances for the additional state,
        // the propagation will become more accurate around the "fusion start" date.
        // Therefore, the mass and temperature will be accurate.

        // System.out.println("Temp : " + ads[7] + " - Mass : " + ads[8]);
        // System.out.println("RefTemp : " + refTemp + " - RefMass : " + refMass);

        VAL.assertEquals(ads[0], refTemp, BETTER_EPSILON_LIN, refTemp, BETTER_EPSILON_LIN, "accurate temperature");
        VAL.assertEquals(ads[1], refMass, BETTER_EPSILON_LIN, refMass, BETTER_EPSILON_LIN, "accurate mass");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ADDITIONAL_STATES_TOLERANCE_VALUES}
     * 
     @testedMethod {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)}
     * 
     * @description Validation test with one custom additional state and no tolerances
     * 
     * @input an additional state : temperature and mass, less linear fusion modelization
     * 
     * @output additional state at the end of the propagation
     * 
     * @testPassCriteria propagation without tolerances for the additional state, expected unaccurate, with epsilon
     *                   AWFUL_EPSILON_LINTRI - no external references for this test.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateLinTriOne() throws PatriusException {
        final double startTemp = 0.;
        final double startMass = 0.;
        final AdditionalEquations aeq = new TemperatureAndMassEquationsLinTri(startTemp, startMass,
            this.initialState.getDate());

        final double[] addState = { startTemp, startMass };

        // duration after which the fusion is supposed to start :
        final double fusionDuration = TemperatureAndMassEquationsLinTri.referenceFusionStart(startTemp);
        // State expected at end of propagation
        final double refDuration = fusionDuration + 200.;
        final double refTemp = TemperatureAndMassEquationsLinTri.referenceTemp(startTemp, refDuration);
        final double refMass = TemperatureAndMassEquationsLinTri.referenceMass(startTemp, startMass, refDuration);

        // Add the equation to the propagator
        this.propagator.addAdditionalEquations(aeq);
        // Initialize the additional state
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), addState);
        // Propagation without custom tolerances
        this.propagator.setInitialState(this.initialState);
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));
        // Get the final additional state
        final double[] ads = this.adsHandler.saveState;

        // Since there are no custom tolerances for the additional state,
        // the propagation will not become more accurate around the "fusion start" date.
        // Therefore, the mass and temperature will NOT be accurate at all!

        // System.out.println("Temp : " + ads[7] + " - Mass : " + ads[8]);
        // System.out.println("RefTemp : " + refTemp + " - RefMass : " + refMass);

        VAL.assertEquals(ads[0], refTemp, AWFUL_EPSILON_LINTRI, refTemp, AWFUL_EPSILON_LINTRI, "unaccurate temperature");
        VAL.assertEquals(ads[1], refMass, AWFUL_EPSILON_LINTRI, refMass, AWFUL_EPSILON_LINTRI, "unaccurate mass");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#ADDITIONAL_STATES_TOLERANCE_VALUES}
     * 
     @testedMethod {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)}
     * 
     * @description Validation test with one custom additional state and tolerances
     * 
     * @input an additional state : temperature and mass, less linear fusion modelization
     * 
     * @output additional state at the end of the propagation
     * 
     * @testPassCriteria propagation with tolerances for the additional state, expected accurate, with epsilon
     *                   BETTER_EPSILON_LINTRI - no external references for this test.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateLinTriTwo() throws PatriusException {

        final double startTemp = 0.;
        final double startMass = 0.;
        final AdditionalEquations aeq = new TemperatureAndMassEquationsLinTri(startTemp, startMass,
            this.initialState.getDate());

        final double[] addState = { startTemp, startMass };

        // duration after which the fusion is supposed to start :
        final double fusionDuration = TemperatureAndMassEquationsLinTri.referenceFusionStart(startTemp);
        // State expected at end of propagation
        final double refDuration = fusionDuration + 200.;
        final double refTemp = TemperatureAndMassEquationsLinTri.referenceTemp(startTemp, refDuration);
        final double refMass = TemperatureAndMassEquationsLinTri.referenceMass(startTemp, startMass, refDuration);

        // Add the equation to the propagator
        this.propagator.addAdditionalEquations(aeq);
        // Initialize the additional state
        // with olerances
        final double[] absTols = { 1e-10, 1e-10 };
        final double[] relTols = { 1e-12, 1e-12 };
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), addState);
        // Propagation without custom tolerances
        this.propagator.setAdditionalStateTolerance(aeq.getName(), absTols, relTols);
        // Propagation with custom tolerances
        this.propagator.setInitialState(this.initialState);
        // Propagation with custom tolerances
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));
        // Get the final additional state
        final double[] ads = this.adsHandler.saveState;

        // Since there are custom tolerances for the additional state,
        // the propagation will become more accurate around the "fusion start" date.
        // Therefore, the mass and temperature will be accurate.

        // System.out.println("Temp : " + ads[7] + " - Mass : " + ads[8]);
        // System.out.println("RefTemp : " + refTemp + " - RefMass : " + refMass);

        VAL.assertEquals(ads[0], refTemp, BETTER_EPSILON_LINTRI, refTemp, BETTER_EPSILON_LINTRI, "accurate temperature");
        VAL.assertEquals(ads[1], refMass, BETTER_EPSILON_LINTRI, refMass, BETTER_EPSILON_LINTRI, "accurate mass");
    }

    /**
     * Setup before class.
     * 
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException, IOException {
        Utils.setDataRoot("regular-dataCNES-2003:potentialCNES/shm-format");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        VAL = new Validate(AdditionalStatesValTest.class);
    }

    /**
     * Setup.
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Before
    public void setUp() throws PatriusException {
        this.mu = 3.9860047e14;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        this.initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.initDate, this.mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        this.initialState = new SpacecraftState(orbit, attitude);
        final double[] absTolerance = {
            1.0e-6, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
        };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
        };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0., 30, absTolerance, relTolerance);
        integrator.setInitialStepSize(30);
        this.propagator = new NumericalPropagator(integrator);
        this.propagator.setInitialState(this.initialState);
        this.adsHandler = new AddStatesStepHandler();
        this.propagator.setMasterMode(this.adsHandler);
    }

    /**
     * Teardown.
     * 
     * @throws PatriusException
     *         should not happen
     */
    @After
    public void tearDown() throws PatriusException {
        this.initDate = null;
        this.initialState = null;
        this.propagator = null;
        this.adsHandler = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Teardown after class.
     * 
     * @throws PatriusException
     *         should not happen
     * @throws URISyntaxException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @AfterClass
    public static void tearDownAfterClass() throws PatriusException, IOException, URISyntaxException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        VAL.produceLog();
    }

    /**
     * Stores the interpolator state during the numerical propagation.
     */
    private final class AddStatesStepHandler implements PatriusStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 6304463427775124657L;
        public double[] saveState;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // Nothing to do
        }

        @Override
        public void
                handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                            throws PropagationException {
            try {
                this.saveState = interpolator.getInterpolatedState().getAdditionalState(
                    TemperatureAndMassEquationsLinTri.KEY);
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }
    }

}
