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
 * @history Created 10/12/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1951:10/12/2018: Creation Test AlternateEquinoctialParametersTest.java
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
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.AlternateEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AlternateEquinoctialParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AlternateEquinoctialParametersTest.class.getSimpleName(),
                "Alternate Equinoctial parameters");
    }

    @Test
    public void testAltEquinoctialToEquinoctialEll() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final EquinoctialOrbit equi = new EquinoctialOrbit(a, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
        final Vector3D pos = altEqui.getPVCoordinates().getPosition();
        final Vector3D vit = altEqui.getPVCoordinates().getVelocity();

        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vit);

        final AlternateEquinoctialOrbit param = new AlternateEquinoctialOrbit(pvCoordinates,
                FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(param.getN(), altEqui.getN(), Utils.epsilonTest * altEqui.getN());
        Assert.assertEquals(param.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonE
                * MathLib.abs(altEqui.getE()));
        Assert.assertEquals(param.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonE
                * MathLib.abs(altEqui.getE()));
        Assert.assertEquals(param.getHx(), altEqui.getHx(),
                Utils.epsilonAngle * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(param.getHy(), altEqui.getHy(),
                Utils.epsilonAngle * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv(), altEqui.getLv()),
                altEqui.getLv(), Utils.epsilonAngle * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getLM(), altEqui.getLM()),
                altEqui.getLM(), Utils.epsilonAngle * MathLib.abs(altEqui.getLM()));

        Assert.assertEquals(equi.getN(), altEqui.getN(), Utils.epsilonTest * altEqui.getN());
        Assert.assertEquals(equi.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonE
                * MathLib.abs(altEqui.getE()));
        Assert.assertEquals(equi.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonE
                * MathLib.abs(altEqui.getE()));
        Assert.assertEquals(equi.getHx(), altEqui.getHx(),
                Utils.epsilonAngle * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(equi.getHy(), altEqui.getHy(),
                Utils.epsilonAngle * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(equi.getLv(), altEqui.getLv()),
                altEqui.getLv(), Utils.epsilonAngle * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(MathUtils.normalizeAngle(equi.getLM(), altEqui.getLM()),
                altEqui.getLM(), Utils.epsilonAngle * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(MathUtils.normalizeAngle(equi.getLE(), altEqui.getLE()),
                altEqui.getLE(), Utils.epsilonAngle * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testAltEquinoctialToEquinoctialCirc() {
        final double a = 42166.712;
        final double n = MathLib.sqrt(this.mu / a) / a;

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        // circular orbit
        final AlternateEquinoctialOrbit equiCir = new AlternateEquinoctialOrbit(n, 0.1e-10,
                -0.1e-10, hx, hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        final Vector3D posCir = equiCir.getPVCoordinates().getPosition();
        final Vector3D vitCir = equiCir.getPVCoordinates().getVelocity();

        final PVCoordinates pvCoordinates = new PVCoordinates(posCir, vitCir);

        final AlternateEquinoctialOrbit paramCir = new AlternateEquinoctialOrbit(pvCoordinates,
                FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(paramCir.getA(), equiCir.getA(), Utils.epsilonTest * equiCir.getA());
        Assert.assertEquals(paramCir.getEquinoctialEx(), equiCir.getEquinoctialEx(),
                Utils.epsilonEcir * MathLib.abs(equiCir.getE()));
        Assert.assertEquals(paramCir.getEquinoctialEy(), equiCir.getEquinoctialEy(),
                Utils.epsilonEcir * MathLib.abs(equiCir.getE()));
        Assert.assertEquals(paramCir.getHx(), equiCir.getHx(),
                Utils.epsilonAngle * MathLib.abs(equiCir.getI()));
        Assert.assertEquals(paramCir.getHy(), equiCir.getHy(),
                Utils.epsilonAngle * MathLib.abs(equiCir.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), equiCir.getLv()),
                equiCir.getLv(), Utils.epsilonAngle * MathLib.abs(equiCir.getLv()));
    }

    @Test
    public void testAltEquinoctialToCartesian() {

        Report.printMethodHeader("testEquinoctialToCartesian", "Equinoctial to cartesian",
                "Orekit", Utils.epsilonTest, ComparisonType.RELATIVE);

        final double a = 42166.712;
        final double n = MathLib.sqrt(this.mu / a) / a;

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final AlternateEquinoctialOrbit equi = new AlternateEquinoctialOrbit(n, -7.900e-06,
                1.100e-04, hx, hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(),
                this.date, this.mu);
        final Vector3D pos = equi.getPVCoordinates().getPosition();
        final Vector3D vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        final double oneovera = (2. / pos.getNorm()) - vit.getNorm() * vit.getNorm() / this.mu;
        Assert.assertEquals(oneovera, 1. / equi.getA(), 1.0e-7);

        checkDouble(0.233745668678733e+05, pos.getX(),
                Utils.epsilonTest * MathLib.abs(pos.getX()), "X");
        checkDouble(-0.350998914352669e+05, pos.getY(),
                Utils.epsilonTest * MathLib.abs(pos.getY()), "Y");
        checkDouble(-0.150053723123334e+01, pos.getZ(),
                Utils.epsilonTest * MathLib.abs(pos.getZ()), "Z");

        checkDouble(0.809135038364960e+05, vit.getX(),
                Utils.epsilonTest * MathLib.abs(vit.getX()), "VX");
        checkDouble(0.538902268252598e+05, vit.getY(),
                Utils.epsilonTest * MathLib.abs(vit.getY()), "VY");
        checkDouble(0.158527938296630e+02, vit.getZ(),
                Utils.epsilonTest * MathLib.abs(vit.getZ()), "VZ");

    }

    @Test
    public void testAltEquinoctialToKeplerian() {

        Report.printMethodHeader("testEquinoctialToKeplerian", "Equinoctial to Keplerian",
                "Orekit", Utils.epsilonTest, ComparisonType.RELATIVE);

        final double a = 42166.712;
        final double n = MathLib.sqrt(this.mu / a) / a;

        final double ix = 1.20e-4;
        final double iy = -1.16e-4;
        final double i = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4));
        final double hx = MathLib.tan(i / 2) * ix / (2 * MathLib.sin(i / 2));
        final double hy = MathLib.tan(i / 2) * iy / (2 * MathLib.sin(i / 2));

        final AlternateEquinoctialOrbit equi = new AlternateEquinoctialOrbit(n, -7.900e-6,
                1.100e-4, hx, hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        final KeplerianOrbit kep = new KeplerianOrbit(equi);

        checkDouble(42166.71200, equi.getA(), Utils.epsilonTest * kep.getA(), "a");
        checkDouble(0.110283316961361e-03, kep.getE(),
                Utils.epsilonE * MathLib.abs(kep.getE()), "e");
        checkDouble(0.166901168553917e-03, kep.getI(),
                Utils.epsilonAngle * MathLib.abs(kep.getI()), "i");
        checkDouble(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                kep.getPerigeeArgument(),
                Utils.epsilonTest * MathLib.abs(kep.getPerigeeArgument()), "Pa");
        checkDouble(
                MathUtils.normalizeAngle(5.51473467358854, kep.getRightAscensionOfAscendingNode()),
                kep.getRightAscensionOfAscendingNode(),
                Utils.epsilonTest * MathLib.abs(kep.getRightAscensionOfAscendingNode()), "RAAN");
        checkDouble(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()),
                kep.getMeanAnomaly(), Utils.epsilonTest * MathLib.abs(kep.getMeanAnomaly()), "M");

    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolic() {
        final double a = 42166.712;
        final double n = MathLib.sqrt(this.mu / a) / a;
        new AlternateEquinoctialOrbit(n, 0.9, 0.5, 0.01, -0.02, 5.300, PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolic2() {
        final Vector3D pos = new Vector3D(7000000., 0., 0.);
        final Vector3D vel = new Vector3D(0., 50000., 0.);
        final PVCoordinates pv = new PVCoordinates(pos, vel);
        new AlternateEquinoctialOrbit(pv, FramesFactory.getEME2000(), this.date, this.mu);
    }

    @Test
    public void testNumericalIssue25() throws PatriusException {
        final Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        final Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        final AlternateEquinoctialOrbit orbit = new AlternateEquinoctialOrbit(new PVCoordinates(
                position, velocity), FramesFactory.getEME2000(), new AbsoluteDate(
                "2004-01-01T23:00:00.000", TimeScalesFactory.getUTC()), 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testAnomaly() {

        // elliptic orbit
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        AlternateEquinoctialOrbit p = new AlternateEquinoctialOrbit(new PVCoordinates(position,
                velocity), FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kep = new KeplerianOrbit(p);

        final double e = p.getE();
        final double eRatio = MathLib.sqrt((1 - e) / (1 + e));
        final double paPraan = kep.getPerigeeArgument() + kep.getRightAscensionOfAscendingNode();

        final double lv = 1.1;
        // formulations for elliptic case
        double lE = 2 * MathLib.atan(eRatio * MathLib.tan((lv - paPraan) / 2)) + paPraan;
        double lM = lE - e * MathLib.sin(lE - paPraan);

        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), lE, PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(),
                p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), lM, PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));

        // circular orbit
        p = new AlternateEquinoctialOrbit(p.getN(), 0, 0, p.getHx(), p.getHy(), p.getLv(),
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), lE, PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(),
                p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                p.getHx(), p.getHy(), lM, PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
    }

    @Test
    public void testPositionVelocityNorms() {
        final double sma = 42166.712;
        final double n = MathLib.sqrt(this.mu / sma) / sma;

        // elliptic and non equatorial (i retrograde) orbit
        final AlternateEquinoctialOrbit p = new AlternateEquinoctialOrbit(n, 0.5, -0.5, 1.200, 2.1,
                0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi = 1 + ex * MathLib.cos(lv) + ey * MathLib.sin(lv);
        double nu = ex * MathLib.sin(lv) - ey * MathLib.cos(lv);
        double epsilon = MathLib.sqrt(1 - ex * ex - ey * ey);

        double a = p.getA();
        double na = MathLib.sqrt(p.getMu() / a);

        Assert.assertEquals(a * epsilon * epsilon / ksi, p.getPVCoordinates().getPosition()
                .getNorm(),
                Utils.epsilonTest * MathLib.abs(p.getPVCoordinates().getPosition().getNorm()));
        Assert.assertEquals(na * MathLib.sqrt(ksi * ksi + nu * nu) / epsilon, p.getPVCoordinates()
                .getVelocity().getNorm(),
                Utils.epsilonTest * MathLib.abs(p.getPVCoordinates().getVelocity().getNorm()));

        // circular and equatorial orbit
        final AlternateEquinoctialOrbit pCirEqua = new AlternateEquinoctialOrbit(n, 0.1e-8, 0.1e-8,
                0.1e-8, 0.1e-8, 0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
                this.mu);

        ex = pCirEqua.getEquinoctialEx();
        ey = pCirEqua.getEquinoctialEy();
        lv = pCirEqua.getLv();
        ksi = 1 + ex * MathLib.cos(lv) + ey * MathLib.sin(lv);
        nu = ex * MathLib.sin(lv) - ey * MathLib.cos(lv);
        epsilon = MathLib.sqrt(1 - ex * ex - ey * ey);

        a = pCirEqua.getA();
        na = MathLib.sqrt(pCirEqua.getMu() / a);

        Assert.assertEquals(
                a * epsilon * epsilon / ksi,
                pCirEqua.getPVCoordinates().getPosition().getNorm(),
                Utils.epsilonTest
                        * MathLib.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));
        Assert.assertEquals(
                na * MathLib.sqrt(ksi * ksi + nu * nu) / epsilon,
                pCirEqua.getPVCoordinates().getVelocity().getNorm(),
                Utils.epsilonTest
                        * MathLib.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    @Test
    public void testGeometry() {
        final double sma = 42166.712;
        final double n = MathLib.sqrt(this.mu / sma) / sma;

        // elliptic and non equatorial (i retrograde) orbit
        AlternateEquinoctialOrbit p = new AlternateEquinoctialOrbit(n, 0.5, -0.5, 1.200, 2.1, 0.67,
                PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            p = new AlternateEquinoctialOrbit(p.getN(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                    p.getHx(), p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), p.getDate(),
                    p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius,
            // apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }

        // circular and equatorial orbit
        AlternateEquinoctialOrbit pCirEqua = new AlternateEquinoctialOrbit(n, 0.1e-8, 0.1e-8,
                0.1e-8, 0.1e-8, 0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
                this.mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            pCirEqua = new AlternateEquinoctialOrbit(pCirEqua.getN(), pCirEqua.getEquinoctialEx(),
                    pCirEqua.getEquinoctialEy(), pCirEqua.getHx(), pCirEqua.getHy(), lv,
                    PositionAngle.TRUE, pCirEqua.getFrame(), p.getDate(), p.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius,
            // apogee radius]
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    @Test
    public void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);

        AlternateEquinoctialOrbit p = new AlternateEquinoctialOrbit(new PVCoordinates(position,
                velocity), FramesFactory.getEME2000(), this.date, this.mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        velocity = new Vector3D(-60376.2, 76208., 2.7E-4);

        p = new AlternateEquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.date, this.mu);

        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);
    }

    @Test
    public void testJacobianReference() throws PatriusException {
        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final double sma = 7000000.0;
        final double n = MathLib.sqrt(mu / sma) / sma;
        final AlternateEquinoctialOrbit orbEqu = new AlternateEquinoctialOrbit(n, 0.01, -0.02, 1.2,
                2.1, MathLib.toRadians(40.), PositionAngle.MEAN, FramesFactory.getEME2000(),
                dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        // program equ_jacobian
        //
        // use mslib
        // implicit none
        //
        // integer, parameter :: nb = 11
        // integer :: i,j
        // type(tm_code_retour) :: code_retour
        //
        // real(pm_reel), parameter :: mu= 3.986004415e+14_pm_reel
        // real(pm_reel),dimension(3)::vit_car,pos_car
        // type(tm_orb_cir_equa)::cir_equa
        // real(pm_reel), dimension(6,6)::jacob
        // real(pm_reel)::norme,hx,hy,f,dix,diy
        // intrinsic sqrt
        //
        // cir_equa%a=7000000_pm_reel
        // cir_equa%ex=0.01_pm_reel
        // cir_equa%ey=-0.02_pm_reel
        //
        // ! mslib cir-equ parameters use ix = 2 sin(i/2) cos(gom) and iy = 2 sin(i/2) sin(gom)
        // ! equinoctial parameters use hx = tan(i/2) cos(gom) and hy = tan(i/2) sin(gom)
        // ! the conversions between these parameters and their differentials can be computed
        // ! from the ratio f = 2cos(i/2) which can be found either from (ix, iy) or (hx, hy):
        // ! f = sqrt(4 - ix^2 - iy^2) = 2 / sqrt(1 + hx^2 + hy^2)
        // ! hx = ix / f, hy = iy / f
        // ! ix = hx * f, iy = hy *f
        // ! dhx = ((1 + hx^2) / f) dix + (hx hy / f) diy, dhy = (hx hy / f) dix + ((1 + hy^2) /f)
        // diy
        // ! dix = ((1 - ix^2 / 4) f dhx - (ix iy / 4) f dhy, diy = -(ix iy / 4) f dhx + (1 - iy^2 /
        // 4) f dhy
        // hx=1.2_pm_reel
        // hy=2.1_pm_reel
        // f=2_pm_reel/sqrt(1+hx*hx+hy*hy)
        // cir_equa%ix=hx*f
        // cir_equa%iy=hy*f
        //
        // cir_equa%pso_M=40_pm_reel*pm_deg_rad
        //
        // call mv_cir_equa_car(mu,cir_equa,pos_car,vit_car,code_retour)
        // write(*,*)code_retour%valeur
        // write(*,1000)pos_car,vit_car
        //
        //
        // call mu_norme(pos_car,norme,code_retour)
        // write(*,*)norme
        //
        // call mv_car_cir_equa (mu, pos_car, vit_car, cir_equa, code_retour, jacob)
        // write(*,*)code_retour%valeur
        //
        // f=sqrt(4_pm_reel-cir_equa%ix*cir_equa%ix-cir_equa%iy*cir_equa%iy)
        // hx=cir_equa%ix/f
        // hy=cir_equa%iy/f
        // write(*,*)"ix = ", cir_equa%ix, ", iy = ", cir_equa%iy
        // write(*,*)"equinoctial = ", cir_equa%a, cir_equa%ex, cir_equa%ey, hx, hy,
        // cir_equa%pso_M*pm_rad_deg
        //
        // do j = 1,6
        // dix=jacob(4,j)
        // diy=jacob(5,j)
        // jacob(4,j)=((1_pm_reel+hx*hx)*dix+(hx*hy)*diy)/f
        // jacob(5,j)=((hx*hy)*dix+(1_pm_reel+hy*hy)*diy)/f
        // end do
        //
        // do i = 1,6
        // write(*,*) " ",(jacob(i,j),j=1,6)
        // end do
        //
        // 1000 format (6(f24.15,1x))
        // end program equ_jacobian
        final Vector3D pRef = new Vector3D(2004367.298657628707588, 6575317.978060320019722,
                -1518024.843913963763043);
        final Vector3D vRef = new Vector3D(5574.048661495634406, -368.839015744295409,
                5009.529487849066754);
        // Same jRef of EquinoctialParametersTest : but the first row has to be scaled by c0
        final double c0 = -1.5 * MathLib.sqrt(mu / sma) / (sma * sma);
        final double[][] jRef = {
                { c0 * 0.56305379787310628, c0 * 1.8470954710993663, c0 * -0.42643364527246025,
                        c0 * 1370.4369387322224, c0 * -90.682848736736688, c0 * 1231.6441195141242 },
                { 9.52434720041122055E-008, 9.49704503778007296E-008, 4.46607520107935678E-008,
                        1.69704446323098610E-004, 7.05603505855828105E-005,
                        1.14825140460141970E-004 },
                { -5.41784097802642701E-008, 9.54903765833015538E-008, -8.95815777332234450E-008,
                        1.01864980963344096E-004, -1.03194262242761416E-004,
                        1.40668700715197768E-004 },
                { 1.96680305426455816E-007, -1.12388745957974467E-007, -2.27118924123407353E-007,
                        2.06472886488132167E-004, -1.17984506564646906E-004,
                        -2.38427023682723818E-004 },
                { -2.24382495052235118E-007, 1.28218568601277626E-007, 2.59108357381747656E-007,
                        1.89034327703662092E-004, -1.08019615830663994E-004,
                        -2.18289640324466583E-004 },
                { -3.04001022071876804E-007, 1.22214683774559989E-007, 1.35141804810132761E-007,
                        -1.34034616931480536E-004, -2.14283975204169379E-004,
                        1.29018773893081404E-004 } };

        final PVCoordinates pv = orbEqu.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 2.0e-16 * pRef.getNorm());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 2.0e-16 * vRef.getNorm());

        final double[][] jacobian = new double[6][6];
        orbEqu.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            final double[] row = jacobian[i];
            final double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 4.0e-15);
            }
        }
    }

    @Test
    public void testJacobianFinitedifferences() throws PatriusException {
        final double sma = 7000000.0;
        final double n = MathLib.sqrt(this.mu / sma) / sma;
        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final AlternateEquinoctialOrbit orbEqu = new AlternateEquinoctialOrbit(n, 0.01, -0.02, 1.2,
                2.1, MathLib.toRadians(40.), PositionAngle.MEAN, FramesFactory.getEME2000(),
                dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbEqu, hP);
            final double[][] jacobian = new double[6][6];
            orbEqu.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 5.0e-9);
                }
            }
        }

    }

    @Test
    public void testJacobianInverseFinitedifferences() throws PatriusException {
        final double sma = 7000000.0;
        final double n = MathLib.sqrt(this.mu / sma) / sma;
        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final AlternateEquinoctialOrbit orbEqu = new AlternateEquinoctialOrbit(n, 0.01, -0.02, 1.2,
                2.1, MathLib.toRadians(40.), PositionAngle.MEAN, FramesFactory.getEME2000(),
                dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbEqu, hP);
            final double[][] ref = new QRDecomposition(new BlockRealMatrix(finiteDiffJacobian))
                    .getSolver().getInverse().getData(false);
            final double[][] jacobian = new double[6][6];
            orbEqu.getJacobianWrtParameters(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = ref[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 3.0e-8);
                }
            }
        }
    }

    /**
     * Method to compute Jacobian by finite differences.
     */
    private static double[][] finiteDifferencesJacobian(final PositionAngle type,
                                                 final AlternateEquinoctialOrbit orbit, final double hP) {
        final double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    /**
     * Method to compute one column of Jacobian by finite differences.
     */
    private static void fillColumn(final PositionAngle type, final int i,
            final AlternateEquinoctialOrbit orbit, final double hP, final double[][] jacobian) {

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

        final AlternateEquinoctialOrbit oM4h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, -4, dP), new Vector3D(1, v, -4, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oM3h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, -3, dP), new Vector3D(1, v, -3, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oM2h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, -2, dP), new Vector3D(1, v, -2, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oM1h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, -1, dP), new Vector3D(1, v, -1, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oP1h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, +1, dP), new Vector3D(1, v, +1, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oP2h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, +2, dP), new Vector3D(1, v, +2, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oP3h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, +3, dP), new Vector3D(1, v, +3, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final AlternateEquinoctialOrbit oP4h = new AlternateEquinoctialOrbit(new PVCoordinates(
                new Vector3D(1, p, +4, dP), new Vector3D(1, v, +4, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getN() - oM4h.getN()) + 32 * (oP3h.getN() - oM3h.getN()) - 168
                * (oP2h.getN() - oM2h.getN()) + 672 * (oP1h.getN() - oM1h.getN()))
                / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getEquinoctialEx() - oM4h.getEquinoctialEx()) + 32
                * (oP3h.getEquinoctialEx() - oM3h.getEquinoctialEx()) - 168
                * (oP2h.getEquinoctialEx() - oM2h.getEquinoctialEx()) + 672 * (oP1h
                .getEquinoctialEx() - oM1h.getEquinoctialEx())) / (840 * h);
        jacobian[2][i] = (-3 * (oP4h.getEquinoctialEy() - oM4h.getEquinoctialEy()) + 32
                * (oP3h.getEquinoctialEy() - oM3h.getEquinoctialEy()) - 168
                * (oP2h.getEquinoctialEy() - oM2h.getEquinoctialEy()) + 672 * (oP1h
                .getEquinoctialEy() - oM1h.getEquinoctialEy())) / (840 * h);
        jacobian[3][i] = (-3 * (oP4h.getHx() - oM4h.getHx()) + 32 * (oP3h.getHx() - oM3h.getHx())
                - 168 * (oP2h.getHx() - oM2h.getHx()) + 672 * (oP1h.getHx() - oM1h.getHx()))
                / (840 * h);
        jacobian[4][i] = (-3 * (oP4h.getHy() - oM4h.getHy()) + 32 * (oP3h.getHy() - oM3h.getHy())
                - 168 * (oP2h.getHy() - oM2h.getHy()) + 672 * (oP1h.getHy() - oM1h.getHy()))
                / (840 * h);
        jacobian[5][i] = (-3 * (oP4h.getL(type) - oM4h.getL(type)) + 32
                * (oP3h.getL(type) - oM3h.getL(type)) - 168 * (oP2h.getL(type) - oM2h.getL(type)) + 672 * (oP1h
                .getL(type) - oM1h.getL(type))) / (840 * h);

    }

    @Test
    public void testToString() {
        final double a = 42166.712;
        final double n = MathLib.sqrt(this.mu / a) / a;
        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        // elliptic orbit
        final AlternateEquinoctialOrbit equi = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx, hy,
                5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(
                equi.toString(),
                "equinoctial parameters: {n: 2.3057610397575123; ex: 0.5; ey: -0.5; hx: 6.000000020892001E-5; hy: -5.8000000201956006E-5; lM: 303.6676314193363;}");
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
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final AlternateEquinoctialOrbit initialOrbit = new AlternateEquinoctialOrbit(
                new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date, ehMu);

        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialOrbit,
                ae, ehMu, initialOrbit.getFrame(), c20, c30, c40, c50, c60,
                ParametersType.OSCULATING);

        // set up a 5 points sample
        final List<Orbit> sample = new ArrayList<>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError = 0;
        double maxInterpolationError = 0;
        for (double dt = 0; dt < 241.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Vector3D shifted = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            final Vector3D interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates()
                    .getPosition();
            final Vector3D propagated = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = MathLib.max(maxShiftError, shifted.subtract(propagated).getNorm());
            maxInterpolationError = MathLib.max(maxInterpolationError,
                    interpolated.subtract(propagated).getNorm());
        }
        Assert.assertTrue(maxShiftError > 390.0);
        Assert.assertTrue(maxInterpolationError < 0.04);

        // slightly past sample end, interpolation should quickly increase, but remain reasonable
        maxShiftError = 0;
        maxInterpolationError = 0;
        for (double dt = 240; dt < 300.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Vector3D shifted = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            final Vector3D interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates()
                    .getPosition();
            final Vector3D propagated = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = MathLib.max(maxShiftError, shifted.subtract(propagated).getNorm());
            maxInterpolationError = MathLib.max(maxInterpolationError,
                    interpolated.subtract(propagated).getNorm());
        }
        Assert.assertTrue(maxShiftError < 610.0);
        Assert.assertTrue(maxInterpolationError < 1.3);

        // far past sample end, interpolation should become really wrong
        // (in this test case, break even occurs at around 863 seconds, with a 3.9 km error)
        maxShiftError = 0;
        maxInterpolationError = 0;
        for (double dt = 300; dt < 1000; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final Vector3D shifted = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            final Vector3D interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates()
                    .getPosition();
            final Vector3D propagated = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = MathLib.max(maxShiftError, shifted.subtract(propagated).getNorm());
            maxInterpolationError = MathLib.max(maxInterpolationError,
                    interpolated.subtract(propagated).getNorm());
        }
        Assert.assertTrue(maxShiftError < 5000.0);
        Assert.assertTrue(maxInterpolationError > 8800.0);

    }

    @Test
    public void testParameters() {
        // Initialization
        final double a = 10000E3;
        final double n = MathLib.sqrt(this.mu / a) / a;
        final AlternateEquinoctialParameters param = new AlternateEquinoctialParameters(n, 0.1,
                0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE, this.mu);
        final AlternateEquinoctialOrbit orbit = new AlternateEquinoctialOrbit(param,
                FramesFactory.getEME2000(), this.date);

        // Check parameters
        final AlternateEquinoctialParameters actual = orbit.getAlternateEquinoctialParameters();
        Assert.assertEquals(6.313481369260545E-4, actual.getN(), 0);
        Assert.assertEquals(0.1, actual.getEquinoctialEx(), 0);
        Assert.assertEquals(0.2, actual.getEquinoctialEy(), 0);
        Assert.assertEquals(0.3, actual.getHx(), 0);
        Assert.assertEquals(0.4, actual.getHy(), 0);
        Assert.assertEquals(0.5, actual.getLv(), 0);

        final AlternateEquinoctialParameters actual2 = param.getAlternateEquinoctialParameters();
        Assert.assertEquals(6.313481369260545E-4, actual2.getN(), 0);
        Assert.assertEquals(0.1, actual2.getEquinoctialEx(), 0);
        Assert.assertEquals(0.2, actual2.getEquinoctialEy(), 0);
        Assert.assertEquals(0.3, actual2.getHx(), 0);
        Assert.assertEquals(0.4, actual2.getHy(), 0);
        Assert.assertEquals(0.5, actual2.getLv(), 0);

        // Check Stela equinoctial parameters
        final StelaEquinoctialParameters stelaParams = param.getStelaEquinoctialParameters();
        final double aStela = stelaParams.getA();
        final double nStela = MathLib.sqrt(this.mu / aStela) / aStela;
        Assert.assertEquals(6.313481369260545E-4, nStela, 0);
        Assert.assertEquals(0.1, stelaParams.getEquinoctialEx(), 0);
        Assert.assertEquals(0.2, stelaParams.getEquinoctialEy(), 0);
        Assert.assertEquals(param.getKeplerianParameters().getStelaEquinoctialParameters().getIx(),
                stelaParams.getIx(), 1E-15);
        Assert.assertEquals(param.getKeplerianParameters().getStelaEquinoctialParameters().getIy(),
                stelaParams.getIy(), 0);
        Assert.assertEquals(param.getLM(), stelaParams.getLM(), 0);

    }

    @Test
    public void testAltEquinoctialConvCirc() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 2.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final CircularOrbit circ = new CircularOrbit(altEqui);

        // Comparison AlternateEquinoctialOrbit-CartesianOrbit
        Assert.assertEquals(circ.getN(), altEqui.getN(),
                Utils.epsilonTest * MathLib.abs(altEqui.getN()));
        Assert.assertEquals(circ.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEx()));
        Assert.assertEquals(circ.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEy()));
        Assert.assertEquals(circ.getHx(), altEqui.getHx(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHx()));
        Assert.assertEquals(circ.getHy(), altEqui.getHy(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHy()));
        Assert.assertEquals(circ.getI(), altEqui.getI(),
                Utils.epsilonTest * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(circ.getLv(), altEqui.getLv(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(circ.getLM(), altEqui.getLM(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(circ.getLE(), altEqui.getLE(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testAltEquinoctialConvCart() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 2.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final CartesianOrbit cart = new CartesianOrbit(altEqui);

        // Comparison AlternateEquinoctialOrbit-CartesianOrbit
        Assert.assertEquals(cart.getN(), altEqui.getN(),
                Utils.epsilonTest * MathLib.abs(altEqui.getN()));
        Assert.assertEquals(cart.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEx()));
        Assert.assertEquals(cart.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEy()));
        Assert.assertEquals(cart.getHx(), altEqui.getHx(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHx()));
        Assert.assertEquals(cart.getHy(), altEqui.getHy(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHy()));
        Assert.assertEquals(cart.getI(), altEqui.getI(),
                Utils.epsilonTest * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(cart.getLv(), altEqui.getLv(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(cart.getLM(), altEqui.getLM(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(cart.getLE(), altEqui.getLE(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testAltEquinoctialConvApsis() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 2.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final ApsisOrbit apsis = new ApsisOrbit(altEqui);

        // Comparison AlternateEquinoctialOrbit-CartesianOrbit
        Assert.assertEquals(apsis.getN(), altEqui.getN(),
                Utils.epsilonTest * MathLib.abs(altEqui.getN()));
        Assert.assertEquals(apsis.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEx()));
        Assert.assertEquals(apsis.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEy()));
        Assert.assertEquals(apsis.getHx(), altEqui.getHx(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHx()));
        Assert.assertEquals(apsis.getHy(), altEqui.getHy(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHy()));
        Assert.assertEquals(apsis.getI(), altEqui.getI(),
                Utils.epsilonTest * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(apsis.getLv(), altEqui.getLv(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(apsis.getLM(), altEqui.getLM(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(apsis.getLE(), altEqui.getLE(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testAltEquinoctialConvEquat() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 2.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final EquatorialOrbit equat = new EquatorialOrbit(altEqui);

        // Comparison AlternateEquinoctialOrbit-CartesianOrbit
        Assert.assertEquals(equat.getN(), altEqui.getN(),
                Utils.epsilonTest * MathLib.abs(altEqui.getN()));
        Assert.assertEquals(equat.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEx()));
        Assert.assertEquals(equat.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEy()));
        Assert.assertEquals(equat.getHx(), altEqui.getHx(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHx()));
        Assert.assertEquals(equat.getHy(), altEqui.getHy(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHy()));
        Assert.assertEquals(equat.getI(), altEqui.getI(),
                Utils.epsilonTest * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(equat.getLv(), altEqui.getLv(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(equat.getLM(), altEqui.getLM(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(equat.getLE(), altEqui.getLE(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testAltEquinoctialConvEquin() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 2.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final EquinoctialOrbit equin = new EquinoctialOrbit(altEqui);

        // Comparison AlternateEquinoctialOrbit-CartesianOrbit
        Assert.assertEquals(equin.getN(), altEqui.getN(),
                Utils.epsilonTest * MathLib.abs(altEqui.getN()));
        Assert.assertEquals(equin.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEx()));
        Assert.assertEquals(equin.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEy()));
        Assert.assertEquals(equin.getHx(), altEqui.getHx(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHx()));
        Assert.assertEquals(equin.getHy(), altEqui.getHy(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHy()));
        Assert.assertEquals(equin.getI(), altEqui.getI(),
                Utils.epsilonTest * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(equin.getLv(), altEqui.getLv(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(equin.getLM(), altEqui.getLM(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(equin.getLE(), altEqui.getLE(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testAltEquinoctialConvKepl() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 2.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        final KeplerianOrbit kepl = new KeplerianOrbit(altEqui);

        // Comparison AlternateEquinoctialOrbit-CartesianOrbit
        Assert.assertEquals(kepl.getN(), altEqui.getN(),
                Utils.epsilonTest * MathLib.abs(altEqui.getN()));
        Assert.assertEquals(kepl.getEquinoctialEx(), altEqui.getEquinoctialEx(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEx()));
        Assert.assertEquals(kepl.getEquinoctialEy(), altEqui.getEquinoctialEy(), Utils.epsilonTest
                * MathLib.abs(altEqui.getEquinoctialEy()));
        Assert.assertEquals(kepl.getHx(), altEqui.getHx(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHx()));
        Assert.assertEquals(kepl.getHy(), altEqui.getHy(),
                Utils.epsilonTest * MathLib.abs(altEqui.getHy()));
        Assert.assertEquals(kepl.getI(), altEqui.getI(),
                Utils.epsilonTest * MathLib.abs(altEqui.getI()));
        Assert.assertEquals(kepl.getLv(), altEqui.getLv(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLv()));
        Assert.assertEquals(kepl.getLM(), altEqui.getLM(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLM()));
        Assert.assertEquals(kepl.getLE(), altEqui.getLE(),
                Utils.epsilonTest * MathLib.abs(altEqui.getLE()));
    }

    @Test
    public void testEquals() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double mu = 3.986004415e+14;
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 20, TimeScalesFactory.getTAI());

        // elliptic orbit
        final double a = 42166.712;
        final double n = MathLib.sqrt(mu / a) / a;

        final AlternateEquinoctialOrbit altEqui1 = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx,
                hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        AlternateEquinoctialOrbit altEqui2 = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx, hy,
                5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        Assert.assertTrue(altEqui1.equals(altEqui1));
        Assert.assertTrue(altEqui1.equals(altEqui2));
        Assert.assertEquals(altEqui1.hashCode(), altEqui2.hashCode());

        altEqui2 = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx, hy, 5.400, PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, mu);
        Assert.assertFalse(altEqui1.equals(altEqui2));
        Assert.assertFalse(altEqui1.hashCode() == altEqui2.hashCode());
        
        final KeplerianOrbit orbitKep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
            1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                3.986004415e+14);
        
        Assert.assertFalse(altEqui1.equals(orbitKep));
        
        // Parameters tests
        
        final AlternateEquinoctialParameters altEquiParam1 = new AlternateEquinoctialParameters(n, 0.5, -0.5, hx,
                hy, 5.300, PositionAngle.MEAN, mu);
        
        AlternateEquinoctialParameters altEquiParam2 = new AlternateEquinoctialParameters(n, 0.5, -0.5, hx, hy,
                5.300, PositionAngle.MEAN, mu);

        Assert.assertTrue(altEquiParam1.equals(altEquiParam1));
        Assert.assertTrue(altEquiParam1.equals(altEquiParam2));
        Assert.assertEquals(altEquiParam1.hashCode(), altEquiParam2.hashCode());

        altEquiParam2 = new AlternateEquinoctialParameters(n, 0.5, -0.5, hx, hy, 5.400, PositionAngle.MEAN,
                mu);
        Assert.assertFalse(altEquiParam1.equals(altEquiParam2));
        Assert.assertFalse(altEquiParam1.hashCode() == altEquiParam2.hashCode());
        
        final KeplerianParameters KepParam = new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, 3.986004415e+14);
        
        Assert.assertFalse(altEquiParam1.equals(KepParam));
        
        
    }

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        this.date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        this.mu = 3.9860047e14;
    }

    @After
    public void tearDown() {
        this.date = null;
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
    private static void checkDouble(final double expected, final double actual, final double threshold,
            final String tag) {
        Assert.assertEquals(expected, actual, threshold);
        Report.printToReport(tag, expected, actual);
    }
}
