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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:571:12/04/2017:Remove useless atmospheric height scale factor
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.util.ArrayList;

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
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.PiecewiseFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the {@link AeroCrossSectionProperty} class.
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class AeroCrossSectionPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Aero cross section property
         * 
         * @featureDescription Aerodynamic property for a cylinder, parallelepiped or sphere : functional tests
         */
        AERO_CROSS_SECTION_PROPERTY
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_CROSS_SECTION_PROPERTY}
     * 
     * @testedMethod {@link AeroCrossSectionProperty#AeroCrossSectionProperty(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider)}
     * @testedMethod {@link AeroCrossSectionProperty#AeroCrossSectionProperty(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider, double)}
     * @testedMethod {@link AeroCrossSectionProperty#AeroCrossSectionProperty(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider, IParamDiffFunction)}
     * @testedMethod {@link AeroCrossSectionProperty#AeroCrossSectionProperty(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider, double, double)}
     * @testedMethod {@link AeroCrossSectionProperty#AeroCrossSectionProperty(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider, double, IParamDiffFunction)}
     * @testedMethod {@link AeroCrossSectionProperty#getAtmScaleHeight()}
     * @testedMethod {@link AeroCrossSectionProperty#getDragForce()}
     * @testedMethod {@link AeroCrossSectionProperty#getDragForceDerivativeValue(Parameter, SpacecraftState)}
     * @testedMethod {@link AeroCrossSectionProperty#getType()}
     * 
     * @description Test for all class methods except cross section.
     * 
     * @input A main shape as the property constructor
     * 
     * @output An AeroCrossSectionProperty instance.
     * 
     * @testPassCriteria The instance exists and returns the expected values.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testAeroCrossSectionProperty() throws PatriusException {
        // Use a cylinder
        final RightCircularCylinder shape = new RightCircularCylinder(Vector3D.ZERO, Vector3D.PLUS_K, 1.0, 4.0);

        // Define the property with default Cx
        final AeroCrossSectionProperty property = new AeroCrossSectionProperty(shape);

        // Value of Cx is default value 1.7
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        Assert.assertEquals(property.getDragForce().value(state), AeroCrossSectionProperty.DEFAULT_C_X, 0.);

        // Type property is the one expected
        Assert.assertTrue(property.getType() == PropertyType.AERO_CROSS_SECTION);

        // Define the property with Cx as double or parameter
        final AeroCrossSectionProperty property2 = new AeroCrossSectionProperty(shape,
            AeroCrossSectionProperty.DEFAULT_C_X);
        final AeroCrossSectionProperty property3 = new AeroCrossSectionProperty(shape, new Parameter("C_X",
            AeroCrossSectionProperty.DEFAULT_C_X));
        Assert.assertEquals(property2.getDragForce().value(state), AeroCrossSectionProperty.DEFAULT_C_X);
        Assert.assertEquals(property3.getDragForce().value(state), AeroCrossSectionProperty.DEFAULT_C_X);

        // Define the property with cx as a parameter : the latter is added to Parameterizable params array
        final Parameter cxParam = new Parameter("C_X", AeroCrossSectionProperty.DEFAULT_C_X);
        final AeroCrossSectionProperty propertyParam = new AeroCrossSectionProperty(shape, cxParam);
        Assert.assertTrue(propertyParam.supportsParameter(cxParam));

        // Define C_X as a linear piecewise function
        final Parameter a1 = new Parameter("a1", -1);
        final Parameter b1 = new Parameter("b1", 1);
        final Parameter a2 = new Parameter("a2", 0);
        final Parameter a3 = new Parameter("a3", -3);
        final Parameter b3 = new Parameter("b3", 3);

        // f1 = a1.t + b1
        final AbsoluteDate t1 = new AbsoluteDate();
        final LinearFunction f1 = new LinearFunction(t1, b1, a1);

        // f2 = a2
        final AbsoluteDate t2 = t1.shiftedBy(10.0);
        final ConstantFunction f2 = new ConstantFunction(a2);

        // f3 = a3.t + b3
        final AbsoluteDate t3 = t1.shiftedBy(15.0);
        final LinearFunction f3 = new LinearFunction(t3, b3, a3);

        final ArrayList<AbsoluteDate> listDate = new ArrayList<AbsoluteDate>();
        listDate.add(t1);
        listDate.add(t2);

        final ArrayList<IParamDiffFunction> listFct = new ArrayList<IParamDiffFunction>();
        listFct.add(f1);
        listFct.add(f2);
        listFct.add(f3);

        final PiecewiseFunction cx = new PiecewiseFunction(listFct, listDate);
        final AeroCrossSectionProperty property4 = new AeroCrossSectionProperty(shape, cx);

        // Expected C_X value and derivative value
        final AbsoluteDate t = t1.shiftedBy(16.0);
        final CircularOrbit orbit2 = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), t, Constants.EGM96_EARTH_MU);
        final SpacecraftState state2 = new SpacecraftState(orbit2);
        final double value = property4.getDragForce().value(state2);
        Assert.assertEquals(value, 0.0, 1.0e-15);

        final double derivativeValue = property4.getDragForceDerivativeValue(a3, state2);
        Assert.assertEquals(derivativeValue, 1.0, 1.0e-15);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_CROSS_SECTION_PROPERTY}
     * 
     * @testedMethod {@link AeroCrossSectionProperty#getCrossSection(SpacecraftState, fr.cnes.sirius.patrius.frames.Frame, fr.cnes.sirius.patrius.assembly.IPart, Vector3D)}
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
    public final void testCrossSectionComputation() throws PatriusException {
        // Use a cylinder
        final RightCircularCylinder cylinder = new RightCircularCylinder(new Vector3D(10, 20, 30), Vector3D.PLUS_K, 2,
            10.);

        // Define the property
        final AeroCrossSectionProperty property = new AeroCrossSectionProperty(cylinder);

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
        Assert.assertEquals(4. * FastMath.PI, property.getCrossSection(state, Vector3D.MINUS_I, mainFrame, partFrame),
            Precision.EPSILON);
        Assert.assertEquals(40., property.getCrossSection(state, Vector3D.PLUS_K, mainFrame, partFrame),
            Precision.EPSILON);

        // Check with null velocity
        Assert.assertEquals(00., property.getCrossSection(state, Vector3D.ZERO, mainFrame, partFrame),
            Precision.EPSILON);
    }
}
