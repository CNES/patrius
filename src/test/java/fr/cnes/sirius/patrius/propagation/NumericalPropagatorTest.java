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
 * @history created 18/03/2015
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
* VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
* VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:469:05/11/2015:Corrected problem with first guess of adaptive step size integrator
 * VERSION::DM:455:05/11/2015:Amélioration de la performance et de la précision d'interpolation de TabulatedAttitude
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * VERSION::FA:469:30/11/2015:Replay testFirstGuess after changes in class AdaptiveStepsizeIntegrator : propagation successful
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:653:02/08/2016:change error estimation
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::DM:684:27/03/2018:add 2nd order RK6 interpolator
 * VERSION::FA:1851:09/10/2018: new test "testLeakFlowFix"
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::DM:1948:14/11/2018: new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018: generalisation of low-level math framework
 * VERSION::FA:XXXX:29/01/2019: Update the testRemoveDetectorMultiplePropagation test
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.AttitudesSequence;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitude;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.DTM2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.US76;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.DTMSolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince54Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.HighamHall54Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisRadiusParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.AnomalyDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.stela.forces.atmospheres.MSIS00Adapter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for Orekit numerical propagator. This class was be set in PATRIUS since test cases requires some PATRIUS
 * objects.
 * 
 * @version $Id$
 * @since 3.1
 */
public class NumericalPropagatorTest {

    /** Counter for test testAnalyticalEphemeris. */
    static int count = 0;

    /**
     * @testType UT
     * 
     * @description check that solar activity data availability is properly checked at the beginning of the propagation.
     * 
     * @testPassCriteria solar activity data availability is properly checked (with an exception if data is not fully available on propagation timespan)
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testDataValidity() throws PatriusException {
        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        final FramesConfiguration configSvg = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(false));
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, FramesFactory.getGCRF());
        final AbsoluteDate start = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate end = start.shiftedBy(Constants.JULIAN_DAY);

        // Simple atmosphere, no solar activity required, no exception should be thrown
        checkPropagationWithDataControl(new SimpleExponentialAtmosphere(earthShape, 0.0004, 42000.0, 7500.0), false);

        // US76 atmosphere, no solar activity required, no exception should be thrown
        checkPropagationWithDataControl(new US76(earthShape), false);

        // MSIS with enough flux and Ap data, no exception should be thrown
        AbsoluteDate minFlux = start.shiftedBy(-41 * Constants.JULIAN_DAY);
        AbsoluteDate maxFlux = end.shiftedBy(41 * Constants.JULIAN_DAY);
        AbsoluteDate minApKp = start.shiftedBy(-60 * 3600);
        AbsoluteDate maxApKp = end.shiftedBy(12 * 3600);
        checkPropagationWithDataControl(new MSISE2000(new ContinuousMSISE2000SolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), earthShape, new MeeusSun()), false);
        
        // MSIS with not enough flux data, an exception should be thrown
        minFlux = start.shiftedBy(-40 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(40 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(-60 * 3600);
        maxApKp = end.shiftedBy(12 * 3600);
        checkPropagationWithDataControl(new MSISE2000(new ContinuousMSISE2000SolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), earthShape, new MeeusSun()), true);
        
        // MSIS with not enough Ap data, an exception should be thrown
        minFlux = start.shiftedBy(-41 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(41 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(-59 * 3600);
        maxApKp = end.shiftedBy(12 * 3600);
        checkPropagationWithDataControl(new MSISE2000(new ContinuousMSISE2000SolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), earthShape, new MeeusSun()), true);

        // MSIS adapter (STELA) with enough flux and Ap data, no exception should be thrown
        minFlux = start.shiftedBy(-41 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(41 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(-60 * 3600);
        maxApKp = end.shiftedBy(12 * 3600);
        checkPropagationWithDataControl(new MSIS00Adapter(new ContinuousMSISE2000SolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), earthShape.getEquatorialRadius(), 0, new MeeusSun()), false);
        
        // MSIS adapter (STELA) with not enough flux data, an exception should be thrown
        minFlux = start.shiftedBy(-40 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(40 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(-60 * 3600);
        maxApKp = end.shiftedBy(12 * 3600);
        checkPropagationWithDataControl(new MSIS00Adapter(new ContinuousMSISE2000SolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), earthShape.getEquatorialRadius(), 0, new MeeusSun()), true);
        
        // MSIS adapter (STELA) with not enough Ap data, an exception should be thrown
        minFlux = start.shiftedBy(-41 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(41 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(-59 * 3600);
        maxApKp = end.shiftedBy(12 * 3600);
        checkPropagationWithDataControl(new MSIS00Adapter(new ContinuousMSISE2000SolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), earthShape.getEquatorialRadius(), 0, new MeeusSun()), true);

        // DTM with enough flux and Kp data, no exception should be thrown
        minFlux = start.shiftedBy(-41 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(41 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(3 * 3600);
        maxApKp = end.shiftedBy(21 * 3600);
        checkPropagationWithDataControl(new DTM2000(new DTMSolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), new MeeusSun(), earthShape), false);
        
        // DTM with not enough flux data, an exception should be thrown
        minFlux = start.shiftedBy(-40 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(40 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(3 * 3600);
        maxApKp = end.shiftedBy(21 * 3600);
        checkPropagationWithDataControl(new DTM2000(new DTMSolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), new MeeusSun(), earthShape), true);
        
        // DTM with not enough Kp data, an exception should be thrown
        minFlux = start.shiftedBy(-41 * Constants.JULIAN_DAY);
        maxFlux = end.shiftedBy(41 * Constants.JULIAN_DAY);
        minApKp = start.shiftedBy(4 * 3600);
        maxApKp = end.shiftedBy(21 * 3600);
        checkPropagationWithDataControl(new DTM2000(new DTMSolarData(getProvider(minFlux, maxFlux, minApKp, maxApKp)), new MeeusSun(), earthShape), true);

        FramesFactory.setConfiguration(configSvg);
    }
    
    /**
     *  Build solar activity data provider valid between min and max date.
     * @param minFlux min flux date
     * @param maxFlux max flux date
     * @param minApKp min Ap/Kp date
     * @param maxApKp max Ap/Kp date
     * @return solar activity data provider valid between min and max date
     */
    private SolarActivityDataProvider getProvider(final AbsoluteDate minFlux, final AbsoluteDate maxFlux,
            final AbsoluteDate minApKp, final AbsoluteDate maxApKp) {
        return new SolarActivityDataProvider() {
            
            @Override
            public AbsoluteDate getMinDate() {
                return minFlux;
            }
            
            @Override
            public AbsoluteDate getMaxDate() {
                return maxFlux;
            }
            
            @Override
            public double getKp(final AbsoluteDate date) throws PatriusException {
                return 3;
            }
            
            @Override
            public SortedMap<AbsoluteDate, Double> getInstantFluxValues(final AbsoluteDate date1,
                    final AbsoluteDate date2) throws PatriusException {
                final SortedMap<AbsoluteDate, Double> res = new TreeMap<AbsoluteDate, Double>();
                res.put(date1, 140.);
                res.put(date2, 140.);
                return res;
            }
            
            @Override
            public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
                return 140;
            }
            
            @Override
            public AbsoluteDate getFluxMinDate() {
                return minFlux;
            }
            
            @Override
            public AbsoluteDate getFluxMaxDate() {
                return maxFlux;
            }
            
            @Override
            public SortedMap<AbsoluteDate, Double[]> getApKpValues(final AbsoluteDate date1,
                    final AbsoluteDate date2) throws PatriusException {
                final SortedMap<AbsoluteDate, Double[]> res = new TreeMap<AbsoluteDate, Double[]>();
                res.put(date1, new Double[] { 15., 3.});
                res.put(date2, new Double[] { 15., 3.});
                return res;
            }
            
            @Override
            public AbsoluteDate getApKpMinDate() {
                return minApKp;
            }
            
            @Override
            public AbsoluteDate getApKpMaxDate() {
                return maxApKp;
            }
            
            @Override
            public double getAp(final AbsoluteDate date) throws PatriusException {
                return 15;
            }

            @Override
            public double getStepApKp() throws PatriusException {
                return 10800;
            }

            @Override
            public double getStepF107() throws PatriusException {
                return 86400;
            }
        };
    }
    
    /**
     * Check an exception is properly thrown or not when checking data at the beginning of the propagation.
     * @param atmosphere atmosphere
     * @param expectedException true if an exception should be expected
     * @throws PatriusException thrown if failed
     */
    private void checkPropagationWithDataControl(final Atmosphere atmosphere, final boolean expectedException) throws PatriusException {
        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        
        // Build assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(1000.), "Main");
        builder.addProperty(new AeroSphereProperty(1., 2.2), "Main");
        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame"));
        final Assembly assembly = builder.returnAssembly();
        final MassModel massModel = new MassModel(assembly);
        
        // Add force
        propagator.addForceModel(new DragForce(atmosphere, new AeroModel(assembly)));

        propagator.setMassProviderEquation(massModel);
        propagator.setInitialState(new SpacecraftState(initialOrbit, massModel));

        // Propagation
        try {
            propagator.propagate(initialDate.shiftedBy(Constants.JULIAN_DAY));
            Assert.assertFalse(expectedException);
        } catch (final PatriusExceptionWrapper e) {
            Assert.assertTrue(expectedException);
        }
    }

    /**
     * @testType UT
     * 
     * @description check that the end of a propagation is performed properly with only one isLast call and without
     *              retropropagation.
     * 
     * @testPassCriteria end date is expected end date, there is no retropolation and there is only one isLast
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testAccurateFinalDatePropagation() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(0L, 0.8960208213694609);
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(5), MathLib.toRadians(3),
            MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE, FramesFactory.getGCRF(),
            initialDate, Constants.WGS84_EARTH_MU);
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator.setInitialState(new SpacecraftState(initialOrbit));

        // First propagation: one detection expected
        final AbsoluteDate finalDate = new AbsoluteDate(1L, 0.0210208213694609);
        propagator.setMasterMode(new PatriusStepHandler(){

            /** Counter to count number of "isLast" calls. */
            int countIsLast = 0;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public
                    void
                    handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                                throws PropagationException {
                final AbsoluteDate date = interpolator.getInterpolatedDate();
                if (date.compareTo(finalDate) > 0) {
                    // Check there is no retropolation
                    Assert.fail();
                }
                if (isLast) {
                    this.countIsLast++;
                }
                // Check that "isLast" is true only once
                Assert.assertTrue(this.countIsLast <= 1);
            }
        });
        final SpacecraftState state = propagator.propagate(finalDate);

        // Check final date is as expected
        Assert.assertTrue(state.getDate().equals(finalDate));
        
        // Test method getSpacecraftState
        final SpacecraftState state2 = propagator.getSpacecraftState(finalDate);
        Assert.assertEquals(state.getPVCoordinates().getPosition().getNorm(), 
                state2.getPVCoordinates().getPosition().getNorm(), Utils.epsilonTest );
    }

    /**
     * @testType UT
     * 
     * @description check that after multiple propagations with same propagator,
     *              once detector is removed, it is not detected on future propagations.
     * 
     * @testPassCriteria only one event is detected
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public void testRemoveDetectorMultiplePropagation() throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(5), MathLib.toRadians(3),
            MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE, FramesFactory.getGCRF(),
            initDate, Constants.WGS84_EARTH_MU);
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator.setInitialState(new SpacecraftState(initialOrbit));

        // Add node detector
        final NodeDetector detector =
            new NodeDetector(FramesFactory.getGCRF(), NodeDetector.ASCENDING, 1.e2, 1.e-4, Action.CONTINUE, true){
                private static final long serialVersionUID = -4087881340627575587L;

                private int count = 0;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    this.count++;
                    // Check that only one occurrence is detected
                    Assert.assertTrue(this.count == 1);
                    return super.eventOccurred(s, increasing, forward);
                }
            };
        propagator.addEventDetector(detector);

        // First propagation: one detection expected
        propagator.propagate(initDate.shiftedBy(3600 * 12));

        propagator.addEventDetector(new DateDetector(AbsoluteDate.J2000_EPOCH));

        // Second propagation: no detection expected
        propagator.propagate(initDate.shiftedBy(3600 * 24));
    }

    /**
     * @testType UT
     * 
     * @description check the "Action.STOP" behavior within a detector, especially if the detector is well
     *              removed when the event occurred and it's expected with its attribute.
     *
     * @testPassCriteria the detector is removed when the event occurred and the remove attribute is set to "true", and
     *                   the detector isn't removed when this attribute is set to "false".
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public void testRemoveStopDetector() throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(5), MathLib.toRadians(3),
            MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE, FramesFactory.getGCRF(),
            initDate, Constants.WGS84_EARTH_MU);
        final NumericalPropagator propagator1 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator1.setInitialState(new SpacecraftState(initialOrbit));
        final NumericalPropagator propagator2 = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator2.setInitialState(new SpacecraftState(initialOrbit));

        // Detector 1 : Should STOP when the event occured, then the detector should be removed
        final EventDetector detector1 = new AnomalyDetector(PositionAngle.TRUE, 0, 1000, 1e-6,
            Action.STOP, true){
            private static final long serialVersionUID = 2938260570832585765L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return super.getAction();
            }

            @Override
            public double g(final SpacecraftState s) throws PatriusException {
                return super.g(s);
            }
        };

        propagator1.addEventDetector(detector1);
        // Add the detector : the propagator should contain 1 detector
        Assert.assertEquals(propagator1.getEventsDetectors().size(), 1);

        propagator1.propagate(initDate.shiftedBy(3600 * 12));

        // The STOP Action during the propagation process should then remove the detector
        Assert.assertEquals(propagator1.getEventsDetectors().size(), 0);

        // Detector 2 : Should STOP when the event occur, then the detector shouldn't be removed
        final EventDetector detector2 = new AnomalyDetector(PositionAngle.TRUE, 0, 1000, 1e-6,
            Action.STOP, false){
            private static final long serialVersionUID = 2938260570832585765L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return super.getAction();
            }

            @Override
            public double g(final SpacecraftState s) throws PatriusException {
                return super.g(s);
            }
        };

        propagator2.addEventDetector(detector2);
        // Add the detector : the propagator should contain 1 detector
        Assert.assertEquals(propagator2.getEventsDetectors().size(), 1);

        propagator2.propagate(initDate.shiftedBy(3600 * 12));

        // The STOP Action during the propagation process shouldn't remove the detector
        Assert.assertEquals(propagator2.getEventsDetectors().size(), 1);
    }

    /**
     * @testType UT
     * 
     * @description check that propagation in some numerically degraded case ends exactly at required date.
     * 
     * @testPassCriteria propagation ends at exactly required date
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testAccurateFinalDatePropagation2() {
        final AbsoluteDate tStart = new AbsoluteDate(9, 0.9303707549150645);
        final AbsoluteDate tEnd = new AbsoluteDate(10, 0.05537075491506447);

        double t1 = tEnd.durationFrom(tStart);
        t1 = FastMath.nextAfter(t1, 0.);
        final double t1p = FastMath.nextAfter(t1, 0.);

        final DormandPrince853Integrator dop = new DormandPrince853Integrator(0., 300., 1e-15, 0.);

        final EventHandler handler = new EventHandler(){

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public int getSlopeSelection() {
                return 0;
            }

            @Override
            public double g(final double t, final double[] y) {
                return t - t1p;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }
        };
        dop.addEventHandler(handler, 1.0, 1e-6, 200);

        final double t0 = 0.;
        final double[] y = new double[] { 0. };
        final FirstOrderDifferentialEquations equations = new FirstOrderDifferentialEquations(){

            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                yDot[0] = FastMath.sin(30000 * t);
            }
        };
        final double t2 = dop.integrate(equations, t0, y, t1, y);

        // Check
        Assert.assertEquals(t1, t2, 0.);
    }

    /**
     * @testType UT
     * 
     * @description test accuracy of RK6 interpolator with respect to DOPRI 8 results for LEO orbit
     * 
     * @testPassCriteria accuracy is as expected ( < 0.2m, reference: DOPRI integration)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testInterpolationRK6() throws PropagationException {

        // Initial date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date,
                Constants.CNES_STELA_MU);

        // Propagation with RK6
        final FirstOrderIntegrator integrator = new RungeKutta6Integrator(10.);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.setInitialState(new SpacecraftState(orbit));
        propagator2.setOrbitType(OrbitType.CARTESIAN);
        propagator2.setEphemerisMode();
        propagator2.propagate(date.shiftedBy(86400));
        final BoundedPropagator ephemeris1 = propagator2.getGeneratedEphemeris();

        // Propagation with RK6 with small time-step (considered as reference ephemeris)
        final FirstOrderIntegrator integrator2 = new RungeKutta6Integrator(1.);
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator2);
        propagator3.setInitialState(new SpacecraftState(orbit));
        propagator3.setOrbitType(OrbitType.CARTESIAN);
        propagator3.setEphemerisMode();
        propagator3.propagate(date.shiftedBy(86400));
        final BoundedPropagator ephemeris2 = propagator3.getGeneratedEphemeris();

        // Check sub-sampled ephemeris are identical
        // Accuracy:
        // - Position: 22cm (120m for linear interpolator)
        // - Velocity: 0.24mm/s
        for (int i = 0; i < 5000; i++) {
            final AbsoluteDate target = AbsoluteDate.J2000_EPOCH.shiftedBy(i);
            final SpacecraftState state2 = ephemeris1.propagate(target);
            final SpacecraftState state3 = ephemeris2.propagate(target);
            final double diffPos =
                state2.getPVCoordinates().getPosition().distance(state3.getPVCoordinates().getPosition());
            final double diffVel =
                state2.getPVCoordinates().getVelocity().distance(state3.getPVCoordinates().getVelocity());
            Assert.assertEquals(0., diffPos, 0.22);
            Assert.assertEquals(0., diffVel, 0.00024);
        }
    }

    /**
     * FT-469.
     * 
     * @testType UT
     * 
     * @description
     *              Test first guess of adaptive stepsize integrators: first guess may be too far in the
     *              future (some data such as attitude data may therefore be missing). Solution is to clamp first guess
     *              value
     *              within integrator min and max steps. In this test case, first guess is 21 days further than last
     *              available
     *              attitude data.
     * 
     * @testPassCriteria Propagation successful (no exception).
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testFirstGuess() throws IllegalArgumentException, IOException, ParseException, PatriusException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Initial orbit
        final AbsoluteDate initDate = new AbsoluteDate(2014, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(24400e3, 0.72, MathLib.toRadians(5), MathLib.toRadians(180),
            MathLib.toRadians(2), MathLib.toRadians(180),
            PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);
        final AttitudeLeg attLeg = new Guidage(FramesFactory.getGCRF(), initDate, 86400);
        final Assembly spacecraft = getSphericalVehicle(1000, 1, 2.3, 0.3, 0.4, 0.3);
        final MassProvider massModel = new MassModel(spacecraft);
        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel, attLeg);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        // Propagation
        try {
            propagator.propagate(initDate.shiftedBy(3600));
            Assert.assertTrue(true);
        } catch (final PropagationException e) {
            Assert.fail();
        }
    }

    /**
     * FT-1422.
     * 
     * @testType UT
     * 
     * @description
     *              Performs a retropropagation with an event at t=0s
     *              Check that ephemeris is properly generated (originally ephemeris generation via step handler
     *              was skipped due to bad retropolation management)
     * 
     * @testPassCriteria ephemeris is properly generated (has the right number of elements)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testRetropolationHandler() throws PatriusException {

        // Patrius Dataset initialization (needed for example to get the UTC time)
        Utils.setDataRoot("regular-dataPBASE");

        // Recovery of the UTC time scale using a "factory" (not to duplicate such unique object)
        final TimeScale TUC = TimeScalesFactory.getUTC();

        // Date of the orbit given in UTC time scale)
        final AbsoluteDate date = new AbsoluteDate("2010-01-01T12:00:00.000", TUC);

        // Getting the frame with wich will defined the orbit parameters
        // As for time scale, we will use also a "factory".
        final Frame GCRF = FramesFactory.getGCRF();

        // Initial orbit
        final double sma = 7200.e+3;
        final double exc = 0.01;
        final double per = sma * (1. - exc);
        final double apo = sma * (1. + exc);
        final double inc = MathLib.toRadians(98.);
        final double pa = MathLib.toRadians(0.);
        final double raan = MathLib.toRadians(0.);
        final double anm = MathLib.toRadians(0.);
        final double MU = Constants.WGS84_EARTH_MU;

        final ApsisRadiusParameters par =
            new ApsisRadiusParameters(per, apo, inc, pa, raan, anm, PositionAngle.MEAN, MU);
        final Orbit iniOrbit = new ApsisOrbit(par, GCRF, date);

        // We create a spacecratftstate
        final SpacecraftState iniState = new SpacecraftState(iniOrbit);

        // Initialization of the Runge Kutta integrator with a 2 s step
        final double pasRk = 2.;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(pasRk);

        // Initialization of the propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(iniState);

        // Forcing integration using cartesian equations
        propagator.setOrbitType(OrbitType.CARTESIAN);

        // Adding attitude sequence
        final AttitudesSequence seqAtt = new AttitudesSequence();

        // Laws to be taken into account in the sequence
        final AttitudeLaw law1 = new ConstantAttitudeLaw(GCRF, new Rotation(RotationOrder.ZYX, 0., 0., 0.));

        // Events that will switch from a law to another
        final double maxCheck = 10.;
        final double threshold = 1.e-3;
        final EventDetector event0 = new DateDetector(date.shiftedBy(0.), maxCheck, threshold, Action.RESET_STATE);

        // Adding switches
        seqAtt.addSwitchingCondition(law1, event0, true, false, law1);

        propagator.setAttitudeProvider(seqAtt);
        seqAtt.registerSwitchEvents(propagator);

        // Creation of a fixed step handler
        final ArrayList<SpacecraftState> listOfStates = new ArrayList<SpacecraftState>();
        final PatriusFixedStepHandler myStepHandler = new PatriusFixedStepHandler(){
            private static final long serialVersionUID = 1L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // Nothing to do ...
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                // Adding S/C to the list
                listOfStates.add(currentState);
            }
        };
        // The handler frequency is set to 60s
        propagator.setMasterMode(60., myStepHandler);

        // Propagating 100s
        final double dt = -3600.;
        final AbsoluteDate finalDate = date.shiftedBy(dt);
        propagator.propagate(finalDate);

        Assert.assertEquals(60, listOfStates.size());
    }

    /**
     * FT-468.
     * 
     * @testType UT
     * 
     * @description
     *              Performs a propagation with maneuvers and analytical propagator in ephemeris mode. Some events are
     *              detected.
     *              Generated ephemeris is then propagated with same event detectors.
     *              Events are expected to be detected and to be the same.
     * 
     * @testPassCriteria Event detected in propagated generated ephemeris.
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testAnalyticalEphemeris() throws IllegalArgumentException, PatriusException, IOException,
                                         ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Propagator
        final AbsoluteDate initDate = new AbsoluteDate("2008-01-01T00:00:00", TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(42164.e3, 0.001, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);

        final AttitudeProvider lofPointingAtt = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        final Assembly spacecraft = getSphericalVehicle(1000, 1, 2.3, 0.3, 0.4, 0.3);
        final MassProvider massModel = new MassModel(spacecraft);

        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, lofPointingAtt);

        final EventDetector visiCentreTerre = new CircularFieldOfViewDetector(CelestialBodyFactory.getEarth(),
            Vector3D.PLUS_J, MathLib.toRadians(3), 60.){
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (!increasing) {
                    count++;
                }
                return Action.CONTINUE;
            }
        };

        final AbsoluteDate tman = initDate.shiftedBy(10.);
        final ImpulseManeuver impulse =
            new ImpulseManeuver(new DateDetector(tman, 60, 1e-6), new Vector3D(-1000, 0, 0),
                FramesFactory.getGCRF(), 300, massModel, "Reservoir1");
        propagator.addEventDetector(impulse);

        // Propagation (analytical)
        propagator.setEphemerisMode();
        propagator.addEventDetector(visiCentreTerre);
        propagator.propagate(tman.shiftedBy(1000));
        Assert.assertEquals(count, 1);

        // Propagation (ephemeris)
        final BoundedPropagator ephemPropag = propagator.getGeneratedEphemeris();
        ephemPropag.addEventDetector(visiCentreTerre);
        ephemPropag.propagate(initDate, tman.shiftedBy(1000));
        Assert.assertEquals(count, 2);
    }

    /**
     * FT-1851.
     * 
     * @testType UT
     * 
     * @description Initially, a numerical error could happened in some cases when the flow rate
     *              changes from a specific value (0,1kg/s for instance). The propagator behavior
     *              has been fixed and this test checks if the mass continue to leak or if it stays
     *              constant after all the thrusts stop firing.
     * 
     * @testPassCriteria The tank mass from the current step compared to the last step's stays
     *                   constant after each of the two thrusts are turned off.
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     */
    @Test
    public void testLeakFlowFix() throws IllegalArgumentException, IOException, ParseException,
                                 PatriusException {

        // ====================== Initialization ======================

        // Test case flow rate value goal
        final double flowRateGoal = 0.1;

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2002, 01, 02, TimeScalesFactory
            .getTAI());

        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(10.);
        final NumericalPropagator numericalPropagator =
            new NumericalPropagator(integrator);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(1000.), "Main");

        final double tankMass = 116.;
        final TankProperty tank = new TankProperty(tankMass);
        builder.addPart("Tank", "Main", Transform.IDENTITY);
        builder.addProperty(tank, "Tank");
        final Assembly assembly = builder.returnAssembly();
        final MassProvider massProviders = new MassModel(assembly);

        final double thrust = 100;
        final double isp = thrust / (Constants.G0_STANDARD_GRAVITY * flowRateGoal);

        final ContinuousThrustManeuver continousThrust1 = new ContinuousThrustManeuver(
            initialDate.shiftedBy(120), 150, new PropulsiveProperty(thrust, isp),
            Vector3D.PLUS_I, massProviders, tank);

        final ContinuousThrustManeuver continousThrust2 = new ContinuousThrustManeuver(
            initialDate.shiftedBy(100), 100, new PropulsiveProperty(thrust * 10, isp),
            Vector3D.PLUS_I, massProviders, tank);

        // Set master mode
        class MyPatriusStepHandler implements PatriusStepHandler {

            private static final long serialVersionUID = 1L;

            // Temporary Tank mass stored from last step
            double tempMass;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                this.tempMass = tankMass;
            }

            @Override
            public
                    void
                    handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                                                                                                throws PropagationException {

                try {
                    /*
                     * String text = interpolator.getInterpolatedState().getDate() + "";
                     * if(continousThrust1.isFiring() || continousThrust2.isFiring()) { text +=
                     * " firing : true"; } else { text += " firing : false"; } text += " " +
                     * interpolator.getInterpolatedState().getMass("Tank");
                     * System.out.println(text);
                     */
                    if (!continousThrust1.isFiring() && !continousThrust2.isFiring()) {
                        // Check if the tank's mass stays constant when the engines aren't firing
                        Assert.assertEquals(interpolator.getInterpolatedState().getMass("Tank"),
                            this.tempMass, 0.);
                    }

                    this.tempMass = interpolator.getInterpolatedState().getMass("Tank");

                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        }
        ;

        final PatriusStepHandler satStepHandler = new MyPatriusStepHandler();
        numericalPropagator.setMasterMode(satStepHandler);

        final AttitudeProvider attitudeProvider =
            new ConstantAttitudeLaw(FramesFactory.getCIRF(), Rotation.IDENTITY);

        final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0.001, 1.5, 0, 0, 10,
            PositionAngle.MEAN, FramesFactory.getGCRF(), initialDate, Constants.EGM96_EARTH_MU);
        final SpacecraftState initialState =
            new SpacecraftState(initialOrbit, massProviders);

        numericalPropagator.setInitialState(initialState);
        numericalPropagator.setAttitudeProvider(attitudeProvider);
        numericalPropagator.setMassProviderEquation(massProviders);

        // Add maneuver
        numericalPropagator.addForceModel(continousThrust1);
        numericalPropagator.addForceModel(continousThrust2);

        // ====================== Propagation ======================

        final AbsoluteDate finalDate = initialDate.shiftedBy(1000);

        numericalPropagator.propagate(finalDate);
    }

    /**
     * FT-653.
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @description
     *              Check that a propagation with or without partial derivatives returns exactly the same PV
     *              coordinates.
     *              This means adaptive stepsize integrators do not take into account partial derivatives (abs tol =
     *              +inf, rel tol = 0)
     *              when estimating error. Test is performed with DormandPrince853Integrator, DormandPrince54Integrator
     *              and
     *              HighamHall54Integrator integrators.
     * 
     * @testPassCriteria PV coordinates are exactly the same with or without partial derivatives.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testPropagationErrorEstimation() throws PatriusException {
        // Intializations
        Utils.setDataRoot("regular-dataPBASE");
        final double[] vecAbsoluteTolerance = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] vecRelativeTolerance = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };

        // Tests all embedded Runge Kutta integrators
        this.testErrorEstimation(new DormandPrince853Integrator(0.01, 500., vecAbsoluteTolerance, vecRelativeTolerance));
        this.testErrorEstimation(new DormandPrince54Integrator(0.01, 500., vecAbsoluteTolerance, vecRelativeTolerance));
        this.testErrorEstimation(new HighamHall54Integrator(0.01, 500., vecAbsoluteTolerance, vecRelativeTolerance));
    }

    /**
     * Generic test for error estimation.
     * 
     * @param integrator
     *        integrator
     */
    private void testErrorEstimation(final FirstOrderIntegrator integrator) throws PatriusException {
        // Initial orbit
        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        SpacecraftState initialState = new SpacecraftState(orbit);

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(1100.), "Main");
        builder.addProperty(new RadiativeSphereProperty(1.), "Main");
        builder.addProperty(new RadiativeProperty(0.5, 0.5, 0.5), "Main");
        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame"));
        final Assembly assembly = builder.returnAssembly();

        // Propagation
        final NumericalPropagator p = new NumericalPropagator(integrator);
        p.setOrbitType(OrbitType.CARTESIAN);

        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        final CelestialBody sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);
        final RadiationSensitive radiativeModel = new DirectRadiativeModel(assembly);
        p.addForceModel(new SolarRadiationPressureCircular(sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            radiativeModel, true));

        // No DP
        p.setInitialState(initialState);
        final PVCoordinates pvNoDP = p.propagate(date.shiftedBy(86400.)).getPVCoordinates();

        // With DP
        final PartialDerivativesEquations pde = new PartialDerivativesEquations("", p);
        initialState = pde.setInitialJacobians(initialState);
        p.setInitialState(initialState);
        final PVCoordinates pvDP = p.propagate(date.shiftedBy(86400.)).getPVCoordinates();

        // Check
        Assert.assertEquals(0., pvNoDP.getPosition().subtract(pvDP.getPosition()).getNorm(), 0.);
        Assert.assertEquals(0., pvNoDP.getVelocity().subtract(pvDP.getVelocity()).getNorm(), 0.);
    }

    public static Assembly getSphericalVehicle(final double mass, final double radius, final double DragCoef,
                                               final double ka, final double ks,
                                               final double kd) throws PatriusException {

        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);

        // MASSES : 50% BODY, 30% Reservoir1, 20% Reservoir2
        builder.addProperty(new MassProperty(0.5 * mass), "BODY");
        builder.addProperty(new MassProperty(0.3 * mass), "Reservoir1");
        builder.addProperty(new MassProperty(0.2 * mass), "Reservoir2");
        builder.addProperty(new AeroSphereProperty(1., DragCoef), "BODY");
        builder.addProperty(new RadiativeSphereProperty(radius), "BODY");
        builder.addProperty(new RadiativeProperty(ka, ks, kd), "BODY");

        return builder.returnAssembly();
    }

    /**
     * Attitude guidance.
     */
    private class Guidage implements AttitudeProvider, AttitudeLeg {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        /** TabulatedAttitude which provides Attitude */
        private TabulatedAttitude tabulatedAttitude;

        /**
         * Constructor
         */
        public Guidage(final Frame inertialFrame, final AbsoluteDate refDate, final double duration) {
            final Attitude att1 = new Attitude(refDate, inertialFrame, Rotation.IDENTITY, Vector3D.ZERO);
            final Attitude att2 =
                new Attitude(refDate.shiftedBy(duration), inertialFrame, Rotation.IDENTITY, Vector3D.ZERO);
            final ArrayList<Attitude> quatList = new ArrayList<Attitude>();
            quatList.add(att1);
            quatList.add(att2);
            try {
                this.tabulatedAttitude = new TabulatedAttitude(quatList, 2);
            } catch (final PatriusException e) {
                // should not happen
            }
        }

        /**
         * {@inheritDoc} WARNING : the parameter <b> pvProv </b> is not used in EOR_Guidance
         */
        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                final Frame frame) throws PatriusException {
            return this.tabulatedAttitude.getAttitude(null, date, frame);
        }

        @Override
        public AbsoluteDateInterval getTimeInterval() {
            return this.tabulatedAttitude.getTimeInterval();
        }

        @Override
        public Attitude getAttitude(final Orbit orbit) throws PatriusException {
            return null;
        }

        @Override
        public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        }

        @Override
        public String getNature() {
            return "";
        }

        @Override
        public TabulatedAttitude copy(final AbsoluteDateInterval newInterval) {
            return this.tabulatedAttitude.copy(newInterval);
        }
    }

    /**
     * Numerical propagator.
     */
    private class NumTestPropagator extends NumericalPropagator {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        /**
         * Numerical propagator.
         */
        public NumTestPropagator(final Orbit initialOrbit, final Assembly spacecraft, final MassProvider massProvider,
            final AttitudeProvider attProv) throws PatriusException, IOException, ParseException {
            super(new DormandPrince853Integrator(1e-6, 500, new double[] { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 },
                new double[] { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 }));

            // Mass
            this.setMassProviderEquation(massProvider);
            this.setAdditionalStateTolerance("MASS_BODY", new double[] { 1e-06 }, new double[] { 1e-09 });
            this.setAdditionalStateTolerance("MASS_Reservoir1", new double[] { 1e-06 }, new double[] { 1e-09 });
            this.setAdditionalStateTolerance("MASS_Reservoir2", new double[] { 1e-06 }, new double[] { 1e-09 });

            // Forces
            final JPLEphemeridesLoader loader = this.initJPLLoader();
            final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);
            final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);
            this.addForceModel(new ThirdBodyAttraction(sun));
            this.addForceModel(new ThirdBodyAttraction(moon));
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765, FramesFactory.getITRF());
            final RadiationSensitive radiativeModel = new DirectRadiativeModel(spacecraft);
            this.addForceModel(new SolarRadiationPressureCircular(sun, earth.getEquatorialRadius(), radiativeModel));

            // Attitude
            this.setAttitudeProvider(attProv);
            final Attitude initAtt = attProv.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());

            // Initial state
            this.setInitialState(new SpacecraftState(initialOrbit, initAtt, massProvider));
        }

        /**
         * Initializes the Celestial Body Factory.
         */
        private JPLEphemeridesLoader initJPLLoader() throws PatriusException {
            CelestialBodyFactory.clearCelestialBodyLoaders();
            final JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
                JPLEphemeridesLoader.EphemerisType.SUN);
            final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
                JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
            final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader("unxp2000.405",
                JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);
            CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
            CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
            return loader;
        }
    }
}
