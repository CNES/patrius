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
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for fast cosine transformer.
 * <p>
 * FCT algorithm is exact, the small tolerance number is used only to account for round-off errors.
 * 
 * @version $Id: FastCosineTransformerTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
@RunWith(value = Parameterized.class)
public final class FastCosineTransformerTest
    extends RealTransformerAbstractTest {

    private final DctNormalization normalization;

    private final int[] invalidDataSize;

    private final double[] relativeTolerance;

    private final int[] validDataSize;

    public FastCosineTransformerTest(final DctNormalization normalization) {
        this.normalization = normalization;
        this.validDataSize = new int[] {
            2, 3, 5, 9, 17, 33, 65, 129
        };
        this.invalidDataSize = new int[] {
            128
        };
        this.relativeTolerance = new double[] {
            1E-15, 1E-15, 1E-14, 1E-13, 1E-13, 1E-12, 1E-11, 1E-10
        };
    }

    /**
     * Returns an array containing {@code true, false} in order to check both
     * standard and orthogonal DCTs.
     * 
     * @return an array of parameters for this parameterized test
     */
    @Parameters
    public static Collection<Object[]> data() {
        final DctNormalization[] normalization = DctNormalization.values();
        final Object[][] data = new DctNormalization[normalization.length][1];
        for (int i = 0; i < normalization.length; i++) {
            data[i][0] = normalization[i];
        }
        return Arrays.asList(data);
    }

    @Override
    RealTransformer createRealTransformer() {
        return new FastCosineTransformer(this.normalization);
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
        final double[] cos = new double[2 * (n - 1)];
        for (int i = 0; i < cos.length; i++) {
            cos[i] = MathLib.cos(FastMath.PI * i / (n - 1.0));
        }
        int sgn = 1;
        for (int j = 0; j < n; j++) {
            double yj = 0.5 * (x[0] + sgn * x[n - 1]);
            for (int i = 1; i < n - 1; i++) {
                yj += x[i] * cos[(i * j) % cos.length];
            }
            y[j] = yj;
            sgn *= -1;
        }
        final double s;
        if (type == TransformType.FORWARD) {
            if (this.normalization == DctNormalization.STANDARD_DCT_I) {
                s = 1.0;
            } else if (this.normalization == DctNormalization.ORTHOGONAL_DCT_I) {
                s = MathLib.sqrt(2.0 / (n - 1.0));
            } else {
                throw new MathIllegalStateException();
            }
        } else if (type == TransformType.INVERSE) {
            if (this.normalization == DctNormalization.STANDARD_DCT_I) {
                s = 2.0 / (n - 1.0);
            } else if (this.normalization == DctNormalization.ORTHOGONAL_DCT_I) {
                s = MathLib.sqrt(2.0 / (n - 1.0));
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

    /** Test of transformer for the ad hoc data. */
    @Test
    public void testAdHocData() {
        FastCosineTransformer transformer;
        transformer = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);
        double result[];
        final double tolerance = 1E-12;

        final double x[] = {
            0.0, 1.0, 4.0, 9.0, 16.0, 25.0, 36.0, 49.0, 64.0
        };
        final double y[] =
        {
            172.0, -105.096569476353, 27.3137084989848, -12.9593152353742,
            8.0, -5.78585076868676, 4.68629150101524, -4.15826451958632,
            4.0
        };

        result = transformer.transform(x, TransformType.FORWARD);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(y[i], result[i], tolerance);
        }

        result = transformer.transform(y, TransformType.INVERSE);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(x[i], result[i], tolerance);
        }

        TransformUtils.scaleArray(x, MathLib.sqrt(0.5 * (x.length - 1)));

        transformer = new FastCosineTransformer(DctNormalization.ORTHOGONAL_DCT_I);
        result = transformer.transform(y, TransformType.FORWARD);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(x[i], result[i], tolerance);
        }

        result = transformer.transform(x, TransformType.INVERSE);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(y[i], result[i], tolerance);
        }
    }

    /** Test of parameters for the transformer. */
    @Test
    public void testParameters()
                                throws Exception {
        final UnivariateFunction f = new Sin();
        FastCosineTransformer transformer;
        transformer = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);

        try {
            // bad interval
            transformer.transform(f, 1, -1, 65, TransformType.FORWARD);
            Assert.fail("Expecting IllegalArgumentException - bad interval");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        try {
            // bad samples number
            transformer.transform(f, -1, 1, 1, TransformType.FORWARD);
            Assert
                .fail("Expecting IllegalArgumentException - bad samples number");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        try {
            // bad samples number
            transformer.transform(f, -1, 1, 64, TransformType.FORWARD);
            Assert
                .fail("Expecting IllegalArgumentException - bad samples number");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }

    /** Test of transformer for the sine function. */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        FastCosineTransformer transformer;
        transformer = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);
        double min, max, result[];
        final double tolerance = 1E-12;
        final int N = 9;

        final double expected[] =
        {
            0.0, 3.26197262739567, 0.0, -2.17958042710327, 0.0,
            -0.648846697642915, 0.0, -0.433545502649478, 0.0
        };
        min = 0.0;
        max = 2.0 * FastMath.PI * N / (N - 1);
        result = transformer.transform(f, min, max, N, TransformType.FORWARD);
        for (int i = 0; i < N; i++) {
            Assert.assertEquals(expected[i], result[i], tolerance);
        }

        min = -FastMath.PI;
        max = FastMath.PI * (N + 1) / (N - 1);
        result = transformer.transform(f, min, max, N, TransformType.FORWARD);
        for (int i = 0; i < N; i++) {
            Assert.assertEquals(-expected[i], result[i], tolerance);
        }
    }
}
