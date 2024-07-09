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
 * @history created 12/04/2013
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:219:03/04/2014:Changed API and corrected reference frame
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections
 * (move IDirection in package attitudes.directions + units/javadoc modifications)
 * VERSION::FA:367:21/11/2014:Recette V2.3 corrections (modify unity)
 * VERSION::FA:602:18/10/2016:Correct timescale
 * END-HISTORY
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class provides the Sun direction at a specific date, according to a simple analytical model.<br>
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class BasicBoardSun implements IDirection {

    /** Serial UID. */
    private static final long serialVersionUID = 6052919441302392515L;

    /** GCRF frame. */
    private static final Frame GCRF = FramesFactory.getGCRF();

    /** Sun mean longitude default constant &alpha;1 (rad). */
    private static final double MEAN_LONGITUDE_0 = 280.460 * MathUtils.DEG_TO_RAD;

    /** Sun mean longitude default constant &alpha;2 (rad/day). */
    private static final double MEAN_LONGITUDE_1 = 0.9856091 * MathUtils.DEG_TO_RAD;

    /** Sun mean anomaly default constant &nu;1 (rad). */
    private static final double MEAN_ANOMALY_0 = 357.528 * MathUtils.DEG_TO_RAD;

    /** Sun mean anomaly default constant &nu;2 (rad/day). */
    private static final double MEAN_ANOMALY_1 = 0.9856003 * MathUtils.DEG_TO_RAD;

    /** Sun longitude amplitude l1 default value (rad). */
    private static final double LONGITUDE_AMPLITUDE = 1.915 * MathUtils.DEG_TO_RAD;

    /** Equatorial plan / ecliptic plan inclination &epsilon; default value (rad). */
    private static final double EQUAT_ECLIPT_INCLINATION = 23.43 * MathUtils.DEG_TO_RAD;

    /** Default reference date */
    private static final AbsoluteDate DEFAULT_REF_DATE = new AbsoluteDate(DateComponents.J2000_EPOCH,
        TimeComponents.H12, TimeScalesFactory.getTAI());

    /** Sun mean longitude constant &alpha;1 (rad). */
    private final double a1;

    /** Sun mean longitude constant &alpha;2 (rad/s). */
    private final double a2;

    /** Sun mean anomaly constant &nu;1 (rad). */
    private final double n1;

    /** Sun mean anomaly constant &nu;2 (rad/s). */
    private final double n2;

    /** Sun mean anomaly constant l1 (rad). */
    private final double l1;

    /** Equatorial plan / ecliptic plane inclination &epsilon; (rad). */
    private final double eps;

    /** Reference date. */
    private final AbsoluteDate refDate;

    /**
     * Constructor with default values.
     */
    public BasicBoardSun() {
        // call underlying constructor
        this(DEFAULT_REF_DATE, MEAN_LONGITUDE_0, MEAN_LONGITUDE_1 / Constants.JULIAN_DAY,
            MEAN_ANOMALY_0, MEAN_ANOMALY_1 / Constants.JULIAN_DAY, LONGITUDE_AMPLITUDE, EQUAT_ECLIPT_INCLINATION);
    }

    /**
     * Constructor with user values.
     * 
     * @param ref
     *        Reference date
     * @param alpha1
     *        Sun mean longitude constant &alpha;1 (rad)
     * @param alpha2
     *        Sun mean longitude constant &alpha;2 (rad/s)
     * @param nu1
     *        Sun mean anomaly default constant &nu;1 (rad)
     * @param nu2
     *        Sun mean anomaly default constant &nu;2 (rad/s)
     * @param lon1
     *        Sun longitude amplitude
     * @param epsilon epsilon
     */
    public BasicBoardSun(final AbsoluteDate ref, final double alpha1, final double alpha2,
        final double nu1, final double nu2, final double lon1, final double epsilon) {
        // GCRF frame
        this.refDate = ref;
        this.a1 = alpha1;
        this.a2 = alpha2;
        this.n1 = nu1;
        this.n2 = nu2;
        this.l1 = lon1;
        this.eps = epsilon;
    }

    /**
     * <p>
     * Get the direction of the sun. The parameter pvCoord is not used because all directions are colinear.
     * 
     * </p>
     * {@inheritDoc}
     */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {

        // time measured in Julian days from the epoch J2000:
        final double dt = date.offsetFrom(this.refDate, TimeScalesFactory.getTAI());

        // Mean longitude of the Sun computation:
        double alpha = this.a1 + this.a2 * dt;
        alpha = MathUtils.normalizeAngle(alpha, FastMath.PI);

        // Mean anomaly of the Sun computation:
        double nu = this.n1 + this.n2 * dt;
        nu = MathUtils.normalizeAngle(nu, FastMath.PI);

        // Longitude of the Sun computation:
        final double lon = alpha + this.l1 * MathLib.sin(nu);

        // Sun direction in GCRF frame
        final double[] sincosLon = MathLib.sinAndCos(lon);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];
        final double[] sincosEps = MathLib.sinAndCos(this.eps);
        final double sinEps = sincosEps[0];
        final double cosEps = sincosEps[1];
        final Vector3D direction = new Vector3D(cosLon, sinLon * cosEps, sinLon * sinEps);

        // transformation from GCRF to target frame
        final Transform transform = GCRF.getTransformTo(frame, date);

        return transform.transformVector(direction);
    }

    /**
     * <p>
     * Get the line from the position in pvCoord to the Sun.
     * </p>
     * {@inheritDoc}
     */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {
        // satellite position in specified frame
        final Vector3D satPosition = (pvCoord == null) ? Vector3D.ZERO
            : pvCoord.getPVCoordinates(date, frame).getPosition();

        // line
        return Line.createLine(satPosition, this.getVector(pvCoord, date, frame));
    }
}
