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
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::FA:323:05/11/2014: anomalies of class EquatorialOrbit
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
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EquatorialParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Bad orbit configuration
    private EquatorialOrbit equa;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EquatorialParametersTest.class.getSimpleName(),
                "Equatorial parameters");
    }

    @Test
    public void testEquatorialToEquatorial() {

        final EquatorialOrbit equi = new EquatorialOrbit(42166.712, 0.5, -0.5, 1.200e-04,
                -1.16e-04, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        final Vector3D pos = equi.getPVCoordinates().getPosition();
        final Vector3D vit = equi.getPVCoordinates().getVelocity();

        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vit);

        final EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates,
                FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(param.getA(), equi.getA(), Utils.epsilonTest * equi.getA());
        Assert.assertEquals(param.getEquinoctialEx(), equi.getEquinoctialEx(), Utils.epsilonE
                * MathLib.abs(equi.getE()));
        Assert.assertEquals(param.getEquinoctialEy(), equi.getEquinoctialEy(), Utils.epsilonE
                * MathLib.abs(equi.getE()));
        Assert.assertEquals(param.getHx(), equi.getHx(),
                Utils.epsilonAngle * MathLib.abs(equi.getI()));
        Assert.assertEquals(param.getHy(), equi.getHy(),
                Utils.epsilonAngle * MathLib.abs(equi.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv(), equi.getLv()), equi.getLv(),
                Utils.epsilonAngle * MathLib.abs(equi.getLv()));

    }

    @Test
    public void testEquatorialToCartesian() {

        Report.printMethodHeader("testEquatorialToCartesian", "Equatorial to cartesian", "Orekit",
                Utils.epsilonTest, ComparisonType.RELATIVE);

        final EquatorialOrbit equa = new EquatorialOrbit(42166.712, 1.1028331696129433E-4,
                1.6424914134994362, 1.200e-04, -1.16e-04, -2.6256767206790226, PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);
        final Vector3D pos = equa.getPVCoordinates().getPosition();
        final Vector3D vit = equa.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        final double oneovera = (2. / pos.getNorm()) - vit.getNorm() * vit.getNorm() / this.mu;
        Assert.assertEquals(oneovera, 1. / equa.getA(), 1.0e-7);

        this.checkDouble(0.233745668678733e+05, pos.getX(),
                Utils.epsilonTest * MathLib.abs(pos.getX()), "X");
        this.checkDouble(-0.350998914352669e+05, pos.getY(),
                Utils.epsilonTest * MathLib.abs(pos.getY()), "Y");
        this.checkDouble(-0.150053723123334e+01, pos.getZ(),
                Utils.epsilonTest * MathLib.abs(pos.getZ()), "Z");

        this.checkDouble(0.809135038364960e+05, vit.getX(),
                Utils.epsilonTest * MathLib.abs(vit.getX()), "VX");
        this.checkDouble(0.538902268252598e+05, vit.getY(),
                Utils.epsilonTest * MathLib.abs(vit.getY()), "VY");
        this.checkDouble(0.158527938296630e+02, vit.getZ(),
                Utils.epsilonTest * MathLib.abs(vit.getZ()), "VZ");

        final EquatorialOrbit equa2 = new EquatorialOrbit(new PVCoordinates(pos, vit),
                FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertEquals(equa.getA(), equa2.getA(), 1.0e-10);
        Assert.assertEquals(equa.getE(), equa2.getE(), 1.0e-10);
        Assert.assertEquals(equa.getPomega(), equa2.getPomega(), 1.0e-10);
        Assert.assertEquals(equa.getHx(), equa2.getHx(), 1.0e-10);
        Assert.assertEquals(equa.getHy(), equa2.getHy(), 1.0e-10);
        Assert.assertEquals(equa.getLM(), equa2.getLM(), 1.0e-10);
    }

    @Test
    public void testEquatorialToKeplerian() {

        Report.printMethodHeader("testEquatorialToKeplerian", "Equatorial to Keplerian", "Orekit",
                Utils.epsilonAngle, ComparisonType.RELATIVE);

        final EquatorialOrbit equa = new EquatorialOrbit(42166.712, 1.1028331696129433E-4,
                1.6424914134994362, 1.20e-4, -1.16e-4, -2.6256767206790226, PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kep = new KeplerianOrbit(equa);

        this.checkDouble(42166.71200, equa.getA(), Utils.epsilonTest * kep.getA(), "a");
        this.checkDouble(0.110283316961361e-03, kep.getE(),
                Utils.epsilonE * MathLib.abs(kep.getE()), "e");
        this.checkDouble(0.166901168553917e-03, kep.getI(),
                Utils.epsilonAngle * MathLib.abs(kep.getI()), "i");
        this.checkDouble(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                kep.getPerigeeArgument(),
                Utils.epsilonTest * MathLib.abs(kep.getPerigeeArgument()), "Pa");
        this.checkDouble(
                MathUtils.normalizeAngle(5.51473467358854, kep.getRightAscensionOfAscendingNode()),
                kep.getRightAscensionOfAscendingNode(),
                Utils.epsilonTest * MathLib.abs(kep.getRightAscensionOfAscendingNode()), "RAAN");
        this.checkDouble(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()),
                kep.getMeanAnomaly(), Utils.epsilonTest * MathLib.abs(kep.getMeanAnomaly()), "M");

    }

    @Test
    public void testAnomaly() {

        // elliptic orbit
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        EquatorialOrbit p = new EquatorialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kep = new KeplerianOrbit(p);

        final double e = p.getE();
        final double eRatio = MathLib.sqrt((1 - e) / (1 + e));
        final double paPraan = kep.getPerigeeArgument() + kep.getRightAscensionOfAscendingNode();

        final double lv = 1.1;
        // formulations for elliptic case
        final double lE = 2 * MathLib.atan(eRatio * MathLib.tan((lv - paPraan) / 2)) + paPraan;
        final double lM = lE - e * MathLib.sin(lE - paPraan);

        p = new EquatorialOrbit(p.getA(), e, p.getPomega(), p.getHx(), p.getHy(), lv - paPraan,
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new EquatorialOrbit(p.getA(), e, p.getPomega(), p.getHx(), p.getHy(), 0,
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new EquatorialOrbit(p.getA(), e, p.getPomega(), p.getHx(), p.getHy(), lE - paPraan,
                PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new EquatorialOrbit(p.getA(), e, p.getPomega(), p.getHx(), p.getHy(), 0,
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new EquatorialOrbit(p.getA(), e, p.getPomega(), p.getHx(), p.getHy(), lM - paPraan,
                PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
    }

    /**
     * @throws PatriusException
     */
    @Test
    public void testJacobianReference() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final EquatorialOrbit orbEqua = new EquatorialOrbit(7000000.0, 0.01, 6.2631853071796044,
                9.1699286388104417E-01, 1.6047375117918272, 6.9813170079771392E-01,
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //
        final Vector3D pRef = new Vector3D(1718418.5353298995, 6504481.985169383,
                -1730597.7146434935);
        final Vector3D vRef = new Vector3D(5716.750735342533, -124.18985175547641,
                5012.043037660162);
        final double[][] jRef = {
                { 5.0237164660592248E-01, 1.9015549809469323E+00, -5.0593217289232251E-01,
                        1.4055217047810729E+03, -3.0533346692333485E+01, 1.2322621014725996E+03 },
                { 9.7288218209169520E-08, 1.0138180231936563E-07, 3.4081276476708132E-08,
                        1.7245942820815307E-04, 7.6472703442232939E-05, 1.1150414643564616E-04 },
                { -6.1032601587229970E-06, 9.1569759244894640E-06, -9.3575683176629313E-06,
                        1.0376306567866743E-02, -9.7472668415237614E-03, 1.3967550049314523E-02 },
                { 1.8121357271001060E-07, -1.0355061297714890E-07, -2.0925853039132173E-07,
                        7.8947283302687504E-05, -4.5112733315821421E-05, -9.1165315242389090E-05 },
                { -1.1748846863044726E-07, 6.7136267788827108E-08, 1.3567120782325457E-07,
                        -1.1908586561692509E-05, 6.8049066066814764E-06, 1.3751582101002136E-05 },
                { 5.7954708636770484E-06, -9.0403051577250573E-06, 9.4923113414399473E-06,
                        -1.0509653218688292E-02, 9.5398170353140840E-03, -1.3821877196545287E-02 } };

        final PVCoordinates pv = orbEqua.getPVCoordinates();

        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 1.0e-15 * pRef.getNorm());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 1.0e-15 * vRef.getNorm());

        final double[][] jacobian = new double[6][6];
        orbEqua.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            final double[] row = jacobian[i];
            final double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                // System.out.println(" i " + i + " j " + j);
                // System.out.println(rowRef[j]+ " " + row[j]);
                Assert.assertEquals(0, MathLib.abs((row[j] - rowRef[j]) / rowRef[j]), 1.0e-12);
            }
        }

    }

    /**
     * @throws PatriusException
     */
    @Test
    public void testJacobianFiniteDifferences() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final EquatorialOrbit orbEqua = new EquatorialOrbit(7000000.0, 0.01, 6.2631853071796044,
                9.1699286388104417E-01, 1.6047375117918272, 6.9813170079771392E-01,
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = this.finiteDifferencesJacobian(type, orbEqua, hP);
            final double[][] jacobian = new double[6][6];
            orbEqua.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    final double errRel = MathLib.abs((row[j] - rowRef[j]) / rowRef[j]);
                    // System.out.println(type + " i " + i + " j " + j);
                    // System.out.println(rowRef[j] + " " + row[j] + " " + errRel );
                    Assert.assertEquals(0., errRel, 1.0e-7);
                }
            }
        }
    }

    private double[][] finiteDifferencesJacobian(final PositionAngle type,
            final EquatorialOrbit orbit, final double hP) throws PatriusException {
        final double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            this.fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(final PositionAngle type, final int i, final EquatorialOrbit orbit,
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

        final EquatorialOrbit oM4h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, -4,
                dP), new Vector3D(1, v, -4, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oM3h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, -3,
                dP), new Vector3D(1, v, -3, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oM2h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, -2,
                dP), new Vector3D(1, v, -2, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oM1h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, -1,
                dP), new Vector3D(1, v, -1, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oP1h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, +1,
                dP), new Vector3D(1, v, +1, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oP2h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, +2,
                dP), new Vector3D(1, v, +2, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oP3h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, +3,
                dP), new Vector3D(1, v, +3, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquatorialOrbit oP4h = new EquatorialOrbit(new PVCoordinates(new Vector3D(1, p, +4,
                dP), new Vector3D(1, v, +4, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getA() - oM4h.getA()) + 32 * (oP3h.getA() - oM3h.getA()) - 168
                * (oP2h.getA() - oM2h.getA()) + 672 * (oP1h.getA() - oM1h.getA()))
                / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getE() - oM4h.getE()) + 32 * (oP3h.getE() - oM3h.getE()) - 168
                * (oP2h.getE() - oM2h.getE()) + 672 * (oP1h.getE() - oM1h.getE()))
                / (840 * h);
        jacobian[2][i] = (-3 * (oP4h.getPomega() - oM4h.getPomega()) + 32
                * (oP3h.getPomega() - oM3h.getPomega()) - 168
                * (oP2h.getPomega() - oM2h.getPomega()) + 672 * (oP1h.getPomega() - oM1h
                .getPomega())) / (840 * h);
        jacobian[3][i] = (-3 * (oP4h.getIx() - oM4h.getIx()) + 32 * (oP3h.getIx() - oM3h.getIx())
                - 168 * (oP2h.getIx() - oM2h.getIx()) + 672 * (oP1h.getIx() - oM1h.getIx()))
                / (840 * h);
        jacobian[4][i] = (-3 * (oP4h.getIy() - oM4h.getIy()) + 32 * (oP3h.getIy() - oM3h.getIy())
                - 168 * (oP2h.getIy() - oM2h.getIy()) + 672 * (oP1h.getIy() - oM1h.getIy()))
                / (840 * h);
        jacobian[5][i] = (-3 * (oP4h.getAnomaly(type) - oM4h.getAnomaly(type)) + 32
                * (oP3h.getAnomaly(type) - oM3h.getAnomaly(type)) - 168
                * (oP2h.getAnomaly(type) - oM2h.getAnomaly(type)) + 672 * (oP1h.getAnomaly(type) - oM1h
                .getAnomaly(type))) / (840 * h);

    }

    @Test
    public void testLowEccentricityOrbit() {

        // Low eccentricity orbit test to cover eMeSinE(double) method.
        final EquatorialOrbit equa = new EquatorialOrbit(42166.712, 0.99, -0.5, 1.2e-4, -1.16e-4,
                0.001, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertNotNull(equa);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadOrbit() {

        // Hyperbolic orbit
        new EquatorialOrbit(-42166.712, 0.5, 2.4, 1.200e-04, -1.16e-04, 5., PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetEccentricAnomaly() {

        // Orbit test (with a < 0 and e > 1) to cover getEccentricAnomaly() method.
        this.equa.getEccentricAnomaly();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMeanAnomaly() {

        // Orbit test (with a < 0 and e > 1) to cover getMeanAnomaly() method.
        this.equa.getMeanAnomaly();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputeJacobianMeanWrtCartesian() {

        // Orbit test (with a < 0 and e > 1) to cover computeJacobianMeanWrtCartesian() method.
        this.equa.computeJacobianMeanWrtCartesian();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputeJacobianEccentricWrtCartesian() {

        // Orbit test (with a < 0 and e > 1) to cover computeJacobianEccentricWrtCartesian() method.
        this.equa.computeJacobianEccentricWrtCartesian();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputeJacobianTrueWrtCartesian() {

        // Orbit test (with a < 0 and e > 1) to cover computeJacobianTrueWrtCartesian() method.
        this.equa.computeJacobianTrueWrtCartesian();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolicOrbitMean() {

        // Hyperbolic orbit
        new EquatorialOrbit(-42166.712, 2., 2.4, 1.200e-04, -1.16e-04, 5., PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolicOrbitEccentric() {

        // Hyperbolic orbit
        new EquatorialOrbit(-42166.712, 2., 2.4, 1.200e-04, -1.16e-04, 5., PositionAngle.ECCENTRIC,
                FramesFactory.getEME2000(), this.date, this.mu);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testOrbitTrue() {

        new EquatorialOrbit(42166.712, -2., 2.4, 1.200e-04, -1.16e-04, 0.1, PositionAngle.TRUE,
                FramesFactory.getEME2000(), this.date, this.mu);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolicPV() {

        final Vector3D pos = new Vector3D(-32627., -23619., -600000.);
        final Vector3D vit = new Vector3D(92744., -41677., 5.);
        ;

        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vit);

        new EquatorialOrbit(pvCoordinates, FramesFactory.getEME2000(), this.date, this.mu);
    }

    @Test
    public void testToString() {

        // elliptic orbit
        final EquatorialOrbit orbit = new EquatorialOrbit(42166.712, 0.5, 2.4, 1.200e-04,
                -1.16e-04, 5., PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        System.out.println(orbit.toString());
        Assert.assertEquals(
                orbit.toString(),
                "equatorial parameters: {a: 42166.712; e: 0.5; pomega: 137.50987083139756; ix: 1.2E-4; iy: -1.16E-4; v: 230.4407212574401;}");
    }

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
        final EquatorialOrbit initialOrbit = new EquatorialOrbit(7208669, 0.1, 2.4, 0.003437,
                -0.0033, 5., PositionAngle.MEAN, FramesFactory.getEME2000(), date, this.mu);

        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialOrbit,
                ae, ehMu, initialOrbit.getFrame(), c20, c30, c40, c50, c60,
                ParametersType.OSCULATING);

        // set up a 5 points sample
        final List<Orbit> sample = new ArrayList<Orbit>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
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
        Assert.assertTrue(maxShiftPositionError > 400.0);
        Assert.assertTrue(maxInterpolationPositionError < 0.02);
        Assert.assertTrue(maxShiftEccentricityError > 3e-4);
        Assert.assertTrue(maxInterpolationEccentricityError < 6.e-10);

    }

    /**
     * Tests the correction of anomalies raised in FA 323 : the inclinaison vector components
     * entered
     * in the constructor should respect ix^2 + iy^2 < 4 to ensure the inclinaison angle can be
     * computed
     * using an arcsinus.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongInclinaisonVector() {
        new EquatorialOrbit(7.0e6, 0.01, MathLib.toRadians(30.0), 2.5, 0.0,
                MathLib.toRadians(30.0), PositionAngle.TRUE, FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
    }

    /**
     * Tests the correction of anomalies raised in FA 323 : the inclinaison vector components
     * entered
     * in the constructor should respect ix^2 + iy^2 < 4 to ensure the inclinaison angle can be
     * computed
     * using an arcsinus.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongInclinaisonVector2() {
        new EquatorialOrbit(new PVCoordinates(new Vector3D(3469602.3621336166,
                -1.2770246093212387E7, 1E-2), new Vector3D(-6573.132776377002, -8156.959795116592,
                1E-2)), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
    }

    @Test
    public void testParameters() {
        // Initialization
        final EquatorialParameters param = new EquatorialParameters(10000E3, 0.1, 0.2, 0.3, 0.4,
                0.5, PositionAngle.TRUE, this.mu);
        final EquatorialOrbit orbit = new EquatorialOrbit(param, FramesFactory.getEME2000(),
                this.date);

        // Check parameters
        final EquatorialParameters actual = orbit.getEquatorialParameters();
        Assert.assertEquals(10000E3, actual.getA(), 0);
        Assert.assertEquals(0.1, actual.getE(), 0);
        Assert.assertEquals(0.2, actual.getPomega(), 0);
        Assert.assertEquals(0.3, actual.getIx(), 0);
        Assert.assertEquals(0.4, actual.getIy(), 0);
        Assert.assertEquals(0.5, actual.getTrueAnomaly(), 0);

        final EquatorialParameters actual2 = param.getEquatorialParameters();
        Assert.assertEquals(10000E3, actual2.getA(), 0);
        Assert.assertEquals(0.1, actual2.getE(), 0);
        Assert.assertEquals(0.2, actual2.getPomega(), 0);
        Assert.assertEquals(0.3, actual2.getIx(), 0);
        Assert.assertEquals(0.4, actual2.getIy(), 0);
        Assert.assertEquals(0.5, actual2.getTrueAnomaly(), 0);

        // Additional check for coverage purpose
        Assert.assertEquals(0.5, orbit.getTrueAnomaly(), 0);

        // Check Apsis/radius parameters
        final ApsisRadiusParameters apsisParams = param.getApsisRadiusParameters();
        final ApsisRadiusParameters apsisExp = param.getKeplerianParameters()
                .getApsisRadiusParameters();

        Assert.assertEquals(apsisExp.getPeriapsis(), apsisParams.getPeriapsis(), 0);
        Assert.assertEquals(apsisExp.getApoapsis(), apsisParams.getApoapsis(), 0);
        Assert.assertEquals(apsisExp.getI(), apsisParams.getI(), 0);
        Assert.assertEquals(apsisExp.getPerigeeArgument(), apsisParams.getPerigeeArgument(), 0);
        Assert.assertEquals(apsisExp.getRightAscensionOfAscendingNode(),
                apsisParams.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(apsisExp.getAnomaly(PositionAngle.TRUE),
                apsisParams.getAnomaly(PositionAngle.TRUE), 0);

        // Check Stela equinoctial parameters
        final StelaEquinoctialParameters stelaParams = param.getStelaEquinoctialParameters();
        final StelaEquinoctialParameters stelaExp = param.getKeplerianParameters()
                .getStelaEquinoctialParameters();
        Assert.assertEquals(stelaExp.getA(), stelaParams.getA(), 0);
        Assert.assertEquals(stelaExp.getEquinoctialEx(), stelaParams.getEquinoctialEx(), 0);
        Assert.assertEquals(stelaExp.getEquinoctialEy(), stelaParams.getEquinoctialEy(), 1E-15);
        Assert.assertEquals(stelaExp.getIx(), stelaParams.getIx(), 0);
        Assert.assertEquals(stelaExp.getIy(), stelaParams.getIy(), 0);
        Assert.assertEquals(stelaExp.getLM(), stelaParams.getLM(), 0);
    }

    @Test
    public void testEquals() throws PatriusException {

        final EquatorialOrbit equa1 = new EquatorialOrbit(42166.712, 0.5, -0.5, 1.200e-04,
                -1.16e-04, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        EquatorialOrbit equa2 = new EquatorialOrbit(42166.712, 0.5, -0.5, 1.200e-04,
                -1.16e-04, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        Assert.assertTrue(equa1.equals(equa1));
        
        Assert.assertTrue(equa1.equals(equa2));
        Assert.assertEquals(equa1.hashCode(), equa2.hashCode());

        equa2 = new EquatorialOrbit(42170.712, 0.5, -0.5, 1.200e-04, -1.16e-04, 5.300,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertFalse(equa1.equals(equa2));
        Assert.assertFalse(equa1.hashCode() == equa2.hashCode());
        
        final KeplerianOrbit orbitKep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                3.986004415e+14);
        
        Assert.assertFalse(equa1.equals(orbitKep));
        
        // Parameters tests
        
        final EquatorialParameters equiParam1 = new EquatorialParameters(42166.712, 0.5, -0.5, 1.200e-04,
                -1.16e-04, 5.300, PositionAngle.MEAN, this.mu);

        EquatorialParameters equiParam2 = new EquatorialParameters(42166.712, 0.5, -0.5, 1.200e-04, -1.16e-04,
                5.300, PositionAngle.MEAN, this.mu);

        Assert.assertTrue(equiParam1.equals(equiParam1));
        Assert.assertTrue(equiParam1.equals(equiParam2));
        Assert.assertEquals(equiParam1.hashCode(), equiParam2.hashCode());

        equiParam2 = new EquatorialParameters(42170.712, 0.5, -0.5, 1.200e-04, -1.16e-04, 5.300,
                PositionAngle.MEAN, this.mu);

        Assert.assertFalse(equiParam1.equals(equiParam2));
        Assert.assertFalse(equiParam1.hashCode() == equiParam2.hashCode());
        
        final KeplerianParameters KepParam = new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, 3.986004415e+14);
        
        Assert.assertFalse(equiParam1.equals(KepParam));
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

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        this.date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        this.mu = 3.9860047e14;

        // Bad orbit configuration with a < 0 and e > 1
        this.equa = new EquatorialOrbit(-42166.712, 1.1, 2.4, 1.200e-04, -1.16e-04, 5.,
                PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);
    }

    @After
    public void tearDown() {
        this.date = null;
    }

}
