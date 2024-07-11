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
 * @history Created on 09/11/2015
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:09/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:1421:13/03/2018: Correction of GST epoch
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class implements the PVCoordinatesProvider to compute position velocity of a GPS or Galileo constellation
 * satellite from its almanac parameters.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author chabaudp
 * 
 * @version $Id: AlmanacPVCoordinates.java 17584 2017-05-10 13:26:39Z bignon $
 * 
 * @since 3.1
 * 
 */
public class AlmanacPVCoordinates implements PVCoordinatesProvider {

    /** Almanach parameters. */
    private final AlmanacParameter almanachParams;

    /** Earth's gravitational constant. */
    private final double muCste;

    /** Earth's rotation rate. */
    private final double earthRotationRate;

    /** Number of cycles. */
    private final int nCycles;

    /**
     * Creates an instance of AlmanacPVCoordinates.
     * 
     * @param parameters
     *        Almanac parameters
     * @param cycle
     *        number of cycles
     * @param mu
     *        earth's gravitational constant
     * @param rotationRate
     *        earth's rotation rate
     * @since 3.1
     */
    public AlmanacPVCoordinates(final AlmanacParameter parameters, final int cycle, final double mu,
        final double rotationRate) {
        this.almanachParams = parameters;
        this.muCste = mu;
        this.earthRotationRate = rotationRate;
        this.nCycles = cycle;
    }

    /**
     * Geometric computation of the position to a date.
     * Computes a finite difference between position at date minus step and position at date plus step.
     * If Input frame is null, results are expressed in WGS84.
     * 
     * @param date
     *        Date to compute coordinates
     * @param frame
     *        Results expression frame.
     * @return position velocity coordinates at input date in frame
     * @throws PatriusException
     *         if input frame is different from WGS84 and configuration has not been defined
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // Cartesian parameters
        final CartesianParameters cartesianParams = this.getCartesianParameters(date);

        // compute position in WGS84
        final Vector3D position = cartesianParams.getPosition();

        // compute velocity in WGS84 (remove effect of the rotation earth)
        final Vector3D omegaVectP = new Vector3D(0, 0, this.earthRotationRate).crossProduct(position);
        final Vector3D velocity = cartesianParams.getVelocity().subtract(omegaVectP);

        // coordinates in WGS84
        PVCoordinates gPVCoord = new PVCoordinates(position, velocity);

        // Convert coordinates in output frame
        if (frame != null && frame != FramesFactory.getITRF()) {
            final Transform wgs84ToOutputFrame = FramesFactory.getITRF().getTransformTo(frame, date);
            gPVCoord = wgs84ToOutputFrame.transformPVCoordinates(gPVCoord);
        }

        return gPVCoord;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return FramesFactory.getITRF();
    }

    /**
     * Compute the Cartesian parameters from the almanac parameters in WGS84.
     * 
     * @param date
     *        Position's date
     * @return Cartesian parameters at input date in WGS84
     * @throws PatriusException if UTC data can't be load
     * @since 3.1
     */
    private CartesianParameters getCartesianParameters(final AbsoluteDate date) throws PatriusException {

        // Get total number of weeks
        final int offsetCycle = this.almanachParams.getRolloverWeeks() * this.nCycles;

        // duration from beginning of the week
        final double duration = date.durationFrom(this.almanachParams.getDate(this.almanachParams.getWeekRef()
            + offsetCycle, 0));
        // duration from almanac reference date
        final double durationFromReference = duration - this.almanachParams.gettRef();
        // Semi major axis
        final double a = this.almanachParams.getSqrtA() * this.almanachParams.getSqrtA();
        // Eccectricity
        final double e = this.almanachParams.getEccentricity();
        // Inlicnation
        final double i = this.almanachParams.getI();
        // Perigee argument
        final double pa = this.almanachParams.getW();
        // Longitude of ascending node
        final double raan = (this.almanachParams.getOmegaInit()) +
            ((this.almanachParams.getOmegaRate() - this.earthRotationRate) * durationFromReference) -
            (this.earthRotationRate * this.almanachParams.gettRef());

        // Mean motion (rad / s)
        final double n = MathLib.sqrt(MathLib.divide(this.muCste, MathLib.pow(a, 3)));
        // Mean anomaly (rad)
        final double anoM = this.almanachParams.getMeanAnomalyInit() + (n * durationFromReference);

        // Associated keplerian parameters
        final KeplerianParameters kepParams =
            new KeplerianParameters(a, e, i, pa, raan, anoM, PositionAngle.MEAN, this.muCste);

        return kepParams.getCartesianParameters();
    }
}
