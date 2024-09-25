/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularValueDecomposition;
import fr.cnes.sirius.patrius.math.optim.joptimizer.TestUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * @author x
 *
 */
public class Matrix1NornRescalerTest extends TestCase {
    
    /** String factorization/matrix */
    final String pathFactMat = "factorization" + File.separator + "matrix";
    /** String .txt */
    final String txt = ".txt";
    /** String space */
    final String space = " ";
    /** String 7 */
    final String seven = "7";

    /**
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleScalingNoSymm() throws PatriusException {

        final double[][] a = new double[][] { { 1, 0, 0 }, { 0, 0, 2 }, { 2, 3, 0 }, { 0, 0, 4 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final MatrixRescaler rescaler = new Matrix1NornRescaler();
        final RealVector[] uV = rescaler.getMatrixScalingFactors(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(uV[0], aMatrix, uV[1]);
        final double cn0 = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cn1 = new SingularValueDecomposition(aScaled).getConditionNumber();
        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();
        assertTrue(rescaler.checkScaling(aMatrix, uV[0], uV[1]));
        assertFalse(cn1 > cn0);// not guaranteed by the rescaling
        assertFalse(norm1 > norm0);// not guaranteed by the rescaling
    }

    /**
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleScalingSymm() throws PatriusException {

        final double[][] a = new double[][] { { 1., 0.5e7, 0 }, { 0.5e7, 2., 0 }, { 0., 0., 3.e-9 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final MatrixRescaler rescaler = new Matrix1NornRescaler(1.e-3);
        final RealVector u = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(u, aMatrix, u);
        final double cn0 = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cn1 = new SingularValueDecomposition(aScaled).getConditionNumber();
        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();
        assertTrue(rescaler.checkScaling(aMatrix, u, u));
        assertFalse(cn1 > cn0);
        assertFalse(norm1 > norm0);
    }

    /**
     * Test with a symmetric not definite matrix.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleScalingSymm2() throws PatriusException {

        final double[][] a = new double[][] {
                { -5.179606612022133, -0.02504297960603635, 24.852749934356265,
                        -0.0022539455614544743 },
                { -0.025042979606036346, -5.164290426157521, -25.64347279666032,
                        0.003046228687793972 },
                { 24.852749934356265, -25.64347279666032, -242.263120568969, -0.01274431884602755 },
                { -0.0022539455614544743, 0.003046228687793972, -0.01274431884602755,
                        5.549439055094576E-4 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final MatrixRescaler rescaler = new Matrix1NornRescaler(1.e-3);
        final RealVector u = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(u, aMatrix, u);
        final double cn0 = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cn1 = new SingularValueDecomposition(aScaled).getConditionNumber();
        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();
        assertTrue(rescaler.checkScaling(aMatrix, u, u));
        assertFalse(cn1 > cn0);
        assertFalse(norm1 > norm0);
    }

    /**
     * Test with a symmetric positive definite matrix.
     * 
     * @throws PatriusException if an error occurs
     */
    public void testSimpleScalingSymm3() throws PatriusException {

        final double[][] a = new double[][] { { 15.228317792554328, 0.0 },
                { 37.94218078014462, 94.59822454787903 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final MatrixRescaler rescaler = new Matrix1NornRescaler(1.e-3);
        final RealVector u = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(u, aMatrix, u);
        final double cn0 = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cn1 = new SingularValueDecomposition(aScaled).getConditionNumber();
        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();
        assertTrue(rescaler.checkScaling(AlgebraUtils.fillSubdiagonalSymmetricMatrix(aMatrix), u, u));
        assertFalse(cn1 > cn0);
        assertFalse(norm1 > norm0);
    }

    /**
     * Test of the matrix in Gajulapalli example 2.1.
     * It is a Pathological Square Matrix.
     * @throws PatriusException if an error occurs
     * @see Gajulapalli, Lasdon "Scaling Sparse Matrices for Optimization Algorithms"
     */
    public void testPathologicalScalingNoSymm() throws PatriusException {

        final double[][] a = new double[][] { { 1.e0, 1.e10, 1.e20 }, { 1.e10, 1.e30, 1.e50 },
                { 1.e20, 1.e40, 1.e80 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final MatrixRescaler rescaler = new Matrix1NornRescaler();
        final RealVector[] uV = rescaler.getMatrixScalingFactors(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(uV[0], aMatrix, uV[1]);
        final double cn0 = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cn1 = new SingularValueDecomposition(aScaled).getConditionNumber();
        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();
        assertTrue(rescaler.checkScaling(aMatrix, uV[0], uV[1]));
        assertFalse(cn1 > cn0);// not guaranteed by the rescaling
        assertFalse(norm1 > norm0);// not guaranteed by the rescaling
    }

    /**
     * Test of the matrix in Gajulapalli example 3.1.
     * It is a Pathological Square Matrix.*
     * @throws PatriusException if an error occurs
     * @see Gajulapalli, Lasdon "Scaling Sparse Matrices for Optimization Algorithms"
     */
    public void testPathologicalScalingSymm() throws PatriusException {

        final double[][] a = new double[][] { { 1.e0, 1.e20, 1.e10, 1.e0 },
                { 1.e20, 1.e20, 1.e0, 1.e40 }, { 1.e10, 1.e0, 1.e40, 1.e50 },
                { 1.e0, 1.e40, 1.e50, 1.e0 } };
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final MatrixRescaler rescaler = new Matrix1NornRescaler();
        final RealVector[] uV = rescaler.getMatrixScalingFactors(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(uV[0], aMatrix, uV[1]);
        final double cn0 = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cn1 = new SingularValueDecomposition(aScaled).getConditionNumber();
        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();
        assertTrue(rescaler.checkScaling(aMatrix, uV[0], uV[1]));
        assertFalse(cn1 > cn0);// not guaranteed by the rescaling
        assertFalse(norm1 > norm0);// not guaranteed by the rescaling
    }

    /**
     * Test the matrix norm before and after scaling.
     * Note that scaling is not guaranteed to give a better condition number.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testMatrixNormScaling7() throws PatriusException, IOException {
        final String matrixId = seven;
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(pathFactMat + matrixId + txt, space.charAt(0));
        final RealMatrix aMatrix = new BlockRealMatrix(a);

        final MatrixRescaler rescaler = new Matrix1NornRescaler(1.e-3);
        final RealVector u = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(u, aMatrix, u);

        final double norm0 = aMatrix.getNorm();
        final double norm1 = aScaled.getNorm();

        assertTrue(rescaler.checkScaling(aMatrix, u, u));
        assertFalse(norm1 > norm0);// note: this is not guaranteed
    }

/**
	 * Test the rescaling of a is diagonal with some element < 1.e^16.
	 * 
	 * @throws PatriusException if an error occurs
	 */
    public void testGetConditionNumberDiagonal() throws PatriusException {

        final double[] a = new double[] { 1.E-17, 168148.06378241107, 5.333317404302006E-11,
                9.724301428859958E-11, 4.343924031677448E-10, 53042.618161481514,
                1.2550281021203455E-12, 55714.086057404944, 16564.267914476874,
                1.6265469281243343E-12, 7.228925943265697E-11, 19486.564364392565,
                315531.47099006834, 236523.83171379057, 202769.6735227342, 2.4925304834427544E-13,
                2.7996276724404553E-13, 2.069135405949759E-12, 2530058.817281487,
                4.663208124742273E-15, 2.5926311225234777E-12, 2454865.060218241,
                7.564594931528804E-14, 2.944935006524965E-13, 7.938509176903875E-13,
                2546775.969599124, 4.36659839706981E-15, 3.772728220251383E-9, 985020.987902404,
                971715.0611851265, 1941150.6250316042, 3.3787344131154E-10, 2.8903135775881254E-11,
                1263.9864262585922, 873899.9914494107, 153097.08545910483, 3.738245318154646E-11,
                1267390.1117847422, 6.50494734416794E-10, 3.588511203703992E-11,
                1231.6604599987518, 3.772810869560189E-9, 85338.92515278656,
                3.7382488244903144E-11, 437165.36165859725, 9.954549425029816E-11,
                1.8376434881340742E-9, 86069.90894488744, 1.2087907925307217E11,
                1.1990761432334067E11, 1.163424797835085E11, 1.1205515861349094E11,
                1.2004378300642543E11, 8.219259112337953E8, 1.1244633984805448E-11,
                1.1373907469271675E-12, 1.9743774924311214E-12, 6.301661187526759E-16,
                6.249382377266375E-16, 8.298198098742164E-16, 6.447686765999485E-16,
                1.742229837554675E-16, 1.663041351618635E-16 };

        final RealMatrix aMatrix = AlgebraUtils.diagonal(new ArrayRealVector(a));
        final MatrixRescaler rescaler = new Matrix1NornRescaler();
        final RealVector u = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(u, aMatrix, u);

        final double cnOriginal = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cnScaled = new SingularValueDecomposition(aScaled).getConditionNumber();

        assertTrue(rescaler.checkScaling(aMatrix, u, u));// NB: this MUST BE guaranteed by the
                                                         // scaling algorithm
        assertTrue(cnScaled < cnOriginal);// NB: this IS NOT guaranteed by the scaling algorithm
    }

    /**
     * Test the condition number before and after scaling.
     * Note that scaling is not guaranteed to give a better condition number.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testGetConditionNumberFromFile7() throws PatriusException, IOException {

        final String matrixId = seven;
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(pathFactMat + matrixId + txt, space.charAt(0));
        final BlockRealMatrix aMatrix = (BlockRealMatrix) new BlockRealMatrix(a);

        final MatrixRescaler rescaler = new Matrix1NornRescaler();
        final RealVector uv = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(uv, aMatrix, uv);

        final double cnOriginal = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cnScaled = new SingularValueDecomposition(aScaled).getConditionNumber();

        assertTrue(rescaler.checkScaling(aMatrix, uv, uv));// NB: this MUST BE guaranteed by the
                                                           // scaling algorithm
        assertTrue(cnScaled < cnOriginal);// NB: this IS NOT guaranteed by the scaling algorithm
    }

    /**
     * Test the condition number before and after scaling.
     * Note that scaling is not guaranteed to give a better condition number.
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testGetConditionNumberFromFile13() throws PatriusException, IOException {

        final String matrixId = "13";
        final double[][] a = TestUtils.loadDoubleMatrixFromFile(pathFactMat + matrixId + ".csv");
        final BlockRealMatrix aMatrix = (BlockRealMatrix) new BlockRealMatrix(a);

        final MatrixRescaler rescaler = new Matrix1NornRescaler();
        final RealVector uv = rescaler.getMatrixScalingFactorsSymm(aMatrix);
        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(uv, aMatrix, uv);

        final double cnOriginal = new SingularValueDecomposition(aMatrix).getConditionNumber();
        final double cnScaled = new SingularValueDecomposition(aScaled).getConditionNumber();

        assertTrue(rescaler.checkScaling(aMatrix, uv, uv));// NB: this MUST BE guaranteed by the
                                                           // scaling algorithm
        assertTrue(cnScaled < cnOriginal);// NB: this IS NOT guaranteed by the scaling algorithm
    }
}
