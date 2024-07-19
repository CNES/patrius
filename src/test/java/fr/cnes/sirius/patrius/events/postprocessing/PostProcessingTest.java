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
 * created 13/03/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3131:10/05/2022:[PATRIUS] Anomalie dans la methode applyTo de la classe OrCriterion 
 * VERSION:4.9:DM:DM-3130:10/05/2022:[PATRIUS] Robustifier le calcul des phenomenes des CodedEventsLogger, ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.CodingEventDetector;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Class that tests the post processing.
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class PostProcessingTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Element type filter
         * 
         * @featureDescription test the filter that removes or keeps a specified type of events / phenomena.
         * 
         * @coveredRequirements DV-EVT_67
         */
        ELEMENT_TYPE,
        /**
         * @featureTitle Occurrence filter
         * 
         * @featureDescription test the filter that removes or keeps the N k occurrences of a specified type of events /
         *                     phenomena.
         * 
         * @coveredRequirements DV-EVT_67, DV-EVT_80
         */
        OCCURRENCE,
        /**
         * @featureTitle Delay filter
         * 
         * @featureDescription test the filter that creates delayed events for a given type.
         * 
         * @coveredRequirements DV-EVT_67, DV-EVT_70, DV-EVT_30
         */
        DELAY,
        /**
         * @featureTitle Merge filter
         * 
         * @featureDescription test the filter that merges two successive phenomena when the time lapse between them is
         *                     below a given value.
         * 
         * @coveredRequirements DV-EVT_67, DV-VISI_60
         */
        MERGE,
        /**
         * @featureTitle Time filter
         * 
         * @featureDescription test the filter that removes or keeps only all of the events and phenomena that happen
         *                     during a given time interval.
         * 
         * @coveredRequirements DV-EVT_67
         */
        TIME,
        /**
         * @featureTitle Duration filter
         * 
         * @featureDescription test the filter that removes phenomena witch are longer / shorter than a given duration.
         * 
         * @coveredRequirements DV-EVT_67
         */
        DURATION,
        /**
         * @featureTitle Events during phenomena filter
         * 
         * @featureDescription test the filter that removes or keeps only a specified type of events during a specified
         *                     type of phenomena.
         * 
         * @coveredRequirements DV-EVT_67
         */
        EVENTS_DURING_PHENOMENA,
        /**
         * @featureTitle "and" and "or" criteria
         * 
         * @featureDescription "and" : criterion that to the phenomena list of a TimeLine object the phenomena
         *                     corresponding to each time intervals when phenomena of particular types A and B occur at
         *                     the same time. "or" : criterion that to the phenomena list of a TimeLine object the
         *                     phenomena corresponding to each time intervals when phenomena of particular types A OR B
         *                     occur.
         * 
         * @coveredRequirements DV-EVT_67, DV-EVT_30
         */
        AND_OR_CRITERION,
        /**
         * @featureTitle Merging of two timelines
         * 
         * @featureDescription tests the merging of two timelines.
         * 
         * @coveredRequirements DV-EVT_67
         */
        MERGE_TIMELINES
    }

    /** Propagation interval. */
    private static AbsoluteDateInterval interval;
    /** Events logger. */
    private static CodedEventsLogger log;
    /** Initial state. */
    private static SpacecraftState initialState;
    /** Apogee/perigee passages detector. */
    private static CodingEventDetector apogeePergieePassages;
    /** Nodes passages detector. */
    private static CodingEventDetector nodesPassages;
    /** Eclipse detector. */
    private static CodingEventDetector eclipse;
    /** Penumbra detector. */
    private static CodingEventDetector penumbra;
    /** Station visibility. */
    private static CodingEventDetector stationVisi35;
    /** Station visibility. */
    private static CodingEventDetector stationVisi30;
    /** Station visibility. */
    private static CodingEventDetector stationVisi20;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ELEMENT_TYPE}
     * 
     * @testedMethod {@link ElementTypeFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the element type filter
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output filtered timeline
     * 
     * @testPassCriteria the given elements have been correctly filtered
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testF1() throws PatriusException {
        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // initial events number
        int eventsNumber = list.getCodedEventsList().size();
        // initial phenomena number
        int phenomenaNumber = list.getPhenomenaList().size();

        // number of elements that have to be removed
        int removedItems = 0;
        // number of elements that have to be kept
        int keptItems = 0;

        Set<Phenomenon> phenomena;
        Set<CodedEvent> events;

        Set<Phenomenon> previousPhenomena;
        Set<CodedEvent> previousEvents;

        /*
         * Filter on the eclipse phenomena
         */

        String code = "eclipse";

        // phenomena list linked to the eclipse phenomena, this list should not be empty
        previousPhenomena = list.getPhenomena().getPhenomena(code, null, null);
        Assert.assertTrue(!previousPhenomena.isEmpty());

        // filter creation
        final PostProcessing f1 = new ElementTypeFilter(code, true);
        // number of elements that have to be removed
        removedItems = previousPhenomena.size();

        // application of the filter on the timeline
        f1.applyTo(list);

        // phenomena list of the timeline : it should be empty now
        phenomena = list.getPhenomena().getPhenomena(code, null, null);
        Assert.assertTrue(phenomena.isEmpty());

        // when a phenomenon is removed from the list, the associated events should be also removed : but for the first
        // and the last eclipses, the start event and the end event may be undefined, it is the case here.
        Assert.assertEquals(eventsNumber - 2 * removedItems + 2, list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber - removedItems, list.getPhenomenaList().size());

        final LinkedList<Phenomenon> phenList = new LinkedList<>(previousPhenomena);
        Assert.assertEquals(phenList.getFirst().getStartingEvent().getCode(), "UNDEFINED_EVENT");
        Assert.assertEquals(phenList.getLast().getEndingEvent().getCode(), "UNDEFINED_EVENT");

        eventsNumber = list.getCodedEventsList().size();
        phenomenaNumber = list.getPhenomenaList().size();

        /*
         * Filter on the penumbra entrance
         */

        code = "penumbra entrance";

        // penumbra entrance events list, this list should not be empty
        previousEvents = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(!previousEvents.isEmpty());

        // creation of the filter
        final PostProcessing f2 = new ElementTypeFilter(code, true);

        // number of elements that have to be removed
        removedItems = previousEvents.size();

        // application of the filter on the timeline
        f2.applyTo(list);

        // events list of the timeline : it should be empty
        events = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(events.isEmpty());

        // phenomena list of the timeline : it should contain less than 1 phenomenon (phenomenon with undefined events
        // can remain in the list : typically a penumbra that has started just before the propagation)
        phenomena = list.getPhenomena().getPhenomena("penumbra", null, null);
        if (!phenomena.isEmpty()) {
            // if the phenomena list is not empty then the first phenomenon should has an undefined started event
            Assert.assertTrue(phenomena.size() == 1);
            Assert.assertEquals(phenList.getFirst().getStartingEvent().getCode(), "UNDEFINED_EVENT");
        }

        // when an event which is related to a phenomenon is removed, the associated phenomenon has to be removed also.
        // In our case, one penumbra exit event remains.
        Assert.assertEquals(eventsNumber - 2 * removedItems + 1, list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber - removedItems, list.getPhenomenaList().size());

        eventsNumber = list.getCodedEventsList().size();
        phenomenaNumber = list.getPhenomenaList().size();

        /*
         * Filter apogee passage
         */

        code = "apogee passage";

        // apogee passages events, this list should not be empty
        previousEvents = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(!previousEvents.isEmpty());

        // creation of the filter
        final PostProcessing f3 = new ElementTypeFilter(code, true);

        // number of elements that have to be removed
        removedItems = previousEvents.size();

        // application of the filter on the timeline
        f3.applyTo(list);

        // events list of the timeline : it should be empty
        events = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(events.isEmpty());

        // the phenomena number remains constant and the events number decreases by the previsouly computed amount
        Assert.assertEquals(eventsNumber - removedItems, list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber, list.getPhenomenaList().size());

        eventsNumber = list.getCodedEventsList().size();
        phenomenaNumber = list.getPhenomenaList().size();

        /*
         * Filter right ascending node (it keeps only those events)
         */

        code = "ascending node";

        // apogee passages events, this list should not be empty
        previousEvents = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(!previousEvents.isEmpty());

        // creation of the filter
        final PostProcessing f4 = new ElementTypeFilter(code, false);

        // number of elements that have to be kept
        keptItems = previousEvents.size();

        // application of the filter on the timeline
        f4.applyTo(list);

        // there is no phenomena left, the events number is equal to the previsouly computed amount
        Assert.assertEquals(keptItems, list.getCodedEventsList().size());
        Assert.assertEquals(0, list.getPhenomenaList().size());

        // all of the remained events are ascending node event
        for (final CodedEvent event : list.getCodedEventsList()) {
            Assert.assertEquals(code, event.getCode());
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ELEMENT_TYPE}
     * 
     * @testedMethod {@link ElementTypeFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the element type filter
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output filtered timeline
     * 
     * @testPassCriteria the given elements have been correctly filtered
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testF1ListCodes() throws PatriusException {
        // creation of the timeline from the logger used during the propagation
        Timeline list = new Timeline(log, interval, null);
        // initial phenomena number
        final int phenomenaNumber = list.getPhenomenaList().size();

        // number of elements that have to be removed
        int removedItems = 0;
        final Set<Phenomenon> previousPhenomena = new TreeSet<>();

        // filter on the following phenomena:
        final String code1 = "eclipse";
        final String code2 = "station B visibility";
        final String code3 = "station C visibility";
        // list of codes:
        final List<String> listCode = new ArrayList<>();
        listCode.add(code1);
        listCode.add(code2);
        listCode.add(code3);
        // phenomena list linked to the eclipse phenomena, this list should not be empty
        previousPhenomena.addAll(list.getPhenomena().getPhenomena(code1, null, null));
        previousPhenomena.addAll(list.getPhenomena().getPhenomena(code2, null, null));
        previousPhenomena.addAll(list.getPhenomena().getPhenomena(code3, null, null));
        Assert.assertTrue(!previousPhenomena.isEmpty());

        /**
         * Filter 1 : removes all the phenomena of the selected type
         */
        final PostProcessing f1 = new ElementTypeFilter(listCode, true);
        // elements that have to be removed:
        final Set<Phenomenon> keptItems1 = list.getPhenomena().getPhenomena(code1, null, null);
        final Set<Phenomenon> keptItems2 = list.getPhenomena().getPhenomena(code2, null, null);
        final Set<Phenomenon> keptItems3 = list.getPhenomena().getPhenomena(code3, null, null);

        removedItems = keptItems1.size() + keptItems2.size() + keptItems3.size();
        // application of the filter on the timeline
        f1.applyTo(list);

        // phenomena list of the timeline : it should be empty:
        Assert.assertEquals(0, list.getPhenomena().getPhenomena(code1, null, null).size());
        Assert.assertEquals(0, list.getPhenomena().getPhenomena(code2, null, null).size());
        Assert.assertEquals(0, list.getPhenomena().getPhenomena(code3, null, null).size());
        Assert.assertEquals(phenomenaNumber - removedItems, list.getPhenomenaList().size());

        /**
         * Filter 2 : keeps only the phenomena of the selected type
         */
        // Re-creation of the timeline from the logger:
        list = new Timeline(log, interval, null);
        // filter that keeps all the elements with the selected code:
        final PostProcessing f2 = new ElementTypeFilter(listCode, false);
        // number of elements that have to be kept:
        removedItems = previousPhenomena.size();

        // application of the filter on the timeline
        f2.applyTo(list);

        // phenomena list of the timeline : it should contain only the selected elements:
        Assert.assertEquals(keptItems1.size(), list.getPhenomena().getPhenomena(code1, null, null).size());
        Assert.assertEquals(keptItems2.size(), list.getPhenomena().getPhenomena(code2, null, null).size());
        Assert.assertEquals(keptItems3.size(), list.getPhenomena().getPhenomena(code3, null, null).size());
        Assert.assertEquals(removedItems, list.getPhenomenaList().size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TIME}
     * 
     * @testedMethod {@link TimeFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the time filter
     * 
     * @input timeline that contains events and phenomena that are inside, outside the time filter interval as well as
     *        phenomena which overlaps this interval
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the correct ones
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testF2() throws PatriusException {
        // creation of the timeline
        final CodedEventsLogger dummyLogger = new CodedEventsLogger();

        // creation of the time interval
        AbsoluteDate startDate = new AbsoluteDate(2012, 03, 18, 12, 0, 0, TimeScalesFactory.getTAI());
        AbsoluteDate endDate = new AbsoluteDate(2012, 03, 21, 12, 0, 0, TimeScalesFactory.getTAI());

        AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(dummyLogger, interval, null);

        startDate = new AbsoluteDate(2012, 03, 19, 12, 0, 0, TimeScalesFactory.getTAI());
        endDate = new AbsoluteDate(2012, 03, 20, 12, 0, 0, TimeScalesFactory.getTAI());

        interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate, IntervalEndpointType.CLOSED);

        /*
         * events
         */
        final CodedEvent eventBefore = new CodedEvent("event", "before", startDate.shiftedBy(-10), false);
        final CodedEvent eventLowerLimit = new CodedEvent("event", "lower limit", startDate, false);
        final CodedEvent eventDuring = new CodedEvent("event", "during", startDate.shiftedBy(10), false);
        final CodedEvent eventUpperLimit = new CodedEvent("event", "upper limit", endDate, false);
        final CodedEvent eventAfter = new CodedEvent("event", "after", endDate.shiftedBy(10), false);
        final CodedEvent otherEventBefore = new CodedEvent("other event", "before", startDate.shiftedBy(-10), false);
        final CodedEvent otherEventLowerLimit = new CodedEvent("other event", "lower limit", startDate, false);
        final CodedEvent otherEventDuring = new CodedEvent("other event", "during", startDate.shiftedBy(10), false);
        final CodedEvent otherEventUpperLimit = new CodedEvent("other event", "upper limit", endDate, false);
        final CodedEvent otherEventAfter = new CodedEvent("other event", "after", endDate.shiftedBy(10), false);

        list.addCodedEvent(eventAfter);
        list.addCodedEvent(eventUpperLimit);
        list.addCodedEvent(eventDuring);
        list.addCodedEvent(eventLowerLimit);
        list.addCodedEvent(eventBefore);
        list.addCodedEvent(otherEventAfter);
        list.addCodedEvent(otherEventUpperLimit);
        list.addCodedEvent(otherEventDuring);
        list.addCodedEvent(otherEventLowerLimit);
        list.addCodedEvent(otherEventBefore);

        /*
         * phenomena
         */
        CodedEvent eclipseEntrance = CodedEvent.buildUndefinedEvent(startDate.shiftedBy(-120), true);
        CodedEvent eclipseExit = CodedEvent.buildUndefinedEvent(startDate.shiftedBy(-60), false);
        final Phenomenon eclipseBefore = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "eclipse", "before");
        final Phenomenon otherEclipseBefore = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "other eclipse",
            "before");

        list.addPhenomenon(eclipseBefore);
        list.addPhenomenon(otherEclipseBefore);
        list.addCodedEvent(eclipseEntrance);
        list.addCodedEvent(eclipseExit);

        eclipseEntrance = CodedEvent.buildUndefinedEvent(startDate.shiftedBy(-30), true);
        eclipseExit = CodedEvent.buildUndefinedEvent(startDate.shiftedBy(30), false);
        final Phenomenon eclipseOverlapping1 = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "eclipse",
            "overlapping");
        final Phenomenon otherEclipseOverlapping1 = new Phenomenon(eclipseEntrance, true, eclipseExit, true,
            "other eclipse",
            "overlapping");

        list.addPhenomenon(eclipseOverlapping1);
        list.addPhenomenon(otherEclipseOverlapping1);
        list.addCodedEvent(eclipseEntrance);
        list.addCodedEvent(eclipseExit);

        eclipseEntrance = CodedEvent.buildUndefinedEvent(startDate.shiftedBy(60), true);
        eclipseExit = CodedEvent.buildUndefinedEvent(endDate.shiftedBy(-60), false);
        final Phenomenon eclipseDuring = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "eclipse", "during");
        final Phenomenon otherEclipseDuring = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "other eclipse",
            "during");

        list.addPhenomenon(eclipseDuring);
        list.addPhenomenon(otherEclipseDuring);
        list.addCodedEvent(eclipseEntrance);
        list.addCodedEvent(eclipseExit);

        eclipseEntrance = CodedEvent.buildUndefinedEvent(endDate.shiftedBy(-30), true);
        eclipseExit = CodedEvent.buildUndefinedEvent(endDate.shiftedBy(30), false);
        final Phenomenon eclipseOverlapping2 = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "eclipse",
            "overlapping");
        final Phenomenon otherEclipseOverlapping2 = new Phenomenon(eclipseEntrance, true, eclipseExit, true,
            "other eclipse",
            "overlapping");

        list.addPhenomenon(eclipseOverlapping2);
        list.addPhenomenon(otherEclipseOverlapping2);
        list.addCodedEvent(eclipseEntrance);
        list.addCodedEvent(eclipseExit);

        eclipseEntrance = CodedEvent.buildUndefinedEvent(endDate.shiftedBy(60), true);
        eclipseExit = CodedEvent.buildUndefinedEvent(endDate.shiftedBy(120), false);
        final Phenomenon eclipseAfter = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "eclipse", "after");
        final Phenomenon otherEclipseAfter = new Phenomenon(eclipseEntrance, true, eclipseExit, true, "otherEclipse",
            "after");

        list.addPhenomenon(eclipseAfter);
        list.addPhenomenon(otherEclipseAfter);
        list.addCodedEvent(eclipseEntrance);
        list.addCodedEvent(eclipseExit);

        List<Phenomenon> phenomena;
        List<CodedEvent> events;

        /*
         * Filter on a time interval : removes the events and phenomena inside the interval
         */
        // filter on the following events/phenomena:
        final String code1 = "event";
        final String code2 = "eclipse";
        // list of codes:
        final List<String> listCode = new ArrayList<>();
        listCode.add(code1);
        listCode.add(code2);
        PostProcessing f1 = new TimeFilter(code1, interval, true);

        f1.applyTo(list);

        f1 = new TimeFilter(code2, interval, true);

        f1.applyTo(list);

        phenomena = list.getPhenomenaList();
        events = list.getCodedEventsList();

        // the eclipse "before" is kept
        Assert.assertTrue(phenomena.contains(eclipseBefore));
        Assert.assertTrue(events.contains(eclipseBefore.getStartingEvent())
                && events.contains(eclipseBefore.getEndingEvent()));
        // the "other" eclipse "before" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseBefore));
        // the eclipse "after" is kept
        Assert.assertTrue(phenomena.contains(eclipseAfter));
        Assert.assertTrue(events.contains(eclipseAfter.getStartingEvent())
                && events.contains(eclipseAfter.getEndingEvent()));
        // the "other" eclipse "after" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseAfter));
        // the eclipse "during" is removed
        Assert.assertTrue(!phenomena.contains(eclipseDuring));
        Assert.assertTrue(!events.contains(eclipseDuring.getStartingEvent())
                && !events.contains(eclipseDuring.getEndingEvent()));
        // the "other" eclipse "during" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseDuring));
        // the eclipse "overlapping1" is cut
        Assert.assertTrue(!phenomena.contains(eclipseOverlapping1));
        Assert.assertTrue(events.contains(eclipseOverlapping1.getStartingEvent())
                && !events.contains(eclipseOverlapping1.getEndingEvent()));
        // the "other" eclipse "overlapping1" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseOverlapping1));
        // the eclipse "overlapping2" is cut
        Assert.assertTrue(!phenomena.contains(eclipseOverlapping2));
        Assert.assertTrue(!events.contains(eclipseOverlapping2.getStartingEvent())
                && events.contains(eclipseOverlapping2.getEndingEvent()));
        // the "other" eclipse "overlapping2" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseOverlapping2));

        // only the "during" event and the event that occurs on the upper limit of the interval are removed
        Assert.assertTrue(events.contains(eventBefore));
        Assert.assertTrue(events.contains(eventAfter));
        Assert.assertTrue(!events.contains(eventDuring));
        Assert.assertTrue(events.contains(eventLowerLimit));
        Assert.assertTrue(!events.contains(eventUpperLimit));
        // all the "other" events are kept:
        Assert.assertTrue(events.contains(otherEventBefore));
        Assert.assertTrue(events.contains(otherEventAfter));
        Assert.assertTrue(events.contains(otherEventDuring));
        Assert.assertTrue(events.contains(otherEventLowerLimit));
        Assert.assertTrue(events.contains(otherEventUpperLimit));

        /*
         * Complementary filter : keeps only the events and phenomena inside the interval
         */

        // creation of the timeline
        final Timeline list2 = new Timeline(dummyLogger, interval, null);

        list2.addCodedEvent(eventAfter);
        list2.addCodedEvent(eventUpperLimit);
        list2.addCodedEvent(eventDuring);
        list2.addCodedEvent(eventLowerLimit);
        list2.addCodedEvent(eventBefore);
        list2.addCodedEvent(otherEventAfter);
        list2.addCodedEvent(otherEventUpperLimit);
        list2.addCodedEvent(otherEventDuring);
        list2.addCodedEvent(otherEventLowerLimit);
        list2.addCodedEvent(otherEventBefore);

        list2.addPhenomenon(eclipseBefore);
        list2.addCodedEvent(eclipseBefore.getStartingEvent());
        list2.addCodedEvent(eclipseBefore.getEndingEvent());
        list2.addPhenomenon(eclipseOverlapping1);
        list2.addCodedEvent(eclipseOverlapping1.getStartingEvent());
        list2.addCodedEvent(eclipseOverlapping1.getEndingEvent());
        list2.addPhenomenon(eclipseDuring);
        list2.addCodedEvent(eclipseDuring.getStartingEvent());
        list2.addCodedEvent(eclipseDuring.getEndingEvent());
        list2.addPhenomenon(eclipseOverlapping2);
        list2.addCodedEvent(eclipseOverlapping2.getStartingEvent());
        list2.addCodedEvent(eclipseOverlapping2.getEndingEvent());
        list2.addPhenomenon(eclipseAfter);
        list2.addCodedEvent(eclipseAfter.getStartingEvent());
        list2.addCodedEvent(eclipseAfter.getEndingEvent());

        list2.addPhenomenon(otherEclipseBefore);
        list2.addPhenomenon(otherEclipseAfter);
        list2.addPhenomenon(otherEclipseDuring);
        list2.addPhenomenon(otherEclipseOverlapping1);
        list2.addPhenomenon(otherEclipseOverlapping2);

        final PostProcessing f2 = new TimeFilter(listCode, interval, false);

        f2.applyTo(list2);

        phenomena = list2.getPhenomenaList();
        events = list2.getCodedEventsList();

        // the eclipse "before" is removed
        Assert.assertTrue(!phenomena.contains(eclipseBefore));
        Assert.assertTrue(!events.contains(eclipseBefore.getStartingEvent())
                && !events.contains(eclipseBefore.getEndingEvent()));
        // the "other" eclipse "before" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseBefore));
        // the eclipse "after" is removed
        Assert.assertTrue(!phenomena.contains(eclipseAfter));
        Assert.assertTrue(!events.contains(eclipseAfter.getStartingEvent())
                && !events.contains(eclipseAfter.getEndingEvent()));
        // the "other" eclipse "after" is kept
        Assert.assertTrue(phenomena.contains(otherEclipseAfter));
        // the eclipse "during" is kept
        Assert.assertTrue(phenomena.contains(eclipseDuring));
        Assert.assertTrue(events.contains(eclipseDuring.getStartingEvent())
                && events.contains(eclipseDuring.getEndingEvent()));
        // the "other" eclipse "during" is removed
        Assert.assertTrue(!phenomena.contains(otherEclipseDuring));
        // the eclipse "overlapping1" is cut
        Assert.assertTrue(!phenomena.contains(eclipseOverlapping1));
        // the eclipse "overlapping2" is cut
        Assert.assertTrue(!phenomena.contains(eclipseOverlapping2));

        // only the "during" event and the event that occurs on the upper limit of the interval are kept
        Assert.assertTrue(!events.contains(eventBefore));
        Assert.assertTrue(!events.contains(eventAfter));
        Assert.assertTrue(events.contains(eventDuring));
        Assert.assertTrue(!events.contains(eventLowerLimit));
        Assert.assertTrue(events.contains(eventUpperLimit));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TIME}
     * 
     * @testedMethod {@link TimeFilter#TimeFilter(AbsoluteDateInterval, boolean)}
     * @testedMethod {@link TimeFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the time filter when all the elements in a timeline
     *              must be removed, or kept.
     * 
     * @input timeline that contains events and phenomena that are inside, outside the time filter interval as well as
     *        phenomena which overlaps this interval
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the correct ones
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testF2AllElements() throws PatriusException {
        // creation of the timeline
        final CodedEventsLogger dummyLogger = new CodedEventsLogger();

        // creation of the time interval
        final AbsoluteDate start = new AbsoluteDate(2012, 06, 20, 12, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate end = new AbsoluteDate(2012, 06, 21, 12, 0, 0, TimeScalesFactory.getTAI());

        final AbsoluteDateInterval time = new AbsoluteDateInterval(IntervalEndpointType.OPEN, start, end,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final AbsoluteDateInterval timelineInterval = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            start.shiftedBy(-100000.), end.shiftedBy(100000.), IntervalEndpointType.OPEN);
        final Timeline list = new Timeline(dummyLogger, timelineInterval, null);

        // Set up events and phenomena:
        // phenomenon whose time interval includes the time interval of the filter:
        final CodedEvent longStart = new CodedEvent("long", "start", start.shiftedBy(-600.), true);
        final CodedEvent longEnd = new CodedEvent("long", "end", end.shiftedBy(600.), false);
        final Phenomenon longPhenomenon = new Phenomenon(longStart, true, longEnd, true, "long phenomenon", "");
        // phenomenon overlapping the time interval of the filter (on its lower boundary):
        final CodedEvent overlap1Start = new CodedEvent("overlap1", "start", start.shiftedBy(-1200.), true);
        final CodedEvent overlap1End = new CodedEvent("overlap1", "end", start.shiftedBy(1200.), false);
        final Phenomenon overlap1Phenomenon = new Phenomenon(overlap1Start, true, overlap1End, true,
            "overlapping 1 phenomenon", "");
        // phenomenon overlapping the time interval of the filter (on its upper boundary):
        final CodedEvent overlap2Start = new CodedEvent("overlap2", "start", end.shiftedBy(-460.), true);
        final CodedEvent overlap2End = new CodedEvent("overlap2", "end", end.shiftedBy(460.), false);
        final Phenomenon overlap2Phenomenon = new Phenomenon(overlap2Start, true, overlap2End, true,
            "overlapping 2 phenomenon", "");
        // phenomenon included in the time interval of the filter:
        final CodedEvent inclStart = new CodedEvent("incl", "start", start.shiftedBy(1000.), true);
        final CodedEvent inclEnd = new CodedEvent("incl", "end", start.shiftedBy(1500.), false);
        final Phenomenon inclPhenomenon = new Phenomenon(inclStart, true, inclEnd, true, "included phenomenon", "");
        // phenomenon not included the time interval of the filter:
        final CodedEvent exclStart = new CodedEvent("excl", "start", start.shiftedBy(-1000.), true);
        final CodedEvent exclEnd = new CodedEvent("excl", "end", start.shiftedBy(-500.), false);
        final Phenomenon exclPhenomenon = new Phenomenon(exclStart, true, exclEnd, true, "excluded phenomenon", "");

        // Add events and phenomena to the timeline:
        list.addCodedEvent(longStart);
        list.addCodedEvent(longEnd);
        list.addCodedEvent(overlap1Start);
        list.addCodedEvent(overlap1End);
        list.addCodedEvent(overlap2Start);
        list.addCodedEvent(overlap2End);
        list.addCodedEvent(inclStart);
        list.addCodedEvent(inclEnd);
        list.addCodedEvent(exclStart);
        list.addCodedEvent(exclEnd);
        list.addPhenomenon(longPhenomenon);
        list.addPhenomenon(overlap1Phenomenon);
        list.addPhenomenon(overlap2Phenomenon);
        list.addPhenomenon(inclPhenomenon);
        list.addPhenomenon(exclPhenomenon);
        final Timeline list2 = new Timeline(list);
        /*
         * Filter on a time interval : removes all the events and phenomena inside the interval
         */
        final PostProcessing f1 = new TimeFilter(time, true);
        f1.applyTo(list);

        List<CodedEvent> events = list.getCodedEventsList();
        List<Phenomenon> phenomena = list.getPhenomenaList();

        // the "long" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(longPhenomenon));
        // its lower and upper event should have been kept:
        Assert.assertTrue(events.contains(longStart));
        Assert.assertTrue(events.contains(longEnd));

        // the "overlapping 1" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(overlap1Phenomenon));
        // its lower event should have been kept, its upper event should have been removed:
        Assert.assertTrue(events.contains(overlap1Start));
        Assert.assertFalse(events.contains(overlap1End));

        // the "overlapping 2" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(overlap2Phenomenon));
        // its upper event should have been kept, its lower event should have been removed:
        Assert.assertFalse(events.contains(overlap2Start));
        Assert.assertTrue(events.contains(overlap2End));

        // the "included" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(inclPhenomenon));
        // its upper and its lower event should have been removed:
        Assert.assertFalse(events.contains(inclStart));
        Assert.assertFalse(events.contains(inclEnd));

        // the "excluded" phenomenon should have been kept:
        Assert.assertTrue(phenomena.contains(exclPhenomenon));
        // its upper and its lower event should have been kept as well:
        Assert.assertTrue(events.contains(exclStart));
        Assert.assertTrue(events.contains(exclEnd));

        // There should be 5 phenomena in the list:
        Assert.assertEquals(5, phenomena.size());
        // There should be 2 "long" phenomena in the list:
        CodedEventsList eventsL = list.getCodedEvents();
        PhenomenaList phenomenaL = list.getPhenomena();
        Assert.assertEquals(2, phenomenaL.getPhenomena("long phenomenon", "", null).size());
        Assert.assertEquals(2, eventsL.getEvents("long", null, null).size());

        /*
         * Filter on a time interval : keeps only all the events and phenomena inside the interval
         */
        final PostProcessing f2 = new TimeFilter(time, false);
        f2.applyTo(list2);

        events = list2.getCodedEventsList();
        phenomena = list2.getPhenomenaList();

        // the "long" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(longPhenomenon));
        // its lower and upper event should have been removed:
        Assert.assertFalse(events.contains(longStart));
        Assert.assertFalse(events.contains(longEnd));

        // the "overlapping 1" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(overlap1Phenomenon));
        // its upper event should have been kept, its lower event should have been removed:
        Assert.assertFalse(events.contains(overlap1Start));
        Assert.assertTrue(events.contains(overlap1End));

        // the "overlapping 2" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(overlap2Phenomenon));
        // its lower event should have been kept, its upper event should have been removed:
        Assert.assertTrue(events.contains(overlap2Start));
        Assert.assertFalse(events.contains(overlap2End));

        // the "included" phenomenon should have been kept:
        Assert.assertTrue(phenomena.contains(inclPhenomenon));
        // its upper and its lower event should have been kept:
        Assert.assertTrue(events.contains(inclStart));
        Assert.assertTrue(events.contains(inclEnd));

        // the "excluded" phenomenon should have been removed:
        Assert.assertFalse(phenomena.contains(exclPhenomenon));
        // its upper and its lower event should have been removed as well:
        Assert.assertFalse(events.contains(exclStart));
        Assert.assertFalse(events.contains(exclEnd));

        // There should be 5 phenomena in the list:
        Assert.assertEquals(4, phenomena.size());
        // There should be 1 "long" phenomenon in the list:
        eventsL = list2.getCodedEvents();
        phenomenaL = list2.getPhenomena();
        Assert.assertEquals(1, phenomenaL.getPhenomena("long phenomenon", "", null).size());
        Assert.assertEquals(0, eventsL.getEvents("long", null, null).size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#OCCURRENCE}
     * 
     * @testedMethod {@link OccurrenceFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the occurrence filter
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output filtered timeline
     * 
     * @testPassCriteria the given elements have been correctly filtered
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testF3() throws PatriusException {
        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // initial events number
        int eventsNumber = list.getCodedEventsList().size();
        // initial phenomena number
        int phenomenaNumber = list.getPhenomenaList().size();

        // number of elements that have to be removed
        int removedItems = 0;
        // number of elements that have to be kept
        int keptItems = 0;

        Set<Phenomenon> previousPhenomena;
        Set<CodedEvent> previousEvents;

        /*
         * Filter on the eclipse phenomena
         */

        String code = "eclipse";

        // phenomena list linked to the eclipse phenomena, this list should not be empty
        previousPhenomena = list.getPhenomena().getPhenomena(code, null, null);
        Assert.assertTrue(!previousPhenomena.isEmpty());

        // filter creation
        final PostProcessing f1 = new OccurrenceFilter(code, 3, true);

        // number of elements that have to be removed
        removedItems = previousPhenomena.size() / 3;

        // application of the filter on the timeline
        f1.applyTo(list);

        // the 3xk occurrences are erased from the phenomena list and thus from the events list as well
        Assert.assertEquals(eventsNumber - 2 * removedItems, list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber - removedItems, list.getPhenomenaList().size());

        eventsNumber = list.getCodedEventsList().size();
        phenomenaNumber = list.getPhenomenaList().size();

        /*
         * Filter on the penumbra phenomena (it keeps the given occurrences and removes the others)
         */

        code = "penumbra";

        // phenomena list linked to the penumbra phenomena, this list should not be empty
        previousPhenomena = list.getPhenomena().getPhenomena(code, null, null);
        Assert.assertTrue(!previousPhenomena.isEmpty());

        // number of events related to penumbra phenomena
        final int penumbraEvents = list.getCodedEvents().getEvents("penumbra entrance", null, null).size()
                + list.getCodedEvents().getEvents("penumbra exit", null, null).size();

        // filter creation
        final PostProcessing f2 = new OccurrenceFilter(code, 3, false);

        // number of elements that have to be kept
        keptItems = previousPhenomena.size() / 3;

        // application of the filter on the timeline
        f2.applyTo(list);

        // only the 2xk occurrences are kept in the phenomena list which impacts as well the events list
        Assert.assertEquals(eventsNumber - (penumbraEvents - 2 * keptItems), list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber - (previousPhenomena.size() - keptItems), list.getPhenomenaList().size());

        eventsNumber = list.getCodedEventsList().size();
        phenomenaNumber = list.getPhenomenaList().size();

        /*
         * Filter on the perigee passage (it keeps the given occurrences and removes the others)
         */

        code = "perigee passage";

        // perigee passages events, this list should not be empty
        previousEvents = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(!previousEvents.isEmpty());

        // filter creation
        final PostProcessing f3 = new OccurrenceFilter(code, 5, false);

        // number of elements that have to be kept
        keptItems = previousEvents.size() / 5;

        // application of the filter on the timeline
        f3.applyTo(list);

        // only the 5xk occurrences are kept in the events list, the phenomena list remains constant
        Assert.assertEquals(eventsNumber - (previousEvents.size() - keptItems), list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber, list.getPhenomenaList().size());

        eventsNumber = list.getCodedEventsList().size();
        phenomenaNumber = list.getPhenomenaList().size();

        /*
         * Filter on the perigee passage (it keeps the given occurrences and removes the others)
         */

        code = "apogee passage";

        // perigee passages events, this list should not be empty
        previousEvents = list.getCodedEvents().getEvents(code, null, null);
        Assert.assertTrue(!previousEvents.isEmpty());

        // filter creation
        final PostProcessing f4 = new OccurrenceFilter(code, 5, true);

        // number of elements that have to be kept
        removedItems = previousEvents.size() / 5;

        // application of the filter on the timeline
        f4.applyTo(list);

        // only the 5xk occurrences are kept in the events list, the phenomena list remains constant
        Assert.assertEquals(eventsNumber - removedItems, list.getCodedEventsList().size());
        Assert.assertEquals(phenomenaNumber, list.getPhenomenaList().size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#DURATION}
     * 
     * @testedMethod {@link PhenomenonDurationFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the phenomenon duration filter
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output filtered timeline
     * 
     * @testPassCriteria the given elements have been correctly filtered
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         if a problem occurs during time line creation
     */
    @Test
    public void testF4() throws PatriusException {

        // min duration criterion
        // ========================

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // initial phenomena number
        int phenomenaNumberInit = list.getPhenomenaList().size();

        // test with "min duration = 900 s" criterion
        // (there are 85 of them in this case amongst a total of 402)
        final double duration = 900.0;

        // test of the input list durations
        int shorterPhenomenaNumber = 0;
        List<Phenomenon> phenoList = list.getPhenomenaList();
        for (final Phenomenon p : phenoList) {
            if (Comparators.lowerStrict(p.getTimespan().getDuration(), duration, Precision.DOUBLE_COMPARISON_EPSILON)) {
                shorterPhenomenaNumber++;
            }
        }

        final List<String> phen = new ArrayList<>();
        phen.add("station A visibility");
        phen.add("station B visibility");
        phen.add("station C visibility");
        phen.add("eclipse");
        phen.add("penumbra");

        PostProcessing filter = new PhenomenonDurationFilter(phen, duration, true);
        filter.applyTo(list);

        // test of the remaining list
        int phenoRemainingNumber = list.getPhenomenaList().size();
        Assert.assertEquals(phenomenaNumberInit - shorterPhenomenaNumber, phenoRemainingNumber);

        // test of the outup list durations
        phenoList = list.getPhenomenaList();
        for (final Phenomenon p : phenoList) {
            Assert.assertTrue(Comparators.greaterOrEqual(p.getTimespan().getDuration(), duration,
                Precision.DOUBLE_COMPARISON_EPSILON));
        }

        // max duration criterion
        // ========================

        // creation of the timeline from the logger used during the propagation
        // list = new Timeline(log, interval);

        // initial phenomena number
        phenomenaNumberInit = list.getPhenomenaList().size();

        // test with "max duration = 900 s" criterion
        // (there are 317 of them in this case amongst a total of 402)

        // test of the input list durations
        int longerPhenomenaNumber = 0;
        phenoList = list.getPhenomenaList();
        for (final Phenomenon p : phenoList) {
            if (Comparators.greaterStrict(p.getTimespan().getDuration(), duration, Precision.DOUBLE_COMPARISON_EPSILON)
                    && p.getCode() == "penumbra") {
                longerPhenomenaNumber++;
            }
        }

        filter = new PhenomenonDurationFilter("penumbra", duration, false);
        filter.applyTo(list);

        // test of the remaining list
        phenoRemainingNumber = list.getPhenomenaList().size();
        Assert.assertEquals(phenomenaNumberInit - longerPhenomenaNumber, phenoRemainingNumber);

        // test of the outup list durations
        phenoList = list.getPhenomenaList();
        for (final Phenomenon p : phenoList) {
            if (p.getCode() == "penumbra") {
                Assert.assertTrue(Comparators.lowerOrEqual(p.getTimespan().getDuration(), duration,
                    Precision.DOUBLE_COMPARISON_EPSILON));
            }
        }
    }

    /**
     * @throws PatriusException
     *         if a problem occurs during time line creation
     * @testType UT
     * 
     * @testedFeature {@link features#EVENTS_DURING_PHENOMENA}
     * 
     * @testedMethod {@link EventsDuringPhenomenaFilter#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the events during phenomena filter
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output filtered timeline
     * 
     * @testPassCriteria the given elements have been correctly filtered
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testF5() throws PatriusException {
        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        Set<Phenomenon> phenomena;
        Set<CodedEvent> events;
        Set<Phenomenon> previousPhenomena;
        Set<CodedEvent> previousEvents;

        /*
         * Filter on the eclipse entrance during penumbra phenomena
         */

        final String codePhenomenon = "penumbra";

        final String codeEvent = "station visibility 20 entrance";

        previousEvents = list.getCodedEvents().getEvents(codeEvent, null, null);
        previousPhenomena = list.getPhenomena().getPhenomena(codePhenomenon, null, null);

        Assert.assertTrue(!previousEvents.isEmpty());
        Assert.assertTrue(!previousPhenomena.isEmpty());

        final PostProcessing f1 = new EventsDuringPhenomenaFilter(codeEvent, codePhenomenon, true);

        f1.applyTo(list);

        phenomena = list.getPhenomena().getPhenomena(codePhenomenon, null, null);
        events = list.getCodedEvents().getEvents(codeEvent, null, null);

        for (final Phenomenon phen : phenomena) {
            for (final CodedEvent event : events) {
                if (phen.getTimespan().contains(event.getDate())) {
                    Assert.fail();
                }
                if (event.getDate().compareTo(phen.getTimespan().getUpperData()) == 1) {
                    break;
                }
            }
        }

        /*
         * Complementary filter
         */

        Assert.assertTrue(!list.getCodedEvents().getEvents(codeEvent, null, null).isEmpty());

        phenomena = list.getPhenomena().getPhenomena("penumbra", null, null);

        final PostProcessing f2 = new EventsDuringPhenomenaFilter(codeEvent, codePhenomenon, false);

        f2.applyTo(list);

        phenomena = list.getPhenomena().getPhenomena(codePhenomenon, null, null);

        Assert.assertTrue(phenomena.size() == previousPhenomena.size());

        Assert.assertTrue(list.getCodedEvents().getEvents(codeEvent, null, null).isEmpty());

        /*
         * Filter with a list of events
         */

        final List<String> eventList = new ArrayList<>();
        eventList.add("station visibility 35 entrance");
        eventList.add("descending node");

        int eventInitialNumber;

        eventInitialNumber = list.getCodedEvents().getEvents(eventList.get(0), null, null).size();

        eventInitialNumber += list.getCodedEvents().getEvents(eventList.get(1), null, null).size();

        final PostProcessing f3 = new EventsDuringPhenomenaFilter(eventList, codePhenomenon, false);

        f3.applyTo(list);

        int eventFinalNumber;

        eventFinalNumber = list.getCodedEvents().getEvents(eventList.get(0), null, null).size();

        eventFinalNumber += list.getCodedEvents().getEvents(eventList.get(1), null, null).size();

        // somme events have been removed
        Assert.assertTrue(eventInitialNumber > eventFinalNumber);

        events.clear();

        events.addAll(list.getCodedEvents().getEvents(eventList.get(0), null, null));
        events.addAll(list.getCodedEvents().getEvents(eventList.get(1), null, null));

        // we check if the station visibility 35 entrance events and the descending node events that remain in the list
        // occur during the penumbra
        final Iterator<Phenomenon> iterator = phenomena.iterator();
        AbsoluteDate date;
        Phenomenon currentPhen = iterator.next();
        for (final CodedEvent event : events) {
            date = event.getDate();
            while (date.compareTo(currentPhen.getTimespan().getUpperData()) == 1 && iterator.hasNext()) {
                currentPhen = iterator.next();
            }
            if (!currentPhen.getTimespan().contains(date)) {
                Assert.fail();
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#DELAY}
     * 
     * @testedMethod {@link DelayCriterion#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the delay events criterion
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the correct ones
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testC1() throws PatriusException {
        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        Set<CodedEvent> previousEvents;
        Set<CodedEvent> events;

        /*
         * Criterion : delay the ascending node
         */
        String code = "ascending node";

        previousEvents = list.getCodedEvents().getEvents(code, null, null);

        final int initialSize = previousEvents.size();

        final PostProcessing c1 = new DelayCriterion(code, 5, "Delayed ascending node", "delayed event");

        c1.applyTo(list);

        events = list.getCodedEvents().getEvents("Delayed ascending node", null, null);

        Assert.assertEquals(initialSize, events.size());

        /*
         * Criterion : delay the descending node
         */
        code = "descending node";

        final PostProcessing c2 = new DelayCriterion(code, 5);

        c2.applyTo(list);

        events = list.getCodedEvents().getEvents("Delayed descending node", null, null);

        Assert.assertEquals(initialSize, events.size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MERGE}
     * 
     * @testedMethod {@link MergePhenomenaCriterion#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the merge phenomena criterion
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the correct ones
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testC2() throws PatriusException {
        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        Set<Phenomenon> phenomena;

        Set<Phenomenon> previousPhenomena;

        /*
         * Criterion : merge close successive phenomena
         */

        final String codeA = "station A visibility";

        final String codeB = "station B visibility";

        // phenomena list linked to the station A visibility, this list should not be empty
        previousPhenomena = list.getPhenomena().getPhenomena(codeA, null, null);
        Assert.assertTrue(!previousPhenomena.isEmpty());

        final Phenomenon[] phenA = previousPhenomena.toArray(new Phenomenon[previousPhenomena.size()]);

        // creation of a new phenomenon close to one of the previous list
        final Random r = new Random(5487);
        int i = (int) ((previousPhenomena.size() - 1) * r.nextDouble());
        final Phenomenon phenomenonA = phenA[i];
        final CodedEvent startA = new CodedEvent(phenomenonA.getStartingEvent().getCode(), phenomenonA
            .getStartingEvent()
            .getComment(), phenomenonA.getEndingEvent().getDate().shiftedBy(10), true);
        final CodedEvent endA = new CodedEvent(phenomenonA.getEndingEvent().getCode(), phenomenonA.getEndingEvent()
            .getComment(), startA.getDate().shiftedBy(300), false);
        final Phenomenon closePhenA = new Phenomenon(startA, true, endA, true, codeA, "");

        // phenomena list linked to the station B visibility, this list should not be empty
        previousPhenomena = list.getPhenomena().getPhenomena(codeB, null, null);
        Assert.assertTrue(!previousPhenomena.isEmpty());

        final Phenomenon[] phenB = previousPhenomena.toArray(new Phenomenon[previousPhenomena.size()]);

        // creation of a new phenomenon close to one of the previous list
        i = (int) ((previousPhenomena.size() - 1) * r.nextDouble());
        final Phenomenon phenomenonB = phenB[i];
        final CodedEvent startB = new CodedEvent(phenomenonB.getStartingEvent().getCode(), phenomenonB
            .getStartingEvent()
            .getComment(), phenomenonB.getEndingEvent().getDate().shiftedBy(10), true);
        final CodedEvent endB = new CodedEvent(phenomenonB.getEndingEvent().getCode(), phenomenonB.getEndingEvent()
            .getComment(), startB.getDate().shiftedBy(300), false);
        final Phenomenon closePhenB = new Phenomenon(startB, true, endB, true, codeB, "");

        // this close phenomena are added to the list
        list.addPhenomenon(closePhenA);
        list.addPhenomenon(closePhenB);

        int previousPhenomenaA = list.getPhenomena().getPhenomena(codeA, null, null).size();

        Assert.assertTrue(list.getPhenomenaList().contains(closePhenA));

        // filter creation on station A visibility
        final double timelapse = 15.;
        final PostProcessing f1 = new MergePhenomenaCriterion(codeA, timelapse, "Merged " + codeA);

        // application of the filter on the timeline
        f1.applyTo(list);

        phenomena = list.getPhenomena().getPhenomena("Merged " + codeA, null, null);

        Assert.assertEquals(previousPhenomenaA - 1, phenomena.size());

        int previousPhenomenaB = list.getPhenomena().getPhenomena(codeB, null, null).size();

        // filter creation on station B visibility
        final PostProcessing f2 = new MergePhenomenaCriterion(codeB, timelapse);

        // application of the filter on the timeline
        f2.applyTo(list);

        phenomena = list.getPhenomena().getPhenomena("Merged " + codeB, null, null);

        Assert.assertEquals(previousPhenomenaB - 1, phenomena.size());

        final Iterator<Phenomenon> iterator = phenomena.iterator();
        final Phenomenon previousPhen = iterator.next();
        Phenomenon currentPhen;
        while (iterator.hasNext()) {
            currentPhen = iterator.next();
            Assert.assertTrue(MathLib.abs(currentPhen.getTimespan().getLowerData()
                .durationFrom(previousPhen.getTimespan().getUpperData())) >= timelapse);
        }

        Assert.assertFalse(list.getPhenomenaList().contains(closePhenA));
        Assert.assertFalse(list.getPhenomenaList().contains(closePhenB));

        /*
         * Test other constructors
         */

        // again the close phenomena are added to the list
        list.addPhenomenon(closePhenA);
        list.addCodedEvent(closePhenA.getStartingEvent());
        list.addCodedEvent(closePhenA.getEndingEvent());
        list.addPhenomenon(closePhenB);
        list.addCodedEvent(closePhenB.getStartingEvent());
        list.addCodedEvent(closePhenB.getEndingEvent());
        list.addPhenomenon(phenomenonA);
        list.addCodedEvent(phenomenonA.getStartingEvent());
        list.addCodedEvent(phenomenonA.getEndingEvent());
        list.addPhenomenon(phenomenonB);
        list.addCodedEvent(phenomenonB.getStartingEvent());
        list.addCodedEvent(phenomenonB.getEndingEvent());

        previousPhenomenaA = list.getPhenomena().getPhenomena("Merged " + codeA, null, null).size();
        previousPhenomenaB = list.getPhenomena().getPhenomena("Merged " + codeB, null, null).size();
        final List<String> phenCodesList = new ArrayList<>();
        phenCodesList.add(codeA);
        phenCodesList.add(codeB);

        final PostProcessing f3 = new MergePhenomenaCriterion(phenCodesList, timelapse);

        // application of the filter on the timeline
        f3.applyTo(list);

        phenomena = list.getPhenomena().getPhenomena("Merged " + codeA, null, null);

        Assert.assertEquals(previousPhenomenaA, phenomena.size());

        phenomena = list.getPhenomena().getPhenomena("Merged " + codeB, null, null);

        Assert.assertEquals(previousPhenomenaB, phenomena.size());

        // again the close phenomena are added to the list
        list.addPhenomenon(closePhenA);
        list.addCodedEvent(closePhenA.getStartingEvent());
        list.addCodedEvent(closePhenA.getEndingEvent());
        list.addPhenomenon(closePhenB);
        list.addCodedEvent(closePhenB.getStartingEvent());
        list.addCodedEvent(closePhenB.getEndingEvent());
        list.addPhenomenon(phenomenonA);
        list.addCodedEvent(phenomenonA.getStartingEvent());
        list.addCodedEvent(phenomenonA.getEndingEvent());
        list.addPhenomenon(phenomenonB);
        list.addCodedEvent(phenomenonB.getStartingEvent());
        list.addCodedEvent(phenomenonB.getEndingEvent());
        list.removePhenomenon(list.getPhenomena().getPhenomena("Merged " + codeA, null, null).iterator().next());
        list.removePhenomenon(list.getPhenomena().getPhenomena("Merged " + codeB, null, null).iterator().next());

        final Map<String, String> phenCodesMap = new HashMap<>();
        phenCodesMap.put(codeA, "I am the merged A phenomenon");
        phenCodesMap.put(codeB, "I am the merged B phenomenon");

        final PostProcessing f4 = new MergePhenomenaCriterion(phenCodesMap, timelapse);

        // application of the filter on the timeline
        f4.applyTo(list);

        phenomena = list.getPhenomena().getPhenomena("I am the merged A phenomenon", null, null);

        Assert.assertEquals(1, phenomena.size());

        phenomena = list.getPhenomena().getPhenomena("I am the merged B phenomenon", null, null);

        Assert.assertEquals(1, phenomena.size());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AND_OR_CRITERION}
     * 
     * @testedMethod {@link OrCriterion#applyTo(Timeline)}
     * @testedMethod {@link AndCriterion#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the "and" and "or" boolean phenomena criterion
     * 
     * @input timeline with known phenomena (dates known).
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the correct ones
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         if a problem occurs during time line creation
     */
    @Test
    public void testC3C4() throws PatriusException {

        // creation of the an empty logger
        final CodedEventsLogger log1 = new CodedEventsLogger();

        // creation of the first timeline from the first logger
        final Timeline list = new Timeline(log1, interval, null);

        // dates list
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = initialDate.shiftedBy(10.0);
        final AbsoluteDate date2 = initialDate.shiftedBy(20.0);
        final AbsoluteDate date3 = initialDate.shiftedBy(30.0);
        final AbsoluteDate date4 = initialDate.shiftedBy(40.0);
        final AbsoluteDate date5 = initialDate.shiftedBy(50.0);
        final AbsoluteDate date6 = initialDate.shiftedBy(60.0);
        final AbsoluteDate date7 = initialDate.shiftedBy(70.0);
        final AbsoluteDate date8 = initialDate.shiftedBy(80.0);
        final AbsoluteDate date9 = initialDate.shiftedBy(72.0);
        final AbsoluteDate date10 = initialDate.shiftedBy(78.0);

        // events creation
        final String comment = "comment";
        final CodedEvent event0 = new CodedEvent("event0", comment, initialDate, true);
        final CodedEvent event1 = new CodedEvent("event1", comment, date1, false);
        final CodedEvent event2 = new CodedEvent("event2", comment, date2, false);
        final CodedEvent event3 = new CodedEvent("event3", comment, date3, true);
        final CodedEvent event4 = new CodedEvent("event4", comment, date4, true);
        final CodedEvent event5 = new CodedEvent("event5", comment, date5, true);
        final CodedEvent event6 = new CodedEvent("event6", comment, date6, false);
        final CodedEvent event7 = new CodedEvent("event7", comment, date7, false);
        final CodedEvent event8 = new CodedEvent("event8", comment, date8, true);
        final CodedEvent event9 = new CodedEvent("event9", comment, date9, true);
        final CodedEvent event10 = new CodedEvent("event10", comment, date10, false);

        // phenomena
        final String aPheno = "A phenomenon";
        final String bPheno = "B phenomenon";
        final Phenomenon pA1 = new Phenomenon(event0, true, event1, true, aPheno, comment);
        final Phenomenon pA2 = new Phenomenon(event4, true, event5, true, aPheno, comment);
        final Phenomenon pA3 = new Phenomenon(event7, true, event8, true, aPheno, comment);
        final Phenomenon pB1 = new Phenomenon(event0, true, event2, true, bPheno, comment);
        final Phenomenon pB2 = new Phenomenon(event3, true, event6, true, bPheno, comment);
        final Phenomenon pB3 = new Phenomenon(event9, true, event10, true, bPheno, comment);

        // list filling
        list.addPhenomenon(pA1);
        list.addPhenomenon(pA2);
        list.addPhenomenon(pA3);
        list.addPhenomenon(pB1);
        list.addPhenomenon(pB2);
        list.addPhenomenon(pB3);

        // OR criterion
        final String aOrB = "A OR B";
        PostProcessing criterion = new OrCriterion(aPheno, bPheno, aOrB, comment);

        criterion.applyTo(list);

        PhenomenaList phenomenaList = list.getPhenomena();
        final Set<Phenomenon> aOrBPhenomena = phenomenaList.getPhenomena(aOrB, null, null);

        Assert.assertEquals(3, aOrBPhenomena.size());

        // getting of the associated array
        final Phenomenon[] aOrBArray = aOrBPhenomena.toArray(new Phenomenon[3]);

        AbsoluteDate dateToTest = aOrBArray[0].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(initialDate));
        dateToTest = aOrBArray[0].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date2));

        dateToTest = aOrBArray[1].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(date3));
        dateToTest = aOrBArray[1].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date6));

        dateToTest = aOrBArray[2].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(date7));
        dateToTest = aOrBArray[2].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date8));

        // AND criterion
        final String aAndB = "A AND B";
        criterion = new AndCriterion(aPheno, bPheno, aAndB, comment);

        criterion.applyTo(list);

        phenomenaList = list.getPhenomena();
        final Set<Phenomenon> aAndBPhenomena = phenomenaList.getPhenomena(aAndB, null, null);

        Assert.assertEquals(3, aAndBPhenomena.size());

        // getting of the associated array
        final Phenomenon[] aAndBArray = aAndBPhenomena.toArray(new Phenomenon[2]);

        // test of each phenomenon starting and ending dates
        dateToTest = aAndBArray[0].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(initialDate));
        dateToTest = aAndBArray[0].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date1));

        dateToTest = aAndBArray[1].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(date4));
        dateToTest = aAndBArray[1].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date5));

        dateToTest = aAndBArray[2].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(date9));
        dateToTest = aAndBArray[2].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date10));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AND_OR_CRITERION}
     * 
     * @testedMethod {@link OrCriterion#applyTo(Timeline)}
     * 
     * @description tests the mechanism of the "or" boolean phenomena criterion
     * 
     * @input timeline with known phenomena (dates known).
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the correct ones
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     * 
     * @throws PatriusException
     *         if a problem occurs during time line creation
     */
    @Test
    public void testORCriterion() throws PatriusException {

        // creation of the an empty logger
        final CodedEventsLogger log1 = new CodedEventsLogger();

        // dates list
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = initialDate.shiftedBy(10.0);
        final AbsoluteDate date2 = initialDate.shiftedBy(20.0);
        final AbsoluteDate date3 = initialDate.shiftedBy(30.0);
        final AbsoluteDate date4 = initialDate.shiftedBy(40.0);
        final AbsoluteDate date5 = initialDate.shiftedBy(50.0);

        // creation of the first timeline from the first logger
        final Timeline list = new Timeline(log1, new AbsoluteDateInterval(initialDate, date5), null);

        // events creation
        final String comment = "comment";
        final CodedEvent event0 = new CodedEvent("event0", comment, initialDate, true);
        final CodedEvent event1 = new CodedEvent("event1", comment, date1, false);
        final CodedEvent event2 = new CodedEvent("event2", comment, date2, false);
        final CodedEvent event3 = new CodedEvent("event3", comment, date3, true);
        final CodedEvent event4 = new CodedEvent("event4", comment, date4, true);
        final CodedEvent event5 = new CodedEvent("event5", comment, date5, true);

        // phenomena
        final String aPheno = "A phenomenon";
        final String bPheno = "B phenomenon";
        final Phenomenon pA1 = new Phenomenon(event0, true, event4, true, aPheno, comment);
        final Phenomenon pB1 = new Phenomenon(event1, true, event2, true, bPheno, comment);
        final Phenomenon pB2 = new Phenomenon(event3, true, event5, true, aPheno, comment);

        // list filling
        list.addPhenomenon(pA1);
        list.addPhenomenon(pB1);
        list.addPhenomenon(pB2);

        // OR criterion
        final String aOrB = "A OR B";
        final PostProcessing criterion = new OrCriterion(aPheno, bPheno, aOrB, comment);

        criterion.applyTo(list);

        final PhenomenaList phenomenaList = list.getPhenomena();
        final Set<Phenomenon> aOrBPhenomena = phenomenaList.getPhenomena(aOrB, null, null);

        Assert.assertEquals(1, aOrBPhenomena.size());

        // getting of the associated array
        final Phenomenon[] aOrBArray = aOrBPhenomena.toArray(new Phenomenon[1]);

        AbsoluteDate dateToTest = aOrBArray[0].getTimespan().getLowerData();
        Assert.assertEquals(0, dateToTest.compareTo(initialDate));
        dateToTest = aOrBArray[0].getTimespan().getUpperData();
        Assert.assertEquals(0, dateToTest.compareTo(date5));
    }

    /**
     * Tests the method : <br>
     * {@link NotCriterion#applyTo(Timeline)}
     */
    @Test
    public void testNotCriterion() {

        // dates list
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = initialDate.shiftedBy(10.0);
        final AbsoluteDate date2 = initialDate.shiftedBy(20.0);
        final AbsoluteDate date3 = initialDate.shiftedBy(30.0);
        final AbsoluteDate date4 = initialDate.shiftedBy(40.0);
        final AbsoluteDate date5 = initialDate.shiftedBy(50.0);

        final AbsoluteDateInterval validity = new AbsoluteDateInterval(initialDate, date5);

        // events creation
        final String comment = "comment";
        final CodedEvent event0 = new CodedEvent("event0", comment, initialDate, true);
        final CodedEvent event1 = new CodedEvent("event1", comment, date1, false);
        final CodedEvent event2 = new CodedEvent("event2", comment, date2, false);
        final CodedEvent event3 = new CodedEvent("event3", comment, date3, true);
        final CodedEvent event4 = new CodedEvent("event4", comment, date4, true);
        final CodedEvent event5 = new CodedEvent("event5", comment, date5, true);

        final CodedEvent startInterval = new CodedEvent("Start of validity interval", comment, initialDate, true);
        final CodedEvent endInterval = new CodedEvent("End of validity interval", comment, date5, false);

        // phenomena
        final String aPheno = "A phenomenon";
        final Phenomenon pA1 = new Phenomenon(event0, true, event1, true, aPheno, comment);
        final Phenomenon pA2 = new Phenomenon(event1, true, event3, true, aPheno, comment);
        final Phenomenon pA3 = new Phenomenon(event2, true, event4, true, aPheno, comment);
        final Phenomenon pA4 = new Phenomenon(event3, true, event4, true, aPheno, comment);
        final Phenomenon pA5 = new Phenomenon(event4, true, event5, true, aPheno, comment);

        // Not criterion
        final String complementary = "complementary";
        final NotCriterion notCriterion = new NotCriterion(aPheno, complementary, comment);

        // buils a timeline
        Timeline list;
        list = new Timeline(validity);

        // Empty phenomena list
        list = new Timeline(validity);
        notCriterion.applyTo(list);
        // Time line contains a unique phenomenon which the whole validity interval
        Assert.assertTrue(list.getPhenomenaList().size() == 1);
        Assert.assertEquals(list.getPhenomenaList(),
            Arrays.asList(new Phenomenon(startInterval, true, endInterval, true, complementary, comment)));
        Assert.assertEquals(list.getPhenomenaCodesList(), Arrays.asList("complementary"));

        // Single phenomenon stored : the start date of the phenomenon corresponds to the validity
        // interval start date
        list = new Timeline(validity);
        list.addPhenomenon(pA1);
        notCriterion.applyTo(list);
        Assert.assertEquals(2, list.getPhenomenaList().size());
        Assert.assertEquals(list.getPhenomenaCodesList(), Arrays.asList("A phenomenon", complementary));
        // first event
        Assert.assertTrue(list.getPhenomenaList().get(0).equals(pA1));
        // complementary
        Assert.assertTrue(list.getPhenomenaList().get(1).getStartingEvent().equals(event1));
        Assert.assertTrue(list.getPhenomenaList().get(1).getEndingEvent().equals(endInterval));

        // Single phenomenon stored : the end date of the phenomenon corresponds to the validity
        // interval end date
        list = new Timeline(validity);
        list.addPhenomenon(pA5);
        notCriterion.applyTo(list);

        Assert.assertEquals(2, list.getPhenomenaList().size());
        Assert.assertEquals(list.getPhenomenaCodesList(), Arrays.asList("A phenomenon", complementary));
        // complementary
        Assert.assertTrue(list.getPhenomenaList().get(0).getStartingEvent().equals(startInterval));
        Assert.assertTrue(list.getPhenomenaList().get(0).getEndingEvent().equals(event4));

        // time line with 3 overlapping phenomena
        list = new Timeline(validity);
        list.addPhenomenon(pA2);
        list.addPhenomenon(pA3);
        list.addPhenomenon(pA4);
        notCriterion.applyTo(list);

        Assert.assertEquals(5, list.getPhenomenaList().size());
        Assert.assertEquals(list.getPhenomenaCodesList(), Arrays.asList("A phenomenon", complementary));
        // first complementary
        Assert.assertTrue(list.getPhenomenaList().get(0).getStartingEvent().equals(startInterval));
        Assert.assertTrue(list.getPhenomenaList().get(0).getEndingEvent().equals(event1));
        // last complementary
        Assert.assertTrue(list.getPhenomenaList().get(4).getStartingEvent().equals(event4));
        Assert.assertTrue(list.getPhenomenaList().get(4).getEndingEvent().equals(endInterval));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#MERGE_TIMELINES}
     * 
     * @testedMethod {@link MergeTimelines#applyTo(Timeline)}
     * 
     * @description tests the merging of two timelines
     * 
     * @input two timelines, one to be added to the other one.
     * 
     * @output the timeline after the merging
     * 
     * @testPassCriteria the merged timelines contains all the events and phenomena of the two timelines.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         if a problem occurs during time line creation
     */
    @Test
    public void testC5() throws PatriusException {
        // for this test, create two loggers and two propagators:
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        // propagator
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));
        final CodedEventsLogger log1 = new CodedEventsLogger();
        final CodedEventsLogger log2 = new CodedEventsLogger();
        propagator1.addEventDetector(log1.monitorDetector(apogeePergieePassages));
        propagator1.addEventDetector(log1.monitorDetector(nodesPassages));
        propagator1.addEventDetector(log1.monitorDetector(eclipse));
        propagator2.addEventDetector(log2.monitorDetector(penumbra));
        propagator2.addEventDetector(log2.monitorDetector(stationVisi35));
        propagator2.addEventDetector(log2.monitorDetector(stationVisi30));
        propagator2.addEventDetector(log2.monitorDetector(stationVisi20));
        propagator1.resetInitialState(initialState);
        propagator2.resetInitialState(initialState);
        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator1.propagate(interval.getLowerData(), interval.getUpperData());
        propagator2.propagate(interval.getLowerData(), interval.getUpperData());

        // creation of the first timeline from the first logger
        Timeline list1;
        // creation of the second timeline from the second logger
        Timeline list2;
        list1 = new Timeline(log1, interval, null);
        final int list1EventsSize = list1.getCodedEventsList().size();
        final int list1PhensSize = list1.getPhenomenaList().size();
        list2 = new Timeline(log2, interval, null);
        final int list2EventsSize = list2.getCodedEventsList().size();
        final int list2PhensSize = list2.getPhenomenaList().size();
        // creation of the third timeline from the second logger with a different interval of
        // validity:
        final AbsoluteDate date2 = new AbsoluteDate(2005, 1, 2, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date2,
            date2.shiftedBy(10 * 23 * 3600), IntervalEndpointType.CLOSED);
        final Timeline list3 = new Timeline(log2, interval2, null);

        // creation of the timelines merging criterion:
        final PostProcessing criterion1 = new MergeTimelines(list2);
        final PostProcessing criterion2 = new MergeTimelines(list3);

        // criteria application
        criterion1.applyTo(list1);

        final int listEventsSize = list1.getCodedEventsList().size();
        final int listPhensSize = list1.getPhenomenaList().size();
        Assert.assertEquals(listEventsSize, list1EventsSize + list2EventsSize);
        Assert.assertEquals(listPhensSize, list1PhensSize + list2PhensSize);
        try {
            // the merging should fail:
            criterion2.applyTo(list1);
        } catch (final IllegalArgumentException e1) {
            // expected
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ELEMENT_TYPE}
     * 
     * @testedMethod {@link PostProcessing#applyTo(Timeline)}
     * 
     * @description tests the post-processing when filters are applied on timelines coming from the same logger, in
     *              order to check that there are not conflicts between the elements of the timelines
     * 
     * @input three timelines, created from the same logger.
     * 
     * @output the timeline after the post-processing
     * 
     * @testPassCriteria the post-processing properly filters the timelines without conflicts
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         if a problem occurs during time line creation
     */
    @Test
    public void testNew() throws PatriusException {
        // creation of the timelines 1 and 2 from the logger used during the propagation:
        final Timeline list1 = new Timeline(log, interval, null);
        final Timeline list2 = new Timeline(log, interval, null);

        // initial phenomena number
        final int initialPhenomenaNumber2 = list2.getPhenomenaList().size();
        final String code1 = "eclipse";
        final String code2 = "station A visibility";
        final PostProcessing f1 = new ElementTypeFilter(code1, false);
        final PostProcessing f2 = new ElementTypeFilter(code2, true);
        // initial events/phenomena number list 1
        final int phenomenaNumber1 = list1.getPhenomena().getPhenomena(code1, null, null).size();
        // initial events/phenomena number list 2
        final int phenomenaNumber2 = list2.getPhenomena().getPhenomena(code2, null, null).size();

        f1.applyTo(list1);
        f2.applyTo(list2);

        // creation of the timeline 3
        final Timeline list3 = new Timeline(log, interval, null);
        final String code3 = "penumbra";
        // initial events/phenomena number list 3
        final int eventsNumber3 = list3.getCodedEventsList().size();
        final int phenomenaNumber3 = list3.getPhenomena().getPhenomena(code3, null, null).size();
        final PostProcessing f3 = new ElementTypeFilter(code3, false);
        f3.applyTo(list3);

        // List 1:
        final int finalEventsNumber1 = list1.getCodedEventsList().size();
        final int finalPhenomenaNumber1 = list1.getPhenomenaList().size();
        Assert.assertEquals(phenomenaNumber1, finalPhenomenaNumber1);
        Assert.assertEquals(phenomenaNumber1 * 2, finalEventsNumber1);
        // List 2:
        final int finalEventsNumber2 = list2.getCodedEventsList().size();
        final int finalPhenomenaNumber2 = list2.getPhenomenaList().size();
        Assert.assertEquals(eventsNumber3 - phenomenaNumber2 * 2, finalEventsNumber2);
        Assert.assertEquals(initialPhenomenaNumber2 - phenomenaNumber2, finalPhenomenaNumber2);
        // List 3:
        final int finalEventsNumber3 = list3.getCodedEventsList().size();
        final int finalPhenomenaNumber3 = list3.getPhenomenaList().size();
        Assert.assertEquals(phenomenaNumber3, finalPhenomenaNumber3);
        Assert.assertEquals(phenomenaNumber3 * 2, finalEventsNumber3);
    }

    @BeforeClass
    public static void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        // integrator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        // propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        // initial orbit
        final AbsoluteDate date = new AbsoluteDate(2005, 1, 2, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getGCRF();
        final double re = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.EGM96_EARTH_MU;
        final double a = 7200000;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.001, MathLib.toRadians(40), MathLib.toRadians(10),
            MathLib.toRadians(15), MathLib.toRadians(20), PositionAngle.MEAN, frame, date, mu);

        // final double period = MathUtils.TWO_PI * FastMath.sqrt(FastMath.pow(a, 3) / mu);

        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date, date.shiftedBy(10 * 24 * 3600),
            IntervalEndpointType.CLOSED);

        initialState = new SpacecraftState(initialOrbit);
        propagator.resetInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));

        // events

        // apogee perigee passages
        final EventDetector apogeePergieePassagesDet = new ApsideDetector(initialOrbit, 2){
            private static final long serialVersionUID = 7149796307062112194L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        apogeePergieePassages = new GenericCodingEventDetector(apogeePergieePassagesDet, "apogee passage",
            "perigee passage");

        // nodes passages
        final EventDetector nodesPassagesDet = new NodeDetector(initialOrbit, initialOrbit.getFrame(),
            NodeDetector.ASCENDING_DESCENDING){
            private static final long serialVersionUID = 1528780196650676150L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        nodesPassages = new GenericCodingEventDetector(nodesPassagesDet, "ascending node", "descending node");

        final JPLCelestialBodyLoader loaderSun = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);

        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        final CelestialBody sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);

        final JPLCelestialBodyLoader loaderEarth = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH);

        final CelestialBody earth = loaderEarth.loadCelestialBody(CelestialBodyFactory.EARTH);

        // eclipse
        final EventDetector eclipseDet = new EclipseDetector(sun, Constants.SUN_RADIUS, earth, re, 0, 300, 0.001){
            private static final long serialVersionUID = -2984027140864819559L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        eclipse = new GenericCodingEventDetector(eclipseDet, "eclipse exit", "eclipse entrance", false, "eclipse");

        // penumbra
        final EventDetector penumbraDet = new EclipseDetector(sun, Constants.SUN_RADIUS, earth, re, 1, 300, 0.001){
            private static final long serialVersionUID = 5098112473308858265L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        penumbra = new GenericCodingEventDetector(penumbraDet, "penumbra exit", "penumbra entrance", false, "penumbra");

        final int maxCheck = 120;

        final OneAxisEllipsoid earthBody = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
            FramesFactory.getITRF());

        final EllipsoidPoint point1 = new EllipsoidPoint(earthBody, earthBody.getLLHCoordinatesSystem(),
            MathLib.toRadians(40), MathLib.toRadians(300), 0, "point1");
        final EllipsoidPoint point2 = new EllipsoidPoint(earthBody, earthBody.getLLHCoordinatesSystem(),
            MathLib.toRadians(-30), MathLib.toRadians(250), 0, "point2");
        final EllipsoidPoint point3 = new EllipsoidPoint(earthBody, earthBody.getLLHCoordinatesSystem(),
            MathLib.toRadians(-12), MathLib.toRadians(30), 0, "point3");

        final PVCoordinatesProvider station1 = new TopocentricFrame(point1, "station 1");
        final PVCoordinatesProvider station2 = new TopocentricFrame(point2, "station 2");
        final PVCoordinatesProvider station3 = new TopocentricFrame(point3, "station 3");

        // station visibility
        final EventDetector stationVisi35Det = new CircularFieldOfViewDetector(station1, Vector3D.PLUS_I,
            MathLib.toRadians(35),
            maxCheck){
            private static final long serialVersionUID = -54150076610577203L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        stationVisi35 = new GenericCodingEventDetector(stationVisi35Det, "station visibility 35 entrance",
            "station visibility 35 exit", true, "station A visibility");

        final EventDetector stationVisi30Det = new CircularFieldOfViewDetector(station2, Vector3D.PLUS_I,
            MathLib.toRadians(30),
            maxCheck){
            private static final long serialVersionUID = -7242813421915186858L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        stationVisi30 = new GenericCodingEventDetector(stationVisi30Det, "station visibility 30 entrance",
            "station visibility 30 exit", true, "station B visibility");

        final Vector3D center = Vector3D.MINUS_I;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = Vector3D.PLUS_J;
        final EventDetector stationVisi20Det = new DihedralFieldOfViewDetector(station3, center, axis1,
            MathLib.toRadians(20),
            axis2, MathLib.toRadians(50), maxCheck){
            private static final long serialVersionUID = 1278789570580110865L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        stationVisi20 = new GenericCodingEventDetector(stationVisi20Det, "station visibility 20 entrance",
            "station visibility 20 exit", true, "station C visibility");

        propagator.setAttitudeProvider(new NadirPointing(earthBody));

        log = new CodedEventsLogger();

        propagator.resetInitialState(initialState);

        propagator.addEventDetector(log.monitorDetector(apogeePergieePassages));
        propagator.addEventDetector(log.monitorDetector(nodesPassages));
        propagator.addEventDetector(log.monitorDetector(eclipse));
        propagator.addEventDetector(log.monitorDetector(penumbra));
        propagator.addEventDetector(log.monitorDetector(stationVisi35));
        propagator.addEventDetector(log.monitorDetector(stationVisi30));
        propagator.addEventDetector(log.monitorDetector(stationVisi20));

        propagator.propagate(interval.getLowerData(), interval.getUpperData());
    }
}
