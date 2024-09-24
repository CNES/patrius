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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.groundstation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the GeometricStationAntenna class.
 * 
 * @author tbonit
 */
public class GeometricStationAntennaTest {

    /** The geometric ground station antenna model. */
    private static GeometricStationAntenna station;

    /** The topocentric frame. */
    private static TopocentricFrame topoFrame;

    /** The field of view. */
    private static IFieldOfView fieldOfView;

    /**
     * @testedMethod {@link GeometricStationAntenna#getTopoFrame()}
     * @testedMethod {@link GeometricStationAntenna#getFOV()}
     * 
     * @description test the getters of the geometric station antenna model.
     * 
     * @input the geometric ground station antenna model
     * 
     * @output the values
     * 
     * @testPassCriteria the values must be the expected ones.
     */
    @Test
    public void testStationAntennaModelGetters() {
        // tests the getters:
        Assert.assertEquals(topoFrame, station.getTopoFrame());
        Assert.assertEquals(fieldOfView, station.getFOV());
    }

    /**
     * @throws PatriusException
     *         if position cannot be computed in given frame
     * @description Evaluate the ground antenna serialization / deserialization process.
     *
     * @testPassCriteria The ground antenna can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final GeometricStationAntenna deserializedStation = TestUtils.serializeAndRecover(station);

        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate("2011-11-09T12:00:00Z",
            TimeScalesFactory.getTT());

        // Can't compare directly the TopoFrame or the FOV as new instances are created and they
        // don't override the equals() method, so directly compare the getPVCoordinates output to
        // evaluate the top frame and only check the fov's name attribute
        Assert.assertEquals(station.getPVCoordinates(date, frame),
            deserializedStation.getPVCoordinates(date, frame));
        Assert.assertEquals(station.getFOV().getName(), deserializedStation.getFOV().getName());
    }

    /**
     * Setup for all unit tests in the class.
     * It provides an geometric station antenna property.
     * 
     * @throws PatriusException
     *         if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        final OneAxisEllipsoid earthSpheric = new OneAxisEllipsoid(6378136.460, 0.,
            FramesFactory.getITRF());
        final EllipsoidPoint point = new EllipsoidPoint(earthSpheric, earthSpheric.getLLHCoordinatesSystem(), 0., 0.,
            0., "");

        topoFrame = new TopocentricFrame(point, "zero");
        fieldOfView = new OmnidirectionalField("FOV");
        station = new GeometricStationAntenna(topoFrame, fieldOfView);
    }
}
