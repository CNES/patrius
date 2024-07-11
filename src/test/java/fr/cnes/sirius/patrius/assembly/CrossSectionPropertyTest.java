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
 * @history creation 8/03/2012
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

import fr.cnes.sirius.patrius.assembly.properties.CrossSectionProviderProperty;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plate;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              Test class for the "cross section provider" part property.
 *              </p>
 * 
 * @see CrossSectionProviderProperty
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class CrossSectionPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Cross section provider
         * 
         * @featureDescription The part property that can provide the cross section
         *                     of a part for forces computations
         * 
         * @coveredRequirements DV-VEHICULE_110
         */
        CROSS_SECTION_PROVIDER
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
     * @testedFeature {@link features#CROSS_SECTION_PROVIDER}
     * 
     * @testedMethod {@link CrossSectionProviderProperty#getCrossSection(Vector3D)}
     * 
     * @description Creation of an assembly and adding of cross section properties. Test of the
     *              cross sections.
     * 
     * @input Assembly with cross section properties
     * 
     * @output the cross sections
     * 
     * @testPassCriteria the computed cross sections are the expected ones
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void getCrossSectionTest() {

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // initialisations
        final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
        final Rotation rot = new Rotation(Vector3D.PLUS_J, FastMath.PI / 4.0);
        final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
        final Transform transform2 = new Transform(AbsoluteDate.J2000_EPOCH, rot);
        final Transform transformOK = new Transform(AbsoluteDate.J2000_EPOCH, transform2, transform1);

        // properties adding
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            builder.addPart(this.part2, this.mainBody, transformOK);

            // adding of cross section properties
            final CrossSectionProvider sphere = new Sphere(Vector3D.ZERO, 5.0);
            final CrossSectionProvider plate = new Plate(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.PLUS_J, 4.0, 6.0);

            final IPartProperty sphereProp = new CrossSectionProviderProperty(sphere);
            final IPartProperty plateProp = new CrossSectionProviderProperty(plate);

            builder.addProperty(sphereProp, this.mainBody);
            builder.addProperty(plateProp, this.part2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        final IPart mainPart = assembly.getPart(this.mainBody);
        final IPartProperty propMainPart = mainPart.getProperty(PropertyType.CROSS_SECTION);

        final IPart secPart = assembly.getPart(this.part2);
        final IPartProperty propSecPart = secPart.getProperty(PropertyType.CROSS_SECTION);

        // cross section with the direction : +I in the main part frame
        try {
            // main part
            final double crossSectionMainPart = ((CrossSectionProviderProperty) propMainPart)
                .getCrossSection(Vector3D.PLUS_I);

            Assert.assertEquals(FastMath.PI * 25.0, crossSectionMainPart, this.comparisonEpsilon);

            // second part
            final Transform toSecFrame = mainPart.getFrame().getTransformTo(secPart.getFrame(), this.date);
            final Vector3D directionInSecFrame = toSecFrame.transformVector(Vector3D.PLUS_I);
            final double crossSectionSecPart = ((CrossSectionProviderProperty) propSecPart)
                .getCrossSection(directionInSecFrame);

            Assert.assertEquals(24.0 / MathLib.sqrt(2.0), crossSectionSecPart, this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

}
