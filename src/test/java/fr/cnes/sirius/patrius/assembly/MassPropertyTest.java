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
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:19/09/2013:New API for Mass properties
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.properties.CrossSectionProviderProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plate;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for the mass property.
 *              </p>
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class MassPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Mass property.
         * 
         * @featureDescription Mass property for an assembly's part.
         * 
         * @coveredRequirements DV-VEHICULE_130
         */
        MASS_PROPERTY
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
     * @throws PatriusException
     *         if the mass is negative
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_PROPERTY}
     * 
     * @testedMethod {@link MassProperty#MassProperty(double)}
     * 
     * @description Creation of an assembly and testing the mass property of the parts.
     * 
     * @input Assembly with mass properties.
     * 
     * @output Mass property.
     * 
     * @testPassCriteria The mass properties are right.
     * 
     * @referenceVersion 1.1
     * @nonRegressionVersion 1.1
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void massPropertyTest() throws PatriusException {

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

            // adding of mass properties
            final IPartProperty massPropMainPart = new MassProperty(1000.);
            final IPartProperty massPropPart2 = new MassProperty(20.7);

            builder.addProperty(massPropMainPart, this.mainBody);
            builder.addProperty(massPropPart2, this.part2);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            final IPart mainPart = assembly.getPart(this.mainBody);
            final IPart partTwo = assembly.getPart(this.part2);

            final IPartProperty propMainPart = mainPart.getProperty(PropertyType.MASS);
            final IPartProperty proPart2 = partTwo.getProperty(PropertyType.MASS);

            // main part
            double mass = ((MassProperty) propMainPart).getMass();
            Assert.assertEquals(1000., mass, this.comparisonEpsilon);

            // part 2
            mass = ((MassProperty) proPart2).getMass();
            Assert.assertEquals(20.7, mass, this.comparisonEpsilon);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     *         if the mass is negative
     * @testType UT
     * @testedFeature {@link features#MASS_PROPERTY}
     * @testedMethod {@link MassProperty#MassProperty(double)}
     * @description Creation of a mass property with a negative mass : he won't like this
     * @testPassCriteria OrekitException with PatriusMessages.NOT_POSITIVE_MASS
     * @since 2.3
     */
    @Test(expected = PatriusException.class)
    public final void constructorErrorTest() throws PatriusException {

        final double negativeMass = -1000.0;
        new MassProperty(negativeMass);
    }

    /**
     * @testType UT
     * @throws PatriusException
     *         if the mass is negative
     * @testedFeature {@link features#MASS_PROPERTY}
     * @testedMethod {@link MassProperty#updateMass(double)}
     * @description update mass property with negative mass
     * @testPassCriteria OrekitException with PatriusMessages.NOT_POSITIVE_MASS
     * @since 2.3.1
     */
    @Test(expected = PatriusException.class)
    public final void updateMassErrorTest() throws PatriusException {
        final MassProperty mass = new MassProperty(10);
        mass.updateMass(-10);
    }
}
