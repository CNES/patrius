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
 * @history 23/01/2013
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:85:18/07/2013:Moved the Wrench object, test class and WrenchModel interface to orekit
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Screw;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for Wrench
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: WrenchTest.java 18089 2017-10-02 17:02:50Z bignon $
 * 
 * @since 1.3
 * 
 */
public class WrenchTest {

    /** threshold */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Wrench tests
         * 
         * @featureDescription Perform simple unit tests.
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_25, DV-COUPLES_35
         */
        WRENCH_TESTS
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(WrenchTest.class.getSimpleName(), "Wrench");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#WRENCH_TESTS}
     * 
     * @testedMethod {@link Wrench#getOrigin()}
     * @testedMethod {@link Wrench#getForce()}
     * @testedMethod {@link Wrench#getTorque()}
     * @testedMethod {@link Wrench#add(Wrench)}
     * @testedMethod {@link Wrench#Wrench(Vector3D, Vector3D, Vector3D)}
     * 
     * @description test the sum of two wrenches
     * 
     * @input wrench
     * 
     * @output wrench
     * 
     * @testPassCriteria the actual wrench is the same as the expected one, to the eps machine threshold
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSum() {

        Report.printMethodHeader("testSum", "Sum computation", "Math", this.eps, ComparisonType.ABSOLUTE);

        final Wrench s1 = new Wrench(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        final Wrench s2 = new Wrench(Vector3D.MINUS_J, Vector3D.MINUS_K, Vector3D.MINUS_I);

        final Wrench sd = s1.add(s2);
        final Wrench sd1 = Wrench.sum(s1, s2);

        this.assertVectors(sd.getOrigin(), Vector3D.MINUS_I, this.eps);
        this.assertVectors(sd.getForce(), Vector3D.MINUS_J.add(Vector3D.MINUS_K), this.eps);
        this.assertVectors(sd.getTorque(), Vector3D.MINUS_K.add(Vector3D.PLUS_J), this.eps);

        this.assertVectors(sd1.getOrigin(), Vector3D.MINUS_I, this.eps);
        this.assertVectors(sd1.getForce(), Vector3D.MINUS_J.add(Vector3D.MINUS_K), this.eps);
        this.assertVectors(sd1.getTorque(), Vector3D.MINUS_K.add(Vector3D.PLUS_J), this.eps);

        Report.printToReport("Wrench origin", Vector3D.MINUS_I, sd.getOrigin());
        Report.printToReport("Wrench force", Vector3D.MINUS_J.add(Vector3D.MINUS_K), sd.getForce());
        Report.printToReport("Wrench torque", Vector3D.MINUS_K.add(Vector3D.PLUS_J), sd.getTorque());

        Report.printToReport("Wrench origin 2", Vector3D.MINUS_I, sd1.getOrigin());
        Report.printToReport("Wrench force 2", Vector3D.MINUS_J.add(Vector3D.MINUS_K), sd1.getForce());
        Report.printToReport("Wrench torque 2", Vector3D.MINUS_K.add(Vector3D.PLUS_J), sd1.getTorque());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#WRENCH_TESTS}
     * 
     * @testedMethod {@link Wrench#getOrigin()}
     * @testedMethod {@link Wrench#getForce()}
     * @testedMethod {@link Wrench#getTorque()}
     * @testedMethod {@link Wrench#getTorque(Vector3D)}
     * @testedMethod {@link Wrench#displace(Vector3D)}
     * @testedMethod {@link Wrench#Wrench(Vector3D, Vector3D, Vector3D)}
     * 
     * @description test the displacement of a wrenches
     * 
     * @input new location
     * 
     * @output displaced wrench
     * 
     * @testPassCriteria the actual torque is the same as the expected one, to the eps machine threshold
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testDisplace() {

        Report.printMethodHeader("testDisplace", "Displacement computation", "Math", this.eps, ComparisonType.ABSOLUTE);

        final Wrench s = new Wrench(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);

        final Wrench sd = s.displace(Vector3D.PLUS_I);
        final Wrench sd1 = Wrench.displace(s, Vector3D.PLUS_I);

        this.assertVectors(sd.getOrigin(), Vector3D.PLUS_I, this.eps);
        this.assertVectors(sd.getForce(), Vector3D.MINUS_J, this.eps);
        this.assertVectors(sd.getTorque(), Vector3D.PLUS_K, this.eps);

        this.assertVectors(sd.getTorque(Vector3D.PLUS_I), Vector3D.PLUS_K, this.eps);

        this.assertVectors(sd1.getOrigin(), Vector3D.PLUS_I, this.eps);
        this.assertVectors(sd1.getForce(), Vector3D.MINUS_J, this.eps);
        this.assertVectors(sd1.getTorque(), Vector3D.PLUS_K, this.eps);

        this.assertVectors(sd1.getTorque(Vector3D.PLUS_I), Vector3D.PLUS_K, this.eps);

        Report.printToReport("Wrench origin", Vector3D.PLUS_I, sd.getOrigin());
        Report.printToReport("Wrench force", Vector3D.MINUS_J, sd.getForce());
        Report.printToReport("Wrench torque", Vector3D.PLUS_K, sd.getTorque());
        Report.printToReport("Wrench origin (other point)", Vector3D.PLUS_K, sd.getTorque(Vector3D.PLUS_I));

        Report.printToReport("Wrench origin 2", Vector3D.PLUS_I, sd1.getOrigin());
        Report.printToReport("Wrench force 2", Vector3D.MINUS_J, sd1.getForce());
        Report.printToReport("Wrench torque 2", Vector3D.PLUS_K, sd1.getTorque());
        Report.printToReport("Wrench origin (other point) 2", Vector3D.PLUS_K, sd1.getTorque(Vector3D.PLUS_I));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#WRENCH_TESTS}
     * 
     * @testedMethod {@link Wrench#getOrigin()}
     * @testedMethod {@link Wrench#getForce()}
     * @testedMethod {@link Wrench#getTorque()}
     * @testedMethod {@link Wrench#Wrench(Vector3D, Vector3D, Vector3D)}
     * 
     * @description test the getters
     * 
     * @input wrench
     * 
     * @output origin, force, torque
     * 
     * @testPassCriteria the actual wrench is the same as the expected one, to the eps machine threshold
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGetters() {
        final Wrench s = new Wrench(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        this.assertVectors(s.getOrigin(), Vector3D.MINUS_I, this.eps);
        this.assertVectors(s.getForce(), Vector3D.MINUS_J, this.eps);
        this.assertVectors(s.getTorque(), Vector3D.MINUS_K, this.eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#WRENCH_TESTS}
     * 
     * @testedMethod {@link Screw#toString()}
     * 
     * @description test toString method
     * 
     * @input wrench
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
        final Wrench s = new Wrench(Vector3D.MINUS_I, Vector3D.MINUS_J, Vector3D.MINUS_K);
        Assert.assertEquals(s.toString(), "Wrench{Origin{-1; 0; 0},Force{0; -1; 0},Torque{0; 0; -1}}");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#WRENCH_TESTS}
     * 
     * @testedMethod {@link Screw#toString()}
     * 
     * @description test toString method
     * 
     * @input wrench
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
    public void testDoubleArray() throws PatriusException {
        try {
            new Wrench(new double[] { 1 });
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }
        final Wrench s = new Wrench(new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        this.assertEquals(new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, s.getWrench(), this.eps);
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
    public void assertEquals(final double[] exp, final double[] act, final double thr) {
        Assert.assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            Assert.assertEquals(act[i], exp[i], thr);
        }
    }
}
