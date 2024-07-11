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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.PerfTestUtils;

/**
 * Performance tests for FastMath.
 * Not enabled by default, as the class does not end in Test.
 * 
 * Invoke by running<br/>
 * {@code mvn test -Dtest=FastMathTestPerformance}<br/>
 * or by running<br/>
 * {@code mvn test -Dtest=FastMathTestPerformance -DargLine="-DtestRuns=1234 -server"}<br/>
 */
public class FastMathTestPerformance {
    private static final int RUNS = Integer.parseInt(System.getProperty("testRuns", "10000000"));
    private static final double F1 = 1d / RUNS;

    // Header format
    private static final String FMT_HDR = "%-5s %13s %13s %13s Runs=%d Java %s (%s) %s (%s)";
    // Detail format
    private static final String FMT_DTL = "%-5s %6d %6.1f %6d %6.4f %6d %6.4f";

    @BeforeClass
    public static void header() {
        System.out.println(String.format(FMT_HDR,
            "Name", "StrictMath", "FastMath", "Math", RUNS,
            System.getProperty("java.version"),
            System.getProperty("java.runtime.version", "?"),
            System.getProperty("java.vm.name"),
            System.getProperty("java.vm.version")
            ));
    }

    private static void report(final String name, final long strictMathTime, final long fastMathTime,
                               final long mathTime) {
        final long unitTime = strictMathTime;
        System.out.println(String.format(FMT_DTL,
            name,
            strictMathTime / RUNS, (double) strictMathTime / unitTime,
            fastMathTime / RUNS, (double) fastMathTime / unitTime,
            mathTime / RUNS, (double) mathTime / unitTime
            ));
    }

    @Test
    public void testLog() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.log(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long strictMath = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.log(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.log(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long mathTime = System.nanoTime() - time;

        report("log", strictMath, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testLog10() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.log10(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long strictMath = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.log10(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.log10(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long mathTime = System.nanoTime() - time;

        report("log10", strictMath, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testLog1p() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.log1p(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long strictMath = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.log1p(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.log1p(Math.PI + i/* 1.0 + i/1e9 */);
        }
        final long mathTime = System.nanoTime() - time;

        report("log1p", strictMath, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testPow() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.pow(Math.PI + i * F1, i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.pow(Math.PI + i * F1, i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.pow(Math.PI + i * F1, i * F1);
        }
        final long mathTime = System.nanoTime() - time;
        report("pow", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testExp() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.exp(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.exp(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.exp(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("exp", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testSin() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.sin(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.sin(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.sin(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("sin", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testAsin() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.asin(i / (double) RUNS);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.asin(i / (double) RUNS);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.asin(i / (double) RUNS);
        }
        final long mathTime = System.nanoTime() - time;

        report("asin", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testCos() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.cos(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.cos(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.cos(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("cos", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testAcos() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.acos(i / (double) RUNS);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.acos(i / (double) RUNS);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.acos(i / (double) RUNS);
        }
        final long mathTime = System.nanoTime() - time;
        report("acos", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testTan() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.tan(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.tan(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.tan(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("tan", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testAtan() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.atan(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.atan(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.atan(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("atan", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testAtan2() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.atan2(i * F1, i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.atan2(i * F1, i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.atan2(i * F1, i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("atan2", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testHypot() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.hypot(i * F1, i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.hypot(i * F1, i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.hypot(i * F1, i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("hypot", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testCbrt() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.cbrt(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.cbrt(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.cbrt(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("cbrt", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testSqrt() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.sqrt(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.sqrt(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.sqrt(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("sqrt", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testCosh() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.cosh(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.cosh(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.cosh(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("cosh", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testSinh() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.sinh(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.sinh(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.sinh(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("sinh", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testTanh() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.tanh(i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.tanh(i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.tanh(i * F1);
        }
        final long mathTime = System.nanoTime() - time;

        report("tanh", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testExpm1() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.expm1(-i * F1);
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.expm1(-i * F1);
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.expm1(-i * F1);
        }
        final long mathTime = System.nanoTime() - time;
        report("expm1", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testAbs() {
        double x = 0;
        long time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += StrictMath.abs(i * (1 - 0.5 * RUNS));
        }
        final long strictTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += MathLib.abs(i * (1 - 0.5 * RUNS));
        }
        final long fastTime = System.nanoTime() - time;

        x = 0;
        time = System.nanoTime();
        for (int i = 0; i < RUNS; i++) {
            x += Math.abs(i * (1 - 0.5 * RUNS));
        }
        final long mathTime = System.nanoTime() - time;

        report("abs", strictTime, fastTime, mathTime);
        Assert.assertTrue(!Double.isNaN(x));
    }

    @Test
    public void testSimpleBenchmark() {
        final String SM = "StrictMath";
        final String M = "Math";
        final String FM = "FastMath";

        final int numStat = 100;
        final int numCall = RUNS / numStat;

        final double x = Math.random();
        final double y = Math.random();

        PerfTestUtils.timeAndReport("log",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.log(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.log(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.log(x);
                }
            });

        PerfTestUtils.timeAndReport("log10",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.log10(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.log10(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.log10(x);
                }
            });

        PerfTestUtils.timeAndReport("log1p",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.log1p(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.log1p(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.log1p(x);
                }
            });

        PerfTestUtils.timeAndReport("pow",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.pow(x, y);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.pow(x, y);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.pow(x, y);
                }
            });

        PerfTestUtils.timeAndReport("exp",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.exp(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.exp(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.exp(x);
                }
            });

        PerfTestUtils.timeAndReport("sin",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.sin(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.sin(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.sin(x);
                }
            });

        PerfTestUtils.timeAndReport("asin",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.asin(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.asin(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.asin(x);
                }
            });

        PerfTestUtils.timeAndReport("cos",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.cos(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.cos(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.cos(x);
                }
            });

        PerfTestUtils.timeAndReport("acos",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.acos(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.acos(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.acos(x);
                }
            });

        PerfTestUtils.timeAndReport("tan",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.tan(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.tan(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.tan(x);
                }
            });

        PerfTestUtils.timeAndReport("atan",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.atan(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.atan(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.atan(x);
                }
            });

        PerfTestUtils.timeAndReport("atan2",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.atan2(x, y);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.atan2(x, y);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.atan2(x, y);
                }
            });

        PerfTestUtils.timeAndReport("hypot",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.hypot(x, y);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.hypot(x, y);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.hypot(x, y);
                }
            });

        PerfTestUtils.timeAndReport("cbrt",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.cbrt(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.cbrt(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.cbrt(x);
                }
            });

        PerfTestUtils.timeAndReport("sqrt",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.sqrt(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.sqrt(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.sqrt(x);
                }
            });

        PerfTestUtils.timeAndReport("cosh",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.cosh(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.cosh(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.cosh(x);
                }
            });

        PerfTestUtils.timeAndReport("sinh",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.sinh(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.sinh(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.sinh(x);
                }
            });

        PerfTestUtils.timeAndReport("tanh",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.tanh(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.tanh(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.tanh(x);
                }
            });

        PerfTestUtils.timeAndReport("expm1",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.expm1(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.expm1(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.expm1(x);
                }
            });

        PerfTestUtils.timeAndReport("abs",
            numCall,
            numStat,
            false,
            new PerfTestUtils.RunTest(SM){
                @Override
                public Double call() throws Exception {
                    return StrictMath.abs(x);
                }
            },
            new PerfTestUtils.RunTest(M){
                @Override
                public Double call() throws Exception {
                    return Math.abs(x);
                }
            },
            new PerfTestUtils.RunTest(FM){
                @Override
                public Double call() throws Exception {
                    return MathLib.abs(x);
                }
            });
    }
}
