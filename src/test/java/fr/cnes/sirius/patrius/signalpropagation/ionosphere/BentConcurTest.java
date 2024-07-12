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
 * @history created 12/12/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the BentModel class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Concurrency test for the BentIonoCorrection use case.
 * Shared instances are in fact of the R12Loader and USKLoader types.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public class BentConcurTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Ionospheric correction : Bent model
         * 
         * @featureDescription Computation of the electronic content with the Bent model for
         *                     the correction of the ionosphere effects.
         * 
         * @coveredRequirements DV-MES_FILT_450
         */
        IONO_BENT_MODEL
    }

    /**
     * Setup.
     * 
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     */
    @BeforeClass
    public static void setUp() throws PatriusException, IOException {
        Utils.setDataRoot("bent");
        FramesFactory.setConfiguration(Utils.getZOOMConfiguration());
    }

    /**
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#computeSignalDelay(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description Parallel Bent computations. R12Loader and USKLoader are the shared instances
     *              (not BentIonoCorrection).
     *              <p>
     *              This test puts in evidence the need to use <code>synchronized</code> tag in the
     *              {@link BentModel#computeSignalDelay(AbsoluteDate, Vector3D, Frame)
     *              computeSignalDelay} method.
     *              </p>
     * 
     * @testPassCriteria non-parallel and parallel results are identical.
     */
    @Test
    public void parallelBentTestBis() throws PatriusException {

        // Tasks number
        final int tasksNumber = 10000;

        // Solar activity
        final SolarActivityDataProvider solarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader("CCIR12");
        // NEWUSK
        final USKProvider uskProv = new USKLoader("NEWUSK");

        // Frame and date initialization
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Station initialization
        final BodyShape earth = new OneAxisEllipsoid(
                Constants.GRS80_EARTH_EQUATORIAL_RADIUS, Constants.GRS80_EARTH_FLATTENING,
                FramesFactory.getITRF(), "Earth");
        final GeodeticPoint coordinates = new GeodeticPoint(FastMath.toRadians(67.8805741),
                FastMath.toRadians(21.0310484), 521.18);
        final TopocentricFrame topoFrame = new TopocentricFrame(earth, coordinates, "topo");
        final Vector3D stationPosInTopoFrame = Vector3D.ZERO;
        final double freq = 1e5;

        // Bent model initialization
        final BentModel bentModel = new BentModel(r12Prov, solarActivity, uskProv, earth,
            stationPosInTopoFrame, topoFrame);

        // Build the N satellite positions to evaluate (express in GCRF)
        final Map<Vector3D, Double> satPosList = new HashMap<>(tasksNumber);
        for (int i = 0; i < tasksNumber; i++) {
            final Vector3D satCord = new Vector3D(7_000_000. + i * 10, 2_000., -15_000);
            final double bentRef = bentModel.computeSignalDelay(freq, date, satCord, gcrf);
            satPosList.put(satCord, bentRef);
        }

        // Multi-threads analysis (shared on all available threads):
        // Compute the N signal delays with the bent model on each satellite position
        satPosList.entrySet().parallelStream().forEach(entry -> {
            try {
                final double bentValue = bentModel.computeSignalDelay(freq, date, entry.getKey(), gcrf);
                Assert.assertEquals(new Double(bentValue), entry.getValue());
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        });
    }
}
