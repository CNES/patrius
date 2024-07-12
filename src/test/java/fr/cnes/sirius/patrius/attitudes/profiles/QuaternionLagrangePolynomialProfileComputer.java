/**
 * HISTORY
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Besoin de forcer la normalisation dans la classe QuaternionPolynomialSegment
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3156:10/05/2022:[Patrius] Erreur dans le calcul de l'attitude par QuaternionPolynomialProfile ...
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * The class aims at computing polynomial attitude quaternion profiles.
 *
 * @author Miguel Morere
 *
 * @since 4.9
 *
 */
public class QuaternionLagrangePolynomialProfileComputer implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -5706892092595644309L;

    /**
     * Pico second
     */
    private static final double PICO_SECOND = 1.E-12;

    /**
     * Default nature of a single polynomial segment
     */
    private static final String DEFAULT_NATURE = "QUATERNION_POLYNOMIAL_PROFILE";

    /**
     * Frame in which the coefficients are expressed
     */
    private final Frame coefsExprFrame;

    /**
     * Order of the polynomial
     */
    private final int order;

    /**
     * Constructor
     *
     * @param coefsExprFrame
     *        Frame in which the coefficients are expressed
     * @param order
     *        Order of the polynomial
     */
    public QuaternionLagrangePolynomialProfileComputer(final Frame coefsExprFrame, final int order) {

        super();
        this.coefsExprFrame = coefsExprFrame;
        this.order = order;
    }

    /**
     * Compute the quaternion Lagrange polynomial profile.
     *
     * @param attLegsSequence
     *        Sequence of attitude legs whose profile is desired
     * @param pvProv
     *        Spacecraft PV coordinates provider
     * @return AttitudeProfileServiceResponse
     * @throws PatriusException
     *         If the computation cannot be performed:
     *         <ul>
     *         <li>The input {@link PVCoordinatesProvider} is <code>null</code> and it is required to get a LOF</li>
     *         <li>If the computation interval is not covered by the attitude provider</li>
     *         </ul>
     */
    public QuaternionPolynomialProfile compute(final StrictAttitudeLegsSequence<AttitudeLeg> attLegsSequence,
                                               final PVCoordinatesProvider pvProv)
        throws PatriusException {

        return this.compute(attLegsSequence, pvProv, attLegsSequence.getTimeInterval(), DEFAULT_NATURE);
    }

    /**
     * Compute the quaternion Lagrange polynomial profile with the given date interval and segment nature.
     *
     * @param attLegsSequence
     *        Sequence of attitude legs whose profile is desired
     * @param pvProv
     *        Spacecraft PV coordinates provider
     * @param interval
     *        Date interval on which the polynomial profile should be computed
     * @param nature
     *        Nature of the polynomial segment
     * @return the attitude profile service response
     * @throws PatriusException
     *         If the computation cannot be performed:
     *         <ul>
     *         <li>The input {@link PVCoordinatesProvider} is <code>null</code> and it is required to get a LOF</li>
     *         <li>If the computation interval is not covered by the attitude provider</li>
     *         </ul>
     */
    public QuaternionPolynomialProfile compute(final StrictAttitudeLegsSequence<AttitudeLeg> attLegsSequence,
                                               final PVCoordinatesProvider pvProv,
                                               final AbsoluteDateInterval interval, final String nature)
        throws PatriusException {

        return computeProfile(attLegsSequence, pvProv, interval, nature);
    }

    /**
     * This methods split the polynomial profile into polynomial segments in order to respect the constraint on the
     * maximum acceptable error. The error between the ideal profile and the polynomial profile is evaluated with the
     * angular velocities sampling step. If the error exceeds the maximum acceptable error, then the profile is divided
     * into as many polynomial segments as needed to respect the maximum error constraint.
     *
     * @param attLegsSequence
     *        Sequence of attitude legs whose profile is desired
     * @param pvProv
     *        Spacecraft PV coordinates provider
     * @param interval
     *        Date interval on which the polynomial profile should be computed
     * @param nature
     *        Nature of the polynomial segment
     * @return the angular velocities polynomial attitude profile
     *
     * @throws PatriusException
     *         If the computation cannot be performed:
     *         <ul>
     *         <li>The input {@link PVCoordinatesProvider} is <code>null</code> and it is required to get a LOF</li>
     *         <li>If the computation interval is not covered by the attitude provider</li>
     *         </ul>
     */
    private QuaternionPolynomialProfile computeProfile(final StrictAttitudeLegsSequence<AttitudeLeg> attLegsSequence,
                                                       final PVCoordinatesProvider pvProv,
                                                       final AbsoluteDateInterval interval,
                                                       final String nature)
        throws PatriusException {

        // Get the coefficients expression frame
        final Frame coefsExprFrame = this.coefsExprFrame;

        final List<QuaternionPolynomialSegment> polynomials = new ArrayList<>();

        // Loop on the attitude legs
        for (final AttitudeLeg leg : attLegsSequence) {
            // Compute the quaternion polynomial segment and add it to the list
            polynomials.add(computeSegment(leg, pvProv, coefsExprFrame, leg.getTimeInterval(), this.order,
                DEFAULT_NATURE));
        }

        // Create and return the quaternion polynomial profile
        return new QuaternionPolynomialProfile(coefsExprFrame, interval, polynomials);
    }

    /**
     * Compute an angular velocities polynomial segment on a given interval for a given attitude leg.
     *
     * @param attProv
     *        Attitude provider whose polynomial profile should be provided
     * @param pvProv
     *        Spacecraft PV coordinates provider
     * @param coefsExprFrame
     *        Frame in which the coefficients are expressed
     * @param interval
     *        Date interval on which the polynomial profile should be computed. It must be included in the interval of
     *        the definition of the attitude leg.
     * @param order
     *        Order of the polynomial
     * @param nature
     *        Nature of the current polynomial segment
     * @return The angular velocities polynomial segment of the given leg, on the given interval.
     * @throws PatriusException
     *         If the computation interval is not covered by the attitude provider
     */
    private static QuaternionPolynomialSegment computeSegment(final AttitudeProvider attProv,
                                                       final PVCoordinatesProvider pvProv,
                                                       final Frame coefsExprFrame, final AbsoluteDateInterval interval,
                                                       final int order,
                                                       final String nature)
        throws PatriusException {

        // Initial and final times
        // 1E-12s are subtracted from tFinal because numerical errors when reconstructing the date can lead to
        // exceptions.
        final AbsoluteDate tInitial = interval.getLowerData();
        final double duration = interval.getDuration();
        final double tFinal = interval.getDuration() - PICO_SECOND;
        final double timestep = tFinal / order;

        // Polynomial function for the X component of the velocity

        final double[] t0 = new double[order + 1];
        final double[] q0v = new double[order + 1];
        final double[] q1v = new double[order + 1];
        final double[] q2v = new double[order + 1];
        final double[] q3v = new double[order + 1];
        AbsoluteDate current;
        // Loop on the orders of the polynomial
        for (int i = 0; i < (order + 1); i++) {
            t0[i] = i * timestep;
            current = tInitial.shiftedBy(t0[i]);
            // Reduced time
            t0[i] = t0[i] / duration;
            final double[] qv = attProv.getAttitude(pvProv, current, coefsExprFrame).getRotation().getQi();
            q0v[i] = qv[0];
            q1v[i] = qv[1];
            q2v[i] = qv[2];
            q3v[i] = qv[3];
        }

        // Create the polynomial function Lagrange forms
        final PolynomialFunctionLagrangeForm q0 = new PolynomialFunctionLagrangeForm(t0, q0v);
        final PolynomialFunctionLagrangeForm q1 = new PolynomialFunctionLagrangeForm(t0, q1v);
        final PolynomialFunctionLagrangeForm q2 = new PolynomialFunctionLagrangeForm(t0, q2v);
        final PolynomialFunctionLagrangeForm q3 = new PolynomialFunctionLagrangeForm(t0, q3v);

        // Create and return the quaternion polynomial segment
        return new QuaternionPolynomialSegment(q0, q1, q2, q3, interval, false);

    }

}
