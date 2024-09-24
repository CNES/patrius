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
 * @history creation 05/04/2017
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:remove TankProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Units tests for the {@link TankProperty} class.
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class TankPropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Tank property
         * 
         * @featureDescription Functional tests for the tank property
         */
        TANK_PROPERTY
    }

    /** Tank. */
    private static final String TANK = "Tank";

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TANK_PROPERTY}
     * 
     * @testedMethod {@link TankProperty#TankProperty(double)}
     * @testedMethod {@link TankProperty#getMassProperty()}
     * @testedMethod {@link TankProperty#getMass()}
     * @testedMethod {@link TankProperty#getPartName()}
     * @testedMethod {@link TankProperty#getParameters()}
     * @testedMethod {@link TankProperty#getType()}
     * 
     * @description Test for all class methods.
     * 
     * @input A name for tank property, a mass
     * 
     * @output A TankProperty instance.
     * 
     * @testPassCriteria The instance exists and returns the expected values.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testTankProperty() throws PatriusException {

        // Create the property : error if mass is negative
        double mass = -1000.;
        TankProperty tank;
        try {
            tank = new TankProperty(mass);
            Assert.fail();
        } catch (final PatriusException e) {
            // This must happen
            Assert.assertTrue(true);
        }

        mass = 1000.;
        tank = new TankProperty(mass);

        // Test getters : name and mass
        Assert.assertEquals(tank.getMass(), mass, 0.);
        Assert.assertEquals(tank.getMass(), tank.getMassProperty().getMass(), 0.);
        Assert.assertSame(tank.getPartName(), "");
        tank.setPartName(TANK);
        Assert.assertSame(tank.getPartName(), TANK);

        // The property has no parameters
        Assert.assertEquals(tank.getParameters().size(), 0, 0);

        // The property is of type TANK
        Assert.assertTrue(tank.getType() == PropertyType.TANK);

        // Test copy constructor
        final TankProperty tank2 = new TankProperty(tank);
        Assert.assertEquals(tank.getMass(), tank2.getMass(), 0);
        Assert.assertEquals(tank.getPartName(), tank2.getPartName());

        // Test serialization
        final TankProperty tank3 = TestUtils.serializeAndRecover(tank);
        Assert.assertEquals(tank.getMass(), tank3.getMass(), 0);
        Assert.assertEquals(tank.getPartName(), tank3.getPartName());

        Assert.assertNotNull(tank.toString());
    }
}
