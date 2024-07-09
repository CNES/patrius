/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * @history 23/01/2013
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Moved the Screw object and test class to commons math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test class for Screw
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: ScrewTest.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 1.3
 * 
 */
public class ScrewTest {

    /** threshold */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Screw tests
         * 
         * @featureDescription Perform simple unit tests.
         * 
         * @coveredRequirements DV-MATHS_480, DV-MATHS_490
         */
        SCREW_TESTS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SCREW_TESTS}
     * 
     * @testedMethod {@link Screw#getOrigin()}
     * @testedMethod {@link Screw#getTranslation()}
     * @testedMethod {@link Screw#getRotation()}
     * @testedMethod {@link Screw#sum(Screw)}
     * @testedMethod {@link Screw#Screw(Vector3D, Vector3D, Vector3D)}
     * 
     * @description test the getters
     * 
     * @input screw
     * 
     * @output origin, translation, rotation
     * 
     * @testPassCriteria the actual screw is the same as the expected one, to the eps machine threshold
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSum() {
        final Screw s1 = new Screw(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        final Screw s2 = new Screw(Vector3D.MINUS_J, Vector3D.MINUS_K, Vector3D.MINUS_I);

        final Screw sd = s1.sum(s2);

        this.assertVectors(sd.getOrigin(), Vector3D.MINUS_I, this.eps);
        this.assertVectors(sd.getTranslation(), Vector3D.MINUS_J.add(Vector3D.MINUS_K), this.eps);
        this.assertVectors(sd.getRotation(), Vector3D.MINUS_K.add(Vector3D.PLUS_J), this.eps);

        final Screw s3 = new Screw(s1);

        this.assertVectors(s1.getOrigin(), s3.getOrigin(), 0);
        this.assertVectors(s1.getTranslation(), s3.getTranslation(), 0);
        this.assertVectors(s1.getRotation(), s3.getRotation(), 0);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SCREW_TESTS}
     * 
     * @testedMethod {@link Screw#getOrigin()}
     * @testedMethod {@link Screw#getTranslation()}
     * @testedMethod {@link Screw#getRotation()}
     * @testedMethod {@link Screw#displace(Vector3D)}
     * @testedMethod {@link Screw#Screw(Vector3D, Vector3D, Vector3D)}
     * 
     * @description test the getters
     * 
     * @input screw
     * 
     * @output origin, translation, rotation
     * 
     * @testPassCriteria the actual screw is the same as the expected one, to the eps machine threshold
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testDisplace() {
        final Screw s = new Screw(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        final Screw sd = s.displace(Vector3D.PLUS_I);

        this.assertVectors(sd.getOrigin(), Vector3D.PLUS_I, this.eps);
        this.assertVectors(sd.getTranslation(), Vector3D.MINUS_J, this.eps);
        this.assertVectors(sd.getRotation(), Vector3D.PLUS_K, this.eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SCREW_TESTS}
     * 
     * @testedMethod {@link Screw#getOrigin()}
     * @testedMethod {@link Screw#getTranslation()}
     * @testedMethod {@link Screw#getRotation()}
     * @testedMethod {@link Screw#Screw(Vector3D, Vector3D, Vector3D)}
     * 
     * @description test the getters
     * 
     * @input screw
     * 
     * @output origin, translation, rotation
     * 
     * @testPassCriteria the actual screw is the same as the expected one, to the eps machine threshold
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetters() {
        final Screw s = new Screw(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        this.assertVectors(s.getOrigin(), Vector3D.MINUS_I, this.eps);
        this.assertVectors(s.getTranslation(), Vector3D.MINUS_J, this.eps);
        this.assertVectors(s.getRotation(), Vector3D.MINUS_K, this.eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SCREW_TESTS}
     * 
     * @testedMethod {@link Screw#toString()}
     * 
     * @description test toString method
     * 
     * @input screw
     * 
     * @output String
     * 
     * @testPassCriteria the actual String is the same as the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testToString() {
        final Screw s = new Screw(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        Assert.assertEquals(s.toString(), "Screw{Origin{-1; 0; 0},Translation{0; -1; 0},Rotation{0; 0; -1}}");
    }

    /**
     * test vectors
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param thr
     *        threshold
     */
    public void assertVectors(final Vector3D exp, final Vector3D act, final double thr) {
        Assert.assertEquals(exp.getX(), act.getX(), thr);
        Assert.assertEquals(exp.getY(), act.getY(), thr);
        Assert.assertEquals(exp.getZ(), act.getZ(), thr);
    }
}
