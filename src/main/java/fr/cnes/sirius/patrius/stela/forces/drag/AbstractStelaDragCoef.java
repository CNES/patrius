package fr.cnes.sirius.patrius.stela.forces.drag;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract class for drag coefficient
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public abstract class AbstractStelaDragCoef implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3039914832344045354L;

    /**
     * Maximum fraction digits
     */
    public static final int MAXIMUM_FRACTION_DIGITS = 50;

    /**
     * Drag coefficient type
     */
    protected final StelaDragCoefType stelaDragCoefType;

    /**
     * Basis constructor
     *
     * @param stelaDragCoefType Drag coefficient type
     */
    protected AbstractStelaDragCoef(final StelaDragCoefType stelaDragCoefType) {
        this.stelaDragCoefType = stelaDragCoefType;
    }

    /**
     * Get the drag coefficient type.
     * 
     * @return drag coefficient type
     */
    public StelaDragCoefType getDragCoefType() {
        return this.stelaDragCoefType;
    }

    /**
     * Get the drag coefficient value.
     * 
     * @param stelaDragCoefInput input necessary for drag coefficient computation
     * @return drag coefficient
     * @throws PatriusException thrown if a problem occurs while processing the drag coefficient computation
     */
    public abstract double getDragCoef(final StelaDragCoefInput stelaDragCoefInput) throws PatriusException;

    /**
     * Get statistical drag coefficient information.
     *
     * @param data the data to format
     * 
     * @return statistical drag coefficient information
     */
    protected static String getStatInformation(final double data) {
        // We always want the english formatting
        final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(new Locale("en"));
        // We don't want the hundreds separator
        final DecimalFormat rez = new DecimalFormat("#0.###", dfs);
        // Set max number of fraction digits
        rez.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

        return rez.format(data);
    }

    /**
     * Copy drag coefficient.
     *
     * @return copied drag coefficient
     */
    public abstract AbstractStelaDragCoef copy();

}
