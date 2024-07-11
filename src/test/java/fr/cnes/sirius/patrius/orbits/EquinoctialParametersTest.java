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
 * VERSION::DM:596:12/04/2016:Improve test coherence
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
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class EquinoctialParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EquinoctialParametersTest.class.getSimpleName(),
                "Equinoctial parameters");
    }

    @Test
    public void testEquinoctialToEquinoctialEll() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        // elliptic orbit
        final EquinoctialOrbit equi = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
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
    public void testEquinoctialToEquinoctialCirc() {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        // circular orbit
        final EquinoctialOrbit equiCir = new EquinoctialOrbit(42166.712, 0.1e-10, -0.1e-10, hx, hy,
                5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final Vector3D posCir = equiCir.getPVCoordinates().getPosition();
        final Vector3D vitCir = equiCir.getPVCoordinates().getVelocity();

        final PVCoordinates pvCoordinates = new PVCoordinates(posCir, vitCir);

        final EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates,
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
    public void testEquinoctialToCartesian() {

        Report.printMethodHeader("testEquinoctialToCartesian", "Equinoctial to cartesian",
                "Orekit", Utils.epsilonTest, ComparisonType.RELATIVE);

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final EquinoctialOrbit equi = new EquinoctialOrbit(42166.712, -7.900e-06, 1.100e-04, hx,
                hy, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final Vector3D pos = equi.getPVCoordinates().getPosition();
        final Vector3D vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        final double oneovera = (2. / pos.getNorm()) - vit.getNorm() * vit.getNorm() / this.mu;
        Assert.assertEquals(oneovera, 1. / equi.getA(), 1.0e-7);

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

    }

    @Test
    public void testEquinoctialToKeplerian() {

        Report.printMethodHeader("testEquinoctialToKeplerian", "Equinoctial to Keplerian",
                "Orekit", Utils.epsilonTest, ComparisonType.RELATIVE);

        final double ix = 1.20e-4;
        final double iy = -1.16e-4;
        final double i = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4));
        final double hx = MathLib.tan(i / 2) * ix / (2 * MathLib.sin(i / 2));
        final double hy = MathLib.tan(i / 2) * iy / (2 * MathLib.sin(i / 2));

        final EquinoctialOrbit equi = new EquinoctialOrbit(42166.712, -7.900e-6, 1.100e-4, hx, hy,
                5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kep = new KeplerianOrbit(equi);

        this.checkDouble(42166.71200, equi.getA(), Utils.epsilonTest * kep.getA(), "a");
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

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolic() {
        new EquinoctialOrbit(42166.712, 0.9, 0.5, 0.01, -0.02, 5.300, PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolic2() {
        final Vector3D pos = new Vector3D(7000000., 0., 0.);
        final Vector3D vel = new Vector3D(0., 50000., 0.);
        final PVCoordinates pv = new PVCoordinates(pos, vel);
        new EquinoctialOrbit(pv, FramesFactory.getEME2000(), this.date, this.mu);
    }

    @Test
    public void testNumericalIssue25() throws PatriusException {
        final Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        final Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), new AbsoluteDate("2004-01-01T23:00:00.000",
                        TimeScalesFactory.getUTC()), 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testAnomaly() {

        // elliptic orbit
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kep = new KeplerianOrbit(p);

        final double e = p.getE();
        final double eRatio = MathLib.sqrt((1 - e) / (1 + e));
        final double paPraan = kep.getPerigeeArgument() + kep.getRightAscensionOfAscendingNode();

        final double lv = 1.1;
        // formulations for elliptic case
        double lE = 2 * MathLib.atan(eRatio * MathLib.tan((lv - paPraan) / 2)) + paPraan;
        double lM = lE - e * MathLib.sin(lE - paPraan);

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), lE, PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), lM, PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));

        // circular orbit
        p = new EquinoctialOrbit(p.getA(), 0, 0, p.getHx(), p.getHy(), p.getLv(),
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), lE, PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), 0, PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(), p.getHx(),
                p.getHy(), lM, PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * MathLib.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * MathLib.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * MathLib.abs(lM));
    }

    @Test
    public void testPositionVelocityNorms() {

        // elliptic and non equatorial (i retrograde) orbit
        final EquinoctialOrbit p = new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1, 0.67,
                PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

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
        final EquinoctialOrbit pCirEqua = new EquinoctialOrbit(42166.712, 0.1e-8, 0.1e-8, 0.1e-8,
                0.1e-8, 0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

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

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p = new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1, 0.67,
                PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(),
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
        EquinoctialOrbit pCirEqua = new EquinoctialOrbit(42166.712, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            pCirEqua = new EquinoctialOrbit(pCirEqua.getA(), pCirEqua.getEquinoctialEx(),
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

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.date, this.mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        velocity = new Vector3D(-60376.2, 76208., 2.7E-4);

        p = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(),
                this.date, this.mu);

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
        final EquinoctialOrbit orbEqu = new EquinoctialOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                MathLib.toRadians(40.), PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

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
        final double[][] jRef = {
                { 0.56305379787310628, 1.8470954710993663, -0.42643364527246025,
                        1370.4369387322224, -90.682848736736688, 1231.6441195141242 },
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

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final EquinoctialOrbit orbEqu = new EquinoctialOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                MathLib.toRadians(40.), PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = this.finiteDifferencesJacobian(type, orbEqu, hP);
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

    private double[][] finiteDifferencesJacobian(final PositionAngle type,
            final EquinoctialOrbit orbit, final double hP) throws PatriusException {
        final double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            this.fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(final PositionAngle type, final int i, final EquinoctialOrbit orbit,
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

        final EquinoctialOrbit oM4h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -4,
                dP), new Vector3D(1, v, -4, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oM3h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -3,
                dP), new Vector3D(1, v, -3, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oM2h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -2,
                dP), new Vector3D(1, v, -2, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oM1h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -1,
                dP), new Vector3D(1, v, -1, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oP1h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +1,
                dP), new Vector3D(1, v, +1, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oP2h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +2,
                dP), new Vector3D(1, v, +2, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oP3h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +3,
                dP), new Vector3D(1, v, +3, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());
        final EquinoctialOrbit oP4h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +4,
                dP), new Vector3D(1, v, +4, dV)), orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getA() - oM4h.getA()) + 32 * (oP3h.getA() - oM3h.getA()) - 168
                * (oP2h.getA() - oM2h.getA()) + 672 * (oP1h.getA() - oM1h.getA()))
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
        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        // elliptic orbit
        final EquinoctialOrbit equi = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(
                equi.toString(),
                "equinoctial parameters: {a: 42166.712; ex: 0.5; ey: -0.5; hx: 6.000000020892001E-5; hy: -5.8000000201956006E-5; lv: 242.18906079293217;}");
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
        final EquinoctialOrbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position,
                velocity), FramesFactory.getEME2000(), date, ehMu);

        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialOrbit,
                ae, ehMu, initialOrbit.getFrame(), c20, c30, c40, c50, c60,
                ParametersType.OSCULATING);

        // set up a 5 points sample
        final List<Orbit> sample = new ArrayList<Orbit>();
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
        final EquinoctialParameters param = new EquinoctialParameters(10000E3, 0.1, 0.2, 0.3, 0.4,
                0.5, PositionAngle.TRUE, this.mu);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(param, FramesFactory.getEME2000(),
                this.date);

        // Check parameters
        final EquinoctialParameters actual = orbit.getEquinoctialParameters();
        Assert.assertEquals(10000E3, actual.getA(), 0);
        Assert.assertEquals(0.1, actual.getEquinoctialEx(), 0);
        Assert.assertEquals(0.2, actual.getEquinoctialEy(), 0);
        Assert.assertEquals(0.3, actual.getHx(), 0);
        Assert.assertEquals(0.4, actual.getHy(), 0);
        Assert.assertEquals(0.5, actual.getLv(), 0);

        final EquinoctialParameters actual2 = param.getEquinoctialParameters();
        Assert.assertEquals(10000E3, actual2.getA(), 0);
        Assert.assertEquals(0.1, actual2.getEquinoctialEx(), 0);
        Assert.assertEquals(0.2, actual2.getEquinoctialEy(), 0);
        Assert.assertEquals(0.3, actual2.getHx(), 0);
        Assert.assertEquals(0.4, actual2.getHy(), 0);
        Assert.assertEquals(0.5, actual2.getLv(), 0);

        // Check Stela equinoctial parameters
        final StelaEquinoctialParameters stelaParams = param.getStelaEquinoctialParameters();
        Assert.assertEquals(10000E3, stelaParams.getA(), 0);
        Assert.assertEquals(0.1, stelaParams.getEquinoctialEx(), 0);
        Assert.assertEquals(0.2, stelaParams.getEquinoctialEy(), 0);
        Assert.assertEquals(param.getKeplerianParameters().getStelaEquinoctialParameters().getIx(),
                stelaParams.getIx(), 1E-15);
        Assert.assertEquals(param.getKeplerianParameters().getStelaEquinoctialParameters().getIy(),
                stelaParams.getIy(), 0);
        Assert.assertEquals(param.getLM(), stelaParams.getLM(), 0);

    }

    @Test
    public void testEquals() throws PatriusException {

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        // elliptic orbit
        final EquinoctialOrbit equi1 = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        EquinoctialOrbit equi2 = new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertTrue(equi1.equals(equi1));
        Assert.assertTrue(equi1.equals(equi2));
        Assert.assertEquals(equi1.hashCode(), equi2.hashCode());

        equi2 = new EquinoctialOrbit(42180.712, 0.5, -0.5, hx, hy, 5.300, PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertFalse(equi1.equals(equi2));
        Assert.assertFalse(equi1.hashCode() == equi2.hashCode());
        
        final KeplerianOrbit orbitKep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                3.986004415e+14);
        Assert.assertFalse(equi1.equals(orbitKep));
        
        // Parameters tests
        final EquinoctialParameters equiParam1 = new EquinoctialParameters(42166.712, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, this.mu);

        EquinoctialParameters equiParam2 = new EquinoctialParameters(42166.712, 0.5, -0.5, hx, hy, 5.300,
                PositionAngle.MEAN, this.mu);

        Assert.assertTrue(equiParam1.equals(equiParam1));
        Assert.assertTrue(equiParam1.equals(equiParam2));
        Assert.assertEquals(equiParam1.hashCode(), equiParam2.hashCode());

        equiParam2 = new EquinoctialParameters(42180.712, 0.5, -0.5, hx, hy, 5.300, PositionAngle.MEAN,
                this.mu);

        Assert.assertFalse(equiParam1.equals(equiParam2));
        Assert.assertFalse(equiParam1.hashCode() == equiParam2.hashCode());
        
        final KeplerianOrbit KepParam = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                3.986004415e+14);
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
    }

    @After
    public void tearDown() {
        this.date = null;
    }

}
