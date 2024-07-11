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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:571:12/04/2017:Remove useless atmospheric height scale factor
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.PiecewiseFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the AeroSphereProperty class.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AeroSpherePropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Aero sphere property
         * 
         * @featureDescription Aerodynamic property for a sphere in an assembly part
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_420, DV-VEHICULE_450, DV-VEHICULE_460
         */
        AERO_SPHERE_PROPERTY
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_SPHERE_PROPERTY}
     * 
     * @testedMethod {@link AeroSphereProperty#AeroSphereProperty(double)}
     * @testedMethod {@link AeroSphereProperty#getSphereRadius()}
     * @testedMethod {@link AeroSphereProperty#getSphereArea()}
     * @testedMethod {@link AeroSphereProperty#getType()}
     * 
     * @description Test for all class methods.
     * 
     * @input A sphere as a constructor parameter.
     * 
     * @output An AeroSphereProperty instance.
     * 
     * @testPassCriteria The instance exists and returns the expected values.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testAeroSphereProperty() throws PatriusException {
        final double expectedRadius = 6.55957;
        final double expectedArea = FastMath.PI * expectedRadius * expectedRadius;
        final AeroSphereProperty sphereProp = new AeroSphereProperty(expectedRadius);
        // Members of the instance must have the expected values.
        Assert.assertEquals(expectedRadius, sphereProp.getSphereRadius(), 0.);
        Assert.assertEquals(expectedArea, sphereProp.getSphereArea(), 0.);
        Assert.assertEquals(PropertyType.AERO_CROSS_SECTION, sphereProp.getType());

        // linear piecewize Function for C_X
        final Parameter a1 = new Parameter("a1", -1);
        final Parameter b1 = new Parameter("b1", 1);
        final Parameter a2 = new Parameter("a2", -2);
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

        final PiecewiseFunction pwf = new PiecewiseFunction(listFct, listDate);

        final AeroSphereProperty spherePropDiff = new AeroSphereProperty(expectedRadius, pwf);

        a2.setValue(0);
        final ArrayList<Parameter> paramList = spherePropDiff.getParameters();
        final Parameter p = new Parameter("a2", 0);
        for (int i = 0; i < paramList.size(); i++) {
            final Parameter param = paramList.get(i);
            if (a2.getName() == param.getName()) {
                Assert.assertNotSame(a2, p);
            }
        }

        final AbsoluteDate t = t1.shiftedBy(16.0);
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), t, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        final double value = spherePropDiff.getDragForce().value(state);
        Assert.assertEquals(value, 0.0, 1.0e-15);

        final double derivativeValue = spherePropDiff.getDragForceDerivativeValue(a3, state);
        Assert.assertEquals(derivativeValue, 1.0, 1.0e-15);

        // testing other constructor
        final Vector3D relativeVelocity = new Vector3D(100.0, 2300.0, -20.0);

        new AeroSphereProperty(expectedRadius, 0., relativeVelocity);
        new AeroSphereProperty(expectedRadius, 0.);
        new AeroSphereProperty(expectedRadius, 0., relativeVelocity);

        // Check negative surface
        final AeroSphereProperty aeroSpherePropNegative = new AeroSphereProperty(new Parameter("", -5), 1.);
        Assert.assertEquals(-5, aeroSpherePropNegative.getSphereArea(), 0);
        try {
            aeroSpherePropNegative.getSphereRadius();
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }
}
