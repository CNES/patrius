/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history created 28/03/13
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:706:13/12/2016: synchronisation problem with the Assemby mass
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Validation tests for detector based on additional states in {@link NumericalPropagator}.
 * 
 * @author chabaudp
 * 
 * @version $Id: AdditionalStatesDetectorValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 */
public class AdditionalStatesDetectorValTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Detector on additional states
         * 
         * @featureDescription Specific detectors using spacecraftstate and additional state vector
         * 
         * @coveredRequirements DV-PROPAG_60, DV-INTEG_70
         */
        DETECTOR_ON_ADDITIONAL_STATES
    }

    private static Validate VAL;

    private AbsoluteDate initDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;
    private AddStatesStepHandler adsHandler;

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DETECTOR_ON_ADDITIONAL_STATES}
     * 
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link AbstractAdditionalStatesEventDetector#g(SpacecraftState, Map)}
     * 
     * @description a unique event independent from spacecraftState will stop the propagation
     * 
     * @input an additional state : temperature and mass, linear fusion modelization,
     *        a detector of fusion date
     * 
     * @output additional state when the fusion state is detected
     * 
     * @testPassCriteria expected temperature and mass of fusion with {@link FusionStateDetector#DEFAULT_THRESHOLD}
     *                   precision
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateEventDetector() throws PatriusException {

        final AdditionalEquations aeq = new TemperatureAndMassEquationsLinear();
        final double startTemp = 0.;
        final double startMass = 1000;

        final double[] addState = { startTemp, startMass };

        // duration after which the fusion is supposed to start :
        final double fusionDuration = TemperatureAndMassEquationsLinear.referenceFusionStart(startTemp);
        // propagation more than fusionDuration
        final double refDuration = fusionDuration + 200.;
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), addState);
        // Add the equation to the propagator
        this.propagator.addAdditionalEquations(aeq);

        // Initialize the additional state
        // with tolerances
        final double[] absTols = { 1e-10, 1e-10 };
        final double[] relTols = { 1e-12, 1e-12 };
        this.propagator.setAdditionalStateTolerance(aeq.getName(), absTols, relTols);

        // Create and add a detector of fusion state
        final double reference = startMass - TemperatureAndMassEquationsLinear.TFUSION;
        final EventDetector fusionDetector = new FusionStateDetector(aeq.getName(), reference);
        this.propagator.addEventDetector(fusionDetector);
        this.propagator.setInitialState(this.initialState);
        // Propagation
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));

        // Get the final additional state
        final double[] ads = this.adsHandler.saveState;
        // the detection state should be : temperature = TFUSION and mass = 1000 at precision DEFAULT_THRESHOLD
        final double[] addStateExpected = { TemperatureAndMassEquationsLinear.TFUSION, 1000.0 };

        VAL.assertEquals(ads[0], addStateExpected[0], AbstractDetector.DEFAULT_THRESHOLD,
            addStateExpected[0], AbstractDetector.DEFAULT_THRESHOLD, "Temperature");
        VAL.assertEquals(ads[1], addStateExpected[1], AbstractDetector.DEFAULT_THRESHOLD,
            addStateExpected[1], AbstractDetector.DEFAULT_THRESHOLD, "Mass");

    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DETECTOR_ON_ADDITIONAL_STATES}
     * 
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link AbstractAdditionalStatesEventDetector#g(SpacecraftState, Map)}
     * @testedMethod {@link AdditionalStatesEventsLogger#monitorDetector(org.orekit.propagation.events.AdditionalStatesEventDetector)}
     * @testedMethod {@link AdditionalStatesEventsLogger#getLoggedEvents()}
     * 
     * @description Validation test for the logger
     * 
     * @input an additional state and equation : electrical power consumption depending on the PSO
     * 
     * @output log of all detection of passing a value of electrical consumption in decreasing way
     * 
     * @testPassCriteria all the detected events are at expected PSO and expected Electrical consumption
     *                   with MAX_TRESHOLD precision
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAddStateEventDetectorLogger() throws PatriusException {

        final AdditionalEquations aeq = new ElectricalPowerEquation();
        final double initialPower = 100;
        final double powerTodetect = initialPower + 50;

        final double[] addState = { initialPower };
        this.initialState = this.initialState.addAdditionalState(aeq.getName(), addState);
        // propagation duration of five orbital period
        final int numberOfOrbit = 5;
        final double refDuration = numberOfOrbit * this.initialState.getKeplerianPeriod();

        // Add the equation to the propagator
        this.propagator.addAdditionalEquations(aeq);

        // Initialize the additional state
        // with tolerances
        final double[] absTols = { 1e-10 };
        final double[] relTols = { 1e-12 };
        this.propagator.setAdditionalStateTolerance(aeq.getName(), absTols, relTols);

        // Create a nominal power consumption
        final ElectricalPowerDetector electricalPowerDetector =
            new ElectricalPowerDetector(aeq.getName(), powerTodetect,
                EventDetector.DECREASING){
                /** serial uid. */
                private static final long serialVersionUID = 1L;

                @Override
                public Action eventOccurred(final SpacecraftState s,
                                            final boolean increasing, final boolean forward) throws PatriusException {
                    return Action.CONTINUE;
                }
            };

        // the logger
        final EventsLogger adsLog = new EventsLogger();
        this.propagator.addEventDetector(adsLog.monitorDetector(electricalPowerDetector));
        this.propagator.setInitialState(this.initialState);
        // Propagation
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));

        // Compute expected PSO
        final CircularOrbit orbit = new CircularOrbit(this.initialState.getOrbit());
        final double initPso = orbit.getAlphaM();
        final double meanMotion = orbit.getKeplerianMeanMotion();
        final double period = orbit.getKeplerianPeriod();

        final double[] minMax = ElectricalPowerEquation.minMaxexpected(initPso, meanMotion, period, initialPower);
        final double expectedPSO = ElectricalPowerEquation.getPsoReference(powerTodetect, minMax[0], minMax[1],
            meanMotion, EventDetector.DECREASING);

        // validate the number of event detected should be equals to the number of orbit
        final List<LoggedEvent> listAdsEvent = adsLog.getLoggedEvents();
        Assert.assertEquals(numberOfOrbit, listAdsEvent.size());

        // validate each detected event
        for (final LoggedEvent event : listAdsEvent) {

            // compute current event PSO
            final SpacecraftState s = event.getState();
            final CircularOrbit actualOrbit = new CircularOrbit(s.getOrbit());
            final double currentEventPSO = MathUtils.normalizeAngle(actualOrbit.getAlphaM(), FastMath.PI);

            VAL.assertEquals(currentEventPSO, expectedPSO, AbstractDetector.DEFAULT_THRESHOLD,
                expectedPSO, AbstractDetector.DEFAULT_THRESHOLD, "PSO");

            // the detection state should be the detected one
            final double[] addStateExpected = { powerTodetect };
            // Get the current event additional state
            final double[] currentAds = event.getState().getAdditionalStates().get(aeq.getName());
            VAL.assertEquals(currentAds[0], addStateExpected[0],
                AbstractDetector.DEFAULT_THRESHOLD,
                addStateExpected[0], AbstractDetector.DEFAULT_THRESHOLD,
                "Power");
        }
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DETECTOR_ON_ADDITIONAL_STATES}
     * 
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link AbstractAdditionalStatesEventDetector#g(SpacecraftState, Map)}
     * @testedMethod {@link AdditionalStatesEventsLogger#monitorDetector(org.orekit.propagation.events.AdditionalStatesEventDetector)}
     * @testedMethod {@link AdditionalStatesEventsLogger#getLoggedEvents()}
     * 
     * @description Validate the using of the additional states in the logger when there are more additional states than
     *              used in detector
     * 
     * @input additional states and equation for temperature - mass, and power consumption
     * @input additional state detector for fusion and power consumption
     * 
     * @output all the detected event in two loggers.
     * 
     * @testPassCriteria for each power consumption event, validate the expected PSO, Power, Temperature and mass
     *                   expected
     *                   with MAX_THRESHOLD precision
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void multipleAdditionalStatesTest() throws PatriusException {

        // TVT
        // Temperature and mass additional equation
        final AdditionalEquations aeqTempMass = new TemperatureAndMassEquationsLinear();
        final double startTemp = 0.;
        final double startMass = 1000;

        final double[] addStateTempMass = { startTemp, startMass };
        this.initialState = this.initialState.addAdditionalState(aeqTempMass.getName(), addStateTempMass);

        // duration after which the fusion is supposed to start :
        final double fusionDuration = TemperatureAndMassEquationsLinear.referenceFusionStart(startTemp);

        // electrical power equation
        final AdditionalEquations aeqElec = new ElectricalPowerEquation();
        final double initialPower = 100;
        final double powerTodetect = initialPower + 50;

        final double[] addStateElec = { initialPower };
        this.initialState = this.initialState.addAdditionalState(aeqElec.getName(), addStateElec);

        // propagation duration of five orbital period
        final int numberOfOrbit = 5;
        final double refDuration = numberOfOrbit * this.initialState.getKeplerianPeriod();

        // Add the two equations to the propagator
        this.propagator.addAdditionalEquations(aeqTempMass);
        this.propagator.addAdditionalEquations(aeqElec);

        // Initialize the additional states
        // with tolerances
        final double[] absTolsTempMass = { 1e-10, 1e-10 };
        final double[] relTolsTempMass = { 1e-12, 1e-12 };
        final double[] absTolsElec = { 1e-10 };
        final double[] relTolsElec = { 1e-12 };

        this.propagator.setAdditionalStateTolerance(aeqTempMass.getName(), absTolsTempMass, relTolsTempMass);
        this.propagator.setAdditionalStateTolerance(aeqElec.getName(), absTolsElec, relTolsElec);

        // Create a detector of fusion state
        final double reference = startMass - TemperatureAndMassEquationsLinear.TFUSION;
        final FusionStateDetector fusionDetector = new FusionStateDetector(aeqTempMass.getName(), reference){
            /** serial uid */
            private static final long serialVersionUID = 6050388605586127764L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                return Action.CONTINUE;
            }
        };

        // Create a detector of power consumption
        final ElectricalPowerDetector electricalPowerDetector =
            new ElectricalPowerDetector(aeqElec.getName(), powerTodetect,
                EventDetector.DECREASING){
                /** serial uid. */
                private static final long serialVersionUID = 1L;

                @Override
                public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                            final boolean forward) throws PatriusException {
                    return Action.CONTINUE;
                }
            };

        // the fusion event logger
        final EventsLogger adsFusionLog = new EventsLogger();
        this.propagator.addEventDetector(adsFusionLog.monitorDetector(fusionDetector));

        // the electrical event logger
        final EventsLogger adsElecLog = new EventsLogger();
        this.propagator.addEventDetector(adsElecLog.monitorDetector(electricalPowerDetector));

        // Propagation
        this.propagator.setInitialState(this.initialState);
        this.propagator.propagate(this.initialState.getDate().shiftedBy(refDuration));

        // validate the number of electrical event detected should be equals to the number of orbit
        // and only one fusion event
        final List<LoggedEvent> listAdsElecEvent = adsElecLog.getLoggedEvents();
        final List<LoggedEvent> listAdsFusionEvent = adsFusionLog.getLoggedEvents();
        Assert.assertEquals(numberOfOrbit, listAdsElecEvent.size());
        Assert.assertEquals(1, listAdsFusionEvent.size());

        // Compute expected PSO
        final CircularOrbit orbit = new CircularOrbit(this.initialState.getOrbit());
        final double initPso = orbit.getAlphaM();
        final double meanMotion = orbit.getKeplerianMeanMotion();
        final double period = orbit.getKeplerianPeriod();

        final double[] minMax = ElectricalPowerEquation.minMaxexpected(initPso, meanMotion, period, initialPower);
        final double expectedPSO = ElectricalPowerEquation.getPsoReference(powerTodetect, minMax[0], minMax[1],
            meanMotion, EventDetector.DECREASING);

        // Validation : for each electrical events, validate that
        // all additional states are the expected ones
        for (final LoggedEvent event : listAdsElecEvent) {
            // compute current event PSO
            final SpacecraftState s = event.getState();
            final CircularOrbit actualOrbit = new CircularOrbit(s.getOrbit());
            final double currentEventPSO = MathUtils.normalizeAngle(actualOrbit.getAlphaM(), FastMath.PI);

            VAL.assertEquals(currentEventPSO, expectedPSO, AbstractDetector.DEFAULT_THRESHOLD,
                expectedPSO, AbstractDetector.DEFAULT_THRESHOLD, "PSO");

            // the detection state should be the detected one
            final double[] addStateExpected = { powerTodetect };
            // Get the current event additional state
            final double[] currentAds = event.getState().getAdditionalStates().get(aeqElec.getName());
            VAL.assertEquals(currentAds[0], addStateExpected[0],
                AbstractDetector.DEFAULT_THRESHOLD,
                addStateExpected[0], AbstractDetector.DEFAULT_THRESHOLD,
                "Power");

            // if duration is less than fusion duration, the mass should be 1000
            // and if it's more, the temperature should be FUSION_TEMP
            final double[] currentTempMassAds =
                event.getState().getAdditionalStates().get(aeqTempMass.getName());
            if (s.getDate().durationFrom(this.initDate) > fusionDuration) {
                VAL.assertEquals(currentTempMassAds[0], TemperatureAndMassEquationsLinear.TFUSION,
                    AbstractDetector.DEFAULT_THRESHOLD,
                    TemperatureAndMassEquationsLinear.TFUSION,
                    AbstractDetector.DEFAULT_THRESHOLD,
                    "Temperature");
            }
        }
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
        VAL = new Validate(AdditionalStatesDetectorValTest.class);
    }

    /**
     * Setup.
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Before
    public void setUp() throws PatriusException {

        // define a basic keplerian orbit. Notice that default mass is 1000 kg.
        this.initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7.0e6, 0.00001, MathLib.toRadians(5), 0.0, 0.0, 0.0,
            PositionAngle.TRUE,
            FramesFactory.getGCRF(), this.initDate, Constants.EGM96_EARTH_MU);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        this.initialState = new SpacecraftState(orbit, attitude);

        // define basic absolute and relative tolerance
        final double[] absTolerance = {
            1.0e-6, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
        };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
        };

        // define a variable step integrator
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0., 30, absTolerance, relTolerance);
        integrator.setInitialStepSize(30);

        // Initialise the numerical propagator
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
                try {
                    this.saveState = interpolator.getInterpolatedState().getAdditionalState("electrical power");
                } catch (final PatriusException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
