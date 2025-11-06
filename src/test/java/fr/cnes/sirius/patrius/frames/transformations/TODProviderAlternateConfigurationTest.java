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
* VERSION:4.15:OPENFD-360:21/11/2024:[PATRIUS] Erreur de lecture des EOP 1980 C04
* VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
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

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TODProviderAlternateConfigurationTest {
    
    @Test
    public void testAASReferenceLEO() throws PatriusException {

        Report.printMethodHeader("testAASReferenceLEO", "Frame conversion", "Vallado paper", 1.e-4,
            ComparisonType.ABSOLUTE);

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // However, the article does not provide details for its conversion process, therefore our
        // conversion could lead to notable differences with the references of the article (roughly 2m for LEO case).
        // We choose Patrius results for TOD coordinates as references and we will use those to ensure non regression in the future.
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());

        final Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        final Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76 (obtained with Patrius)
        final PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(5094514.781242073, 6127366.460903622, 6380344.5323226815),
                new Vector3D(-4746.088523255131, 786.077215684502, 5531.93125928957));
        // TOD iau76 w corr (obtained with Patrius)
        final PVCoordinates pvTODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094516.202522456, 6127365.277913713, 6380344.5335636735),
                new Vector3D(-4746.088341436813, 786.0783159743551, 5531.931259119594));
        // MOD iau76 (from Vallado's paper)
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                new Vector3D(-4746.262495, 786.014149, 5531.791025));
        // MOD iau76 w corr (from Vallado's paper)
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                new Vector3D(-4746.263052, 786.014045, 5531.790562));
        
        // Usual thresholds for positions and velocities : 1e-14
        final double positionThresh = Precision.DOUBLE_COMPARISON_EPSILON; 
        final double velocitiesThresh = Precision.DOUBLE_COMPARISON_EPSILON; 
        
        checkPV(pvTODiau76Wcorr, tt.transformPVCoordinates(pvMODiau76Wcorr), positionThresh, velocitiesThresh);
        
        Report.printToReport("Position", pvTODiau76Wcorr.getPosition(), tt.transformPVCoordinates(pvMODiau76Wcorr).getPosition());
        Report.printToReport("Velocity", pvTODiau76Wcorr.getVelocity(), tt.transformPVCoordinates(pvMODiau76Wcorr).getVelocity());
        
        checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), positionThresh, velocitiesThresh);

        Report.printToReport("Position", pvTODiau76.getPosition(), tf.transformPVCoordinates(pvMODiau76).getPosition());
        Report.printToReport("Velocity", pvTODiau76.getVelocity(), tf.transformPVCoordinates(pvMODiau76).getVelocity());
    }
    
    @Test
    public void testAASReferenceGEO() throws PatriusException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // However, the article does not provide details for its conversion process, therefore our
        // conversion could lead to notable differences with the references of the article (roughly 10m for GEO case).
        // We choose Patrius results for TOD coordinates as references and we will use those to ensure non regression in the future.

        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final Transform tt = FramesFactory.getMOD(true).getTransformTo(FramesFactory.getTOD(true), t0);
        final Transform tf = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76 (obtained with Patrius)
        final PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(-4.057742774999117E7, -1.1500096130989186E7, 10293.258625969644),
                new Vector3D(837.5523543379746, -2957.52423416509, -0.9287538021113216));
        // TOD iau76 w corr (obtained with Patrius)
        final PVCoordinates pvTODiau76Wcorr =
            new PVCoordinates(new Vector3D(-4.0577430499285676E7, -1.1500086430230182E7, 10293.24873144946),
                new Vector3D(837.5516468163653, -2957.5244320444654, -0.9287590425415224));
        // MOD iau76 (from Vallado's paper)
        final PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                new Vector3D(837.708020, -2957.480118, -0.814275));
        // MOD iau76 w corr (from Vallado's paper)
        final PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                new Vector3D(837.708020, -2957.480117, -0.814253));
        
        // Usual thresholds for positions and velocities : 1e-14
        final double positionThresh = Precision.DOUBLE_COMPARISON_EPSILON; 
        final double velocitiesThresh = Precision.DOUBLE_COMPARISON_EPSILON; 

        checkPV(pvTODiau76Wcorr, tt.transformPVCoordinates(pvMODiau76Wcorr), positionThresh, velocitiesThresh);
        
        Report.printToReport("Position", pvTODiau76Wcorr.getPosition(), tt.transformPVCoordinates(pvMODiau76Wcorr).getPosition());
        Report.printToReport("Velocity", pvTODiau76Wcorr.getVelocity(), tt.transformPVCoordinates(pvMODiau76Wcorr).getVelocity());
        
        checkPV(pvTODiau76, tf.transformPVCoordinates(pvMODiau76), positionThresh, velocitiesThresh);
        
        Report.printToReport("Position", pvTODiau76.getPosition(), tf.transformPVCoordinates(pvMODiau76).getPosition());
        Report.printToReport("Velocity", pvTODiau76.getVelocity(), tf.transformPVCoordinates(pvMODiau76).getVelocity());
    }
    
    private static void checkPV(final PVCoordinates reference,
            final PVCoordinates result, final double positionThreshold,
            final double velocityThreshold) {

        Assert.assertEquals(reference.getPosition().getX(), result.getPosition().getX(), positionThreshold);
        Assert.assertEquals(reference.getPosition().getY(), result.getPosition().getY(), positionThreshold);
        Assert.assertEquals(reference.getPosition().getZ(), result.getPosition().getZ(), positionThreshold);
        Assert.assertEquals(reference.getVelocity().getX(), result.getVelocity().getX(), velocityThreshold);
        Assert.assertEquals(reference.getVelocity().getY(), result.getVelocity().getY(), velocityThreshold);
        Assert.assertEquals(reference.getVelocity().getZ(), result.getVelocity().getZ(), velocityThreshold);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.clear();
        Utils.setDataRoot("testpef-data");

        // Add EOP data
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder(FramesConfigurationFactory.getIERS2010Configuration());
        builder.setEOPHistory(EOPHistoryFactory.getEOP1980History());
        FramesFactory.setConfiguration(builder.getConfiguration());
        FramesFactory.clear();
    }
}
