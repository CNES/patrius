/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 18/10/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPV;

/**
 * Topocentric pv coordinates test class.
 * 
 * @author Julie Anton
 * 
 */
public class TopocentricPVTest {
    /** Features description. */
    public enum features {
        /**
         * @featureTitle Topocentric pv coordinates
         * 
         * @featureDescription Topocentric pv coordinates test
         * 
         * @coveredRequirements DV-COORD_140, DV-COORD_150
         */
        PV_COORDINATES_TOPOCENTRIC
    }

    /** Smallest positive number such that 1 - EPSILON is not numerically equal to 1. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PV_COORDINATES_TOPOCENTRIC}
     * 
     * @testedMethod {@link TopocentricPV#getAzimuth()}
     * @testedMethod {@link TopocentricPV#getAzimuthRate()}
     * @testedMethod {@link TopocentricPV#getElevation()}
     * @testedMethod {@link TopocentricPV#getElevationRate()}
     * @testedMethod {@link TopocentricPV#getRange()}
     * @testedMethod {@link TopocentricPV#getRangeRate()}
     * @testedMethod {@link TopocentricPV#getPosition()}
     * 
     * @description test the constructor and the assessors
     * 
     * @input TopocentricPV topo = (0.1,0.5,2.,1.3,1.2,0.3) : topo pv coordinates
     * 
     * @output the components
     * 
     * @testPassCriteria the elevation should be 0.1, the azimuth should be 0.5, the range should be 2.0, the elevation
     *                   rate should be 1.3, the azimuth rate should be 1.2 and the range rate should be 0.3 with an
     *                   epsilon of 1e-16 due to machine errors only (getters on values given at the construction)
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void constructorTest() {
        final TopocentricPV topo = new TopocentricPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);

        Assert.assertEquals(0.1, topo.getElevation(), this.machineEpsilon);
        Assert.assertEquals(0.5, topo.getAzimuth(), this.machineEpsilon);
        Assert.assertEquals(2., topo.getRange(), this.machineEpsilon);
        Assert.assertEquals(1.3, topo.getElevationRate(), this.machineEpsilon);
        Assert.assertEquals(1.2, topo.getAzimuthRate(), this.machineEpsilon);
        Assert.assertEquals(0.3, topo.getRangeRate(), this.machineEpsilon);

        Assert.assertEquals(0.1, topo.getPosition().getX(), this.machineEpsilon);
        Assert.assertEquals(0.5, topo.getPosition().getY(), this.machineEpsilon);
        Assert.assertEquals(2., topo.getPosition().getZ(), this.machineEpsilon);
        Assert.assertEquals(1.3, topo.getVelocity().getX(), this.machineEpsilon);
        Assert.assertEquals(1.2, topo.getVelocity().getY(), this.machineEpsilon);
        Assert.assertEquals(0.3, topo.getVelocity().getZ(), this.machineEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PV_COORDINATES_TOPOCENTRIC}
     * 
     * @testedMethod {@link TopocentricPV#toString()}
     * 
     * @description test the method toString()
     * 
     * @input TopocentricPV topo = (0.1,0.5,2.,1.3,1.2,0.3) : topo pv coordinates
     * 
     * @output the String representation of the coordinates
     * 
     * @testPassCriteria the String representation should be "{P(0.1, 0.5, 2.0), V(1.3, 1.2, 0.3)}"
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void toStringTest() {
        final TopocentricPV topo = new TopocentricPV(0.1, 0.5, 2., 1.3, 1.2, 0.3);

        Assert.assertEquals("{P(0.1, 0.5, 2.0), V(1.3, 1.2, 0.3)}", topo.toString());
    }
}
