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
* VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
* VERSION:4.9:FA:FA-3184:10/05/2022:[PATRIUS] Non detection d'evenement pour une propagation de duree tres courte
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* VERSION:4.7:DM:DM-2728:18/05/2021:implementation default de AttitudeProvider#getAttitude(Orbit) 
* VERSION:4.7:FA:FA-2886:18/05/2021:Pas de pas de propagation par defaut avec une orbite hyperbolique 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:468:22/10/2015:Proper handling of ephemeris mode for analytical propagators
 * VERSION::FA:478:22/10/2015:Proper handling of ephemeris when calling getGeneratedEphemeris()
 * VERSION::DM:484:25/09/2015:Get additional state from an AbsoluteDate
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::DM:426:30/10/2015: Tests the new functionalities on orbit definition and orbit propagation
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.io.IOException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterGroundPointing;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AltitudeDetector;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.ElevationDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandlerMultiplexer;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class KeplerianPropagatorTest {

    // Body mu
    private double mu;
    private Orbit initialOrbitPV;
    private AbsoluteDate initDatePV;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(KeplerianPropagatorTest.class.getSimpleName(), "Keplerian propagator");
    }

    /**
     * FT-3184.
     * 
     * @testType UT
     * 
     * @description
     *              Test proper event detection on propagation duration < 1-14s.
     * 
     * @testPassCriteria event properly detected.
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testEventDetectionShortInterval() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate(2014, 1, 1, TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(24400e3, 0.72, MathLib.toRadians(5), MathLib.toRadians(180),
            MathLib.toRadians(2), MathLib.toRadians(180),
            PositionAngle.TRUE, FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        // Add event detector
        propagator.addEventDetector(new DateDetector(initialDate));
        
        // Propagation on interval < 1E-14s
        final SpacecraftState state = propagator.propagate(initialDate.shiftedBy(1E-15));

        // Check propagator has stopped on event
        Assert.assertEquals(state.getDate().durationFrom(initialDate), 0., 0.);
    }

    @Test
    public void sameDateCartesian() throws PatriusException {

        /** Default mass. */
        final MassProvider DEFAULT_MASS = new SimpleMassModel(1000.0, "DEFAULT");

        // Extrapolator definition
        // -----------------------
        final KeplerianPropagator extrapolator =
            new KeplerianPropagator(this.initialOrbitPV, null, this.initialOrbitPV.getMu(),
                DEFAULT_MASS);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = this.initDatePV.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        final double a = finalOrbit.getA();
        // another way to compute n
        final double n = MathLib.sqrt(finalOrbit.getMu() / MathLib.pow(a, 3));

        Assert.assertEquals(n * delta_t,
            finalOrbit.getLM() - this.initialOrbitPV.getLM(),
            Utils.epsilonTest * MathLib.abs(n * delta_t));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(), this.initialOrbitPV.getLM()),
            this.initialOrbitPV.getLM(), Utils.epsilonAngle * MathLib.abs(this.initialOrbitPV.getLM()));

        Assert.assertEquals(finalOrbit.getA(), this.initialOrbitPV.getA(),
            Utils.epsilonTest * this.initialOrbitPV.getA());
        Assert.assertEquals(finalOrbit.getE(), this.initialOrbitPV.getE(), Utils.epsilonE * this.initialOrbitPV.getE());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(), this.initialOrbitPV.getI()),
            this.initialOrbitPV.getI(),
            Utils.epsilonAngle * MathLib.abs(this.initialOrbitPV.getI()));
        Assert.assertEquals(1000.0, finalOrbit.getMass("DEFAULT"), 0.0);
        // test the exception of the getMass method:
        boolean rez = false;
        try {
            Assert.assertEquals(1000.0, finalOrbit.getMass("WRONG"), 0.0);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    @Test
    public void sameDateKeplerian() throws PatriusException {
        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
            6.2, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, this.mu);

        // Extrapolator definition
        // -----------------------
        final SimpleMassModel massModel = new SimpleMassModel(1000.0, "DEFAULT_MASS");
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        final KeplerianPropagator extrapolator =
            new KeplerianPropagator(initialOrbit, bodyCenterPointing, inertial, this.mu,
                massModel);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), 0.0);
        Assert.assertEquals(finalOrbit.getE(), initialOrbit.getE(), 0.0);
        Assert.assertEquals(finalOrbit.getI(), initialOrbit.getI(), 0.0);
        Assert.assertEquals(finalOrbit.getLM(), initialOrbit.getLM(), 0.0);

        // Check attitude
        final Attitude expAttForces = bodyCenterPointing.getAttitude(initialOrbit, initialOrbit.getDate(),
            initialOrbit.getFrame());
        final Attitude actualAttForces = finalOrbit.getAttitudeForces();
        final Attitude expAttEvents = inertial.getAttitude(initialOrbit, initialOrbit.getDate(),
            initialOrbit.getFrame());
        final Attitude actualAttEvents = finalOrbit.getAttitudeEvents();
        Assert.assertEquals(expAttForces.getRotation().getQi()[0], actualAttForces.getRotation().getQi()[0],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttForces.getRotation().getQi()[1], actualAttForces.getRotation().getQi()[1],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttForces.getRotation().getQi()[2], actualAttForces.getRotation().getQi()[2],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttForces.getRotation().getQi()[3], actualAttForces.getRotation().getQi()[3],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttForces.getSpin().getX(), actualAttForces.getSpin().getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttForces.getSpin().getY(), actualAttForces.getSpin().getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttForces.getSpin().getZ(), actualAttForces.getSpin().getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);

        Assert.assertEquals(expAttEvents.getRotation().getQi()[0], actualAttEvents.getRotation().getQi()[0],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttEvents.getRotation().getQi()[1], actualAttEvents.getRotation().getQi()[1],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttEvents.getRotation().getQi()[2], actualAttEvents.getRotation().getQi()[2],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttEvents.getRotation().getQi()[3], actualAttEvents.getRotation().getQi()[3],
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttEvents.getSpin().getX(), actualAttEvents.getSpin().getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttEvents.getSpin().getY(), actualAttEvents.getSpin().getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expAttEvents.getSpin().getZ(), actualAttEvents.getSpin().getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);

    }

    @Test
    public void propagatedCartesian() throws PatriusException {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final double mu = 3.9860047e14;

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        final KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit, new ConstantAttitudeLaw(
            FramesFactory.getEME2000(), Rotation.IDENTITY), mu);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        final double delta_t = 100000.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        // computation of (M final - M initial) with another method
        final double a = finalOrbit.getA();
        // another way to compute n
        final double n = MathLib.sqrt(finalOrbit.getMu() / MathLib.pow(a, 3));

        Assert.assertEquals(n * delta_t,
            finalOrbit.getLM() - initialOrbit.getLM(),
            Utils.epsilonAngle);

        // computation of M final orbit
        final double LM = finalOrbit.getLE()
            - finalOrbit.getEquinoctialEx() * MathLib.sin(finalOrbit.getLE())
            + finalOrbit.getEquinoctialEy() * MathLib.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(MathLib.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()),
            Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        final double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        final double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        final double delta = finalOrbit.getEquinoctialEx()
            * (MathLib.sin(finalOrbit.getLE()) - MathLib.sin(initialOrbit.getLE()))
            - finalOrbit.getEquinoctialEy()
            * (MathLib.cos(finalOrbit.getLE()) - MathLib.cos(initialOrbit.getLE()));

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assert.assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

        // for final orbit
        final double ex = finalOrbit.getEquinoctialEx();
        final double ey = finalOrbit.getEquinoctialEy();
        final double hx = finalOrbit.getHx();
        final double hy = finalOrbit.getHy();
        final double LE = finalOrbit.getLE();

        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double hx2 = hx * hx;
        final double hy2 = hy * hy;
        final double h2p1 = 1. + hx2 + hy2;
        final double beta = 1. / (1. + MathLib.sqrt(1. - ex2 - ey2));

        final double x3 = -ex + (1. - beta * ey2) * MathLib.cos(LE) + beta * ex * ey * MathLib.sin(LE);
        final double y3 = -ey + (1. - beta * ex2) * MathLib.sin(LE) + beta * ex * ey * MathLib.cos(LE);

        final Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1,
            (2. * hx * hy) / h2p1,
            (-2. * hy) / h2p1);

        final Vector3D V = new Vector3D((2. * hx * hy) / h2p1,
            (1. - hx2 + hy2) / h2p1,
            (2. * hx) / h2p1);

        final Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
            Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void propagatedKeplerian() throws PatriusException {

        Report.printMethodHeader("propagatedKeplerian", "Propagation", "Math", Utils.epsilonAngle,
            ComparisonType.RELATIVE);

        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
            6.2, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, this.mu);

        // Extrapolator definition
        // -----------------------
        final KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        final double delta_t = 100000.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);
        Assert.assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod(), 1.0e-12);
        Assert.assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion(), 1.0e-16);

        // computation of (M final - M initial) with another method
        final double a = finalOrbit.getA();
        // another way to compute n
        final double n = MathLib.sqrt(finalOrbit.getMu() / MathLib.pow(a, 3));

        Assert.assertEquals(n * delta_t,
            finalOrbit.getLM() - initialOrbit.getLM(),
            Utils.epsilonAngle);

        // computation of M final orbit
        final double LM = finalOrbit.getLE()
            - finalOrbit.getEquinoctialEx() * MathLib.sin(finalOrbit.getLE())
            + finalOrbit.getEquinoctialEy() * MathLib.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(MathLib.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()),
            Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        final double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        final double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        final double delta = finalOrbit.getEquinoctialEx()
            * (MathLib.sin(finalOrbit.getLE()) - MathLib.sin(initialOrbit.getLE()))
            - finalOrbit.getEquinoctialEy()
            * (MathLib.cos(finalOrbit.getLE()) - MathLib.cos(initialOrbit.getLE()));

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        Assert.assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

        Report.printToReport("a", initialOrbit.getA(), finalOrbit.getA());
        Report.printToReport("ex", initialOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEx());
        Report.printToReport("ey", initialOrbit.getEquinoctialEy(), finalOrbit.getEquinoctialEy());
        Report.printToReport("hx", initialOrbit.getHx(), finalOrbit.getHx());
        Report.printToReport("hy", initialOrbit.getHy(), finalOrbit.getHy());
        Report.printToReport("LM", deltaE - delta, deltaM);

        // for final orbit
        final double ex = finalOrbit.getEquinoctialEx();
        final double ey = finalOrbit.getEquinoctialEy();
        final double hx = finalOrbit.getHx();
        final double hy = finalOrbit.getHy();
        final double LE = finalOrbit.getLE();

        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double hx2 = hx * hx;
        final double hy2 = hy * hy;
        final double h2p1 = 1. + hx2 + hy2;
        final double beta = 1. / (1. + MathLib.sqrt(1. - ex2 - ey2));

        final double x3 = -ex + (1. - beta * ey2) * MathLib.cos(LE) + beta * ex * ey * MathLib.sin(LE);
        final double y3 = -ey + (1. - beta * ex2) * MathLib.sin(LE) + beta * ex * ey * MathLib.cos(LE);

        final Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1,
            (2. * hx * hy) / h2p1,
            (-2. * hy) / h2p1);

        final Vector3D V = new Vector3D((2. * hx * hy) / h2p1,
            (1. - hx2 + hy2) / h2p1,
            (2. * hx) / h2p1);

        final Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
            Utils.epsilonTest * r.getNorm());

    }

    @Test(expected = PropagationException.class)
    public void wrongAttitude() throws PropagationException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(1.0e10, 1.0e-4, 1.0e-2, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final AttitudeProvider wrongLaw = new AttitudeProvider(){
            private static final long serialVersionUID = 5918362126173997016L;

            @Override
            public
                    Attitude
                    getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
                                                                                                               throws PatriusException {
                throw new PatriusException(new DummyLocalizable("gasp"), new RuntimeException());
            }

            @Override
            public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
            }
        };
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, wrongLaw);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
    }

    @Test(expected = PropagationException.class)
    public void testException() throws PropagationException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final PatriusStepHandlerMultiplexer multiplexer = new PatriusStepHandlerMultiplexer();
        propagator.setMasterMode(multiplexer);
        multiplexer.add(new PatriusStepHandler(){
            private static final long serialVersionUID = 8183822352839222377L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if (isLast) {
                    throw new PropagationException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });
        propagator.setMasterMode(new PatriusStepHandler(){
            private static final long serialVersionUID = 8183822352839222377L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if (isLast) {
                    throw new PropagationException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-3600));

    }

    @Test
    public void ascendingNode() throws PatriusException {
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(true, 0.1,
            0.2, 0.4, 0.1)));

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, bodyCenterPointing, inertial);
        propagator.addEventDetector(new NodeDetector(orbit, FramesFactory.getITRF(), 0));
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        final PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF());
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 4000.0);
        Assert.assertEquals(0, pv.getPosition().getZ(), 2.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ() > 0);
    }

    @Test
    public void stopAtTargetDate() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new NodeDetector(orbit, FramesFactory.getITRF(), 2){
            private static final long serialVersionUID = -1486037976198573520L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        });
        final AbsoluteDate farTarget = orbit.getDate().shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0.0, MathLib.abs(farTarget.durationFrom(propagated.getDate())), 1.0e-3);
    }

    @Test
    public void testRemoveDetector() throws PatriusException {
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), initDate, 3.986004415e14);
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double dt = 1000;
        // this date should always be detected
        final AbsoluteDate d1 = initDate.shiftedBy(dt);
        // this date should not be detected if eventOccured returns REMOVE_DETECTOR
        // this date should be detected if eventOccured returns CONTINUE
        final AbsoluteDate d2 = initDate.shiftedBy(dt * 2.0);
        final MyDoubleDateDetector detectorA = new MyDoubleDateDetector(d1, d2, Action.CONTINUE, false);
        final MyDoubleDateDetector detectorB = new MyDoubleDateDetector(d1, d2, Action.CONTINUE, true);
        propagator.addEventDetector(detectorA);
        final double dt_propagation = 3200;
        propagator.propagate(initDate.shiftedBy(dt_propagation));
        Assert.assertEquals(2, detectorA.getCount());
        propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(detectorB);
        propagator.propagate(initDate.shiftedBy(dt_propagation));
        Assert.assertEquals(1, detectorB.getCount());
    }

    @Test
    public void testRemoveDetectorMultiplePropagation() throws PatriusException {

        // Initialization
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(5), MathLib.toRadians(3),
            MathLib.toRadians(2), MathLib.toRadians(1), PositionAngle.TRUE, FramesFactory.getGCRF(),
            initDate, Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

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

        // Second propagation: no detection expected
        propagator.propagate(initDate.shiftedBy(3600 * 24));
    }

    @Test
    public void perigee() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.addEventDetector(new ApsideDetector(orbit, ApsideDetector.PERIGEE));
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        final PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF());
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
        Assert.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pv.getPosition().getNorm(), 1.0e-6);
    }

    @Test
    public void altitude() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final BodyShape bodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF());
        final AltitudeDetector detector =
            new AltitudeDetector(1500000,
                bodyShape, 0.05 * orbit.getKeplerianPeriod());
        Assert.assertEquals(1500000, detector.getAltitude(), 1.0e-12);
        propagator.addEventDetector(detector);
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 5400.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 5500.0);
        final GeodeticPoint gp = bodyShape.transform(propagated.getPVCoordinates().getPosition(),
            propagated.getFrame(), propagated.getDate());
        Assert.assertEquals(1500000, gp.getAltitude(), 0.1);
    }

    @Test
    public void date() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit, 3.986004415e14);
        final AbsoluteDate stopDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }

    @Test
    public void setting() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF());
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        propagator.addEventDetector(new ElevationDetector(0.09, topo, 60));
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition(),
            propagated.getFrame(),
            propagated.getDate());
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 7900.0);
        Assert.assertEquals(0.09, elevation, 1.0e-9);
        Assert.assertTrue(zVelocity < 0);
    }

    @Test
    public void fixedStep() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = 100.0;
        propagator.setMasterMode(step, new PatriusFixedStepHandler(){
            private static final long serialVersionUID = 5343978335581094125L;
            private AbsoluteDate previous;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                if (this.previous != null) {
                    Assert.assertEquals(step, currentState.getDate().durationFrom(this.previous), 1.0e-10);
                }
                this.previous = currentState.getDate();
            }
        });
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
        Assert.assertEquals(propagator.getMode(), Propagator.MASTER_MODE);
    }

    @Test
    public void variableStep() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = orbit.getKeplerianPeriod() / 100;
        propagator.setMasterMode(new PatriusStepHandler(){
            private static final long serialVersionUID = -7257691813065811595L;
            private AbsoluteDate previous;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if ((this.previous != null) && !isLast) {
                    Assert.assertEquals(step, interpolator.getCurrentDate().durationFrom(this.previous), 1.0e-10);
                }
                this.previous = interpolator.getCurrentDate();
            }
        });
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    /**
     * Check that a KeplerianPropagator works in variable master mode with an hyperbolic trajectory.
     */
    @Test
    public void variableStepHyperbolic() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(-7.8e6, 1.1, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final double step = 2.0 * MathLib.PI / 100 / orbit.getKeplerianMeanMotion();
        propagator.setMasterMode(new PatriusStepHandler(){
            private static final long serialVersionUID = -7257691813065811595L;
            private AbsoluteDate previous;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if ((this.previous != null) && !isLast) {
                    Assert.assertEquals(step, interpolator.getCurrentDate().durationFrom(this.previous), 1.0e-10);
                }
                this.previous = interpolator.getCurrentDate();
            }
        });
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(1000.0);
        propagator.propagate(farTarget);
    }

    @Test
    public void ephemeris() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        propagator.setEphemerisMode();
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        propagator.setEphemerisMode();
        propagator.propagate(farTarget);
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        Assert.assertEquals(0.0, ephemeris.getMinDate().durationFrom(orbit.getDate()), 1.0e10);
        Assert.assertEquals(0.0, ephemeris.getMaxDate().durationFrom(farTarget), 1.0e10);

        propagator.propagate(AbsoluteDate.J2000_EPOCH);
        ephemeris = propagator.getGeneratedEphemeris();
        ephemeris.resetInitialState(new SpacecraftState(orbit));
        Assert.assertEquals(orbit.getDate(), ephemeris.getInitialState().getDate());
    }

    @Test
    public void testIssue14() throws PatriusException {
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit initialOrbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), initialDate, 3.986004415e14);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        final PVCoordinates pv1 = propagator.getPVCoordinates(initialDate, FramesFactory.getEME2000());

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        final PVCoordinates pv2 = propagator.getGeneratedEphemeris()
            .getPVCoordinates(initialDate, FramesFactory.getEME2000());

        // Check PV1 = PV2
        Assert.assertEquals(0.0, pv1.getPosition().subtract(pv2.getPosition()).getNorm() / pv1.getPosition().getNorm(),
            1.0e-15);
        Assert.assertEquals(0.0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm() / pv1.getVelocity().getNorm(),
            1.0e-15);
    }

    /**
     * FT 289.
     * 
     * @throws PropagationException
     *         error raised by KeplerianPropagator constructor
     */
    @Test
    public void testGetSetTWOAttitudeProvider() throws PropagationException {
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(true, 0.1,
            0.2, 0.4, 0.1)));

        // Extrapolator definition
        // -----------------------
        final KeplerianPropagator extrapolator = new KeplerianPropagator(this.initialOrbitPV);
        extrapolator.setAttitudeProviderForces(bodyCenterPointing);
        Assert.assertEquals(bodyCenterPointing, extrapolator.getAttitudeProviderForces());
        Assert.assertEquals(bodyCenterPointing, extrapolator.getAttitudeProvider());
        extrapolator.setAttitudeProviderEvents(inertial);
        Assert.assertEquals(inertial, extrapolator.getAttitudeProviderEvents());

        boolean testOk = false;
        // The test should fail because a two attitudes treatment is expected
        try {
            extrapolator.setAttitudeProvider(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    /**
     * FT 289.
     * 
     * @throws PropagationException
     *         error raised by KeplerianPropagator constructor
     */
    @Test
    public void testGetSetONEAttitudeProvider() throws PropagationException {
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());

        // Extrapolator definition
        // -----------------------
        final KeplerianPropagator extrapolator = new KeplerianPropagator(this.initialOrbitPV);
        extrapolator.setAttitudeProvider(bodyCenterPointing);
        Assert.assertEquals(bodyCenterPointing, extrapolator.getAttitudeProvider());

        boolean testOk = false;
        // The test should fail because a single attitude treatment is expected
        try {
            extrapolator.setAttitudeProviderForces(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // The test should fail because a single attitude treatment is expected
        try {
            extrapolator.setAttitudeProviderEvents(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    private static double tangLEmLv(final double Lv, final double ex, final double ey) {
        // tan ((LE - Lv) /2)) =
        return (ey * MathLib.cos(Lv) - ex * MathLib.sin(Lv)) /
            (1 + ex * MathLib.cos(Lv) + ey * MathLib.sin(Lv) + MathLib.sqrt(1 - ex * ex - ey * ey));
    }

    /**
     * Test FT-478: getGeneratedEphemeris() must return a proper ephemeris.
     */
    @Test
    public void testGetGeneratedEphemeris() throws PatriusException, ParseException, IOException {
        // FT-478
        final Orbit orb = new KeplerianOrbit(7200000.0, 0.01, 0.0, 0.0, 0.0, 0.0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
        final Propagator kepProp = new KeplerianPropagator(orb);
        kepProp.setEphemerisMode();
        final BoundedPropagator ephem = kepProp.getGeneratedEphemeris();
        ephem.getFrame();
        Assert.assertTrue(true);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link AbstractPropagator#setOrbitFrame(Frame)}
     * 
     * @description This test aims at verifying that an exception is raised if a non
     *              pseudo-inertial or non inertial frame is provided for propagation
     * 
     * @input KeplerianOrbit(8.0E6, 0.0, 0.0, 0.0, 0.0, 0.0)
     * @input KeplerianPropagator
     * @input Frame provided for propagation : TIRF
     * 
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testSetNonInertialFrame() throws PatriusException {

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(),
            initDate, Constants.EGM96_EARTH_MU);

        // Propagator
        final KeplerianPropagator prop = new KeplerianPropagator(orbit);
        final Frame from = FramesFactory.getTIRF();

        // An exception should occur here !
        prop.setOrbitFrame(from);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link AbstractPropagator#propagate(AbsoluteDate, AbsoluteDate)}
     * 
     * @description This test aims at verifying that an exception is risen if the non
     *              pseudo-inertial or non inertial orbit's frame is used for propagation
     * 
     * @input KeplerianOrbit(8.0E6, 0.0, 0.0, 0.0, 0.0, 0.0)
     * @input KeplerianPropagator
     * @input Frame ITRF is used to define the orbit and then for propagation
     * 
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testPropagateWithNonInertialFrame() throws PatriusException {

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getITRF(), initDate, Constants.EGM96_EARTH_MU);
        final AbsoluteDate t0 = orbit.getDate().shiftedBy(1000.0);

        // Propagator
        final KeplerianPropagator prop = new KeplerianPropagator(orbit);

        prop.propagate(t0);
    }

    /**
     * @throws PatriusException
     * @testType VT
     * 
     * @testedMethod {@link AbstractPropagator#propagate(AbsoluteDate)}
     * 
     * @description This test is up to ensure that the propagation of an orbit in a
     *              different frame from the one in which the orbit is defined (not
     *              necessary inertial or pseudo-inertial) provide the same final state.
     * 
     * @input KeplerianOrbit(7350036.7731690155, 0.01906887725038333, 1.711148611453987, -1.9818436549702414,
     *        -2.0071033459669896, 0.002140087127289445) in ITRF
     * @input KeplerianPropagator
     * @input Frame ITRF is used to define the orbit
     *        propagation is done in frame EME2000
     * 
     * @output The SpacecraftState final state of propagation
     * @testPassCriteria The orbital elements of the output orbit must be the same as the one expected
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void propagatedKeplerianInTIRF() throws PatriusException {

        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);

        // Frames used to define the orbit and for propagation
        final Frame orbitFrame = FramesFactory.getTIRF();
        final Frame propFrame = FramesFactory.getEME2000();

        final Orbit initialOrbit = new KeplerianOrbit(7350036.7731690155, 0.01906887725038333, 1.711148611453987,
            -1.9818436549702414, -2.0071033459669896,
            0.002140087127289445, PositionAngle.TRUE, orbitFrame, initDate, this.mu);

        final OrbitType type = initialOrbit.getType();

        // Extrapolator definition
        // -----------------------
        final KeplerianPropagator extrapolator = new KeplerianPropagator(initialOrbit);
        Assert.assertTrue(extrapolator.getFrame() == null);

        // Set Frame to EME2000 for propagation
        extrapolator.setOrbitFrame(propFrame);
        Assert.assertTrue(extrapolator.getFrame() == propFrame);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        final double delta_t = 100000.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);
        Assert.assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod(), 1.0e-12);
        Assert.assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion(), 1.0e-16);;
        
        // computation of (M final - M initial) with another method
        final double a = finalOrbit.getA();
        // another way to compute n
        final double n = MathLib.sqrt(finalOrbit.getMu() / MathLib.pow(a, 3));

        // Compute the converted initial orbit in Frame EME2000 in order to allow comparisons
        final Orbit convInitialOrbit = type.convertOrbit(initialOrbit, propFrame);

        Assert.assertEquals(n * delta_t,
            finalOrbit.getLM() - convInitialOrbit.getLM(),
            Utils.epsilonAngle);

        // computation of M final orbit
        final double LM = finalOrbit.getLE()
            - finalOrbit.getEquinoctialEx() * MathLib.sin(finalOrbit.getLE())
            + finalOrbit.getEquinoctialEy() * MathLib.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(MathLib.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()),
            Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        final double deltaM = finalOrbit.getLM() - convInitialOrbit.getLM();
        final double deltaE = finalOrbit.getLE() - convInitialOrbit.getLE();
        final double delta = finalOrbit.getEquinoctialEx() * (MathLib.sin(finalOrbit.getLE()) -
            MathLib.sin(convInitialOrbit.getLE())) - finalOrbit.getEquinoctialEy() *
            (MathLib.cos(finalOrbit.getLE()) - MathLib.cos(convInitialOrbit.getLE()));

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA(), convInitialOrbit.getA(), Utils.epsilonTest * convInitialOrbit.getA());
        Assert.assertEquals(finalOrbit.getEquinoctialEx(), convInitialOrbit.getEquinoctialEx(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy(), convInitialOrbit.getEquinoctialEy(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx(), convInitialOrbit.getHx(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy(), convInitialOrbit.getHy(), Utils.epsilonAngle);

        // for final orbit
        final double ex = finalOrbit.getEquinoctialEx();
        final double ey = finalOrbit.getEquinoctialEy();
        final double hx = finalOrbit.getHx();
        final double hy = finalOrbit.getHy();
        final double LE = finalOrbit.getLE();

        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double hx2 = hx * hx;
        final double hy2 = hy * hy;
        final double h2p1 = 1. + hx2 + hy2;
        final double beta = 1. / (1. + MathLib.sqrt(1. - ex2 - ey2));

        final double x3 = -ex + (1. - beta * ey2) * MathLib.cos(LE) + beta * ex * ey * MathLib.sin(LE);
        final double y3 = -ey + (1. - beta * ex2) * MathLib.sin(LE) + beta * ex * ey * MathLib.cos(LE);

        final Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1,
            (2. * hx * hy) / h2p1,
            (-2. * hy) / h2p1);

        final Vector3D V = new Vector3D((2. * hx * hy) / h2p1,
            (1. - hx2 + hy2) / h2p1,
            (2. * hx) / h2p1);

        final Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
            Utils.epsilonTest * r.getNorm());
        
        // check the method getSpacecraftState
        final SpacecraftState finalOrbit2 = extrapolator.getSpacecraftState(extrapDate);
        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), 
                finalOrbit2.getPVCoordinates().getPosition().getNorm(), Utils.epsilonTest);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link AbstractPropagator# setEphemerisMode()},
     *               {@link AbstractPropagator# setAttitudeProvider(AttitudeProvider)}
     * 
     * @description This test shows that no exception is raised in the case where the
     *              propagator is set in ephemeris mode without having fed before with an attitude (it is fed after).
     *              The rotation of the output spacecraft obtained is accessed after the propagation.
     * 
     * @input a propagator, an orbit, an attitude law
     * 
     * @output none
     * @testPassCriteria no exception should be raised when the rotations are obtained
     *                   with getRotation().
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testSetEphemerisMode() throws PatriusException {

        // GCRF, ITRF frame
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // Constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRIM5C1_EARTH_FLATTENING;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // The earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

        // Initial orbit
        final double a = ae + 400.e3;
        final double e = .001;
        final double i = MathLib.toRadians(116.6);
        final double pa = 0;
        final double raan = MathLib.toRadians(75);
        final double w = 0;

        // Initial date
        final AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(
            2010, 6, 4), new TimeComponents(12, 0, 0.),
            TimeScalesFactory.getTAI());

        // Initial orbit
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE,
            gcrf, initialDate, mu);

        // Final date : propagation on a day
        final AbsoluteDate endDate = initialDate.shiftedBy(86400);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        // Attitude law : pointing earth center
        final BodyCenterGroundPointing law = new BodyCenterGroundPointing(earth);

        // Set ephemeris mode : no attitude law provided yet !
        propagator.setEphemerisMode();

        // Set attitude law
        propagator.setAttitudeProvider(law);

        // propagation until "endDate"
        final SpacecraftState finalState = propagator.propagate(endDate);

        // Get generated ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // Get rotation at final date
        try {
            ephemeris.getAttitudeProvider().getAttitude(finalState.getOrbit()).getRotation();
        } catch (final NullPointerException exception) {
            // Should never happend !
            Assert.fail();
        }
    }

    /**
     * Test identical to testSetEphemerisMode with attitude force and attitude events.
     * {@link AnalyticalEphemerisModeHandler#setAttitudeProviderEvents(AttitudeProvider)}
     * {@link AbstractPropagator#setAttitudeProviderForces(AttitudeProvider)}
     * {@link AbstractPropagator#setAttitudeProviderEvents(AttitudeProvider)}
     */
    @Test
    public void testSetAttitudeProvider() throws PatriusException {

        // ITRF frame
        final Frame itrf = FramesFactory.getITRF();

        // Constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRIM5C1_EARTH_FLATTENING;

        // The earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(this.initialOrbitPV);

        // Attitude law : pointing earth center
        final BodyCenterGroundPointing law = new BodyCenterGroundPointing(earth);

        // Set ephemeris mode : no attitude law provided yet !
        propagator.setEphemerisMode();

        // Set attitude law for forces and events
        propagator.setAttitudeProviderForces(law);
        propagator.setAttitudeProviderEvents(law);

        // propagation until "endDate"
        final SpacecraftState finalState = propagator.propagate(this.initialOrbitPV.getDate().shiftedBy(86400));

        // Get generated ephemeris
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        // Get rotation at final date
        try {
            ephemeris.getAttitudeProviderEvents().getAttitude(finalState.getOrbit())
                .getRotation();
            ephemeris.getAttitudeProviderForces().getAttitude(finalState.getOrbit())
                .getRotation();
        } catch (final NullPointerException exception) {
            // Should never happen !
            Assert.fail();
        }
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.clear();
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.mu = 3.9860047e14;
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        this.initDatePV = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        this.initialOrbitPV = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.initDatePV, this.mu);
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        this.mu = Double.NaN;
    }

    /**
     * Custom event detector.
     * This date detector should detect two dates.
     * The returned Action of eventOccured method is entered as a parameter of the constructor.
     * If action = Action.CONTINUE : eventOccurred should be called two times.
     */
    class MyDoubleDateDetector extends DateDetector {

        private int count = 0;
        private final Action action;

        public MyDoubleDateDetector(final AbsoluteDate target1, final AbsoluteDate target2, final Action action,
            final boolean remove) {
            super(target1, 10., 10.e-10, action, remove);
            this.action = action;
            this.addEventDate(target2);
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            this.count++;
            return this.action;
        }

        public int getCount() {
            return this.count;
        }
    }

}
