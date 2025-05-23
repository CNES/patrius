/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.transform;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.Sinc;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for fast sine transformer.
 * <p>
 * FST algorithm is exact, the small tolerance number is used only to account for round-off errors.
 * 
 * @version $Id: FastSineTransformerTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
@RunWith(value = Parameterized.class)
public final class FastSineTransformerTest extends RealTransformerAbstractTest {

    private final DstNormalization normalization;

    private final int[] invalidDataSize;

    private final double[] relativeTolerance;

    private final int[] validDataSize;

    public FastSineTransformerTest(final DstNormalization normalization) {
        this.normalization = normalization;
        this.validDataSize = new int[] {
            1, 2, 4, 8, 16, 32, 64, 128
        };
        this.invalidDataSize = new int[] {
            129
        };
        this.relativeTolerance = new double[] {
            1E-15, 1E-15, 1E-14, 1E-14, 1E-13, 1E-12, 1E-11, 1E-11
        };
    }

    /**
     * Returns an array containing {@code true, false} in order to check both
     * standard and orthogonal DSTs.
     * 
     * @return an array of parameters for this parameterized test
     */
    @Parameters
    public static Collection<Object[]> data() {
        final DstNormalization[] normalization = DstNormalization.values();
        final Object[][] data = new DstNormalization[normalization.length][1];
        for (int i = 0; i < normalization.length; i++) {
            data[i][0] = normalization[i];
        }
        return Arrays.asList(data);
    }

    /**
     * {@inheritDoc}
     * 
     * Overriding the default implementation allows to ensure that the first
     * element of the data set is zero.
     */
    @Override
    double[] createRealData(final int n) {
        final double[] data = super.createRealData(n);
        data[0] = 0.0;
        return data;
    }

    @Override
    RealTransformer createRealTransformer() {
        return new FastSineTransformer(this.normalization);
    }

    @Override
    int getInvalidDataSize(final int i) {
        return this.invalidDataSize[i];
    }

    @Override
    int getNumberOfInvalidDataSizes() {
        return this.invalidDataSize.length;
    }

    @Override
    int getNumberOfValidDataSizes() {
        return this.validDataSize.length;
    }

    @Override
    double getRelativeTolerance(final int i) {
        return this.relativeTolerance[i];
    }

    @Override
    int getValidDataSize(final int i) {
        return this.validDataSize[i];
    }

    @Override
    UnivariateFunction getValidFunction() {
        return new Sinc();
    }

    @Override
    double getValidLowerBound() {
        return 0.0;
    }

    @Override
    double getValidUpperBound() {
        return FastMath.PI;
    }

    @Override
    double[] transform(final double[] x, final TransformType type) {
        final int n = x.length;
        final double[] y = new double[n];
        final double[] sin = new double[2 * n];
        for (int i = 0; i < sin.length; i++) {
            sin[i] = MathLib.sin(FastMath.PI * i / n);
        }
        for (int j = 0; j < n; j++) {
            double yj = 0.0;
            for (int i = 0; i < n; i++) {
                yj += x[i] * sin[(i * j) % sin.length];
            }
            y[j] = yj;
        }
        final double s;
        if (type == TransformType.FORWARD) {
            if (this.normalization == DstNormalization.STANDARD_DST_I) {
                s = 1.0;
            } else if (this.normalization == DstNormalization.ORTHOGONAL_DST_I) {
                s = MathLib.sqrt(2.0 / n);
            } else {
                throw new MathIllegalStateException();
            }
        } else if (type == TransformType.INVERSE) {
            if (this.normalization == DstNormalization.STANDARD_DST_I) {
                s = 2.0 / n;
            } else if (this.normalization == DstNormalization.ORTHOGONAL_DST_I) {
                s = MathLib.sqrt(2.0 / n);
            } else {
                throw new MathIllegalStateException();
            }
        } else {
            /*
             * Should never occur. This clause is a safeguard in case other
             * types are used to TransformType (which should not be done).
             */
            throw new MathIllegalStateException();
        }
        TransformUtils.scaleArray(y, s);
        return y;
    }

    /*
     * Additional tests.
     */
    @Test
    public void testTransformRealFirstElementNotZero() {
        final TransformType[] type = TransformType.values();
        final double[] data = new double[] {
            1.0, 1.0, 1.0, 1.0
        };
        final RealTransformer transformer = this.createRealTransformer();
        for (final TransformType element : type) {
            try {
                transformer.transform(data, element);
                Assert.fail(element.toString());
            } catch (final MathIllegalArgumentException e) {
                // Expected: do nothing
            }
        }
    }

    /*
     * Additional (legacy) tests.
     */

    /**
     * Test of transformer for the ad hoc data.
     */
    @Test
    public void testAdHocData() {
        FastSineTransformer transformer;
        transformer = new FastSineTransformer(DstNormalization.STANDARD_DST_I);
        double result[];
        final double tolerance = 1E-12;

        final double x[] = { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
        final double y[] = { 0.0, 20.1093579685034, -9.65685424949238,
            5.98642305066196, -4.0, 2.67271455167720,
            -1.65685424949238, 0.795649469518633 };

        result = transformer.transform(x, TransformType.FORWARD);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(y[i], result[i], tolerance);
        }

        result = transformer.transform(y, TransformType.INVERSE);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(x[i], result[i], tolerance);
        }

        TransformUtils.scaleArray(x, MathLib.sqrt(x.length / 2.0));
        transformer = new FastSineTransformer(DstNormalization.ORTHOGONAL_DST_I);

        result = transformer.transform(y, TransformType.FORWARD);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(x[i], result[i], tolerance);
        }

        result = transformer.transform(x, TransformType.INVERSE);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(y[i], result[i], tolerance);
        }
    }

    /**
     * Test of transformer for the sine function.
     */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        FastSineTransformer transformer;
        transformer = new FastSineTransformer(DstNormalization.STANDARD_DST_I);
        double min, max, result[];
        final double tolerance = 1E-12;
        final int N = 1 << 8;

        min = 0.0;
        max = 2.0 * FastMath.PI;
        result = transformer.transform(f, min, max, N, TransformType.FORWARD);
        Assert.assertEquals(N >> 1, result[2], tolerance);
        for (int i = 0; i < N; i += (i == 1 ? 2 : 1)) {
            Assert.assertEquals(0.0, result[i], tolerance);
        }

        min = -FastMath.PI;
        max = FastMath.PI;
        result = transformer.transform(f, min, max, N, TransformType.FORWARD);
        Assert.assertEquals(-(N >> 1), result[2], tolerance);
        for (int i = 0; i < N; i += (i == 1 ? 2 : 1)) {
            Assert.assertEquals(0.0, result[i], tolerance);
        }
    }

    /**
     * Test of parameters for the transformer.
     */
    @Test
    public void testParameters() throws Exception {
        final UnivariateFunction f = new Sin();
        FastSineTransformer transformer;
        transformer = new FastSineTransformer(DstNormalization.STANDARD_DST_I);

        try {
            // bad interval
            transformer.transform(f, 1, -1, 64, TransformType.FORWARD);
            Assert.fail("Expecting IllegalArgumentException - bad interval");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        try {
            // bad samples number
            transformer.transform(f, -1, 1, 0, TransformType.FORWARD);
            Assert.fail("Expecting IllegalArgumentException - bad samples number");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        try {
            // bad samples number
            transformer.transform(f, -1, 1, 100, TransformType.FORWARD);
            Assert.fail("Expecting IllegalArgumentException - bad samples number");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }
}
