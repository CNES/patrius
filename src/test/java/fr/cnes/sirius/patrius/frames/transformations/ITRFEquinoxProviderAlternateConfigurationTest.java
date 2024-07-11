/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ITRFEquinoxProviderAlternateConfigurationTest {

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        // ITRF
        final PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(-1033479.3830, 7901295.2754, 6380356.5958),
                new Vector3D(-3225.636520, -2872.451450, 5531.924446));

        // GTOD
        final PVCoordinates pvGTOD =
            new PVCoordinates(new Vector3D(-1033475.0313, 7901305.5856, 6380344.5328),
                new Vector3D(-3225.632747, -2872.442511, 5531.931288));

        final Transform t = FramesFactory.getGTOD(true).getTransformTo(FramesFactory.getITRFEquinox(), t0);
        this.checkPV(pvITRF, t.transformPVCoordinates(pvGTOD), 5.6e-5, 3.7e-7);

    }

    @Test
    public void testAASReferenceGEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final Transform t = FramesFactory.getGTOD(true).getTransformTo(FramesFactory.getITRFEquinox(), t0);

        // GTOD
        final PVCoordinates pvGTOD =
            new PVCoordinates(new Vector3D(24796919.2956, -34115870.9001, 10293.2583),
                new Vector3D(-0.979178, -1.476540, -0.928772));

        // ITRF
        final PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(24796919.2915, -34115870.9234, 10226.0621),
                new Vector3D(-0.979178, -1.476538, -0.928776));

        this.checkPV(pvITRF, t.transformPVCoordinates(pvGTOD), 0.028, 4.7e-7);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("testpef-data");

        // Add EOP data
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(FramesFactory.getConfiguration());
        builder.setEOPHistory(EOPHistoryFactory.getEOP1980History());
        FramesFactory.setConfiguration(builder.getConfiguration());
    }

    private void checkPV(final PVCoordinates reference,
                         final PVCoordinates result, final double positionThreshold,
                         final double velocityThreshold) {

        final Vector3D dP = result.getPosition().subtract(reference.getPosition());
        final Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

}
