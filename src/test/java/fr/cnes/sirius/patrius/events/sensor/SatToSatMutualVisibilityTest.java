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
 * @history creation 30/05/2012
 *
 * HISTORY
 * VERSION:4.13.4:FA:FA-346:10/06/2024:[PATRIUS] Problème dans l’utilisation du
 * SatToSatMutualVisibilityDetector en mode de propagation MULTI
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3245:03/11/2022:[PATRIUS] Ajout du sens de propagation du signal dans ...
 * VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2955:15/11/2021:[PATRIUS] Tests manquants pour certains evenements avec prise en compte du temps de propagation 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:300:22/04/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::FA:1308:12/09/2017:correct Ellipsoid non-convergence issue
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.SatToSatMutualVisibilityDetector;
import fr.cnes.sirius.patrius.events.detectors.SatToSatMutualVisibilityDetector.LinkType;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plate;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvLagrange;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.PVCoordinatesPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * Test class for mutual spacecraft visibility detector
 * </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SatToSatMutualVisibilityTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle mutual spacecraft to spacecraft visibility
         * 
         * @featureDescription Detector for the mutual spacecraft to spacecraft visibility : the
         *                     main spacecraft's sensor sees the secondary one while reciprocally.
         * 
         * @coveredRequirements DV-VISI_10, DV-VISI_20, DV-VISI_30, DV-VISI_40, DV-EVT_160,
         *                      DV-EVT_150
         */
        MUTUAL_SPACECRAFT_VISIBILITY
    }

    /** First state Id. */
    private static final String STATE1 = "state1";

    /** Second state Id. */
    private static final String STATE2 = "state2";

    /** Solar panel. */
    private static final String SOLAR_PANEL = "solarPanel";

    /** Epsilon for dates comparison. */
    private final double datesComparisonEpsilon = 1.0e-3;

    /** Frame */
    private Frame eme2000Frame;

    /** Initial date */
    private AbsoluteDate date;

    /** Main orbit */
    private Orbit mainOrbit;

    /** Secondary orbit */
    private Orbit secondaryOrbit;

    /** Attitude provider */
    private AttitudeProvider attitudeProv;

    /** Period */
    private double period;

    /** Spacecraft sensor model 1 */
    private SensorModel mainSpacecraftSensorModel1;

    /** Spacecraft sensor model 2 */
    private SensorModel mainSpacecraftSensorModel2;

    /** Spacecraft sensor model 3 */
    private SensorModel mainSpacecraftSensorModel3;

    /** Spacecraft sensor model 1 */
    private SensorModel secondarySpacecraftSensorModel1;

    /** Spacecraft sensor model 2 */
    private SensorModel secondarySpacecraftSensorModel2;

    /** Spacecraft sensor model 3 */
    private SensorModel secondarySpacecraftSensorModel3;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MUTUAL_SPACECRAFT_VISIBILITY}
     * 
     * @testedMethod {@link SatToSatMutualVisibilityDetector#g(SpacecraftState)}
     * @testedMethod {@link SatToSatMutualVisibilityDetector#eventOccurred(SpacecraftState, boolean)}
     * 
     * @description Test of the "mutual spacecraft visibility" detector during two keplerian orbits.
     *              Test in a single spacecraft propagation mode.
     * 
     * @input Two sensor models, two SpacecraftState
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria the g function has the expected sign
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public void mutualVisibilityTest() throws PatriusException {
        // Initialization
        Utils.setDataRoot("regular-data");

        Propagator mainPropagator = new KeplerianPropagator(this.mainOrbit, this.attitudeProv);
        Propagator secondaryPropagator = new KeplerianPropagator(this.secondaryOrbit, this.attitudeProv);

        // FIRST TEST
        // ============
        // main spacecraft leaves first the sensor of the second one

        // event detector with first sensors
        final double maxCheck = 1.;
        final double threshold = 10.e-10;
        final EventDetector detector1 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, secondaryPropagator,
            false, maxCheck, threshold);
        mainPropagator.addEventDetector(detector1);

        // time to be detected
        final double timeDetected = this.period / 4.;

        // test
        SpacecraftState endState = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // FIRST TEST - BIS: downlink case (default case)
        // ================
        // main spacecraft leaves first the sensor of the second one, light speed is taken into account
        mainPropagator = new KeplerianPropagator(this.mainOrbit, this.attitudeProv);
        secondaryPropagator = new KeplerianPropagator(this.secondaryOrbit, this.attitudeProv);

        // event detector with first sensors
        final SatToSatMutualVisibilityDetector detector5 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, secondaryPropagator,
            false, maxCheck, threshold);
        mainPropagator.addEventDetector(detector5);
        detector5.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, this.eme2000Frame);

        // test
        final SpacecraftState endState2 = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState2.getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // FIRST TEST - TER: uplink case
        // ================
        // main spacecraft leaves first the sensor of the second one, light speed is taken into account
        mainPropagator = new KeplerianPropagator(this.mainOrbit, this.attitudeProv);
        secondaryPropagator = new KeplerianPropagator(this.secondaryOrbit, this.attitudeProv);

        // event detector with first sensors
        final SatToSatMutualVisibilityDetector detector6 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, secondaryPropagator,
            false, maxCheck, threshold, Action.CONTINUE, Action.STOP, false, false, LinkType.SECONDARY_TO_MAIN);
        mainPropagator.addEventDetector(detector6);
        detector6.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, this.eme2000Frame);

        // test
        final SpacecraftState endState3 = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState3.getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // SECOND TEST
        // ============
        // second spacecraft leaves first the sensor of the main one

        // propagators reset
        mainPropagator = new KeplerianPropagator(this.mainOrbit, this.attitudeProv);
        secondaryPropagator = new KeplerianPropagator(this.secondaryOrbit, this.attitudeProv);

        // event detector with second sensors
        final EventDetector detector2 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel2, this.secondarySpacecraftSensorModel1,
            secondaryPropagator, false, maxCheck, threshold);
        mainPropagator.addEventDetector(detector2);

        // test
        endState = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // THRID TEST
        // ===========
        // a masking occurs during mutual visibility

        // propagators reset
        mainPropagator = new KeplerianPropagator(this.mainOrbit, this.attitudeProv);
        secondaryPropagator = new KeplerianPropagator(this.secondaryOrbit, this.attitudeProv);

        // masking panel adding
        final String[] maskingParts = { SOLAR_PANEL };
        this.mainSpacecraftSensorModel3.addOwnMaskingParts(maskingParts);

        // event detector with second sensors
        // the fields are a bit larger than PI/4, so that without
        // masking detection, the end of the visibility is a bbit after T/4
        // But the masking make it happen at T/4.

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        this.secondarySpacecraftSensorModel3.setMainTarget(this.mainSpacecraftSensorModel3, radius);
        this.mainSpacecraftSensorModel3.setMainTarget(this.secondarySpacecraftSensorModel3, radius);

        final EventDetector detector3 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel3, this.secondarySpacecraftSensorModel3,
            secondaryPropagator, true, maxCheck, threshold);
        mainPropagator.addEventDetector(detector3);

        // test
        endState = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endState.getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // FOURTH TEST
        // ===========
        // Detector with defined actions to be performed
        final SatToSatMutualVisibilityDetector detector4 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel3, this.secondarySpacecraftSensorModel3,
            secondaryPropagator, true, maxCheck, threshold, Action.CONTINUE, Action.RESET_STATE);
        Assert.assertEquals(true, detector4.isMaskingCheck());
        Assert.assertEquals(this.mainSpacecraftSensorModel3.hashCode(), detector4
            .getSensorMainSpacecraft().hashCode());
        Assert.assertEquals(this.secondarySpacecraftSensorModel3.hashCode(), detector4
            .getSensorSecondarySpacecraft().hashCode());
        Assert.assertEquals(this.mainSpacecraftSensorModel3.getAssembly().hashCode(), detector4
            .getMainSpacecraft().hashCode());
        Assert.assertEquals(this.secondarySpacecraftSensorModel3.getAssembly().hashCode(),
            detector4.getSecondarySpacecraft().hashCode());
        Assert.assertEquals(Action.CONTINUE, detector4.eventOccurred(endState, true, true));
        Assert.assertEquals(Action.CONTINUE, detector4.eventOccurred(endState, true, false));
        Assert.assertEquals(Action.RESET_STATE, detector4.eventOccurred(endState, false, true));
        Assert.assertEquals(Action.RESET_STATE, detector4.eventOccurred(endState, false, false));
        // Copy test
        final SatToSatMutualVisibilityDetector detectorCopy = (SatToSatMutualVisibilityDetector) detector4
            .copy();
        Assert.assertEquals(detector4.getInMainSpacecraftId(), detectorCopy.getInMainSpacecraftId());
    }

    /**
     * 
     * 
     * @throws PatriusException
     */
    @Test
    public void multiMutualVisibilityLightSpeedTest() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-data");

        // Orbits and propagators initialization
        final Attitude mainAttitude = this.attitudeProv.getAttitude(this.mainOrbit, this.date, this.eme2000Frame);
        final SpacecraftState mainState = new SpacecraftState(this.mainOrbit, mainAttitude);
        final Attitude secondaryAttitude = this.attitudeProv.getAttitude(this.secondaryOrbit, this.date,
            this.eme2000Frame);
        final SpacecraftState secondaryState = new SpacecraftState(this.secondaryOrbit,
            secondaryAttitude);

        // Create the main multinumerical propagator
        final FirstOrderIntegrator integratorMultiSat = new DormandPrince853Integrator(.1, 60,
            1e-9, 1e-9);
        MultiNumericalPropagator mainPropagator = new MultiNumericalPropagator(integratorMultiSat);
        mainPropagator.addInitialState(mainState, STATE1);
        mainPropagator.addInitialState(secondaryState, STATE2);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainState.getMu())), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(secondaryState.getMu())),
            STATE2);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE1);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE2);

        // event detector with first sensors
        final double maxCheck1 = 0.01;
        final double threshold1 = 10.e-10;

        // time to be detected
        final double timeDetected = this.period / 4.;

        // event detector with first sensors
        final SatToSatMutualVisibilityDetector detector = new SatToSatMutualVisibilityDetector(
            STATE1, STATE2, this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, false,
            maxCheck1, threshold1);
        detector.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED,
            mainState.getFrame().getFirstCommonPseudoInertialAncestor(secondaryState.getFrame()));
        mainPropagator.addEventDetector(detector);

        Assert.assertEquals(STATE1, detector.getInMainSpacecraftId());
        Assert.assertEquals(STATE2, detector.getInSecondarySpacecraftId());

        // test
        Map<String, SpacecraftState> otherEndStates = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        final double durationLightSpeed = otherEndStates.get(STATE1).getDate().durationFrom(this.date);
        Assert.assertEquals(timeDetected, durationLightSpeed,
            this.datesComparisonEpsilon);
    }
    
    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MUTUAL_SPACECRAFT_VISIBILITY}
     * 
     * @testedMethod {@link SatToSatMutualVisibilityDetector#g(Map)}
     * @testedMethod {@link SatToSatMutualVisibilityDetector#eventOccurred(Map, boolean, boolean)}
     * @testedMethod {@link SatToSatMutualVisibilityDetector#getInMainSpacecraftId()}
     * @testedMethod {@link SatToSatMutualVisibilityDetector#getInSecondarySpacecraftId()}
     * 
     * @description Test of the "mutual spacecraft visibility" detector during two keplerian orbits.
     *              Test in a multi spacecraft propagation mode.
     * 
     * @input Two sensor models, two SpacecraftState
     * 
     * @output dates of the detected events are the expected ones.
     * 
     * @testPassCriteria the g function has the expected sign
     * @throws PatriusException if a frame problem occurs
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void multiMutualVisibilityTest() throws PatriusException {

        // Orbits and propagators initialization
        final Attitude mainAttitude = this.attitudeProv.getAttitude(this.mainOrbit, this.date, this.eme2000Frame);
        final SpacecraftState mainState = new SpacecraftState(this.mainOrbit, mainAttitude);
        final Attitude secondaryAttitude = this.attitudeProv.getAttitude(this.secondaryOrbit, this.date,
            this.eme2000Frame);
        final SpacecraftState secondaryState = new SpacecraftState(this.secondaryOrbit,
            secondaryAttitude);

        final FirstOrderIntegrator integratorMultiSat = new DormandPrince853Integrator(.1, 60,
            1e-9, 1e-9);
        MultiNumericalPropagator mainPropagator = new MultiNumericalPropagator(integratorMultiSat);
        mainPropagator.addInitialState(mainState, STATE1);
        mainPropagator.addInitialState(secondaryState, STATE2);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainState.getMu())), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(secondaryState.getMu())),
            STATE2);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE1);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE2);

        // FIRST TEST
        // ============
        // main spacecraft leaves first the sensor of the second one

        // event detector with first sensors
        final double maxCheck = 0.01;
        final double threshold = 10.e-10;
        final SatToSatMutualVisibilityDetector detector1 = new SatToSatMutualVisibilityDetector(
            STATE1, STATE2, this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, false,
            maxCheck, threshold);
        mainPropagator.addEventDetector(detector1);

        // Test id getters
        Assert.assertEquals(STATE1, detector1.getInMainSpacecraftId());
        Assert.assertEquals(STATE2, detector1.getInSecondarySpacecraftId());

        // time to be detected
        final double timeDetected = this.period / 4.;

        // test
        Map<String, SpacecraftState> endStates = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endStates.get(STATE1).getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // SECOND TEST
        // ============
        // second spacecraft leaves first the sensor of the main one

        // propagators reset
        mainPropagator = new MultiNumericalPropagator(integratorMultiSat);
        mainPropagator.addInitialState(mainState, STATE1);
        mainPropagator.addInitialState(secondaryState, STATE2);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator
            .getInitialStates().get(STATE1).getMu())), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator
            .getInitialStates().get(STATE2).getMu())), STATE2);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE1);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE2);

        // event detector with second sensors
        final MultiEventDetector detector2 = new SatToSatMutualVisibilityDetector(STATE1, STATE2,
            this.mainSpacecraftSensorModel2, this.secondarySpacecraftSensorModel1, false, maxCheck,
            threshold);
        mainPropagator.addEventDetector(detector2);

        // test
        endStates = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endStates.get(STATE1).getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // THRID TEST
        // ===========
        // a masking occurs during mutual visibility

        // propagators reset
        mainPropagator = new MultiNumericalPropagator(integratorMultiSat);
        mainPropagator.addInitialState(mainState, STATE1);
        mainPropagator.addInitialState(secondaryState, STATE2);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator
            .getInitialStates().get(STATE1).getMu())), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainPropagator
            .getInitialStates().get(STATE2).getMu())), STATE2);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE1);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE2);

        // masking panel adding
        final String[] maskingParts = { SOLAR_PANEL };
        this.mainSpacecraftSensorModel3.addOwnMaskingParts(maskingParts);

        // event detector with second sensors
        // the fields are a bit larger than PI/4, so that without
        // masking detection, the end of the visibility is a bbit after T/4
        // But the masking make it happen at T/4.

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        this.secondarySpacecraftSensorModel3.setMainTarget(this.mainSpacecraftSensorModel3, radius);
        this.mainSpacecraftSensorModel3.setMainTarget(this.secondarySpacecraftSensorModel3, radius);

        final MultiEventDetector detector3 = new SatToSatMutualVisibilityDetector(STATE1, STATE2,
            this.mainSpacecraftSensorModel3, this.secondarySpacecraftSensorModel3, true, maxCheck,
            threshold);
        mainPropagator.addEventDetector(detector3);

        // test
        endStates = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(timeDetected, endStates.get(STATE1).getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);

        // FOURTH TEST
        // ===========
        // Detector with defined actions to be performed
        final SatToSatMutualVisibilityDetector detector4 = new SatToSatMutualVisibilityDetector(
            STATE1, STATE2, this.mainSpacecraftSensorModel3, this.secondarySpacecraftSensorModel3, true,
            maxCheck, threshold, Action.CONTINUE, Action.RESET_STATE);
        Assert.assertEquals(true, detector4.isMaskingCheck());
        Assert.assertEquals(this.mainSpacecraftSensorModel3.hashCode(), detector4
            .getSensorMainSpacecraft().hashCode());
        Assert.assertEquals(this.secondarySpacecraftSensorModel3.hashCode(), detector4
            .getSensorSecondarySpacecraft().hashCode());
        Assert.assertEquals(this.mainSpacecraftSensorModel3.getAssembly().hashCode(), detector4
            .getMainSpacecraft().hashCode());
        Assert.assertEquals(this.secondarySpacecraftSensorModel3.getAssembly().hashCode(), detector4
            .getSecondarySpacecraft().hashCode());
        Assert.assertEquals(Action.CONTINUE,
            detector4.eventOccurred(endStates.get(STATE1), true, true));
        Assert.assertEquals(Action.CONTINUE,
            detector4.eventOccurred(endStates.get(STATE1), true, false));
        Assert.assertEquals(Action.RESET_STATE,
            detector4.eventOccurred(endStates.get(STATE1), false, true));
        Assert.assertEquals(Action.RESET_STATE,
            detector4.eventOccurred(endStates.get(STATE1), false, false));

        // FIFTH TEST
        // Test to cover resetStates method
        // ============
        // propagators reset
        mainPropagator = new MultiNumericalPropagator(integratorMultiSat);
        mainPropagator.addInitialState(mainState, STATE1);
        mainPropagator.addInitialState(secondaryState, STATE2);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE1);
        mainPropagator.setAttitudeProvider(this.attitudeProv, STATE2);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(mainState.getMu())), STATE1);
        mainPropagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(secondaryState.getMu())),
            STATE2);

        // main spacecraft leaves first the sensor of the second one
        final MultiEventDetector detector5 = new SatToSatMutualVisibilityDetector(STATE1, STATE2,
            this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, false, maxCheck,
            threshold, Action.CONTINUE, Action.RESET_STATE);
        mainPropagator.addEventDetector(detector5);

        // test
        endStates = mainPropagator.propagate(this.date.shiftedBy(10000.0));

        Assert.assertEquals(10000.0, endStates.get(STATE1).getDate().durationFrom(this.date),
            this.datesComparisonEpsilon);
    }

    /**
     * @throws PropagationException
     * @testType UT
     * 
     * @testedFeature {@link features#MUTUAL_SPACECRAFT_VISIBILITY}
     * 
     * @testedMethod {@link SatToSatMutualVisibilityDetector#g(Map)}
     * @testedMethod {@link SatToSatMutualVisibilityDetector#g(SpacecraftState)}
     * 
     * @testPassCriteria exception raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testExceptions() throws PropagationException {
        final double maxCheck = 1.;
        final double threshold = 10.e-10;
        final Propagator secondaryPropagator = new KeplerianPropagator(this.secondaryOrbit, this.attitudeProv);
        final SatToSatMutualVisibilityDetector detector1 = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, secondaryPropagator,
            false, maxCheck, threshold);

        final Map<String, SpacecraftState> states = new HashMap<>();

        boolean testOk = false;
        try {
            detector1.g(states);
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.MONO_MULTI_DETECTOR.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);

        final SatToSatMutualVisibilityDetector detector2 = new SatToSatMutualVisibilityDetector(
            STATE1, STATE2, this.mainSpacecraftSensorModel1, this.secondarySpacecraftSensorModel2, false,
            maxCheck, threshold);

        testOk = false;
        try {
            detector2.g(new SpacecraftState(this.mainOrbit));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.MONO_MULTI_DETECTOR.getSourceString(),
                e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    /**
     * Initializations
     */
    @Before
    public void setUp() {
        Utils.clear();
        // Orbits and propagators initialization
        this.date = AbsoluteDate.J2000_EPOCH;
        this.eme2000Frame = FramesFactory.getEME2000();

        this.attitudeProv = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final double a = 7500000.0;
        this.mainOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, 0.0, 0.0,
            PositionAngle.TRUE, this.eme2000Frame, this.date, Utils.mu);
        this.secondaryOrbit = new KeplerianOrbit(a, 0.0, MathUtils.HALF_PI, 0.0, FastMath.PI, 0.0,
            PositionAngle.TRUE, this.eme2000Frame, this.date, Utils.mu);
        this.period = 2.0 * FastMath.PI * MathLib.sqrt(a * a * a / Utils.mu);

        // building the MAIN assembly
        // =======================
        final String mainBody = "mainBody";
        final String secondPart = "secondPart";
        final String thirdPart = "thirdPart";
        final Vector3D translationMain = new Vector3D(-50.0, 50.0, 0.0);
        final AssemblyBuilder builder = new AssemblyBuilder();

        // sensors
        // main part field
        final String name = "circularField";
        final Vector3D mainFieldDirection = Vector3D.MINUS_J;
        final IFieldOfView mainField = new CircularField(name, 5. * FastMath.PI / 8.0,
            mainFieldDirection);

        // main part sensor property creation
        final SensorProperty sensorProperty = new SensorProperty(mainFieldDirection);
        sensorProperty.setMainFieldOfView(mainField);

        // second part field
        final String name2 = "circularField2";
        final Vector3D mainFieldDirection2 = Vector3D.MINUS_J;
        final IFieldOfView mainField2 = new CircularField(name2, FastMath.PI / 4.0,
            mainFieldDirection2);

        // second part sensor property creation
        final SensorProperty sensorProperty2 = new SensorProperty(mainFieldDirection2);
        sensorProperty2.setMainFieldOfView(mainField2);

        // third part field
        final String nameThird = "circularField";
        final Vector3D mainFieldDirection6 = Vector3D.MINUS_J;
        final IFieldOfView mainField6 = new CircularField(nameThird, 3. * FastMath.PI / 8.0,
            mainFieldDirection6);

        // third part sensor property creation
        final SensorProperty sensorProperty6 = new SensorProperty(mainFieldDirection6);
        sensorProperty6.setMainFieldOfView(mainField6);

        // solar panel for maskings geometry property creation
        final Vector3D solarPanelCenter = new Vector3D(21., -2., 0.);
        final SolidShape solarPanelShape = new Plate(solarPanelCenter, Vector3D.PLUS_I,
            Vector3D.PLUS_K, 40., 20.);
        final GeometricProperty solarPanelGeom = new GeometricProperty(solarPanelShape);

        // assembly building
        try {
            // add main part
            builder.addMainPart(mainBody);

            // add second part
            builder.addPart(secondPart, mainBody, translationMain, Rotation.IDENTITY);

            // add second part
            builder.addPart(thirdPart, mainBody, Vector3D.ZERO, Rotation.IDENTITY);

            // add sensors
            builder.addProperty(sensorProperty, mainBody);
            builder.addProperty(sensorProperty2, secondPart);
            builder.addProperty(sensorProperty6, thirdPart);

            // add solar panel
            builder.addPart(SOLAR_PANEL, mainBody, Vector3D.PLUS_I, Rotation.IDENTITY);
            builder.addProperty(solarPanelGeom, SOLAR_PANEL);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(this.eme2000Frame, Transform.IDENTITY,
                "mainFrame");
            builder.initMainPartFrame(mainFrame);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }
        final Assembly mainAssembly = builder.returnAssembly();

        // spacecraft sensor model
        this.mainSpacecraftSensorModel1 = new SensorModel(mainAssembly, mainBody);
        this.mainSpacecraftSensorModel2 = new SensorModel(mainAssembly, secondPart);
        this.mainSpacecraftSensorModel3 = new SensorModel(mainAssembly, thirdPart);

        // building the SECONDARY assembly
        // =======================
        final String mainBody2 = "mainBody2";
        final String secondPart2 = "secondPart2";
        final String thirdPart2 = "thirdPart2";
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        final Vector3D translationSecond = new Vector3D(50.0, -50.0, 0.0);

        // sensors
        // main part field
        final String name3 = "circularField3";
        final Vector3D mainFieldDirection3 = Vector3D.PLUS_J;
        final IFieldOfView mainField3 = new CircularField(name3, 5. * FastMath.PI / 8.0,
            mainFieldDirection3);

        // main part sensor property creation
        final SensorProperty sensorProperty3 = new SensorProperty(mainFieldDirection3);
        sensorProperty3.setMainFieldOfView(mainField3);

        // second part field
        final String name4 = "circularField4";
        final Vector3D mainFieldDirection4 = Vector3D.PLUS_J;
        final IFieldOfView mainField4 = new CircularField(name4, FastMath.PI / 4.0,
            mainFieldDirection4);

        // second part sensor property creation
        final SensorProperty sensorProperty4 = new SensorProperty(mainFieldDirection4);
        sensorProperty4.setMainFieldOfView(mainField4);

        // second part field
        final String name5 = "circularField5";
        final Vector3D mainFieldDirection5 = Vector3D.PLUS_J;
        final IFieldOfView mainField5 = new CircularField(name5, 3. * FastMath.PI / 8.0,
            mainFieldDirection5);

        // third part sensor property creation
        final SensorProperty sensorProperty5 = new SensorProperty(mainFieldDirection5);
        sensorProperty5.setMainFieldOfView(mainField5);

        // assembly building
        try {
            // add main part
            builder2.addMainPart(mainBody2);

            // add second part
            builder2.addPart(secondPart2, mainBody2, translationSecond, Rotation.IDENTITY);

            // add thrid part
            builder2.addPart(thirdPart2, mainBody2, translationSecond, Rotation.IDENTITY);

            // add sensors
            builder2.addProperty(sensorProperty3, mainBody2);
            builder2.addProperty(sensorProperty4, secondPart2);
            builder2.addProperty(sensorProperty5, thirdPart2);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame2 = new UpdatableFrame(this.eme2000Frame, Transform.IDENTITY,
                "mainFrame2");
            builder2.initMainPartFrame(mainFrame2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }
        final Assembly secondaryAssembly = builder2.returnAssembly();

        // spacecraft sensor model
        this.secondarySpacecraftSensorModel1 = new SensorModel(secondaryAssembly, mainBody2);
        this.secondarySpacecraftSensorModel2 = new SensorModel(secondaryAssembly, secondPart2);
        this.secondarySpacecraftSensorModel3 = new SensorModel(secondaryAssembly, thirdPart2);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // targets settings
        this.mainSpacecraftSensorModel1.setMainTarget(this.secondarySpacecraftSensorModel2, radius);
        this.secondarySpacecraftSensorModel2.setMainTarget(this.mainSpacecraftSensorModel1, radius);

        this.mainSpacecraftSensorModel2.setMainTarget(this.secondarySpacecraftSensorModel1, radius);
        this.secondarySpacecraftSensorModel1.setMainTarget(this.mainSpacecraftSensorModel2, radius);
    }

    AbsoluteDate actualDate;

    /**
     * @testType UT
     * 
     * @description checks that mutual visibility between two objects is properly detected. This
     *              tests aims at checking the mutual visibility computation is properly performed
     *              (before PATRIUS v4.0, computation fails due to convergence issues in Ellipsoid
     *              class).
     * 
     * @input Propagators, SensorModel
     * 
     * @output mutual visibility event date
     * 
     * @testPassCriteria mutual visibility event date is as expected (reference: same case without
     *                   earth flattening, threshold: 2s)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testMutualVisibility() throws PatriusException {

        // Initialization
        Utils.setDataRoot("other/sattosatmutualvisibility");

        // Ephemeris data for both satellites
        // Sat-1
        final AbsoluteDate[] ephemeris_dates_sat1 = {
            new AbsoluteDate("2018-06-23T03:41:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:41:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:42:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:42:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:43:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:43:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:44:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:44:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:45:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:45:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:46:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:46:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:47:00.000", TimeScalesFactory.getTAI()) };
        final PVCoordinates[] ephemeris_pv_sat1 = {
            new PVCoordinates(new Vector3D(-1500978.4739827851, 5237524.2946794545,
                4671559.8121918375), new Vector3D(2742.288571151632, -4166.74601484179,
                5538.157493073076)),
            new PVCoordinates(new Vector3D(-1417996.27245676, 5110006.0123326965,
                4835408.685267304), new Vector3D(2789.4227865501343, -4333.826310435023,
                5384.245155669499)),
            new PVCoordinates(new Vector3D(-1333639.4544333983, 4977534.194769331,
                4994563.3105959445), new Vector3D(2833.9194058603753, -4496.948260676792,
                5225.208771512757)),
            new PVCoordinates(new Vector3D(-1247988.5052968955, 4840232.19326191,
                5148869.849408658), new Vector3D(2875.675748211986, -4655.758614034853,
                5061.041168565689)),
            new PVCoordinates(new Vector3D(-1161127.492817218, 4698236.343797837,
                5298175.541626028), new Vector3D(2914.576453571626, -4809.811044319869,
                4891.855437472712)),
            new PVCoordinates(new Vector3D(-1073142.8938058214, 4551693.2655165335,
                5442333.73171918), new Vector3D(2950.582333406779, -4958.901702873263,
                4717.92472430088)),
            new PVCoordinates(new Vector3D(-984121.1695573932, 4400750.850890425,
                5581206.246549292), new Vector3D(2983.726659534378, -5103.134357568736,
                4539.510868790309)),
            new PVCoordinates(new Vector3D(-894147.5170025934, 4245551.912671739,
                5714660.883724766), new Vector3D(3014.0461594726203, -5242.686569071614,
                4356.744286771325)),
            new PVCoordinates(new Vector3D(-803306.9216077881, 4086237.098520252,
                5842567.979270623), new Vector3D(3041.510937714916, -5377.48146808685,
                4169.689163665733)),
            new PVCoordinates(new Vector3D(-711686.4399978967, 3922954.981491078,
                5964800.95025061), new Vector3D(3066.0185452469614, -5507.079326326448,
                3978.51217889222)),
            new PVCoordinates(new Vector3D(-619376.249481663, 3755869.1448472207,
                6081240.712981177), new Vector3D(3087.485194886268, -5631.009294412654,
                3783.5351080956043)),
            new PVCoordinates(new Vector3D(-526467.7208180372, 3585152.4015888353,
                6191778.229414318), new Vector3D(3105.914260976059, -5749.153895732372,
                3585.0679266030234)),
            new PVCoordinates(new Vector3D(-433051.2749141364, 3410975.6609340245,
                6296311.146822414), new Vector3D(3121.3540729614565, -5861.723675464169,
                3383.23061540934)) };

        // GPS-28
        final AbsoluteDate[] ephemeris_dates_gps28 = {
            new AbsoluteDate("2018-06-23T03:41:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:41:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:42:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:42:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:43:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:43:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:44:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:44:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:45:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:45:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:46:00.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:46:30.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2018-06-23T03:47:00.000", TimeScalesFactory.getTAI()) };
        final PVCoordinates[] ephemeris_pv_gps28 = {
            new PVCoordinates(new Vector3D(-2.038991254604443E7, 6229489.814706905,
                -1.52113573528436E7), new Vector3D(871.193761343022, -3047.1342472015453,
                -2320.9622409238746)),
            new PVCoordinates(new Vector3D(-2.0363571758019663E7, 6138018.757617596,
                -1.528083410549455E7), new Vector3D(884.7576113375652, -3051.2509163699833,
                -2310.813073219612)),
            new PVCoordinates(new Vector3D(-2.0336824302501548E7, 6046425.096283544,
                -1.5350005661579443E7), new Vector3D(898.3063401574675, -3055.307420033344,
                -2300.615838082452)),
            new PVCoordinates(new Vector3D(-2.030967063751587E7, 5954710.637035207,
                -1.5418870581987165E7), new Vector3D(911.8396599722015, -3059.303651081934,
                -2290.370729848169)),
            new PVCoordinates(new Vector3D(-2.028211122954826E7, 5862877.189986892,
                -1.5487427433455285E7), new Vector3D(925.3572829991363, -3063.23950371637,
                -2280.077944077343)),
            new PVCoordinates(new Vector3D(-2.0254146554015208E7, 5770926.567375789,
                -1.5555674788606105E7), new Vector3D(938.8589217561562,
                -3067.1148733786913, -2269.7376775523285)),
            new PVCoordinates(new Vector3D(-2.0225777094657693E7, 5678860.5855877595,
                -1.5623611225984132E7), new Vector3D(952.344288757621, -3070.9296568508153,
                -2259.3501282731618)),
            new PVCoordinates(new Vector3D(-2.0197003344047703E7, 5586681.06335015,
                -1.5691235330091683E7), new Vector3D(965.8130967896589,
                -3074.6837521777716, -2248.915495454459)),
            new PVCoordinates(new Vector3D(-2.0167825803206712E7, 5494389.823017369,
                -1.5758545691426048E7), new Vector3D(979.2650587170131,
                -3078.3770587341387, -2238.4339795214423)),
            new PVCoordinates(new Vector3D(-2.0138244981889263E7, 5401988.689502462,
                -1.5825540906515189E7), new Vector3D(992.6998876474138,
                -3082.0094771795884, -2227.9057821065376)),
            new PVCoordinates(new Vector3D(-2.010826139845137E7, 5309479.490679951,
                -1.5892219577954018E7), new Vector3D(1006.1172968723687,
                -3085.5809094838587, -2217.3311060455208)),
            new PVCoordinates(new Vector3D(-2.0077875579693228E7, 5216864.057929868,
                -1.595858031444087E7), new Vector3D(1019.5169997851306, -3089.09125896091,
                -2206.7101553735883)),
            new PVCoordinates(new Vector3D(-2.0047088061276905E7, 5124144.224491428,
                -1.6024621730812617E7), new Vector3D(1032.8987101355763, -3092.54043019217,
                -2196.0431353219747)) };

        // Build both ephemeris (8th order Lagrange)
        // Sat-1
        final SpacecraftState[] tab_sat1 = new SpacecraftState[ephemeris_dates_sat1.length];
        for (int i = 0; i < tab_sat1.length; i++) {
            tab_sat1[i] = new SpacecraftState(new CartesianOrbit(ephemeris_pv_sat1[i],
                FramesFactory.getGCRF(), ephemeris_dates_sat1[i], Constants.GRS80_EARTH_MU));
        }
        final double[] tab_dates_sat1 = new double[ephemeris_dates_sat1.length];
        for (int i = 0; i < tab_dates_sat1.length; i++) {
            tab_dates_sat1[i] = ephemeris_dates_sat1[i].durationFrom(ephemeris_dates_sat1[0]);
        }
        final EphemerisPvLagrange ephemeris_sat1 = new EphemerisPvLagrange(tab_sat1, 8,
            new BinarySearchIndexClosedOpen(tab_dates_sat1));
        final Propagator propagator_sat1 = new PVCoordinatesPropagator(ephemeris_sat1,
            ephemeris_dates_sat1[3], Constants.GRS80_EARTH_MU, FramesFactory.getGCRF());
        final AttitudeProvider attitude_law_sat1 = new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY);

        // GPS-28
        final SpacecraftState[] tab_gps28 = new SpacecraftState[ephemeris_dates_gps28.length];
        for (int i = 0; i < tab_gps28.length; i++) {
            tab_gps28[i] = new SpacecraftState(new CartesianOrbit(ephemeris_pv_gps28[i],
                FramesFactory.getGCRF(), ephemeris_dates_gps28[i], Constants.GRS80_EARTH_MU));
        }
        final double[] tab_dates_gps28 = new double[ephemeris_dates_gps28.length];
        for (int i = 0; i < tab_dates_gps28.length; i++) {
            tab_dates_gps28[i] = ephemeris_dates_gps28[i].durationFrom(ephemeris_dates_gps28[0]);
        }
        final EphemerisPvLagrange ephemeris_gps28 = new EphemerisPvLagrange(tab_gps28, 8,
            new BinarySearchIndexClosedOpen(tab_dates_gps28));
        final Propagator propagator_gps28 = new PVCoordinatesPropagator(ephemeris_gps28,
            ephemeris_dates_gps28[3], Constants.GRS80_EARTH_MU, FramesFactory.getGCRF());
        final AttitudeProvider attitude_law_gps28 = new ConstantAttitudeLaw(
            FramesFactory.getGCRF(), Rotation.IDENTITY);
        propagator_gps28.setAttitudeProvider(attitude_law_gps28);

        // Assemblies and sensor models
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(
            Constants.GRS80_EARTH_EQUATORIAL_RADIUS, Constants.GRS80_EARTH_FLATTENING,
            FramesFactory.getITRF(), "Earth");
        earth.setConvergenceThreshold(1E-10);

        // Sat-1
        final AssemblyBuilder builder_sat1 = new AssemblyBuilder();
        builder_sat1.addMainPart("Main");
        builder_sat1.addPart("sensor_sat1", "Main", Transform.IDENTITY);
        final SensorProperty sensor_sat1 = new SensorProperty(Vector3D.MINUS_K);
        sensor_sat1.setMainFieldOfView(new CircularField("FOV", MathLib.toRadians(80.),
            Vector3D.MINUS_K));
        sensor_sat1.setMainTarget(propagator_gps28, new ConstantRadiusProvider(0.));
        builder_sat1.addProperty(sensor_sat1, "sensor_sat1");
        final SpacecraftState initialState_sat1 = new SpacecraftState(tab_sat1[0].getOrbit(),
            attitude_law_sat1.getAttitude(tab_sat1[0].getOrbit()));
        builder_sat1.initMainPartFrame(initialState_sat1);
        final Assembly assembly_sat1 = builder_sat1.returnAssembly();

        final SensorModel model_sat1 = new SensorModel(assembly_sat1, "sensor_sat1");
        model_sat1.addMaskingCelestialBody(earth);

        // GPS-28
        final AssemblyBuilder builder_gps28 = new AssemblyBuilder();
        builder_gps28.addMainPart("Main2");
        builder_gps28.addPart("sensor_gps28", "Main2", Transform.IDENTITY);
        final SensorProperty sensor_gps28 = new SensorProperty(Vector3D.PLUS_I);
        sensor_gps28.setMainFieldOfView(new OmnidirectionalField("GPSAntenna"));
        sensor_gps28.setMainTarget(CelestialBodyFactory.getSun(), new ConstantRadiusProvider(0.));
        builder_gps28.addProperty(sensor_gps28, "sensor_gps28");
        final SpacecraftState initialState_gps28 = new SpacecraftState(tab_gps28[0].getOrbit(),
            attitude_law_gps28.getAttitude(tab_gps28[0].getOrbit()));
        builder_gps28.initMainPartFrame(initialState_gps28);
        final Assembly assembly_gps28 = builder_gps28.returnAssembly();

        final SensorModel model_gps28 = new SensorModel(assembly_gps28, "sensor_gps28");
        model_gps28.addMaskingCelestialBody(earth);

        // Propagation with event detection
        final SatToSatMutualVisibilityDetector detector = new SatToSatMutualVisibilityDetector(
            model_sat1, model_gps28, propagator_gps28, true, 60., 0.0001, Action.CONTINUE,
            Action.CONTINUE){
            /** Serializable UID. */
            private static final long serialVersionUID = 5881697827468058823L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                SatToSatMutualVisibilityTest.this.actualDate = s.getDate();
                return super.eventOccurred(s, increasing, forward);
            }

        };
        propagator_sat1.addEventDetector(detector);
        propagator_sat1.setAttitudeProvider(attitude_law_gps28);
        propagator_sat1.propagate(ephemeris_dates_sat1[ephemeris_dates_sat1.length - 5]);

        // Check
        final AbsoluteDate expectedDate = new AbsoluteDate("2018-06-23T03:44:02.553",
            TimeScalesFactory.getTAI());
        Assert.assertEquals(0., expectedDate.durationFrom(this.actualDate), 2);
    }

    /**
     * @description Test this event detector wrap feature in {@link SignalPropagationWrapperDetector}
     * 
     * @input this event detector in INSTANTANEOUS & LIGHT_SPEED
     *        <p>
     *        Note: in this test we build a new secondary sensor with a larger field of view and a new secondary
     *        propagator with an orbit further from the main object.<br>
     *        The aim is to be able to evaluate emitter/receiver dates with LIGHT_SPEED mode.
     *        </p>
     * 
     * @output the emitter & receiver dates
     * 
     * @testPassCriteria The results containers as expected (non regression)
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSignalPropagationWrapperDetector() throws PatriusException {

        // Initialization
        Utils.setDataRoot("regular-data");

        // building the SECONDARY assembly
        // =======================
        final String mainBody2 = "mainBody2";
        final String secondPart2 = "secondPart2";
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        final Vector3D translationSecond = new Vector3D(50.0, -50.0, 0.0);

        // sensors
        // main part field
        final String name3 = "circularField3";
        final Vector3D mainFieldDirection3 = Vector3D.PLUS_I;
        final IFieldOfView mainField3 = new CircularField(name3, FastMath.PI / 2.0,
            mainFieldDirection3);

        // main part sensor property creation
        final SensorProperty sensorProperty3 = new SensorProperty(mainFieldDirection3);
        sensorProperty3.setMainFieldOfView(mainField3);

        // second part field
        final String name4 = "circularField4";
        final Vector3D mainFieldDirection4 = Vector3D.PLUS_I;
        final IFieldOfView mainField4 = new CircularField(name4, FastMath.PI / 2.0,
            mainFieldDirection4);

        // second part sensor property creation
        final SensorProperty sensorProperty4 = new SensorProperty(mainFieldDirection4);
        sensorProperty4.setMainFieldOfView(mainField4);

        // second part field
        final String name5 = "circularField5";
        final Vector3D mainFieldDirection5 = Vector3D.PLUS_I;
        final IFieldOfView mainField5 = new CircularField(name5, FastMath.PI / 2.0,
            mainFieldDirection5);

        // third part sensor property creation
        final SensorProperty sensorProperty5 = new SensorProperty(mainFieldDirection5);
        sensorProperty5.setMainFieldOfView(mainField5);

        // assembly building
        try {
            // add main part
            builder2.addMainPart(mainBody2);

            // add second part
            builder2.addPart(secondPart2, mainBody2, translationSecond, Rotation.IDENTITY);

            // add sensors
            builder2.addProperty(sensorProperty3, mainBody2);
            builder2.addProperty(sensorProperty4, secondPart2);

            // assembly INITIAL link to the tree of frames
            final UpdatableFrame mainFrame2 = new UpdatableFrame(this.eme2000Frame, Transform.IDENTITY,
                "mainFrame2");
            builder2.initMainPartFrame(mainFrame2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }
        final Assembly secondaryAssembly = builder2.returnAssembly();

        // spacecraft sensor model
        final SensorModel secondarySpacecraftSensorModel2Bis = new SensorModel(secondaryAssembly, secondPart2);

        // Radius provider
        final ApparentRadiusProvider radius = new ConstantRadiusProvider(0.0);

        // targets settings
        secondarySpacecraftSensorModel2Bis.setMainTarget(this.mainSpacecraftSensorModel2, radius);

        final Propagator mainPropagator = new KeplerianPropagator(this.mainOrbit, this.attitudeProv);
        final KeplerianOrbit secondOrbit = new KeplerianOrbit(1e7, 0.0, MathUtils.HALF_PI, 0., FastMath.PI, 0.,
            PositionAngle.TRUE, this.eme2000Frame, this.date, Utils.mu);
        final Propagator secondaryPropagator = new KeplerianPropagator(secondOrbit, this.attitudeProv);

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED) for
        // MAIN_TO_SECONDARY & SECONDARY_TO_MAIN
        final SatToSatMutualVisibilityDetector eventDetector1M2S = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel1, secondarySpacecraftSensorModel2Bis, secondaryPropagator, false, 1.,
            1e-9, Action.CONTINUE, Action.CONTINUE, false, false, LinkType.MAIN_TO_SECONDARY);
        final SatToSatMutualVisibilityDetector eventDetector2M2S = (SatToSatMutualVisibilityDetector) eventDetector1M2S
            .copy();
        eventDetector2M2S.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        final SatToSatMutualVisibilityDetector eventDetector1S2M = new SatToSatMutualVisibilityDetector(
            this.mainSpacecraftSensorModel1, secondarySpacecraftSensorModel2Bis, secondaryPropagator, false, 1.,
            1e-9, Action.CONTINUE, Action.CONTINUE, false, false, LinkType.SECONDARY_TO_MAIN);
        final SatToSatMutualVisibilityDetector eventDetector2S2M = (SatToSatMutualVisibilityDetector) eventDetector1S2M
            .copy();
        eventDetector2S2M.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1M2S = new SignalPropagationWrapperDetector(eventDetector1M2S);
        final SignalPropagationWrapperDetector wrapper2M2S = new SignalPropagationWrapperDetector(eventDetector2M2S);
        final SignalPropagationWrapperDetector wrapper1S2M = new SignalPropagationWrapperDetector(eventDetector1S2M);
        final SignalPropagationWrapperDetector wrapper2S2M = new SignalPropagationWrapperDetector(eventDetector2S2M);

        // Add them in the propagator, then propagate
        mainPropagator.addEventDetector(wrapper1M2S);
        mainPropagator.addEventDetector(wrapper2M2S);
        mainPropagator.addEventDetector(wrapper1S2M);
        mainPropagator.addEventDetector(wrapper2S2M);
        final SpacecraftState finalState = mainPropagator.propagate(this.date.shiftedBy(2 * 3600.));

        // MAIN_TO_SECONDARY

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1M2S.getNBOccurredEvents());
        Assert.assertTrue(wrapper1M2S.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.085"), 1e-3));
        Assert.assertTrue(wrapper1M2S.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.085"), 1e-3));
        Assert.assertTrue(wrapper1M2S.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.796"), 1e-3));
        Assert.assertTrue(wrapper1M2S.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.796"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2M2S.getNBOccurredEvents());
        Assert.assertTrue(wrapper2M2S.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.101"), 1e-3));
        Assert.assertTrue(wrapper2M2S.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.110"), 1e-3));
        Assert.assertTrue(wrapper2M2S.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.708"), 1e-3));
        Assert.assertTrue(wrapper2M2S.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.724"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(finalState.getOrbit(), eventDetector1M2S.getEmitter(finalState));
        Assert.assertEquals(secondaryPropagator, eventDetector1M2S.getReceiver(null));
        Assert.assertEquals(DatationChoice.EMITTER, eventDetector1M2S.getDatationChoice());

        // SECONDARY_TO_MAIN

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1S2M.getNBOccurredEvents());
        Assert.assertTrue(wrapper1S2M.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.085"), 1e-3));
        Assert.assertTrue(wrapper1S2M.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.085"), 1e-3));
        Assert.assertTrue(wrapper1S2M.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.796"), 1e-3));
        Assert.assertTrue(wrapper1S2M.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.796"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2S2M.getNBOccurredEvents());
        Assert.assertTrue(wrapper2S2M.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.059"), 1e-3));
        Assert.assertTrue(wrapper2S2M.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2000-01-01T12:33:11.068"), 1e-3));
        Assert.assertTrue(wrapper2S2M.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.868"), 1e-3));
        Assert.assertTrue(wrapper2S2M.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2000-01-01T13:42:32.885"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(secondaryPropagator, eventDetector1S2M.getEmitter(null));
        Assert.assertEquals(finalState.getOrbit(), eventDetector1S2M.getReceiver(finalState));
        Assert.assertEquals(DatationChoice.RECEIVER, eventDetector1S2M.getDatationChoice());
    }
}
