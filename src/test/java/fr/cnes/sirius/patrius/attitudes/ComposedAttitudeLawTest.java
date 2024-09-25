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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Coverage tests for the composed attitude provider.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ComposedAttitudeLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class ComposedAttitudeLawTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Composed attitude
         * 
         * @featureDescription Composition of an attitude law and one or several orientation laws.
         * 
         * @coveredRequirements DV-ATT_80, DV-REPERES_60
         */
        COMPOSED_ATTITUDE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Reference frame. */
    private final Frame refFrame = FramesFactory.getGCRF();

    /** reference date */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ComposedAttitudeLawTest.class.getSimpleName(), "Composed attitude provider");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COMPOSED_ATTITUDE}
     * 
     * @testedMethod {@link ComposedAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link ComposedAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * 
     * @description creation of a composed attitude law.
     * 
     * @input one BasicAttitudeProvider (returns a constant attitude) and two BasicOrientationLaw (applies a constant
     *        rotation)
     * 
     * @output composed attitude law
     * 
     * @testPassCriteria the attitude is the expected one (analytical result)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void composedAttitudeTest() {

        Report.printMethodHeader("composedAttitudeTest", "Attitude computation", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // basic attitude law creation
        final Rotation pAttitude = new Rotation(Vector3D.PLUS_K, FastMath.PI / 2.0);
        final Attitude inAttitude = new Attitude(this.date, this.refFrame, pAttitude, Vector3D.ZERO);
        final AttitudeLaw attitudeLaw = new BasicAttitudeProvider(inAttitude);

        // first basic orientation law creation
        final Rotation orientationRotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 8.0);
        final IOrientationLaw orientationLaw1 = new BasicOrientationLaw(orientationRotation, this.refFrame);

        // second basic orientation law creation
        final Rotation orientationRotation2 = new Rotation(Vector3D.PLUS_K, FastMath.PI / 4.0);
        final IOrientationLaw orientationLaw2 = new BasicOrientationLaw(orientationRotation2, this.refFrame);

        // pv coordinates creation
        final Vector3D position = new Vector3D(8.0, 5.0, -4.0);
        final Vector3D velocity = new Vector3D(7.0, -3.0, 2.0);
        final PVCoordinates inCoordinates = new PVCoordinates(position, velocity);
        final PVCoordinatesProvider pv = new BasicPVCoordinatesProvider(inCoordinates, this.refFrame);

        // list creation
        final LinkedList<IOrientationLaw> modifiers = new LinkedList<>();

        modifiers.add(orientationLaw1);
        modifiers.add(orientationLaw2);

        try {

            // composition creation
            final ComposedAttitudeLaw composedAtt = new ComposedAttitudeLaw(attitudeLaw, modifiers);

            // first attitude method getting
            final Attitude att = composedAtt.getAttitude(pv, this.date, this.refFrame);

            Rotation outputRot = att.getRotation();
            final double expectedAngle = FastMath.PI / 4.0 + FastMath.PI / 8.0 + FastMath.PI / 2.0;
            Assert.assertEquals(expectedAngle, outputRot.getAngle(), this.comparisonEpsilon);
            Report.printToReport("Angle", expectedAngle, outputRot.getAngle());

            outputRot = att.getRotation();
            Assert.assertEquals(expectedAngle, outputRot.getAngle(), this.comparisonEpsilon);
            Assert.assertEquals(0.0, att.getSpin().getNorm(), this.comparisonEpsilon);

            // main law getting
            final AttitudeProvider outputLaw = composedAtt.getUnderlyingAttitudeLaw();
            final Attitude mainAtt = outputLaw.getAttitude(pv, this.date, this.refFrame);
            final double outputMainAngle = mainAtt.getRotation().getAngle();
            Assert.assertEquals(FastMath.PI / 2.0, outputMainAngle, this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * Test impact of Delta-T on spin computation.
     */
    @Test
    public void testDeltaT() throws PatriusException {


        // basic attitude law creation
        final AttitudeLaw attitudeLaw = new BodyCenterPointing();

        // first basic orientation law creation
        final Rotation orientationRotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 8.0);
        final IOrientationLaw orientationLaw1 = new BasicOrientationLaw(orientationRotation, this.refFrame);

        // second basic orientation law creation
        final Rotation orientationRotation2 = new Rotation(Vector3D.PLUS_K, FastMath.PI / 4.0);
        final IOrientationLaw orientationLaw2 = new BasicOrientationLaw(orientationRotation2, this.refFrame);

        // list creation
        final LinkedList<IOrientationLaw> modifiers = new LinkedList<>();

        modifiers.add(orientationLaw1);
        modifiers.add(orientationLaw2);

        // composition creation
        final ComposedAttitudeLaw law1 = new ComposedAttitudeLaw(attitudeLaw, modifiers);
        final ComposedAttitudeLaw law2 = new ComposedAttitudeLaw(attitudeLaw, modifiers, 0.5);

        // Get rotation
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, 0.5, 0.6, 0.7, 0.8, PositionAngle.TRUE, FramesFactory.getGCRF(),
            this.date, Constants.CNES_STELA_MU);
        final Attitude attitude1 = law1.getAttitude(orbit, this.date, FramesFactory.getGCRF());
        final Attitude attitude2 = law2.getAttitude(orbit, this.date, FramesFactory.getGCRF());
        
        // Check delta-t has been taken into account
        Assert.assertTrue(Vector3D.distance(attitude1.getSpin(), attitude2.getSpin()) > 1E-15);
    }
}
