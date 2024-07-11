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
 * @history creation 12/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::FA:1192:30/08/2017:update parts frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for the RadiativeSphereProperty property.
 *              </p>
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class RadiativeSpherePropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle RadiativeSphereProperty property.
         * 
         * @featureDescription RadiativeSphereProperty property for an assembly's part.
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430
         */
        RADIATIVE_SPHERE_PROPERTY
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
     * @testedFeature {@link features#RADIATIVE_SPHERE_PROPERTY}
     * 
     * @testedMethod {@link RadiativeSphereProperty#RadiativeSphereProperty(double)}
     * 
     * @description Creation of an assembly and testing the radiative sphere property of the parts.
     * 
     * @input Assembly with radiative sphere properties.
     * 
     * @output RadiativeSphereProperty property.
     * 
     * @testPassCriteria The radiative sphere properties are right.
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void radiativeSpherePropertyTest() {

        /*
         * Test on a simple spacecraft
         */

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // properties adding
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

            // add the radiative sphere properties
            final IPartProperty radMainSphereProp = new RadiativeSphereProperty(5.);
            builder.addProperty(radMainSphereProp, this.mainBody);

            final IPartProperty radPartSphereProp = new RadiativeSphereProperty(2.);
            builder.addProperty(radPartSphereProp, this.part2);

            // add the radiative properties on parts
            final IPartProperty radPropMain = new RadiativeProperty(0.7, 0.2, 0.2);
            builder.addProperty(radPropMain, this.mainBody);

            final IPartProperty radPropPart2 = new RadiativeProperty(0.5, 0.1, 0.);
            builder.addProperty(radPropPart2, this.part2);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            final IPart mainPart = assembly.getPart(this.mainBody);
            final IPart partTwo = assembly.getPart(this.part2);

            final IPartProperty propMainPart = mainPart.getProperty(PropertyType.RADIATIVE_CROSS_SECTION);
            final IPartProperty proPart2 = partTwo.getProperty(PropertyType.RADIATIVE_CROSS_SECTION);

            // main part
            final double mainSphereRadius = ((RadiativeSphereProperty) propMainPart).getSphereRadius();
            Assert.assertEquals(5., mainSphereRadius, this.comparisonEpsilon);

            final double mainSphereArea = ((RadiativeSphereProperty) propMainPart).getSphereArea();
            Assert.assertEquals(5. * 5. * FastMath.PI, mainSphereArea, this.comparisonEpsilon);

            // part 2
            final double part2SphereRadius = ((RadiativeSphereProperty) proPart2).getSphereRadius();

            Assert.assertEquals(2., part2SphereRadius, this.comparisonEpsilon);

            // Check negative surface
            final RadiativeSphereProperty radiativePropNegative = new RadiativeSphereProperty(new Parameter("", -5));
            Assert.assertEquals(-5, radiativePropNegative.getSphereArea(), 0);
            try {
                radiativePropNegative.getSphereRadius();
                Assert.fail();
            } catch (final PatriusException e) {
                Assert.assertTrue(true);
            }

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

}
