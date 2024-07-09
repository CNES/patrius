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
 * @history 02/11/2015 (creation)
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:482:02/11/2015:Covariance Matrix Propagation
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.analytical.covariance;

import java.io.Serializable;

import fr.cnes.sirius.patrius.covariance.OrbitalCovariance;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class storing orbit covariance and method for coordinate and frame conversion.
 * 
 * @concurrency immutable
 * 
 * @author rodriguest
 * 
 * @version $Id: OrbitCovariance.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 * @deprecated Prefer {@link OrbitalCovariance}
 *
 */
@Deprecated
public class OrbitCovariance implements TimeStamped, Serializable {

    /** Serializable UID */
    private static final long serialVersionUID = 4738799398400178791L;

    /** Date */
    private final AbsoluteDate date;

    /** Reference Frame */
    private final Frame frame;

    /** Coordinates Type */
    private final OrbitType type;

    /** Covariance Matrix */
    private final ArrayRowSymmetricMatrix covariance;

    /**
     * Simple constructor
     * 
     * @param covDate
     *        Covariance Date.
     * @param refFrame
     *        Reference frame.
     * @param coordType
     *        Coordinates Type.
     * @param covMat
     *        Covariance Matrix
     */
    public OrbitCovariance(final AbsoluteDate covDate, final Frame refFrame, final OrbitType coordType,
        final RealMatrix covMat) {
        this.date = covDate;
        this.frame = refFrame;
        this.type = coordType;
        this.covariance = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, covMat);
    }

    /**
     * Simple constructor.
     * 
     * @param orbit
     *        Reference orbit.
     * @param coordType
     *        Coordinates Type
     * @param covMat
     *        Covariance matrix.
     */
    public OrbitCovariance(final Orbit orbit, final OrbitType coordType, final RealMatrix covMat) {
        this(orbit.getDate(), orbit.getFrame(), coordType, covMat);
    }

    /**
     * Simple constructor.
     * 
     * @param orbit
     *        Reference orbit.
     * @param covMat
     *        Covariance matrix.
     */
    public OrbitCovariance(final Orbit orbit, final RealMatrix covMat) {
        this(orbit.getDate(), orbit.getFrame(), orbit.getType(), covMat);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Covariance matrix getter.
     * 
     * @return the covariance matrix.
     */
    public ArrayRowSymmetricMatrix getCovarianceMatrix() {
        return this.covariance;
    }

    /**
     * Covariance matrix getter. Performs conversion to the output coordinates and frame requested.
     * 
     * @param refOrbit
     *        Reference orbit.
     * @param covTypeOut
     *        Output orbital parameters for covariance.
     * @param frameOut
     *        Output frame for covariance.
     * @return Covariance matrix in output coordinate and frame.
     * @throws PatriusException thrown if computation failed
     */

    public ArrayRowSymmetricMatrix getCovarianceMatrix(final Orbit refOrbit, final OrbitType covTypeOut,
                                               final Frame frameOut) throws PatriusException {

        RealMatrix covMat = this.covariance;
        OrbitType inCovType = this.type;

        // Synchronize orbit
        final Orbit orbit = refOrbit.shiftedBy(this.date.durationFrom(refOrbit.getDate()));

        // Check frame change
        if (frameOut != this.frame) {

            // Check type (frame conversion could be only performed on cartesian)
            if (inCovType != OrbitType.CARTESIAN) {

                // Converting covariance coordinates
                covMat = convertCovariance(covMat, orbit, this.type, OrbitType.CARTESIAN);

                // Update covariance coordinates
                inCovType = OrbitType.CARTESIAN;
            }

            // Convert frame
            covMat = convertCovariance(covMat, this.date, this.frame, frameOut);

        }

        // Convert Coordinates
        covMat = convertCovariance(covMat, orbit, inCovType, covTypeOut);

        return new ArrayRowSymmetricMatrix(SymmetryType.UPPER, covMat);
    }

    /**
     * Convert covariance matrix.
     * 
     * @param covMat
     *        Input covariance matrix.
     * @param epoch
     *        Epoch for conversion.
     * @param frameIn
     *        Input frame for the covariance.
     * @param frameOut
     *        Output frame for the covariance.
     * @return Covariance in output frame.
     * @throws PatriusException thrown if computation failed
     */

    private static RealMatrix convertCovariance(final RealMatrix covMat, final AbsoluteDate epoch,
                                                final Frame frameIn, final Frame frameOut) throws PatriusException {

        // Getting jacobian
        final RealMatrix frameJacobMat = frameIn.getTransformJacobian(frameOut, epoch);

        // Performs conversion J*P*Jt
        return frameJacobMat.multiply(covMat.multiply(frameJacobMat.transpose()));
    }

    /**
     * Convert covariance matrix.
     * 
     * @param covMat
     *        Input covariance matrix.
     * @param orbit
     *        Input orbit
     * @param covTypeIn
     *        Input coordinates type.
     * @param covTypeOut
     *        Output coordinates type.
     * @return Covariance in output coordinates.
     */

    private static ArrayRowSymmetricMatrix convertCovariance(final RealMatrix covMat, final Orbit orbit,
                                                     final OrbitType covTypeIn, final OrbitType covTypeOut) {

        RealMatrix outCovMat = covMat;

        // If output coordinates is different to covariance coordinates. Performs conversion.
        if (!covTypeIn.equals(covTypeOut)) {

            // Getting jacobian
            final RealMatrix jacobian = orbit.getJacobian(covTypeOut, covTypeIn);

            // Performs conversion J*P*Jt
            outCovMat = jacobian.multiply(outCovMat.multiply(jacobian.transpose()));
        }

        return new ArrayRowSymmetricMatrix(SymmetryType.UPPER, outCovMat);
    }

    /**
     * Propagate covariance. Performs a simple Keplerian propagation of the covariance.
     * 
     * @param refOrbit
     *        Reference orbit.
     * @param target
     *        Target epoch.
     * @return Propagated covariance to target epoch, in output coordinates and frame.
     */

    public OrbitCovariance propagate(final Orbit refOrbit, final AbsoluteDate target) {

        // Synchronize orbit to covariance date
        final Orbit orbit = refOrbit.shiftedBy(this.date.durationFrom(refOrbit.getDate()));

        // Compute propagation shift
        final double dt = target.durationFrom(orbit.getDate());

        // Getting state transition matrix in covariance coordinates
        RealMatrix transMat = orbit.getKeplerianTransitionMatrix(dt);

        final OrbitType kepType = OrbitType.KEPLERIAN;
        final Orbit finalOrbit = orbit.shiftedBy(dt);
        // Convert coordinates
        if (this.type != kepType) {
            final RealMatrix jacobT0 = orbit.getJacobian(kepType, this.type);
            final RealMatrix jacobT = finalOrbit.getJacobian(this.type, kepType);

            transMat = jacobT.multiply(transMat.multiply(jacobT0));
        }

        // Propagate
        final RealMatrix covMatPropagated = transMat.multiply(this.covariance.multiply(transMat.transpose()));

        return new OrbitCovariance(finalOrbit, this.type, covMatPropagated);
    }
}
