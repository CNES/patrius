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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateMatrixFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * <p>
 * Some of the unit tests are re-implementations of the MINPACK <a
 * href="http://www.netlib.org/minpack/ex/file17">file17</a> and <a
 * href="http://www.netlib.org/minpack/ex/file22">file22</a> test files. The redistribution policy for MINPACK is
 * available <a href="http://www.netlib.org/minpack/disclaimer">here</a>, for convenience, it is reproduced below.
 * </p>
 * 
 * <table border="0" width="80%" cellpadding="10" align="center" bgcolor="#E0E0E0">
 * <tr>
 * <td>
 * Minpack Copyright Notice (1999) University of Chicago. All rights reserved</td>
 * </tr>
 * <tr>
 * <td>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <ol>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>The end-user documentation included with the redistribution, if any, must include the following acknowledgment:
 * <code>This product includes software developed by the University of
 *           Chicago, as Operator of Argonne National Laboratory.</code> Alternately, this acknowledgment may appear in
 * the software itself, if and wherever such third-party acknowledgments normally appear.</li>
 * <li><strong>WARRANTY DISCLAIMER. THE SOFTWARE IS SUPPLIED "AS IS" WITHOUT WARRANTY OF ANY KIND. THE COPYRIGHT HOLDER,
 * THE UNITED STATES, THE UNITED STATES DEPARTMENT OF ENERGY, AND THEIR EMPLOYEES: (1) DISCLAIM ANY WARRANTIES, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * TITLE OR NON-INFRINGEMENT, (2) DO NOT ASSUME ANY LEGAL LIABILITY OR RESPONSIBILITY FOR THE ACCURACY, COMPLETENESS, OR
 * USEFULNESS OF THE SOFTWARE, (3) DO NOT REPRESENT THAT USE OF THE SOFTWARE WOULD NOT INFRINGE PRIVATELY OWNED RIGHTS,
 * (4) DO NOT WARRANT THAT THE SOFTWARE WILL FUNCTION UNINTERRUPTED, THAT IT IS ERROR-FREE OR THAT ANY ERRORS WILL BE
 * CORRECTED.</strong></li>
 * <li><strong>LIMITATION OF LIABILITY. IN NO EVENT WILL THE COPYRIGHT HOLDER, THE UNITED STATES, THE UNITED STATES
 * DEPARTMENT OF ENERGY, OR THEIR EMPLOYEES: BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL OR PUNITIVE
 * DAMAGES OF ANY KIND OR NATURE, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS OR LOSS OF DATA, FOR ANY REASON
 * WHATSOEVER, WHETHER SUCH LIABILITY IS ASSERTED ON THE BASIS OF CONTRACT, TORT (INCLUDING NEGLIGENCE OR STRICT
 * LIABILITY), OR OTHERWISE, EVEN IF ANY OF SAID PARTIES HAS BEEN WARNED OF THE POSSIBILITY OF SUCH LOSS OR
 * DAMAGES.</strong></li>
 * <ol></td>
 * </tr>
 * </table>
 * 
 * @author Argonne National Laboratory. MINPACK project. March 1980 (original fortran minpack tests)
 * @author Burton S. Garbow (original fortran minpack tests)
 * @author Kenneth E. Hillstrom (original fortran minpack tests)
 * @author Jorge J. More (original fortran minpack tests)
 * @author Luc Maisonobe (non-minpack tests and minpack tests Java translation)
 */
public class MinpackTest {

    @Test
    public void testMinpackLinearFullRank() {
        this.minpackTest(new LinearFullRankFunction(10, 5, 1.0,
            5.0, 2.23606797749979), false);
        this.minpackTest(new LinearFullRankFunction(50, 5, 1.0,
            8.06225774829855, 6.70820393249937), false);
    }

    @Test
    public void testMinpackLinearRank1() {
        this.minpackTest(new LinearRank1Function(10, 5, 1.0,
            291.521868819476, 1.4638501094228), false);
        this.minpackTest(new LinearRank1Function(50, 5, 1.0,
            3101.60039334535, 3.48263016573496), false);
    }

    @Test
    public void testMinpackLinearRank1ZeroColsAndRows() {
        this.minpackTest(new LinearRank1ZeroColsAndRowsFunction(10, 5, 1.0), false);
        this.minpackTest(new LinearRank1ZeroColsAndRowsFunction(50, 5, 1.0), false);
    }

    @Test
    public void testMinpackRosenbrok() {
        this.minpackTest(new RosenbrockFunction(new double[] { -1.2, 1.0 },
            MathLib.sqrt(24.2)), false);
        this.minpackTest(new RosenbrockFunction(new double[] { -12.0, 10.0 },
            MathLib.sqrt(1795769.0)), false);
        this.minpackTest(new RosenbrockFunction(new double[] { -120.0, 100.0 },
            11.0 * MathLib.sqrt(169000121.0)), false);
    }

    @Test
    public void testMinpackHelicalValley() {
        this.minpackTest(new HelicalValleyFunction(new double[] { -1.0, 0.0, 0.0 },
            50.0), false);
        this.minpackTest(new HelicalValleyFunction(new double[] { -10.0, 0.0, 0.0 },
            102.95630140987), false);
        this.minpackTest(new HelicalValleyFunction(new double[] { -100.0, 0.0, 0.0 },
            991.261822123701), false);
    }

    @Test
    public void testMinpackPowellSingular() {
        this.minpackTest(new PowellSingularFunction(new double[] { 3.0, -1.0, 0.0, 1.0 },
            14.6628782986152), false);
        this.minpackTest(new PowellSingularFunction(new double[] { 30.0, -10.0, 0.0, 10.0 },
            1270.9838708654), false);
        this.minpackTest(new PowellSingularFunction(new double[] { 300.0, -100.0, 0.0, 100.0 },
            126887.903284750), false);
    }

    @Test
    public void testMinpackFreudensteinRoth() {
        this.minpackTest(new FreudensteinRothFunction(new double[] { 0.5, -2.0 },
            20.0124960961895, 6.99887517584575,
            new double[] {
                11.4124844654993,
                -0.896827913731509
            }), false);
        this.minpackTest(new FreudensteinRothFunction(new double[] { 5.0, -20.0 },
            12432.833948863, 6.9988751744895,
            new double[] {
                11.41300466147456,
                -0.896796038685959
            }), false);
        this.minpackTest(new FreudensteinRothFunction(new double[] { 50.0, -200.0 },
            11426454.595762, 6.99887517242903,
            new double[] {
                11.412781785788564,
                -0.8968051074920405
            }), false);
    }

    @Test
    public void testMinpackBard() {
        this.minpackTest(new BardFunction(1.0, 6.45613629515967, 0.0906359603390466,
            new double[] {
                0.0824105765758334,
                1.1330366534715,
                2.34369463894115
            }), false);
        this.minpackTest(new BardFunction(10.0, 36.1418531596785, 4.17476870138539,
            new double[] {
                0.840666673818329,
                -158848033.259565,
                -164378671.653535
            }), false);
        this.minpackTest(new BardFunction(100.0, 384.114678637399, 4.17476870135969,
            new double[] {
                0.840666673867645,
                -158946167.205518,
                -164464906.857771
            }), false);
    }

    @Test
    public void testMinpackKowalikOsborne() {
        this.minpackTest(new KowalikOsborneFunction(new double[] { 0.25, 0.39, 0.415, 0.39 },
            0.0728915102882945,
            0.017535837721129,
            new double[] {
                0.192807810476249,
                0.191262653354071,
                0.123052801046931,
                0.136053221150517
            }), false);
        this.minpackTest(new KowalikOsborneFunction(new double[] { 2.5, 3.9, 4.15, 3.9 },
            2.97937007555202,
            0.032052192917937,
            new double[] {
                728675.473768287,
                -14.0758803129393,
                -32977797.7841797,
                -20571594.1977912
            }), false);
        this.minpackTest(new KowalikOsborneFunction(new double[] { 25.0, 39.0, 41.5, 39.0 },
            29.9590617016037,
            0.0175364017658228,
            new double[] {
                0.192948328597594,
                0.188053165007911,
                0.122430604321144,
                0.134575665392506
            }), false);
    }

    @Test
    public void testMinpackMeyer() {
        this.minpackTest(new MeyerFunction(new double[] { 0.02, 4000.0, 250.0 },
            41153.4665543031, 9.37794514651874,
            new double[] {
                0.00560963647102661,
                6181.34634628659,
                345.223634624144
            }), false);
        this.minpackTest(new MeyerFunction(new double[] { 0.2, 40000.0, 2500.0 },
            4168216.89130846, 792.917871779501,
            new double[] {
                1.42367074157994e-11,
                33695.7133432541,
                901.268527953801
            }), true);
    }

    @Test
    public void testMinpackWatson() {
        this.minpackTest(new WatsonFunction(6, 0.0,
            5.47722557505166, 0.0478295939097601,
            new double[] {
                -0.0157249615083782, 1.01243488232965,
                -0.232991722387673, 1.26043101102818,
                -1.51373031394421, 0.99299727291842
            }), false);
        this.minpackTest(new WatsonFunction(6, 10.0,
            6433.12578950026, 0.0478295939096951,
            new double[] {
                -0.0157251901386677, 1.01243485860105,
                -0.232991545843829, 1.26042932089163,
                -1.51372776706575, 0.99299573426328
            }), false);
        this.minpackTest(new WatsonFunction(6, 100.0,
            674256.040605213, 0.047829593911544,
            new double[] {
                -0.0157247019712586, 1.01243490925658,
                -0.232991922761641, 1.26043292929555,
                -1.51373320452707, 0.99299901922322
            }), false);
        this.minpackTest(new WatsonFunction(9, 0.0,
            5.47722557505166, 0.00118311459212420,
            new double[] {
                -0.153070644166722e-4, 0.999789703934597,
                0.0147639634910978, 0.146342330145992,
                1.00082109454817, -2.61773112070507,
                4.10440313943354, -3.14361226236241,
                1.05262640378759
            }), false);
        this.minpackTest(new WatsonFunction(9, 10.0,
            12088.127069307, 0.00118311459212513,
            new double[] {
                -0.153071334849279e-4, 0.999789703941234,
                0.0147639629786217, 0.146342334818836,
                1.00082107321386, -2.61773107084722,
                4.10440307655564, -3.14361222178686,
                1.05262639322589
            }), false);
        this.minpackTest(new WatsonFunction(9, 100.0,
            1269109.29043834, 0.00118311459212384,
            new double[] {
                -0.153069523352176e-4, 0.999789703958371,
                0.0147639625185392, 0.146342341096326,
                1.00082104729164, -2.61773101573645,
                4.10440301427286, -3.14361218602503,
                1.05262638516774
            }), false);
        this.minpackTest(new WatsonFunction(12, 0.0,
            5.47722557505166, 0.217310402535861e-4,
            new double[] {
                -0.660266001396382e-8, 1.00000164411833,
                -0.000563932146980154, 0.347820540050756,
                -0.156731500244233, 1.05281515825593,
                -3.24727109519451, 7.2884347837505,
                -10.271848098614, 9.07411353715783,
                -4.54137541918194, 1.01201187975044
            }), false);
        this.minpackTest(new WatsonFunction(12, 10.0,
            19220.7589790951, 0.217310402518509e-4,
            new double[] {
                -0.663710223017410e-8, 1.00000164411787,
                -0.000563932208347327, 0.347820540486998,
                -0.156731503955652, 1.05281517654573,
                -3.2472711515214, 7.28843489430665,
                -10.2718482369638, 9.07411364383733,
                -4.54137546533666, 1.01201188830857
            }), false);
        this.minpackTest(new WatsonFunction(12, 100.0,
            2018918.04462367, 0.217310402539845e-4,
            new double[] {
                -0.663806046485249e-8, 1.00000164411786,
                -0.000563932210324959, 0.347820540503588,
                -0.156731504091375, 1.05281517718031,
                -3.24727115337025, 7.28843489775302,
                -10.2718482410813, 9.07411364688464,
                -4.54137546660822, 1.0120118885369
            }), false);
    }

    @Test
    public void testMinpackBox3Dimensional() {
        this.minpackTest(new Box3DimensionalFunction(10, new double[] { 0.0, 10.0, 20.0 },
            32.1115837449572), false);
    }

    @Test
    public void testMinpackJennrichSampson() {
        this.minpackTest(new JennrichSampsonFunction(10, new double[] { 0.3, 0.4 },
            64.5856498144943, 11.1517793413499,
            new double[] {
                // 0.2578330049, 0.257829976764542
                0.2578199266368004, 0.25782997676455244
            }), false);
    }

    @Test
    public void testMinpackBrownDennis() {
        this.minpackTest(new BrownDennisFunction(20,
            new double[] { 25.0, 5.0, -5.0, -1.0 },
            2815.43839161816, 292.954288244866,
            new double[] {
                -11.59125141003, 13.2024883984741,
                -0.403574643314272, 0.236736269844604
            }), false);
        this.minpackTest(new BrownDennisFunction(20,
            new double[] { 250.0, 50.0, -50.0, -10.0 },
            555073.354173069, 292.954270581415,
            new double[] {
                -11.5959274272203, 13.2041866926242,
                -0.403417362841545, 0.236771143410386
            }), false);
        this.minpackTest(new BrownDennisFunction(20,
            new double[] { 2500.0, 500.0, -500.0, -100.0 },
            61211252.2338581, 292.954306151134,
            new double[] {
                -11.5902596937374, 13.2020628854665,
                -0.403688070279258, 0.236665033746463
            }), false);
    }

    @Test
    public void testMinpackChebyquad() {
        this.minpackTest(new ChebyquadFunction(1, 8, 1.0,
            1.88623796907732, 1.88623796907732,
            new double[] { 0.5 }), false);
        this.minpackTest(new ChebyquadFunction(1, 8, 10.0,
            5383344372.34005, 1.88424820499951,
            new double[] { 0.9817314924684 }), false);
        this.minpackTest(new ChebyquadFunction(1, 8, 100.0,
            0.118088726698392e19, 1.88424820499347,
            new double[] { 0.9817314852934 }), false);
        this.minpackTest(new ChebyquadFunction(8, 8, 1.0,
            0.196513862833975, 0.0593032355046727,
            new double[] {
                0.0431536648587336, 0.193091637843267,
                0.266328593812698, 0.499999334628884,
                0.500000665371116, 0.733671406187302,
                0.806908362156733, 0.956846335141266
            }), false);
        this.minpackTest(new ChebyquadFunction(9, 9, 1.0,
            0.16994993465202, 0.0,
            new double[] {
                0.0442053461357828, 0.199490672309881,
                0.23561910847106, 0.416046907892598,
                0.5, 0.583953092107402,
                0.764380891528940, 0.800509327690119,
                0.955794653864217
            }), false);
        this.minpackTest(new ChebyquadFunction(10, 10, 1.0,
            0.183747831178711, 0.0806471004038253,
            new double[] {
                0.0596202671753563, 0.166708783805937,
                0.239171018813509, 0.398885290346268,
                0.398883667870681, 0.601116332129320,
                0.60111470965373, 0.760828981186491,
                0.833291216194063, 0.940379732824644
            }), false);
    }

    @Test
    public void testMinpackBrownAlmostLinear() {
        this.minpackTest(new BrownAlmostLinearFunction(10, 0.5,
            16.5302162063499, 0.0,
            new double[] {
                0.979430303349862, 0.979430303349862,
                0.979430303349862, 0.979430303349862,
                0.979430303349862, 0.979430303349862,
                0.979430303349862, 0.979430303349862,
                0.979430303349862, 1.20569696650138
            }), false);
        this.minpackTest(new BrownAlmostLinearFunction(10, 5.0,
            9765624.00089211, 0.0,
            new double[] {
                0.979430303349865, 0.979430303349865,
                0.979430303349865, 0.979430303349865,
                0.979430303349865, 0.979430303349865,
                0.979430303349865, 0.979430303349865,
                0.979430303349865, 1.20569696650135
            }), false);
        this.minpackTest(new BrownAlmostLinearFunction(10, 50.0,
            0.9765625e17, 0.0,
            new double[] {
                1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.0, 1.0, 1.0
            }), false);
        this.minpackTest(new BrownAlmostLinearFunction(30, 0.5,
            83.476044467848, 0.0,
            new double[] {
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 0.997754216442807,
                0.997754216442807, 1.06737350671578
            }), false);
        this.minpackTest(new BrownAlmostLinearFunction(40, 0.5,
            128.026364472323, 0.0,
            new double[] {
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                1.00000000000002, 1.00000000000002,
                0.999999999999121
            }), false);
    }

    @Test
    public void testMinpackOsborne1() {
        this.minpackTest(new Osborne1Function(new double[] { 0.5, 1.5, -1.0, 0.01, 0.02, },
            0.937564021037838, 0.00739249260904843,
            new double[] {
                0.375410049244025, 1.93584654543108,
                -1.46468676748716, 0.0128675339110439,
                0.0221227011813076
            }), false);
    }

    @Test
    public void testMinpackOsborne2() {
        this.minpackTest(new Osborne2Function(new double[] {
            1.3, 0.65, 0.65, 0.7, 0.6,
            3.0, 5.0, 7.0, 2.0, 4.5, 5.5
        },
            1.44686540984712, 0.20034404483314,
            new double[] {
                1.30997663810096, 0.43155248076,
                0.633661261602859, 0.599428560991695,
                0.754179768272449, 0.904300082378518,
                1.36579949521007, 4.82373199748107,
                2.39868475104871, 4.56887554791452,
                5.67534206273052
            }), false);
    }

    private void minpackTest(final MinpackFunction function, final boolean exceptionExpected) {
        final LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer(MathLib.sqrt(2.22044604926e-16),
            MathLib.sqrt(2.22044604926e-16),
            2.22044604926e-16);
        try {
            final PointVectorValuePair optimum = optimizer.optimize(new MaxEval(400 * (function.getN() + 1)),
                function.getModelFunction(),
                function.getModelFunctionJacobian(),
                new Target(function.getTarget()),
                new Weight(function.getWeight()),
                new InitialGuess(function.getStartPoint()));
            Assert.assertFalse(exceptionExpected);
            function.checkTheoreticalMinCost(optimizer.getRMS());
            function.checkTheoreticalMinParams(optimum);
        } catch (final TooManyEvaluationsException e) {
            Assert.assertTrue(exceptionExpected);
        }
    }

    private static abstract class MinpackFunction {
        protected int n;
        protected int m;
        protected double[] startParams;
        protected double theoreticalMinCost;
        protected double[] theoreticalMinParams;
        protected double costAccuracy;
        protected double paramsAccuracy;

        protected MinpackFunction(final int m, final double[] startParams,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            this.m = m;
            this.n = startParams.length;
            this.startParams = startParams.clone();
            this.theoreticalMinCost = theoreticalMinCost;
            this.theoreticalMinParams = theoreticalMinParams;
            this.costAccuracy = 1.0e-8;
            this.paramsAccuracy = 1.0e-5;
        }

        protected static double[] buildArray(final int n, final double x) {
            final double[] array = new double[n];
            Arrays.fill(array, x);
            return array;
        }

        public double[] getTarget() {
            return buildArray(this.m, 0.0);
        }

        public double[] getWeight() {
            return buildArray(this.m, 1.0);
        }

        public double[] getStartPoint() {
            return this.startParams.clone();
        }

        protected void setCostAccuracy(final double costAccuracy) {
            this.costAccuracy = costAccuracy;
        }

        protected void setParamsAccuracy(final double paramsAccuracy) {
            this.paramsAccuracy = paramsAccuracy;
        }

        public int getN() {
            return this.startParams.length;
        }

        public void checkTheoreticalMinCost(final double rms) {
            final double threshold = this.costAccuracy * (1.0 + this.theoreticalMinCost);
            Assert.assertEquals(this.theoreticalMinCost, MathLib.sqrt(this.m) * rms, threshold);
        }

        public void checkTheoreticalMinParams(final PointVectorValuePair optimum) {
            final double[] params = optimum.getPointRef();
            if (this.theoreticalMinParams != null) {
                for (int i = 0; i < this.theoreticalMinParams.length; ++i) {
                    final double mi = this.theoreticalMinParams[i];
                    final double vi = params[i];
                    Assert.assertEquals(mi, vi, this.paramsAccuracy * (1.0 + MathLib.abs(mi)));
                }
            }
        }

        public ModelFunction getModelFunction() {
            return new ModelFunction(new MultivariateVectorFunction(){
                @Override
                public double[] value(final double[] point) {
                    return MinpackFunction.this.computeValue(point);
                }
            });
        }

        public ModelFunctionJacobian getModelFunctionJacobian() {
            return new ModelFunctionJacobian(new MultivariateMatrixFunction(){
                @Override
                public double[][] value(final double[] point) {
                    return MinpackFunction.this.computeJacobian(point);
                }
            });
        }

        public abstract double[][] computeJacobian(double[] variables);

        public abstract double[] computeValue(double[] variables);
    }

    private static class LinearFullRankFunction extends MinpackFunction {
        public LinearFullRankFunction(final int m, final int n, final double x0,
            final double theoreticalStartCost,
            final double theoreticalMinCost) {
            super(m, buildArray(n, x0), theoreticalMinCost,
                buildArray(n, -1.0));
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double t = 2.0 / this.m;
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                jacobian[i] = new double[this.n];
                for (int j = 0; j < this.n; ++j) {
                    jacobian[i][j] = (i == j) ? (1 - t) : -t;
                }
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            double sum = 0;
            for (int i = 0; i < this.n; ++i) {
                sum += variables[i];
            }
            final double t = 1 + 2 * sum / this.m;
            final double[] f = new double[this.m];
            for (int i = 0; i < this.n; ++i) {
                f[i] = variables[i] - t;
            }
            Arrays.fill(f, this.n, this.m, -t);
            return f;
        }
    }

    private static class LinearRank1Function extends MinpackFunction {
        public LinearRank1Function(final int m, final int n, final double x0,
            final double theoreticalStartCost,
            final double theoreticalMinCost) {
            super(m, buildArray(n, x0), theoreticalMinCost, null);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                jacobian[i] = new double[this.n];
                for (int j = 0; j < this.n; ++j) {
                    jacobian[i][j] = (i + 1) * (j + 1);
                }
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double[] f = new double[this.m];
            double sum = 0;
            for (int i = 0; i < this.n; ++i) {
                sum += (i + 1) * variables[i];
            }
            for (int i = 0; i < this.m; ++i) {
                f[i] = (i + 1) * sum - 1;
            }
            return f;
        }
    }

    private static class LinearRank1ZeroColsAndRowsFunction extends MinpackFunction {
        public LinearRank1ZeroColsAndRowsFunction(final int m, final int n, final double x0) {
            super(m, buildArray(n, x0),
                MathLib.sqrt((m * (m + 3) - 6) / (2.0 * (2 * m - 3))),
                null);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                jacobian[i] = new double[this.n];
                jacobian[i][0] = 0;
                for (int j = 1; j < (this.n - 1); ++j) {
                    if (i == 0) {
                        jacobian[i][j] = 0;
                    } else if (i != (this.m - 1)) {
                        jacobian[i][j] = i * (j + 1);
                    } else {
                        jacobian[i][j] = 0;
                    }
                }
                jacobian[i][this.n - 1] = 0;
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double[] f = new double[this.m];
            double sum = 0;
            for (int i = 1; i < (this.n - 1); ++i) {
                sum += (i + 1) * variables[i];
            }
            for (int i = 0; i < (this.m - 1); ++i) {
                f[i] = i * sum - 1;
            }
            f[this.m - 1] = -1;
            return f;
        }
    }

    private static class RosenbrockFunction extends MinpackFunction {
        public RosenbrockFunction(final double[] startParams, final double theoreticalStartCost) {
            super(2, startParams, 0.0, buildArray(2, 1.0));
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            return new double[][] { { -20 * x1, 10 }, { -1, 0 } };
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            return new double[] { 10 * (x2 - x1 * x1), 1 - x1 };
        }
    }

    private static class HelicalValleyFunction extends MinpackFunction {
        public HelicalValleyFunction(final double[] startParams,
            final double theoreticalStartCost) {
            super(3, startParams, 0.0, new double[] { 1.0, 0.0, 0.0 });
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double tmpSquare = x1 * x1 + x2 * x2;
            final double tmp1 = twoPi * tmpSquare;
            final double tmp2 = MathLib.sqrt(tmpSquare);
            return new double[][] {
                { 100 * x2 / tmp1, -100 * x1 / tmp1, 10 },
                { 10 * x1 / tmp2, 10 * x2 / tmp2, 0 },
                { 0, 0, 1 }
            };
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            double tmp1;
            if (x1 == 0) {
                tmp1 = (x2 >= 0) ? 0.25 : -0.25;
            } else {
                tmp1 = MathLib.atan(x2 / x1) / twoPi;
                if (x1 < 0) {
                    tmp1 += 0.5;
                }
            }
            final double tmp2 = MathLib.sqrt(x1 * x1 + x2 * x2);
            return new double[] {
                10.0 * (x3 - 10 * tmp1),
                10.0 * (tmp2 - 1),
                x3
            };
        }

        private static final double twoPi = 2.0 * FastMath.PI;
    }

    private static class PowellSingularFunction extends MinpackFunction {
        public PowellSingularFunction(final double[] startParams,
            final double theoreticalStartCost) {
            super(4, startParams, 0.0, buildArray(4, 0.0));
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            return new double[][] {
                { 1, 10, 0, 0 },
                { 0, 0, sqrt5, -sqrt5 },
                { 0, 2 * (x2 - 2 * x3), -4 * (x2 - 2 * x3), 0 },
                { 2 * sqrt10 * (x1 - x4), 0, 0, -2 * sqrt10 * (x1 - x4) }
            };
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            return new double[] {
                x1 + 10 * x2,
                sqrt5 * (x3 - x4),
                (x2 - 2 * x3) * (x2 - 2 * x3),
                sqrt10 * (x1 - x4) * (x1 - x4)
            };
        }

        private static final double sqrt5 = MathLib.sqrt(5.0);
        private static final double sqrt10 = MathLib.sqrt(10.0);
    }

    private static class FreudensteinRothFunction extends MinpackFunction {
        public FreudensteinRothFunction(final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(2, startParams, theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x2 = variables[1];
            return new double[][] {
                { 1, x2 * (10 - 3 * x2) - 2 },
                { 1, x2 * (2 + 3 * x2) - 14, }
            };
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            return new double[] {
                -13.0 + x1 + ((5.0 - x2) * x2 - 2.0) * x2,
                -29.0 + x1 + ((1.0 + x2) * x2 - 14.0) * x2
            };
        }
    }

    private static class BardFunction extends MinpackFunction {
        public BardFunction(final double x0,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(15, buildArray(3, x0), theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double tmp1 = i + 1;
                final double tmp2 = 15 - i;
                final double tmp3 = (i <= 7) ? tmp1 : tmp2;
                double tmp4 = x2 * tmp2 + x3 * tmp3;
                tmp4 *= tmp4;
                jacobian[i] = new double[] { -1, tmp1 * tmp2 / tmp4, tmp1 * tmp3 / tmp4 };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                final double tmp1 = i + 1;
                final double tmp2 = 15 - i;
                final double tmp3 = (i <= 7) ? tmp1 : tmp2;
                f[i] = y[i] - (x1 + tmp1 / (x2 * tmp2 + x3 * tmp3));
            }
            return f;
        }

        private static final double[] y = {
            0.14, 0.18, 0.22, 0.25, 0.29,
            0.32, 0.35, 0.39, 0.37, 0.58,
            0.73, 0.96, 1.34, 2.10, 4.39
        };
    }

    private static class KowalikOsborneFunction extends MinpackFunction {
        public KowalikOsborneFunction(final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(11, startParams, theoreticalMinCost,
                theoreticalMinParams);
            if (theoreticalStartCost > 20.0) {
                this.setCostAccuracy(2.0e-4);
                this.setParamsAccuracy(5.0e-3);
            }
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double tmp = v[i] * (v[i] + x3) + x4;
                final double j1 = -v[i] * (v[i] + x2) / tmp;
                final double j2 = -v[i] * x1 / tmp;
                final double j3 = j1 * j2;
                final double j4 = j3 / v[i];
                jacobian[i] = new double[] { j1, j2, j3, j4 };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                f[i] = y[i] - x1 * (v[i] * (v[i] + x2)) / (v[i] * (v[i] + x3) + x4);
            }
            return f;
        }

        private static final double[] v = {
            4.0, 2.0, 1.0, 0.5, 0.25, 0.167, 0.125, 0.1, 0.0833, 0.0714, 0.0625
        };

        private static final double[] y = {
            0.1957, 0.1947, 0.1735, 0.1600, 0.0844, 0.0627,
            0.0456, 0.0342, 0.0323, 0.0235, 0.0246
        };
    }

    private static class MeyerFunction extends MinpackFunction {
        public MeyerFunction(final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(16, startParams, theoreticalMinCost,
                theoreticalMinParams);
            if (theoreticalStartCost > 1.0e6) {
                this.setCostAccuracy(7.0e-3);
                this.setParamsAccuracy(2.0e-2);
            }
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double temp = 5.0 * (i + 1) + 45.0 + x3;
                final double tmp1 = x2 / temp;
                final double tmp2 = MathLib.exp(tmp1);
                final double tmp3 = x1 * tmp2 / temp;
                jacobian[i] = new double[] { tmp2, tmp3, -tmp1 * tmp3 };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                f[i] = x1 * MathLib.exp(x2 / (5.0 * (i + 1) + 45.0 + x3)) - y[i];
            }
            return f;
        }

        private static final double[] y = {
            34780.0, 28610.0, 23650.0, 19630.0,
            16370.0, 13720.0, 11540.0, 9744.0,
            8261.0, 7030.0, 6005.0, 5147.0,
            4427.0, 3820.0, 3307.0, 2872.0
        };
    }

    private static class WatsonFunction extends MinpackFunction {
        public WatsonFunction(final int n, final double x0,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(31, buildArray(n, x0), theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double[][] jacobian = new double[this.m][];

            for (int i = 0; i < (this.m - 2); ++i) {
                final double div = (i + 1) / 29.0;
                double s2 = 0.0;
                double dx = 1.0;
                for (int j = 0; j < this.n; ++j) {
                    s2 += dx * variables[j];
                    dx *= div;
                }
                final double temp = 2 * div * s2;
                dx = 1.0 / div;
                jacobian[i] = new double[this.n];
                for (int j = 0; j < this.n; ++j) {
                    jacobian[i][j] = dx * (j - temp);
                    dx *= div;
                }
            }

            jacobian[this.m - 2] = new double[this.n];
            jacobian[this.m - 2][0] = 1;

            jacobian[this.m - 1] = new double[this.n];
            jacobian[this.m - 1][0] = -2 * variables[0];
            jacobian[this.m - 1][1] = 1;

            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double[] f = new double[this.m];
            for (int i = 0; i < (this.m - 2); ++i) {
                final double div = (i + 1) / 29.0;
                double s1 = 0;
                double dx = 1;
                for (int j = 1; j < this.n; ++j) {
                    s1 += j * dx * variables[j];
                    dx *= div;
                }
                double s2 = 0;
                dx = 1;
                for (int j = 0; j < this.n; ++j) {
                    s2 += dx * variables[j];
                    dx *= div;
                }
                f[i] = s1 - s2 * s2 - 1;
            }

            final double x1 = variables[0];
            final double x2 = variables[1];
            f[this.m - 2] = x1;
            f[this.m - 1] = x2 - x1 * x1 - 1;

            return f;
        }
    }

    private static class Box3DimensionalFunction extends MinpackFunction {
        public Box3DimensionalFunction(final int m, final double[] startParams,
            final double theoreticalStartCost) {
            super(m, startParams, 0.0,
                new double[] { 1.0, 10.0, 1.0 });
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double tmp = (i + 1) / 10.0;
                jacobian[i] = new double[] {
                    -tmp * MathLib.exp(-tmp * x1),
                    tmp * MathLib.exp(-tmp * x2),
                    MathLib.exp(-i - 1) - MathLib.exp(-tmp)
                };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                final double tmp = (i + 1) / 10.0;
                f[i] = MathLib.exp(-tmp * x1) - MathLib.exp(-tmp * x2)
                    + (MathLib.exp(-i - 1) - MathLib.exp(-tmp)) * x3;
            }
            return f;
        }
    }

    private static class JennrichSampsonFunction extends MinpackFunction {
        public JennrichSampsonFunction(final int m, final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(m, startParams, theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double t = i + 1;
                jacobian[i] = new double[] { -t * MathLib.exp(t * x1), -t * MathLib.exp(t * x2) };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                final double temp = i + 1;
                f[i] = 2 + 2 * temp - MathLib.exp(temp * x1) - MathLib.exp(temp * x2);
            }
            return f;
        }
    }

    private static class BrownDennisFunction extends MinpackFunction {
        public BrownDennisFunction(final int m, final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(m, startParams, theoreticalMinCost,
                theoreticalMinParams);
            this.setCostAccuracy(2.5e-8);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double temp = (i + 1) / 5.0;
                final double ti = MathLib.sin(temp);
                final double tmp1 = x1 + temp * x2 - MathLib.exp(temp);
                final double tmp2 = x3 + ti * x4 - MathLib.cos(temp);
                jacobian[i] = new double[] {
                    2 * tmp1, 2 * temp * tmp1, 2 * tmp2, 2 * ti * tmp2
                };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                final double temp = (i + 1) / 5.0;
                final double tmp1 = x1 + temp * x2 - MathLib.exp(temp);
                final double tmp2 = x3 + MathLib.sin(temp) * x4 - MathLib.cos(temp);
                f[i] = tmp1 * tmp1 + tmp2 * tmp2;
            }
            return f;
        }
    }

    private static class ChebyquadFunction extends MinpackFunction {
        private static double[] buildChebyquadArray(final int n, final double factor) {
            final double[] array = new double[n];
            final double inv = factor / (n + 1);
            for (int i = 0; i < n; ++i) {
                array[i] = (i + 1) * inv;
            }
            return array;
        }

        public ChebyquadFunction(final int n, final int m, final double factor,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(m, buildChebyquadArray(n, factor), theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                jacobian[i] = new double[this.n];
            }

            final double dx = 1.0 / this.n;
            for (int j = 0; j < this.n; ++j) {
                double tmp1 = 1;
                double tmp2 = 2 * variables[j] - 1;
                final double temp = 2 * tmp2;
                double tmp3 = 0;
                double tmp4 = 2;
                for (int i = 0; i < this.m; ++i) {
                    jacobian[i][j] = dx * tmp4;
                    double ti = 4 * tmp2 + temp * tmp4 - tmp3;
                    tmp3 = tmp4;
                    tmp4 = ti;
                    ti = temp * tmp2 - tmp1;
                    tmp1 = tmp2;
                    tmp2 = ti;
                }
            }

            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double[] f = new double[this.m];

            for (int j = 0; j < this.n; ++j) {
                double tmp1 = 1;
                double tmp2 = 2 * variables[j] - 1;
                final double temp = 2 * tmp2;
                for (int i = 0; i < this.m; ++i) {
                    f[i] += tmp2;
                    final double ti = temp * tmp2 - tmp1;
                    tmp1 = tmp2;
                    tmp2 = ti;
                }
            }

            final double dx = 1.0 / this.n;
            boolean iev = false;
            for (int i = 0; i < this.m; ++i) {
                f[i] *= dx;
                if (iev) {
                    f[i] += 1.0 / (i * (i + 2));
                }
                iev = !iev;
            }

            return f;
        }
    }

    private static class BrownAlmostLinearFunction extends MinpackFunction {
        public BrownAlmostLinearFunction(final int m, final double factor,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(m, buildArray(m, factor), theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                jacobian[i] = new double[this.n];
            }

            double prod = 1;
            for (int j = 0; j < this.n; ++j) {
                prod *= variables[j];
                for (int i = 0; i < this.n; ++i) {
                    jacobian[i][j] = 1;
                }
                jacobian[j][j] = 2;
            }

            for (int j = 0; j < this.n; ++j) {
                double temp = variables[j];
                if (temp == 0) {
                    temp = 1;
                    prod = 1;
                    for (int k = 0; k < this.n; ++k) {
                        if (k != j) {
                            prod *= variables[k];
                        }
                    }
                }
                jacobian[this.n - 1][j] = prod / temp;
            }

            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double[] f = new double[this.m];
            double sum = -(this.n + 1);
            double prod = 1;
            for (int j = 0; j < this.n; ++j) {
                sum += variables[j];
                prod *= variables[j];
            }
            for (int i = 0; i < this.n; ++i) {
                f[i] = variables[i] + sum;
            }
            f[this.n - 1] = prod - 1;
            return f;
        }
    }

    private static class Osborne1Function extends MinpackFunction {
        public Osborne1Function(final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(33, startParams, theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            final double x5 = variables[4];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double temp = 10.0 * i;
                final double tmp1 = MathLib.exp(-temp * x4);
                final double tmp2 = MathLib.exp(-temp * x5);
                jacobian[i] = new double[] {
                    -1, -tmp1, -tmp2, temp * x2 * tmp1, temp * x3 * tmp2
                };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x1 = variables[0];
            final double x2 = variables[1];
            final double x3 = variables[2];
            final double x4 = variables[3];
            final double x5 = variables[4];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                final double temp = 10.0 * i;
                final double tmp1 = MathLib.exp(-temp * x4);
                final double tmp2 = MathLib.exp(-temp * x5);
                f[i] = y[i] - (x1 + x2 * tmp1 + x3 * tmp2);
            }
            return f;
        }

        private static final double[] y = {
            0.844, 0.908, 0.932, 0.936, 0.925, 0.908, 0.881, 0.850, 0.818, 0.784, 0.751,
            0.718, 0.685, 0.658, 0.628, 0.603, 0.580, 0.558, 0.538, 0.522, 0.506, 0.490,
            0.478, 0.467, 0.457, 0.448, 0.438, 0.431, 0.424, 0.420, 0.414, 0.411, 0.406
        };
    }

    private static class Osborne2Function extends MinpackFunction {
        public Osborne2Function(final double[] startParams,
            final double theoreticalStartCost,
            final double theoreticalMinCost,
            final double[] theoreticalMinParams) {
            super(65, startParams, theoreticalMinCost,
                theoreticalMinParams);
        }

        @Override
        public double[][] computeJacobian(final double[] variables) {
            final double x01 = variables[0];
            final double x02 = variables[1];
            final double x03 = variables[2];
            final double x04 = variables[3];
            final double x05 = variables[4];
            final double x06 = variables[5];
            final double x07 = variables[6];
            final double x08 = variables[7];
            final double x09 = variables[8];
            final double x10 = variables[9];
            final double x11 = variables[10];
            final double[][] jacobian = new double[this.m][];
            for (int i = 0; i < this.m; ++i) {
                final double temp = i / 10.0;
                final double tmp1 = MathLib.exp(-x05 * temp);
                final double tmp2 = MathLib.exp(-x06 * (temp - x09) * (temp - x09));
                final double tmp3 = MathLib.exp(-x07 * (temp - x10) * (temp - x10));
                final double tmp4 = MathLib.exp(-x08 * (temp - x11) * (temp - x11));
                jacobian[i] = new double[] {
                    -tmp1,
                    -tmp2,
                    -tmp3,
                    -tmp4,
                    temp * x01 * tmp1,
                    x02 * (temp - x09) * (temp - x09) * tmp2,
                    x03 * (temp - x10) * (temp - x10) * tmp3,
                    x04 * (temp - x11) * (temp - x11) * tmp4,
                    -2 * x02 * x06 * (temp - x09) * tmp2,
                    -2 * x03 * x07 * (temp - x10) * tmp3,
                    -2 * x04 * x08 * (temp - x11) * tmp4
                };
            }
            return jacobian;
        }

        @Override
        public double[] computeValue(final double[] variables) {
            final double x01 = variables[0];
            final double x02 = variables[1];
            final double x03 = variables[2];
            final double x04 = variables[3];
            final double x05 = variables[4];
            final double x06 = variables[5];
            final double x07 = variables[6];
            final double x08 = variables[7];
            final double x09 = variables[8];
            final double x10 = variables[9];
            final double x11 = variables[10];
            final double[] f = new double[this.m];
            for (int i = 0; i < this.m; ++i) {
                final double temp = i / 10.0;
                final double tmp1 = MathLib.exp(-x05 * temp);
                final double tmp2 = MathLib.exp(-x06 * (temp - x09) * (temp - x09));
                final double tmp3 = MathLib.exp(-x07 * (temp - x10) * (temp - x10));
                final double tmp4 = MathLib.exp(-x08 * (temp - x11) * (temp - x11));
                f[i] = y[i] - (x01 * tmp1 + x02 * tmp2 + x03 * tmp3 + x04 * tmp4);
            }
            return f;
        }

        private static final double[] y = {
            1.366, 1.191, 1.112, 1.013, 0.991,
            0.885, 0.831, 0.847, 0.786, 0.725,
            0.746, 0.679, 0.608, 0.655, 0.616,
            0.606, 0.602, 0.626, 0.651, 0.724,
            0.649, 0.649, 0.694, 0.644, 0.624,
            0.661, 0.612, 0.558, 0.533, 0.495,
            0.500, 0.423, 0.395, 0.375, 0.372,
            0.391, 0.396, 0.405, 0.428, 0.429,
            0.523, 0.562, 0.607, 0.653, 0.672,
            0.708, 0.633, 0.668, 0.645, 0.632,
            0.591, 0.559, 0.597, 0.625, 0.739,
            0.710, 0.729, 0.720, 0.636, 0.581,
            0.428, 0.292, 0.162, 0.098, 0.054
        };
    }
}
