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
 * HISTORY
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3130:10/05/2022:[PATRIUS] Robustifier le calcul des phenomenes des CodedEventsLogger, ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
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
 *              Class that tests the polarization post processing.
 *              </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class PolarizationPostProcessingTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Polarization single selection
         * 
         * @featureDescription test the creation of a polarization single selection phenomenon.
         * 
         * @coveredRequirements DV-VISI_60, DV-EVT_150
         */
        SINGLE_SELECTION,
        /**
         * @featureTitle Polarization selection switch
         * 
         * @featureDescription test the creation of polarization switch events.
         * 
         * @coveredRequirements DV-VISI_60, DV-EVT_150
         */
        POLARIZATION_SWITCH
    }

    /** Events logger. */
    private static CodedEventsLogger log;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SINGLE_SELECTION}
     * 
     * @testedMethod {@link PolarizationSingleSelection#PolarizationSingleSelection(String, String, double, double)}
     * @testedMethod {@link PolarizationSingleSelection#applyTo(Timeline)}
     * 
     * @description tests the creation of the polarization single selection phenomenon
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created element is the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSingleSelection() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 04, 01, 11, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 04, 01, 21, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // events/phenomena VISI_L to add to the list:
        final CodedEvent eventL1enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 12, 0, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL1exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 12, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL1 = new Phenomenon(eventL1enter, true, eventL1exit, true, "VISI_L", "");
        final CodedEvent eventL2enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 14, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL2exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 15, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL2 = new Phenomenon(eventL2enter, true, eventL2exit, true, "VISI_L", "");
        final CodedEvent eventL3enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 17, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL3exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 20, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL3 = new Phenomenon(eventL3enter, true, eventL3exit, true, "VISI_L", "");
        // add the VISI_L events and phenomena to the timeline:
        list.addCodedEvent(eventL1enter);
        list.addCodedEvent(eventL1exit);
        list.addCodedEvent(eventL2enter);
        list.addCodedEvent(eventL2exit);
        list.addCodedEvent(eventL3enter);
        list.addCodedEvent(eventL3exit);
        list.addPhenomenon(phenL1);
        list.addPhenomenon(phenL2);
        list.addPhenomenon(phenL3);

        // events/phenomena VISI_R to add to the list:
        final CodedEvent eventR1enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 12, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR1exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 14, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR1 = new Phenomenon(eventR1enter, true, eventR1exit, true, "VISI_R", "");
        final CodedEvent eventR2enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 15, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR2exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 18, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR2 = new Phenomenon(eventR2enter, true, eventR2exit, true, "VISI_R", "");
        final CodedEvent eventR3enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 19, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR3exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 20, 15, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR3 = new Phenomenon(eventR3enter, true, eventR3exit, true, "VISI_R", "");
        // add the VISI_R events and phenomena to the timeline:
        list.addCodedEvent(eventR1enter);
        list.addCodedEvent(eventR1exit);
        list.addCodedEvent(eventR2enter);
        list.addCodedEvent(eventR2exit);
        list.addCodedEvent(eventR3enter);
        list.addCodedEvent(eventR3exit);
        list.addPhenomenon(phenR1);
        list.addPhenomenon(phenR2);
        list.addPhenomenon(phenR3);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSingleSelection creation = new PolarizationSingleSelection("VISI_L", "VISI_R", 3600, 3600);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // the events list has not been changed:
        Assert.assertEquals(beforeEventsSize, afterEventsSize);
        // the phenomena list has a new element:
        Assert.assertEquals(beforePhensSize + 1, afterPhensSize);
        final Set<Phenomenon> phensL = list.getPhenomena().getPhenomena("VISI_L Selection", "", null);
        final Set<Phenomenon> phensR = list.getPhenomena().getPhenomena("VISI_R Selection", "", null);
        Assert.assertEquals(0, phensL.size());
        Assert.assertEquals(1, phensR.size());
        Assert.assertEquals(eventR1enter, phensR.iterator().next().getStartingEvent());
        Assert.assertEquals(eventR2exit, phensR.iterator().next().getEndingEvent());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#POLARIZATION_SWITCH}
     * 
     * @testedMethod {@link PolarizationSwitch#PolarizationSwitch(String, String, double, double)}
     * @testedMethod {@link PolarizationSwitch#applyTo(Timeline)}
     * 
     * @description tests the creation of the polarization selection switch events
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSwitch1() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 04, 01, 12, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 04, 01, 22, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // events/phenomena VISI_L to add to the list:
        final CodedEvent eventL1enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 12, 0, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL1exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 12, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL1 = new Phenomenon(eventL1enter, true, eventL1exit, true, "VISI_L", "");
        final CodedEvent eventL2enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 14, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL2exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 20, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL2 = new Phenomenon(eventL2enter, true, eventL2exit, true, "VISI_L", "");
        final CodedEvent eventL3enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 22, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL3exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 23, 15, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL3 = new Phenomenon(eventL3enter, true, eventL3exit, true, "VISI_L", "");
        // add the VISI_L events and phenomena to the timeline:
        list.addCodedEvent(eventL1enter);
        list.addCodedEvent(eventL1exit);
        list.addCodedEvent(eventL2enter);
        list.addCodedEvent(eventL2exit);
        list.addCodedEvent(eventL3enter);
        list.addCodedEvent(eventL3exit);
        list.addPhenomenon(phenL1);
        list.addPhenomenon(phenL2);
        list.addPhenomenon(phenL3);

        // events/phenomena VISI_R to add to the list:
        final CodedEvent eventR1enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 12, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR1exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 15, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR1 = new Phenomenon(eventR1enter, true, eventR1exit, true, "VISI_R", "");
        final CodedEvent eventR2enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 18, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR2exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 21, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR2 = new Phenomenon(eventR2enter, true, eventR2exit, true, "VISI_R", "");
        // add the VISI_R events and phenomena to the timeline:
        list.addCodedEvent(eventR1enter);
        list.addCodedEvent(eventR1exit);
        list.addCodedEvent(eventR2enter);
        list.addCodedEvent(eventR2exit);
        list.addPhenomenon(phenR1);
        list.addPhenomenon(phenR2);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSwitch creation = new PolarizationSwitch("VISI_L", "VISI_R", 15 * 60, 3600);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // the events list has 4 new elements:
        Assert.assertEquals(beforeEventsSize + 4, afterEventsSize);
        // the phenomena list has not changed:
        Assert.assertEquals(beforePhensSize, afterPhensSize);
        final Set<CodedEvent> eventsL = list.getCodedEvents().getEvents("VISI_L Selection", "", null);
        final Set<CodedEvent> eventsR = list.getCodedEvents().getEvents("VISI_R Selection", "", null);
        Assert.assertEquals(2, eventsL.size());
        Assert.assertEquals(2, eventsR.size());
        final Iterator<CodedEvent> iterL = eventsL.iterator();
        final Iterator<CodedEvent> iterR = eventsR.iterator();
        Assert.assertEquals(eventR1enter.getDate(), iterR.next().getDate());
        Assert.assertEquals(eventR1exit.getDate(), iterL.next().getDate());
        Assert.assertEquals(eventL2exit.getDate(), iterR.next().getDate());
        Assert.assertEquals(eventR2exit.getDate(), iterL.next().getDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#POLARIZATION_SWITCH}
     * 
     * @testedMethod {@link PolarizationSwitch#PolarizationSwitch(String, String, double, double)}
     * @testedMethod {@link PolarizationSwitch#applyTo(Timeline)}
     * 
     * @description tests the creation of the polarization selection switch events
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSwitch2() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 04, 01, 12, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 04, 01, 22, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // events/phenomena VISI_L to add to the list:
        final CodedEvent eventL1enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 14, 10, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL1exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 14, 50, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL1 = new Phenomenon(eventL1enter, true, eventL1exit, true, "VISI_L", "");
        final CodedEvent eventL2enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 16, 50, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL2exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 18, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL2 = new Phenomenon(eventL2enter, true, eventL2exit, true, "VISI_L", "");
        final CodedEvent eventL3enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 18, 45, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL3exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 19, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL3 = new Phenomenon(eventL3enter, true, eventL3exit, true, "VISI_L", "");
        // add the VISI_L events and phenomena to the timeline:
        list.addCodedEvent(eventL1enter);
        list.addCodedEvent(eventL1exit);
        list.addCodedEvent(eventL2enter);
        list.addCodedEvent(eventL2exit);
        list.addCodedEvent(eventL3enter);
        list.addCodedEvent(eventL3exit);
        list.addPhenomenon(phenL1);
        list.addPhenomenon(phenL2);
        list.addPhenomenon(phenL3);

        // events/phenomena VISI_R to add to the list:
        final CodedEvent eventR1enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 14, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR1exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 15, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR1 = new Phenomenon(eventR1enter, true, eventR1exit, true, "VISI_R", "");
        final CodedEvent eventR2enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 15, 55, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR2exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 17, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR2 = new Phenomenon(eventR2enter, true, eventR2exit, true, "VISI_R", "");
        final CodedEvent eventR3enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 17, 50, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR3exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 19, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR3 = new Phenomenon(eventR3enter, true, eventR3exit, true, "VISI_R", "");
        // add the VISI_R events and phenomena to the timeline:
        list.addCodedEvent(eventR1enter);
        list.addCodedEvent(eventR1exit);
        list.addCodedEvent(eventR2enter);
        list.addCodedEvent(eventR2exit);
        list.addCodedEvent(eventR3enter);
        list.addCodedEvent(eventR3exit);
        list.addPhenomenon(phenR1);
        list.addPhenomenon(phenR2);
        list.addPhenomenon(phenR3);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSwitch creation = new PolarizationSwitch("VISI_L", "VISI_R", 45 * 60, 10 * 60);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // the events list has 3 new elements:
        Assert.assertEquals(beforeEventsSize + 3, afterEventsSize);
        // the phenomena list has not changed:
        Assert.assertEquals(beforePhensSize, afterPhensSize);
        final Set<CodedEvent> eventsL = list.getCodedEvents().getEvents("VISI_L Selection", "", null);
        final Set<CodedEvent> eventsR = list.getCodedEvents().getEvents("VISI_R Selection", "", null);
        Assert.assertEquals(1, eventsL.size());
        Assert.assertEquals(2, eventsR.size());
        final Iterator<CodedEvent> iterL = eventsL.iterator();
        final Iterator<CodedEvent> iterR = eventsR.iterator();
        Assert.assertEquals(list.getIntervalOfValidity().getLowerData(), iterR.next().getDate());
        Assert.assertEquals(eventR2exit.getDate(), iterL.next().getDate());
        Assert.assertEquals(eventL2exit.getDate(), iterR.next().getDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#POLARIZATION_SWITCH}
     * 
     * @testedMethod {@link PolarizationSwitch#PolarizationSwitch(String, String, double, double)}
     * @testedMethod {@link PolarizationSwitch#applyTo(Timeline)}
     * 
     * @description tests the creation of the polarization selection switch events
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSwitch3() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 04, 01, 12, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 04, 01, 22, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // events/phenomena VISI_L to add to the list:
        final CodedEvent eventL1enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 12, 0, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL1exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 15, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL1 = new Phenomenon(eventL1enter, true, eventL1exit, true, "VISI_L", "");
        final CodedEvent eventL2enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 04, 1, 19, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL2exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 04, 1, 20, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL2 = new Phenomenon(eventL2enter, true, eventL2exit, true, "VISI_L", "");
        // add the VISI_L events and phenomena to the timeline:
        list.addCodedEvent(eventL1enter);
        list.addCodedEvent(eventL1exit);
        list.addCodedEvent(eventL2enter);
        list.addCodedEvent(eventL2exit);
        list.addPhenomenon(phenL1);
        list.addPhenomenon(phenL2);

        // events/phenomena VISI_R to add to the list:
        final CodedEvent eventR1enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 12, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR1exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 15, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR1 = new Phenomenon(eventR1enter, true, eventR1exit, true, "VISI_R", "");
        final CodedEvent eventR2enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 04, 1, 18, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR2exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 04, 1, 21, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR2 = new Phenomenon(eventR2enter, true, eventR2exit, true, "VISI_R", "");
        // add the VISI_R events and phenomena to the timeline:
        list.addCodedEvent(eventR1enter);
        list.addCodedEvent(eventR1exit);
        list.addCodedEvent(eventR2enter);
        list.addCodedEvent(eventR2exit);
        list.addPhenomenon(phenR1);
        list.addPhenomenon(phenR2);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSwitch creation = new PolarizationSwitch("VISI_L", "VISI_R", 15 * 60, 3600);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // the events list has 2 new elements:
        Assert.assertEquals(beforeEventsSize + 2, afterEventsSize);
        // the phenomena list has not changed:
        Assert.assertEquals(beforePhensSize, afterPhensSize);
        final Set<CodedEvent> eventsL = list.getCodedEvents().getEvents("VISI_L Selection", "", null);
        final Set<CodedEvent> eventsR = list.getCodedEvents().getEvents("VISI_R Selection", "", null);
        Assert.assertEquals(1, eventsL.size());
        Assert.assertEquals(1, eventsR.size());
        final Iterator<CodedEvent> iterL = eventsL.iterator();
        final Iterator<CodedEvent> iterR = eventsR.iterator();
        Assert.assertEquals(eventL1enter.getDate(), iterL.next().getDate());
        Assert.assertEquals(eventL1exit.getDate(), iterR.next().getDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#POLARIZATION_SWITCH}
     * 
     * @testedMethod {@link PolarizationSwitch#PolarizationSwitch(String, String, double, double)}
     * @testedMethod {@link PolarizationSwitch#applyTo(Timeline)}
     * 
     * @description tests the creation of the polarization selection switch events
     * 
     * @input timeline that contains all of the events detected through the propagation and the corresponding phenomena.
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created elements are the expected one
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSwitch4() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 05, 01, 5, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 05, 01, 23, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // events/phenomena VISI_L to add to the list:
        final CodedEvent eventL1enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 05, 1, 9, 0, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL1exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 05, 1, 11, 15, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL1 = new Phenomenon(eventL1enter, true, eventL1exit, true, "VISI_L", "");
        final CodedEvent eventL2enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 05, 1, 13, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL2exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 05, 1, 18, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL2 = new Phenomenon(eventL2enter, true, eventL2exit, true, "VISI_L", "");
        final CodedEvent eventL3enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 05, 1, 20, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL3exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 05, 1, 21, 30, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL3 = new Phenomenon(eventL3enter, true, eventL3exit, true, "VISI_L", "");
        // add the VISI_L events and phenomena to the timeline:
        list.addCodedEvent(eventL1enter);
        list.addCodedEvent(eventL1exit);
        list.addCodedEvent(eventL2enter);
        list.addCodedEvent(eventL2exit);
        list.addCodedEvent(eventL3enter);
        list.addCodedEvent(eventL3exit);
        list.addPhenomenon(phenL1);
        list.addPhenomenon(phenL2);
        list.addPhenomenon(phenL3);

        // events/phenomena VISI_R to add to the list:
        final CodedEvent eventR1enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 05, 1, 8, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR1exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 05, 1, 11, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR1 = new Phenomenon(eventR1enter, true, eventR1exit, true, "VISI_R", "");
        final CodedEvent eventR2enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 05, 1, 17, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR2exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 05, 1, 18, 15, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR2 = new Phenomenon(eventR2enter, true, eventR2exit, true, "VISI_R", "");
        // add the VISI_R events and phenomena to the timeline:
        list.addCodedEvent(eventR1enter);
        list.addCodedEvent(eventR1exit);
        list.addCodedEvent(eventR2enter);
        list.addCodedEvent(eventR2exit);
        list.addPhenomenon(phenR1);
        list.addPhenomenon(phenR2);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSwitch creation = new PolarizationSwitch("VISI_L", "VISI_R", 15 * 60, 15 * 60);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // the events list has 2 new elements:
        Assert.assertEquals(beforeEventsSize + 2, afterEventsSize);
        // the phenomena list has not changed:
        Assert.assertEquals(beforePhensSize, afterPhensSize);
        final Set<CodedEvent> eventsL = list.getCodedEvents().getEvents("VISI_L Selection", "", null);
        final Set<CodedEvent> eventsR = list.getCodedEvents().getEvents("VISI_R Selection", "", null);
        Assert.assertEquals(1, eventsL.size());
        Assert.assertEquals(1, eventsR.size());
        final Iterator<CodedEvent> iterL = eventsL.iterator();
        final Iterator<CodedEvent> iterR = eventsR.iterator();
        Assert.assertEquals(startDate, iterR.next().getDate());
        Assert.assertEquals(eventR1exit.getDate(), iterL.next().getDate());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#POLARIZATION_SWITCH}
     * 
     * @testedMethod {@link PolarizationSwitch#PolarizationSwitch(String, String, double, double)}
     * @testedMethod {@link PolarizationSwitch#applyTo(Timeline)}
     * 
     * @description tests the polarization selection switch algorithm when there are no R events/phenomena in the
     *              timeline
     * 
     * @input timeline with only one R event
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created event is the expected one (R event)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSwitchNoLeftEvents() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 05, 01, 5, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 05, 01, 23, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // creates two events/ one phenomenon VISI_R to add to the list:
        final CodedEvent eventR1enter = new CodedEvent("ENTER_VISI_R", "", new AbsoluteDate(2012, 05, 1, 8, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventR1exit = new CodedEvent("EXIT_VISI_R", "", new AbsoluteDate(2012, 05, 1, 11, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenR1 = new Phenomenon(eventR1enter, true, eventR1exit, true, "VISI_R", "");
        // add the VISI_R events and phenomenon to the timeline:
        list.addCodedEvent(eventR1enter);
        list.addCodedEvent(eventR1exit);
        list.addPhenomenon(phenR1);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSwitch creation = new PolarizationSwitch("VISI_L", "VISI_R", 15 * 60, 15 * 60);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // there is ONE new events:
        Assert.assertEquals(beforeEventsSize + 1, afterEventsSize);
        // there are no new phenomena:
        Assert.assertEquals(beforePhensSize, afterPhensSize);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#POLARIZATION_SWITCH}
     * 
     * @testedMethod {@link PolarizationSwitch#PolarizationSwitch(String, String, double, double)}
     * @testedMethod {@link PolarizationSwitch#applyTo(Timeline)}
     * 
     * @description tests the polarization selection switch algorithm when there are no L events/phenomena in the
     *              timeline
     * 
     * @input timeline with only one L event
     * 
     * @output timeline after this post processing
     * 
     * @testPassCriteria the created event is the expected one (L event)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSwitchNoRightEvents() throws PatriusException {

        // creation of the time interval
        final AbsoluteDate startDate = new AbsoluteDate(2012, 05, 01, 5, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDate endDate = new AbsoluteDate(2012, 05, 01, 23, 0, 0, TimeScalesFactory.getTAI());
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, startDate, endDate,
            IntervalEndpointType.OPEN);

        // creation of the timeline from the logger used during the propagation
        final Timeline list = new Timeline(log, interval, null);

        // creates two events/ one phenomenon VISI_L to add to the list:
        final CodedEvent eventL1enter = new CodedEvent("ENTER_VISI_L", "", new AbsoluteDate(2012, 05, 1, 8, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final CodedEvent eventL1exit = new CodedEvent("EXIT_VISI_L", "", new AbsoluteDate(2012, 05, 1, 11, 00, 0,
            TimeScalesFactory.getTAI()), false);
        final Phenomenon phenL1 = new Phenomenon(eventL1enter, true, eventL1exit, true, "VISI_L", "");
        // add the VISI_R events and phenomenon to the timeline:
        list.addCodedEvent(eventL1enter);
        list.addCodedEvent(eventL1exit);
        list.addPhenomenon(phenL1);

        final int beforeEventsSize = list.getCodedEventsList().size();
        final int beforePhensSize = list.getPhenomenaList().size();

        final PolarizationSwitch creation = new PolarizationSwitch("VISI_L", "VISI_R", 15 * 60, 15 * 60);
        creation.applyTo(list);
        final int afterEventsSize = list.getCodedEventsList().size();
        final int afterPhensSize = list.getPhenomenaList().size();
        // there is ONE new events:
        Assert.assertEquals(beforeEventsSize + 1, afterEventsSize);
        // there are no new phenomena:
        Assert.assertEquals(beforePhensSize, afterPhensSize);
    }

    /**
     * Setup for all unit tests in the class.
     * 
     * @throws PatriusException
     *         should not happen here
     */
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
        final AbsoluteDate date = new AbsoluteDate(2012, 04, 1, 11, 0, 0, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getGCRF();
        final double re = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.EGM96_EARTH_MU;
        final double a = 7200000;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.001, MathLib.toRadians(40), MathLib.toRadians(10),
            MathLib.toRadians(15), MathLib.toRadians(20), PositionAngle.MEAN, frame, date, mu);

        // final double period = MathUtils.TWO_PI * FastMath.sqrt(FastMath.pow(a, 3) / mu);

        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date,
            date.shiftedBy(10 * 24 * 3600), IntervalEndpointType.CLOSED);

        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        propagator.resetInitialState(initialState);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initialState.getMu())));

        // events

        // apogee perigee passages
        final EventDetector apogeePerigeePassagesDet = new ApsideDetector(initialOrbit, 2){
            private static final long serialVersionUID = 7149796307062112194L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final GenericCodingEventDetector apogeePerigeePassages = new GenericCodingEventDetector(
            apogeePerigeePassagesDet, "apogee passage", "perigee passage");

        // nodes passages
        final EventDetector nodesPassagesDet = new NodeDetector(initialOrbit, initialOrbit.getFrame(),
            NodeDetector.ASCENDING_DESCENDING){
            private static final long serialVersionUID = 1528780196650676150L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final GenericCodingEventDetector nodesPassages = new GenericCodingEventDetector(nodesPassagesDet,
            "ascending node", "descending node");

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
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final GenericCodingEventDetector eclipse = new GenericCodingEventDetector(eclipseDet, "eclipse exit",
            "eclipse entrance", false, "eclipse");

        log = new CodedEventsLogger();

        propagator.resetInitialState(initialState);

        propagator.addEventDetector(log.monitorDetector(apogeePerigeePassages));
        propagator.addEventDetector(log.monitorDetector(nodesPassages));
        propagator.addEventDetector(log.monitorDetector(eclipse));
        propagator.propagate(interval.getLowerData(), interval.getUpperData());
    }
}
