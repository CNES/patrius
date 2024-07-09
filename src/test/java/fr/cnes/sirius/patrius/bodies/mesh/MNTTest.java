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
 * HISTORY
* VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.fieldsofview.CircularField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.OmnidirectionalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation tests for {@link FacetCelestialBody} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class MNTTest {

    /**
     * Benchmark test for all {@link FacetCelestialBody} features.
     */
    @Test
    @Ignore
    public final void benchMarkTest() throws PatriusException, IOException, URISyntaxException {

        // Initialization
        System.out.println("Item                      | Duration | Target duration");

        // Load
        final String modelFile = "mnt" + File.separator + "Phobos_Ernst_HD.obj";
        final String fullName = FacetCelestialBody.class.getClassLoader().getResource(modelFile).toURI().getPath();
        double t0 = System.currentTimeMillis();
        final FacetCelestialBody body = new FacetCelestialBody("", new MyPVCoordinatesProvider(), 0, IAUPoleFactory.getIAUPole(null),
                new ObjMeshLoader(fullName));
        System.out.println("Load                      | " + (System.currentTimeMillis() - t0) / 1000. + "s   | 10s");
        System.gc();
        System.out.println("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + "Mb");

        // Initialization: ephemeris
        final List<SpacecraftState> statesList100000 = buildEphemeris(100000);
        final List<SpacecraftState> statesList2000000 = buildEphemeris(2000000);
        final List<SpacecraftState> statesList43200 = buildEphemeris(43200);

        // Intersections: 2 000 000 intersections
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 2000000; i++) {
            final Line line = new Line(new Vector3D(0, 0, 0), new Vector3D(Math.random(), Math.random(), Math.random()));
            body.getIntersection(line, line.getOrigin(), body.getBodyFrame(), AbsoluteDate.J2000_EPOCH);
        }
        System.out.println("Intersections (2 000 000) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 60s");

        // Get neighbors: 43 200 calls, 3000m radius
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 43200; i++) {
            body.getNeighbors(body.getTriangles()[i % body.getTriangles().length], 3000.).size();
        }
        System.out.println("Neighbors (43 200 calls, 3000m radius) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 600s");

        // Get surface pointed data ephemeris: 2 000 000 points ephemeris
        t0 = System.currentTimeMillis();
        body.getSurfacePointedDataEphemeris(statesList2000000, Vector3D.PLUS_K, new MeeusSun(), 0.1);
        System.out.println("Surface data ephemeris (2 000 000 points ephemeris) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 600s");

        // Get field data ephemeris: 43 200 calls, 3000m radius surface view
        t0 = System.currentTimeMillis();
        final IFieldOfView fieldOfView = new CircularField("", MathLib.asin(3000. / 10000.), Vector3D.PLUS_K);
        for (int i = 0; i < 43200; i++) {
            body.getFieldData(statesList100000.get(i), fieldOfView, Vector3D.PLUS_K);
        }
        System.out.println("Field data (43 200 calls, 3000m radius surface view) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 600s");

        // Get never visible triangles list: 43 200 points ephemeris
        t0 = System.currentTimeMillis();
        body.getNeverVisibleTriangles(statesList43200, new OmnidirectionalField(""));
        System.out.println("Never visible triangles list (43 200 points ephemeris) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 600s");
        
        // Get visible and enlightened triangles list with field if view: 43 200 points ephemeris
        t0 = System.currentTimeMillis();
        body.getVisibleAndEnlightenedTriangles(statesList43200, new MeeusSun(), fieldOfView);
        System.out.println("Visible and enlightened triangles list (43 200 points ephemeris) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 600s");

        // Compute eclipse: 43 200 points ephemeris
        final EclipseDetector detector = new EclipseDetector(new MeeusSun(), 700000E3, body, 1, 600, 1E-6, Action.CONTINUE, Action.CONTINUE);
        t0 = System.currentTimeMillis();
        for (int i = 0; i < 43200; i++) {
            detector.g(statesList43200.get(i));
        }
        System.out.println("Detection (43 200 points ephemeris) | " + (System.currentTimeMillis() - t0) / 1000. + "s | 300s");
    }

    /**
     * Build a circular ephemeris around Phobos.
     * @param statesNumber number of states in ephemeris
     * @return an ephemeris
     */
    public List<SpacecraftState> buildEphemeris(final int statesNumber) throws PatriusException {
        final List<SpacecraftState> statesList = new ArrayList<SpacecraftState>();
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        for (int i = 0; i < statesNumber; i++) {
            final Orbit orbit = new KeplerianOrbit(20000, 0, MathLib.PI / 2., 0, 0, MathLib.PI / 2., PositionAngle.TRUE,
                    FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, 1E5).shiftedBy(i * 100);
            final Attitude attitude = attitudeProvider.getAttitude(orbit);
            statesList.add(new SpacecraftState(orbit, attitude));
        }
        return statesList;
    }

    private class MyPVCoordinatesProvider implements PVCoordinatesProvider {
        @Override
        public PVCoordinates getPVCoordinates(AbsoluteDate date,
                Frame frame) throws PatriusException {
            return PVCoordinates.ZERO;
        }
    }
}
