package fr.cnes.sirius.patrius.stela.forces.solaractivity.constant;

import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.IStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

import java.io.IOException;
import java.text.ParseException;

/**
 * Constant model of solar activity.
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaConstantSolarActivity extends ConstantSolarActivity implements IStelaSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = -8000969588699990575L;

    /**
     * F10.7 value
     */
    private final double f107;

    /**
     * AP value
     */
    private final double ap;

    /**
     * Solar activity type.
     */
    protected StelaSolarActivityType solActType;

    /**
     * Basis constructor
     *
     * @param f107 F10.7 value
     * @param ap AP value
     */
    public StelaConstantSolarActivity(final double f107, final double ap) throws PatriusException {
        super(f107, ap);
        this.solActType = StelaSolarActivityType.CONSTANT;
        this.f107 = super.getInstantFluxValue(null);
        this.ap = super.getAp(null);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getSolarActivity(final AbsoluteDate date) throws PatriusException {
        return new double[] { this.f107, this.f107, this.ap, this.ap, this.ap, this.ap, this.ap, this.ap, this.ap };
    }

    /** {@inheritDoc} */
    @Override
    public StelaSolarActivityType getSolActType() {
        return this.solActType;
    }

    /** {@inheritDoc} */
    @Override
    public StelaConstantSolarActivity copy() throws PatriusException, IOException, ParseException {
        return new StelaConstantSolarActivity(this.f107, this.ap);
    }
}
