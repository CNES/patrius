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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
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
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TODProviderAlternateConfigurationTest {

    @Test
    public void testAASReferenceLEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        final Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        final Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76
        final PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                new Vector3D(-4746.088567, 786.077222, 5531.931288));
        // MOD iau76
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                new Vector3D(-4746.262495, 786.014149, 5531.791025));
        // MOD iau76 w corr
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                new Vector3D(-4746.263052, 786.014045, 5531.790562));

        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76Wcorr), 1.8, 1.6e-3);
        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76), 2.5, 1.5e-3);
        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), 1.1e-3, 5.3e-5);
        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76Wcorr), 0.90615, 7.4e-4);

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

        final Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        final Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76
        final PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                new Vector3D(837.552338, -2957.524176, -0.928772));
        // MOD iau76
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                new Vector3D(837.708020, -2957.480118, -0.814275));
        // MOD iau76 w corr
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                new Vector3D(837.708020, -2957.480117, -0.814253));

        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76Wcorr), 1.4, 8.1e-4);
        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76Wcorr), 4.5, 7.2e-5);

        this.checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), 5.2e-4, 6.4e-5);
        this.checkPV(pvTODiau76, tt.transformPVCoordinates(pvMODiau76), 3.5, 7.9e-4);

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("testpef-data");

        // Add EOP data
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(FramesConfigurationFactory.getIERS2010Configuration());
        builder.setEOPHistory(EOPHistoryFactory.getEOP1980History());
        FramesFactory.setConfiguration(builder.getConfiguration());
        FramesFactory.clear();
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
