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
 * @history Created 25/02/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::FA:412:05/05/2015:Changed IParamDiffFunction into Parameter in RadiativeProperty
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link StelaSpacecraftFactory}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public class StelaSpacecraftFactoryTest {

    /** threshold */
    private static final double EPS = Precision.EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela spacecraft factory test
         * 
         * @featureDescription tests the correctness of the Stela spacecraft factory
         * 
         * @coveredRequirements
         */
        STELA_SC_FACTORY
    }

    /**
     * @throws IllegalAccessException
     *         if fails
     * @throws InstantiationException
     *         if fails
     * @throws IllegalArgumentException
     *         if fails
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_SC_FACTORY}
     * 
     * @testedMethod {@link StelaSpacecraftFactory#createStelaCompatibleSpacecraft(String, double, double, double, double, double)}
     * 
     * @description tests the spacecraft creation method
     * 
     * @input aero and radiative properties
     * 
     * @output assembly
     * 
     * @testPassCriteria the property values are the same as the expected ones, the threshold is the largest
     *                   double-precision floating-point number such that 1 + EPSILON is numerically equal to 1. This
     *                   value is an upper bound on the relative error due to rounding real numbers to double precision
     *                   floating-point numbers.
     * 
     * 
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void test() throws PatriusException {

        // tests
        final Assembly sc = StelaSpacecraftFactory.createStelaCompatibleSpacecraft("main", 1, 2, 3, 4, 5);
        final CircularOrbit orbit = new CircularOrbit(8080000, .0, .0, MathLib.toRadians(20), .0, .0,
            PositionAngle.MEAN,
            FramesFactory.getEME2000(), new AbsoluteDate(), Constants.EGM96_EARTH_MU);
        new SpacecraftState(orbit);
        Assert.assertEquals(
            ((AeroSphereProperty) sc.getMainPart().getProperty(PropertyType.AERO_CROSS_SECTION)).getSphereRadius(),
            MathLib.sqrt(2 / FastMath.PI), EPS);

        Assert.assertEquals(
            ((RadiativeProperty) sc.getMainPart().getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio()
                .getValue(),
            (5 - 1) * 9 / 4., EPS);
        Assert.assertEquals(
            ((RadiativeSphereProperty) sc.getMainPart().getProperty(PropertyType.RADIATIVE_CROSS_SECTION))
                .getSphereRadius(), MathLib.sqrt(4 / FastMath.PI), EPS);

    }
}
