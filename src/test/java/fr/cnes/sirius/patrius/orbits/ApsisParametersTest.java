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
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:426:30/10/2015: Suppression of testNonInertialFrame regarding the new functionalities for orbit definition
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ApsisRadiusParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquatorialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Apsis class tests
 * 
 * @author ClaudeD
 * 
 */
public class ApsisParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    private double per;
    private double apo;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ApsisParametersTest.class.getSimpleName(), "Apsis parameters");
    }

    /**
     * Constructors tests
     */
    @SuppressWarnings("unused")
    @Test
    public void testApsisDoubleDoubleDoubleDoubleDoubleDoubleIntFrameAbsoluteDateDouble() {
        final ApsisOrbit orbit1 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertNotNull(orbit1);
        final ApsisOrbit orbit2 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertNotNull(orbit2);
        final ApsisOrbit orbit3 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertNotNull(orbit3);
        try {
            new ApsisOrbit(2., 6., 0.349065850399, 0.104719755120e1, 0.959931088597,
                    0.174532925199, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                    this.mu);
        } catch (final IllegalArgumentException e) {
            // nothing to do as the exception was expected

        }
        try {
            new ApsisOrbit(-3.5, 2.5, 0.349065850399, 0.104719755120e1, 0.959931088597,
                    0.174532925199, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                    this.mu);
        } catch (final IllegalArgumentException e) {
            // nothing to do as the exception was expected

        }
    }

    /**
     * method tests
     */
    @Test
    public void testGetHxHy() {
        final ApsisOrbit orbit = new ApsisOrbit(this.per, this.apo, FastMath.PI, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final double hx = orbit.getHx();
        final double hy = orbit.getHy();
        Assert.assertEquals(Double.NaN, hx, 1.e-14);
        Assert.assertEquals(Double.NaN, hy, 1.e-14);
    }

    /**
     * Constructors tests
     */
    @Test
    public void testApsisPVCoordinatesFrameAbsoluteDateDouble() {
        final ApsisOrbit orbit = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertNotNull(orbit);
    }

    /**
     * Generic constructor test
     */
    @Test
    public void testApsisOrbit() {
        final Vector3D position = new Vector3D(-5910180.0, 4077714.0, -620640.0);
        final Vector3D velocity = new Vector3D(129.0, -1286.0, -7325.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CircularOrbit circularOrbit = new CircularOrbit(pvCoordinates,
                FramesFactory.getEME2000(), this.date, this.mu);
        final ApsisOrbit apsisOrbit = new ApsisOrbit(circularOrbit);
        Assert.assertNotNull(apsisOrbit);
    }

    /**
     * Type test
     */
    @Test
    public void testGetType() {
        final ApsisOrbit orbit = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(OrbitType.APSIS, orbit.getType());
    }

    /**
     * apsis to keplerian test
     */
    @Test
    public void testApsisToKeplerian() {

        Report.printMethodHeader("testApsisToKeplerian", "Apsis to Keplerian", "Orekit",
                Utils.epsilonAngle, ComparisonType.RELATIVE);

        // elliptic orbit
        final ApsisOrbit apsis = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        final Vector3D pos = apsis.getPVCoordinates().getPosition();
        final Vector3D vit = apsis.getPVCoordinates().getVelocity();

        final ApsisOrbit param = new ApsisOrbit(new PVCoordinates(pos, vit),
                FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(param.getPeriapsis(), apsis.getPeriapsis(),
                Utils.epsilonTest * apsis.getPeriapsis());
        Assert.assertEquals(param.getApoapsis(), apsis.getApoapsis(),
                Utils.epsilonTest * apsis.getApoapsis());
        Assert.assertEquals(param.getA(), apsis.getA(), Utils.epsilonTest * apsis.getA());
        Assert.assertEquals(param.getE(), apsis.getE(), Utils.epsilonE * MathLib.abs(apsis.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getI(), apsis.getI()), apsis.getI(),
                Utils.epsilonAngle * MathLib.abs(apsis.getI()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(param.getPerigeeArgument(), apsis.getPerigeeArgument()),
                apsis.getPerigeeArgument(),
                Utils.epsilonAngle * MathLib.abs(apsis.getPerigeeArgument()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(param.getRightAscensionOfAscendingNode(),
                        apsis.getRightAscensionOfAscendingNode()),
                apsis.getRightAscensionOfAscendingNode(),
                Utils.epsilonAngle * MathLib.abs(apsis.getRightAscensionOfAscendingNode()));

        // circular orbit
        final ApsisOrbit apsisCir = new ApsisOrbit(24464560.0, 24464560.0, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        final Vector3D posCir = apsisCir.getPVCoordinates().getPosition();
        final Vector3D vitCir = apsisCir.getPVCoordinates().getVelocity();

        final ApsisOrbit paramCir = new ApsisOrbit(new PVCoordinates(posCir, vitCir),
                FramesFactory.getEME2000(), this.date, this.mu);
        this.checkDouble(param.getPeriapsis(), apsis.getPeriapsis(),
                Utils.epsilonTest * apsis.getPeriapsis(), "Perigee");
        this.checkDouble(param.getApoapsis(), apsis.getApoapsis(),
                Utils.epsilonTest * apsis.getApoapsis(), "Apogee");
        this.checkDouble(paramCir.getA(), apsisCir.getA(), Utils.epsilonTest * apsisCir.getA(), "a");
        this.checkDouble(paramCir.getE(), apsisCir.getE(),
                Utils.epsilonE * MathLib.max(1., MathLib.abs(apsisCir.getE())), "e");
        this.checkDouble(MathUtils.normalizeAngle(paramCir.getI(), apsisCir.getI()),
                apsisCir.getI(), Utils.epsilonAngle * MathLib.abs(apsisCir.getI()), "i");
        this.checkDouble(MathUtils.normalizeAngle(paramCir.getLM(), apsisCir.getLM()),
                apsisCir.getLM(), Utils.epsilonAngle * MathLib.abs(apsisCir.getLM()), "LM");
        this.checkDouble(MathUtils.normalizeAngle(paramCir.getLE(), apsisCir.getLE()),
                apsisCir.getLE(), Utils.epsilonAngle * MathLib.abs(apsisCir.getLE()), "LE");
        this.checkDouble(MathUtils.normalizeAngle(paramCir.getLv(), apsisCir.getLv()),
                apsisCir.getLv(), Utils.epsilonAngle * MathLib.abs(apsisCir.getLv()), "LV");
    }

    /**
     * apsis to cartesian test
     */
    @Test
    public void testApsisToCartesian() {

        Report.printMethodHeader("testApsisToCartesian", "Cartesian to equinoctial", "Orekit",
                Utils.epsilonTest, ComparisonType.RELATIVE);

        final ApsisOrbit apsis = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        final Vector3D pos = apsis.getPVCoordinates().getPosition();
        final Vector3D vit = apsis.getPVCoordinates().getVelocity();
        this.checkDouble(-0.107622532467967e+07, pos.getX(),
                Utils.epsilonTest * MathLib.abs(pos.getX()), "X");
        this.checkDouble(-0.676589636432773e+07, pos.getY(),
                Utils.epsilonTest * MathLib.abs(pos.getY()), "Y");
        this.checkDouble(-0.332308783350379e+06, pos.getZ(),
                Utils.epsilonTest * MathLib.abs(pos.getZ()), "Z");

        this.checkDouble(0.935685775154103e+04, vit.getX(),
                Utils.epsilonTest * MathLib.abs(vit.getX()), "VX");
        this.checkDouble(-0.331234775037644e+04, vit.getY(),
                Utils.epsilonTest * MathLib.abs(vit.getY()), "VY");
        this.checkDouble(-0.118801577532701e+04, vit.getZ(),
                Utils.epsilonTest * MathLib.abs(vit.getZ()), "VZ");
    }

    /**
     * apsis to equinoctial test
     */
    @Test
    public void testApsisToEquinoctial() {

        final ApsisOrbit apsis = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertEquals(24464560.0, apsis.getA(), Utils.epsilonTest * apsis.getA());
        Assert.assertEquals(-0.412036802887626, apsis.getEquinoctialEx(),
                Utils.epsilonE * MathLib.abs(apsis.getE()));
        Assert.assertEquals(-0.603931190671706, apsis.getEquinoctialEy(),
                Utils.epsilonE * MathLib.abs(apsis.getE()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(2 * MathLib.asin(MathLib.sqrt((MathLib.pow(
                        0.652494417368829e-01, 2) + MathLib.pow(0.103158450084864, 2)) / 4.)),
                        apsis.getI()), apsis.getI(), Utils.epsilonAngle * MathLib.abs(apsis.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.416203300000000e+01, apsis.getLM()),
                apsis.getLM(), Utils.epsilonAngle * MathLib.abs(apsis.getLM()));
    }

    /**
     * method test
     */
    @Test
    public void testAddKeplerContribution() {
        final ApsisOrbit orbit1 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final ApsisOrbit orbit2 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), this.date, this.mu);
        final ApsisOrbit orbit3 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);
        final double[] pDot = new double[6];
        orbit1.addKeplerContribution(PositionAngle.MEAN, this.mu, pDot);
        Assert.assertEquals(1.649919628773712e-4, pDot[5], 1.e-14);
        pDot[5] = 0.;
        orbit2.addKeplerContribution(PositionAngle.ECCENTRIC, this.mu, pDot);
        Assert.assertEquals(6.116366933160957E-4, pDot[5], 1.e-14);
        pDot[5] = 0.;
        orbit3.addKeplerContribution(PositionAngle.TRUE, this.mu, pDot);
        Assert.assertEquals(0.0015552801666705183, pDot[5], 1.e-14);
    }

    /**
     * Jacobian test
     * 
     * @throws PatriusException
     */
    @Test
    public void testJacobianFinitedifferencesEllipse() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final double per1 = 7000000.0 * (1. - 0.01);
        final double apo1 = 7000000.0 * (1. + 0.01);
        final ApsisOrbit orbAps = new ApsisOrbit(per1, apo1, MathLib.toRadians(80.),
                MathLib.toRadians(80.), MathLib.toRadians(20.), MathLib.toRadians(40.),
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = this.finiteDifferencesJacobian(type, orbAps, hP);
            final double[][] jacobian = new double[6][6];
            orbAps.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    // System.out.println(j);
                    // System.out.println("row =" + row[j]);

                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 1.0e-7);
                }
            }
        }
    }

    /**
     * @throws PatriusException
     */
    @Test
    public void testJacobianFinitedifferencesHyperbola() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final double per1 = -7000000.0 * (1. - 1.2);
        final double apo1 = -7000000.0 * (1. + 1.2);
        final ApsisOrbit orbAps = new ApsisOrbit(per1, apo1, MathLib.toRadians(80.),
                MathLib.toRadians(80.), MathLib.toRadians(20.), MathLib.toRadians(40.),
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = this.finiteDifferencesJacobian(type, orbAps, hP);
            final double[][] jacobian = new double[6][6];
            orbAps.getJacobianWrtCartesian(type, jacobian);
            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-7);
                }
            }
        }

    }

    /**
     * method test
     */
    @Test
    public void testToString() {
        final ApsisOrbit orbit = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final String out = new String(
                "apsis parameters: {periapsis: 6578520.184, apoapsis: 4.2350599816E7, i: 6.997991918168848, pa: 178.00996553801494, raan: 57.68596377156641, v: 25.421887733782746;}");
        Assert.assertEquals(out, orbit.toString());
    }

    /**
     * method test
     * 
     * @throws PatriusException
     */
    @Test
    public void testInterpolation() throws PatriusException {

        final double ehMu = 3.9860047e14;
        final double ae = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final ApsisOrbit initialOrbit = new ApsisOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), date, ehMu);

        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialOrbit,
                ae, ehMu, initialOrbit.getFrame(), c20, c30, c40, c50, c60,
                ParametersType.OSCULATING);

        // set up a 5 points sample
        final List<Orbit> sample = new ArrayList<Orbit>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be slightly better than Keplerian shift
        // the relative bad behaviour here is due to eccentricity, which cannot be
        // accurately interpolated with a polynomial in this case
        double maxShiftPositionError = 0;
        double maxInterpolationPositionError = 0;
        double maxShiftEccentricityError = 0;
        double maxInterpolationEccentricityError = 0;
        for (double dt = 0; dt < 241.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Vector3D shiftedP = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            final Vector3D interpolatedP = initialOrbit.interpolate(t, sample).getPVCoordinates()
                    .getPosition();
            final Vector3D propagatedP = propagator.propagate(t).getPVCoordinates().getPosition();
            final double shiftedE = initialOrbit.shiftedBy(dt).getE();
            final double interpolatedE = initialOrbit.interpolate(t, sample).getE();
            final double propagatedE = propagator.propagate(t).getE();
            maxShiftPositionError = MathLib.max(maxShiftPositionError,
                    shiftedP.subtract(propagatedP).getNorm());
            maxInterpolationPositionError = MathLib.max(maxInterpolationPositionError,
                    interpolatedP.subtract(propagatedP).getNorm());
            maxShiftEccentricityError = MathLib.max(maxShiftEccentricityError,
                    MathLib.abs(shiftedE - propagatedE));
            maxInterpolationEccentricityError = MathLib.max(maxInterpolationEccentricityError,
                    MathLib.abs(interpolatedE - propagatedE));
        }
        Assert.assertTrue(maxShiftPositionError > 390.0);
        Assert.assertTrue(maxInterpolationPositionError < 62.0);
        Assert.assertTrue(maxShiftEccentricityError > 4.5e-4);
        Assert.assertTrue(maxInterpolationEccentricityError < 2.9e-6);
    }

    private double[][] finiteDifferencesJacobian(final PositionAngle type, final ApsisOrbit orbit,
            final double hP) throws PatriusException {
        final double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            this.fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(final PositionAngle type, final int i, final ApsisOrbit orbit,
            final double hP, final double[][] jacobian) {

        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        final Vector3D p = orbit.getPVCoordinates().getPosition();
        final Vector3D v = orbit.getPVCoordinates().getVelocity();
        final double hV = orbit.getMu() * hP / (v.getNorm() * p.getNormSq());

        double h;
        Vector3D dP = Vector3D.ZERO;
        Vector3D dV = Vector3D.ZERO;
        switch (i) {
            case 0:
                h = hP;
                dP = new Vector3D(hP, 0, 0);
                break;
            case 1:
                h = hP;
                dP = new Vector3D(0, hP, 0);
                break;
            case 2:
                h = hP;
                dP = new Vector3D(0, 0, hP);
                break;
            case 3:
                h = hV;
                dV = new Vector3D(hV, 0, 0);
                break;
            case 4:
                h = hV;
                dV = new Vector3D(0, hV, 0);
                break;
            default:
                h = hV;
                dV = new Vector3D(0, 0, hV);
                break;
        }

        final ApsisOrbit oM4h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, -4, dP),
                new Vector3D(1, v, -4, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oM3h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, -3, dP),
                new Vector3D(1, v, -3, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oM2h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, -2, dP),
                new Vector3D(1, v, -2, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oM1h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, -1, dP),
                new Vector3D(1, v, -1, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oP1h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, +1, dP),
                new Vector3D(1, v, +1, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oP2h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, +2, dP),
                new Vector3D(1, v, +2, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oP3h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, +3, dP),
                new Vector3D(1, v, +3, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final ApsisOrbit oP4h = new ApsisOrbit(new PVCoordinates(new Vector3D(1, p, +4, dP),
                new Vector3D(1, v, +4, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getPeriapsis() - oM4h.getPeriapsis()) + 32
                * (oP3h.getPeriapsis() - oM3h.getPeriapsis()) - 168
                * (oP2h.getPeriapsis() - oM2h.getPeriapsis()) + 672 * (oP1h.getPeriapsis() - oM1h
                .getPeriapsis())) / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getApoapsis() - oM4h.getApoapsis()) + 32
                * (oP3h.getApoapsis() - oM3h.getApoapsis()) - 168
                * (oP2h.getApoapsis() - oM2h.getApoapsis()) + 672 * (oP1h.getApoapsis() - oM1h
                .getApoapsis())) / (840 * h);
        jacobian[2][i] = (-3 * (oP4h.getI() - oM4h.getI()) + 32 * (oP3h.getI() - oM3h.getI()) - 168
                * (oP2h.getI() - oM2h.getI()) + 672 * (oP1h.getI() - oM1h.getI()))
                / (840 * h);
        jacobian[3][i] = (-3 * (oP4h.getPerigeeArgument() - oM4h.getPerigeeArgument()) + 32
                * (oP3h.getPerigeeArgument() - oM3h.getPerigeeArgument()) - 168
                * (oP2h.getPerigeeArgument() - oM2h.getPerigeeArgument()) + 672 * (oP1h
                .getPerigeeArgument() - oM1h.getPerigeeArgument())) / (840 * h);
        jacobian[4][i] = (-3
                * (oP4h.getRightAscensionOfAscendingNode() - oM4h
                        .getRightAscensionOfAscendingNode())
                + 32
                * (oP3h.getRightAscensionOfAscendingNode() - oM3h
                        .getRightAscensionOfAscendingNode())
                - 168
                * (oP2h.getRightAscensionOfAscendingNode() - oM2h
                        .getRightAscensionOfAscendingNode()) + 672 * (oP1h
                .getRightAscensionOfAscendingNode() - oM1h.getRightAscensionOfAscendingNode()))
                / (840 * h);
        jacobian[5][i] = (-3 * (oP4h.getAnomaly(type) - oM4h.getAnomaly(type)) + 32
                * (oP3h.getAnomaly(type) - oM3h.getAnomaly(type)) - 168
                * (oP2h.getAnomaly(type) - oM2h.getAnomaly(type)) + 672 * (oP1h.getAnomaly(type) - oM1h
                .getAnomaly(type))) / (840 * h);

    }

    @Test
    public void testParameters() {
        // Initialization
        final ApsisRadiusParameters param = new ApsisRadiusParameters(10000E3, 20000E3, 0.2, 0.3,
                0.4, 0.5, PositionAngle.TRUE, this.mu);
        final ApsisOrbit orbit = new ApsisOrbit(param, FramesFactory.getEME2000(), this.date);

        // Check parameters
        final ApsisRadiusParameters actual = orbit.getApsisParameters();
        Assert.assertEquals(10000E3, actual.getPeriapsis(), 0);
        Assert.assertEquals(20000E3, actual.getApoapsis(), 0);
        Assert.assertEquals(0.2, actual.getI(), 0);
        Assert.assertEquals(0.3, actual.getPerigeeArgument(), 0);
        Assert.assertEquals(0.4, actual.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(0.5, actual.getAnomaly(PositionAngle.TRUE), 0);

        final ApsisRadiusParameters actual2 = param.getApsisRadiusParameters();
        Assert.assertEquals(10000E3, actual2.getPeriapsis(), 0);
        Assert.assertEquals(20000E3, actual2.getApoapsis(), 0);
        Assert.assertEquals(0.2, actual2.getI(), 0);
        Assert.assertEquals(0.3, actual2.getPerigeeArgument(), 0);
        Assert.assertEquals(0.4, actual2.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(0.5, actual2.getAnomaly(PositionAngle.TRUE), 0);

        // Check equatorial parameters
        final EquatorialParameters equatParams = param.getEquatorialParameters();
        final EquatorialParameters equatExp = param.getKeplerianParameters()
                .getEquatorialParameters();
        Assert.assertEquals(equatExp.getA(), equatParams.getA(), 0);
        Assert.assertEquals(equatExp.getE(), equatParams.getE(), 0);
        Assert.assertEquals(equatExp.getPomega(), equatParams.getPomega(), 0);
        Assert.assertEquals(equatExp.getIx(), equatParams.getIx(), 0);
        Assert.assertEquals(equatExp.getIy(), equatParams.getIy(), 0);
        Assert.assertEquals(equatExp.getTrueAnomaly(), equatParams.getTrueAnomaly(), 0);

        // Check Stela equinoctial parameters
        final StelaEquinoctialParameters stelaParams = param.getStelaEquinoctialParameters();
        final StelaEquinoctialParameters stelaExp = param.getKeplerianParameters()
                .getStelaEquinoctialParameters();
        Assert.assertEquals(stelaExp.getA(), stelaParams.getA(), 0);
        Assert.assertEquals(stelaExp.getEquinoctialEx(), stelaParams.getEquinoctialEx(), 1E-15);
        Assert.assertEquals(stelaExp.getEquinoctialEy(), stelaParams.getEquinoctialEy(), 1E-15);
        Assert.assertEquals(stelaExp.getIx(), stelaParams.getIx(), 0);
        Assert.assertEquals(stelaExp.getIy(), stelaParams.getIy(), 0);
        Assert.assertEquals(stelaExp.getLM(), stelaParams.getLM(), 1E-15);
    }

    /**
     * Check double method (and write to report).
     * 
     * @param expected
     *        expected value
     * @param actual
     *        actual value
     * @param threshold
     *        threshold
     * @param tag
     *        tag for report
     */
    private void checkDouble(final double expected, final double actual, final double threshold,
            final String tag) {
        Assert.assertEquals(expected, actual, threshold);
        Report.printToReport(tag, expected, actual);
    }

    @Test
    public void testEquals() throws PatriusException {

        final ApsisOrbit orbit1 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        ApsisOrbit orbit2 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertTrue(orbit1.equals(orbit1));
        Assert.assertTrue(orbit1.equals(orbit2));
        Assert.assertEquals(orbit1.hashCode(), orbit2.hashCode());

        orbit2 = new ApsisOrbit(this.per, this.apo, 0.122138, 3.10001, 1.00681, 0.048363,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertFalse(orbit1.equals(orbit2));
        Assert.assertFalse(orbit1.hashCode() == orbit2.hashCode());
        
        final KeplerianOrbit orbitKep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                3.986004415e+14);
        
        Assert.assertFalse(orbit1.equals(orbitKep));
        
        // Parameters tests
        
        final ApsisRadiusParameters apsisParam1 = new ApsisRadiusParameters(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, this.mu);
        ApsisRadiusParameters apsisParam2 = new ApsisRadiusParameters(this.per, this.apo, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, this.mu);

        Assert.assertTrue(apsisParam1.equals(apsisParam1));
        Assert.assertTrue(apsisParam1.equals(apsisParam2));
        Assert.assertEquals(apsisParam1.hashCode(), apsisParam2.hashCode());

        apsisParam2 = new ApsisRadiusParameters(this.per, this.apo, 0.122138, 3.10001, 1.00681, 0.048363,
                PositionAngle.MEAN, this.mu);
        Assert.assertFalse(apsisParam1.equals(apsisParam2));
        Assert.assertFalse(apsisParam1.hashCode() == apsisParam2.hashCode());
        
        final KeplerianParameters KepParam = new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, 3.986004415e+14);
        
        Assert.assertFalse(apsisParam1.equals(KepParam));
        
        
    }

    /**
     * setup
     */
    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        this.date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        this.mu = 3.9860047e14;

        // perapsis and apoapsis values
        this.per = 24464560.0 * (1. - 0.7311);
        this.apo = 24464560.0 * (1. + 0.7311);
    }

    /**
     * teardown
     */
    @After
    public void tearDown() {
        this.date = null;
    }

}
