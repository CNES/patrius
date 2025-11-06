/**
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-129:22/08/2024: [PATRIUS] Interpolation de trajectoire avec la methode de Lagrange
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;


/**
 * Abstract class defining common methods and elements for an interpolation of an ephemeris PV via Lagrange-Hermite
 * methods.
 * 
 * @author Nicola NATALE, Manuel AMOUROUX
 *
 */
public abstract class AbstractEphemerisPvHermiteLagrange extends AbstractBoundedPVProvider {

    /** Serial version UID of the class */
    private static final long serialVersionUID = -1553614468092862108L;

    /** Ephemeris Hermite interpolator */
    protected HermiteInterpolator interpolator;

    /**
     * Instantiation of AbstractEphemerisPvHermiteLagrange attributes.<br>
     * Only used by children classes.
     * 
     * @param tabPV
     *        position velocity coordinates table
     *        (table is not copied and so internal class state can be modified from outside)
     * @param order
     *        Lagrange/Hermite interpolation order. It must be even.
     * @param frame
     *        coordinates expression frame
     * @param tabDate
     *        table of dates for each position velocity
     *        (table is not copied and so internal class state can be modified from outside)
     * @param algo
     *        class to find the nearest date index from a given date in the date table
     *        (If null, algo will be, by default, a BinarySearchIndexOpenClosed
     *        based on a table of duration since the first date of the dates table)
     */
    protected AbstractEphemerisPvHermiteLagrange(final PVCoordinates[] tabPV, final int order, final Frame frame,
                                                 final AbsoluteDate[] tabDate, final ISearchIndex algo) {
        super(tabPV, order, frame, tabDate, algo);
    }

    /**
     * Creates an instance of AbstractEphemerisPvHermiteLagrange from a SpacecraftState table.<br>
     * Only used by children classes.
     * 
     * @param tabState
     *        SpacecraftState table
     * @param order
     *        Lagrange/Hermite interpolation order. It must be even.
     * @param algo
     *        class to find the nearest date index from a given date
     *        (If null, algo will be BinarySearchIndexOpenClosed by default
     *        based on a table of duration since the first date of the dates table)
     */
    protected AbstractEphemerisPvHermiteLagrange(final SpacecraftState[] tabState, final int order,
                                                 final ISearchIndex algo) {
        super(tabState, order, algo);
    }

    /**
     * Common method to get PV coordinates using Hermite interpolation, either in a "Lagrange-like" manner, or in a
     * traditional manner.<br>
     * The "Lagrange-like" manner uses a Hermite interpolation, without acceleration, and considers the velocities as
     * additional mesh dimensions, not derivatives. We therefore interpolate a 6D vector, without derivatives. Using
     * this interpolation rather than the traditional Lagrange one improves significantly the speed, with numerical
     * errors considered acceptable.<br>
     * The traditional manner uses a Hermite interpolation, with velocities vector as derivatives, and optional
     * accelerations as second order derivatives.
     * 
     * @param date
     *        date of interpolation
     * @param frame
     *        frame of coordinates expression. (can be null)
     * @param tAcc
     *        date of interpolation
     * @param velocityAsDerivative
     *        The velocities are considered as additional mesh dimensions (Lagrange-like interpolation) when true, as
     *        derivatives when false
     * @throws PatriusException
     *         if date of interpolation is too near from min and max input dates
     *         compare to Lagrange order
     * @return PVcoordinates at interpolation date in the chosen frame
     */
    protected PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame, final Vector3D[] tAcc,
                                             final boolean velocityAsDerivative)
        throws PatriusException {

        // Duration from reference to search index
        final double duration = date.durationFrom(this.getDateRef());

        // Check if date is exactly on validity interval bounds, in that case (!= null) returns boundary state
        PVCoordinates interpolPV = this.checkBounds(date);

        if (interpolPV == null) {
            // get the nearest index for this duration
            final int index = this.getSearchIndex().getIndex(duration);

            // the interpolation is valid only if 0<= index +1 -interpoOrder/2 or index + order/2 <= maximalIndex
            final int i0 = this.indexValidity(index);

            // checks if this index has already been considered and stores for future computations
            if (index != this.getPreviousIndex()) {

                this.interpolator = new HermiteInterpolator();
                this.setPreviousIndex(index);

                /// get the PV coordinates and the delta t from startDate
                double deltat;
                double[] pos;
                double[] vel;
                double[] acc;
                for (int i = 0; i < this.polyOrder; i++) {
                    pos = this.tPVCoord[i0 + i].getPosition().toArray();
                    vel = this.tPVCoord[i0 + i].getVelocity().toArray();

                    if (velocityAsDerivative) {
                        // Classical Hermite interpolation : velocities (and acceleration if provided) are considered as
                        // derivative

                        deltat = this.tDate[i0 + i].durationFrom(this.tDate[i0]);
                        // If acceleration table is available, compute interpolation using acceleration
                        if (tAcc == null) {
                            this.interpolator.addSamplePoint(deltat, pos, vel);
                        } else {
                            acc = tAcc[i0 + i].toArray();
                            this.interpolator.addSamplePoint(deltat, pos, vel, acc);
                        }
                    } else {
                        deltat = this.tDate[i0 + i].durationFrom(this.getDateRef());
                        // Lagrange type interpolation with HermiteInterpolator (derivatives not used)
                        final double[] pv = new double[] { pos[0], pos[1], pos[2], vel[0], vel[1], vel[2] };

                        this.interpolator.addSamplePoint(deltat, pv);
                    }
                }
            }

            // Duration from closest date in order to minimize numerical quality issues
            final double durationI0 = date.durationFrom(this.tDate[i0]);

            // Get the interpolation results
            if (velocityAsDerivative) {
                // Hermite interpolation
                // Get the hermite interpolation results
                final Vector3D p = new Vector3D(this.interpolator.value(durationI0));
                final Vector3D v = new Vector3D(this.interpolator.derivative(durationI0));

                interpolPV = new PVCoordinates(p, v);
            } else {
                final double[] pvArray = this.interpolator.value(duration);
                final Vector3D p = new Vector3D(pvArray[0], pvArray[1], pvArray[2]);
                final Vector3D v = new Vector3D(pvArray[3], pvArray[4], pvArray[5]);

                interpolPV = new PVCoordinates(p, v);
            }
        }

        // If needed, convert position, velocity to the right frame
        if ((frame != null) && (this.getFrame() != frame)) {
            final Transform t = this.getFrame().getTransformTo(frame, date);
            interpolPV = t.transformPVCoordinates(interpolPV);
        }

        return interpolPV;
    }
}
