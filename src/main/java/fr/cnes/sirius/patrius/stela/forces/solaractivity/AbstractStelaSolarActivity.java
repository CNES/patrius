package fr.cnes.sirius.patrius.stela.forces.solaractivity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Solar activity abstract class.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public abstract class AbstractStelaSolarActivity implements IStelaSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = -7568390582372826992L;

    /**
     * Number 8
     */
    private static final int NB_DIGITS = 8;

    /**
     * Solar activity type.
     */
    private final StelaSolarActivityType solActType;

    /**
     * Basis constructor
     *
     * @param solActType the solar activity type
     */
    protected AbstractStelaSolarActivity(final StelaSolarActivityType solActType) {
        this.solActType = solActType;
    }

    /**
     * Get the Stela decimal format
     *
     * @return the Stela decimal format
     */
    protected static DecimalFormat getStelaDoubleFormat() {
        // We always want the english formatting
        final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(new Locale("en"));
        // We don't want the hundreds separator
        final DecimalFormat rez = new DecimalFormat("#0.###", dfs);
        // Set max number of fraction digits
        rez.setMaximumFractionDigits(NB_DIGITS);
        return rez;
    }

    /** {@inheritDoc} */
    @Override
    public StelaSolarActivityType getSolActType() {
        return this.solActType;
    }
}
