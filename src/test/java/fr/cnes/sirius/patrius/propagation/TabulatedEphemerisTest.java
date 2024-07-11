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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::FA:416:12/02/2015:Changed EcksteinHechlerPropagator constructor signature
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class TabulatedEphemerisTest {

    @Test
    public void testInterpolation() throws ParseException, PatriusException {

        final double mu = 3.9860047e14;
        final double mass = 2500;
        final double a = 7187990.1979844316;
        final double e = 0.5e-4;
        final double i = 1.7105407051081795;
        final double omega = 1.9674147913622104;
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());
        final AbsoluteDate finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());
        final double deltaT = finalDate.durationFrom(initDate);

        final Orbit transPar = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            FramesFactory.getEME2000(), initDate, mu);
        final MassProvider massModel = new SimpleMassModel(mass, "DEFAULD_MASS");
        final int nbIntervals = 720;
        final EcksteinHechlerPropagator eck =
            new EcksteinHechlerPropagator(transPar, this.ae, mu, FramesFactory.getCIRF(), this.c20, this.c30, this.c40,
                this.c50, this.c60,
                massModel, ParametersType.OSCULATING);
        eck.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        final List<SpacecraftState> tab = new ArrayList<SpacecraftState>(nbIntervals + 1);
        for (int j = 0; j <= nbIntervals; j++) {
            final AbsoluteDate current = initDate.shiftedBy((j * deltaT) / nbIntervals);
            tab.add(eck.propagate(current));
        }

        final Ephemeris te = new Ephemeris(tab, 2);

        Assert.assertEquals(0.0, te.getMaxDate().durationFrom(finalDate), 1.0e-9);
        Assert.assertEquals(0.0, te.getMinDate().durationFrom(initDate), 1.0e-9);

        this.checkEphemerides(eck, te, initDate.shiftedBy(3600), 1.0e-9, true);
        this.checkEphemerides(eck, te, initDate.shiftedBy(3660), 30, false);
        this.checkEphemerides(eck, te, initDate.shiftedBy(3720), 1.0e-9, true);
    }

    @Test
    public void testPiWraping() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Frame frame = FramesFactory.getEME2000();
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final AbsoluteDate t0 = new AbsoluteDate(2009, 10, 29, 0, 0, 0, utc);

        final AbsoluteDate t1 = new AbsoluteDate(t0, 1320.0);
        final Vector3D p1 = new Vector3D(-0.17831296727974E+08, 0.67919502669856E+06, -0.16591008368477E+07);
        final Vector3D v1 = new Vector3D(-0.38699705630724E+04, -0.36209408682762E+04, -0.16255053872347E+03);
        final Orbit orbit1 = new EquinoctialOrbit(new PVCoordinates(p1, v1), frame, t1, mu);
        final Attitude attitude1 = new LofOffset(orbit1.getFrame(), LOFType.LVLH).getAttitude(orbit1, orbit1.getDate(),
            orbit1.getFrame());
        final SpacecraftState s1 = new SpacecraftState(orbit1, attitude1);

        final AbsoluteDate t2 = new AbsoluteDate(t0, 1440.0);
        final Vector3D p2 = new Vector3D(-0.18286942572033E+08, 0.24442124296930E+06, -0.16777961761695E+07);
        final Vector3D v2 = new Vector3D(-0.37252897467918E+04, -0.36246628128896E+04, -0.14917724596280E+03);
        final Orbit orbit2 = new EquinoctialOrbit(new PVCoordinates(p2, v2), frame, t2, mu);
        final Attitude attitude2 = new LofOffset(orbit2.getFrame(), LOFType.LVLH).getAttitude(orbit2, orbit2.getDate(),
            orbit2.getFrame());
        final SpacecraftState s2 = new SpacecraftState(orbit2, attitude2);

        final AbsoluteDate t3 = new AbsoluteDate(t0, 1560.0);
        final Vector3D p3 = new Vector3D(-0.18725635245837E+08, -0.19058407701834E+06, -0.16949352249614E+07);
        final Vector3D v3 = new Vector3D(-0.35873348682393E+04, -0.36248828501784E+04, -0.13660045394149E+03);
        final Orbit orbit3 = new EquinoctialOrbit(new PVCoordinates(p3, v3), frame, t3, mu);
        final Attitude attitude3 = new LofOffset(orbit3.getFrame(), LOFType.LVLH).getAttitude(orbit3, orbit3.getDate(),
            orbit3.getFrame());
        final SpacecraftState s3 = new SpacecraftState(orbit3, attitude3);

        final Ephemeris ephem = new Ephemeris(Arrays.asList(s1, s2, s3), 2);

        final AbsoluteDate tA = new AbsoluteDate(t0, 24 * 60);
        final Vector3D pA = ephem.propagate(tA).getPVCoordinates(frame).getPosition();
        Assert.assertEquals(
            1.766,
            Vector3D.distance(pA, s1.shiftedBy(tA.durationFrom(s1.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);
        Assert.assertEquals(
            0.000,
            Vector3D.distance(pA, s2.shiftedBy(tA.durationFrom(s2.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);
        Assert.assertEquals(
            1.556,
            Vector3D.distance(pA, s3.shiftedBy(tA.durationFrom(s3.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);

        final AbsoluteDate tB = new AbsoluteDate(t0, 25 * 60);
        final Vector3D pB = ephem.propagate(tB).getPVCoordinates(frame).getPosition();
        Assert.assertEquals(
            2.646,
            Vector3D.distance(pB, s1.shiftedBy(tB.durationFrom(s1.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);
        Assert.assertEquals(
            2.619,
            Vector3D.distance(pB, s2.shiftedBy(tB.durationFrom(s2.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);
        Assert.assertEquals(
            2.632,
            Vector3D.distance(pB, s3.shiftedBy(tB.durationFrom(s3.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);

        final AbsoluteDate tC = new AbsoluteDate(t0, 26 * 60);
        final Vector3D pC = ephem.propagate(tC).getPVCoordinates(frame).getPosition();
        Assert.assertEquals(
            6.851,
            Vector3D.distance(pC, s1.shiftedBy(tC.durationFrom(s1.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);
        Assert.assertEquals(
            1.605,
            Vector3D.distance(pC, s2.shiftedBy(tC.durationFrom(s2.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);
        Assert.assertEquals(
            0.000,
            Vector3D.distance(pC, s3.shiftedBy(tC.durationFrom(s3.getDate())).getPVCoordinates(frame).getPosition()),
            1.0e-3);

    }

    private void checkEphemerides(final Propagator eph1, final Propagator eph2, final AbsoluteDate date,
                                  final double threshold, final boolean expectedBelow)
                                                                                      throws PropagationException {
        final SpacecraftState state1 = eph1.propagate(date);
        final SpacecraftState state2 = eph2.propagate(date);
        double maxError = MathLib.abs(state1.getA() - state2.getA());
        maxError = MathLib.max(maxError, MathLib.abs(state1.getEquinoctialEx() - state2.getEquinoctialEx()));
        maxError = MathLib.max(maxError, MathLib.abs(state1.getEquinoctialEy() - state2.getEquinoctialEy()));
        maxError = MathLib.max(maxError, MathLib.abs(state1.getHx() - state2.getHx()));
        maxError = MathLib.max(maxError, MathLib.abs(state1.getHy() - state2.getHy()));
        maxError = MathLib.max(maxError, MathLib.abs(state1.getLv() - state2.getLv()));
        if (expectedBelow) {
            Assert.assertTrue(maxError <= threshold);
        } else {
            Assert.assertTrue(maxError >= threshold);
        }
    }

    private interface StateFilter {
        public SpacecraftState filter(final SpacecraftState state) throws IllegalArgumentException, PatriusException;
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

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

}
