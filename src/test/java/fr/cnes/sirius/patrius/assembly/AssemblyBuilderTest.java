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
 * @history creation 9/02/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:402:07/05/2015:Removal of the double inversion of the rotation
 * VERSION::FA:513:09/03/2016:Make Frame class multithread safe
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:part can have either a Tank or a Mass property, not both
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.EllipticCone;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RectangleCone;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              - Test class for the assembly builder.
 *              </p>
 * 
 * @see AssemblyBuilder
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class AssemblyBuilderTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle assembly builder
         * 
         * @featureDescription Creation of an assembly using the associated builder
         * 
         * @coveredRequirements DV-VEHICULE_10, DV-VEHICULE_40, DV-VEHICULE_340
         */
        ASSEMBLY_BUILDER
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
     * 3rd part's name
     */
    private final String part3 = "part3";
    /**
     * 4th part's name
     */
    private final String part4 = "part4";
    /**
     * J2000 date
     */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_BUILDER}
     * 
     * @testedMethod {@link AssemblyBuilder#addMainPart(String)}
     * @testedMethod {@link AssemblyBuilder#addPart(String, String, fr.cnes.sirius.patrius.frames.transformations.Transform)}
     * @testedMethod {@link AssemblyBuilder#getPart(String)}
     * @testedMethod {@link AssemblyBuilder#removePart(String)}
     * 
     * @description Creation of assembly and adding of parts. Test of those parts.
     *              Test of the removing of the parts.
     * 
     * @input Parts
     * 
     * @output A builder containing the assembly with the expected parts
     * 
     * @testPassCriteria The parts are correctly added and removed, the final assembly contains the
     *                   expected ones.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void partsAddingRemovingTest() {

        Utils.clear();
        
        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // name test
            final IPart mainPart = builder.getPart(this.mainBody);
            Assert.assertEquals(mainPart.getName(), this.mainBody);

            // frame test
            final Frame mainFrame = mainPart.getFrame();
            // name
            Assert.assertEquals(mainFrame.getName(), "mainBodyFrame");

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // two adding test
        try {
            builder.addMainPart("mainBody2");
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // parts adding test
        try {
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Vector3D doubleTranslation = new Vector3D(2.0, translation);

            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
            final Transform transform2 = new Transform(AbsoluteDate.J2000_EPOCH, doubleTranslation);

            builder.addPart(this.part2, this.mainBody, transform1);
            builder.addPart(this.part3, this.mainBody, transform1);
            builder.addPart(this.part4, this.part2, transform1);

            // double transformation check
            final IPart mainPart = builder.getPart(this.mainBody);
            final Frame mainFrame = mainPart.getFrame();
            final IPart fourthPart = builder.getPart(this.part4);
            final Frame fourthFrame = fourthPart.getFrame();

            final Transform mainToFourth = mainFrame.getTransformTo(fourthFrame, this.date);

            Assert.assertTrue(checkEqualTransform(transform2, mainToFourth));

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        }

        // twice part adding test : part 2
        try {
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
            builder.addPart(this.part2, this.mainBody, transform1);
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // parts removing test
        try {
            // the removing of the part 2 also removes its children : part 4
            builder.removePart(this.part2);

            // test : we can't access to the part 2 or 4
            try {
                builder.getPart(this.part2);
            } catch (final IllegalArgumentException e) {
                // expected !
            }
            try {
                builder.getPart(this.part4);
            } catch (final IllegalArgumentException e) {
                // expected !
            }

            // the removing of the main part also removes the remaining part 3
            builder.removePart(this.mainBody);

            // test : we can't access to the part 3 or main part
            try {
                builder.getPart(this.mainBody);
            } catch (final IllegalArgumentException e) {
                // expected !
            }
            try {
                builder.getPart(this.part3);
            } catch (final IllegalArgumentException e) {
                // expected !
            }

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_BUILDER}
     * 
     * @testedMethod {@link AssemblyBuilder#addProperty(IPartProperty, String)}
     * @testedMethod {@link AssemblyBuilder#returnAssembly()}
     * 
     * @description Creation of an assembly with one main part. Adding of properties to this part.
     * 
     * @input A mass property
     * 
     * @output An assembly
     * 
     * @testPassCriteria The property is correctly added, and can't be added twice.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void propertiesAddingTest() {

        Utils.clear();

        // building the assembly
        AssemblyBuilder builder = new AssemblyBuilder();

        // main part property adding test
        try {
            // add main part
            builder.addMainPart(this.mainBody);
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
            builder.addPart(this.part2, this.mainBody, transform1);

            // adding a geometric property
            // - first part is a sphere
            // - second part is a cone
            final SolidShape sphere = new Sphere(Vector3D.ZERO, 1);
            final IPartProperty shapeProp = new GeometricProperty(sphere);
            builder.addProperty(shapeProp, this.mainBody);
            final SolidShape cone = new EllipticCone(Vector3D.ZERO, translation, translation.orthogonal(),
                MathUtils.DEG_TO_RAD * 30, MathUtils.DEG_TO_RAD * 20, 1.5);
            final IPartProperty shapeProp2 = new GeometricProperty(cone);
            builder.addProperty(shapeProp2, this.part2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // property value test
        final IPart mainPart = assembly.getPart(this.mainBody);
        final IPartProperty prop = mainPart.getProperty(PropertyType.GEOMETRY);
        final SolidShape mainPartShape = ((GeometricProperty) prop).getShape();
        Assert.assertTrue(Sphere.class.isInstance(mainPartShape));

        final IPart partNumber2 = assembly.getPart(this.part2);
        final IPartProperty prop2 = partNumber2.getProperty(PropertyType.GEOMETRY);
        final SolidShape part2Shape = ((GeometricProperty) prop2).getShape();
        Assert.assertTrue(EllipticCone.class.isInstance(part2Shape));

        // recreation of a builder with this assembly
        builder = new AssemblyBuilder(assembly);

        final SolidShape otherShape = new RectangleCone(Vector3D.ZERO, Vector3D.MINUS_I, Vector3D.PLUS_J, 2, 3, 4);

        // twice the same property adding test
        try {
            // adding of a second simple mass property

            final IPartProperty massProp = new GeometricProperty(otherShape);
            builder.addProperty(massProp, this.mainBody);
            Assert.fail();

        } catch (final IllegalArgumentException e) {
            // expected !
        }
        try {
            // adding of a second simple mass property
            final IPartProperty massProp = new GeometricProperty(otherShape);
            builder.addProperty(massProp, this.part2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_BUILDER}
     * 
     * @testedMethod {@link AssemblyBuilder#setMainPartFrame(Frame)}
     * @testedMethod {@link AssemblyBuilder#setMainPartPV(SpacecraftState, Frame)}
     * 
     * @description Creation of an assembly with one main part. Test of the link to the OREKIT
     *              tree of frames.
     * 
     * @input A frame / a SpacecraftState
     * 
     * @output An assembly
     * 
     * @testPassCriteria The transformation between the assembly's main frame and the OREKIT frame
     *                   can be computed only if the trees are linked. When it's done, the transformations are the
     *                   expected
     *                   ones. When the assembly main frame is unknown and can't be linked to an Orekit frame, a
     *                   NullPointerException
     *                   is thrown.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void mainFrameDefinitionTest() throws PatriusException {

        Utils.clear();

        // building the assembly
        AssemblyBuilder builder = new AssemblyBuilder();

        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add second part
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
            builder.addPart(this.part2, this.mainBody, transform1);

            final Rotation rot = new Rotation(Vector3D.PLUS_K, FastMath.PI / 2.);
            builder.addPart(this.part3, this.mainBody, translation, rot);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        Assembly assembly = builder.returnAssembly();
        MainPart mainPart = (MainPart) assembly.getPart(this.mainBody);
        Assert.assertTrue(!mainPart.isLinkedToOrekitTree());
        IPart part = assembly.getPart(this.part2);
        Frame partFrame = part.getFrame();

        // test of the part 3 positionning
        final Frame part3Frame = assembly.getPart(this.part3).getFrame();
        final Frame mainPartFrame = assembly.getPart(this.mainBody).getFrame();
        final Transform t = mainPartFrame.getTransformTo(part3Frame, this.date);
        final Vector3D posInMainPartFrame = new Vector3D(0., 5., 0.);
        final Vector3D resPosInPart3Frame = t.transformPosition(posInMainPartFrame);

        Assert.assertEquals(4., resPosInPart3Frame.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(2., resPosInPart3Frame.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-5., resPosInPart3Frame.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // try to create the link to the GCRF frame : shall fail
        final Frame GCRF = FramesFactory.getGCRF();
        try {
            GCRF.getTransformTo(partFrame, this.date);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
        }

        // link to the main tree of frames
        builder = new AssemblyBuilder(assembly);
        final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
        final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);
        final UpdatableFrame newMainFrame = new UpdatableFrame(GCRF, transform1, "newMainFrame");

        try {
            builder.initMainPartFrame(newMainFrame);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // test of the link
        assembly = builder.returnAssembly();
        mainPart = (MainPart) assembly.getPart(this.mainBody);
        Assert.assertTrue(mainPart.isLinkedToOrekitTree());

        part = assembly.getPart(this.part2);
        partFrame = part.getFrame();

        final Vector3D translation2 = new Vector3D(4.0, 2.0, 10.0);
        final Transform transform2 = new Transform(AbsoluteDate.J2000_EPOCH, translation2);

        try {
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(checkEqualTransform(trans, transform2));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // set PV test
        final Rotation rot = Rotation.IDENTITY;
        final double mu = Utils.mu;
        final Vector3D position = new Vector3D(16.0, 10000.0, 0.0);
        final PVCoordinates pvAssembly = new PVCoordinates(position, Vector3D.ZERO);
        final Attitude att = new Attitude(this.date, GCRF, rot, Vector3D.ZERO);
        final Orbit orbit1 = new CartesianOrbit(pvAssembly, GCRF, this.date, mu);
        final SpacecraftState state = new SpacecraftState(orbit1, att);

        builder = new AssemblyBuilder(assembly);
        try {
            builder.initMainPartFrame(state);
        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test of the transformation
        final Transform transform3 = new Transform(AbsoluteDate.J2000_EPOCH, translation.add(position));
        assembly = builder.returnAssembly();
        part = assembly.getPart(this.part2);
        partFrame = part.getFrame();

        try {
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(checkEqualTransform(trans, transform3));

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * Compares two Transform objects
     * 
     * @param t1
     *        the first transformation
     * @param t2
     *        the second transformation
     * @return true if the transformations are equal
     */
    public static boolean checkEqualTransform(final Transform t1, final Transform t2) {

        boolean result = true;
        result = result & Precision.equals(t1.getRotation().applyTo(t2.getRotation().revert()).getAngle(), 0, 1);
        result = result & Precision.equals(t1.getRotationRate().subtract(t2.getRotationRate()).getNormSq(), 0, 1);
        result = result & Precision.equals(t1.getTranslation().subtract(t2.getTranslation()).getNormSq(), 0, 1);
        result = result & Precision.equals(t1.getVelocity().subtract(t2.getVelocity()).getNormSq(), 0, 1);

        return result;
    }

}
