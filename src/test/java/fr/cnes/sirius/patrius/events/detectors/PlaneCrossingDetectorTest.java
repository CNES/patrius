/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-129:22/08/2024: [PATRIUS] Interpolation de trajectoire avec la methode de Lagrange
 * VERSION:4.14:OPENFD-160:22/08/2024: [PATRIUS] Repere defini par 2 directions
 * VERSION:4.14:OPENFD-142:22/08/2024: [PATRIUS] Nouvel evenement PlaneCrossingDetector
 * VERSION:4.14:OPENFD-311:22/08/2024: [PATRIUS] getInputCoord sur EllipsoidPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;



import org.junit.Assert;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsList;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.postprocessing.CodingEventDetector;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.postprocessing.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;
import fr.cnes.sirius.patrius.Utils;

/**
 * Class to test PlaneCrossingDetector class. In particular, it assesses if the NodeDetector class can be recreated
 * using this class.
 * 
 * @author Mathilde Lefevre
 *
 */
public class PlaneCrossingDetectorTest {
    /**
     * Tests the different constructors for PlaneCrossingDetectors and the getters for the class.
     * 
     * @throws PatriusException
     */
    @Test
    public void testGetters() throws PatriusException {
        final Frame referenceFrame = FramesFactory.getITRF();
        final Vector3D point = Vector3D.ZERO;
        final Vector3D normalVector = Vector3D.PLUS_K;
        final PlaneCrossingDetector detector = new PlaneCrossingDetector(point, normalVector, referenceFrame,
            PlaneCrossingDetector.INCREASING, Action.CONTINUE, false, PlaneCrossingDetector.DEFAULT_MAXCHECK,
            PlaneCrossingDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(point, detector.getPoint());
        Assert.assertEquals(referenceFrame, detector.getFrame());
        Assert.assertEquals(normalVector, detector.getNormalVector());
    }
    
    /**
     * Tests the exception is thrown in the case of a plane defined with a zero normal vector.
     * 
     * @throws PatriusException
     */
    @Test
    public void testExceptionZeroNormalVector() throws PatriusException{
        final Frame referenceFrame = FramesFactory.getITRF();
        final Vector3D point = Vector3D.ZERO;
        final Vector3D normalVector = Vector3D.ZERO;
        try {
            final PlaneCrossingDetector detector = new PlaneCrossingDetector(point, normalVector, referenceFrame,
                PlaneCrossingDetector.INCREASING, Action.CONTINUE, false, PlaneCrossingDetector.DEFAULT_MAXCHECK,
                PlaneCrossingDetector.DEFAULT_THRESHOLD);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(),
                PatriusMessages.ZERO_NORM_VECTOR_FOR_PLANE_DEFINITION.getSourceString());
        }
        try {
            final PlaneCrossingDetector detector =
                new PlaneCrossingDetector(point, normalVector, referenceFrame, Action.STOP, Action.STOP, true, true,
                    PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(),
                PatriusMessages.ZERO_NORM_VECTOR_FOR_PLANE_DEFINITION.getSourceString());
        }

    }

    /**
     * Tests all the constructors in different cases to assert the Action objects are correctly initialized in different
     * cases.
     * 
     * @throws PatriusException
     */
    @Test
    public void testSpecificConstructors() throws PatriusException {
        // Constructor with default point = (0, 0, 0)
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Vector3D normalVector = Vector3D.PLUS_K;

        // Constructor with one action at increasing crossing
        final Vector3D point = new Vector3D(1,1,1);
        final PlaneCrossingDetector detectorWithAction = new PlaneCrossingDetector(point, normalVector, referenceFrame,
            PlaneCrossingDetector.INCREASING, Action.CONTINUE, true, PlaneCrossingDetector.DEFAULT_MAXCHECK,
            PlaneCrossingDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(Action.CONTINUE, detectorWithAction.getActionAtEntry());
        Assert.assertEquals(true, detectorWithAction.isRemoveAtEntry());
        Assert.assertEquals(null, detectorWithAction.getActionAtExit());
        Assert.assertEquals(false, detectorWithAction.isRemoveAtExit());

        // Constructor with one action at decreasing crossing
        final PlaneCrossingDetector detectorWithActionDN =
            new PlaneCrossingDetector(point, normalVector, referenceFrame, PlaneCrossingDetector.DECREASING,
                Action.CONTINUE, true, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(null, detectorWithActionDN.getActionAtEntry());
        Assert.assertEquals(false, detectorWithActionDN.isRemoveAtEntry());
        Assert.assertEquals(Action.CONTINUE, detectorWithActionDN.getActionAtExit());
        Assert.assertEquals(true, detectorWithActionDN.isRemoveAtExit());

        // Constructor with identical actions at increasing and decreasing crossing
        final PlaneCrossingDetector detectorWithIdenticalAction =
            new PlaneCrossingDetector(point, normalVector, referenceFrame, PlaneCrossingDetector.INCREASING_DECREASING,
                Action.STOP, true, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(Action.STOP, detectorWithIdenticalAction.getActionAtEntry());
        Assert.assertEquals(true, detectorWithIdenticalAction.isRemoveAtEntry());
        Assert.assertEquals(Action.STOP, detectorWithIdenticalAction.getActionAtExit());
        Assert.assertEquals(true, detectorWithIdenticalAction.isRemoveAtExit());

        // Constructor with different actions at increasing and decreasing crossing
        final PlaneCrossingDetector detectorWithDifferentAction =
            new PlaneCrossingDetector(point, normalVector, referenceFrame, Action.CONTINUE, Action.STOP, true, false,
                PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        Assert.assertEquals(Action.CONTINUE, detectorWithDifferentAction.getActionAtEntry());
        Assert.assertEquals(true, detectorWithDifferentAction.isRemoveAtEntry());
        Assert.assertEquals(Action.STOP, detectorWithDifferentAction.getActionAtExit());
        Assert.assertEquals(false, detectorWithDifferentAction.isRemoveAtExit());

    }


    /**
     * Tests the copy() function for PlaneCrossingDetector.
     * 
     * @throws PatriusException
     * @throws PropagationException
     */
    @Test
    public void testCopy() throws PropagationException, PatriusException {
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Vector3D normalVector = Vector3D.PLUS_K;
        
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        final Orbit orbit = new EquatorialOrbit(24400e3, 0, 0, 0, 0, 0, PositionAngle.TRUE, referenceFrame, initialDate,
            Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        
        // Test copy case Decreasing
        final PlaneCrossingDetector detector =
            new PlaneCrossingDetector(normalVector, referenceFrame, PlaneCrossingDetector.DECREASING, Action.CONTINUE,
                false, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        final PlaneCrossingDetector copyOfDetector = (PlaneCrossingDetector) detector.copy();
        compareDetectorWithCopy(detector, copyOfDetector);
        Assert.assertEquals(Action.CONTINUE,
            copyOfDetector.eventOccurred(propagator.propagate(initialDate.shiftedBy(3600 * 24)), true, true));

        // Test copy case Increasing
        final PlaneCrossingDetector detectorAN =
            new PlaneCrossingDetector(normalVector, referenceFrame, PlaneCrossingDetector.INCREASING, Action.CONTINUE,
                true, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        final PlaneCrossingDetector copyOfDetectorAN = (PlaneCrossingDetector) detectorAN.copy();
        compareDetectorWithCopy(detectorAN, copyOfDetectorAN);
        Assert.assertEquals(Action.CONTINUE,
            copyOfDetectorAN.eventOccurred(propagator.propagate(initialDate.shiftedBy(3600 * 24)), true, true));

        // Test copy case Increasing_Decreasing
        final PlaneCrossingDetector detectorANDN =
            new PlaneCrossingDetector(Vector3D.ZERO, normalVector, referenceFrame, Action.CONTINUE, Action.STOP, false,
                false, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        final PlaneCrossingDetector copyOfDetectorANDN = (PlaneCrossingDetector) detectorANDN.copy();
        compareDetectorWithCopy(detectorANDN, copyOfDetectorANDN);
        Assert.assertEquals(Action.CONTINUE,
            copyOfDetectorANDN.eventOccurred(propagator.propagate(initialDate.shiftedBy(3600 * 24)), true, true));

    }


    /**
     * Tests a detector and its copy are defined with the same plane.
     * 
     * @param detector
     *        A PlaneCrossingDetector to be compared with its copy.
     * @param copy
     *        The detector that has to be a copy of the PlaneCrossingDetector.
     */
    public void compareDetectorWithCopy(final PlaneCrossingDetector detector, final PlaneCrossingDetector copy) {
        Assert.assertEquals(detector.getPoint(), copy.getPoint());
        Assert.assertEquals(detector.getFrame(), copy.getFrame());
        Assert.assertEquals(detector.getNormalVector(), copy.getNormalVector());
    }

    /**
     * Tests the creation of a detector for a plane not containing the origin and not aligned with an axis, and the
     * event dates are correctly aligned with the expected dates.
     * 
     * @throws PropagationException
     * 
     */
    @Test
    public void testOffsetInOriginDates() throws PropagationException {
        // Definition of the plane to be crossed
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Vector3D point = new Vector3D(0, 0, 2250e3 * Math.sqrt(2));
        final Vector3D normalVector = new Vector3D(0, 1, 1);

        // Creation of the plane crossing detector
        final PlaneCrossingDetector detector =
            new PlaneCrossingDetector(point, normalVector, referenceFrame, Action.CONTINUE, Action.CONTINUE, false,
                false, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);

        // Creation of an equatorial circular orbit such that the first crossing occurs after an eighth of the orbit and
        // the second crossing a fourth of the period later.
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        final Orbit orbit =
            new EquatorialOrbit(2 * 2250e3, 0, 0, 0, 0, 0, PositionAngle.TRUE, referenceFrame,
            initialDate,
            Constants.WGS84_EARTH_MU);

        // Retrieving the period of the orbit
        final double period = orbit.getKeplerianPeriod();
        
        // Expected crossing dates
        final AbsoluteDate firstCrossingDate = initialDate.shiftedBy(period * 1 / 8);
        final AbsoluteDate secondCrossingDate = initialDate.shiftedBy(period * 3 / 8);

        // Propagation of the orbit with the detector and the results are retrieved
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);
        final CodingEventDetector codeur = new GenericCodingEventDetector(detector, "Increasing", "Decreasing");
        final CodedEventsLogger logger = new CodedEventsLogger();
        propagator.addEventDetector(logger.monitorDetector(codeur));

        propagator.propagate(initialDate.shiftedBy(period));
        final CodedEventsList detectedEvents = logger.getCodedEventsList();
        final AbsoluteDate firstDetectedCrossingDate = detectedEvents.getList().get(0).getDate();
        final AbsoluteDate secondDetectedCrossingDate = detectedEvents.getList().get(1).getDate();

        // Assertion of the results
        Assert.assertEquals(firstDetectedCrossingDate.getEpoch(), firstCrossingDate.getEpoch(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(firstDetectedCrossingDate.getOffset(), firstCrossingDate.getOffset(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(secondDetectedCrossingDate.getEpoch(), secondCrossingDate.getEpoch(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        // The addition of normalization in detector impacts the detection
        Assert.assertEquals(secondDetectedCrossingDate.getOffset(), 0.5773589527731247,
            Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * Tests the creation of a detector for a plane not containing the origin and not aligned with an axis.
     * 
     * @throws PropagationException
     */
    @Test
    public void testOffsetInOrigin() throws PropagationException {
        // Definition of the plane to be crossed
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Vector3D point = new Vector3D(0, 0, 2250.0);
        final Vector3D normalVector = new Vector3D(0, 1, 1);
        final Vector3D normalVector2 = new Vector3D(0, -1, -1);

        // Creation of the plane crossing detector
        final PlaneCrossingDetector detector =
            new PlaneCrossingDetector(point, normalVector, referenceFrame, Action.CONTINUE, Action.CONTINUE, true, true,
                PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);
        final PlaneCrossingDetector detector2 =
            new PlaneCrossingDetector(point, normalVector2, referenceFrame, Action.CONTINUE, Action.CONTINUE, true,
                true,
                PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);

        // Creation of an equatorial circular orbit such that the altitude of the orbit is the same as the altitude of
        // the point of the reference frame
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        final Orbit orbit = new EquatorialOrbit(24400e3, 0, 0, 0, 0, 0, PositionAngle.TRUE, referenceFrame, initialDate,
            Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detector);
        final CodingEventDetector codeur = new GenericCodingEventDetector(detector, "Ascending", "Descending");
        final CodedEventsLogger logger = new CodedEventsLogger();
        propagator.addEventDetector(logger.monitorDetector(codeur));

        propagator.addEventDetector(detector2);
        final CodingEventDetector codeur2 = new GenericCodingEventDetector(detector2, "Ascending", "Descending");
        final CodedEventsLogger logger2 = new CodedEventsLogger();
        propagator.addEventDetector(logger2.monitorDetector(codeur2));

        propagator.propagate(initialDate.shiftedBy(3600 * 36));
        final CodedEventsList detectedEvents = logger.getCodedEventsList();
        final CodedEventsList detectedEvents2 = logger2.getCodedEventsList();
        for (int i = 0; i < detectedEvents.getList().size(); i++) {
            Assert.assertEquals(detectedEvents.getList().get(i).getDate(), detectedEvents2.getList().get(i).getDate());
        }

    }

    /**
     * Compares a NodeDetector and an equivalent PlaneCrossingDetector for event detection.
     * 
     * @throws PropagationException
     */
    @Test
    public void testComparisonWithNodeDetector() throws PropagationException {
        final Frame referenceFrame = FramesFactory.getGCRF();

        // Test with propagation
        final NodeDetector detector = new NodeDetector(referenceFrame, NodeDetector.DEFAULT_MAXCHECK,
            PlaneCrossingDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        Assert.assertEquals(detector.getNormalVector(), Vector3D.PLUS_K);

        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(24400e3, 0.72, MathLib.toRadians(5), MathLib.toRadians(180),
            MathLib.toRadians(2), MathLib.toRadians(180), PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate,
            Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);


        // Equivalent PlaneCrossing Detector
        final Vector3D point = Vector3D.ZERO;
        final Vector3D normalVector = Vector3D.PLUS_K;
        final PlaneCrossingDetector detectorEquivalent =
            new PlaneCrossingDetector(point, normalVector, referenceFrame, Action.CONTINUE, Action.CONTINUE, false,
                false, PlaneCrossingDetector.DEFAULT_MAXCHECK, PlaneCrossingDetector.DEFAULT_THRESHOLD);

        // Comparison and display of the results obtained with the NodeDetector and the Equivalent detector built using
        // its parent class
        propagator.addEventDetector(detector);
        propagator.addEventDetector(detectorEquivalent);
        final CodingEventDetector codeur = new GenericCodingEventDetector(detector, "Ascending", "Descending");
        final CodedEventsLogger logger = new CodedEventsLogger();
        propagator.addEventDetector(logger.monitorDetector(codeur));

        propagator.addEventDetector(detectorEquivalent);
        final CodingEventDetector codeur2 = new GenericCodingEventDetector(detectorEquivalent, "Ascending",
            "Descending");
        final CodedEventsLogger logger2 = new CodedEventsLogger();
        propagator.addEventDetector(logger2.monitorDetector(codeur2));

        propagator.propagate(initialDate.shiftedBy(3600 * 36));
        final CodedEventsList detectedEvents = logger.getCodedEventsList();
        final CodedEventsList detectedEvents2 = logger2.getCodedEventsList();
        for (int i = 0; i < detectedEvents.getList().size(); i++) {
            Assert.assertEquals(detectedEvents.getList().get(i).getDate(), detectedEvents2.getList().get(i).getDate());
        }

    }

    /**
     * Tests the EventOccured function.
     * 
     * @throws PatriusException
     */
    @Test
    public void testEventOccured() throws PatriusException {
        // Builds the PlaneCrossingDetector
        final Vector3D point = Vector3D.ZERO;
        final Vector3D normalVector = Vector3D.PLUS_K;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final PlaneCrossingDetector detector = new PlaneCrossingDetector(normalVector, referenceFrame,
            PlaneCrossingDetector.INCREASING, Action.STOP, false, PlaneCrossingDetector.DEFAULT_MAXCHECK,
            PlaneCrossingDetector.DEFAULT_THRESHOLD);
        // Builds the orbit for propagation
        final AbsoluteDate initialDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(24400e3, 0.72, MathLib.toRadians(5), MathLib.toRadians(180),
            MathLib.toRadians(2), MathLib.toRadians(180), PositionAngle.TRUE, referenceFrame, initialDate,
            Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        propagator.addEventDetector(detector);
        final SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(36 * 3600));
        final Action action = detector.eventOccurred(finalState, true, true);
        Assert.assertEquals(Action.STOP, action);
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
