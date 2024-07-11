/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.SortedSet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.CodedEventsLogger.LoggedCodedEvent;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GroundMaskElevationDetectorTest {

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    @Test
    public void testEvent() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, this.mu);

        final Propagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);

        // Earth and frame
        final double ae = 6378137.0; // equatorial radius in meter
        final double f = 1.0 / 298.257223563; // flattening
        final Frame ITRF2005 = FramesFactory.getITRF(); // terrestrial frame at an arbitrary date
        final BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(48.833),
            MathLib.toRadians(2.333),
            0.0);
        final TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        final double[][] masque = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(30), MathLib.toRadians(4) },
            { MathLib.toRadians(60), MathLib.toRadians(3) },
            { MathLib.toRadians(90), MathLib.toRadians(2) },
            { MathLib.toRadians(120), MathLib.toRadians(3) },
            { MathLib.toRadians(150), MathLib.toRadians(4) },
            { MathLib.toRadians(180), MathLib.toRadians(5) },
            { MathLib.toRadians(210), MathLib.toRadians(6) },
            { MathLib.toRadians(240), MathLib.toRadians(5) },
            { MathLib.toRadians(270), MathLib.toRadians(4) },
            { MathLib.toRadians(300), MathLib.toRadians(3) },
            { MathLib.toRadians(330), MathLib.toRadians(4) } };
        final GroundMaskElevationDetector detector =
            new GroundMaskElevationDetector(masque, topo){
                /** Serializable UID. */
                private static final long serialVersionUID = 7515758050410436713L;

                @Override
                public
                        Action
                        eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                               throws PatriusException {
                    return increasing ? Action.STOP : Action.CONTINUE;
                }
            };

        final AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        // Coded events logger
        final CodedEventsLogger eventsLogger = new CodedEventsLogger();
        // Coding event detector
        final GenericCodingEventDetector eventDetector = new GenericCodingEventDetector(detector, "ENTER",
            "EXIT", true, "GroundMaskElevation");
        // Date interval
        final AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(startDate, Constants.JULIAN_DAY);
        // Check that the event set is empty
        final SortedSet<LoggedCodedEvent> eventSet = eventsLogger.getLoggedCodedEventSet();
        Assert.assertTrue(eventSet.isEmpty());
        // Log events over time interval
        final SpacecraftState fs = AbstractDetector.logEventsOverTimeInterval(eventsLogger, propagator,
            eventDetector, dateInterval);
        // Check that the event set is not empty anymore, but it now contains 1 element
        Assert.assertEquals(1, eventSet.size());
        // Check that the event detector has been cleared
        Assert.assertTrue(propagator.getEventsDetectors().isEmpty());
        final double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(0.065, elevation, 2.0e-5);

    }

    @Test
    public void testMasque() throws PatriusException {

        // Earth and frame
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF());
        final TopocentricFrame topo = new TopocentricFrame(earth, new GeodeticPoint(0.0, 0.0, 0.0), "");
        final double[][] masque = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(180), MathLib.toRadians(3) },
            { MathLib.toRadians(-90), MathLib.toRadians(4) } };
        final GroundMaskElevationDetector detector = new GroundMaskElevationDetector(masque, topo);

        final double azimuth = MathLib.toRadians(90);
        final double elevation = detector.getElevation(azimuth);
        Assert.assertEquals(MathLib.toRadians(4), elevation, 1.0e-15);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testException() throws PatriusException {

        // Earth and frame
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF());
        final TopocentricFrame topo = new TopocentricFrame(earth, new GeodeticPoint(0.0, 0.0, 0.0), "");
        final double[][] masque = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(360), MathLib.toRadians(4) } };
        new GroundMaskElevationDetector(masque, topo);

    }

    @Test
    public void testConstructor() throws PatriusException {

        // Earth and frame
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF());
        final TopocentricFrame topo = new TopocentricFrame(earth, new GeodeticPoint(0.0, 0.0, 0.0), "");
        final double[][] masque = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(180), MathLib.toRadians(3) },
            { MathLib.toRadians(-90), MathLib.toRadians(4) } };
        final GroundMaskElevationDetector detector =
            new GroundMaskElevationDetector(masque, topo, 10, 0.1, Action.CONTINUE,
                Action.STOP);
        final GroundMaskElevationDetector detector2 = (GroundMaskElevationDetector) detector.copy();
        // test getter
        Assert.assertEquals(topo, detector2.getTopocentricFrame());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        this.mu = 3.9860047e14;
        this.ae = 6.378137e6;
        this.c20 = -1.08263e-3;
        this.c30 = 2.54e-6;
        this.c40 = 1.62e-6;
        this.c50 = 2.3e-7;
        this.c60 = -5.5e-7;
    }

    @After
    public void tearDown() {
        this.mu = Double.NaN;
        this.ae = Double.NaN;
        this.c20 = Double.NaN;
        this.c30 = Double.NaN;
        this.c40 = Double.NaN;
        this.c50 = Double.NaN;
        this.c60 = Double.NaN;
    }

}
