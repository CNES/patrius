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
 * @history creation 10/02/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:06/10/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:669:26/08/2016:remove exception from updateMainPartFrame method
 * VERSION::FA:1192:30/08/2017:update parts frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.attitudes.orientations.OrientationAngleProvider;
import fr.cnes.sirius.patrius.attitudes.orientations.OrientationAngleTransform;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.EllipticCone;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              - Test class for the assembly once built.
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
public class AssemblyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Assembly
         * 
         * @featureDescription Creation of an assembly from its parts, and some basic operations on it
         * 
         * @coveredRequirements DV-VEHICULE_10, DV-VEHICULE_40
         */
        ASSEMBLY,
        /**
         * @featureTitle Assembly parts
         * 
         * @featureDescription Basic operations on the assembly's parts
         * 
         * @coveredRequirements DV-VEHICULE_10, DV-VEHICULE_40
         */
        ASSEMBLY_PARTS,
        /**
         * @featureTitle Assembly frames
         * 
         * @featureDescription Creation of the assembly frames tree
         * 
         * @coveredRequirements DV-VEHICULE_200, DV-REPERES_100, DV-VEHICULE_340
         */
        ASSEMBLY_FRAMES
    }

    /**
     * Main part's name
     */
    private final String mainBody = "main_part";
    /**
     * 2nd part's name
     */
    private final String part2 = "part2";
    /**
     * 3rd part's name
     */
    private final String part3 = "part3";

    /**
     * J2000 date
     */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY}
     * 
     * @testedMethod {@link Assembly#getMainPart()}
     * @testedMethod {@link Assembly#getPart(String)}
     * @testedMethod {@link Assembly#getAllPartsNames()}
     * @testedMethod {@link Assembly#getParts()}
     * 
     * @description Creation of assembly and adding of parts. Test of the getting of the parts from the final assembly.
     * 
     * @input Part's names and features
     * 
     * @output The assembly containing all the parts
     * 
     * @testPassCriteria The parts have the right names and features
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void partsCheckTest() {

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);

            builder.addPart(this.part2, this.mainBody, transform1);
            builder.addPart(this.part3, this.mainBody, transform1);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // getting of the parts
        final IPart mainPart = assembly.getMainPart();
        final IPart partTwo = assembly.getPart(this.part2);
        final IPart partThree = assembly.getPart(this.part3);

        // features
        Assert.assertEquals(mainPart.getName(), this.mainBody);
        Assert.assertEquals(partThree.getName(), this.part3);
        Assert.assertEquals(partTwo.getName(), this.part2);

        // getting of all parts
        final Map<String, IPart> parts = assembly.getParts();
        Assert.assertEquals(parts.get(this.mainBody).getName(), this.mainBody);
        Assert.assertEquals(parts.get(this.part3).getName(), this.part3);

        // getting of all names
        final Set<String> names = assembly.getAllPartsNames();
        Assert.assertEquals(names.size(), 3);
        Assert.assertTrue(names.contains(this.mainBody));
        Assert.assertTrue(names.contains(this.part2));
        Assert.assertTrue(names.contains(this.part3));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_PARTS}
     * 
     * @testedMethod {@link Part#getProperty(PropertyType)}
     * @testedMethod {@link Part#hasProperty(PropertyType)}
     * @testedMethod {@link Part#addProperty(IPartProperty)}
     * @testedMethod {@link MainPart#getProperty(PropertyType)}
     * @testedMethod {@link MainPart#hasProperty(PropertyType)}
     * @testedMethod {@link MainPart#addProperty(IPartProperty)}
     * 
     * @description Creation of assembly and adding of parts and properties. Test of the properties of each part.
     * 
     * @input Assembly
     * 
     * @output Parts containing properties
     * 
     * @testPassCriteria The parts contain the right properties
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void partsProperiesTest() {
        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);

            builder.addPart(this.part2, this.mainBody, transform1);
            builder.addPart(this.part3, this.mainBody, transform1);

            // adding of a simple mass property
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

        // getting of the parts
        final IPart mainPart = assembly.getMainPart();
        final IPart partTwo = assembly.getPart(this.part2);
        final IPart partThree = assembly.getPart(this.part3);

        // main part and part 2 have the mass property, but not part 3
        Assert.assertFalse(mainPart.hasProperty(PropertyType.MASS));
        Assert.assertFalse(partTwo.hasProperty(PropertyType.MASS));
        Assert.assertTrue(mainPart.hasProperty(PropertyType.GEOMETRY));
        Assert.assertTrue(partTwo.hasProperty(PropertyType.GEOMETRY));
        Assert.assertFalse(partThree.hasProperty(PropertyType.GEOMETRY));
        Assert.assertFalse(partThree.hasProperty(PropertyType.MASS));

        // getting of the properies
        final IPartProperty prop1 = mainPart.getProperty(PropertyType.GEOMETRY);
        final IPartProperty prop2 = partTwo.getProperty(PropertyType.GEOMETRY);

        Assert.assertNull(mainPart.getProperty(PropertyType.DRAG));
        Assert.assertNull(partTwo.getProperty(PropertyType.DRAG));
        Assert.assertNull(partThree.getProperty(PropertyType.DRAG));

        final SolidShape mainPartShape = ((GeometricProperty) prop1).getShape();
        Assert.assertTrue(Sphere.class.isInstance(mainPartShape));

        final SolidShape part2Shape = ((GeometricProperty) prop2).getShape();
        Assert.assertTrue(EllipticCone.class.isInstance(part2Shape));
    }

    /**
     * @throws PatriusException
     *         if no attitude is defined
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_FRAMES}
     * 
     * @testedMethod {@link Part#getFrame()}
     * @testedMethod {@link Part#getParent()}
     * @testedMethod {@link MainPart#getFrame()}
     * @testedMethod {@link MainPart#getParent()}
     * @testedMethod {@link MainPart#updateFrame()}
     * 
     * @description Creation of assembly. Test of the local tree of frames and link to the main tree with the "updatePV"
     *              methods
     * 
     * @input an assembly, the GCRF frame
     * 
     * @output The assembly correctly linked to the tree of frames, and its parts correctly defined from each other
     * 
     * @testPassCriteria The frames are defined as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void partsTreeOfFramesTest() throws PatriusException {

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
        final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            builder.addPart(this.part2, this.mainBody, transform1);
            builder.addPart(this.part3, this.part2, transform1);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        final IPart mainPart = assembly.getMainPart();
        final IPart partThree = assembly.getPart(this.part3);
        final IPart partTwo = partThree.getParent();
        Assert.assertEquals(this.part2, partTwo.getName());

        Assert.assertNull(mainPart.getParent());

        // set PV test
        final Frame GCRF = FramesFactory.getGCRF();
        final Rotation rot = Rotation.IDENTITY;
        final double mu = Utils.mu;
        Vector3D position = new Vector3D(-16.0, -10000.0, 0.0);
        PVCoordinates pvAssembly = new PVCoordinates(position, Vector3D.ZERO);
        Attitude att = new Attitude(this.date, GCRF, rot, Vector3D.ZERO);
        Orbit orbit1 = new CartesianOrbit(pvAssembly, GCRF, this.date, mu);
        SpacecraftState state = new SpacecraftState(orbit1, att);

        // upadte main part without frame : shall fail
        try {
            assembly.updateMainPartFrame(state);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // update main part without frame : shall fail
        try {
            mainPart.updateFrame(transform1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        final Transform trans1 = new Transform(this.date, pvAssembly);
        // set without frame : shall fail
        try {
            assembly.updateMainPartFrame(trans1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        // set with frame
        final String mainFrameName = assembly.getMainPart().getFrame().getName();
        final String part2FrameName = assembly.getPart(this.part2).getFrame().getName();
        final String part3FrameName = assembly.getPart(this.part3).getFrame().getName();
        assembly.initMainPartFrame(state);
        // test the main part frame name has not changed:
        final String mainFrameNewName = assembly.getMainPart().getFrame().getName();
        Assert.assertEquals(mainFrameName, mainFrameNewName);
        // test the parts frame names have not changed:
        final String part2FrameNewName = assembly.getPart(this.part2).getFrame().getName();
        final String part3FrameNewName = assembly.getPart(this.part3).getFrame().getName();
        Assert.assertEquals(part2FrameName, part2FrameNewName);
        Assert.assertEquals(part3FrameName, part3FrameNewName);

        // test of the transformation
        Transform transform3 = new Transform(AbsoluteDate.J2000_EPOCH, translation.add(position));
        IPart part = assembly.getPart(this.part2);
        Frame partFrame = part.getFrame();

        try {
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(AssemblyBuilderTest.checkEqualTransform(trans, transform3));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // with same frame test
        position = new Vector3D(576.0, 4500.0, 230.0);
        pvAssembly = new PVCoordinates(position, Vector3D.ZERO);
        orbit1 = new CartesianOrbit(pvAssembly, GCRF, this.date, mu);
        state = new SpacecraftState(orbit1, att);

        try {
            assembly.updateMainPartFrame(state);
        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test of the transformation
        transform3 = new Transform(AbsoluteDate.J2000_EPOCH, translation.add(position));
        part = assembly.getPart(this.part2);
        partFrame = part.getFrame();

        try {
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(AssemblyBuilderTest.checkEqualTransform(trans, transform3));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test with part frame update
        transform3 = new Transform(AbsoluteDate.J2000_EPOCH, translation.negate().add(position));
        try {
            part.updateFrame(transform1.getInverse());
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(AssemblyBuilderTest.checkEqualTransform(trans, transform3));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // Test with different frame

        // GCRF case (no transform)
        position = new Vector3D(576.0, 4500.0, 230.0);
        pvAssembly = new PVCoordinates(position, Vector3D.ZERO);
        orbit1 = new CartesianOrbit(pvAssembly, GCRF, this.date, mu);
        att = new Attitude(this.date, GCRF, rot, Vector3D.ZERO);
        state = new SpacecraftState(orbit1, att);
        assembly.updateMainPartFrame(state);
        final Transform t1 = assembly.getMainPart().getFrame().getTransformTo(FramesFactory.getGCRF(), this.date);

        // EME2000 case (EME2000 => GCRF transform)
        final Transform tDiff = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), this.date);
        orbit1 =
            new CartesianOrbit(tDiff.transformPVCoordinates(pvAssembly), FramesFactory.getEME2000(), this.date, mu);
        att = new Attitude(this.date, GCRF, rot, Vector3D.ZERO).withReferenceFrame(FramesFactory.getEME2000());
        state = new SpacecraftState(orbit1, att);
        assembly.updateMainPartFrame(state);
        final Transform t2 = assembly.getMainPart().getFrame().getTransformTo(FramesFactory.getGCRF(), this.date);

        // t1 and t2 should be equal
        Assert.assertEquals(0., Rotation.distance(t1.getRotation(), t2.getRotation()), 0.);

        // Test updateMainPartFrame(Transform)
        try {
            assembly.updateMainPartFrame(trans1);

            partFrame = assembly.getMainPart().getFrame();
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(AssemblyBuilderTest.checkEqualTransform(trans, trans1));
        } catch (final PatriusException e) {
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // test of a manual update of the main part's frame
        position = new Vector3D(-576.0, 4500.0, -230.0);
        transform3 = new Transform(AbsoluteDate.J2000_EPOCH, position);
        try {
            mainPart.updateFrame(transform3);

            partFrame = assembly.getMainPart().getFrame();
            final Transform trans = GCRF.getTransformTo(partFrame, this.date);
            Assert.assertTrue(AssemblyBuilderTest.checkEqualTransform(trans, transform3));
        } catch (final PatriusException e) {
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // to cover this useless case...
        mainPart.updateFrame(Transform.IDENTITY);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_FRAMES}
     * 
     * @testedMethod {@link Part#getFrame()}
     * @testedMethod {@link MainPart#getFrame()}
     * @testedMethod {@link Assembly#setAssemblyPV(SpacecraftState, Frame)}
     * 
     * @description Creation of assembly. Test the correctness of the transformations (with account of speed and
     *              rotation rates).
     * 
     * @input an assembly, the GCRF frame
     * 
     * @output A transformed PV, with speed and rotation rates effects.
     * 
     * @testPassCriteria The actual pvs are the same as the expected ones. The threshold is set to 1e-14 in relative
     *                   scale.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void transformationsBetweenFrames() throws PatriusException {

        final AbsoluteDate myDate = new AbsoluteDate(1950, 1, 1, 0, 0, 0, TimeScalesFactory.getTT());
        final Frame gcrf = FramesFactory.getGCRF();

        final PVCoordinates pv = new PVCoordinates(new Vector3D(7100e3, 0, 0), new Vector3D(0, 7000, 100));
        final Orbit orbit = new CartesianOrbit(pv, gcrf, myDate, Constants.EGM96_EARTH_MU);

        final LofOffset attProv = new LofOffset(gcrf, LOFType.TNW, RotationOrder.YZX, .1, .2, .3);
        final Attitude initialAttitude = attProv.getAttitude(orbit, myDate, gcrf);

        final Transform tT = new Transform(myDate, pv);
        final Transform tR = new Transform(myDate, initialAttitude.getOrientation());
        final Transform i2s = new Transform(myDate, tT, tR);

        final Transform tT1 = new Transform(myDate, new Vector3D(4, 0, 0));
        final Transform tR1 = new Transform(myDate, new Rotation(Vector3D.PLUS_J, .5));
        final Transform s2p = new Transform(myDate, tT1, tR1);

        final Transform i2p = new Transform(myDate, i2s, s2p);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("main");
        builder.addPart("body", "main", s2p);
        final Assembly assembly = builder.returnAssembly();
        assembly.initMainPartFrame(new SpacecraftState(orbit, initialAttitude));

        // test PVs
        final PVCoordinates toT = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(1, 2, 3));

        // analytical
        final PVCoordinates exp = i2p.transformPVCoordinates(toT);
        // assembly computed
        final PVCoordinates act = gcrf.getTransformTo(assembly.getPart("body").getFrame(), myDate)
            .transformPVCoordinates(toT);

        Assert.assertEquals(0, (exp.getPosition().getX() - act.getPosition().getX()) / act.getPosition().getX(),
            Precision.EPSILON);
        Assert.assertEquals(0, (exp.getPosition().getY() - act.getPosition().getY()) / act.getPosition().getY(),
            Precision.EPSILON);
        Assert.assertEquals(0, (exp.getPosition().getZ() - act.getPosition().getZ()) / act.getPosition().getZ(),
            Precision.EPSILON);
        Assert.assertEquals(0, (exp.getVelocity().getX() - act.getVelocity().getX()) / act.getVelocity().getX(),
            Precision.EPSILON);
        Assert.assertEquals(0, (exp.getVelocity().getY() - act.getVelocity().getY()) / act.getVelocity().getY(),
            Precision.EPSILON);
        Assert.assertEquals(0, (exp.getVelocity().getZ() - act.getVelocity().getZ()) / act.getVelocity().getZ(),
            Precision.EPSILON);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_FRAMES}
     * 
     * @testedMethod {@link MobilePart#updateFrame(SpacecraftState)}
     * 
     * @description Check that a mobile part is properly updated i.e.rotates according to provided law.
     * 
     * @input an assembly with one mobile part. This part has 1 degree of liberty and is rotating at constant rate Pi / 4 rad/s around z-axis. 
     * 
     * @output updated parts frame
     * 
     * @testPassCriteria part frame is properly oriented (i.e. has rotated of Pi / 4 after 1s)
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public final void mobilePartTest() throws PatriusException {

        // State
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
            initialDate, Constants.EGM96_EARTH_MU);
        final AttitudeLaw attitudeLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final Attitude attitude = attitudeLaw.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Transform provider
        final OrientationAngleProvider oap = new OrientationAngleProvider() {            
            @Override
            public Double getOrientationAngle(final PVCoordinatesProvider pvProv, final AbsoluteDate date) throws PatriusException {
                return date.durationFrom(initialDate) * FastMath.PI / 4.;
            }
        };
        final OrientationAngleTransform t = new OrientationAngleTransform(Transform.IDENTITY, Vector3D.PLUS_K, oap);
        
        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        final Rotation expectedRot0 = Rotation.IDENTITY;
        final Vector3D expectedRate0 = Vector3D.PLUS_K.scalarMultiply(FastMath.PI / 4.);
        builder.addPart("Solar panel", "Main", t);
        builder.initMainPartFrame(state);
        final Assembly assembly = builder.returnAssembly();

        // Check that transformation at initial date is provided transformation
        final AbsoluteDate date1 = initialDate;
        assembly.updateMainPartFrame(state);
        final Rotation rot1 = FramesFactory.getGCRF().getTransformTo(assembly.getPart("Solar panel").getFrame(), date1)
            .getRotation();
        final Vector3D rate1 = FramesFactory.getGCRF()
            .getTransformTo(assembly.getPart("Solar panel").getFrame(), date1).getRotationRate();
        Assert.assertEquals(expectedRot0.getAngle(), rot1.getAngle(), 0.);
        Assert.assertEquals(0., rot1.getAxis().subtract(expectedRot0.getAxis()).getNorm(), 0.);

        // Check that transformation at initial date + 5s is initial transformation with rotation around z of Pi / 4.
        // (1s at a rate of Pi / 4)
        final AbsoluteDate date2 = initialDate.shiftedBy(1.);
        assembly.updateMainPartFrame(state.shiftedBy(1.));
        final Rotation rot2 = FramesFactory.getGCRF().getTransformTo(assembly.getPart("Solar panel").getFrame(), date2)
            .getRotation();
        final Vector3D rate2 = FramesFactory.getGCRF()
            .getTransformTo(assembly.getPart("Solar panel").getFrame(), date2).getRotationRate();
        Assert.assertEquals(expectedRot0.getAngle(), rot2.getAngle(), FastMath.PI / 4.);
        Assert.assertEquals(0., rot2.getAxis().subtract(Vector3D.PLUS_K).getNorm(), 0.);
        
        // Other mobile parts tests (functional)
        try {
            assembly.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame"));
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
        
        final MobilePart mobilePart = (MobilePart) assembly.getPart("Solar panel");
        Assert.assertEquals(mobilePart.getTransformProvider(), t);
        Assert.assertNotNull(mobilePart.getFrame());
        try {
            mobilePart.updateFrame(Transform.IDENTITY);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
        
        // Check getters
        Assert.assertEquals(Transform.IDENTITY, t.getReference());
        Assert.assertEquals(Vector3D.PLUS_K, t.getAxis());
        Assert.assertEquals(oap, t.getOrientationAngleProvider());
    }
}
