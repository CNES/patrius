/**
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-292:22/08/2024: Implementation de multi-propagateurs mixtes
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProviderWrapper;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.events.detectors.DistanceDetector;
import fr.cnes.sirius.patrius.events.detectors.ExtremaGenericDetector;
import fr.cnes.sirius.patrius.events.detectors.ExtremaGenericDetector.ExtremumType;
import fr.cnes.sirius.patrius.events.detectors.ThreeBodiesAngleDetector;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEvent;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.events.utils.OneSatEventDetectorWrapper;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TranslatedFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MultiPropagator;
import fr.cnes.sirius.patrius.propagation.PVCoordinatesPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.SpacecraftStateProvider;
import fr.cnes.sirius.patrius.propagation.analytical.multi.MultiAnalyticalPropagator;
import fr.cnes.sirius.patrius.propagation.events.ThreeBodiesAngleDetectorTest;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * Validation class for {@link MultiAnalyticalPropagator}.
 * </p>
 *
 * @author Maxime Astruc
 *
 * @version 4.14
 *
 * @since 4.14
 *
 */
public class MultiAnalyticalPropagatorTest {

    /**
     * Tolerance 1e-14.
     */
    private static final double E_14 = Precision.DOUBLE_COMPARISON_EPSILON;
    
    /**
     * Tolerance vector 1e-14.
     */
    private static final Vector3D VE_14 = new Vector3D(E_14, E_14, E_14);

    /**
     * Initial date.
     */
    private AbsoluteDate initialDate;

    /**
     * MU.
     */
    private double mu;

    /**
     * GCRF frame.
     */
    private Frame gcrf;
    
    /**
     * Earth body.
     */
    private CelestialBody earth;
    
    /**
     * Default orbit, anomaly is zero.
     */
    private KeplerianOrbit orbit1;
    
    /**
     * Default orbit, anomaly is pi/2.
     */
    private KeplerianOrbit orbit2;
    
    /**
     * Default orbit, anomaly is 2*pi/3.
     */
    private KeplerianOrbit orbit3;
    
    /**
     * Attitude law.
     */
    private AttitudeLaw attLaw;
    
    /**
     * Default step for fixed step handlers.
     */
    private double defaultDt;
    
    /**
     * Mono step handler.
     */
    private MyMonoStepHandler monoStepHandler;
    
    /**
     * Multi step handler.
     */
    private MyMultiStepHandler multiStepHandler;
    
    /**
     * Two simple analytical propagations in slave mode without detectors: comparison between two analytical
     * propagations and one multi-sat analytical propagation.
     *
     * @throws PatriusException
     * 
     * @testType UT
     *
     * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
     *
     * @description Single analytical and one multi-analytical propagations in slave mode (without detectors) using
     * keplerian orbits
     *
     * @input two analytical propagators and one multi-sat analytical propagator
     *
     * @output PV coordinates and attitude from propagations
     *
     * @testPassCriteria results provided by the different propagators are consistent and are the same
     *
     * @referenceVersion 4.14
     *
     * @nonRegressionVersion 4.14
     */
     @Test
     public void testDefaultMethods() throws PatriusException {

         /*
          * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
          * The two satellites follow the same orbit, but with a pi/2 difference in true anomaly:
          * - sat1 starts in GCRF +Y direction and therefore stops in GCRF -Y direction
          * - sat2 starts in GCRF -X direction (apogee) and therefore stops in GCRF +X direction (perigee)
          * 
          * Since they both follow a Earth-centric attitude law with +Z axis, it is expected that:
          * - sat1 +Z axis corresponds to GCRF +Y axis at the end of the propagation
          * - sat2 +Z axis corresponds to GCRF -X axis at the end of the propagation
          */
         final double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // 6 778 137
         final double e = 0;
         final double i = 0;
         final double pa = 0;
         final double raan = 0;
         final Orbit orbit1 = new KeplerianOrbit(a, e, i, pa, raan, MathLib.PI / 2, PositionAngle.TRUE, this.gcrf,
             this.initialDate, this.mu);

         final double dt = orbit1.getKeplerianPeriod() / 2;
         final AbsoluteDate halfTarget = this.initialDate.shiftedBy(dt / 2);
         final AbsoluteDate target = this.initialDate.shiftedBy(dt);

         KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(orbit1);
         keplerianPropagator1.setAttitudeProvider(this.attLaw);
         final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

         final Orbit orbit2 = new KeplerianOrbit(a, e, i, pa, raan, MathLib.PI, PositionAngle.TRUE, this.gcrf,
             this.initialDate, this.mu);

         KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(orbit2);
         keplerianPropagator2.setAttitudeProvider(this.attLaw);
         final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

         final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
         final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();

         /*
          * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
          */
         // Re-instanciate propagators so as to be sure they are fully reseted
         keplerianPropagator1 = new KeplerianPropagator(orbit1);
         keplerianPropagator1.setAttitudeProvider(this.attLaw);
         keplerianPropagator2 = new KeplerianPropagator(orbit2);
         keplerianPropagator2.setAttitudeProvider(this.attLaw);
         
         // Create multi propagator
         final String id1 = "sat1";
         final String id2 = "sat2";
         final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(this.initialDate);
         multiProp.addPropagator(keplerianPropagator1, id1);
         
         // Check the reference date
         Assert.assertEquals(this.initialDate, multiProp.getReferenceDate());
         
         // Exceptions are checked before adding the 2nd propagator: null, empty and already known ID
         // Exception: try to add an instance of NumericalPropagator
         final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
         final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
         final double minstep = 0.001;
         final double maxstep = 200.;
         final DormandPrince853Integrator integrator = new DormandPrince853Integrator(minstep, maxstep, absTolerance, relTolerance);
         final NumericalPropagator numProp = new NumericalPropagator(integrator);
         try {
             multiProp.addPropagator(numProp, "num");
             Assert.fail();
         } catch (final PatriusException pe) {
             Assert.assertTrue("illegal state".equals(pe.getMessage()));
         }
         // Null ID not authorized
         try {
             multiProp.addPropagator(keplerianPropagator2, null);
             Assert.fail();
         } catch (final IllegalStateException is) {
             Assert.assertTrue("The input sat ID is null".equals(is.getMessage()));
         }
         // Empty String ID not authorized
         try {
             multiProp.addPropagator(keplerianPropagator2, "");
             Assert.fail();
         } catch (final IllegalStateException is) {
             Assert.assertTrue("The input sat ID is null".equals(is.getMessage()));
         }
         // Already added ID not authorized
         try {
             multiProp.addPropagator(keplerianPropagator2, id1);
             Assert.fail();
         } catch (final IllegalStateException is) {
             Assert.assertTrue("Unexpected or missing sat ID sat1 in collection".equals(is.getMessage()));
         }
         
         // Add the 2nd propagator for real
         multiProp.addPropagator(keplerianPropagator2, id2);
         
         // Try to add an initial state
         // Exception: unknown ID
         try {
             multiProp.addInitialState(keplerianPropagator1.getInitialState(), "unknownID");
             Assert.fail();
         } catch (final IllegalStateException is) {
             Assert.assertTrue("Unexpected or missing sat ID unknownID in collection".equals(is.getMessage()));
         }
         // Exception: date mismatch
         try {
             final SpacecraftState finalStateWrongDate = keplerianPropagator1.propagate(halfTarget);
             multiProp.addInitialState(finalStateWrongDate, id1);
             Assert.fail();
         } catch (final PatriusException pe) {
             Assert.assertTrue(
                 ("The added state date " + halfTarget + " does not match previous states date " + this.initialDate)
                     .equals(pe.getMessage()));
         }
         
         // Add initial state for coverage purpose
         multiProp.addInitialState(keplerianPropagator1.getInitialState(), id1);
         
         // Try propagators' getter
         final Map<String, SpacecraftStateProvider> propsMap = multiProp.getPropagators();
         Assert.assertEquals(2, propsMap.size());
         Assert.assertTrue(keplerianPropagator1 == propsMap.get(id1));
         Assert.assertTrue(keplerianPropagator2 == propsMap.get(id2));
         
         // Change modes (slave mode is default mode), finish with slave mode
         Assert.assertEquals(MultiPropagator.SLAVE_MODE, multiProp.getMode());
         multiProp.setMasterMode(null);
         Assert.assertEquals(MultiPropagator.MASTER_MODE, multiProp.getMode());
         try {
             multiProp.setEphemerisMode();
             Assert.fail();
         } catch (final RuntimeException re) {
             Assert.assertTrue("illegal state".equals(re.getCause().getMessage()));
         }
         multiProp.setSlaveMode();
         Assert.assertEquals(MultiPropagator.SLAVE_MODE, multiProp.getMode());
         
         // Exception: method to generate ephemeris not permitted yet
         try {
             multiProp.getGeneratedEphemeris(id1);
             Assert.fail();
         } catch (final RuntimeException re) {
             Assert.assertTrue("illegal state".equals(re.getCause().getMessage()));
         }
         // Exception: try to add a multi event detector through this method is not permitted
         try {
             multiProp.addEventDetector(null);
             Assert.fail();
         } catch (final RuntimeException re) {
             Assert.assertTrue("illegal state".equals(re.getCause().getMessage()));
         }
         // Exception: if the detector returns a PatriusException because of g
         try {
             multiProp.addEventDetector(new MockDetector(), id1);
             multiProp.propagate(halfTarget);
             Assert.fail();
         } catch (final RuntimeException re) {
             Assert.assertTrue("illegal state".equals(re.getCause().getMessage()));
         }
         
         // Check event detectors and method clearEventsDetectors
         multiProp.addEventDetector(new MyDateDetector(), id1);
         Collection<MultiEventDetector> detectors = multiProp.getEventsDetectors();
         Assert.assertEquals(1, detectors.size());
         final Iterator<MultiEventDetector> iterator = detectors.iterator();
         final MultiEventDetector next = iterator.next();
         Assert.assertTrue(next instanceof OneSatEventDetectorWrapper);
         Assert.assertTrue(id1.equals(((OneSatEventDetectorWrapper) next).getID()));
         multiProp.clearEventsDetectors();
         detectors = multiProp.getEventsDetectors();
         Assert.assertEquals(0, detectors.size());
         
         // Check attitude providers
         final String otherId1 = "anotherID1";
         final String otherId2 = "anotherID2";
         final String otherId3 = "anotherID3";
         final KeplerianPropagator kepPropSup1 = new KeplerianPropagator(this.orbit1);
         final MyKeplerianPropagator kepPropSup2 = new MyKeplerianPropagator(this.orbit1);
         kepPropSup2.setAttitudeProviderForces(this.attLaw);
         final MyKeplerianPropagator kepPropSup3 = new MyKeplerianPropagator(this.orbit1);
         kepPropSup3.setAttitudeProviderEvents(this.attLaw);
         multiProp.addPropagator(kepPropSup1, otherId1);
         multiProp.addPropagator(kepPropSup2, otherId2);
         multiProp.addPropagator(kepPropSup3, otherId3);
         Assert.assertTrue(this.attLaw == ((MultiAttitudeProviderWrapper) multiProp.getAttitudeProvider(id1)).getAttitudeProvider());
         Assert.assertNull(multiProp.getAttitudeProviderForces(id1));
         Assert.assertNull(multiProp.getAttitudeProviderEvents(id1));
         Assert.assertNull(multiProp.getAttitudeProvider(otherId1));
         Assert.assertNull(multiProp.getAttitudeProviderForces(otherId1));
         Assert.assertNull(multiProp.getAttitudeProviderEvents(otherId1));
         Assert.assertTrue(this.attLaw == ((MultiAttitudeProviderWrapper) multiProp.getAttitudeProvider(otherId2)).getAttitudeProvider());
         Assert.assertTrue(this.attLaw == ((MultiAttitudeProviderWrapper) multiProp.getAttitudeProviderForces(otherId2)).getAttitudeProvider());
         Assert.assertNull(multiProp.getAttitudeProviderEvents(otherId2));
         Assert.assertTrue(this.attLaw == ((MultiAttitudeProviderWrapper) multiProp.getAttitudeProvider(otherId3)).getAttitudeProvider());
         Assert.assertNull(multiProp.getAttitudeProviderForces(otherId3));
         Assert.assertTrue(this.attLaw == ((MultiAttitudeProviderWrapper) multiProp.getAttitudeProviderEvents(otherId3)).getAttitudeProvider());
         
         // Check the propagation frames
         Assert.assertEquals(this.gcrf, multiProp.getFrame(id1));
         Assert.assertEquals(this.gcrf, multiProp.getFrame(id2));
         
         // Finally propagate
         Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

         /*
          * 5 elements because 5 propagators were added
          * Check states for two of them
          */
         Assert.assertEquals(5, finalStatesMap.size());
         Assert.assertNotNull(finalStatesMap.get(id1));
         Assert.assertNotNull(finalStatesMap.get(id2));
         final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
         final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

         // Check PVs
         final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
         final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
         checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
         checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
         checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
         checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);

         // Check attitudes
         final Rotation rotGcrfToSatMulti1 = finalStateMulti1.getAttitude().getRotation();
         final Vector3D zSatInGcrfMulti1 = rotGcrfToSatMulti1.applyTo(Vector3D.PLUS_K);
         final Rotation rotGcrfToSatMulti2 = finalStateMulti2.getAttitude().getRotation();
         final Vector3D zSatInGcrfMulti2 = rotGcrfToSatMulti2.applyTo(Vector3D.PLUS_K);
         checkVectors(Vector3D.PLUS_J, zSatInGcrfMulti1, VE_14);
         checkVectors(Vector3D.MINUS_I, zSatInGcrfMulti2, VE_14);
         
         /*
          * Propagate again with a different frame after resetting the initial state of satellite 1
          * The new Frame is simply a new instance of original GCRF, with a zero-translation, which means the frame is
          * exactly the same (and so are the results)
          */
         final double a2 = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3;
         final double e2 = 0.03;
         final Frame newFrame = new TranslatedFrame(this.gcrf, this.gcrf, "newGcrf", true);
         final Orbit newOrbit = new KeplerianOrbit(a2, e2, 0., 0., 0., MathLib.PI / 2, PositionAngle.TRUE, newFrame,
             this.initialDate, this.mu);
         multiProp.resetSingleInitialState(new SpacecraftState(newOrbit), id1);
         
         finalStatesMap = multiProp.propagate(target);
         Assert.assertEquals(5, finalStatesMap.size());
         Assert.assertNotNull(finalStatesMap.get(id1));
         Assert.assertNotNull(finalStatesMap.get(id2));
         final SpacecraftState finalStateMulti1New = finalStatesMap.get(id1);
         final SpacecraftState finalStateMulti2New = finalStatesMap.get(id2);
         final PVCoordinates pvsMulti1New = finalStateMulti1New.getPVCoordinates();
         final PVCoordinates pvsMulti2New = finalStateMulti2New.getPVCoordinates();
         checkVectors(pvsMono1.getPosition(), pvsMulti1New.getPosition(), VE_14);
         checkVectors(pvsMono1.getVelocity(), pvsMulti1New.getVelocity(), VE_14);
         checkVectors(pvsMono2.getPosition(), pvsMulti2New.getPosition(), VE_14);
         checkVectors(pvsMono2.getVelocity(), pvsMulti2New.getVelocity(), VE_14);
         final Rotation rotGcrfToSatMulti1New = finalStateMulti1New.getAttitude().getRotation();
         final Vector3D zSatInGcrfMulti1New = rotGcrfToSatMulti1New.applyTo(Vector3D.PLUS_K);
         final Rotation rotGcrfToSatMulti2New = finalStateMulti2New.getAttitude().getRotation();
         final Vector3D zSatInGcrfMulti2New = rotGcrfToSatMulti2New.applyTo(Vector3D.PLUS_K);
         checkVectors(Vector3D.PLUS_J, zSatInGcrfMulti1New, VE_14);
         checkVectors(Vector3D.MINUS_I, zSatInGcrfMulti2New, VE_14);

     }
    
    /**
    * Two simple analytical propagations in slave mode without detectors: comparison between two analytical
    * propagations and one multi-sat analytical propagation.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode (without detectors) using
    * keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates and attitude from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the same
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
    @Test
    public void testSlaveModeTwoSatsCircularOrbitsNoEvents() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit, but with a pi/2 difference in true anomaly:
         * - sat1 starts in GCRF +Y direction and therefore stops in GCRF -Y direction
         * - sat2 starts in GCRF -X direction (apogee) and therefore stops in GCRF +X direction (perigee)
         * 
         * Since they both follow a Earth-centric attitude law with +Z axis, it is expected that:
         * - sat1 +Z axis corresponds to GCRF +Y axis at the end of the propagation
         * - sat2 +Z axis corresponds to GCRF -X axis at the end of the propagation
         */
        final double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // 6 778 137
        final double e = 0;
        final double i = 0;
        final double pa = 0;
        final double raan = 0;
        final Orbit orbit1 = new KeplerianOrbit(a, e, i, pa, raan, MathLib.PI / 2, PositionAngle.TRUE, this.gcrf,
            this.initialDate, this.mu);

        final double dt = orbit1.getKeplerianPeriod() / 2;
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);

        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(orbit1);
        keplerianPropagator1.setAttitudeProvider(this.attLaw);
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        final Orbit orbit2 = new KeplerianOrbit(a, e, i, pa, raan, MathLib.PI, PositionAngle.TRUE, this.gcrf,
            this.initialDate, this.mu);

        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(orbit2);
        keplerianPropagator2.setAttitudeProvider(this.attLaw);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();

        final Rotation rotGcrfToSatMono1 = finalStateMono1.getAttitude().getRotation();
        final Vector3D zSatInGcrfMono1 = rotGcrfToSatMono1.applyTo(Vector3D.PLUS_K);
        final Rotation rotGcrfToSatMono2 = finalStateMono2.getAttitude().getRotation();
        final Vector3D zSatInGcrfMono2 = rotGcrfToSatMono2.applyTo(Vector3D.PLUS_K);

        checkVectors(Vector3D.PLUS_J, zSatInGcrfMono1, VE_14);
        checkVectors(Vector3D.MINUS_I, zSatInGcrfMono2, VE_14);

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(orbit1);
        keplerianPropagator1.setAttitudeProvider(this.attLaw);
        keplerianPropagator2 = new KeplerianPropagator(orbit2);
        keplerianPropagator2.setAttitudeProvider(this.attLaw);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);

        // Check attitudes
        final Rotation rotGcrfToSatMulti1 = finalStateMulti1.getAttitude().getRotation();
        final Vector3D zSatInGcrfMulti1 = rotGcrfToSatMulti1.applyTo(Vector3D.PLUS_K);
        final Rotation rotGcrfToSatMulti2 = finalStateMulti2.getAttitude().getRotation();
        final Vector3D zSatInGcrfMulti2 = rotGcrfToSatMulti2.applyTo(Vector3D.PLUS_K);
        checkVectors(Vector3D.PLUS_J, zSatInGcrfMulti1, VE_14);
        checkVectors(Vector3D.MINUS_I, zSatInGcrfMulti2, VE_14);

    }
   
   /**
    * Two simple analytical propagations in slave mode with detectors: comparison between two analytical
    * propagations and one multi-sat analytical propagation.
    * The first detection shall stop the propagation.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the same
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsWithEventsStopSat1() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly:
         * - sat1 starts in GCRF +Y direction
         * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
         * 
         * All propagators include an extremum distance detector, it is expected that:
         * - sat1 reaches apogee before sat 2 reaches perigee
         * - propagation stops for both satellites when sat1 apogee extremum is detected (case multi)
         */

        final double dt = this.orbit1.getKeplerianPeriod() / 2;
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);

        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        keplerianPropagator1.addEventDetector(extremDistDetect1);
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect2 = new ExtremaGenericDetector<>(distDetect2,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        keplerianPropagator2.addEventDetector(extremDistDetect2);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        
        // Check dates and positions consistency with mono propagations
        Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs and stops propagation
        Assert.assertTrue(stopDateMono2.durationFrom(target) < 0); // Sat2 event occurs and stops propagation
        Assert.assertTrue(stopDateMono2.durationFrom(stopDateMono1) > 0); // Sat1 event occurs before sat2's event
        Assert.assertEquals(-6_981_481.11, pvsMono1.getPosition().getX(), E_14); // Apogee reached
        Assert.assertEquals(6_574_792.89, pvsMono2.getPosition().getX(), E_14); // Perigee reached

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(extremDistDetect1, id1);
        multiProp.addEventDetector(extremDistDetect2, id2);
        
        // Check event detectors
        final Collection<MultiEventDetector> detectors = multiProp.getEventsDetectors();
        Assert.assertEquals(2, detectors.size());
        final Iterator<MultiEventDetector> iterator = detectors.iterator();
        MultiEventDetector next = iterator.next();
        Assert.assertTrue(next instanceof OneSatEventDetectorWrapper);
        Assert.assertTrue(id1.equals(((OneSatEventDetectorWrapper) next).getID()));
        next = iterator.next();
        Assert.assertTrue(next instanceof OneSatEventDetectorWrapper);
        Assert.assertTrue(id2.equals(((OneSatEventDetectorWrapper) next).getID()));
        
        // Propagate
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the final date of sat 1 (in case mono, because sat1 stops propagation)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(stopDateMono1, stopDateMulti1, E_14);
        checkDates(stopDateMono1, stopDateMulti2, E_14);
        
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        // Recompute PVs of satellite 2 at stop date (triggered by satellite 1) thanks to the propagator mono
        final PVCoordinates recomputedSat2Pvs = keplerianPropagator2.propagate(stopDateMono1).getPVCoordinates();
        checkVectors(recomputedSat2Pvs.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(recomputedSat2Pvs.getVelocity(), pvsMulti2.getVelocity(), VE_14);

    }
   
   /**
    * Two simple analytical propagations in slave mode with detectors: comparison between two analytical
    * propagations and one multi-sat analytical propagation.
    * The second detection shall stop the propagation.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the same
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsWithEventsStopSat2() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly:
         * - sat1 starts in GCRF +Y direction
         * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
         * 
         * All propagators include an extremum distance detector, it is expected that:
         * - sat1 reaches apogee before sat 2 reaches perigee
         * - propagation continues until sat 2 reaches perigee
         * - propagation stops for both satellites when sat2 perigee extremum is detected (case multi)
         */

        final double dt = this.orbit1.getKeplerianPeriod() / 2;
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);

        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator1.addEventDetector(extremDistDetect1);
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect2 = new ExtremaGenericDetector<>(distDetect2,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
        keplerianPropagator2.addEventDetector(extremDistDetect2);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        
        // Check dates and positions consistency with mono propagations
        Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs but does not stop
        checkDates(stopDateMono1, target, E_14);
        Assert.assertTrue(stopDateMono2.durationFrom(target) < 0); // Sat2 event occurs and stops propagation

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(extremDistDetect1, id1);
        multiProp.addEventDetector(extremDistDetect2, id2);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);
        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the final date of sat 2 (in case mono, because sat2 stops propagation)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(stopDateMono2, stopDateMulti1, E_14);
        checkDates(stopDateMono2, stopDateMulti2, E_14);
        
        // Recompute PVs of satellite 1 at stop date (triggered by satellite 2) thanks to the propagator mono
        final PVCoordinates recomputedSat1Pvs = keplerianPropagator1.propagate(stopDateMono2).getPVCoordinates();
        checkVectors(recomputedSat1Pvs.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(recomputedSat1Pvs.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);

    }
   
   /**
    * Two simple analytical propagations in slave mode with detectors: comparison between two analytical
    * propagations and one multi-sat analytical propagation.
    * Propagation is not stopped before target because all detectors are asked to CONTINUE when detecting an event.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the same
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsWithEventsNoStop() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly:
         * - sat1 starts in GCRF +Y direction
         * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
         * 
         * All propagators include an extremum distance detector, it is expected that:
         * - sat1 reaches apogee before sat 2 reaches perigee
         * - propagation continues and sat 2 reaches perigee
         * - propagation continues until target date is reached
         */

        final double dt = this.orbit1.getKeplerianPeriod() / 2;
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);

        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator1.addEventDetector(extremDistDetect1);
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect2 = new ExtremaGenericDetector<>(distDetect2,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator2.addEventDetector(extremDistDetect2);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        
        // Check dates and positions consistency with mono propagations
        Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs but does not stop
        Assert.assertTrue(stopDateMono2.durationFrom(this.initialDate) > 0); // Sat2 event occurs but does not stop
        checkDates(stopDateMono1, target, E_14);
        checkDates(stopDateMono2, target, E_14);

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(extremDistDetect1, id1);
        multiProp.addEventDetector(extremDistDetect2, id2);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the target date (because propagation never stops)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(target, stopDateMulti1, E_14);
        checkDates(target, stopDateMulti2, E_14);
        
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);
    }
   
   /**
    * Two simple analytical propagations in slave mode with detectors: comparison between two analytical
    * propagations and one multi-sat analytical propagation.
    * The second detection calls a reset state.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the same
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsWithEventsResetSat2() throws PatriusException {
        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly:
         * - sat1 starts in GCRF +Y direction
         * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
         * 
         * All propagators include an extremum distance detector, it is expected that:
         * - sat1 reaches apogee before sat 2 reaches perigee
         * - propagation continues until sat 2 reaches perigee
         * - propagation continues for both satellites and sat2 resets when sat2 perigee extremum is detected (case multi)
         * - propagation continues until target date is reached
         */

        final double dt = this.orbit1.getKeplerianPeriod() / 2;
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);

        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator1.addEventDetector(extremDistDetect1);
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);
        
        class MyExtremaDistanceDetector extends ExtremaGenericDetector<DistanceDetector> {
            
            private static final long serialVersionUID = -4956127971013701283L;
            private int count;
            private int nResets;

            private MyExtremaDistanceDetector(final DistanceDetector underlyingDetector,
                                              final ExtremumType extremumType,
                                              final double halfComputationStep, final double maxCheck,
                                              final double threshold, final Action actionMin, final Action actionMax) {
                super(underlyingDetector, extremumType, halfComputationStep, maxCheck, threshold, actionMin, actionMax);
                this.count = 0;
                this.nResets = 0;
            }
            
            /** {@inheritDoc} */
            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) throws PatriusException {
                this.count = 0;
                this.nResets = 0;
            }
            
            /** {@inheritDoc} */
            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
                this.count++;
                return super.eventOccurred(s, increasing, forward);
            }

            /** {@inheritDoc} */
            @Override
            public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
                this.nResets++;
                return oldState;
            }
            
            public int getCount() {
                return this.count;
            }
            
            public int getNResets() {
                return this.nResets;
            }

        }

        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final MyExtremaDistanceDetector extremDistDetect2 =
            new MyExtremaDistanceDetector(distDetect2, ExtremumType.MIN_MAX,
                ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP, ExtremaGenericDetector.DEFAULT_MAXCHECK,
                ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.RESET_STATE, Action.RESET_STATE);
        keplerianPropagator2.addEventDetector(extremDistDetect2);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        
        // Check dates and positions consistency with mono propagations
        Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs but does not stop
        Assert.assertTrue(stopDateMono2.durationFrom(this.initialDate) > 0); // Sat2 event occurs but does not stop
        checkDates(stopDateMono1, target, E_14);
        checkDates(stopDateMono2, target, E_14);
        
        // Check the value of custom detector's counters
        // 1 event detected and 1 reset performed
        Assert.assertEquals(1, extremDistDetect2.getCount());
        Assert.assertEquals(1, extremDistDetect2.getNResets());

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Check the value of custom detector's counters
        // Nothing detected yet
        extremDistDetect2.init(null, null);
        Assert.assertEquals(0, extremDistDetect2.getCount());
        Assert.assertEquals(0, extremDistDetect2.getNResets());
        
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(extremDistDetect1, id1);
        multiProp.addEventDetector(extremDistDetect2, id2);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);
        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the final date of sat 2 (in case mono, because sat2 stops propagation)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(stopDateMono2, stopDateMulti1, E_14);
        checkDates(stopDateMono2, stopDateMulti2, E_14);
        
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);
        
        // Check the value of custom detector's counters
        // 1 event detected (twice, as expected from javadoc) and 1 reset performed
        Assert.assertEquals(2, extremDistDetect2.getCount());
        Assert.assertEquals(1, extremDistDetect2.getNResets());

    }
   
   /**
    * <p>Three simple analytical propagations in slave mode with detectors: comparison between three
    * analytical
    * propagations and one multi-sat analytical propagation.
    * Propagation is not stopped before target because all detectors are asked to CONTINUE when
    * detecting an event.</p>
    * 
    * <p>Each satellite has two different event detectors. The first is an apogee detector, and the
    * other one is linked to another satellite :</p>
    * 
    * <ul>
    * <li>Distance between sat 1 and sat 2</li>
    * <li>Distance between sat 2 and sat 3</li>
    * <li>Distance between sat 3 and sat 1</li>
    * </ul>
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using
    *              keplerian orbits
    *
    * @input three analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the
    *                   same (PV & all generated events)
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeThreeSatsEllipticOrbitsWithEventsNoStop() throws PatriusException {

        /*
         * THREE PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The three satellites follow the same orbit (1 period), but with a difference in true anomaly:
         * - sat1 starts in GCRF +Y direction
         * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
         * - sat3 starts with a true anomaly of 2*pi/3.
         * 
         * All propagators include an extremum distance detector, it is expected that:
         * - sat1 reaches apogee before sat 2 reaches perigee
         * - propagation continues and sat 2 reaches perigee
         * - propagation continues until target date is reached
         */

        final double dt = this.orbit1.getKeplerianPeriod();
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);

        // Create propagators for each satellite
        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        KeplerianPropagator keplerianPropagator3 = new KeplerianPropagator(this.orbit3);
        
        // Create event loggers for mono propagations
        final CodedEventsLogger codedEventsLoggerMono1 = new CodedEventsLogger();
        final CodedEventsLogger codedEventsLoggerMono2 = new CodedEventsLogger();
        
        // First satellite
        // Apogee detector
        final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator1.addEventDetector(extremDistDetect1);
        
        // Distance to 2nd satellite
        final DistanceDetector distDetect11 = new DistanceDetector(keplerianPropagator2.getPvProvider(), 0., AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect11 = new ExtremaGenericDetector<>(distDetect11,
                ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
                ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final GenericCodingEventDetector codingExtremDistDetect11 = new GenericCodingEventDetector(extremDistDetect11, "increasing", "decreasing");
        keplerianPropagator1.addEventDetector(codedEventsLoggerMono1.monitorDetector(codingExtremDistDetect11));
        
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        // Second satellite
        // Apogee detector
        final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect2 = new ExtremaGenericDetector<>(distDetect2,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator2.addEventDetector(extremDistDetect2);
        
        // Distance to 3rd satellite
        final DistanceDetector distDetect21 = new DistanceDetector(keplerianPropagator3.getPvProvider(), 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect21 = new ExtremaGenericDetector<>(distDetect21,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final GenericCodingEventDetector codingExtremDistDetect21 = new GenericCodingEventDetector(extremDistDetect21, "increasing", "decreasing");
        keplerianPropagator2.addEventDetector(codedEventsLoggerMono2.monitorDetector(codingExtremDistDetect21));
        
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        // Third satellite
        // Apogee detector
        final DistanceDetector distDetect3 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect3 = new ExtremaGenericDetector<>(distDetect3,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator3.addEventDetector(extremDistDetect3);

        // Distance to 1st satellite
        final DistanceDetector distDetect31 = new DistanceDetector(keplerianPropagator1.getPvProvider(), 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect31 = new ExtremaGenericDetector<>(distDetect31,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator3.addEventDetector(extremDistDetect31);
        
        final SpacecraftState finalStateMono3 = keplerianPropagator3.propagate(target);
        
        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final PVCoordinates pvsMono3 = finalStateMono3.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        final AbsoluteDate stopDateMono3 = finalStateMono3.getDate();
        
        // Check dates and positions consistency with mono propagations
        Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs but does not stop
        Assert.assertTrue(stopDateMono2.durationFrom(this.initialDate) > 0); // Sat2 event occurs but does not stop
        Assert.assertTrue(stopDateMono3.durationFrom(this.initialDate) > 0); // Sat3 event occurs but does not stop
        checkDates(stopDateMono1, target, E_14);
        checkDates(stopDateMono2, target, E_14);
        checkDates(stopDateMono3, target, E_14);

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        keplerianPropagator3 = new KeplerianPropagator(this.orbit3);
        
        // Event loggers for multi-propagation
        final CodedEventsLogger codedEventsLoggerMulti1 = new CodedEventsLogger();
        final CodedEventsLogger codedEventsLoggerMulti2 = new CodedEventsLogger();
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        final String id3 = "sat3";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        propsMap.put(id3, keplerianPropagator3);
        
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        
        multiProp.addEventDetector(extremDistDetect1, id1);
        multiProp.addEventDetector(codedEventsLoggerMulti1.monitorDetector(codingExtremDistDetect11), id1);
        
        multiProp.addEventDetector(extremDistDetect2, id2);
        multiProp.addEventDetector(codedEventsLoggerMulti2.monitorDetector(codingExtremDistDetect21), id2);
        
        multiProp.addEventDetector(extremDistDetect3, id3);
        multiProp.addEventDetector(extremDistDetect31, id3);
        
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(3, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        Assert.assertNotNull(finalStatesMap.get(id3));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);
        final SpacecraftState finalStateMulti3 = finalStatesMap.get(id3);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final PVCoordinates pvsMulti3 = finalStateMulti3.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        final AbsoluteDate stopDateMulti3 = finalStateMulti3.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the target date (because propagation never stops)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(stopDateMulti1, stopDateMulti3, E_14);
        checkDates(target, stopDateMulti1, E_14);
        checkDates(target, stopDateMulti2, E_14);
        checkDates(target, stopDateMulti3, E_14);
        
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);
        checkVectors(pvsMono3.getPosition(), pvsMulti3.getPosition(), VE_14);
        checkVectors(pvsMono3.getVelocity(), pvsMulti3.getVelocity(), VE_14);

        // Check that the detected events between sat1 <-> sat2 and sat2 <-> sat3 agree with the mono-propagation
        checkEventList(codedEventsLoggerMono1.getCodedEventsList().getList(),
                codedEventsLoggerMulti1.getCodedEventsList().getList(), E_14);
        checkEventList(codedEventsLoggerMono2.getCodedEventsList().getList(),
                codedEventsLoggerMulti2.getCodedEventsList().getList(), E_14);
    }
   

   /**
    * Test of usage of an ephemeris for one sat and an analytical propagator for the another one.
    * Some events detectors are added to both sats and the events generated using the hybrid
    * ephemeris-keplerian propagator technique must agree with the pure analytical solution.
    * 
    * Propagation is not stopped before target because all detectors are asked to CONTINUE when
    * detecting an event.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using
    *              keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator using an ephemeris
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsAndEphemeris() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly:
         * - sat1 starts in GCRF +Y direction
         * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
         * 
         * All propagators include an extremum distance detector, it is expected that:
         * - sat1 reaches apogee before sat 2 reaches perigee
         * - propagation continues and sat 2 reaches perigee
         * - propagation continues until target date is reached
         */

        final double dt = this.orbit1.getKeplerianPeriod();
        final double dtEphem = dt / 10.0;
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);
        
        // Create event logger for mono propagations
        final CodedEventsLogger codedEventsLoggerMono = new CodedEventsLogger();

        // Create Keplerian propagators
        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Generate ephemeris for second satellite
        final List<SpacecraftState> ephemListSat2 = MultiAnalyticalPropagatorTest.generateEphemerisFromAnalyticalOrbit(this.orbit2,
                this.initialDate.shiftedBy(-86400.0), target.shiftedBy(86400.0), dtEphem, this.orbit2.getFrame());
        final Ephemeris ephemSat2 = new Ephemeris(ephemListSat2, 8);
        
        // First satellite detectors
        final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator1.addEventDetector(extremDistDetect1);
        
        // Distance to 2nd satellite using the analytical orbit
        final DistanceDetector distDetect12analytical = new DistanceDetector(keplerianPropagator2.getPvProvider(), 0., AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect12analytical = new ExtremaGenericDetector<>(distDetect12analytical,
                ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
                ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final GenericCodingEventDetector codingExtremDistDetect12analytical = new GenericCodingEventDetector(extremDistDetect12analytical, "increasing", "decreasing");
        keplerianPropagator1.addEventDetector(codedEventsLoggerMono.monitorDetector(codingExtremDistDetect12analytical));
        
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        // Second satellite detectors
        final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect2 = new ExtremaGenericDetector<>(distDetect2,
            ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        keplerianPropagator2.addEventDetector(extremDistDetect2);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        
        // Check dates and positions consistency with mono propagations
        Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs but does not stop
        Assert.assertTrue(stopDateMono2.durationFrom(this.initialDate) > 0); // Sat2 event occurs but does not stop
        checkDates(stopDateMono1, target, E_14);
        checkDates(stopDateMono2, target, E_14);

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create event logger for multi propagation
        final CodedEventsLogger codedEventsLoggerMulti = new CodedEventsLogger();
        
        // Distance to 2nd satellite using the ephemeris
        final DistanceDetector distDetect12ephem = new DistanceDetector(ephemSat2, 0., AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect12ephem = new ExtremaGenericDetector<>(distDetect12ephem,
                ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
                ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final GenericCodingEventDetector codingExtremDistDetect12ephem = new GenericCodingEventDetector(extremDistDetect12ephem, "increasing", "decreasing");
        
         
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, ephemSat2);// Use ephemeris for sat 2
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(extremDistDetect1, id1);
        multiProp.addEventDetector(codedEventsLoggerMulti.monitorDetector(codingExtremDistDetect12ephem), id1);
        multiProp.addEventDetector(extremDistDetect2, id2);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the target date (because propagation never stops)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(target, stopDateMulti1, E_14);
        checkDates(target, stopDateMulti2, E_14);
        
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), new Vector3D(1e-12,1e-12,1e-12));
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), new Vector3D(1e-12,1e-12,1e-12));
        
        // Compare the events generated using the Keplerian orbit and the ephemeris
        // They agree to about 1 µs, which is acceptable.
        // The difference comes from the interpolation technique of the ephemeris not matching
        // exactly the underlaying orbit.
        checkEventList(codedEventsLoggerMono.getCodedEventsList().getList(), codedEventsLoggerMulti.getCodedEventsList().getList(), 1e-3);
   }
   
   /**
    * Check the consistency of a multi-sat analytical propagation.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description A multi-analytical propagation in slave mode with extremum distance detector using keplerian orbits
    *
    * @input one multi-sat analytical propagator
    *
    * @output the date of minimum distance between satellites
    *
    * @testPassCriteria results provided by the propagator are those expected
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsInteractingCircularOrbits() throws PatriusException {

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         * The two satellites follow circular orbits called "in" and "out", in resonance 1:2:
         * - satIn starts in GCRF +X direction and describes orbitIn with a given radius
         * - satOut starts in GCRF -X direction and describes orbitOut whose radius is computed so as orbitOut period is twice orbitIn's
         * 
         * From this configuration, it is expected that propagation stops when:
         * - satIn describes a full orbit
         * - satOut describes half an orbit
         * - the propagation end date corresponds to the start date + orbitIn's period
         */
        final double aIn = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // 6 778 137
        final double e = 0;
        final double i = 0;
        final double pa = 0;
        final double raan = 0;
        final Orbit orbitIn = new KeplerianOrbit(aIn, e, i, pa, raan, 0., PositionAngle.TRUE, this.gcrf, this.initialDate, this.mu);
        final double periodRatio = 2.; // orbit out shall have twice the inside orbit's period
        final double aOut = aIn * MathLib.pow(periodRatio, 2. / 3.);
        final Orbit orbitOut = new KeplerianOrbit(aOut, e, i, pa, raan, MathLib.PI, PositionAngle.TRUE, this.gcrf,
            this.initialDate, this.mu);
        
        Assert.assertEquals(2.0, orbitOut.getKeplerianPeriod() / orbitIn.getKeplerianPeriod(), 0.);

        // Allow propagation on a whole orbit out period
        final AbsoluteDate target = this.initialDate.shiftedBy(orbitOut.getKeplerianPeriod());
        
        // The satellite out watches the distance evolution with satellite in
        final DistanceDetector distOfSatIn = new DistanceDetector(orbitIn, 0., AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        final ExtremaGenericDetector<DistanceDetector> extremDistDetect = new ExtremaGenericDetector<>(distOfSatIn,
            ExtremumType.MIN, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
            ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);

        final KeplerianPropagator keplerianPropagatorIn = new KeplerianPropagator(orbitIn);
        keplerianPropagatorIn.setAttitudeProvider(this.attLaw);
        final KeplerianPropagator keplerianPropagatorOut = new KeplerianPropagator(orbitOut);
        keplerianPropagatorOut.setAttitudeProvider(this.attLaw);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String idIn = "satIn";
        final String idOut = "satOut";
        propsMap.put(idIn, keplerianPropagatorIn);
        propsMap.put(idOut, keplerianPropagatorOut);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(extremDistDetect, idOut);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(idIn));
        Assert.assertNotNull(finalStatesMap.get(idOut));
        final SpacecraftState finalStateIn = finalStatesMap.get(idIn);
        final SpacecraftState finalStateOut = finalStatesMap.get(idOut);
        
        final AbsoluteDate expectedDate = this.initialDate.shiftedBy(orbitIn.getKeplerianPeriod());
        checkDates(finalStateIn.getDate(), finalStateOut.getDate(), E_14);
        checkDates(expectedDate, finalStateIn.getDate(), E_14);

    }
   
   /**
    * Two simple analytical propagations in slave mode with detectors: comparison between two analytical
    * propagations and one multi-sat analytical propagation.
    * The second detection shall stop the propagation. Propagation is performed backwards.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description Single analytical and one multi-analytical propagations in slave mode using keplerian orbits
    *
    * @input two analytical propagators and one multi-sat analytical propagator
    *
    * @output PV coordinates from propagations
    *
    * @testPassCriteria results provided by the different propagators are consistent and are the same
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsWithEventsStopSat2Retropolation() throws PatriusException {

        /*
         * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly.
         * Propagation is performed backwards in time.
         * 
         * All propagators include a date detector. Calling T the orbit's period it is expected that:
         * - sat1 triggers an event at t0 - T/2 (propagation continues)
         * - propagation stops for both satellites when sat2 triggers an event at t0 - 2T/3 (case multi)
         */

        final double dt = this.orbit1.getKeplerianPeriod();
        final AbsoluteDate target = this.initialDate.shiftedBy(-dt);
        final int slope = 2;

        KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        final AbsoluteDate date1 = this.initialDate.shiftedBy(-dt/2);
        final MyDateDetector dateDetector1 = new MyDateDetector(date1, slope, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, false);
        keplerianPropagator1.addEventDetector(dateDetector1);
        final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

        KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        final AbsoluteDate date2 = this.initialDate.shiftedBy(-2*dt/3);
        final MyDateDetector dateDetector2 = new MyDateDetector(date2, slope, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, false);
        keplerianPropagator2.addEventDetector(dateDetector2);
        final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);
        
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
        final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
        
        // Check dates and positions consistency with mono propagations
        checkDates(stopDateMono1, target, E_14);
        checkDates(stopDateMono2, date2, E_14);
        Assert.assertTrue(stopDateMono2.durationFrom(target) > 0); // Sat2 event occurs and stops propagation
        Assert.assertTrue(stopDateMono2.durationFrom(stopDateMono1) > 0); // Sat2 event occurs before sat1's event

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         */
        // Re-instanciate propagators so as to be sure they are fully reseted
        keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(dateDetector1, id1);
        multiProp.addEventDetector(dateDetector2, id2);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

        // Check PVs
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the final date of sat 2 (in case mono, because sat2 stops propagation)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(stopDateMono2, stopDateMulti1, E_14);
        checkDates(stopDateMono2, stopDateMulti2, E_14);
        
        // Recompute PVs of satellite 1 at stop date (triggered by satellite 2) thanks to the propagator mono
        final PVCoordinates recomputedSat1Pvs = keplerianPropagator1.propagate(stopDateMono2).getPVCoordinates();
        checkVectors(recomputedSat1Pvs.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(recomputedSat1Pvs.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);

    }
   
   /**
    * Check that detectors are removed if asked to.
    *
    * @throws PatriusException
    * 
    * @testType UT
    *
    * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
    *
    * @description A multi-analytical propagation in slave mode with extremum distance detector using keplerian orbits
    *
    * @input one multi-sat analytical propagator
    *
    * @output the number of events detected
    *
    * @testPassCriteria results provided by the propagator are those expected
    *
    * @referenceVersion 4.14
    *
    * @nonRegressionVersion 4.14
    */
   @Test
    public void testSlaveModeTwoSatsEllipticOrbitsWithEventsRemoveDetector() throws PatriusException {

        /*
         * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
         * The two satellites follow the same orbit (1 period), but with a difference in true anomaly.
         * 
         * All propagators include a date detector. Calling T the orbit's period it is expected that:
         * - sat1 triggers N events every T/N after t0 (not included)
         * - sat2 triggers only 1 event : it should be M events every T/M after t0 (not included) but the detector is removed at first event occurrence
         */

        final double dt = this.orbit1.getKeplerianPeriod();
        final AbsoluteDate target = this.initialDate.shiftedBy(dt);
        final int slope = 2;
        
        // Instantiate detectors (the 2nd one asks for removal at detection, both continue propagation)
        // Slight multiplication factor to avoid precision errors that make the last event detection fail (theoretically
        // occurs exactly at target date, just make it a little bit before)
        final int nSteps1 = 5;
        final double step1 = 0.999*dt/nSteps1;
        final AbsoluteDate date1 = this.initialDate.shiftedBy(step1);
        final MyDateDetector dateDetector1 = new MyDateDetector(date1, step1, slope, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, false);
        final int nSteps2 = 10;
        final double step2 = 0.999 * dt / nSteps2;
        final AbsoluteDate date2 = this.initialDate.shiftedBy(step2);
        final MyDateDetector dateDetector2 = new MyDateDetector(date2, step2, slope, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, true);

        // Instantiate propagators
        final KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
        final KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
        
        // Create propagation map
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, keplerianPropagator1);
        propsMap.put(id2, keplerianPropagator2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
        multiProp.addEventDetector(dateDetector1, id1);
        multiProp.addEventDetector(dateDetector2, id2);
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall both be equal to the target date
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(target, stopDateMulti1, 1E-11);
        checkDates(target, stopDateMulti2, 1E-11);

        // Check the number of detections of each detector
        // The 2nd one, asking for removal, shall have one event only (should have nSteps2 events if no removal
        // happened, as shown by the other detector that detects nSteps1 events)
        Assert.assertEquals(nSteps1, dateDetector1.getNDetections());
        Assert.assertEquals(1, dateDetector2.getNDetections());

    }
   
    /**
     * Check that the propagator acts nominally when using a {@link MultiEventDetector} such as
     * {@link ThreeBodiesAngleDetector}. The test in mono propagation is taken from
     * {@link ThreeBodiesAngleDetectorTest#testTwoOrbits} and kept as such (reference).
     * 
     * @throws PatriusException
     * 
     * @testType UT
     *
     * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)} confronted to
     *               {@link ThreeBodiesAngleDetector}
     *
     * @description A multi-analytical propagation in slave mode with ThreeBodiesAngleDetector using keplerian orbits
     *
     * @input one multi-sat analytical propagator
     *
     * @output output orbits and angles between bodies
     *
     * @testPassCriteria results provided by the propagator are those expected
     *
     * @referenceVersion 4.14
     *
     * @nonRegressionVersion 4.14
     */
   @Test
   public void testSlaveModeMultiEventDetector() throws PatriusException {
       
       /*
        * Reference test (kept as such).
        * Some initializations are performed first (attributes of the other test class).
        */
       
       // Date initialization
       final AbsoluteDate iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTAI());
       
       // Historic test
       // sets up the orbits:
       final double mu = CelestialBodyFactory.getEarth().getGM();
       // first orbit:
       final double a1 = 8000000;
       final Orbit orbit1 = new KeplerianOrbit(a1, 0.01, 0.2, 0, 0, FastMath.PI, PositionAngle.MEAN,
           FramesFactory.getGCRF(), iniDate, mu);
       // second orbit:
       final double a2 = 8200000;
       final Orbit orbit2 = new KeplerianOrbit(a2, 0.005, 0.3, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getGCRF(),
           iniDate, mu);
       // set up the propagator:
       final KeplerianPropagator propagator1 = new KeplerianPropagator(orbit1);
       // detects the following angle between satellite 1 - Earth - satellite 2:
       final double angle = FastMath.PI / 2;
       final ThreeBodiesAngleDetector detector = new ThreeBodiesAngleDetector(orbit1, CelestialBodyFactory.getEarth(),
           orbit2, angle);
       propagator1.addEventDetector(detector);
       // the propagation should stop when the angle is reached:
       final SpacecraftState curState = propagator1.propagate(iniDate.shiftedBy(100000));

       // computes the satellites positions:
       final Vector3D position1 = orbit1.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();
       final Vector3D position2 = orbit2.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();
       // computes the angle between the two satellites:
       final double actualAngle = Vector3D.angle(position1, position2);
       // verifies that this angle is = PI / 2:
       Assert.assertEquals(angle, actualAngle, Utils.epsilonTest);
       
       /*
        * Test in case multi.
        */
       final KeplerianPropagator propagator2 = new KeplerianPropagator(orbit2);
       final Map<String, Propagator> propsMap = new HashMap<>();
       final String id1 = "sat1";
       final String id2 = "sat2";
       final String earthId = "Earth";
       propsMap.put(id1, propagator1);
       propsMap.put(id2, propagator2);
       propsMap.put(earthId, new PVCoordinatesPropagator(this.earth, iniDate, mu, this.gcrf));
       
       final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, iniDate);
       multiProp.addEventDetector(detector, earthId);
       final Map<String, SpacecraftState> finalStateMulti = multiProp.propagate(iniDate.shiftedBy(100000));
       
       // Assertions
       final SpacecraftState state1 = finalStateMulti.get(id1);
       final SpacecraftState state2 = finalStateMulti.get(id2);
       final Vector3D position1Multi = state1.getPVCoordinates().getPosition();
       final Vector3D position2Multi = state2.getPVCoordinates().getPosition();
       final double actualAngleMulti = Vector3D.angle(position1Multi, position2Multi);
       checkDates(state1.getDate(), state2.getDate(), E_14);
       checkDates(curState.getDate(), state1.getDate(), E_14);
       checkDates(curState.getDate(), state2.getDate(), E_14);
       checkVectors(position1, position1Multi, VE_14.scalarMultiply(10.)); // loss of precision here
       checkVectors(position2, position2Multi, VE_14);
       Assert.assertEquals(angle, actualAngleMulti, Utils.epsilonTest);
       
       /*
        * /!\ Keep these comments /!\
        * 
        * Difference of PVs coming from:
        *   SpacecraftState.getPVCoordinates();
        *       this.orbit.getPVCoordinates();
        *           KeplerianOrbit.initPVCoordinates();
        *               this.parameters.getCartesianParameters();
        *                   this.initPVCoordinatesElliptical(p, q) (same p and q values)
        *                       final double mE = this.getEccentricAnomaly();
        *                           final double[] sincos = MathLib.sinAndCos(v);
        * 
        * Note: the loss of precision is due to the computation of the true anomaly when shifting orbits.
        * True anomaly is the same when instantiating KeplerianParameters (relative error 1.55336E-16).
        * But the error propagates when computing sine/cosine to compute the eccentric anomaly, whose value is precise
        * enough when computing relative error (1.55370E-16), but the 10E-13 digit changes. The value is used again to
        * compute sine/cosine (relative error not OK, 1.24E-15 and 3.92E-14 respectively).
        * As a result coordinates of position in the orbital plane also have a relative error slightly higher than 1E-14
        * (3.72E-14 for x and -1.30E-15 for y).
        */
//       System.out.println("trueAnomaly relError = " + (45.74223309011373-45.74223309011372)/45.74223309011373);
//       System.out.println("mE relError = " + (45.7324020179105-45.732402017910495)/45.7324020179105);
//       System.out.println("sin(mE) relError = " + (0.9839672492178307-0.983967249217832)/0.9839672492178307);
//       System.out.println("cos(mE) relError = " + (-0.17834924296642093+0.17834924296641394)/(-0.17834924296642093));
//       System.out.println("x relError = " + (-1506793.9437313676+1506793.9437313115)/(-1506793.9437313676));
//       System.out.println("y relError = " + (7871344.397002794-7871344.397002804)/7871344.397002794);

   }
    
    /**
     * Two analytical propagations in master mode without detectors: comparison between two analytical propagations and
     * one multi-sat analytical propagation.
     *
     * @throws PatriusException
     * 
     * @testType UT
     *
     * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
     *
     * @description Single analytical and one multi-analytical propagations in master mode (without detectors) using
     *              keplerian orbits
     *
     * @input two analytical propagators and one multi-sat analytical propagator
     *
     * @output PV coordinates and attitude from propagations
     *
     * @testPassCriteria results provided by the different propagators are consistent and are the same
     *
     * @referenceVersion 4.14
     *
     * @nonRegressionVersion 4.14
     */
    @Test
    public void testMasterModeTwoSatsCircularOrbitsNoEvents() throws PatriusException {
        
        // Orbits
        final double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // 6 778 137
        final KeplerianOrbit orbit1 = new KeplerianOrbit(a, 0, 0, 0, 0, 0, PositionAngle.TRUE, this.gcrf, this.initialDate, this.mu);
        final KeplerianOrbit orbit2 = new KeplerianOrbit(a, 0, 0, 0, 0, MathLib.PI, PositionAngle.TRUE, this.gcrf, this.initialDate, this.mu);
        
        /*
         * Instantiate propagators
         */
        final KeplerianPropagator monoProp1 = new KeplerianPropagator(orbit1, this.attLaw);
        final KeplerianPropagator monoProp2 = new KeplerianPropagator(orbit2, this.attLaw);
        final Map<String, Propagator> propsMap = new HashMap<>();
        final String id1 = "sat1";
        final String id2 = "sat2";
        propsMap.put(id1, monoProp1);
        propsMap.put(id2, monoProp2);
        final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);

        /*
         * Set master mode for all propagators
         */
        monoProp1.setMasterMode(this.defaultDt, this.monoStepHandler);
        monoProp2.setMasterMode(this.defaultDt, this.monoStepHandler);
        multiProp.setMasterMode(this.defaultDt, this.multiStepHandler);

        /*
         * Propagations.
         * Check that propagation mode for mono propagators is master and then slave (switch between modes is performed
         * when multi propagation starts).
         */
        final int nSteps = 15;
        final AbsoluteDate target = this.initialDate.shiftedBy(this.defaultDt * nSteps);
        // TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
        final SpacecraftState finalStateMono1 = monoProp1.propagate(target);
        final SpacecraftState finalStateMono2 = monoProp2.propagate(target);
        Assert.assertEquals(monoProp1.getMode(), MultiPropagator.MASTER_MODE);
        Assert.assertEquals(monoProp2.getMode(), MultiPropagator.MASTER_MODE);
        // ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
        final Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

        Assert.assertEquals(this.multiStepHandler.getCount(), this.monoStepHandler.getCount());
        Assert.assertEquals(monoProp1.getMode(), MultiPropagator.SLAVE_MODE);
        Assert.assertEquals(monoProp2.getMode(), MultiPropagator.SLAVE_MODE);
        Assert.assertEquals(multiProp.getMode(), MultiPropagator.MASTER_MODE);
        Assert.assertEquals(2, finalStatesMap.size());
        Assert.assertNotNull(finalStatesMap.get(id1));
        Assert.assertNotNull(finalStatesMap.get(id2));
        final SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
        final SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);
        final PVCoordinates pvsMono1 = finalStateMono1.getPVCoordinates();
        final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
        final PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
        final PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
        final AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
        final AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
        
        // Final states dates shall be the same in case multi
        // They shall be equal to the target date (because propagation never stops)
        checkDates(stopDateMulti1, stopDateMulti2, E_14);
        checkDates(target, stopDateMulti1, E_14);
        checkDates(target, stopDateMulti2, E_14);

        // Check PVs
        checkVectors(pvsMono1.getPosition(), pvsMulti1.getPosition(), VE_14);
        checkVectors(pvsMono1.getVelocity(), pvsMulti1.getVelocity(), VE_14);
        checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
        checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);
        
        // Check attitudes
        final Rotation rotGcrfToSatMono1 = finalStateMono1.getAttitude().getRotation();
        final Rotation rotGcrfToSatMono2 = finalStateMono2.getAttitude().getRotation();
        final Rotation rotGcrfToSatMulti1 = finalStateMulti1.getAttitude().getRotation();
        final Rotation rotGcrfToSatMulti2 = finalStateMulti2.getAttitude().getRotation();
        Assert.assertTrue(MathLib.abs(rotGcrfToSatMono1.applyInverseTo(rotGcrfToSatMulti1).getAngle()) < E_14);
        Assert.assertTrue(MathLib.abs(rotGcrfToSatMono2.applyInverseTo(rotGcrfToSatMulti2).getAngle()) < E_14);
        
    }
    
    /**
     * Two simple analytical propagations in slave mode with detectors: comparison between two analytical
     * propagations and one multi-sat analytical propagation.
     * The second detection shall stop the propagation.
     *
     * @throws PatriusException
     * 
     * @testType UT
     *
     * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
     *
     * @description Single analytical and one multi-analytical propagations in master mode using keplerian orbits
     *
     * @input two analytical propagators and one multi-sat analytical propagator
     *
     * @output PV coordinates from propagations
     *
     * @testPassCriteria results provided by the different propagators are consistent and are the same
     *
     * @referenceVersion 4.14
     *
     * @nonRegressionVersion 4.14
     */
    @Test
     public void testMasterModeTwoSatsEllipticOrbitsWithEventsStopSat2() throws PatriusException {

         /*
          * TWO PROPAGATIONS WITH TWO ANALYTICAL PROPAGATORS
          * The two satellites follow the same orbit (1 period), but with a difference in true anomaly:
          * - sat1 starts in GCRF +Y direction
          * - sat2 starts a bit "after" GCRF -X direction (orbit's course), that is to say just after reaching apogee
          * 
          * All propagators include an extremum distance detector, it is expected that:
          * - sat1 reaches apogee before sat 2 reaches perigee
          * - propagation continues until sat 2 reaches perigee
          * - propagation stops for both satellites when sat2 perigee extremum is detected (case multi)
          */

         final double dt = this.orbit1.getKeplerianPeriod() / 2;
         final AbsoluteDate target = this.initialDate.shiftedBy(dt);

         KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
         keplerianPropagator1.setMasterMode(this.defaultDt, this.monoStepHandler);
         final DistanceDetector distDetect1 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
             AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
         final ExtremaGenericDetector<DistanceDetector> extremDistDetect1 = new ExtremaGenericDetector<>(distDetect1,
             ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
             ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
         keplerianPropagator1.addEventDetector(extremDistDetect1);
         final SpacecraftState finalStateMono1 = keplerianPropagator1.propagate(target);

         KeplerianPropagator keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
         keplerianPropagator2.setMasterMode(this.defaultDt, this.monoStepHandler);
         final DistanceDetector distDetect2 = new DistanceDetector(this.earth, 0., AbstractDetector.DEFAULT_MAXCHECK,
             AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
         final ExtremaGenericDetector<DistanceDetector> extremDistDetect2 = new ExtremaGenericDetector<>(distDetect2,
             ExtremumType.MIN_MAX, ExtremaGenericDetector.DEFAULT_HALF_COMPUTATION_STEP,
             ExtremaGenericDetector.DEFAULT_MAXCHECK, ExtremaGenericDetector.DEFAULT_THRESHOLD, Action.STOP, Action.STOP);
         keplerianPropagator2.addEventDetector(extremDistDetect2);
         final SpacecraftState finalStateMono2 = keplerianPropagator2.propagate(target);

         final PVCoordinates pvsMono2 = finalStateMono2.getPVCoordinates();
         final AbsoluteDate stopDateMono1 = finalStateMono1.getDate();
         final AbsoluteDate stopDateMono2 = finalStateMono2.getDate();
         
         // Check dates and positions consistency with mono propagations
         Assert.assertTrue(stopDateMono1.durationFrom(this.initialDate) > 0); // Sat1 event occurs but does not stop
         checkDates(stopDateMono1, target, E_14);
         Assert.assertTrue(stopDateMono2.durationFrom(target) < 0); // Sat2 event occurs and stops propagation

         /*
          * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
          */
         // Re-instanciate propagators so as to be sure they are fully reseted
         keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
         keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
         
         // Create propagation map
         Map<String, Propagator> propsMap = new HashMap<>();
         final String id1 = "sat1";
         final String id2 = "sat2";
         propsMap.put(id1, keplerianPropagator1);
         propsMap.put(id2, keplerianPropagator2);
         final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
         multiProp.addEventDetector(extremDistDetect1, id1);
         multiProp.addEventDetector(extremDistDetect2, id2);
         multiProp.setMasterMode(this.defaultDt, this.multiStepHandler);
         Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);

         Assert.assertEquals(2, finalStatesMap.size());
         Assert.assertNotNull(finalStatesMap.get(id1));
         Assert.assertNotNull(finalStatesMap.get(id2));
         SpacecraftState finalStateMulti1 = finalStatesMap.get(id1);
         SpacecraftState finalStateMulti2 = finalStatesMap.get(id2);

         // Check PVs
         PVCoordinates pvsMulti1 = finalStateMulti1.getPVCoordinates();
         PVCoordinates pvsMulti2 = finalStateMulti2.getPVCoordinates();
         AbsoluteDate stopDateMulti1 = finalStateMulti1.getDate();
         AbsoluteDate stopDateMulti2 = finalStateMulti2.getDate();
         
         // Final states dates shall be the same in case multi
         // They shall be equal to the final date of sat 2 (in case mono, because sat2 stops propagation)
         checkDates(stopDateMulti1, stopDateMulti2, E_14);
         checkDates(stopDateMono2, stopDateMulti1, E_14);
         checkDates(stopDateMono2, stopDateMulti2, E_14);
         
         // Recompute PVs of satellite 1 at stop date (triggered by satellite 2) thanks to the propagator mono
         PVCoordinates recomputedSat1Pvs = keplerianPropagator1.propagate(stopDateMono2).getPVCoordinates();
         checkVectors(recomputedSat1Pvs.getPosition(), pvsMulti1.getPosition(), VE_14);
         checkVectors(recomputedSat1Pvs.getVelocity(), pvsMulti1.getVelocity(), VE_14);
         checkVectors(pvsMono2.getPosition(), pvsMulti2.getPosition(), VE_14);
         checkVectors(pvsMono2.getVelocity(), pvsMulti2.getVelocity(), VE_14);
         
         /*
          * ONE SINGLE PROPAGATION WITH A MULTI-SAT ANALYTICAL PROPAGATOR
          * Replay test in master mode with the other master mode setter (implies fixedStepSize = NaN)
          * Since the step is recomputed it is not the same (300 in previous case, ~55 now from debug view), therefore
          * the precision on the result is low
          */
         // New precision constants
         final double datePrecision = 2e-6;
         final Vector3D posPrecision = VE_14.scalarMultiply(5e5); // 5e-9
         final Vector3D velPrecision = VE_14.scalarMultiply(5e6); // 5e-10
         
         // Re-instanciate propagators so as to be sure they are fully reseted
         keplerianPropagator1 = new KeplerianPropagator(this.orbit1);
         keplerianPropagator2 = new KeplerianPropagator(this.orbit2);
         
         // Create propagation map
         propsMap = new HashMap<>();
         propsMap.put(id1, keplerianPropagator1);
         propsMap.put(id2, keplerianPropagator2);
         final MultiAnalyticalPropagator multiProp2 = new MultiAnalyticalPropagator(propsMap, this.initialDate);
         multiProp2.addEventDetector(extremDistDetect1, id1);
         multiProp2.addEventDetector(extremDistDetect2, id2);
         multiProp2.setMasterMode(new MultiPatriusStepNormalizer(this.defaultDt, this.multiStepHandler));
         finalStatesMap = multiProp2.propagate(target);

         Assert.assertEquals(2, finalStatesMap.size());
         Assert.assertNotNull(finalStatesMap.get(id1));
         Assert.assertNotNull(finalStatesMap.get(id2));
         finalStateMulti1 = finalStatesMap.get(id1);
         finalStateMulti2 = finalStatesMap.get(id2);

         // Check PVs
         pvsMulti1 = finalStateMulti1.getPVCoordinates();
         pvsMulti2 = finalStateMulti2.getPVCoordinates();
         stopDateMulti1 = finalStateMulti1.getDate();
         stopDateMulti2 = finalStateMulti2.getDate();
         
         // Final states dates shall be the same in case multi
         // They shall be equal to the final date of sat 2 (in case mono, because sat2 stops propagation)
         checkDates(stopDateMulti1, stopDateMulti2, E_14);
         checkDates(stopDateMono2, stopDateMulti1, datePrecision);
         checkDates(stopDateMono2, stopDateMulti2, datePrecision);
         
         // Recompute satellite 1 and 2 PVs at stop date (triggered by satellite 2) thanks to mono propagators
         recomputedSat1Pvs = keplerianPropagator1.propagate(stopDateMulti1).getPVCoordinates();
         final PVCoordinates recomputedSat2Pvs = keplerianPropagator2.propagate(stopDateMulti2).getPVCoordinates();
         checkVectors(recomputedSat1Pvs.getPosition(), pvsMulti1.getPosition(), posPrecision);
         checkVectors(recomputedSat1Pvs.getVelocity(), pvsMulti1.getVelocity(), velPrecision);
         checkVectors(recomputedSat2Pvs.getPosition(), pvsMulti2.getPosition(), VE_14);
         checkVectors(recomputedSat2Pvs.getVelocity(), pvsMulti2.getVelocity(), VE_14);

     }
    
    /**
     * Cover exceptions cases with their catch clauses.
     *
     * @throws PatriusException
     * 
     * @testType UT
     *
     * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
     *
     * @description Cover exceptions by using mocks
     *
     * @input various classes mocks (e.g. propagators)
     *
     * @output exception messages
     *
     * @testPassCriteria expected exceptions are thrown and their messages are those expected
     *
     * @referenceVersion 4.14
     *
     * @nonRegressionVersion 4.14
     */
     @Test
     public void testExceptions() throws PatriusException {
         
         class SpacecraftStateMock extends SpacecraftState {
             private static final long serialVersionUID = 1L;
             public SpacecraftStateMock(final AttitudeProvider attProviderForces, final AttitudeProvider attProviderEvents,
                                        final Orbit orbitIn) {
                 super(attProviderForces, attProviderEvents, orbitIn);
             }
             /** {@inheritDoc} */
             @Override
             public SpacecraftState shiftedBy(final double dt) throws PatriusException {
                 throw new PatriusException(PatriusMessages.SIMPLE_MESSAGE,
                     "spacecraft state shiftedBy patrius exception");
             }
         }
         class MockKeplerianProp extends KeplerianPropagator {
             private static final long serialVersionUID = 1L;
             public MockKeplerianProp(final Orbit initialOrbit) throws PropagationException {
                 super(initialOrbit);
             }
             /** {@inheritDoc} */
             @Override
             public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
                 return new SpacecraftStateMock(attLaw, attLaw, orbit1);
             }
         }
         class MockMultiAnalyticalProp1 extends MultiAnalyticalPropagator {
             public MockMultiAnalyticalProp1(final AbsoluteDate referenceDate) throws PatriusException {
                 super(referenceDate);
             }
             /** {@inheritDoc} */
             @Override
             public Map<String, SpacecraftState> getInitialStates() throws PatriusException {
                 throw new PatriusException(PatriusMessages.SIMPLE_MESSAGE, "get initial states patrius exception");
             }
         }
         class MockMultiAnalyticalProp2 extends MultiAnalyticalPropagator {
            public MockMultiAnalyticalProp2(final AbsoluteDate referenceDate) throws PatriusException {
                super(referenceDate);
            }
            @Override
            protected void manageStateFrame() throws PatriusException {
                throw new PatriusException(PatriusMessages.SIMPLE_MESSAGE, "manage state frame patrius exception");
            }
         }
         class MockMultiAnalyticalProp3 extends MultiAnalyticalPropagator {
             public MockMultiAnalyticalProp3(final AbsoluteDate referenceDate) throws PatriusException {
                 super(referenceDate);
             }
             @Override
            protected void manageStateFrame() throws PropagationException {
                 throw new PropagationException(PatriusMessages.SIMPLE_MESSAGE, "manage state frame propagation exception");
             }
         }

         /*
          * Create propagators and throw exceptions
          */
         final KeplerianPropagator keplerianPropagator1 = new KeplerianPropagator(orbit1);
         keplerianPropagator1.setAttitudeProvider(this.attLaw);
         final String id1 = "sat1";
         final AbsoluteDate target = this.initialDate.shiftedBy(10.);
         
         final MultiAnalyticalPropagator multiPropOk = new MultiAnalyticalPropagator(this.initialDate);
         final MockMultiAnalyticalProp1 multiPropMock1 = new MockMultiAnalyticalProp1(this.initialDate);
         multiPropMock1.addPropagator(keplerianPropagator1, id1);
         final MockMultiAnalyticalProp2 multiPropMock2 = new MockMultiAnalyticalProp2(this.initialDate);
         multiPropMock2.addPropagator(keplerianPropagator1, id1);
         final MockMultiAnalyticalProp3 multiPropMock3 = new MockMultiAnalyticalProp3(this.initialDate);
         multiPropMock3.addPropagator(keplerianPropagator1, id1);
         final MultiAnalyticalPropagator multiPropMock4 = new MultiAnalyticalPropagator(this.initialDate);
         multiPropMock4.addPropagator(new MockKeplerianProp(this.orbit1), id1);
         
         try {
             multiPropOk.propagate(target);
             Assert.fail();
         } catch (final PropagationException pe) {
             Assert.assertTrue("A non-empty collection is expected (propagators map)".equals(pe.getMessage()));
         }
         try {
             multiPropOk.addPropagator(keplerianPropagator1, id1);
             final Orbit orbitNonInertialframe = new KeplerianOrbit(7e7, 0., 0., 0., 0., 0., PositionAngle.TRUE,
                 FramesFactory.getITRF(), this.initialDate, this.mu);
             final SpacecraftState ssNonInertialFrame = new SpacecraftState(orbitNonInertialframe);
             multiPropOk.resetSingleInitialState(ssNonInertialFrame, id1);
             multiPropOk.propagate(target);
             Assert.fail();
         } catch (final PropagationException pe) {
             Assert.assertTrue("The propagation frame must be inertial or pseudo inertial".equals(pe.getMessage()));
         }
         try {
             multiPropMock1.propagate(target);
             Assert.fail();
         } catch (final PropagationException pe) {
             Assert.assertTrue("get initial states patrius exception".equals(pe.getMessage()));
         }
         try {
             multiPropMock2.propagate(target);
             Assert.fail();
         } catch (final PropagationException pe) {
             Assert.assertTrue("manage state frame patrius exception".equals(pe.getMessage()));
         }
         try {
             multiPropMock3.propagate(target);
             Assert.fail();
         } catch (final PropagationException pe) {
             Assert.assertTrue("manage state frame propagation exception".equals(pe.getMessage()));
         }
         try {
             multiPropMock4.propagate(this.initialDate, target);
             Assert.fail();
         } catch (final PropagationException pe) {
             Assert.assertTrue("spacecraft state shiftedBy patrius exception".equals(pe.getMessage()));
         }
     
     }
     
     /**
      * Cover cases that imply a detector with RESET_STATE.
      *
      * @throws PatriusException
      * 
      * @testType UT
      *
      * @testedMethod {@link MultiAnalyticalPropagator#propagate(AbsoluteDate)}
      *
      * @description Test propagation cases with a detector that uses RESET_STATE
      *
      * @input one multi-sat analytical propagator
      *
      * @output output orbits and angles between bodies
      *
      * @testPassCriteria results provided by the propagator are those expected
      *
      * @referenceVersion 4.14
      *
      * @nonRegressionVersion 4.14
      */
      @Test
      public void testResetStateCases() throws PatriusException {

          final double maxcheck = 1.;
          final double threshold = 0.;
          final double propDuration = 101.;
          final double eventOccurenceDelay = 3 * maxcheck; // > maxcheck so that detection is performed nominally
          final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
          final AbsoluteDate target = initDate.shiftedBy(propDuration);
          final AbsoluteDate eventDate1 = initDate.shiftedBy(propDuration / 3);
          final AbsoluteDate eventDate2 = initDate.shiftedBy(2 * propDuration / 3);

          // The satellite out watches the distance evolution with satellite in
          final MyDateDetector dateDetect1 = new MyDateDetector(eventDate1, eventOccurenceDelay, 1, maxcheck, threshold, Action.CONTINUE, false);
          final MyDateDetector dateDetect2 = new MyDateDetector(eventDate1, eventOccurenceDelay, 0, maxcheck, threshold, Action.CONTINUE, false);
          final MyDateDetector dateDetect3 = new MyDateDetector(eventDate2, 2, maxcheck, threshold, Action.RESET_STATE, false);
          final MyDateDetector dateDetect4 = new MyDateDetector(eventDate2, 2, maxcheck, threshold, Action.RESET_STATE, true);

          final KeplerianPropagator keplerianPropagator = new KeplerianPropagator(this.orbit1);
          keplerianPropagator.setAttitudeProvider(this.attLaw);

          // Create propagation map
          final Map<String, Propagator> propsMap = new HashMap<>();
          final String satId = "satID";
          propsMap.put(satId, keplerianPropagator);
          final MultiAnalyticalPropagator multiProp = new MultiAnalyticalPropagator(propsMap, this.initialDate);
          multiProp.addEventDetector(dateDetect1, satId);
          multiProp.addEventDetector(dateDetect2, satId);
          multiProp.addEventDetector(dateDetect3, satId);
          multiProp.addEventDetector(dateDetect4, satId);
          
          Map<String, SpacecraftState> finalStatesMap = multiProp.propagate(target);
          
          Assert.assertEquals(1, finalStatesMap.size());
          Assert.assertNotNull(finalStatesMap.get(satId));
          SpacecraftState finalState = finalStatesMap.get(satId);
          
          checkDates(target, finalState.getDate(), E_14);
          Assert.assertEquals(0, dateDetect1.getNDetections()); // detector 1 slope is descending, so no event is detected
          final int expNOccurences = (int) (target.durationFrom(eventDate1) / eventOccurenceDelay) + 1;
          Assert.assertEquals(expNOccurences, dateDetect2.getNDetections());
          
          /*
           * Additional case to cover the STOP (5th) if clause in case of another event calling RESET_STATE (3rd)
           */
          final MyDateDetector dateDetect5 = new MyDateDetector(eventDate2, 2, maxcheck, threshold, Action.STOP, true);
          multiProp.clearEventsDetectors();
          multiProp.addEventDetector(dateDetect3, satId);
          multiProp.addEventDetector(dateDetect5, satId);
          
          finalStatesMap = multiProp.propagate(target);
          
          Assert.assertEquals(1, finalStatesMap.size());
          Assert.assertNotNull(finalStatesMap.get(satId));
          finalState = finalStatesMap.get(satId);
          
          checkDates(eventDate2, finalState.getDate(), E_14);

      }

    /**
     * Initializations.
     *
     * @throws PatriusException
     *
     * @since 3.0
     */
    @Before
    public void setUp() throws PatriusException {

        Utils.clear();
        // Initializations
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        // Start date
        this.initialDate = AbsoluteDate.J2000_EPOCH.shiftedBy(0., TimeScalesFactory.getTAI());

        // Define MU
        this.mu = Constants.WGS84_EARTH_MU;
        
        // GCRF frame
        this.gcrf = FramesFactory.getGCRF();
        
        // Earth body
        this.earth = CelestialBodyFactory.getEarth();
        
        // Default elliptical orbits
        final double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // 6 778 137
        final double e = 0.03; // perigee = a(1-e) = 6 574 792.89 m and apogee = a(1+e) = 6 981 481.11 m
        this.orbit1 = new KeplerianOrbit(a, e, 0., 0., 0., MathLib.PI / 2, PositionAngle.TRUE, this.gcrf,
            this.initialDate, this.mu);
        this.orbit2 = new KeplerianOrbit(a, e, 0., 0., 0., MathLib.PI * 1.05, PositionAngle.TRUE, this.gcrf,
            this.initialDate, this.mu);
        this.orbit3 = new KeplerianOrbit(a, e, 0., 0., 0., 2.0 * MathLib.PI / 3.0, PositionAngle.TRUE, this.gcrf,
                this.initialDate, this.mu);

        // Earth-centric attitude law
        this.attLaw = new BodyCenterPointing(this.gcrf);
        
        // Default step for step handlers
        this.defaultDt = 300.;
        
        // Default step handler for mono propagation
        this.monoStepHandler = new MyMonoStepHandler();
        
        // Default step handler for multi propagation
        this.multiStepHandler = new MyMultiStepHandler();
        
    }

    /**
     * Compare each terms of two vectors by relative differences.
     */
    private static void checkVectors(final Vector3D expected, final Vector3D actual, final Vector3D tol) {
        
        if (MathLib.abs(expected.getX()) > E_14) {
            Assert.assertEquals(0., MathLib.abs((expected.getX() - actual.getX()) / expected.getX()), tol.getX());
        } else {
            Assert.assertEquals(0., MathLib.abs(expected.getX() - actual.getX()), tol.getX());
        }
        
        if (MathLib.abs(expected.getY()) > E_14) {
            Assert.assertEquals(0., MathLib.abs((expected.getY() - actual.getY()) / expected.getY()), tol.getY());
        } else {
            Assert.assertEquals(0., MathLib.abs(expected.getY() - actual.getY()), tol.getY());
        }
        
        if (MathLib.abs(expected.getZ()) > E_14) {
            Assert.assertEquals(0., MathLib.abs((expected.getZ() - actual.getZ()) / expected.getZ()), tol.getZ());
        } else {
            Assert.assertEquals(0., MathLib.abs(expected.getZ() - actual.getZ()), tol.getZ());
        }
        
    }
    
    /**
     * Compare dates by absolute value of the duration between them.
     */
    private static void checkDates(final AbsoluteDate expected, final AbsoluteDate actual, final double tol) {
        Assert.assertTrue(MathLib.abs(actual.durationFrom(expected)) < tol);
    }
    
    /**
     * Compare two event lists : number of events in each list, and that each event is found in both lists (type and date). 
     * 
     * @param eventList1 First event list.
     * @param eventList2 Second event list.
     */
    private static void checkEventList(final List<CodedEvent> eventList1, final List<CodedEvent> eventList2, final double tol) {
        Assert.assertEquals(eventList1.size(), eventList2.size());
        
        for (int i = 0 ; i < eventList1.size() ; i++) {
            final CodedEvent evt1 = eventList1.get(i);
            final CodedEvent evt2 = eventList2.get(i);
            Assert.assertEquals(evt1.getCode(), evt2.getCode());
            checkDates(evt1.getDate(), evt2.getDate(), tol);
        }
    }
    
    /**
     * Generates an ephemeris from a given analytical orbit between two dates with a fixed step size.
     * The last point is guaranteed to be exactly at the end date. The time step may therefore not be identical for the last step.
     * 
     * @param orbit Input orbit.
     * @param startDate Start date of the ephemeris.
     * @param endDate End date of the ephemeris.
     * @param dt Time step of the ephemeris in seconds.
     * @param frame Frame in which the ephemeris must be computed.
     * @return
     * @throws PatriusException If frame conversion fails.
     */
    private static List<SpacecraftState> generateEphemerisFromAnalyticalOrbit(final Orbit orbit,
            final AbsoluteDate startDate, final AbsoluteDate endDate, final double dt, final Frame frame) throws PatriusException {
        final List<SpacecraftState> ephem = new ArrayList<>();
        
        // Compute full duration of ephemeris
        final double duration = endDate.durationFrom(startDate);
        
        // Loop over every time step
        for (int i = 0 ; i < (int)(duration/dt) ; i++) {
            // Compute current date for current step
            final AbsoluteDate currentDate = startDate.shiftedBy(i * dt);
            
            // Get PV at current date in the desired frame
            final PVCoordinates pv = orbit.getPVCoordinates(currentDate, frame);   

            // Convert the PV into a SpacecraftState using a CartesianOrbit
            final SpacecraftState ss = new SpacecraftState(new CartesianOrbit(pv, frame,
                    currentDate, orbit.getMu()));
            
            // Add current SpacecraftState to the ephemeris
            ephem.add(ss);
        }
        
        // Add last point at end date
        if (ephem.get(ephem.size()-1).getDate() != endDate) {
            ephem.add(new SpacecraftState(new CartesianOrbit(orbit.getPVCoordinates(endDate, frame),
                    frame, endDate, orbit.getMu())));
        }
        
        return ephem;
    }
    
    private class MyMonoStepHandler implements PatriusFixedStepHandler {
        /** Serializable UID. */
        private static final long serialVersionUID = 5003349266622633767L;
        public int count;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.count = 0;
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast)
            throws PropagationException {
            this.count++;
        }

        public int getCount() {
            return this.count;
        }
    }
    
    private class MyMultiStepHandler implements MultiPatriusFixedStepHandler {
        /** Serializable UID. */
        private static final long serialVersionUID = 2151689080824095205L;
        int count;

        @Override
        public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
            this.count = 0;
        }

        @Override
        public void handleStep(final Map<String, SpacecraftState> currentStates, final boolean isLast)
            throws PropagationException {
            this.count++;
        }

        public int getCount() {
            return this.count;
        }
    }
    
    /**
     * This detector triggers an event when reaching a given date.
     */
    private class MyDateDetector extends AbstractDetector {

        private static final long serialVersionUID = 2194486837569694560L;
        /**
         * Date triggering event.
         */
        AbsoluteDate date;
        
        /**
         * Duration shift for next dates to be detected (constant step).
         */
        double stepDuration;
        
        /**
         * Number of events detected.
         */
        int nDetections = 0;
        
        /**
         * Default constructor.
         */
        public MyDateDetector() {
            this(AbsoluteDate.J2000_EPOCH, 2, 0., 0., Action.CONTINUE, false);
        }
        
        /**
         * @param date
         * @param slopeSelectionIn
         * @param maxCheckIn
         * @param thresholdIn
         * @param actionIn
         * @param removeIn
         */
        public MyDateDetector(final AbsoluteDate date, final int slopeSelectionIn, final double maxCheckIn,
                              final double thresholdIn, final Action actionIn, final boolean removeIn) {
            this(date, 0., slopeSelectionIn, maxCheckIn, thresholdIn, actionIn, removeIn);
        }
        
        /**
         * @param date
         * @param stepDuration
         * @param slopeSelectionIn
         * @param maxCheckIn
         * @param thresholdIn
         * @param actionIn
         * @param removeIn
         */
        public MyDateDetector(final AbsoluteDate date, final double stepDuration, final int slopeSelectionIn,
                              final double maxCheckIn, final double thresholdIn, final Action actionIn,
                              final boolean removeIn) {
            super(slopeSelectionIn, maxCheckIn, thresholdIn, actionIn, removeIn);
            this.date = new AbsoluteDate(date.getEpoch(), date.getOffset());
            this.stepDuration = stepDuration;
        }

        @Override
        public EventDetector copy() {
            return null;
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            return s.getDate().durationFrom(this.date);
        }
        
        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
            this.date = this.date.shiftedBy(this.stepDuration);
            this.nDetections++;
            return super.eventOccurred(s, increasing, forward);
        }
        
        /**
         * @return the number of detected events
         */
        public int getNDetections() {
            return this.nDetections;
        }
        
    }
    
    /**
     * This detector triggers an event when reaching a given date.
     */
    private class MockDetector extends AbstractDetector {
        
        private static final long serialVersionUID = -7957234542071622454L;

        public MockDetector() {
            super(0., 0.);
        }

        @Override
        public EventDetector copy() {
            return null;
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            throw new PatriusException(PatriusMessages.ILLEGAL_STATE);
        }
        
    }
    
    /**
     * This propagator does not have an attitude provider returned by getAttitudeProvider (coverage purpose).
     */
    private class MyKeplerianPropagator extends KeplerianPropagator {
        
        private static final long serialVersionUID = -7957234542071622454L;
        
        public MyKeplerianPropagator(final Orbit initialOrbit) throws PropagationException {
            super(initialOrbit);
        }
        
        /** {@inheritDoc} */
        @Override
        public AttitudeProvider getAttitudeProvider() {
            return null;
        }
        
    }
}
