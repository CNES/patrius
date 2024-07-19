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
* VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.7:DM:DM-2795:18/05/2021:Evolution du package orbits 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
* VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:426:30/10/2015: Suppression of testNonInertialFrame regarding the new functionalities for orbit definition
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:482:02/12/2015: Add tests for new methods of class Orbit
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.io.IOException;
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
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquatorialParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class KeplerianParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    /** Epsilon used for double comparisons. */
    private final double epsilonComparison = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(KeplerianParametersTest.class.getSimpleName(),
                "Keplerian parameters");
    }

    @SuppressWarnings("unused")
    @Test
    public void testKeplerianDoubleDoubleDoubleDoubleDoubleDoubleIntFrameAbsoluteDateDouble() {
        final KeplerianOrbit orbit1 = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        Assert.assertNotNull(orbit1);
        final KeplerianOrbit orbit2 = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.ECCENTRIC, FramesFactory.getEME2000(), this.date,
                this.mu);
        Assert.assertNotNull(orbit2);
        final KeplerianOrbit orbit3 = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date,
                this.mu);
        Assert.assertNotNull(orbit3);
        try {
            new KeplerianOrbit(4.0, 5.0, 0.349065850399, 0.104719755120e1, 0.959931088597,
                    0.174532925199, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                    this.mu);
        } catch (final IllegalArgumentException e) {
            // nothing to do as the exception was expected
        }
        try {
            new KeplerianOrbit(-4.0, 5.0, 0.349065850399, 0.104719755120e1, 0.959931088597,
                    0.174532925199, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                    this.mu);
        } catch (final IllegalArgumentException e) {
            // nothing to do as the exception was expected
        }
    }

    @Test
    public void testGetHxHy() {
        final KeplerianOrbit orbit = new KeplerianOrbit(24464560.0, 0.7311, FastMath.PI, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        final double hx = orbit.getHx();
        final double hy = orbit.getHy();
        Assert.assertEquals(Double.NaN, hx, 1.e-14);
        Assert.assertEquals(Double.NaN, hy, 1.e-14);
    }

    @Test
    public void testKeplerianPVCoordinatesFrameAbsoluteDateDouble() {
        final KeplerianOrbit orbit = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        Assert.assertNotNull(orbit);
    }

    @Test
    public void testKeplerianOrbit() {
        final Vector3D position = new Vector3D(-5910180.0, 4077714.0, -620640.0);
        final Vector3D velocity = new Vector3D(129.0, -1286.0, -7325.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CircularOrbit circularOrbit = new CircularOrbit(pvCoordinates,
                FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(circularOrbit);
        Assert.assertNotNull(keplerianOrbit);
    }

    @Test
    public void testGetType() {
        final KeplerianOrbit orbit = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        Assert.assertEquals(OrbitType.KEPLERIAN, orbit.getType());
    }

    @Test
    public void testKeplerianToKeplerian() {

        // elliptic orbit
        final KeplerianOrbit kep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        final Vector3D pos = kep.getPVCoordinates().getPosition();
        final Vector3D vit = kep.getPVCoordinates().getVelocity();

        final KeplerianOrbit param = new KeplerianOrbit(new PVCoordinates(pos, vit),
                FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(param.getA(), kep.getA(), Utils.epsilonTest * kep.getA());
        Assert.assertEquals(param.getE(), kep.getE(), Utils.epsilonE * MathLib.abs(kep.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getI(), kep.getI()), kep.getI(),
                Utils.epsilonAngle * MathLib.abs(kep.getI()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(param.getPerigeeArgument(), kep.getPerigeeArgument()),
                kep.getPerigeeArgument(),
                Utils.epsilonAngle * MathLib.abs(kep.getPerigeeArgument()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(param.getRightAscensionOfAscendingNode(),
                        kep.getRightAscensionOfAscendingNode()),
                kep.getRightAscensionOfAscendingNode(),
                Utils.epsilonAngle * MathLib.abs(kep.getRightAscensionOfAscendingNode()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getMeanAnomaly(), kep.getMeanAnomaly()),
                kep.getMeanAnomaly(), Utils.epsilonAngle * MathLib.abs(kep.getMeanAnomaly()));

        // circular orbit
        final KeplerianOrbit kepCir = new KeplerianOrbit(24464560.0, 0.0, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        final Vector3D posCir = kepCir.getPVCoordinates().getPosition();
        final Vector3D vitCir = kepCir.getPVCoordinates().getVelocity();

        final KeplerianOrbit paramCir = new KeplerianOrbit(new PVCoordinates(posCir, vitCir),
                FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(paramCir.getA(), kepCir.getA(), Utils.epsilonTest * kepCir.getA());
        Assert.assertEquals(paramCir.getE(), kepCir.getE(),
                Utils.epsilonE * MathLib.max(1., MathLib.abs(kepCir.getE())));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getI(), kepCir.getI()),
                kepCir.getI(), Utils.epsilonAngle * MathLib.abs(kepCir.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLM(), kepCir.getLM()),
                kepCir.getLM(), Utils.epsilonAngle * MathLib.abs(kepCir.getLM()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLE(), kepCir.getLE()),
                kepCir.getLE(), Utils.epsilonAngle * MathLib.abs(kepCir.getLE()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), kepCir.getLv()),
                kepCir.getLv(), Utils.epsilonAngle * MathLib.abs(kepCir.getLv()));

        // hyperbolic orbit
        final KeplerianOrbit kepHyp = new KeplerianOrbit(-24464560.0, 1.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        final Vector3D posHyp = kepHyp.getPVCoordinates().getPosition();
        final Vector3D vitHyp = kepHyp.getPVCoordinates().getVelocity();

        final KeplerianOrbit paramHyp = new KeplerianOrbit(new PVCoordinates(posHyp, vitHyp),
                FramesFactory.getEME2000(), this.date, this.mu);
        Assert.assertEquals(paramHyp.getA(), kepHyp.getA(),
                Utils.epsilonTest * MathLib.abs(kepHyp.getA()));
        Assert.assertEquals(paramHyp.getE(), kepHyp.getE(),
                Utils.epsilonE * MathLib.abs(kepHyp.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramHyp.getI(), kepHyp.getI()),
                kepHyp.getI(), Utils.epsilonAngle * MathLib.abs(kepHyp.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramHyp.getPerigeeArgument(),
                kepHyp.getPerigeeArgument()), kepHyp.getPerigeeArgument(), Utils.epsilonAngle
                * MathLib.abs(kepHyp.getPerigeeArgument()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(paramHyp.getRightAscensionOfAscendingNode(),
                        kepHyp.getRightAscensionOfAscendingNode()),
                kepHyp.getRightAscensionOfAscendingNode(),
                Utils.epsilonAngle * MathLib.abs(kepHyp.getRightAscensionOfAscendingNode()));
        Assert.assertEquals(
                MathUtils.normalizeAngle(paramHyp.getMeanAnomaly(), kepHyp.getMeanAnomaly()),
                kepHyp.getMeanAnomaly(), Utils.epsilonAngle * MathLib.abs(kepHyp.getMeanAnomaly()));

    }

    @Test
    public void testKeplerianToCartesian() {

        Report.printMethodHeader("testKeplerianToCartesian", "Keplerian to cartesian", "Orekit",
                Utils.epsilonTest, ComparisonType.RELATIVE);

        final KeplerianOrbit kep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        final Vector3D pos = kep.getPVCoordinates().getPosition();
        final Vector3D vit = kep.getPVCoordinates().getVelocity();
        checkDouble(-0.107622532467967e+07, pos.getX(),
                Utils.epsilonTest * MathLib.abs(pos.getX()), "X");
        checkDouble(-0.676589636432773e+07, pos.getY(),
                Utils.epsilonTest * MathLib.abs(pos.getY()), "Y");
        checkDouble(-0.332308783350379e+06, pos.getZ(),
                Utils.epsilonTest * MathLib.abs(pos.getZ()), "Z");

        checkDouble(0.935685775154103e+04, vit.getX(),
                Utils.epsilonTest * MathLib.abs(vit.getX()), "VX");
        checkDouble(-0.331234775037644e+04, vit.getY(),
                Utils.epsilonTest * MathLib.abs(vit.getY()), "VY");
        checkDouble(-0.118801577532701e+04, vit.getZ(),
                Utils.epsilonTest * MathLib.abs(vit.getZ()), "VZ");
    }

    @Test
    public void testKeplerianToEquinoctial() {

        Report.printMethodHeader("testKeplerianToEquinoctial", "Keplerian to equinoctial",
                "Orekit", Utils.epsilonAngle, ComparisonType.RELATIVE);

        final KeplerianOrbit kep = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        checkDouble(24464560.0, kep.getA(), Utils.epsilonTest * kep.getA(), "a");
        checkDouble(-0.412036802887626, kep.getEquinoctialEx(),
                Utils.epsilonE * MathLib.abs(kep.getE()), "ex");
        checkDouble(-0.603931190671706, kep.getEquinoctialEy(),
                Utils.epsilonE * MathLib.abs(kep.getE()), "ey");
        checkDouble(MathUtils.normalizeAngle(2 * MathLib.asin(MathLib.sqrt((MathLib.pow(
                0.652494417368829e-01, 2) + MathLib.pow(0.103158450084864, 2)) / 4.)), kep.getI()),
                kep.getI(), Utils.epsilonAngle * MathLib.abs(kep.getI()), "i");
        checkDouble(MathUtils.normalizeAngle(0.416203300000000e+01, kep.getLM()), kep.getLM(),
                Utils.epsilonAngle * MathLib.abs(kep.getLM()), "LM");

    }

    @Test
    public void testAnomaly() {

        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.date, mu);

        // elliptic orbit
        final double e = p.getE();
        final double eRatio = MathLib.sqrt((1 - e) / (1 + e));

        final double v = 1.1;
        // formulations for elliptic case
        double E = 2 * MathLib.atan(eRatio * MathLib.tan(v / 2));
        double M = E - e * MathLib.sin(E);

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), v, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * MathLib.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * MathLib.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * MathLib.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), 0, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), E, PositionAngle.ECCENTRIC, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * MathLib.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * MathLib.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * MathLib.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), 0, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * MathLib.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * MathLib.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * MathLib.abs(M));

        // circular orbit
        p = new KeplerianOrbit(p.getA(), 0, p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), p.getLv(), PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());

        E = v;
        M = E;

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), v, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * MathLib.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * MathLib.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * MathLib.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), 0, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), E, PositionAngle.ECCENTRIC, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * MathLib.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * MathLib.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * MathLib.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), 0, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * MathLib.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * MathLib.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * MathLib.abs(M));

    }

    @Test
    public void testPositionVelocityNorms() {
        final double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        final KeplerianOrbit p = new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, mu);

        double e = p.getE();
        double v = p.getTrueAnomaly();
        double ksi = 1 + e * MathLib.cos(v);
        double nu = e * MathLib.sin(v);
        double epsilon = MathLib.sqrt((1 - e) * (1 + e));

        double a = p.getA();
        double na = MathLib.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a * epsilon * epsilon / ksi, p.getPVCoordinates().getPosition()
                .getNorm(),
                Utils.epsilonTest * MathLib.abs(p.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na * MathLib.sqrt(ksi * ksi + nu * nu) / epsilon, p.getPVCoordinates()
                .getVelocity().getNorm(),
                Utils.epsilonTest * MathLib.abs(p.getPVCoordinates().getVelocity().getNorm()));

        // circular and equatorial orbit
        final KeplerianOrbit pCirEqua = new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686,
                1.00681, 0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, mu);

        e = pCirEqua.getE();
        v = pCirEqua.getTrueAnomaly();
        ksi = 1 + e * MathLib.cos(v);
        nu = e * MathLib.sin(v);
        epsilon = MathLib.sqrt((1 - e) * (1 + e));

        a = pCirEqua.getA();
        na = MathLib.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(
                a * epsilon * epsilon / ksi,
                pCirEqua.getPVCoordinates().getPosition().getNorm(),
                Utils.epsilonTest
                        * MathLib.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(
                na * MathLib.sqrt(ksi * ksi + nu * nu) / epsilon,
                pCirEqua.getPVCoordinates().getVelocity().getNorm(),
                Utils.epsilonTest
                        * MathLib.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    @Test
    public void testGeometry() {
        final double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p = new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681, 0.67,
                PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                    p.getRightAscensionOfAscendingNode(), lv, PositionAngle.TRUE, p.getFrame(),
                    p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are
            // normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }

        // apsides
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), 0, PositionAngle.TRUE, p.getFrame(),
                p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), perigeeRadius,
                perigeeRadius * Utils.epsilonTest);

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), FastMath.PI, PositionAngle.TRUE,
                p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), apogeeRadius,
                apogeeRadius * Utils.epsilonTest);

        // nodes
        // descending node
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), FastMath.PI - p.getPerigeeArgument(),
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(MathLib.abs(p.getPVCoordinates().getPosition().getZ()) < p
                .getPVCoordinates().getPosition().getNorm()
                * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() < 0);

        // ascending node
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                p.getRightAscensionOfAscendingNode(), 2.0 * FastMath.PI - p.getPerigeeArgument(),
                PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(MathLib.abs(p.getPVCoordinates().getPosition().getZ()) < p
                .getPVCoordinates().getPosition().getNorm()
                * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() > 0);

        // circular and equatorial orbit
        KeplerianOrbit pCirEqua = new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();
        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            pCirEqua = new KeplerianOrbit(pCirEqua.getA(), pCirEqua.getE(), pCirEqua.getI(),
                    pCirEqua.getPerigeeArgument(), pCirEqua.getRightAscensionOfAscendingNode(), lv,
                    PositionAngle.TRUE, pCirEqua.getFrame(), pCirEqua.getDate(), pCirEqua.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are
            // normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }
    }

    @Test
    public void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
        Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
        final double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.date, mu);
        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(1742382., -2.440243e7, -0.014517);
        velocity = new Vector3D(4026.2, 287.479, -3.e-6);

        p = new KeplerianOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(),
                this.date, mu);
        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

    }

    @Test
    public void testPeriod() {
        final KeplerianOrbit orbit = new KeplerianOrbit(7654321.0, 0.1, 0.2, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, this.mu);
        Assert.assertEquals(6664.5521723383589487, orbit.getKeplerianPeriod(), 1.0e-12);
        Assert.assertEquals(0.00094277682051291315229, orbit.getKeplerianMeanMotion(), 1.0e-16);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolic() {
        new KeplerianOrbit(7654321.0, 1.1, 0.2, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, this.mu);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHyperbolic2() {
        new KeplerianOrbit(7654321.0, 1.1, 0.2, 0, 0, 0, PositionAngle.MEAN,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, this.mu);
    }

    @Test
    public void testHyperbola() {
        final KeplerianOrbit orbit = new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, this.mu);
        final Vector3D perigeeP = orbit.getPVCoordinates().getPosition();
        final Vector3D u = perigeeP.normalize();
        final Vector3D focus1 = Vector3D.ZERO;
        final Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            final PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            final double d1 = Vector3D.distance(pv.getPosition(), focus1);
            final double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assert.assertEquals(-2 * orbit.getA(), MathLib.abs(d1 - d2), 1.0e-6);
            final KeplerianOrbit rebuilt = new KeplerianOrbit(pv, orbit.getFrame(), orbit.getDate()
                    .shiftedBy(dt), this.mu);
            Assert.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assert.assertEquals(2.5, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    public void testKeplerEquation() {

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            final KeplerianOrbit pElliptic = new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686,
                    1.00681, M, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
            final double E = pElliptic.getEccentricAnomaly();
            final double e = pElliptic.getE();
            Assert.assertEquals(M, E - e * MathLib.sin(E), 2.0e-14);
        }

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            final KeplerianOrbit pAlmostParabolic = new KeplerianOrbit(24464560.0, 0.9999, 2.1,
                    3.10686, 1.00681, M, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                    this.mu);
            final double E = pAlmostParabolic.getEccentricAnomaly();
            final double e = pAlmostParabolic.getE();
            Assert.assertEquals(M, E - e * MathLib.sin(E), 3.0e-13);
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void testOutOfRangeV() throws PatriusException {
        new KeplerianOrbit(-7000434.460140012, 1.1999785407363386, 1.3962787004479158,
                1.3962320168955138, 0.3490728321331678, -2.55593407037698, PositionAngle.TRUE,
                FramesFactory.getEME2000(), new AbsoluteDate("2000-01-01T12:00:00.391",
                        TimeScalesFactory.getUTC()), 3.986004415E14);
    }

    @Test
    public void testNumericalIssue25() throws PatriusException {
        final Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        final Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        final KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), new AbsoluteDate("2004-01-01T23:00:00.000",
                        TimeScalesFactory.getUTC()), 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testPerfectlyEquatorial() throws PatriusException {
        final Vector3D position = new Vector3D(6957904.3624652653594, 766529.11411558074507, 0);
        final Vector3D velocity = new Vector3D(-7538.2817012412102845, 342.38751001881413381, 0.);
        final KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), new AbsoluteDate("2004-01-01T23:00:00.000",
                        TimeScalesFactory.getUTC()), 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getI(), 2.0e-14);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode(), 2.0e-14);
    }

    @Test
    public void testJacobianReferenceEllipse() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final KeplerianOrbit orbKep = new KeplerianOrbit(7000000.0, 0.01, MathLib.toRadians(80.),
                MathLib.toRadians(80.), MathLib.toRadians(20.), MathLib.toRadians(40.),
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        // program kep_jacobian
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
        // type(tm_orb_kep)::kep
        // real(pm_reel), dimension(6,6)::jacob
        // real(pm_reel)::norme
        //
        // kep%a=7000000_pm_reel
        // kep%e=0.01_pm_reel
        // kep%i=80_pm_reel*pm_deg_rad
        // kep%pom=80_pm_reel*pm_deg_rad
        // kep%gom=20_pm_reel*pm_deg_rad
        // kep%M=40_pm_reel*pm_deg_rad
        //
        // call mv_kep_car(mu,kep,pos_car,vit_car,code_retour)
        // write(*,*)code_retour%valeur
        // write(*,1000)pos_car,vit_car
        //
        //
        // call mu_norme(pos_car,norme,code_retour)
        // write(*,*)norme
        //
        // call mv_car_kep (mu, pos_car, vit_car, kep, code_retour, jacob)
        // write(*,*)code_retour%valeur
        //
        // write(*,*)"kep = ", kep%a, kep%e, kep%i*pm_rad_deg,&
        // kep%pom*pm_rad_deg, kep%gom*pm_rad_deg, kep%M*pm_rad_deg
        //
        // do i = 1,6
        // write(*,*) " ",(jacob(i,j),j=1,6)
        // end do
        //
        // 1000 format (6(f24.15,1x))
        // end program kep_jacobian
        final Vector3D pRef = new Vector3D(-3691555.569874833337963, -240330.253992714860942,
                5879700.285850423388183);
        final Vector3D vRef = new Vector3D(-5936.229884450408463, -2871.067660163344044,
                -3786.209549192726627);
        final double[][] jRef = {
                { -1.0792090588217809, -7.02594292049818631E-002, 1.7189029642216496,
                        -1459.4829009393857, -705.88138246206040, -930.87838644776593 },
                { -1.31195762636625214E-007, -3.90087231593959271E-008, 4.65917592901869866E-008,
                        -2.02467187867647177E-004, -7.89767994436215424E-005,
                        -2.81639203329454407E-005 },
                { 4.18334478744371316E-008, -1.14936453412947957E-007, 2.15670500707930151E-008,
                        -2.26450325965329431E-005, 6.22167157217876380E-005,
                        -1.16745469637130306E-005 },
                { 3.52735168061691945E-006, 3.82555734454450974E-006, 1.34715077236557634E-005,
                        -8.06586262922115264E-003, -6.13725651685311825E-003,
                        -1.71765290503914092E-002 },
                { 2.48948022169790885E-008, -6.83979069529389238E-008, 1.28344057971888544E-008,
                        3.86597661353874888E-005, -1.06216834498373629E-004,
                        1.99308724078785540E-005 },
                { -3.41911705254704525E-006, -3.75913623359912437E-006, -1.34013845492518465E-005,
                        8.19851888816422458E-003, 6.16449264680494959E-003,
                        1.69495878276556648E-002 } };

        final PVCoordinates pv = orbKep.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 1.0e-15 * pRef.getNorm());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 1.0e-16 * vRef.getNorm());

        final double[][] jacobian = new double[6][6];
        orbKep.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            final double[] row = jacobian[i];
            final double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-12);
            }
        }

        // coverage tests:
        final double[][] jac = new double[6][6];
        Assert.assertNull(orbKep.getJacobianWrtParametersEccentric());
        orbKep.setJacobianWrtParametersEccentric(jac);
        Assert.assertEquals(jac[0][0], orbKep.getJacobianWrtParametersEccentric()[0][0], 0.0);
    }

    @Test
    public void testJacobianFinitedifferencesEllipse() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final KeplerianOrbit orbKep = new KeplerianOrbit(7000000.0, 0.01, MathLib.toRadians(80.),
                MathLib.toRadians(80.), MathLib.toRadians(20.), MathLib.toRadians(40.),
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP);
            final double[][] jacobian = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-7);
                }
            }
        }

    }

    @Test
    public void testJacobianReferenceHyperbola() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final KeplerianOrbit orbKep = new KeplerianOrbit(-7000000.0, 1.2, MathLib.toRadians(80.),
                MathLib.toRadians(80.), MathLib.toRadians(20.), MathLib.toRadians(40.),
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        // program kep_hyperb_jacobian
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
        // type(tm_orb_kep)::kep
        // real(pm_reel), dimension(6,6)::jacob
        // real(pm_reel)::norme
        //
        // kep%a=7000000_pm_reel
        // kep%e=1.2_pm_reel
        // kep%i=80_pm_reel*pm_deg_rad
        // kep%pom=80_pm_reel*pm_deg_rad
        // kep%gom=20_pm_reel*pm_deg_rad
        // kep%M=40_pm_reel*pm_deg_rad
        //
        // call mv_kep_car(mu,kep,pos_car,vit_car,code_retour)
        // write(*,*)code_retour%valeur
        // write(*,1000)pos_car,vit_car
        //
        //
        // call mu_norme(pos_car,norme,code_retour)
        // write(*,*)norme
        //
        // call mv_car_kep (mu, pos_car, vit_car, kep, code_retour, jacob)
        // write(*,*)code_retour%valeur
        //
        // write(*,*)"kep = ", kep%a, kep%e, kep%i*pm_rad_deg,&
        // kep%pom*pm_rad_deg, kep%gom*pm_rad_deg, kep%M*pm_rad_deg
        //
        // ! convert the sign of da row since mslib uses a > 0 for all orbits
        // ! whereas we use a < 0 for hyperbolic orbits
        // write(*,*) " ",(-jacob(1,j),j=1,6)
        // do i = 2,6
        // write(*,*) " ",(jacob(i,j),j=1,6)
        // end do
        //
        // 1000 format (6(f24.15,1x))
        // end program kep_hyperb_jacobian
        final Vector3D pRef = new Vector3D(-7654711.206549182534218, -3460171.872979687992483,
                -3592374.514463655184954);
        final Vector3D vRef = new Vector3D(-7886.368091820805603, -4359.739012331759113,
                -7937.060044548694350);
        final double[][] jRef = {
                { -0.98364725131848019, -0.44463970750901238, -0.46162803814668391,
                        -1938.9443476028839, -1071.8864775981751, -1951.4074832397598 },
                { -1.10548813242982574E-007, -2.52906747183730431E-008, 7.96500937398593591E-008,
                        -9.70479823470940108E-006, -2.93209076428001017E-005,
                        -1.37434463892791042E-004 },
                { 8.55737680891616672E-008, -2.35111995522618220E-007, 4.41171797903162743E-008,
                        -8.05235180390949802E-005, 2.21236547547460423E-004,
                        -4.15135455876865407E-005 },
                { -1.52641427784095578E-007, 1.10250447958827901E-008, 1.21265251605359894E-007,
                        7.63347077200903542E-005, -3.54738331412232378E-005,
                        -2.31400737283033359E-004 },
                { 7.86711766048035274E-008, -2.16147281283624453E-007, 4.05585791077187359E-008,
                        -3.56071805267582894E-005, 9.78299244677127374E-005,
                        -1.83571253224293247E-005 },
                { -2.41488884881911384E-007, -1.00119615610276537E-007, -6.51494225096757969E-008,
                        -2.43295075073248163E-004, -1.43273725071890463E-004,
                        -2.91625510452094873E-004 } };

        final PVCoordinates pv = orbKep.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm() / pRef.getNorm(), 1.0e-16);
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm() / vRef.getNorm(), 3.0e-16);

        final double[][] jacobian = new double[6][6];
        orbKep.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            final double[] row = jacobian[i];
            final double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 1.0e-14);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferencesHyperbola() throws PatriusException {

        final AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final double mu = 3.986004415e+14;
        final KeplerianOrbit orbKep = new KeplerianOrbit(-7000000.0, 1.2, MathLib.toRadians(80.),
                MathLib.toRadians(80.), MathLib.toRadians(20.), MathLib.toRadians(40.),
                PositionAngle.MEAN, FramesFactory.getEME2000(), dateTca, mu);

        for (final PositionAngle type : PositionAngle.values()) {
            final double hP = 2.0;
            final double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP);
            final double[][] jacobian = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobian);
            for (int i = 0; i < jacobian.length; i++) {
                final double[] row = jacobian[i];
                final double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 3.0e-8);
                }
            }
        }

    }

    private static double[][] finiteDifferencesJacobian(final PositionAngle type,
            final KeplerianOrbit orbit, final double hP) {
        final double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private static void fillColumn(final PositionAngle type, final int i,
            final KeplerianOrbit orbit,
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

        final KeplerianOrbit oM4h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, -4, dP), new Vector3D(1, v, -4, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oM3h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, -3, dP), new Vector3D(1, v, -3, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oM2h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, -2, dP), new Vector3D(1, v, -2, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oM1h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, -1, dP), new Vector3D(1, v, -1, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oP1h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, +1, dP), new Vector3D(1, v, +1, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oP2h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, +2, dP), new Vector3D(1, v, +2, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oP3h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, +3, dP), new Vector3D(1, v, +3, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());
        final KeplerianOrbit oP4h = new KeplerianOrbit(new PVCoordinates(
                new Vector3D(1, p, +4, dP), new Vector3D(1, v, +4, dV)), orbit.getFrame(),
                orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getA() - oM4h.getA()) + 32 * (oP3h.getA() - oM3h.getA()) - 168
                * (oP2h.getA() - oM2h.getA()) + 672 * (oP1h.getA() - oM1h.getA()))
                / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getE() - oM4h.getE()) + 32 * (oP3h.getE() - oM3h.getE()) - 168
                * (oP2h.getE() - oM2h.getE()) + 672 * (oP1h.getE() - oM1h.getE()))
                / (840 * h);
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
    public void testToString() {
        final KeplerianOrbit orbit = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        final String out = new String(
                "keplerian parameters: {a: 2.446456E7; e: 0.7311; i: 6.997991918168848; pa: 178.00996553801494; raan: 57.68596377156641; v: 25.421887733782746;}");
        Assert.assertEquals(out, orbit.toString());
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
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(
                new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date, ehMu);

        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialOrbit,
                ae, ehMu, initialOrbit.getFrame(), c20, c30, c40, c50, c60,
                ParametersType.OSCULATING);

        // set up a 5 points sample
        final List<Orbit> sample = new ArrayList<>();
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
        Assert.assertTrue(maxInterpolationEccentricityError < 2.6e-5);

        // slightly past sample end, bad eccentricity interpolation shows up
        // (in this case, interpolated eccentricity exceeds 1.0 btween 1900
        // and 1910s, while semi-majaxis remains positive, so this is not
        // even a proper hyperbolic orbit...)
        maxShiftPositionError = 0;
        maxInterpolationPositionError = 0;
        maxShiftEccentricityError = 0;
        maxInterpolationEccentricityError = 0;
        for (double dt = 240; dt < 600; dt += 1.0) {
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
        Assert.assertTrue(maxShiftPositionError < 2200.0);
        Assert.assertTrue(maxInterpolationPositionError > 72000.0);
        Assert.assertTrue(maxShiftEccentricityError < 1.2e-3);
        Assert.assertTrue(maxInterpolationEccentricityError > 3.8e-3);

    }

    @Test
    public void testParameters() {
        // Initialization
        final KeplerianParameters param = new KeplerianParameters(10000E3, 0.1, 0.2, 0.3, 0.4, 0.5,
                PositionAngle.TRUE, this.mu);
        final KeplerianOrbit orbit = new KeplerianOrbit(param, FramesFactory.getEME2000(),
                this.date);

        // Check parameters
        final KeplerianParameters actual = orbit.getKeplerianParameters();
        Assert.assertEquals(10000E3, actual.getA(), 0);
        Assert.assertEquals(0.1, actual.getE(), 0);
        Assert.assertEquals(0.2, actual.getI(), 0);
        Assert.assertEquals(0.3, actual.getPerigeeArgument(), 0);
        Assert.assertEquals(0.4, actual.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(0.5, actual.getTrueAnomaly(), 0);

        final KeplerianParameters actual2 = param.getKeplerianParameters();
        Assert.assertEquals(10000E3, actual2.getA(), 0);
        Assert.assertEquals(0.1, actual2.getE(), 0);
        Assert.assertEquals(0.2, actual2.getI(), 0);
        Assert.assertEquals(0.3, actual2.getPerigeeArgument(), 0);
        Assert.assertEquals(0.4, actual2.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(0.5, actual2.getTrueAnomaly(), 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link Orbit#getJacobian(OrbitType,OrbitType)}
     * 
     * @description Validation of the jacobian related to the conversion between 2 orbit types
     * 
     * @input Cartesian Orbit (-29536113.0, 30329259.0, -100125.0, -2194.0, -2141.0, -8.0)
     * 
     * @output The jacobian of the conversion
     * 
     * @testPassCriteria the jacobian computed is the same as the one expected
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testgetJacobian() throws PatriusException {

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final AbsoluteDate dateP = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000,
                TimeScalesFactory.getUTC());
        final KeplerianOrbit orbKep = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                dateP, this.mu);

        RealMatrix jacobCartesianToCartesian = new Array2DRowRealMatrix(6, 6);
        RealMatrix jacobCartesianToEquinoctial = new Array2DRowRealMatrix(6, 6);
        RealMatrix jacobApsisToCartesian = new Array2DRowRealMatrix(6, 6);
        RealMatrix jacobKeplerianToKeplerian = new Array2DRowRealMatrix(6, 6);
        RealMatrix jacobEquinoctialToEquinoctial = new Array2DRowRealMatrix(6, 6);
        RealMatrix jacobEquatorialToEquatorial = new Array2DRowRealMatrix(6, 6);
        RealMatrix jacobTypeToAnotherType = new Array2DRowRealMatrix(6, 6);

        jacobCartesianToCartesian = orbKep.getJacobian(OrbitType.CARTESIAN, OrbitType.CARTESIAN);
        jacobCartesianToEquinoctial = orbKep
                .getJacobian(OrbitType.CARTESIAN, OrbitType.EQUINOCTIAL);
        jacobApsisToCartesian = orbKep.getJacobian(OrbitType.APSIS, OrbitType.CARTESIAN);
        jacobKeplerianToKeplerian = orbKep.getJacobian(OrbitType.KEPLERIAN, OrbitType.KEPLERIAN);
        jacobEquinoctialToEquinoctial = orbKep.getJacobian(OrbitType.EQUINOCTIAL,
                OrbitType.EQUINOCTIAL);
        jacobEquatorialToEquatorial = orbKep
                .getJacobian(OrbitType.EQUATORIAL, OrbitType.EQUATORIAL);
        jacobTypeToAnotherType = orbKep.getJacobian(OrbitType.CIRCULAR, OrbitType.APSIS);

        double[][] jac1 = new double[6][6];
        double[][] jac2 = new double[6][6];
        double[][] jac3 = new double[6][6];
        double[][] jac4 = new double[6][6];
        double[][] jac5 = new double[6][6];
        double[][] jac6 = new double[6][6];
        double[][] jac7 = new double[6][6];
        final double[][] convJacobianParameters1 = new double[6][6];
        final double[][] convJacobianParameters2 = new double[6][6];
        final double[][] convJacobianCartesian1 = new double[6][6];
        final double[][] convJacobianCartesian2 = new double[6][6];
        double[][] convJacobianTypeToAnotherType = new double[6][6];

        jac1 = jacobCartesianToCartesian.getData(false);
        jac2 = jacobCartesianToEquinoctial.getData(false);
        jac3 = jacobApsisToCartesian.getData(false);
        jac4 = jacobKeplerianToKeplerian.getData(false);
        jac5 = jacobEquinoctialToEquinoctial.getData(false);
        jac6 = jacobEquatorialToEquatorial.getData(false);
        jac7 = jacobTypeToAnotherType.getData(false);

        OrbitType.EQUINOCTIAL.convertType(orbKep).getJacobianWrtParameters(PositionAngle.MEAN,
                convJacobianParameters1);
        OrbitType.APSIS.convertType(orbKep).getJacobianWrtCartesian(PositionAngle.MEAN,
                convJacobianCartesian1);
        OrbitType.CIRCULAR.convertType(orbKep).getJacobianWrtCartesian(PositionAngle.MEAN,
                convJacobianCartesian2);
        OrbitType.APSIS.convertType(orbKep).getJacobianWrtParameters(PositionAngle.MEAN,
                convJacobianParameters2);
        final double[][] ident = MatrixUtils.createRealIdentityMatrix(6).getData();
        final RealMatrix produitJac = (new Array2DRowRealMatrix(convJacobianCartesian2))
                .multiply(new Array2DRowRealMatrix(convJacobianParameters2));
        convJacobianTypeToAnotherType = produitJac.getData();

        for (int i = 0; i < jac1.length; ++i) {
            final double[] rowI = ident[i];
            final double[] rowJP = convJacobianParameters1[i];
            final double[] rowJC = convJacobianCartesian1[i];
            final double[] rowTy2OthTy = convJacobianTypeToAnotherType[i];
            final double[] rowJ1 = jac1[i];
            final double[] rowJ2 = jac2[i];
            final double[] rowJ3 = jac3[i];
            final double[] rowJ4 = jac4[i];
            final double[] rowJ5 = jac5[i];
            final double[] rowJ6 = jac6[i];
            final double[] rowJ7 = jac7[i];

            for (int j = 0; j < rowJ1.length; ++j) {
                Assert.assertEquals(rowJ1[j], rowI[j], 1E-7);
                Assert.assertEquals(rowJ2[j], rowJP[j], 1E-7);
                Assert.assertEquals(rowJ3[j], rowJC[j], 1E-7);
                Assert.assertEquals(rowJ4[j], rowI[j], 1E-7);
                Assert.assertEquals(rowJ5[j], rowI[j], 1E-7);
                Assert.assertEquals(rowJ6[j], rowI[j], 1E-7);
                Assert.assertEquals(rowJ7[j], rowTy2OthTy[j], 1E-7);
            }

        }
    }


    /**
     * @testType UT
     * 
     * @testedMethod {@link Orbit#getKeplerianTransitionMatrix(double)}
     * 
     * @description test for the method in the case where input orbit
     *              is not of type keplerian, so that conversion have to be done
     *              to compute the transition matrix
     * 
     * @testPassCriteria the matrix computed after conversion is the same
     *                   than the one obtained with the orbit of same parameter but of
     *                   keplerian type
     * 
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     * @throws IOException
     * @throws PatriusException
     */
    @Test
    public void testGetKeplerianTransitionMatrix() throws IOException, PatriusException {

        // Load ephemeris
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7350036.7731690155, 0.01906887725038333,
                1.711148611453987, -1.9818436549702414, -2.0071033459669896, 0.002140087127289445,
                PositionAngle.TRUE, frame, date, this.mu);
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final OrbitType otherType = OrbitType.EQUINOCTIAL;

        // Start from a cartesian orbit : the transition
        // matrix computed is the reference
        final Orbit orbit1 = OrbitType.CARTESIAN.convertOrbit(orbit, frame);
        final RealMatrix mat1 = orbit1.getKeplerianTransitionMatrix(date.durationFrom(date));

        // Start from a keplerian orbit : conversion have to be done
        RealMatrix mat = orbit.getKeplerianTransitionMatrix(date.durationFrom(date));

        // Conversion
        final RealMatrix jac = orbit.getJacobian(otherType, orbitType);
        final RealMatrix jacInv = orbit.getJacobian(orbitType, otherType);
        mat = jacInv.multiply(mat).multiply(jac);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(mat.getEntry(i, j), mat1.getEntry(i, j), 1.0E-11);
            }
        }

    }

    @Test
    public void testSerialisation() {
        final KeplerianParameters param1 = new KeplerianParameters(10000E3, 0.1, 0.2, 0.3, 0.4,
                0.5, PositionAngle.TRUE, this.mu);
        final KeplerianParameters param2 = new KeplerianParameters(30000000, 0.1,
                MathLib.toRadians(179.9), MathLib.toRadians(20), MathLib.toRadians(30),
                MathLib.toRadians(60), PositionAngle.MEAN, this.mu);
        final KeplerianParameters param3 = new KeplerianParameters(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, this.mu);

        final KeplerianParameters[] params = { param1, param2, param3 };
        for (final KeplerianParameters param : params) {
            final KeplerianParameters paramBis = TestUtils.serializeAndRecover(param);
            assertEqualsKeplerianParameters(paramBis, param);
        }
    }

    @Test
    public void testEquals() {

        final KeplerianOrbit orbit1 = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);

        KeplerianOrbit orbit2 = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertTrue(orbit1.equals(orbit1));
        Assert.assertTrue(orbit1.equals(orbit2));
        Assert.assertEquals(orbit1.hashCode(), orbit2.hashCode());

        orbit2 = new KeplerianOrbit(24464000.0, 0.7311, 0.122138, 3.10686, 1.00681, 0.048363,
                PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertFalse(orbit1.equals(orbit2));
        Assert.assertFalse(orbit1.hashCode() == orbit2.hashCode());
        
        final EquatorialOrbit orbitEqui = new EquatorialOrbit(42166.712, 0.5, -0.5, 1.200e-04,
                -1.16e-04, 5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), this.date,
                this.mu);
        Assert.assertFalse(orbit1.equals(orbitEqui));
        
        // Parameters tests
        
        final KeplerianParameters keplerParam1 = new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686,
                1.00681, 0.048363, PositionAngle.MEAN, this.mu);

        KeplerianParameters keplerParam2 = new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.MEAN, this.mu);

        Assert.assertTrue(keplerParam1.equals(keplerParam1));
        Assert.assertTrue(keplerParam1.equals(keplerParam2));
        Assert.assertEquals(keplerParam1.hashCode(), keplerParam2.hashCode());

        keplerParam2 = new KeplerianParameters(24464000.0, 0.7311, 0.122138, 3.10686, 1.00681, 0.048363,
                PositionAngle.MEAN, this.mu);

        Assert.assertFalse(keplerParam1.equals(keplerParam2));
        Assert.assertFalse(keplerParam1.hashCode() == keplerParam2.hashCode());
        
        final EquatorialParameters equiParam = new EquatorialParameters(42166.712, 0.5, -0.5, 1.200e-04,
                -1.16e-04, 5.300, PositionAngle.MEAN, this.mu);
        Assert.assertFalse(keplerParam1.equals(equiParam));
        
        
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
    private static void checkDouble(final double expected, final double actual,
            final double threshold,
            final String tag) {
        Assert.assertEquals(expected, actual, threshold);
        Report.printToReport(tag, expected, actual);
    }

    public static void assertEqualsKeplerianParameters(final KeplerianParameters param1,
            final KeplerianParameters param2) {
        Assert.assertEquals(param1.getMu(), param2.getMu(), 0);
        Assert.assertEquals(param1.getA(), param2.getA(), 0);
        Assert.assertEquals(param1.getE(), param2.getE(), 0);
        Assert.assertEquals(param1.getI(), param2.getI(), 0);
        Assert.assertEquals(param1.getPerigeeArgument(), param2.getPerigeeArgument(), 0);
        Assert.assertEquals(param1.getRightAscensionOfAscendingNode(),
                param2.getRightAscensionOfAscendingNode(), 0);
        Assert.assertEquals(param1.getTrueAnomaly(), param2.getTrueAnomaly(), 0);
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
