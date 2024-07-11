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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:652:28/09/2016:Link budget correction finalisation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.sensor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.RFLinkBudgetModel;
import fr.cnes.sirius.patrius.assembly.properties.RFAntennaProperty;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.RFStationAntenna;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the RF visibility event detector.
 * 
 * @see RFVisibilityDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFVisibilityDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle RF visibility detector
         * 
         * @featureDescription RF visibility detector computation.
         * 
         * @coveredRequirements DV-EVT_150
         * 
         */
        VISIBILITY_DETECTOR
    }

    /**
     * The RF antenna property.
     */
    static RFAntennaProperty property;

    /**
     * The assembly representing the satellite antenna.
     */
    static Assembly antenna;

    /**
     * The date.
     */
    static AbsoluteDate date;

    /**
     * The ground station antenna model.
     */
    static RFStationAntenna station;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_DETECTOR}
     * 
     * @testedMethod {@link RFVisibilityDetector#RFVisibilityDetector(RFLinkBudgetModel, double, double, double)}
     * @testedMethod {@link RFVisibilityDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests the eventOccurred method
     * 
     * @input the RF link budget detector input parameters
     * 
     * @output the eventOccurred method output
     * 
     * @testPassCriteria the eventOccurred method returns the right value
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testEventOccurred() throws PatriusException {

        // creates the RF link budget model:
        final RFLinkBudgetModel rfModel = new RFLinkBudgetModel(station, antenna, "mainBody");
        // creates the RF visibility detector
        final RFVisibilityDetector detector = new RFVisibilityDetector(rfModel, 5, 600, 1.e-6);
        final Orbit initialOrbit = new KeplerianOrbit(15000000.0, 0.0, MathUtils.HALF_PI, 0.0, 0.0,
            0.0, PositionAngle.TRUE, FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
            Utils.mu);

        Action actual = detector.eventOccurred(new SpacecraftState(initialOrbit), true, true);
        Assert.assertEquals(Action.CONTINUE, actual);
        actual = detector.eventOccurred(new SpacecraftState(initialOrbit), false, true);
        Assert.assertEquals(Action.STOP, actual);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VISIBILITY_DETECTOR}
     * 
     * @testedMethod {@link RFVisibilityDetector#RFVisibilityDetector(double, double, double, double, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters: the incidence, earth the earth shape, the max check value and
     *        the threshold value and the STOP Action.
     * 
     * @output a {@link RFVisibilityDetector}
     * 
     * @testPassCriteria the {@link RFVisibilityDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testConstructor() throws PatriusException {

        // creates the RF link budget model:
        final RFLinkBudgetModel rfModel = new RFLinkBudgetModel(station, antenna, "mainBody");
        // creates the RF visibility detector
        final RFVisibilityDetector detector = new RFVisibilityDetector(rfModel, 5, 600, 1.e-6,
            Action.CONTINUE, Action.STOP);
        // Test getters
        Assert.assertEquals(rfModel, detector.getLbModel());
        Assert.assertEquals(5, detector.getLbThreshold(), Utils.epsilonTest);

        final RFVisibilityDetector detectorCopy = (RFVisibilityDetector) detector.copy();
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);
    }

    /**
     * Setup for all unit tests in the class. It provides a ground station, a satellite antenna and
     * a date.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUpBeforeClass() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        // sets the date:
        date = AbsoluteDate.J2000_EPOCH;

        // sets the gain and ellipticity diagram values:
        final double[] xx = new double[] { 0. };
        final double[] yy = new double[] { 0. };
        final double[][] gain = new double[][] { { 5.6 } };
        final double[][] ellipticity = new double[][] { { 2.0 } };
        // creates the RF antenna property:
        property = new RFAntennaProperty(13.4, xx, yy, gain, ellipticity, 0.1, 2.0, 45.5E06,
            8.253E09);

        // building the assembly representing the satellite:
        final String mainBody = "mainBody";
        final AssemblyBuilder builder = new AssemblyBuilder();
        try {
            // add main part
            builder.addMainPart(mainBody);
            // add RF antenna property:
            builder.addProperty(property, mainBody);
            // assembly link to the tree of frames
            final UpdatableFrame mainFrame = new UpdatableFrame(FramesFactory.getEME2000(),
                Transform.IDENTITY, "mainFrame");
            builder.initMainPartFrame(mainFrame);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        // build the satellite antenna:
        antenna = builder.returnAssembly();

        // creates the RF ground station antenna:
        final double[][] atmLoss = new double[][] { { 0., 2.0 } };
        final double[][] pointLoss = new double[][] { { 0., 0.2 } };
        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0.,
            FramesFactory.getITRF());
        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "zero");
        station = new RFStationAntenna(topoFrame, 35, 2.0, 2.0, atmLoss, pointLoss, 0.);
    }
}
