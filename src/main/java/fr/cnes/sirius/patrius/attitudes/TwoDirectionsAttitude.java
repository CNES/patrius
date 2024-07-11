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
 * @history creation 02/04/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:655:27/07/2016:add finite differences step
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.kinematics.AbstractOrientationFunction;
import fr.cnes.sirius.patrius.attitudes.kinematics.OrientationFunction;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class implements a generic two directions attitude law. The first direction is aligned with a given satellite
 * axis, the second direction is aligned at best with another given satellite axis.
 * </p>
 * 
 * @concurrency unconditionally thread safe
 * 
 * @see IDirection
 * 
 * @author Julie Anton
 * 
 * @version $Id: TwoDirectionsAttitude.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class TwoDirectionsAttitude extends AbstractAttitudeLaw {

    /** IUD. */
    private static final long serialVersionUID = -5992543210599145752L;

    /** Default step for finite differences spin computation. */
    private static final double DEFAULT_STEP_VEL = 0.2;

    /** Default step for finite differences spin computation. */
    private static final double DEFAULT_STEP_ACC = 0.001;

    /** First direction. */
    private final IDirection firstDir;
    /** Second direction. */
    private final IDirection secondDir;
    /** The first satellite axis is aligned with the first direction. */
    private final Vector3D firstSatAxis;
    /** The Second satellite axis is aligned with the second direction at best. */
    private final Vector3D secondSatAxis;

    /** Step for finite differences spin computation. */
    private final double stepSpin;

    /** Step for finite differences spin derivative computation. */
    private final double stepAcc;

    /**
     * Constructor with default step values for spin and spin derivatives computation using finite differences
     * (0.2s for spin, 0.001s for spin derivative).
     * 
     * @param firstDirection
     *        first direction
     * @param secondDirection
     *        second direction
     * @param firstAxis
     *        satellite axis that has to be aligned with the fisrt direction
     * @param secondAxis
     *        satellite axis that has to be aligned at best with the second direction
     */
    public TwoDirectionsAttitude(final IDirection firstDirection, final IDirection secondDirection,
        final Vector3D firstAxis, final Vector3D secondAxis) {
        this(firstDirection, secondDirection, firstAxis, secondAxis, DEFAULT_STEP_VEL, DEFAULT_STEP_ACC);
    }

    /**
     * Constructor.
     * 
     * @param firstDirection
     *        first direction
     * @param secondDirection
     *        second direction
     * @param firstAxis
     *        satellite axis that has to be aligned with the fisrt direction
     * @param secondAxis
     *        satellite axis that has to be aligned at best with the second direction
     * @param dtSpin
     *        step for finite differences spin computation
     * @param dtAcc
     *        step for finite differences spin derivative computation
     */
    public TwoDirectionsAttitude(final IDirection firstDirection, final IDirection secondDirection,
        final Vector3D firstAxis, final Vector3D secondAxis, final double dtSpin,
        final double dtAcc) {
        super();
        this.firstDir = firstDirection;
        this.secondDir = secondDirection;
        this.firstSatAxis = firstAxis;
        this.secondSatAxis = secondAxis;
        this.stepSpin = dtSpin;
        this.stepAcc = dtAcc;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {

        // Get rotation
        final Rotation q = this.computeOrientation(pvProv, date, frame);

        // Get spin (finite differences)
        final Vector3DFunction spinFunction = this.getSpinFunction(pvProv, frame, date);
        final Vector3D spin = spinFunction.getVector3D(date);

        // Get spin derivative
        Vector3D spinDerivative = null;
        if (this.getSpinDerivativesComputation()) {
            spinDerivative = spinFunction.nthDerivative(1).getVector3D(date);
        }
        return new Attitude(frame, new TimeStampedAngularCoordinates(date, q, spin, spinDerivative));
    }

    /**
     * Get orientation.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which orientation function is computed
     * @param date
     *        date
     * @return orientation at date
     * @throws PatriusException
     *         thrown if direction could not be computed
     */
    private Rotation computeOrientation(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                        final Frame frame) throws PatriusException {
        final Vector3D u1 = this.firstDir.getVector(pvProv, date, frame);
        final Vector3D u2 = this.secondDir.getVector(pvProv, date, frame);
        return new Rotation(this.firstSatAxis, this.secondSatAxis, u1, u2);
    }

    /**
     * Get orientation function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which orientation function is computed
     * @param zeroAbscissa
     *        the date for which x=0 for orientation function of date
     * @return the orientation (quaternion) function of the date
     */
    private OrientationFunction getOrientationFunction(final PVCoordinatesProvider pvProv, final Frame frame,
                                                       final AbsoluteDate zeroAbscissa) {
        return new AbstractOrientationFunction(zeroAbscissa){
            /** {@inheritDoc} */
            @Override
            public Rotation getOrientation(final AbsoluteDate date) throws PatriusException {
                return TwoDirectionsAttitude.this.computeOrientation(pvProv, date, frame);
            }
        };
    }

    /**
     * Get spin function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @return spin function of date relative
     */
    private Vector3DFunction getSpinFunction(final PVCoordinatesProvider pvProv, final Frame frame,
                                             final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa, new FiniteDifferencesDifferentiator(4, this.stepAcc)){
            /** {@inheritDoc} */
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                final OrientationFunction rot = TwoDirectionsAttitude.this.getOrientationFunction(pvProv, frame, date);
                return rot.estimateRateFunction(TwoDirectionsAttitude.this.stepSpin, AbsoluteDateInterval.INFINITY)
                        .getVector3D(date);
            }
        };
    }

    /**
     * Getter for the satellite axis aligned with the first direction.
     * 
     * @return the satellite axis aligned with the first direction
     */
    public Vector3D getFirstAxis() {
        return this.firstSatAxis;
    }

    /**
     * Getter for the satellite axis aligned at best with the second direction.
     * 
     * @return the satellite axis aligned at best with the second direction
     */
    public Vector3D getSecondAxis() {
        return this.secondSatAxis;
    }
}
