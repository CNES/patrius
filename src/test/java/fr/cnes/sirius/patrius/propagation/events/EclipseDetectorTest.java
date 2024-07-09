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
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.5:DM:DM-2245:27/05/2020:Ameliorations de EclipseDetector 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * VERSION::FA:382:09/12/2014:Eclipse detector corrections
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:491:04/11/2015: Added test testSatUnderOcculingBodySurface
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class EclipseDetectorTest {

    private double mu;
    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;

    private final double sunRadius = 696000000.;
    private final double earthRadius = 6400000.;

    @Test
    public void testEclipse() throws PatriusException {
        this.propagator.addEventDetector(new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 0,
            60., 1.e-3){
            private static final long serialVersionUID = 1L;

            @Override
            public Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                            throws PatriusException {
                return increasing ? Action.CONTINUE : Action.STOP;
            }
        });
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(this.iniDate), 1.0e-3);

    }

    // test for coverage
    @Test
    public void testConstructors() throws PatriusException {
        // fake GeometricBodyShape:
        final GeometricBodyShape body = new GeometricBodyShape(){

            @Override
            public Frame getBodyFrame() {
                return null;
            }

            @Override
            public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                    final AbsoluteDate date)
                    throws PatriusException {
                return null;
            }

            @Override
            public GeodeticPoint
                    transform(final Vector3D point, final Frame frame, final AbsoluteDate date) throws PatriusException {
                return null;
            }

            @Override
            public Vector3D transform(final GeodeticPoint point) {
                return null;
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO);
            }

            @Override
            public Vector3D[]
                    getIntersectionPoints(final Line line, final Frame frame, final AbsoluteDate date)
                            throws PatriusException {
                final Vector3D[] list = new Vector3D[3];
                list[0] = new Vector3D(0, 0.1, 0);
                list[1] = new Vector3D(0, 0.5, 0);
                list[2] = new Vector3D(0, 1, 0);
                return list;
            }

            @Override
            public double
                    distanceTo(final Line line, final Frame frame, final AbsoluteDate date) throws PatriusException {
                return 200.0;
            }

            @Override
            public String getName() {
                return "Fake GeometricBodyShape";
            }

            @Override
            public double getLocalRadius(final Vector3D position, final Frame frame, final AbsoluteDate date,
                    final PVCoordinatesProvider occultedBody) throws PatriusException {
                if (date.durationFrom(EclipseDetectorTest.this.iniDate) == 0.0) {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                } else {
                    return 0;
                }

            }

            @Override
            public GeodeticPoint getIntersectionPoint(Line line,
                    Vector3D close,
                    Frame frame,
                    AbsoluteDate date,
                    double altitude) throws PatriusException {
                return null;
            }

        };
        final EclipseDetector eclipse1 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        boolean isTestOk = false;
        try {
            eclipse1.g(this.initialState);
        } catch (final PatriusExceptionWrapper e) {
            isTestOk = true;
        }
        Assert.assertTrue(isTestOk);

        new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1, 100, 1E-6);
        final SpacecraftState state = this.initialState.shiftedBy(2000.0);
        // Cover the EclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, double, double,
        // double)
        // constructor:
        EclipseDetector eclipse3 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1e-12, 100, 1E-6);
        Assert.assertTrue(eclipse3.isTotalEclipse());
        eclipse3 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1.0 - 1e-12, 100, 1E-6);
        Assert.assertFalse(eclipse3.isTotalEclipse());
        eclipse3 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 0.5, 100, 1E-6);
        Assert.assertFalse(eclipse3.isTotalEclipse());

        // Cover the EclipseDetector(PVCoordinatesProvider, double, GeometricBodyShape, double, double, double)
        // constructor:
        EclipseDetector eclipse4 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6);
        Assert.assertTrue(eclipse4.isTotalEclipse());
        eclipse4 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1.0 - 1e-12, 100, 1E-6);
        Assert.assertFalse(eclipse4.isTotalEclipse());
        eclipse4 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 0.5, 100, 1E-6);
        Assert.assertFalse(eclipse4.isTotalEclipse());

        // Cover the EclipseDetector with two actions
        final EclipseDetector eclipse5 =
            new EclipseDetector(null, CelestialBodyFactory.getSun(), this.sunRadius, 100, 1E-6,
                Action.RESET_STATE, Action.CONTINUE);
        final EclipseDetector detector2 = (EclipseDetector) eclipse5.copy();
        Assert.assertTrue(detector2.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(state, false, true));
        Assert.assertNull(detector2.getOccultedDirection());

        // Cover the EclipseDetector with two actions
        final EclipseDetector eclipse6 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1e-12, 100, 1E-6, Action.RESET_STATE, Action.CONTINUE);
        Assert.assertTrue(eclipse6.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse6.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, eclipse6.eventOccurred(state, false, true));

        // Cover the EclipseDetector with two actions
        final EclipseDetector eclipse7 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6, Action.RESET_STATE, Action.CONTINUE);
        Assert.assertTrue(eclipse7.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse7.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, eclipse7.eventOccurred(state, false, true));

        Assert.assertEquals(Action.RESET_STATE, eclipse7.getActionAtEntry());
        Assert.assertEquals(Action.CONTINUE, eclipse7.getActionAtExit());
        Assert.assertEquals(false, eclipse7.removeAtEntry());
        Assert.assertEquals(false, eclipse7.removeAtExit());

        // Constructors for GENOPUS
        final EclipseDetector eclipse8 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 0, EclipseDetector.EXIT, 100, 1E-6, Action.RESET_STATE, false);
        Assert.assertEquals(Action.RESET_STATE, eclipse8.eventOccurred(state, true, true));
        final EclipseDetector eclipse9 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 1, EclipseDetector.ENTRY, 100, 1E-6, Action.RESET_DERIVATIVES, false);
        Assert.assertEquals(Action.RESET_DERIVATIVES, eclipse9.eventOccurred(state, false, true));
        final EclipseDetector eclipse10 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 0.5, EclipseDetector.ENTRY, 100, 1E-6, Action.RESET_DERIVATIVES, false);
        Assert.assertEquals(Action.RESET_DERIVATIVES, eclipse10.eventOccurred(state, false, true));

        // Evaluate the EclipseDetector(PVCoordinatesProvider, double, GeometricBodyShape,
        // double, double, double, int) constructor
        final EclipseDetector eclipse11 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6, 0);
        Assert.assertTrue(eclipse11.isTotalEclipse());
        Assert.assertEquals(Action.STOP, eclipse11.eventOccurred(state, true, true));
        Assert.assertEquals(Action.STOP, eclipse11.eventOccurred(state, false, true));

        Assert.assertEquals(null, eclipse11.getActionAtEntry());
        Assert.assertEquals(Action.STOP, eclipse11.getActionAtExit());
        Assert.assertEquals(false, eclipse11.removeAtEntry());
        Assert.assertEquals(false, eclipse11.removeAtExit());
        
        //Evaluate the EclipseDetector(PVCoordinatesProvider, double, GeometricBodyShape, double, double, double,
        // Action, boolean, int) constructor
        final EclipseDetector eclipse12 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6, Action.CONTINUE, false, 2);
        Assert.assertTrue(eclipse12.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse12.eventOccurred(state, true, true));
        Assert.assertEquals(Action.CONTINUE, eclipse12.eventOccurred(state, false, true));

        Assert.assertEquals(Action.CONTINUE, eclipse12.getActionAtEntry());
        Assert.assertEquals(Action.CONTINUE, eclipse12.getActionAtExit());
        Assert.assertEquals(false, eclipse12.removeAtEntry());
        Assert.assertEquals(false, eclipse12.removeAtExit());

        // Evaluate the EclipseDetector(PVCoordinatesProvider, double, GeometricBodyShape, double, double, double,
        // Action, Action, boolean, boolean, int) constructor
        final EclipseDetector eclipse13 = (EclipseDetector) new EclipseDetector(CelestialBodyFactory.getSun(),
                this.sunRadius, body, 1e-12, 100, 1E-6, Action.RESET_STATE, Action.CONTINUE, false, true, 2).copy();
        Assert.assertTrue(eclipse13.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse13.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, eclipse13.eventOccurred(state, false, true));

        Assert.assertEquals(Action.RESET_STATE, eclipse13.getActionAtEntry());
        Assert.assertEquals(Action.CONTINUE, eclipse13.getActionAtExit());
        Assert.assertEquals(false, eclipse13.removeAtEntry());
        Assert.assertEquals(true, eclipse13.removeAtExit());
    }

    @Test
    public void testPenumbra() throws PatriusException {
        this.propagator.addEventDetector(new EclipseDetector(
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD));
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(4388.1558707427685, finalState.getDate().durationFrom(this.iniDate), 1.0e-6);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @description test the EclipseDetector when the satellite is under the occulting body surface (possible margin is
     *              taken into account)
     *              with occulted occulting satellite angle OBTU
     *              This test was created following the FT-491
     *              The occulted body is a fictitious Sun and the occulting body is a fictitious Moon, whose coordinates
     *              are:
     *              Moon coordinates : [0, -(x/2 + delta), 0], radius Moon = 1/2x
     *              Sun coordinates : [0, 3x, 0], radius Sun = 1/2x
     *              x is the semi-major axis of the equatorial and circular orbit.
     * @input an equatorial orbit, a fictitious Sun and Moon
     * @output the final spacecraft state
     * @testPassCriteria exit the eclipse at 90° occulted - occulting - satellite angle. Spacecraft initially in the
     *                   shadow (and below the occulting body surface).
     * @referenceVersion 3.1
     * @nonregressionVersion 3.1
     */
    @Test
    public void testSatUnderOcculingBodySurfaceAngleObtu() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double x = 7780e3;
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(0.0, -x, 0.0),
            new Vector3D(7157.792507, 0.0, 0.0)), FramesFactory.getGCRF(), date, this.mu);
        final double keplerianPeriod = orbit.getKeplerianPeriod();

        // delta (can represent the margin in the occulting body radius)
        final double delta = x / 100;

        // Fictitious Sun:
        final PVCoordinatesProvider testSun = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(0, 3 * x, 0), Vector3D.ZERO);
            }
        };
        // Fictitious Moon:
        final PVCoordinatesProvider testMoon = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(0, -(x / 2 + delta), 0.0), Vector3D.ZERO);
            }
        };
        final double radiusSun = 0.5 * x;
        final double radiusMoon = 0.5 * x;

        final SpacecraftState initState = new SpacecraftState(orbit);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initState);

        // Eclipse detector, propagation will stop when exiting the eclipse
        final EclipseDetector eclipseDetector =
            new EclipseDetector(testSun, radiusSun, testMoon, radiusMoon, 0.0, 1000, 1E-6);

        // Check that the satellite is under the occulting body surface
        final double dSatOccculting = initState.getPVCoordinates().getPosition()
            .distance(testMoon.getPVCoordinates(this.iniDate, FramesFactory.getGCRF()).getPosition());
        Assert.assertTrue(dSatOccculting < radiusMoon);

        // Propagation with eclipse detector
        propagator.addEventDetector(eclipseDetector);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(keplerianPeriod));

        // As the angle occulted occulting satellite angle is OBTU and the radius of the fictitious Sun and the
        // fictitious moon are equal, final position of spacecraft should be x/2 (make a drawing):
        Assert.assertEquals(finalState.getPVCoordinates().getPosition().getX(), 0.5 * x, 1e-4);

        // Check that g function is negative around initial date (below the occulting body surface but in the shadow)
        Assert.assertTrue(eclipseDetector.g(initState) < 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(100)) < 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(-100)) < 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @description test the EclipseDetector when the satellite is under the occulting body surface (possible margin is
     *              taken into account)
     *              with occulted occulting satellite angle AIGU
     *              This test was created following the FT-491
     *              The occulted body is a fictitious Sun and the occulting body is a fictitious Moon, whose coordinates
     *              are:
     *              Moon coordinates : [0, -(x/2 + delta), 0], radius Moon = 1/2x
     *              Sun coordinates : [2x, -3x, 0], radius Sun = 1/2x
     *              x is the semi-major axis of the equatorial and circular orbit.
     * @input an equatorial orbit, a fictitious Sun and Moon
     * @output the final spacecraft state
     * @testPassCriteria exit the eclipse at -90° occulted - occulting - satellite angle. Spacecraft initially not in
     *                   the shadow (although below the occulting body surface).
     * @referenceVersion 3.1
     * @nonregressionVersion 3.1
     */
    @Test
    public void testSatUnderOcculingBodySurfaceAngleAigu() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double x = 7780e3;
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(0.0, -x, 0.0),
            new Vector3D(7157.792507, 0.0, 0.0)), FramesFactory.getGCRF(), date, this.mu);
        final double keplerianPeriod = orbit.getKeplerianPeriod();

        // delta (can represent the margin in the occulting body radius)
        final double delta = x / 100;

        // Fictitious Sun:
        final PVCoordinatesProvider testSun = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(0, -3 * x, 0), Vector3D.ZERO);
            }
        };
        // Fictitious Moon:
        final PVCoordinatesProvider testMoon = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(new Vector3D(0, -(x / 2 + delta), 0.0), Vector3D.ZERO);
            }
        };
        final double radiusSun = 0.5 * x;
        final double radiusMoon = 0.5 * x;

        final SpacecraftState initState = new SpacecraftState(orbit);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initState);

        // Eclipse detector, propagation will stop when exiting the eclipse
        final EclipseDetector eclipseDetector =
            new EclipseDetector(testSun, radiusSun, testMoon, radiusMoon, 0.0, 1000, 1E-6);

        // Check that the satellite is under the occulting body surface
        final double dSatOccculting = initState.getPVCoordinates().getPosition()
            .distance(testMoon.getPVCoordinates(this.iniDate, FramesFactory.getGCRF()).getPosition());
        Assert.assertTrue(dSatOccculting < radiusMoon);

        // Propagation with eclipse detector
        propagator.addEventDetector(eclipseDetector);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(keplerianPeriod));

        // As the angle occulted occulting satellite angle is AIGU and the radius of the fictitious Sun and the
        // fictitious moon are equal, final position of spacecraft should be - x/2 (make a drawing)
        Assert.assertEquals(finalState.getPVCoordinates().getPosition().getX(), -0.5 * x, 1e-4);

        // Check that g function is positive around initial date (below the occulting body surface but enlightened)
        Assert.assertTrue(eclipseDetector.g(initState) > 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(100)) > 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(-100)) > 0);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        this.mu = 3.9860047e14;
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.iniDate, this.mu);
        this.initialState = new SpacecraftState(orbit);
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6
        };
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7
        };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        this.propagator = new NumericalPropagator(integrator);
        this.propagator.setInitialState(this.initialState);
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
        this.propagator = null;
    }

}
