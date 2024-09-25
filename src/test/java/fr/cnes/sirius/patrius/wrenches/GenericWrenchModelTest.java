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
 * @history Created 27/04/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:183:17/03/2014:Added test details to javadoc
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * test class for the generic wrench model.
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class GenericWrenchModelTest {

    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
    private SpacecraftState s;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Generic wrench model
         * 
         * @featureDescription Generic wrench model
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30
         */
        GENERIC_WRENCH,

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_WRENCH}
     * 
     * @testedMethod {@link GenericWrenchModel#computeWrench(SpacecraftState, Vector3D, Frame)}
     * @testedMethod {@link GenericWrenchModel#computeTorque(SpacecraftState, Vector3D, Frame)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input a force (1, 2, 3) N in the spacecraft frame, an application point (-1, 0, 0)
     * 
     * @output the resulting wrench at the centre of mass
     * 
     * @testPassCriteria the wrench computed is as expected, to 1e-14 on a relative scale for the norm
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     */
    @Test
    public void testComputeWrenchSpacecraftState() throws PatriusException {

        final ForceModel force = new ForceModel(){

            /** Serializable UID. */
            private static final long serialVersionUID = -5819978053860661530L;

            @Override
            public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
                return new Vector3D(1, 2, 3);
            }

            @Override
            public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
                throws PatriusException {
                // nothing to do
            }

            @Override
            public boolean supportsParameter(final Parameter param) {
                return false;
            }

            @Override
            public ArrayList<Parameter> getParameters() {
                return null;
            }

            @Override
            public EventDetector[] getEventsDetectors() {
                return null;
            }
            
            /** {@inheritDoc} */
            @Override
            public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
                // Nothing to do
            }
        };
        final Vector3D origin = Vector3D.MINUS_I;

        final GenericWrenchModel wrench = new GenericWrenchModel(force, origin);

        final Wrench act = wrench.computeWrench(this.s);

        checkV(Vector3D.ZERO, act.getOrigin(), this.eps);
        checkV(force.computeAcceleration(null), act.getForce(), this.eps);
        checkV(origin.crossProduct(force.computeAcceleration(null)), act.getTorque(), this.eps);

        final Vector3D tor = wrench.computeTorque(this.s);
        checkV(origin.crossProduct(force.computeAcceleration(null)), tor, this.eps);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_WRENCH}
     * 
     * @testedMethod {@link GenericWrenchModel#computeWrench(SpacecraftState, Vector3D, Frame)}
     * @testedMethod {@link GenericWrenchModel#computeTorque(SpacecraftState, Vector3D, Frame)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input a force (1, 2, 3) N in the spacecraft frame, an application point (-1, 0, 0)
     *        and a point where to express the wrench (0, 0, +1)
     * 
     * @output the resulting wrench in the desired origin
     * 
     * @testPassCriteria the wrench computed is as expected, to 1e-14 on a relative scale for the norm
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     */
    @Test
    public void testComputeWrenchSpacecraftStateVector3DFrame() throws PatriusException {

        // combination of the position and the attitude
        final Transform rotation = new Transform(AbsoluteDate.J2000_EPOCH, this.s.getAttitude()
            .getOrientation());
        final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, this.s.getOrbit().getPVCoordinates());
        final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, translation, rotation);

        // main part frame
        final Frame fr = new Frame(this.s.getFrame(), transform, "mainPartFrame");

        final ForceModel force = new ForceModel(){

            /** Serializable UID. */
            private static final long serialVersionUID = -2908291562978878023L;

            @Override
            public EventDetector[] getEventsDetectors() {
                return null;
            }

            @Override
            public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
                return new Vector3D(1, 2, 3);
            }

            @Override
            public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
                throws PatriusException {
                // nothing to do
            }

            @Override
            public ArrayList<Parameter> getParameters() {
                return null;
            }

            @Override
            public boolean supportsParameter(final Parameter param) {
                return false;
            }
            
            /** {@inheritDoc} */
            @Override
            public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
                // Nothing to do
            }
        };

        final Vector3D origin = Vector3D.MINUS_I;
        final Vector3D otherOrigin = Vector3D.PLUS_K;

        final GenericWrenchModel wrench = new GenericWrenchModel(force, origin);
        final Wrench act = wrench.computeWrench(this.s, otherOrigin, fr);

        checkV(otherOrigin, act.getOrigin(), this.eps);
        checkV(force.computeAcceleration(this.s), act.getForce(), this.eps);
        checkV(origin.crossProduct(force.computeAcceleration(this.s)).add(
            otherOrigin.negate().crossProduct(force.computeAcceleration(this.s))), act.getTorque(), this.eps);

        final Vector3D tor = wrench.computeTorque(this.s, otherOrigin, fr);
        checkV(origin.crossProduct(force.computeAcceleration(this.s)).add(
            otherOrigin.negate().crossProduct(force.computeAcceleration(this.s))), tor, this.eps);
    }

    @Before
    public void setup() {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // spacecraft
        this.s = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getGCRF(),
            Rotation.IDENTITY, Vector3D.ZERO));
    }

    private static void checkV(final Vector3D exp, final Vector3D act, final double e) {
        Assert.assertEquals(exp.getX(), act.getX(), e);
        Assert.assertEquals(exp.getY(), act.getY(), e);
        Assert.assertEquals(exp.getZ(), act.getZ(), e);
    }
}
