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
 * @history creation 04/04/2017
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:04/04/2017:create vehicle object
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the {@link RadiativeCrossSectionProperty} class.
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since version 3.4
 * 
 */
public class RadiativeCrossSectionPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative cross section property
         * 
         * @featureDescription Radiative property for a cylinder, parallelepiped or sphere : functional tests
         */
        RADIATIVE_CROSS_SECTION_PROPERTY
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_CROSS_SECTION_PROPERTY}
     * 
     * @testedMethod {@link RadiativeCrossSectionProperty#RadiativeCrossSectionProperty(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider)}
     * @testedMethod {@link RadiativeCrossSectionProperty#getType()}
     * 
     * @description Creation of an assembly and testing the radiative cross section property of the parts.
     * 
     * @input Assembly with radiative sphere properties.
     * 
     * @output RadiativeCrossSectionProperty property.
     * 
     * @testPassCriteria The radiative cross section properties are right.
     * 
     * @referenceVersion 3.4
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testRadiativeCrossSectionProperty() {

        // Create the property
        final RightCircularCylinder shape = new RightCircularCylinder(Vector3D.ZERO, Vector3D.PLUS_K, 1.0, 4.0);
        final RadiativeCrossSectionProperty radProp = new RadiativeCrossSectionProperty(shape);

        // The property is of expected type
        Assert.assertTrue(radProp.getType() == PropertyType.RADIATIVE_CROSS_SECTION);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_CROSS_SECTION_PROPERTY}
     * 
     * @testedMethod {@link RadiativeCrossSectionProperty#getCrossSection(SpacecraftState, fr.cnes.sirius.patrius.assembly.IPart, Vector3D)}
     * 
     * @description This test aims at validating the cross section method in the case where an Assembly part is defined
     *              by any transform
     *              wrt the main part (different from identity transform).
     *              The velocity computation in the part's frame is recomputed manually using the good rotations to find
     *              the expected cross section.
     * 
     * @input A main shape as the property constructor
     * 
     * @output Main shape cross section
     * 
     * @testPassCriteria actual and expected cross sections must be the same.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testCrossSectionComputation() throws PatriusException {
        // Use a cylinder
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K, 2,
            10.);

        // Define the property
        final RadiativeCrossSectionProperty property = new RadiativeCrossSectionProperty(cylinder);

        // Attitude law: body center pointing
        final AttitudeLaw law = new BodyCenterPointing();

        // Define satellite position (on Y-axis, with velocity toward -X)
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D pos = new Vector3D(0, 7000000, 0);
        final Vector3D vel = new Vector3D(-7000, 0, 0);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel),
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, law.getAttitude(orbit));

        // Define frames:
        // - Main frame is built according to attitude law
        // - part frame is such that +x is along +y and +y is along +z
        final Rotation rot1 = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, Vector3D.MINUS_I, Vector3D.MINUS_J);
        final Frame mainFrame = new Frame(FramesFactory.getGCRF(), new Transform(date, rot1), "ParentFrame");
        final Rotation rot2 = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final Frame partFrame = new Frame(mainFrame, new Transform(date, rot2), "PartFrame");

        // Checks with different directions
        Assert.assertEquals(4. * FastMath.PI, property.getCrossSection(state, Vector3D.MINUS_I, partFrame),
            Precision.EPSILON);
        Assert.assertEquals(40., property.getCrossSection(state, Vector3D.PLUS_K, partFrame), Precision.EPSILON);
    }
}
