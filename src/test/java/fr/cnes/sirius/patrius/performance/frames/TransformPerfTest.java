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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.performance.frames;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * @description test class for Transform
 * 
 * @author Julie Anton
 * 
 * @version $Id: TransformPerfTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class TransformPerfTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle robustness
         * 
         * @featureDescription test the robustness of the transformation
         * 
         * @coveredRequirements none
         */
        ROBUSTNESS
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon for angle comparison. */
    private final double angleEpsilon = 1e-10;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#ROBUSTNESS}
     * 
     * @testedMethod {@link Transform#transformPosition(Vector3D)}
     * @testedMethod {@link Transform#transformVector(Vector3D)}
     * 
     * @description it is a robustness test for transformations based on rotations
     * 
     * @input point A (2.5,-4.7,0.0001)
     * 
     * @output image point of the point A by the rotation around z axis and of angle 1° applied 3600000 times
     * 
     * @testPassCriteria the obtained point has the same coordinates that the initial point A with an epsilon equal to
     *                   the number of trials times 1e-14 (which takes into account the computation errors) for the
     *                   vector component and 1e-10 for the angle.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testRobustnessRotation() {

        final Rotation rotation = new Rotation(Vector3D.PLUS_K, MathUtils.DEG_TO_RAD);
        final Transform transformation = new Transform(AbsoluteDate.J2000_EPOCH, rotation);
        final Vector3D a = new Vector3D(2.5, -4.7, 0.0001);
        Vector3D tmpA = a;
        Vector3D tmpB = a;
        final int numberOfTrials = 10000 * 360;
        for (int i = 0; i < numberOfTrials; ++i) {
            tmpA = transformation.transformPosition(tmpA);
            tmpB = transformation.transformVector(tmpB);
        }
        this.checkVectors(tmpA, a, numberOfTrials);
        this.checkVectors(tmpB, a, numberOfTrials);
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#ROBUSTNESS}
     * 
     * @testedMethod {@link Transform#transformPosition(Vector3D)}
     * @testedMethod {@link Transform#transformVector(Vector3D)}
     * 
     * @description it is a robustness test for transformations based on translations
     * 
     * @input point A (2.5,-4.7,0.0001)
     * 
     * @output image point of the point A by the translation of vector i applied 10000 times
     * 
     * @testPassCriteria the obtained point has the following coordinates : (10002.5,-4.7,0.0001) with an epsilon equal
     *                   to the number of trials times 1e-14 (which takes into account the computation errors) for the
     *                   vector component and 1e-10 for the angle
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testRobustnessTranslation() {

        final Transform transformation = new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I);
        final Vector3D a = new Vector3D(2.5, -4.7, 0.0001);
        Vector3D tmpA = a;
        final int numberOfTrials = 10000;
        for (int i = 0; i < numberOfTrials; ++i) {
            tmpA = transformation.transformPosition(tmpA);
        }
        this.checkVectors(tmpA, a.add(10000, Vector3D.MINUS_I), numberOfTrials);
    }

    /**
     * checkVectors.
     * 
     * @param v1
     *        v1
     * @param v2
     *        v2
     * @param n
     *        n
     */
    private void checkVectors(final Vector3D v1, final Vector3D v2, final int n) {

        final Vector3D d = v1.subtract(v2);

        Assert.assertEquals(0, d.getX(), n * this.comparisonEpsilon);
        Assert.assertEquals(0, d.getY(), n * this.comparisonEpsilon);
        Assert.assertEquals(0, d.getZ(), n * this.comparisonEpsilon);

        if ((v1.getNorm() > this.machineEpsilon) && (v2.getNorm() > this.machineEpsilon)) {
            final Rotation r = new Rotation(v1, v2);
            Assert.assertEquals(0, r.getAngle(), n * this.angleEpsilon);

        }

    }
}
