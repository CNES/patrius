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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.frames;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.SortedMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.validation.externaltools.TransitionMatricesDataLoader;

/**
 * <p>
 * The purpose of this test class is to validate the frame transformation from ITRF to ICRF, both global and step by
 * step.
 * </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: IERSTransformationsValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class IERSTransformationsValTest {

    /** Position epsilon. */
    private static final double POSEPS = 1e-2;
    /** Velocity epsilon. */
    private static final double VELEPS = 1e-4;
    /** Validate instance. */
    private static Validate val;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ITRF to GCRF transformation
         * 
         * @featureDescription test whether the transformation corresponds to the IERS standards
         * 
         * @coveredRequirements DV-REPERES_110
         */
        IERSTransformation
    }

    @BeforeClass
    public static void setupcl() throws IOException {
        val = new Validate(IERSTransformationsValTest.class);
    }

    @AfterClass
    public static void teardowncl() throws IOException, URISyntaxException {
        val.produceLog();
    }

    /**
     * Before the tests.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("testIERS");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(false));
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#IERSTransformation}
     * 
     * @testedMethod {@link FramesFactory#getITRF()}
     * @testedMethod {@link FramesFactory#getTIRF()}
     * @testedMethod {@link FramesFactory#getCIRF()}
     * @testedMethod {@link FramesFactory#getICRF()}
     * 
     * @description the purpose of this test is to validate the transformation between terrestrial and celestial frames.
     *              It will transform a bulletin and compare it step by step to references.
     * 
     * @input a bulletin expressed in the ITRS frame
     * 
     * @output a bulletin expressed successively in the TIRF, CIRF and GCRF frames
     * 
     * @testPassCriteria the transforms give the correct results
     * 
     * @see "Référentiels des conventions IERS pour les satellites en orbite terrestre"
     * 
     * @comments no comments
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     */
    @Test
    public final void testTransformationsIERS() throws PatriusException {
        // date of the bulletin
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2009, 10, 31), new TimeComponents(06, 26, 00),
            TimeScalesFactory.getUTC());

        // get the successive frames and the transformations between them
        // the boolean values says whether to IGNORE the tide effects or not.
        final Frame itrf = FramesFactory.getITRF();
        final Frame tirf = FramesFactory.getTIRF();
        final Transform itrfTOtirf = itrf.getTransformTo(tirf, date);
        final Frame cirf = FramesFactory.getCIRF();
        final Transform tirfTOcirf = tirf.getTransformTo(cirf, date);
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform cirfTOgcrf = cirf.getTransformTo(gcrf, date);
        // final Transform itrfTOgcrf = itrf.getTransformTo(gcrf, date);

        // Coordinates in the ITRF frame
        final Vector3D pITRF = new Vector3D(5723612.614, 3104440.257, 4140628.559);
        final Vector3D vITRF = new Vector3D(-4396.69184, 1009.57490, 5315.04476);
        final PVCoordinates pvITRF = new PVCoordinates(pITRF, vITRF);

        // Coordinates in the TIRF frame
        final Vector3D pTIRF = new Vector3D(5723607.850378315896, 3104445.416114046704, 4140631.275721705519);
        final Vector3D vTIRF = new Vector3D(-4396.697954802033564, 1009.581522671059474, 5315.038443770359663);
        // final PVCoordinates expectedPvTIRF = new PVCoordinates(pTIRF, vTIRF);

        // Coordinates in the CIRF frame
        final Vector3D pCIRF = new Vector3D(-6275764.920040362515, 1735523.792673145188, 4140631.275721705519);
        final Vector3D vCIRF = new Vector3D(2339.231892342299034, -4235.214049399352007, 5315.038443770359663);
        // final PVCoordinates expectedPvCIRF = new PVCoordinates(pCIRF, vCIRF);

        // Coordinates in the GCRF frame
        final Vector3D pGCRF = new Vector3D(-6271696.077571173199, 1735602.948251032736, 4146758.522584746592);
        final Vector3D vGCRF = new Vector3D(2344.449804125199080, -4235.112594866310246, 5312.819793781392036);
        // final PVCoordinates expectedPvGCRF = new PVCoordinates(pGCRF, vGCRF);

        // put this on to see the translation present in the ITRF2000 and its effects
        // System.out.println("translation" + itrfTOtirf.getTranslation().toString());
        // itrfTOtirf = new Transform(itrfTOtirf.getRotation());

        // compute the successive transformations

        final PVCoordinates orekitPvTIRF = itrfTOtirf.transformPVCoordinates(pvITRF);
        final PVCoordinates orekitPvCIRF = tirfTOcirf.transformPVCoordinates(orekitPvTIRF);
        final PVCoordinates orekitPvGCRF = cirfTOgcrf.transformPVCoordinates(orekitPvCIRF);
        // final PVCoordinates orekitPvtotal = itrfTOgcrf.transformPVCoordinates(pvITRF);

        // compare to the expected coordinates
        // ICRF to TIRF transformation
        // System.out.println();
        // System.out.println("TIRF Position distance : " + orekitPvTIRF.getPosition().distance(pTIRF));
        // System.out.println("TIRF Velocity distance : " + orekitPvTIRF.getVelocity().distance(vTIRF));
        double pexpected = orekitPvTIRF.getPosition().distance(pTIRF);
        val.assertEquals(pexpected, 0., POSEPS, 0., POSEPS, "pTIRF");
        double vexpected = orekitPvTIRF.getVelocity().distance(vTIRF);
        val.assertEquals(vexpected, 0., VELEPS, 0., POSEPS, "vTIRF");

        // TIRF to CIRF transformation
        // System.out.println();
        // System.out.println("CIRF Position distance : " + orekitPvCIRF.getPosition().distance(pCIRF));
        // System.out.println("CIRF Velocity distance : " + orekitPvCIRF.getVelocity().distance(vCIRF));
        pexpected = orekitPvCIRF.getPosition().distance(pCIRF);
        val.assertEquals(pexpected, 0., POSEPS, 0., POSEPS, "pCIRF");
        vexpected = orekitPvCIRF.getVelocity().distance(vCIRF);
        val.assertEquals(vexpected, 0., VELEPS, 0., POSEPS, "vCIRF");

        // CIRF to GCRF transformation
        // System.out.println();
        // System.out.println("GCRF Position distance : " + orekitPvGCRF.getPosition().distance(pGCRF));
        // System.out.println("GCRF Velocity distance : " + orekitPvGCRF.getVelocity().distance(vGCRF));
        pexpected = orekitPvGCRF.getPosition().distance(pGCRF);
        val.assertEquals(pexpected, 0., POSEPS, 0., POSEPS, "pGCRF");
        vexpected = orekitPvGCRF.getVelocity().distance(vGCRF);
        val.assertEquals(vexpected, 0., VELEPS, 0., POSEPS, "vGCRF");

        // Expected rotation W
        final double[][] expectedW = {
            { 9.999999999993381961e-01, 2.383462949435531011e-11, -1.150475543010272969e-06 },
            { -2.240113099761196033e-11, 9.999999999992237321e-01, 1.246005189287953182e-06 },
            { 1.150475543039077807e-06, -1.246005189261356559e-06, 9.999999999985619281e-01 } };
        // System.out.println();

        // Orekited rotation
        final double[][] orekitW = itrfTOtirf.getRotation().revert().getMatrix();

        // System.out.println("orekitedW * expectedRotationT = I");
        assertEqualRotationMatrix(orekitW, expectedW, 1e-9);

        // Expected rotation R
        final double[][] expectedR = { { -0.7201449480380475165, -0.6938236474892433003, 0.0 },
            { 0.6938236474892433003, -0.7201449480380475165, 0.0 }, { 0.0, 0.0, 1.0 } };
        // System.out.println();

        // Orekited rotation
        final double[][] orekitR = tirfTOcirf.getRotation().revert().getMatrix();

        // System.out.println("orekitedR * expectedRotationT = I");
        assertEqualRotationMatrix(expectedR, orekitR, 1e-9);

        // Expected rotation Q
        final double[][] expectedQ = { { 0.9999995179028657866, -5.033234478788801719e-9, 9.819338246106229951e-4 },
            { -1.371810011903895937e-08, 0.9999999998176651861, 1.909632555013464198e-05 },
            { -9.819338245276984425e-04, -1.909632981411733647e-05, 0.9999995177205309727 } };
        // System.out.println();

        // Orekited rotation
        final double[][] orekitQ = cirfTOgcrf.getRotation().revert().getMatrix();

        // System.out.println("orekitedQ * expectedRotationT = I");
        assertEqualRotationMatrix(expectedQ, orekitQ, 1e-9);
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#IERSTransformation}
     * 
     * @testedMethod {@link FramesFactory#getITRF()}
     * @testedMethod {@link FramesFactory#getCIRF()}
     * @testedMethod {@link FramesFactory#getICRF()}
     * 
     * @description the purpose of this test is to validate the use of multiple transformations between terrestrial and
     *              celestial frames and back
     * 
     * @input a bulletin expressed in the ITRS frame
     * 
     * @output a bulletin expressed in the ITRF frame
     * 
     * @testPassCriteria the transforms give the correct results
     * 
     * @see "Référentiels des conventions IERS pour les satellites en orbite terrestre"
     * 
     * @comments no comments
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     */
    @Test
    public final void testStressTransformationsIERS() throws PatriusException {
        // date of the bulletin
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2009, 10, 31), new TimeComponents(06, 26, 00),
            TimeScalesFactory.getUTC());

        // get the successive frames and the transformations between them
        final Frame itrf = FramesFactory.getITRF();
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform itrfTOgcrf = itrf.getTransformTo(gcrf, date);
        final Transform gcrfTOitrf = gcrf.getTransformTo(itrf, date);

        // Coordinates in the ITRF frame
        final Vector3D pITRF = new Vector3D(5723612.614, 3104440.257, 4140628.559);
        final Vector3D vITRF = new Vector3D(-4396.69184, 1009.57490, 5315.04476);
        final PVCoordinates pvITRF = new PVCoordinates(pITRF, vITRF);

        // compute the successive transformations
        PVCoordinates orekitPvtotal;
        PVCoordinates orekitPvtotalReverse = pvITRF;

        for (int i = 0; i < 100000; i++) {
            orekitPvtotal = itrfTOgcrf.transformPVCoordinates(orekitPvtotalReverse);
            orekitPvtotalReverse = gcrfTOitrf.transformPVCoordinates(orekitPvtotal);
        }

        // compare to the expected coordinates
        // System.out.println("ITRF Position distance : " + orekitPvtotalReverse.getPosition().distance(pITRF));
        // System.out.println("ITRF Velocity distance : " + orekitPvtotalReverse.getVelocity().distance(vITRF));
        assertTrue(orekitPvtotalReverse.getPosition().distance(pITRF) < 1e-3);
        assertTrue(orekitPvtotalReverse.getVelocity().distance(vITRF) < 1e-6);
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#IERSTransformation}
     * 
     * @testedMethod {@link FramesFactory#getTIRF()}
     * @testedMethod {@link FramesFactory#getCIRF()}
     * 
     * @description the purpose of this test is to validate the transformation between TIRF and CIRF
     * 
     * @input a list of dates
     * 
     * @output a list of matrices expressed in the TIRF, CIRF and GCRF frames
     * 
     * @testPassCriteria the transforms give the correct results, the position angle of Earth matches.
     * 
     * @see "Référentiels des conventions IERS pour les satellites en orbite terrestre"
     * 
     * @comments this test method contains some code blocks that are commented away. Those were useful in the set up of
     *           the test and the analysis of the results, and the choice has been made to leave them should the need to
     *           use them arise.
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     */
    @Test
    public final void testTransformationTIRFtoCIRF() throws PatriusException {
        /*
         * load the reference matrix ephemeris and get the dates.
         */
        final AbsoluteDate referenceDate = new AbsoluteDate(DateComponents.FIFTIES_EPOCH, TimeComponents.H00,
            TimeScalesFactory.getUTC());

        // secondly for the TIRF to CIRF transformation
        final TransitionMatricesDataLoader loaderTIRF_CIRF = new TransitionMatricesDataLoader(
            "Matrices_TIRS-CIRS_LinearTide.txt", referenceDate);
        loaderTIRF_CIRF.loadData();
        final SortedMap<AbsoluteDate, double[][]> refTIRF_CIRF = loaderTIRF_CIRF.getEphemeris();

        // get the set of dates
        final Set<AbsoluteDate> dates = refTIRF_CIRF.keySet();

        // declare the frames
        final Frame tirf = FramesFactory.getTIRF();
        final Frame cirf = FramesFactory.getCIRF();

        /*
         * x and y tables for the interpolation of the time or angle deviations. This is then used to analyse the data
         * by putting the values in an Excel worksheet.
         */
        // double[] x = new double[73];
        // double[] y = new double[x.length];
        // int index = 0;
        // for (int i = 0; i < x.length; i++) {
        // x[i] = i;
        // }

        /*
         * the date when to stop the test to avoid problems with the leap second surge of angle deviation. The first
         * surge appears on the 1st of July in 1981, and therefore the test stops before the 30th of June in 1981
         */
        final AbsoluteDate stopDate = new AbsoluteDate(1981, 06, 26, TimeScalesFactory.getUTC());
        // for each date in the set
        for (final AbsoluteDate date : dates) {
            if (date.compareTo(stopDate) > 0) {
                break;
            }
            // System.out.println(date.toString());
            // get the transformation
            final Transform tirfTOcirf = tirf.getTransformTo(cirf, date);
            // Transform tirfTOcirf = cirf.getTransformTo(tirf, date);

            // get the rotation matrix and rotation rate from the transformation object
            final double[][] rotationMatrix2 = tirfTOcirf.getRotation().revert().getMatrix();
            final Vector3D rotationRate2 = tirfTOcirf.getRotationRate();

            // get the transition matrix of reference at the current date
            final double[][] tirfTOcirfTransition = refTIRF_CIRF.get(date);

            // get the references : rotation matrix and rotation rate
            final double[][] rotationMatrix2ref = this.extractRotation(tirfTOcirfTransition, true);
            final double[][] rotationRate2ref = this.extractRotation(tirfTOcirfTransition, false);

            // used to fill the y table in with the angles, the equivalent time deviation, etc...
            // double angleOre = FastMath.acos(rotationMatrix2[0][0])*180/FastMath.PI;
            // double angleRef = FastMath.acos(rotationMatrix2ref[0][0])*180/FastMath.PI;
            // if (index<x.length) {
            // equivalent time deviation between the angles
            // y[index] = (angleRef - angleOre)*4*60;

            // angles
            // y[index] = angleRef;
            // y[index] = angleOre;

            // angle deviation
            // y[index] = (angleRef - angleOre);

            // index++;
            // show the date when the angle surges, meaning that a leap second happened
            // if (Math.abs(angleRef - angleOre) > 1e-7) {
            // System.out.println(date.toString());
            // }
            // }

            // print the matrices in the console
            // printMatrix33(rotationMatrix2);
            // System.out.println();
            // printMatrix33(rotationMatrix2ref);
            // System.out.println();

            // //assert that the transition matrices equal the reference ones
            assertEqualRotationMatrix(rotationMatrix2, rotationMatrix2ref, 1e-6);
            //
            // //compare the rotation rate matrice
            this.assertEqualRotationRate(rotationRate2, rotationRate2ref, 1e-19);
        }

        /*
         * Use splines to interpolate the get the function f : x -> y.
         */
        // UnivariateRealInterpolator interpolator = new SplineInterpolator();
        // UnivariateRealFunction function = interpolator.interpolate(x, y);
        //
        // print in the console the values of the function for each day.
        // for (int i = 0; i < y.length-1; i++) {
        // System.out.println(function.value(i));
        // System.out.println(function.value(i+0.2));
        // System.out.println(function.value(i+0.4));
        // System.out.println(function.value(i+0.6));
        // System.out.println(function.value(i+0.8));
        // }
    }

    /**
     * Extract the rotation and the rotation rate matrices from the transition matrix given as reference. This
     * transition matrix was read column by column, which means that it is transposed. Its transposed matrix hase the
     * form {{R, 0},{WR, R}}
     * 
     * @param transitionMatrix
     *        the reference matrix
     * @param getMatrix
     *        tells whether to return the rotation matrix, or if false the rotation rate matrix. This is done in
     *        order to avoid the duplication of code.
     * @return the rotation or rotation rate matrix
     * 
     * @since 1.0
     */
    private double[][] extractRotation(final double[][] transitionMatrix, final boolean getMatrix) {
        // get the rotation matrix and rotation rate from the transformation object
        final double[][] rotationMatrix = new double[3][3];
        double[][] rotationRate = new double[3][3];

        // pull the rotation matrix from the left upper corner
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                rotationMatrix[j][i] = transitionMatrix[i][j];
            }
        }

        if (getMatrix) {
            // return the rotation matrix
            return rotationMatrix;
        }

        // pull the product of the rate matrix and the rotation matrix from the left upper corner
        final double[][] product = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                product[j - 3][i] = transitionMatrix[i][j];
            }
        }

        /*
         * get the rate matrix : only works if the left upper corner really is a rotation. The lower left corner
         * contains WR, which means that by multiplying it by Rt (R transposed) on the right, we should obtain W (R*Rt =
         * I)
         */
        rotationRate = multiplyMatrix33(product, transposeMatrix33(rotationMatrix));

        // return the rotation rate matrix
        return rotationRate;
    }

    /**
     * Asserts that the rotation rate vector and the rotation rate matrix describe the same rotation.
     * 
     * @param rotationRate
     *        the vector
     * @param rotationRateRef
     *        the matrix
     * @param epsilon
     *        the comparison precision
     * 
     * @since 1.0
     */
    private void assertEqualRotationRate(final Vector3D rotationRate, final double[][] rotationRateRef,
                                         final double epsilon) {
        // get the components of the rotation rate
        final double w1 = rotationRate.getX();
        final double w2 = rotationRate.getY();
        final double w3 = rotationRate.getZ();

        // check if the terms are equals
        // absolute comparison
        val.assertEquals(w1, rotationRateRef[1][1], epsilon, rotationRateRef[1][1], epsilon, "w1");
        val.assertEquals(w2, rotationRateRef[0][2], epsilon, rotationRateRef[0][2], epsilon, "w2");
        val.assertEquals(w3, rotationRateRef[0][1], epsilon, rotationRateRef[0][1], epsilon, "w3");

        // relative comparison
        // assertTrue(Precision.equalsWithRelativeTolerance(w1, rotationRateRef[1][1], epsilon));
        // assertTrue(Precision.equalsWithRelativeTolerance(w2, rotationRateRef[0][2], epsilon));
        // assertTrue(Precision.equalsWithRelativeTolerance(w3, rotationRateRef[0][1], epsilon));
    }

    /**
     * Transposes a 3x3 matrix.
     * 
     * @param matrix
     *        to transpose
     * @return the transposed matrix
     * 
     * @since 1.0
     */
    public static double[][] transposeMatrix33(final double[][] matrix) {
        final double[][] transposeMatrix = new double[3][3];
        for (int i = 0; i < transposeMatrix.length; i++) {
            for (int j = 0; j < transposeMatrix[0].length; j++) {
                transposeMatrix[i][j] = matrix[j][i];
            }
        }
        return transposeMatrix;
    }

    /**
     * Multiplies two 3x3 matrices in the order they are given.
     * 
     * @param matrix1
     *        the first factor
     * @param matrix2
     *        the second factor
     * @return the product of the matrices.
     * 
     * @since 1.0
     */
    public static double[][] multiplyMatrix33(final double[][] matrix1, final double[][] matrix2) {
        final double[][] multiplyMatrix = new double[3][3];
        for (int i = 0; i < multiplyMatrix.length; i++) {
            for (int j = 0; j < multiplyMatrix[0].length; j++) {
                multiplyMatrix[i][j] = matrix1[i][0] * matrix2[0][j] + matrix1[i][1] * matrix2[1][j] + matrix1[i][2]
                    * matrix2[2][j];
            }
        }
        return multiplyMatrix;
    }

    /**
     * Asserts that the matrix is the identity. The diagonal terms are checked by a relative comparison to 1.
     * 
     * @param matrix
     *        the matrix to check
     * @param epsilon
     *        the comparison error accepted
     * 
     * @since 1.0
     */
    public static void assertMatrixI(final double[][] matrix, final double epsilon) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                final double actual = matrix[i][j];
                final String matStr = "ident " + i + " " + j;
                if (i == j) {
                    val.assertEqualsWithRelativeTolerance(actual, 1.0, epsilon, 1.0, epsilon, matStr);
                } else {
                    val.assertEquals(actual, 0.0, epsilon, 0.0, epsilon, matStr);
                }
            }
        }
    }

    /**
     * Asserts that two matrices define the same rotation with the use of the property : R x Rt = I, applied as R1 x R2t
     * = I
     * 
     * @param matrix1
     *        the first rotation matrix
     * @param matrix2
     *        the second rotation matrix
     * @param epsilon
     *        the precision of the comparison
     * 
     * @since 1.0
     */
    private static void assertEqualRotationMatrix(final double[][] matrix1, final double[][] matrix2,
                                                  final double epsilon) {
        final double[][] transposed1 = transposeMatrix33(matrix1);
        final double[][] mult = multiplyMatrix33(transposed1, matrix2);
        assertMatrixI(mult, epsilon);
    }
}
