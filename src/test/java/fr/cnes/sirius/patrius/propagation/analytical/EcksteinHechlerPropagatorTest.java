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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* VERSION:4.7:DM:DM-2728:18/05/2021:implementation default de AttitudeProvider#getAttitude(Orbit) 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:163:12/03/2014:Introduced a public computeMeanOrbit method
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::FA:416:12/02/2015:Changed EcksteinHechlerPropagator constructor signature
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:533:26/02/2016:add body frame to Eckstein-Heschler propagator
 * VERSION::FA:568:26/02/2016:add setter on convergence threshold
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:665:28/07/2016:forbid non-inertial frames
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::FA:1417:13/03/2018:correct 1 attitude case
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

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
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.gravity.CunninghamAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.ElevationDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class EcksteinHechlerPropagatorTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EcksteinHechlerPropagatorTest.class.getSimpleName(), "Eckstein-Hechler propagator");
    }

    @Test
    public void testMultipleMuValues() throws PatriusException {

        // DM368 : Eckstein-Heschler : Back at the "mu"
        // earth mu and 1% decreased value
        final double mu1 = Constants.JPL_SSD_EARTH_GM;
        final double mu2 = Constants.JPL_SSD_EARTH_GM * .99;

        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, this.mu);

        final EcksteinHechlerPropagator propagator1 =
            new EcksteinHechlerPropagator(initialOrbit, this.ae, mu1, initialOrbit.getFrame(), this.c20, this.c30,
                this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        final EcksteinHechlerPropagator propagator2 =
            new EcksteinHechlerPropagator(initialOrbit, this.ae, mu2, initialOrbit.getFrame(), this.c20, this.c30,
                this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        final SpacecraftState finalOrbit1 = propagator1.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
        final SpacecraftState finalOrbit2 = propagator2.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));

        Assert.assertTrue(MathLib.abs(finalOrbit1.getA() - finalOrbit2.getA()) > 0);
        Assert.assertTrue(MathLib.abs(finalOrbit1.getEquinoctialEx() - finalOrbit2.getEquinoctialEx()) > 0);
        Assert.assertTrue(MathLib.abs(finalOrbit1.getEquinoctialEy() - finalOrbit2.getEquinoctialEy()) > 0);
        Assert.assertTrue(MathLib.abs(finalOrbit1.getI() - finalOrbit2.getI()) > 0);
        Assert.assertTrue(MathLib.abs(finalOrbit1.getLM() - finalOrbit2.getLM()) > 0);

    }

    @Test
    public void sameDateCartesian() throws PatriusException {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, this.mu);

        // Extrapolator definition
        // -----------------------
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                this.ae, this.mu, initialOrbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        // positions match perfectly
        Assert.assertEquals(0.0,
            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                finalOrbit.getPVCoordinates().getPosition()),
            2E-8);

        // velocity match perfectly
        Assert.assertEquals(0.0,
            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                finalOrbit.getPVCoordinates().getVelocity()),
            2E-11);
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
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                this.ae, this.mu, initialOrbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        // positions match perfectly
        Assert.assertEquals(0.0,
            Vector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                finalOrbit.getPVCoordinates().getPosition()),
            3.0e-8);

        // velocity match perfectly
        Assert.assertEquals(0.0,
            Vector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                finalOrbit.getPVCoordinates().getVelocity()),
            2.0e-11);

    }

    @Test
    public void almostSphericalBody() throws PatriusException {

        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, this.mu);

        // Initialisation to simulate a keplerian extrapolation
        // To be noticed: in order to simulate a keplerian extrapolation with the
        // analytical
        // extrapolator, one should put the zonal coefficients to 0. But due to
        // numerical pbs
        // one must put a non 0 value.
        final double zc20 = 0.1e-10;
        final double zc30 = 0.1e-13;
        final double zc40 = 0.1e-13;
        final double zc50 = 0.1e-14;
        final double zc60 = 0.1e-14;

        // Extrapolators definitions
        // -------------------------
        final EcksteinHechlerPropagator extrapolatorAna =
            new EcksteinHechlerPropagator(initialOrbit,
                this.ae, this.mu, initialOrbit.getFrame(), zc20, zc30, zc40, zc50, zc60, ParametersType.OSCULATING);
        final KeplerianPropagator extrapolatorKep = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        final double delta_t = 100.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbitAna = extrapolatorAna.propagate(extrapDate);
        final SpacecraftState finalOrbitKep = extrapolatorKep.propagate(extrapDate);

        Assert.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate), 0.0,
            Utils.epsilonTest);
        // comparison of each orbital parameters
        Assert.assertEquals(finalOrbitAna.getA(), finalOrbitKep.getA(), 10
            * Utils.epsilonTest * finalOrbitKep.getA());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEx(), finalOrbitKep.getEquinoctialEx(), Utils.epsilonE
            * finalOrbitKep.getE());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEy(), finalOrbitKep.getEquinoctialEy(), Utils.epsilonE
            * finalOrbitKep.getE());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx(), finalOrbitKep.getHx()),
            finalOrbitKep.getHx(), Utils.epsilonAngle
                * MathLib.abs(finalOrbitKep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy(), finalOrbitKep.getHy()),
            finalOrbitKep.getHy(), Utils.epsilonAngle
                * MathLib.abs(finalOrbitKep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv(), finalOrbitKep.getLv()),
            finalOrbitKep.getLv(), Utils.epsilonAngle
                * MathLib.abs(finalOrbitKep.getLv()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE(), finalOrbitKep.getLE()),
            finalOrbitKep.getLE(), Utils.epsilonAngle
                * MathLib.abs(finalOrbitKep.getLE()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM(), finalOrbitKep.getLM()),
            finalOrbitKep.getLM(), Utils.epsilonAngle
                * MathLib.abs(finalOrbitKep.getLM()));

    }

    /**
     * This test was added to ascertain the correctness of the public mean computation method.
     * In order to ensure this method is disconnected from the internal propagation state, a random orbit
     * is given to the propagation at instantiation time. Then, an orbit taken from another test is used
     * and the mean parameters obtained are compared to the mean parameters contained in the internal state
     * of the propagator. The propagator internal state does not influence the path taken by the
     * computeMeanOrbit method.
     * 
     * @throws PatriusException
     */
    @Test
    public void propagatedMeanComputation() throws PatriusException {

        // Initialize an extrapolator with a correct geo model but a random orbit
        // ----------------------------------------------------------------------
        final AbsoluteDate initDate1 = AbsoluteDate.J2000_EPOCH.shiftedBy(-584.);
        final Orbit initialOrbit1 = new KeplerianOrbit(6371e3 + 400e3, .0001, .5, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate1, this.mu);

        // Extrapolator definition
        // -----------------------
        final MassProvider massModel = new SimpleMassModel(1000., "DEFAULT_MASS");
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit1, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
                Rotation.IDENTITY), this.ae,
                this.mu, initialOrbit1.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60, massModel,
                ParametersType.OSCULATING);

        // The osculating orbit
        // --------------------
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getGCRF(), initDate, this.mu);

        // The computed mean orbit
        // ----------------------
        final Orbit result = extrapolator.osc2mean(initialOrbit);

        // expected pvs
        // ------------
        final double x = 3218979.3216425637;
        final double y = 68910.43928574433;
        final double z = 6446556.933735931;
        final double vx = 6418.837400364468;
        final double vy = -2008.0300764221574;
        final double vz = -3185.4633417343794;

        final Vector3D p = result.getPVCoordinates().getPosition();
        final Vector3D v = result.getPVCoordinates().getVelocity();

        final double xr = p.getX();
        final double yr = p.getY();
        final double zr = p.getZ();
        final double vxr = v.getX();
        final double vyr = v.getY();
        final double vzr = v.getZ();

        Assert.assertEquals(0, (xr - x) / x, 1e-14);
        Assert.assertEquals(0, (yr - y) / y, 1e-14);
        Assert.assertEquals(0, (zr - z) / z, 1e-14);
        Assert.assertEquals(0, (vxr - vx) / vx, 1e-14);
        Assert.assertEquals(0, (vyr - vy) / vy, 1e-14);
        Assert.assertEquals(0, (vzr - vz) / vz, 1e-14);
    }

    /**
     * An additional test is added here to ensure that the computeMeanOrbit method
     * does not corrupt the internal state of the Eckstein Hechler propagator instance.
     * A random orbits mean parameters are computed with a propagator that is used
     * throughout the test otherwise.
     * 
     * @throws PatriusException
     */
    @Test
    public void propagatedCartesian() throws PatriusException {
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, this.mu);

        // Extrapolator definition
        // -----------------------
        final MassProvider massModel = new SimpleMassModel(1000., "DEFAULT_MASS");
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
                Rotation.IDENTITY), this.ae,
                this.mu, initialOrbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60, massModel,
                ParametersType.OSCULATING);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        final double delta_t = 100000.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        // ADDITIONAL TEST
        // Added intermediate computation of mean parameters to assess
        // the correctness of the propagator state
        // -----------------------------------------------------------
        final AbsoluteDate initDate1 = AbsoluteDate.J2000_EPOCH.shiftedBy(-584.);
        new KeplerianOrbit(6371e3 + 400e3, .0001, .5, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initDate1, this.mu);
        finalOrbit = extrapolator.propagate(extrapDate.shiftedBy(-10));

        // recompute expected final orbit
        // ------------------------------
        finalOrbit = extrapolator.propagate(extrapDate);

        Assert.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate), 1.0e-9);

        // computation of M final orbit
        final double LM = finalOrbit.getLE() - finalOrbit.getEquinoctialEx()
            * MathLib.sin(finalOrbit.getLE()) + finalOrbit.getEquinoctialEy()
            * MathLib.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle
            * MathLib.abs(finalOrbit.getLM()));

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(MathLib.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                .getEquinoctialEy()), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        final double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        final double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        final double delta = finalOrbit.getEquinoctialEx() * MathLib.sin(finalOrbit.getLE())
            - initialOrbit.getEquinoctialEx() * MathLib.sin(initialOrbit.getLE())
            - finalOrbit.getEquinoctialEy() * MathLib.cos(finalOrbit.getLE())
            + initialOrbit.getEquinoctialEy() * MathLib.cos(initialOrbit.getLE());

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle
            * MathLib.abs(deltaE - delta));

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

        final double x3 = -ex + (1. - beta * ey2) * MathLib.cos(LE) + beta * ex * ey
            * MathLib.sin(LE);
        final double y3 = -ey + (1. - beta * ex2) * MathLib.sin(LE) + beta * ex * ey
            * MathLib.cos(LE);

        final Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1, (2. * hx * hy) / h2p1,
            (-2. * hy) / h2p1);

        final Vector3D V = new Vector3D((2. * hx * hy) / h2p1, (1. - hx2 + hy2) / h2p1,
            (2. * hx) / h2p1);

        final Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
            Utils.epsilonTest * r.getNorm());

    }

    @Test
    public void propagatedKeplerian() throws PatriusException {
        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
            6.2, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, this.mu);

        // Extrapolator definition
        // -----------------------
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                this.ae, this.mu, initialOrbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        final double delta_t = 100000.0; // extrapolation duration in seconds
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        final SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        Assert.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate), 1.0e-9);

        // computation of M final orbit
        final double LM = finalOrbit.getLE() - finalOrbit.getEquinoctialEx()
            * MathLib.sin(finalOrbit.getLE()) + finalOrbit.getEquinoctialEy()
            * MathLib.cos(finalOrbit.getLE());

        Assert.assertEquals(LM, finalOrbit.getLM(), Utils.epsilonAngle);

        // test of tan((LE - Lv)/2) :
        Assert.assertEquals(MathLib.tan((finalOrbit.getLE() - finalOrbit.getLv()) / 2.),
            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                .getEquinoctialEy()), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        final double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        final double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        final double delta = finalOrbit.getEquinoctialEx() * MathLib.sin(finalOrbit.getLE())
            - initialOrbit.getEquinoctialEx() * MathLib.sin(initialOrbit.getLE())
            - finalOrbit.getEquinoctialEy() * MathLib.cos(finalOrbit.getLE())
            + initialOrbit.getEquinoctialEy() * MathLib.cos(initialOrbit.getLE());

        Assert.assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle
            * MathLib.abs(deltaE - delta));

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

        final double x3 = -ex + (1. - beta * ey2) * MathLib.cos(LE) + beta * ex * ey
            * MathLib.sin(LE);
        final double y3 = -ey + (1. - beta * ex2) * MathLib.sin(LE) + beta * ex * ey
            * MathLib.cos(LE);

        final Vector3D U = new Vector3D((1. + hx2 - hy2) / h2p1, (2. * hx * hy) / h2p1,
            (-2. * hy) / h2p1);

        final Vector3D V = new Vector3D((2. * hx * hy) / h2p1, (1. - hx2 + hy2) / h2p1,
            (2. * hx) / h2p1);

        final Vector3D r = new Vector3D(finalOrbit.getA(), (new Vector3D(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(),
            Utils.epsilonTest * r.getNorm());

    }

    @Test(expected = PropagationException.class)
    public void undergroundOrbit() throws PropagationException {

        // for a semi major axis < equatorial radius
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 800.0, 100.0);
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, this.mu);
        // Extrapolator definition
        // -----------------------
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                this.ae, this.mu, initialOrbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0;
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }

    @Test(expected = PropagationException.class)
    public void tooEllipticalOrbit() throws PropagationException {
        // for an eccentricity too big for the model
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), initDate, this.mu);
        // Extrapolator definition
        // -----------------------
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit,
                this.ae, this.mu, initialOrbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0;
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hyperbolic() throws PropagationException {
        final KeplerianOrbit hyperbolic =
            new KeplerianOrbit(-1.0e10, 2, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(hyperbolic, this.ae, this.mu, hyperbolic.getFrame(), this.c20, this.c30,
                this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
    }

    @Test(expected = PropagationException.class)
    public void criticalInclination() throws PatriusException {
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new CircularOrbit(new PVCoordinates(new Vector3D(-3862363.8474653554,
            -3521533.9758022362,
            4647637.852558916),
            new Vector3D(65.36170817232278,
                -6056.563439401233,
                -4511.1247889782757)),
            FramesFactory.getEME2000(),
            initDate, this.mu);

        // Extrapolator definition
        // -----------------------
        final EcksteinHechlerPropagator extrapolator =
            new EcksteinHechlerPropagator(initialOrbit, this.ae, this.mu, initialOrbit.getFrame(), this.c20, this.c30,
                this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);

        // Extrapolation at the initial date
        // ---------------------------------
        final double delta_t = 0.0;
        final AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);
        extrapolator.propagate(extrapDate);
    }

    @Test
    public void equatorialOrbitOK() throws PropagationException {
        final KeplerianOrbit equa =
            new KeplerianOrbit(1.0e10, 0, 1.0e-9, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(equa, this.ae, this.mu, equa.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        final SpacecraftState finalOrbit = propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
        final CircularOrbit ofinalOrbit = new CircularOrbit(finalOrbit.getOrbit());

        Assert.assertEquals(10.0, ofinalOrbit.getDate().durationFrom(AbsoluteDate.J2000_EPOCH), Utils.epsilonTest);
        Assert.assertEquals(ofinalOrbit.getA(), 1.0e10, Utils.epsilonTest * ofinalOrbit.getA());
        Assert.assertEquals(ofinalOrbit.getCircularEx(), 0.0, Utils.epsilonE);
        Assert.assertEquals(ofinalOrbit.getCircularEy(), 0, Utils.epsilonE);
        Assert.assertEquals(ofinalOrbit.getI(), 0, Utils.epsilonAngle);
        Assert.assertEquals(ofinalOrbit.getRightAscensionOfAscendingNode() % (2. * FastMath.PI), 0, Utils.epsilonAngle);
        Assert.assertEquals(ofinalOrbit.getLM(), 6.283185506829, Utils.epsilonAngle);
    }

    @Test(expected = PropagationException.class)
    public void equatorialOrbitKO1() throws PropagationException {
        final KeplerianOrbit equa =
            new KeplerianOrbit(1.0e10, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(equa, this.ae, this.mu, equa.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));

    }

    @Test(expected = PropagationException.class)
    public void equatorialOrbitKO2() throws PropagationException {
        final KeplerianOrbit equa =
            new KeplerianOrbit(1.0e10, 0, 1.0e-10, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(equa, this.ae, this.mu, equa.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
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
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, wrongLaw, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
                Rotation.IDENTITY),
                this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40, this.c50, this.c60,
                ParametersType.OSCULATING);
        propagator.propagate(AbsoluteDate.J2000_EPOCH.shiftedBy(10.0));
    }

    @Test
    public void ascendingNode() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, this.mu);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        final NodeDetector detector = new NodeDetector(orbit, FramesFactory.getITRF(), 0);
        Assert.assertTrue(FramesFactory.getITRF() == detector.getFrame());
        propagator.addEventDetector(detector);
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        final PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF());
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 4000.0);
        Assert.assertEquals(0, pv.getPosition().getZ(), 1.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ() > 0);
        final Collection<EventDetector> detectors = propagator.getEventsDetectors();
        Assert.assertEquals(1, detectors.size());
        propagator.clearEventsDetectors();
        Assert.assertEquals(0, propagator.getEventsDetectors().size());
    }

    @Test
    public void stopAtTargetDate() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        propagator.addEventDetector(new NodeDetector(orbit, FramesFactory.getITRF(), 2){
            private static final long serialVersionUID = 8805264185199866748L;

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
    public void perigee() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, this.mu);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        propagator.addEventDetector(new ApsideDetector(orbit, ApsideDetector.PERIGEE));
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        final PVCoordinates pv = propagated.getPVCoordinates(FramesFactory.getITRF());
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
        Assert.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pv.getPosition().getNorm(), 400);
    }

    @Test
    public void date() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final MassProvider massModel = new SimpleMassModel(1000., "DEFAULT_MASS");
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60, massModel,
                ParametersType.OSCULATING);
        final AbsoluteDate stopDate = AbsoluteDate.J2000_EPOCH.shiftedBy(500.0);
        propagator.addEventDetector(new DateDetector(stopDate));
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }

    @Test
    public void fixedStep() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
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
    }

    @Test
    public void setting() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final EcksteinHechlerPropagator propagator =
            new EcksteinHechlerPropagator(orbit, this.ae, this.mu, orbit.getFrame(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                ParametersType.OSCULATING);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF());
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        final ElevationDetector detector = new ElevationDetector(0.09, topo, 60, 1.0e-9);
        Assert.assertEquals(0.09, detector.getElevation(), 1.0e-12);
        Assert.assertTrue(topo == detector.getTopocentricFrame());
        propagator.addEventDetector(detector);
        final AbsoluteDate farTarget = AbsoluteDate.J2000_EPOCH.shiftedBy(10000.0);
        final SpacecraftState propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition(),
            propagated.getFrame(),
            propagated.getDate());
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 7900.0);
        Assert.assertEquals(0.09, elevation, 1.0e-11);
        Assert.assertTrue(zVelocity < 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Eckstein-Hechler propagation (nominal case)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagation() throws PatriusException {

        final double eps = 2E-14;

        Report.printMethodHeader("testPropagation", "Propagation", "Celestlab 3.1.0", eps, ComparisonType.RELATIVE);

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new CircularOrbit(7000000, 0., 0.001, 1., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(orbit, 6378136.3, 3.98600442e+14,
            FramesFactory.getCIRF(), -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742,
            -0.000000538219, ParametersType.MEAN);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));
        final CircularOrbit actual = (CircularOrbit) finalState.getOrbit();

        // Check results (Celestlab reference)
        Assert.assertEquals(0., this.relDiff(6998642.12795516569, actual.getA()), eps);
        Assert.assertEquals(0., this.relDiff(-0.00039236065810061, actual.getCircularEx()), eps);
        Assert.assertEquals(0., this.relDiff(0.00099424303447362, actual.getCircularEy()), eps);
        Assert.assertEquals(0., this.relDiff(0.99993775242592675, actual.getI()), eps);
        Assert.assertEquals(0., this.relDiff(0.03183650896649683, actual.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(93.3603771547069101 % (2. * FastMath.PI), actual.getAlphaM()), 2E-13);
        Assert.assertEquals(86400, actual.getDate().durationFrom(date), 0);

        Report.printToReport("a", 6998642.12795516569, actual.getA());
        Report.printToReport("ex", -0.00039236065810061, actual.getCircularEx());
        Report.printToReport("ey", 0.00099424303447362, actual.getCircularEy());
        Report.printToReport("i", 0.99993775242592675, actual.getI());
        Report.printToReport("RAAN", 0.03183650896649683, actual.getRightAscensionOfAscendingNode());
        Report.printToReport("AlphaM", 93.3603771547069101 % (2. * FastMath.PI), actual.getAlphaM());
    }

    /**
     * FA-1417
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Eckstein-Hechler propagation (nominal case) with and without attitude law
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 day propagation
     * 
     * @testPassCriteria orbit is exactly the same with or without attitude
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testPropagationAttitude() throws PatriusException {

        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new CircularOrbit(7000000, 0., 0.001, 1., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(orbit, 6378136.3, 3.98600442e+14,
            FramesFactory.getCIRF(), -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742,
            -0.000000538219, ParametersType.MEAN);
        final AttitudeProvider attitudeLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final EcksteinHechlerPropagator propagator2 =
            new EcksteinHechlerPropagator(orbit, attitudeLaw, 6378136.3, 3.98600442e+14,
                FramesFactory.getCIRF(), -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742,
                -0.000000538219, ParametersType.MEAN);

        // Propagation (with both propagators)
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(86400.));
        final CircularOrbit actual = (CircularOrbit) finalState.getOrbit();

        final SpacecraftState finalState2 = propagator2.propagate(date.shiftedBy(86400.));
        final CircularOrbit actual2 = (CircularOrbit) finalState2.getOrbit();

        // Check results are identical
        Assert.assertEquals(0., this.relDiff(actual2.getA(), actual.getA()), 0);
        Assert.assertEquals(0., this.relDiff(actual2.getCircularEx(), actual.getCircularEx()), 0);
        Assert.assertEquals(0., this.relDiff(actual2.getCircularEy(), actual.getCircularEy()), 0);
        Assert.assertEquals(0., this.relDiff(actual2.getI(), actual.getI()), 0);
        Assert.assertEquals(0.,
            this.relDiff(actual2.getRightAscensionOfAscendingNode(), actual.getRightAscensionOfAscendingNode()), 0);
        Assert.assertEquals(0., this.relDiff(actual2.getAlphaM(), actual.getAlphaM()), 0);
        Assert.assertEquals(86400, actual2.getDate().durationFrom(date), 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagateMeanOrbit(AbsoluteDate)}
     * 
     * @description test the Eckstein-Hechler mean orbit computation
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian mean elements after 1 day propagation
     * 
     * @testPassCriteria orbit is as expected (reference : use of validated osculating propagation and osc <=> mean
     *                   conversion, tolerance: 1E-16)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationMean() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442E14);
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(orbit, 6378000, 3.9860047e14,
            orbit.getFrame(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, -5.5e-7, ParametersType.OSCULATING);

        final double eps = 3E-15;

        // Propagation
        final CircularOrbit actual = (CircularOrbit) propagator.propagateMeanOrbit(date.shiftedBy(0.));

        // Expected
        final CircularOrbit expected = new CircularOrbit(propagator.osc2mean(orbit));

        // Check results (use of validated osculating propagation and osc <=> mean conversion)
        Assert.assertEquals(0., this.relDiff(expected.getA(), actual.getA()), eps);
        Assert.assertEquals(0., this.relDiff(expected.getCircularEx(), actual.getCircularEx()), eps);
        Assert.assertEquals(0., this.relDiff(expected.getCircularEy(), actual.getCircularEy()), eps);
        Assert.assertEquals(0., this.relDiff(expected.getI(), actual.getI()), eps);
        Assert.assertEquals(0.,
            this.relDiff(expected.getRightAscensionOfAscendingNode(), actual.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(expected.getAlphaM(), actual.getAlphaM()), eps);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#mean2osc(Orbit)}
     * @testedMethod {@link EcksteinHechlerPropagator#osc2mean(Orbit)}
     * 
     * @description test the Eckstein-Hechler mean <=> osculating conversion
     * 
     * @input Circular orbit in mean/osculating parameters
     * 
     * @output Circular orbit in osculating/mean parameters
     * 
     * @testPassCriteria orbit is as expected (reference : Celestlab 3.1.0, tolerance: 1E-15)
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testMeanOscConversion() throws PatriusException {

        final double eps = 5E-15;

        Report.printMethodHeader("testMeanOscConversion", "Mean <=> Osculating conversion", "Celestlab 3.1.0", eps,
            ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new CircularOrbit(7000000, 0., 0.001, 1., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(orbit, 6378136.3, 3.98600442e+14,
            FramesFactory.getCIRF(), -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742,
            -0.000000538219, ParametersType.MEAN);

        // Mean to osc conversion
        final CircularOrbit osc = (CircularOrbit) propagator.mean2osc(orbit);

        // Check results (Celestlab reference)
        // Small difference due to FT-489 (Orekit v7)
        Assert.assertEquals(0., this.relDiff(7006166.97380257677, osc.getA()), eps);
        Assert.assertEquals(0., this.relDiff(0.00061325022465691, osc.getCircularEx()), eps);
        Assert.assertEquals(0., this.relDiff(0.00124881004056462, osc.getCircularEy()), eps);
        Assert.assertEquals(0., this.relDiff(1.000282787687367, osc.getI()), eps);
        Assert.assertEquals(0., this.relDiff(0.10014288651414052, osc.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(0.20019742975551447, osc.getAlphaM()), eps);

        Report.printToReport("a", 7006166.97380257677, osc.getA());
        Report.printToReport("ex", 0.00061325022465691, osc.getCircularEx());
        Report.printToReport("ey", 0.00124881004056462, osc.getCircularEy());
        Report.printToReport("i", 1.000282787687367, osc.getI());
        Report.printToReport("RAAN", 0.10014288651414052, osc.getRightAscensionOfAscendingNode());
        Report.printToReport("AlphaM", 0.20019742975551447 % (2. * FastMath.PI), osc.getAlphaM());

        // Osc to mean conversion
        final CircularOrbit mean = (CircularOrbit) propagator.osc2mean(osc);

        // Check results (Celestlab reference)
        Assert.assertEquals(0., this.relDiff(7000000, mean.getA()), eps);
        Assert.assertEquals(0., this.relDiff(0., mean.getCircularEx()), eps);
        Assert.assertEquals(0., this.relDiff(0.001, mean.getCircularEy()), eps);
        Assert.assertEquals(0., this.relDiff(1., mean.getI()), eps);
        Assert.assertEquals(0., this.relDiff(0.1, mean.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(0.2, mean.getAlphaM()), eps);
    }

    /**
     * FA-568
     * 
     * @throws PropagationException
     * @throws PatriusException
     * @testType UT
     * 
     * @description test the Eckstein-Hechler osculating => mean conversion convergence threshold
     * 
     * @input specific instantiation of Eckstein-Hechler propagator
     * 
     * @output Exception
     * 
     * @testPassCriteria Exception thrown if convergence threshold is 1E-13, no exception if convergence threshold is
     *                   1E-12
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testConvergenceThreshold() throws PropagationException {

        // Initialization
        final AbsoluteDate date = new AbsoluteDate(2018, 6, 23, 0, 00, 0.0, TimeScalesFactory.getTAI());
        final Orbit orbitCIRF = new CircularOrbit(7183363.89522035, -8.941808805400402E-5, 8.383863708607163E-4,
            1.7212891584949481,
            -312.18039057983583, 4435.468035494116, PositionAngle.TRUE, FramesFactory.getCIRF(), date,
            3.986004415E14);

        // Propagation with default convergence threshold (1E-13) - Exception is expected
        try {
            new EcksteinHechlerPropagator(orbitCIRF, 6378136.46,
                3.986004415E14, orbitCIRF.getFrame(), -0.0010826264444855536,
                2.532558865422469E-6, 1.61998777956621E-6, 2.2779633055219165E-7, -5.406540965936291E-7,
                ParametersType.OSCULATING);
            Assert.fail();
        } catch (final PropagationException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Change convergence threshold
        EcksteinHechlerPropagator.setThreshold(1E-12);

        // Propagation with new convergence threshold (1E-12) - Propagation is expected to end properly
        try {
            new EcksteinHechlerPropagator(orbitCIRF, 6378136.46,
                3.986004415E14, orbitCIRF.getFrame(), -0.0010826264444855536,
                2.532558865422469E-6, 1.61998777956621E-6, 2.2779633055219165E-7, -5.406540965936291E-7,
                ParametersType.OSCULATING);
            // Expected
            Assert.assertTrue(true);
        } catch (final PropagationException e) {
            Assert.fail();
        }

        // Back to default threshold
        EcksteinHechlerPropagator.setThreshold(1E-13);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Eckstein-Hechler propagation with orbit frame (GCRF) different from body frame (CIRF)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 orbital period
     * 
     * @testPassCriteria orbit is close (< 22m) to orbit obtained with numerical integration including J2 to J6 only
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationBodyFrame() throws PatriusException, IOException, ParseException {

        // Gravity potential
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double ae = provider.getAe();
        final double[][] c = provider.getC(5, 0, false);
        final double[][] s = provider.getS(5, 0, false);
        final double mu = provider.getMu();
        final Frame bodyFrame = FramesFactory.getCIRF();

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(orbit, ae, mu, bodyFrame, c[2][0],
            c[3][0], c[4][0], c[5][0], 0, ParametersType.OSCULATING);

        // Numerical propagator
        final NumericalPropagator numerical = new NumericalPropagator(new DormandPrince853Integrator(0.01, 100, 1E-7,
            1E-12));
        numerical.addForceModel(new CunninghamAttractionModel(bodyFrame, ae, mu, c, s));
        numerical.setInitialState(new SpacecraftState(orbit));

        final double epsP = 2.;
        final double epsV = 0.05;

        // Propagation (actual and expected)
        final Orbit actual = propagator.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();
        final Orbit expected = new CircularOrbit(numerical.propagate(date.shiftedBy(orbit.getKeplerianPeriod()))
            .getOrbit());

        final PVCoordinates actualPV = actual.getPVCoordinates(FramesFactory.getGCRF());
        final PVCoordinates expectedPV = expected.getPVCoordinates(FramesFactory.getGCRF());

        // Check results
        Assert.assertEquals(0, expectedPV.getPosition().distance(actualPV.getPosition()), epsP);
        Assert.assertEquals(0, expectedPV.getVelocity().distance(actualPV.getVelocity()), epsV);
        Assert.assertEquals(actual.getFrame(), FramesFactory.getGCRF());
        Assert.assertEquals(propagator.propagateMeanOrbit(date.shiftedBy(orbit.getKeplerianPeriod())).getFrame(),
            FramesFactory.getGCRF());
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Eckstein-Hechler propagation with orbit frame (GCRF) different from body frame (ITRF)
     * 
     * @input Keplerian orbit
     * 
     * @output exception
     * 
     * @testPassCriteria an exception is thrown
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testPropagationBodyFrameITRF() throws PatriusException, IOException, ParseException {

        try {
            final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
            final double[][] c = provider.getC(5, 0, false);
            final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.001, FastMath.PI / 2., 0.1, 0.2,
                PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.FIFTIES_EPOCH_TAI, provider.getMu());
            new EcksteinHechlerPropagator(orbit, provider.getAe(),
                provider.getMu(), FramesFactory.getITRF(), c[2][0], c[3][0], c[4][0], c[5][0], 0,
                ParametersType.OSCULATING);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test the Eckstein-Hechler propagation with orbit frame (MOD) different from propagation frame (GCRF)
     * 
     * @input Keplerian orbit
     * 
     * @output Keplerian orbit after 1 orbital period
     * 
     * @testPassCriteria orbit obtained with propagation of initial state in MOD is identical to orbit obtained with
     *                   propagation of initial state in GCRF
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationPropagationFrame() throws PatriusException, IOException, ParseException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, 3.98600442e+14);
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(orbit, 6378136.3, 3.98600442e+14,
            FramesFactory.getCIRF(), -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, -5.41e-7,
            ParametersType.OSCULATING);

        // Propagation in GCRF
        final Orbit actual = propagator.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();

        // Propagation in GCRF frame with initial orbit in MOD
        final Orbit orbit2 = orbit.getType().convertOrbit(orbit, FramesFactory.getMOD(false));
        final EcksteinHechlerPropagator propagator2 = new EcksteinHechlerPropagator(orbit2, 6378136.3, 3.98600442e+14,
            FramesFactory.getCIRF(), -0.001082626613, 0.000002532393, 0.000001619137, 0.000000227742, -5.41e-7,
            ParametersType.OSCULATING);
        propagator2.setOrbitFrame(FramesFactory.getGCRF());
        final Orbit actual2 = propagator2.propagate(date.shiftedBy(orbit.getKeplerianPeriod())).getOrbit();

        final double eps2 = 8E-14;

        // Check results
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getPosition().getX(), actual.getPVCoordinates().getPosition()
                .getX()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getPosition().getY(), actual.getPVCoordinates().getPosition()
                .getY()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getPosition().getZ(), actual.getPVCoordinates().getPosition()
                .getZ()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getVelocity().getX(), actual.getPVCoordinates().getVelocity()
                .getX()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getVelocity().getY(), actual.getPVCoordinates().getVelocity()
                .getY()),
            eps2);
        Assert.assertEquals(
            0.,
            this.relDiff(actual2.getPVCoordinates().getVelocity().getZ(), actual.getPVCoordinates().getVelocity()
                .getZ()),
            eps2);
        Assert.assertEquals(actual2.getFrame(), FramesFactory.getGCRF());

        // Complementary test: check reset state
        final Orbit orbit_rs1 = propagator.propagateOrbit(orbit.getDate());
        propagator.resetInitialState(new SpacecraftState(orbit));
        final Orbit orbit_rs2 = propagator.propagateOrbit(orbit.getDate());
        final double eps3 = 2E-15;

        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getPosition().getX(), orbit_rs2.getPVCoordinates().getPosition()
                .getX()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getPosition().getY(), orbit_rs2.getPVCoordinates().getPosition()
                .getY()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getPosition().getZ(), orbit_rs2.getPVCoordinates().getPosition()
                .getZ()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getVelocity().getX(), orbit_rs2.getPVCoordinates().getVelocity()
                .getX()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getVelocity().getY(), orbit_rs2.getPVCoordinates().getVelocity()
                .getY()), eps3);
        Assert.assertEquals(
            0.,
            this.relDiff(orbit_rs1.getPVCoordinates().getVelocity().getZ(), orbit_rs2.getPVCoordinates().getVelocity()
                .getZ()), eps3);
    }

    /**
     * Compute relative difference.
     * 
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @return relative difference
     */
    private double relDiff(final double expected, final double actual) {
        if (expected == 0) {
            return MathLib.abs(expected - actual);
        } else {
            return MathLib.abs((expected - actual) / expected);
        }
    }

    private static double tangLEmLv(final double Lv, final double ex, final double ey) {
        // tan ((LE - Lv) /2)) =
        return (ey * MathLib.cos(Lv) - ex * MathLib.sin(Lv))
            / (1 + ex * MathLib.cos(Lv) + ey * MathLib.sin(Lv) + MathLib.sqrt(1 - ex * ex
                - ey * ey));
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        Utils.setDataRoot("regular-data:potential/shm-format");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.mu = 3.9860047e14;
        this.ae = 6.378137e6;
        this.c20 = -1.08263e-3;
        this.c30 = 2.54e-6;
        this.c40 = 1.62e-6;
        this.c50 = 2.3e-7;
        this.c60 = -5.5e-7;
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

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

}
