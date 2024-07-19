/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.5:DM:DM-2367:27/05/2020:Configuration de changement de repère simplifiee 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.frames.configuration;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModel;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Coverage tests.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: FramesConfigurationImplementationTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class FramesConfigurationImplementationTest {

    @Test
    public void testGetTimeIntervalOfValidity() throws IllegalArgumentException {

        Utils.setDataRoot("regular-data");

        final FramesConfiguration conf = FramesConfigurationFactory.getIERS2010Configuration();
        final AbsoluteDateInterval interval = conf.getTimeIntervalOfValidity();
        final AbsoluteDateInterval ref = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, conf.getEOPHistory()
            .getStartDate(),
            conf.getEOPHistory().getEndDate(), IntervalEndpointType.CLOSED);

        Assert.assertEquals(0, interval.compareTo(ref));
    }

    @Test
    public void testGetEOPInterpolationMethod() {
        Utils.setDataRoot("regular-data");

        final FramesConfiguration conf = FramesConfigurationFactory.getIERS2010Configuration();
        final EOPInterpolators res = conf.getEOPInterpolationMethod();
        final EOPInterpolators ref = conf.getEOPHistory().getEOPInterpolationMethod();

        // Gratuitous call, for code coverage only
        final PrecessionNutationModel model = conf.getPrecessionNutationModel().getPrecessionNutationModel();
        Assert.assertEquals(model, new PrecessionNutation(true,
            PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT).getPrecessionNutationModel());

        Assert.assertEquals(res, ref);

        final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
        final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;

        // Coverage of PolarMotion methods:
        final PolarMotion polarMotion = new PolarMotion(true, tides, lib, SPrimeModelFactory.SP_IERS2003);
        Assert.assertEquals(lib, polarMotion.getLibrationCorrectionModel());
        Assert.assertEquals(tides, polarMotion.getTidalCorrectionModel());
        Assert.assertEquals(SPrimeModelFactory.SP_IERS2003, polarMotion.getSPrimeModel());
    }

    /**
     * Test simple frame configuration.
     */
    @Test
    public void testSimpleConfiguration() throws PatriusException {
        // Configuration with default EOP models
        final FramesConfiguration conf = FramesConfigurationFactory.getSimpleConfiguration(true);
        FramesFactory.setConfiguration(conf);
        // Check configuration
        Assert.assertEquals(PrecessionNutationModelFactory.PN_IERS2010_INTERPOLATED_NON_CONSTANT, conf.getPrecessionNutationModel().getPrecessionNutationModel());
        Assert.assertEquals(false, conf.getPrecessionNutationModel().useEopData());
        Assert.assertEquals(LibrationCorrectionModelFactory.NO_LIBRATION, conf.getDiurnalRotationModel().getLibrationCorrectionModel());
        Assert.assertEquals(TidalCorrectionModelFactory.NO_TIDE, conf.getDiurnalRotationModel().getTidalCorrectionModel());
        Assert.assertEquals(SPrimeModelFactory.NO_SP, conf.getPolarMotionModel().getSPrimeModel());
        // TIRF = ITRF
        Assert.assertTrue(FramesFactory.getITRF().getTransformTo(FramesFactory.getTIRF(), AbsoluteDate.J2000_EPOCH).getRotation().isIdentity());

        // Configuration with no EOP models
        final FramesConfiguration conf2 = FramesConfigurationFactory.getSimpleConfiguration(false);
        FramesFactory.setConfiguration(conf2);
        // Check configuration
        Assert.assertEquals(PrecessionNutationModelFactory.NO_PN, conf2.getPrecessionNutationModel().getPrecessionNutationModel());
        // GCRF = CIRF
        Assert.assertTrue(FramesFactory.getGCRF().getTransformTo(FramesFactory.getCIRF(), AbsoluteDate.J2000_EPOCH).getRotation().isIdentity());
        // TIRF = ITRF
        Assert.assertTrue(FramesFactory.getITRF().getTransformTo(FramesFactory.getTIRF(), AbsoluteDate.J2000_EPOCH).getRotation().isIdentity());
    }
}
