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
 * HISTORY
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:FA:FA-2447:27/05/2020:Mathlib.divide() incomplète 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.slew.ConstantSpinSlewComputer;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * This class represents a constant spin slew.
 * </p>
 * <p>
 * The Constant spin slew is a "simple" slew that computes the attitude of the satellite using a spherical interpolation
 * of the quaternions representing the starting and ending attitudes.<br>
 * Some constraints, such as minimal maneuver duration or maximal angular velocity, must be taken into account during
 * the maneuver computation.<br>
 * Like all the other attitude legs, its interval of validity has closed endpoints.
 * </p>
 * 
 * @concurrency not thread safe
 * 
 * @concurrency.comment this class is not thread safe because the slew can be re-computed while the method
 *                      getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame) is called.
 * 
 * @author Tiziana Sabatini
 * @author Julie Anton
 * 
 * @version $Id: ConstantSpinSlew.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class ConstantSpinSlew implements Slew {

    /** IUD. */
    private static final long serialVersionUID = -6284538107289929309L;

    /** Nature. */
    private static final String DEFAULT_NATURE = "CONSTANT_SPIN_SLEW";

    /** Attitude at the beginning. */
    private final Attitude initAtt;

    /** Attitude at the end. */
    private final Attitude finalAtt;

    /** Spin of the slew. */
    private final Vector3D spin;

    /** Flag to indicate if spin derivation computation is activated. */
    private boolean spinDerivativesComputation = false;

    /** Interval of validity of the slew (with closed endpoints). */
    private AbsoluteDateInterval intervalOfValidity;

    /** Nature. */
    private final String nature;

    /**
     * Constructor for slew with a duration constraint.
     * @param initAtt initial attitude at start date
     * @param finalAtt final attitude at end date
     * @throws PatriusException thrown if some frame transformation failed
     */
    public ConstantSpinSlew(final Attitude initAtt,
            final Attitude finalAtt) throws PatriusException {
        this(initAtt, finalAtt, DEFAULT_NATURE);
    }

    /**
     * Constructor for slew with a duration constraint.
     * @param initAtt initial attitude at start date
     * @param finalAtt final attitude at end date
     * @param natureIn nature
     * @throws PatriusException thrown if some frame transformation failed
     */
    public ConstantSpinSlew(final Attitude initAtt,
            final Attitude finalAtt,
            final String natureIn) throws PatriusException {
        this.intervalOfValidity = new AbsoluteDateInterval(initAtt.getDate(), finalAtt.getDate());
        this.nature = natureIn;
        // Conversion to GCRF pivot frame for strict backward compatibility
        this.initAtt = initAtt.withReferenceFrame(FramesFactory.getGCRF());
        this.finalAtt = finalAtt.withReferenceFrame(FramesFactory.getGCRF());
        // Spin in initial attitude frame
        this.spin = this.computeSpin(this.initAtt, this.finalAtt, Double.NaN);
    }

    /**
     * Constructor for slew with an angular velocity constraint.
     * <b>Warning: this constructor should not be used by users. This constructor is to be use only
     * by {@link ConstantSpinSlewComputer} for strict backward compatibility</b>
     * @param initAtt initial attitude at start date
     * @param finalAtt final attitude at end date
     * @param spin spin value
     * @param natureIn nature
     * @throws PatriusException thrown if some frame transformation failed
     */
    public ConstantSpinSlew(final Attitude initAtt,
            final Attitude finalAtt,
            final double spin,
            final String natureIn) throws PatriusException {
        this.intervalOfValidity = new AbsoluteDateInterval(initAtt.getDate(), finalAtt.getDate());
        this.nature = natureIn;
        // Conversion to GCRF pivot frame for strict backward compatibility
        this.initAtt = initAtt.withReferenceFrame(FramesFactory.getGCRF());
        this.finalAtt = finalAtt.withReferenceFrame(FramesFactory.getGCRF());
        // Spin in initial attitude frame
        this.spin = this.computeSpin(this.initAtt, this.finalAtt, spin);
    }

    /**
     * Private constructor for copy method.
     * @param initAtt initial attitude at start date
     * @param finalAtt final attitude at end date
     * @param spin spin value
     * @param natureIn nature
     * @throws PatriusException thrown if some frame transformation failed
     */
    private ConstantSpinSlew(final Attitude initAtt,
            final Attitude finalAtt,
            final Vector3D spin,
            final String natureIn) throws PatriusException {
        this.intervalOfValidity = new AbsoluteDateInterval(initAtt.getDate(), finalAtt.getDate());
        this.nature = natureIn;
        // Conversion to GCRF pivot frame for strict backward compatibility
        this.initAtt = initAtt.withReferenceFrame(FramesFactory.getGCRF());
        this.finalAtt = finalAtt.withReferenceFrame(FramesFactory.getGCRF());
        this.spin = spin;
    }

    /**
     * Private method to compute the spin (axis and value) from the initial and final attitudes.
     * @param initAttitude initial attitude
     * @param finalAttitude final attitude
     * @param angularConstraint spin value in case of angular velocity constraint
     * @return spin vector
     */
    private final Vector3D computeSpin(final Attitude initAttitude,
            final Attitude finalAttitude,
            final double angularConstraint) {
        final Rotation rot = finalAttitude.getRotation().applyInverseTo(initAttitude.getRotation());
        final Vector3D axis = initAttitude.getRotation().applyInverseTo(rot.getAxis());
        final double spinValue;
        if (Double.isNaN(angularConstraint)) {
            spinValue = MathLib.divide(rot.getAngle(), intervalOfValidity.getDuration());
        } else {
            spinValue = angularConstraint;
        }
        return axis.scalarMultiply(spinValue);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final AbsoluteDate date, final Frame frame) throws PatriusException {
        if (!this.intervalOfValidity.contains(date)) {
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW);
        } else {
            // computes the duration of the maneuver:
            final double h = this.intervalOfValidity.getDuration();
            // computes the duration from the beginning of the maneuver to the given date:
            final double k = date.durationFrom(this.intervalOfValidity.getLowerData());
            // calls the slerp function contained in the Rotation class:
            final Rotation orientation = Rotation.slerp(this.initAtt.getRotation(), this.finalAtt.getRotation(),
                MathLib.divide(k, h));
            // Compute attitude (Constant spin slew has derivatives to any order equals to zero)
            final Attitude att = new Attitude(FramesFactory.getGCRF(),
                new TimeStampedAngularCoordinates(date, orientation, this.spin, Vector3D.ZERO));
            // creates the new attitude (rotation acceleration is always 0)
            return att.withReferenceFrame(frame, this.spinDerivativesComputation);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.spinDerivativesComputation = computeSpinDerivatives;
    }

    /**
     * <b>
     * Warning: provided {@link PVCoordinatesProvider} is here not used.
     * </b>
     */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        return this.getAttitude(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public final AbsoluteDateInterval getTimeInterval() {
        return this.intervalOfValidity;
    }

    /** {@inheritDoc} */
    @Override
    public ConstantSpinSlew copy(final AbsoluteDateInterval newIntervalOfValidity) {
        if (getTimeInterval().includes(newIntervalOfValidity)) {
            try {
                final Attitude initAttitude = getAttitude(newIntervalOfValidity.getLowerData(),
                        FramesFactory.getGCRF());
                final Attitude finalAttitude = getAttitude(newIntervalOfValidity.getUpperData(),
                        FramesFactory.getGCRF());
                final ConstantSpinSlew res = new ConstantSpinSlew(initAttitude, finalAttitude, spin, nature);
                res.setSpinDerivativesComputation(spinDerivativesComputation);
                // Return result
                return res;
            } catch (final PatriusException e) {
                // Should not happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
            }
        } else {
            // Interval not included
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }
    }

    /**
     * @return the duration of the time interval of validity of the slew.
     * @throws PatriusException
     *         if the time interval is not computed
     */
    public final double getDuration() throws PatriusException {
        return this.getTimeInterval().getDuration();
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return this.nature;
    }
}
