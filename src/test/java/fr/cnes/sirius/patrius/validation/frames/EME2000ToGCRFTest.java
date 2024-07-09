/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history 19/06/2012
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.frames;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation test class for EME 2000 frame.
 * Test data provided by Bruno Vidal.
 * 
 * @see FramesFactory#getEME2000()
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: EME2000ToGCRFTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class EME2000ToGCRFTest {

    /** Validate instance. */
    private static Validate val;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EME2000 Frame validation test
         * 
         * @featureDescription Make sur the transformation from EME2000 to GCRF in OREKIT is the same as in reference
         *                     software
         * 
         * @coveredRequirements DV-REPERES_120
         */
        VALIDATION_EME2000
    }

    @BeforeClass
    public static void setup() throws IOException {
        Utils.setDataRoot("regular-data");
        val = new Validate(EME2000ToGCRFTest.class);
    }

    @AfterClass
    public static void teardown() throws IOException, URISyntaxException {
        val.produceLog();
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#VALIDATION_EME2000}
     * 
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * 
     * @description Compare transformation from EME2000 to GRCF 2003 and 2010 wrt<br>
     *              <li>CNES script for 2003 IERS convention<br><li>SOPFA software for 2010 IERS convention
     * 
     * @input none
     * 
     * @output transformation matrix from EME200 to GCRF
     * 
     * @testPassCriteria Matrix elements are equal to 1e-14
     * 
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         should not happen
     * 
     */
    @Test
    public void testTransformation() throws PatriusException, IOException {

        // get orekit transformation
        final Transform t = FramesFactory.getEME2000()
            .getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH);
        final Matrix3D OREKIT = new Matrix3D(t.getRotation().revert().getMatrix());

        // non reg
        final Matrix3D OREKIT2003 = new Matrix3D(new double[][] {
            { 0.9999999999999942, 7.078279477857338E-8, -8.056217380986972E-8 },
            { -7.078279744199198E-8, 0.9999999999999971, -3.3060408839805523E-8 },
            { 8.056217146976134E-8, 3.3060414542221364E-8, 0.9999999999999962 }

        });

        this.testMatrix(OREKIT2003, OREKIT, Precision.DOUBLE_COMPARISON_EPSILON);

        // reference matrices data
        final Matrix3D IERS2003 = new Matrix3D(new double[][] {
            { 0.99999999999999423, 0.00000007078279478, -0.00000008056149173 },
            { -0.00000007078279744, 0.99999999999999689, -0.00000003306040884 },
            { 0.00000008056148939, 0.00000003306041454, 0.99999999999999623 } });

        final Matrix3D IERS2010 = new Matrix3D(new double[][] {
            { 0.99999999999999412, 0.00000007078368695, -0.00000008056214212 },
            { -0.00000007078368961, 0.99999999999999700, -0.00000003305943175 },
            { 0.00000008056213978, 0.00000003305943741, 0.99999999999999623 } });

        // reference transformation matrices
        final Matrix3D expected2003 = new Matrix3D(new double[][] {
            { 41999999.9999999925, 0.00000000008271488, -0.00002864734997598 },
            { 0.00000000005991818, 42000000., -0.00000000009329700 },
            { 0.00002864735452797, -0.00000000000614037, 42000000. } });

        final Matrix3D expected2010 = new Matrix3D(new double[][] {
            { 41999999.9999999925, -0.00003747105638239, -0.00000133096873679 },
            { 0.00003747119661120, 42000000.0000000075, -0.00004103955631618 },
            { 0.00000133097162340, 0.00004103777192580, 42000000. } });

        // actual matrices
        final Matrix3D actual2003 = IERS2003.transpose().multiply(OREKIT).multiply(42000000);
        final Matrix3D actual2010 = IERS2010.transpose().multiply(OREKIT).multiply(42000000);

        this.testMatrix(expected2010, actual2010, 10e-7);
        this.testMatrix(expected2003, actual2003, 10e-7);

    }

    /**
     * Test equality of matrices
     * 
     * @param exp
     *        expected matrix
     * @param act
     *        actual matrix
     * @param d
     *        threshold
     */
    private void testMatrix(final Matrix3D exp, final Matrix3D act, final double d) {

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final String acop = "[";
                final String accl = "]";
                final String acmi = "][";
                final String matidx = acop + row + acmi + col + accl;
                final double expected = exp.getEntry(row, col);
                final double actual = act.getEntry(row, col);
                val.assertEqualsWithRelativeTolerance(actual, expected, d, expected, d, "mat" + matidx);
            }
        }

    }
}
