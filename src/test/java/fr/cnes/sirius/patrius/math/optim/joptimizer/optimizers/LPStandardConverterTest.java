/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
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
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.TestUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Standard form conversion test.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 */
public class LPStandardConverterTest extends TestCase {

    /** String lp/standarization/ */
    final String pathStandarization = "lp" + File.separator + "standardization" + File.separator;
    /** String c */
    final String cS = "c";
    /** String G */
    final String gS = "G";
    /** String h */
    final String hS = "h";
    /** String A */
    final String aS = "A";
    /** String b */
    final String bS = "b";
    /** String lb */
    final String lbS = "lb";
    /** String ub */
    final String ubS = "ub";
    /** String .txt */
    final String txt = ".txt";
    /** String sol */
    final String solS = "sol";
    /** String space */
    final String space = " ";
    /** String target */
    final String target = "target";
    /** String standardS */
    final String standardS = "standardS_";
    /** String standardC */
    final String standardC = "standardC_";
    /** String standardA */
    final String standardA = "standardA_";
    /** String standardB */
    final String standardB = "standardB_";
    /** String standardLB */
    final String standardLB = "standardLB_";
    /** String 1 */
    final String one = "1";

    
    /**
     * Standardization of a problem on the form:
     * min(c) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAbLbUb1() throws PatriusException, IOException {

        final String problemId = one;

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(pathStandarization + gS + problemId + 
                txt, space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(pathStandarization + hS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                .operate(MatrixUtils.createRealVector(expectedSol))
                .subtract(MatrixUtils.createRealVector(b)).getNorm();

        // standard form conversion
        final double unboundedLBValue = Double.NEGATIVE_INFINITY;// this is because in the file the
                                                           // unbounded lb are -Infinity values (not
                                                           // the default value)
        final double unboundedUBValue = Double.POSITIVE_INFINITY;// this is because in the file the
                                                           // unbounded ub are +Infinity values
        final LPStandardConverter lpConverter = new LPStandardConverter(unboundedLBValue,
                unboundedUBValue);
        lpConverter.toStandardForm(c, g, h, a, b, lb, ub);
        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        lb = lpConverter.getStandardLB().toArray();
        ub = lpConverter.getStandardUB().toArray();

        // check consistency
        assertEquals(g.length, s);
        assertEquals(s + lpConverter.getOriginalN(), n);
        assertEquals(lb.length, n);
        assertEquals(ub.length, n);

        // check constraints
        final RealMatrix gOrig = new Array2DRowRealMatrix(g);
        final RealVector hOrig = new ArrayRealVector(h);
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final RealVector expectedSolVector = new ArrayRealVector(expectedSol);
        final RealVector gxh = gOrig.operate(expectedSolVector).subtract(hOrig);// G.x - h
        final RealVector slackVariables = new ArrayRealVector(s);
        for (int i = 0; i < s; i++) {
            slackVariables.setEntry(i, 0. - gxh.getEntry(i));// the difference from 0
            assertTrue(slackVariables.getEntry(i) >= 0.);
        }
        final RealVector sol = slackVariables.append(expectedSolVector);
        final RealVector axmb = aStandard.operate(sol).subtract(bStandard);
        assertEquals(0., axmb.getNorm(), expectedTolerance);
        // Check postConvert and getStandardComponents methods
        final double[] x = lpConverter.postConvert(sol.toArray());
        final double[] xStandard = lpConverter.getStandardComponents(x);
        for (int i = 0; i < sol.toArray().length; i++) {
            assertEquals(sol.getEntry(i), xStandard[i], expectedTolerance);
        }
        
        // Utils.writeDoubleArrayToFile(new double[]{s}, target + File.separator +
        // "standardS"+problemId+txt);
        // Utils.writeDoubleArrayToFile(c, target + File.separator +
        // "standardC"+problemId+txt);
        // Utils.writeDoubleMatrixToFile(A, target + File.separator +
        // "standardA"+problemId+txt);
        // Utils.writeDoubleArrayToFile(b, target + File.separator +
        // "standardB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(lb, target + File.separator +
        // "standardLB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(ub, target + File.separator +
        // "standardUB"+problemId+txt);
    }

    /**
     * Standardization (to the strictly standard form) of a problem on the form:
     * min(c) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void xxxtestCGhAbLbUb1Strict() throws PatriusException, IOException {

        final String problemId = one;

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(pathStandarization + gS + problemId 
                + txt, space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(pathStandarization + hS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                .operate(MatrixUtils.createRealVector(expectedSol))
                .subtract(MatrixUtils.createRealVector(b)).getNorm();

        int nOfSlackVariables = 0;
        for (int i = 0; i < c.length; i++) {
            final double lbi = lb[i];
            final int lbCompare = Double.compare(lbi, 0.);
            if (lbCompare != 0 && !Double.isNaN(lbi)) {
                nOfSlackVariables++;
            }
            if (!Double.isNaN(ub[i])) {
                nOfSlackVariables++;
            }
        }
        final int expectedS = g.length + nOfSlackVariables;

        // standard form conversion
        final boolean strictlyStandardForm = true;
        final LPStandardConverter lpConverter = new LPStandardConverter(strictlyStandardForm);
        lpConverter.toStandardForm(c, g, h, a, b, lb, ub);

        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        lb = lpConverter.getStandardLB().toArray();
        if (lpConverter.getStandardUB() == null) {
            ub = null;
        }
        // check consistency
        assertEquals(expectedS, s);
        assertEquals(lb.length, n);
        assertTrue(ub == null);

        // check constraints
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final double[] expectedStandardSol = lpConverter.getStandardComponents(expectedSol);
        final RealVector expectedStandardSolVector = new ArrayRealVector(expectedStandardSol);

        for (int i = 0; i < expectedStandardSolVector.getDimension(); i++) {
            assertTrue(expectedStandardSolVector.getEntry(i) >= 0.);
        }

        final RealVector axmb = aStandard.operate(expectedStandardSolVector).subtract(bStandard);
        assertEquals(0., axmb.getNorm(), expectedTolerance);

//        TestUtils.writeDoubleArrayToFile(new double[] { s }, target + File.separator
//                + standardS + problemId + txt);
//        TestUtils.writeDoubleArrayToFile(c, target + File.separator + standardC + problemId
//                + txt);
//        TestUtils.writeDoubleMatrixToFile(a, target + File.separator + standardA + problemId
//                + txt);
//        TestUtils.writeDoubleArrayToFile(b, target + File.separator + standardB + problemId
//                + txt);
//        TestUtils.writeDoubleArrayToFile(lb, target + File.separator + standardLB + problemId
//                + txt);
        // ub is null TestUtils.writeDoubleArrayToFile(ub, target + File.separator +
        // "standardUB_"+problemId+txt);
    }

    /**
     * Standardization of a problem on the form:
     * min(c) s.t.
     * G.x < h
     * A.x = b
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAb2() throws PatriusException, IOException {

        final String problemId = "2";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(pathStandarization + gS + problemId + txt, 
                space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(pathStandarization + hS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                .operate(MatrixUtils.createRealVector(expectedSol))
                .subtract(MatrixUtils.createRealVector(b)).getNorm();

        // standard form conversion
        final double unboundedLBValue = Double.NEGATIVE_INFINITY;
        final double unboundedUBValue = Double.POSITIVE_INFINITY;
        final LPStandardConverter lpConverter = new LPStandardConverter(unboundedLBValue,
                unboundedUBValue);
        lpConverter.toStandardForm(c, g, h, a, b, null, null);

        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        final double[] lb = lpConverter.getStandardLB().toArray();
        final double[] ub = lpConverter.getStandardUB().toArray();

        // check consistency
        assertEquals(g.length, s);
        assertEquals(a[0].length, n);
        assertEquals(s + lpConverter.getOriginalN(), n);
        assertEquals(lb.length, n);
        assertEquals(ub.length, n);

        // check constraints
        final RealMatrix gOrig = new Array2DRowRealMatrix(g);
        final RealVector hOrig = new ArrayRealVector(h);
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final RealVector expectedSolVector = new ArrayRealVector(expectedSol);
        final RealVector gxh = gOrig.operate(expectedSolVector).subtract(hOrig);// G.x - h
        final RealVector slackVariables = new ArrayRealVector(s);
        for (int i = 0; i < s; i++) {
            slackVariables.setEntry(i, 0. - gxh.getEntry(i));// the difference from 0
            assertTrue(slackVariables.getEntry(i) >= 0.);
        }
        final RealVector sol = slackVariables.append(expectedSolVector);
        final RealVector axmb = aStandard.operate(sol).subtract(bStandard);
        assertEquals(0., axmb.getNorm(), expectedTolerance);
        

        // Utils.writeDoubleArrayToFile(new double[]{s}, target + File.separator +
        // "standardS"+problemId+txt);
        // Utils.writeDoubleArrayToFile(c, target + File.separator +
        // "standardC"+problemId+txt);
        // Utils.writeDoubleMatrixToFile(A, target + File.separator +
        // "standardA"+problemId+txt);
        // Utils.writeDoubleArrayToFile(b, target + File.separator +
        // "standardB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(lb, target + File.separator +
        // "standardLB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(ub, target + File.separator +
        // "standardUB"+problemId+txt);
    }

    /**
     * Standardization of a problem on the form:
     * min(c) s.t.
     * G.x < h
     * A.x = b
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAb3() throws PatriusException, IOException {

        final String problemId = "3";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(pathStandarization + gS + problemId + txt, 
                space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(pathStandarization + hS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                .operate(MatrixUtils.createRealVector(expectedSol))
                .subtract(MatrixUtils.createRealVector(b)).getNorm();

        // standard form conversion
        final LPStandardConverter lpConverter = new LPStandardConverter();
        lpConverter.toStandardForm(c, g, h, a, b, null, null);

        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        final double[] lb = lpConverter.getStandardLB().toArray();
        final double[] ub = lpConverter.getStandardUB().toArray();

        // check consistency
        assertEquals(g.length, s);
        assertEquals(a[0].length, n);
        assertEquals(s + lpConverter.getOriginalN(), n);
        assertEquals(lb.length, n);
        assertEquals(ub.length, n);

        // check constraints
        final RealMatrix gOrig = new Array2DRowRealMatrix(g);
        final RealVector hOrig = new ArrayRealVector(h);
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final RealVector expectedSolVector = new ArrayRealVector(expectedSol);
        final RealVector gxh = gOrig.operate(expectedSolVector).subtract(hOrig);// G.x - h
        final RealVector slackVariables = new ArrayRealVector(s);
        for (int i = 0; i < s; i++) {
            slackVariables.setEntry(i, 0. - gxh.getEntry(i));// the difference from 0
            assertTrue(slackVariables.getEntry(i) >= 0.);
        }
        final RealVector sol = slackVariables.append(expectedSolVector);
        final RealVector axmb = aStandard.operate(sol).subtract(bStandard);
        assertEquals(0., axmb.getNorm(), expectedTolerance);
            

        // Utils.writeDoubleArrayToFile(new double[]{s}, target + File.separator +
        // "standardS"+problemId+txt);
        // Utils.writeDoubleArrayToFile(c, target + File.separator +
        // "standardC"+problemId+txt);
        // Utils.writeDoubleMatrixToFile(A, target + File.separator +
        // "standardA"+problemId+txt);
        // Utils.writeDoubleArrayToFile(b, target + File.separator +
        // "standardB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(lb, target + File.separator +
        // "standardLB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(ub, target + File.separator +
        // "standardUB"+problemId+txt);
    }

    /**
     * Standardization of a problem on the form:
     * min(c) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * @throws IOException 
     */
    public void testCGhAbLbUb4() throws IOException {

        final String problemId = "4";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(pathStandarization + gS + problemId + txt, 
                space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(pathStandarization + hS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                .operate(MatrixUtils.createRealVector(expectedSol))
                .subtract(MatrixUtils.createRealVector(b)).getNorm();

        final int nOsSplittingVariables = 0;
        // for(int i=0; i<lb.length; i++){
        // if(Double.compare(lb[i], 0.) != 0){
        // nOsSplittingVariables++;
        // }
        // }

        // standard form conversion
        final double unboundedLBValue = Double.NaN;// this is because in the file the unbounded lb are NaN
                                             // values (and also the default value)
        final double unboundedUBValue = Double.NaN;// this is because in the file the unbounded ub are NaN
                                             // values
        final LPStandardConverter lpConverter = new LPStandardConverter(unboundedLBValue,
                unboundedUBValue);
        lpConverter.toStandardForm(c, g, h, a, b, lb, ub);

        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        lb = lpConverter.getStandardLB().toArray();
        ub = lpConverter.getStandardUB().toArray();

        // check consistency
        assertEquals(g.length, s);
        assertEquals(s + lpConverter.getOriginalN() + nOsSplittingVariables, n);
        assertEquals(lb.length, n);
        assertEquals(ub.length, n);

        // check constraints
        final RealMatrix gOrig = new Array2DRowRealMatrix(g);
        final RealVector hOrig = new ArrayRealVector(h);
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final RealVector expectedSolVector = new ArrayRealVector(expectedSol);
        final RealVector gxh = gOrig.operate(expectedSolVector).subtract(hOrig);// G.x - h
        final RealVector slackVariables = new ArrayRealVector(s);
        for (int i = 0; i < s; i++) {
            slackVariables.setEntry(i, 0. - gxh.getEntry(i));// the difference from 0
            assertTrue(slackVariables.getEntry(i) >= 0.);
        }
        final RealVector sol = slackVariables.append(expectedSolVector);
        final RealVector axmb = aStandard.operate(sol).subtract(bStandard);
        assertEquals(0., axmb.getNorm(), expectedTolerance * 1.001);

        // Utils.writeDoubleArrayToFile(new double[]{s}, target + File.separator +
        // "standardS"+problemId+txt);
        // Utils.writeDoubleArrayToFile(c, target + File.separator +
        // "standardC"+problemId+txt);
        // Utils.writeDoubleMatrixToFile(A, target + File.separator +
        // "standardA"+problemId+txt);
        // Utils.writeDoubleArrayToFile(b, target + File.separator +
        // "standardB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(lb, target + File.separator +
        // "standardLB"+problemId+txt);
        // Utils.writeDoubleArrayToFile(ub, target + File.separator +
        // "standardUB"+problemId+txt);
    }

    /**
     * Standardization (to the strictly standard form) of a problem on the form:
     * min(c) s.t.
     * A.x = b
     * lb <= x <= ub
     * 
     * This is the presolved (with JOptimizer) pilot4 netlib problem.
     * @throws IOException 
     */
    public void testCAbLbUb5Strict() throws IOException {

        final String problemId = "5";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTol = TestUtils.loadDoubleArrayFromFile(pathStandarization + "tolerance" + problemId + txt)[0];

        int nOfSlackVariables = 0;
        for (int i = 0; i < c.length; i++) {
            final double lbi = lb[i];
            final int lbCompare = Double.compare(lbi, 0.);
            if (lbCompare != 0 && !Double.isNaN(lbi)) {
                nOfSlackVariables++;
            }
            if (!Double.isNaN(ub[i])) {
                nOfSlackVariables++;
            }
        }
        final int expectedS = nOfSlackVariables;

        // standard form conversion
        final boolean strictlyStandardForm = true;
        final LPStandardConverter lpConverter = new LPStandardConverter(strictlyStandardForm);
        lpConverter.toStandardForm(c, null, null, a, b, lb, ub);

        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        lb = lpConverter.getStandardLB().toArray();
        if (lpConverter.getStandardUB() == null) {
            ub = null;
        }

        // check consistency
        assertEquals(expectedS, s);
        assertEquals(lb.length, n);
        assertTrue(ub == null);

        // check constraints
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final double[] expectedStandardSol = lpConverter.getStandardComponents(expectedSol);
        final RealVector expectedStandardSolVector = new ArrayRealVector(expectedStandardSol);

        for (int i = 0; i < expectedStandardSolVector.getDimension(); i++) {
            assertTrue(expectedStandardSolVector.getEntry(i) + 1.E-8 >= 0.);
        }

        final RealVector axmb = aStandard.operate(expectedStandardSolVector).subtract(bStandard);
        for (int i = 0; i < axmb.getDimension(); i++) {
            assertEquals(0., axmb.getEntry(i), expectedTol);
        }


//        TestUtils.writeDoubleArrayToFile(new double[] { s }, target + File.separator
//                + standardS + problemId + txt);
//        TestUtils.writeDoubleArrayToFile(c, target + File.separator + standardC + problemId
//                + txt);
//        TestUtils.writeDoubleMatrixToFile(a, target + File.separator + standardA + problemId
//                + txt);
//        TestUtils.writeDoubleArrayToFile(b, target + File.separator + standardB + problemId
//                + txt);
//        TestUtils.writeDoubleArrayToFile(lb, target + File.separator + standardLB + problemId
//                + txt);
        // ub is null TestUtils.writeDoubleArrayToFile(ub, target + File.separator +
        // "standardUB_"+problemId+txt);
    }
    
    /**
     * Test isLBUnbounded and isUPUnbounded methods
     */
    public void testUnbounded(){
        double lb = Double.NaN;
        double ub = Double.NEGATIVE_INFINITY;
        final LPStandardConverter sc = new LPStandardConverter(lb, ub);
        assertTrue(sc.isLbUnbounded(lb));
        assertTrue(sc.isUbUnbounded(ub));
    } 
    
    /**
     * Test LPStandardConverter with non-accepted lower bound
     */
    public void testLBError(){
        double lb = 1;
        double ub = Double.NEGATIVE_INFINITY;
        
        try{
            new LPStandardConverter(lb, ub);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, non-acceptable lower bound
            return;
        }
        fail();
    }
    
    /**
     * Test LPStandardConverter with non-accepted upper bound
     * It throws an exception
     */
    public void testUBError(){
        double lb = Double.NaN;
        double ub = 10;
        
        try{
            new LPStandardConverter(lb, ub);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, non-acceptable upper bound
            return;
        }
        fail();
    }
    
    /**
     * Test toStandard method with different dimensions on lower and upper bounds
     * It throws an exception
     * @throws IOException
     */
    public void testErrorToStandard() throws IOException {

        final String problemId = "5";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = {Double.NaN};

        final LPStandardConverter lpConverter = new LPStandardConverter();
        try{
            lpConverter.toStandardForm(c, null, null, a, b, lb, ub);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, lower bound and upper bound dimension mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test toStandard method with different dimensions on lower and upper bounds
     * -> It throws an exception
     * @throws IOException
     */
    public void testErrorToStandard2() throws IOException {

        final String problemId = "5";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = {Double.NaN};
        
        final RealVector cVector = new ArrayRealVector(c);
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealVector bVector = new ArrayRealVector(b);
        final RealVector lbVector = new ArrayRealVector(lb);
        final RealVector ubVector = new ArrayRealVector(ub);

        final LPStandardConverter lpConverter = new LPStandardConverter();
        try{
            lpConverter.toStandardForm(cVector, null, null, aMatrix, bVector, lbVector, ubVector);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, lower bound and upper bound dimension mismatch
            return;
        }
        fail();
    }
    
    /**
     * Test postConvert, when the vector dimension is not correct
     * an exception has to be thrown
     * 
     * @throws IOException
     */
    public void testErrorPostConvert() throws IOException {
        final String problemId = "5";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        
        final RealVector cVector = new ArrayRealVector(c);
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealVector bVector = new ArrayRealVector(b);
        final RealVector lbVector = new ArrayRealVector(lb);
        final RealVector ubVector = new ArrayRealVector(ub);

        final LPStandardConverter lpConverter = new LPStandardConverter();
        lpConverter.toStandardForm(cVector, null, null, aMatrix, bVector, lbVector, ubVector);
        final int n = lpConverter.getStandardN();
        final double[] x = new double[n+1];
        try{
            lpConverter.postConvert(x);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, wrong dimension of the vector to convert 
            return;
        }
        fail();
    }
    
    /**
     * Test getStandardComponents, when the vector dimension is not correct
     * an exception has to be thrown
     * 
     * @throws IOException
     */
    public void testErrorGetStandardComp() throws IOException {
        final String problemId = "5";

        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = TestUtils.loadDoubleArrayFromFile(pathStandarization + lbS + problemId + txt);
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        
        final RealVector cVector = new ArrayRealVector(c);
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealVector bVector = new ArrayRealVector(b);
        final RealVector lbVector = new ArrayRealVector(lb);
        final RealVector ubVector = new ArrayRealVector(ub);

        final LPStandardConverter lpConverter = new LPStandardConverter();
        lpConverter.toStandardForm(cVector, null, null, aMatrix, bVector, lbVector, ubVector);
        final int n = lpConverter.getStandardN();
        final double[] x = new double[n+1];
        try{
            lpConverter.getStandardComponents(x);
        }catch (IllegalArgumentException e) {
            assertTrue(true);//ok, wrong dimension of the vector to get the components
            return;
        }
        fail();
    }

    
    /**
     * Striclty standardization of a problem on the form:
     * min(c) s.t.
     * G.x < h
     * A.x = b
     * lb <= x <= ub
     * 
     * @throws PatriusException if an error occurs
     * @throws IOException if an error occurs while reading.
     */
    public void testCGhAbLbUb1Strict() throws PatriusException, IOException {

        final String problemId = one;
        double[] c = TestUtils.loadDoubleArrayFromFile(pathStandarization + cS + problemId + txt);
        final double[][] g = TestUtils.loadDoubleMatrixFromFile(pathStandarization + gS + problemId + 
                txt, space.charAt(0));
        final double[] h = TestUtils.loadDoubleArrayFromFile(pathStandarization + hS + problemId + txt);
        double[][] a = TestUtils.loadDoubleMatrixFromFile(pathStandarization + aS + problemId + txt, 
                space.charAt(0));
        double[] b = TestUtils.loadDoubleArrayFromFile(pathStandarization + bS + problemId + txt);
        double[] lb = {-1,4,-5};
        double[] ub = TestUtils.loadDoubleArrayFromFile(pathStandarization + ubS + problemId + txt);
        final double[] expectedSol = TestUtils.loadDoubleArrayFromFile(pathStandarization + solS + problemId + txt);
        final double expectedTolerance = MatrixUtils.createRealMatrix(a)
                .operate(MatrixUtils.createRealVector(expectedSol))
                .subtract(MatrixUtils.createRealVector(b)).getNorm();
        
        int nOfSlackVariables = 0;
        for(int i=0; i<c.length; i++){
            double lbi = lb[i];
            int lbCompare = Double.compare(lbi, 0.); 
            if(lbCompare != 0 && !Double.isNaN(lbi)){
                nOfSlackVariables++;
            }
            if(!Double.isNaN(ub[i])){
                nOfSlackVariables++;
            }
        }
        int expectedS = g.length + nOfSlackVariables;

        // standard form conversion
        final double unboundedLBValue = Double.NEGATIVE_INFINITY;// this is because in the file the
                                                           // unbounded lb are -Infinity values (not
                                                           // the default value)
        final double unboundedUBValue = Double.POSITIVE_INFINITY;// this is because in the file the
                                                           // unbounded ub are +Infinity values
        final LPStandardConverter lpConverter = new LPStandardConverter(true, unboundedLBValue,
                unboundedUBValue);
        lpConverter.toStandardForm(c, g, h, a, b, lb, ub); 
        
        
        final int n = lpConverter.getStandardN();
        final int s = lpConverter.getStandardS();
        c = lpConverter.getStandardC().toArray();
        a = lpConverter.getStandardA().getData();
        b = lpConverter.getStandardB().toArray();
        lb = lpConverter.getStandardLB().toArray();
        if (lpConverter.getStandardUB() == null) {
            ub = null;
        }
        // check consistency
        assertEquals(expectedS, s);
        assertEquals(lb.length, n);
        assertTrue(ub == null);

        // check constraints
        final RealMatrix aStandard = new Array2DRowRealMatrix(a);
        final RealVector bStandard = new ArrayRealVector(b);
        final double[] expectedStandardSol = lpConverter.getStandardComponents(expectedSol);
        final RealVector expectedStandardSolVector = new ArrayRealVector(expectedStandardSol);
        for (int i = 0; i < expectedStandardSolVector.getDimension(); i++) {
            assertTrue(expectedStandardSolVector.getEntry(i)+ 1.E-8 >= 0.);
        }

        final RealVector axmb = aStandard.operate(expectedStandardSolVector).subtract(bStandard);
        for (int i = 0; i < axmb.getDimension(); i++) {
            assertEquals(0., axmb.getEntry(i), expectedTolerance);
        }

     }
    
    /**
     * Standardization of a problem that is already standard, it has to return the same
     * min(c) s.t.
     * A.x = b
     * 
     * @throws PatriusException if an error occurs
     */
    public void testStandardProblem() throws PatriusException{

        double[] c = {1,1};
        final double[][] g = null;
        final double[] h = null;
        double[][] a = {{2,1},{1,-1}};
        double[] b = {4,2};
        double[] lb = null;
        double[] ub = null;
        // standard form conversion
        final double unboundedLBValue = Double.NEGATIVE_INFINITY;// this is because in the file the
                                                           // unbounded lb are -Infinity values (not
                                                           // the default value)
        final double unboundedUBValue = Double.POSITIVE_INFINITY;// this is because in the file the
                                                           // unbounded ub are +Infinity values
        final LPStandardConverter lpConverter = new LPStandardConverter(true, unboundedLBValue,
                unboundedUBValue);
        lpConverter.toStandardForm(c, g, h, a, b, lb, ub);
        final double[] cStandard = lpConverter.getStandardC().toArray();
        final double[][] aStandard = lpConverter.getStandardA().getData(false);
        final double[] bStandard = lpConverter.getStandardB().toArray();
        for(int i=0; i<c.length ; i++){
            assertEquals(c[i], cStandard[i]);
            assertEquals(b[i], bStandard[i]);
        }
        for(int i=0; i<a.length ; i++){
            for(int j=0; i<a.length ; i++){
                assertEquals(a[i][j], aStandard[i][j]);
            }
        }
    }
    
    /**
     * Standardization of a problem that is already standard, it has to return the same
     * min(c) s.t.
     * A.x = b
     * 
     * @throws PatriusException if an error occurs
     */
    public void testStandardProblem2() throws PatriusException{

        double[] c = {1,1};
        double[][] a = {{2,1},{1,-1}};
        double[] b = {4,2};
        
        final RealVector cVector = new ArrayRealVector(c);
        final RealMatrix gMatrix = null;
        final RealVector hVector = null;
        final RealMatrix aMatrix = new BlockRealMatrix(a);
        final RealVector bVector = new ArrayRealVector(b);
        final RealVector lbVector = null;
        final RealVector ubVector = null;
        
        // standard form conversion
        final double unboundedLBValue = Double.NEGATIVE_INFINITY;// this is because in the file the
                                                           // unbounded lb are -Infinity values (not
                                                           // the default value)
        final double unboundedUBValue = Double.POSITIVE_INFINITY;// this is because in the file the
                                                           // unbounded ub are +Infinity values
        final LPStandardConverter lpConverter = new LPStandardConverter(true, unboundedLBValue,
                unboundedUBValue);
        lpConverter.toStandardForm(cVector, gMatrix, hVector, aMatrix, bVector, lbVector, ubVector);
        final double[] cStandard = lpConverter.getStandardC().toArray();
        final double[][] aStandard = lpConverter.getStandardA().getData(false);
        final double[] bStandard = lpConverter.getStandardB().toArray();
        for(int i=0; i<c.length ; i++){
            assertEquals(c[i], cStandard[i]);
            assertEquals(b[i], bStandard[i]);
        }
        for(int i=0; i<a.length ; i++){
            for(int j=0; i<a.length ; i++){
                assertEquals(a[i][j], aStandard[i][j]);
            }
        }
    }

}
