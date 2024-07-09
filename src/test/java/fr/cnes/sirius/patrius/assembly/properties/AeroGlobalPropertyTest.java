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
 * @history Created 24/11/2014
 *
 * HISTORY
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link AeroGlobalProperty} class for the aero drag and lift model
 * 
 * @author Francois Toussaint
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class AeroGlobalPropertyTest {

    /**
     * Doubles comparison epsilon
     */
    private static final double EPS = 1e-14;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Aero global property
         * 
         * @featureDescription Aerodynamic property for drag and lift model in an assembly part
         */
        AERO_GLOBAL_PROPERTY
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_GLOBAL_PROPERTY}
     * 
     * @testedMethod {@link AeroGlobalProperty#AeroGlobalProperty(double)}
     * @testedMethod {@link AeroGlobalProperty#getDragCoef()}
     * @testedMethod {@link AeroGlobalProperty#getLiftCoef()}
     * @testedMethod {@link AeroGlobalProperty#getSurface()}
     * @testedMethod {@link AeroGlobalProperty#getType()}
     * 
     * @description Test for all class methods.
     * 
     * @input Aero coef and surface as constructor functions.
     * 
     * @output An AeroSphereProperty instance.
     * 
     * @testPassCriteria The instance exists and returns the expected values.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testAeroSphereProperty() throws PatriusException {

        // test with first constructor
        final double dragCoef = 1.2;
        final double liftCoef = 1.3;
        final Parameter a1 = new Parameter("a1", 0.5);
        final Parameter b1 = new Parameter("b1", 1.);
        // surf1 = a1.t + b1
        final AbsoluteDate t1 = new AbsoluteDate();
        final double surf1 = 2.4;
        // first constructor
        // aero coef as doubles and surface as IParamDiffFunction
        final AeroGlobalProperty aeroGlobProp1 = new AeroGlobalProperty(dragCoef, liftCoef, new Sphere(Vector3D.ZERO,
            MathLib.sqrt(surf1 / FastMath.PI)));
        final AbsoluteDate t = t1.shiftedBy(10.);

        final Orbit orb = new KeplerianOrbit(6700000, .001, .0, .2, .3, .4, PositionAngle.TRUE,
            FramesFactory.getGCRF(), t, Constants.EGM96_EARTH_MU);
        final SpacecraftState st = new SpacecraftState(orb, new Attitude(t, FramesFactory.getGCRF(),
            AngularCoordinates.IDENTITY));

        final double value1 = aeroGlobProp1.getDragCoef().value(st);
        final double value2 = aeroGlobProp1.getLiftCoef().value(st);
        final double value3 = aeroGlobProp1.getCrossSection(Vector3D.PLUS_I);
        Assert.assertEquals(value1, 1.2, EPS);
        Assert.assertEquals(value2, 1.3, EPS);
        Assert.assertEquals(value3, surf1, EPS);

        Assert.assertEquals(0., aeroGlobProp1.getCrossSection(Vector3D.ZERO), 0.);

        // test with second constructor
        final double surf2 = 1.2;
        final Parameter a2 = new Parameter("a2", 1.);
        final Parameter b2 = new Parameter("b2", 2.);
        // dragCoef1 = a1.t + b1
        // liftCoef1 = a2.t + b2
        final LinearFunction dragCoef1 = new LinearFunction(t1, b1, a1);
        final LinearFunction liftCoef1 = new LinearFunction(t1, b2, a2);
        // second constructor
        // surf as double and aero coef as IParamDiffFunction
        final AeroGlobalProperty aeroGlobProp2 = new AeroGlobalProperty(dragCoef1, liftCoef1, new Sphere(Vector3D.ZERO,
            MathLib.sqrt(surf2 / FastMath.PI)));
        final double value4 = aeroGlobProp2.getDragCoef().value(st);
        final double value5 = aeroGlobProp2.getLiftCoef().value(st);
        final double value6 = aeroGlobProp2.getCrossSection(Vector3D.PLUS_I);
        Assert.assertEquals(value4, 6.0, EPS);
        Assert.assertEquals(value5, 12.0, EPS);
        Assert.assertEquals(value6, surf2, EPS);

        Assert.assertEquals(PropertyType.AERO_GLOBAL, aeroGlobProp2.getType());
    }

}
