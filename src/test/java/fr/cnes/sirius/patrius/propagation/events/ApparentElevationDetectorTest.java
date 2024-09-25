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
 * VERSION:4.13.1:FA:FA-177:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-118:08/12/2023:[PATRIUS] Calcul d'union de PyramidalField invalide
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait 
 *          retourner un CelestialBodyFrame 
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.DatationChoice;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.ApparentElevationDetector;
import fr.cnes.sirius.patrius.events.utils.SignalPropagationWrapperDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
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
        final CelestialBodyFrame ITRF2005 = FramesFactory.getITRF(); // terrestrial frame at an arbitrary date
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(48.833), MathLib.toRadians(2.333), 0.0, "");
        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");
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
        final CelestialBodyFrame ITRF2005 = FramesFactory.getITRF(); // terrestrial frame at an arbitrary date
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(48.833), MathLib.toRadians(2.333), 0.0, "");
        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");
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
    public void testDefaultConstructor() throws PatriusException {
        // Earth and frame
        final double ae = 6378137.0; // equatorial radius in meter
        final double f = 1.0 / 298.257223563; // flattening
        final CelestialBodyFrame ITRF2005 = FramesFactory.getITRF(); // terrestrial frame at an arbitrary date
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(48.833), MathLib.toRadians(2.333), 0.0, "");
        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");
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

    /**
     * @description Test this event detector wrap feature in {@link SignalPropagationWrapperDetector}
     * 
     * @input this event detector in INSTANTANEOUS & LIGHT_SPEED
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

        // Build two identical event detectors (the first in INSTANTANEOUS, the second in LIGHT_SPEED)
        final double ae = 6378137.0; // equatorial radius in meter
        final double f = 1.0 / 298.257223563; // flattening
        final CelestialBodyFrame ITRF2005 = FramesFactory.getITRF(); // terrestrial frame at an arbitrary date
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(48.833), MathLib.toRadians(2.333), 0.0, "");
        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");

        final ApparentElevationDetector eventDetector1 = new ApparentElevationDetector(MathLib.toRadians(2.0), topo,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        eventDetector1.setPressure(101325);
        eventDetector1.setTemperature(290);
        final ApparentElevationDetector eventDetector2 = (ApparentElevationDetector) eventDetector1.copy();
        eventDetector2.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());

        // Wrap these event detectors
        final SignalPropagationWrapperDetector wrapper1 = new SignalPropagationWrapperDetector(eventDetector1);
        final SignalPropagationWrapperDetector wrapper2 = new SignalPropagationWrapperDetector(eventDetector2);

        // Add them in the propagator, then propagate
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, TimeScalesFactory.getTAI());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(),
            date, this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance,
            relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        propagator.addEventDetector(wrapper1);
        propagator.addEventDetector(wrapper2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(6000));

        // Evaluate the first event detector wrapper (INSTANTANEOUS) (emitter dates should be equal to receiver dates)
        Assert.assertEquals(2, wrapper1.getNBOccurredEvents());
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2003-09-16T00:28:50.970"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2003-09-16T00:28:50.970"), 1e-3));
        Assert.assertTrue(wrapper1.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2003-09-16T00:38:54.286"), 1e-3));
        Assert.assertTrue(wrapper1.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2003-09-16T00:38:54.286"), 1e-3));

        // Evaluate the second event detector wrapper (LIGHT_SPEED) (emitter dates should be before receiver dates)
        Assert.assertEquals(2, wrapper2.getNBOccurredEvents());
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(0)
            .equals(new AbsoluteDate("2003-09-16T00:28:50.970"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(0)
            .equals(new AbsoluteDate("2003-09-16T00:28:50.980"), 1e-3));
        Assert.assertTrue(wrapper2.getEmitterDatesList().get(1)
            .equals(new AbsoluteDate("2003-09-16T00:38:54.285"), 1e-3));
        Assert.assertTrue(wrapper2.getReceiverDatesList().get(1)
            .equals(new AbsoluteDate("2003-09-16T00:38:54.295"), 1e-3));

        // Evaluate the AbstractSignalPropagationDetector's abstract methods implementation
        Assert.assertEquals(finalState.getOrbit(), eventDetector1.getEmitter(finalState));
        Assert.assertEquals(topo, eventDetector1.getReceiver(null));
        Assert.assertEquals(DatationChoice.EMITTER, eventDetector1.getDatationChoice());
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
