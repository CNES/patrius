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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.slew;

import java.io.Serializable;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 *
 **/
/**
 * <p>
 * Class for constant spin slew computation with angular velocity constraint. Computation of slew returns a
 * {@link ConstantSpinSlew}.
 * </p>
 * <p>
 * The Constant spin slew is a "simple" slew that computes the attitude of the satellite using a spherical interpolation
 * of the quaternions representing the starting and ending attitudes.<br>
 * Like all the other attitude legs, its interval of validity has closed endpoints.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.5
 */
public class ConstantSpinSlewComputer implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -629085118980793220L;

    /** Maximum number of iterations when it comes to solve the maneuver duration. */
    private static final int DEFAULT_MAX_ITERATIONS = 1000;

    /** Default maximum duration of the maneuver. */
    private static final double DEFAULT_MAX_DURATION = 3600;

    /** Nature. */
    private static final String DEFAULT_NATURE = "CONSTANT_SPIN_SLEW";

    /** Value of the angular velocity constraint. */
    private final double constraint;

    /** Nature. */
    private final String nature;

    /**
     * Builds an instance from an angular velocity constraint.
     * 
     * @param constraintValue angular velocity constraint
     */
    public ConstantSpinSlewComputer(final double constraintValue) {
        this(constraintValue, DEFAULT_NATURE);
    }

    /**
     * Builds an instance from an angular velocity constraint.
     * 
     * @param constraintValue angular velocity constraint
     * @param natureIn leg nature
     */
    public ConstantSpinSlewComputer(final double constraintValue, final String natureIn) {
        this.constraint = constraintValue;
        this.nature = natureIn;
    }

    /**
     * Compute the slew.
     * @param pvProv satellite PV coordinates through time
     * @param initialLaw initial attitude law (before the slew)
     * @param initialDate slew start date (null if slew defined with its end date)
     * @param finalLaw final attitude law (after the slew)
     * @param finalDate slew end date (null if slew defined with its start date)
     * @return built slew
     * @throws PatriusException thrown if computation failed
     */
    public ConstantSpinSlew compute(final PVCoordinatesProvider pvProv, final AttitudeProvider initialLaw,
            final AbsoluteDate initialDate,
            final AttitudeProvider finalLaw,
            final AbsoluteDate finalDate) throws PatriusException {

        // Get slew date and check consistency
        checkInputs(initialDate, finalDate);
        final AbsoluteDate slewDate = initialDate != null ? initialDate : finalDate;
        final boolean isStart = initialDate != null;
        
        // Initialization
        final Attitude initAtt;
        final Attitude finalAtt;

        // constraintType == Constraint.ANGULAR_VELOCITY
        // in this case, we have to compute the duration

        // solver initialization
        double duration = 0;
        final UnivariateSolver solver = new BrentSolver();
        final int maxIteration = DEFAULT_MAX_ITERATIONS;
        final double maxDuration = DEFAULT_MAX_DURATION;

        if (isStart) {
            // in this case, the initial attitude is known : the law and the date are given
            initAtt = initialLaw.getAttitude(pvProv, slewDate, FramesFactory.getGCRF());
            final UnivariateFunction f = new UnivariateFunction() {
                /** Serializable UID. */
                private static final long serialVersionUID = -2510706897603231509L;

                // this function returns the angular distance between
                // the initial law and the final spaced from the given duration
                /** {@inheritDoc} */
                @Override
                public double value(final double t) {
                    final AbsoluteDate date = slewDate.shiftedBy(t);
                    try {
                        final Rotation finalRot = finalLaw.getAttitude(pvProv, date, FramesFactory.getGCRF())
                                .getRotation();
                        return MathLib.divide(Rotation.distance(initAtt.getRotation(), finalRot), t)
                                - ConstantSpinSlewComputer.this.constraint;
                    } catch (final PatriusException e) {
                        throw new PatriusExceptionWrapper(e);
                    }
                }
            };
            // duration computation
            // Slew duration should be at least Precision.EPSILON (cannot be zero)
            duration = solver.solve(maxIteration, f, Precision.EPSILON, maxDuration);
            // final attitude computation
            finalAtt = finalLaw.getAttitude(pvProv, slewDate.shiftedBy(duration), FramesFactory.getGCRF());

        } else {
            // case : isStart is false
            // in this case, the final attitude is known : the law and the date are given
            finalAtt = finalLaw.getAttitude(pvProv, slewDate, FramesFactory.getGCRF());
            final UnivariateFunction f = new UnivariateFunction() {
                /** Serializable UID. */
                private static final long serialVersionUID = 5609388475585670590L;

                // this function returns the angular distance between
                // the initial law and the final spaced from the given duration
                /** {@inheritDoc} */
                @Override
                public double value(final double t) {
                    final AbsoluteDate date = slewDate.shiftedBy(-t);
                    try {
                        final Rotation initialRot = initialLaw.getAttitude(pvProv, date, FramesFactory.getGCRF())
                                .getRotation();
                        return MathLib.divide(Rotation.distance(initialRot, finalAtt.getRotation()), t)
                                - ConstantSpinSlewComputer.this.constraint;
                    } catch (final PatriusException e) {
                        throw new PatriusExceptionWrapper(e);
                    }
                }
            };
            // duration computation
            // Slew duration should be at least Precision.EPSILON (cannot be zero)
            duration = solver.solve(maxIteration, f, Precision.EPSILON, maxDuration);
            // initial attitude computation
            initAtt = initialLaw.getAttitude(pvProv, slewDate.shiftedBy(-duration), FramesFactory.getGCRF());
        }
        
        // Return slew
        return new ConstantSpinSlew(initAtt, finalAtt, constraint, nature);
    }

    /**
     * Check input dates
     * @param initialDate slew initial date
     * @param finalDate slew final date
     * @throws PatriusException thrown if dates are incoherent
     */
    private static void checkInputs(final AbsoluteDate initialDate, final AbsoluteDate finalDate)
            throws PatriusException {
        // Check type of slew
        if ((initialDate == null && finalDate == null) || (initialDate != null && finalDate != null)) {
            // Incoherent case
            throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
        }
    }
}
