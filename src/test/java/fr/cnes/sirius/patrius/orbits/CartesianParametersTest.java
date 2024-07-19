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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:426:30/10/2015: Suppression of testNonInertialFrame regarding the new functionalities for orbit definition
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:513:09/03/2016:Frame class modified, more data serialized
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::FA:658:29/07/2016:Correction in conversion keplerian orbit <=> cartesian orbit
 * VERSION::FA:836:17/02/2017:Code optimization for getA(), orbitShiftedBy()
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class CartesianParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CartesianParametersTest.class.getSimpleName(),
                "Cartesian parameters");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link CartesianParameters# getKeplerianParameters(double)}
     * 
     * @description This test ensures that the same orbit is retrieved if we start from
     *              a given keplerian orbit, convert it to a cartesian orbit and then covert it back
     *              to a keplerian one.
     * 
     * @input a keplerian orbit
     * 
     * @output a keplerian orbit converted back from an intermediate cartesian orbit
     * @testPassCriteria the orbital parameters should be the same according the given threshold
     *                   (eps = 1.0E-16).
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void conversionParametersTest() {

        // Epsilon for orbital parameters comparison
        final double eps = Precision.EPSILON;

        // Keplerian orbit
        final double a = 6998046.5446979;
        final double e = 0.000279142942193666;
        final double i = 0.;
        final double pa = MathLib.toRadians(180.);
        final double raan = 0.;
        final double v = 0.;

        final KeplerianOrbit kepOrbit = new KeplerianOrbit(a, e, i, pa, raan, v,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        // Convert to cartesian orbit
        final CartesianOrbit cartOrbit = new CartesianOrbit(kepOrbit);

        // Convert back to keplerian
        final KeplerianOrbit kepOrbitBack = new KeplerianOrbit(cartOrbit);

        // Compare initial orbit to the new one
        Assert.assertEquals(MathLib.abs(kepOrbit.getA() - kepOrbitBack.getA()) / kepOrbit.getA(),
                0., 2 * eps);
        Assert.assertEquals(kepOrbit.getE(), kepOrbitBack.getE(), 2 * eps);
        Assert.assertEquals(kepOrbit.getI(), kepOrbitBack.getI(), eps);
        Assert.assertEquals(kepOrbit.getPerigeeArgument(), kepOrbitBack.getPerigeeArgument(), eps);
        Assert.assertEquals(kepOrbit.getRightAscensionOfAscendingNode(),
                kepOrbitBack.getRightAscensionOfAscendingNode(), eps);
        Assert.assertEquals(kepOrbit.getAnomaly(PositionAngle.MEAN),
                kepOrbitBack.getAnomaly(PositionAngle.MEAN), eps);
    }

    @Test
    public void testEquals() throws PatriusException {

        final Vector3D position1 = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity1 = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates1 = new PVCoordinates(position1, velocity1);
        final AbsoluteDate date1 = new AbsoluteDate("2004-01-01T23:00:00.000",
                TimeScalesFactory.getUTC());
        final double mu = 3.9860047e14;

        final Vector3D position2 = new Vector3D(-29000000.0, 30329259.0, -100125.0);
        final Vector3D velocity2 = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates2 = new PVCoordinates(position2, velocity2);
        final AbsoluteDate date2 = new AbsoluteDate("2004-01-01T23:00:00.000",
                TimeScalesFactory.getUTC());

        final CartesianOrbit orb1 = new CartesianOrbit(pvCoordinates1, FramesFactory.getEME2000(),
                date1, mu);

        final CartesianOrbit orb2 = new CartesianOrbit(pvCoordinates2, FramesFactory.getEME2000(),
                date2, mu);

        Assert.assertTrue(orb1.equals(orb1));
        Assert.assertFalse(orb1.equals(orb2));
        Assert.assertFalse(orb1.hashCode() == orb2.hashCode());
        
        final KeplerianOrbit orbitKep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                3.986004415e+14);
        
        Assert.assertFalse(orb1.equals(orbitKep));
        
        // Parameters tests
        
        final CartesianParameters cartParam1 = new CartesianParameters(pvCoordinates1, mu);

        final CartesianParameters cartParam2 = new CartesianParameters(pvCoordinates2, mu);

        Assert.assertTrue(cartParam1.equals(cartParam1));
        Assert.assertFalse(cartParam1.equals(cartParam2));
        Assert.assertFalse(cartParam1.hashCode() == cartParam2.hashCode());
        
        final KeplerianParameters KepParam = new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, 3.986004415e+14);
        
        Assert.assertFalse(cartParam1.equals(KepParam));
        
        
    }

    @Test
    public void testCartesianToCartesian() {

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final double mu = 3.9860047e14;

        final CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, mu);

        Assert.assertEquals(p.getPVCoordinates().getPosition().getX(), pvCoordinates.getPosition()
                .getX(), Utils.epsilonTest * MathLib.abs(pvCoordinates.getPosition().getX()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getY(), pvCoordinates.getPosition()
                .getY(), Utils.epsilonTest * MathLib.abs(pvCoordinates.getPosition().getY()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getZ(), pvCoordinates.getPosition()
                .getZ(), Utils.epsilonTest * MathLib.abs(pvCoordinates.getPosition().getZ()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getX(), pvCoordinates.getVelocity()
                .getX(), Utils.epsilonTest * MathLib.abs(pvCoordinates.getVelocity().getX()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getY(), pvCoordinates.getVelocity()
                .getY(), Utils.epsilonTest * MathLib.abs(pvCoordinates.getVelocity().getY()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getZ(), pvCoordinates.getVelocity()
                .getZ(), Utils.epsilonTest * MathLib.abs(pvCoordinates.getVelocity().getZ()));

        final CartesianOrbit pbis = new CartesianOrbit(p);
        Assert.assertEquals(p.getPVCoordinates().getPosition().getX(), pbis.getPVCoordinates()
                .getPosition().getX(),
                Utils.epsilonTest * MathLib.abs(pvCoordinates.getPosition().getX()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getY(), pbis.getPVCoordinates()
                .getPosition().getY(),
                Utils.epsilonTest * MathLib.abs(pvCoordinates.getPosition().getY()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getZ(), pbis.getPVCoordinates()
                .getPosition().getZ(),
                Utils.epsilonTest * MathLib.abs(pvCoordinates.getPosition().getZ()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getX(), pbis.getPVCoordinates()
                .getVelocity().getX(),
                Utils.epsilonTest * MathLib.abs(pvCoordinates.getVelocity().getX()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getY(), pbis.getPVCoordinates()
                .getVelocity().getY(),
                Utils.epsilonTest * MathLib.abs(pvCoordinates.getVelocity().getY()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getZ(), pbis.getPVCoordinates()
                .getVelocity().getZ(),
                Utils.epsilonTest * MathLib.abs(pvCoordinates.getVelocity().getZ()));
    }

    @Test
    public void testCartesianToEquinoctial() {

        Report.printMethodHeader("testCartesianToEquinoctial", "Cartesian to equinoctial",
                "Orekit", Utils.epsilonAngle, ComparisonType.RELATIVE);

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        final CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);

        checkDouble(42255170.0028257, p.getA(), Utils.epsilonTest * p.getA(), "a");
        checkDouble(0.592732497856475e-03, p.getEquinoctialEx(),
                Utils.epsilonE * MathLib.abs(p.getE()), "ex");
        checkDouble(-0.206274396964359e-02, p.getEquinoctialEy(),
                Utils.epsilonE * MathLib.abs(p.getE()), "ey");
        checkDouble(
                MathLib.sqrt(MathLib.pow(0.592732497856475e-03, 2)
                        + MathLib.pow(-0.206274396964359e-02, 2)), p.getE(), Utils.epsilonAngle
                        * MathLib.abs(p.getE()), "e");
        checkDouble(MathUtils.normalizeAngle(2 * MathLib.asin(MathLib.sqrt((MathLib.pow(
                0.128021863908325e-03, 2) + MathLib.pow(-0.352136186881817e-02, 2)) / 4.)), p
                .getI()), p.getI(), Utils.epsilonAngle * MathLib.abs(p.getI()), "i");
        checkDouble(MathUtils.normalizeAngle(0.234498139679291e+01, p.getLM()), p.getLM(),
                Utils.epsilonAngle * MathLib.abs(p.getLM()), "LM");
    }

    @Test
    public void testCartesianToKeplerian() {

        Report.printMethodHeader("testCartesianToKeplerian", "Cartesian to Keplerian", "Orekit",
                Utils.epsilonAngle, ComparisonType.RELATIVE);

        final Vector3D position = new Vector3D(-26655470.0, 29881667.0, -113657.0);
        final Vector3D velocity = new Vector3D(-1125.0, -1122.0, 195.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final double mu = 3.9860047e14;

        final CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, mu);
        final KeplerianOrbit kep = new KeplerianOrbit(p);

        checkDouble(22979265.3030773, p.getA(), Utils.epsilonTest * p.getA(), "a");
        checkDouble(0.743502611664700, p.getE(), Utils.epsilonE * MathLib.abs(p.getE()), "e");
        checkDouble(0.122182096220906, p.getI(), Utils.epsilonAngle * MathLib.abs(p.getI()),
                "i");
        checkDouble(2.3298867611497447, p.getLE(), Utils.epsilonTest * p.getLE(), "Le");
        checkDouble(2.2990194129226746, p.getLv(), Utils.epsilonTest * p.getLv(), "Lv");

        final double pa = kep.getPerigeeArgument();
        Assert.assertEquals(MathUtils.normalizeAngle(3.09909041016672, pa), pa, Utils.epsilonAngle
                * MathLib.abs(pa));
        final double raan = kep.getRightAscensionOfAscendingNode();
        Assert.assertEquals(MathUtils.normalizeAngle(2.32231010979999, raan), raan,
                Utils.epsilonAngle * MathLib.abs(raan));
        final double m = kep.getMeanAnomaly();
        Assert.assertEquals(MathUtils.normalizeAngle(3.22888977629034, m), m, Utils.epsilonAngle
                * MathLib.abs(MathLib.abs(m)));
    }

    @Test
    public void testPositionVelocityNorms() {

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        final CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);

        final double e = p.getE();
        final double v = new KeplerianOrbit(p).getTrueAnomaly();
        final double ksi = 1 + e * MathLib.cos(v);
        final double nu = e * MathLib.sin(v);
        final double epsilon = MathLib.sqrt((1 - e) * (1 + e));

        final double a = p.getA();
        final double na = MathLib.sqrt(this.mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a * epsilon * epsilon / ksi, p.getPVCoordinates().getPosition()
                .getNorm(),
                Utils.epsilonTest * MathLib.abs(p.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na * MathLib.sqrt(ksi * ksi + nu * nu) / epsilon, p.initPVCoordinates()
                .getVelocity().getNorm(),
                Utils.epsilonTest * MathLib.abs(p.getPVCoordinates().getVelocity().getNorm()));

    }

    @Test
    public void testGeometry() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        final Vector3D momentum = pvCoordinates.getMomentum().normalize();

        EquinoctialOrbit p = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);

        final double apogeeRadius = p.getA() * (1 + p.getE());
        final double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                    p.getHx(), p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), this.date, this.mu);
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));
            // Assert.assertTrue(position.getNorm() <= apogeeRadius);
            // Assert.assertTrue(position.getNorm() >= perigeeRadius);

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity().normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are
            // normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    @Test
    public void testNumericalIssue25() throws PatriusException {
        final Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        final Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), new AbsoluteDate("2004-01-01T23:00:00.000",
                        TimeScalesFactory.getUTC()), 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 1000);
        Assert.assertTrue(bos.size() < 1500);

        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final CartesianOrbit deserialized = (CartesianOrbit) ois.readObject();
        final Vector3D dp = orbit.getPVCoordinates().getPosition()
                .subtract(deserialized.getPVCoordinates().getPosition());
        final Vector3D dv = orbit.getPVCoordinates().getVelocity()
                .subtract(deserialized.getPVCoordinates().getVelocity());
        Assert.assertEquals(0.0, dp.getNorm(), 1.0e-10);
        Assert.assertEquals(0.0, dv.getNorm(), 1.0e-10);

    }

    @Test
    public void testShiftElliptic() {
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);
        testShift(orbit, new KeplerianOrbit(orbit), 1.0e-13);
    }

    @Test
    public void testShiftCircular() {
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(MathLib.sqrt(this.mu / position.getNorm()),
                position.orthogonal());
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);
        testShift(orbit, new CircularOrbit(orbit), 1.0e-15);
    }

    @Test
    public void testShiftHyperbolic() {
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(3 * MathLib.sqrt(this.mu / position.getNorm()),
                position.orthogonal());
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);
        testShift(orbit, new KeplerianOrbit(orbit), 1.0e-15);
    }

    private static void testShift(final CartesianOrbit tested, final Orbit reference,
            final double threshold) {
        for (double dt = -1000; dt < 1000; dt += 10.0) {

            final PVCoordinates pvTested = tested.shiftedBy(dt).getPVCoordinates();
            final Vector3D pTested = pvTested.getPosition();
            final Vector3D vTested = pvTested.getVelocity();

            final PVCoordinates pvReference = reference.shiftedBy(dt).getPVCoordinates();
            final Vector3D pReference = pvReference.getPosition();
            final Vector3D vReference = pvReference.getVelocity();

            Assert.assertEquals(0, pTested.subtract(pReference).getNorm(),
                    threshold * pReference.getNorm());
            Assert.assertEquals(0, vTested.subtract(vReference).getNorm(),
                    threshold * vReference.getNorm());
        }
    }

    @Test
    public void testJacobianReference() {

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, this.mu);

        final double[][] jacobian = new double[6][6];
        orbit.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            final double[] row = jacobian[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals((i == j) ? 1 : 0, row[j], 1.0e-15);
            }
        }
    }

    @Test
    public void testToString() {
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final double mu = 3.9860047e14;
        final CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                this.date, mu);
        Assert.assertEquals(
                p.toString(),
                "cartesian parameters: {P(-2.9536113E7, 3.0329259E7, -100125.0), V(-2194.0, -2141.0, -8.0)}");
    }

    @Test
    public void testInterpolation() throws PatriusException {

        final double ehMu = 3.9860047e14;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584. - 32.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CartesianOrbit initialOrbit = new CartesianOrbit(
                new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date, ehMu);

        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        // set up a 5 points sample
        final List<Orbit> sample = new ArrayList<>();
        for (double dt = 0; dt < 251.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        // this is bacause we take the full non-Keplerian acceleration into account in
        // the Cartesian parameters, which in this case is preserved by the
        // Eckstein-Hechler propagator
        double maxShiftPError = 0;
        double maxInterpolationPError = 0;
        double maxShiftVError = 0;
        double maxInterpolationVError = 0;
        for (double dt = 0; dt < 240.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final PVCoordinates propagated = propagator.propagate(t).getPVCoordinates();
            final PVCoordinates shiftError = new PVCoordinates(propagated, initialOrbit.shiftedBy(
                    dt).getPVCoordinates());
            final PVCoordinates interpolationError = new PVCoordinates(propagated, initialOrbit
                    .interpolate(t, sample).getPVCoordinates());
            maxShiftPError = MathLib.max(maxShiftPError, shiftError.getPosition().getNorm());
            maxInterpolationPError = MathLib.max(maxInterpolationPError, interpolationError
                    .getPosition().getNorm());
            maxShiftVError = MathLib.max(maxShiftVError, shiftError.getVelocity().getNorm());
            maxInterpolationVError = MathLib.max(maxInterpolationVError, interpolationError
                    .getVelocity().getNorm());
        }
        Assert.assertTrue(maxShiftPError >= 0.);
        Assert.assertTrue(maxInterpolationPError < 3.0e-8);
        Assert.assertTrue(maxShiftVError >= 0);
        Assert.assertTrue(maxInterpolationVError < 2.0e-9);

        // if we go far past sample end, interpolation becomes worse than Keplerian shift
        maxShiftPError = 0;
        maxInterpolationPError = 0;
        maxShiftVError = 0;
        maxInterpolationVError = 0;
        for (double dt = 500.0; dt < 650.0; dt += 1.0) {
            final AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            final PVCoordinates propagated = propagator.propagate(t).getPVCoordinates();
            final PVCoordinates shiftError = new PVCoordinates(propagated, initialOrbit.shiftedBy(
                    dt).getPVCoordinates());
            final PVCoordinates interpolationError = new PVCoordinates(propagated, initialOrbit
                    .interpolate(t, sample).getPVCoordinates());
            maxShiftPError = MathLib.max(maxShiftPError, shiftError.getPosition().getNorm());
            maxInterpolationPError = MathLib.max(maxInterpolationPError, interpolationError
                    .getPosition().getNorm());
            maxShiftVError = MathLib.max(maxShiftVError, shiftError.getVelocity().getNorm());
            maxInterpolationVError = MathLib.max(maxInterpolationVError, interpolationError
                    .getVelocity().getNorm());
        }
        Assert.assertTrue(maxShiftPError < 2500.0);
        Assert.assertTrue(maxInterpolationPError > 800.0);
        Assert.assertTrue(maxShiftVError < 7.0);
        Assert.assertTrue(maxInterpolationVError > 20.0);
    }

    @Test
    public void testParameters() {
        // Initialization
        final CartesianParameters param = new CartesianParameters(new Vector3D(10000E3, 20000E3,
                30000E3), new Vector3D(10E3, 2E3, 3E3), new Vector3D(5, 6, 7), this.mu);
        final CartesianOrbit orbit = new CartesianOrbit(param, FramesFactory.getEME2000(),
                this.date);

        // Check parameters
        final CartesianParameters actual = orbit.getCartesianParameters();
        Assert.assertEquals(10000E3, actual.getPosition().getX(), 0);
        Assert.assertEquals(20000E3, actual.getPosition().getY(), 0);
        Assert.assertEquals(30000E3, actual.getPosition().getZ(), 0);
        Assert.assertEquals(10E3, actual.getVelocity().getX(), 0);
        Assert.assertEquals(2E3, actual.getVelocity().getY(), 0);
        Assert.assertEquals(3E3, actual.getVelocity().getZ(), 0);

        // Check Stela equinoctial parameters (specific cases)
        try {
            // Check the orbit is hyperbolic
            param.getStelaEquinoctialParameters();
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Particular case for ix and iy (close to 0)
        final KeplerianParameters kepParams = new KeplerianParameters(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, this.mu);
        final StelaEquinoctialParameters stelaParams = kepParams.getCartesianParameters()
                .getStelaEquinoctialParameters();
        Assert.assertEquals(0, stelaParams.getIx(), 0);
        Assert.assertEquals(0, stelaParams.getIy(), 0);
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
