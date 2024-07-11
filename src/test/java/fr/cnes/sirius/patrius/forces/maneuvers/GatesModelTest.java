/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.7:DM:DM-2818:18/05/2021:[PATRIUS|COLOSUS] Classe GatesModel
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.CheckUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * Unit tests for {@link GatesModel}.
 *
 * @author GMV
 */
public class GatesModelTest {
    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0;
    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /**
     * Sets the default locale.
     */
    @BeforeClass
    public static void setDefaultLocale() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests the constructor and the basic getters.
     */
    @Test
    public void testConstructor() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.01;

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);
        Assert.assertEquals(sigmaMagnitude, gatesModel.getSigmaMagnitude(), 0.);
        Assert.assertEquals(sigmaDirection, gatesModel.getSigmaDirection(), 0.);
    }

    /**
     * Tests the methods:<br>
     * {@link GatesModel#getCovarianceMatrix3x3(Vector3D)}<br>
     * {@link GatesModel#getCovarianceMatrix3x3(Vector3D, double, double)}<br>
     * with a non-zero &Delta;V vector.
     */
    @Test
    public void testGetCovarianceMatrix3x3() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.01;
        final Vector3D deltaV = new Vector3D(0.01, 0.5, 0.02);

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);

        final double[][] data = { { +1.280002045612029E-05, +1.224817518332142E-05, +4.899270073328567E-07 },
                { +1.224817518332142E-05, +6.249638161185160E-04, +2.449635036664249E-05 },
                { +4.899270073328567E-07, +2.449635036664249E-05, +1.353491096711955E-05 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        RealMatrix computed;

        computed = gatesModel.getCovarianceMatrix3x3(deltaV);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);

        computed = GatesModel.getCovarianceMatrix3x3(deltaV, sigmaMagnitude, sigmaDirection);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods:<br>
     * {@link GatesModel#getCovarianceMatrix3x3(Vector3D)}<br>
     * {@link GatesModel#getCovarianceMatrix3x3(Vector3D, double, double)}<br>
     * with a zero &Delta;V vector.
     */
    @Test
    public void testGetCovarianceMatrix3x3ZeroDeltaV() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.01;
        final Vector3D deltaV = Vector3D.ZERO;

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);
        final RealMatrix expected = new Array2DRowRealMatrix(3, 3);

        RealMatrix computed;

        computed = gatesModel.getCovarianceMatrix3x3(deltaV);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);

        computed = GatesModel.getCovarianceMatrix3x3(deltaV, sigmaMagnitude, sigmaDirection);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods:<br>
     * {@link GatesModel#getCovarianceMatrix6x6(Vector3D)}<br>
     * {@link GatesModel#getCovarianceMatrix6x6(Vector3D, double, double)}<br>
     * with a non-zero &Delta;V vector.
     */
    @Test
    public void testGetCovarianceMatrix6x6() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.01;
        final Vector3D deltaV = new Vector3D(0.01, 0.5, 0.02);

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);

        final double[][] data = {
                { +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00,
                        +0.000000000000000E+00, +0.000000000000000E+00 },
                { +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00,
                        +0.000000000000000E+00, +0.000000000000000E+00 },
                { +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00,
                        +0.000000000000000E+00, +0.000000000000000E+00 },
                { +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00, +1.280002045612029E-05,
                        +1.224817518332142E-05, +4.899270073328567E-07 },
                { +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00, +1.224817518332142E-05,
                        +6.249638161185160E-04, +2.449635036664249E-05 },
                { +0.000000000000000E+00, +0.000000000000000E+00, +0.000000000000000E+00, +4.899270073328567E-07,
                        +2.449635036664249E-05, +1.353491096711955E-05 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        RealMatrix computed;

        computed = gatesModel.getCovarianceMatrix6x6(deltaV);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);

        computed = GatesModel.getCovarianceMatrix6x6(deltaV, sigmaMagnitude, sigmaDirection);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods:<br>
     * {@link GatesModel#getCovarianceMatrix6x6(Vector3D)}<br>
     * {@link GatesModel#getCovarianceMatrix6x6(Vector3D, double, double)}<br>
     * with a zero &Delta;V vector.
     */
    @Test
    public void testGetCovarianceMatrix6x6ZeroDeltaV() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.01;
        final Vector3D deltaV = Vector3D.ZERO;

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);
        final RealMatrix expected = new Array2DRowRealMatrix(6, 6);

        RealMatrix computed;

        computed = gatesModel.getCovarianceMatrix6x6(deltaV);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);

        computed = GatesModel.getCovarianceMatrix6x6(deltaV, sigmaMagnitude, sigmaDirection);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods:<br>
     * {@link GatesModel#getMeanDeltaV(Vector3D)}<br>
     * {@link GatesModel#getMeanDeltaV(Vector3D, double)}<br>
     * with a non-zero &Delta;V vector.
     */
    @Test
    public void testGetMeanDeltaV() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.5;
        final Vector3D deltaV = new Vector3D(0.01, 0.5, 0.02);

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);
        final Vector3D expected = new Vector3D(+8.824969025846063E-03, +4.412484512922978E-01, +1.764993805169191E-02);

        Vector3D computed;

        computed = gatesModel.getMeanDeltaV(deltaV);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);

        computed = GatesModel.getMeanDeltaV(deltaV, sigmaDirection);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods:<br>
     * {@link GatesModel#getMeanDeltaV(Vector3D)}<br>
     * {@link GatesModel#getMeanDeltaV(Vector3D, double)}<br>
     * with a zero &Delta;V vector.
     */
    @Test
    public void testGetMeanDeltaVZeroDeltaV() {
        final double sigmaMagnitude = 0.05;
        final double sigmaDirection = 0.5;
        final Vector3D deltaV = Vector3D.ZERO;

        final GatesModel gatesModel = new GatesModel(sigmaMagnitude, sigmaDirection);
        final Vector3D expected = Vector3D.ZERO;

        Vector3D computed;

        computed = gatesModel.getMeanDeltaV(deltaV);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);

        computed = GatesModel.getMeanDeltaV(deltaV, sigmaDirection);
        CheckUtils.checkEquality(expected, computed, ABSTOL, RELTOL);
    }
}
