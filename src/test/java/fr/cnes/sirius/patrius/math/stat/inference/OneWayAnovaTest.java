/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.stat.inference;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

/**
 * Test cases for the OneWayAnovaImpl class.
 * 
 * @version $Id: OneWayAnovaTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class OneWayAnovaTest {

    protected OneWayAnova testStatistic = new OneWayAnova();

    private final double[] emptyArray = {};

    private final double[] classA =
    { 93.0, 103.0, 95.0, 101.0, 91.0, 105.0, 96.0, 94.0, 101.0 };
    private final double[] classB =
    { 99.0, 92.0, 102.0, 100.0, 102.0, 89.0 };
    private final double[] classC =
    { 110.0, 115.0, 111.0, 117.0, 128.0, 117.0 };

    @Test
    public void testAnovaFValue() {
        // Target comparison values computed using R version 2.6.0 (Linux version)
        final List<double[]> threeClasses = new ArrayList<double[]>();
        threeClasses.add(this.classA);
        threeClasses.add(this.classB);
        threeClasses.add(this.classC);

        Assert.assertEquals("ANOVA F-value", 24.67361709460624,
            this.testStatistic.anovaFValue(threeClasses), 1E-12);

        final List<double[]> twoClasses = new ArrayList<double[]>();
        twoClasses.add(this.classA);
        twoClasses.add(this.classB);

        Assert.assertEquals("ANOVA F-value", 0.0150579150579,
            this.testStatistic.anovaFValue(twoClasses), 1E-12);

        final List<double[]> emptyContents = new ArrayList<double[]>();
        emptyContents.add(this.emptyArray);
        emptyContents.add(this.classC);
        try {
            this.testStatistic.anovaFValue(emptyContents);
            Assert.fail("empty array for key classX, MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }

        final List<double[]> tooFew = new ArrayList<double[]>();
        tooFew.add(this.classA);
        try {
            this.testStatistic.anovaFValue(tooFew);
            Assert.fail("less than two classes, MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testAnovaPValue() {
        // Target comparison values computed using R version 2.6.0 (Linux version)
        final List<double[]> threeClasses = new ArrayList<double[]>();
        threeClasses.add(this.classA);
        threeClasses.add(this.classB);
        threeClasses.add(this.classC);

        Assert.assertEquals("ANOVA P-value", 6.959446E-06,
            this.testStatistic.anovaPValue(threeClasses), 1E-12);

        final List<double[]> twoClasses = new ArrayList<double[]>();
        twoClasses.add(this.classA);
        twoClasses.add(this.classB);

        Assert.assertEquals("ANOVA P-value", 0.904212960464,
            this.testStatistic.anovaPValue(twoClasses), 1E-12);

    }

    @Test
    public void testAnovaTest() {
        // Target comparison values computed using R version 2.3.1 (Linux version)
        final List<double[]> threeClasses = new ArrayList<double[]>();
        threeClasses.add(this.classA);
        threeClasses.add(this.classB);
        threeClasses.add(this.classC);

        Assert.assertTrue("ANOVA Test P<0.01", this.testStatistic.anovaTest(threeClasses, 0.01));

        final List<double[]> twoClasses = new ArrayList<double[]>();
        twoClasses.add(this.classA);
        twoClasses.add(this.classB);

        Assert.assertFalse("ANOVA Test P>0.01", this.testStatistic.anovaTest(twoClasses, 0.01));
    }

}
