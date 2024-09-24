/**
 *
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
 * @history Created 22/02/2013
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Ajout des courtes periodes dues a la traînee atmospherique et a la pression de radiation solaire dans STELA
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:316:26/02/2015:take into account Sun-Satellite direction in PRS computation
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.radiation;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link SRPSquaring}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class SRPSquaringTest {

    /** holders */
    private int quadPoints;
    /** holders */
    private SRPSquaring srp;
    /** holders */
    private StelaEquinoctialOrbit orbit;
    /** holders */
    private OrbitNatureConverter converter;
    /** holders */
    private SRPSquaring srp2;
    /** sun */
    CelestialBody sun;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle SRP Perturbation
         * 
         * @featureDescription Validation of inner methods, including acceleration computation
         * 
         * @coveredRequirements
         */
        SRP_INNER,

        /**
         * @featureTitle SRP Perturbation
         * 
         * @featureDescription Validation of perturbations, quadratic approximation
         * 
         * @coveredRequirements
         */
        SRP_PERTURBATION,

        /**
         * @featureTitle SRP Perturbation
         * 
         * @featureDescription Validation of short periods
         * 
         * @coveredRequirements
         */
        SRP_SHORT,

        /**
         * @featureTitle SRP Perturbation
         * 
         * @featureDescription Validation of partial derivatives
         * 
         * @coveredRequirements
         */
        SRP_DV;
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SRPSquaringTest.class.getSimpleName(), "STELA SRP force");
    }

    /**
     * @throws PatriusException
     *         if perturbation computation fails
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_PERTURBATION}
     * 
     * @testedMethod {@link SRPSquaring#computePerturbation(StelaEquinoctialOrbit, OrbitNatureConverter)}
     * 
     * @description tests inner method
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria references from Stela : 1e-13 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputePerturbation() throws PatriusException {
        // TU Satlight 05/09/2013

        Report.printMethodHeader("testComputePerturbation", "SRP computation", "STELA", 1e-13, ComparisonType.RELATIVE);

        // Test
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(86400. * 22370 + 35);
        final StelaEquinoctialOrbit orbit2 = new StelaEquinoctialOrbit(new KeplerianOrbit(24350500.0, 0.72887620377405,
            MathLib.toRadians(12.0),
            MathLib.toRadians(30), MathLib.toRadians(30), MathLib.toRadians(50), PositionAngle.MEAN,
            FramesFactory.getMOD(false), date, Constants.CNES_STELA_MU));

        final double[] derivatives = this.srp.computePerturbation(orbit2, this.converter);

        // Comparison
        final double tol = 1e-13;
        final double[] expDeriv = { -0.0000907383609692599, -2.0674895257307548E-11, -4.068115887354839E-12,
            1.952497050317277E-11, 1.0157572975303943E-12, 2.0793740493786304E-12 };
        Report.printToReport("Perturbation", expDeriv, derivatives);
        checkDouble(expDeriv, derivatives, tol, ComparisonTypes.RELATIVE);
    }

    /**
     * @throws NoSuchMethodException
     *         if fail
     * @throws IllegalAccessException
     *         if fail
     * @throws PatriusException
     * @throws IllegalArgumentException
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#getFlux(Orbit)}
     * 
     * @description tests inner method
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria 1e-14 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetFlux() throws IllegalArgumentException, PatriusException {
        final Vector3D result = this.srp.getFlux(this.orbit,
            this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));

        final double[] exp = { 4.4861005171844035E-6, 7.945057080666681E-7, 3.444362761606319E-7 };
        checkDouble(exp, result.negate().toArray(), 1e-12, ComparisonTypes.RELATIVE);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_DV}
     * 
     * @testedMethod {@link SRPSquaring#computePartialDerivatives(StelaEquinoctialOrbit)}
     * 
     * @description tests partial derivatives method
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria 1e-14 absolute scale because not implemented yet
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeSRPDvs() throws PatriusException {

        final double[][] result = this.srp.computePartialDerivatives(this.orbit);

        // Comparison
        final double tol = 1e-14;
        final double[][] expected = new double[6][6];

        checkDouble(expected, result, tol, ComparisonTypes.ABSOLUTE);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_SHORT}
     * 
     * @testedMethod {@link SRPSquaring#computeAcceleration(StelaEquinoctialOrbit)}
     * 
     * @description tests inner methods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria Stela threshold : 1e-12 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeSRP() throws PatriusException {

        final Vector3D result =
            this.srp.computeAcceleration(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));

        // Comparison
        final double tol = 1e-12;
        final double[] expectedFxyz = { -8.354804412937963E-8, -1.4796680926859718E-8, -6.414697372516489E-9 };

        checkDouble(expectedFxyz[0], result.getX(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expectedFxyz[1], result.getY(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expectedFxyz[2], result.getZ(), tol, ComparisonTypes.RELATIVE);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#computeSunBetaPhi(StelaEquinoctialOrbit)}
     * 
     * @description tests inner methods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria Stela threshold : 1e-11 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeSunBetaPhi() throws PatriusException {
        // TU Satlight 28/11/2011

        // Test
        final double[] betaPhi =
            this.srp.computeSunBetaPhi(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));

        // Comparison
        final double tol = 1e-11;
        final double[] expBetaPhi = { 0.14500802250842973, -0.3263570624864882 };

        checkDouble(expBetaPhi[0], betaPhi[0], tol, ComparisonTypes.RELATIVE);
        checkDouble(expBetaPhi[1], betaPhi[1], tol, ComparisonTypes.RELATIVE);

        // Test
        final double mu = 3.9860044144982E14;
        final StelaEquinoctialOrbit orbitEq = new StelaEquinoctialOrbit(new KeplerianOrbit(42500000, 0.5, 0, 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(), mu));
        final double[] betaPhiEq = this.srp.computeSunBetaPhi(orbitEq,
            this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));

        // Comparison
        final double[] expBetaPhiEq = { 0.07545849537773242, 0.17528627635107336 };

        checkDouble(expBetaPhiEq[0], betaPhiEq[0], tol, ComparisonTypes.RELATIVE);
        checkDouble(expBetaPhiEq[1], betaPhiEq[1], tol, ComparisonTypes.RELATIVE);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#computeInOutTrueAnom(StelaEquinoctialOrbit)}
     * 
     * @description tests inner methods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria Stela threshold : 1e-11 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeInOutTrueAnom() throws PatriusException {

        // Test
        final double[] nuInOut = this.srp.computeInOutTrueAnom(this.orbit,
            this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));

        // In and out anomalies are true anomalies. Need to be converted into eccentric anomalies
        final double ex = this.orbit.getEquinoctialEx();
        final double ey = this.orbit.getEquinoctialEy();
        final double e = MathLib.sqrt(ex * ex + ey * ey);
        final double sqrt1minusE2 = MathLib.sqrt(1. - e * e);
        final double cosNuIn = MathLib.cos(nuInOut[0]);
        final double cosNuOut = MathLib.cos(nuInOut[1]);
        final double sinEeccIn = sqrt1minusE2 * MathLib.sin(nuInOut[0]) / (1. + e * cosNuIn);
        final double sinEeccOut = sqrt1minusE2 * MathLib.sin(nuInOut[1]) / (1. + e * cosNuOut);
        double anomEccIn = MathLib.atan2(sinEeccIn, (cosNuIn + e) / (1. + e * cosNuIn));
        double anomEccOut = MathLib.atan2(sinEeccOut, (cosNuOut + e) / (1. + e * cosNuOut));
        anomEccIn = JavaMathAdapter.mod(anomEccIn, FastMath.PI * 2);
        anomEccOut = JavaMathAdapter.mod(anomEccOut, FastMath.PI * 2);

        // Comparison
        final double tol = 1e-11;
        final double[] expNuInOut = { 0.9683920180541002, 1.7167145424457575 };

        checkDouble(expNuInOut[0], anomEccIn, tol, ComparisonTypes.RELATIVE);
        checkDouble(expNuInOut[1], anomEccOut, tol, ComparisonTypes.RELATIVE);

        // test no eclipse
        final double mu = 3.9860044144982E14;
        final StelaEquinoctialOrbit orbit2 = new StelaEquinoctialOrbit(new KeplerianOrbit(52500000, 0.001,
            MathLib.toRadians(90), MathLib.toRadians(0), MathLib.toRadians(0), MathLib.toRadians(0),
            PositionAngle.MEAN, FramesFactory.getEME2000(), this.orbit.getDate(), mu));

        final double[] nuInOut2 = this.srp.computeInOutTrueAnom(orbit2,
            this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
        Assert.assertEquals(0, nuInOut2[0], 0);
        Assert.assertEquals(0, nuInOut2[1], 0);

        // test Bi-quadratic equation

        final StelaEquinoctialOrbit orequ = new StelaEquinoctialOrbit(42500000, 0, 0, 0.3, 0.001, 0,
            FramesFactory.getEME2000(), new AbsoluteDate(), mu);
        final double[] nuInOut3 = this.srp.computeInOutTrueAnom(orequ,
            this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
        Assert.assertEquals(3.1827937842005998, nuInOut3[0], 1e-13);
        Assert.assertEquals(3.4754876234998022, nuInOut3[1], 1e-13);
    }

    /**
     * @throws PatriusException
     *         if fail
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#computeInOutTrueAnom(StelaEquinoctialOrbit)}
     * 
     * @description tests inner methods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria Stela threshold : 1e-11 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeInOutTrueAnomSwitchCase() throws PatriusException {
        // test switch case

        final double mass = 114.907;
        final double reflectingArea = 1.07;
        final double reflectCoef = (2 - 1) * 9 / 4.;

        final AssemblyBuilder builder = new AssemblyBuilder();
        final String main = "main3";
        builder.addMainPart(main);
        builder.addProperty(new MassProperty(mass), main);
        builder.addProperty(new RadiativeProperty(0, 0, reflectCoef), main);
        builder.addProperty(new RadiativeSphereProperty(MathLib.sqrt(reflectingArea / FastMath.PI)), main);
        final DirectRadiativeModel model = new DirectRadiativeModel(builder.returnAssembly());

        final SRPSquaring srpTest0 = new SRPSquaring(model, this.sun, Constants.CNES_STELA_AE){
            /** Serializable UID. */
            private static final long serialVersionUID = -4069376285319029838L;

            @Override
            protected int getPolyOrder(final double a0, final double a1, final double a2, final double a3,
                                       final double a4) {
                final int res = 0;
                return res;
            }
        };
        try {
            srpTest0.computeInOutTrueAnom(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        final SRPSquaring srpTest1 = new SRPSquaring(model, this.sun, Constants.CNES_STELA_AE){
            /** Serializable UID. */
            private static final long serialVersionUID = 3662791688072580695L;

            @Override
            protected int getPolyOrder(final double a0, final double a1, final double a2, final double a3,
                                       final double a4) {
                final int res = 1;
                return res;
            }
        };

        try {
            srpTest1.computeInOutTrueAnom(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        final SRPSquaring srpTest5 = new SRPSquaring(model, this.sun, Constants.CNES_STELA_AE){
            /** Serializable UID. */
            private static final long serialVersionUID = -972428860391452200L;

            @Override
            protected int getPolyOrder(final double a0, final double a1, final double a2, final double a3,
                                       final double a4) {
                final int res = 5;
                return res;
            }
        };

        try {
            srpTest5.computeInOutTrueAnom(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
        try {
            final Complex[] rootsCC = { new Complex(0, 0), new Complex(0, 0), new Complex(0, 0) };
            srpTest5.rootsFiltering(rootsCC);
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        final SRPSquaring srpTest2 = new SRPSquaring(model, this.sun, Constants.CNES_STELA_AE){
            /** Serializable UID. */
            private static final long serialVersionUID = -2947664804875303001L;

            @Override
            protected int getPolyOrder(final double a0, final double a1, final double a2, final double a3,
                                       final double a4) {
                final int res = 2;
                return res;
            }

            @Override
            protected Complex[] solvePolyDeg2(final Complex c2, final Complex c1, final Complex c0) {
                final Complex[] roots = { new Complex(4.56, 0), new Complex(0, 1.23) };
                return roots;
            }

            @Override
            protected double[] rootsFiltering(final Complex[] roots) {
                if (roots[0].getReal() == 4.56 && roots[0].getImaginary() == 1.23) {
                    final double[] r = { 0, 0 };
                    return r;
                }
                return super.rootsFiltering(roots);
            }
        };

        final double[] roots =
            srpTest2.computeInOutTrueAnom(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
        Assert.assertEquals(0.0, roots[0], 0);
        Assert.assertEquals(0.0, roots[1], 0);

        final SRPSquaring srpTest3 = new SRPSquaring(model, this.sun, Constants.CNES_STELA_AE){
            /** Serializable UID. */
            private static final long serialVersionUID = 2628022247467960165L;

            @Override
            protected int getPolyOrder(final double a0, final double a1, final double a2, final double a3,
                                       final double a4) {
                final int res = 3;
                return res;
            }

            @Override
            protected Complex[] solvePolyDeg3(final double c3, final double c2, final double c1, final double c0) {
                final Complex[] roots = { new Complex(4.56, 0), new Complex(0, 1.23), new Complex(0, 7.89) };
                return roots;
            }

            @Override
            protected double[] rootsFiltering(final Complex[] roots) {
                if (roots[0].getReal() == 4.56 && roots[0].getImaginary() == 1.23 && roots[0].getImaginary() == 7.89) {
                    final double[] r = { 0, 0 };
                    return r;
                }
                return super.rootsFiltering(roots);
            }
        };

        final double[] roots3 =
            srpTest3.computeInOutTrueAnom(this.orbit,
                this.sun.getPVCoordinates(this.orbit.getDate(), this.orbit.getFrame()));
        Assert.assertEquals(0.0, roots3[0], 0);
        Assert.assertEquals(0.0, roots3[1], 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#rootsFiltering(Complex[])}
     * 
     * @description tests inner methods
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output
     * 
     * @testPassCriteria Stela threshold : 1e-4 absolute scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testRootsFiltering() {
        // Initialization

        final Complex[] inputRoots = new Complex[5];
        // Corresponding to (2pi/3, 0) for alpha
        inputRoots[0] = new Complex(0.5773503, 0);
        // Should be filtered since it's a complex root
        inputRoots[1] = new Complex(0.5773503, -0.1);
        // Corresponding to (pi/4, 0) for alpha
        inputRoots[2] = new Complex(2.4142136, 0);
        // Corresponding to (3pi/2, 0) for alpha
        inputRoots[3] = new Complex(-1, 0);
        // Should be filtered since it's a complex root
        inputRoots[4] = new Complex(-1, 1);

        // Test
        final double[] alphaInOut = this.srp2.rootsFiltering(inputRoots);

        // Comparison
        final double tol = 1E-4;
        final double expAlphIn = 2. * FastMath.PI / 3.;
        final double expAlphOut = 3. * FastMath.PI / 2.;

        checkDouble(expAlphIn, alphaInOut[0], tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expAlphOut, alphaInOut[1], tol, ComparisonTypes.ABSOLUTE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#getPolyOrder(double, double, double, double, double)}
     * 
     * @description tests inner methods
     * 
     * @input coefficients
     * 
     * @output polynom order
     * 
     * @testPassCriteria Stela threshold : strict, integers
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetPolyOrder() {

        // True fourth order
        final double a0 = 0.5;
        double a1 = 1.5;
        double a2 = 2.5;
        double a3 = 3.5;
        double a4 = 4.5;
        int order = this.srp2.getPolyOrder(a0, a1, a2, a3, a4);
        int expOrder = 4;
        checkInt(expOrder, order, 0);

        // Third order
        a4 = 1E-08;
        order = this.srp2.getPolyOrder(a0, a1, a2, a3, a4);
        expOrder = 3;
        checkInt(expOrder, order, 0);

        // Second order
        a3 = 1E-08;
        order = this.srp2.getPolyOrder(a0, a1, a2, a3, a4);
        expOrder = 2;
        checkInt(expOrder, order, 0);

        // First order
        a2 = 1E-08;
        order = this.srp2.getPolyOrder(a0, a1, a2, a3, a4);
        expOrder = 1;
        checkInt(expOrder, order, 0);

        // Zero order
        a1 = 1E-08;
        order = this.srp2.getPolyOrder(a0, a1, a2, a3, a4);
        expOrder = 0;
        checkInt(expOrder, order, 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#solvePolyDeg2(Complex, Complex, Complex)}
     * 
     * @description tests inner methods
     * 
     * @input coefficients
     * 
     * @output polynom root
     * 
     * @testPassCriteria Stela threshold : 1E-15 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSolvePolyDeg2() {

        final double tol = 1E-15;

        // Two real roots
        final Complex c2 = new Complex(3, 0);
        final Complex c1 = new Complex(2, 0);
        Complex c0 = new Complex(-1, 0);
        Complex[] roots = this.srp2.solvePolyDeg2(c2, c1, c0);
        double expRoot1Re = -1.;
        double expRoot2Re = 1. / 3.;
        double expRoot1Im = 0.;
        double expRoot2Im = 0.;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);

        // Two conjugate complex roots
        c0 = c0.negate();
        roots = this.srp2.solvePolyDeg2(c2, c1, c0);
        expRoot1Re = -1. / 3.;
        expRoot2Re = -1. / 3.;
        expRoot1Im = -MathLib.sqrt(2.) / 3.;
        expRoot2Im = MathLib.sqrt(2.) / 3.;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);

        // Complex coefficients
        final Complex c2c = new Complex(1, 0);
        final Complex c1c = new Complex(0, -MathLib.sqrt(3));
        final Complex c0c = new Complex(-1, 0);
        roots = this.srp2.solvePolyDeg2(c2c, c1c, c0c);
        expRoot1Re = -1. / 2.;
        expRoot2Re = 1. / 2.;
        expRoot1Im = MathLib.sqrt(3.) / 2.;
        expRoot2Im = MathLib.sqrt(3.) / 2.;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#solvePolyDeg3(double, double, double, double)}
     * 
     * @description tests inner methods
     * 
     * @input coefficients
     * 
     * @output polynom root
     * 
     * @testPassCriteria Stela threshold : 1E-08 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSolvePolyDeg3() {
        // Here the coefficients can only be reals

        final double tol = 1E-08;

        // First case : p = 0
        double c3 = 1;
        double c2 = -2;
        double c1 = 4. / 3.;
        double c0 = 1;
        Complex[] roots = this.srp2.solvePolyDeg3(c3, c2, c1, c0);
        double expRoot1Re = -0.42368877006286321;
        double expRoot2Re = 1.21184438503143155;
        double expRoot3Re = 1.21184438503143155;
        double expRoot1Im = 0;
        double expRoot2Im = -0.94427550736224908;
        double expRoot3Im = 0.94427550736224908;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.ABSOLUTE);

        // Second case : delta = 0
        c0 = 1.;
        c1 = -MathLib.pow(27. / 4., 1. / 3.);
        c2 = 0.;
        c3 = 1.;
        roots = this.srp2.solvePolyDeg3(c3, c2, c1, c0);
        expRoot1Re = -1.58740105196819936;
        expRoot2Re = 0.79370052595430962;
        expRoot3Re = 0.79370052595430962;
        expRoot1Im = 0;
        expRoot2Im = 0;
        expRoot3Im = 0;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.ABSOLUTE);

        // Third case : delta < 0
        c0 = 2;
        c1 = -1;
        c2 = -2;
        c3 = 1;
        roots = this.srp2.solvePolyDeg3(c3, c2, c1, c0);
        expRoot1Re = 2;
        expRoot2Re = -1;
        expRoot3Re = 1;
        expRoot1Im = 0;
        expRoot2Im = 0;
        expRoot3Im = 0;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.ABSOLUTE);

        // Fourth case : delta > 0
        c0 = 1;
        c1 = 1;
        c2 = 1;
        c3 = 1;
        roots = this.srp2.solvePolyDeg3(c3, c2, c1, c0);
        expRoot1Re = -1;
        expRoot2Re = 0;
        expRoot3Re = 0;
        expRoot1Im = 0;
        expRoot2Im = 1;
        expRoot3Im = -1;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.ABSOLUTE);

        // Fifth Case, p !=0 && delat = 0
        c0 = 9;
        c1 = 3;
        c2 = -5;
        c3 = 1;
        roots = this.srp2.solvePolyDeg3(c3, c2, c1, c0);
        expRoot1Re = -1;
        expRoot2Re = 3;
        expRoot3Re = 3;
        expRoot1Im = 0;
        expRoot2Im = 0;
        expRoot3Im = 0;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.ABSOLUTE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.ABSOLUTE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#solveBiquadratic(double, double, double)}
     * 
     * @description tests inner methods
     * 
     * @input coefficients
     * 
     * @output polynom root
     * 
     * @testPassCriteria Stela threshold : 1E-15 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSolveBiquadratic() {

        final double tol = 1E-15;

        // solution of second order equation is two real roots
        double c4 = 3;
        double c2 = 2;
        double c0 = -1;
        Complex[] roots = this.srp2.solveBiquadratic(c4, c2, c0);
        double expRoot1Re = 0;
        double expRoot2Re = 0;
        double expRoot3Re = 0.57735026918962573;
        double expRoot4Re = -0.57735026918962573;
        double expRoot1Im = 1.;
        double expRoot2Im = -1.;
        double expRoot3Im = 0.;
        double expRoot4Im = 0.;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Re, roots[3].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Im, roots[3].getImaginary(), tol, ComparisonTypes.RELATIVE);

        // Solution of second order equation is two conjugate complex roots
        c4 = 3;
        c2 = 2;
        c0 = 1;
        roots = this.srp2.solveBiquadratic(c4, c2, c0);
        expRoot1Re = 0.34929710552500459;
        expRoot2Re = -0.34929710552500459;
        expRoot3Re = 0.34929710552500459;
        expRoot4Re = -0.34929710552500459;
        expRoot1Im = -0.67479019054923994;
        expRoot2Im = 0.67479019054923994;
        expRoot3Im = 0.67479019054923994;
        expRoot4Im = -0.67479019054923994;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Re, roots[3].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Im, roots[3].getImaginary(), tol, ComparisonTypes.RELATIVE);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SRP_INNER}
     * 
     * @testedMethod {@link SRPSquaring#solvePolyDeg4(double, double, double, double, double)}
     * 
     * @description tests inner methods
     * 
     * @input coefficients
     * 
     * @output polynom root
     * 
     * @testPassCriteria Stela threshold : 1E-14 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSolvePolyDeg4() {

        final double tol = 1E-14;

        // b != 0
        double c0 = 1;
        double c1 = 1;
        double c2 = 2;
        double c3 = -1;
        double c4 = -3;
        Complex[] roots = this.srp2.solvePolyDeg4(c4, c3, c2, c1, c0);
        double expRoot1Re = -1;
        double expRoot2Re = 1;
        double expRoot3Re = -0.16666666666666669;
        double expRoot4Re = -0.16666666666666669;
        double expRoot1Im = 0;
        double expRoot2Im = 0;
        double expRoot3Im = -0.55277079839256660;
        double expRoot4Im = 0.55277079839256660;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Re, roots[3].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Im, roots[3].getImaginary(), tol, ComparisonTypes.RELATIVE);

        // b = 0
        c0 = 1;
        c1 = 0;
        c2 = 2;
        c3 = 0;
        c4 = -3;
        roots = this.srp2.solvePolyDeg4(c4, c3, c2, c1, c0);
        expRoot1Re = 0;
        expRoot2Re = 0;
        expRoot3Re = 1;
        expRoot4Re = -1;
        expRoot1Im = 0.57735026918962573;
        expRoot2Im = -0.57735026918962573;
        expRoot3Im = 0;
        expRoot4Im = 0;
        checkDouble(expRoot1Re, roots[0].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot1Im, roots[0].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Re, roots[1].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot2Im, roots[1].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Re, roots[2].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot3Im, roots[2].getImaginary(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Re, roots[3].getReal(), tol, ComparisonTypes.RELATIVE);
        checkDouble(expRoot4Im, roots[3].getImaginary(), tol, ComparisonTypes.RELATIVE);
    }

    @Before
    public void setup() throws PatriusException {
        // StelaVectorConverter.setTtMinusUt1(66.184); // Old TT - UT1 used for the test

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // final AbsoluteDate date = new AbsoluteDate(new DateComponents(2011, 1), TimeComponents.H00,
        // TimeScalesFactory.getUT1());

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2011, 04, 01),
            new TimeComponents(0, 0, 34.), TimeScalesFactory.getTAI());
        // Assert.assertEquals(66.184, TimeScalesFactory.getTT().offsetFromTAI(date)
        // - TimeScalesFactory.getUTC().offsetFromTAI(date), EPS);

        this.sun = new MeeusSun(MODEL.STELA);

        // SRP initialization for SRP tests
        final double mass = 114.907;
        final double reflectingArea = 1.07;
        final double reflectCoef = (2 - 1) * 9 / 4.;

        final AssemblyBuilder builder = new AssemblyBuilder();
        final String main = "main";
        builder.addMainPart(main);
        builder.addProperty(new MassProperty(mass), main);
        builder.addProperty(new RadiativeProperty(0, 0, reflectCoef), main);
        builder.addProperty(new RadiativeSphereProperty(MathLib.sqrt(reflectingArea / FastMath.PI)), main);
        final DirectRadiativeModel model = new DirectRadiativeModel(builder.returnAssembly());

        this.srp = new SRPSquaring(model, this.sun, Constants.CNES_STELA_AE);

        this.orbit = new StelaEquinoctialOrbit(2.43505E+7, 0.3644381018870251, 0.6312253086822904, 0.09052430460833645,
            0.05226423163382672, 1.919862177193762, FramesFactory.getMOD(false), date, Constants.CNES_STELA_MU);

        final ArrayList<StelaForceModel> forcelist = new ArrayList<>();
        forcelist.add(this.srp);
        this.converter = new OrbitNatureConverter(forcelist);

        // SRP initialization for polynomial roots tests
        final double mass2 = 100;
        final double reflectingArea2 = 15;
        builder.addMainPart("Main2");
        builder.addProperty(new MassProperty(mass2), "Main2");
        builder.addProperty(new RadiativeProperty(0, 0, reflectCoef), "Main2");
        builder.addProperty(new RadiativeSphereProperty(MathLib.sqrt(reflectingArea2 / FastMath.PI)), "Main2");
        new DirectRadiativeModel(builder.returnAssembly());

        this.srp2 = new SRPSquaring(model, this.quadPoints, this.sun, Constants.CNES_STELA_AE);
    }

    /**
     * Enumeration of comparison possibilities.
     */
    public enum ComparisonTypes {
        ABSOLUTE, RELATIVE;
    }

    public static void checkInt(final int iExpected, final int iActual, final int tol) {
        final int absDiff = iActual - iExpected;
        Assert.assertEquals(0, MathLib.abs(absDiff), tol);
    }

    public static void checkInt(final int[] dExpected, final int[] dActual, final int tol) {
        for (int i = 0; i < dExpected.length; i++) {
            checkInt(dExpected[i], dActual[i], tol);
        }
    }

    public static void checkDouble(final double dExpected, final double dActual, final double tol,
                                   final ComparisonTypes compType) {
        final double absDiff = dActual - dExpected;
        final double relDiff = dExpected == 0 & absDiff == 0 ? 0 : absDiff / dExpected;
        switch (compType) {
            case ABSOLUTE:
                Assert.assertEquals(0, MathLib.abs(absDiff), tol);
                break;
            case RELATIVE:
                Assert.assertEquals(0, MathLib.abs(relDiff), tol);
                break;
            default:
                break;
        }
    }

    public static void checkDouble(final double[] dExpected, final double[] dActual, final double[] tol,
                                   final ComparisonTypes compType) {
        for (int i = 0; i < dExpected.length; i++) {
            checkDouble(dExpected[i], dActual[i], tol[i], compType);
        }
    }

    public static void checkDouble(final double[] dExpected, final double[] dActual, final double tol,
                                   final ComparisonTypes compType) {
        Assert.assertEquals(dExpected.length, dActual.length);
        for (int i = 0; i < dExpected.length; i++) {
            checkDouble(dExpected[i], dActual[i], tol, compType);
        }
    }

    public static void checkDouble(final double[][] dExpected, final double[][] dActual, final double tol,
                                   final ComparisonTypes compType) {
        Assert.assertEquals(dExpected.length, dActual.length);
        for (int i = 0; i < dExpected.length; i++) {
            checkDouble(dExpected[i], dActual[i], tol, compType);
        }
    }
}
