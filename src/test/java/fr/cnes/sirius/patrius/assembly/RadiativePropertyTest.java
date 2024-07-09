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
 * @history creation 12/03/2012
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:412:05/05/2015:Changed IParamDiffFunction into Parameter in RadiativeProperty
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.properties.CrossSectionProviderProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plate;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * @description <p>
 *              Test class for the radiative property in the visible domain.
 *              </p>
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class RadiativePropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative property (visible domain).
         * 
         * @featureDescription Radiative property for an assembly's part.
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430
         */
        RADIATIVE_PROPERTY
    }

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";

    /**
     * 2nd part's name
     */
    private final String part2 = "part2";

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_PROPERTY}
     * 
     * @testedMethod {@link RadiativeProperty#RadiativeProperty(double, double, double)}
     * 
     * @description Creation of an assembly and testing the radiative property of the parts.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output Radiative property.
     * 
     * @testPassCriteria The radiative properties are right.
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void radiativePropertyTest() {

        /*
         * Test on a simple spacecraft
         */

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

            // adding of radiative properties
            final IPartProperty radPropMain = new RadiativeProperty(0.7, 0.2, 0.2);
            final IPartProperty radPropPart2 = new RadiativeProperty(0.5, 0.1, 0.);

            builder.addProperty(radPropMain, this.mainBody);
            builder.addProperty(radPropPart2, this.part2);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            final IPart mainPart = assembly.getPart(this.mainBody);
            final IPart partTwo = assembly.getPart(this.part2);

            final IPartProperty propMainPart = mainPart.getProperty(PropertyType.RADIATIVE);
            final IPartProperty proPart2 = partTwo.getProperty(PropertyType.RADIATIVE);

            // main part
            double absCoef = ((RadiativeProperty) propMainPart).getAbsorptionRatio().getValue();
            double speCoef = ((RadiativeProperty) propMainPart).getSpecularReflectionRatio().getValue();
            double difCoef = ((RadiativeProperty) propMainPart).getDiffuseReflectionRatio().getValue();

            Assert.assertEquals(0.7, absCoef, this.comparisonEpsilon);
            Assert.assertEquals(0.2, speCoef, this.comparisonEpsilon);
            Assert.assertEquals(0.2, difCoef, this.comparisonEpsilon);

            // part 2
            absCoef = ((RadiativeProperty) proPart2).getAbsorptionRatio().getValue();
            speCoef = ((RadiativeProperty) proPart2).getSpecularReflectionRatio().getValue();
            difCoef = ((RadiativeProperty) proPart2).getDiffuseReflectionRatio().getValue();

            Assert.assertEquals(0.5, absCoef, this.comparisonEpsilon);
            Assert.assertEquals(0.1, speCoef, this.comparisonEpsilon);
            Assert.assertEquals(0.0, difCoef, this.comparisonEpsilon);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

}
