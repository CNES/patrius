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
 * @history creation 5/03/1012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.EllipticCone;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              Test class for the geometric property
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class GeometricPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Geometric property
         * 
         * @featureDescription Geometric property for an assembly's part
         * 
         * @coveredRequirements DV-VEHICULE_50, DV-VEHICULE_60, DV-VEHICULE_70,
         *                      DV-VEHICULE_80, DV-VEHICULE_90, DV-VEHICULE_100
         */
        GEOMETRIC_PROPERTY
    }

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * 2nd part's name
     */
    private final String part2 = "part2";

    /**
     * J2000 date
     */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GEOMETRIC_PROPERTY}
     * 
     * @testedMethod {@link GeometricProperty#getShape()}
     * 
     * @description Creation of an assembly and adding of geometric properties. Test of the
     *              properties characteristics.
     * 
     * @input Assembly
     * 
     * @output geometric properties
     * 
     * @testPassCriteria The geometric properties are right and with correct values once expressed
     *                   in a different frame
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void geometryTest() {
        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // init
        final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
        final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);

        // properties adding
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            builder.addPart(this.part2, this.mainBody, transform1);

            // adding of a geometric property
            final SolidShape sphere = new Sphere(Vector3D.ZERO, 1);
            final IPartProperty shapeProp = new GeometricProperty(sphere);
            builder.addProperty(shapeProp, this.mainBody);
            final SolidShape cone = new EllipticCone(Vector3D.PLUS_K, translation, translation.orthogonal(),
                MathUtils.DEG_TO_RAD * 30, MathUtils.DEG_TO_RAD * 20, 1.5);
            final IPartProperty shapeProp2 = new GeometricProperty(cone);
            builder.addProperty(shapeProp2, this.part2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // getting of the parts
        final IPart mainPart = assembly.getMainPart();
        final IPart partTwo = assembly.getPart(this.part2);

        // getting of the part 2 property
        final IPartProperty prop2 = partTwo.getProperty(PropertyType.GEOMETRY);

        final SolidShape part2Shape = ((GeometricProperty) prop2).getShape();
        Assert.assertTrue(EllipticCone.class.isInstance(part2Shape));

        try {
            // main part frame transform
            final Frame mainFrame = mainPart.getFrame();
            final Frame secondFrame = partTwo.getFrame();

            final Transform toMainFrame = secondFrame.getTransformTo(mainFrame, this.date);

            // test of the direction vector of the shape
            final Vector3D directionInMainFrame =
                toMainFrame.transformVector(((EllipticCone) part2Shape).getDirection());
            final Vector3D positionInMainFrame =
                toMainFrame.transformPosition(((EllipticCone) part2Shape).getOrigin());

            Assert.assertEquals(0.0,
                Vector3D.crossProduct(directionInMainFrame, translation).getNorm(), this.comparisonEpsilon);

            Assert.assertEquals(translation.getX(), positionInMainFrame.getX(), this.comparisonEpsilon);
            Assert.assertEquals(translation.getY(), positionInMainFrame.getY(), this.comparisonEpsilon);
            Assert.assertEquals(translation.getZ() + 1.0, positionInMainFrame.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

}
