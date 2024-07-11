/**
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013:new test added and different validation values frome Stela software
 * VERSION::FA:345:03/11/2014:coverage
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:524:10/03/2016:serialization
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/**
 */
package fr.cnes.sirius.patrius.stela.orbits;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.PVCoordinatesTest;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test class for the class StelaEquinoctialOrbit.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaEquinoctialOrbitTest {

    /** Epsilon for tests: */
    public static final double EPSILON = 1.e-11;

    /** Stela relative difference epsilon for doubles: */
    public static final double RELEPS = 1.e-7;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela equinoctial parameters conversion
         * 
         * @featureDescription test the conversion from/to equinoctial parameters
         * 
         * @coveredRequirements
         */
        STELA_EQUINOCTIAL_PARAMETERS_CONVERSION,
        /**
         * @featureTitle Stela equinoctial parameters jacobian computation
         * 
         * @featureDescription test the jacobian computation
         * 
         * @coveredRequirements
         */
        STELA_EQUINOCTIAL_PARAMETERS_JACOBIAN
    }

    /** The Stela equinoctial orbit used for test. */
    private static StelaEquinoctialOrbit equi;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(StelaEquinoctialOrbitTest.class.getSimpleName(), "STELA equinoctial parameters");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(Orbit)}
     * @testedMethod {@link StelaEquinoctialOrbit#getA()}
     * @testedMethod {@link StelaEquinoctialOrbit#getE()}
     * @testedMethod {@link StelaEquinoctialOrbit#getI()}
     * @testedMethod {@link StelaEquinoctialOrbit#getEquinoctialEx()}
     * @testedMethod {@link StelaEquinoctialOrbit#getEquinoctialEy()}
     * @testedMethod {@link StelaEquinoctialOrbit#getIx()}
     * @testedMethod {@link StelaEquinoctialOrbit#getIy()}
     * @testedMethod {@link StelaEquinoctialOrbit#getLM()}
     * @testedMethod {@link StelaEquinoctialOrbit#getEquinoctialParameters()}
     * 
     * @description test the construction of an equinoctial orbit from a keplerian orbit.
     * 
     * @input a keplerian orbit
     * 
     * @output the equinoctial orbit after the conversion
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testConstructor() {

        final KeplerianOrbit kepl = new KeplerianOrbit(7208670.3341911305, 1.4111983719431118E-4, 1.6999918855003606,
            -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(kepl);
        // The reference values have been generated from a keplerian-->equinoctial conversion in STELA:
        Assert.assertEquals(kepl.getA(), actual.getA(), 0.0);
        Assert.assertEquals(kepl.getE(), actual.getE(), 0.0);
        Assert.assertEquals(kepl.getI(), actual.getI(), 0.0);
        Assert.assertEquals(kepl.getHx(), actual.getHx(), 0.0);
        Assert.assertEquals(kepl.getHy(), actual.getHy(), 0.0);
        Assert.assertEquals(-1.256575691324651E-4, actual.getEquinoctialEx(), 0.0);
        Assert.assertEquals(6.422603653868609E-5, actual.getEquinoctialEy(), 0.0);
        Assert.assertEquals(-0.729462289821196, actual.getIx(), 0.0);
        Assert.assertEquals(0.17973032977932682, actual.getIy(), 0.0);
        Assert.assertEquals(4.9166137644093855, actual.getLM(), 0.0);

        // CHeck parameters
        final StelaEquinoctialParameters param = actual.getEquinoctialParameters();
        Assert.assertEquals(param.getA(), actual.getA(), 0.0);
        Assert.assertEquals(param.getEquinoctialEx(), actual.getEquinoctialEx(), 0.0);
        Assert.assertEquals(param.getEquinoctialEy(), actual.getEquinoctialEy(), 0.0);
        Assert.assertEquals(param.getIx(), actual.getIx(), 0.0);
        Assert.assertEquals(param.getIy(), actual.getIy(), 0.0);
        Assert.assertEquals(param.getLM(), actual.getLM(), 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(PVCoordinates, Frame, AbsoluteDate, double)}
     * 
     * @description test the construction of an equinoctial orbit from PV coordinates.
     * 
     * @input a keplerian orbit
     * 
     * @output the equinoctial orbit after the conversion
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testConstructor2() {
        final KeplerianOrbit kepl = new KeplerianOrbit(7208670.3341911305, 1.4111983719431118E-4, 1.6999918855003606,
            -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        final PVCoordinates keplPV = kepl.getPVCoordinates();
        final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(keplPV, FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        // Epsilons are worse because of numeric losses with intermediate PV object in Stela.
        // But the relative difference epsilon from Stela is still in check,
        // so it's good enough.
        Assert.assertEquals(0., relDel(kepl.getA(), actual.getA()), RELEPS);
        Assert.assertEquals(0., relDel(kepl.getE(), actual.getE()), RELEPS);
        Assert.assertEquals(0., relDel(kepl.getI(), actual.getI()), RELEPS);
        Assert.assertEquals(0., relDel(kepl.getHx(), actual.getHx()), RELEPS);
        Assert.assertEquals(0., relDel(kepl.getHy(), actual.getHy()), RELEPS);
        Assert.assertEquals(0., relDel(-1.256575691324651E-4, actual.getEquinoctialEx()), RELEPS);
        Assert.assertEquals(0., relDel(6.422603653868609E-5, actual.getEquinoctialEy()), RELEPS);
        Assert.assertEquals(0., relDel(-0.729462289821196, actual.getIx()), RELEPS);
        Assert.assertEquals(0., relDel(0.17973032977932682, actual.getIy()), RELEPS);
        Assert.assertEquals(0., relDel(4.916613764409386 - 2 * FastMath.PI, actual.getLM()), RELEPS);

        // test inclination null

        final KeplerianOrbit kepl2 = new KeplerianOrbit(7208670.3341911305, 1.4111983719431118E-4, 0,
            -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        final PVCoordinates keplPV2 = kepl2.getPVCoordinates();
        final StelaEquinoctialOrbit actual2 = new StelaEquinoctialOrbit(keplPV2, FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        // Epsilons are worse because of numeric losses with intermediate PV object in Stela.
        // But the relative difference epsilon from Stela is still in check,
        // so it's good enough.
        Assert.assertEquals(0., relDel(kepl2.getA(), actual2.getA()), RELEPS);
        Assert.assertEquals(0., relDel(kepl2.getE(), actual2.getE()), RELEPS);
        Assert.assertEquals(kepl2.getI(), actual2.getI(), 0);
        Assert.assertEquals(0, actual2.getIx(), 0);
        Assert.assertEquals(0, actual2.getIy(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(Orbit, Boolean)}
     * @testedMethod {@link StelaEquinoctialOrbit#getA()}
     * @testedMethod {@link StelaEquinoctialOrbit#getE()}
     * @testedMethod {@link StelaEquinoctialOrbit#getI()}
     * @testedMethod {@link StelaEquinoctialOrbit#getEquinoctialEx()}
     * @testedMethod {@link StelaEquinoctialOrbit#getEquinoctialEy()}
     * @testedMethod {@link StelaEquinoctialOrbit#getIx()}
     * @testedMethod {@link StelaEquinoctialOrbit#getIy()}
     * @testedMethod {@link StelaEquinoctialOrbit#getLM()}
     * 
     * @description test the construction of an equinoctial orbit from a keplerian orbit with boolean.
     * 
     * @input a keplerian orbit
     * 
     * @output the equinoctial orbit after the conversion
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testConstructor3() {

        final KeplerianOrbit kepl = new KeplerianOrbit(7208670.3341911305, 1.4111983719431118E-4, 1.6999918855003606,
            -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        final StelaEquinoctialOrbit expected = new StelaEquinoctialOrbit(kepl);
        final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(kepl, true);

        // The reference values have been generated from a keplerian-->equinoctial conversion in STELA:
        Assert.assertEquals(expected.getA(), actual.getA(), 0.0);
        Assert.assertEquals(expected.getE(), actual.getE(), 0.0);
        Assert.assertEquals(expected.getI(), actual.getI(), 0.0);
        Assert.assertEquals(expected.getHx(), actual.getHx(), 0.0);
        Assert.assertEquals(expected.getHy(), actual.getHy(), 0.0);
        Assert.assertEquals(expected.getEquinoctialEx(), actual.getEquinoctialEx(), 0.0);
        Assert.assertEquals(expected.getEquinoctialEy(), actual.getEquinoctialEy(), 0.0);
        Assert.assertEquals(expected.getIx(), actual.getIx(), 0.0);
        Assert.assertEquals(expected.getIy(), actual.getIy(), 0.0);
        Assert.assertEquals(expected.getLM(), actual.getLM(), 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(Orbit)}
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(PVCoordinates, Frame, AbsoluteDate, double)}
     * 
     * @description test the hyperbolic orbit exception
     * 
     * @input a keplerian orbit
     * 
     * @output OrekitException
     * 
     * @testPassCriteria OrekitException
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testConstructorsExceptions() {
        final PatriusException refException = new PatriusException
            (PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                StelaEquinoctialParameters.class.getName());
        final String refMsg = refException.getMessage();

        try {
            final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(-6497631.848597864, 0.6,
                0.8, -0.84, -0.09, 5.75, FramesFactory.getEME2000(),
                new AbsoluteDate(),
                3.9860044144982E14);
            Assert.fail(actual.toString());
        } catch (final IllegalArgumentException e) {
            final String msg = e.getMessage();
            Assert.assertEquals(refMsg, msg);
        }

        final KeplerianOrbit kepl = new KeplerianOrbit(-7208670.3341911305, 1.2, 1.6999918855003606,
            -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.MEAN,
            FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
        try {
            final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(kepl);
            Assert.fail(actual.toString());
        } catch (final IllegalArgumentException e) {
            final String msg = e.getMessage();
            Assert.assertEquals(refMsg, msg);
        }

        try {
            final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(kepl, true);
            Assert.fail(actual.toString());
        } catch (final IllegalArgumentException e) {
            final String msg = e.getMessage();
            Assert.assertEquals(refMsg, msg);
        }

        final PVCoordinates keplPV = kepl.getPVCoordinates();
        try {
            final StelaEquinoctialOrbit actual = new StelaEquinoctialOrbit(keplPV, FramesFactory.getEME2000(),
                new AbsoluteDate(), 3.9860044144982E14);
            Assert.fail(actual.toString());
        } catch (final IllegalArgumentException e) {
            final String msg = e.getMessage();
            final PatriusException refException2 = new PatriusException
                (PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                    CartesianParameters.class.getName());
            final String refMsg2 = refException2.getMessage();
            Assert.assertEquals(refMsg2, msg);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(double, double, double, double, double, double, Frame, AbsoluteDate, double)}
     * @testedMethod {@link StelaEquinoctialOrbit#initPVCoordinates()}
     * 
     * @description test the computation of PV coordinates from an equinoctial orbit.
     * 
     * @input an equinoctial orbit
     * 
     * @output the corresponding PV coordinates
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testPVCoordinatesComputation() throws PatriusException {

        Report.printMethodHeader("testPVCoordinatesComputation", "Equinoctial to cartesian", "STELA", EPSILON,
            ComparisonType.RELATIVE);

        final PVCoordinates pvs = equi.getPVCoordinates();
        // The reference values have been generated from an equinoctial-->cartesian conversion in STELA:
        this.checkDouble(4643107.10407, pvs.getPosition().getX(),
            EPSILON * MathLib.abs(pvs.getPosition().getX()), "X");
        this.checkDouble(1432369.73327, pvs.getPosition().getY(),
            EPSILON * MathLib.abs(pvs.getPosition().getY()), "Y");
        this.checkDouble(-4945115.63544, pvs.getPosition().getZ(),
            EPSILON * MathLib.abs(pvs.getPosition().getZ()), "Z");
        this.checkDouble(4643107.10407, pvs.getPosition().getX(),
            EPSILON * MathLib.abs(pvs.getPosition().getX()), "VX");
        this.checkDouble(1432369.73327, pvs.getPosition().getY(),
            EPSILON * MathLib.abs(pvs.getPosition().getY()), "VY");
        this.checkDouble(3850.91197413, pvs.getVelocity().getZ(),
            EPSILON * MathLib.abs(pvs.getVelocity().getZ()), "VZ");

        // Check at other date
        final PVCoordinates pv2 = equi.getPVCoordinates(equi.getDate().shiftedBy(10), equi.getFrame());
        Assert.assertFalse(pv2.getPosition().subtract(pvs.getPosition()).getNorm() == 0);
        Assert.assertFalse(pv2.getVelocity().subtract(pvs.getVelocity()).getNorm() == 0);

        final Orbit orbit3 = equi.orbitShiftedBy(10);
        final PVCoordinates pv3 = orbit3.getPVCoordinates(orbit3.getDate(), orbit3.getFrame());
        Assert.assertEquals(pv2.getPosition().subtract(pv3.getPosition()).getNorm(), 0, 0);
        Assert.assertEquals(pv2.getVelocity().subtract(pv3.getVelocity()).getNorm(), 0, 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_JACOBIAN}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#getJacobianWrtCartesian(PositionAngle, double[][])}
     * @testedMethod {@link StelaEquinoctialOrbit#computeJacobianMeanWrtCartesian()}
     * 
     * @description test the computation of the jacobian with respect to cartesian parameters.
     * 
     * @input an equinoctial orbit
     * 
     * @output the jacobian with respect to cartesian parameters
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testJacobianWrtCartesian() {
        final double[][] jacobian = new double[6][6];
        // the jacobian computation using true anomaly is not implemented:
        boolean rez = false;
        try {
            equi.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
        // the jacobian computation using eccentric anomaly is not implemented:
        rez = false;
        try {
            equi.getJacobianWrtCartesian(PositionAngle.ECCENTRIC, jacobian);
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);

        equi.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);
        // The reference values have been generated using the JacobianConverter class of STELA:
        // da/d(x,y,z) and da/d(vx,vy,vz):
        Assert.assertEquals(1.3655289406533975, jacobian[0][0], EPSILON);
        Assert.assertEquals(0.4212572057153177, jacobian[0][1], EPSILON);
        Assert.assertEquals(-1.4543490735218583, jacobian[0][2], EPSILON);
        Assert.assertEquals(475.3145739087273, jacobian[0][3], EPSILON);
        Assert.assertEquals(1544.798153449275, jacobian[0][4], EPSILON);
        Assert.assertEquals(946.786140257342, jacobian[0][5], EPSILON);
        // dex/d(x,y,z) and dex/d(vx,vy,vz):
        Assert.assertEquals(-7.145027992895271E-8, jacobian[1][0], EPSILON);
        Assert.assertEquals(-9.45671714477089E-8, jacobian[1][1], EPSILON);
        Assert.assertEquals(-9.650104897354529E-8, jacobian[1][2], EPSILON);
        Assert.assertEquals(-2.201781470629032E-4, jacobian[1][3], EPSILON);
        Assert.assertEquals(-2.4150355935743516E-5, jacobian[1][4], EPSILON);
        Assert.assertEquals(1.551350204578595E-4, jacobian[1][5], EPSILON);
        // dey/d(x,y,z) and dey/d(vx,vy,vz):
        Assert.assertEquals(9.738755528522957E-8, jacobian[2][0], EPSILON);
        Assert.assertEquals(2.7166420777097842E-8, jacobian[2][1], EPSILON);
        Assert.assertEquals(-1.0476741492351928E-7, jacobian[2][2], EPSILON);
        Assert.assertEquals(6.807961979623164E-5, jacobian[2][3], EPSILON);
        Assert.assertEquals(2.178270300297071E-4, jacobian[2][4], EPSILON);
        Assert.assertEquals(1.3453570127402743E-4, jacobian[2][5], EPSILON);
        // dix/d(x,y,z) and dix/d(vx,vy,vz):
        Assert.assertEquals(-4.086021967874539E-8, jacobian[3][0], EPSILON);
        Assert.assertEquals(-1.1989140616488395E-7, jacobian[3][1], EPSILON);
        Assert.assertEquals(-6.894950666356124E-8, jacobian[3][2], EPSILON);
        Assert.assertEquals(-9.124220703764643E-5, jacobian[3][3], EPSILON);
        Assert.assertEquals(-3.370179314123917E-5, jacobian[3][4], EPSILON);
        Assert.assertEquals(9.015643005720976E-5, jacobian[3][5], EPSILON);
        // diy/d(x,y,z) and diy/d(vx,vy,vz):
        Assert.assertEquals(5.857858120158332E-9, jacobian[4][0], EPSILON);
        Assert.assertEquals(-4.393393590118749E-9, jacobian[4][1], EPSILON);
        Assert.assertEquals(4.227544953185063E-9, jacobian[4][2], EPSILON);
        Assert.assertEquals(4.800222900523306E-5, jacobian[4][3], EPSILON);
        Assert.assertEquals(-3.600167175392479E-5, jacobian[4][4], EPSILON);
        Assert.assertEquals(3.46426247973417E-5, jacobian[4][5], EPSILON);
        // dL/d(x,y,z) and dL/d(vx,vy,vz):
        Assert.assertEquals(-4.8324350182950007E-8, jacobian[5][0], EPSILON);
        Assert.assertEquals(3.6243262637212483E-8, jacobian[5][1], EPSILON);
        Assert.assertEquals(-3.487509573317488E-8, jacobian[5][2], EPSILON);
        Assert.assertEquals(-8.082086259815776E-6, jacobian[5][3], EPSILON);
        Assert.assertEquals(6.061564694861816E-6, jacobian[5][4], EPSILON);
        Assert.assertEquals(-5.8327433471480214E-6, jacobian[5][5], EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#kepEq(double, double)}
     * 
     * @description test kepler equation solver.
     * 
     * @input an eccentricity and an eccentric anomaly
     * 
     * @output the mean anomaly
     * 
     * @testPassCriteria the output values are the expected ones (the reference values are in STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testKeplerEquation() {

        Report.printMethodHeader("testKeplerEquation", "Kepler equation", "STELA", 3e-15, ComparisonType.RELATIVE);

        double e = 0.;
        double m = -FastMath.PI;
        double eccAnom;
        double computedM;
        for (int i = 0; i < 10; i++) {
            e = i / 10.;
            for (int j = 0; j < 360; j++) {
                m = MathLib.toRadians(j);
                eccAnom = equi.kepEq(e, m);
                computedM = eccAnom - e * MathLib.sin(eccAnom);
                double diffRel = 0;
                if (m != 0) {
                    diffRel = MathLib.abs(computedM - m) / m;

                } else {
                    diffRel = MathLib.abs(computedM - m);
                }

                Assert.assertEquals(diffRel, 0, 3e-15);
            }
        }

        m = MathLib.toRadians(-200);
        eccAnom = equi.kepEq(e, m);
        computedM = eccAnom - e * MathLib.sin(eccAnom);

        m = JavaMathAdapter.mod(m, 2 * FastMath.PI);
        final double diffRel = MathLib.abs(computedM - m) / m;
        Assert.assertEquals(0, diffRel, 3e-15);

        Report.printToReport("Mean anomaly", m, computedM);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_JACOBIAN}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#getJacobianWrtParameters(PositionAngle, double[][])}
     * @testedMethod {@link StelaEquinoctialOrbit#computeJacobianMeanWrtCartesian()}
     * 
     * @description test the computation of the jacobian with respect to equinoctial parameters.
     * 
     * @input an equinoctial orbit
     * 
     * @output the jacobian of the cartesian parameters with respect to the orbital parameters
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testJacobianWrtParameters() {
        final double[][] jacobian = new double[6][6];
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(7000000, 0.01, 0.02, 0.3, 0.4, 0.0,
            FramesFactory.getEME2000(), new AbsoluteDate(), 3.9860044144982E14);
        orbit.getJacobianWrtParameters(PositionAngle.MEAN, jacobian);
        // The reference values have been generated using the JacobianConverter class of STELA:
        // d(x,y,z,Vx,Vy,Vz)/da:
        Assert.assertEquals((0.6633010148675095 - jacobian[0][0]) / jacobian[0][0], 0, EPSILON);
        Assert.assertEquals((1793374.5513522457 - jacobian[0][1]) / jacobian[0][1], 0, EPSILON);
        Assert.assertEquals((-4780047.329169823 - jacobian[0][2]) / jacobian[0][2], 0, EPSILON);
        Assert.assertEquals((-3560290.560600666 - jacobian[0][3]) / jacobian[0][3], 0, EPSILON);
        Assert.assertEquals((-224534.85816420987 - jacobian[0][4]) / jacobian[0][4], 0, EPSILON);
        Assert.assertEquals((-1.1251854229556866E7 - jacobian[0][5]) / jacobian[0][5], 0, EPSILON);
        // d(x,y,z,Vx,Vy,Vz)/dex:
        Assert.assertEquals((0.20462424761024872 - jacobian[1][0]) / jacobian[1][0], 0, EPSILON);
        Assert.assertEquals((5828564.591633714 - jacobian[1][1]) / jacobian[1][1], 0, EPSILON);
        Assert.assertEquals((-1739499.9265580082 - jacobian[1][2]) / jacobian[1][2], 0, EPSILON);
        Assert.assertEquals((-1.1572442617458265E7 - jacobian[1][3]) / jacobian[1][3], 0, EPSILON);
        Assert.assertEquals((5878528.830213177 - jacobian[1][4]) / jacobian[1][4], 0, EPSILON);
        Assert.assertEquals((4156294.907225133 - jacobian[1][5]) / jacobian[1][5], 0, EPSILON);
        // d(x,y,z,Vx,Vy,Vz)/dey:
        Assert.assertEquals((-0.7064450907771179 - jacobian[2][0]) / jacobian[2][0], 0, EPSILON);
        Assert.assertEquals((3572249.332789418 - jacobian[2][1]) / jacobian[2][1], 0, EPSILON);
        Assert.assertEquals((4815686.517828362 - jacobian[2][2]) / jacobian[2][2], 0, EPSILON);
        Assert.assertEquals((-7093151.836133245 - jacobian[2][3]) / jacobian[2][3], 0, EPSILON);
        Assert.assertEquals((1491914.0261625722 - jacobian[2][4]) / jacobian[2][4], 0, EPSILON);
        Assert.assertEquals((-9360794.912021628 - jacobian[2][5]) / jacobian[2][5], 0, EPSILON);
        // d(x,y,z,Vx,Vy,Vz)/dix:
        Assert.assertEquals((-1.380908155885945E-4 - jacobian[3][0]) / jacobian[3][0], 0, EPSILON);
        Assert.assertEquals((-5152.177075414166 - jacobian[3][1]) / jacobian[3][1], 0, EPSILON);
        Assert.assertEquals((2089.5498400562046 - jacobian[3][2]) / jacobian[3][2], 0, EPSILON);
        Assert.assertEquals((5135.701424242863 - jacobian[3][3]) / jacobian[3][3], 0, EPSILON);
        Assert.assertEquals((6093.7856304603465 - jacobian[3][4]) / jacobian[3][4], 0, EPSILON);
        Assert.assertEquals((4322.9610367223895 - jacobian[3][5]) / jacobian[3][5], 0, EPSILON);
        // d(x,y,z,Vx,Vy,Vz)/diy:
        Assert.assertEquals((-4.4880264279573503E-4 - jacobian[4][0]) / jacobian[4][0], 0, EPSILON);
        Assert.assertEquals((-1589.4146608866224 - jacobian[4][1]) / jacobian[4][1], 0, EPSILON);
        Assert.assertEquals((6331.982897773967 - jacobian[4][2]) / jacobian[4][2], 0, EPSILON);
        Assert.assertEquals((1469.9950091970748 - jacobian[4][3]) / jacobian[4][3], 0, EPSILON);
        Assert.assertEquals((-9016.989352629082 - jacobian[4][4]) / jacobian[4][4], 0, EPSILON);
        Assert.assertEquals((92.76681979607953 - jacobian[4][5]) / jacobian[4][5], 0, EPSILON);
        // d(x,y,z,Vx,Vy,Vz)/dL:
        Assert.assertEquals((-2.750651410095833E-4 - jacobian[5][0]) / jacobian[5][0], 0, EPSILON);
        Assert.assertEquals((5487.2978031968805 - jacobian[5][1]) / jacobian[5][1], 0, EPSILON);
        Assert.assertEquals((3685.0244646212136 - jacobian[5][2]) / jacobian[5][2], 0, EPSILON);
        Assert.assertEquals((-5588.573013752782 - jacobian[5][3]) / jacobian[5][3], 0, EPSILON);
        Assert.assertEquals((11653.068113334411 - jacobian[5][4]) / jacobian[5][4], 0, EPSILON);
        Assert.assertEquals((-2321.6144614438786 - jacobian[5][5]) / jacobian[5][5], 0, EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#addKeplerContribution(PositionAngle, double, double[])}
     * 
     * @description test the keplerian contribution of a Stela equinoctial orbit
     * 
     * @input an equinoctial orbit
     * 
     * @output the corresponding keplerian contribution
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testAddKeplerContribution() {
        final double[] yDot = new double[6];
        equi.addKeplerContribution(PositionAngle.MEAN, 3.9860044144982E14, yDot);
        // The reference values have been generated from an equinoctial-->cartesian conversion in STELA:
        Assert.assertEquals(0.0, yDot[0], 0.0);
        for (int i = 2; i < 6; i++) {
            Assert.assertEquals(0.0, yDot[i], 0.0);
        }
        Assert.assertEquals(MathLib.sqrt(3.9860044144982E14 / equi.getA()) / equi.getA(), yDot[1], 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#mapOrbitToArray()}
     * 
     * @description test the mapping to an array of a Stela equinoctial orbit
     * 
     * @input an equinoctial orbit
     * 
     * @output the corresponding array
     * 
     * @testPassCriteria the output values are the expected ones
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testMapOrbitToArray() {
        final double[] map = equi.mapOrbitToArray();
        Assert.assertEquals(6, map.length);
        // No differences expected
        Assert.assertEquals(equi.getA(), map[0], 0.);
        Assert.assertEquals(equi.getEquinoctialEx(), map[2], 0.);
        Assert.assertEquals(equi.getEquinoctialEy(), map[3], 0.);
        Assert.assertEquals(equi.getIx(), map[4], 0.);
        Assert.assertEquals(equi.getIy(), map[5], 0.);
        Assert.assertEquals(equi.getLM(), map[1], 0.);
    }

    /**
     * 
     * @testType UT
     * 
     * @throws PatriusException
     * 
     * @description test exception throwing when periapsis is below Earth surface
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(Orbit)}
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(Orbit, boolean)}
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void negativePeriapsisTest() throws PatriusException {
        final double mu = 3.9860044144982E14;
        final Orbit or = new ApsisOrbit(100, 36000, 10, 20, 30, 40, PositionAngle.MEAN, FramesFactory.getEME2000(),
            new AbsoluteDate(), mu);
        try {

            new StelaEquinoctialOrbit(or);
            Assert.assertTrue(false);

        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        try {

            new StelaEquinoctialOrbit(or, true);
            Assert.assertTrue(false);

        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        try {

            new StelaEquinoctialOrbit(24250000, 0.48, 0.57, 0.08, 0.03, MathLib.toRadians(90),
                FramesFactory.getEME2000(), new AbsoluteDate(), mu, true);
            Assert.assertTrue(false);

        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        try {

            new StelaEquinoctialOrbit(new KeplerianOrbit(-42500000, 1.1, MathLib.toRadians(10), 0, 0, 0,
                PositionAngle.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(), mu), true);
            Assert.assertTrue(false);

        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(Orbit, Boolean)}
     * @testedMethod {@link StelaEquinoctialParameters#correctInclination(double, double)}
     * 
     * @description test the inclination correction around 180°
     * 
     * @input an equinoctial orbit
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testInclinationCorrection() {
        // Check 1
        final double mu = 3.9860044144982E14;
        final Orbit kep1 = new KeplerianOrbit(30000000, 0.1, MathLib.toRadians(179.9), MathLib.toRadians(20),
            MathLib.toRadians(30), MathLib.toRadians(60), PositionAngle.MEAN, FramesFactory.getEME2000(),
            new AbsoluteDate(), mu);
        final StelaEquinoctialOrbit eqor = new StelaEquinoctialOrbit(kep1, true);

        Assert.assertEquals(MathLib.toRadians(179.5), eqor.getI(), 1e-14);

        // Check 2
        final KeplerianParameters params = new KeplerianParameters(30000000, 0.1, MathLib.toRadians(179.9),
            MathLib.toRadians(20),
            MathLib.toRadians(30), MathLib.toRadians(60), PositionAngle.MEAN, mu);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(params, FramesFactory.getEME2000(),
            new AbsoluteDate());
        Assert.assertEquals(MathLib.toRadians(179.5), orbit.getI(), 1e-14);

        // Check 3
        final double[] actual = ((StelaEquinoctialParameters) orbit.getParameters()).correctInclination(0.3, 0.2);
        final double[] expected = orbit.getEquinoctialParameters().correctInclination(0.3, 0.2);
        Assert.assertEquals(actual[0], expected[0], 0);
        Assert.assertEquals(actual[1], expected[1], 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_EQUINOCTIAL_PARAMETERS_CONVERSION}
     * 
     * @testedMethod {@link StelaEquinoctialOrbit#StelaEquinoctialOrbit(double, double, double, double, double, double, Frame, AbsoluteDate, double)}
     * @testedMethod {@link StelaEquinoctialOrbit#initPVCoordinates()}
     * 
     * @description test the not implemented methods for coverage purposes.
     * 
     * @input an equinoctial orbit
     * 
     * @output an exception
     * 
     * @testPassCriteria the output values are the expected ones (the reference values have been generated with STELA)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testNotImplementedMethods() {
        boolean rez = false;
        try {
            equi.interpolate(new AbsoluteDate(), null);
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            equi.getType();
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            equi.getLE();
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            equi.getLv();
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        final double gm = 3.9860044144982E14;
        final double[] pDot = new double[6];
        try {
            equi.addKeplerContribution(PositionAngle.TRUE, gm, pDot);
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
        rez = false;
        try {
            equi.addKeplerContribution(PositionAngle.ECCENTRIC, gm, pDot);
        } catch (final IllegalArgumentException e) {
            // should pass here:
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    @Test
    public void serialisationTest() {
        // init
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(7000000, 0.01, 0.02, 0.3, 0.4, 0.0,
            FramesFactory.getEME2000(), new AbsoluteDate(), 3.9860044144982E14);

        // serialisation
        final StelaEquinoctialOrbit orbit2 = TestUtils.serializeAndRecover(orbit);

        // test equals
        this.assertEqualsStelaOrbit(orbit, orbit2);
    }
    
    @Test
    public void testEquals() throws PatriusException {

        final KeplerianOrbit kepl1 = new KeplerianOrbit(7208670.3341911305, 1.4111983719431118E-4, 1.6999918855003606,
                -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.MEAN,
                FramesFactory.getEME2000(),
                new AbsoluteDate(), 3.9860044144982E14);
        final StelaEquinoctialOrbit stelaOrbit1 = new StelaEquinoctialOrbit(kepl1);
        
        StelaEquinoctialOrbit stelaOrbit2 = new StelaEquinoctialOrbit(kepl1);

        Assert.assertTrue(stelaOrbit1.equals(stelaOrbit1));
        Assert.assertTrue(stelaOrbit1.equals(stelaOrbit2));
        Assert.assertEquals(stelaOrbit1.hashCode(), stelaOrbit2.hashCode());
        
        final KeplerianOrbit kepl2 = new KeplerianOrbit(7208670.3341911305, 1.4111983719431118E-4, 1.6999918855003606,
                -0.23092789737694353, 2.9000169475307023, 2.2475247142556274, PositionAngle.TRUE,
                FramesFactory.getEME2000(),
                new AbsoluteDate(), 3.9860044144982E14);

        stelaOrbit2 = new StelaEquinoctialOrbit(kepl2);

        Assert.assertFalse(stelaOrbit1.equals(stelaOrbit2));
        Assert.assertFalse(stelaOrbit1.hashCode() == stelaOrbit2.hashCode());
        Assert.assertFalse(stelaOrbit1.equals(kepl1));
    }

    private void assertEqualsStelaOrbit(final StelaEquinoctialOrbit orbit1, final StelaEquinoctialOrbit orbit2) {
        this.assertEqualsOrbit(orbit1, orbit2);
        final StelaEquinoctialParameters param1 = orbit1.getEquinoctialParameters();
        final StelaEquinoctialParameters param2 = orbit2.getEquinoctialParameters();
        Assert.assertEquals(param1.getA(), param2.getA(), 0);
        Assert.assertEquals(param1.getEquinoctialEx(), param2.getEquinoctialEx(), 0);
        Assert.assertEquals(param1.getEquinoctialEy(), param2.getEquinoctialEy(), 0);
        Assert.assertEquals(param1.getIx(), param2.getIx(), 0);
        Assert.assertEquals(param1.getIy(), param2.getIy(), 0);
        Assert.assertEquals(param1.getLM(), param2.getLM(), 0);
        Assert.assertEquals(param1.getMu(), param2.getMu(), 0);
    }

    private void assertEqualsOrbit(final Orbit orbit1, final Orbit orbit2) {
        Assert.assertEquals(orbit1.getDate(), orbit2.getDate());
        Assert.assertEquals(orbit1.getMu(), orbit2.getMu(), 0);
        PVCoordinatesTest.assertEqualsPVCoordinates(orbit1.getPVCoordinates(), orbit2.getPVCoordinates());
        Assert.assertEquals(orbit1.getFrame().getName(), orbit2.getFrame().getName());
    }

    /**
     * Set up method before running the test.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        equi = new StelaEquinoctialOrbit(7000000, 0.01, 0.02, 0.3, 0.4, 0.0, FramesFactory.getEME2000(),
            new AbsoluteDate(), 3.9860044144982E14);
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
    private void checkDouble(final double expected, final double actual, final double threshold, final String tag) {
        Assert.assertEquals(expected, actual, threshold);
        Report.printToReport(tag, expected, actual);
    }

    /**
     * Relative difference.
     * 
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @return relative difference
     */
    private static double relDel(final double expected, final double actual) {
        final double rez = (MathLib.abs(expected - actual) / expected);
        return rez;
    }

}
