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
 * @history created 09/11/2012
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.PolarMotion;
import fr.cnes.sirius.patrius.frames.configuration.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.IERS20032010PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationConvention;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModel;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class A1013PerfTest {

    /**
     * @param args
     * @throws PatriusException
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws PatriusException, InterruptedException {

        // Thread.sleep(10000);
        System.out.println("PERFTEST begin...");
        /*
         * Setup OREKIT ...
         * DataProviderManager configuration
         */
        PatriusData.configureDataManager();

        // WARNING : 2 ==> IERS2003 , 3 ==> IERS2010
        configureFrameTree(2);

        // final List<Vector3D> rezList = new ArrayList<Vector3D>();

        final long t0 = System.currentTimeMillis();
        final AbsoluteDate startDate = FramesFactory.getConfiguration().getEOPHistory().getStartDate(); // .shiftedBy(3
        // * 365 * 24 *
        // 60 * 60);
        for (int i = 0; i < 400000; i++) {
            // create a bogus position vector
            Vector3D bogusPos = new Vector3D(i * 123.456, i * -0.25, i * 33.12 * FastMath.PI);
            // convert it from ITRF2008 Frame to GCRF Frame
            final Transform t = FramesFactory.getITRF().getTransformTo(FramesFactory.getGCRF(),
                startDate.shiftedBy(3205 * i));
            t.transformPosition(bogusPos);

            bogusPos = new Vector3D(i * 0.08, i * 9.45, i * 712);
            t.transformPosition(bogusPos);
            t.transformPosition(bogusPos);
            t.transformPosition(bogusPos);

        }
        final long t1 = System.currentTimeMillis();
        System.out.println("Time : " + (t1 - t0) + " ms");
        // // print first
        // System.out.println(rezList.get(0));
        // // print last
        // System.out.println(rezList.get(rezList.size() - 1));

    }

    private static void configureFrameTree(final int mode) throws PatriusException {

        if (mode == 0) {
            /*
             * Example of frame configuration (IERS2010 convention)
             */
            // Define tides correction model
            final TidalCorrectionModel tides = TidalCorrectionModelFactory.TIDE_IERS2010_INTERPOLATED;
            // Define libration correction model
            final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.LIBRATION_IERS2010;
            // Define polar motion model, using tides, libration and S' model
            final PolarMotion defaultPolarMotion = new PolarMotion(true, tides,
                lib, SPrimeModelFactory.SP_IERS2010);
            // Define polar motion model, using tides, libration
            final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(
                tides, lib);
            // Define precession Nutation model to use
            // model and cache to use
            final PrecessionNutationModel model = new IERS20032010PrecessionNutation(
                PrecessionNutationConvention.IERS2010, true);
            // final PrecessionNutationModel cachedModel = new
            // PrecessionNutationCache(model);
            // Precession nutation (you choose either the direct computation
            // (model) or the interpolating model (cachedModel, much faster)
            final PrecessionNutation precNut = new PrecessionNutation(true, model);
            // Crete the configuration ...
            final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
            builder.setEOPHistory(new NoEOP2000History());
            builder.setDiurnalRotation(defaultDiurnalRotation);
            builder.setPolarMotion(defaultPolarMotion);
            builder.setPrecessionNutation(precNut);
            FramesFactory.setConfiguration(builder.getConfiguration());
        }

        // SIMPLIFIED CONFIGURATION
        if (mode == 1) {
            /*
             * Example of frame configuration (IERS2010 convention)
             */
            // Define tides correction model
            final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
            // Define libration correction model
            final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;
            // Define polar motion model, using tides, libration and S' model
            final PolarMotion defaultPolarMotion = new PolarMotion(true, tides,
                lib, SPrimeModelFactory.NO_SP);
            // Define polar motion model, using tides, libration
            final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(
                tides, lib);
            // Define precession Nutation model to use
            // model and cache to use
            final PrecessionNutationModel model = new IERS20032010PrecessionNutation(
                PrecessionNutationConvention.IERS2010, true);
            // final PrecessionNutationModel cachedModel = new
            // PrecessionNutationCache(model);
            // Precession nutation (you choose either the direct computation
            // (model) or the interpolating model (cachedModel, much faster)
            final PrecessionNutation precNut = new PrecessionNutation(true, model);
            // Crete the configuration ...
            final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
            builder.setEOPHistory(new NoEOP2000History());
            builder.setDiurnalRotation(defaultDiurnalRotation);
            builder.setPolarMotion(defaultPolarMotion);
            builder.setPrecessionNutation(precNut);
            // and pass it to the frames factory
            FramesFactory.setConfiguration(builder.getConfiguration());
        }

        if (mode == 2) {
            FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        }

        if (mode == 3) {
            /*
             * Example of frame configuration (IERS2010 convention)
             */
            // Define tides correction model
            final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
            // Define libration correction model
            final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;
            // Define polar motion model, using tides, libration and S' model
            final PolarMotion defaultPolarMotion = new PolarMotion(false, tides,
                lib, SPrimeModelFactory.NO_SP);
            // Define polar motion model, using tides, libration
            final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(
                tides, lib);
            // Define precession Nutation model to use
            // model and cache to use
            final PrecessionNutationModel model = new IERS20032010PrecessionNutation(
                PrecessionNutationConvention.IERS2010, true);
            // final PrecessionNutationModel cachedModel = new
            // PrecessionNutationCache(model);
            // Precession nutation (you choose either the direct computation
            // (model) or the interpolating model (cachedModel, much faster)
            final PrecessionNutation precNut = new PrecessionNutation(true, model);
            // Crete the configuration ...
            final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
            builder.setEOPHistory(new NoEOP2000History());
            builder.setDiurnalRotation(defaultDiurnalRotation);
            builder.setPolarMotion(defaultPolarMotion);
            builder.setPrecessionNutation(precNut);
            // and pass it to the frames factory
            FramesFactory.setConfiguration(builder.getConfiguration());
            // and pass it to the frames factory

        }

    }

    private static final class PatriusData {

        // private constructor
        private PatriusData() {

        }

        /*
         * Method to configure Orekit. To run a propagation it is necessary to
         * rewrite the path variable with the path where resources are contained
         */
        public static void configureDataManager() {
            Utils.setDataRoot("D1006_PATRIUS_DATASET");
        }

    }

}

// CI-DESSOUS variante de PerfTest adaptee a la release 2
// package org.orekit.frames;
//
// import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
// import org.orekit.Utils;
// import org.orekit.errors.OrekitException;
// import org.orekit.time.AbsoluteDate;
//
//
// public class PerfTest {
//
// /**
// * @param args
// * @throws OrekitException
// * @throws InterruptedException
// */
// public static void main(String[] args) throws OrekitException, InterruptedException {
//
// Thread.sleep(10000);
// System.out.println("PERFTEST begin...");
// /*
// * Setup OREKIT ...
// *
// * DataProviderManager configuration
// */
// PatriusData.configureDataManager();
//
// //final List<Vector3D> rezList = new ArrayList<Vector3D>();
//
// long t0 = System.currentTimeMillis();
// for (int i = 0; i < 400000; i++) {
// final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH.shiftedBy(3*365*24*60*60);
// // create a bogus position vector
// Vector3D bogusPos = new Vector3D(i * 123.456, i * -0.25, i * 33.12 * Math.PI);
// // convert it from ITRF2008 Frame to GCRF Frame
// Transform t = FramesFactory.getITRF2008().getTransformTo(FramesFactory.getGCRF(),
// startDate.shiftedBy(3205 * i));
// Vector3D convBogusPos = t.transformPosition(bogusPos);
//
// bogusPos = new Vector3D(i * 0.08, i * 9.45, i * 712);
// convBogusPos = t.transformPosition(bogusPos);
// convBogusPos = t.transformPosition(bogusPos);
// convBogusPos = t.transformPosition(bogusPos);
//
// }
// long t1 = System.currentTimeMillis();
// System.out.println("Time : "+(t1-t0)+" ms");
// // // print first
// // System.out.println(rezList.get(0));
// // // print last
// // System.out.println(rezList.get(rezList.size() - 1));
//
//
// }
//
// private static class PatriusData {
//
// // private constructor
// private PatriusData() {
//
// }
//
// /*
// * Method to configure Orekit. To run a propagation it is necessary to
// * rewrite the path variable with the path where resources are contained
// */
// public static void configureDataManager() {
// Utils.setDataRoot("D1006_PATRIUS_DATASET");
// }
//
//
// }
//
// }

