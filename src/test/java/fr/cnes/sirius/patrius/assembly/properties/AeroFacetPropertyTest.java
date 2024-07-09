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
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the AeroFacetProperty class.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AeroFacetPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Aero facet property
         * 
         * @featureDescription Aerodynamic property for a facet in an assembly part
         * 
         * @coveredRequirements DV-VEHICULE_111, DV-VEHICULE_410, DV-VEHICULE_420, DV-VEHICULE_450, DV-VEHICULE_460
         */
        AERO_FACET_PROPERTY
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_FACET_PROPERTY}
     * 
     * @testedMethod {@link AeroFacetProperty#AeroFacetProperty(Facet)}
     * @testedMethod {@link AeroFacetProperty#getFacet()}
     * @testedMethod {@link AeroFacetProperty#getType()}
     * 
     * @description Test for all class methods.
     * 
     * @input A facet as a constructor parameter.
     * 
     * @output An AeroFacetProperty instance.
     * 
     * @testPassCriteria The instance exists and returns the expected values.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public final void testAeroFacetProperty() throws PatriusException {
        final Facet expectedFacet = new Facet(Vector3D.PLUS_I, 6.55957);
        final AeroFacetProperty facetProp = new AeroFacetProperty(expectedFacet);
        // Members of the instance must have the expected values.
        Assert.assertEquals(expectedFacet, facetProp.getFacet());
        Assert.assertEquals(PropertyType.AERO_FACET, facetProp.getType());

        // IParamDiffFucntion for C_N and C_T
        final Parameter an = new Parameter("an", -1);
        final Parameter bn = new Parameter("bn", 1);
        final Parameter bt = new Parameter("bt", -2);

        final AbsoluteDate t0 = new AbsoluteDate();

        final LinearFunction cn = new LinearFunction(t0, bn, an);
        final LinearFunction ct = new LinearFunction(t0, bt, an);

        final AeroFacetProperty facetPropDiff = new AeroFacetProperty(expectedFacet, cn, ct);
        final ArrayList<Parameter> paramList = facetPropDiff.getParameters();
        for (int i = 0; i < paramList.size(); i++) {
            final Parameter param = paramList.get(i);
            if (an.getName() == param.getName()) {
                Assert.assertEquals(an, param);
            }
        }

        an.setValue(0);
        final Parameter p = new Parameter("an", 0);
        for (int i = 0; i < paramList.size(); i++) {
            final Parameter param = paramList.get(i);
            if (an.getName() == param.getName()) {
                Assert.assertNotSame(an, p);
            }
        }

        final AbsoluteDate t = t0.shiftedBy(15.0);
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), t, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);
        an.setValue(-1 / 15.0);
        final double value = facetPropDiff.getNormalCoef().value(state);
        Assert.assertEquals(value, 0.0, 1.0e-15);

        final double derivativeValueWRan = facetPropDiff.getNormalCoef().derivativeValue(an, state);
        Assert.assertEquals(derivativeValueWRan, 15.0, 1.0e-15);
        final double derivativeValueWRbn = facetPropDiff.getNormalCoef().derivativeValue(bn, state);
        final double un = 1.0;
        Assert.assertEquals(derivativeValueWRbn, un, 1.0e-15);
    }

}
