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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

public class HermiteInterpolatorTest {

    @Test
    public void testZero() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        interpolator.addSamplePoint(0.0, new double[] { 0.0 });
        for (double x = -10; x < 10; x += 1.0) {
            Assert.assertEquals(0.0, interpolator.value(x)[0], 1.0e-15);
            Assert.assertEquals(0.0, interpolator.derivative(x)[0], 1.0e-15);
        }
        checkPolynomial(new PolynomialFunction(new double[] { 0.0 }),
            interpolator.getPolynomials()[0]);
    }

    @Test
    public void testQuadratic() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        interpolator.addSamplePoint(0.0, new double[] { 2.0 });
        interpolator.addSamplePoint(1.0, new double[] { 0.0 });
        interpolator.addSamplePoint(2.0, new double[] { 0.0 });
        for (double x = -10; x < 10; x += 1.0) {
            Assert.assertEquals((x - 1.0) * (x - 2.0), interpolator.value(x)[0], 1.0e-15);
            Assert.assertEquals(2 * x - 3.0, interpolator.derivative(x)[0], 1.0e-15);
        }
        checkPolynomial(new PolynomialFunction(new double[] { 2.0, -3.0, 1.0 }),
            interpolator.getPolynomials()[0]);
    }

    @Test
    public void testMixedDerivatives() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        interpolator.addSamplePoint(0.0, new double[] { 1.0 }, new double[] { 2.0 });
        interpolator.addSamplePoint(1.0, new double[] { 4.0 });
        interpolator.addSamplePoint(2.0, new double[] { 5.0 }, new double[] { 2.0 });
        Assert.assertEquals(4, interpolator.getPolynomials()[0].degree());
        Assert.assertEquals(1.0, interpolator.value(0.0)[0], 1.0e-15);
        Assert.assertEquals(2.0, interpolator.derivative(0.0)[0], 1.0e-15);
        Assert.assertEquals(4.0, interpolator.value(1.0)[0], 1.0e-15);
        Assert.assertEquals(5.0, interpolator.value(2.0)[0], 1.0e-15);
        Assert.assertEquals(2.0, interpolator.derivative(2.0)[0], 1.0e-15);
        checkPolynomial(new PolynomialFunction(new double[] { 1.0, 2.0, 4.0, -4.0, 1.0 }),
            interpolator.getPolynomials()[0]);
    }

    @Test
    public void testRandomPolynomialsValuesOnly() {

        final Random random = new Random(0x42b1e7dbd361a932l);

        for (int i = 0; i < 100; ++i) {

            int maxDegree = 0;
            final PolynomialFunction[] p = new PolynomialFunction[5];
            for (int k = 0; k < p.length; ++k) {
                final int degree = random.nextInt(7);
                p[k] = randomPolynomial(degree, random);
                maxDegree = MathLib.max(maxDegree, degree);
            }

            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (int j = 0; j < 1 + maxDegree; ++j) {
                final double x = 0.1 * j;
                final double[] values = new double[p.length];
                for (int k = 0; k < p.length; ++k) {
                    values[k] = p[k].value(x);
                }
                interpolator.addSamplePoint(x, values);
            }

            for (double x = 0; x < 2; x += 0.1) {
                final double[] values = interpolator.value(x);
                Assert.assertEquals(p.length, values.length);
                for (int k = 0; k < p.length; ++k) {
                    Assert.assertEquals(p[k].value(x), values[k], 1.0e-8 * MathLib.abs(p[k].value(x)));
                }
            }

            final PolynomialFunction[] result = interpolator.getPolynomials();
            for (int k = 0; k < p.length; ++k) {
                checkPolynomial(p[k], result[k]);
            }
        }
    }

    @Test
    public void testRandomPolynomialsFirstDerivative() {

        final Random random = new Random(0x570803c982ca5d3bl);

        for (int i = 0; i < 100; ++i) {

            int maxDegree = 0;
            final PolynomialFunction[] p = new PolynomialFunction[5];
            final PolynomialFunction[] pPrime = new PolynomialFunction[5];
            for (int k = 0; k < p.length; ++k) {
                final int degree = random.nextInt(7);
                p[k] = randomPolynomial(degree, random);
                pPrime[k] = p[k].polynomialDerivative();
                maxDegree = MathLib.max(maxDegree, degree);
            }

            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (int j = 0; j < 1 + maxDegree / 2; ++j) {
                final double x = 0.1 * j;
                final double[] values = new double[p.length];
                final double[] derivatives = new double[p.length];
                for (int k = 0; k < p.length; ++k) {
                    values[k] = p[k].value(x);
                    derivatives[k] = pPrime[k].value(x);
                }
                interpolator.addSamplePoint(x, values, derivatives);
            }

            for (double x = 0; x < 2; x += 0.1) {
                final double[] values = interpolator.value(x);
                final double[] derivatives = interpolator.derivative(x);
                Assert.assertEquals(p.length, values.length);
                for (int k = 0; k < p.length; ++k) {
                    Assert.assertEquals(p[k].value(x), values[k], 1.0e-8 * MathLib.abs(p[k].value(x)));
                    Assert.assertEquals(pPrime[k].value(x), derivatives[k], 4.0e-8 * MathLib.abs(p[k].value(x)));
                }
            }

            final PolynomialFunction[] result = interpolator.getPolynomials();
            for (int k = 0; k < p.length; ++k) {
                checkPolynomial(p[k], result[k]);
            }
        }
    }

    @Test
    public void testSine() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        for (double x = 0; x < FastMath.PI; x += 0.5) {
            interpolator.addSamplePoint(x, new double[] { MathLib.sin(x) });
        }
        for (double x = 0.1; x <= 2.9; x += 0.01) {
            Assert.assertEquals(MathLib.sin(x), interpolator.value(x)[0], 3.5e-5);
            Assert.assertEquals(MathLib.cos(x), interpolator.derivative(x)[0], 1.3e-4);
        }
    }

    @Test
    public void testSquareRoot() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        for (double x = 1.0; x < 3.6; x += 0.5) {
            interpolator.addSamplePoint(x, new double[] { MathLib.sqrt(x) });
        }
        for (double x = 1.1; x < 3.5; x += 0.01) {
            Assert.assertEquals(MathLib.sqrt(x), interpolator.value(x)[0], 1.5e-4);
            Assert.assertEquals(0.5 / MathLib.sqrt(x), interpolator.derivative(x)[0], 8.5e-4);
        }
    }

    @Test
    public void testWikipedia() {
        // this test corresponds to the example from Wikipedia page:
        // http://en.wikipedia.org/wiki/Hermite_interpolation
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        interpolator.addSamplePoint(-1, new double[] { 2 }, new double[] { -8 }, new double[] { 56 });
        interpolator.addSamplePoint(0, new double[] { 1 }, new double[] { 0 }, new double[] { 0 });
        interpolator.addSamplePoint(1, new double[] { 2 }, new double[] { 8 }, new double[] { 56 });
        for (double x = -1.0; x <= 1.0; x += 0.125) {
            final double x2 = x * x;
            final double x4 = x2 * x2;
            final double x8 = x4 * x4;
            Assert.assertEquals(x8 + 1, interpolator.value(x)[0], 1.0e-15);
            Assert.assertEquals(8 * x4 * x2 * x, interpolator.derivative(x)[0], 1.0e-15);
        }
        checkPolynomial(new PolynomialFunction(new double[] { 1, 0, 0, 0, 0, 0, 0, 0, 1 }),
            interpolator.getPolynomials()[0]);
    }

    @Test
    public void testOnePointParabola() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        interpolator.addSamplePoint(0, new double[] { 1 }, new double[] { 1 }, new double[] { 2 });
        for (double x = -1.0; x <= 1.0; x += 0.125) {
            Assert.assertEquals(1 + x * (1 + x), interpolator.value(x)[0], 1.0e-15);
            Assert.assertEquals(1 + 2 * x, interpolator.derivative(x)[0], 1.0e-15);
        }
        checkPolynomial(new PolynomialFunction(new double[] { 1, 1, 1 }),
            interpolator.getPolynomials()[0]);
    }

    private static PolynomialFunction randomPolynomial(final int degree, final Random random) {
        final double[] coeff = new double[1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    @Test(expected = IllegalStateException.class)
    public void testEmptySample() {
        new HermiteInterpolator().value(0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicatedAbscissa() {
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        interpolator.addSamplePoint(1.0, new double[] { 0.0 });
        interpolator.addSamplePoint(1.0, new double[] { 1.0 });
    }

    private static void checkPolynomial(final PolynomialFunction expected, final PolynomialFunction result) {
        Assert.assertTrue(result.degree() >= expected.degree());
        final double[] cE = expected.getCoefficients();
        final double[] cR = result.getCoefficients();
        for (int i = 0; i < cE.length; ++i) {
            Assert.assertEquals(cE[i], cR[i], 1.0e-8 * MathLib.abs(cE[i]));
        }
        for (int i = cE.length; i < cR.length; ++i) {
            Assert.assertEquals(0.0, cR[i], 1.0e-9);
        }
    }

    /**
     * @description Evaluate the two interpolations methods.
     *
     * @testedMethod {@link HermiteInterpolator#value(DerivativeStructure)}
     * @testedMethod {@link HermiteInterpolator#valueAndDerivative(double, boolean)}
     *
     * @testPassCriteria The two interpolations methods should produce the same result.
     */
    @Test
    public void testCompareInterpolationMethods() {

        // Initialize some samples for the interpolation
        final int size = 10;
        final AbsoluteDate refDate = AbsoluteDate.J2000_EPOCH;

        final TimeStampedPVCoordinates[] samples = new TimeStampedPVCoordinates[size];
        for (int i = 0; i < size; i++) {
            final AbsoluteDate date = refDate.shiftedBy(i);
            final Vector3D pos = new Vector3D(7179992.82 + i * 10.2, 2276.519 - i * 10.1, -14178.396 + i * 10.8);
            final Vector3D vel = new Vector3D(7.450848 - i * 0.7, -1181.198684 + i * 0.2, 7356.62864 + i * 0.66);
            final Vector3D acc = new Vector3D(i * 0.65, -1.184 + i * 0.12, 7.64 + i * 0.06);
            samples[i] = new TimeStampedPVCoordinates(date, pos, vel, acc);
        }

        final boolean[] computeDoubleDerivativeArray = new boolean[] { false, true };

        // Loop over each boolean case
        for (final boolean computeDoubleDerivative : computeDoubleDerivativeArray) {

            // Loop on each filter to evaluate them all
            for (final CartesianDerivativesFilter filter : CartesianDerivativesFilter.values()) {

                final HermiteInterpolator interpolator = new HermiteInterpolator();

                for (int i = 0; i < size; i++) {
                    final TimeStampedPVCoordinates sample = samples[i];
                    final AbsoluteDate date = sample.getDate();

                    final Vector3D position = sample.getPosition();
                    final double[] value = new double[] { position.getX(), position.getY(), position.getZ() };

                    switch (filter) {
                        case USE_P:
                            interpolator.addSamplePoint(date.durationFrom(refDate), value);
                            break;
                        case USE_PV:
                            final Vector3D velocity = sample.getVelocity();
                            final double[] derivativeValue = new double[] { velocity.getX(), velocity.getY(), velocity.getZ() };
                            interpolator.addSamplePoint(date.durationFrom(refDate), value, derivativeValue);
                            break;
                        case USE_PVA:
                            final Vector3D velocitybis = sample.getVelocity();
                            final double[] derivativeValuebis = new double[] { velocitybis.getX(), velocitybis.getY(), velocitybis.getZ() };
                            final Vector3D acceleration = sample.getAcceleration();
                            final double[] doubleDerivative =
                                new double[] { acceleration.getX(), acceleration.getY(), acceleration.getZ() };
                            interpolator.addSamplePoint(date.durationFrom(refDate), value, derivativeValuebis, doubleDerivative);
                            break;
                        default:
                            throw new EnumConstantNotPresentException(CartesianDerivativesFilter.class, "filter");
                    }

                    // Interpolate with the DerivativeStructure[] value(DerivativeStructure) method

                    final DerivativeStructure zero;
                    if (computeDoubleDerivative) {
                        zero = new DerivativeStructure(1, 3, 0, date.durationFrom(refDate));
                    } else {
                        zero = new DerivativeStructure(1, 2, 0, date.durationFrom(refDate));
                    }

                    final DerivativeStructure[] derivativeStructure = interpolator.value(zero);
                    final DerivativeStructure x = derivativeStructure[0];
                    final DerivativeStructure y = derivativeStructure[1];
                    final DerivativeStructure z = derivativeStructure[2];
                    final Vector3D pos1 = new Vector3D(x.getValue(), y.getValue(), z.getValue());
                    final Vector3D vel1 = new Vector3D(x.getPartialDerivative(1), y.getPartialDerivative(1), z.getPartialDerivative(1));

                    // Interpolate with the double[][] valueAndDerivative(double, boolean) method
                    final double duration = date.durationFrom(refDate);
                    final double[][] valueAndDerivative = interpolator.valueAndDerivative(duration, computeDoubleDerivative);
                    final Vector3D pos2 = new Vector3D(valueAndDerivative[0]);
                    final Vector3D vel2 = new Vector3D(valueAndDerivative[1]);

                    // Compare the interpolation outputs
                    Assert.assertEquals(pos1, pos2);
                    Assert.assertEquals(vel1, vel2);

                    if (computeDoubleDerivative) {
                        final Vector3D acc1 = new Vector3D(x.getPartialDerivative(2), y.getPartialDerivative(2), z.getPartialDerivative(2));
                        final Vector3D acc2 = new Vector3D(valueAndDerivative[2]);
                        Assert.assertEquals(acc1, acc2);
                    }
                }
            }
        }
    }
}
