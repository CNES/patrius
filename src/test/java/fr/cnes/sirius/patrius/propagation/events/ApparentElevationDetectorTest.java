/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
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
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ApparentElevationDetectorTest {

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;
    private Orbit orbit;

    @Test
    public void testHorizon() throws PatriusException {

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
        final ApparentElevationDetector detector = new ApparentElevationDetector(MathLib.toRadians(0.0), topo,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.CONTINUE);

        final AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        final double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(MathLib.toRadians(-0.5746255623877098), elevation, 2.0e-5);

    }

    @Test
    public void testPresTemp() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();

        final Propagator propagator =
            new EcksteinHechlerPropagator(this.orbit, this.ae, this.mu, this.orbit.getFrame(), this.c20, this.c30,
                this.c40, this.c50, this.c60,
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
        final ApparentElevationDetector detector = new ApparentElevationDetector(MathLib.toRadians(2.0), topo,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.STOP, Action.CONTINUE);
        detector.setPressure(101325);
        detector.setTemperature(290);

        final AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        final double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(MathLib.toRadians(1.7026104902251749), elevation, 2.0e-5);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link ApparentElevationDetector#ApparentElevationDetector(double, TopocentricFrame)}
     * 
     * @description Test default constructor with default actions
     * 
     * @input a default constructor
     * 
     * @output The default behavior : {@link EventDetector.Action#CONTINUE continue} propagation at raising and to
     *         {@link EventDetector.Action#STOP stop} propagation at setting
     * 
     * @testPassCriteria The default actions should be returned by eventOccurred
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testDefaultConstructor() throws PatriusException {
        // Earth and frame
        final double ae = 6378137.0; // equatorial radius in meter
        final double f = 1.0 / 298.257223563; // flattening
        final Frame ITRF2005 = FramesFactory.getITRF(); // terrestrial frame at an arbitrary date
        final BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(48.833),
            MathLib.toRadians(2.333),
            0.0);
        final TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        final ApparentElevationDetector detector = new ApparentElevationDetector(MathLib.toRadians(2.0), topo);
        final ApparentElevationDetector detector2 = (ApparentElevationDetector) detector.copy();
        final SpacecraftState s = new SpacecraftState(this.orbit);
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, true, true));
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(s, true, false));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(s, false, true));
        Assert.assertEquals(Action.STOP, detector2.eventOccurred(s, false, false));

        final ApparentElevationDetector detector3 = new ApparentElevationDetector(MathLib.toRadians(2.0), topo,
            AbstractDetector.DEFAULT_MAXCHECK);
        Assert.assertNotNull(detector3);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.mu = 3.9860047e14;
        this.ae = 6.378137e6;
        this.c20 = -1.08263e-3;
        this.c30 = 2.54e-6;
        this.c40 = 1.62e-6;
        this.c50 = 2.3e-7;
        this.c60 = -5.5e-7;

        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, TimeScalesFactory.getUTC());
        this.orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, this.mu);
    }

    @After
    public void tearDown() throws PatriusException {
        this.mu = Double.NaN;
        this.ae = Double.NaN;
        this.c20 = Double.NaN;
        this.c30 = Double.NaN;
        this.c40 = Double.NaN;
        this.c50 = Double.NaN;
        this.c60 = Double.NaN;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

    }

}
