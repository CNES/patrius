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
 * @history created 06/08/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.fieldsofview.PyramidalField;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the satellite enters a ground zone (several zones can be defined at the same time).
 * 
 * <p>
 * The default implementation behaviour is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when the spacecraft
 * enters the zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE continue} when
 * it leaves it. This can be changed by using one of the provided constructors.
 * </p>
 * <p>
 * Beware of the MaxCheck and Threshold parameters : if the zone is complex and the MaxCheck too large, the event
 * detection could fail, because it would not manage to converge. To compute an approximative event even if the zone is
 * precise and complex, set a large enough Threshold.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * @see PyramidalField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class EarthZoneDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -1486877151128101288L;

    /** the central body */
    private final BodyShape centralBodyShape;

    /**
     * The pyramidal field that represent the cone of space over the zone.
     */
    private final List<IFieldOfView> fields;

    /**
     * Body attached frame
     */
    private final Frame frame;

    /**
     * Constructor for the earth zones entering detector with default maxCheck and convergence
     * threshold.
     * <p>
     * The zones are defined by an array of geodetic points. The points must be given in the right order : from the
     * point i to the point i + 1, if the associated vector from the center of the earth are v(i) and v(i + 1), the
     * inside of the zone is on the side of the positive cross vector from the earth center v(i) * v(i+1).
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft's nadir point to get the angular distance
     * to the
     * </p>
     * border of the zones.
     * 
     * @param centralBody the central body
     * @param zonesPoints The zones : for each of the list, the points that define the zone is given
     *        as an array of {latitude, longitude}
     */
    public EarthZoneDetector(final BodyShape centralBody, final List<double[][]> zonesPoints) {
        this(centralBody, zonesPoints, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the earth zones entering detector.
     * <p>
     * The zones are defined by an array of geodetic points. The points must be given in the right order : from the
     * point i to the point i + 1, if the associated vector from the center of the earth are v(i) and v(i + 1), the
     * inside of the zone is on the side of the positive cross vector from the earth center v(i) * v(i+1).
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft's nadir point to get the angular distance
     * to the border of the zones.
     * </p>
     * <p>
     * The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when the spacecraft
     * point enters the zone and to {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#CONTINUE
     * continue} when it leaves it.
     * </p>
     * 
     * @param centralBody the central body
     * @param zonesPoints The zones : for each of the list, the points that define the zone is given
     *        as an array of {latitude, longitude}
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     */
    public EarthZoneDetector(final BodyShape centralBody, final List<double[][]> zonesPoints,
                             final double maxCheck, final double threshold) {
        this(centralBody, zonesPoints, maxCheck, threshold, Action.STOP, Action.CONTINUE);
    }

    /**
     * Constructor for the earth zones entering detector.
     * <p>
     * The zones are defined by an array of geodetic points. The points must be given in the right order : from the
     * point i to the point i + 1, if the associated vector from the center of the earth are v(i) and v(i + 1), the
     * inside of the zone is on the side of the positive cross vector from the earth center v(i) * v(i+1).
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft's nadir point to get the angular distance
     * to the border of the zones.
     * </p>
     * 
     * @param centralBody the central body
     * @param zonesPoints The zones : for each of the list, the points that define the zone is given
     *        as an array of {latitude, longitude}
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param entry when the spacecraft point enters the zone.
     * @param exit when the spacecraft point leave the zone.
     */
    public EarthZoneDetector(final BodyShape centralBody, final List<double[][]> zonesPoints,
                             final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(centralBody, zonesPoints, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor for the earth zones entering detector.
     * <p>
     * The zones are defined by an array of geodetic points. The points must be given in the right order : from the
     * point i to the point i + 1, if the associated vector from the center of the earth are v(i) and v(i + 1), the
     * inside of the zone is on the side of the positive cross vector from the earth center v(i) * v(i+1).
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft's nadir point to get the angular distance
     * to the border of the zones.
     * </p>
     * 
     * @param centralBody the central body
     * @param zonesPoints The zones : for each of the list, the points that define the zone is given
     *        as an array of {latitude, longitude}
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param entry when the spacecraft point enters the zone.
     * @param exit when the spacecraft point leave the zone.
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     * @since 3.1
     */
    public EarthZoneDetector(final BodyShape centralBody, final List<double[][]> zonesPoints,
                             final double maxCheck, final double threshold, final Action entry, final Action exit,
                             final boolean removeEntry, final boolean removeExit) {

        // initialisations
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.centralBodyShape = centralBody;
        this.frame = this.centralBodyShape.getBodyFrame();

        this.fields = new ArrayList<>();
        for (final double[][] zonePoints : zonesPoints) {
            // definition of the conic field
            final Vector3D[] directions = new Vector3D[zonePoints.length];
            for (int i = 0; i < zonePoints.length; i++) {
                directions[i] = this.centralBodyShape.transform(new GeodeticPoint(zonePoints[i][0],
                    zonePoints[i][1], 0.));
            }

            checkVectors(directions.clone());
            this.fields.add(new PyramidalField("earth zone", directions));
        }
    }

    /**
     * Constructor for the earth zones entering detector.
     * <p>
     * The zones are defined by an array of geodetic points. The points must be given in the right order : from the
     * point i to the point i + 1, if the associated vector from the center of the earth are v(i) and v(i + 1), the
     * inside of the zone is on the side of the positive cross vector from the earth center v(i) * v(i+1).
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft's nadir point to get the angular distance
     * to the border of the zones.
     * </p>
     * 
     * @param centralBody the central body
     * @param fieldsIn list of fields of view
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param entry when the spacecraft point enters the zone.
     * @param exit when the spacecraft point leave the zone.
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     * @since 3.1
     */
    public EarthZoneDetector(final List<IFieldOfView> fieldsIn, final BodyShape centralBody,
                             final double maxCheck, final double threshold, final Action entry, final Action exit,
                             final boolean removeEntry, final boolean removeExit) {

        // initialisations
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.centralBodyShape = centralBody;
        this.frame = this.centralBodyShape.getBodyFrame();
        this.fields = fieldsIn;
    }

    /**
     * Constructor for the earth zones entering detector with default maxCheck and convergence
     * threshold.
     * <p>
     * The zones are defined by an array of points of space expressed in the earth attached frame. The points must be
     * given in the right order : from the vector i to the point i + 1, the inside of the zone is on the side of the
     * positive cross vector from the earth center v(i) * v(i+1).
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft position itself to get the angular
     * distance to the border of the zones.
     * </p>
     * 
     * @param directionsList The zones : for each of the list, the directions from the earth center
     *        define the zone
     * @param bodyFrame the frame attached to the earth
     */
    public EarthZoneDetector(final List<Vector3D[]> directionsList, final Frame bodyFrame) {
        this(directionsList, bodyFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the earth zones entering detector.
     * <p>
     * The zones are defined by an array of points of space expressed in the earth attached frame. The points must be
     * given in the right order : from the vector i to the point i + 1, the inside of the zone is on the side of the
     * positive cross vector from the earth center v(i) * v(i+1)..
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft position itself to get the angular
     * distance to the border of the zones.
     * </p>
     * <p>
     * The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when the spacecraft
     * point enters the zone and to continue when it leaves it.
     * </p>
     * 
     * @param directionsList The zones : for each of the list, the directions from the earth center
     *        define the zone
     * @param bodyFrame the frame attached to the earth
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     */
    public EarthZoneDetector(final List<Vector3D[]> directionsList, final Frame bodyFrame,
                             final double maxCheck, final double threshold) {
        this(directionsList, bodyFrame, maxCheck, threshold, Action.STOP, Action.CONTINUE);
    }

    /**
     * Constructor for the earth zones entering detector.
     * <p>
     * The zones are defined by an array of points of space expressed in the Earth attached frame. The points must be
     * given in the right order : from the vector i to the point i + 1, the inside of the zone is on the side of the
     * positive cross vector from the earth center v(i) * v(i+1)..
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft position itself to get the angular
     * distance to the border of the zones.
     * </p>
     * 
     * @param directionsList The zones : for each of the list, the directions from the earth center
     *        define the zone
     * @param bodyFrame the frame attached to the earth
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param entry when the spacecraft point enters the zone.
     * @param exit when the spacecraft point leave the zone.
     */
    public EarthZoneDetector(final List<Vector3D[]> directionsList, final Frame bodyFrame,
                             final double maxCheck, final double threshold, final Action entry, final Action exit) {
        this(directionsList, bodyFrame, maxCheck, threshold, entry, exit, false, false);
    }

    /**
     * Constructor.
     * <p>
     * The zones are defined by an array of points of space expressed in the Earth attached frame. The points must be
     * given in the right order : from the vector i to the point i + 1, the inside of the zone is on the side of the
     * positive cross vector from the earth center v(i) * v(i+1)..
     * </p>
     * <p>
     * The arcs defined by two consecutive points shall not cross each other. Two Consecutive points shall not be too
     * close. A point shall not be exactly on the arc defined by two consecutive others.
     * </p>
     * <p>
     * Using that constructor, the switching function will test the spacecraft position itself to get the angular
     * distance to the border of the zones.
     * </p>
     * 
     * @param directionsList The zones : for each of the list, the directions from the earth center
     *        define the zone
     * @param bodyFrame the frame attached to the earth
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param entry when the spacecraft point enters the zone.
     * @param exit when the spacecraft point leave the zone.
     * @param removeEntry when the spacecraft point enters the zone.
     * @param removeExit when the spacecraft point leaves the zone.
     */
    public EarthZoneDetector(final List<Vector3D[]> directionsList, final Frame bodyFrame,
                             final double maxCheck, final double threshold, final Action entry, final Action exit,
                             final boolean removeEntry, final boolean removeExit) {

        // initialisations
        super(maxCheck, threshold, entry, exit, removeEntry, removeExit);
        this.centralBodyShape = null;
        this.frame = bodyFrame;

        this.fields = new ArrayList<>();
        for (final Vector3D[] directions : directionsList) {
            // definitiobn of the conic field
            checkVectors(directions.clone());
            this.fields.add(new PyramidalField("earthZone", directions));
        }
    }

    /**
     * Validity check of the vectors defining the zone. Two consecutive points must not be too close
     * (e-10 on the norm of their difference) and the arc between two consecutive points must not
     * cross the arc between two consecutive others.
     * 
     * @param directions the direction vectors
     */
    private static void checkVectors(final Vector3D[] directions) {

        // length : at least 3
        final int length = directions.length;
        if (length < 3) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.PDB_TOO_FEW_DIRECTIONS);
        }

        // normalisation
        for (int i = 0; i < length; i++) {
            directions[i] = directions[i].normalize();
        }

        // test that there isn't two consecutive identical vectors

        for (int i = 0; i < length; i++) {

            // the last is considered consecutive to the first
            final Vector3D nextVect;
            if (i == length - 1) {
                nextVect = directions[0];
            } else {
                nextVect = directions[i + 1];
            }

            // test on the difference between all consecutive vectors
            final Vector3D diff = directions[i].subtract(nextVect);
            if (diff.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PDB_CLOSE_CONSECUTIVE_DIRECTIONS);
            }
        }

        // test that no angular field crosses another
        if (length > 3) {
            for (int i = -1; i < length - 3; i++) {

                // the last is considered consecutive to the first
                final Vector3D arcRef1;
                int i1 = i;
                final Vector3D arcRef2 = directions[i + 1];
                if (i == -1) {
                    arcRef1 = directions[length - 1];
                    i1 = length - 1;
                } else {
                    arcRef1 = directions[i];
                }

                // with the ref arc i, test of the crossing with all other arcs
                for (int j = i + 2; j < length - 2; j++) {
                    crossTest(arcRef1, arcRef2, directions[j], directions[j + 1], i1, i + 1, j, j + 1);
                }
            }
        }
    }

    /**
     * Tests that a given arc defined by two vectors does not cross a reference arc defined by other
     * vectors. All vectors shall be normalized
     * 
     * @param arcRef1 first vector of the reference arc
     * @param arcRef2 second vector of the reference arc
     * @param arc1 first vector of the arc to test
     * @param arc2 second vector of the arc to test
     * @param i1 number of the arcRef1 point
     * @param i2 number of the arcRef2 point
     * @param j1 number of the arc1 point
     * @param j2 number of the arc2 point
     */
    private static void crossTest(final Vector3D arcRef1, final Vector3D arcRef2, final Vector3D arc1,
                           final Vector3D arc2, final int i1, final int i2, final int j1, final int j2) {

        // vectors used in the criteria computations
        final Vector3D n = Vector3D.crossProduct(arcRef1, arcRef2);
        final Vector3D m = Vector3D.crossProduct(arc1, arc2);
        final Vector3D x = Vector3D.crossProduct(n, m);
        final Vector3D a = arcRef1.add(arcRef2);
        final Vector3D b = arc1.add(arc2);

        // the three criteria for the crossing of those two arcs
        final boolean c1 = (Vector3D.dotProduct(n, arc1) * Vector3D.dotProduct(n, arc2)) < 0.;
        final boolean c2 = (Vector3D.dotProduct(m, arcRef1) * Vector3D.dotProduct(m, arcRef2)) < 0.;
        final boolean c3 = (Vector3D.dotProduct(a, x) * Vector3D.dotProduct(b, x)) > 0.;

        // if all are true, there is a crossing
        if (c1 && c2 && c3) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.PDB_CROSSING_ARCS, i1, i2, j1, j2);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle the event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when entering or leaving the zone.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        if (increasing) {
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        }
        return increasing ? this.getActionAtEntry() : this.getActionAtExit();
    }

    /**
     * Compute the value of the switching function. For each zone : if the zone has been defined
     * with latitudes ans longitudes, this function measures the angular distance from the
     * spacecraft's nadir point to the border of the conic field of space that defines the inside of
     * the zone. If the zone has been defined with direction vectors, it measures the angular
     * distance from the spacecraft itself to the border of that zone. This value is positive when
     * the spacecraft is in the field, negative otherwise.
     * 
     * @param s the current state information: date, kinematics, attitude
     * 
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {

        // the point to consider
        final Vector3D point;

        if (this.centralBodyShape == null) {
            // position of the spacecraft in the central body attached frame
            point = s.getPVCoordinates(this.frame).getPosition();
        } else {
            // position of the spacecraft's nadir in the central body attached frame
            final Vector3D position = s.getPVCoordinates().getPosition();
            final GeodeticPoint nadirGeoPoint = this.centralBodyShape.transform(position, s.getFrame(),
                s.getDate());
            point = this.centralBodyShape.transform(new GeodeticPoint(nadirGeoPoint.getLatitude(),
                nadirGeoPoint.getLongitude(), 0.));
        }

        // angular distance from the considered point to each conic zone
        Double maxDist = Double.NEGATIVE_INFINITY;
        for (final IFieldOfView field : this.fields) {
            maxDist = MathLib.max(field.getAngularDistance(point), maxDist);
        }
        return maxDist;
    }

    /**
     * Get the central body shape.
     * 
     * @return the central Body shape
     */
    public BodyShape getCentralBodyShape() {
        return this.centralBodyShape;
    }

    /**
     * Get the frame.
     * 
     * @return the frame
     */
    public Frame getFrame() {
        return this.frame;
    }

    /**
     * Get FOV list
     * 
     * @return the FOV list.
     */
    public List<IFieldOfView> getFOV() {
        return this.fields;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>centralBodyShape: {@link BodyShape}</li>
     * <li>fields: list of {@link IFieldOfView}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new EarthZoneDetector(this.fields, this.centralBodyShape, this.getMaxCheckInterval(),
            this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(),
            this.isRemoveAtExit());
    }
}
