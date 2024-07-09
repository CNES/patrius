package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.legs.StrictLegsSequence;

/**
 * A “base” implementation of an <i>attitude legs sequence</i>.
 * This implementation has strict legs which means legs cannot be simultaneous or overlap and are strictly ordered by
 * starting date.
 *
 * @param <L>
 *        Any {@code AttitudeLeg} class.
 * 
 * @author Julien Anxionnat (CNES — DSO/DV/MP)
* HISTORY
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* END-HISTORY
 *
 * @see AttitudeLeg
 * 
 * @since 4.7
 */
public class StrictAttitudeLegsSequence<L extends AttitudeLeg> extends StrictLegsSequence<L> implements
        AttitudeProvider {

    /** Serializable’s UID. */
    private static final long serialVersionUID = 3148900747224919462L;

    /** Spin derivative computation. */
    private boolean computeSpinDerivatives = false;

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        final L leg = this.current(date);
        if (leg != null) {
            return leg.getAttitude(pvProv, date, frame);
        } else {
            throw new PatriusException(PatriusMessages.DATE_OUTSIDE_ATTITUDE_SEQUENCE, date);
        }
    };

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivativesIn) {
        this.computeSpinDerivatives = computeSpinDerivativesIn;
        for (final AttitudeLeg attitudeLeg : this) {
            attitudeLeg.setSpinDerivativesComputation(computeSpinDerivativesIn);
        }
    }

    /**
     * Returns the spin derivatives computation flag.
     * @return the spin derivatives computation flag
     */
    public boolean isSpinDerivativesComputation() {
        return computeSpinDerivatives;
    }
}
